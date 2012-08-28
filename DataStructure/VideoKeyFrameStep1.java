package DataStructure;

import DataStructure.ShotForKeyFrame;
import java.io.Serializable;

public class VideoKeyFrameStep1 implements Serializable{
	public ShotForKeyFrame[] shot;
	public long[] shotBoundaryRGB; // dynamic long array, keep frame numbers of shot boundaries
	public long[] shotBoundaryHSV; // dynamic long array, keep frame numbers of shot boundaries
	
}
