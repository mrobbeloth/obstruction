package robbeloth.research;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.Damerau;
import info.debatty.java.stringsimilarity.JaroWinkler;
import info.debatty.java.stringsimilarity.LongestCommonSubsequence;
import info.debatty.java.stringsimilarity.MetricLCS;
import info.debatty.java.stringsimilarity.NGram;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import info.debatty.java.stringsimilarity.OptimalStringAlignment;
import info.debatty.java.stringsimilarity.QGram;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.impl.common.Levenshtein;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import plplot.core.*;
import static plplot.core.plplotjavacConstants.*;

/**
 * This class implements the Local-Global (LG) Algorithm based on a 
 * highly derivative MATLAB code and since extensively modified for use
 * in a Java environment using OpenCV and other third party APIs
 * 
 * @author Michael Robbeloth
 * @category Projects
 * @since 2/7/2015 
 * @version 0.3
 * <br/><br/>
 * Class: CEG7900<br/>
 * <h2>Revision History</h2><br/>
 * <b>Date						Revision</b>
 *    7/18/2015					 (0.3) Place source into github
 *                                     closest approximation to source used
 *                                     at NAECON 2015 talk
 *                                     
 *                                     Remove writing a second threshold
 *                                     copy to disk
 *                                     
 *                                     Write hierarchy contour data to disk
 *                                     after contours are found, not before
 *                                     
 *                                     fix problems with second set of contours
 *                                     being written to disk using the wrong
 *                                     data set
 *                                     
 *                                     add documentation
 *                                     
 *                                     remove writing copied data to images for
 *                                     verification of operations, I think it's
 *                                     safe to skip this...if something weird
 *                                     occurs it can easily be added back in
 *                                     as a single line piece of code
 *                                     
 *                                     After removing the writing of threshold2 
 *                                     images use threhold images the only diff 
 *                                     with threshold2 was its use in ops whose 
 *                                     output is separately wrote to disk
 *     
 *     5/29/2016                 (0.4) Revision history is now in github logs 
 *     
 */
public class LGAlgorithm {
	private final static String avRowsString = "Average Rows";
	private final static String avColsString = "Average Columns";
	private final static String avIntString = "Average Itensity";
	
	/* This enumeration tells the LG Algorithm how to process the image */
	public enum Mode {
		PROCESS_MODEL, 
		PROCESS_SAMPLE;
	}
	
	/**
	 * Local Global (LG) Graph Run Me Bootstrap Algorithm
	 * @param data -- input image
	 * @param K -- number of sets to partition data into
	 * @param clustered_data -- holder for data clusters
	 * @param criteria -- termination criteria
	 * @param attempts -- number of iterations to use in partitioning data
	 * @param flags -- special processing indicators (not used 
	 * @param filename -- name of file that is being processed
	 * @param pa -- partitioning algorithm choice for OpenCV partitioning
	 * @param debug_flag -- calls to add extra output files or data where
	 * needed to verify correct operation
	 */
	public static CompositeMat LGRunME(Mat data, int K, Mat clustered_data, 
			                            TermCriteria criteria, int attempts,
			                            int flags, String filename, 
			                            ProjectUtilities.Partitioning_Algorithm pa,
			                            Mode mode, boolean debug_flag){	
		// Deliverables
		Mat labels = null;		
		
		// sanity check the number of clusters
		if (K < 2) {
			System.err.println("The number of clusters must be greater than or equal to two.");
			System.exit(1);
		}
		
		// sanity check that there is some data to work with
		if (data.total() == 0) {
			System.err.println("There must be some input data to work with for analysis.");
			System.exit(2);
		}
		
		/* Minimizing cpu/memory requirements to lower processing overhead
		 * Michael 2/27/2017 */
		Mat converted_data_8U = new Mat(data.rows(), data.cols(), CvType.CV_8U);
		data.convertTo(converted_data_8U, CvType.CV_8U);
			
		/* verify we have the actual full model image to work with
		 * at the beginning of the process */
		if (debug_flag) {
			Imgcodecs.imwrite("output/verify_full_image_in_ds" + "_" 
                    + System.currentTimeMillis() + ".jpg",
			          converted_data_8U);			
		}
		
		if ((flags & Core.KMEANS_USE_INITIAL_LABELS) == 0x1) {
			labels = 
					ProjectUtilities.setInitialLabelsGrayscale(
							converted_data_8U.rows(), 
							converted_data_8U.height(), K);
			System.out.println("Programming initial labels");
			System.out.println("Labels are:");
			System.out.println(labels.dump());
		}
		else {
			labels = new Mat();
		}
		
		// start by smoothing the image -- let's get the obvious artifacts removed
		// start by smoothing the image -- let's get the obvious artifacts removed
		Mat centers = new Mat();
		kMeansNGBContainer container = null;
		long tic = System.nanoTime();
		
		/* Aggressively sharpen and then remove noise */
		converted_data_8U = ProjectUtilities.sharpen(converted_data_8U);
		if (debug_flag) {
			Imgcodecs.imwrite("output/" + filename.substring(
			          filename.lastIndexOf('/')+1,filename.lastIndexOf('.'))
			          +"_sharpen.jpg", 
			          converted_data_8U);	
		}
		/* the h parameter here is quite high, 85, to remove lots of detail that
		 * would otherwise generate extra segments from the clusters -- 
		 * we loose fine details, but processing times are lower */
		Photo.fastNlMeansDenoising(
				converted_data_8U, converted_data_8U, 85, 7, 21);	
		Imgcodecs.imwrite("output/" + filename.substring(
		          filename.lastIndexOf('/')+1,filename.lastIndexOf('.'))
		          +"_denoise.jpg", 
		          converted_data_8U);	
		
		// after smoothing, let's partition the image
		/* produce the segmented image using NGB or OpenCV Kmeans algorithm */
		if (pa.equals(ProjectUtilities.Partitioning_Algorithm.OPENCV)) {
			Mat colVec = converted_data_8U.reshape(
					1, converted_data_8U.rows()*converted_data_8U.cols());
			Mat colVecFloat = new Mat(
					colVec.rows(), colVec.cols(), colVec.type());
			colVec.convertTo(colVecFloat, CvType.CV_32F);
			   
			/* labels -- i/o integer array that stores the cluster indices 
			 * for every sample 
			 * 
			 * centers --  Output matrix of the cluster centers, one row per 
			 * each cluster center.
			 * 
			 * Note this does not change the image data sent to the array, the 
			 * clustering of image data itself has to be done in a 
			 * post processing step */
			System.out.println("flags="+flags);
			double compatness = Core.kmeans(colVecFloat, K, labels, criteria, attempts, 
					                         flags, centers);
			System.out.println("Compatness="+compatness);
			Mat labelsFromImg = labels.reshape(1, converted_data_8U.rows());
			container = opencv_kmeans_postProcess(converted_data_8U,  labelsFromImg, centers);
		}
		else if (pa.equals(ProjectUtilities.Partitioning_Algorithm.NGB)) {
			data.convertTo(converted_data_8U, CvType.CV_8U);
			container = kmeansNGB(converted_data_8U, K, attempts);			
		}
		else {
			System.err.println("Paritioning algorithm not valid, returning");
			return null;
		}
		
		
		clustered_data = container.getClustered_data();
		long toc = System.nanoTime();
		System.out.println("Partitioning time: " + 
				TimeUnit.MILLISECONDS.convert(toc - tic, TimeUnit.NANOSECONDS) + " ms");		
		
		// look at intermediate output from kmeans
		if (debug_flag && pa.equals(ProjectUtilities.Partitioning_Algorithm.OPENCV)) {
			Imgcodecs.imwrite("output/" + "opencv" + "_" + System.currentTimeMillis() + ".jpg", 
			          clustered_data);		
		}
		else if (debug_flag && pa.equals(ProjectUtilities.Partitioning_Algorithm.NGB)) {
			Imgcodecs.imwrite("output/" + "kmeansNGB" + "_" + System.currentTimeMillis() + ".jpg", 
			          clustered_data);		
		}
	
		// scan the image and produce one binary image for each segment
		CompositeMat cm = ScanSegments(clustered_data);
		cm.setFilename(filename);
		ArrayList<Mat> cm_al_ms = cm.getListofMats();
		int segCnt = 0;
		for(Mat m : cm_al_ms) {
			Mat n = new Mat(m.rows(), m.cols(), m.type());
			if (m.type() != CvType.CV_8U) {
				n = new Mat(m.rows(), m.cols(), m.type());
				m.convertTo(n, CvType.CV_8U);	
				
			}
			else {
				n = m;
			}

			/* Just retain the edge pixels in white for each section for each 
			 * segment there will be more segments as the user asks for more 
			 * clusters -- no thresholds to get as many edges as possible, 
			 * lots of extraenous details removed in preprocessing ops */
			Imgproc.Canny(n, n, 0, 0);

			/* Dilate edges to make them stand out better*/
			Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
			                                       new Size( 2, 2 ),
			                                       new Point( 1, 1 ) );
			Imgproc.dilate(n, n, element);
			if (debug_flag) {
				Imgcodecs.imwrite("output/" + filename.substring(
						   filename.lastIndexOf('/')+1, 
				           filename.lastIndexOf('.')) +
						   "_segments_after_threshold"
				           + (++segCnt) + "_" + System.currentTimeMillis() 
				           + ".jpg", n);				
			}
			
			/* WARNING: Do not autocrop otherwise L-G Graph Algorithm
			 * calculations will be utterly wrong */
		}
	
		// Show time to scan each segment
		Mat scanTimesPerSegment = cm.getMat();
		int rowsSTPS = scanTimesPerSegment.rows();
		int colsSTPS = scanTimesPerSegment.cols();
		StringBuilder sb = new StringBuilder();
		sb.append("Scan Times/Segment:");
		long totalTime = 0;
		if (rowsSTPS == 1) {
			sb.append("[");
			
			for (int i = 0; i < colsSTPS ; i++) {
				Double segScanTime = scanTimesPerSegment.get(0, i)[0];
				long convertedSegScanTime = (long)(double)segScanTime;
				long time = TimeUnit.MILLISECONDS.convert(
						convertedSegScanTime, TimeUnit.NANOSECONDS);
				sb.append(time + " ms ,");
				totalTime += (long)(double)segScanTime;
			}
			sb.append("]");
			sb.deleteCharAt(sb.length()-1);
			sb.append("\n");
			sb.append("Average Scan Time/Segment: " +
			          TimeUnit.MILLISECONDS.convert(
			        		  totalTime / colsSTPS, TimeUnit.NANOSECONDS)  
			          + "ms\n");
			sb.append("Total scan time: " +  TimeUnit.MILLISECONDS.convert(
			           totalTime, TimeUnit.NANOSECONDS) + " ms" + "\n");
			System.out.print(sb.toString());
		}				
		
		// calculate the local global graph, specify string similarity method for now
		// maybe move up to user choice later on
		List<String> ssaChoices = Arrays.asList("QGram (Ukkonen) Distance", 
											    "Longest-Common-Subsequence");
		localGlobal_graph(cm_al_ms, container, filename, 
				          pa, mode, debug_flag, cm, ssaChoices);
		
