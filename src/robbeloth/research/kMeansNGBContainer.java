package robbeloth.research;

import java.util.HashMap;
import java.util.Set;

import org.opencv.core.Mat;

public class kMeansNGBContainer {
	private Mat clustered_data;
	private HashMap<String, Mat> stats;
	
	public kMeansNGBContainer(Mat clustered_data, HashMap<String, Mat>stats){
		this.clustered_data = clustered_data.clone();
		this.stats = new HashMap<String, Mat>(stats.size());
		Set<String> keys = stats.keySet();
		for(String key : keys) {
			Mat value = stats.get(key);
			this.stats.put(key, value);
		}
	}

	public Mat getClustered_data() {
		return clustered_data;
	}

	public HashMap<String, Mat> getStats() {
		return stats;
	}	
}
