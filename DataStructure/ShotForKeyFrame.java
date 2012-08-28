package DataStructure;
import java.io.Serializable;

public class ShotForKeyFrame implements Serializable {	
	public double[] difference; // dynamic double array, keep frame difference between two consecutive video.
	public long firstFrame; // dynamic long array, keep frame numbers of video.
	public long[] keyFrame;
	
}
