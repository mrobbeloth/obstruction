package robbeloth.research;
import java.awt.Point;
import java.util.ArrayList;

/**
 * This class provides skeletonization operators
 * @author Michael Robbeloth
 * @category Projects
 * @since 10/03/2014 
 * @version 1.0
 * <br/><br/>
 * Class: CS7900<br/>
 * <h2>Revision History</h2><br/>
 * <b>Date						Revision</b>
 *
 */

public class Skeletonization extends AlgorithmGroup {

	// Neighbor bit values array
	private static int[] N = new int[]{32,64,128,16,0,1,8,4,2};
	
	/**
	 * Implementation of the K3M skeletonization algorithm 
	 * @param p -- input image data array converted to binary
	 * @param width  -- width of image
	 * @param length -- length of image
	 * @return -- the skeleton of the binary image
	 */
	public static int[] k3m (int[] p, int width, int length) {
		int q[] = new int[p.length];
		for(int asc = 0; asc < q.length; asc++) {
			q[asc] = p[asc];			
		}
		
		// termination flag
		boolean flag = true;

		// lookup arrays for the different phases 
		int[] A0 = new int[]{3,6,7,12,14,15,24,28,30,31,48,56,60,62,63,96,112,120,124, 
				              126,127,129,131,135,143,159,191,192,193,195,199,207,223,224,
		                      225,227,231,239,240,241,243,247,248,249,251,252,253,254};
		int[] A1 = new int[]{7, 14, 28, 56, 112, 131, 193, 224};
		int[] A2 = new int[]{7, 14, 15, 28, 30, 56, 60, 112, 120, 131, 135, 193, 
				              195, 224, 225, 240};
		int[] A3 = new int[]{7, 14, 15, 28, 30, 31, 56, 60, 62, 112, 120, 124, 
				              131, 135, 143, 193, 195, 199, 224, 225, 227, 240, 
				              241, 248};
		int[] A4 = new int[]{7, 14, 15, 28, 30, 31, 56, 60, 62, 63, 112, 120, 
				              124, 126, 131, 135, 143, 159, 193, 195, 199, 207, 
				              224, 225, 227, 231, 240, 241, 243, 248, 249, 252};
		int[] A5 = new int[]{7, 14, 15, 28, 30, 31, 56, 60, 62, 63, 112, 120, 
				              124, 126, 131, 135, 143, 159, 191, 193, 195, 199, 
				              207, 224, 225, 227, 231, 239, 240, 241, 243, 248, 
				              249, 251, 252, 254};
		int[] A1pix = new int[]{3, 6, 7, 12, 14, 15, 24, 28, 30, 31, 48, 56, 
				                 60, 62, 63, 96, 112, 120, 124, 126, 127, 129, 
				                 131, 135, 143, 159, 191, 192, 193, 195, 199, 
				                 207, 223, 224, 225, 227, 231, 239, 240, 241, 
				                 243, 247, 248, 249, 251, 252, 253, 254};

		//k3m depends on the use of straight binary values zero and one
		for (int i = 0; i < q.length; i++) {
			if (q[i] == 255) {
				q[i] = 0;
			}
			else {
				q[i] = 1;
			}
		}	
		
/*		System.out.print("[");
		for(int a = 0; a < width; a++) {
			System.out.print("[");
			for (int b = 0; b < length; b++) {
				System.out.print(q[b*width+a]+",");
			}
			System.out.print("],\n");
		}
		System.out.print("]\n");*/
		
		// Core of the algorithm here
		do {
			// Mark the borders here
			int[] border_array_phase0 = 
					mark_borders_phase(q, width, length, A0);
			
			int[] copy_border_array_phase0 = 
					new int[border_array_phase0.length];
			
			for (int yac = 0; yac < border_array_phase0.length; yac++) {
				copy_border_array_phase0[yac] = border_array_phase0[yac];
			}
			
			// Delete pixels having three sticking neighbors
			K3MContainer pod_phase1 = 
					deletion_phase(q, width, length, 
									copy_border_array_phase0, A1);
			
			// Delete pixels having three or four sticking neighbors
			K3MContainer pod_phase2 = 
					deletion_phase(pod_phase1.getImageData(), width, length, 
							       pod_phase1.getBorderData(), A2);
			
			// Delete pixels having three, four, or five sticking neighbors
			K3MContainer pod_phase3 = 
					deletion_phase(pod_phase2.getImageData(), width, length,
							       pod_phase2.getBorderData(), A3);
			
			// Delete pixels having three, four, five, or six sticking neighbors
			K3MContainer pod_phase4 = 
					deletion_phase(pod_phase3.getImageData(), width, length,
							       pod_phase3.getBorderData(), A4);
			
			// Delete pixels having 3 to 7 sticking neighbors
			K3MContainer pod_phase5 = 
					deletion_phase(pod_phase4.getImageData(), width, length,
							       pod_phase4.getBorderData(), A5);
			
			/* Preserve intermediate image data result for possible next 
			 * iteration */
			int[] imageDataPhase5 = pod_phase5.getImageData();
			for (int cpyCnt = 0; cpyCnt < q.length; cpyCnt++) {
				q[cpyCnt] = imageDataPhase5[cpyCnt];
			}
			
			/* test to terminate is no change from border marking
			   to removal of excess interior pixels */
			int totalPhase0 = 0;
			for (int asc2 = 0; asc2 < border_array_phase0.length; asc2++) {
				if (border_array_phase0[asc2] == 1) {
					totalPhase0++;
				}
			}
			
			int totalPhase5 = 0;
			int[] borderArrayData = pod_phase5.getBorderData();
			for (int asc3 = 0; asc3 < borderArrayData.length; asc3++) {
				if (borderArrayData[asc3] == 1) {
					totalPhase5++;
				}
			}
			
			System.out.println("Number border cells at beg=" + totalPhase0);
			System.out.println("Number border cells at end=" + totalPhase5);
			
			/* if the number of border pixels have not change from the 
			   marking border phase to the final deleting pixel phase, 
			   the image data array has not been modified and we can
			   terminate to thin everything down to its final one pixel
			   skeleton */
			if (totalPhase0 == totalPhase5) {
				flag = false;
			}
			
		} while (flag);
		
		// thin image to one-pexel width skeleton
		q = thinning_phase(q, width, length, A1pix);
		
		//restore the true binary gray scale value for viewing
		for (int i = 0; i < q.length; i++) {
			if (q[i] == 0) {
				q[i] = 255;
			}
			else {
				q[i] = 0;
			}
		}
		
		return q;
	}

