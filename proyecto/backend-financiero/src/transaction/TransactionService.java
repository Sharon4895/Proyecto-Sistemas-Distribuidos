package transaction;

import java.sql.*;
import com.google.gson.Gson;
import pubsub.PubSubSimulator;
import audit.AuditService;

public class TransactionService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
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
