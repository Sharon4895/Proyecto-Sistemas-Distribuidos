package admin;

import java.sql.*;
import com.google.gson.Gson;

public class AdminService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/financiero_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private final Gson gson = new Gson();

    public String getStats() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            Statement s1 = conn.createStatement(); ResultSet r1 = s1.executeQuery("SELECT COUNT(*) as t FROM users WHERE role='USER'"); int tU = r1.next() ? r1.getInt("t") : 0;
            Statement s2 = conn.createStatement(); ResultSet r2 = s2.executeQuery("SELECT SUM(balance) as t FROM accounts"); double tM = r2.next() ? r2.getDouble("t") : 0;
            Statement s3 = conn.createStatement(); ResultSet r3 = s3.executeQuery("SELECT COUNT(*) as t FROM transactions WHERE DATE(date) = CURDATE()"); int tTx = r3.next() ? r3.getInt("t") : 0;
            return String.format("{\"totalUsers\": %d, \"totalMoney\": %.2f, \"todayTx\": %d}", tU, tM, tTx);
        } catch (Exception e) { e.printStackTrace(); return "{}"; }
    }

    public String getUsers() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ResultSet rs = conn.prepareStatement("SELECT u.id, u.name, u.curp, COALESCE(a.balance, 0) as balance FROM users u LEFT JOIN accounts a ON u.id = a.user_id WHERE u.role = 'USER'").executeQuery();
            StringBuilder json = new StringBuilder("["); boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(String.format("{\"id\":%d, \"name\":\"%s\", \"curp\":\"%s\", \"balance\":%.2f}", rs.getLong("id"), rs.getString("name"), rs.getString("curp"), rs.getDouble("balance")));
                first = false;
            }
            json.append("]"); return json.toString();
        } catch (Exception e) { e.printStackTrace(); return "[]"; }
    }

    public String getCharts() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            ResultSet rsLine = conn.prepareStatement("SELECT HOUR(date) as h, COUNT(*) as c FROM transactions WHERE DATE(date) = CURDATE() GROUP BY HOUR(date) ORDER BY h ASC").executeQuery();
            java.util.List<Integer> ll = new java.util.ArrayList<>(), ld = new java.util.ArrayList<>(); while(rsLine.next()){ll.add(rsLine.getInt("h")); ld.add(rsLine.getInt("c"));}
            ResultSet rsBar = conn.prepareStatement("SELECT DATE(date) as d, SUM(amount) as t FROM transactions WHERE date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) GROUP BY DATE(date) ORDER BY d ASC").executeQuery();
            java.util.List<String> bl = new java.util.ArrayList<>(); java.util.List<Double> bd = new java.util.ArrayList<>(); while(rsBar.next()){bl.add(rsBar.getString("d")); bd.add(rsBar.getDouble("t"));}
            String json = "{ \"line\": { \"labels\": " + gson.toJson(ll) + ", \"data\": " + gson.toJson(ld) + "}, \"bar\": { \"labels\": " + gson.toJson(bl) + ", \"data\": " + gson.toJson(bd) + "} }";
            return json;
        } catch (Exception e) { e.printStackTrace(); return "{}"; }
    }

    public String getUserLogs(String userId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement ps = conn.prepareStatement("SELECT t.date, t.type, t.description, t.amount FROM transactions t JOIN accounts a ON t.account_id = a.id WHERE a.user_id = ? ORDER BY t.date DESC");
            ps.setLong(1, Long.parseLong(userId)); ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("["); boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append(String.format("{\"date\":\"%s\", \"type\":\"%s\", \"description\":\"%s\", \"amount\":%.2f}", rs.getTimestamp("date"), rs.getString("type"), rs.getString("description"), rs.getDouble("amount")));
                first = false;
            }
            json.append("]"); return json.toString();
        } catch (Exception e) { e.printStackTrace(); return "[]"; }
    }
}
