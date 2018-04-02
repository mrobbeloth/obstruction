package robbeloth.research;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
/**
 * A complex container class whose objects store<br/>
 * 1. x coordinates in segment <br/>
 * 2. y coordinates in segment <br/>
 * 3. segment time<br/>
 * 
 * @author mrobbeloth
 *
 */
public class LineSegmentContainer implements Serializable{
	private static final long serialVersionUID = 123459L;
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("x entries (" + segment_x.size() + " total): ");
		int nCnt = 0;
		for (Mat sx : segment_x) {
			for (int i  = 0; i < sx.cols(); i++) {
				sb.append(sx.get(0,i)[0] + ",");
			}

			if ((nCnt % 100) == 1) {
				sb.append("\n");
			}
			nCnt++;
			
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("\n");
		sb.append("y entries (" + segment_y.size() + " total): ");
		nCnt = 0;
		for (Mat sy : segment_y) {
			for (int i  = 0; i < sy.cols(); i++) {
				sb.append(sy.get(0,i)[0] + ",");
			}			

			if ((nCnt % 100) == 1) {
				sb.append("\n");
			}
			nCnt++;
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("\n");
		sb.append("Line Segment Generation time: " + TimeUnit.MILLISECONDS.convert(
				segment_time, TimeUnit.NANOSECONDS) + " ms\n");
		return sb.toString();
	}
}