	/**
	 * Thin the final skeletonization candidate down to a one pixel wide 
	 * skeleton
	 * @param p -- input image data array converted to binary
	 * @param width  -- width of image
	 * @param length -- length of image
	 * @param lookup_array -- emperically derived table of weights to mark
	 * a pixel for removal 
	 * @return
	 */
	private static int[] thinning_phase(int[] p, int width, int length, 
			 							int[] lookup_array) {
		int q[] = new int[p.length];
		
		// Work with a copy
		for (int cnt = 0; cnt < p.length; cnt++) {
			q[cnt] = p[cnt];
		}
		
		// for all pixels in the image
		for (int y = 1; y < length-1; y++) {
			for (int x = 1; x < width-1; x++) {
				// calculate the neighborhood weight
				int weight = 0;
				for(int i = -1; i < 2; i++) {
					for(int j = -1; j < 2; j++) {
						weight += N[((i+1)*3)+(j+1)] * q[(y+i)*width+(x+j)];
					}
				}
				
				// if the weight is in the one pixel lookup array, remove (x,y)
				for (int cnt = 0; cnt < lookup_array.length; cnt++) {
					if (weight == lookup_array[cnt]) {
						q[cnt] = 0;
					}
				}
			}
		}
		return q;
	}
	
	/**
	 * Mark the borders in the k3m algorithm
	 * @param p -- input image data array converted to binary
	 * @param width  -- width of image
	 * @param length -- length of image
	 * @param neighboor_lookup -- emperically derived table of weights to mark
	 * a pixel for removal 
	 * @return a mirror of the input image data array but with only those
	 * border pixels that need to be evaluated marked as such
	 */
	private static int[] mark_borders_phase(int[] p, int width, int length,
										    int[] neighboor_lookup) {
		int[] q = new int[p.length];
		int total=0;
		// for all the pixels in the image
		for (int y = 1; y < length-1; y++) {
			for (int x = 1; x < width-1; x++) {
				
				// get the next image pixel
				int pixel = p[y*width+x];	
				
				//System.out.println("BORDER: accessing element:" + (y*width+x) + " (" + y + "," + x + ")");
				// ignore background pixels
				if (pixel == 0) {
					continue;
				}
				
				// calculate the neighborhood weight of the pixel 
				int weight = 0;
					for(int i = -1; i < 2; i++) {
						for(int j = -1; j < 2; j++) {
							// weight calcuation (2) from k3m paper
							weight += N[((i+1)*3)+(j+1)] * 
											p[(y+i)*width+(x+j)];
						}								
					}
					
					for (int cnt = 0; cnt < neighboor_lookup.length; cnt++) {
						// is the weight present in the lookup array?
						if (weight == neighboor_lookup[cnt]) {
							// flag pixel as border
							q[y*width+x] = 1;
							// System.out.println(++total + ":" + "("+y+", "+x+")");
						}
					}
			}
		}
		
/*		int daCounter = 0;
		for (int foobar = 0; foobar < q.length; foobar++) {
			daCounter += q[foobar];
		}
		System.out.println("daCounter="+daCounter);*/
		
		return q;
	}
	
