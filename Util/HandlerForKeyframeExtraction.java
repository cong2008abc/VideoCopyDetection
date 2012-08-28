package Util;

import DataStructure.ShotInVideo;
import DataStructure.VideoInfo;
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

public class HandlerForKeyframeExtraction extends DataSourceHandler 
										implements BufferTransferHandler {
	
	private static final int MaxDistanceKeyFrame = 50;		// maximum distance between key frames	
	private int imgWidth;
    private int imgHeight;
    private int WxH;
		
	PullBufferStream pullStrms[] = null;
	PushBufferStream pushStrms[] = null;
	Buffer readBuffer;
	ProcInfo mPInfo; // for transferData
	private VideoInfo videoInfo;
	private VideoKeyFrameStep1 ob1;
	
	private int shotIndexHSV = 0;    
	// because useFrameData is called recursively, shotIndexRGB should be global.
	private int j = 0;
	private int k = 0;
	private int m = 0;
	private int n = 0;
	private long startFrameNumber = 0;
	private long preFrameNumber = 0; 
	private long nextFrameNumber = 0;
	private long temp1; // the first frame in a current shot (lower boundary in a shot)
	private long temp2; // the first frame in a next shot (upper outer boundray in a shot)

	public HandlerForKeyframeExtraction(String input1, VideoKeyFrameStep1 input2) {
		super(input1);
		
		this.videoInfo = new VideoInfo();
    	this.ob1 = input2;
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
			
			try{
				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream("video_new_" + this.mVideoName + ".dat"));			
				oos.writeObject(videoInfo);
				oos.close();
				System.exit(0);
			}catch(Exception e){System.out.println("Cannot write files : " +e);}			
		}
        
		// Do useful stuff or wait
		useFrameData(mPInfo, inBuffer);
	}
	
    protected void useFrameData(ProcInfo pInfo, Buffer inBuffer) {
		try	{
			if(inBuffer.getData()!=null){				
				System.arraycopy((int[])inBuffer.getData(), 0, pInfo.outvid, 0, pInfo.outvid.length);
				// save the first frame as a key frame
				if(inBuffer.getSequenceNumber() == 0){					
					videoInfo.shot = new ShotInVideo[NumberOfShot];
					temp1=ob1.shot[k].firstFrame;
					temp2=ob1.shot[k].keyFrame[m];					
				}				
				// if k is higher that total number of frames, then return to transferData
				if(k == ob1.shotBoundaryHSV.length) return;				
				
				preFrameNumber=ob1.shotBoundaryHSV[k];// the first frame in a current shot
				
				if(k<ob1.shotBoundaryHSV.length-1){
					nextFrameNumber=ob1.shotBoundaryHSV[k+1]; // the first frame in a next shot
					// frames within one shot
					if(inBuffer.getSequenceNumber()>=preFrameNumber && inBuffer.getSequenceNumber()<nextFrameNumber){
						long gap = inBuffer.getSequenceNumber()-temp1; // distance of abscence of key frames\
						// the first frame of a shot
						if(inBuffer.getSequenceNumber()==ob1.shot[k].firstFrame){ // obtain key frames by taking frmes per every 30 frames				
							if(videoInfo.shot[k]==null){
								videoInfo.shot[k] = new ShotInVideo();
							}
							HSVHistogram(pInfo, 256); // this will use "global variable of outvidHSVHistogram
							AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
							AddYCbCrAverage(pInfo.outvid,k);
							System.out.println("FirstkeyFrame="+inBuffer.getSequenceNumber()+"   shotIndexHSV="+k);
							return;
						}
						// 50th frame from the last key frame
						if(gap%MaxDistanceKeyFrame==0 && inBuffer.getSequenceNumber()<temp2){
							HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
							AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
							AddYCbCrAverage(pInfo.outvid,k);
							AddMV(pInfo.outvid,pInfo.previd,k);
							System.out.println("keyFrame="+inBuffer.getSequenceNumber()+"   shotIndexHSV="+k);
							return;
						}
						// from the second key frame
						if((ob1.shot[k].keyFrame[m]==inBuffer.getSequenceNumber()) && (inBuffer.getSequenceNumber()!=ob1.shot[k].firstFrame)){
							temp1=ob1.shot[k].keyFrame[m];
							if(videoInfo.shot[k]==null){
								videoInfo.shot[k] = new ShotInVideo();
							}
							HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
							AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
							AddYCbCrAverage(pInfo.outvid,k);
							AddMV(pInfo.outvid,pInfo.previd,k);
							System.out.println("keyFrame="+ob1.shot[k].keyFrame[m]+"   shotIndexHSV="+k);
							// reset temp 2 to the first frame of a next shot
							if(m<ob1.shot[k].keyFrame.length-1){
								m++;
								temp2=ob1.shot[k].keyFrame[m];
							}
							else temp2 = ob1.shot[k+1].keyFrame[0];						
						}
					}
					// frames within two different shots
					else{
						k++; m=0;				
						System.out.println();
						// reset the first frame in a current shot
						temp1=ob1.shot[k].firstFrame;
						long gap = inBuffer.getSequenceNumber()-temp1;
						// first frame
						if(inBuffer.getSequenceNumber()==ob1.shot[k].firstFrame){ // obtain key frames by taking frames per every 30 frames				
							if(videoInfo.shot[k]==null){
								videoInfo.shot[k] = new ShotInVideo();
							}
							HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
							AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
							AddYCbCrAverage(pInfo.outvid,k);
							System.out.println("FirstkeyFrame="+inBuffer.getSequenceNumber()+"   shotIndexHSV="+k);
							return;
						}
						// 50th frame from the last key frame
						if(gap%MaxDistanceKeyFrame==0 && inBuffer.getSequenceNumber()<temp2){
							HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
							AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
							AddYCbCrAverage(pInfo.outvid,k);
							AddMV(pInfo.outvid,pInfo.previd,k);
							System.out.println("keyFrame="+inBuffer.getSequenceNumber()+"   shotIndexHSV="+k);
							return;
						}
						// from the second key frames
						if((ob1.shot[k].keyFrame[m]==inBuffer.getSequenceNumber()) && (inBuffer.getSequenceNumber()!=ob1.shot[k].firstFrame)){
							temp1=ob1.shot[k].keyFrame[m];
							if(videoInfo.shot[k]==null){
								videoInfo.shot[k] = new ShotInVideo();
							}
							HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
							AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
							AddYCbCrAverage(pInfo.outvid,k);
							AddMV(pInfo.outvid,pInfo.previd,k);
							System.out.println();
							System.out.println("keyFrame="+ob1.shot[k].keyFrame[m]+"   shotIndexHSV="+k);
							if(m<ob1.shot[k].keyFrame.length-1){
								m++;
								temp2=ob1.shot[k].keyFrame[m];
							}
							else temp2 = ob1.shot[k+1].keyFrame[0];						
						}
					}
				}
				else{					
					long gap = inBuffer.getSequenceNumber()-temp1;
					if(inBuffer.getSequenceNumber()==ob1.shot[k].firstFrame){ // obtain key frames by taking frmes per every 30 frames				
						if(videoInfo.shot[k]==null){
							videoInfo.shot[k] = new ShotInVideo();
						}
						HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
						AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
						AddYCbCrAverage(pInfo.outvid,k);
						System.out.println("FirstkeyFrame="+inBuffer.getSequenceNumber()+"   shotIndexHSV="+k);
						return;
					}
					if(gap%MaxDistanceKeyFrame==0 && inBuffer.getSequenceNumber()<temp2){
						HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
						AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
						AddYCbCrAverage(pInfo.outvid,k);
						AddMV(pInfo.outvid,pInfo.previd,k);
						System.out.println("keyFrame="+inBuffer.getSequenceNumber()+"   shotIndexHSV="+k);
						return;
					}
					if((ob1.shot[k].keyFrame[m]==inBuffer.getSequenceNumber()) && (inBuffer.getSequenceNumber()!=ob1.shot[k].firstFrame)){
						temp1=ob1.shot[k].keyFrame[m];
						if(videoInfo.shot[k]==null){
							videoInfo.shot[k] = new ShotInVideo();
						}
						HSVHistogram(pInfo,256); // this will use "global variable of outvidHSVHistogram
						AddHSVHistogram(pInfo.outvidHSVHistogram,k,inBuffer.getSequenceNumber());					
						AddYCbCrAverage(pInfo.outvid,k);
						AddMV(pInfo.outvid,pInfo.previd,k);
						System.out.println("keyFrame="+ob1.shot[k].keyFrame[m]+"   shotIndexHSV="+k);
						if(m<ob1.shot[k].keyFrame.length-1){
							m++;
							temp2=ob1.shot[k].keyFrame[m];
						}
						else temp2 = temp1*10000;
					}
				}
				System.arraycopy(pInfo.outvid,0,pInfo.previd,0,WxH);
				if(inBuffer.getSequenceNumber()%500 ==0) System.gc();				
			}
			// when all features are extracted, genreate signatures of all key frames in a video.
			AddYCCSignature(videoInfo);
		}		
		catch(Exception e){System.out.println(e);}	
	}
	public void AddYCCSignature(VideoInfo ob2){
		// add binary image signature using extreme quantization.
		int k=0;
    	while(ob2.shot[k]!=null){
			double[][] signatureD = new double[ob2.shot[k].YCbCrAverage.length][25];// use 25 bits of signature of one frame.
			boolean[][] signatureB = new boolean[ob2.shot[k].YCbCrAverage.length][25]; // quantize and change double to boolean data
			for(int m=0; m<ob2.shot[k].YCbCrAverage.length;m++){
				for(int j=0;j<15;j++){
					for(int i=0;i<20;i++){
						int signatureIndex = j/3*(5)+i/4;
						signatureD[m][signatureIndex] = signatureD[m][signatureIndex]+ob2.shot[k].YCbCrAverage[m][j*20+i];
					}
				}
				double[] temp = new double[25];
				System.arraycopy(signatureD[m],0,temp,0,25);
				java.util.Arrays.sort(temp);
				double medianSignature=temp[12];
			
				for (int i=0;i<25;i++){
					if (signatureD[m][i]>medianSignature) signatureB[m][i]=true;
					else signatureB[m][i]=false;
				}
			}
			ob2.shot[k].YCbCrSignature=signatureB;
			k++;		
    	}
	}
	
	public void AddMV(int[] outvid, int[] previd, int shotIndex){
		// obtain blocking-matching SSE between previous frame and current frame from Y component
		int ROutvid,GOutvid,BOutvid;
		int RPrevid,GPrevid,BPrevid;
		double[] YOutvid =  new double[WxH];
		double[] YPrevid =  new double[WxH];
		
		int blockSize = 16;
		int YCbCrSize = WxH/blockSize/blockSize;
		int[] MV = new int[YCbCrSize];
		// extract Y component only		
		for(int ly=0; ly<imgHeight; ++ly) {
			for(int lx=0; lx<imgWidth; ++lx) {
				ROutvid = ( (outvid[ly * imgWidth + lx] >> 16) & 0xff );
			   	GOutvid = ( (outvid[ly * imgWidth + lx] >>  8) & 0xff );
			   	BOutvid = ( (outvid[ly * imgWidth + lx]      ) & 0xff );			
				YOutvid[ly * imgWidth + lx] =  (16.0 + 1/256.0*(65.738*ROutvid + 129.057*GOutvid + 25.064*BOutvid))/256.0;
				RPrevid = ( (previd[ly * imgWidth + lx] >> 16) & 0xff );
			   	GPrevid = ( (previd[ly * imgWidth + lx] >>  8) & 0xff );
			   	BPrevid = ( (previd[ly * imgWidth + lx]      ) & 0xff );			
				YPrevid[ly * imgWidth + lx] =  (16.0 + 1/256.0*(65.738*RPrevid + 129.057*GPrevid + 25.064*BPrevid))/256.0;				
			}
		}
		// Initialization of average value.
		double[] previdMacroBlock = new double[blockSize*blockSize];
		double[] outvidMacroBlock = new double[blockSize*blockSize];
		
		double[] tempDistance = new double[9];
		int indexMV=0;		
		for(int ly=0; ly<imgHeight; ly=ly+16){
			for(int lx=0; lx<imgWidth; lx=lx+16){
				for(int ky=0; ky<blockSize; ky++){
					for(int kx=0; kx<blockSize; kx++){
						outvidMacroBlock[kx+ky*blockSize]=YOutvid[lx+kx+(ly+ky)*imgWidth];
						previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx+(ly+ky)*imgWidth];
						// order of index comparing to MV
						// 0:[0,0], 1:[0,1], 2:[1,0], 3:[1,1], 4:[-1,-1], 5:[-1,0];, 6:[0,-1]
						// 7:[-1,1], 8:[1,-1];
						tempDistance[0] = measureDistance(outvidMacroBlock, previdMacroBlock);
						// if matching block is out of bound, then put 1 (maximum value) of distance
					}
				}
				// if upper most blocks
				if(ly < imgHeight-blockSize){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx+(ly+ky+1)*imgWidth];							
							tempDistance[1] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}
				else tempDistance[1]=1; // maximum value is assigned when matching block is out of bound
				
				// if right most blocks
				if(lx < imgWidth-blockSize){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx+1+(ly+ky)*imgWidth];							
							tempDistance[2] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}
				else tempDistance[2]=1; // maximum value is assigned when matching block is out of bound
				
				// if right upper block
				if(ly < imgHeight-blockSize && lx < imgWidth-blockSize){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx+1+(ly+ky+1)*imgWidth];
							tempDistance[3] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}				
				else tempDistance[3]=1;
				
				// left lower block
				if(ly>0 && lx>0){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx-1+(ly+ky-1)*imgWidth];
							tempDistance[4] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}				
				else tempDistance[4]=1;
				
				// order of index comparing to MV
				// 0:[0,0], 1:[0,1], 2:[1,0], 3:[1,1], 4:[-1,-1], 5:[-1,0];, 6:[0,-1]
				// 7:[-1,1], 8:[1,-1];
				
				// left most block
				if(lx>0){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx-1+(ly+ky)*imgWidth];
							tempDistance[5] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}				
				else tempDistance[5]=1;
				
				// lower block
				if(ly>0){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx+(ly+ky-1)*imgWidth];
							tempDistance[6] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}				
				else tempDistance[6]=1;
				
				// left upper block
				if(lx>0 && ly < imgHeight-blockSize){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx-1+(ly+ky+1)*imgWidth];
							tempDistance[7] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}				
				else tempDistance[7]=1;
				
				// right lower block
				if(lx<imgWidth-blockSize && ly>0){
					for(int ky=0; ky<blockSize; ky++){
						for(int kx=0; kx<blockSize; kx++){							
							previdMacroBlock[kx+ky*blockSize]=YPrevid[lx+kx+1+(ly+ky-1)*imgWidth];
							tempDistance[8] = measureDistance(outvidMacroBlock, previdMacroBlock);							
						}
					}
				}				
				else tempDistance[8]=1;
				// find minimum value of mse
				double minDistanceMV=1;
								
				for(int k=0; k<9; k++){
					if(minDistanceMV>tempDistance[k]){
						minDistanceMV = tempDistance[k];
						MV[indexMV]=k;
					}
				}
				indexMV++;
			}
		}
		int[] MVhist = new int[9];
		
		for (int k=0; k<YCbCrSize;k++){
			MVhist[MV[k]]=MVhist[MV[k]]+1;
		}
		
		// put MV to videoInfo
		if(videoInfo.shot[shotIndex].MV==null){
			videoInfo.shot[shotIndex].MV = new int[1][9]; // memory allocation for 320x240/(8x8) x 3 (Y,Cb,Cr)
			System.arraycopy(MVhist,0,videoInfo.shot[shotIndex].MV[0],0,9);
		}
		else{
			int newSize=videoInfo.shot[shotIndex].MV.length+1;
			int[][] temp = new int[newSize][9];
			for(int j=0;j<videoInfo.shot[shotIndex].MV.length;j++){
				System.arraycopy(videoInfo.shot[shotIndex].MV[j],0,temp[j],0,9);
			}
			System.arraycopy(MVhist,0,temp[newSize-1],0,9);
			videoInfo.shot[shotIndex].MV = temp;
		}
	}
	
	public double measureDistance(double[] currentBlock, double[] previousBlock){
		double mse=0;
		for(int i=0; i<currentBlock.length; i++){
			mse=mse+Math.abs(currentBlock[i]-previousBlock[i]);
		}
		mse=mse/256.0;
		return mse;
	}	
	
	public void AddYCbCrAverage(int[] pixel, int shotIndex) {
		int R,G,B;
		double[] Y =  new double[WxH];
		double[] Cb = new double[WxH];
		double[] Cr = new double[WxH];
		
		int blockSize = 16;
		int YCbCrSize = WxH/blockSize/blockSize;
		double[] YAverage = new double[YCbCrSize];// 8x8 is a block size.
		double[] CbAverage = new double[YCbCrSize];
		double[] CrAverage = new double[YCbCrSize];
		double[] YCbCrAverageValue = new double [YCbCrSize];
		// Read RGB values and change to YCbCr
		for(int ly=0; ly<imgHeight; ++ly) {
			for(int lx=0; lx<imgWidth; ++lx) {
				R = ( (pixel[ly * imgWidth + lx] >> 16) & 0xff );
			   	G = ( (pixel[ly * imgWidth + lx] >>  8) & 0xff );
			   	B = ( (pixel[ly * imgWidth + lx]      ) & 0xff );			
				Y[ly * imgWidth + lx] =  (16.0 + 1/256.0*(65.738*R + 129.057*G + 25.064*B))/256.0;
				Cb[ly * imgWidth + lx] = (128.0 + 1/256.0*(-37.945*R - 74.494*G + 112.439*B))/256.0;
				Cr[ly * imgWidth + lx] = (128.0 + 1/256.0*(112.439*R - 94.154*G - 18.285*B))/256.0;
			}
		}
		// Initialization of average value.
		for(int k=0;k<YCbCrSize;k++){
			YAverage[k]=0;CbAverage[k]=0;CrAverage[k]=0;
		}
		for(int ly=0; ly<imgHeight; ++ly) {
			for(int lx=0; lx<imgWidth; ++lx) {
				int YCCindex = ly/blockSize*(imgWidth/blockSize)+lx/blockSize;
				YAverage[YCCindex] = YAverage[YCCindex]+Y[ly * imgWidth + lx];
				CbAverage[YCCindex] = CbAverage[YCCindex]+Cb[ly * imgWidth + lx];
				CrAverage[YCCindex] = CrAverage[YCCindex]+Cr[ly * imgWidth + lx];
			}
		}
		for(int k=0;k<YCbCrSize;k++){
			YCbCrAverageValue[k] = (2.0/3.0*YAverage[k] + 1.0/6.0*CbAverage[k] + 1.0/6.0*CrAverage[k])/((double)blockSize*blockSize);			
		}
		// put average value of YCbCr in to videoInfo.
		if(videoInfo.shot[shotIndex].YCbCrAverage==null){
			videoInfo.shot[shotIndex].YCbCrAverage = new double[1][YCbCrSize]; // memory allocation for 320x240/(8x8) x 3 (Y,Cb,Cr)
			System.arraycopy(YCbCrAverageValue,0,videoInfo.shot[shotIndex].YCbCrAverage[0],0,YCbCrSize);
		}
		else{
			int newSize=videoInfo.shot[shotIndex].YCbCrAverage.length+1;
			double[][] temp = new double[newSize][YCbCrSize];
			for(int j=0;j<videoInfo.shot[shotIndex].YCbCrAverage.length;j++){
				System.arraycopy(videoInfo.shot[shotIndex].YCbCrAverage[j],0,temp[j],0,YCbCrSize);
			}
			System.arraycopy(YCbCrAverageValue,0,temp[newSize-1],0,YCbCrSize);
			videoInfo.shot[shotIndex].YCbCrAverage = temp;
		}
	}
	
	// Add HSV histogram of key frames to a shot
	public void AddHSVHistogram(int[] HSVHist, int shotIndex, long frameNumber) {
		if(videoInfo.shot[shotIndex].HSVHistogram==null){
			videoInfo.shot[shotIndex].HSVHistogram = new int[1][256];
			System.arraycopy(HSVHist,0,videoInfo.shot[shotIndex].HSVHistogram[0],0,256);			
		}
		else{
			int newSize=videoInfo.shot[shotIndex].HSVHistogram.length+1;
			int[][] temp = new int[newSize][256];
			for(int j=0;j<videoInfo.shot[shotIndex].HSVHistogram.length;j++){
				System.arraycopy(videoInfo.shot[shotIndex].HSVHistogram[j],0,temp[j],0,256);
			}
			System.arraycopy(HSVHist,0,temp[newSize-1],0,256);
			videoInfo.shot[shotIndex].HSVHistogram = temp;
		}
	}	

	/** Get Histogram of each frame */ 
	void HSVHistogram(ProcInfo pInfo, int binSize) {	
		// global variable : outvidHistogram		
		int t1R;int t1G;int t1B;
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
}
