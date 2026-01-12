
package auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.JWTVerifier;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Date;

/**
 * Servicio de autenticación listo para despliegue distribuido.
 * DB_URL, DB_USER y DB_PASS se pueden parametrizar por variables de entorno.
 * Ejemplo de ejecución:
 *   DB_URL=jdbc:mysql://host/db DB_USER=usuario DB_PASS=clave java -cp ... auth.AuthService
 */
public class AuthService {
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
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
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
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
                exchange.close();
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            System.out.println("AuthService escuchando en el puerto " + port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static final String JWT_SECRET = "super_secret_key_2026";
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "root");
    private final Gson gson = new Gson();

    public String register(String curp, String password, String name) {
        if (curp == null || password == null || name == null) {
            return "{\"error\": \"Datos incompletos\"}";
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE curp = ?");
            checkStmt.setString(1, curp);
            if (checkStmt.executeQuery().next()) {
                conn.rollback();
                return "{\"error\": \"El CURP ya está registrado\"}";
            }
            String hashedPassword = hashPassword(password);
            String sqlUser = "INSERT INTO users (name, curp, password, role) VALUES (?, ?, ?, 'USER')";
            PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, name);
            psUser.setString(2, curp);
            psUser.setString(3, hashedPassword);
            int affectedRows = psUser.executeUpdate();
            if (affectedRows == 0) { conn.rollback(); throw new SQLException("Fallo al crear usuario"); }
            long newUserId = 0;
            try (ResultSet generatedKeys = psUser.getGeneratedKeys()) {
                if (generatedKeys.next()) newUserId = generatedKeys.getLong(1);
                else { conn.rollback(); throw new SQLException("No se obtuvo ID"); }
            }
            String sqlAcc = "INSERT INTO accounts (user_id, balance) VALUES (?, ?)";
            PreparedStatement psAcc = conn.prepareStatement(sqlAcc);
            psAcc.setLong(1, newUserId);
            psAcc.setDouble(2, 0.00);
            psAcc.executeUpdate();
            conn.commit();
            return "{\"success\": true, \"message\": \"Usuario registrado\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Error interno\"}";
        }
    }

    public String login(String curp, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT id, name, role FROM users WHERE curp = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
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
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Error BD\"}";
        }
    }

    public DecodedJWT validateJWT(HttpExchange exchange) throws java.io.IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(JWT_SECRET)).build();
            return verifier.verify(token);
        } catch (Exception e) {
            return null;
        }
    }

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
