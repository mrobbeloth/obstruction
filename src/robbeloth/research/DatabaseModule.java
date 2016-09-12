package robbeloth.research;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
			   + " ( ID INTEGER GENERATED ALWAYS AS IDENTITY,"
			   + " FILENAME VARCHAR(255),"
			   + " SEGMENTNUMBER INTEGER,"
               + " CHAINCODE CLOB, "
               + " PRIMARY KEY ( ID ))";
	private static String selectAllStmt = "SELECT * FROM " + databaseTableName;
	private static String insertStmt = 
			"INSERT INTO " + databaseTableName + " " +  
			"(FILENAME, SEGMENTNUMBER, CHAINCODE) VALUES (";
	private static String getLastIdStmt = "SELECT TOP 1 ID FROM " + databaseTableName + " ORDER BY ID DESC";
	private static String doesDBExistStmt = "SELECT COUNT(TABLE_NAME) FROM " + 
	                                          "INFORMATION_SCHEMA.SYSTEM_TABLES WHERE " +
			                                  "TABLE_NAME='OBSTRUCTION'";
	private static String selectChainCode = "SELECT CHAINCODE FROM " + databaseTableName + 
			                                " WHERE ID=?";
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
		/* example: insert into obstruction (FILENAME, SEGMENTNUMBER, CHAINCODE)
		 *  values (100, 'blah/blah.jpg', 200, '1,2,3');*/
		String finalInsertStmt = insertStmt + "'" + filename.replace('/',':') + "'," 
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
	
	/**
	 * Get the last unique identifier used so far in the primary obstruction
	 * table 
	 * @return the identifier 
	 */
	public static int getLastId() {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			System.err.println("Unable to get last id in database");
			return -1;
		}
		
		/* Selects just one record after getting all the ids and 
		 * ordering the values in the id column in descending order */
		String stmt = getLastIdStmt;
		System.out.println("Retrieve statement: " + stmt);
		if (connection != null){
			try {
				ResultSet rs = statement.executeQuery(stmt);
				if (rs.next()){
					// return the first row's single column value
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
	
	/**
	 * Drop the primary table holding the chain codes
	 * @return true if the table was scrubbed; false otherwise
	 */
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
	
	/**
	 * Build the primary obstruction table holding the chain codes 
	 * for the different segments in each model image
	 * @return true if the table was created properly; false otherwise
	 */
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
	
	/**
	 * Display all the records in the primary obstruction table of the
	 * database (the chain code table) <br/> <br/> 
	 * Does not destroy the table
	 * @return true if the dump was successful; false otherwise
	 */
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
		
		/* The database exists, let's see if there is anything in it */
		if (connection != null) {
			try {
				boolean result = statement.execute(selectAllStmt);
				
				/* Check to see if there are records to process
				 * and get the set of records if there are  */
				ResultSet dumpAllRecordsSet = null;				
				if (result) {
					dumpAllRecordsSet = statement.getResultSet();
				}
				
				/* Show each record */
				if (dumpAllRecordsSet != null) {					
					/* Move the cursor to the first record */
					boolean recordsToProcess = dumpAllRecordsSet.next();
					
					/* Process the first and all remaining records */
					while (recordsToProcess) {
						int id = dumpAllRecordsSet.getInt(1);
						String filename = dumpAllRecordsSet.getString(2);
						int segNumber = dumpAllRecordsSet.getInt(3);
						Clob chaincode = dumpAllRecordsSet.getClob(4);
						long ccLen = chaincode.length();
						
						/* Only show a small part of the chain code */
						String ccCodeStart = 
								chaincode.getSubString(1, (int) ((ccLen > 20) ? 20 : ccLen));						
						System.out.println(id + "," + filename + "," + 
								           segNumber + ",(" +ccCodeStart + ")" + 
								           ", CC Length=" + ccLen);
						
						/* advance the cursor */
						recordsToProcess = dumpAllRecordsSet.next();
					}
				}
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Determine if the primary obstruction table exists
	 * @return true if the database exists; false otherwise
	 */
	public static boolean doesDBExist() {
		try {
			
			/* Pull all the system tables and look for the one that says
			 * obstruction -- in the future may make the primary table
			 * the chain code table and create additional tables with 
			 * other attributes and foreign keys? */
			ResultSet existSet = statement.executeQuery(doesDBExistStmt);
			int tblCnt = -1;
			if (existSet.next()) {
				tblCnt = existSet.getInt(1);	
			}
			else {
			   return false;	
			}
			
			/* the obstruction table was found */
			if (tblCnt > 0) {
				System.out.println(databaseTableName + " exists ");
				return true;
			}
			else {
				System.out.println(databaseTableName + " does not exist");
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Terminate access to the database in a controlled manner 
	 * @return true if the shutdown was w/o error; false otherwise
	 */
	public static boolean shutdown() {
		boolean result = false;
		try {
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = connection.prepareStatement("shutdown");
				ps.execute();
				connection.close();
				System.out.println("shutdown(): shutdown command issued");
			} 
			else if (connection == null) {
				System.err.println("shutdown(): connection was not available");
			}
			else if ((connection != null) && (connection.isClosed())) {
				System.err.println("shutdown(): connection was closed but "
						+ "resource was not released");
			}
			return result;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} 
	}
	/**
	 * Return the chain code of the image given the uniquely 
	 * assigned id to each image and segment
	 * Note: if you want to use segments numbers, a separate
	 * method will have to be used and the filename supplied
	 * as a parameter.
	 * @param id -- database id which is unique for each entry
	 * @return chain code of the row containing the id
	 */
	public static String getChainCode(int id) {
		try {
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(selectChainCode);
				ps.setInt(1, id);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					rs.next();
					return rs.getString("CHAINCODE");
				}
				else {
					return null;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return null; 
	}
}
