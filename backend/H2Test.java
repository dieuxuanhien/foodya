import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class H2Test {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Statement stmt = conn.createStatement();
        
        String[] queries = {
            "SELECT CURRENT_TIMESTAMP - INTERVAL '3 days'",
            "SELECT CURRENT_TIMESTAMP - INTERVAL '3' DAY",
            "SELECT CURRENT_TIMESTAMP - CAST('3 days' AS INTERVAL)"
        };
        
        for (String q : queries) {
            try {
                ResultSet rs = stmt.executeQuery(q);
                rs.next();
                System.out.println("SUCCESS: " + q + " -> " + rs.getString(1));
            } catch (Exception e) {
                System.out.println("FAILED:  " + q + " -> " + e.getMessage());
            }
        }
    }
}
