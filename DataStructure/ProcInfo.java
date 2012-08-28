package DataStructure;

import java.awt.Image;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;

import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import DataStructure.Shot;

public class ProcInfo {
	public MediaLocator ml;
	public Processor p;
	public PushBufferDataSource ds;
	public TrackControl tc;
	public PushBufferStream pbs;	
	public DirectColorModel dcm = new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF);
	public MemoryImageSource sourceImage;
	public Image outputImage;
	public int[] outvid;
	public int[] previd;
	public int[] outvidRGBHistogram;
	public int[] prevideoRGBHistogram;
	public int[] outvidHSVHistogram;
	public int[] prevideoHSVHistogram;
	public Shot[] shot;
	public long[] shotBoundaryRGB; // dynamic long array, keep frame numbers of shot boundaries
	public long[] shotBoundaryHSV; // dynamic long array, keep frame numbers of shot boundaries
}
