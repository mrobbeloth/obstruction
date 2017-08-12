package robbeloth.research;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;

import plplot.core.PLStream;
import robbeloth.research.ProjectUtilities.Partitioning_Algorithm;
/**
 * This class controls the application of image computing algorithms on input 
 * data while saving the modified image and possibly providing secondary
 * outputs
 * @author Michael Robbeloth
 * @category Projects
 * @since 2/7/2015 
 * @version 0.3
 * <br/><br/>
 * Class: CEG7900<br/>
 * <h2>Revision History</h2><br/>
 * <b>Date						Revision</b>
 *    2/7/2015                  (0.1) Initial Version
 *    3/21/2015                 (0.2) Matlab code converted
 *                                    LGGraph line metadata object created
 *    7/18/2015					 (0.3) Place source into github
 *                                     closest approximation to source used
 *                                     at NAECON 2015 talk
 */
public class ProjectController {

	public static void main(String[] args) {
		final double VERSION = 0.5;
		int imgCnt = 1;
		final String[] commands = {"--version", 
				                    "--process_model_image",
				                    "--drop_model_database",
				                    "--create_model_database",
				                    "--test",
				                    "--dump_model_database",
				                    "--find_match",
				                    "--backup_database"};
		
		/* General process here (original thought process) in processing an image: 
		 * 
		 * 1 For each image in the database
		 * 2.   For each view of the image in the database 
		 * 3.       1. Partition image into segments using kmeans
		 *          2. Generate binary image segments using binarized image 
		 *          3. Generate LGGraph representation of view
		 *          4. Store LGGraph representation into memory
		 * 4.   Process unknown partial image 
		 *      4.1 Apply 3.1 to 3.4 above for unknown partial image 
		 * 5. Apply comparison routine(s) to unknown 
		 *    1. Approach comparison problem as a longest common substring
		 *       problem using the string representation dump of the LGGraph 
		 *       based on the collection of metadata objects built 
		 *       when processing the views of the images 
		 *       -- dynamic programming approach is classical choice here
		 *    2. Score the operation as the following ratio (for all
		 *       views of the database image against the unknown image: 
		 *       max(
		 *         (length of largest substring found) /
		 *         (length of candidate view string))
		 *     3. Apply contour based matching routine
		 *     4. Score the operation
		 *     5. Use majority vote operator to find best match 
		 *  6. Display model image view with highest ratio value
		 *  7. Generate table of top ten values with model image and view filenames 
		 *  8. Display best match
		 *  9. Display procesing time 
		 *  
		 *  Original thought process has changed into:
		 *  1. Match based on using chain codes in a series of different string
		 *     similarity algorithms
		 *  2. Match based on use of Moments
		 *  
		 *  #1 has showed moderately interesting results
		 *  #2 has not shown good results as obstructions move moments significantly 
		 *     as the obstruction moves the segmentation 
	     **/		
		
		// Redirect console output to file to prevent java heap space issues with console
		
        // Creating a File object that represents the disk file.
		PrintStream o = null;
        try {
			o = new PrintStream(
					new File("/media/mrobbeloth/EOS_DIGITAL/console_"+System.currentTimeMillis()+".txt"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        // Assign o to output stream
        if (o != null) {
            
            // Use stored value for output stream	
        	System.setOut(o);
        }        
        
        System.out.println("Execution time:" + Calendar.getInstance());
		
		// OpenCV Initialization
		if (!args[0].equals(commands[0])) {
			// print the path just in case there is a problem loading various native libraries		
			System.out.println("trying to load: lib" + Core.NATIVE_LIBRARY_NAME + 
					           ".so");
			// load opencv library
			System.loadLibrary(Core.NATIVE_LIBRARY_NAME);		
		}
		
	    /* Initialize plplot and handle an override of the directory where the 
	      java wrapped plplot jni bindings library is located */
		System.out.println("plplot.libdir="+System.getProperty("plplot.libdir"));
		if (args.length > 1) {
			if (args[1].contains("plplot.libdir")) {
				int valueStartLoc = args[1].indexOf('=')+1;
				String value = args[1].substring(
						valueStartLoc, args[1].length());
				System.setProperty("plplot.libdir", value);
				System.out.println("plplot.libdir is now " + 
						System.getProperty("plplot.libdir"));
				imgCnt++;
			}
		}
		PLStream pls = new PLStream();
		
		// connect to the database
		DatabaseModule dbm = DatabaseModule.getInstance();
		System.out.println("Database Module initialized " + dbm.toString());
		
		if (args.length < 1) {
			StringBuilder sbCmds = new StringBuilder();
			for (String cmd : commands) {
				sbCmds.append(cmd + " | ");				
			}			
			System.out.println("Usage: java -jar " + 
							    ProjectController.class.getProtectionDomain().getCodeSource().getLocation().getFile() 
					            + "{" + sbCmds.toString() + "}" +   " [options plplot_libdir=] " + 
							    " image_1, image_2, ..., image_n");				
		}
		// --version
		else if (args[0].equals(commands[0])) {
			System.out.println("Version: " + VERSION);

			/* Report all the properties */
			System.out.println("*** SYSTEM PROPERTIES ***");
			Properties props = System.getProperties();
			Set<Entry<Object, Object>> values = props.entrySet();
			for(Entry<Object, Object> e : values) {
				System.out.println(e.getKey().toString() + 
						           "=" +e.getValue().toString());
			}
			System.out.println("*** END SYSTEM PROPERTIES ***");
			
			Map<String,String> entries = System.getenv();
			for (Entry<Object, Object> entry : values) {
				System.out.println(entry.getKey()+"="+entry.getValue());
			}
			
			/* Report basic characteristics about application */
			System.out.println("*** SYSTEM IMAGE CAPABILITIES ***");
			System.out.println(
			ApplicationInformation.reportOnSupportedImageReadingCapabilities());
			System.out.println(
			ApplicationInformation.reportOnSupportedImageWritingCapabilities());
			System.out.println("*** END SYSTEM IMAGE CAPABILITIES ***");
			
			/* Report the visualization library directory */	
			System.out.println("*** VISUALIZATION PROPERTIES ***");
			
			String libdir = System.getProperty("plplot.libdir");
			System.out.println("plplot.libdir="+libdir);
			
			if (pls != null) {
				StringBuffer sb = new StringBuffer();
				pls.gver(sb);
				System.out.println("PLPLOT Libary version: "+sb.toString());
				sb = new StringBuffer();
				pls.gdev(sb);
				System.out.println("PLPLOT Device: "+ 
				    ((sb.length() == 0) ? "Not defined" : sb.toString()));
				sb = new StringBuffer();
				pls.gfnam(sb);
				System.out.println("PLPLOT GFNAM: "+ 
				    ((sb.length() == 0) ? "Not defined" : sb.toString()));
			}
			
			System.out.println("*** END VISUALIZATION PROPERTIES ***");
		}
		// --process_model_image
		else if (args[0].equals(commands[1])){
			for(int i = 1; i < args.length; i++) {
				System.out.println("arg="+args[i]);
			}
			
			/* Process images imgCnt is defined earlier to handle
			 * optional arguments prior to list of inputs
			 * 
			 *  Processing to grayscale is based on the assumption
			 *  that color will not significantly improve our results
			 *  when trying to recognize images by chain codes and
			 *  centroids although it could have a place in future
			 *  work along with other elements like texture, for 
			 *  now it just adds to computational complexity */
			for (; imgCnt < args.length; imgCnt++) {
				Mat src = Imgcodecs.imread(args[imgCnt], 
						  Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
				
				// Prep to run LG algorithm
				Mat bestLabels = new Mat();				
				
				/* Terminate an algorithm based on an eplison of one
				 * or twenty iterations */
				TermCriteria criteria = new TermCriteria(
						TermCriteria.EPS+TermCriteria.MAX_ITER, 16, 1.0);			
				
				/* Segment and cluster via kmeans -- input data or pixels will
				 * be clustered around these centers.
				 * 
				 *  Divide the source data into two sets using the termination
				 *  criteria above as the means by which to stop segmentation
				 *  
				 *  K has a stronger influence with NGB K-means
				 *  
				 *  Only call the algorithm segmentation algorithm once, it's
				 *  slow enough as is already, but allow it to run 20 iterations
				 *  on the data
				 *  
				 *  args[imgCnt] simply passes the filename for labeling of 
				 *  intermediate files and final deliverable data
				 * 
				 * PP_CENTERS is a flag to use the Arthur and Vassilvitskii 
				 * [Arthur2007] center initialization -- alternatives are 
				 * random centers or user specified  
				 * 
				 * In this call we are processing an image for inclusion as
				 * a model image in the global database */
				long startTime = System.nanoTime();
				CompositeMat cm = 
						LGAlgorithm.LGRunME(src, 4, bestLabels, criteria, 
						 criteria.maxCount, 
						 Core.KMEANS_PP_CENTERS, 
						 args[imgCnt], 
			             Partitioning_Algorithm.OPENCV,
			             LGAlgorithm.Mode.PROCESS_MODEL, false);
				long endTime = System.nanoTime();
				long duration = (endTime - startTime);
				System.out.println("Model Processing Took: " + TimeUnit.SECONDS.convert(
						duration, TimeUnit.NANOSECONDS) + " seconds");
				System.out.println("Model Processing Took: " + TimeUnit.MINUTES.convert(
						duration, TimeUnit.NANOSECONDS) + " minute");
				
				/* Synthesize regions of Model Image*/
				startTime = System.nanoTime();
				CompositeMat SynSegmentMats = LGAlgorithm.Synthesize(cm, false);
				endTime = System.nanoTime();
				duration = (endTime - startTime);
				
				System.out.println("Synthesis Took: " + TimeUnit.SECONDS.convert(
						duration, TimeUnit.NANOSECONDS) + " seconds");
				System.out.println("Synthesis Took: " + TimeUnit.MINUTES.convert(
						duration, TimeUnit.NANOSECONDS) + " minute");
				
				/* Now apply LG algorithm to the synthesized segments */
				LGAlgorithm.localGlobal_graph(SynSegmentMats.getListofMats(), null, 
											  SynSegmentMats.getFilename(), 
						                      Partitioning_Algorithm.OPENCV, 
						                      LGAlgorithm.Mode.PROCESS_MODEL, 
						                      false, SynSegmentMats);
				
			}	
		}
		// --drop_model_database
		else if (args[0].equals(commands[2])){
			DatabaseModule.dropDatabase();
		}
		// --create_model_database
		else if (args[0].equals(commands[3])){
			DatabaseModule.createModel();
		}
		/* unit tests using handout (text) as source 
		 * NOTE: COMPONENT_LEVEL_MAX should be changed to 15 prior 
		 * to running and restored to 255 upon completion
		 * 
		 *  --test */
		else if (args[0].equals(commands[4])) {
			run_unit_tests(args);
		}
		// --dump_model_database
		/* Show a user friendly dump of the model database */
		else if (args[0].equals(commands[5])) {
			DatabaseModule.dumpModel();
		}
		// --find_match
		else if (args[0].equals(commands[6])) {
			System.out.println("Matching sample image to database");
			
			for(int i = 1; i < args.length; i++) {
				System.out.println("arg="+args[i]);
			}
			
			/* Process images imgCnt is defined earlier to handle
			 * optional arguments prior to list of inputs */
			for (;imgCnt < args.length; imgCnt++) {
				
				 /*  Processing to grayscale is based on the assumption
				  *  that color will not significantly improve our results
				  *  when trying to recognize images by chain codes and
				  *  centroids although it could have a place in future
				  *  work along with other elements like texture, for 
				  *  now it just adds to computational complexity */
				Mat src = Imgcodecs.imread(args[imgCnt], 
						  Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
				
				// Prep to run LG algorithm
				Mat bestLabels = new Mat();
				
				/* Terminate an algorithm based on an eplison of one
				 * or twenty iterations */
				TermCriteria criteria = new TermCriteria(
						TermCriteria.EPS+TermCriteria.MAX_ITER, 20, 1.0);			
				
				/* Segment and cluster via kmeans -- input data or pixels will
				 * be clustered around these centers.
				 * 
				 *  Divide the source data into two sets using the termination
				 *  criteria above as the means by which to stop segmentation
				 *  
				 *  K has a stronger influence with NGB K-means
				 *  
				 *  Only call the algorithm segmentation algorithm once, it's
				 *  slow enough as is already, but allow it to run 20 iterations
				 *  on the data
				 *  
				 *  args[imgCnt] simply passes the filename for labeling of 
				 *  intermediate files and final deliverable data
				 * 
				 * PP_CENTERS is a flag to use the Arthur and Vassilvitskii 
				 * [Arthur2007] center initialization -- alternatives are 
				 * random centers or user specified
				 * 
				 * In my implementation if the user specified flag is set
				 * a static method called setInitialLabelsGrayscale is accessed
				 * to create a uniformly distributed set of locations
				 * 
				 * I had an earlier belief this would create a better 
				 * apples-to-apples comparison across model and sample,
				 * but this is most likely not true due to how obstructions
				 * can redistribute the centroids, unless there is something
				 * else that I am doing wrong -- robbeloth 10/8/2016 				  
				 * 
				 * In this call we are processing a sample image for
				 * matching against the database */
				long startTime = System.nanoTime();
				LGAlgorithm.LGRunME(src, 4, bestLabels, criteria, 
						 criteria.maxCount, 
						 Core.KMEANS_PP_CENTERS, 
						 args[imgCnt], 
			             ProjectUtilities.Partitioning_Algorithm.OPENCV,
			             LGAlgorithm.Mode.PROCESS_SAMPLE, false);
				long endTime = System.nanoTime();
				long duration = (endTime - startTime);
				System.out.println("Took : " + TimeUnit.SECONDS.convert(
						duration, TimeUnit.NANOSECONDS) + " seconds");
				System.out.println("Took : " + TimeUnit.MINUTES.convert(
						duration, TimeUnit.NANOSECONDS) + "  minutes");
			}	
		}
		// --backup_database
		else if (args[0].equals(commands[7])) {
			File location = new File(args[1]);
			System.out.println("Backup up database to: " + 
							    location.getAbsolutePath());
			DatabaseModule.backupDatabase(new File(args[1]));
		}
		
		// release resources
		DatabaseModule.shutdown();
	}

	/**
	 * This is pretty much dead code now that was used during earlier classroom
	 * work and initial research 
	 * @param args
	 */
	private static void run_unit_tests(String[] args) {
		File fn = null;
		BufferedImage bImg = null;
		Raster r = null;
		BufferedImage oBImg = null;
		
		for (int imgCnt = 1; imgCnt < args.length; imgCnt++) {
			try {
				fn = new File(args[imgCnt]);
				bImg =  Imaging.getBufferedImage(fn);
				oBImg = ProjectUtilities.deepCopy(bImg);
			} catch (ImagingException e) {
				System.out.println("Apache Commons Imaging Returned: " 
			                       + e.getMessage() + "\n" + 
			                       e.getStackTrace());
				 try {
					bImg = ImageIO.read(fn);
					oBImg = ProjectUtilities.deepCopy(bImg);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} catch (IOException ioe) {
				 System.out.println(ioe.getMessage());
				 return;
			}
			
			//http://stackoverflow.com/questions/14958643/converting-bufferedimage-to-mat-in-opencv
			System.out.println("bImg:");
			bImg.toString();
			System.out.println(ImageInformation.reportBasicInformation(bImg));
			System.out.println(ImageInformation.reportOnColorModel(bImg));
			oBImg.toString();
			System.out.println(ImageInformation.reportBasicInformation(oBImg));
			System.out.println(ImageInformation.reportOnColorModel(oBImg));
			
			Mat src = Imgcodecs.imread(args[imgCnt], 
					  Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
			
			// Prep to run LG algorithm
			Mat bestLabels = new Mat();
			TermCriteria criteria = new TermCriteria(
					TermCriteria.EPS+TermCriteria.MAX_ITER, 20, 1.0);
			
			// Call LG algorithm
			/* For IMG5162:
			 *			 LGAlgorithm.LGRunME(src, 8, bestLabels, criteria, 1, 
					 Core.KMEANS_USE_INITIAL_LABELS|Core.KMEANS_PP_CENTERS, 
					 args[imgCnt], 
		             ProjectUtilities.Partioning_Algorithm.OPENCV);*/
			 LGAlgorithm.LGRunME(src, 2, bestLabels, criteria, 1, 
					 Core.KMEANS_PP_CENTERS, 
					 args[imgCnt], 
		             ProjectUtilities.Partitioning_Algorithm.OPENCV, 
		             LGAlgorithm.Mode.PROCESS_SAMPLE, false);
			
			/* For cell2.pgm 
			LGAlgorithm.LGRunME(dst, 6, bestLabels, criteria, 6, 
					            Core.KMEANS_RANDOM_CENTERS, 
					            args[imgCnt]);
					            */
		}
	}

}
