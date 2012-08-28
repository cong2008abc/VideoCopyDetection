import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.control.TrackControl;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;

import DataStructure.ProcInfo;
import DataStructure.VideoKeyFrameStep1;
import Util.HandlerForKeyframeExtraction;
import Util.StateWaiter;


public class KeyframeExtraction implements ControllerListener {
    private String mVideoName;						// video name of one video for pre-processing    
    
    private MediaLocator mMediaLocator;
    private VideoKeyFrameStep1 ob1;
    private ProcInfo mProcInfo;             
	
	public KeyframeExtraction(String input1, String input2) 
			throws IOException, ClassNotFoundException {
		
		this.mMediaLocator = createMediaLocator(input1);
		this.mVideoName = input1.substring(5, input1.length());
		ObjectInputStream ois = 
				new ObjectInputStream(new FileInputStream(input2));
    	this.ob1 = (VideoKeyFrameStep1)ois.readObject();
    	ois.close();
	}
	
	private static MediaLocator createMediaLocator(String url) {
		MediaLocator ml = null;
		if(url.indexOf(":") > 0 && (ml = new MediaLocator(url)) != null) { 
			return ml;
		}
		else {
			if(url.startsWith(File.separator)) {
		    	if ((ml = new MediaLocator("file:" + url)) != null) {
		    		return ml;
		    	}			    	
			} else {
		    	String file = "file:" + System.getProperty("user.dir") + File.separator + url;
		    	if ((ml = new MediaLocator(file)) != null) {
		    		return ml;
		    	}		    	
			}		
		}		
		return null;
    }
	
	public void run() {
		if(!init()) {
			System.err.println("Failed to proceed the inputs");
		}
		
		if(!execute()){
			System.err.println("Failed to extract the tracks.");
		}
	}
	
	protected boolean init() {
		// Build the ProcInfo data structure for each processor.
		this.mProcInfo = new ProcInfo();		
		this.mProcInfo.ml = this.mMediaLocator;
		
		try {
			System.err.println("- Create processor for: " + this.mMediaLocator);
			this.mProcInfo.p = Manager.createProcessor(this.mMediaLocator);
	    } catch (Exception e) {
			System.err.println("Yikes!  Cannot create a processor from the given url: " + e);
			return false;
    	}
		return true;
	}
	
	protected boolean execute() {										    	    
		TrackControl tcs[];

		this.mProcInfo.p.addControllerListener(this);
	    if (!waitForState(this.mProcInfo.p, this.mProcInfo.p.Configured)) {
			System.err.println("- Failed to configure the processor.");
			return false;
	    }
		
	    this.mProcInfo.p.setContentDescriptor(new ContentDescriptor(ContentDescriptor.RAW));
	    
	    tcs = this.mProcInfo.p.getTrackControls();
	    
//	  	Extract Video track from a video. (other tracks are set to disable)
	    for(int j = 0; j < tcs.length; j++) {
			if(tcs[j].getFormat() instanceof VideoFormat)
				tcs[j].setFormat(
						new RGBFormat(null,-1,Format.intArray,-1.0F,32,0x00FF0000, 0x0000FF00, 0x000000FF));					
			else tcs[j].setEnabled(false);
		}
	    
	    if (!waitForState(this.mProcInfo.p, this.mProcInfo.p.Realized)) {
			System.err.println("- Failed to realize the processor.");
			return false;
    	}
	    
	    this.mProcInfo.ds = (PushBufferDataSource)this.mProcInfo.p.getDataOutput();
	    HandlerForKeyframeExtraction handler = new HandlerForKeyframeExtraction(mVideoName, ob1);
		try	{
			handler.setSource(this.mProcInfo, this.mProcInfo.ds);	// also determines image size				
		}
		catch(IncompatibleSourceException e){
			System.out.println("Cannot handle the output DataSource from the processor: " + this.mProcInfo.ds);
			return false;
		}		
					
		handler.start();
	
		// Prefetch the processor.
		this.mProcInfo.p.prefetch();
	
		if(!waitForState(this.mProcInfo.p, this.mProcInfo.p.Prefetched)){
			System.out.println("Failed to prefetch the processor.");
			return false;
		}

		// Start the processor			
		this.mProcInfo.p.start();
	    	
		return true;
	}
	
	private boolean waitForState(Processor p, int state) {
		return (new StateWaiter(p)).waitForState(state);
    }
	
    public void controllerUpdate(ControllerEvent evt) {
		if (evt instanceof ControllerErrorEvent) {
	    	System.err.println("Failed to extract the files.");
	    	System.exit(-1);
		}
		else if (evt instanceof EndOfMediaEvent){
			//evt.getSourceController().close();
		}
    }
    
    public static void main(String[] args) 
    		throws IOException, ClassNotFoundException {
    	
    	if(args.length < 2) {
    		System.err.println("THe Arguments are less than 2.");
    		System.exit(0);
    	}
    	
    	KeyframeExtraction kf = new KeyframeExtraction(args[0], args[1]);
    	kf.run();
    }
}
