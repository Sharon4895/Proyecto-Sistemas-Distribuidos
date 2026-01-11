import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class WebServer {

    // Configuración
    private static final int DEFAULT_PORT = 8080;
    private static final String JWT_SECRET = "super_secret_key_2026";
    private final int port;
    
    // URLs de Microservicios
    private final List<String> accountUrls = new ArrayList<>();
    private final List<String> transactionUrls = new ArrayList<>();
    private String authUrl;
    private String adminUrl;
    private String auditUrl;

    // Índices atómicos para Round-Robin (Thread-safe)
    private final AtomicInteger accountIdx = new AtomicInteger(0);
    private final AtomicInteger transactionIdx = new AtomicInteger(0);

    public static void main(String[] args) {
        int port = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new WebServer(port).startServer();
    }

    public WebServer(int port) {
        this.port = port;
        loadConfiguration();
    }

    private void loadConfiguration() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("src/config.properties")) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Advertencia: No se pudo leer config.properties, usando valores por defecto.");
        }

        this.authUrl = props.getProperty("auth.url", "http://localhost:8081");
        
        this.auditUrl = props.getProperty("audit.url", "http://localhost:8084");    
        this.adminUrl = props.getProperty("admin.url", "http://localhost:8085");
        
        loadListFromProp(props, "account.urls", "http://localhost:8082", accountUrls);
        loadListFromProp(props, "transaction.urls", "http://localhost:8083", transactionUrls);
    }

    private void loadListFromProp(Properties props, String key, String defaultVal, List<String> list) {
        list.clear();
        String val = props.getProperty(key, defaultVal);
        Arrays.stream(val.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(list::add);
    }

    public void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Rutas Públicas (Auth)
            server.createContext("/api/auth/register", ex -> proxyRequest(ex, authUrl + "/register", "POST"));
            server.createContext("/api/auth/login", ex -> proxyRequest(ex, authUrl + "/login", "POST"));

            // Rutas Protegidas (Account & Transactions)
            server.createContext("/api/account/balance", this::handleBalanceRequest);
            server.createContext("/api/account/operate", ex -> handleAuthenticatedProxy(ex, accountUrls, "/operate", "POST"));
            server.createContext("/api/account/transfer", ex -> {
                System.out.println("[LOG] Llega petición a /api/account/transfer: método=" + ex.getRequestMethod());
                handleAuthenticatedProxy(ex, accountUrls, "/transfer", "POST");
            });
            server.createContext("/api/transactions", this::handleTransactionsRequest);

            // Rutas Admin & Audit
            server.createContext("/api/admin/users", ex -> handleSimpleProxy(ex, adminUrl + "/users"));
            server.createContext("/api/admin/stats", ex -> handleSimpleProxy(ex, adminUrl + "/stats"));
            server.createContext("/api/admin/charts", ex -> handleSimpleProxy(ex, adminUrl + "/charts"));
            server.createContext("/api/admin/user-logs", ex -> handleSimpleProxy(ex, adminUrl + "/userlogs"));
            server.createContext("/api/admin", ex -> handleSimpleProxy(ex, adminUrl + ex.getRequestURI().getPath()));
            server.createContext("/api/audit/logs", ex -> handleSimpleProxy(ex, auditUrl + "/logs"));

            server.setExecutor(Executors.newFixedThreadPool(4));
            server.start();
            System.out.println(">>> API Gateway v5 escuchando en el puerto " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= HANDLERS ESPECÍFICOS =================

    // Handler para Balance: Requiere inyectar CURP en Query Params
    private void handleBalanceRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        DecodedJWT jwt = validateToken(exchange);
        if (jwt == null) return;
        String curp = jwt.getClaim("curp").asString();
        String targetUrl = getNextUrl(accountUrls, accountIdx) + "/balance?curp=" + curp;
        proxyRequest(exchange, targetUrl, "GET");
    }

    // Handler para Transacciones: Requiere inyectar CURP en Query Params
    private void handleTransactionsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        DecodedJWT jwt = validateToken(exchange);
        if (jwt == null) return;
        String curp = jwt.getClaim("curp").asString();
        String targetUrl = getNextUrl(transactionUrls, transactionIdx) + "/transactions?curp=" + curp;
        proxyRequest(exchange, targetUrl, "GET");
    }

    // Helper para rutas protegidas genéricas (POST/PUT)
    private void handleAuthenticatedProxy(HttpExchange exchange, List<String> urlPool, String path, String method) throws IOException {
        if (handleCORS(exchange)) return;
        DecodedJWT jwt = validateToken(exchange);
        if (jwt == null) return;
        String curp = jwt.getClaim("curp").asString();
        String targetUrl = getNextUrl(urlPool, accountIdx) + path;
        // Si es /operate y método POST, inyectar el curp en el body
        if ("/operate".equals(path) && method.equalsIgnoreCase("POST")) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exchange.getRequestBody().transferTo(baos);
            String bodyStr = baos.toString(StandardCharsets.UTF_8);
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(bodyStr).getAsJsonObject();
            json.addProperty("curp", curp);
            byte[] newBody = json.toString().getBytes(StandardCharsets.UTF_8);
            proxyRequestWithBody(exchange, targetUrl, method, newBody);
        } else if ("/transfer".equals(path) && method.equalsIgnoreCase("POST")) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exchange.getRequestBody().transferTo(baos);
            String bodyStr = baos.toString(StandardCharsets.UTF_8);
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(bodyStr).getAsJsonObject();
            json.addProperty("sourceCurp", curp);
            byte[] newBody = json.toString().getBytes(StandardCharsets.UTF_8);
            proxyRequestWithBody(exchange, targetUrl, method, newBody);
        } else {
            proxyRequest(exchange, targetUrl, method);
        }
    }

    // Proxy con body modificado
    private void proxyRequestWithBody(HttpExchange exchange, String targetUrl, String method, byte[] body) throws IOException {
        if (handleCORS(exchange)) return;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null) conn.setRequestProperty("Authorization", authHeader);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int status = conn.getResponseCode();
            InputStream respStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            byte[] responseBytes;
            if (respStream != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                respStream.transferTo(buffer);
                responseBytes = buffer.toByteArray();
                respStream.close();
            } else {
                responseBytes = new byte[0];
            }
            sendResponse(exchange, status, responseBytes);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 502, "{\"error\": \"Error de comunicación con microservicio: " + e.getMessage() + "\"}");
        }
    }
    

    // Helper para rutas simples sin modificación de URL
    private void handleSimpleProxy(HttpExchange exchange, String targetUrl) throws IOException {
        // Reenviar query string si existe
        String query = exchange.getRequestURI().getQuery();
        String fullUrl = targetUrl;
        if (query != null && !query.isEmpty()) {
            fullUrl += (targetUrl.contains("?") ? "&" : "?") + query;
        }
        proxyRequest(exchange, fullUrl, exchange.getRequestMethod());
    }

    // ================= NÚCLEO: PROXY GENÉRICO =================

    /**
     * Método centralizado para reenviar peticiones a microservicios.
     * Maneja Input/Output streams, cabeceras y códigos de respuesta.
     */
    private void proxyRequest(HttpExchange exchange, String targetUrl, String method) throws IOException {
        if (handleCORS(exchange)) return;
        
        // Validar método si es necesario, o forzar el método deseado
        if (!method.equals(exchange.getRequestMethod()) && !method.equals("GET")) {
             // Si forzamos POST pero viene OPTIONS, el CORS lo maneja. Si viene GET, error.
             if(!exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                 sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                 return;
             }
        }

        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            
            // Copiar cabeceras relevantes
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null) conn.setRequestProperty("Authorization", authHeader);
            conn.setRequestProperty("Content-Type", "application/json");

            // Si hay cuerpo (POST/PUT), reenviarlo
            if (exchange.getRequestMethod().equalsIgnoreCase("POST") || exchange.getRequestMethod().equalsIgnoreCase("PUT")) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream(); InputStream is = exchange.getRequestBody()) {
                    is.transferTo(os); // Java 9+: Transferencia eficiente de bytes
                }
            }

            // Leer respuesta del microservicio
            int status = conn.getResponseCode();
            InputStream respStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
            
            byte[] responseBytes;
            if (respStream != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                respStream.transferTo(buffer);
                responseBytes = buffer.toByteArray();
                respStream.close();
            } else {
                responseBytes = new byte[0];
            }

            // Enviar respuesta al cliente original
            sendResponse(exchange, status, responseBytes);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 502, "{\"error\": \"Error de comunicación con microservicio: " + e.getMessage() + "\"}");
        }
    }

    // ================= UTILIDADES =================

    private String getNextUrl(List<String> urls, AtomicInteger idx) {
        if (urls.isEmpty()) throw new RuntimeException("No hay URLs configuradas para el servicio");
        // Lógica Round-Robin segura
        int i = idx.getAndIncrement();
        if (i < 0) { idx.set(0); i = 0; } // Evitar overflow
        return urls.get(i % urls.size());
    }

    private DecodedJWT validateToken(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, "{\"error\": \"Token faltante\"}");
            return null;
        }
        try {
            String token = authHeader.substring(7);
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(JWT_SECRET)).build();
            return verifier.verify(token);
        } catch (Exception e) {
            sendResponse(exchange, 401, "{\"error\": \"Token inválido\"}");
            return null;
        }
    }

    private boolean handleCORS(HttpExchange exchange) throws IOException {
        if (!exchange.getResponseHeaders().containsKey("Access-Control-Allow-Origin")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
        }
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        sendResponse(exchange, status, json.getBytes(StandardCharsets.UTF_8));
    }

    private void sendResponse(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        // No añadir Access-Control-Allow-Origin aquí, solo en handleCORS
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}