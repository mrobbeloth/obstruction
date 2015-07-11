package robbeloth.ceg7900;
import javax.imageio.ImageIO;

/**
 * This class describes image processing capabilities of this program
 * @author Michael Robbeloth
 * @category Projects
 * @since 9/6/2014 
 * @version 1.0
 * <br/><br/>
 * Class: CS7900<br/>
 * <h2>Revision History</h2><br/>
 * <b>Date						Revision</b>
 */
public class ApplicationInformation {
	/**
	 * Describe what image formats the application can read
	 * @return a description of the image formats the application can read
	 */
	static String reportOnSupportedImageReadingCapabilities(){		
		StringBuilder sb = new StringBuilder();
		sb.append("*** APPLICATION PROPERTIES *** \n");
		
		String[] readerSuffixes = ImageIO.getReaderFileSuffixes();
		if (readerSuffixes != null) {
			for (int i = 0; i < readerSuffixes.length; i++) {
				sb.append("Application can read file with suffix: "+
						readerSuffixes[i] + "\n");
			}
		}
		sb.append("\n");
		
		String[] readerFormats = ImageIO.getReaderFormatNames();
		if (readerFormats != null) {
			for (int i = 0; i < readerFormats.length; i++) {
				sb.append("Application can read format: "+
						  readerFormats[i] + "\n");
			}
		}
		sb.append("\n");
		
		String[] readerMIMETypes = ImageIO.getReaderMIMETypes();
		if (readerMIMETypes != null) {
			for (int i = 0; i < readerMIMETypes.length; i++) {
				sb.append("Application can read MIME type: "+
						readerMIMETypes[i] + "\n");
			}
		}
		sb.append("\n");
		return sb.toString();
	}
	
	/**
	 * Describe what image formats the application can write
	 * @return a description of the image formats the application can write
	 */
	static String reportOnSupportedImageWritingCapabilities(){
		StringBuilder sb = new StringBuilder();
		String writerNames[] = ImageIO.getWriterFormatNames();
		if (writerNames != null) {
			for (int i = 0; i < writerNames.length; i++) {
				sb.append("Application can write image format: " + 
						writerNames[i] + "\n");
			}
		}		
		sb.append("\n");
		
		String writerSuffixes[] = ImageIO.getWriterFileSuffixes();
		if (writerSuffixes != null) {
			for (int i = 0; i < writerSuffixes.length; i++) {
				sb.append("Application can write image suffix: " + 
						writerSuffixes[i] + "\n");
			}
		}		
		sb.append("\n");
		
		String writerMIMETypes[] = ImageIO.getWriterMIMETypes();
		if (writerMIMETypes != null) {
			for (int i = 0; i < writerMIMETypes.length; i++) {
				sb.append("Application can write image MIME type: " + 
						writerMIMETypes[i] + "\n");
			}
		}		
		sb.append("\n");
		return sb.toString();		
	}
}
