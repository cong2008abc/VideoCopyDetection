package DataStructure;

import DataStructure.ShotInVideo;

import java.io.Serializable;

public class VideoInfo implements Serializable{
	public ShotInVideo[] shot;	
	public long[] shotBoundaryHSV; // dynamic long array, keep frame numbers of shot boundaries    
}
