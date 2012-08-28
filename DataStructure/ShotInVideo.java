package DataStructure;

import java.io.Serializable;

public class ShotInVideo implements Serializable{		
	public int[][] HSVHistogram; // new int[shotNumber][sizeOfHistogram], dynamic 2D int array	
	public long[] keyFrame; // dynamic long array, keep frame numbers of video.
	public double[][] YCbCrAverage;	
	public boolean[][] YCbCrSignature;
	public int[][] MV;
}
