import admin.AdminService;
import transaction.TransactionService;
import account.AccountService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson; 
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.*;
import auth.AuthService;

public class WebServer {
    private final AdminService adminService = new AdminService();
    private final TransactionService transactionService = new TransactionService();
    private final AccountService accountService = new AccountService();
    // Método auxiliar para validar JWT y obtener claims
    private DecodedJWT validateToken(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse("{\"error\": \"Token faltante\"}", exchange, 401);
            return null;
        }
        String token = authHeader.substring(7);
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(JWT_SECRET)).build();
            return verifier.verify(token);
        } catch (Exception e) {
            sendResponse("{\"error\": \"Token inválido\"}", exchange, 401);
            return null;
        }
    }
    private static final String JWT_SECRET = "super_secret_key_2026";

    private static final String DB_URL = "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    // --- ENDPOINTS ---
    private static final String LOGIN_ENDPOINT = "/api/auth/login"; 
    private static final String BALANCE_ENDPOINT = "/api/account/balance";
    private static final String TRANSACTIONS_ENDPOINT = "/api/transactions";
    private static final String OPERATE_ENDPOINT = "/api/account/operate";
    private static final String TRANSFER_ENDPOINT = "/api/account/transfer";

    private final int port;
    private HttpServer server;
    private final Gson gson = new Gson(); 
    private final AuthService authService = new AuthService();

    public static void main(String[] args) {
        int serverPort = 8080;
        if (args.length == 1) {
            serverPort = Integer.parseInt(args[0]);
        }
        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();
        System.out.println(">>> Servidor Financiero COMPLETISIMO v3 escuchando en el puerto " + serverPort);
    }

    public WebServer(int port) {
        this.port = port;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Registrar TODAS las rutas
        server.createContext(LOGIN_ENDPOINT, this::handleLoginRequest);
        server.createContext(BALANCE_ENDPOINT, this::handleBalanceRequest);
        server.createContext(TRANSACTIONS_ENDPOINT, this::handleTransactionsRequest);
        server.createContext(OPERATE_ENDPOINT, this::handleOperationRequest);
        server.createContext(TRANSFER_ENDPOINT, this::handleTransferRequest);
        // Endpoint temporal para depuración
        server.createContext("/api/debug/users", this::handleDebugUsersRequest);
        
        // Rutas de Admin y Registro
        server.createContext("/api/admin/stats", this::handleAdminStatsRequest);
        server.createContext("/api/admin/users", this::handleAdminUsersRequest);
        server.createContext("/api/admin/charts", this::handleAdminChartsRequest);
        server.createContext("/api/admin/user-logs", this::handleAdminUserLogsRequest);
        server.createContext("/api/auth/register", this::handleRegisterRequest);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }
    // Endpoint temporal para depuración: lista los últimos 5 usuarios
    private void handleDebugUsersRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement ps = conn.prepareStatement("SELECT id, name, curp FROM users ORDER BY id DESC LIMIT 5");
            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"curp\":\"%s\"}", rs.getLong("id"), rs.getString("name"), rs.getString("curp")));
                first = false;
            }
            json.append("]");
            sendResponse(json.toString(), exchange, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("[]", exchange, 500);
        }
    }

    // ================= HANDLERS (MÉTODOS) =================

    // 1. REGISTRO (Delegado a AuthService)
    private void handleRegisterRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        RegisterRequest req = gson.fromJson(isr, RegisterRequest.class);
        String result = authService.register(req.curp, req.password, req.name);
        int status = result.contains("success") ? 201 : (result.contains("ya está registrado") ? 409 : 400);
        sendResponse(result, exchange, status);
    }

    // 2. LOGIN (Delegado a AuthService)
    private void handleLoginRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        LoginRequest req = gson.fromJson(isr, LoginRequest.class);
        String result = authService.login(req.curp, req.password);
        int status = result.contains("success\": true") ? 200 : 401;
        sendResponse(result, exchange, status);
    }

    // 3. BALANCE (Delegado a AccountService)
    private void handleBalanceRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        DecodedJWT jwt = authService.validateJWT(exchange);
        if (jwt == null) { sendResponse("{\"error\": \"Token inválido\"}", exchange, 401); return; }
        String curp = jwt.getClaim("curp").asString();
        String result = accountService.getBalance(curp);
        int status = result.contains("error") ? 500 : 200;
        sendResponse(result, exchange, status);
    }

    // 4. OPERACIONES (Delegado a AccountService)
    private void handleOperationRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }
        DecodedJWT jwt = authService.validateJWT(exchange);
        if (jwt == null) { sendResponse("{\"error\": \"Token inválido\"}", exchange, 401); return; }
        String curp = jwt.getClaim("curp").asString();
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        OperationRequest req = gson.fromJson(isr, OperationRequest.class);
        String result = accountService.operate(curp, req.type, req.amount);
        int status = result.contains("success") ? 200 : 400;
        sendResponse(result, exchange, status);
    }

    // 5. TRANSFERENCIAS (Delegado a AccountService)
    private void handleTransferRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }
        DecodedJWT jwt = authService.validateJWT(exchange);
        if (jwt == null) { sendResponse("{\"error\": \"Token inválido\"}", exchange, 401); return; }
        String sourceCurp = jwt.getClaim("curp").asString();
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        TransferRequest req = gson.fromJson(isr, TransferRequest.class);
        String result = accountService.transfer(sourceCurp, req.targetCurp, req.amount, req.description);
        int status = result.contains("success") ? 200 : 400;
        sendResponse(result, exchange, status);
    }

    // 6. HISTORIAL (TRANSACTIONS)
    // Delegar a TransactionService
    private void handleTransactionsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        DecodedJWT jwt = authService.validateJWT(exchange);
        if (jwt == null) { sendResponse("[]", exchange, 401); return; }
        String curp = jwt.getClaim("curp").asString();
        String result = transactionService.getUserTransactions(curp);
        sendResponse(result, exchange, 200);
    }

    // --- MÉTODOS DE ADMIN (STATS, USERS, CHARTS, LOGS) ---
    
    private void handleAdminStatsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String result = adminService.getStats();
        sendResponse(result, exchange, 200);
    }

    private void handleAdminUsersRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String result = adminService.getUsers();
        sendResponse(result, exchange, 200);
    }

    private void handleAdminChartsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String result = adminService.getCharts();
        sendResponse(result, exchange, 200);
    }

    private void handleAdminUserLogsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String userId = parseQuery(exchange.getRequestURI().getQuery()).get("userId");
        if (userId == null) { sendResponse("[]", exchange, 400); return; }
        String result = adminService.getUserLogs(userId);
        sendResponse(result, exchange, 200);
    }

    // --- UTILERÍAS ---
    private boolean handleCORS(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization,X-User-Curp");
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void sendResponse(String json, HttpExchange exchange, int status) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) result.put(entry[0], entry[1]);
        }
        return result;
    }

    private String generateAccountNumber() {
        return "4152" + String.format("%012d", new Random().nextLong(1000000000000L));
    }

    private String hashPassword(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // --- DTOs ---
    static class LoginRequest { String curp, password; }
    static class RegisterRequest { String curp, password, name; }
    static class OperationRequest { String curp, type; double amount; }
    static class TransferRequest { String sourceCurp, targetCurp, description; double amount; }
    static class TransactionResponse { String id, date, type, description, status; double amount; TransactionResponse(String i, String d, String t, double a, String de, String s){id=i;date=d;type=t;amount=a;description=de;status=s;} }
}