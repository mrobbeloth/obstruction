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
import java.util.Iterator;
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
	public static final int NUMBER_RELATIONS = 4;
	private static final String databasePath = "data/obstruction";
	private static final String databaseName = 
			databasePath.substring(databasePath.lastIndexOf('/')+1); 
	private static final String dbLocalTable = "obstruction_local";
	private static final String dbGlobalTable = "obstruction_global";
	private static final String dbGlobalMetaTable = "obstruction_meta_table";
	private static final String dbGlobalDelGrpTbl = "obstruction_del_grph_table";
	private static final String destroyLocalTable = "DROP TABLE " + dbLocalTable;
	private static final String destroyGlobalTable = "DROP TABLE " + dbGlobalTable;
	private static final String destroyGlobalMetaTable = "DROP TABLE " + dbGlobalMetaTable;	
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
	private static final String DISTANCE_COLUMN = "DISTANCE";
	private static final String THETA1_COLUMN = "THETA_1_ANGLE";
	private static final String THETA2_COLUMN = "THETA_2_ANGLE";
	private static final String SIZE_COLUMN = "SIZE_PIXELS";	
	private static final String SIMG_SCORE_DELAUNAY = "SIMG_SCORE_DELAUNAY";
	private static final String TRIAD_X1 = "TX1";
	private static final String TRIAD_Y1 = "TY1";
	private static final String TRIAD_X2 = "TX2";
	private static final String TRIAD_Y2 = "TY2";
	private static final String TRIAD_X3 = "TX3";
	private static final String TRIAD_Y3 = "TY3";
	private static final String createLocalTblStmt = "CREATE TABLE " 
	           + dbLocalTable
			   + " ( " + ID_COLUMN + " INTEGER GENERATED ALWAYS AS IDENTITY,"
			   + " " + FILENAME_COLUMN + " VARCHAR(255) NOT NULL,"
			   + " " + SEGMENT_COLUMN + " INTEGER NOT NULL,"              
               + " " + CHAINCODE_COLUMN + " CLOB," 
               + " " + STARTCCX_COLUMN + " INTEGER, "
               + " " + STARTCCY_COLUMN + " INTEGER, "
               + " " + SEGMENT_TYPE_COLUMN + " CHARACTER(1), "
               + " " + SEGMENT_ROTATION_COLUMN + " SMALLINT, "
               + " PRIMARY KEY ( " + ID_COLUMN + " ))";
	private static final String createGlbTblStmt = "CREATE TABLE "
			   + dbGlobalTable
			   + " ( " + ID_COLUMN      + "  INTEGER GENERATED ALWAYS AS IDENTITY,"
			   + " " + MOMENTX_COLUMN   + " INTEGER, "
               + " " + MOMENTY_COLUMN   + " INTEGER, "
               + " " + DISTANCE_COLUMN  + " DOUBLE, "
               + " " + THETA1_COLUMN    + " DOUBLE, "
               + " " + THETA2_COLUMN    + " DOUBLE, "
               + " " + SIZE_COLUMN + " INTEGER, " 
               + " " + "FOREIGN KEY (" + ID_COLUMN +") REFERENCES " + dbLocalTable + ")";
	private static final String createGlbMetaTblStmt = "CREATE TABLE "
			   + dbGlobalMetaTable
			   + " ( " + FILENAME_COLUMN + " VARCHAR(255) NOT NULL,"
			   + " " + SIMG_SCORE_DELAUNAY + " DOUBLE, "
			   + " PRIMARY KEY(" + FILENAME_COLUMN + "))";
	/* This structure assumes a flattened set of nodes where each node links to the next*/
	private static final String createGlbDelaunayTable = "CREATE TABLE "
			   + dbGlobalDelGrpTbl
			   + " ( " + ID_COLUMN + " INTEGER GENERATED ALWAYS AS IDENTITY,"
			   + " " + FILENAME_COLUMN + " VARCHAR(255) NOT NULL,"
			   + " " + TRIAD_X1 + " INTEGER, "
			   + " " + TRIAD_Y1 + " INTEGER, "
			   + " " + TRIAD_X2 + " INTEGER, "
			   + " " + TRIAD_Y2 + " INTEGER, "
			   + " " + TRIAD_X3 + " INTEGER, "
			   + " " + TRIAD_Y3 + " INTEGER, "
			   + " PRIMARY KEY( " + ID_COLUMN + " ))";
	private static final String selectAllLocalStmt = "SELECT * FROM " + dbLocalTable;
	private static final String selectAllGlbStmt = "SELECT * FROM " + dbGlobalTable;
	private static final String selectAllGlbMetaStmt = "SELECT * FROM " + dbGlobalMetaTable;
	private static final String selectAllDelaGlbStmt = "SELECT * FROM " + dbGlobalDelGrpTbl;
	private static String insLocalTuple = 
			"INSERT INTO " + dbLocalTable + " " +  
			"(" + FILENAME_COLUMN         + ", " 
				+ SEGMENT_COLUMN          + ", " 
			    + CHAINCODE_COLUMN        + ", " 
			    + STARTCCX_COLUMN         + ", "  
			    + STARTCCY_COLUMN         + ", " 
			    + SEGMENT_TYPE_COLUMN     + ", " 
			    + SEGMENT_ROTATION_COLUMN + ") "			
			+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
	private static String insGblTuple = 
			"INSERT INTO " + dbGlobalTable + " " +  
			"(" + MOMENTX_COLUMN           + ", " 
			    + MOMENTY_COLUMN           + ", " 
			    + DISTANCE_COLUMN          + ", "  
			    + THETA1_COLUMN            + ", " 
			    + THETA2_COLUMN            + ", "
			    + SIZE_COLUMN              + ") "
			+ "VALUES (?, ?, ?, ?, ?, ?)";
	private static String insGlbMetaTuple = 
			"INSERT INTO " + dbGlobalMetaTable + " " +
			"(" + FILENAME_COLUMN     + ", "
			    + SIMG_SCORE_DELAUNAY + ") "
			 + "VALUES (?, ?)";
	private static String insDelaGlbTuple = 
			"INSERT INTO " + dbGlobalDelGrpTbl + " " +
			"(" + FILENAME_COLUMN     + ", "
			    + TRIAD_X1 + ", " 
			    + TRIAD_Y1 + ", " 
			    + TRIAD_X2 + ", " 
			    + TRIAD_Y2 + ", " 
			    + TRIAD_X3 + ", " 
			    + TRIAD_Y3 + ") " 
			 + "VALUES (?, ?, ?, ?, ?, ?, ?)";
	private static String deleteImageLocalTable = 
			"DELETE FROM " + dbLocalTable + " " +
			"WHERE " + FILENAME_COLUMN + "=?";
	private static String deleteImageGlobalTable = 
			"DELETE FROM " + dbGlobalTable + " " +
			"WHERE " + ID_COLUMN + " BETWEEN ? AND ?";
	private static String deleteImgGlblDelTbl = 
			"DELETE FROM " + dbGlobalDelGrpTbl + " " + 
			"WHERE " + FILENAME_COLUMN + "=?";
	private static final String getLastIdStmt = "SELECT TOP 1 ID FROM " + dbLocalTable + " ORDER BY ID DESC";
	private static String getLastIdStmtWithFilename = "SELECT TOP 1 ID FROM " + 
	                                                  dbLocalTable + " WHERE FILENAME=?"
			                                          + " ORDER BY ID DESC";
	private static String getStartIdStmtWithFilename = "SELECT TOP 1 ID FROM " + 
													   dbLocalTable + " WHERE FILENAME=?"
													   + " ORDER BY ID ASC";
	private static String getSegmentCnt = "SELECT COUNT(FILENAME) AS SEGMENTCOUNT FROM " + 
			     						   dbLocalTable + " WHERE FILENAME=?";
	private static String doesDBExistStmt = "SELECT COUNT(TABLE_NAME) FROM " + 
	                                          "INFORMATION_SCHEMA.SYSTEM_TABLES WHERE " +
			                                  "TABLE_NAME LIKE 'OBSTRUCTION%'";
	private static String selectChainCode = "SELECT " + CHAINCODE_COLUMN +  " FROM " + dbLocalTable + 
			                                " WHERE " + ID_COLUMN + "=?";
	private static String selectMoment = "SELECT " + MOMENTX_COLUMN + "," + MOMENTY_COLUMN + " FROM " + dbGlobalTable + 
											" WHERE " + ID_COLUMN + "=?";
	private static String selectFn = "SELECT " + FILENAME_COLUMN + " FROM " + dbLocalTable + 
									 " WHERE " + ID_COLUMN + "=?";
	private static String selectFilesWMoment = "SELECT " + FILENAME_COLUMN +  " FROM " + dbLocalTable + 
									 " WHERE ID IN(SELECT " + ID_COLUMN + " FROM " + dbGlobalTable + 
									 " WHERE " +  MOMENTX_COLUMN + "=? AND " + MOMENTY_COLUMN + "=?)";  
	private static String selectccStart = "SELECT " + FILENAME_COLUMN + " FROM " + dbLocalTable +
										  " WHERE " + STARTCCX_COLUMN + "=? AND " + STARTCCY_COLUMN + "=? AND " 
										  + SEGMENT_TYPE_COLUMN + "=? AND " + SEGMENT_ROTATION_COLUMN + "=?" ;
	private static String selectModelFilenames = "SELECT DISTINCT " + FILENAME_COLUMN + " FROM " + dbLocalTable;
	private static String selectUpperThresholds = "SELECT " + THETA2_COLUMN + " FROM " + dbGlobalTable + 
												  " WHERE " + ID_COLUMN + " BETWEEN " + "? AND ?";
	private static String selectLowerThresholds = "SELECT " + THETA1_COLUMN + " FROM " + dbGlobalTable + 
											      " WHERE " + ID_COLUMN + " BETWEEN " + "? AND ?";
	private static String selectsimGDelaunayValue = "SELECT " + SIMG_SCORE_DELAUNAY + " FROM " + dbGlobalMetaTable
			                                        + " WHERE " + FILENAME_COLUMN + "=?";
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
					                                 + ";shutdown=true;hsqldb.cache_rows=250000"
					                                 + ";hsqldb.cache_size=250000", "sa", "");
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
			e.printStackTrace();
		}
		
		// Create the object for passing SQL statements			
		try {
			statement = connection.createStatement();			
		} catch (SQLException e) {
			e.printStackTrace();
		}		
				
	}
	
	private DatabaseModule() {
		if (doesDBExist() == 2) 
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
	public static synchronized int insertIntoModelDBLocalRelation(
			String filename, int segmentNumber, String cc,
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
				ps = connection.prepareStatement(insLocalTuple);
				
				/* fill in placeholders in insertion statement*/
				ps.setString(1, filename.replace('/', ':'));
				ps.setInt(2, segmentNumber);
				ps.setString(3, cc);	
				if ((Double.isNaN(startCC.x)) || (Double.isNaN(startCC.y))) {
					ps.setDouble(4, 0.0);
					ps.setDouble(5, 0.0);	
					System.err.println("Start of chain code is NaN, setting to 0,0");
				}
				else {
					ps.setDouble(4, startCC.x);
					ps.setDouble(5, startCC.y);					
				}
				ps.setString(6, String.valueOf(segmentType));
				ps.setShort(7, segmentRotation);
				
				/* Insert data into database */
				ps.execute();
				
				/* Return the id from the last insert operation */
				return getLastId();
			} catch (SQLException e) {
				e.printStackTrace();
				return -100;
			}			
		}
		System.err.println("Failed to add segment " + segmentNumber 
				           + " into database");
		return -200;
	}
	
	public static synchronized int insertIntoModelDBGlobalRelation(
			Point moment, 
			double distance,
			double theta1,
			double theta2,
			LGNode node) {
		PreparedStatement ps;
		if ((connection != null) && (statement != null)){
			try {
				/* Supply insertion statement with placeholders 
				 * for actual data */
				ps = connection.prepareStatement(insGblTuple);
				
				/* fill in placeholders in insertion statement*/
				if ((Double.isNaN(moment.x)) || (Double.isNaN(moment.y))) {
					ps.setDouble(1, 0.0);
					ps.setDouble(2, 0.0);	
					System.err.println("Centroid is NaN, setting to 0,0");
				}
				else {
					ps.setDouble(1, moment.x);
					ps.setDouble(2, moment.y);					
				}
				
				if (Double.isNaN(distance)) {
					ps.setDouble(3, 0.0);
					System.err.println("Distance is NaN, setting to 0.0");
				}
				else {
					ps.setDouble(3, distance);
				}
				
				if (Double.isNaN(theta1)) {
					ps.setDouble(4, 0.0);	
					System.err.println("theta1 angle is NaN, setting to 0.0 degree");
				}
				else {
					ps.setDouble(4, theta1);					
				}
				if (Double.isNaN(theta2)) {
					ps.setDouble(5, 0.0);	
					System.err.println("theta2 angle is NaN, setting to 0.0 degrees");
				}
				else {
					ps.setDouble(5, theta2);					
				}			
				
				if ((node != null) && (!Double.isNaN(node.getSize()))) {
					ps.setDouble(6, node.getSize());
				}
				else {
					System.err.println("Node does not have a valid size, setting to zero");
					ps.setDouble(6, 0.0);
				}
				
				/* Insert data into database */
				ps.execute();
				
				/* Return normal result */
				return 0;
			} catch (SQLException e) {
				e.printStackTrace();
				return -100;
			}			
		}
		System.err.println("insertIntoModelDBGlobalRelation(): "
				+ "Failed to add tuple into database");
		return -200;		
		
	}
	
	/**
	 * Insert into the Global Database the simG score for a model using the Delaunay calcuation of angle
	 * thresholds. This is different from the simG that comes from angle differences of start node to destination
	 * node network
	 * @param filename -- model image
	 * @param simGScore -- similarity score given threshold measurements from Delaunay construction
	 * @return
	 */
	public static synchronized int insertIntoModelDBGlobaMetaRelation(
			String filename, double simGScore) {
		PreparedStatement ps;
		if ((connection != null) && (statement != null)){
			try {
				/* Supply insertion statement with placeholders 
				 * for actual data */
				ps = connection.prepareStatement(insGlbMetaTuple);
				
				if ((filename != null) && (!filename.isEmpty())) {
					ps.setString(1, filename);	
				}
				else {
					return -300;
				}
				
			    ps.setDouble(2, simGScore);
				
				/* Insert data into database */
				ps.execute();
				
				/* Return normal result*/
				return 0;
			} catch (SQLException e) {
				e.printStackTrace();
				return -100;
			}			
		}
		System.err.println("insertIntoModelDBGlobalRelation(): "
				+ "Failed to add tuple into database");
		return -200;		
		
	}
	
	public static synchronized int insertIntoModelDBGblDelGraph(String filename, List<Point> triads) {
		
		// Sanity checks
		if ((filename == null) || (filename.isEmpty()) || (triads == null) || (triads.isEmpty())) {
			return -1;
		}
		
		PreparedStatement ps;
		if ((connection != null) && (statement != null)){
			try {
				for (int i = 0; i < triads.size(); i+=3) {					
					/* Supply insertion statement with placeholders 
					 * for actual data */
					ps = connection.prepareStatement(insDelaGlbTuple);
					
					/* prepare parameters */
					ps.setString(1, filename);
					ps.setDouble(2, triads.get(i).x);
					ps.setDouble(3, triads.get(i).y);
					ps.setDouble(4, triads.get(i+1).x);
					ps.setDouble(5, triads.get(i+1).y);
					ps.setDouble(6, triads.get(i+2).x);
					ps.setDouble(7, triads.get(i+2).y);
					
					
					/* Insert data into database */
					ps.execute();
				}
				
			}
			catch (SQLException e) {
				e.printStackTrace();
				return -100;				
			}
		}
		
		/* Return normal result*/
		return 0;		
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
				 * for actual data 
				 * 
				 * 1. Get starting id for image
				 * 2. Get ending id for image
				 * 3. Remove tuples inclusive of starting and ending ids 
				 *    from global table 
				 * 4. Remove image tuples from local table */
				
				// remove entries from local table
				int startingID = getStartId(filename);
				int endingID = getLastId(filename);
				ps = connection.prepareStatement(deleteImageGlobalTable);
				ps.setInt(1, startingID);
				ps.setInt(2, endingID);
				
				boolean result = ps.execute();
				System.out.println("deleteImageFromDB() Number of entries removed-global: " 
	                       + ps.getUpdateCount());
				
				// remove entries from global table
				ps = connection.prepareStatement(deleteImageLocalTable);
				
				/* fill in placeholders in insertion statement*/
				ps.setString(1, filename);				
				/* Insert data into database */
				ps.execute();
				
				/* Return the id from the last insert operation */
				int entrsRm = ps.getUpdateCount();
				
				/* Delete associated Delaunay graph*/
				ps = connection.prepareStatement(deleteImgGlblDelTbl);
				ps.setString(1, filename);
				ps.execute();
				
				System.out.println("deleteImageFromDB(): Number of entries removed-local: " + entrsRm);
				return entrsRm;
			} catch (SQLException e) {
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
	public static int getLastId() {
		/* Sanity check database existence*/
		int gotDB = doesDBExist();
		if (gotDB == 0) {
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
	public static int getLastId(String filename) {
		/* Sanity check database existence*/
		int gotDB = doesDBExist();
		if (gotDB == 0) {
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
	public static int getStartId(String filename) {
		/* Sanity check database existence*/
		int gotDB = doesDBExist();
		if (gotDB == 0) {
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
		int gotDB = doesDBExist();
		if (gotDB == 0) {
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
		System.out.println("Dropping old database table " + dbLocalTable + "...");	
		
		/* Sanity check database existence*/
		int gotDB = doesDBExist();
		if (gotDB == 0) {
			System.err.println(databaseName  
					+ " database does not exist, no point in trying "
					+ "to remove it");
			return false;
		}
		
		/* Database exists, so start w/ global meta table and fk to local table*/
		if (connection != null) {
			try {
				statement.execute(destroyGlobalMetaTable);			
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/* Database exists, so start w/ global table and fk to local table*/
		if (connection != null) {
			try {
				statement.execute(destroyGlobalTable);			
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		/* work on local table */
		if (connection != null) {
			try {		
				statement.execute(destroyLocalTable);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if (doesDBExist() == 0) {
			System.out.println("Database removed");
			return true;
		}
		else {
			System.out.println("Still holds table(s)");
			return false;
		}
				
	}
	
	/**
	 * Build the obstruction database relations
	 * @return true if the table was created properly; false otherwise
	 */
	public static synchronized boolean createModel() {
		/* New let's build the new database and its schema */
		System.out.println("Creating database...");	
				
		/* Sanity check database existence*/
		int gotDB = doesDBExist();		
		if (gotDB == NUMBER_RELATIONS) {
			System.err.println(databaseName + " database already exists");
			return false;
		}
		else {
			System.out.println(databaseName + " database does not exist yet "
					+ "or is not fully created");
		}
		
		
		/* Create local table first */
		if (connection != null) {
			try {
				System.out.println("Executing create table statement " + createLocalTblStmt);
				statement.execute(createLocalTblStmt);
			} catch (SQLException e) {
				e.printStackTrace(); 
			}
		}
		
		/* Create global table next */
		if (connection != null) {
			try {
				System.out.println("Executing create table statement " + createGlbTblStmt);
				statement.execute(createGlbTblStmt);	
			} catch (SQLException e) {
				System.err.println("Unable to run create table statement " + createGlbTblStmt);
				e.printStackTrace(); 
			}
		}
		
		/* Create global meta table next */
		if (connection != null) {
			try {
				System.out.println("Executing create table statement " + createGlbMetaTblStmt);
				statement.execute(createGlbMetaTblStmt);	
			} catch (SQLException e) {
				System.err.println("Unable to run create table statement " + createGlbMetaTblStmt);
				e.printStackTrace(); 
			}
		}
		
		/* Finally, create Global Delaunay Graph Table  */
		if (connection != null) {
			try {
				System.out.println("Executing create table statement " + createGlbDelaunayTable);
				statement.execute(createGlbDelaunayTable);	
			} catch (SQLException e) {
				System.err.println("Unable to run create table statement " + createGlbDelaunayTable);
				e.printStackTrace(); 
			}
		}
		
		/* Verify creation */
		if (doesDBExist() == NUMBER_RELATIONS) {
			System.out.println(databaseName + " database created");
			dumpDBMetadata();
		}
		else {
			System.err.println(databaseName + " database not properly created ");
			return false;
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
		    
		    /* Get local table information */
		    ResultSet columns = dbmd.getColumns(null, "PUBLIC", 
		    									dbLocalTable, null);
		    ResultSetMetaData rsmd = columns.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s) from " 
		    				   + dbLocalTable + " meta information");
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    Statement st = connection.createStatement();
		    ResultSet rsAll = st.executeQuery(selectAllLocalStmt);
		    rsmd = rsAll.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s)" 
		    		           + " from data columns of " + dbLocalTable);
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    
		    /* Get global table information */
		    columns = dbmd.getColumns(null, "PUBLIC", 
		    						  dbGlobalTable, null);
		    rsmd = columns.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s) from " 
 				   + dbGlobalTable + " meta information");
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    st = connection.createStatement();
		    rsAll = st.executeQuery(selectAllGlbStmt);
		    rsmd = rsAll.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s)" 
		    		           + " from data columns of " + dbLocalTable);
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    
		    /* Get global meta table information */
		    columns = dbmd.getColumns(null, "PUBLIC", 
		    						  dbGlobalMetaTable, null);
		    rsmd = columns.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s) from " 
 				   + dbGlobalMetaTable + " meta information");
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    st = connection.createStatement();
		    rsAll = st.executeQuery(selectAllGlbMetaStmt);
		    rsmd = rsAll.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s)" 
		    		           + " from data columns of " + dbGlobalMetaTable);
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }
		    
		    /* Get Delaunay table information*/
		    
		    /* Get global meta table information */
		    columns = dbmd.getColumns(null, "PUBLIC", 
		    						  dbGlobalDelGrpTbl, null);
		    rsmd = columns.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s) from " 
 				   + dbGlobalDelGrpTbl + " meta information");	
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }	
		    st = connection.createStatement();
		    rsAll = st.executeQuery(selectAllDelaGlbStmt);
		    rsmd = rsAll.getMetaData();
		    System.out.println("");
		    System.out.println("Found " + rsmd.getColumnCount() + " column(s)" 
		    		           + " from data columns of " + dbGlobalDelGrpTbl);
		    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
		    	System.out.println("Name=" + rsmd.getColumnName(i));
		    	System.out.println("Type=" + rsmd.getColumnType(i));
		    }		    
		    
		} catch (SQLException e) {
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
		int gotDB = doesDBExist();		
		if (gotDB == NUMBER_RELATIONS) {
			System.out.println(databaseName + " database already exists");
			
		}
		else {
			System.err.println(databaseName  
					+ " database does not exist yet or is not fully created");						
			return false;
		}
		
		/* Display metadata on database before showing records to see
		 * if database overall was correctly structured */
		dumpDBMetadata();
		
		/* The database exists, let's see if there is anything in it */
		if (connection != null) {
			try {
				boolean result = statement.execute(selectAllLocalStmt);
				
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
				
				/* Show each record from) local table*/
				if (dumpAllRecordsSet != null) {					
					/* Move the cursor to the first record */
					System.out.println("Moving to the first record");
					boolean recordsToProcess = dumpAllRecordsSet.next();
					
					/* Process the first and all remaining records */
					while (recordsToProcess) {
						int id = dumpAllRecordsSet.getInt(ID_COLUMN);
						String filename = dumpAllRecordsSet.getString(FILENAME_COLUMN);
						int segNumber = dumpAllRecordsSet.getInt(SEGMENT_COLUMN);
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
								           segNumber + ",(" +ccCodeStart + ")" 
								           + "CC Length=" + ccLen 
								           + " start("+startccx+","+startccy+")" 
								           + " rotation=" + segrotation
								           + " type " + segType);
						
						/* advance the cursor */
						recordsToProcess = dumpAllRecordsSet.next();					
					}
				}
				else {
					System.err.println("dumpModel(): No local graph result set entries to process");
					return false;
				}				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				boolean result = statement.execute(selectAllGlbStmt);
				
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
				
				/* Show each record from global table*/
				if (dumpAllRecordsSet != null) {					
					/* Move the cursor to the first record */
					System.out.println("Moving to the first record");
					boolean recordsToProcess = dumpAllRecordsSet.next();
					
					/* Process the first and all remaining records */
					while (recordsToProcess) {
						int id = dumpAllRecordsSet.getInt(ID_COLUMN);
						int momentx = dumpAllRecordsSet.getInt(MOMENTX_COLUMN);
						int momenty = dumpAllRecordsSet.getInt(MOMENTY_COLUMN);
						double distance = dumpAllRecordsSet.getDouble(DISTANCE_COLUMN);
						double theta1_angle = dumpAllRecordsSet.getDouble(THETA1_COLUMN);
						double theta2_angle = dumpAllRecordsSet.getDouble(THETA2_COLUMN);
						int size_seg_pixels = dumpAllRecordsSet.getInt(SIZE_COLUMN);
												
						System.out.println(id + ", moment(" + momentx + "," + momenty + 
								   "), distance="+ distance + ", theta1_angle=" 
								   + theta1_angle + ", theta2_angle=" + theta2_angle
								   + ", segment_area=" + size_seg_pixels + " pixels");
						
						/* advance the cursor */
						recordsToProcess = dumpAllRecordsSet.next();					
					}
				}
				else {
					System.err.println("No result set entries to process");
					return false;
				}				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				boolean result = statement.execute(selectAllGlbMetaStmt);
				
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
				
				/* Show each record from global table*/
				if (dumpAllRecordsSet != null) {					
					/* Move the cursor to the first record */
					System.out.println("Moving to the first record");
					boolean recordsToProcess = dumpAllRecordsSet.next();
					
					/* Process the first and all remaining records */
					while (recordsToProcess) {
						String filename = dumpAllRecordsSet.getString(FILENAME_COLUMN);
						double simg_score = dumpAllRecordsSet.getDouble(SIMG_SCORE_DELAUNAY);
												
						System.out.println("Model " + filename + " has sim_g score " + simg_score);
						
						/* advance the cursor */
						recordsToProcess = dumpAllRecordsSet.next();					
					}
				}
				else {
					System.err.println("No result set entries to process");
					return false;
				}				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				boolean result = statement.execute(selectAllDelaGlbStmt);
				
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
				
				/* Show each record from global table*/
				if (dumpAllRecordsSet != null) {					
					/* Move the cursor to the first record */
					System.out.println("Moving to the first record");
					boolean recordsToProcess = dumpAllRecordsSet.next();
					
					/* Process the first and all remaining records */
					while (recordsToProcess) {
						String filename = dumpAllRecordsSet.getString(FILENAME_COLUMN);
						Point p1 = new Point(dumpAllRecordsSet.getDouble(TRIAD_X1),
								 			 dumpAllRecordsSet.getDouble(TRIAD_Y1));
						Point p2 = new Point(dumpAllRecordsSet.getDouble(TRIAD_X2),
					 			 			 dumpAllRecordsSet.getDouble(TRIAD_Y2));
						Point p3 = new Point(dumpAllRecordsSet.getDouble(TRIAD_X3),
		 			 			 			 dumpAllRecordsSet.getDouble(TRIAD_Y3));
												
						System.out.println("Model " + filename);
						
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
				                + File.separatorChar + dbLocalTable + "_" 
				                + System.currentTimeMillis() 
				                + ".tgz' BLOCKING";
		
		int gotDB = doesDBExist();		
		if (gotDB == NUMBER_RELATIONS) {
			System.err.println(databaseName + " database already exists");
		}
		else {
			System.out.println(databaseName + " database does not exist "
					+ "or was not fully created");
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
	public static synchronized int doesDBExist() {
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
			   return 0;	
			}
			
			/* the obstruction table was found */
			if (tblCnt > 0) {
				System.out.println(databaseName + " database exists");
				System.out.println("A total of " + tblCnt + " relations were found");
				return tblCnt;
			}
			else {
				System.out.println(databaseName + " does not exists");
				return 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
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
	public static String getChainCode(int id) {
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
	public static Point getMoment(int id) {
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
	
	public static double[] getThresholds(int firstID, int lastID, boolean retrieveUpper) {
		ArrayList<Double> thresholds = null;
		try {			
			if ((connection != null) && 
					(!connection.isClosed())) {
				
				/* Prepare statement to get upper/lower thresholds for a particular image r*/
				PreparedStatement ps = null;
				if (retrieveUpper) {
					ps = connection.prepareStatement(selectUpperThresholds);					
				}
				else {
					ps = connection.prepareStatement(selectLowerThresholds);
				}
				
				/* set starting and ending IDs needed to get the thresholds for a particular image */				
				ps.setInt(1, firstID);
				ps.setInt(2, lastID);
				
				/* Execute the SQL to get all the upper/lower thresholds for a given image */
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					thresholds = new ArrayList<Double>(ps.getFetchSize());
					if (retrieveUpper) {
						while (rs.next()) {
							thresholds.add(rs.getDouble(THETA2_COLUMN));														
						}						
					}
					else {
						while (rs.next()) {
							thresholds.add(rs.getDouble(THETA1_COLUMN));
						}
					}					
					
				}
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return thresholds.stream().mapToDouble(d -> d).toArray();
	}
	
	/**
	 * Return the model images containing a given moment
	 * in any of the segments
	 * @param momentx -- x coordinate of the moment
	 * @param momenty -- y coordinate of the moment
	 * @return models containing the moment
	 */
	public static ArrayList<String> getFilesWithMoment(
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
	public static String getFileName(int id) {
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
	 * Get the simG score for a Delaunay graph of a model image
	 * @param filename -- model image filename
	 * @return simG score
	 */
	public static double getSimGScore(String filename) {
		try {
			
			// There are no negative ids or segments
			if ((filename == null) || (filename.isEmpty())) {
				return -1.0;
			}
			
			if ((connection != null) && (!connection.isClosed())) {
				PreparedStatement ps = 
						connection.prepareStatement(selectsimGDelaunayValue);
				ps.setString(1, filename);
				boolean result = ps.execute();
				if (result) {
					ResultSet rs = ps.getResultSet();
					if (rs != null) {
						result = rs.next();
						if (result)
						   return rs.getDouble(1);
						else {
							System.err.println("Error retrieving "
									+ "individual simG score for " + filename);
						}
					}
					else {
						System.err.println("No result set for " + filename);
					}
				}
				else {
					System.err.println("There was no result from the query for " + filename);
					return -2;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return -3;
		}
		return -4.0; 
	}
	
	/**
	 * Retrieve all the model filenames in the database
	 * @return
	 */
	public static List<String> getAllModelFileName(){	
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
			e.printStackTrace();
		}
		return modelNames;
	}
	
	public static List<PointMatchContainer> getImagesMatchingCCStart(Point ccStart){
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
