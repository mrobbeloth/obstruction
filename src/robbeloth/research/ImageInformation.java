package robbeloth.research;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

public class ImageInformation {
	/**
	 * Report on basic image characteristics
	 * @param img -- in memory representation of the image
	 * @return printed output to standard output of basic image stats
	 */
	static String reportBasicInformation(BufferedImage img) {
		StringBuilder sb = new StringBuilder();
		sb.append("*** BASIC INFORMATION *** \n");
		sb.append("Image is: "+img.getWidth()+" pixels in width\n");
		sb.append("Image is: "+img.getHeight()+" pixels in height\n");
		sb.append("Image type: " + img.getType() + " \n");
		int imageType = img.getType();
		switch(imageType) {
			case BufferedImage.TYPE_3BYTE_BGR:
				sb.append("Image type is three byte Blue-Green-Red (BGR) \n");
			break;
			case BufferedImage.TYPE_4BYTE_ABGR:
				sb.append("Image type is four byte alpha-blue-green-red (ABGR) \n");
			break;
			case BufferedImage.TYPE_4BYTE_ABGR_PRE:
				sb.append("Image type is four byte alpha-blue-green-red "
						  + "(ABGR) with PRE specification\n");
			break;
			case BufferedImage.TYPE_BYTE_BINARY:
				sb.append("Image type is 1, 2, or 4 bit BINARY \n");
			break;
			case BufferedImage.TYPE_BYTE_GRAY:
				sb.append("Image type is Grayscale \n");
			break;
			case BufferedImage.TYPE_BYTE_INDEXED:
				sb.append("Image type is 256 color 6/6/6 color cube pattern \n");
			break;
			case BufferedImage.TYPE_INT_ARGB:
				sb.append("Image type is 8bit ARGB \n");
			break;
			case BufferedImage.TYPE_INT_ARGB_PRE:
				sb.append("Image type is 8bit ARGB PRE\n");
			break;
			case BufferedImage.TYPE_INT_BGR:
				sb.append("Image type is 8bit BGR\n");
			break;
			case BufferedImage.TYPE_INT_RGB:
				sb.append("Image type is 8bit RGB\n");
			break;
			case BufferedImage.TYPE_USHORT_555_RGB:
				sb.append("Image type is RGB with 5 bits for each component\n");
			break;
			case BufferedImage.TYPE_USHORT_565_RGB:
				sb.append("Image type is RGB with 5 bits for "
						+ "red and blue and six bits for green \n");
			break;
			default:
				sb.append("Image type is custom \n");
			break;
		}
		String[] propNames = img.getPropertyNames();
		if (propNames != null) {
			for(int i = 0; i < propNames.length; i++) {
				sb.append("Image has retrievable property: " + 
						  propNames[i]+"\n");
			}	
		}
		sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * Tell the user about the image color model
	 * @param img -- in memory representation of the image
	 * @return printed output to standard output of image color
	 * model stats
	 */
	static String reportOnColorModel(BufferedImage img) {
		/* Describe more detailed characteristics of the image */
		StringBuilder sb = new StringBuilder();
		ColorModel cm = null;
		
		sb.append("*** IMAGE COLOR MODEL PROPERTIES *** \n");
		cm = img.getColorModel();
		sb.append("Image has " + cm.getPixelSize() + 
				           " bit(s) per pixel\n");
		sb.append("Image has " + cm.getNumColorComponents() + 
				           " color components\n");
		sb.append("Image pixels are represented in an array of type "+ 
				            cm.getTransferType() + "\n");
		int transparencyType = cm.getTransparency();
		switch(transparencyType) {
			case Transparency.BITMASK:
				sb.append("Image has bitmask transperency \n");
			break;
			case Transparency.OPAQUE:
				sb.append("Image has opaque transperency \n");
			break;
			case Transparency.TRANSLUCENT:
				sb.append("Image has translucent transperency \n");
			break;
			default:
				sb.append("Image has unknown transperency \n");
			break;
		}
		sb.append("\n");
		return sb.toString();
	}
	
	static String reportOnRaster(BufferedImage img) {
		StringBuilder sb = new StringBuilder();
		Raster r = img.getRaster();
		Rectangle rt = r.getBounds();
		sb.append("Bounding Rectangle MinX: " + rt.x + "\n");
		sb.append("Bounding Rectangle MinY: " + rt.y + "\n");
		sb.append("Bounding Rectangle Width: " + rt.width + "\n");
		sb.append("Bounding Rectangle Heigth: " + rt.height + "\n");
		sb.append("Raster pixel widgth: " + r.getWidth() + "\n");
		sb.append("Raster pixel height: " +  r.getHeight() + "\n");
		sb.append("Raster pixel reported MinX: " +  r.getMinX() + "\n");
		sb.append("Raster pixel reported MinY: " +  r.getMinY() + "\n");
		sb.append("Raster pixel reported MaxX: " +  r.getWidth() + "\n");
		sb.append("Raster pixel reported MaxY: " +  r.getHeight() + "\n");
		sb.append("Samples per pixel: " + r.getNumBands() + "\n");
		sb.append("Number of data elements/pixel: " + r.getNumDataElements() + "\n");
		//sb.append("" + r.)
		sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * Tell the user about the grayscale image
	 * @param img -- in memory representation of the image
	 * @return printed output to standard output of image characteristics
	 */
	static String displayGrayscaleRaster(BufferedImage img) {
		StringBuilder sb = new StringBuilder();
		Raster r = img.getRaster();
		Rectangle rt = r.getBounds();
		int[] pixelArray = new int[rt.width * rt.height*3];
		pixelArray = r.getPixels(rt.x, rt.y, rt.width, rt.height, pixelArray);
		int numPixels = rt.width * rt.height;
		int returnIndex = (int) Math.sqrt(rt.width);
		int numSamplesPPixel = r.getNumBands();
		sb.append("*** PIXEL ARRAY SUPPLIED *** " + "\n");
		int returnCnt = 1;
		/* It appears for grayscale images that the same value is stored in each 8 bit RGB component
		 * so we only need to display the R component for each pixel...what a waste of memory!*/
		for (int i = 0; i < numPixels; i+=numSamplesPPixel, returnCnt++) {
			sb.append(pixelArray[i] + " ");
			if ( (returnCnt % returnIndex) == 0) {
				sb.append("\n");
				returnCnt = 0;
			}
		}
		sb.append("\n");
		return sb.toString();
	}
}
