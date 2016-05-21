package robbeloth.research;
import java.util.ArrayList;
/**
 * This utility class provides a container for use by the K3M algorithm
 * @author Michael Robbeloth
 * @category Projects
 * @since 10/03/2014 
 * @version 1.0
 * <br/><br/>
 * Class: CS7900<br/>
 * <h2>Revision History</h2><br/>
 * <b>Date						Revision</b>
 *
 */
public class K3MContainer {
	private ArrayList<Integer> imageData;
	private ArrayList<Integer> borderData;
	
	/**
	 * Create a K3M container 
	 * @param imageData -- a variable sized array of pixels
	 * @param borderData -- a variable sized array of border markings
	 */
	public K3MContainer(ArrayList<Integer> imageData, 
			            ArrayList<Integer> borderData) {
		this.imageData = imageData;
		this.borderData = borderData;
	}
	
	/**
	 * Create a K3M container
	 * @param imageData -- a fixed sized array of pixels
	 * @param borderData -- a fixed sized array of border markingss
	 */
	public K3MContainer(int[] imageData, int[] borderData) {
		int imageDataSize = imageData.length;
		int borderDataSize = borderData.length;
		this.imageData = new ArrayList<Integer>(imageDataSize);
		this.borderData = new ArrayList<Integer>(borderDataSize);
		
		for (int i = 0; i < imageDataSize; i++) {
			this.imageData.add(i, imageData[i]);
		}
		
		for (int i = 0; i < borderDataSize; i++) {
			this.borderData.add(i, borderData[i]);
		}
	}
	
	/**
	 * Return the image data as a fixed sized primitive array
	 * @return (see above)
	 */
	public int[] getImageData() {

	    int[] ret = new int[imageData.size()];
	    for (int i=0; i < ret.length; i++)
	    {
	        ret[i] = imageData.get(i).intValue();
	    }
	    
		return ret;
	}
	
	/**
	 * Return the image data as a variable sized object array
	 * @return (see above)
	 */
	public ArrayList<Integer> getImageDataAL() {
		return imageData;
	}
	
	/**
	 * Return the border data as a variable sized object array
	 * @return (see above)
	 */
	public ArrayList<Integer> getBorderDataAL() {
		return borderData;
	}
	
	/**
	 * Return the border data as a fixed sized primitive array
	 * @return
	 */
	public int[] getBorderData() {
	    int[] ret = new int[borderData.size()];
	    for (int i=0; i < ret.length; i++)
	    {
	        ret[i] = borderData.get(i).intValue();
	    }
	    
		return ret;
	}
}