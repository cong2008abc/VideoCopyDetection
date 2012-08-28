// Programmed by Hungsik Kim
// Gaussian Fliter
import java.io.*;

public class GaussianFilter {
    
    double[] g;
    /**
     * Creates a new instance of <code>GaussianFilter</code>.
     */
    public GaussianFilter(double sigma, int length) {
    	// make gaussian filter
    	double[] gTemp = new double[length];
    	for(int i=0;i<length;i++){
    		gTemp[i]=1.0/(Math.pow(2.0*Math.PI, 1.0/2.0)*sigma)*Math.exp(-Math.pow((double)i,2.0)/(2.0*Math.pow(sigma,2.0)));
    	}
    	
    	g = new double[2*length-1];
    	System.arraycopy(gTemp,0,g,length-1,length);
    	for(int i=0;i<length-1;i++){
    		g[i]=gTemp[length-i-1];
    	}
    }
    
    public void conv(double[] sum, double[] difference, double[] g){    	
    	// add symmetric values on both side of input vector
    	double[] d = new double[difference.length+g.length-1];
    	int iStart=(int)g.length/2;
    	System.arraycopy(difference,0,d,iStart,difference.length);
    	for(int i=0;i<iStart;i++){
    		d[i]=difference[iStart-i];
    		d[d.length-iStart+i]=difference[difference.length-2-i];
    	}
		// length of sum is same as length of difference (not d)
    	int m=0;
    	for(int i=0; i<difference.length;i++){
    		for(int k=i;k<i+g.length;k++){
    			sum[m]=sum[m]+d[k]*g[k-i];
    		}
    		m++;
    	}
    	System.out.println();
    }
}
