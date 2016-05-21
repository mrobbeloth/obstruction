package robbeloth.research;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class ChainCodingContainer {
	private Mat border;					// object contour pixel set
	private double chain_time;			// time to produce chain code for seg
	private ArrayList<Double> cc;		// the chain code
	private Point start;				// location of segment centroid

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
}
