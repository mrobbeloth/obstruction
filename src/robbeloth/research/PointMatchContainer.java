package robbeloth.research;

import org.opencv.core.Point;

public class PointMatchContainer {
	private Point pt;
	private String match;
		
	PointMatchContainer(Point pt) {
		if (pt != null) {
			this.pt = pt;	
		}
		else {
			this.pt = new Point(0,0);
		}
	}

	public Point getPoint() {
		return pt;
	}

	public String getMatch() {
		return match;
	}

	public void setMatch(String match) {
		this.match = match;
	}		
	
}
