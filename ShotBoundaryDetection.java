import java.io.File;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.media.Buffer;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;

import Util.HandlerForShotBoundaryDetection;
import Util.StateWaiter;
import DataStructure.*;

public class ShotBoundaryDetection implements ControllerListener {
    private String mVideoName;						// video name of one video for pre-processing    
    
    private MediaLocator mMediaLocator;
    private ProcInfo mProcInfo;             
	
	public ShotBoundaryDetection(String inputString) {
		this.mMediaLocator = createMediaLocator(inputString);
		this.mVideoName = inputString.substring(5, inputString.length());
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
	    HandlerForShotBoundaryDetection handler = new HandlerForShotBoundaryDetection(mVideoName);
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
      
    public static void main(String[] args) {
    	if(args.length < 1) {
    		System.err.println("No argument.");
    		System.exit(0);
    	}
    	
    	ShotBoundaryDetection sbd = new ShotBoundaryDetection(args[0]);
    	sbd.run();
    }
}
