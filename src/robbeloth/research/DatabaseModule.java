package robbeloth.research;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 
 * @author mrobbeloth
 * 
 * Note: use java -cp hsqldb.jar org.hsqldb.util.DatabaseManagerSwing 
 * for debugging
 */
 public final class DatabaseModule {
	private static Connection connection;
	private static Statement statement = null;
	private static String databasePath = "data/obstruction";
	private static String databaseName = "obstruction";
	private static String destroyDB = "DROP TABLE " + databaseName;
	private static String createTblStmt = "CREATE TABLE " 
	           + databaseName
			   + " ( ID INTEGER IDENTITY,"
			   + " FILENAME VARCHAR(255),"
			   + " SEGMENTNUMBER INTEGER,"
               + " CHAINCODE CLOB, "
               + " PRIMARY KEY ( ID ))";
	private static String selectAllStmt = "SELECT * FROM " + databaseName;
	private static String insertStmt = 
			"INSERT INTO " + databaseName + " " +  
			"(ID, FILENAME, SEGMENTNUMBER, CHAINCODE) VALUES (";
	private static String getLastIdStmt = "SELECT TOP 1 Id FROM " + databaseName + " ORDER BY ID DESC";
	private static volatile DatabaseModule singleton = null;
	private static int id = 0;
	private static final String TABLE_NAME = "TABLE_NAME";
	private static final String TABLE_SCHEMA = "TABLE_SCHEMA";
	private static final String[] TABLE_TYPES = {"TABLE"};
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
			connection = DriverManager.getConnection("jdbc:hsqldb:file:" + databasePath, "sa", "");
			connection.setAutoCommit(true);
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
		try {
			statement = connection.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		/* example: insert into obstruction (ID, FILENAME, SEGMENTNUMBER, CHAINCODE)
		 *  values (100, 'blah/blah.jpg', 200, '1,2,3');*/
		String finalInsertStmt = insertStmt + id++ + ",'" + filename.replace('/',':') + "'," 
	                             + segmentNumber + ",'" + cc + "')";
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
	
	public static boolean getLastId(String filename, int segmentNumber, String cc) {
		String stmt = getLastIdStmt;
		System.out.println("Retrieve statement: " + stmt);
		if (connection != null){
			try {
				statement.execute(stmt);
				return true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}			
		}
		return false;
	}
	
	
	public static boolean dropDatabase() {
		System.out.println("Dropping old database...");	
		if (connection != null) {
			try {
				boolean result = statement.execute(destroyDB);
				if (result) {
					System.out.println("There is a result set from dropping the database table");
				}
				else {
					System.err.println("No update count or result set from dropping the table");
				}				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;		
	}
	
	public static boolean createModel() {
		/* Need to remove the old database first */
		dropDatabase();
		
		/* New let's build the new database and its schema */
		System.out.println("Creating database...");	
		if (connection != null) {
			try {
				boolean result = statement.execute(createTblStmt);
				if (result) {
					System.out.println("There is a result set from creating the database table");
				}
				else {
					System.err.println("No update count or result set from creating the table");
				}
				DatabaseMetaData dbmd = connection.getMetaData();
			    ResultSet tables = dbmd.getTables(null, null, null, TABLE_TYPES);
			    while (tables.next()) {
			      System.out.println("TABLE NAME: "   + tables.getString(TABLE_NAME));
			      System.out.println("TABLE SCHEMA: " + tables.getString(TABLE_SCHEMA));
			    }
			    ResultSet columns = dbmd.getColumns(null, null, "MODEL", null);
			    int colCnt = 1;
			    while (columns.next()) {
			    	System.out.print(columns.getString(colCnt++) + ",");
			    	System.out.print("\n");
			    }			    
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
				return false;
			}
		}
		return true;
	}
	
	public static boolean dumpModel() {
		if (connection != null) {
			try {
				boolean result = statement.execute(selectAllStmt);
				ResultSet dumpAllRecordsSet = null;
				if (result) {
					dumpAllRecordsSet = statement.getResultSet();
				}
				if (dumpAllRecordsSet != null) {
					boolean recordsToProcess = dumpAllRecordsSet.next();
					while (recordsToProcess) {
						int id = dumpAllRecordsSet.getInt(1);
						String filename = dumpAllRecordsSet.getString(2);
						int segNumber = dumpAllRecordsSet.getInt(3);
						Clob chaincode = dumpAllRecordsSet.getClob(4);
						long ccLen = chaincode.length();
						String ccCodeStart = 
								chaincode.getSubString(1, (int) ((ccLen > 20) ? 20 : ccLen));
						System.out.println("id"+","+filename+","+segNumber+",("+ccCodeStart+")");
						
						/* advance the cursor */
						recordsToProcess = dumpAllRecordsSet.next();
					}
				}
				return true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
		
	public static boolean shutdown() {
		try {
			if (connection != null) {
				connection.prepareStatement("shutdown").execute();	
			}			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} 
	}
}
