package DataStructure;

public class Shot {
	public long shotNumber;
	public int[][] RGBHistogram;
	public int[][] HSVHistogram; 
	// new int[shotNumber][sizeOfHistogram], dynamic 2D int array
	public int[][] frameBinary; 
	// dynamic int 2D array
	public long[] keyFrame; 
	// dynamic int array, keep frame numbers of video.
}
