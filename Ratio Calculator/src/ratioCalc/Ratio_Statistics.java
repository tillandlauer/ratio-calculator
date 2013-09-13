package ratioCalc;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.text.TextPanel;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TestUtils;

public class Ratio_Statistics
    {   
    private int files = 0; // number of files to be opened
    private String dir = ""; // dir for saving files manually
    private String[] xTitle; // x-labels for the different files (statistics)

	private int drawMax=0;
	private double normValue=0.0d;
	private double defaultScale=0.0d;
	private int maxPossibleValue=0;
	private int scaleOption=0;
	private long maskCounter=0;
	private long satCounter=0;
	public boolean logWindow=false;
	private String prefix="";
	private String memoryError="";
	private String title="";
    
    public Ratio_Statistics(int nFiles, String directory, String[] names)
    	{
    	files = nFiles;
    	dir = directory;
    	xTitle = names;
    	}

    
    public Ratio_Statistics(int drawMax2, double normValue2, double defaultScale2, int maxPossibleValue2, int scaleOption2, long maskCounter2, long satCounter2, boolean logWindow2, String prefix2, String memoryError2, String title2)
		{
    	drawMax = drawMax2;
    	normValue = normValue2;
		defaultScale = defaultScale2;
        maxPossibleValue = maxPossibleValue2;
        scaleOption = scaleOption2;
        maskCounter = maskCounter2;
        satCounter = satCounter2;
        logWindow = logWindow2;
        prefix = prefix2;
        memoryError = memoryError2;
    	title = title2;
		}
    
    
    public static double getMean(int[] data) // used for calculating the mean histogram, calculates the average of a bin 
        {
		int n = data.length;
		double sum=0.0d;
		for (int i=0; i<n; i++) 
		  {
		  sum += data[i];
		  }
		double result = sum/n;
		return result;
        }


    public static double getMean(double[] data) // used for calculating the mean histogram, calculates the average of a bin 
	    {
		int n = data.length;
		double sum=0.0d;
		for (int i=0; i<n; i++) 
		  {
		  sum += data[i];
		  }
		double result = sum/n;
		return result;
	    }


    public static double getSD(int[] data, double mean) // used for calculating the standard deviation of a mean histogram bin 
        {
		int n = data.length;
		double sum=0.0d;
		for (int i=0; i<n; i++) 
		  {
		  sum += (data[i]-mean)*(data[i]-mean);
		  }
		double stdDev = Math.sqrt(sum/(n-1));
		return stdDev;
        }

    
    public static double getSD(double[] data, double mean) // used for calculating the standard deviation of a mean histogram bin 
	    {
		int n = data.length;
		double sum=0.0d;
		for (int i=0; i<n; i++) 
		  {
		  sum += (data[i]-mean)*(data[i]-mean);
		  }
		double stdDev = Math.sqrt(sum/(n-1));
		return stdDev;
	    }     


    /**
     * Calculates the median of an array; the input already has to be sorted
     * <br>In case of ties the lower value is chosen
     *
     * @param matrix <code>double[]</code> array
     * @return Median 
	  * @see calcStats
     */
    protected static double getMedian(double[] matrix, boolean sort) // Get median value of a matrix.
       {
       if(sort) Arrays.sort(matrix);

       double medPos = (matrix.length+1)/2.0d;
       double median = 0;
       
       if (matrix.length % 2 == 1) // odd size
           {
           median = matrix[(int)medPos-1];
           }   
       else // even size
           {
           int bM = (int)Math.round(medPos-1.5d);
           int aM = (int)Math.round(medPos-0.5d);
           double vbm = matrix[bM];
           double vam = matrix[aM];
           median = (vbm+vam)/2.0d;
           }   
       return median;        
       }   


    /**
     * Calculates the median of an array; the input already has to be sorted
     * <br>In case of ties the lower value is chosen
     *
     * @param matrix <code>int[]</code> array
     * @return Median 
	  * @see calcStats
     */
    protected static double getMedian(int[] matrix, boolean sort) // Get median value of a matrix.Used by calcStats().
       {
       if(sort) Arrays.sort(matrix);

       double medPos = (matrix.length+1)/2.0d;
       double median = 0;
       
       if (matrix.length % 2 == 1) // odd size
           {
           median = matrix[(int)medPos-1];
           }   
       else // even size
           {
           int bM = (int)Math.round(medPos-1.5d);
           int aM = (int)Math.round(medPos-0.5d);
           double vbm = matrix[bM];
           double vam = matrix[aM];
           median = (vbm+vam)/2.0d;
           }   
       return median;        
       }

    
    public static double getSEMed(int[] data) // get standard error of the median. Used for calculating histogram in createFile().
        {
        // standard error of the median according to: Lothar Sachs, Angewandte Statistik, 11. Auflage 2003, S. 160. s = (a-b) / 3.4641; a = (n/2 + sqrt(3n)/2) th observation, b = (n/2 - sqrt(3n)/2) th observation, rounded up to the next number. 
        double a = (data.length/2.0d + Math.sqrt(3.0d*data.length)/2);
        double b = (data.length/2.0d - Math.sqrt(3.0d*data.length)/2);
        double sem = Math.abs((data[(int)Math.ceil(a)] - data[(int)Math.ceil(b)]) / 3.4641d);
		return sem;
        }


    public static double getSEMed(double[] data)  // get standard error of the median. Used for calculating statistics in createFile().
        {
        // standard error of the median according to: Lothar Sachs, Angewandte Statistik, 11. Auflage 2003, S. 160. s = (a-b) / 3.4641; a = (n/2 + sqrt(3n)/2) th observation, b = (n/2 - sqrt(3n)/2) th observation, rounded up to the next number. 
        double a = (data.length/2.0d + Math.sqrt(3.0d*data.length)/2);
        double b = (data.length/2.0d - Math.sqrt(3.0d*data.length)/2);
        double sem = Math.abs((data[(int)Math.ceil(a)] - data[(int)Math.ceil(b)]) / 3.4641d);
		return sem;
        }

    
    public boolean statTestT(ArrayList<ArrayList<Double>> dList, String addToFilename) // T-test on statistics of all files.
    {
    // the use of ArrayLists is necessary because the different files can contain different amounts of samples
    ArrayList<double[]> data = new ArrayList<double[]>(files); // will contain the data
    ArrayList<Double> tempList; // ArrayList for each file
    double[] tempData; // array for each file

    // read files from ArrayList dList and add them as double[] arrays into the ArrayList data
    for (int i=0; i<files; i++) // cycle through files
        {
        tempList = (ArrayList<Double>)dList.get(i); // load file into ArrayList
        tempData = new double[tempList.size()]; // create an array for the file
        for (int j=0; j<tempData.length; j++)
            {
            tempData[j] = (double)tempList.get(j);
            }
        data.add(tempData); // add to final ArrayList
        }

    // do the tests
    int t1 = 1, t2 = 1; // counter (test file 1 and test file 2)
    int fileCounter = 0; // counter
    String filename = "00 Statistics Summary "+addToFilename+"T-Test.txt"; // output file
    TextPanel tp = new TextPanel(); // content of output file
    double pValue, uStat;

    // Bonferroni correction
    double alpha = 0.05d;
    int nTests = files-1;
    for (int i=nTests-1; i>0; i--)
    	{
    	nTests+=i;
    	}
    double bon = alpha/nTests;
    
    // t-test
    for (int i=0; i<files; i++) // compare all files with each other
    {
    fileCounter++;
    for (int j=0; j<(files-fileCounter); j++)
        {
        t2++;
        tp.append("T-test of "+xTitle[t1-1]+" ("+IJ.d2s(t1,0)+") vs "+xTitle[t2-1]+" ("+IJ.d2s(t2,0)+"):");
        pValue = TestUtils.tTest((double[])data.get(t1-1),(double[])data.get(t2-1)); // T-test of two files
        tp.append("Two-tailed exact p-value: "+IJ.d2s(pValue,9));
        uStat = TestUtils.t((double[])data.get(t1-1),(double[])data.get(t2-1)); // T-test of two files
        tp.append("T statistic: "+IJ.d2s(uStat,0));
        if (TestUtils.tTest((double[])data.get(t1-1),(double[])data.get(t2-1), bon)) tp.append("Significant.");
        else if (TestUtils.tTest((double[])data.get(t1-1),(double[])data.get(t2-1), alpha)) tp.append("Not significant in case of "+nTests+" tests.");
        else tp.append("Not significant.");
        tp.append("");
        }
    t1++;
    t2=t1;
    }

    tp.append("Bonferroni's adjustment to alpha ("+alpha+"): "+IJ.d2s(nTests,0)+" test(s) -> "+IJ.d2s(bon,9));
    tp.saveAs(dir+filename); // save file   
    
    return true;
    } // end of statTestT()
    

    public boolean statTestMWU(ArrayList<ArrayList<Double>> dList, String addToFilename) // Mann-Whitney U-test on statistics of all files.
	    {
	    // the use of ArrayLists is necessary because the different files can contain different amounts of samples
	    ArrayList<double[]> data = new ArrayList<double[]>(files); // will contain the data
	    ArrayList<Double> tempList; // ArrayList for each file
	    double[] tempData; // array for each file
	
	    // read files from ArrayList dList and add them as double[] arrays into the ArrayList data
	    for (int i=0; i<files; i++) // cycle through files
	        {
	        tempList = (ArrayList<Double>)dList.get(i); // load file into ArrayList
	        tempData = new double[tempList.size()]; // create an array for the file
	        for (int j=0; j<tempData.length; j++)
	            {
	            tempData[j] = (double)tempList.get(j);
	            }
	        data.add(tempData); // add to final ArrayList
	        }
	
	    // do the tests
	    MannWhitneyUTest mwtA;       
	    int t1 = 1, t2 = 1; // counter (test file 1 and test file 2)
	    int fileCounter = 0; // counter
	    String filename = "00 Statistics Summary "+addToFilename+"MWU.txt"; // output file
	    TextPanel tp = new TextPanel(); // content of output file
	    double pValue, uStat;

	    // Bonferroni correction
	    double alpha = 0.05d;
	    int nTests = files-1;
	    for (int i=nTests-1; i>0; i--)
	    	{
	    	nTests+=i;
	    	}
	    double bon = alpha/nTests;
	    
	    // MWU
	    for (int i=0; i<files; i++) // compare all files with each other
	    {
	    fileCounter++;
	    for (int j=0; j<(files-fileCounter); j++)
	        {
	        t2++;
	        tp.append("Mann-Whitney U-test of "+xTitle[t1-1]+" ("+IJ.d2s(t1,0)+") vs "+xTitle[t2-1]+" ("+IJ.d2s(t2,0)+"):");
	        mwtA = new MannWhitneyUTest();
	        pValue = mwtA.mannWhitneyUTest((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
	        tp.append("Two-tailed exact p value: "+IJ.d2s(pValue,9));
	        uStat = mwtA.mannWhitneyU((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
	        tp.append("U statistic: "+IJ.d2s(uStat,0));
	        if (pValue<bon) tp.append("Significant.");
	        else if (pValue<alpha) tp.append("Not significant in case of "+nTests+" tests.");
	        else tp.append("Not significant.");
	        tp.append("");
	        }
	    t1++;
	    t2=t1;
	    }

	    tp.append("Bonferroni's adjustment to alpha ("+alpha+"): "+IJ.d2s(nTests,0)+" test(s) -> "+IJ.d2s(bon,9));
	    tp.saveAs(dir+filename); // save file   
	        
	    return true;
	    } // end of statTestMWU()


    /**
     * Divides an array into two halves; the input already has to be sorted
     *
     * @param matrix <code>double[]</code> array
     * @return <code>double[x][y]</code>, <code>x=0</code>: lower half, <code>x=1</code>: upper half 
	  * @see calcStats
     */
    protected static double[][] getHalf(double[] matrix) // Divide a matrix into two halves. The input matrix already has to be sorted. Used by calcStats().
       {        
       double[] result_below;
       double[] result_above;

       if (matrix.length % 2 == 1) // odd size
           {
           int size = (int)(matrix.length-1)/2;
           result_below = new double[size];
           result_above = new double[size];
           for (int i=0; i<size; i++)
               {
               result_below[i] = matrix[i];
               result_above[i] = matrix[size+1+i];
               }
           }   
       else // even size
           {
           int size = (int)Math.round(((matrix.length-1)/2.0d)-0.5d);
           result_below = new double[size];
           result_above = new double[size];
           for (int i=0; i<size; i++)
               {
               result_below[i] = matrix[i];
               result_above[i] = matrix[size+i];
               }
           }   

       double[][] result = new double[2][result_below.length];
       result[0] = result_below;
       result[1] = result_above;    
           
       return result;        
       }


    /**
     * Divides an array into two halves; the input already has to be sorted
     *
     * @param matrix <code>double[]</code> array
     * @return <code>double[x][y]</code>, <code>x=0</code>: lower half, <code>x=1</code>: upper half 
	  * @see calcStats
     */
    protected static int[][] getHalf(int[] matrix) // Divide a matrix into two halves. The input matrix already has to be sorted. Used by calcStats().
       {        
       int[] result_below;
       int[] result_above;

       if (matrix.length % 2 == 1) // odd size
           {
           int size = (int)(matrix.length-1)/2;
           result_below = new int[size];
           result_above = new int[size];
           for (int i=0; i<size; i++)
               {
               result_below[i] = matrix[i];
               result_above[i] = matrix[size+1+i];
               }
           }   
       else // even size
           {
           int size = (int)Math.round(((matrix.length-1)/2.0d)-0.5d);
           result_below = new int[size];
           result_above = new int[size];
           for (int i=0; i<size; i++)
               {
               result_below[i] = matrix[i];
               result_above[i] = matrix[size+i];
               }
           }   

       int[][] result = new int[2][result_below.length];
       result[0] = result_below;
       result[1] = result_above;    
           
       return result;        
       }


    /**
     * Calculates the min/max values of an array
     *
     * @param matrix <code>double[]</code> array
     * @return <code>double[x]</code>, <code>x=0</code>: minimum, <code>x=1</code>: maximum 
	  * @see calcStats
     */
    protected static double[] getMinMax(double[] matrix) // Get min/max values of a matrix. Used by calcStats().
       {
       double[] minMax = new double[2];
       minMax[0] = matrix[0];
       minMax[1] = matrix[0];
         
       for (int i=0; i<matrix.length; i++)
           {  
           if (matrix[i] < minMax[0]) minMax[0] = matrix[i];
           if (matrix[i] > minMax[1]) minMax[1] = matrix[i];
           }  

       return minMax;        
       }
   
    
    /**
     * Calculates the min/max values of an array
     *
     * @param matrix <code>double[]</code> array
     * @return <code>double[x]</code>, <code>x=0</code>: minimum, <code>x=1</code>: maximum 
	  * @see calcStats
     */
    protected static int[] getMinMax(int[] matrix) // Get min/max values of a matrix. Used by calcStats().
       {
       int[] minMax = new int[2];
       minMax[0] = matrix[0];
       minMax[1] = matrix[0];
         
       for (int i=0; i<matrix.length; i++)
           {  
           if (matrix[i] < minMax[0]) minMax[0] = matrix[i];
           if (matrix[i] > minMax[1]) minMax[1] = matrix[i];
           }  

       return minMax;        
       }


    /**
     * Calculate histograms.
     *
     * @param histoData the actual input data
     * @param xSize the width of the histogram
     * @param ySize the height of the histogram
     * @param bins the number of bins
     * @return the image plus
     */
    public HistoWrapper calcHisto(double[] histoData, int xSize, int ySize, int bins, boolean lowHisto, boolean logWindow2) // Generate histogram plot & table from a ratio matrix.
        {
    	logWindow = logWindow2;
    	int drawMax_temp = drawMax;
    	if (lowHisto) drawMax = (int)Math.round(drawMax/normValue);
        int[] histoSize = new int[2];
        histoSize[0] = xSize; // width of the histogram window
        histoSize[1] = ySize; // height of the histogram window
		double maxYvalue = (double)defaultScale; // max frequency (-> scale down all values to this factor, even for results table, if scaleOption=0)       
        if (bins > histoSize[0]) histoSize[0]=bins; // otherwise no output is created.        
        double factor = (double)maxPossibleValue/(double)bins; // factor for scaling frequencies into the final bins
        int binWidth = (int)Math.round(histoSize[0] / (bins-1.0d)); // width of each bin; only for plotting
        histoSize[0] = histoSize[0] + binWidth; // otherwise the last bin gets cut off during plotting
        double[] histo = new double[bins+1]; // array for the final, binned frequencies
		int realBin = 0; // temporary variable for final bins

        for (int i=0; i<histoData.length; i++) // put frequencies into final bins
            {
            realBin = (int)Math.round(i/factor); // each value of histoData corresponds to a rank
            if (realBin>bins) realBin = bins; // corrects for rounding errors
		    histo[realBin] = histo[realBin] + histoData[i]; // add the frequencies from histoData to the bins
            }
        
        double max = histo[0]; // find the maximum frequency (for scaling)
        for (int i=0; i<histo.length; i++)
            {  
            if (histo[i] > max) max = histo[i];
            }  

        boolean tempScale = false;
        if (scaleOption == 3) // normalize all bins by total amount of data
            {
            long pixels = maskCounter-satCounter; // total amount of data
            double maxScale = (double)defaultScale; // scale by this factor after normalization to get values back >1
            for (int i=0; i<histo.length; i++)
                {  
                histo[i] = maxScale * (histo[i] / pixels); // normalize and scale all bins
                }  
            double compMax = maxScale*(max/pixels); // highest normalized value
            if (compMax > drawMax) // test whether highest value fits into the plot
                {
                IJ.log("Caution: Histogram was cut off, consider re-plotting it. Max value: "+IJ.d2s(compMax,0)+" ("+IJ.d2s(drawMax,0)+")");
                logWindow = true;
                }
            max = (int)drawMax; // set height of plot to the value entered by the user
            scaleOption = 1; // continue plotting in the scale to max mode
            tempScale = true;
            }

        if (scaleOption != 0) maxYvalue = max; // Fixed value scaling off
        double factor2 = max / maxYvalue; // scaling factor for the frequencies      
        double scaleHeight = (double)histoSize[1] / maxYvalue; // scaling factor for the image (for plotting)                
        if (scaleOption == 2) // No scaling 
            {
            scaleHeight = 1;
            if ((int)max>histoSize[1]) histoSize[1] = (int)max; // set the image height to the max. frequency
            }
        if (tempScale) scaleOption = 3; // restore original scaleOption for next plot

        String imp_out_title = prefix+"Histogram plot";
        ImagePlus imp_out = NewImage.createByteImage(imp_out_title, histoSize[0], histoSize[1], 1, 1);
        if (imp_out == null) // produce an error if this fails
            {
            IJ.error(title, memoryError);
            return null;
            }
        WindowManager.checkForDuplicateName = true; // add a number to the title if name already exists  

        ImageProcessor ip_out = imp_out.getProcessor();

        ResultsTable rt = ResultsTable.getResultsTable();
        rt.reset();
        rt.setPrecision(9);
        int picPos = 0; // position within the image
        double maxLoop = maxYvalue*scaleHeight;

        for(int i=0; i<=bins; i++)
            {
            histo[i] = Math.round(histo[i]/factor2); // scaling of frequencies to a fixed value

            rt.incrementCounter();
            rt.addValue("Frequency", histo[i]);

            histo[i] = Math.round(histo[i] * scaleHeight); // scaling for the plot
            for(int j=0; j<maxLoop; j++) // create image
                {
                for (int k=0; k<binWidth; k++)
                    {
                    if(j<=histo[i]) ip_out.putPixel(picPos+k,j,255);
                    else ip_out.putPixel(picPos+k,j,0);
                    }
                }
            picPos = picPos + binWidth; // move on to the next bin
            }

        rt.show("Results");
        ip_out.flipVertical();
        
        HistoWrapper hw = new HistoWrapper(); // Object containing histo image and table
        hw.setImage(imp_out);
        hw.setTable(rt);
        
        drawMax = drawMax_temp;
        return hw;
        }
    
    } 