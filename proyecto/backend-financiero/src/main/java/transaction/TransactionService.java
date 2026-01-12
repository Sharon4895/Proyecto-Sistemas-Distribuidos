package transaction;

import java.sql.*;
import com.google.gson.Gson;
import pubsub.PubSubSimulator;
import audit.AuditService;
import java.io.FileInputStream; // Nuevo import
import java.io.IOException;     // Nuevo import
import java.util.Properties;    // Nuevo import

public class TransactionService {
    
    // Variables estáticas modificables
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASS;
    
    // Bloque estático para cargar configuración
    static {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
            System.out.println(">>> Configuración cargada desde config.properties");
        } catch (IOException e) {
            System.err.println(">>> No se encontró config.properties, usando valores por defecto/entorno.");
        }

        // Prioridad: 1. Variable de Entorno -> 2. Archivo config -> 3. Localhost (Default)
        DB_URL = System.getenv().getOrDefault("DB_URL", props.getProperty("db.url", "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true"));
        DB_USER = System.getenv().getOrDefault("DB_USER", props.getProperty("db.user", "root"));
        DB_PASS = System.getenv().getOrDefault("DB_PASS", props.getProperty("db.pass", "root"));
        
        System.out.println(">>> TransactionService conectando a DB en: " + DB_URL);
    }

    public static void main(String[] args) {
        int port = 8083;
        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(port), 0);
            TransactionService service = new TransactionService();
            server.createContext("/transactions", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    String query = exchange.getRequestURI().getQuery();
                    String curp = null;
                    if (query != null && query.startsWith("curp=")) {
                        curp = query.substring(5);
                    }
                    String response = service.getUserTransactions(curp);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // CORS básico por si acaso
                    exchange.sendResponseHeaders(200, response.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
                    exchange.getResponseBody().write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
                exchange.close();
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            System.out.println("TransactionService escuchando en el puerto " + port);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Gson gson = new Gson();

    // Simulador Pub/Sub y servicio de auditoría
    private static final PubSubSimulator pubsub = new PubSubSimulator();
    private static final AuditService auditService = new AuditService(pubsub);

    public TransactionService() {
        // Suscribirse a eventos de transacción (simulación)
        pubsub.subscribe("transactions", this::processTransactionEvent);
    }

    public String getUserTransactions(String curp) {
        // Usamos las variables estáticas cargadas arriba
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT t.* FROM transactions t JOIN accounts a ON t.account_id = a.id JOIN users u ON a.user_id = u.id WHERE u.curp = ? ORDER BY t.date DESC LIMIT 10";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, curp);
            ResultSet rs = pstmt.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while(rs.next()) {
                if(!first) json.append(",");
                json.append(String.format("{\"id\":\"%s\",\"date\":\"%s\",\"type\":\"%s\",\"amount\":%.2f,\"description\":\"%s\",\"status\":\"%s\"}",
                    rs.getString("id"), rs.getString("date"), rs.getString("type"), rs.getDouble("amount"), rs.getString("description"), rs.getString("status")));
                first = false;
            }
            json.append("]");
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error conectando a DB: " + e.getMessage());
            return "[]";
        }
    }

    public void publishTransactionEvent(String eventJson) {
        pubsub.publish("transactions", eventJson);
    }

    private void processTransactionEvent(String eventJson) {
        System.out.println("[TRANSACTION] Evento recibido: " + eventJson);
        auditService.logTransaction(eventJson);
    }
}