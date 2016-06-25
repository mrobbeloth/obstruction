package robbeloth.research;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class ChainCodingContainer {
	private Mat border;				// object contour pixel set
	private double chain_time;		// time to produce chain code for seg in ns
	private ArrayList<Double> cc;	// the chain code
	private Point start;			// location of segment centroid
	private enum Direction {		
		S(0), SW(1), W(2), NW(3), N(4), NE(5), E(6), SE(7);
		
		private int value; 
		
		private Direction(int value) {
			this.value = value;
		}
		
		public static String getEnumByString(int value) {
			String cardinalDirection = new String();
			for (Direction d : Direction.values()) {
				if (d.value == value) {
					cardinalDirection = d.name();
					break;
				}
			}
			return cardinalDirection;
		}
		
	}

	/**
	 * Constructor for passing a complex object after running chain code 
	 * algorithm on some input data
	 * @param border -- data needed to draw the border of the region
	 * @param chain_time -- the time it took to generate the chain code 
	 * for this region. 0 is south, 6 is east, 4 is north, and 2 is west
	 * numbers increment clockwise 0 to 7 (eight total directions)
	 * @param cc -- the chain code
	 * @param start -- start point of segment
	 */
	public ChainCodingContainer(Mat border, double chain_time, 
			                     ArrayList<Double> cc, Point start) {
		this.border = border.clone();
		this.chain_time = chain_time;
		
		this.cc = new ArrayList<Double>(cc.size());
		for (Double dblValue : cc) {
			this.cc.add(dblValue);
		}
		this.start = start.clone();
	}

	/**
	 * Get the object border contour set
	 * @return
	 */
	public Mat getBorder() {
		return border;
	}

	/**
	 * Return the amount of time it took to generate the chain code
	 * @return
	 */
	public double getChain_time() {
		return chain_time;
	}

	/**
	 * Get the chain code for the segment -- direction of each line segment
	 * from a relative starting point
	 * @return
	 */
	public ArrayList<Double> getCc() {
		return cc;
	}

	/**
	 * Get the centroid of the segment/region, the first non-zero element
	 * So, row, col --> i, j --> x, y
	 * @return
	 */
	public Point getStart() {
		return start;
	}
	
	/**
	 * 
	 * @param border
	 */
	public void setBorder(Mat border) {
		if (border != null) {
			this.border = border.clone();
		}
	}
	
	/**
	 * Convert the matrix holding the chain code into a human readable
	 * chain code -- could be used for longest substring matching
	 * @return chain code in human readable format
	 */
	public String chainCodeString() {
		StringBuilder sb = new StringBuilder();
		for (Double c : cc) {
			sb.append(c.intValue() + ",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length()-1);	
		}		
		return sb.toString();
	}
	
	/* Provides human readable form of the chain code algorithm return
	 * container object
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Segment starts at " + start + "\n");
		sb.append("It took " + 
		          TimeUnit.MILLISECONDS.convert(
		        		  (long) chain_time, TimeUnit.NANOSECONDS) 
				  + " ms to generate the segment \n");
		sb.append("Chain code is:");
		chainCodeString();
		sb.append("\n");
		for (Double c : cc) {
			String cardinalDir = Direction.getEnumByString(c.intValue());
			sb.append(cardinalDir + ",");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("\n");
		sb.append("Chain code length: " + cc.size());
		return sb.toString();
	}
}
