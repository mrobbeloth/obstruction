package robbeloth.research;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDJpeg;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat6;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
* This class provides base class for all shared image operator classes
* This class is abstract and must be instantiated with a child class
* @author Michael Robbeloth
* @category Projects
* @since 2/7/2015 
* @version 1.0
* <br/><br/>
* Class: CEG7900<br/>
* <h2>Revision History</h2><br/>
* <b>Date						Revision</b>
*    2/7/2014                   Initial Version
*                               Original CS7900 AlgorithmGroup
*/
public class ProjectUtilities {

	/**
	 * Enumeration to specify the title of partitioning algorithm to use
	 * <br/><br/>
	 * OPENCV -- OpenCV KMeans (there is also a separate partitioning 
	 * routine not supported by this code)
	 * <br/>
	 * NGB -- KMeans algorithm by N. Bourbakis (originally in MATLAB code)
	 * <br/>
	 * NONE -- Do use any partitioning algorithm
	 * <br/>
	 * @author mrobbeloth
	 *
	 */
	public static enum Partitioning_Algorithm {
		OPENCV,
		NGB,
		NONE
	}
	
	/**
	 * Append a string to a filename and create a new extension to a file 
	 * object
	 * <br/>
	 * if you don't want to append a string, just set the extension part
	 * <br/>
	 * @param fn -- original file
	 * @param appendStr -- modified text to add to file name
	 * @param extension -- file extension of new file
	 * @return a new abstract representation of some new file
	 */
	public static File modifyFileName(File fn, String appendStr, String extension) {
		long dt = new Date().getTime();
		String fnName = null;
		try {			
			// find where the current extension starts
			String canonicalPath = fn.getCanonicalPath();
			int lastIndex = canonicalPath.lastIndexOf(".");
			
			// Append string and extension or just extension
			if ((appendStr != null) && (appendStr.length() > 0)) {
				fnName = canonicalPath.substring(0, lastIndex) 
						 + "_"  + appendStr + "_" 
						 + "_" + dt + "_." + extension;
			}
			else {
				fnName = canonicalPath.substring(0, lastIndex) 
						 + "_" + dt + "_." + extension;					
			}
		} catch (IOException e) {
			 System.out.println(e.getMessage());
			 return null;
		}
		
		// Return file object with new filename 
		File outputFile = new File(fnName);
		return outputFile;
	}
	
	/**
	 * Write all image formats to disk supported by the system
	 * @param bImg -- image with accessible data buffer
	 * @param fn -- filename to use
	 * @param appendStr -- text to append to filename
	 */
	public static void writeImagesToDisk(BufferedImage bImg, File fn, String appendStr) {
		/* retrieve list of all image file suffixes registered with different image formats
		   on the system */
		String[] formatsToWrite = ImageIO.getWriterFileSuffixes();
		
		/* Process each registered image format extension */
		for (int i = 0; i < formatsToWrite.length; i++) {
			long dt = new Date().getTime();
			String fnName = null;
			try {
				// Set the file name for the ith registered extension
				String canonicalPath = fn.getCanonicalPath();
				int lastIndex = canonicalPath.lastIndexOf(".");
				if ((appendStr != null) && (appendStr.length() > 0)) {
					fnName = canonicalPath.substring(0, lastIndex) 
							 + "_"  + appendStr + "_" 
							 + "_" + dt + "_." + formatsToWrite[i];
				}
				else {
					fnName = canonicalPath.substring(0, lastIndex) 
							 + "_" + dt + "_." + formatsToWrite[i];					
				}
			} catch (IOException e) {
				 System.out.println(e.getMessage());
				 return;
			}
			
			/* write the image file out to disk for the ith 
			   registered extension */
 			File outputFile = new File(fnName);
			try {
				ImageIO.write(bImg, formatsToWrite[i], outputFile);
			} catch (IOException e) {
				 System.out.println(e.getMessage());
				 return;
			}
		}
	}
	
	/**	
	 * Write image to disk with a specific format
	 * @param bImg -- image with accessible data buffer
	 * @param fn -- filename to use
	 * @param appendStr -- text to append to filename
	 * @param format -- type of image to write (jpg, png, gif, etc.)
	 */
	public static void writeImagesToDisk(BufferedImage bImg, File fn, String appendStr, String format) {
		// get the current date and time in seconds since 1/1/1970 format
		long dt = new Date().getTime();
		String fnName = null;
		try {
			/* modify the filename as needed with optional
			 * appending string and extension format */
			String canonicalPath = fn.getCanonicalPath();
			int lastIndex = canonicalPath.lastIndexOf(".");
			if ((appendStr != null) && (appendStr.length() > 0)) {
				fnName = canonicalPath.substring(0, lastIndex) 
						 + "_"  + appendStr + "_" 
						 + "_" + dt + "_." + format;
			}
			/* Nothing to add to the end of the original filename, 
			 * just add the new extension */
			else {
				fnName = canonicalPath.substring(0, lastIndex) 
						 + "_" + dt + "_." + format;					
			}
		} catch (IOException e) {
			 System.out.println(e.getMessage());
			 return;
		}
		
		/* Call a different method to write jpeg images */
		File outputFile = new File(fnName);
		if (format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("jpg")) {
			writeJPEGImagestoDisk(bImg, outputFile);
		}
		else {
			/* Use javax I/O to write non-JPEG formats to disk*/
			try {
				ImageIO.write(bImg, format, outputFile);
			} catch (IOException e) {
				 System.out.println(e.getMessage());
				 return;
			}			
		}
	}

