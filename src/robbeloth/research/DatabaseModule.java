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
	private static String databaseTableName = "obstruction";
	private static String destroyDB = "DROP TABLE " + databaseTableName;
	private static String createTblStmt = "CREATE TABLE " 
	           + databaseTableName
			   + " ( ID INTEGER IDENTITY,"
			   + " FILENAME VARCHAR(255),"
			   + " SEGMENTNUMBER INTEGER,"
               + " CHAINCODE CLOB, "
               + " PRIMARY KEY ( ID ))";
	private static String selectAllStmt = "SELECT * FROM " + databaseTableName;
	private static String insertStmt = 
			"INSERT INTO " + databaseTableName + " " +  
			"(ID, FILENAME, SEGMENTNUMBER, CHAINCODE) VALUES (";
	private static String getLastIdStmt = "SELECT TOP 1 ID FROM " + databaseTableName + " ORDER BY ID DESC";
	private static String doesDBExistStmt = "SELECT COUNT(TABLE_NAME) FROM " + 
	                                          "INFORMATION_SCHEMA.SYSTEM_TABLES WHERE " +
			                                  "TABLE_NAME='OBSTRUCTION'";
	private static volatile DatabaseModule singleton = null;
	private static int id = 0;
	private static final String TABLE_NAME = "TABLE_NAME";
	/* It really is TABLE_SCHEM for TABLE_SCHEMA*/
	private static final String TABLE_SCHEMA = "TABLE_SCHEM";
	private static final String[] TABLE_TYPES = {"TABLE"};
	static {	    
		// Load the SQL database driver
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (ClassNotFoundException e) {
			System.err.println("Cannot find the HypserSQL library, exiting");
			e.printStackTrace();
			System.exit(-1);
		}
		
		// Connect to the database
		try {
			connection = DriverManager.getConnection("jdbc:hsqldb:file:" + databasePath, "sa", "");
			connection.setAutoCommit(true);
			if (connection == null) {
				System.err.println("Connection not established, terminating program");
				System.exit(-2);
			}
			else {
				System.out.println("Connection established");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create the object for passing SQL statements			
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
	
	public static Connection getConnection() {
		return connection;
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
	
	public static int getLastId() {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			createModel();
		}
		
		String stmt = getLastIdStmt;
		System.out.println("Retrieve statement: " + stmt);
		if (connection != null){
			try {
				ResultSet rs = statement.executeQuery(stmt);
				if (rs.next()){
					return rs.getInt(1);
				}
				return 0;
			} catch (SQLException e) {
				e.printStackTrace();
				return 0;
			}			
		}
		return 0;
	}	
	
	public static boolean dropDatabase() {
		System.out.println("Dropping old database table " + databaseTableName + "...");	
		
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			System.err.println(databaseTableName + " does not exist, no point "
					+ "in trying to remove it");
			return false;
		}
		
		/* Database exists, so drop table */
		if (connection != null) {
			try {
				statement.execute(destroyDB);
				if (!doesDBExist()) {
					System.out.println(databaseTableName + " deleted");
				}
				else {
					System.err.println(databaseTableName + "still exists");
				}				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;		
	}
	
	public static boolean createModel() {
		/* New let's build the new database and its schema */
		System.out.println("Creating database...");	
				
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();		
		if (gotDB) {
			System.err.println(databaseTableName + " table already exists");
			return false;
		}
		else {
			System.out.println(databaseTableName + " table does not exist yet");
		}
		
		if (connection != null) {
			try {
				statement.execute(createTblStmt);
				if (doesDBExist()) {
					System.out.println(databaseTableName + " created");
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
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();		
		if (gotDB) {
			System.err.println(databaseTableName + " table already exists");
		}
		else {
			System.out.println(databaseTableName + " table does not exist");
			return false;
		}
		
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
	public static boolean doesDBExist() {
		try {
			ResultSet existSet = statement.executeQuery(doesDBExistStmt);
			int tblCnt = -1;
			if (existSet.next()) {
				tblCnt = existSet.getInt(1);	
			}
			else {
			   return false;	
			}
			
			if (tblCnt > 0) {
				System.out.println(databaseTableName + " exists " + existSet.getInt(1));
				return true;
			}
			else {
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static boolean shutdown() {
		try {
			if (connection != null) {
				connection.prepareStatement("shutdown").execute();	
				connection.close();
			}			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} 
	}
}
