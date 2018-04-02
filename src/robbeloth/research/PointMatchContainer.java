package robbeloth.research;

import java.io.Serializable;

import org.opencv.core.Point;

public class PointMatchContainer implements Serializable {
	private static final long serialVersionUID = 12345L;
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
	
	/**
	 * Copy constructor
	 * @param pmc
	 */
	public PointMatchContainer(PointMatchContainer pmc) {
		if (pmc.pt != null) {
			this.pt = new Point(pmc.getPoint().x, pmc.getPoint().y);
		}
		else {
			this.pt = new Point(0,0);
		}

		if(pmc.getMatch() != null) {
			this.match = new String(pmc.getMatch());	
		}		
		else {
			this.match = "";
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