	/**
	 * Largely adopted from reference source to provide 100% quality storage in jpeg format
	 * Reference: 
	 * @see http://stackoverflow.com/questions/17108234/setting-jpg-compression-level-with-imageio-in-java
	 * by John Szakmeister 
	 * @param bImg -- Buffered image data object (image with color data and raster)
	 * @param outputFile -- filename for image data to use
	 * @return nothing (post-condition -- image written to disk at 100% quality)
	 */
	private static void writeJPEGImagestoDisk(BufferedImage bImg, File outputFile) {
		// use IIORegistry to get the available services
		IIORegistry registry = IIORegistry.getDefaultInstance();
		
		// return an iterator for the available ImageWriterSpi for jpeg images
		Iterator<ImageWriterSpi> services = registry.getServiceProviders(ImageWriterSpi.class,
		                                                 new ServiceRegistry.Filter() {   
		        public boolean filter(Object provider) {
		            if (!(provider instanceof ImageWriterSpi)) return false;

		            ImageWriterSpi writerSPI = (ImageWriterSpi) provider;
		            String[] formatNames = writerSPI.getFormatNames();
		            for (int i = 0; i < formatNames.length; i++) {
		                if (formatNames[i].equalsIgnoreCase("JPEG")) {
		                    return true;
		                }
		                else {
		                	System.out.println(
		                			"writeJPEGImagestoDisk(): Skipping " + 
		                	        formatNames[i]);
		                }
		            }

		            return false;
		        }
		    },
		   true);
		
		//...assuming that servies.hasNext() == true, 
		// jszakmeister gets the first available service.
		ImageWriterSpi writerSpi = services.next();
		ImageWriter writer = null;
		try {
			writer = writerSpi.createWriterInstance();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// specifies where the jpg image has to be written
		try {
			writer.setOutput(new FileImageOutputStream(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// writes the file with given compression level -- 100% is 1f
		// from your JPEGImageWriteParam instance
		JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
		jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(1f);
		try {
			writer.write(null, new IIOImage(bImg, null, null), jpegParams);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Make a full copy of all attributes and data of a buffered image object
	 * (including color model and raster data) into a new buffered image
	 * data object
	 * @see http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
	 * by Klark
	 * @param bi -- image data object to copy
	 * @return a new image data object (e.g., pointers don't point to the same
	 * object in memory)
	 */
	public static BufferedImage deepCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
	
	/**
	 *  Write a two image pdf to disk with captions
	 * @param fn -- file to write to
	 * @param title -- caption text (optional, pass null to use default)
	 * @param author -- author of doc (optional, pass null to set
	 * Michael Robbeloth)
	 * @param creator -- creator of PDF (optional, pass null to set
	 * PDFBox)
	 * @param producer -- the class this pdf is being generated for
	 * (optional, pass null to set CS 8920)
	 * @param keywords -- keywords to set in the pdf (optional,
	 * pass null to use "obstruction identify simple image hidden")
	 * @param oImg -- original image
	 * @param bImg -- modified image
	 */
	public static void writePDFtoDisk(
			File fn, String title, String author, 
			String creator, String producer,
			String keywords,
			BufferedImage oImg, BufferedImage bImg) {
		// create a new empty document
		PDDocument doc = new PDDocument();
		
		// Set document metadata
		PDDocumentInformation info = new PDDocumentInformation();
		if ((author != null) && (author.length() > 0)) {
			info.setAuthor(author);
		}
		else {
			info.setAuthor("Michael Robbeloth");	
		}
		if ((creator != null) && (creator.length() > 0)) {
			info.setCreator(creator);
		}
		else {
			info.setCreator("PDFBox");	
		}
		
		if ((title != null) && (title.length() > 0)) {
			info.setTitle(title);	
		}
		else {
			info.setTitle("Before and After Images");
		}
		
		if ((producer != null) && (producer.length() > 0)) {
			info.setProducer(producer);
		}
		else {
			info.setProducer(
					"CS8920 - Independent Studies -- Dr. Bourbakis");
		}
		
		if ((keywords != null) && (keywords.length() > 0)) {
			info.setKeywords(keywords);
		}
		else {
			info.setKeywords(
					"obstruction identify simple image hidden");	
		}
		
		/* program the current date and time along with metadata 
		 * into the object that is to be written to disk */
		info.setCreationDate(Calendar.getInstance()); 
		doc.setDocumentInformation(info);
		
		// add a new blank page and add it to the document
		PDPage newPg = new PDPage();
		doc.addPage(newPg);
		
		// Create a new font object selecting one of the PDF base fonts
		PDFont font = PDType1Font.TIMES_ROMAN;
		
		// Start a new content stream which will "hold" the to be created content
		PDPageContentStream cs = null;
		try {
			cs = new PDPageContentStream(doc, newPg);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/* Place the modified image and text, note we work up from the 
		 * bottom of the page when constructing it */
		PDXObjectImage xImg = null;
		try {
			xImg = new PDJpeg(doc, bImg);
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
		try {
			cs.drawXObject(xImg, 0, 40, 384, 384);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.beginText();
			cs.setFont(font, 10);
			cs.moveTextPositionByAmount(0, 30);
			cs.drawString("Modified: " + title);
			cs.endText();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		 * Place the original image and text
		 */
		try {
			xImg = new PDJpeg(doc, oImg);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.drawXObject(xImg, 0, 440, 384, 384);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.beginText();
			cs.setFont(font, 10);
			cs.moveTextPositionByAmount(0, 427);
			cs.drawString("Original: " + title);
			cs.endText();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// close the content stream before writing file to disk
		try {
			cs.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Save the newly created document
		try {			
			if (fn.toString().endsWith(".pdf")) {
				doc.save(fn.toString());
			}
			else {
				doc.save(fn.toString() + ".pdf");
			}
		} catch (COSVisitorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// release resources
		try {

			doc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Write a three image pdf to disk with captions
	 * @param fn -- file to write to
	 * @param title -- caption text
	 * @param oImg1 -- original image source
	 * @param oImg2 -- original image destination
	 * @param bImg -- merged image with operator applied
	 */
	public static void writePDFtoDiskDblSrc(
			File fn, String title, String author, 
			String creator, String producer,
			String keywords, BufferedImage oImg1, 
			BufferedImage oImg2, BufferedImage bImg) {
		// create a new empty document
		PDDocument doc = new PDDocument();
		
		// Set document metadata
		PDDocumentInformation info = new PDDocumentInformation();
		if ((author != null) && (author.length() > 0)) {
			info.setAuthor(author);
		}
		else {
			info.setAuthor("Michael Robbeloth");	
		}
		if ((creator != null) && (creator.length() > 0)) {
			info.setCreator(creator);
		}
		else {
			info.setCreator("PDFBox");	
		}
		
		if ((title != null) && (title.length() > 0)) {
			info.setTitle(title);	
		}
		else {
			info.setTitle("Before and After Images");
		}
		
		if ((producer != null) && (producer.length() > 0)) {
			info.setProducer(producer);
		}
		else {
			info.setProducer(
					"CS8920 - Independent Studies -- Dr. Bourbakis");
		}
		
		if ((keywords != null) && (keywords.length() > 0)) {
			info.setKeywords(keywords);
		}
		else {
			info.setKeywords(
					"obstruction identify simple image hidden");	
		}
		info.setCreationDate(Calendar.getInstance()); 
		doc.setDocumentInformation(info);
		
		// add a new blank page and add it to the document
		PDPage newPg = new PDPage();
		doc.addPage(newPg);
		
		// Create a new font object selecting one of the PDF base fonts
		PDFont font = PDType1Font.TIMES_ROMAN;
		
		// Start a new content stream which will "hold" the to be created content
		PDPageContentStream cs = null;
		try {
			cs = new PDPageContentStream(doc, newPg);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/* Place the modified image and text, note we work up from the 
		 * bottom of the page when constructing it */
		PDXObjectImage xImg = null;
		try {
			xImg = new PDJpeg(doc, bImg);
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
		try {
			cs.drawXObject(xImg, 0, 50, 384, 384);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.beginText();
			cs.setFont(font, 10);
			cs.moveTextPositionByAmount(0, 40);
			cs.drawString("Modified Image: " + title);
			cs.endText();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		/*
		 * Place the original image and text
		 */
		try {
			xImg = new PDJpeg(doc, oImg1);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.drawXObject(xImg, 0, 450, 256, 256);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			xImg = new PDJpeg(doc, oImg2);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.drawXObject(xImg, 260, 450, 256, 256);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		try {
			cs.beginText();
			cs.setFont(font, 10);
			cs.moveTextPositionByAmount(0, 427);
			cs.drawString("Original Images: " + title);
			cs.endText();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// close the content stream before writing file to disk
		try {
			cs.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Save the newly created document
		try {			
			if (fn.toString().endsWith(".pdf")) {
				doc.save(fn.toString());
			}
			else {
				doc.save(fn.toString() + ".pdf");
			}
		} catch (COSVisitorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// release resources
		try {

			doc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Collect all the pdfs in the directory and merge them together
	 * @param dir -- directory to operate on
	 * @return whether or not the merge completed successfully
	 */
	public static boolean mergePDFs(File dir) {
		// We are dealing with a directory, right?
		if (!dir.isDirectory()) {
			return false;
		}
		
		// Collect a list of just pdf extension files (not looking at each file's magic)
		FileFilter ff = new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.toString().endsWith(".pdf");
			}
			
		};
		File[] files = dir.listFiles(ff);		
		
		// Apply a time created comparator, need to delay each operator so that pdfs are created
		// in the order the operators were applied 
		if (files.length > 0) {
			Arrays.sort(files, new Comparator<Object>() {

				public int compare(Object o1, Object o2) {
					if ((o1 instanceof File) && (o2 instanceof File)) {
						File f1 = (File)o1;
						File f2 = (File)o2;
						Long l1 = f1.lastModified();
						Long l2 = f2.lastModified();
						if (l1 < l2) {
							return -1;
						}
						else if (l1 > l2) {
							return 1;
						}
						else {
							return 0;
						}
					}
					return 0;
				}
			});
		}
		
		// initialize the PDFBox utilities
		PDFMergerUtility pdfMU = new PDFMergerUtility();
		
		// set dest
		pdfMU.setDestinationFileName(dir.toString()+File.separator+"merged.pdf");
		
		// add in pdf sources
		for(int i = 0; i < files.length; i++) {
			File f = files[i];
			pdfMU.addSource(f);
		}
		
		// generate final merged pdf
		try {
			pdfMU.mergeDocuments();
		} catch (COSVisitorException e) {
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * A necessary evil to ensure that the pdfs are created and later 
	 * collected in the order the operators were applied in the control class
	 * @param milliseconds -- nubmer of milliseconds to delay program execution
	 */
	public static void delayMe(long milliseconds) {
		// to allow pdfs to be created in sequence, 1/10 delay between iterations
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Generate a Powerpoint presentation document (pptx) with each pdf on
	 * its own slide 
	 * @param dir -- directory to place final presentation
	 * @param title -- title for presentaiton (optional, set null
	 * to use CS8920 Independent Study)
	 * @param author -- author of presentation (optional, set null
	 * to use Michael Robbeloth)
	 * @param author -- author of presentation
	 * @return
	 */
	public static boolean generatePresentation(File dir, String title, 
			String author) {
		// We are dealing with a directory, right?
		if (!dir.isDirectory()) {
			return false;
		}
		
		// Collect a list of just pdf extension files (not looking at each file's magic)
		FileFilter ff = new FileFilter() {

			public boolean accept(File pathname) {
				return pathname.toString().endsWith(".jpg");
			}
			
		};
		File[] files = dir.listFiles(ff);
		
		
		// Apply a time created comparator, need to delay each operator so that pdfs are created
		// in the order the operators were applied 
		if (files.length > 0) {
			Arrays.sort(files, new Comparator<Object>() {

				public int compare(Object o1, Object o2) {
					if ((o1 instanceof File) && (o2 instanceof File)) {
						File f1 = (File)o1;
						File f2 = (File)o2;
						Long l1 = f1.lastModified();
						Long l2 = f2.lastModified();
						if (l1 < l2) {
							return -1;
						}
						else if (l1 > l2) {
							return 1;
						}
						else {
							return 0;
						}
					}
					return 0;
				}
			});
		}
		
		// Create new Presentation 
		XMLSlideShow presentation  = new XMLSlideShow();
		
		// Add first slide
        XSLFSlideMaster master = presentation.getSlideMasters().get(0);
        XSLFSlideLayout layout1 = master.getLayout(SlideLayout.TITLE);
		XSLFSlide titleSlide = presentation.createSlide(layout1);
		XSLFTextShape[] ph1 = titleSlide.getPlaceholders();
		XSLFTextShape tp1 = ph1[0];
		if ((title != null) && (title.length() > 0)) {
			tp1.setText(title);
		}
		else {
			tp1.setText("CS8920 Independent Study Presentation");	
		}
		
		XSLFTextShape tp2 = ph1[1];
		
		if ((author != null) && (author.length() > 0)) {
			tp2.setText(author + "\n"  + 
		            new Date(System.currentTimeMillis()));
		}
		else {
			tp2.setText("By Michael Robbeloth \n" + 
		            new Date(System.currentTimeMillis()));			
		}
		
		XSLFSlide picSlide = null;
		Map<String, Byte[]> masterImages = 
				new HashMap<String, Byte[]>();
		for(int i = 0; i < files.length; i++) {
			File f = files[i];
			byte[] picData = null;
			try {
				picData = IOUtils.toByteArray(
						new FileInputStream(f));				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			BufferedImage bi = null;
			try {
				bi = ImageIO.read(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Graphics g = bi.getGraphics();
			g.setClip(0, 0, 512, 512);
			XSLFSlideLayout layout2 = 
					master.getLayout(SlideLayout.TITLE_AND_CONTENT);
			picSlide = presentation.createSlide(layout2);
			XSLFTextShape[] ph = picSlide.getPlaceholders();
			tp1 = ph[0];
			String theFileName = f.getName();
			int whereToStrip = theFileName.indexOf("__");
			String strippedText = null;
			String keyToUse = null;
			if (whereToStrip != -1) {
				strippedText = theFileName.substring(0, whereToStrip);
				Set<String> masterFileNames = masterImages.keySet();
				Iterator<String> it = 
						masterFileNames.iterator();
				while (it.hasNext()) {
					String daFileName = it.next();					
					if (strippedText.contains(daFileName)) {						
						keyToUse = daFileName;
					}
				}
			}
			else {
				strippedText = theFileName;
				int noExtensionLoc = theFileName.indexOf(".jpg");
				String noExtensionFn = theFileName.substring(0, noExtensionLoc);
				Byte[] byteObjects = new Byte[picData.length];
				int toByteCnt = 0;
				for(byte b: picData)
					   byteObjects[toByteCnt++] = b;
				masterImages.put(noExtensionFn, byteObjects);
			}
			String replacementText = strippedText.replace("_", " ");
			
			tp1.setText(
					(replacementText != null) ? replacementText : strippedText);
			if (picData != null) {
				XSLFPictureData idx = presentation.addPicture(picData, 
						PictureData.PictureType.JPEG);
				
				Byte[] byteObjects = null;
				byte[] bytes;
				int j = 0;
				XSLFPictureData masterIdx = null;
				if (keyToUse != null) {
					byteObjects = masterImages.get(keyToUse);
					bytes = new byte[byteObjects.length];
					j=0;
					// Unboxing byte values. (Byte[] to byte[])
					for(Byte b: byteObjects)
					    bytes[j++] = b.byteValue();
					masterIdx = presentation.addPicture(bytes, 
							 PictureData.PictureType.JPEG);
					
					XSLFPictureShape picMaster = picSlide.createPicture(masterIdx);
					picMaster.setAnchor(new Rectangle(0, 100, 357, 440));
					
					XSLFPictureShape pic = picSlide.createPicture(idx);
					pic.setAnchor(new Rectangle(360, 100, 355, 440));
				}
				else {
					XSLFPictureShape pic = picSlide.createPicture(idx);
					pic.setAnchor(new Rectangle(0, 100, 715, 440));
				}

				
				// this is placeholder text that shows up for some reason
				XSLFTextShape tp3 = ph[1];
				tp3.setText("");
			}

		}
		
		// Write presentation to disk
		try {
			presentation.write(
					new FileOutputStream(dir.toString() + File.separator+ "merged.pptx"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			presentation.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}	
	
	/**
	 * Convert 1D integer array into 2D integer square matrix
	 * @param p -- input array 
	 * @param width -- width of a column (e.g., num. elements in a row)
	 *                  whose root must be square
	 * @return 2D square matrix
	 */
	public static int[][] Convert1DArrayto2DMatrix(int[] p, int width) {
		int q[][] = new int[width][width];
		
		// Make sure we can create a square matrix
		boolean isWidthSquare = ProjectUtilities.isPerfectSquare(width);
		if (!isWidthSquare) {
			return null;
		}
		
		// convert elements
		for (int y = 0; y < width; y++) {
			for (int x = 0; x < width; x++) {
				q[y][x] = p[x+(width*y)];
			}
		}
		
		// return new 2D square integer array
		return q;
	}
	
	/**
	 * Convert 1D integer array into 2D integer matrix
	 * 
	 * @param p -- input array
	 * @param rows -- number of rows for matrix
	 * @param cols -- number of columns for matrix
	 * @return 2D integer matrix
	 */
	public static int[][] Convert1DArrayto2DMatrix(int[] p, int rows, int cols) {
		int q[][] = new int[rows][cols];
		
		// convert elements
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				q[y][x] = p[x+(cols*y)];
			}
		}
		
		// return new 2D matrix
		return q;
	}
	
	/**
	 * Convert 1D array into double precision floating point 2D matrix
	 * @param p -- input data
	 * @param width -- create square matrix of size width
	 * @return new double precision floating point 2D matrix
	 */
	public static double[][] Convert1DArrayto2DMatrixD(int[] p, int width) {
		double q[][] = new double[width][width];
		
		// convert elements
		for (int y = 0; y < width; y++) {
			for (int x = 0; x < width; x++) {
				q[y][x] = p[x+(width*y)];
			}
		}
		
		// return new 2D array
		return q;
	}
	
	/**
	 * Convert 1D double precision floating point array into double 
	 * precision floating point 2D matrix
	 * 
	 * @param p -- input data
	 * @param width -- create square matrix of size width
	 * @return new double precision floating point 2D matrix
	 */
	public static double[][] Convert1DArrayto2DMatrixD(double[] p, int width) {
		double q[][] = new double[width][width];
		
		boolean isWidthSquare = ProjectUtilities.isPerfectSquare(width);
		if (!isWidthSquare) {
			return null;
		}
		
		// convert elements
		for (int y = 0; y < width; y++) {
			for (int x = 0; x < width; x++) {
				q[y][x] = p[x+(width*y)];
			}
		}
		
		// return new 2D array
		return q;
	}
	
	/**
	 * Convert 2D matrix into 1D array
	 * @param p -- input square matrix
	 * @return 1D array
	 */
	public static int[] Convert2DMatrixto1DArray(int[][] p) {
		
		// calc size for final array
		int total = 0; 
		for (int i = 0; i < p.length; i++) {
			total += p[i].length; 
		}
		int q[] = new int[total];
		
		// convert elements
		for (int y = 0; y < p.length; y++) {
			for (int x = 0; x < p.length; x++) {
				q[x+(y*p.length)] = p[y][x];
			}
		}
		return q;
	}
	
	/**
	 * Convert 2D integer array into 1D integer array
	 * 
	 * @param p -- input 2D integer array
	 * @param rows -- number of rows in input array
	 * @param cols -- number of columns in input array 
	 * @return converted 1D integer array
	 */
	public static int[] Convert2DMatrixto1DArray(int[][] p, int rows, int cols) {
		// allocate space for new 1D integer array that is rows * cols in isze
		int q[] = new int[rows * cols];
		
		// copy elements to new array
		for(int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				q[i*cols+j] = p[i][j];
			}
		}
		
		// return new 1D integer array
		return q;
	}
	
	/**
	 * Convert 2D double array into 1D integer array
	 * @param p -- input data
	 * @return new 1D integer array
	 */
	public static int[] Convert2DMatrixDto1DArray(double[][] p) {
		
		// calc size for final array
		int total = 0; 
		for (int i = 0; i < p.length; i++) {
			total += p[i].length; 
		}
		
		// allocate space for new 1D integer array
		int q[] = new int[total];
		
		// convert elements
			for (int y = 0; y < p.length; y++) {
				for (int x = 0; x < p.length; x++) {
					q[x+(y*p.length)] = (int) p[y][x];
				}
			}
			
			// return converted 1D integer array
			return q;
		}
	
	/**
	 * Convert a buffered image object one color mode to another color mode. Note
	 * the destinated color mode needs to exist (no checking is done in this method)
	 * @see http://stackoverflow.com/questions/6881578/java-converting-between-color-models
	 * by eric232322
	 * @param src -- original image data object
	 * @param bufImgType -- new color model type 
	 * @return image data object with new color model applied
	 */
	public static BufferedImage convert(BufferedImage src, int bufImgType) {
		// Create new buffered image data object with new color model  
	    BufferedImage img= new BufferedImage(
	    							src.getWidth(), src.getHeight(),
	    							bufImgType);
	    
	    // copy original image raster contents into new buffered image object
	    Graphics2D g2d= img.createGraphics();
	    g2d.drawImage(src, 0, 0, null);
	    g2d.dispose();
	    
	    // return new buffered image object
	    return img;
	}
	
	/**
	 * 
	 * @param cutout
	 * @param labels
	 * @param centers
	 * @return
	 */
	public static List<Mat> showClusters (Mat cutout, Mat labels, Mat centers) {
		centers.convertTo(centers, CvType.CV_8UC1, 255.0);
		centers.reshape(3);
		List<Mat> clusters = new ArrayList<Mat>();
		for(int i = 0; i < centers.rows(); i++) {
			clusters.add(Mat.zeros(cutout.size(), cutout.type()));
		}
		
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		for(int i = 0; i < centers.rows(); i++) counts.put(i, 0);
			int rows = 0;
			for(int y = 0; y < cutout.rows(); y++) {
				for(int x = 0; x < cutout.cols(); x++) {
					int label = (int)labels.get(rows, 0)[0];
					int r = (int)centers.get(label, 2)[0];
					int g = (int)centers.get(label, 1)[0];
					int b = (int)centers.get(label, 0)[0];
					counts.put(label, counts.get(label) + 1);
					clusters.get(label).put(y, x, b, g, r);
					rows++;
				}
			}
		return clusters;
	}
	
	public static Mat round(Mat input) {
		Mat output = 
				new Mat(
						input.rows(), input.cols(), input.type(), 
						Scalar.all(0));
		for (int i = 0; i < input.rows(); i++) {
			for (int j = 0; j < input.cols(); j++) {
				double value = input.get(i, j)[0];
				double roundedValue = Math.round(value);
				output.put(i, j, roundedValue);
			}
		} 
		return output;
	}
	
	public static Mat multiplyScalar(Mat m, double i)
	{
	    return m = m.mul(new Mat((int)m.size().height, 
	    		                  (int)m.size().width,
	    		                  m.type(), new Scalar(i)));
	}
	
	/**
	 * Limited version of findInMat operator from MatLab. Basically
	 * find k instances of a non-zero entry in an input array
	 * starting from the beginning or end of the input array 
	 * @param input -- data to find non-zero values in
	 * @param k -- number of entries to find
	 * @param direction -- start at beginning "first" or end "last"
	 * @return -- the list of non-zero indices from input data
	 * x = row; y=col
	 */
	public static ArrayList<Point> findInMat(
			Mat input, int k, String direction) {
		ArrayList<Point> locNonZeroElements = new ArrayList<Point>();
		// System.out.println(input.dump());
		if (direction.equals("first")) {
			for (int i = 0; i < input.rows(); i++) {
				for (int j = 0; j < input.cols(); j++) {
					double[] valueArray = input.get(i,j);
					double value = valueArray[0];
					if (value != 0) {
						Point p = new Point(i,j);
						locNonZeroElements.add(p);
					}
					
					if (locNonZeroElements.size() == k) {
						return locNonZeroElements;
					}
				}
			}			
		}
		else if (direction.equals("last")){
			for (int i = input.rows()-1; i >= 0; i--) {
				for (int j = input.cols()-1; j >= 0; j--) {
					if (input.get(i,j)[0] != 0) {
						Point p = new Point(i,j);
						locNonZeroElements.add(p);
					}
					
					if (locNonZeroElements.size() == k) {
						return locNonZeroElements;
					}
				}
			}					
		}

		return locNonZeroElements;
	}
	
	public static int[][] convertMatToIntArray(Mat p){
		int rows = p.rows();
		int cols = p.cols();
		int[][] q = new int[rows][cols];

		if (p.channels() > 1) {
			System.out.println("Warning: more than one channel, "
					         + "color information will be lost");
		}
		
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				q[i][j] = (int) p.get(i,j)[0];
			}
		}
		return q;
	}
	
	public static double[][] convertMatToDoubleArray(Mat p){
		int rows = p.rows();
		int cols = p.cols();
		double[][] q = new double[rows][cols];

		if (p.channels() > 1) {
			System.out.println("Warning: more than one channel, "
					         + "color information will be lost");
		}
		
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				q[i][j] = (double) p.get(i,j)[0];
			}
		}
		return q;
	}
	
	public static byte[][] convertMatToByteArray(Mat p){
		int rows = p.rows();
		int cols = p.cols();
		byte[][] q = new byte[rows][cols];

		if (p.channels() > 1) {
			System.out.println("Warning: more than one channel, "
					         + "color information will be lost");
		}
		
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				q[i][j] = (byte) p.get(i,j)[0];
			}
		}
		return q;
	}
	
	public static Mat convertInttoGrayscaleMat(int[][] p, int rows, 
			                                     int cols) {
		Mat q = new 
				Mat(rows, cols, CvType.CV_64FC1, Scalar.all(0d));
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				q.put(i, j, new double[]{p[i][j]});
			}
		}
		return q;
	}
	
	public static Mat convertDoubletoGrayscaleMat(double[][] p, int rows, 
            int cols) {
		Mat q = new 
				Mat(rows, cols, CvType.CV_64FC1, Scalar.all(0d));
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				q.put(i, j, new double[]{p[i][j]});
			}
		}
		return q;
	}	
	
	/*
	 *http://stackoverflow.com/questions/11734322/matlab-type-arrays-in-c
	 *genearateRange and linspace based on this reference with modifications
	 *for java  
	 */
	/**
	 * Generate a sequence of values from a to c with interval b
	 * @param a -- start of range
	 * @param b -- interval
	 * @param c -- end of range
	 * @return floating point vector with sequence of values
	 */
	public static Vector<Double> generateRange(double a, double b, double c) {
		Vector<Double> array = new Vector<Double>();
		while (a <= c) {
			array.add(a);
			a += b;
		}
		return array;
	}
	
	/**
	 * Generate a sequence of values from a to c with interval b
	 * @param a -- start of range
	 * @param b -- interval
	 * @param c -- end of range
	 * @return OpenCV matrix with sequence of values
	 */
	public static Mat generateRange_Mat(double a, double b, double c) {
		Vector<Double> array = ProjectUtilities.generateRange(a, b, c);
		int size = array.size();
		Mat arrayMat = new Mat(1, size, CvType.CV_64FC1);
		for( int i = 0; i < size; i++) {
			arrayMat.put(1, i, array.get(i));
		}
		return arrayMat;
	}
	
	/**
	 * Take two values and return them as a floating point vector
	 * 
	 * @param a -- the first value of the matrix
	 * @param b -- the second value of the matrix
	 * @param n -- number of elements in the matrix (can be more than 2)
	 * @return vector with two values inserted
	 */	
	public static Vector<Double> linspace(double a, double b, int n) {
		Vector<Double> array = new Vector<Double>();
		double step = (b - a) / (n - 1);
		
		for(int i = 0; i < n; i++) {
			array.add(a);
			a += step;
		}
		/*while(a <= b) {
			array.add(a);
			a += step;
		}*/
		return array;
	}
	
	/**
	 * Take two values and return them as a new OpenCV matrix
	 * 
	 * @param a -- the first value of the matrix
	 * @param b -- the second value of the matrix
	 * @param n -- number of elements in the matrix (can be more than 2)
	 * @return matrix with two values inserted
	 */
	public static Mat linspace_Mat(double a, double b, int n) {
		Vector<Double> array = ProjectUtilities.linspace(a, b, n);
		int size = array.size();
		Mat arrayMat = new Mat(1, size, CvType.CV_64FC1, 
				               Scalar.all(0));
		for( int i = 0; i < size; i++) {
			double value = array.get(i).doubleValue();
			arrayMat.put(0, i, value);
		}
		return arrayMat.clone();
	}
	
	/**
	 * Convert subscripts of matrix into index for 1D addressing
	 * @param row -- row of 2D matrix
	 * @param col -- column of 2D matrix
	 * @param rows -- number of rows in 2D matrix
	 * @param cols -- number of columns in 2D matrix
	 * @return index of matrix in 1D addressing format
	 */
	public static int sub2ind(int row, int col, int rows, int cols) {
		int index = -1;
		if ((row < rows) && (col < cols)) {
			index = row*cols+col;
		}
		return ((index < rows*cols) ? index : -1);
	}
	
	/**
	 * Convert index into 2D subscript
	 * @param sub -- index of matrix in 1D vector form
	 * @param rows -- number of rows in 2D matrix table
	 * @param cols -- number of cols in 2D matrix table
	 * @return -- p.y is col p.x is row
	 */
	public static Mat ind2sub(int sub, int rows, int cols) {
		Mat m = new Mat(1, 2, CvType.CV_64FC1);
		if (cols != 0) {
			m.put(0, 0, sub / cols);
			m.put(0, 1, sub % cols);			
		}
		else {
			// Handle divide by zero possibility 
			m.put(0, 0, 0);
			m.put(0, 1, 0);				
		}
		return m.clone();
	}
	 
	/** Fast method to determine if a value is a perfect square root (e.g., 
	 * the root of a squared number gives us the original non-squared value)
	 * @see http://stackoverflow.com/questions/295579/fastest-way-to-determine-if-an-integers-square-root-is-an-integer
	 * 
	 * @param n -- value to check
	 * @return whether or not the value is a perfect square root
	 */
	private final static boolean isPerfectSquare(long n)
	{
	  if (n < 0)
	    return false;

	  switch((int)(n & 0x3F))
	  {
	  case 0x00: case 0x01: case 0x04: case 0x09: case 0x10: case 0x11:
	  case 0x19: case 0x21: case 0x24: case 0x29: case 0x31: case 0x39:
	    long sqrt;
	    if(n < 410881L)
	    {
	      /* John Carmack hack, converted to Java.
	         See: http://www.codemaestro.com/reviews/9
	         actually the stackoverflow post above has more efficient 
	         methods */
	      int i;
	      float x2, y;

	      x2 = n * 0.5F;
	      y  = n;
	      i  = Float.floatToRawIntBits(y);
	      i  = 0x5f3759df - ( i >> 1 );
	      y  = Float.intBitsToFloat(i);
	      y  = y * ( 1.5F - ( x2 * y * y ) );

	      sqrt = (long)(1.0F/y);
	    }
	    else
	    {
	      //Carmack hack gives incorrect answer for n >= 410881.
	      sqrt = (long)Math.sqrt(n);
	    }
	    return sqrt*sqrt == n;

	  default:
	    return false;
	  }
	}
	
	/**
	 * Given three colinear points p, q, r, check to see if point q
	 * lies on line segment pr
	 * 
	 * @see http://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/
	 * @see http://www.dcs.gla.ac.uk/~pat/52233/slides/Geometry1x1.pdf
	 * @see ProjectUtilities.isInside 
	 * @see ProjectUtilities.DetermineNodeSize
	 * @see ProjectUtilities.doIntersect
	 * 
	 * @param p -- First point forming line segment
	 * @param q -- Possible point on line segment
	 * @param r -- Second point forming line segment 
	 * @return whether or not point q lies on line segment pr
	 */
	public static boolean onSegment (Point p, Point q, Point r) {
		if ((q.x <= Math.max(p.x, r.x)) && (q.x >= Math.min(p.x, r.x)) && 
				(q.y <= Math.max(p.y, r.y)) && (q.y >= Math.min(p.y, r.y))) {
			return true;
		}						
		return false;
	}
	
	/**
	 * To find orientation of ordered triplet (p, q, r)
	 * 
	 * @see http://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/
	 * @see http://www.dcs.gla.ac.uk/~pat/52233/slides/Geometry1x1.pdf
	 * @see ProjectUtilities.isInside 
	 * @see ProjectUtilities.DetermineNodeSize
	 * @see ProjectUtilities.doIntersect
	 * 
	 * @param p -- First point
	 * @param q -- Second point
	 * @param r -- Third point
	 * 
	 * @return 0 == collinear, 1 == clockwise, 2 == counterclockwise
	 */
	public static int orientation(Point p, Point q, Point r) {
		
		double val = (q.y - p.y) * (r.x - q.x) - 
				      (q.x - p.x) * (r.y - q.y);
		if (val == 0) {
			// no slope between two other points, collinear
			return (int) 0;
		}
		else {
			// positive slope
			return (int) ((val > 0) ? 1 : 2);
		}
	}
	
	/**
	 *  The function that returns true if line segment 'p1q1' and 
	 *  'p2q2' intersect
	 * 
	 * @see 
	 * http://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/
	 * @see 
	 * http://www.dcs.gla.ac.uk/~pat/52233/slides/Geometry1x1.pdf
	 * 
	 * @see ProjectUtilities.isInside 
	 * @see ProjectUtilities.DetermineNodeSize
	 * 
	 * @param p1 -- Starting point of first line segment
	 * @param q1 -- Ending point of first line segment
	 * @param p2 -- Starting point of second line segment
	 * @param q2 -- Ending point of second line segment
	 * @return -- whether or not a two line segments intersect each other
	 * or not
	 */
	public static boolean doIntersect(Point p1, Point q1, Point p2, Point q2) {
		
		/**
		 * Find the four orientations needed for general and
		 * special cases
		 */
	    int o1 = orientation(p1, q1, p2);
	    int o2 = orientation(p1, q1, q2);
	    int o3 = orientation(p2, q2, p1);
	    int o4 = orientation(p2, q2, q1);

	    // General case
	    if (o1 != o2 && o3 != o4) {
	    	return true;
	    }
	    
	    // Special Cases
     	// p1, q1 and p2 are colinear and p2 lies on segment p1q1
	    if (o1 == 0 && onSegment(p1, p2, q1)) {
	    	return true;
	    }
	    
	    // p1, q1 and p2 are colinear and q2 lies on segment p1q1
	    if (o2 == 0 && onSegment(p1, q2, q1))  {
	    	return true;
	    }
	    
	    // p2, q2 and p1 are colinear and p1 lies on segment p2q2
	    if (o3 == 0 && onSegment(p2, p1, q2)) {
	    	return true;
	    }
	    
	    // p2, q2 and q1 are colinear and q1 lies on segment p2q2
	    if (o4 == 0 && onSegment(p2, q1, q2)) {
	    	return true;
	    }
	    
		return false; // Doesn't fall in any of the above cases
	}
	
	
	/**
	 * Returns true if the point p lies inside the polygon[] with n vertices
	 * 
	 * @see 
	 * http://www.geeksforgeeks.org/how-to-check-if-a-given-point-lies-inside-a-polygon/
	 * @see http://www.dcs.gla.ac.uk/~pat/52233/slides/Geometry1x1.pdf
	 * 
	 * @see ProjectUtilities.DetermineNodeSize
	 * 
	 * @param nonZeroBorderPoints -- a matrix listing the points containing 
	 * non-zero
	 * values -- e.g., the border pixels
	 * @param n -- number of vertices
	 * @param p -- is point (x,y) inside the pixels defining the border region
	 * @return whether or not the point is inside the polygon
	 */
	public static boolean isInside(Mat nonZeroBorderPoints, int n, Point p) {
		// There must be at least 3 vertices in polygon[]
		if (n < 3) {
			return false;
		}
		
		Point extreme  = new Point(10000, p.y);
		
	    // Count intersections of the above line with sides of polygon
	    int count = 0, i = 0;
	    
	    do {
	    	
	    	int next = (i+1)%n;
	    	
	    	/*Check if the line segment from 'p' to 'extreme' 
	    	 *intersects with the line segment from 'polygon[i]'
	    	 * to 'polygon[next]' */
	    	 double[] pairp1 = nonZeroBorderPoints.get(i, 0);
	    	 Point p1 = new Point(pairp1[0], pairp1[1]);
	    	 
	    	 double[] pairq1 = nonZeroBorderPoints.get(next, 0);
	    	 Point q1 = new Point(pairq1[0], pairq1[1]);
	    	 
	    	 //Point p1 = new Point 
	    	 if (doIntersect(p1, q1, p, extreme)) {
	    		 /*If the point 'p' is colinear with line segment 'i-next',
	    		  *then check if it lies on segment. If it lies, return true, 
	    		  *otherwise, false*/
	    		 if ((orientation(p1, p, q1) == 0)) {
	    			 return onSegment(p1, p, q1);
	    		 }
	    		 count++;
	    	 }
	    	 
	    	 i = next;
		} while (i != 0);
	    
	    return (count%2 == 1);
	}
	
	/**
	 * Determine the number of pixels belonging to the node/region/segment
	 * @param border -- border of region as identify by LG algorithm
	 * @see ProjectUtilities.Inside
	 * @return size in pixels of region
	 */
	public static long DetermineNodeSize(Mat border) {
		Mat nonZeroLocations = new Mat();
		Mat temp = new Mat();
		
		/* Find the number and location of all border pixels 
		 * we don't need much accuracy for this routine, 
		 * just non-zero values, should improve performance */
		if (border.type() != CvType.CV_8UC1) {
			border.convertTo(temp, CvType.CV_8UC1);	
		}
		else {
			temp = border.clone();
		}
		
		
		/* Each entry contains the row and column of a non-zero 
		 * pixel. Remember, some weird segments may have the 
		 * border running around the edge of the image, lots
		 * of x,1 locations at the beginning of the image */
		Core.findNonZero(temp, nonZeroLocations);
		
		/* Changing type will not change the number of channels
		 * or depth of the image */
		nonZeroLocations.convertTo(nonZeroLocations, border.type());		
		
		/* total should give me the same result as countNonZero; 
		 * however, findnonzero gives me a two channel result
		 * and countNonZero expects one channel, sigh 
		 * 
		 * the same result as it's first channel * second (always = 1) */
		int n = (int)nonZeroLocations.total();
		
		/* Try to reduce burden on the research system and 
		 * still maintain a high level of accuracy 
		 * 
		 * a rule of thumb matrix here to handle large
		 * raster complex polygon borders */
		Mat reducednonZeroLocations = null;
		if (n > 1000) {
			reducednonZeroLocations = 
					ProjectUtilities.returnEveryNthElement(
							nonZeroLocations, 100);	
		}		
		
		if (reducednonZeroLocations != null) {
			nonZeroLocations = reducednonZeroLocations;
			n = (int)reducednonZeroLocations.total();
		}
	
		/* Determine the extents of the border region*/
		double xmin = Double.MAX_VALUE;
		double xmax = Double.MIN_VALUE;
		double ymin = Double.MAX_VALUE;
		double ymax = Double.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			double[] data = nonZeroLocations.get(i, 0);
			double xVal = data[0];
			double yVal = data[1];
			
			if (xVal < xmin) {
				xmin = xVal;
			}
			
			if (xVal > xmax) {
				xmax = xVal;
			}
			
			if (yVal < ymin) {
				ymin = yVal;
			}
			
			if (yVal > ymax) {
				ymax = yVal;
			}
		}
		
		
		/* this will give the full size of the image file, 
		 * not just the border part extents */
		long size = 0;
		
		/* So using the border pixels, each pixel being a vertex (not ideal,
		 * but this is a raster polygon, not a vector one, determine which 
		 * pixels in the extent area inside the polygon to determine
		 * the size of the polygon */
		int yMinInt = (int) ymin;
		int xMinInt = (int) xmin;
		int yMaxInt = (int) ymax;	
		int xMaxInt = (int) xmax;
		for(int i = yMinInt; i < yMaxInt; i++) {
			for (int j = xMinInt; j < xMaxInt; j++) {
				if (isInside(nonZeroLocations, n, new Point(j, i))) {
					size++;
				}
			}
		}
		return size;
	}
	
	/**
	 * Convert an array of 1xn OpenCV matrices into a 1xn double array
	 * 
	 * @param MatAL -- a list of OpenCV matrices
	 * @param duplicateBegEnd -- whether or not to close the ending of the
	 * array with the beginning
	 * @return a 1xn array of copied floating values from an OpenCV
	 * array
	 */
	public static double[] convertMat1xn(ArrayList<Mat> MatAL, Boolean duplicateBegEnd) {
		/* Find the total size of array needed for conversion*/
		int total = 0;
		int currentcnt = 0;
		for(Mat m : MatAL) {
			total += m.total();
		}
		
		/* perform conversion */
		double[] q;
		if (duplicateBegEnd) {
			q = new double[total];	
		}
		else {
			q = new double[total+1];
		}
		
		for (Mat m : MatAL) {
			int numElements = (int) m.total();
			Size s = m.size();
			if (s.height != 1) {
				/* ignore Mats not 1xn in size */
				continue;
			}
			for(int i = 0; i < numElements; i++) {
				q[currentcnt++] = m.get(0, i)[0];
			}
		}
		
		if ((duplicateBegEnd) && (total - 1 >= 0)) {
			q[total-1] = q[0]; 
		}
		
		return q;
	}

	/**
	 * Fix the minimum value in an array of floating point values
	 * @param array -- list of values to search
	 * @return Smallest value
	 */
	public static double findMin(double[] array) {
		double minValue = Double.MAX_VALUE;
		for (int i = 0; i < array.length; i++) {
			if (array[i] < minValue) {
				minValue = array[i];
			}
		}
		return minValue;
	}
	
	/**
	 * Fix the maximum value in an array of floating point values
	 * with O(n) runtime
	 *  
	 * @param array -- list of values to search
	 * @return Largest value
	 */
	public static double findMax(double[] array) {
		double maxValue = Double.MIN_VALUE;
		for (int i = 0; i < array.length; i++) {
			if (array[i] > maxValue) {
				maxValue = array[i];
			}
		}
		return maxValue;
	}
	
	/**
	 * Calculate the distance between two points on a 2D plane
	 * A variant of the Pythagorean theorem
	 * 
	 * @param x1 -- X coordinate first point
	 * @param x2 -- X coordinate second point
	 * @param y1 -- Y coordinate first point
	 * @param y2 -- Y coordinate second point
	 * @return distance
	 */
	public static double distance(
			double x1, double x2, double y1, double y2) {
		// Formula: ((x2-x1)^2+(y2-y1)^2)^(1/2)
		return Math.sqrt(Math.pow((x2 - x1),2) + Math.pow((y2 - y1),2));
	}
	
	/**
	 * Calculate the distance between two points on a 2D plane
	 * A variant of the Pythagorean theorem
	 * 
	 * @param start -- Coordinates of starting point
	 * @param end -- Coordinates of ending point
	 * @return distance
	 */
	public static double distance(Point start, Point end) {
		// Formula: ((x2-x1)^2+(y2-y1)^2)^(1/2)
		
		// sanity check
		if((start == null) || (end == null)) {
			System.err.println(" Start or end point is null");
			return Double.MAX_VALUE;
		}
		
		double x1 = start.x;
		double x2 = end.x;
		double y1 = start.y;
		double y2 = end.y;
		return Math.sqrt(Math.pow((x2 - x1),2) + Math.pow((y2 - y1),2));
	}	
	
	/**
	 * Crop a grayscale image without human intervention
	 * @param segment -- OpenCV encoded matrix with image data to crop
	 * @author user Constantine on stackoverflow.com (anonymous credential)
	 * @see http://stackoverflow.com/questions/23134304/crop-out-part-from-images-findcontours-opencv-java
	 * @return cropped grayscale image
	 */
	public static Mat autoCropGrayScaleImage(Mat segment, boolean apply_threshold) {
		
		Mat original = segment.clone();
		Mat image = segment.clone();
		
	    // thresholding the image to make a binary image
		if (apply_threshold) {
			Imgproc.threshold(image, image, 100, 255, Imgproc.THRESH_BINARY_INV);	
		}	    
		
	    // find the center of the image
	    double[] centers = {(double)image.width()/2, (double)image.height()/2};
	    Point image_center = new Point(centers);
	    
		// finding the contours
	    ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	    Mat hierarchy = new Mat();
	    Imgproc.findContours(
	    		image, contours, hierarchy, 
	    		Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
	    
	    // finding best bounding rectangle for a contour whose distance is closer to the image center that other ones
	    double d_min = Double.MAX_VALUE;
	    Rect rect_min = new Rect();
	    for (MatOfPoint contour : contours) {
	        Rect rec = Imgproc.boundingRect(contour);
	        // find the best candidates
	        if ((rec.height > image.height()/2) && (rec.width > image.width()/2)) {
	        	Mat edges = new Mat();
	        	Imgproc.Canny(image, edges, 0, 2);
	        	return edges.clone();
	        }
	             
	        Point pt1 = new Point((double)rec.x, (double)rec.y);
	        Point center = new Point(rec.x+(double)(rec.width)/2, rec.y + (double)(rec.height)/2);
	        double d = Math.sqrt(Math.pow((double)(pt1.x-image_center.x),2) + Math.pow((double)(pt1.y -image_center.y), 2));            
	        if (d < d_min)
	        {
	            d_min = d;
	            rect_min = rec;
	        }                   
	    }
	    // slicing the image for result region
	    int pad = 5;        
	    rect_min.x = rect_min.x - pad;
	    rect_min.y = rect_min.y - pad;
	    
	    if (rect_min.x <= 0) {
	    	rect_min.x = 1;
	    }

	    if (rect_min.y <= 0) {
	    	rect_min.y = 1;
	    }
	    
	    rect_min.width = rect_min.width + 2*pad;
	    rect_min.height = rect_min.height + 2*pad;

	    if ( rect_min.width <= 0) {
	    	rect_min.width = 1;
	    }
	    
	    if (rect_min.height <= 0) {
	    	rect_min.height = 1;
	    }
	    
	    if (rect_min.x >= original.width()) {
	    	rect_min.x = original.width()-1;
	    }
	    
	    if (rect_min.y >= original.height()) {
	    	rect_min.y = original.height()-1;
	    }
	    
	    if ((rect_min.x + rect_min.width) > original.width()) {
	    	rect_min.width -= (rect_min.x + rect_min.width) - original.width();
	    }
	    
	    if ((rect_min.y + rect_min.height) > original.height()) {
	    	rect_min.height -= (rect_min.y + rect_min.height) - original.height();
	    }
	    
	    // Size down the original
	    Mat result = original.submat(rect_min);
	    
	    // debug line
	    //Imgcodecs.imwrite("cropped_"+Math.abs(new Random().nextLong())+".jpg", result);
	    
	    // return the cropped image
	    return result.clone();
	}
	
	/**
	 * Open the most recent version of an image fitting a given pattern and
	 * using a set of optional flags
	 * 
	 * <br/>
	 * NOTE: currently uses user.dir as the directory to perform the search
	 * in
	 * <br/>
	 * @see org.apache.commons.io.filefilter.WildcardFileFilter.WildcardFileFilter
	 * 
	 * @param filenamePattern -- the filename string pattern of the image to
	 * search for in the directory
	 * @param imgCodecsFlag -- (optional) flags to use in opening, pass 0 to
	 * not specify any flags
	 * @return the OpenCV matrix encoding of the image
	 */
	public static Mat openMostRecentImage(
			String filenamePattern, int imgCodecsFlag) { 
		//TODO: add a param to use a different directory or user.dir if null
		/* Get a list of all the files matching the filter */
		File dir = new File(System.getProperty("user.dir"));
		FileFilter fileFilter =
				new WildcardFileFilter(filenamePattern);
		File[] files = dir.listFiles(fileFilter);
		long lastMod = Long.MIN_VALUE;
		File choice = null;
		
		/* Move through all the files in the directory that made it through
		 * the filter*/ 
		for (int i = 0; i < files.length; i++) {
			File f = files[i];
			if (f.lastModified() > lastMod){
				choice = f;
				lastMod = f.lastModified();
			}				
		}
		
		// If we found an image to use, open it and read
		Mat resultMat = null;
		if (choice != null) {
			resultMat = Imgcodecs.imread(choice.getAbsolutePath(),
					                     imgCodecsFlag);
			
			// return the OpenCV encoded matrix
			return resultMat;
		}
		else {
			return null;
		}		
	}
	
	/**
	 * Find the extents (e.g., extreme points) of a Matrix of Points
	 * object
	 * 
	 * @param mop -- Matrix of Points object
	 * @return the borders of the region identified by the object
	 */
	public static MinMaxLocResult findMMLRExtents(MatOfPoint mop) {		
		/* set up the border edge points */
		double borders[] = new double[4];
		borders[0] = Double.MAX_VALUE; // left
		borders[1] = Double.MAX_VALUE; // top
		borders[2] = 0; // right
		borders[3] = 0; // bottom

		/* Move through each point in the matrix and determine
		 * the border based on all those points */
		for(int i = 0; i < mop.rows(); i++) {
			for(int j = 0; j < mop.cols(); j++) {
				double row = mop.get(i, j)[1];
				double col = mop.get(i, j)[0];
				Point pt = new Point(col, row);
				if (pt.x < borders[0]) {
					borders[0] = pt.x;
				}
				
				if (pt.y < borders[1]) {
					borders[1] = pt.y;
				}
				
				if (pt.x > borders[2]) {
					borders[2] = pt.x;
				}
				
				if (pt.y > borders[3]) {
					borders[3] = pt.y;
				}
			}			
		}
		
		/* Send the border extents back to the calling routine */
		MinMaxLocResult mmlr = new MinMaxLocResult();
		mmlr.minLoc = new Point(borders[0],borders[1]);
		mmlr.maxLoc = new Point(borders[2],borders[3]);
		return mmlr;
	}
	/**
	 * This method creates an evenly distributed set of initial center points
	 * to use with the OpenCV paritioning algorithm -- needed to ensure that 
	 * partitioning between candidate and model images are similar -- e.g., 
	 * the use of a uniform random scheme for setting initial centeroid
	 * locations will lower confidence matches to unacceptably low levels
	 * 
	 * @param width -- width of image in pixels
	 * @param height -- height of image in pixels
	 * @param k -- Number of clusters to use in partitioning algorithm
	 * @return A vector with the initial centroid locations in the image to use
	 * with a partioning algorithm like OpenCV's kmeans
	 */
	public static Mat setInitialLabelsGrayscale(int width, int height, int k) {
		int totalCells = width * height;
		int index = 0;
		int count = 0;
		int jump = totalCells/k;
		Mat labels = new Mat(k,1,CvType.CV_32S);
		while (count < k) {
			index+=jump;
			labels.put(count, 0, index-1);
			count++;
		}
		return labels;
	}
	
	/**
	 * Take a 2D byte array (matrix) and flatten it into a 1D vector
	 * @param p -- input 2D array (matrix)
	 * @return the flatten array
	 */
	public static byte[] flatten2DByteArray(byte[][] p) {
		int nArrays = p.length;
		int nRows = 0;
		int nCols = 0;
		
		/* Determine num elements in flattened array
		 * Move through each array 
		 * Could have something like: 
		 * int [][] array = { {7, 5, 3} {2}, {44, 75}  */
		for (int i = 0; i < nArrays; i++) {
			nRows++;
			
			// Move through elements of that array
			int szArray = p[i].length;
			for (int j = 0; j < szArray; j++) {
				nCols++;
			}
		}		
		byte[] q = new byte[nRows*nCols];
		
		/* Move through each array */
		int cnt = 0;
		for (int i = 0; i < nArrays; i++) {
			
			/* Move through elements of each array */
			int szArray = p[i].length;
			for (int j = 0; j < szArray; j++) {
			    q[cnt++] = p[i][j];
			}
		}
		return q;
	}
	
	/**
	 * Return every nth element from the opencv vector
	 * input must be a vector
	 * @param p -- input matrix
	 * @param n -- extract every nth element
	 * @return null if not a vector or every nth element otherwise
	 * in a vector
	 */
	public static Mat returnEveryNthElement(Mat p, int n) {
		if ((p.rows() != 1) && p.cols() != 1) { 
			return null;
		}
		
		Mat q = null;
		if (p.rows() != 1) {
			q = new Mat(p.rows()/n, 1, p.type());
		}
		else {
			q = new Mat(1, p.cols()/n, p.type());
		}
		
		for (int i = 0; i < p.rows(); i++) {
			for (int j = 0; j < p.cols(); j++) {
				if (((i+1) % n == 0) || ((j+1) % n == 0)) {
					double[] value = p.get(i, j);					
					q.put(i, j, value[0], value[1]);	// why won't this copy the values?
				}					
			}
		}
		return q;
	}
	
	/**
	 * Perform a custom sharpening (blurring and weighted addition)
	 * @param input -- image to be sharpened
	 * @return sharpened image
	 */
	public static Mat sharpen(Mat input) {
		// prepare return object
		Mat output = 
				new Mat(
						input.rows(), input.cols(), 
						input.type(), new Scalar(0,0));
		
		// reduce image noise and extraneous details (inner segment details)
		Imgproc.GaussianBlur(input, output, new Size(0, 0), 6);
		
		/* Emphasizes the input by 50%, deemphasizes the blur by 50% and then
		   adds the two together to keep the major features of the image 
		   strong, but removes the small details, the important parts
		   looked to be emphasized 
		   
		   dst = input*alpha + output*beta + gamma;
		   dst = 1.5(input) + (-0.5)output; */
		Core.addWeighted(input, 1.5, output, -0.5, 0, output);
		return output;
	}
	
	public static void heapStatistics() {
		int mb = 1024*1024;
		
		//Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();
		
		//Print used memory
		System.out.println("Used Memory:" 
			+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

		//Print free memory
		System.out.println("Free Memory:" 
			+ runtime.freeMemory() / mb);
		
		//Print total available memory
		System.out.println("Total Memory:" + runtime.totalMemory() / mb);

		//Print Maximum available memory
		System.out.println("Max Memory:" + runtime.maxMemory() / mb);
	}
	
	public static int findRowInSpreadSheet(XSSFSheet sheet, String cellContent) {
	    for (Row row : sheet) {
	        for (Cell cell : row) {
	            if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
	                if (cell.getRichStringCellValue().getString().trim().equalsIgnoreCase(cellContent)) {
	                    return row.getRowNum();  
	                }
	            }
	        }
	    }               
	    return -1;
	}
	
	/**}
	 * Find a rectangle covering an array of points 
	 * @param pts array of points
	 * @return rectangle covering array of points
	 */
	public static Rect calcRectCoveringPts(ArrayList<Point> pts) {
		
		// determine extents for rectangle
		double x1 = pts.get(0).x;
		double y1 = pts.get(0).y;
		double x2 = pts.get(0).x;
		double y2 = pts.get(0).y;
		for (Point p : pts) {
			if(p.x < x1) {
				x1 = p.x;
			}
			
			if (p.y < y1) {
				y1 = p.y;
			}
			
			if (p.x > x2) {
				x2 = p.x;
			}
			
			if (p.y > y2) {
				y2 = p.y;
			}
		}
		
		// build rectangle covering the set points, sub/add 5% to prevent rect trimming on creation
		Point p1 = new Point(x1*0.95, y1*0.95);
		Point p2 = new Point(x2*1.05, y2*1.05);
		Rect r = new Rect(p1, p2);
		return r;
	}
	
	public static List<Point> convertMatOfFloat6(MatOfFloat6 input) {
		int inputRows = input.rows();
		List<Point> output = new ArrayList<Point>();		
		for (int inputCnt = 0, outputCnt = 0; inputCnt < inputRows; inputCnt++, outputCnt+=6) {
			output.add(new Point(input.get(inputCnt, 0)[0], input.get(inputCnt, 0)[1]));
			output.add(new Point(input.get(inputCnt, 0)[2], input.get(inputCnt, 0)[3]));
			output.add(new Point(input.get(inputCnt, 0)[4], input.get(inputCnt, 0)[5]));
		}
		return output;
	}
}


