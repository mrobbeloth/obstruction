package robbeloth.research;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Point;

/**
 * Based on SH = U{Ln_j, R^c_j,j+1, Ln_j+1}
 * with S = Ln_n1 . R^c_12, Ln_n2, ...
 * Lj = {sp, l, d, cu}
 * sp=starting point
 * l = length
 * d = orientation
 * cu = curvature
 * 
 * @author mrobbeloth
 *
 */
public class CurveLineSegMetaData {
	private Point sp;				// starting point of a curved line segment
	private Point ep;				// ending point of a curved line segment
	private double length;   		// length of curved line segment (pixels)
	private double orientation;	// in degrees
	private double curvature;		//
	private long lineNumber;		// sequence number
	private long totalTime;        // total time to calc curved line segment in ns
	private ArrayList<CurveLineSegMetaData> connList; 
		
	/**
	 * 
	 * @param sp	-- starting point of a curved line segment
	 * @param ep	-- ending point of a curved line segment
	 * @param length 	-- length of curved line segment (pixels?)
	 * @param orientation
	 * @param curvature
	 */
	public CurveLineSegMetaData(Point sp, Point ep, 
			double length, double orientation,
			double curvature, long lineNumber) {
		super();
		this.sp = sp;
		this.ep = ep;
		this.length = length;
		this.orientation = orientation;
		this.curvature = curvature;
		this.lineNumber = lineNumber;
		this.totalTime = 0;
	}
	
	public CurveLineSegMetaData() {
		super();
		this.sp = new Point(0, 0);
		this.ep = new Point(0, 0);
		this.length = 0;
		this.orientation = 0;
		this.curvature = 0;
		this.lineNumber = 0;
		this.totalTime = 0;
	}
	
	public long getLineNumber() {
		return lineNumber;
	}

	public ArrayList<CurveLineSegMetaData> getConnList() {
		return connList;
	}

	public void setConnList(ArrayList<CurveLineSegMetaData> connList) {
		this.connList = connList;
	}

	public Point getSp() {
		return sp;
	}
	public void setSp(Point sp) {
		this.sp = sp;
	}
	public double getLength() {
		return length;
	}
	public void setLength(double length) {
		this.length = length;
	}
	public double getOrientation() {
		return orientation;
	}
	public void setOrientation(double orientation) {
		this.orientation = orientation;
	}
	public double getCurvature() {
		return curvature;
	}
	public void setCurvature(double curvature) {
		this.curvature = curvature;
	}

	public Point getEp() {
		return ep;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(curvature);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((ep == null) ? 0 : ep.hashCode());
		temp = Double.doubleToLongBits(length);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (lineNumber ^ (lineNumber >>> 32));
		temp = Double.doubleToLongBits(orientation);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((sp == null) ? 0 : sp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CurveLineSegMetaData other = (CurveLineSegMetaData) obj;
		if (Double.doubleToLongBits(curvature) != Double
				.doubleToLongBits(other.curvature))
			return false;
		if (ep == null) {
			if (other.ep != null)
				return false;
		} else if (!ep.equals(other.ep))
			return false;
		if (Double.doubleToLongBits(length) != Double
				.doubleToLongBits(other.length))
			return false;
		if (lineNumber != other.lineNumber)
			return false;
		if (Double.doubleToLongBits(orientation) != Double
				.doubleToLongBits(other.orientation))
			return false;
		if (sp == null) {
			if (other.sp != null)
				return false;
		} else if (!sp.equals(other.sp))
			return false;
		return true;
	}

	public void setEp(Point ep) {
		this.ep = ep;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Curved Line Segment MetaData " + "L" + lineNumber + "[sp=" + sp 
				+ ", ep=" + ep + ", length=" + length
				+ ", orientation=" + orientation + ", curvature=" + curvature
				+ "]\n");
		if (connList != null) {
			sb.append("(");
			for(CurveLineSegMetaData l : connList) {
				sb.append("RC"+this.lineNumber+","+l.getLineNumber()+l.getEp()+",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append(")\n");			
		}
		sb.append("Total time to generate curved line segment " + 
				TimeUnit.MICROSECONDS.convert(totalTime, TimeUnit.NANOSECONDS) + " us\n");
		return sb.toString();
	}

	/**
	 * value returned is in ns
	 * @return
	 */
	public long getTotalTime() {
		return totalTime;
	}

	/**
	 * 
	 * @param totalTime -- time in ns
	 */
	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}	
}
