package robbeloth.research;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

 public final class DatabaseModule {
	private static Connection connection;
	private static Statement statement = null;
	private static String database = "data/obstruction.db";
	private static String destroyDB = "DROP TABLE MODEL IF EXISTS";
	private static String createTblStmt = "CREATE TABLE IF NOT EXISTS MODEL" 
			   + " ( id INTEGER IDENTITY,"
			   + " filename VARCHAR(255),"
			   + " segmentNumber INTEGER,"
               + " chaincode CLOB, "
               + " PRIMARY KEY ( id ))";
	private static String insertStmt = 
			"INSERT INTO MODEL(id, filename, segmentNumber, chaincode) VALUES (";
	private static volatile DatabaseModule singleton = null;
	private static int id = 0;
	static {
		
		// Load the SQL database driver
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Connect to the database
		try {
			connection = DriverManager.getConnection("jdbc:hsqldb:" + database, "sa", "");
			if (connection == null) {
				System.err.println("Database does not exist, create new one");
			}
			else {
				System.out.println("Database was successfully initialized " + connection.toString());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create the database
		System.out.println("Creating database...");					
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (statement != null) {
			try {
				statement.execute(destroyDB);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (statement != null) {
			try {
				statement.execute(createTblStmt);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private DatabaseModule() {}
	
	public static DatabaseModule getInstance() {
		if (singleton == null) {		
			System.out.println("First call to Database Module");
			singleton = new DatabaseModule();
		}
		else {
			System.err.println("Database Module already initialized");
		}
		return singleton;
	}
	
	public static boolean insertIntoModelDB(String filename, int segmentNumber, String cc) {
		String finalInsertStmt = insertStmt + id++ + "," + filename + "," 
	                             + segmentNumber + ",(" + cc + "))";
		System.out.println("Insert statement: " + finalInsertStmt);
		if ((connection != null) && (statement != null)){
			try {
				statement.execute(finalInsertStmt);
				return true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}			
		}
		return false;
	}
}
