import java.sql.*;

class Datbase {
	public static Connection connection;
	
	public static void connect() {
		try {
			Class.forName("com.mysql.cj.jbdc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://localhost/doctors_office?serverTimezone=EST", "root", "database28");
				} catch (Exception e) {
					System.out.println(e);
				}
	}
}
public class Example1 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		

	}

}