	/**
	 * Mark the borders in the k3m algorithm
	 * @param p -- input image data array converted to binary
	 * @param width  -- width of image
	 * @param length -- length of image
	 * @param border_array -- a mirror of the input image array but with 
	 * with only the candidate border pixels marked
	 * @param lookup_array -- emperically derived table of weights to mark
	 * a pixel for removal 
	 * @return a Container with the change skeletonization candidate and
	 * the remaining border pixels to check in the next phase/iteration 
	 */	
	private static K3MContainer deletion_phase(int[] p,int width, int length, 
			 							int[] border_array, 
										int[] lookup_array) {
		int q[] = new int[p.length];
		
		// work with a copy
		for (int z = 0; z < q.length; z++) {
				q[z] = p[z];
		}
		
		// For all pixels in the border array
		for(int y = 1; y < length-1; y++) {
			for (int x = 1; x < width-1; x++) {
				int weight = 0;
				int pixel = border_array[y*width+x];
				
				// skip pixels not marked as a border pixel
				if (pixel == 0) {
					continue;
				}
				
				// calculate the neighboorhood weight w(x,y)
				//System.out.println("PHASE: accessing element:" + (y*width+x) + " (" + y + "," + x + ")");
				for(int i = -1; i < 2; i++) {
					for(int j = -1; j < 2; j++) {				
						//System.out.println("PHASE: accessing neighbor:" + ((y+i)*width+(x+j)) + " (" + (y+i) + "," + (x+j) + ")");
						weight += N[((i+1)*3)+(j+1)] * 	q[(y+i)*width+(x+j)];
						//System.out.println("weight="+weight);
					}
				}
				
				// Is the weight w(x,y) present in lookup Array Ai
				//System.out.println("Weight="+weight);
				for (int cnt = 0; cnt < lookup_array.length; cnt++) {
					if (weight == lookup_array[cnt]) {
						// yes, set pixel (x,y) to background color
						//System.out.println("PHASE: Removing element:" + (y*width+x) + " (" + y + "," + x + ")");
						q[y*width+x] = 0;
						
						// Remove border pixel from dup. future consideration
						border_array[y*width+x] = 0;
					}
				}
			}
		}
/*		System.out.print("[");
		for(int a = 0; a < width; a++) {
			System.out.print("[");
			for (int b = 0; b < length; b++) {
				System.out.print(q[b*width+a]+",");
			}
			System.out.print("],\n");
		} 
		System.out.print("]\n"); */
		K3MContainer pod = new K3MContainer(q, border_array);
		return pod;
	}
}
