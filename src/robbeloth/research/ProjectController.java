package robbeloth.research;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
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
		BufferedImage image = null;
		File f = null;
		InputStream in = null;
		final String[] commands = {"--version", 
				                    "--process_model_image",
				                    "--drop_model_database",
				                    "--create_model_database",
				                    "--test",
				                    "--dump_model_database"};
		
		// Java library path information
		// print the path just in case there is a problem loading various native libraries
		System.out.println("Java Library Path:"+System.getProperty("java.library.path"));
		System.out.println("trying to load: lib" + Core.NATIVE_LIBRARY_NAME + 
				           ".so");
		
		// OpenCV Initialization
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
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
		 * 5. Apply comparison routine to unknown 
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
		 *  6. Display model image view with highest ratio value
		 *  7. Generate table of top ten values with model image and view filenames 
	     **/			
		
		if (args.length < 1) {
			StringBuilder sbCmds = new StringBuilder();
			for (String cmd : commands) {
				sbCmds.append(cmd + " | ");				
			}			
			System.out.println("Usage: java -jar " + 
							    ProjectController.class.getProtectionDomain().getCodeSource().getLocation().getFile() 
					            + "{" + sbCmds.toString() + "}" +   " image_1, image_2, ..., image_n");				
		}
		else {
			System.out.println("Trying to execute command " + args[0]);
		}
		
		
		if (args[0].equals(commands[0])) {
			System.out.println("Version: " + VERSION);
			
			/* Report basic characteristics about application */
			System.out.println(
			ApplicationInformation.reportOnSupportedImageReadingCapabilities());
			System.out.println(
			ApplicationInformation.reportOnSupportedImageWritingCapabilities());
		}
		else if (args[0].equals(commands[1])){
			for(int i = 1; i < args.length; i++) {
				System.out.println("arg="+args[i]);
			}
			
			/* Process images */
			for (int imgCnt = 1; imgCnt < args.length; imgCnt++) {
				Mat src = Imgcodecs.imread(args[imgCnt], 
						  Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
				
				// Prep to run LG algorithm
				Mat bestLabels = new Mat();
				TermCriteria criteria = new TermCriteria(
						TermCriteria.EPS+TermCriteria.MAX_ITER, 20, 1.0);			
				
				LGAlgorithm.LGRunME(src, 2, bestLabels, criteria, 1, 
						 Core.KMEANS_PP_CENTERS, 
						 args[imgCnt], 
			             ProjectUtilities.Partioning_Algorithm.OPENCV);
			}	
		}
		else if (args[0].equals(commands[2])){
			DatabaseModule.dropDatabase();
			DatabaseModule.shutdown();
		}
		else if (args[0].equals(commands[3])){
			DatabaseModule.createModel();
			DatabaseModule.shutdown();
		}
		/* unit tests using handout (text) as source 
		 * NOTE: COMPONENT_LEVEL_MAX should be changed to 15 prior 
		 * to running and restored to 255 upon completion */
		else if (args[0].equals(commands[4])) {
			run_unit_tests(args);
		}
		/* Show a user friendly dump of the model database */
		else if (args[0].equals(commands[5])) {
			DatabaseModule.dumpModel();
			DatabaseModule.shutdown();
		}
	}

	private static void run_unit_tests(String[] args) {
		File fn = null;
		BufferedImage bImg = null;
		Raster r = null;
		BufferedImage oBImg = null;
		BufferedImage cnvImg = null;
		
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
		             ProjectUtilities.Partioning_Algorithm.OPENCV);
			
			/* For cell2.pgm 
			LGAlgorithm.LGRunME(dst, 6, bestLabels, criteria, 6, 
					            Core.KMEANS_RANDOM_CENTERS, 
					            args[imgCnt]);
					            */
		}
	}

}
