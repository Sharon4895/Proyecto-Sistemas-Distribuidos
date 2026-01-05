import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson; 

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.*;
import java.security.MessageDigest; 

public class WebServer {

    // --- CONFIGURACIÓN DE BASE DE DATOS ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root"; // <--- ¡VERIFICA TU CONTRASEÑA!

    // --- ENDPOINTS ---
    private static final String LOGIN_ENDPOINT = "/api/auth/login"; 
    private static final String BALANCE_ENDPOINT = "/api/account/balance";
    private static final String TRANSACTIONS_ENDPOINT = "/api/transactions";
    private static final String OPERATE_ENDPOINT = "/api/account/operate";
    private static final String TRANSFER_ENDPOINT = "/api/account/transfer";

    private final int port;
    private HttpServer server;
    private final Gson gson = new Gson(); 

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
        
        // Rutas de Admin y Registro
        server.createContext("/api/admin/stats", this::handleAdminStatsRequest);
        server.createContext("/api/admin/users", this::handleAdminUsersRequest);
        server.createContext("/api/admin/charts", this::handleAdminChartsRequest);
        server.createContext("/api/admin/user-logs", this::handleAdminUserLogsRequest);
        server.createContext("/api/auth/register", this::handleRegisterRequest);

        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    // ================= HANDLERS (MÉTODOS) =================

    // 1. REGISTRO (CON HASH Y USERNAME)
    private void handleRegisterRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        RegisterRequest req = gson.fromJson(isr, RegisterRequest.class);

        if (req.curp == null || req.password == null || req.name == null) {
            sendResponse("{\"error\": \"Datos incompletos\"}", exchange, 400); return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false); 

            // Verificar duplicados
            PreparedStatement checkStmt = conn.prepareStatement("SELECT id FROM users WHERE curp = ?");
            checkStmt.setString(1, req.curp);
            if (checkStmt.executeQuery().next()) {
                conn.rollback();
                sendResponse("{\"error\": \"El CURP ya está registrado\"}", exchange, 409);
                return;
            }

            // Generar Username y Hash
            String generatedUsername = generateUniqueUsername(req.curp);
            String hashedPassword = hashPassword(req.password);

