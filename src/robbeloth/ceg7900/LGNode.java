package robbeloth.ceg7900;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class LGNode {
	/* location of the node, correspondent to region's centroid 
	 * or the center of gravity */
	private Point center;  
	
	// Region stats
	private HashMap<String, Double> stats;
	
	/* Contains average of all rows, cols, and intensity for region
	   Note there are no stats for regions created by region growing
	   process where small regions are inserted within the original
	   clusters */
	public HashMap<String, Double> getStats() {
		return stats;
	}

	// local graph associated with this node (region)
	private ArrayList<CurveLineSegMetaData> L;  
	
	// object contour pixel set
	private Mat border;
	
	// number of pixels belonging to this region
	private long size;
	
	// Partitioned scanned region grown data
	private Mat segment;
	
	// Partitioning algorithm used
	private ProjectUtilities.Partioning_Algorithm pa = 
			ProjectUtilities.Partioning_Algorithm.NONE;
	
	// Node index for LG Algorithm processing
	private int node_id = -1;
	
	/**
	 * Default constructor
	 */
	public LGNode() {
		super();
		this.stats = new HashMap<String, Double>();		
		this.center = new Point(0,0);
		this.L = new ArrayList<CurveLineSegMetaData>();
		this.border = new Mat();
		this.size = 0;
		this.segment = new Mat();
	}
	
	public LGNode(Point center, Mat border, ProjectUtilities.Partioning_Algorithm pa, 
			       int node_id) {
		this();
		
		// copy the center for the region (segment)
		if (center != null) {
			this.center = center;	
		}
		
		// copy the object contour pixel set
		this.border = border.clone();
		
		// Record the partitioning algorithm used
		if (pa != null) {
			this.pa = pa;	
		}
		
		/* total number of non-zero pixels in region */
		this.size = ProjectUtilities.DetermineNodeSize(border);
		
		/* Record region (Node) identifier*/
		this.node_id = node_id;
	}
	
	public LGNode(Point center, HashMap<String, Double> stats, 
			       Mat border, ProjectUtilities.Partioning_Algorithm pa,
			       int node_id) {
		// call simpler constructor for node center
		this(center, border, pa, node_id);
		
		// now save stats
		this.stats = new HashMap<String, Double>(stats.size());
		Set<String> keys = stats.keySet();
		for(String key : keys) {
			Double value = stats.get(key);
			this.stats.put(key, value);
		}
	}
	
	public LGNode(Point center, HashMap<String, Double> stats, 
			Mat border, ArrayList<CurveLineSegMetaData> lmd, 
			Mat segment, ProjectUtilities.Partioning_Algorithm pa, 
			int node_id) {
		this(center, stats, border, pa, node_id);
		
		// now save local graph associated with this node/region/segment
		this.L = new ArrayList<CurveLineSegMetaData>(lmd.size());
		for(int i = 0; i < lmd.size(); i++) {
			this.L.add((lmd.get(i)));
		}
		
		if (segment != null) {
			this.segment = segment.clone();
		}
	}

	public Point getCenter() {
		return center;
	}

	public ArrayList<CurveLineSegMetaData> getL() {
		return L;
	}

	public Mat getBorder() {
		return border;
	}

	public long getSize() {
		return size;
	}

	public void setL(ArrayList<CurveLineSegMetaData> lmd) {
		// now save local graph associated with this node/region/segment
		this.L = new ArrayList<CurveLineSegMetaData>(lmd.size());
		for(int i = 0; i < lmd.size(); i++) {
			this.L.add((lmd.get(i)));
		}
	}

	public void setBorder(Mat border) {
		this.border = border;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("LGNode [center=" + center + ", stats="
				+ stats + ", L=" + L + ", border=" + border.toString() 
				+ ", size=" + size + "segment=" + segment.toString() + "]\n");
		return sb.toString();
	}
		
}
