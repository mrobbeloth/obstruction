package robbeloth.research;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
 */
public class LGAlgorithm {
	private final static String avRowsString = "Average Rows";
	private final static String avColsString = "Average Columns";
	private final static String avIntString = "Average Itensity";
	
	/**
	 * Local Global (LG) Graph Run Me Bootstrap Algorithm
	 * @param data -- input image
	 * @param K -- number of sets to partition data into
	 * @param clustered_data -- holder for data clusters
	 * @param criteria -- termination criteria
	 * @param attempts -- number of iterations to use in partitioning data
	 *                     the greater the number of iterations the possible
	 *                     greater number of regions the image will be 
	 *                     partitioned into...the stochastic nature of the
	 *                     partitioning kmeans algorithm
	 * @param flags -- special processing indicators (not used 
	 * @param filename -- name of file that is being processed
	 * @param pa -- partitioning algorithm choice
	 * since switching back to NGB kmeans algorithm)
	 */
	public static void LGRunME(Mat data, int K, Mat clustered_data, 
			                   TermCriteria criteria, int attempts,
			                   int flags, String filename, 
			                   ProjectUtilities.Partioning_Algorithm pa){	
		
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
		
		Mat converted_data_32F = new Mat(data.rows(), data.cols(), CvType.CV_32F);
		data.convertTo(converted_data_32F, CvType.CV_32F);
			
		/* verify we have the actual full model image to work with
		 * at the beginning of the process */
		Imgcodecs.imwrite("output/verify_full_image_in_ds" + "_" 
                          + System.currentTimeMillis() + ".jpg",
				          converted_data_32F);
		
		
		/* produce the segmented image using NGB or OpenCV Kmeans algorithm */
		Mat output = new Mat();
		Mat labels = null;
		if ((flags & Core.KMEANS_USE_INITIAL_LABELS) == 0x1) {
			labels = 
					ProjectUtilities.setInitialLabelsGrayscale(
							converted_data_32F.rows(), 
							converted_data_32F.height(), K);
			System.out.println("Programming initial labels");
			System.out.println("Labels are:");
			System.out.println(labels.dump());
		}
		else {
			labels = new Mat();
		}
		
		// start by smoothing the image -- let's get the obvious artificats removed
		Mat centers = new Mat();
		kMeansNGBContainer container = null;
		long tic = System.nanoTime();
		Imgproc.blur(converted_data_32F, converted_data_32F, new Size(9,9));
		Imgcodecs.imwrite("output/" + filename.substring(
				          filename.lastIndexOf('/')+1)+"_smoothed.jpg", 
				          converted_data_32F);
		
		// after smoothing, let's partition the image
		if (pa.equals(ProjectUtilities.Partioning_Algorithm.OPENCV)) {
			Mat colVec = converted_data_32F.reshape(
					1, converted_data_32F.rows()*converted_data_32F.cols());
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
			double compatness = Core.kmeans(colVecFloat, K, labels, criteria, attempts, 
					                         flags, centers);
			System.out.println("Compatness="+compatness);
			Mat labelsFromImg = labels.reshape(1, converted_data_32F.rows());
			container = opencv_kmeans_postProcess(converted_data_32F,  labelsFromImg, centers);
		}
		else if (pa.equals(ProjectUtilities.Partioning_Algorithm.NGB)) {
			data.convertTo(converted_data_32F, CvType.CV_32F);
			container = kmeansNGB(converted_data_32F, K, attempts);			
		}
		else {
			System.err.println("Paritioning algorithm not valid, returning");
			return;
		}
		
		
		clustered_data = container.getClustered_data();
		long toc = System.nanoTime();
		System.out.println("Partitioning time: " + 
				TimeUnit.MILLISECONDS.convert(toc - tic, TimeUnit.NANOSECONDS) + " ms");		
		
		// look at intermediate output from kmeans
		if (pa.equals(ProjectUtilities.Partioning_Algorithm.OPENCV)) {
			Imgcodecs.imwrite("output/" + "opencv" + "_" + System.currentTimeMillis() + ".jpg", 
			          clustered_data);		
		}
		else if (pa.equals(ProjectUtilities.Partioning_Algorithm.NGB)) {
			Imgcodecs.imwrite("output/" + "kmeansNGB" + "_" + System.currentTimeMillis() + ".jpg", 
			          clustered_data);		
		}
	
		// scan the image and produce one binary image for each segment
		CompositeMat cm = ScanSegments(clustered_data);
		ArrayList<Mat> cm_al_ms = cm.getListofMats();
		int segCnt = 0;
		for(Mat m : cm_al_ms) {
			Mat n = new Mat(m.rows(), m.cols(), m.type());
			m.convertTo(n, CvType.CV_8U);
			//System.out.println("n before threshold="+n.dump());
			Imgproc.threshold(n, n, 0, 255, Imgproc.THRESH_BINARY_INV);
			//System.out.println("n after threshold="+n.dump());
			Imgcodecs.imwrite("output/" + filename.substring(
					   filename.lastIndexOf('/')+1, 
			           filename.lastIndexOf('.')) +
					   "_binary_inv_scan_segments_"
			           + (++segCnt) + "_" + System.currentTimeMillis() 
			           + ".jpg", n);
			
			// Note: this is doing something horribly wrong, almost the entire image is gone
			Mat nCropped = ProjectUtilities.autoCropGrayScaleImage(n);
			Imgcodecs.imwrite("output/" + filename.substring(
					   filename.lastIndexOf('/')+1, 
			           filename.lastIndexOf('.')) +
					   "_cropped_binary_inv_scan_segments_"
			           + (segCnt) + "_" + System.currentTimeMillis() 
			           + ".jpg", nCropped);		
			
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
			           totalTime, TimeUnit.NANOSECONDS) + " ms");
			System.out.print(sb.toString());
		}				
		
		// calculate the local global graph
		localGlobal_graph(cm_al_ms, container, filename, pa);
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
	 * @param kMeansData -- data from application of kMeans algorithm
	 * @param filename   -- name of file being worked on
	 * @param pa         -- partitioning algorithm used
	 * @return the local global graph description of the image 
	 */
	private static ArrayList<LGNode> localGlobal_graph(ArrayList<Mat> Segments, 
			                                kMeansNGBContainer kMeansData, 
			                                String filename,
			                                ProjectUtilities.Partioning_Algorithm pa) {
		
		Mat clustered_data = kMeansData.getClustered_data();
		ArrayList<LGNode> global_graph = new ArrayList<LGNode>(Segments.size());
		int n = Segments.size();
		System.out.println("There are " + n + " total segments");
		
		double lg_time = 0;
		ArrayList<Double> t1 = new ArrayList<Double>();
		ArrayList<Double> t2 = new ArrayList<Double>();
		ArrayList<Long> t = new ArrayList<Long>();
		//ArrayList<Mat> skeleton = new ArrayList<Mat>();
		ArrayList<Double> end_line = new ArrayList<Double>();
		ArrayList<Point>S = new ArrayList<Point>(n);
		ChainCodingContainer ccc = null;
		//int skelCnt = 1;
		
		/* This section does the following two things:
		   1. Construct the local portion of the Local Global graph..
		   this portion focuses on the geometric description of the
		    line segments that  define the segment under analysis 
		
		   2. Build the overall Global part of the Local-Global graph
	       the global portion focuses on the establishment of the centroid 
	       regions and the connection of the starting segment centroid to
	       the other centroids in the other segments as a part of creating
	       an overall geometrical description of a model or target image
	       */
		for(int i = 0; i < n; i++) {
			long tic = System.nanoTime();
			
			/* Generate a representation of the segment based upon how
			 * the various pixels are connected to one another  */
			Mat segment = Segments.get(i).clone();
			ccc = chaincoding1(segment);
			System.out.println(ccc);
			t1.add(ccc.getChain_time());
			ArrayList<Double> cc = ccc.getCc();
			Point start = ccc.getStart();

			/* Use the chain code description of the segment to create a 
			 * border */
			Mat border = ccc.getBorder();	
			Mat convertedborder = new Mat(
					border.rows(), border.cols(), border.type());
			border.convertTo(convertedborder, CvType.CV_8U);
			Imgproc.threshold(convertedborder, convertedborder, 0, 255, 
			          Imgproc.THRESH_BINARY_INV);
			
			Imgcodecs.imwrite("output/" + filename.substring(
					   filename.lastIndexOf('/')+1, 
			           filename.lastIndexOf('.')) + 
			           "_border_"+(i+1)+ "_" + System.currentTimeMillis() 
			           + ".jpg",convertedborder);
			Mat croppedBorder = 
					ProjectUtilities.autoCropGrayScaleImage(convertedborder);
			Imgcodecs.imwrite("output/" + filename.substring(
					   filename.lastIndexOf('/')+1, 
			           filename.lastIndexOf('.')) + 
			           "_cropped_border_"+(i+1)+ "_" + System.currentTimeMillis() 
			           + ".jpg",croppedBorder);
			ccc.setBorder(croppedBorder);
			
			/* Using the chain code from the previous step, generate 
			 * the line segments of the segment */
			LineSegmentContainer lsc = 
					line_segment(cc, start, 1);		
			System.out.println(lsc);
			/* Generate a pictoral representation of the line segments
			 * using plplot and save to disk */
			
		    // Initialize plplot
			PLStream   pls = new PLStream();
	        // Parse and process command line arguments
			pls.parseopts( new String[]{""}, PL_PARSE_FULL | PL_PARSE_NOPROGRAM );
	        pls.setopt("verbose","verbose");
	        pls.setopt("dev","jpeg");
	        pls.setopt("o", "output/" + filename.substring(
					   filename.lastIndexOf('/')+1, 
			           filename.lastIndexOf('.')) + "_line_segment_" 
					   + (i+1) + "_" + System.currentTimeMillis() + ".jpg");
	        // Initialize plplot
	        pls.init();
	        
	        /* Convert segment arrays into a format suitable for
	         * plplot use */
			ArrayList<Mat> segx = lsc.getSegment_x();
			ArrayList<Mat> segy = lsc.getSegment_y();
	        double[] x = ProjectUtilities.convertMat1xn(segx);
	        double[] y = ProjectUtilities.convertMat1xn(segy);
	        
	        /* Determine limits to set in plot graph for plplot
	         * establish environment and labels of plot 
	         * Add ten pixels of padding for border */
	        double xmin = ProjectUtilities.findMin(x);
	        double xmax =  ProjectUtilities.findMax(x);
	        double ymin = ProjectUtilities.findMin(y);
	        double ymax =  ProjectUtilities.findMax(y);
	        pls.env(xmin-10, xmax+10, ymax+10, ymin-10, 0, 0);
	        pls.lab( "x", "y", "Segment Plot for segment " + i);
	        
	        // Plot the data that was prepared above.
	        pls.line( x, y );

	        // Close PLplot library
	        pls.end();
	        
			/* Derive the local graph shape description of segment 
			 * under consideration */
			ArrayList<CurveLineSegMetaData> lmd = shape_expression(segx, segy);
			if (lmd != null) {
				determine_line_connectivity(lmd);	
			}
			else {
				lmd = new ArrayList<CurveLineSegMetaData>();
				lmd.add(new CurveLineSegMetaData());
			}
			
			/* Store the amount of time it took to generate SH for 
			 * segment i, see (1) and (2) in 2008 paper */
			t2.add((double) lsc.getSegment_time());			
			lg_time = t1.get(i) + t2.get(i);
			
			//segm_skeleton = bwmorph(Segments(:,:,i),'skel',inf); 
			//ArrayList<Point> pt = ProjectUtilities.findInMat(segment, 1, "first");
//			Mat nConverted = new Mat(segment.rows(), segment.cols(), segment.type());
//			segment.convertTo(nConverted, CvType.CV_8U);
//			Imgproc.threshold(nConverted, nConverted, 0, 255, 
//					          Imgproc.THRESH_BINARY_INV);
			// int rows = nConverted.rows();
			// int cols = nConverted.cols();
			//int[][] p = ProjectUtilities.convertMatToIntArray(nConverted);
			
			// int[] q = ProjectUtilities.Convert2DMatrixto1DArray(p, rows, cols);
			
			// test conversion to 1D image buffer format
			/*
			  BufferedImage bi = new BufferedImage(m.cols(), m.rows(), 
			                                       BufferedImage.TYPE_BYTE_BINARY);
			  bi.getRaster().setPixels(0, 0, bi.getWidth(), bi.getHeight(), q);
			  AlgorithmGroup.writeImagesToDisk(
			     bi, new File("scan_segments_" + skelCnt + "_2dto1d.jpg"), null, "jpg");
			*/
			
			/* Robbeloth 5-16-2015 note that the skeltonization is not being used 
			 * anywhere right now just a carry over from the converted Matlab source
			 * code ... disabling for now */ 
			/* int[] segm_skeleton1D = 
					Skeletonization.k3m(q, cols, rows); */
			
			// test operator before conversion back to opencv mat
		    /* BufferedImage bi = new BufferedImage(m.cols(), m.rows(), 
                                                 BufferedImage.TYPE_BYTE_BINARY);
		    bi.getRaster().setPixels(0, 0, bi.getWidth(), bi.getHeight(), segm_skeleton1D);
		    AlgorithmGroup.writeImagesToDisk(
		    		bi, new File(
		    				"scan_segments_" + skelCnt + "_skel1d.jpg"), null, "jpg"); */
			/*
			int[][] segm_skeleton2D = 
					ProjectUtilities.Convert1DArrayto2DMatrix(
							segm_skeleton1D, rows, cols);
			Mat segm_skeleton = 
					new Mat(nConverted.rows(), nConverted.cols(), 
							nConverted.type());
			segm_skeleton = 
					ProjectUtilities.convertInttoGrayscaleMat(
							segm_skeleton2D, rows, cols);				
			skeleton.add(segm_skeleton);
			*/
			
			// Let's get a sanity check here by outputting intermediate format
			// Imgcodecs.imwrite("scan_segments_" + (skelCnt++) + "_skel.jpg", segm_skeleton);
			
			/* call S(i)  = regionprops(Segments(:,:,i), 'centroid');
			   Note moments have not been exposed through JNI on opencv 3.0 yet
			   Moments are used as part of curve matching, in particular to find
			   the scale parameter of an object s = (mom'/mom)^(1/2)
			   
			    From Bourbakis paper, moments help us to find the center point
			    of a region...
			    
			    Should hold up to translation and rotation on a candidate object */
			double[][] img = ProjectUtilities.convertMatToDoubleArray(segment);		
			System.out.println("Raw Moment: " + Moments.getRawCentroid(img));
			Point centroid = Moments.getRawCentroid(img);
			
			/* keep a copy of centroids for use in the construction of the
			   global portion of the geometric/pictorial description of the image
			   
			   this will aid in future matching of multiple regions using this 
			   method (section 3.0 of 2008 Bourbakis paper) */
			S.add(centroid);
			
			// store time to generate LG graph on segment
			long toc = System.nanoTime();
			t.add(toc - tic);
			
			/* Build ith node containing local node description, which 
			 * forms a part of the overall global geometric description
			 * of the image */
			HashMap<String, Mat> stats = null;
			HashMap<String, Double> segment_stats = null;
			if (pa.equals(ProjectUtilities.Partioning_Algorithm.NGB)) {
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
			else if (pa.equals(ProjectUtilities.Partioning_Algorithm.OPENCV)){
				stats = kMeansData.getStats();
				segment_stats = new HashMap<String, Double>();
				Set<String> statKeys = stats.keySet();
				for (String s : statKeys) {
					Mat m = stats.get(s);
					Double d = m.get(0,0)[0];
					segment_stats.put(s, d);
				}
			}
			
			/* Create the node */
			LGNode lgnode = new LGNode(centroid, segment_stats, 
					                   border, lmd, segment, pa, i);
			
			/* Add local region info to overall global description */
			global_graph.add(lgnode);
			
			/* Debug -- show info about region to a human */
			// System.out.println(lgnode.toString());
		}
		
	    // Initialize plplot
		PLStream   pls = new PLStream();
        // Parse and process command line arguments
		pls.parseopts( new String[]{""}, PL_PARSE_FULL | PL_PARSE_NOPROGRAM );
        pls.setopt("verbose","verbose");
        pls.setopt("dev","jpeg");
        pls.setopt("o", filename.substring(
				   filename.lastIndexOf('/')+1, 
		           filename.lastIndexOf('.')) +
        		   "_centroids_for_image" + "_" + System.currentTimeMillis() 
        		   + ".jpg");
        // Initialize plplot
        pls.init();
        
        /* Convert Point objects into a format suitable for
         * use by plplot
         */
        int sizeConversion = S.size();
        double[] xValues = new double[S.size()];
        double[] yValues = new double[S.size()];
        int sizeForLines = S.size()*2;
        double[] xValuePrime = new double[sizeForLines-1];
        double[] yValuePrime = new double[sizeForLines-1];
        double startingX = S.get(0).x;
        double startingY = S.get(0).y;
		for(int cnt = 0; cnt < sizeConversion; cnt++) {
			xValues[cnt] = S.get(cnt).x;
			yValues[cnt] = S.get(cnt).y;
		}
		int indexOtherArray = 1;
		for(int cnt = 0; indexOtherArray < sizeConversion; cnt+=2) {
			xValuePrime[cnt] = startingX;
			xValuePrime[cnt+1] = S.get(indexOtherArray).x;
			yValuePrime[cnt] = startingY;
			yValuePrime[cnt+1] = S.get(indexOtherArray++).y;
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
		
		//TODO rest of the lg_graph method
		Long T = 0l;
		int cntTs = 1;
		for(Long l : t) {
			System.out.println("Time to generate segment " 
		                        + cntTs++ + " is " + 
		                        TimeUnit.SECONDS.convert(l, 
		                        TimeUnit.NANOSECONDS));
			T += l;
		}
		
		/* Build the structures needed for the displaying of the LG
		 * graph over the segmented image */	
		Mat C = new Mat(2, n, CvType.CV_64FC1);
		// Setup for getting directional vectors from centroids
		// C(:,i) = S(1,i).Centroid; and C = floor (C);
		// x's are in first row, y's in second 
		for (int i = 0; i < n; i++) {
			Point Sp = S.get(i);
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
		for (int i = 0; i < n-1; i++) {
			if (i == n-1) {
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
		System.out.println("Time to calcuate angle_time: " + angle_time/1.0E-6  
				           + " ms");
		
		Mat lined = clustered_data.clone();		
		for (int i = 0; i < (n-1); i++) {
			// coords = [C(2,1) C(1,1);C(2,i+1) C(1,i+1)];
			Mat coords = new Mat(2,2,CvType.CV_64FC1);
			coords.put(0, 0, C.get(1, 0));
			coords.put(0, 1, C.get(0, 0));
			coords.put(1, 0, C.get(1, i+1));
			coords.put(1, 1, C.get(0, i+1));
			
			// lined = plotlines(lined, coords);
			System.out.println("Building lines for segment " + i);
			lined = constructLines(lined, coords);
		}
		
		Mat border = null;
		if (ccc != null) {
			border = ccc.getBorder().clone();	
		}
		
/*		// [y1] = zeros(size(border));
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
		 * and write image data out to disk */
		Mat clustered_data_clone = clustered_data.clone();
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
		Point startCentroid = S.get(0);
		for(Point p : S) {
			/* Write moment to standard output */
			System.out.println("Moment: " + p.x + "," + p.y);
			
			/* Superimpose moment as a line from the starting
			 * region to the ith region center of mass */
			Imgproc.circle(
					clustered_data_clone, S.get(index), 5, 
					new Scalar(25, 25, 112));
			Imgproc.line(clustered_data_clone, S.get(0), 
					     S.get(index), new Scalar(25, 25, 112));
			
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
		boolean imWriteResult = 
				Imgcodecs.imwrite(filename.substring(filename.lastIndexOf('/')+1) 
				          		  + "_moments_over_clustered_data" + "_" 
						          + System.currentTimeMillis() 
				          		  + ".jpg",
				          		  clustered_data_clone);
		System.out.println("Result of merging centroids onto clustered image: " 
				          		  + imWriteResult);
		
		/* Calculate angle threshold differences and write them out to 
		 * the spreadsheet*/
		Mat angle_differences  = calc_angle_differences(ccc.getStart(), S);
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
		return global_graph;
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
	 * Generate the shape description for a region
	 * Line length is based on line distance formula from beg. to end
	 * Line orientation is based on the derivative of the line 
	 * @param segx
	 * @param segy
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
		double x1 = segx1Mat.get(0, 0)[0];
		double y1 = segy1Mat.get(0, 0)[0];
		// store basic line information including length
		for(int i = 1; i < sz; i++) {			
			Mat segx2Mat = segx.get(i);
			Mat segy2Mat = segy.get(i);

			double x2 = segx2Mat.get(0, 1)[0];
			double y2 = segy2Mat.get(0, 1)[0];
			
			// distance calculation
			double distance = Math.sqrt(
					Math.pow((x1 - x2),2) + 
					Math.pow((y1 - y2),2));
			
			// orientation calculation
			double dy = y2 - y1;
			double dx = x2 - x1;
			double orientation = Math.atan2(dy,dx);
			orientation *= 180/Math.PI; // convert to degrees
			
			if (distance > 0) {
				CurveLineSegMetaData lmdObj = new CurveLineSegMetaData(
						new Point(x1,y1), 
                        new Point(x2,y2), 
                        distance, orientation, 0, lineNumber++);
				lmd.add(lmdObj);						
			}
			segx1Mat = segx2Mat.clone();
			segy1Mat = segy2Mat.clone();
			x1 = segx1Mat.get(0, 0)[0];
			y1 = segy1Mat.get(0, 0)[0];
		}			
		return lmd;
	}

	/***
	 * Create lines connecting the segments?
	 * @param labels
	 * @param coords
	 * @return
	 */
	private static Mat constructLines(Mat labels, Mat coords) {
		if (labels == null) {
			System.out.println("constructLines(): WARNING: labels is null");
			return null;
		}
		
		if (coords == null) {
			System.out.println("constructLines(): WARNING: coords is null");
			return null;			
		}
		
		// total number of points to generate between a and b
		int n = 1000;
		
		// points from x1 to x2
		Mat cpts = ProjectUtilities.linspace_Mat(
				coords.get(0, 0)[0], coords.get(1,0)[0], n);
		// points from y1 to y2
		Mat rpts = ProjectUtilities.linspace_Mat(
				coords.get(0, 1)[0], coords.get(1,1)[0], n);
		int rows = labels.rows();
		int cols = labels.cols();		
		
		// index = sub2ind([r c],round(cpts),round(rpts));
		// Convert all the 2d subscripts to linear indices
		Mat index = new Mat(1, n, cpts.type(), Scalar.all(0));
		for(int i = 0; i < rows; i++) {
			int rowSub = (int) Math.round(cpts.get(0, i)[0]);
			int colSub = (int) Math.round(rpts.get(0, i)[0]);
			int value = ProjectUtilities.sub2ind(rowSub, colSub, 
											   rows-1, cols-1);
			index.put(0, i, value);				
		}
		//  bbw(index) = 1;
		int size = index.cols();
		for (int i = 0; i < size; i++) {
			double ind = index.get(0, i)[0];
			Mat m = ProjectUtilities.ind2sub((int)ind, rows, cols);
			labels.put((int)m.get(0, 0)[0], (int)m.get(0,  1)[0], 1);
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
		
		long tic = System.nanoTime();
		double lines = 0;
		
		Point coords = start.clone();
		
		/* Use of Arraylists instead of Mat here because size for each call
		  is indeterminate, might be able to use Mat rescale? is it more
		  expensive than array list dynamic resizing? */
		
		/* */
		double mean = 0;
		ArrayList<Double> start_line = new ArrayList<Double>();
		start_line.add(1d);
		ArrayList<Double> end_line = new ArrayList<Double>();
		end_line.add(1d);
		
		/* All the points in x and y directions making up the line
		 * segments of a region */
		ArrayList<Mat> segment_x = new ArrayList<Mat>();
		ArrayList<Mat> segment_y = new ArrayList<Mat>();
		
		/* we need to idenitfy the border */
		Mat border = new Mat();
		
		/* holds the latest x and y point for one of line segments
		 * to be added in the future */
		Mat newMatx = new Mat(1,2, CvType.CV_64FC1, 
				              Scalar.all(0));
		Mat newMaty = new Mat(1,2, CvType.CV_64FC1, 
				              Scalar.all(0));
		
		int count1 = 0;

		/* indicies to keep track of start and end of current line segment 
		   for region */
		double index_sens_start = 0d;
		double index_sens_end = 0d;
		
		// Move through each value in the chain code 
		for (int i = 1; i < cc.size(); i++) {

			/* For a new line segment, establish its starting and
			   ending locations */
			if (count1 == 0) {
				index_sens_start = start_line.get((int) lines);
				index_sens_end = index_sens_start;
			}
			/* for a current line segment, update the starting and
			 * ending locations taking the count to sensitivity 
			 * ratio into account */
			else {
				index_sens_start = (start_line.get((int)lines) + 
								   ((count1/sensitivity)-1)*sensitivity);
				index_sens_end = (start_line.get((int)lines) + 
						           ((count1/sensitivity))*sensitivity)-1; 
			}
			
			// calculate the chain code mean value  
			double sum = 0.0;
			for (int index = (int) index_sens_start; 
					  index <= index_sens_end; index++) {
				sum += cc.get(index).doubleValue();
			}
			mean = sum / ((index_sens_end - index_sens_start) + 1);
			
			// on first iteration, use starting point for fist line segment
			if (i == 1) {				
				newMatx.put(0, 0, coords.x);
				newMaty.put(0, 0, coords.y);
			}
			/*  on subsequent iterations, "connect" each point with a
			 *  line segment
			 *  
			 *  the mean of the cc segment should be greater than the specified
			 *  sensitivity 
			 */
			else if (mean >= sensitivity) {
				end_line.add((double) i); //?
				newMatx.put(0, 1, coords.x);
				newMaty.put(0, 1, coords.y);	

				// add segment
				segment_x.add(newMatx.clone());
				segment_y.add(newMaty.clone());
				
				// prepare new segment			
				lines++;					
				count1 = 0;
				
				// add index of chain start position for line
				start_line.add((double)i);
				
				// get next component in chain
				Point temp_coords = new Point();
				temp_coords.x = coords.x + 
						   directions[(int) (cc.get(i-1).intValue())][0];
				temp_coords.y = coords.y + 
						   directions[(int) (cc.get(i-1).intValue())][1];
				// init new component in chain with next set of coordinates
				newMatx = new Mat(1,2, CvType.CV_64FC1, 
						          Scalar.all(0));
				newMaty = new Mat(1,2, CvType.CV_64FC1, 
						          Scalar.all(0));
				newMatx.put(0, 0, temp_coords.x);
				newMaty.put(0, 0, temp_coords.y);
			}
			
			/* for each line segment of a region, grow or shape a border
			 * around it */
			int curRsize = border.rows();
			if (curRsize > coords.y) {
				border.reshape(0, curRsize);
			}
			border.put((int)coords.x, 
					   (int)coords.y, 1d);			
			
			//coords = coords + directions(cc(ii-1)+1,:);
			/* Move to a coordinates neighbor because upon the chain code 
			 * value */
			coords.x = coords.x + directions[(int) (cc.get(i-1).intValue())][0];
			coords.y = coords.y + directions[(int) (cc.get(i-1).intValue())][1];
			count1++;		
		}
		
		// how long in ns did it take for us to generate the line segments
		long segment_time = System.nanoTime() - tic;
		
		/* package all the line segment coordinates and times into a
		   composite object */
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
		MinMaxLocResult mm = Core.minMaxLoc(data);
		data.convertTo(input, data.type(), 1.0/255.0);
		
		// create return matrix
		Mat Label = new Mat(data.rows(), data.cols(), data.type(), 
				            Scalar.all(0.0));
		
		int nrows = data.rows();
		int ncols = data.cols();
		
		// random seed
		Mat Temprows = new Mat(1, nclusters, CvType.CV_32SC1);
		Mat Tempcols = new Mat(1, nclusters, CvType.CV_32SC1);

		// test data for cell2.pgm with 6 clusters with 6 iterations
/*		Temprows.put(0, 0, 162);
		Temprows.put(0, 1, 137);
		Temprows.put(0, 2, 55);
		Temprows.put(0, 3, 33);
		Temprows.put(0, 4, 45);
		Temprows.put(0, 5, 118);
		Tempcols.put(0, 0, 245);
		Tempcols.put(0, 1, 92);
		Tempcols.put(0, 2, 220);
		Tempcols.put(0, 3, 104);
		Tempcols.put(0, 4, 29);
		Tempcols.put(0, 5, 118);*/
		
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
			 * 1x10e-5 */
			double max_intensity_distance = 0.00001;
			ArrayList<Mat> JAndTemp = 
					regiongrowing(Temp, i, j, max_intensity_distance);			
			Mat output_region_image = JAndTemp.get(0);
			//System.out.println("output_region_image="+output_region_image.dump());
			Temp = JAndTemp.get(1);
			//System.out.print("Temp="+Temp.dump());
			
			/* pad the array and copy the image segment with its
			   grown regions into it */
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
		}
		
		Mat allScanTimes = new Mat(1, ScanTimes.size(), CvType.CV_64FC1);
		for (int i = 0; i < ScanTimes.size(); i++) {
			allScanTimes.put(0, i, ScanTimes.get(i));
		}
		CompositeMat compositeSetMats = new CompositeMat(Segment, allScanTimes);
		return compositeSetMats;
	}

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
}
