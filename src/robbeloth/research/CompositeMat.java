package robbeloth.research;

import java.util.ArrayList;
import java.util.Iterator;

import org.opencv.core.Mat;

public class CompositeMat {
	private ArrayList<Mat> listofMats;
	private Mat mat;
	
	public CompositeMat() {
		listofMats = new ArrayList<Mat>();
		mat = new Mat();
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
		this.mat = mat.clone();
	}

	public ArrayList<Mat> getListofMats() {
		return listofMats;
	}

	public Mat getMat() {
		return mat;
	}
	
	
}
