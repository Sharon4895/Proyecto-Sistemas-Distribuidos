
package transaction;

import java.sql.*;
import com.google.gson.Gson;
import pubsub.PubSubSimulator;
import audit.AuditService;

/**
 * Servicio de transacciones listo para despliegue distribuido.
 * DB_URL, DB_USER y DB_PASS se pueden parametrizar por variables de entorno.
 * Ejemplo de ejecución:
 *   DB_URL=jdbc:mysql://host/db DB_USER=usuario DB_PASS=clave java -cp ... transaction.TransactionService
 */
public class TransactionService {
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
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "root");
    private final Gson gson = new Gson();

    // Simulador Pub/Sub y servicio de auditoría
    private static final PubSubSimulator pubsub = new PubSubSimulator();
    private static final AuditService auditService = new AuditService(pubsub);

    public TransactionService() {
        // Suscribirse a eventos de transacción (simulación)
        pubsub.subscribe("transactions", this::processTransactionEvent);
    }

    public String getUserTransactions(String curp) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT t.* FROM transactions t JOIN accounts a ON t.account_id = a.id JOIN users u ON a.user_id = u.id WHERE u.curp = ? ORDER BY t.date DESC LIMIT 10";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, curp);
            ResultSet rs = pstmt.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while(rs.next()) {
                if(!first) json.append(",");
                // TransactionResponse: id, date, type, amount, description, status
                json.append(String.format("{\"id\":\"%s\",\"date\":\"%s\",\"type\":\"%s\",\"amount\":%.2f,\"description\":\"%s\",\"status\":\"%s\"}",
                    rs.getString("id"), rs.getString("date"), rs.getString("type"), rs.getDouble("amount"), rs.getString("description"), rs.getString("status")));
                first = false;
            }
            json.append("]");
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    // Publicar evento de transacción (simulación)
    public void publishTransactionEvent(String eventJson) {
        pubsub.publish("transactions", eventJson);
    }

    // Procesar evento de transacción (simulación)
    private void processTransactionEvent(String eventJson) {
        // Aquí se podría procesar la transacción y luego auditarla
        System.out.println("[TRANSACTION] Evento recibido: " + eventJson);
        auditService.logTransaction(eventJson);
        // TODO: Actualizar balances, guardar en BD, etc.
    }
}
