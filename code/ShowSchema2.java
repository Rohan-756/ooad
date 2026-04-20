import java.sql.*;

public class ShowSchema2 {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:hrms.db");

        String[] tables = {"exit_interviews", "risk_assessments", "attrition_records"};
        for (String t : tables) {
            System.out.println("\n=== " + t + " ===");
            try {
                ResultSet cols = conn.createStatement().executeQuery("PRAGMA table_info(" + t + ")");
                while (cols.next())
                    System.out.println(cols.getInt(1) + " | " + cols.getString(2) + " | " + cols.getString(3));
            } catch (Exception e) { System.out.println("  (not found)"); }
        }

        // Sample employment_status values
        System.out.println("\n=== employment_status distinct values ===");
        ResultSet rs = conn.createStatement().executeQuery(
            "SELECT DISTINCT employment_status FROM employees LIMIT 20");
        while (rs.next()) System.out.println(rs.getString(1));

        // Sample emp_id values
        System.out.println("\n=== sample emp_id values ===");
        ResultSet rs2 = conn.createStatement().executeQuery(
            "SELECT rowid, emp_id, name, employment_status FROM employees LIMIT 5");
        while (rs2.next())
            System.out.println(rs2.getInt(1) + " | " + rs2.getString(2) + " | " + rs2.getString(3) + " | " + rs2.getString(4));

        conn.close();
    }
}
