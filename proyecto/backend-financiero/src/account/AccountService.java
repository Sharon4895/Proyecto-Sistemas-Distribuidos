package account;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import java.sql.*;
import java.nio.charset.StandardCharsets;

public class AccountService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private final Gson gson = new Gson();

    public String getBalance(String curp) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, curp);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return "{\"balance\": " + rs.getDouble("balance") + "}";
            } else {
                return "{\"balance\": 0.00}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Error BD\"}";
        }
    }

    public String operate(String curp, String type, double amount) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            String sqlGet = "SELECT a.id, a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement pStmtGet = conn.prepareStatement(sqlGet);
            pStmtGet.setString(1, curp);
            ResultSet rs = pStmtGet.executeQuery();
            if (!rs.next()) { return "{\"error\": \"Cuenta no encontrada\"}"; }
            long accId = rs.getLong("id");
            double currentBalance = rs.getDouble("balance");
            if (type.equals("WITHDRAW") && currentBalance < amount) {
                return "{\"error\": \"Fondos insuficientes\"}";
            }
            double newBalance = type.equals("DEPOSIT") ? (currentBalance + amount) : (currentBalance - amount);
            PreparedStatement pUpd = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            pUpd.setDouble(1, newBalance);
            pUpd.setLong(2, accId);
            pUpd.executeUpdate();
            PreparedStatement pTx = conn.prepareStatement("INSERT INTO transactions (account_id, amount, type, description, status, date) VALUES (?, ?, ?, ?, 'COMPLETED', NOW())");
            pTx.setLong(1, accId);
            pTx.setDouble(2, amount);
            pTx.setString(3, type);
            pTx.setString(4, type.equals("DEPOSIT") ? "Depósito Ventanilla" : "Retiro Cajero");
            pTx.executeUpdate();
            conn.commit();
            return "{\"success\": true, \"newBalance\": " + newBalance + "}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Error procesando\"}";
        }
    }

    public String transfer(String sourceCurp, String targetCurp, double amount, String description) {
        if (targetCurp.equals(sourceCurp)) {
            return "{\"error\": \"No puedes transferirte a ti mismo\"}";
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            String sqlSource = "SELECT a.id, a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement psSource = conn.prepareStatement(sqlSource);
            psSource.setString(1, sourceCurp);
            ResultSet rsSource = psSource.executeQuery();
            if (!rsSource.next()) { conn.rollback(); return "{\"error\": \"Cuenta origen no existe\"}"; }
            long sourceAccId = rsSource.getLong("id");
            double sourceBalance = rsSource.getDouble("balance");
            if (sourceBalance < amount) { conn.rollback(); return "{\"error\": \"Fondos insuficientes\"}"; }
            String sqlTarget = "SELECT a.id, a.balance FROM accounts a JOIN users u ON a.user_id = u.id WHERE u.curp = ?";
            PreparedStatement psTarget = conn.prepareStatement(sqlTarget);
            psTarget.setString(1, targetCurp);
            ResultSet rsTarget = psTarget.executeQuery();
            if (!rsTarget.next()) { conn.rollback(); return "{\"error\": \"Destino no existe\"}"; }
            long targetAccId = rsTarget.getLong("id");
            double targetBalance = rsTarget.getDouble("balance");
            PreparedStatement updateSource = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            updateSource.setDouble(1, sourceBalance - amount);
            updateSource.setLong(2, sourceAccId);
            updateSource.executeUpdate();
            PreparedStatement updateTarget = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
            updateTarget.setDouble(1, targetBalance + amount);
            updateTarget.setLong(2, targetAccId);
            updateTarget.executeUpdate();
            String motivo = (description != null && !description.isEmpty()) ? description : "Transferencia";
            String sqlHist = "INSERT INTO transactions (account_id, amount, type, description, status, date) VALUES (?, ?, ?, ?, 'COMPLETED', NOW())";
            PreparedStatement psHist1 = conn.prepareStatement(sqlHist);
            psHist1.setLong(1, sourceAccId);
            psHist1.setDouble(2, amount);
            psHist1.setString(3, "TRANSFER_SENT");
            psHist1.setString(4, motivo + " (Para: " + targetCurp + ")");
            psHist1.executeUpdate();
            PreparedStatement psHist2 = conn.prepareStatement(sqlHist);
            psHist2.setLong(1, targetAccId);
            psHist2.setDouble(2, amount);
            psHist2.setString(3, "TRANSFER_RECEIVED");
            psHist2.setString(4, motivo + " (De: " + sourceCurp + ")");
            psHist2.executeUpdate();
            conn.commit();
            return "{\"success\": true, \"message\": \"Transferencia exitosa\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Error en transferencia\"}";
        }
    }
}
