package Util;

import DataStructure.ShotForKeyFrame;
import DataStructure.VideoKeyFrameStep1;

import java.awt.Color;
import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import javax.media.Buffer;
import javax.media.IncompatibleSourceException;
import javax.media.format.VideoFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.DataSource;
import javax.media.protocol.PullBufferDataSource;
import javax.media.protocol.PullBufferStream;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import DataStructure.ProcInfo;

public class HandlerForShotBoundaryDetection extends DataSourceHandler 
											implements BufferTransferHandler {		
	private int imgWidth;
    private int imgHeight;
    private int WxH;
		
	PullBufferStream pullStrms[] = null;
	PushBufferStream pushStrms[] = null;
	Buffer readBuffer;
	ProcInfo mPInfo; // for transferData	
	private VideoKeyFrameStep1 videoInfo;
	private int shotIndexHSV = 0;

	public HandlerForShotBoundaryDetection(String vName) {
		super(vName);		
		this.videoInfo = new VideoKeyFrameStep1();
	}
	/**
	* Sets the media source this MediaHandler should use to obtain content.
	*/
	public void setSource(ProcInfo pInfo, DataSource source) 
			throws IncompatibleSourceException {
		
		// Different types of DataSources need to handled differently.
		if(source instanceof PushBufferDataSource) {		
			System.out.println("Push Buffer DataSource");
			pushStrms = ((PushBufferDataSource) source).getStreams();				
			
			// Set the transfer handler to receive pushed data from the push DataSource.
			pushStrms[0].setTransferHandler(this);
			// Set image size
			imageProfile((VideoFormat)pushStrms[0].getFormat());
			pInfo.outvid = new int[WxH];
			pInfo.previd = new int[WxH];
			this.mPInfo = pInfo;				
		}
		else
		if(source instanceof PullBufferDataSource){
			System.out.println("PullBufferDataSource!");
		
			// This handler only handles push buffer datasource.
			throw new IncompatibleSourceException();
		}
		
		this.source = source;
		readBuffer = new Buffer();			
	}
	
	/**
	* Sets image size
	*/
	private void imageProfile(VideoFormat vidFormat) {	
		System.out.println("Push Format "+vidFormat);
		Dimension d = (vidFormat).getSize();
		System.out.println("Video frame size: "+ d.width+"x"+d.height);
		imgWidth = d.width;
		imgHeight = d.height;
		WxH = imgWidth*imgHeight;		
	}
	
	/**
	* This will get called when there's data pushed from the PushBufferDataSource.
	*/
	public void transferData(PushBufferStream stream){
		try {		
			stream.read(readBuffer);
		}
		catch(Exception e) {		
			System.out.println(e);
			return;
		}

		// Just in case contents of data object changed by some other thread
		Buffer inBuffer = (Buffer)(readBuffer.clone());
		inBuffer.setFlags(readBuffer.getFlags() | Buffer.FLAG_NO_SYNC);

		// Check for end of stream
		if(readBuffer.isEOM()) {
		
			System.out.println("End of stream");
			System.out.println();
			
			///////////////////////////////////////////////
			// Show frame number of shot boundaries
			///////////////////////////////////////////////
			int shotNum = 0;
			if(RunHSVHistogram == true) {
				for(int j=0; j< mPInfo.shotBoundaryHSV.length; j++){
					System.out.println("video " + " ::  HSV shot boundary : " + mPInfo.shotBoundaryHSV[j]);
					shotNum++;
				}
				System.out.println();
				System.out.println("Number of shot boundaries : " + shotNum);
			}
			
			/////////
			// save object (video data and information)
			/////////////
			
			try {
				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream("video_"+ this.mVideoName +"_diff"+".dat"));
				oos.writeObject(this.videoInfo);
				oos.close();
				System.exit(0);
			} catch(Exception e) {
				System.out.println("Cannot write files : " +e);
			}
		}
        
		// Do useful stuff or wait
		useFrameData(mPInfo, inBuffer);
	}

	protected void useFrameData(ProcInfo pInfo, Buffer inBuffer) {
		try	{
			if(inBuffer.getData()!=null){				
				System.arraycopy((int[])inBuffer.getData(), 0, pInfo.outvid, 0, pInfo.outvid.length);				
									
				// compute HSV histogram with 256 bins (16(H)x4(S)x4(V))
				HSVHistogram(pInfo, 256); 
				// the first frame of a shot
				if(inBuffer.getSequenceNumber() == 0){					
					videoInfo.shot = new ShotForKeyFrame[NumberOfShot];
					// memory allication of shots and add key frame number
					AddFirstFrameNumber(shotIndexHSV, inBuffer.getSequenceNumber());
					if(RunHSVHistogram == true){						
						pInfo.prevideoHSVHistogram = new int[256];
						// keep two adjecent histogram features to compute difference.
						System.arraycopy(pInfo.outvidHSVHistogram, 0, pInfo.prevideoHSVHistogram, 0, 256);
						System.arraycopy(pInfo.outvid, 0, pInfo.previd, 0, WxH);// save current frame as raw data
						// first frame of a shot is a shot boundary
						System.out.println("HSV Shot Boundary : " + inBuffer.getSequenceNumber());
						AddShotBoundaryHSV(pInfo, inBuffer.getSequenceNumber()); // modify pInfo (for internal processing)
						AddShotBoundaryHSV(inBuffer.getSequenceNumber()); // modify videoInfo (containing output features)
					}
					return;
				}				
				// starting from 2nd frames of a shot
				if(RunHSVHistogram == true) {
					if(compareHistogram(pInfo.prevideoHSVHistogram, pInfo.outvidHSVHistogram, 0.92)==true) {
						System.out.println("HSV Shot Boundary : " + inBuffer.getSequenceNumber());
						if(pInfo.shotBoundaryHSV.length == NumberOfShot){
							System.out.println("The number of shots is more than expected");
							System.exit(-1);
						}
						if(inBuffer.getSequenceNumber()-pInfo.shotBoundaryHSV[pInfo.shotBoundaryHSV.length-1] < MinNumberOfFrames){
							System.out.println("This shot of " + inBuffer.getSequenceNumber() + " is not shot boundary.");
							double difference=compareHistogram(pInfo.prevideoHSVHistogram,pInfo.outvidHSVHistogram);
							AddDifference(shotIndexHSV,difference);
						}					
						else{
							AddShotBoundaryHSV(pInfo, inBuffer.getSequenceNumber()); // save information in pInfo
							AddShotBoundaryHSV(inBuffer.getSequenceNumber()); // save information in videoInfo
							shotIndexHSV++;// if one removes this, one video will be considered as one shot
							AddFirstFrameNumber(shotIndexHSV, inBuffer.getSequenceNumber()); // modify videoInfo
						}
					}
					else{
						double difference = 
								compareHistogram(pInfo.prevideoHSVHistogram, pInfo.outvidHSVHistogram);
						AddDifference(shotIndexHSV, difference);
					}
					System.arraycopy(pInfo.outvidHSVHistogram,0,pInfo.prevideoHSVHistogram,0,256);
				}
			}
		}		
		catch(Exception e){System.out.println(e);}	
	}
	
	// memory allication of shots and add key frame number
	public void AddFirstFrameNumber(int shotIndex, long frameNumber){
		if(videoInfo.shot[shotIndex]==null) videoInfo.shot[shotIndex] = new ShotForKeyFrame();
		videoInfo.shot[shotIndex].firstFrame = frameNumber;
		System.out.println("firstFrame="+videoInfo.shot[shotIndex].firstFrame);
	}
	
	public void AddDifference(int shotIndex, double diff){
		if(videoInfo.shot[shotIndex]==null) videoInfo.shot[shotIndex] = new ShotForKeyFrame();
		if(videoInfo.shot[shotIndex].difference == null){
			videoInfo.shot[shotIndex].difference = new double[1];
			videoInfo.shot[shotIndex].difference[0]=diff;
		}		
		else{
			int newSize=videoInfo.shot[shotIndex].difference.length+1;
			double[] temp = new double[newSize];
			System.arraycopy(videoInfo.shot[shotIndex].difference,0,temp,0,videoInfo.shot[shotIndex].difference.length);
			temp[videoInfo.shot[shotIndex].difference.length]=diff;
			videoInfo.shot[shotIndex].difference = temp;
		}
	}	
		
	public void AddShotBoundaryHSV(ProcInfo pInfo, long frameNumber){
		if(pInfo.shotBoundaryHSV == null){
			pInfo.shotBoundaryHSV = new long[1];
			pInfo.shotBoundaryHSV[0] = frameNumber;
		}
		else{
			int newSize=pInfo.shotBoundaryHSV.length+1;
			long[] temp = new long[newSize];
			System.arraycopy(pInfo.shotBoundaryHSV,0,temp,0,pInfo.shotBoundaryHSV.length);
			temp[pInfo.shotBoundaryHSV.length]=frameNumber;
			pInfo.shotBoundaryHSV = temp;			
		}
			
	}
	public void AddShotBoundaryHSV(long frameNumber){
		if(videoInfo.shotBoundaryHSV == null){
			videoInfo.shotBoundaryHSV = new long[1];
			videoInfo.shotBoundaryHSV[0] = frameNumber;
		}
		else{
			int newSize = videoInfo.shotBoundaryHSV.length + 1;
			long[] temp = new long[newSize];
			System.arraycopy(videoInfo.shotBoundaryHSV,0,temp,0,videoInfo.shotBoundaryHSV.length);
			temp[videoInfo.shotBoundaryHSV.length] = frameNumber;
			videoInfo.shotBoundaryHSV = temp;			
		}
			
	}

	/** Get Histogram of each frame */ 
	void HSVHistogram(ProcInfo pInfo, int binSize)	{		
		int t1R;
		int t1G;
		int t1B;
		
		int j;
		float[] HSVvalue = new float[3];
		int H,S,V;
		if(pInfo.outvidHSVHistogram==null) pInfo.outvidHSVHistogram = new int[binSize];
		for(j=0;j<pInfo.outvidHSVHistogram.length;j++) pInfo.outvidHSVHistogram[j]=0;
		
		for(j=0;j<WxH;j++){				
			t1R = ((pInfo.outvid[j] >> 20) & 0xff);
			t1G = ((pInfo.outvid[j] >> 12) & 0xff);
			t1B = ((pInfo.outvid[j] >> 4) & 0xff);
			HSVvalue=Color.RGBtoHSB(t1R,t1G,t1B,null);
			
			//H=(int)Math.floor((double)HSVvalue[0]*15.0);
			H=(int)Math.floor(HSVvalue[0]*16.0);
			if(H==16) H=15; // use numbers from 0 to 15 as both value and index)
			S=(int)Math.floor(HSVvalue[1]*4.0);
			if(S==4) S=3; // use numbers from 0 to 3 as both value and index)
			V=(int)Math.floor(HSVvalue[2]*4.0);
			if(V==4) V=3; // use numbers from 0 to 15 as both value and index)
			pInfo.outvidHSVHistogram[H*16+S*4+V]++;
		}
	}
	
	public boolean compareHistogram(int[] preHist, int[] currHist, double cri){// return true or false		
		int sumHist=0;
		double matchRate;
		for(int i=0;i<preHist.length;i++){
			if (preHist[i] > currHist[i]) sumHist+=currHist[i];
			else sumHist+=preHist[i];			
		}
		matchRate=(double)sumHist/(imgWidth*imgHeight);
		if(matchRate<cri) return true;
		else return false;			
	}
	
	public double compareHistogram(int[] preHist, int[] currHist){ // return value is between 0 and 1.		
		int sumHist=0;
		double matchRate;
		for(int i=0; i<preHist.length; i++){
			if (preHist[i] > currHist[i]) sumHist+=currHist[i];
			else sumHist+=preHist[i];			
		}
		matchRate=(double)sumHist/(imgWidth*imgHeight);
		return matchRate;
	}
}
