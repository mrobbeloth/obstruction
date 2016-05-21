package robbeloth.research;

import java.util.ArrayList;

import org.opencv.core.Mat;

public class LineSegmentContainer {
	private ArrayList<Mat> segment_x;
	private ArrayList<Mat> segment_y;
	private long segment_time;
	
	public LineSegmentContainer(ArrayList<Mat> segment_x, 
			                     ArrayList<Mat> segment_y,
			                     long segment_time) {
		
		this.segment_x = new ArrayList<Mat>();
		this.segment_y = new ArrayList<Mat>();
		
		for(Mat m : segment_x) {
			if (m != null) {
				this.segment_x.add(m.clone());	
			}			
		}
		
		for(Mat m : segment_y) {
			if (m != null) {
				this.segment_y.add(m.clone());				
			}
		}
		
		this.segment_time = segment_time;
	}

	public ArrayList<Mat> getSegment_x() {
		return segment_x;
	}

	public ArrayList<Mat> getSegment_y() {
		return segment_y;
	}

	public long getSegment_time() {
		return segment_time;
	}
	
	
}