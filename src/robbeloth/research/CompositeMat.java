package robbeloth.research;

import java.util.ArrayList;
import java.util.Iterator;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * A composite object class where each container holds:<br/>
 * 1. An OpenCV matrix<br/>
 * 2. Time to process a matrix
 * @author mrobbeloth
 */
public class CompositeMat {
	private ArrayList<Mat> listofMats; // Segments from clusters
	private Mat stats;					// Statistics
	private long startingId;			// first id recorded to database
	private long lastId;               // last used id recorded to database
	private String filename;			// file from which segments were generated
	
	public CompositeMat() {
		listofMats = new ArrayList<Mat>();
		stats = new Mat();
		setStartingId(0);
	}
	
	public CompositeMat(ArrayList<Mat> listofMats, Mat mat) {
		this.listofMats = new ArrayList<Mat>();
		Iterator<Mat> i  = listofMats.iterator();
		// with use of native object, need to clone to ensure 
		// we safely get the underlying data, not pointers
		while(i.hasNext()) {
			Mat nextMat = i.next();
			Mat newMat = nextMat.clone();
			this.listofMats.add(newMat);
		}
		this.stats = mat.clone();
		
		this.setStartingId(0);
	}

	public ArrayList<Mat> getListofMats() {
		return listofMats;
	}

	public Mat getMat() {
		return stats;
	}

	public long getStartingId() {
		return startingId;
	}

	public void setStartingId(long startingId) {
		this.startingId = startingId;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getLastId() {
		return lastId;
	}

	public void setLastId(long lastId) {
		this.lastId = lastId;
	}
	
	public void setListOfMat(ArrayList<Mat> listofMats) {
		this.listofMats = new ArrayList<Mat>(listofMats.size());
		Iterator<Mat> i  = listofMats.iterator();
		// with use of native object, need to clone to ensure 
		// we safely get the underlying data, not pointers
		while(i.hasNext()) {
			Mat nextMat = i.next();
			Mat newMat = nextMat.clone();
			this.listofMats.add(newMat);
		}
		this.stats = stats.clone();		
	}
	
	public void addListofMat(ArrayList<Mat> listofMats) {
		if (this.listofMats == null) {
			this.listofMats = new ArrayList<Mat>(listofMats.size());
		}
		Iterator<Mat> i  = listofMats.iterator();
		while(i.hasNext()) {
			Mat nextMat = i.next();
			Mat newMat = nextMat.clone();
			this.listofMats.add(newMat);
		}
		this.stats = stats.clone();
	}
	
	public Mat getCombinedMatrices(){
		Mat finalMat = null;
		int segCnt = 1;
		for (Mat mat : listofMats) {
			/* dst = alpha(src1) + beta(src2) + gamma */
			if (finalMat == null) {
				finalMat = new Mat(mat.rows(), mat.cols(), 
						           mat.type(), new Scalar(0,0));
			}
			Core.add(mat, finalMat, finalMat);			
			Imgcodecs.imwrite("output/blah" + segCnt++ + ".jpg", finalMat);
		}
		return finalMat;
	}
}
