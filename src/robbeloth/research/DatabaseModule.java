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
import java.util.List;

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
	private static final String databasePath = "data/obstruction";
	private static final String databaseTableName = "obstruction";
	private static final String destroyDB = "DROP TABLE " + databaseTableName;
	private static final String ID_COLUMN = "ID";
	private static final String FILENAME_COLUMN = "FILENAME";
	private static final String SEGMENT_COLUMN = "SEGMENTNUMBER";
	private static final String MOMENTX_COLUMN = "MOMENTX";
	private static final String MOMENTY_COLUMN = "MOMENTY";
	private static final String CHAINCODE_COLUMN = "CHAINCODE";
	private static final String STARTCCX_COLUMN = "STARTCC_X";
	private static final String STARTCCY_COLUMN = "STARTCC_Y";
	private static final String SEGMENT_TYPE_COLUMN = "SEGMENT_TYPE";
	private static final String SEGMENT_ROTATION_COLUMN = "SEGMENT_ROTATION";
	private static final String createTblStmt = "CREATE TABLE " 
	           + databaseTableName
			   + " ( " + ID_COLUMN + " INTEGER GENERATED ALWAYS AS IDENTITY,"
			   + " " + FILENAME_COLUMN + " VARCHAR(255) NOT NULL,"
			   + " " + SEGMENT_COLUMN + " INTEGER NOT NULL,"              
			   + " " + MOMENTX_COLUMN + " INTEGER, "
               + " " + MOMENTY_COLUMN + " INTEGER, "
               + " " + CHAINCODE_COLUMN + " CLOB, "
               + " " + STARTCCX_COLUMN + " INTEGER, "
               + " " + STARTCCY_COLUMN + " INTEGER, "
               + " " + SEGMENT_TYPE_COLUMN + " CHARACTER(1), "
               + " " + SEGMENT_ROTATION_COLUMN + " SMALLINT, "
               + " PRIMARY KEY ( ID ))";
	private static final String selectAllStmt = "SELECT * FROM " + databaseTableName;
	private static String insertStmt = 
			"INSERT INTO " + databaseTableName + " " +  
			"(" + FILENAME_COLUMN + ", " + SEGMENT_COLUMN + ", " + MOMENTX_COLUMN
			+ ", " + MOMENTY_COLUMN + ", " + CHAINCODE_COLUMN + ", " 
			+ STARTCCX_COLUMN + ", "  + STARTCCY_COLUMN + ", " + SEGMENT_TYPE_COLUMN 
			+ ", " + SEGMENT_ROTATION_COLUMN + ") "			
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static String deleteImage = 
			"DELETE FROM " + databaseTableName + " " +
			"WHERE FILENAME=?";
	private static final String getLastIdStmt = "SELECT TOP 1 ID FROM " + databaseTableName + " ORDER BY ID DESC";
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
	private static String selectccStart = "SELECT " + FILENAME_COLUMN + " FROM " + databaseTableName +
										  " WHERE " + STARTCCX_COLUMN + "=? AND " + STARTCCY_COLUMN + "=? AND " 
										  + SEGMENT_TYPE_COLUMN + "=? AND " + SEGMENT_ROTATION_COLUMN + "=?" ;
	private static String selectModelFilenames = "SELECT DISTINCT " + FILENAME_COLUMN + " FROM " + databaseTableName;
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
	
	private DatabaseModule() {
		if (doesDBExist()) 
			dumpDBMetadata();}
	
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
	
	/**
	 * Insert a model segment into the database
	 * @param filename -- file where segment originated
	 * @param segmentNumber -- segment number assigned to segment by segmentation and region growing process
	 * @param cc -- chain code representation of border region of segment
	 * @param moment -- centroid of segment
	 * @param startCC -- point where chain code of segment starts
	 * @param segmentType -- type of segment (S for standard, R for rotated standard, Y for synthesis, Z for rotated
	 * synthesis)
	 * @param segmentRotation -- rotation of segment (0 degrees for standard/synthesis standard)
	 * @return -- id assigned to segment in database or error code 
	 */
	public static synchronized int insertIntoModelDB(
			String filename, int segmentNumber, String cc, Point moment, 
			Point startCC, char segmentType, short segmentRotation) {
		/* example: insert into obstruction (FILENAME, SEGMENTNUMBER, CHAINCODE MOMENTX,
		 *           MOMENTY, RAW_SEG_DATA)
		 *  values (100, 'blah/blah.jpg', 200, 100, 100, '1,2,3' <clob>, 0, 0);
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
				if ((Double.isNaN(moment.x)) || (Double.isNaN(moment.y))) {
					ps.setDouble(3, 0.0);
					ps.setDouble(4, 0.0);	
					System.err.println("Centroid is NaN, setting to 0,0");
				}
				else {
					ps.setDouble(3, moment.x);
					ps.setDouble(4, moment.y);					
				}
				ps.setString(5, cc);	
				if ((Double.isNaN(startCC.x)) || (Double.isNaN(startCC.y))) {
					ps.setDouble(6, 0.0);
					ps.setDouble(7, 0.0);	
					System.err.println("Start of chain code is NaN, setting to 0,0");
				}
				else {
					ps.setDouble(6, startCC.x);
					ps.setDouble(7, startCC.y);					
				}
				ps.setString(8, String.valueOf(segmentType));
				ps.setShort(9, segmentRotation);
				
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
		
		/* fix how filename is in i/o verses database representation */
		filename = filename.replaceAll("/", ":");
		
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
				ps.setString(1, filename);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result) {
							return rs.getInt(1);
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
		
		/* fix how filename is in i/o verses database representation */
		filename = filename.replaceAll("/", ":");
		
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
				ps.setString(1, filename);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result) {
							return rs.getInt(1);
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
				ps.setString(1, filename);
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
				System.out.println("Executing create table statement " + createTblStmt);
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
					System.out.println("Retrieved result set");
				}
				else {
					System.err.println("No entries in table");
				}
				
				/* Show each record */
				if (dumpAllRecordsSet != null) {					
					/* Move the cursor to the first record */
					System.out.println("Moving to the first record");
					boolean recordsToProcess = dumpAllRecordsSet.next();
					
					/* Process the first and all remaining records */
					while (recordsToProcess) {
						int id = dumpAllRecordsSet.getInt(ID_COLUMN);
						String filename = dumpAllRecordsSet.getString(FILENAME_COLUMN);
						int segNumber = dumpAllRecordsSet.getInt(SEGMENT_COLUMN);
						int momentx = dumpAllRecordsSet.getInt(MOMENTX_COLUMN);
						int momenty = dumpAllRecordsSet.getInt(MOMENTY_COLUMN);
						Clob chaincode = dumpAllRecordsSet.getClob(CHAINCODE_COLUMN);
						int startccx = dumpAllRecordsSet.getInt(STARTCCX_COLUMN);
						int startccy = dumpAllRecordsSet.getInt(STARTCCY_COLUMN);
						long ccLen = chaincode.length();
						short segrotation = dumpAllRecordsSet.getShort(SEGMENT_ROTATION_COLUMN);
						String segType = dumpAllRecordsSet.getString(SEGMENT_TYPE_COLUMN);
						
						/* Only show a small part of the chain code */
						String ccCodeStart = 
								chaincode.getSubString(1, (int) ((ccLen > 20) ? 20 : ccLen));						
						System.out.println(id + "," + filename + "," + 
								           segNumber + ",(" + momentx + "," + momenty + ")"
								           + ",(" +ccCodeStart + ")" + "CC Length=" + ccLen 
								           + " start("+startccx+","+startccy+")" + " rotation=" + segrotation
								           + " type " + segType);
						
						/* advance the cursor */
						recordsToProcess = dumpAllRecordsSet.next();
					}
					return true;
				}
				else {
					System.err.println("No result set entries to process");
					return false;
				}				
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Backup database to directory
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
	 * Trim size of database
	 * @return result of operation
	 */
	public static synchronized boolean defrag() {
		boolean result = false;
		try {
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = connection.prepareStatement("checkpoint defrag");
				ps.execute();
				System.out.println("defrag(): shrinking database to min size");
			} 
			else if (connection == null) {
				System.err.println("defrag(): connection was not available");
			}
			else if ((connection != null) && (connection.isClosed())) {
				System.err.println("defrag(): connection was closed but "
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
					if (rs == null) {
						return null;
					}
					result = rs.next();
					if (result == false) {
						return null;
					}
					else {
						return rs.getString("CHAINCODE");	
					}					
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
					int momentx = rs.getInt(MOMENTX_COLUMN);
					int momenty = rs.getInt(MOMENTY_COLUMN);
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
	
	/**
	 * Retrieve all the model filenames in the database
	 * @return
	 */
	public static synchronized List<String> getAllModelFileName(){	
		List<String> modelNames = null;
		try {
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps =
						connection.prepareStatement(selectModelFilenames);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					modelNames = new ArrayList<String>(rs.getFetchSize());
					while(rs.next()) {
						modelNames.add(rs.getString(FILENAME_COLUMN));						
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return modelNames;
	}
	
	public static synchronized List<PointMatchContainer> getImagesMatchingCCStart(Point ccStart){
		List<PointMatchContainer> pmcList = new ArrayList<PointMatchContainer>();
		
		try {
			
			// There are no negative coordinates
			if ((ccStart == null) || (ccStart.x < 0) || (ccStart.y < 0)){
				return null;
			}
			
			// if the database is connected, execute the query
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(selectccStart);
				ps.setInt(1, (int)ccStart.x);
				ps.setInt(2, (int)ccStart.y);
				ps.setString(3, String.valueOf('S'));
				ps.setShort(4, (short)0);
				boolean result = ps.execute();
				
				// if there is a result, process it 
				if (result) {
					ResultSet rs = ps.getResultSet();
					while (rs.next()) {
						PointMatchContainer pmc = new PointMatchContainer(ccStart);
						pmc.setMatch(rs.getString(1));
						pmcList.add(new PointMatchContainer(pmc));
					}
					return pmcList;
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
