import java.sql.*;

public class ShowSchema {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:hrms.db");
        // Show all tables
        ResultSet tables = conn.createStatement().executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name");
        System.out.println("=== TABLES ===");
        while (tables.next()) System.out.println(tables.getString(1));
        // Show employees columns
        System.out.println("\n=== employees columns ===");
        ResultSet cols = conn.createStatement().executeQuery("PRAGMA table_info(employees)");
        while (cols.next()) System.out.println(cols.getInt(1) + " | " + cols.getString(2) + " | " + cols.getString(3));
        conn.close();
    }
}
