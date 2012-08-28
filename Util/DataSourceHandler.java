package Util;

import DataStructure.VideoKeyFrameStep1;

import javax.media.Buffer;
import javax.media.IncompatibleSourceException;
import javax.media.protocol.DataSource;

import DataStructure.ProcInfo;

abstract public class DataSourceHandler {
	protected final boolean RunHSVHistogram = true; 	// HSV histogram for shot boundary
	protected final int NumberOfShot = 1000;       		// maximum number of shots in one video
	protected final int MinNumberOfFrames = 20; 		// minimum number of frames in one shot
	
	protected String mVideoName;	
	protected DataSource source;			

	public DataSourceHandler(String vName) {
		this.mVideoName = vName;		
	}
	/**
	* Sets the media source this MediaHandler should use to obtain content.
	*/
	abstract public void setSource(ProcInfo pInfo, DataSource source) throws IncompatibleSourceException;
	
	abstract protected void useFrameData(ProcInfo pInfo, Buffer inBuffer);

	public void start() {		
		try{source.start();}catch(Exception e){System.out.println(e);}
	}
	
	public void stop() {		
		try{source.stop();}catch(Exception e){System.out.println(e);}
	}	
	
	public void close() {
		stop();
	}
	
	public Object[] getControls() {		
		return new Object[0];
	}	    
	
	public Object getControl(String name) {	
		return null;
	}
}