            // Insertar Usuario
            String sqlUser = "INSERT INTO users (name, username, curp, password, role) VALUES (?, ?, ?, ?, 'USER')";
            PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, req.name);
            psUser.setString(2, generatedUsername);
            psUser.setString(3, req.curp);
            psUser.setString(4, hashedPassword); 
            
            int affectedRows = psUser.executeUpdate();
            if (affectedRows == 0) { conn.rollback(); throw new SQLException("Fallo al crear usuario"); }

            long newUserId = 0;
            try (ResultSet generatedKeys = psUser.getGeneratedKeys()) {
                if (generatedKeys.next()) newUserId = generatedKeys.getLong(1);
                else { conn.rollback(); throw new SQLException("No se obtuvo ID"); }
            }

            // Crear Cuenta (Bono $1000)
            String accountNum = generateAccountNumber(); 
            String sqlAcc = "INSERT INTO accounts (user_id, account_number, balance) VALUES (?, ?, ?)";
            PreparedStatement psAcc = conn.prepareStatement(sqlAcc);
            psAcc.setLong(1, newUserId);
            psAcc.setString(2, accountNum);
            psAcc.setDouble(3, 1000.00); 
            psAcc.executeUpdate();

            conn.commit(); 
            System.out.println("Nuevo usuario registrado: " + req.name + " (" + generatedUsername + ")");
            sendResponse("{\"success\": true, \"message\": \"Usuario registrado\"}", exchange, 201);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("{\"error\": \"Error interno\"}", exchange, 500);
        }
    }

    // 2. LOGIN (CON HASH Y TOKEN)
    private void handleLoginRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        LoginRequest req = gson.fromJson(isr, LoginRequest.class);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT id, name, username, role FROM users WHERE curp = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, req.curp);
            pstmt.setString(2, hashPassword(req.password)); // Hasheamos para comparar
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String token = "mock-token-" + System.currentTimeMillis(); 
                String json = String.format("{\"success\": true, \"token\": \"%s\", \"name\": \"%s\", \"username\": \"%s\", \"role\": \"%s\"}", 
                              token, rs.getString("name"), rs.getString("username"), rs.getString("role"));                
                sendResponse(json, exchange, 200);
            } else {
                sendResponse("{\"success\": false, \"message\": \"Credenciales inválidas\"}", exchange, 401);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("{\"error\": \"Error BD\"}", exchange, 500);
        }
    }

    // 3. BALANCE (SALDO)
    private void handleBalanceRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String curp = exchange.getRequestHeaders().getFirst("X-User-Curp");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, curp);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                sendResponse("{\"balance\": " + rs.getDouble("balance") + "}", exchange, 200);
            } else {
                sendResponse("{\"balance\": 0.00}", exchange, 200);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("{\"error\": \"Error BD\"}", exchange, 500);
        }
    }

    // 4. OPERACIONES (DEPOSITO/RETIRO)
    private void handleOperationRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        OperationRequest req = gson.fromJson(isr, OperationRequest.class);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            String sqlGet = "SELECT a.id, a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement pStmtGet = conn.prepareStatement(sqlGet);
            pStmtGet.setString(1, req.curp);
            ResultSet rs = pStmtGet.executeQuery();

            if (!rs.next()) { sendResponse("{\"error\": \"Cuenta no encontrada\"}", exchange, 404); return; }
            
            long accId = rs.getLong("id");
            double currentBalance = rs.getDouble("balance");

            if (req.type.equals("WITHDRAW") && currentBalance < req.amount) {
                sendResponse("{\"error\": \"Fondos insuficientes\"}", exchange, 400); return;
            }

            double newBalance = req.type.equals("DEPOSIT") ? (currentBalance + req.amount) : (currentBalance - req.amount);

            PreparedStatement pUpd = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            pUpd.setDouble(1, newBalance);
            pUpd.setLong(2, accId);
            pUpd.executeUpdate();

            PreparedStatement pTx = conn.prepareStatement("INSERT INTO transactions (account_id, amount, type, description, status, date) VALUES (?, ?, ?, ?, 'COMPLETED', NOW())");
            pTx.setLong(1, accId);
            pTx.setDouble(2, req.amount);
            pTx.setString(3, req.type);
            pTx.setString(4, req.type.equals("DEPOSIT") ? "Depósito Ventanilla" : "Retiro Cajero");
            pTx.executeUpdate();

            conn.commit();
            sendResponse("{\"success\": true, \"newBalance\": " + newBalance + "}", exchange, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("{\"error\": \"Error procesando\"}", exchange, 500);
        }
    }

    // 5. TRANSFERENCIAS
    private void handleTransferRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { exchange.close(); return; }

        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        TransferRequest req = gson.fromJson(isr, TransferRequest.class);

        if (req.targetCurp.equals(req.sourceCurp)) {
            sendResponse("{\"error\": \"No puedes transferirte a ti mismo\"}", exchange, 400); return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            // Verificar Origen
            String sqlSource = "SELECT a.id, a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement psSource = conn.prepareStatement(sqlSource);
            psSource.setString(1, req.sourceCurp);
            ResultSet rsSource = psSource.executeQuery();

            if (!rsSource.next()) { conn.rollback(); sendResponse("{\"error\": \"Cuenta origen no existe\"}", exchange, 404); return; }
            long sourceAccId = rsSource.getLong("id");
            double sourceBalance = rsSource.getDouble("balance");

            if (sourceBalance < req.amount) { conn.rollback(); sendResponse("{\"error\": \"Fondos insuficientes\"}", exchange, 400); return; }

            // Verificar Destino
            String sqlTarget = "SELECT a.id, a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement psTarget = conn.prepareStatement(sqlTarget);
            psTarget.setString(1, req.targetCurp);
            ResultSet rsTarget = psTarget.executeQuery();

            if (!rsTarget.next()) { conn.rollback(); sendResponse("{\"error\": \"Destino no existe\"}", exchange, 404); return; }
            long targetAccId = rsTarget.getLong("id");
            double targetBalance = rsTarget.getDouble("balance");

            // Actualizar Saldos
            PreparedStatement updateSource = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            updateSource.setDouble(1, sourceBalance - req.amount);
            updateSource.setLong(2, sourceAccId);
            updateSource.executeUpdate();

            PreparedStatement updateTarget = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            updateTarget.setDouble(1, targetBalance + req.amount);
            updateTarget.setLong(2, targetAccId);
            updateTarget.executeUpdate();

            // Registrar Logs
            String motivo = (req.description != null && !req.description.isEmpty()) ? req.description : "Transferencia";
            String sqlHist = "INSERT INTO transactions (account_id, amount, type, description, status, date) VALUES (?, ?, ?, ?, 'COMPLETED', NOW())";
            
            // Salida
            PreparedStatement psHist1 = conn.prepareStatement(sqlHist);
            psHist1.setLong(1, sourceAccId);
            psHist1.setDouble(2, req.amount);
            psHist1.setString(3, "TRANSFER_SENT");
            psHist1.setString(4, motivo + " (Para: " + req.targetCurp + ")");
            psHist1.executeUpdate();

            // Entrada
            PreparedStatement psHist2 = conn.prepareStatement(sqlHist);
            psHist2.setLong(1, targetAccId);
            psHist2.setDouble(2, req.amount);
            psHist2.setString(3, "TRANSFER_RECEIVED");
            psHist2.setString(4, motivo + " (De: " + req.sourceCurp + ")");
            psHist2.executeUpdate();

            conn.commit();
            sendResponse("{\"success\": true, \"message\": \"Transferencia exitosa\"}", exchange, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("{\"error\": \"Error en transferencia\"}", exchange, 500);
        }
    }

    // 6. HISTORIAL (TRANSACTIONS)
    private void handleTransactionsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String curp = exchange.getRequestHeaders().getFirst("X-User-Curp");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT t.* FROM transactions t JOIN accounts a ON t.account_id = a.id JOIN users u ON a.user_id = u.id WHERE u.curp = ? ORDER BY t.date DESC LIMIT 10";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, curp);
            ResultSet rs = pstmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while(rs.next()) {
                if(!first) json.append(",");
                TransactionResponse tx = new TransactionResponse(rs.getString("id"), rs.getString("date"), rs.getString("type"), rs.getDouble("amount"), rs.getString("description"), rs.getString("status"));
                json.append(gson.toJson(tx));
                first = false;
            }
            json.append("]");
            sendResponse(json.toString(), exchange, 200);
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse("[]", exchange, 500);
        }
    }

    // --- MÉTODOS DE ADMIN (STATS, USERS, CHARTS, LOGS) ---
    
    private void handleAdminStatsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement s1 = conn.createStatement(); ResultSet r1 = s1.executeQuery("SELECT COUNT(*) as t FROM users WHERE role='USER'"); int tU = r1.next() ? r1.getInt("t") : 0;
            Statement s2 = conn.createStatement(); ResultSet r2 = s2.executeQuery("SELECT SUM(balance) as t FROM accounts"); double tM = r2.next() ? r2.getDouble("t") : 0;
            Statement s3 = conn.createStatement(); ResultSet r3 = s3.executeQuery("SELECT COUNT(*) as t FROM transactions WHERE DATE(date) = CURDATE()"); int tTx = r3.next() ? r3.getInt("t") : 0;
            sendResponse(String.format("{\"totalUsers\": %d, \"totalMoney\": %.2f, \"todayTx\": %d}", tU, tM, tTx), exchange, 200);
        } catch (Exception e) { e.printStackTrace(); sendResponse("{}", exchange, 500); }
    }

    private void handleAdminUsersRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.prepareStatement("SELECT u.id, u.name, u.curp, COALESCE(a.balance, 0) as balance FROM users u LEFT JOIN accounts a ON u.id = a.user_id WHERE u.role = 'USER'").executeQuery();
            StringBuilder json = new StringBuilder("["); boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(String.format("{\"id\":%d, \"name\":\"%s\", \"curp\":\"%s\", \"balance\":%.2f}", rs.getLong("id"), rs.getString("name"), rs.getString("curp"), rs.getDouble("balance")));
                first = false;
            }
            json.append("]"); sendResponse(json.toString(), exchange, 200);
        } catch (Exception e) { e.printStackTrace(); sendResponse("[]", exchange, 500); }
    }

    private void handleAdminChartsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ResultSet rsLine = conn.prepareStatement("SELECT HOUR(date) as h, COUNT(*) as c FROM transactions WHERE DATE(date) = CURDATE() GROUP BY HOUR(date) ORDER BY h ASC").executeQuery();
            List<Integer> ll = new ArrayList<>(), ld = new ArrayList<>(); while(rsLine.next()){ll.add(rsLine.getInt("h")); ld.add(rsLine.getInt("c"));}
            ResultSet rsBar = conn.prepareStatement("SELECT DATE(date) as d, SUM(amount) as t FROM transactions WHERE date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY DATE(date) ORDER BY d ASC").executeQuery();
            List<String> bl = new ArrayList<>(); List<Double> bd = new ArrayList<>(); while(rsBar.next()){bl.add(rsBar.getString("d")); bd.add(rsBar.getDouble("t"));}
            String json = "{ \"line\": { \"labels\": " + gson.toJson(ll) + ", \"data\": " + gson.toJson(ld) + "}, \"bar\": { \"labels\": " + gson.toJson(bl) + ", \"data\": " + gson.toJson(bd) + "} }";
            sendResponse(json, exchange, 200);
        } catch (Exception e) { e.printStackTrace(); sendResponse("{}", exchange, 500); }
    }

    private void handleAdminUserLogsRequest(HttpExchange exchange) throws IOException {
        if (handleCORS(exchange)) return;
        String userId = parseQuery(exchange.getRequestURI().getQuery()).get("userId");
        if (userId == null) { sendResponse("[]", exchange, 400); return; }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement ps = conn.prepareStatement("SELECT t.date, t.type, t.description, t.amount FROM transactions t JOIN accounts a ON t.account_id = a.id WHERE a.user_id = ? ORDER BY t.date DESC");
            ps.setLong(1, Long.parseLong(userId)); ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("["); boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(String.format("{\"date\":\"%s\", \"type\":\"%s\", \"description\":\"%s\", \"amount\":%.2f}", rs.getTimestamp("date"), rs.getString("type"), rs.getString("description"), rs.getDouble("amount")));
                first = false;
            }
            json.append("]"); sendResponse(json.toString(), exchange, 200);
        } catch (Exception e) { e.printStackTrace(); sendResponse("[]", exchange, 500); }
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

    private String generateUniqueUsername(String curp) {
        if (curp == null || curp.length() < 18) return "USER" + new Random().nextInt(9999);
        return curp.substring(0, 4).toUpperCase() + curp.substring(16, 18).toUpperCase() + (new Random().nextInt(900) + 100);
    }

    // --- DTOs ---
    static class LoginRequest { String curp, password; }
    static class RegisterRequest { String curp, password, name; }
    static class OperationRequest { String curp, type; double amount; }
    static class TransferRequest { String sourceCurp, targetCurp, description; double amount; }
    static class TransactionResponse { String id, date, type, description, status; double amount; TransactionResponse(String i, String d, String t, double a, String de, String s){id=i;date=d;type=t;amount=a;description=de;status=s;} }
}