package robbeloth.research;
import java.awt.Graphics;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.FileImageOutputStream;

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

/**
 * This class provides base class for all shared image operator classes
 * This class is abstract and must be instantiated with a child class
 * @author Michael Robbeloth
 * @category Projects
 * @since 9/6/2014 
 * @version 1.0
 * <br/><br/>
 * Class: CS7900<br/>
 * <h2>Revision History</h2><br/>
 * <b>Date						Revision</b>
 *    9/21/2014                 Integrate PDFBox Apache API
 *                              
 *                              Save jpegs with 100% quality (not 
 *                              the default 70-75% quality) 
 */
public abstract class AlgorithmGroup {
	/**
	 * 
	 * @param fn -- original file
	 * @param appendStr -- modified text to add to file name
	 * @param extension -- file extension of new file
	 * @return a new abstract representation of some new file
	 */
	public static File modifyFileName(File fn, String appendStr, String extension) {
		long dt = new Date().getTime();
		String fnName = null;
		try {
			String canonicalPath = fn.getCanonicalPath();
			int lastIndex = canonicalPath.lastIndexOf(".");
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
		String[] formatsToWrite = ImageIO.getWriterFileSuffixes();
		for (int i = 0; i < formatsToWrite.length; i++) {
			long dt = new Date().getTime();
			String fnName = null;
			try {
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
		long dt = new Date().getTime();
		String fnName = null;
		try {
			String canonicalPath = fn.getCanonicalPath();
			int lastIndex = canonicalPath.lastIndexOf(".");
			if ((appendStr != null) && (appendStr.length() > 0)) {
				fnName = canonicalPath.substring(0, lastIndex) 
						 + "_"  + appendStr + "_" 
						 + "_" + dt + "_." + format;
			}
			else {
				fnName = canonicalPath.substring(0, lastIndex) 
						 + "_" + dt + "_." + format;					
			}
		} catch (IOException e) {
			 System.out.println(e.getMessage());
			 return;
		}
		File outputFile = new File(fnName);
		if (format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("jpg")) {
			writeJPEGImagestoDisk(bImg, outputFile);
		}
		else {
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
	 * jszakmeister http://stackoverflow.com/questions/17108234/setting-jpg-compression-level-with-imageio-in-java
	 * @param bImg
	 * @param outputFile
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
	 * Reference: user f1wade http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage
	 * @param bi
	 * @return
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
	 * @param title -- caption text
	 * @param oImg -- original image
	 * @param bImg -- modified image
	 */
	public static void writePDFtoDisk(File fn, String title, BufferedImage oImg, BufferedImage bImg) {
		// create a new empty document
		PDDocument doc = new PDDocument();
		
		// Set document metadata
		PDDocumentInformation info = new PDDocumentInformation();
		info.setAuthor("Michael Robbeloth");
		info.setCreator("PDFBox");
		info.setTitle(title);
		info.setProducer("CS7900 - Image Computing -- Dr. Bourbakis");
		info.setKeywords("image computing operators");
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
	public static void writePDFtoDiskDblSrc(File fn, String title, BufferedImage oImg1, 
			BufferedImage oImg2, BufferedImage bImg) {
		// create a new empty document
		PDDocument doc = new PDDocument();
		
		// Set document metadata
		PDDocumentInformation info = new PDDocumentInformation();
		info.setAuthor("Michael Robbeloth");
		info.setCreator("PDFBox");
		info.setTitle(title);
		info.setProducer("CS7900 - Image Computing -- Dr. Bourbakis");
		info.setKeywords("image computing operators");
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
	 * @return
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
	 * A necessary evil to ensure that the pdfs are created and later collected in the order the
	 * operators were applied in the control class
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

	public static boolean generatePresentation(File dir, String[] args) {
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
        XSLFSlideMaster master = presentation.getSlideMasters()[0];
        XSLFSlideLayout layout1 = master.getLayout(SlideLayout.TITLE);
		XSLFSlide titleSlide = presentation.createSlide(layout1);
		XSLFTextShape[] ph1 = titleSlide.getPlaceholders();
		XSLFTextShape tp1 = ph1[0];
		tp1.setText("CS7900 Image Computing Presentation");
		XSLFTextShape tp2 = ph1[1];
		tp2.setText("By Michael Robbeloth \n" + 
		            new Date(System.currentTimeMillis()));
		
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			BufferedImage bi = null;
			try {
				bi = ImageIO.read(f);
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
				int idx = presentation.addPicture(picData, 
		                	XSLFPictureData.PICTURE_TYPE_JPEG);
				
				Byte[] byteObjects = null;
				byte[] bytes;
				int j = 0;
				int masterIdx = -1;
				if (keyToUse != null) {
					byteObjects = masterImages.get(keyToUse);
					bytes = new byte[byteObjects.length];
					j=0;
					// Unboxing byte values. (Byte[] to byte[])
					for(Byte b: byteObjects)
					    bytes[j++] = b.byteValue();
					masterIdx = presentation.addPicture(bytes, 
									XSLFPictureData.PICTURE_TYPE_JPEG);
					
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
		
		try {
			presentation.write(
					new FileOutputStream(dir.toString() + File.separator+ "merged.pptx"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
		
	}	
	
	/**
	 * Convert 1D array into 2D matrix
	 * @param p -- input array
	 * @param width -- width of a column (e.g., num. elements in a row)
	 * @return 2D square matrix
	 */
	public static int[][] Convert1DArrayto2DMatrix(int[] p, int width) {
		int q[][] = new int[width][width];
		
		// convert elements
		for (int y = 0; y < width; y++) {
			for (int x = 0; x < width; x++) {
				q[y][x] = p[x+(width*y)];
			}
		}
		
		return q;
	}
	
	public static double[][] Convert1DArrayto2DMatrixD(int[] p, int width) {
		double q[][] = new double[width][width];
		
		// convert elements
		for (int y = 0; y < width; y++) {
			for (int x = 0; x < width; x++) {
				q[y][x] = p[x+(width*y)];
			}
		}
		
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
	
	public static int[] Convert2DMatrixDto1DArray(double[][] p) {
		
		// calc size for final array
		int total = 0; 
		for (int i = 0; i < p.length; i++) {
			total += p[i].length; 
		}
		int q[] = new int[total];
		
		// convert elements
		for (int y = 0; y < p.length; y++) {
			for (int x = 0; x < p.length; x++) {
				q[x+(y*p.length)] = (int) p[y][x];
			}
		}
		return q;
	}
}
