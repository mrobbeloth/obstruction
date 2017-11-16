package robbeloth.research;

import java.io.File;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.opencv.core.Point;

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
	private static String fileNameCol = "FILENAME";
	private static String createTblStmt = "CREATE TABLE " 
	           + databaseTableName
			   + " ( ID INTEGER GENERATED ALWAYS AS IDENTITY,"
			   + " " + fileNameCol + " VARCHAR(255) NOT NULL,"
			   + " SEGMENTNUMBER INTEGER NOT NULL,"              
			   + " MOMENTX INTEGER, "
               + " MOMENTY INTEGER, "
               + " CHAINCODE CLOB, "
               + " PRIMARY KEY ( ID ))";
	private static String selectAllStmt = "SELECT * FROM " + databaseTableName;
	private static String insertStmt = 
			"INSERT INTO " + databaseTableName + " " +  
			"(FILENAME, SEGMENTNUMBER, MOMENTX, MOMENTY, " +
			"CHAINCODE) VALUES (?, ?, ?, ?, ?)";
	private static String deleteImage = 
			"DELETE FROM " + databaseTableName + " " +
			"WHERE FILENAME=?";
	private static String getLastIdStmt = "SELECT TOP 1 ID FROM " + databaseTableName + " ORDER BY ID DESC";
	private static String getLastIdStmtWithFilename = "SELECT TOP 1 ID FROM " + 
	                                                  databaseTableName + " WHERE FILENAME=?"
			                                          + " ORDER BY ID DESC";
	private static String getStartIdStmtWithFilename = "SELECT TOP 1 ID FROM " + 
													   databaseTableName + " WHERE FILENAME=?"
													   + " ORDER BY ID ASC";
	private static String getSegmentCnt = "SELECT COUNT(FILENAME) AS SEGMENTCOUNT FROM " + 
			     						   databaseTableName + " WHERE FILENAME=?";
	private static String doesDBExistStmt = "SELECT COUNT(TABLE_NAME) FROM " + 
	                                          "INFORMATION_SCHEMA.SYSTEM_TABLES WHERE " +
			                                  "TABLE_NAME='OBSTRUCTION'";
	private static String selectChainCode = "SELECT CHAINCODE FROM " + databaseTableName + 
			                                " WHERE ID=?";
	private static String selectMoment = "SELECT MOMENTX,MOMENTY FROM " + databaseTableName + 
            								" WHERE ID=?";
	private static String selectFn = "SELECT FILENAME FROM " + databaseTableName + 
			                         " WHERE ID=?";
	private static String selectFilesWMoment = "SELECT FILENAME FROM " + databaseTableName + 
									 " WHERE MOMENTX=? AND MOMENTY=?";  
	private static volatile DatabaseModule singleton = null;
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
			connection = DriverManager.getConnection("jdbc:hsqldb:file:" + databasePath 
					                                 + ";shutdown=true", "sa", "");
			connection.setAutoCommit(true);			
			if (connection == null) {
				System.err.println("Connection not established, terminating program");
				System.exit(-2);
			}
			else {
				System.out.println("Connection established");
				System.out.println("Connection info: " + 
				                    connection.getMetaData().getURL());
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
	
	private DatabaseModule() {dumpDBMetadata();}
	
	public static synchronized DatabaseModule getInstance() {
		if (singleton == null) {		
			System.out.println("First call to Database Module");
			singleton = new DatabaseModule();			
		}
		else {
			System.err.println("Database Module already initialized");
		}
		return singleton;
	}
	
	public static synchronized Connection getConnection() {
		return connection;
	}
	
	public static synchronized int insertIntoModelDB(
			String filename, int segmentNumber, String cc, Point p) {
		/* example: insert into obstruction (FILENAME, SEGMENTNUMBER, CHAINCODE MOMENTX,
		 *           MOMENTY, RAW_SEG_DATA)
		 *  values (100, 'blah/blah.jpg', 200, 100, 100, '1,2,3' <clob>);
		 *  
		 *  Note that moment coordinates are real values, but matching 
		 *  may need to be more flexible due to rotation, shearing, different
		 *  lighting conditions, or a host of other extenuating circumstances
		 *  that may shift segment center's of mass slightly, enough that
		 *  a match out to a number of decimal places is not possible*/

		PreparedStatement ps;
		if ((connection != null) && (statement != null)){
			try {
				/* Supply insertion statement with placeholders 
				 * for actual data */
				ps = connection.prepareStatement(insertStmt);
				
				/* fill in placeholders in insertion statement*/
				ps.setString(1, filename.replace('/', ':'));
				ps.setInt(2, segmentNumber);
				if ((Double.isNaN(p.x)) || (Double.isNaN(p.y))) {
					ps.setDouble(3, 0.0);
					ps.setDouble(4, 0.0);	
					System.err.println("Centroid is NaN, setting to 0,0");
				}
				else {
					ps.setDouble(3, p.x);
					ps.setDouble(4, p.y);					
				}
				ps.setString(5, cc);	
				
				/* Insert data into database */
				ps.execute();
				
				/* Return the id from the last insert operation */
				return getLastId();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -100;
			}			
		}
		System.err.println("Failed to add segment " + segmentNumber 
				           + " into database");
		return -200;
	}
	
	/**
	 * Remove the tuples associated with an image in the filename field
	 * This is just the filename, not a path and filename 
	 * @param filename -- filename prefixed with "data:"
	 * @return
	 */
	public static synchronized int deleteImageFromDB(String filename) {

		PreparedStatement ps;
		if ((connection != null) && (statement != null)){
			try {
				/* Supply insertion statement with placeholders 
				 * for actual data */
				ps = connection.prepareStatement(deleteImage);
				
				/* fill in placeholders in insertion statement*/
				ps.setString(1, filename);				
				/* Insert data into database */
				ps.execute();
				
				/* Return the id from the last insert operation */
				return ps.getUpdateCount();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -300;
			}			
		}
		return -400;
	}
	
	/**
	 * Get the last unique identifier used so far in the primary obstruction
	 * table 
	 * @return the identifier 
	 */
	public static synchronized int getLastId() {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			System.err.println("Unable to find database");
			return 404;
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
	 * Get the last generated id for an image
	 * @param filename -- relative name of file
	 * @return the identifier 
	 */
	public static synchronized int getLastId(String filename) {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			System.err.println("Unable to find database");
			return 404;
		}
		
		/* Selects just one record after getting all the ids and 
		 * ordering the values in the id column in descending order */
		String stmt = getLastIdStmtWithFilename;
		System.out.println("Retrieve statement: " + stmt);
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(stmt);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
		if (ps != null) {
			try {
				ps.setString(0, filename);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result) {
							return rs.getInt(fileNameCol);
						}
					}
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();	
			}
		}
		return 0;
	}	
	
	/**
	 * Get the first generated id for an image
	 * @param filename -- relative name of file
	 * @return the identifier 
	 */
	public static synchronized int getStartId(String filename) {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			System.err.println("Unable to find database");
			return 404;
		}
		
		/* Selects just one record after getting all the ids and 
		 * ordering the values in the id column in descending order */
		String stmt = getStartIdStmtWithFilename;
		System.out.println("Retrieve statement: " + stmt);
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(stmt);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
		if (ps != null) {
			try {
				ps.setString(0, filename);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result) {
							return rs.getInt(fileNameCol);
						}
					}
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();	
			}
		}
		return 0;
	}	
	
	/**
	 * Count the number of segments for a file
	 * @param filename -- relative name of file
	 * @return the identifier 
	 */
	public static synchronized int cntSegmentsForFile(String filename) {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();
		if (!gotDB) {
			System.err.println("Unable to find database");
			return 404;
		}
		
		/* Selects just one record after getting all the ids and 
		 * ordering the values in the id column in descending order */
		String stmt = getSegmentCnt;
		System.out.println("Retrieve statement: " + stmt);
		PreparedStatement ps = null;
		try {
			ps = connection.prepareStatement(stmt);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
				
		if (ps != null) {
			try {
				ps.setString(0, filename);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result) {
							return rs.getInt("SEGMENTCOUNT");
						}
					}
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();	
			}
		}
		return 0;
	}	
	
	/**
	 * Drop the primary table holding the chain codes
	 * @return true if the table was scrubbed; false otherwise
	 */
	public static synchronized boolean dropDatabase() {
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
	public static synchronized boolean createModel() {
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
				dumpDBMetadata();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
				return false;
			}
		}
		return true;
	}
	
	public static synchronized boolean dumpDBMetadata() {
		if (connection == null) {
			System.err.println("Connection is null, returning");
			return false;
		}
		
		DatabaseMetaData dbmd;
		try {
			dbmd = connection.getMetaData();
			System.out.println("Product Name: " + dbmd.getDatabaseProductName());
			System.out.println("Driver Name: " + dbmd.getDriverName());
			System.out.println("URL: " + dbmd.getURL());
			System.out.println("Version: " + dbmd.getDriverVersion());
		    ResultSet tables = dbmd.getTables(null, null, null, TABLE_TYPES);
		    while (tables.next()) {
		      System.out.println("TABLE NAME: "   + tables.getString(TABLE_NAME));
		      System.out.println("TABLE SCHEMA: " + tables.getString(TABLE_SCHEMA));
		    }
		    
		    ResultSet columns = dbmd.getColumns(null, "PUBLIC", 
		    									databaseTableName.toUpperCase(), 
		    									null);
		    ResultSetMetaData rsmd = columns.getMetaData();
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s)");
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    Statement st = connection.createStatement();
		    ResultSet rsAll = st.executeQuery(selectAllStmt);
		    rsmd = rsAll.getMetaData();
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s)");
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		
		return true;
	}
	
	/**
	 * Display all the records in the primary obstruction table of the
	 * database (the chain code table) <br/> <br/> 
	 * Does not destroy the table
	 * @return true if the dump was successful; false otherwise
	 */
	public static synchronized boolean dumpModel() {
		/* Sanity check database existence*/
		boolean gotDB = doesDBExist();		
		if (gotDB) {
			System.err.println(databaseTableName + " table already exists");
		}
		else {
			System.out.println(databaseTableName + " table does not exist");
			return false;
		}
		
		/* Display metadata on database before showing records to see
		 * if database overall was correctly structured */
		dumpDBMetadata();
		
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
						int momentx = dumpAllRecordsSet.getInt(4);
						int momenty = dumpAllRecordsSet.getInt(5);
						Clob chaincode = dumpAllRecordsSet.getClob(6);
						long ccLen = chaincode.length();
						
						/* Only show a small part of the chain code */
						String ccCodeStart = 
								chaincode.getSubString(1, (int) ((ccLen > 20) ? 20 : ccLen));						
						System.out.println(id + "," + filename + "," + 
								           segNumber + ",(" + momentx + "," + momenty + ")"
								           + ",(" +ccCodeStart + ")" + "CC Length=" + ccLen);
						
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
	 * Backup database to direction
	 * @param location -- directory holding database backup
	 * @return a backed up database in location with naming convention
	 * databaseTableName_currentTimeMillis.tgz
	 */
	public static synchronized boolean backupDatabase (File location) {
		/* Build statement to execute, for some reason you can't set
		 * the filename as a SQL parameter */
		String backupDatabase = "BACKUP DATABASE TO " + "'" 
							    + location.getAbsolutePath()
				                + File.separatorChar + databaseTableName + "_" 
				                + System.currentTimeMillis() 
				                + ".tgz' BLOCKING";
		
		boolean gotDB = doesDBExist();		
		if (gotDB) {
			System.err.println(databaseTableName + " table already exists");
		}
		else {
			System.out.println(databaseTableName + " table does not exist");
			return false;
		}	
		
		try {
			if ((connection != null) &&
					(!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(backupDatabase);
				boolean result = ps.execute();
				return result;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	/**
	 * Determine if the primary obstruction table exists
	 * @return true if the database exists; false otherwise
	 */
	public static synchronized boolean doesDBExist() {
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
	public static synchronized boolean shutdown() {
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
	public static synchronized String getChainCode(int id) {
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
	
	/**
	 * Get the moment associated with a unique identifier
	 * @param id -- database id which is unique for each entry
	 * @return moment for given id
	 */
	public static synchronized Point getMoment(int id) {
		try {
			if ((connection != null) && 
					(!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(selectMoment);	
				ps.setInt(1, id);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					result = rs.next();
					if (!result) {
						return null;
					}
					int momentx = rs.getInt(1);
					int momenty = rs.getInt(2);
					return new Point(momentx, momenty);
				}
				else {
					return null;
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
			return null;			
		}
		return null;
	}
	
	/**
	 * Return the model images containing a given moment
	 * in any of the segments
	 * @param momentx -- x coordinate of the moment
	 * @param momenty -- y coordinate of the moment
	 * @return models containing the moment
	 */
	public static synchronized ArrayList<String> getFilesWithMoment(
			int momentx, int momenty) {
		ArrayList<String> filenames = new ArrayList<String>();
		
		try {
			
			// There are no negative coordinates
			if ((momentx < 0) || (momenty < 0)) {
				return null;
			}
			
			// if the database is connected, execute the query
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(selectFilesWMoment);
				ps.setInt(1, momentx);
				ps.setInt(2, momenty);
				boolean result = ps.execute();
				
				// if there is a result, process it 
				if (result) {
					ResultSet rs = ps.getResultSet();
					while (rs.next()) {
						filenames.add(rs.getString(1));
					}
					return filenames;
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
	
	/**
	 * Retrieve the filename field value associated with a given id
	 * @param id -- unique id for a model image and segment
	 * @return filename of model image
	 */
	public static synchronized String getFileName(int id) {
		try {
			
			// There are no negative ids or segments
			if (id < 0) {
				return "N/A";
			}
			
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(selectFn);
				ps.setInt(1, id);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result)
						   return rs.getString("FILENAME");
						else {
							System.err.println("Error retrieving "
									+ "FILENAME field value for id"
									+ ":" + id);
						}
					}
					else {
						System.err.println("There was no cursor to process in "
								+ "retrieveing data for id "+ id);
					}
				}
				else {
					System.err.println("There was no fileanme for id " + id);
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