		return cm;
	}
	/**
	 * Use data from OpenCV kmeans algorithm to partition image data
	 * @param data -- input image
	 * @param labels -- i/o integer array that stores the cluster indices 
	 * for every sample
	 * @param centers -- Output matrix of the cluster centers, one row per 
	 * each cluster center.
	 * @return partitioned image
	 */
	private static kMeansNGBContainer
		opencv_kmeans_postProcess(Mat data, Mat labels, Mat centers) {
		if (data.channels() == 3) {
			data.reshape(3);	
		}
		
		/* Setup data structure holding partitioned image data */
		Mat clustered_data = new Mat(data.rows(), data.cols(), 
				                     data.type(), new Scalar(0));
		HashMap<String, Mat> stats = new HashMap<String, Mat>();
		
		/* Keep stats on partitioning process */
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		for(int i = 0; i < centers.rows(); i++) counts.put(i, 0);
		
		/* Run image against centroids and assign pixels to clusters */
		int data_height = data.height();
		int data_width = data.width();
		MinMaxLocResult mmlr = Core.minMaxLoc(labels);
		
		if (data.channels() == 3) {
			/* For each pixel in the image */
			for(int y = 0; y < data_height; y++) {
				for(int x = 0; x < data_width; x++) {
					/* Get the cluster the pixel is assigned to 
					 * label is in 1D format*/
					int labelb = (int)labels.get(y, x)[0];
					int labelg = (int)labels.get(y, x)[1];
					int labelr = (int)labels.get(y, x)[2];
					
					/* Update stats that this pixel will get assigned to 
					 * label specified cluster */
					counts.put(labelb, counts.get(labelb) + 1);
					
					/* Copy pixel into cluster data structure with the color
					 * of the specified centroid */
					clustered_data.put(y, x, 
							((labelb+mmlr.minVal)/mmlr.maxVal)*255,
							((labelg+mmlr.minVal)/mmlr.maxVal)*255,
							((labelr+mmlr.minVal)/mmlr.maxVal)*255);
				}
			}			
		}
		else if (data.channels() == 1) {
			/* For each pixel in the image */
			for(int y = 0; y < data_height; y++) {
				for(int x = 0; x < data_width; x++) {
					/* Get the cluster the pixel is assigned to 
					 * label is in 1D format*/
					int label = (int)labels.get(y, x)[0];
					
					/* Update stats that this pixel will get assigned to 
					 * label specified cluster */
					counts.put(label, counts.get(label) + 1);
					
					/* Copy pixel into cluster data structure with the color
					 * of the specified centroid */					
					clustered_data.put(y, x,  ((label+mmlr.minVal)/mmlr.maxVal)*255);					
				}
			}
		}
		
		System.out.println(counts);
		Set<Integer> region_cnts = counts.keySet();
		for(Integer rc : region_cnts) {
			Mat m = new Mat(1,1,CvType.CV_32FC1);
			Integer cnt = counts.get(rc);
			m.put(0, 0, cnt);
			stats.put(rc.toString(), m);
		}
		kMeansNGBContainer kmNGBCnt = new kMeansNGBContainer(clustered_data, stats);
		return kmNGBCnt;
	}

	/**
	 * Calculate the local global graph -- this portion constitutes the 
	 * image analysis and feature recognition portion of the system
	 * 
	 * @param Segments   -- list of image segments from segmentation process 
	 *                       with edge detection applied
	 * @param n -- data from application of kMeans algorithm
	 * @param filename   -- name of file being worked on
	 * @param pa         -- partitioning algorithm used
	 * @param mode       -- model or sample image
	 * @param debug_flag -- Whether or not to generate certain types of output
	 * @param cm         -- Composite Matrix of image data (contains unmodified
	 *                       image segments)
	 * to aid in verification or troubleshooting activities
	 * @param ssaChoices  -- String similarity algorithm choices
	 * @return the local global graph description of the image 
	 */
	public static ArrayList<LGNode> localGlobal_graph(ArrayList<Mat> Segments, 
			                                kMeansNGBContainer kMeansData, 
			                                String filename,
			                                ProjectUtilities.Partitioning_Algorithm pa, 
			                                Mode mode, boolean debug_flag, 
			                                CompositeMat cm, List<String> ssaChoices) {
		// Data structures for sample image
		Map<Integer, String> sampleChains = 
				new TreeMap<Integer, String>();
		Map<Integer, Point> sampleMoments = 
				new TreeMap<Integer, Point>();
		ArrayList<Point> sampleccStartPts = new ArrayList<Point>();
		
		// Connect to database
		int segmentNumber = 1;
		int lastSegNumDb = -1;
		if (mode == Mode.PROCESS_MODEL) {
			lastSegNumDb = DatabaseModule.getLastId();
			System.out.println("localGlobal_graph(): Last used id: " + 
								lastSegNumDb);
			
			// Initialize database if necessary
			if (!DatabaseModule.doesDBExist()) {
				DatabaseModule.createModel();
			}
			
		}
		
		/* Initialize the Global Graph based on number of segments from 
		   partitioning  */
		Mat clustered_data = null;
		if (kMeansData != null) {
			clustered_data = kMeansData.getClustered_data();
		}		
		ArrayList<LGNode> global_graph = new ArrayList<LGNode>(Segments.size());
		int n = Segments.size();
		System.out.println("The global graph says there are " + n + " total segments");
		
		// 
		ArrayList<Double> t1 = new ArrayList<Double>();
		ArrayList<Double> t2 = new ArrayList<Double>();
		
		// Initialize array to keep track of time to generate L-G graph on segment
		ArrayList<Long> timing_array = new ArrayList<Long>();
				
		/* Initialize array to hold centroids in L-G graph
		   called S array in original MATLAB code */
		ArrayList<Point> centroid_array = new ArrayList<Point>(n);
		
		/* Initialize array to hold generated chain codes for 
		 * each image segment*/
		ChainCodingContainer ccc = null;
		
        // Make sure output directory exists
        File outputDir = new File("output/");
        if (!outputDir.exists()) {
        	outputDir.mkdirs();
        }
		
		/* This section does the following two things:
		   1. Construct the local portion of the Local Global graph..
		   this portion focuses on the geometric description of the
		    line segments that  define the segment under analysis 
		    
		    big hairy for loop that follows
		
		   2. Build the overall Global part of the Local-Global graph
	       the global portion focuses on the establishment of the centroid 
	       regions and the connection of the starting segment centroid to
	       the other centroids in the other segments as a part of creating
	       an overall geometrical description of a model or target image
	       */
		for(int i = 0; i < n; i++) {
			long tic = System.nanoTime();
			
			/* Generate a representation of the segment based upon how
			 * the various border connected pixels are connected to one another  */
			Mat segment = Segments.get(i).clone();
			ccc = chaincoding1(segment);
			if (debug_flag) {
				System.out.println(ccc);	
			}
			
			t1.add(ccc.getChain_time());
			ArrayList<Double> cc = ccc.getCc();
			Point start = ccc.getStart();

			/* Use the chain code description of the segment to create a 
			 * border */
			Mat border = ccc.getBorder();	
			Mat convertedborder;
			
			/* Down sample border into unsigned 8 bit integer value, 
			 * far less taxing on CPU and memory  */
			if (border.type() != CvType.CV_8U) {
				convertedborder = new Mat(
						border.rows(), border.cols(), border.type());
				border.convertTo(convertedborder, CvType.CV_8U);	
			}			
			else {
				convertedborder = border;
			}
			
			/* Invert the colors but don't remove any data, allow the
			 * entire "color" range to make it through */
			Imgproc.threshold(convertedborder, convertedborder, 0, 255, 
					          Imgproc.THRESH_BINARY_INV);
			
			/* if needed, verify results for chain code to border image
			 * generation for researcher*/
			if (debug_flag) {
				Imgcodecs.imwrite("output/" + filename.substring(
						   filename.lastIndexOf('/')+1, 
				           filename.lastIndexOf('.')) + 
				           "_converted_border_"+(i+1)+ "_" + System.currentTimeMillis() 
				           + ".jpg",convertedborder);				
			}

			Mat croppedBorder = 
					ProjectUtilities.autoCropGrayScaleImage(convertedborder, true);
			if (debug_flag) {
				Imgcodecs.imwrite("output/" + filename.substring(
						   filename.lastIndexOf('/')+1, 
				           filename.lastIndexOf('.')) + 
				           "_cropped_border_"+(i+1)+ "_" + System.currentTimeMillis() 
				           + ".jpg",croppedBorder);				
			}
			ccc.setBorder(croppedBorder);
			System.out.println("original border area=" + convertedborder.size().area());
			System.out.println("cropped border area=" + croppedBorder.size().area());
			if (convertedborder.size().area() < croppedBorder.size().area()) {
				System.out.print("Cropped image is larger, outlier");
				System.out.println(" Redo the chain code, as canny or similar filter was applied");
				ccc = chaincoding1(croppedBorder);
				cc = ccc.getCc();
				System.out.println("New chain code length is " + cc.size());
			}
			
			/* Using the chain code from the previous step, generate 
			 * the line segments of the segment using the greatest
			 * possible sensitivity */
			LineSegmentContainer lsc = 
					line_segment(cc, start, 1);		
			if (debug_flag) System.out.println(lsc);
			
			/* Generate a pictoral representation of the line segments
			 * using plplot and save to disk */
	        
	        /* Convert segment arrays into a format suitable for
	         * plplot use */
			ArrayList<Mat> segx = lsc.getSegment_x();
			ArrayList<Mat> segy = lsc.getSegment_y();
			
			/* if needed, show plplot output of segment's border */
			if (debug_flag) {
			   double[] x = ProjectUtilities.convertMat1xn(segx, true);
			   double[] y = ProjectUtilities.convertMat1xn(segy, true);
			        
		        /* Determine limits to set in plot graph for plplot
		         * establish environment and labels of plot 
		         * Add ten pixels of padding for border
		         * data is reversed coming out of line segment  */
		        double xmin = ProjectUtilities.findMin(y);
		        double xmax =  ProjectUtilities.findMax(y);
		        double ymin = ProjectUtilities.findMin(x);
		        double ymax =  ProjectUtilities.findMax(x);
		        
			    // Initialize plplot
				PLStream pls = new PLStream();			
		        // Parse and process command line arguments
				pls.parseopts( new String[]{""}, PL_PARSE_FULL | PL_PARSE_NOPROGRAM );
		        pls.setopt("verbose","verbose");
		        pls.setopt("dev","jpeg");
		        pls.scolbg(255, 255, 255); // set background to white
		        pls.scol0(15, 0, 0, 0); // axis color is black
		        pls.setopt("o", outputDir.toString() + "/" + filename.substring(
						   filename.lastIndexOf('/')+1, 
				           filename.lastIndexOf('.')) + "_line_segment_" 
						   + (i+1) + "_" + System.currentTimeMillis() + ".jpg");
		        
		        /* Initialize plplot, 
		         * use a ten pixel border using inverted y axis graph
		         * to match pixel arrangement of pc monitor, 
		         * and set the title */
		        pls.init();
		        pls.env(xmin-10, xmax+10, ymax+10, ymin-10, 0, 0);
		        pls.lab( "x", "y", "Rebuilt Segment " + (i+1) + " Using Chain Code");
		        
		        /* Plot the data that was prepared above.
		           Data comes out reversed from line segment construction */
		        pls.line(y,x);

		        // Close PLplot library
		        pls.end();				
			}
	        
			/* Derive the local graph shape description of segment 
			 * under consideration */
			long tic2 = System.nanoTime();
			ArrayList<CurveLineSegMetaData> lmd = shape_expression(segx, segy);
			long toc2 = System.nanoTime();
			long duration2 = toc2 - tic2;
			System.out.println("Shape Expression Took: " + TimeUnit.MILLISECONDS.convert(
					duration2, TimeUnit.NANOSECONDS) + " ms");
			
			if (debug_flag) {
				System.out.println("Shape expression of segment " + (i + 1) + ":");
				System.out.println(lmd);
			}
			
			if (lmd != null) {
				tic2 = System.nanoTime();
				determine_line_connectivity(lmd);	
				toc2 = System.nanoTime();
				duration2 = toc2 - tic2;
				System.out.println("Determining Line Connectivity Took: " + 
						TimeUnit.MILLISECONDS.convert(
						duration2, TimeUnit.NANOSECONDS) + " ms");				
			}
			else {
				lmd = new ArrayList<CurveLineSegMetaData>();
				lmd.add(new CurveLineSegMetaData());
			}
			
			/* Store the amount of time it took to generate SH for 
			 * segment i, see (1) and (2) in 2008 paper */
			t2.add((double) lsc.getSegment_time());			
			double lg_time = t1.get(i) + t2.get(i);
			
			/* call S(i)  = regionprops(Segments(:,:,i), 'centroid');
			   Note moments have not been exposed through JNI on opencv 3.0 yet
			   Moments are used as part of curve matching, in particular to find
			   the scale parameter of an object s = (mom'/mom)^(1/2)
			   
			    From Bourbakis paper, moments help us to find the center point
			    of a region...
			    
			    Should hold up to translation and rotation on a candidate object */
			double[][] img = ProjectUtilities.convertMatToDoubleArray(segment);		
			System.out.println("Raw Moment " + (i+1) + ":" + Moments.getRawCentroid(img));
			Point centroid = Moments.getRawCentroid(img);
			
			/* keep a copy of centroids for use in the construction of the
			   global portion of the geometric/pictorial description of the image
			   
			   this will aid in future matching of multiple regions using this 
			   method (section 3.0 of 2008 Bourbakis paper) */
			centroid_array.add(centroid);
			
			// store time to generate LG graph on segment
			long toc = System.nanoTime();
			timing_array.add(toc - tic);
			
			/* Build ith node containing local node description, which 
			 * forms a part of the overall global geometric description
			 * of the image */
			HashMap<String, Mat> stats = null;
			HashMap<String, Double> segment_stats = null;
			if (pa.equals(ProjectUtilities.Partitioning_Algorithm.NGB)) {
				stats = kMeansData.getStats();
				segment_stats = new HashMap<String, Double>(3);
				Mat avRows = stats.get(avRowsString);
				Double averageRows = null;
				if ((avRows != null) && (avRows.get(0, i) != null)) {
					averageRows = avRows.get(0, i)[0];	
				}
				else {
					System.out.println("WARNING: No Row stats to retrieve");
				}
				
				Mat avCols = stats.get(avColsString);
				Double averageColumns = null;
				if ((avCols != null) && (avCols.get(0, i) != null)) {
					averageColumns = avCols.get(0, i)[0];	
				}
				else {
					System.out.println("WARNING: No Column stats to retrieve");
				}
				Mat avIntensity = stats.get(avIntString);
				Double averageIntensity = null;
				if ((avIntensity != null) && (avIntensity.get(0, i) != null)) {
					averageIntensity = avIntensity.get(0,  i)[0];	
				}
				else {
					System.out.println("WARNING: No Intensity stats to retrieve");
				}
				
				/* warning: segments added from region growing do not contain
				 * Statistical data, they will be null */
				segment_stats.put(avRowsString, averageRows);
				segment_stats.put(avColsString, averageColumns);
				segment_stats.put(avIntString, averageIntensity);				
			}
			else if (pa.equals(ProjectUtilities.Partitioning_Algorithm.OPENCV)){
				if (kMeansData != null) {
					stats = kMeansData.getStats();	
				}				
				segment_stats = new HashMap<String, Double>();
				
				Set<String> statKeys = null;
				if (stats != null) {
					statKeys = stats.keySet();					
				}
				else {
					statKeys = new HashSet<String>();
				}
				
				for (String s : statKeys) {
					Mat m = stats.get(s);
					Double d = m.get(0,0)[0];
					segment_stats.put(s, d);
				}
			}
			
			/* Create the node */
			LGNode lgnode = new LGNode(centroid, segment_stats, 
					                   croppedBorder, lmd, pa, i);
			
			/* Add local region info to overall global description */
			global_graph.add(lgnode);
			
			/* Add entry into database if part of a model image */
			if (mode == Mode.PROCESS_MODEL) {				
				int id = DatabaseModule.insertIntoModelDB(filename, 
						                         segmentNumber++, 
						                         ccc.chainCodeString(), 
						                         centroid_array.get(i),
						                         start);
				
				System.out.println("Added id "+ id + " into database ");
			}			
			else {
				// add to data structure
				sampleChains.put(i, ccc.chainCodeString());
				sampleMoments.put(i, centroid_array.get(i));
				sampleccStartPts.add(start);
			}
			
			/* Debug -- show info about region to a human */
			if (debug_flag) System.out.println(lgnode.toString());
		}  // end big hairy for loop on building local nodes 100s loc earlier
		
	    // Initialize plplot stream object 
		PLStream   pls = new PLStream();
		
        // Parse and process command line arguments
		pls.parseopts( new String[]{""}, PL_PARSE_FULL | PL_PARSE_NOPROGRAM );
        pls.setopt("verbose","verbose");
        pls.setopt("dev","jpeg");
        pls.scolbg(255, 255, 255); // set background to white
        pls.setopt("o", "output/" + filename.substring(
				   filename.lastIndexOf('/')+1, 
		           filename.lastIndexOf('.')) +
        		   "_centroids_for_image" + "_" + System.currentTimeMillis() 
        		   + ".jpg");
        
        // Initialize plplot engine
        pls.init();
        
        /* Convert Point objects into a format suitable for
         * use by plplot
         */
        int sizeConversion = centroid_array.size();
        double[] xValues = new double[centroid_array.size()];
        double[] yValues = new double[centroid_array.size()];
        int sizeForLines = centroid_array.size()*2;
        double[] xValuePrime = new double[sizeForLines-1];
        double[] yValuePrime = new double[sizeForLines-1];
        double startingX = centroid_array.get(0).x;
        double startingY = centroid_array.get(0).y;
		for(int cnt = 0; cnt < sizeConversion; cnt++) {
			xValues[cnt] = centroid_array.get(cnt).x;
			yValues[cnt] = centroid_array.get(cnt).y;
		}
		int indexOtherArray = 1;
		for(int cnt = 0; indexOtherArray < sizeConversion; cnt+=2) {
			xValuePrime[cnt] = startingX;
			xValuePrime[cnt+1] = centroid_array.get(indexOtherArray).x;
			yValuePrime[cnt] = startingY;
			yValuePrime[cnt+1] = centroid_array.get(indexOtherArray++).y;
		}
        
        /* Determine limits to set in plot graph for plplot
         * establish environment and labels of plot */
        double xmin = ProjectUtilities.findMin(xValues);
        double xmax =  ProjectUtilities.findMax(xValues);
        double ymin = ProjectUtilities.findMin(yValues);
        double ymax =  ProjectUtilities.findMax(yValues);
        pls.env(xmin, xmax, ymax, ymin, 0, 0);
        pls.lab( "x", "y", "Centroids for image " + 
        		 filename.substring(filename.lastIndexOf('/')));
        
        // Plot the data that was prepared above.
        // Symbol 25 is a medium sized circle glyph
        pls.poin(xValues, yValues, 25);
        pls.col0(3);
        pls.line(xValuePrime, yValuePrime);

        // Close PLplot library
        pls.end();
		       
        // Display timing data for each segment in algorithm
        if (debug_flag) {
    		Long T = 0l;
    		int cntTs = 1;
    		for(Long l : timing_array) {
    			System.out.println("Time to generate segment " 
    		                        + cntTs++ + " is " + 
    		                        TimeUnit.MILLISECONDS.convert(l, 
    		                        TimeUnit.NANOSECONDS) + " ms");
    			T += l;
    		}    		        
        }

		/* Build the structures needed for the displaying of the LG
		 * graph over the segmented image */	
		Mat C = new Mat(2, n, CvType.CV_64FC1);
		// Setup for getting directional vectors from centroids
		// C(:,i) = S(1,i).Centroid; and C = floor (C);
		// x's are in first row, y's in second 
		for (int i = 0; i < n; i++) {
			Point Sp = centroid_array.get(i);
			C.put(0, i, Math.floor(Sp.x));
			C.put(1, i, Math.floor(Sp.y));
		}
		
		/* Carryover of code from matlab for generating line 
		 * segments from the start node region's centroid to 
		 * every other region's centroid ?*/
		long tic = System.nanoTime();
		double[][] DirVector1 = new double[1][2];
		double[][] DirVector2 = new double[1][2];
		double[] Angle = new double[n];
		/* Calculate the distance and angle of each line from start node
		 * centroid to local graph node centroid 
		 * 
		 *  Does this get used by plplot? -- TODO USE THIS IN MATCHING FRAMEWORK*/
		for (int i = 0; i < n-1; i++) {
			if (i == n-2) {
				// DirVector1 = C(:,1)' - C(:,1+i)';
				DirVector1[0][0] = (C.get(0, 0)[0] - C.get(0, i)[0]);
				DirVector1[0][1] = (C.get(1, 0)[0] - C.get(1, i)[0]);
				// DirVector2 = C(:,1)' - C(:,2)';
				DirVector2[0][0] = (C.get(0, 0)[0] - C.get(0, 2)[0]);
				DirVector2[0][1] = (C.get(1, 0)[0] - C.get(1, 2)[0]);
				/* Angle(i) =
				 * acos( dot(DirVector1,DirVector2)/norm(DirVector1)/norm(DirVector2)); 
				 */
				Mat DirVector1Mat = 
						ProjectUtilities.convertDoubletoGrayscaleMat(DirVector1, 1, 2);
				Mat DirVector2Mat = 
						ProjectUtilities.convertDoubletoGrayscaleMat(DirVector2, 1, 2);
				double dotProduct = DirVector1Mat.dot(DirVector2Mat);
				Angle[i] = Math.acos(dotProduct / Core.norm(DirVector1Mat) / 
						   Core.norm(DirVector2Mat));			
				
			}
			else {
				// DirVector1 = C(:,1)' - C(:,1+i)';
				DirVector1[0][0] = (C.get(0, 0)[0] - C.get(0, i)[0]);
				DirVector1[0][1] = (C.get(1, 0)[0] - C.get(1, i)[0]);
				// DirVector2 = C(:,1)' - C(:,2)';
				DirVector2[0][0] = (C.get(0, 0)[0] - C.get(0, 1)[0]);
				DirVector2[0][1] = (C.get(1, 0)[0] - C.get(1, 1+i)[0]);
				/* Angle(i) =
				 * acos( dot(DirVector1,DirVector2)/norm(DirVector1)/norm(DirVector2)); 
				 */
				Mat DirVector1Mat = 
						ProjectUtilities.convertDoubletoGrayscaleMat(DirVector1, 1, 2);
				Mat DirVector2Mat = 
						ProjectUtilities.convertDoubletoGrayscaleMat(DirVector2, 1, 2);
				double dotProduct = DirVector1Mat.dot(DirVector2Mat);
				Angle[i] = Math.acos(dotProduct / Core.norm(DirVector1Mat) / 
						   Core.norm(DirVector2Mat));		
			}
		}
		long angle_time = System.nanoTime() - tic;
		System.out.println("Time to calcuate angle_time: " + 
							TimeUnit.MICROSECONDS.convert(
									angle_time,TimeUnit.NANOSECONDS)  
				           + " us");
		
		/* Build the line segments, grab the coordinates of the centroids and
		 * clustered data and pass to the constructLines routine */
		Mat lined = null;
		if (clustered_data != null) {
			lined = clustered_data.clone();	
		}
		else {
			lined = new Mat();
		}
				
		for (int i = 0; i < n; i++) {
			System.out.println("Building lines for segment " + i);
			
			// coords = [C(2,1) C(1,1);C(2,i+1) C(1,i+1)];
			// Get coordinates of start node and target node
			Mat coords = new Mat(2,2,CvType.CV_64FC1);
			coords.put(0, 0, C.get(1, 0));
			coords.put(0, 1, C.get(0, 0));
			coords.put(1, 0, C.get(1, i));
			coords.put(1, 1, C.get(0, i));
			
			// lined = plotlines(lined, coords);
			/* Build plot line from source to target/dest node */			
			lined = constructLines(lined, coords);
		}
		
		/*
		Mat border = null;
		if (ccc != null) {
			border = ccc.getBorder().clone();	
		}
		
		// [y1] = zeros(size(border));
 * 		Mat y1 = null;
		if (border != null) {
			Size sz = border.size();
			y1 = new Mat((int)sz.height, (int)sz.width, 
					     border.type(), Scalar.all(0)); 
		}*/
		/* highlight the starting and ending points
		 * connecting the segments with red circles
		 */
		
		// th = 0:pi/50:2*pi;
		Mat th = new Mat(1, 101, CvType.CV_64FC1);
		double angle = 0;
		for (int i = 0; i < n; i++, angle+=(Math.PI)/50) {
			th.put(0,  i, angle);
		}
		
		/* DEBUG print the final moments for me to review 
		 * Superimpose moments over clustered image data
		 * and write image data out to disk in an excel
		 * spreadsheet
		 * 
		 *  Start by creating file itself */
		Mat clustered_data_clone = null;
		if (clustered_data != null) {
			clustered_data_clone = clustered_data.clone();
		}
		else {
			System.err.println("No clustered data to clone");
		}
		int index = 0;
		XSSFWorkbook workbook = new XSSFWorkbook();
		FileOutputStream fileOut = null;
		try {
			fileOut = 
				new FileOutputStream( "output/" + 
						filename.substring(filename.lastIndexOf('/')+1, 
									       filename.lastIndexOf('.'))+ 
									       "_" + System.currentTimeMillis() + ".xlsx");
		} catch (FileNotFoundException e1) {
			System.err.println("File not found exception: " + e1.getMessage());
			e1.printStackTrace();			
		}
		
		/* Create moments sheet/tab within spreadsheet file */
		XSSFSheet sheet = workbook.createSheet(
				filename.substring(filename.lastIndexOf('/')+1, 
						           filename.lastIndexOf('.')) 
				+ "_Moments" + "_" + System.currentTimeMillis());
		XSSFRow headerRow = sheet.createRow(index);
		XSSFCell headerCell = headerRow.createCell(0);
		headerCell.setCellValue("Moment");
		headerCell = headerRow.createCell(1);
		headerCell.setCellValue("X");
		headerCell = headerRow.createCell(2);
		headerCell.setCellValue("Y");
		headerCell = headerRow.createCell(3);
		headerCell.setCellValue("Distance (from Start)");
		Point startCentroid = centroid_array.get(0);
		
		/* writing moment to standard out, to image data structure, 
		 * and spreadsheet */
		int caCnt = 0;
		for(Point p : centroid_array) {
			/* Write moment to standard output */
			System.out.println("Moment " + caCnt + ": " + 
			                    p.x + "," + p.y);
			caCnt++;
			
			/* Superimpose moment as a line from the starting
			 * region to the ith region center of mass */
			if (clustered_data_clone != null) {
				Imgproc.circle(
						clustered_data_clone, centroid_array.get(index), 5, 
						new Scalar(25, 25, 112));
				Imgproc.line(clustered_data_clone, centroid_array.get(0), 
						     centroid_array.get(index), new Scalar(25, 25, 112));				
			}
			
			/* Fill in ith row of the spreadsheet with the ith 
			 * moment */
			XSSFRow row = sheet.createRow(index+1);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue(index);
			cell = row.createCell(1);
			cell.setCellValue( p.x);
			cell = row.createCell(2);
			cell.setCellValue(p.y);
			double d = ProjectUtilities.distance(
						   startCentroid.x, p.x, startCentroid.y, p.y);
			cell = row.createCell(3);
			cell.setCellValue(d);
			/* Work with next region (node) */
			index++;
		}
		if (clustered_data_clone != null) {
			boolean imWriteResult = 
					Imgcodecs.imwrite("output/" + filename.substring(filename.lastIndexOf('/')+1) 
					          		  + "_moments_over_clustered_data" + "_" 
							          + System.currentTimeMillis() 
					          		  + ".jpg",
					          		  clustered_data_clone);
			System.out.println("Result of merging centroids onto clustered image: " 
					          		  + imWriteResult);			
		}
		
		/* Calculate angle threshold differences and write them out to 
		 * the spreadsheet*/
		Mat angle_differences  = calc_angle_differences(ccc.getStart(), centroid_array);
		XSSFSheet arc_sheet = workbook.createSheet(filename.substring(
				   filename.lastIndexOf('/')+1, 
		           filename.lastIndexOf('.')) 
		           + "calc_angle_differences" + "_" + System.nanoTime());
		XSSFRow headerRowarc = arc_sheet.createRow(0);
		XSSFCell headerCellarc = headerRowarc.createCell(0);
		headerCellarc.setCellValue("Node");
		headerCellarc = headerRowarc.createCell(1);
		headerCellarc.setCellValue("\u0398"+"1");
		headerCellarc = headerRowarc.createCell(2);
		headerCellarc.setCellValue("\u0398"+"2");
		headerCellarc = headerRowarc.createCell(3);
		headerCellarc.setCellValue("Size/Area (pixels)");
		headerCellarc = headerRowarc.createCell(4);
		for(int i = 0; i < angle_differences.rows(); i++) {
			
			// report angle thresholds for node
			XSSFRow row = arc_sheet.createRow(i+1);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue(i);
			cell = row.createCell(1);
			cell.setCellValue(angle_differences.get(i,0)[0]);
			cell = row.createCell(2);
			cell.setCellValue(angle_differences.get(i,1)[0]);
			cell = row.createCell(3);
			cell.setCellValue(global_graph.get(i).getSize());
		}
		
		// Free up resources used for spreadsheet
		try {
			workbook.write(fileOut);
			fileOut.close();
			workbook.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// if matching phase, call match method
		if (mode == Mode.PROCESS_SAMPLE) {
			XSSFWorkbook wkbkResults = new XSSFWorkbook();
			buildSummarySheet(wkbkResults);
						
			/* Chaincode matching methods */
			Thread levenshtein_thread = null;
			if (ssaChoices.contains("LevenShtein")) {
				levenshtein_thread = new Thread("Levenshtein") {
					public void run() {
						System.out.println("Matching using Levenshtein measure");
						match_to_model_Levenshtein(sampleChains, wkbkResults);	
					}				
				};
				System.out.println("Running thread: " + levenshtein_thread.getName());
				levenshtein_thread.start();
			}
			
			Thread n_levenshtein_thread = null;
			if (ssaChoices.contains("Normalized Levenshtein"))	{
				n_levenshtein_thread = new Thread("Normalized Levenshtein") {
					public void run() {
						System.out.println("Matching using Normalized Levenshtein measure");
						match_to_model_Normalized_Levenshtein(sampleChains, wkbkResults);	
					}				
				};
				n_levenshtein_thread.start();
				System.out.println("Running thread: " + n_levenshtein_thread.getName());				
			}


			Thread damerau_levenshtein_thread = null; 
			if (ssaChoices.contains("Damerau Levenshtein")) {
				damerau_levenshtein_thread = new Thread("Damerau Levenshtein") {
					public void run() {
						System.out.println("Matching using Damerau-Levenshtein");
						match_to_model_Damerau_Levenshtein(sampleChains, wkbkResults);		
					}				
				};
				damerau_levenshtein_thread.start();
				System.out.println("Running thread: " + damerau_levenshtein_thread.getName());				
			}		

			Thread ost_thread = null;
			if (ssaChoices.contains("Optimal String Alignment")) {
				ost_thread = new Thread("Optimal String Alignment") {
					public void run() {
						System.out.println("Optimal String Alignment");
						match_to_model_Opt_Str_Alignment(sampleChains, wkbkResults);	
					}				
				};
				ost_thread.start();
				System.out.println("Running thread: " + ost_thread.getName());					
			}
		
			Thread jaro_thread = null;
			if (ssaChoices.contains("Jaro-Winkler")) {
				jaro_thread = new Thread("Jaro-Winkler") {
					public void run() {
						System.out.println("Jaro-Winkler");
						match_to_model_Jaro_Winkler(sampleChains, wkbkResults);	
					}				
				};
				jaro_thread.start();
				System.out.println("Running thread: " + jaro_thread.getName());				
			}
			
			/* this is equivalent to matching on string length for line segment properties
			 * just that this is the long border chain code with fine resolution
			 * string similarity match the chain codes in earlier experimental
			 * runs, so let's do that here and add that into final equation match */
			Thread lcs_thread = null;
			if (ssaChoices.contains("Longest-Common-Subsequence")) {
				lcs_thread = new Thread("Longest-Common-SubSequence") {
					public void run() {
						System.out.println("Longest-Common-SubSequence");
						match_to_model_LCS(sampleChains, wkbkResults);	
					}				
				};
				lcs_thread.start();
				System.out.println("Running thread: " + lcs_thread.getName());					
			}
			
			Thread mlcs_thread = null;
			if (ssaChoices.contains("Metric Longest-Common-SubSequence")) {
				mlcs_thread = new Thread("Metric Longest-Common-SubSequence") {
					public void run() {
						System.out.println("Metric Longest-Common-SubSequence");
						match_to_model_MLCS(sampleChains, wkbkResults);	
					}				
				};
				mlcs_thread.start();
				System.out.println("Running thread: " + mlcs_thread.getName());				
			}			
			
			Thread ngram_thread = null;
			if (ssaChoices.contains("NGram Distance")) {
				ngram_thread = new Thread("NGram Distance") {
					public void run() {
						System.out.println("NGram Distance");
						match_to_model_NGram_Distance(sampleChains, wkbkResults);
					}				
				};
				ngram_thread.start();
				System.out.println("Running thread: " + ngram_thread.getName());					
			}
			
			Thread qgram_thread = null;
			if (ssaChoices.contains("QGram (Ukkonen) Distance")) {
				qgram_thread = new Thread("QGram (Ukkonen) Distance") {
					public void run() {
						System.out.println("QGram (Ukkonen) Distance");
						match_to_model_QGram_Distance(sampleChains, wkbkResults);
					}				
				};
				qgram_thread.start();
				System.out.println("Running thread: " + qgram_thread.getName());				
			}
				
			Thread cosSim_thread = null;
			if (ssaChoices.contains("Cosine Similarity")) {
				cosSim_thread = new Thread("Cosine Similarity") {
					public void run() {
						System.out.println("Cosine Similarity");
						match_to_model_COS_Similarity(sampleChains, wkbkResults);
					}				
				};
				cosSim_thread.start();
				System.out.println("Running thread: " + cosSim_thread.getName());				
			}

			/* Ancillary match by moments */
			Thread moments_thread = new Thread("Moments Similarity") {
				public void run() {
					System.out.println("Moments Similarity");
					match_to_model_by_Moments(sampleMoments, wkbkResults);
				}				
			};
			moments_thread.start();
			System.out.println("Running thread: " + moments_thread.getName());	
			
			/* Ancillary match by chain code start location */
			Thread cc_segstart_thread = new Thread("CC Segment Start Location") {
				public void run() {
					System.out.println("CC Segment Start Location");
					/* TODO: add matching method */
					String matching_image_ccSegment = 
							match_to_model_by_CC_Segment_Start(sampleccStartPts, wkbkResults);
				}
			};
			cc_segstart_thread.start();
			System.out.println("Running thread: " + cc_segstart_thread.getName());
			
			try {
				if (levenshtein_thread != null) {
					levenshtein_thread.join();	
				}
				
				if (n_levenshtein_thread != null) {
					n_levenshtein_thread.join();	
				}
				
				if (damerau_levenshtein_thread != null) {
					damerau_levenshtein_thread.join();	
				}
				
				if (ost_thread != null) {
					ost_thread.join();	
				}
				
				if (jaro_thread != null) {
					jaro_thread.join();	
				}
				
				if (lcs_thread != null) {
					lcs_thread.join();	
				}
				
				if (ngram_thread != null) {
					ngram_thread.join();	
				}
				
				if (qgram_thread != null) {
					qgram_thread.join();	
				}
				
				if (cosSim_thread != null) {
					cosSim_thread.join();	
				}
				
				if (mlcs_thread != null) {
					mlcs_thread.join();	
				}
				
				if (moments_thread != null) {
					moments_thread.join();	
				}				
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
			/* Write results spreadsheet to disk */
			FileOutputStream resultFile;
			try {
				resultFile = new FileOutputStream( "output/match_of_" + 
						filename.substring(filename.lastIndexOf('/')+1, 
					       filename.lastIndexOf('.'))+ 
					       	"_" + System.currentTimeMillis() + ".xlsx");
				synchronized(wkbkResults) {
					wkbkResults.write(resultFile);
					wkbkResults.close();					
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		// finalize ids for image
		cm.setStartingId(DatabaseModule.getStartId(filename));
		cm.setLastId(DatabaseModule.getLastId(filename));
		System.out.println(" IDs for image " + cm.getFilename() + " will be" + 
		cm.getStartingId() + " to " + cm.getLastId());
		
		// return to caller
		return global_graph;
	}		
	
	/**
	 * Build the summary sheet page in the spreadsheet
	 * This routine only builds the measure labels and the model filenames
	 * You will need to fill in the values for each model for each type of 
	 * matching measure 
	 * @param wkbkResults -- the spreadsheet to work with
	 */
	private static void buildSummarySheet(XSSFWorkbook wkbkResults) {
		XSSFSheet sheet = wkbkResults.createSheet("Summary");
		XSSFSheet weightSheet = wkbkResults.createSheet("Weights");
		List<String> modelFilenames = DatabaseModule.getAllModelFileName();
		XSSFRow row = sheet.createRow(0);
		XSSFCell cell = row.createCell(0, CellType.STRING);
		cell.setCellValue("Model Filename");
		cell = row.createCell(1, CellType.STRING);
		cell.setCellValue("Si");
		cell = row.createCell(2, CellType.STRING);
		cell.setCellValue("Ci");
		cell = row.createCell(3, CellType.STRING);
		cell.setCellValue("CSi");
		cell = row.createCell(4, CellType.STRING);
		cell.setCellValue("LCSi");
		int i = 1;
		for(String model : modelFilenames) {
			row = sheet.createRow(i++);
			cell = row.createCell(0, CellType.STRING);
			cell.setCellValue(model);
		}		
		
		// Build weights reference sheet -- weights should add to one
		row = weightSheet.createRow(0);
		cell = row.createCell(0, CellType.STRING);
		cell.setCellValue("alpha");
		cell = row.createCell(1, CellType.NUMERIC);
		cell.setCellValue(0.70);
		row = weightSheet.createRow(1);
		cell = row.createCell(0, CellType.STRING);
		cell.setCellValue("beta");
		cell = row.createCell(1, CellType.NUMERIC);
		cell.setCellValue(0.15);
		row = weightSheet.createRow(2);
		cell = row.createCell(0, CellType.STRING);
		cell.setCellValue("delta");
		cell = row.createCell(1, CellType.NUMERIC);
		cell.setCellValue(0.05);
		row = weightSheet.createRow(3);
		cell = row.createCell(0, CellType.STRING);
		cell.setCellValue("eplison");
		cell = row.createCell(1, CellType.NUMERIC);
		cell.setCellValue(0.10);
	}
	
	private static String match_to_model_by_CC_Segment_Start(ArrayList<Point> sampleccStartPts, 
															 XSSFWorkbook wkbkResults) {
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("CCStartMeasure");	
		}
		
		Map<String, Integer> modelFileCnts = new TreeMap<String, Integer>();
		
		/* for one chaincode starting segment in the sample image, find
		 * one matching model images
		 */
		for(Point ccStart : sampleccStartPts) {
			List<PointMatchContainer> pmcList = DatabaseModule.getImagesMatchingCCStart(ccStart);
			
			// if there any matches, count the number of times each model image was matched
			if (pmcList == null) {
				continue;
			}
			for(PointMatchContainer pmc : pmcList) {
				String filename = pmc.getMatch();
				if (modelFileCnts.containsKey(filename)) {
					int cnt = modelFileCnts.get(filename);
					modelFileCnts.put(filename, ++cnt);
				}
				else {
					modelFileCnts.put(pmc.getMatch(),1);	
				}
				
			}
		}
		
		int rowNumber = 1;
		// build header 
		synchronized(wkbkResults) {
			XSSFRow row = sheet.createRow(rowNumber);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue("Model Image");
			cell = row.createCell(1);
			cell.setCellValue("Count");
			cell = row.createCell(2);
			cell.setCellValue("Match Prob.");
		}
		
		// find the key with the largest count since ordering is on keys
		Set<String> keys = modelFileCnts.keySet();
		String modelFilewithLargestCnt = "";
		int largestCnt = Integer.MIN_VALUE;
		for(String key : keys) {
		   int fileCnt = modelFileCnts.get(key);
		   synchronized(wkbkResults) {
			   XSSFRow row = sheet.createRow(++rowNumber);   
			   XSSFCell cell = row.createCell(0);
			   cell.setCellValue(key);
			   cell = row.createCell(1);
			   cell.setCellValue(fileCnt);
			   cell = row.createCell(2);
			   cell.setCellValue(((float)fileCnt)/sampleccStartPts.size());
		   }		   
		   System.out.println("Image " + key+ " has " 
				   + fileCnt + " matching starting points");
		   //sheet.createRow(arg0)
		   if (fileCnt > largestCnt) {
			   largestCnt = fileCnt;
			   modelFilewithLargestCnt = key;
		   }		   
		}

		// report and return the best model image for this type of match
		System.out.println("File with largest CC starting point count is " + 
				 modelFilewithLargestCnt 
				+ " with a count of " + largestCnt);
		return modelFilewithLargestCnt;
	}
	
	private static void match_to_model_by_Moments(Map<Integer, Point> sampleMoments, 
			                                      XSSFWorkbook wkbkResults) {
		// TODO Auto-generated method stub
		/* Simple Method:
		 * For each segment in Sample
		 *     Ask database for number of ids that match x and y moments
		 *     
		 *  Announce best match
		 *  
		 *   A More sophisticated method needs to look at partial regions
		 *   how close is close enough for a likely match or probable
		 *   match...*/
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleMoments == null) || sampleMoments.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleMoments.size();
			cntMatchesSz = (int)(sampleMoments.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
		
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("Moments");
		}		
		
		Map<Integer, HashMap<Integer,Double>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Double>>(
						sampleMoments.size(),(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleMoments.keySet().iterator();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			Point segmentMoment = sampleMoments.get(segment);
			sb.append("Working with sample segment Point " + 
			   segment +  " with coordinates (" + (int)segmentMoment.x + "," 
			   + (int)segmentMoment.y + ")" + "\n");
			ArrayList<String> names = DatabaseModule.getFilesWithMoment(
					(int)segmentMoment.x, (int)segmentMoment.y);
			sb.append("Returned " + names.size() + " model image(s)" + "\n");
			for(String name: names) {
				Integer cnt = cntMatches.get(name);
				if (cnt == null) {
					cntMatches.put(name, 1);					
				}
				else {
					cntMatches.put(name, ++cnt);
				}
			}
		}
		String bestMatch = null;
		Integer bestMatchCnt = Integer.MIN_VALUE;
		Set<String> models = cntMatches.keySet();
		int rowNumber = 1;
		
		// build header 
		synchronized(wkbkResults) {
			XSSFRow row = sheet.createRow(rowNumber);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue("Model Image");
			cell = row.createCell(1);
			cell.setCellValue("# Matching Moments");
			cell = row.createCell(2);
			cell.setCellValue("Match Prob.");
			cell = row.createCell(3);
			cell.setCellValue("Match Prob. Per.");
		}
		
		for (String model : models) {
			Integer cnt = cntMatches.get(model);
			
	    	/* record data in spreadsheet */
			synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(++rowNumber);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(model);
		    	cell = row.createCell(1, CellType.NUMERIC);
		    	cell.setCellValue(cnt);
		    	cell = row.createCell(2,CellType.NUMERIC);
		    	cell.setCellValue(((double)cnt) / sampleMoments.size());	
		    	cell = row.createCell(3,CellType.NUMERIC);
		    	cell.setCellValue((((double)cnt) / sampleMoments.size())*100);
		    	
		    	// update summary sheet as well for final calculation
		    	XSSFSheet summarySheet = wkbkResults.getSheet("Summary");
		    	int sumRowInt = 
		    			ProjectUtilities.findRowInSpreadSheet(summarySheet, model);		    			    	
		    	XSSFRow summaryRow = sheet.getRow(sumRowInt);
		    	XSSFCell summaryCell = summaryRow.createCell(2, CellType.NUMERIC);
		    	summaryCell.setCellValue(((double)cnt) / sampleMoments.size());
			}
			
			if (cnt > bestMatchCnt) {
				bestMatchCnt = cnt;
				bestMatch = model;
			}
		}
		double percentageMatch = ((double)bestMatchCnt) / sampleMoments.size();
		
	    /* Make sure the best results stands out from the other data */
		synchronized(wkbkResults) {
			XSSFRow bestRow = sheet.createRow(rowNumber);
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(bestMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1, CellType.NUMERIC);
		    bestCellinRow.setCellValue(percentageMatch);	
		    bestCellinRow.setCellStyle(style);			
		    bestCellinRow = bestRow.createCell(2, CellType.NUMERIC);
		    bestCellinRow.setCellValue(percentageMatch*100);	
		    bestCellinRow.setCellStyle(style);		
		}
		
		sb.append("Best match using contours (moments) is " + 
		                   bestMatch + " with " + bestMatchCnt +
		                   " contours matching and " + (percentageMatch * 100) 
		                   + "% level of confidence" + "\n");
		System.out.println(sb.toString());
		System.out.println("Done running thread");
	}
	
	private static void match_to_model_COS_Similarity(
			Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		/* The closer to zero, the more similar the two strings as the 
		 * two angles have a large cosine value */
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segment in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("COS_SIM");
		}
		
		Map<Integer, HashMap<Integer,Double>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Double>>(
						sampleChains.size(),(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			double bestDisSoFar = Double.MAX_VALUE;
			int minID = -1;
			for(int i = 0; i < lastEntryID; i++) {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				/* 1 - similarity where similarity is: /**
				 * the cosine of the angle between
				 * these two vectors representation. 
				 * It is computed as V1 . V2 / (|V1| * |V2|)
				 **/
				Cosine c = new Cosine(5);
				double distance = c.distance(segmentChain, modelSegmentChain);
				
				/* We want measures as close to zero as possible*/
				if (distance < bestDisSoFar) {
					bestDisSoFar = distance;
					minID = i;
				}
			}
			HashMap<Integer, Double> hm = 
					new HashMap<Integer, Double>(1, (float) 0.75);
			hm.put(minID, bestDisSoFar);
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID);
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Double> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best COS_SIM Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) + " measure" + "\n");	
	    	}	    	
	    }	
	    
	    /* Tell user probability of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    double bestProbMatch = Double.MIN_VALUE;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	double probMatch = ((double)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) + " %"
	    			            + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1);
		    	cell.setCellValue(probMatch);	    		
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch
	    		           + "\n");
	    
	    synchronized(wkbkResults) {
	    	XSSFRow bestRow = sheet.createRow(probsCnt);
	 	   
		    /* Make sure the best results stands out from the other data */
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);	    	
	    }
	    
	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	private static void match_to_model_QGram_Distance(
			Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segmnent in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
		
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("QGram");
		}
		
		Map<Integer, HashMap<Integer,Integer>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Integer>>(
						sampleChains.size(),(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			AtomicInteger minDistance = new AtomicInteger(Integer.MAX_VALUE);
			AtomicInteger minID = new AtomicInteger(-1);
			IntStream.range(0, lastEntryID).forEach((i) -> {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				QGram qg = new QGram(5);
				if ((segmentChain == null) || (modelSegmentChain == null)) {
					return;
				}
				int distance = (int) qg.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (distance < minDistance.get()) {
					minDistance.set(distance);
					minID.set(i);
				}				
				
			});

			HashMap<Integer, Integer> hm = 
					new HashMap<Integer, Integer>(1, (float) 0.75);
			hm.put(minID.get(), minDistance.get());
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID.get());
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
			
			// try to prevent OutofMemoryError w/ concurrent use of methods
			System.gc();
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Integer> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best QGram Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) 
	    		                    + " mods needed to match" + "\n");	
	    	}	    	
	    }		
	    	   
	    // build header
	    synchronized(wkbkResults) {
			XSSFRow row = sheet.createRow(0);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue("Model Image");
			cell = row.createCell(1);
			cell.setCellValue("Match Prob.");
			cell = row.createCell(2);
			cell.setCellValue("Match Prob. Per.");			
		}	    
	    
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) 
	    			            + " %" + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1, CellType.NUMERIC);
		    	cell.setCellValue(probMatch);	
		    	cell = row.createCell(2,  CellType.NUMERIC);
		    	cell.setCellValue(probMatch*100);
		    	
		    	// update summary sheet as well for final calculation
		    	XSSFSheet summarySheet = wkbkResults.getSheet("Summary");
		    	int sumRowInt = 
		    			ProjectUtilities.findRowInSpreadSheet(summarySheet, filename);		    			    	
		    	XSSFRow summaryRow = sheet.getRow(sumRowInt);
		    	XSSFCell summaryCell = summaryRow.createCell(1, CellType.NUMERIC);
		    	summaryCell.setCellValue(probMatch);
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch
	    		           + "\n");
	    synchronized(wkbkResults) {
		    XSSFRow bestRow = sheet.createRow(probsCnt);
			   
		    /* Make sure the best results stands out from the other data */
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1, CellType.NUMERIC);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);	
		    bestCellinRow = bestRow.createCell(2, CellType.NUMERIC);
		    bestCellinRow.setCellValue(bestProbMatch*100);	
		    bestCellinRow.setCellStyle(style);		 
	    }  
		
	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	private static void match_to_model_NGram_Distance(
			Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segmnent in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}

		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("NGram");
		}
		
		Map<Integer, HashMap<Integer,Integer>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Integer>>(
						sampleChains.size(),(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			int minDistance = Integer.MAX_VALUE;
			int minID = -1;
			for(int i = 0; i < lastEntryID; i++) {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				/* Convert strings into sets of n-grams */
				NGram ng = new NGram(5);
				int distance = (int) ng.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (distance < minDistance) {
					minDistance = distance;
					minID = i;
				}
			}
			HashMap<Integer, Integer> hm = 
					new HashMap<Integer, Integer>(1, (float) 0.75);
			hm.put(minID, minDistance);
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID);
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Integer> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best NGram Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) + " mods needed to match"
	    				            + "\n");	
	    	}	    	
	    }		
	    
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) 
	    			            + " %" + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1);
		    	cell.setCellValue(probMatch);	    		
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch
	    		           + "\n");
	    synchronized(wkbkResults) {
		    XSSFRow bestRow = sheet.createRow(probsCnt);
			   
		    /* Make sure the best results stands out from the other data */
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);	   	    	
	    }

	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	private static void match_to_model_MLCS(Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		// TODO Auto-generated method stub
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segmnent in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}

		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("MLCS");
		}
		
		Map<Integer, HashMap<Integer,Double>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Double>>(
						bestMatchesSz, (float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90);  
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			double minDistance = Double.MAX_VALUE;
			int minID = -1;
			for(int i = 0; i < lastEntryID; i++) {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				/* */
				MetricLCS mlcs = new MetricLCS();
				double distance = mlcs.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (distance < minDistance) {
					minDistance = distance;
					minID = i;
				}
			}
			HashMap<Integer, Double> hm = 
					new HashMap<Integer, Double>(1, (float) 0.75);
			hm.put(minID, minDistance);
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID);
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Double> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best M.L.C.S Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) + " measure"
	    				            + "\n");	
	    	}	    	
	    }		
	    
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) 
	    			            + " %" + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1);
		    	cell.setCellValue(probMatch);	    		
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch + "\n");
	    XSSFRow bestRow = sheet.createRow(probsCnt);
	   
	    /* Make sure the best results stands out from the other data */
	    synchronized(wkbkResults) {
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);		    	
	    }
    
	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	private static String match_to_model_LCS(Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		// TODO Auto-generated method stub
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segmnent in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return null;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}

		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("LCS");
		}
		
		Map<Integer, HashMap<Integer,Integer>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Integer>>(
						bestMatchesSz,(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("LCS(): Working with sample segment " + segment + "\n");
			AtomicInteger minDistance = new AtomicInteger(Integer.MAX_VALUE);
			AtomicInteger minID = new AtomicInteger(-1);
			
			IntStream.range(0, lastEntryID).parallel().forEach((i) -> {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);	
				
				/* Levenshtein measure is
				 * the minimum number of single-character edits 
				 * (insertions, deletions or substitutions) required to 
				 *  change one word into the other */
				LongestCommonSubsequence lcs = new LongestCommonSubsequence();
				int distance = (int) lcs.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (distance < minDistance.get()) {
					minDistance.set(distance);
					minID.set(i);
				}
				
			});						
			
			/* Keep track of the best match for the current segment */
			HashMap<Integer, Integer> hm = 
					new HashMap<Integer, Integer>(1, (float) 0.75);
			hm.put(minID.get(), minDistance.get());
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID.get());
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
			
			// hint to vm to force garage collection, maybe
			// prevent OutOfMemory error 
			System.gc();
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Integer> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best L.C.S Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) + " mods needed to match"
	    				            + "\n");	
	    	}	    	
	    }
	    
	    // build header
	    synchronized(wkbkResults) {
			XSSFRow row = sheet.createRow(0);
			XSSFCell cell = row.createCell(0);
			cell.setCellValue("Model Image");
			cell = row.createCell(1);
			cell.setCellValue("Match Prob.");
			cell = row.createCell(2);
			cell.setCellValue("Match Prob. Per.");			
		}
	    
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 1;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) 
	    			            + " %" + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1, CellType.NUMERIC);
		    	cell.setCellValue(probMatch);
		    	cell = row.createCell(2, CellType.NUMERIC);
		    	cell.setCellValue(probMatch*100);
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch
	    		           + "\n");
	    synchronized(wkbkResults) {
		    XSSFRow bestRow = sheet.createRow(probsCnt);
			   
		    /* Make sure the best results stands out from the other data */
		    
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1, CellType.NUMERIC);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);	
		    bestCellinRow = bestRow.createCell(2, CellType.NUMERIC);
		    bestCellinRow.setCellValue(bestProbMatch*100);	
		    bestCellinRow.setCellStyle(style);	    	
	    }
	    
	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	    return nameOfModelMatch + "," + bestProbMatch;
	}
	
	private static void match_to_model_Jaro_Winkler(Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
			/* 1. Take each segment of sample image 
			 *    for each model image
			 *        for each segmnent in model image 
			 *            apply java-string-similarity method
			 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
			Map<Integer, HashMap<Integer,Double>> bestMatches = 
					new HashMap<Integer, HashMap<Integer,Double>>(
							bestMatchesSz,(float)0.75);
			Map<String, Integer> cntMatches = 
					new HashMap<String, Integer>(cntMatchesSz, 
							(float)0.90); 
			
			XSSFSheet sheet = null;
			synchronized(wkbkResults) {
				sheet = wkbkResults.createSheet("JaroWinkler");
			}
			
			Iterator<Integer> segments = sampleChains.keySet().iterator();
			int lastEntryID = DatabaseModule.getLastId();
			while(segments.hasNext()) {
				Integer segment = segments.next();
				String segmentChain = sampleChains.get(segment);
				sb.append("Working with sample segment " + segment + "\n");
				Double bestLvlOfMatch = Double.MIN_VALUE;
				int bestID = -1;
				for(int i = 0; i < lastEntryID; i++) {
					/* Get the ith chain code from the database */
					String modelSegmentChain = DatabaseModule.getChainCode(i);
					
					/* computes the similarity between 2 strings, and the returned value 
					 * lies in the interval [0.0, 1.0]. It is (roughly) a variation of 
					 * Damerau-Levenshtein, where the substitution of 2 close 
					 * characters is considered less important then the substitution of 
					 * 2 characters that a far from each other.*/
					JaroWinkler jw = new JaroWinkler();
					double similarity = 1 - jw.distance(segmentChain, modelSegmentChain);
					
					/* track entry with the small number of  
					 * edits then report filename and segment of id entry */
					if (similarity > bestLvlOfMatch) {
						bestLvlOfMatch = similarity;
						bestID = i;
					}
				}
				HashMap<Integer, Double> hm = 
						new HashMap<Integer, Double>(1, (float) 0.75);
				hm.put(bestID, bestLvlOfMatch);
				bestMatches.put(segment, hm);
				
				/* For each segment of the sample, track which model image 
				 * and which image model perspective provides the best match*/
				String modelOfInterest = DatabaseModule.getFileName(bestID);
				Integer curCnt = cntMatches.get(modelOfInterest);			
				if (curCnt == null) {
					cntMatches.put(modelOfInterest, 1);	
				}
				else {
					cntMatches.put(modelOfInterest, ++curCnt);
				}
			}
			
			/* Display result */
		    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
		    while (bmIterator.hasNext()) {
		    	Integer key = bmIterator.next();
		    	HashMap <Integer, Double> minValue = bestMatches.get(key);
		    	Iterator<Integer> ii = minValue.keySet().iterator();
		    	while(ii.hasNext()) {
		    		Integer idmin = ii.next();
		    		String filenameOfID = DatabaseModule.getFileName(idmin);
		    		sb.append("Best Jaro Winker match for segment " + key + " is " + 
		    		                    idmin + " (" + filenameOfID +") with " + 
		    				            minValue.get(idmin) + " similarity"
		    				            + "\n");	
		    	}	    	
		    }		
		    
		    /* Tell user probably of matching various images based on how well 
		     * sample segments matched to the database of model images */
		    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
		    float bestProbMatch = Float.MIN_NORMAL;
		    String nameOfModelMatch = null;
		    int probsCnt = 0;
		    while (cntIterator.hasNext()) {
		    	String filename = cntIterator.next();
		    	Integer count = cntMatches.get(filename);
		    	float probMatch = ((float)count) / sampleChains.size();
		    	sb.append("Probablity of matching " + filename 
		    			            + " is :" + (probMatch * 100) 
		    			            + " %" + "\n");
		    	
		    	/* record data in spreadsheet */
		    	synchronized(wkbkResults) {
			    	XSSFRow row = sheet.createRow(probsCnt++);
			    	XSSFCell cell = row.createCell(0);
			    	cell.setCellValue(filename);
			    	cell = row.createCell(1);
			    	cell.setCellValue(probMatch);		    		
		    	}
		    	
		    	/* Track most likely match*/
		    	if (probMatch > bestProbMatch) {
		    		bestProbMatch = probMatch;
		    		nameOfModelMatch = filename;
		    	}
		    }
		    
		    /* Tell user most likely match and record in spreadsheet */
		    sb.append("Best probable match is " + nameOfModelMatch + 
		    		           " with probablity " + bestProbMatch
		    		           + "\n");
		    synchronized(wkbkResults) {
			    XSSFRow bestRow = sheet.createRow(probsCnt);
				   
			    /* Make sure the best results stands out from the other data */
			    XSSFCellStyle style = wkbkResults.createCellStyle();
			    XSSFFont font = wkbkResults.createFont();
			    style.setBorderBottom(BorderStyle.THICK);
			    style.setBorderTop(BorderStyle.THICK);
			    font.setFontHeightInPoints((short) 14);
			    font.setBold(true);
			    style.setFont(font);
			    bestRow.setRowStyle(style);
			    
			    /* Record data in row of spreadsheet */
			    XSSFCell bestCellinRow = bestRow.createCell(0);
			    bestCellinRow.setCellValue(nameOfModelMatch);
			    bestCellinRow.setCellStyle(style);
			    bestCellinRow = bestRow.createCell(1);
			    bestCellinRow.setCellValue(bestProbMatch);
			    bestCellinRow.setCellStyle(style);		    	
		    }
		    
		    System.out.println(sb.toString());
		    System.out.println("Done running thread");
	}
	
	private static void match_to_model_Opt_Str_Alignment(Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
			// TODO Auto-generated method stub
			/* 1. Take each segment of sample image 
			 *    for each model image
			 *        for each segment in model image 
			 *            apply java-string-similarity method
			 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
		
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("OSA");
		}
		
			Map<Integer, HashMap<Integer,Integer>> bestMatches = 
					new HashMap<Integer, HashMap<Integer,Integer>>(
							bestMatchesSz,(float)0.75);
			Map<String, Integer> cntMatches = 
					new HashMap<String, Integer>(cntMatchesSz, 
							(float)0.90); 
			
			Iterator<Integer> segments = sampleChains.keySet().iterator();
			int lastEntryID = DatabaseModule.getLastId();
			while(segments.hasNext()) {
				Integer segment = segments.next();
				String segmentChain = sampleChains.get(segment);
				sb.append("Working with sample segment " + segment + "\n");
				int minDistance = Integer.MAX_VALUE;
				int minID = -1;
				for(int i = 0; i < lastEntryID; i++) {
					/* Get the ith chain code from the database */
					String modelSegmentChain = DatabaseModule.getChainCode(i);
					
					/* the number of edit operations needed to make the strings
					 *  equal under the condition that no substring is edited 
					 *  more than once*/
					OptimalStringAlignment osa = new OptimalStringAlignment();
					int distance = (int) osa.distance(
							segmentChain, modelSegmentChain);
					
					/* track entry with the small number of  
					 * edits then report filename and segment of id entry */
					if (distance < minDistance) {
						minDistance = distance;
						minID = i;
					}
				}
				HashMap<Integer, Integer> hm = 
						new HashMap<Integer, Integer>(1, (float) 0.75);
				hm.put(minID, minDistance);
				bestMatches.put(segment, hm);
				
				/* For each segment of the sample, track which model image 
				 * and which image model perspective provides the best match*/
				String modelOfInterest = DatabaseModule.getFileName(minID);
				Integer curCnt = cntMatches.get(modelOfInterest);			
				if (curCnt == null) {
					cntMatches.put(modelOfInterest, 1);	
				}
				else {
					cntMatches.put(modelOfInterest, ++curCnt);
				}
			}
			
			/* Display result */
		    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
		    while (bmIterator.hasNext()) {
		    	Integer key = bmIterator.next();
		    	HashMap <Integer,Integer> minValue = bestMatches.get(key);
		    	Iterator<Integer> ii = minValue.keySet().iterator();
		    	while(ii.hasNext()) {
		    		Integer idmin = ii.next();
		    		String filenameOfID = DatabaseModule.getFileName(idmin);
		    		sb.append("Best O.S.A. for segment " + key + " is " + 
		    		                    idmin + " (" + filenameOfID +") with " + 
		    				            minValue.get(idmin) 
		    		                    + " mods needed to match" + "\n");	
		    	}	    	
		    }		
		
		    /* Tell user probably of matching various images based on how well 
		     * sample segments matched to the database of model images */
		    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
		    float bestProbMatch = Float.MIN_NORMAL;
		    String nameOfModelMatch = null;
		    int probsCnt = 0;
		    while (cntIterator.hasNext()) {
		    	String filename = cntIterator.next();
		    	Integer count = cntMatches.get(filename);
		    	float probMatch = ((float)count) / sampleChains.size();
		    	sb.append("Probablity of matching " + filename 
		    			            + " is :" + (probMatch * 100) + " %"
		    			            + "\n");
		    	
		    	/* record data in spreadsheet */
		    	synchronized(wkbkResults) {
			    	XSSFRow row = sheet.createRow(probsCnt++);
			    	XSSFCell cell = row.createCell(0);
			    	cell.setCellValue(filename);
			    	cell = row.createCell(1);
			    	cell.setCellValue(probMatch);		    		
		    	}
		    	
		    	/* Track most likely match*/
		    	if (probMatch > bestProbMatch) {
		    		bestProbMatch = probMatch;
		    		nameOfModelMatch = filename;
		    	}
		    }
		    
		    /* Tell user most likely match and record in spreadsheet */
		    sb.append("Best probable match is " + nameOfModelMatch + 
		    		           " with probablity " + bestProbMatch
		    		           + "\n");
		    synchronized(wkbkResults) {
			    XSSFRow bestRow = sheet.createRow(probsCnt);
				   
			    /* Make sure the best results stands out from the other data */
			    XSSFCellStyle style = wkbkResults.createCellStyle();
			    XSSFFont font = wkbkResults.createFont();
			    style.setBorderBottom(BorderStyle.THICK);
			    style.setBorderTop(BorderStyle.THICK);
			    font.setFontHeightInPoints((short) 14);
			    font.setBold(true);
			    style.setFont(font);
			    bestRow.setRowStyle(style);
			    
			    /* Record data in row of spreadsheet */
			    XSSFCell bestCellinRow = bestRow.createCell(0);
			    bestCellinRow.setCellValue(nameOfModelMatch);
			    bestCellinRow.setCellStyle(style);
			    bestCellinRow = bestRow.createCell(1);
			    bestCellinRow.setCellValue(bestProbMatch);	
			    bestCellinRow.setCellStyle(style);		    	
		    }

		    System.out.println(sb.toString());
		    System.out.println("Done running thread");
	}
	
	private static void match_to_model_Damerau_Levenshtein(
			Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		// TODO Auto-generated method stub
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segment in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}

		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("Damerau");
		}
		
		Map<Integer, HashMap<Integer,Integer>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Integer>>(
						sampleChains.size(),(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			int minDistance = Integer.MAX_VALUE;
			int minID = -1;
			for(int i = 0; i < lastEntryID; i++) {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				/* Levenshtein measure is
				 * the minimum number of single-character edits 
				 * (insertions, deletions or substitutions) required to 
				 *  change one word into the other */
				Damerau d = new Damerau();
				int distance = (int) d.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (distance < minDistance) {
					minDistance = distance;
					minID = i;
				}
			}
			HashMap<Integer, Integer> hm = 
					new HashMap<Integer, Integer>(1, (float) 0.75);
			hm.put(minID, minDistance);
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID);
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Integer> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best D-L Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) + 
	    				            " mods needed to match" + "\n");	
	    	}	    	
	    }	
	    
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) + " %"
	    			            + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1);
		    	cell.setCellValue(probMatch);	    		
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch
	    		           + "\n");
	    synchronized(wkbkResults) {
		    XSSFRow bestRow = sheet.createRow(probsCnt);
			   
		    /* Make sure the best results stands out from the other data */
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);	    	
	    }

	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	private static void match_to_model_Normalized_Levenshtein(
			Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		// TODO Auto-generated method stub
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segmnent in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
		
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("NLevenshtein");
		}
		
		Map<Integer, HashMap<Integer,Double>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Double>>(
						bestMatchesSz,(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			Double bestLvlOfMatch = Double.MIN_VALUE;
			int bestID = -1;
			for(int i = 0; i < lastEntryID; i++) {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				/* Levenshtein measure is
				 * the minimum number of single-character edits 
				 * (insertions, deletions or substitutions) required to 
				 *  change one word into the other */
				NormalizedLevenshtein nl = new NormalizedLevenshtein();
				double similarity = 1 - nl.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (similarity > bestLvlOfMatch) {
					bestLvlOfMatch = similarity;
					bestID = i;
				}
				
				/* For each segment of the sample, track which model image 
				 * and which image model perspective provides the best match*/
				String modelOfInterest = DatabaseModule.getFileName(bestID);
				Integer curCnt = cntMatches.get(modelOfInterest);			
				if (curCnt == null) {
					cntMatches.put(modelOfInterest, 1);	
				}
				else {
					cntMatches.put(modelOfInterest, ++curCnt);
				}
				
			}
			HashMap<Integer, Double> hm = 
					new HashMap<Integer, Double>(1, (float) 0.75);
			hm.put(bestID, bestLvlOfMatch);
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(bestID);
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer, Double> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best L.Norm Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) + " similarity"
	    				            + "\n");	
	    	}	    	
	    }
		
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) + " %"
	    			            + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1);
		    	cell.setCellValue(probMatch);	    		
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match is " + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch +
	    		           "\n");
	    synchronized(wkbkResults) {
		    XSSFRow bestRow = sheet.createRow(probsCnt);
			   
		    /* Make sure the best results stands out from the other data */
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1);
		    bestCellinRow.setCellValue(bestProbMatch);
		    bestCellinRow.setCellStyle(style);	
	    }
	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	private static void match_to_model_Levenshtein(
			Map<Integer, String> sampleChains, XSSFWorkbook wkbkResults) {
		// TODO Auto-generated method stub
		/* 1. Take each segment of sample image 
		 *    for each model image
		 *        for each segment in model image 
		 *            apply java-string-similarity method
		 *            O(n)+O(m*n^2)+Runtime_Algorithm */
		StringBuilder sb = new StringBuilder();
		int bestMatchesSz = 1;
		int cntMatchesSz = 1;
		if ((sampleChains == null) || sampleChains.size() == 0) {
			return;
		}
		else {
			bestMatchesSz = sampleChains.size();
			cntMatchesSz = (int)(sampleChains.size() * .1);
			if (cntMatchesSz < 1 ) {
				cntMatchesSz = 1;
			}
		}
		
		XSSFSheet sheet = null;
		synchronized(wkbkResults) {
			sheet = wkbkResults.createSheet("Levenshtein");
		}
		
		Map<Integer, HashMap<Integer,Integer>> bestMatches = 
				new HashMap<Integer, HashMap<Integer,Integer>>(
						bestMatchesSz,(float)0.75);
		Map<String, Integer> cntMatches = 
				new HashMap<String, Integer>(cntMatchesSz, 
						(float)0.90); 
		
		Iterator<Integer> segments = sampleChains.keySet().iterator();
		int lastEntryID = DatabaseModule.getLastId();
		sb.append("Last ID="+lastEntryID + "\n");
		while(segments.hasNext()) {
			Integer segment = segments.next();
			String segmentChain = sampleChains.get(segment);
			sb.append("Working with sample segment " + segment + "\n");
			int minDistance = Integer.MAX_VALUE;
			int minID = -1;
			for(int i = 0; i < lastEntryID; i++) {
				/* Get the ith chain code from the database */
				String modelSegmentChain = DatabaseModule.getChainCode(i);
				
				/* Levenshtein measure is
				 * the minimum number of single-character edits 
				 * (insertions, deletions or substitutions) required to 
				 *  change one word into the other */
				if ((segmentChain == null) || (modelSegmentChain == null)) {
					continue;
				}
				int distance = Levenshtein.distance(segmentChain, modelSegmentChain);
				
				/* track entry with the small number of  
				 * edits then report filename and segment of id entry */
				if (distance < minDistance) {
					minDistance = distance;
					minID = i;
					sb.append("New minDistance of " 
					+ minDistance + " for ID " + minID + "\n");
				}
			}
			/* Track which model segment provides the 
			 * fewest modifications to a match */
			HashMap<Integer, Integer> hm = 
					new HashMap<Integer, Integer>(1, (float) 0.75);
			hm.put(minID, minDistance);
			bestMatches.put(segment, hm);
			
			/* For each segment of the sample, track which model image 
			 * and which image model perspective provides the best match*/
			String modelOfInterest = DatabaseModule.getFileName(minID);
			Integer curCnt = cntMatches.get(modelOfInterest);			
			if (curCnt == null) {
				cntMatches.put(modelOfInterest, 1);	
			}
			else {
				cntMatches.put(modelOfInterest, ++curCnt);
			}
		}
		
		/* Display result */
	    Iterator<Integer> bmIterator = bestMatches.keySet().iterator();
	    while (bmIterator.hasNext()) {
	    	Integer key = bmIterator.next();
	    	HashMap <Integer,Integer> minValue = bestMatches.get(key);
	    	Iterator<Integer> ii = minValue.keySet().iterator();
	    	while(ii.hasNext()) {
	    		Integer idmin = ii.next();
	    		String filenameOfID = DatabaseModule.getFileName(idmin);
	    		sb.append("Best L. Match for segment " + key + " is " + 
	    		                    idmin + " (" + filenameOfID +") with " + 
	    				            minValue.get(idmin) 
	    		                    + " mods needed to match" + "\n");	
	    	}	    	
	    }
	    
	    /* Tell user probably of matching various images based on how well 
	     * sample segments matched to the database of model images */
	    Iterator<String> cntIterator = cntMatches.keySet().iterator(); 
	    float bestProbMatch = Float.MIN_NORMAL;
	    String nameOfModelMatch = null;
	    int probsCnt = 0;
	    while (cntIterator.hasNext()) {
	    	String filename = cntIterator.next();
	    	Integer count = cntMatches.get(filename);
	    	float probMatch = ((float)count) / sampleChains.size();
	    	sb.append("Probablity of matching " + filename 
	    			            + " is :" + (probMatch * 100) + " %"
	    			            + "\n");
	    	
	    	/* record data in spreadsheet */
	    	synchronized(wkbkResults) {
		    	XSSFRow row = sheet.createRow(probsCnt++);
		    	XSSFCell cell = row.createCell(0);
		    	cell.setCellValue(filename);
		    	cell = row.createCell(1);
		    	cell.setCellValue(probMatch);	    		
	    	}
	    	
	    	/* Track most likely match*/
	    	if (probMatch > bestProbMatch) {
	    		bestProbMatch = probMatch;
	    		nameOfModelMatch = filename;
	    	}
	    }
	    
	    /* Tell user most likely match and record in spreadsheet */
	    sb.append("Best probable match with Levenshetin is " 
	                       + nameOfModelMatch + 
	    		           " with probablity " + bestProbMatch + "\n");
	    synchronized(wkbkResults) {
		    XSSFRow bestRow = sheet.createRow(probsCnt);
			   
		    /* Make sure the best results stands out from the other data */
		    XSSFCellStyle style = wkbkResults.createCellStyle();
		    XSSFFont font = wkbkResults.createFont();
		    style.setBorderBottom(BorderStyle.THICK);
		    style.setBorderTop(BorderStyle.THICK);
		    font.setFontHeightInPoints((short) 14);
		    font.setBold(true);
		    style.setFont(font);
		    bestRow.setRowStyle(style);
		    
		    /* Record data in row of spreadsheet */
		    XSSFCell bestCellinRow = bestRow.createCell(0);
		    bestCellinRow.setCellValue(nameOfModelMatch);
		    bestCellinRow.setCellStyle(style);
		    bestCellinRow = bestRow.createCell(1);
		    bestCellinRow.setCellValue(bestProbMatch);	
		    bestCellinRow.setCellStyle(style);	    	
	    }
	    System.out.println(sb.toString());
	    System.out.println("Done running thread");
	}
	
	/**
	 * Calculate the angle thresholds
	 * @param start -- Fixed point from which all centroids are connected
	 * @param s -- list of centroids
	 * @return
	 */
	private static Mat calc_angle_differences(Point start, ArrayList<Point> s) {
		Mat thresholds = new Mat(s.size()-1, 4, CvType.CV_64FC1);
		for (int i = 0; i < s.size()-1; i++) {
			Point p1 = s.get(i);
			Point p2 = s.get(i+1);
			double theta1 = Math.atan2(p1.y - start.y, p1.x - start.x);
			double theta2 = Math.atan2(p2.y - start.y, p2.x - start.x);
			thresholds.put(i, 0, Math.toDegrees(theta1));
			thresholds.put(i, 1, Math.toDegrees(theta2));
		}
		return thresholds;
	}

	private static void determine_line_connectivity(ArrayList<CurveLineSegMetaData> lmd) {
		ArrayList<CurveLineSegMetaData> cList;
		
		for (CurveLineSegMetaData line : lmd) {
			cList = new ArrayList<CurveLineSegMetaData>();
			Point p1 = line.getSp();
			int lineCnt = 0;
			for (CurveLineSegMetaData line2 : lmd) {
				
				//skip the first line
				if (lineCnt == 0) {
					lineCnt++;
					continue;
				}
				else {
					lineCnt++;
				}
				
				Point p2 = line2.getSp();
				if (!p1.equals(p2)) {
					continue;
				}
				else {
					Point ep = line2.getEp();
					
					// don't connect a line that is really a point
					if (p2.equals(ep)) {
						continue;
					}
					cList.add(line2);
				}
			}
			line.setConnList(cList);
		}
	}

	/**
	 * Generate the shape description for a region<br/>
	 * <ul>
	 * <li> Line length is based on line distance formula from beg. 
	 * to end of a curved line segment </li>
	 * <li> Line orientation is based on the derivative of the line 
	 * and relative to the first sp of the first curved line segment </li>
	 * <li> Line curvature is the amount by which a line deviates from 
	 * being straight or how much of a curve it is </li>
	 * </ul>
	 * @param segx -- x entries from line segment generation
	 * @param segy -- y entries from line segment generation
	 */
	private static ArrayList<CurveLineSegMetaData> shape_expression(ArrayList<Mat> segx,
			ArrayList<Mat> segy) {
		int sz = segx.size();
		ArrayList<CurveLineSegMetaData> lmd = new ArrayList<CurveLineSegMetaData>(sz);
		long lineNumber = 0;		
		
		// Sanity check
		if ((segx.size() == 0) || (segy.size() == 0)) {
			System.out.println("WARNING: No segment data to generate " +  
		                       "line shape expression from for image " + 
					           "analysis");
			return null;
		}
		
		
		Mat segx1Mat = segx.get(0);
		Mat segy1Mat = segy.get(0);
		int startingElement = 0;
		Size szFirst = segx1Mat.size();
		while ((startingElement < sz-1) && 
				((szFirst.width == 0) || (szFirst.height == 0))) {
			startingElement++;
			segx1Mat = segx.get(startingElement);
			segy1Mat = segy.get(startingElement);			
		}
		if (startingElement >= sz-1) {
			return null;
		}
		double x1 = segx1Mat.get(0, 0)[0];
		double y1 = segy1Mat.get(0, 0)[0];
		double spX1 = segx1Mat.get(0, 0)[0];
		double spY1 = segy1Mat.get(0, 0)[0];
		double x1C = segx1Mat.get(0, 0)[0];
		double y1C = segy1Mat.get(0, 0)[0];
		// store basic line information including length
		for(int i = startingElement+1; i < sz; i++) {			
			long tic = System.nanoTime();
			Mat segx2Mat = segx.get(i);
			Mat segy2Mat = segy.get(i);

			double x2 = segx2Mat.get(0, 0)[0];
			double y2 = segy2Mat.get(0, 0)[0];
			
			// distance calculation in pixels
			double distance = Math.sqrt(
					Math.pow((x1 - x2),2) + 
					Math.pow((y1 - y2),2));
			
			// orientation calculation in degrees
			double dy = y2 - spY1;
			double dx = x2 - spX1;
			double orientation = Math.atan2(dy,dx);
			orientation = Math.toDegrees(orientation);
			if (orientation < 0) {
				orientation += 360;
			}
			
			/* calculate line curvature -- note that there is 
			  no curvature between two lines so what does 
			  Bourbakis's older work mean when they talk about
			  this -- over two line segments with the first one
			  zero? */
			double curvature = 0;
			if (i == 1) {
				curvature = 0;
			}
			else {
				double Cdx = x2 - x1C;
				double Cdy = y2 - y1C;
				curvature = Math.atan2(Cdy, Cdx) / Math.hypot(Cdy,  Cdx);
				
				/* Note for the entire region it might be 
				 * dx = gradient(seg_x);
				 * dy = graident(seg_y);
				 * curv = gradietn(atan2(dy,dx)/hypot(dx,dy)*/
			}			
			
			// given good values, let's save this curved line segment
			if (distance > 0) {
				CurveLineSegMetaData lmdObj = new CurveLineSegMetaData(
						new Point(x1,y1), 
                        new Point(x2,y2), 
                        distance, orientation, curvature, ++lineNumber);
				
				/* calc time to determine this curved line segments, 
			   	   us to low ms probably, store result*/
				long toc = System.nanoTime();
				long totalTime = toc - tic;
				lmdObj.setTotalTime(totalTime);
				
				/* add curve line segment to data structure for all curved 
				 * line segment for the segmented region of the image */
				lmd.add(lmdObj);						
			}
			segx1Mat = segx2Mat.clone();
			segy1Mat = segy2Mat.clone();
			
			/* starting point of next curved line segment is end 
			 * point of the previous segment */
			x1C = x2;
			y1C = y2;
			spX1 = x2;
			spY1 = y2;
			x1 = x2;
			y1 = y2;
		}				    
		return lmd;
	}

	/***
	 * Create lines connecting the segments
	 * @param labels
	 * @param coords
	 * @return
	 */
	private static Mat constructLines(Mat labels, Mat coords) {
		if (labels == null) {
			System.err.println("constructLines(): WARNING: labels is null");
			return null;
		}
		
		if (coords == null) {
			System.err.println("constructLines(): WARNING: coords is null");
			return null;			
		}
		
		if (coords.empty()) {
			System.err.println("constructLines(): WARNING: coords is empty");
			return null;						
		}
		
		// total number of points to generate between a and b
		int n = 1000;
		
		// points from x1 to x2
		double[] x1 = coords.get(0, 0);
		double[] x2 = coords.get(1, 0);
		Mat cpts = ProjectUtilities.linspace_Mat(x1[0], x2[0], n);
		
		// points from y1 to y2
		double[] y1 = coords.get(0, 1);
		double[] y2 = coords.get(1, 1);
		Mat rpts = ProjectUtilities.linspace_Mat(y1[0], y2[0], n);
		
		int rows = labels.rows();
		int cols = labels.cols();				
		// index = sub2ind([r c],round(cpts),round(rpts));
		// Convert all the 2d subscripts to linear indices
		Mat index = new Mat(1, n, cpts.type(), Scalar.all(0));
		for(int i = 0; i < rows; i++) {
			double[] colPtArray = cpts.get(0, i);
			double[] rowPtArray = rpts.get(0, i);
			if ((colPtArray == null) || (rowPtArray == null) ||
				(colPtArray.length == 0) || (rowPtArray.length == 0)) {
				System.err.println("Part of row " + i + " of total number of rows " 
			    + rows +  " has no data");
				continue;
			}
			int rowSub = (int) Math.round(colPtArray[0]);
			int colSub = (int) Math.round(rowPtArray[0]);
			int value = ProjectUtilities.sub2ind(rowSub, colSub, 
											   rows-1, cols-1);
			index.put(0, i, value);				
		}
		
		/* Allow the column and row points matrices to be release 
		 * back to memory */
		cpts.release();
		rpts.release();
		
		//  bbw(index) = 1;
		int size = index.cols();
		for (int i = 0; i < size; i++) {
			double ind = index.get(0, i)[0];
			Mat m = ProjectUtilities.ind2sub((int)ind, rows, cols);
			labels.put((int)m.get(0, 0)[0], (int)m.get(0,  1)[0], 1);
			m.release();
		}
		
		return labels.clone();
	}

	/**
	 * Generate the line segments of a region
	 * @param cc -- chain code that aids in calculating line lengths and in
	 * determining how the direction changes from segment to segment
	 * @param start -- relative anchor point to use as starting point in
	 * generating line segments, for circular regions, you would see the
	 * last line segment connect back to the starting point
	 * @param sensitivity -- the fineness with which line segments are
	 * generated to more smoothly or roughly define a geometric area 
	 * the higher the value the more change or rougher the generated
	 * line segments will appear
	 * @return a composite object with 
	 * the list of x coordinates of all line segments, the list of
	 * y coordinates of all line segments, and the time to generate the
	 * line segments
	 */
	private static LineSegmentContainer line_segment(ArrayList<Double> cc, 
			                                           Point start,
			                                           int sensitivity) {
		
		// sanity checks
		if ((cc == null) && (start == null)) {
			System.err.println("WARNING: No chain code or start point");
			return null;
		}
		else if ((cc == null) && (start != null)) {
			System.err.println("WARNING: Segment is only a point");
			return null;
		}
		else if ((cc != null) && (start == null)) {
			System.err.println("WARNING: start point not defined");
			return null;
		}
		
		System.out.println("cc length: " + cc.size());
		
		/* offsets for visiting the eight neighbor pixels 
		 * of the current pixel under analysis */
		int[][] directions = new int[][] {
				{1, 0},
				{1, -1},
				{0, -1},
				{-1, -1},
				{-1, 0},
				{-1, 1},
				{0, 1},
				{1, 1}};
		
		/* All the points in x and y directions making up the line
		 * segments of a region */
		ArrayList<Mat> segment_x = new ArrayList<Mat>();
		ArrayList<Mat> segment_y = new ArrayList<Mat>();
		
		long tic = System.nanoTime();
		
		Point coords = start.clone();
		Point startCoordinate = start.clone();
		
		Mat newMatx = new Mat(1,1, CvType.CV_32FC1, 
		          Scalar.all(0));
		Mat newMaty = new Mat(1,1, CvType.CV_32FC1, 
		          Scalar.all(0));
		newMatx.put(0, 0, coords.x);
		newMaty.put(0, 0, coords.y);
		
		segment_x.add(newMatx.clone());
		segment_y.add(newMaty.clone());
		
		// Move through each value in the chain code 
		int limit = cc.size() - 1;
		for (int i = 1; i < limit; i++) {
			Point newCoordinate = new Point(coords.x + directions[(int) (cc.get(i).intValue())][0],
										     coords.y + directions[(int) (cc.get(i).intValue())][1]);
			double distMeasure = Math.sqrt(Math.pow(newCoordinate.x - startCoordinate.x,2) + 
					                        Math.pow(newCoordinate.y - startCoordinate.y,2));
			if (distMeasure >= sensitivity) {
				newMatx = new Mat(1,1, CvType.CV_32FC1, 
				          Scalar.all(0));
			    newMaty = new Mat(1,1, CvType.CV_32FC1, 
				          Scalar.all(0));
				newMatx.put(0, 0, coords.x);
				newMaty.put(0, 0, coords.y);
				
				segment_x.add(newMatx.clone());
				segment_y.add(newMaty.clone());
				
				startCoordinate.x = newCoordinate.x;
				startCoordinate.y = newCoordinate.y; 

			}
			
			coords.x = coords.x + directions[(int) (cc.get(i).intValue())][0];
			coords.y = coords.y + directions[(int) (cc.get(i).intValue())][1];

		}		
		
		// how long in ns did it take for us to generate the line segments
		long segment_time = System.nanoTime() - tic;
		
		/* package all the line segment coordinates and times into a
		   composite object */
		System.out.println("segment_x size="+segment_x.size());
		System.out.println("segment_y size="+segment_y.size());
		LineSegmentContainer lsc = new LineSegmentContainer(
								   segment_x, segment_y, 
								   segment_time);
		return lsc;
	}

	/**
	 * Generate an encoding for the input image
	 * 
	 * the chain code uses a compass metaphor with numbers 0 to 7 
	 * incrementing in a clock wise fashion. South is 0, North is
	 * 4, East is 6, and West is 2
	 * 
	 * NOTE: chain code seems to end with the first break, pixels 
	 * have to be part of one continuous border
	 * 
	 * NOTE: not scale invariant
	 * NOTE: to be rotation invariant, needs difference coding
     *
	 * @param img -- input image
	 * @return a composite object consisting of the image border,
	 * the list of times it took to generate each chain, the chain
	 * code itself, and the starting location for the chain code
	 */
	private static ChainCodingContainer chaincoding1(Mat img) {
		int[][] directions = new int[][] {
			{1, 0},
			{1, -1},
			{0, -1},
			{-1, -1},
			{-1, 0},
			{-1, 1},
			{0, 1},
			{1, 1}};
		
		long tic = System.nanoTime();
		ArrayList<Point> pts = ProjectUtilities.findInMat(img, 1, "first");
		
		/* Verify there is data to process in segment if not return 
		 * empty chain code container */
		if (pts.size() == 0) {
			ArrayList<Double> noData = new ArrayList<Double>(1);
			noData.add(0.0);
			ChainCodingContainer ccc = 
					new ChainCodingContainer(
							img, System.nanoTime() - tic, noData, new Point(0,0));
			return ccc;
		}
		
		/* Get the number of rows and columns in segment to process over the whole
		 * of it */
		int rows = img.rows();
		int cols = img.cols();
		
		// The chain code
		ArrayList<Double> cc = new ArrayList<Double>();
		ArrayList<Double> allD  = new ArrayList<Double>();
		
		// Coordinates of the current pixel		
		Point coord = pts.get(0);		
		Point start = coord.clone();
		
		// The starting direction
		int dir = 1;
		long cnt = 0;
		ArrayList<Point> coordsLookedAt = new ArrayList<Point>();
		coordsLookedAt.add(start);
		while (true) {
			cnt++;
			Point newcoord = new Point(coord.x + directions[dir][0], 
						                coord.y + directions[dir][1]);
			coordsLookedAt.add(newcoord);
			
			double[] value = img.get((int) newcoord.x, (int) newcoord.y);
			if (((int) newcoord.x < rows) && 
				((int) newcoord.y < cols) && 
				(value != null) && (value[0] != 0.0)){
				// not sure about this line cc = [cc, dir] from matlab code
				cc.add(new Double(dir));
				coord = newcoord.clone();
				dir = Math.floorMod(dir+2, 8);
			}
			else {	
				dir = Math.floorMod(dir-1, 8);
			}
			allD.add(new Double(dir));
			
			// Back to starting situation
			if (((int) coord.x == start.x) && 
				((int) coord.y == start.y) &&  
				(dir == 1)) {
				break;
			}
		}
		
		/* Line segment generation using generated line code, set cells to 
		 * almost total black */
		Mat border = new Mat(rows, cols, img.type(), Scalar.all(0));
		Point coords = start.clone();
		for (int i = 0; i < cc.size(); i++) {
			border.put((int) coords.x, (int) coords.y, new double[]{1.0});
			
			// coords = coords + directions(cc(ii)+1,:);
			coords.x = coords.x + directions[(int) (cc.get(i).intValue())][0];
			coords.y = coords.y + directions[(int) (cc.get(i).intValue())][1];
		}
		
		long chain_time = System.nanoTime() - tic;
		
		ChainCodingContainer ccc = 
				new ChainCodingContainer(border, chain_time, cc, start);
		
		return ccc;
	}
	
	/**
	 * Partition the input image data into clusters using NGB provided method
	 * <br/> NOTE: opencv kmeans does not present the data in a useful form w/o
	 * additional post-processing, results are generally different
	 * @param data -- input image (signal data or n observations)
	 * @param nclusters -- number of sets to partition data into
	 * @param niterations -- number of times to attempt partitioning
	 * @return container with clustered data, stats
	 */
	private static kMeansNGBContainer 
		kmeansNGB(Mat data, int nclusters, int niterations) {
		// adjust input to double precision floating
		kMeansNGBContainer container = null;
		Mat input = new Mat(data.rows(), data.cols(), data.type());
		data.convertTo(input, data.type(), 1.0/255.0);
		
		// create return matrix
		Mat Label = new Mat(data.rows(), data.cols(), data.type(), 
				            Scalar.all(0.0));
		
		int nrows = data.rows();
		int ncols = data.cols();
		
		// random seed
		Mat Temprows = new Mat(1, nclusters, CvType.CV_32SC1);
		Mat Tempcols = new Mat(1, nclusters, CvType.CV_32SC1);

		// test data for cell2.pgm with 16 clusters with 16 iterations
		/* Temprows.put(0, 0, 29);
		Temprows.put(0, 1, 114);
		Temprows.put(0, 2, 15);
		Temprows.put(0, 3, 25);
		Temprows.put(0, 4, 171);
		Temprows.put(0, 5, 79);
		Temprows.put(0, 6, 108);
		Temprows.put(0, 7, 168);
		Temprows.put(0, 8, 179);
		Temprows.put(0, 9, 27);
		Temprows.put(0, 10, 69);
		Temprows.put(0, 11, 52);
		Temprows.put(0, 12, 122);
		Temprows.put(0, 13, 101);
		Temprows.put(0, 14, 36);
		Temprows.put(0, 15, 48);
		
		Tempcols.put(0, 0, 116);
		Tempcols.put(0, 1, 34);
		Tempcols.put(0, 2, 16);
		Tempcols.put(0, 3, 4);
		Tempcols.put(0, 4, 243);
		Tempcols.put(0, 5, 44);
		Tempcols.put(0, 6, 189);
		Tempcols.put(0, 7, 212);
		Tempcols.put(0, 8, 167);
		Tempcols.put(0, 9, 246);
		Tempcols.put(0, 10, 61);
		Tempcols.put(0, 11, 25);
		Tempcols.put(0, 12, 12);
		Tempcols.put(0, 13, 148);
		Tempcols.put(0, 14, 91);
		Tempcols.put(0, 15, 141); */
		
		Core.randu(Temprows, 0, input.rows());
		Core.randu(Tempcols, 0, input.cols());
		Mat Indrows = Temprows.clone();
		Mat Indcolumns = Tempcols.clone();
		
		// determine average intensity of randomly chosen clusters
		int counter = 0;
		Mat avItensity = new Mat(1, nclusters, CvType.CV_64FC1, Scalar.all(0));
		Mat avRows = new Mat(1, nclusters, CvType.CV_64FC1, 
	             			 Scalar.all(0.0));
		Mat avCols = new Mat(1, nclusters, CvType.CV_64FC1, 
	             			 Scalar.all(0.0));
		for (int k = 0; k < nclusters; k++) {
			int rowToRetrieve = (int)Indrows.get(0, k)[0];
			int colToRetrieve = (int)Indcolumns.get(0, k)[0];
			double[] value = input.get(rowToRetrieve, colToRetrieve);
			avItensity.put(0, k, value[0]);
			//System.out.println(avItensity.get(k, 0)[0]);
			//System.out.println(avItensity.dump());
		}		
		//System.out.println(avItensity.dump());
		
		Mat ClusterCenter = new Mat(nclusters, 3, CvType.CV_64FC1, Scalar.all(0.0));
		Mat count = new Mat(1, nclusters, CvType.CV_64FC1, Scalar.all(0.0));
		Mat sumInt = new Mat(1, nclusters, CvType.CV_64FC1, Scalar.all(0.0));
		Mat sumy = new Mat(1, nclusters, CvType.CV_64FC1, Scalar.all(0.0));
		Mat sumx = new Mat(1, nclusters, CvType.CV_64FC1, Scalar.all(0.0));
		while (counter < niterations) {
			// assign the cluster center
			for(int k = 0; k < nclusters; k++) {				
				ClusterCenter.put(k, 0, Indrows.get(0, k));
				ClusterCenter.put(k, 1, Indcolumns.get(0, k));
				ClusterCenter.put(k, 2, avItensity.get(0, k));
				count.put(0, k, 0);
				sumInt.put(0, k, 0);
				sumy.put(0, k, 0);
				sumx.put(0, k, 0);
			}

			// assign the pixel to clusters
			Mat distance = new Mat(1, nclusters, CvType.CV_64FC1, Scalar.all(0));
			for (int i = 0; i < nrows; i++) {
				for (int j = 0; j < ncols; j++) {
					for (int k = 0; k < nclusters; k++) {
						double value = 
								Math.pow((i - Indrows.get(0, k)[0]), 2)
						      + Math.pow((j - Indcolumns.get(0, k)[0]), 2) 
						      + Math.pow(
						           ((255*input.get(i, j)[0]) - 
						        		   ClusterCenter.get(k, 2)[0]), 2);
						value = Math.sqrt(value);
						distance.put(0, k, value);
					}
					MinMaxLocResult minmaxlocs = Core.minMaxLoc(distance);
					Point cluster = minmaxlocs.minLoc;
					
					/* this gives pixels in the same area that are assigned to 
					 * the same cluster a standard color */
					double intensity = minmaxlocs.minLoc.x;
					double length = 255 / (double)nclusters;
					
					// place the pixel in its bucket with its artificial color
					Label.put(i, j, length*intensity);
					
					// update stat counts
					double cnt = count.get(0, (int) cluster.x)[0];
					count.put(0,  (int) cluster.x, ++cnt);
					double sumVal = sumInt.get(0, (int) cluster.x)[0] + input.get(i, j)[0];
					sumInt.put(0, (int) cluster.x, sumVal);
					double sumyVal = sumy.get(0,  (int) cluster.x)[0] + i;
					sumy.put(0,  (int) cluster.x, (sumyVal == 0) ? 1 : sumyVal);
					double sumxVal = sumx.get(0,  (int) cluster.x)[0] + j;
					sumx.put(0,  (int) cluster.x, (sumxVal == 0) ? 1 : sumxVal);					
				}
			}
			
			// update stats
			Core.divide(sumy, count, avRows);
			avRows = ProjectUtilities.round(avRows);
			Core.divide(sumx, count, avCols);
			avCols = ProjectUtilities.round(avCols);			
			Core.divide(sumInt, count, avItensity);
			avItensity = ProjectUtilities.multiplyScalar(avItensity, 255.0);
			avItensity = ProjectUtilities.round(avItensity);
			
			Indrows = avRows.clone();
			Indcolumns = avCols.clone();
			
			// Show any intermediate temp values here
			System.out.println("Iteration: " + (counter+1));
			System.out.println(ClusterCenter.dump());
			System.out.println("Percent complete: " + 
			   ((((float)counter+1)/(float)niterations))*100.0 + " %");
			counter++;
		}
		
		/* Adjusting for a future border of width three, 
		 * keep count of pixels' values that are identical 
		 * to each other */
		for (int i = 2; i < (nrows - 3); i++) {
			for (int j = 2; j < (ncols - 3); j++) {
				int count_pixel = 0;
				for (int l = (i - 2); l <= (i + 2); l++) {
					for (int k = (j - 2); k <= (j + 2); k++) {
						double v1 = Label.get(l, k)[0];
						double v2 = Label.get(i, j)[0];
						if (v1 == v2) {
							count_pixel++;
						}
					}
				}
				/* if the pixel count is above some arbitrary threshold
				 * for some row copy the other row to here, what is the
				 * theoretical motivation for this? */
				if (count_pixel > 18) {
					for (int l = (i - 1); l <= (i + 1); l++) {
						for (int k = (j - 1); k <= (j + 1); k++) {
							Label.put(l, k, Label.get(i, j)[0]);
						}
					}
				}
			}
		}
		HashMap<String, Mat> stats = new HashMap<String, Mat>(3);
		stats.put(avRowsString, avRows);
		stats.put(avColsString, avCols);
		stats.put(avIntString, avItensity);
		container = new kMeansNGBContainer(Label, stats);
		return container;
	}

	/**
	 * Generate binary image segments from clustered binary image
	 * <br/> <b> Prerequisite:</b> kmeans clustering and thresholding
	 * to generate global binary image 
	 * @param I -- clustered image with thresholding applied
	 * @return binary image segments and a list of times to generate each
	 * segment
	 */
	private static CompositeMat ScanSegments(Mat I) {	
		ArrayList<Long> ScanTimes = new ArrayList<Long>();
		Mat Temp = null;
		
		// find how many regions we need to segment?
		int rows = I.rows();
		int cols = I.cols();

		ArrayList<Mat> Segment = new ArrayList<Mat>();
		
		// Create a matrix with all rows and all columns of a label
		int changecount = 0;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				double[] labelData = I.get(i, j);
				/* A label is a dark pixel starting point from which we 
				 * will grow a region */
				if (labelData[0] == 0) {
					I.put(i, j, 1.0);
					changecount++;
				}
			}
		}
		
		// convert the input image to double precision
		Temp = new Mat();
		I.convertTo(Temp, I.type());
		//System.out.println("labels="+labels.dump());
		//System.out.println("Temp="+Temp.dump());
		
		// find first non-zero location
		ArrayList<Point> points = 
				ProjectUtilities.findInMat(Temp, 1, "first");
				
		int n = 1;
		int indx = -1;
		int indy = -1;
		if (points != null) {
			indx = (int) points.get(0).x;
			indy = (int) points.get(0).y;			
		}
		
		// keep going while we still have regions to process
		while (points != null) {
			// get the next set of nonzero indices that is pixel of region
			int i = indx;
			int j = indy;
			
			// Start timing code for segment
			long tic = System.nanoTime();
			
			/* pass the image segment to the region growing code along with 
			 * the coordinates of the seed and max intensity distance of 
			 * 1x10e-5 
			 * 
			 * This tends to eat the k-means segmented image starting at the
			 * start pixel. When the original segmented image is consumed, then
			 * we are done scanning for segments */
			double max_intensity_distance = 0.00001;
			ArrayList<Mat> JAndTemp = 
					regiongrowing(Temp, i, j, max_intensity_distance);			
			Mat output_region_image = JAndTemp.get(0);
			//System.out.println("output_region_image="+output_region_image.dump());
			Temp = JAndTemp.get(1);
			//System.out.print("Temp="+Temp.dump());
			
			/* pad the array and copy the extracted image segment with its
			   grown region into it */
			Mat padded = new Mat();
			int padding = 3;
			if (Temp != null) {
				padded.create(output_region_image.rows() + 2*padding, output_region_image.cols() + 2*padding, 
						output_region_image.type());
				padded.setTo(new Scalar(0));
				Rect rect = new Rect(padding, padding, output_region_image.cols(), output_region_image.rows());
				Mat paddedPortion = padded.submat(rect);
				output_region_image.copyTo(paddedPortion);
				//System.out.println("");
				//System.out.println("paddedPortion="+paddedPortion.dump());
				
				
				/* Assign padded array to Segment structure that gets
				   returned to caller */
				//System.out.println("padded="+padded.dump());
				Segment.add(padded);
			}
			
			// increment for storing next image segment
			n++;
			
			// finish timing work on current segment
			long toc = System.nanoTime();
			ScanTimes.add(toc - tic);
			
			// find next non-zero point to grow
			points = ProjectUtilities.findInMat(Temp, 1, "first");
			if ((points != null) && (points.size() > 0)) {
				indx = (int) points.get(0).x;
				indy = (int) points.get(0).y; 				
			}
			else if (points.size() == 0){
				points = null;
			}
			
			/* Generates huge number of files
			Imgcodecs.imwrite("output/padded"+n+".jpg", padded);
			Imgcodecs.imwrite("output/temp"+n+".jpg", Temp);
			*/
		}
		
		Mat allScanTimes = new Mat(1, ScanTimes.size(), CvType.CV_32FC1);
		for (int i = 0; i < ScanTimes.size(); i++) {
			allScanTimes.put(0, i, ScanTimes.get(i));
		}
		CompositeMat compositeSetMats = new CompositeMat(Segment, allScanTimes);
		return compositeSetMats;
	}

	/**
	 * Region based image segmentation method.  This method performs region
	 * growing in an image from a specified seedpoint
	 * 
	 * The region is iteratively grown by comparing all unallocated neigh-
	 * boring pixels to the region. The difference between a pixel's intensity
	 * value and the region's mean is used as a measure of similarity. The
	 * pixel with the smallest difference measured this way is allocated to the 
	 * respective region. This process continues until the intensity
	 * difference between region mean and new pixel become larger than a
	 * certain threshold (t) 
	 * 
	 * Properties:
	 * All pixels must be in a region
	 * Pixels must be connected 
	 * Regions should be disjoint (share border?)
	 * Pixels have approximately same grayscale
	 * Some predicate determines how two pixels are different (intensity 
	 * differences, see above) 
	 * 
	 * Points to remember:
	 * Selecting seed points is important
	 * Helps to have connectivity or pixel adjacent information
	 * Minimum area threshold (min size of segment) could be tweaked
	 * Similarity threshold value -- if diff of set of pixels is less than
	 * some value, all part of same region
	 * 
	 * @param I -- input matrix or image
	 * @param x -- x coordinate of seedpoint
	 * @param y -- y coordinate of seedpoint 
	 * @param reg_maxdist
	 * @return logical output image of region (J in the original matlab code) 
	 */
	private static ArrayList<Mat> regiongrowing(Mat I, int x, int y, double reg_maxdist) {
		// Local neighbor class to aid in region growing
		class Neighbor {
			public Point pt;
			public double[] px;
			
			public Neighbor(Point pt,  double[] px){
				this.pt = pt;
				this.px = px;
			}
			
			public Neighbor() {
				this.pt = new Point();
				this.px = new double[]{0.0};
			}
		}
				
		// Sanity check 1 
		if (reg_maxdist == 0.0) {
			reg_maxdist = 0.2;
		}
		
		// Sanity check 2
		/* in the Kroon code, the user will select a non-zero point to use that 
		 * then gets rounded this is really hard to do in this code at this 
		 * time, will defer implementation 
		 * 
		 * if(exist('y','var')==0), figure, imshow(I,[]); [y,x]=getpts; 
		 * y=round(y(1)); x=round(x(1)); end*/ 
/*		if (y == 0) {
			return null;
		}*/
		// System.out.println("I at beginning (region_growing)="+I.dump());
		// Create output image
		Mat J = new Mat(I.size(), I.type(), Scalar.all(0));
		
		// get dimensions of input image
		int rows = I.rows();
		int cols = I.cols();
		
		// get the mean of the segmented image
		// for get and put use see:
		// http://answers.opencv.org/question/14961/using-get-and-put-to-access-pixel-values-in-java/
		double reg_mean = I.get(x, y)[0];
		
		// set the number of pixels in the region
		int reg_size = 1;
		
		// Free memory to store neighbors of the segmented region
		int neg_free = 10000;
		int neg_pos = 0;
		
		ArrayList<Neighbor> neg_list = new ArrayList<Neighbor>(neg_free);
		//Neighbor[][] neg_list = new Neighbor[neg_free][neg_free];
		
		// Distance of the region newest pixel to the region mean
		double pixdist = 0;
		
		// Neighbor locations (footprint)
		int[][] neigb = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
		while((pixdist < reg_maxdist) && (reg_size < I.total())) {
			for (int j = 0; j < 4; j++) {
				// Add new neighbors pixels
				int xn = x + neigb[j][0];
				int yn = y + neigb[j][1];
						
				// Calculate the neighbor coordinate
				boolean ins = (xn >= 0) && (yn >= 0) && (xn < rows) && (yn < cols);
				
				// Check if neighbor is inside or outside the image
				// only checks one band here, may need to adjust
				double[] outputPt = new double[1];
				if (ins && (J.get(xn, yn) != null)) {
					outputPt[0] = J.get(xn, yn)[0];
				}
				else {
					//System.out.println("J["+xn+","+yn+"]not available");
					continue;
				}
				if (ins && (outputPt[0] == 0)) {
					
					// Add neighbor if inside and not already part of the segmented area
					neg_pos++;
					Point p = new Point(xn, yn);
					Neighbor nObj = 
							new Neighbor(p, I.get(xn, yn));
					neg_list.add(nObj);		
					J.put(xn, yn, 1.0);
				}
			}
			
			// Add a new block of free memory
			if (neg_pos + 10 > neg_free) {				
				neg_free = neg_free+10000;
				neg_list.ensureCapacity(neg_free);
			}
				
			// Add pixel with intensity nearest to the mean of the region
			// to the region
			double min_dist = Double.MAX_VALUE;
			Neighbor minNeighbor = null;
			Neighbor curNeighbor = null;
			for(int neg_pos_cnt = 0; neg_pos_cnt < neg_pos; neg_pos_cnt++) {	
				curNeighbor = neg_list.get(neg_pos_cnt);
				double[] value = curNeighbor.px;
				double dist = Math.abs(value[0] - reg_mean);
				if (dist < min_dist) {
					min_dist = dist;
					minNeighbor = curNeighbor;
				}
			}
			J.put(x, y, 2.0);
			reg_size++;
			
			// Calculate the new mean of the region
			if (minNeighbor != null) {
				// update best min pixel distance
				pixdist = min_dist;
				
				reg_mean = ((reg_mean*reg_size) + 
							minNeighbor.px[0])/(reg_size+1);
				
				/*  Save the x and y coordinates of the pixel 
				 *  (for the neighbour add proccess) */
				Point pForUpdate = minNeighbor.pt;
				x = (int) pForUpdate.x;
				y = (int) pForUpdate.y;
				
				// Remove the pixel from the neighbor (check) list
				neg_list.remove(minNeighbor);				
				neg_pos--;
			}				
		}
		
		//Return the segmented area as logical matrix
		//J=J>1;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if(J.get(i, j)[0] > 1) {
					J.put(i, j, 1);
				}
				else {
					J.put(i, j, 0);
				}
			}
		}
		
		// Remove pixels from region image that been processed
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (J.get(i, j)[0] == 1) {
					I.put(i, j, 0.0);
				}
			}
		}
		
		// Package data structures since Java can only return 1 value
		ArrayList<Mat> JAndTmp = new ArrayList<Mat>();
		// Temp = I
		Mat Temp = I.clone();		
		//System.out.println("I(region_growing)="+I.dump());
		//System.out.println("J(region_growing)="+J.dump());
		//System.out.println("Temp(region_growing)="+Temp.dump());
		JAndTmp.add(J.clone());
		JAndTmp.add(Temp.clone());
		return JAndTmp;
	}
	
	/**
	 * The Synthesize method is intended to join together separate segments into
	 * a larger subcomponent assembly to provide better matching against obstucted
	 * images 
	 * 
	 * The database will be updated in the same manner as during LGRunME except that
	 * instead of rows containing single segments, they will contain subcomponents
	 * (e.g., two or more regions of processed image data as a single unit)
	 * 
	 * @param cm -- the set of matrices used from the initial LGRunME processing
	 * @param debug -- write out extra debug data
 	 * @return an update set of matrices 
	 */
	public static CompositeMat Synthesize(CompositeMat cm, boolean debug) {
		/* Give the database holds many images and views of those images, it is 
		 * important to find the starting and end points for the model image that
		 * was just process and calculate the total number of ids to move through*/
		String filename = cm.getFilename();
		long startingID = cm.getStartingId();
		long lastID = cm.getLastId();
		long totalIDs = lastID - startingID + 1;
		long dbTotalIDs = DatabaseModule.cntSegmentsForFile(filename);
		filename = filename.replace('/', ':');
		int dbFirstID = DatabaseModule.getStartId(filename);
		int dbLastID = DatabaseModule.getLastId(filename);		
		System.out.println("CM would retrive segments for " + filename + 
				" between IDs " +  startingID + " and " + lastID + " with total " + totalIDs);
		System.out.println("Database would retrive segments for " + filename + 
				" between IDs " +  dbFirstID + " and " + dbLastID + " with total " + dbTotalIDs);
		String dbFileNameStart= DatabaseModule.getFileName((int)startingID);
		String dbFileNameEnd= DatabaseModule.getFileName((int)lastID);
		Point startingSegmentMoment = DatabaseModule.getMoment((int)startingID);
		TreeMap<Double, Integer> distances = 
				new TreeMap<Double, Integer>();
		double newSize = Math.pow(cm.getListofMats().size(),2.0);
		ArrayList<Mat> cmsToInsert = new ArrayList<Mat>((int)newSize+1); 
		CompositeMat scm = new CompositeMat();
		
		// Sanity checks
		if ((startingID !=  dbFirstID) || (lastID != dbLastID)) {
			System.err.println("ID Mismatch between segments and database");
			System.exit(500);
		}
		
		if ((dbFileNameStart == null) || (!dbFileNameStart.equalsIgnoreCase(filename))) {
			System.err.println("Filename mismatch between starting "
					            + "segments and database");
			System.exit(501);
		}
		
		if ((dbFileNameStart == null) || (!dbFileNameEnd.equalsIgnoreCase(filename))) {
			System.err.println("Filename mismatch between ending "
					            + "segments and database");
			System.exit(502);
		}
		
		if ( dbTotalIDs != totalIDs) {
			System.err.println("Mismatch on total number of segments");
			System.exit(503);
		}
		
		
		
		/* Calculate distances from ith segment to all other segments
		   took out lastID for now, just three iterations due to the
		   heavy computational burden this highly unoptimized code 
		   is placing on the system */
		for(long i = startingID; i < (startingID+4); i++) {			
			long counter = 0;
			long strtSegment = i;

			/* Move through all the other segments relative to the ith 
			 * segment */	
			long c1 = 0;
			while (counter < totalIDs) {
				
				Point curSegMoment = DatabaseModule.getMoment((int)(strtSegment+counter));
				if (curSegMoment == null) {
					System.err.println("null moment encountered");
					counter++;
					continue;
				}
				double distance = 
						ProjectUtilities.distance(startingSegmentMoment, curSegMoment);
				System.out.println("Distance from " + strtSegment +  " to " + (strtSegment+counter)
						           + " is " + distance);
				
				/* since distances serve as keys, you may have two or more 
				   calculations that come out the same, so this handles the collision */
				boolean distancesHasKey = distances.containsKey(distance);
				if (distancesHasKey) {
					System.err.println("There was a previous value associated "
							+ " with the key " + distance 
							+ " and value counter="+(counter-1));
					System.err.println("Adjusting calculation slightly to include entry");
					Random rnd = new Random();
					distance += (rnd.nextDouble() * 0.001);
					distancesHasKey = distances.containsKey(distance);
					while(distancesHasKey) {
						distance += (rnd.nextDouble() * 0.001);
						distancesHasKey = distances.containsKey(distance);
					}
				}
				distances.put(distance, (int)(strtSegment+counter));
				counter++;
				c1++;
			}
			
			// display sorted distances if debug mode is on
			Set<Double> keys = distances.keySet();
			Iterator<Double> kIt = keys.iterator();
			long c2 = 0;
			while((debug == true) && (kIt.hasNext())) {
				Double key = kIt.next();
				System.out.println(c2 + ".) Sorted distance " + key + " from " 
				                   + (strtSegment+c2) + " to  " + 
						           distances.get(key));
				c2++;
			}

			/* see http://docs.opencv.org/2.4/doc/tutorials/core/adding_images/adding_images.html
			   for reference 
			   
			   Base segment is an intermediate segment, just the trivial 
			   case */			
			counter = 0;
			Mat baseSegment = cm.getListofMats().get((int) counter);
			kIt = keys.iterator();
			long c3 = 0;
			
			/*Synthesize intermediates in a progressive manner
			 * based on calculated distances from start segment
			 * moment to target segment moment */
			while(kIt.hasNext()) {				
				Double key = kIt.next();
				int relativekey = (int) (distances.get(key) - startingID);
				System.out.println("Merging " + distances.get(key) 
				                   + " or relative segment " + 
						           relativekey);
				Mat mergingSegment = 
						cm.getListofMats().get(relativekey);
				
				/* dst = alpha(src1) + beta(src2) + gamma */
				if (debug == true) {
					Imgcodecs.imwrite("output/baseSegment"+filename+"_"+(c3)+".jpg", 
							baseSegment);
					Imgcodecs.imwrite("output/mergingSegment"+filename+(c3)+".jpg", 
							mergingSegment);					
				}

				Core.addWeighted(baseSegment, 0.5, 
						         mergingSegment, 0.5, 0.0, baseSegment);
				
				/* Due to 50% weighting when merging segments, use a threshold
				 * operator to strength or refresh border pixels */
				Imgproc.threshold(baseSegment, baseSegment, 
						          1, 255, Imgproc.THRESH_BINARY);
				
				/* Add synthesize segment into list of segments */
				cmsToInsert.add(baseSegment.clone());
				if (debug == true) {
					Imgcodecs.imwrite("output/mergedSegment_"+strtSegment+filename+"_"+(c3)+".jpg", 
					           baseSegment);					
				}

				/* Imgcodecs.imwrite("output/mergedSegment_"+strtSegment+"_"+(distances.get(key))+".jpg", 
						           baseSegment); */
				c3++;				
			}
			System.out.println("c1="+c1+" and c2="+c2 + " and c3="+c3);
			scm.addListofMat(cmsToInsert);
			
			// initialize values for next loop
			cmsToInsert = new ArrayList<Mat>((int)newSize+1);
			startingSegmentMoment = DatabaseModule.getMoment((int)i+1);
			distances = new TreeMap<Double, Integer>();
		}
		return scm;
	}
	
	public static CompositeMat Synthesize_sequential(CompositeMat cm, boolean debug) {
		/* Give the database holds many images and views of those images, it is 
		 * important to find the starting and end points for the model image that
		 * was just process and calculate the total number of ids to move through*/
		String filename = cm.getFilename();
		long startingID = cm.getStartingId();
		long lastID = cm.getLastId();
		long totalIDs = lastID - startingID + 1;						
		filename = filename.replace('/', ':');
		int dbFirstID = DatabaseModule.getStartId(filename);
		int dbLastID = DatabaseModule.getLastId(filename);	
		long dbTotalIDs = DatabaseModule.cntSegmentsForFile(filename);
		System.out.println("CM would retrive segments for " + filename + 
				" between IDs " +  startingID + " and " + lastID 
				+ " with total " + totalIDs);
		System.out.println("Database would retrive segments for " + filename + 
				" between IDs " +  dbFirstID + " and " + dbLastID 
				+ " with total " + dbTotalIDs);
		System.out.println("Composte Matrices Object says there "
				+ cm.getListofMats().size() 
				+ " matrices available");
;		String dbFileNameStart= DatabaseModule.getFileName((int)startingID);
		String dbFileNameEnd= DatabaseModule.getFileName((int)lastID);
		double newSize = Math.pow(cm.getListofMats().size(),2.0);
		ArrayList<Mat> cmsToInsert = new ArrayList<Mat>((int)newSize+1); 
		CompositeMat scm = new CompositeMat();
		scm.setFilename(cm.getFilename());		
		
		// Sanity checks
		if ((startingID !=  dbFirstID) || (lastID != dbLastID)) {
			System.err.println("ID Mismatch between segments and database");
			System.exit(500);
		}
		
		if ((dbFileNameStart == null) || (!dbFileNameStart.equalsIgnoreCase(filename))) {
			System.err.println("Filename mismatch between starting "
					            + "segments and database");
			System.exit(501);
		}
		
		if ((dbFileNameStart == null) || (!dbFileNameEnd.equalsIgnoreCase(filename))) {
			System.err.println("Filename mismatch between ending "
					            + "segments and database");
			System.exit(502);
		}
		
		if (dbTotalIDs != totalIDs) {
			System.err.println("Mismatch on total number of segments");
			System.exit(503);
		}
		
		/* see http://docs.opencv.org/2.4/doc/tutorials/core/adding_images/adding_images.html
		   for reference 
		   
		   Base segment is an intermediate segment, just the trivial 
		   case */			
		long counter = 0;
		Mat baseSegment = cm.getListofMats().get((int) counter);
		
		for (counter = 0; counter < totalIDs; counter++) {
			Mat mergingSegment = cm.getListofMats().get((int) counter);
			/* dst = alpha(src1) + beta(src2) + gamma */
			if (debug == true) {
				Imgcodecs.imwrite("output/baseSegment"+filename+"_"+(counter)+".jpg", 
						baseSegment);
				Imgcodecs.imwrite("output/mergingSegment"+filename+"_"+(counter)+".jpg",
						mergingSegment);					
			}

			Core.addWeighted(baseSegment, 0.5, 
					         mergingSegment, 0.5, 0.0, baseSegment);
			
			/* Due to 50% weighting when merging segments, use a threshold
			 * operator to strength or refresh border pixels */
			Imgproc.threshold(baseSegment, baseSegment, 
					          1, 255, Imgproc.THRESH_BINARY);
			
			/* Add synthesize segment into list of segments */
			cmsToInsert.add(baseSegment.clone());
			if (debug == true) {
				Imgcodecs.imwrite("output/mergedSegment_"+filename+"_"+(counter)+".jpg", 
				           baseSegment);					
			}
			
			scm.addListofMat(cmsToInsert);
			// initialize values for next loop
			cmsToInsert = new ArrayList<Mat>((int)newSize+1);
		}
		
		// return final result
		return scm;
	}
}
