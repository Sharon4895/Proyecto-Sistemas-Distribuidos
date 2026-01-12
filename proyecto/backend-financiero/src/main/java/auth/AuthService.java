package auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Date;
import java.util.Properties;

public class AuthService {
    
    // Variables estáticas para la configuración
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASS;
    private static final String JWT_SECRET = "super_secret_key_2026";
    private final Gson gson = new Gson();

    // Bloque estático para cargar la configuración al iniciar
    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
            System.out.println(">>> Configuración cargada desde config.properties");
        } catch (IOException e) {
            System.err.println(">>> No se encontró config.properties, usando valores por defecto/entorno.");
        }

        // Prioridad: 1. Variable de Entorno, 2. Archivo config.properties, 3. Default (localhost)
        DB_URL = System.getenv().getOrDefault("DB_URL", props.getProperty("db.url", "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true"));
        DB_USER = System.getenv().getOrDefault("DB_USER", props.getProperty("db.user", "root"));
        DB_PASS = System.getenv().getOrDefault("DB_PASS", props.getProperty("db.pass", "root"));
        
        System.out.println(">>> Conectando a DB en: " + DB_URL);
    }

    public static void main(String[] args) {
        int port = 8081;
        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);
            AuthService service = new AuthService();
            
            server.createContext("/login", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                        java.util.Map<String, String> body = new com.google.gson.Gson().fromJson(isr, java.util.Map.class);
                        String curp = body.get("curp");
                        String password = body.get("password");
                        String response = service.login(curp, password);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                        exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                    }
                } else { exchange.sendResponseHeaders(405, -1); }
                exchange.close();
            });

            server.createContext("/register", exchange -> {
                if ("POST".equals(exchange.getRequestMethod())) {
                    try (InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                        java.util.Map<String, String> body = new com.google.gson.Gson().fromJson(isr, java.util.Map.class);
                        String curp = body.get("curp");
                        String password = body.get("password");
                        String name = body.get("name");
                        String response = service.register(curp, password, name);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                        exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                    }
                } else { exchange.sendResponseHeaders(405, -1); }
                exchange.close();
            });

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            System.out.println("AuthService escuchando en el puerto " + port);
            server.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public String register(String curp, String password, String name) {
        if (curp == null || password == null || name == null) return "{\"error\": \"Datos incompletos\"}";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            // Verificar si existe
            try (PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE curp = ?")) {
                checkStmt.setString(1, curp);
                if (checkStmt.executeQuery().next()) {
                    conn.rollback();
                    return "{\"error\": \"El CURP ya está registrado\"}";
                }
            }

            String hashedPassword = hashPassword(password);
            String sqlUser = "INSERT INTO users (name, curp, password, role) VALUES (?, ?, ?, 'USER')";
            long newUserId = 0;
            
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, name);
                psUser.setString(2, curp);
                psUser.setString(3, hashedPassword);
                int affectedRows = psUser.executeUpdate();
                if (affectedRows == 0) { conn.rollback(); throw new SQLException("Fallo al crear usuario"); }
                try (ResultSet generatedKeys = psUser.getGeneratedKeys()) {
                    if (generatedKeys.next()) newUserId = generatedKeys.getLong(1);
                    else { conn.rollback(); throw new SQLException("No se obtuvo ID"); }
                }
            }

            String sqlAcc = "INSERT INTO accounts (user_id, balance) VALUES (?, ?)";
            try (PreparedStatement psAcc = conn.prepareStatement(sqlAcc)) {
                psAcc.setLong(1, newUserId);
                psAcc.setDouble(2, 0.00);
                psAcc.executeUpdate();
            }
            
            conn.commit();
            return "{\"success\": true, \"message\": \"Usuario registrado\"}";
        } catch (Exception e) {
            e.printStackTrace(); // Imprime el error real en la consola
            return "{\"error\": \"Error interno: " + e.getMessage() + "\"}";
        }
    }

    public String login(String curp, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT id, name, role FROM users WHERE curp = ? AND password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, curp);
                pstmt.setString(2, hashPassword(password));
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String name = rs.getString("name");
                    String role = rs.getString("role");
                    String token = JWT.create()
                        .withClaim("curp", curp)
                        .withClaim("name", name)
                        .withClaim("role", role)
                        .withExpiresAt(new Date(System.currentTimeMillis() + 86400000))
                        .sign(Algorithm.HMAC256(JWT_SECRET));
                    return String.format("{\"success\": true, \"token\": \"%s\"}", token);
                } else {
                    return "{\"success\": false, \"message\": \"Credenciales inválidas\"}";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Error de conexión a BD: " + e.getMessage() + "\"}";
        }
    }

    // Método hashPassword y validateJWT se mantienen igual...
    private String hashPassword(String plainText) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
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
}