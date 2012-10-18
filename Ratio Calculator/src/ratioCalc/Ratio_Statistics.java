package ratioCalc;
import ij.IJ;
import ij.text.TextPanel;

import java.util.ArrayList;

import jsc.independentsamples.MannWhitneyTest;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TestUtils;

public class Ratio_Statistics
    {   
    private int files = 0; // number of files to be opened
    private String dir = ""; // dir for saving files manually
    private String[] xTitle; // x-labels for the different files (statistics)

    public Ratio_Statistics(int nFiles, String directory, String[] names)
    	{
    	files = nFiles;
    	dir = directory;
    	xTitle = names;
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
    protected static double getMedian(double[] matrix) // Get median value of a matrix. The input matrix already has to be sorted. Used by calcStats().
       {
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
    protected static double getMedian(int[] matrix) // Get median value of a matrix. The input matrix already has to be sorted. Used by calcStats().
       {
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
        // standard error of the median according to: Lothar Sachs, Angewandte Statistik, 11. Auflage 2003, S. 160. s = (a-b) / 3.4641; a = (n/2 + sqrt(3n)/n) th observation, b = (n/2 - sqrt(3n)/n) th observation, rounded up to the next number. 
        double a = (data.length/2.0d + Math.sqrt(3.0d*data.length)/data.length);
        double b = (data.length/2.0d - Math.sqrt(3.0d*data.length)/data.length);
        double sem = Math.abs((data[(int)Math.ceil(a)] - data[(int)Math.ceil(b)]) / 3.4641d);
		return sem;
        }


    public static double getSEMed(double[] data)  // get standard error of the median. Used for calculating statistics in createFile().
        {
        // standard error of the median according to: Lothar Sachs, Angewandte Statistik, 11. Auflage 2003, S. 160. s = (a-b) / 3.4641; a = (n/2 + sqrt(3n)/n) th observation, b = (n/2 - sqrt(3n)/n) th observation, rounded up to the next number. 
        double a = (data.length/2.0d + Math.sqrt(3.0d*data.length)/data.length);
        double b = (data.length/2.0d - Math.sqrt(3.0d*data.length)/data.length);
        double sem = Math.abs((data[(int)Math.ceil(a)] - data[(int)Math.ceil(b)]) / 3.4641d);
		return sem;
        }


    public boolean statTest(ArrayList<ArrayList<Double>> dList, String addToFilename) // Mann-Whitney U-test on statistics of all files. Done using the jsc.jar library from A.J. Bertie,  http://www.jsc.nildram.co.uk/
        {
        int t1 = 1, t2 = 1; // counter (test file 1 and test file 2)
        int fileCounter = 0, nTests = 0; // counter
        MannWhitneyTest mwt;

        // the use of ArrayLists is necessary because the different files can contain different amounts of samples
        ArrayList<double[]> data = new ArrayList<double[]>(files); // will contain the data
        ArrayList<Double> tempList; // ArrayList for each file
        double[] tempData; // array for each file

        String filename = "00 Statistics Summary "+addToFilename+"MWU 1.txt"; // output file
        TextPanel tp = new TextPanel(); // content of output file

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
        for (int i=0; i<files; i++) // compare all files with each other
            {
            fileCounter++;
            for (int j=0; j<(files-fileCounter); j++)
                {
                nTests++; // counts all tests
                t2++;
                tp.append("Mann-Whitney U-test of "+xTitle[t1-1]+" ("+IJ.d2s(t1,0)+") vs "+xTitle[t2-1]+" ("+IJ.d2s(t2,0)+"):");
                mwt = new MannWhitneyTest((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
                tp.append("Two-tailed exact p value: "+IJ.d2s(mwt.exactSP(),9));
                tp.append("U statistic: "+IJ.d2s(mwt.getStatistic(),0));
                tp.append("Z (normality): "+IJ.d2s(mwt.getZ(),9)+" (significant if |Z| >= 1.96 and sample size >= 20)");
                tempData = (double[])data.get(t1-1);
                tp.append("Sum of ranks 1: "+IJ.d2s(mwt.getRankSumA(),0)+", sample size: "+IJ.d2s(tempData.length,0));
                tempData = (double[])data.get(t2-1);
                tp.append("Sum of ranks 2: "+IJ.d2s(mwt.getRankSumB(),0)+", sample size: "+IJ.d2s(tempData.length,0));
                tp.append("");
                }
            t1++;
            t2=t1;
            }
        // Bonferroni correction
        double alpha = 0.05d;
        double bon = alpha/nTests;
        tp.append("Bonferroni's adjustment to alpha ("+alpha+"): "+IJ.d2s(nTests,0)+" test(s) -> "+IJ.d2s(bon,9));
        tp.saveAs(dir+filename); // save file   

        
        // using apache commons
        MannWhitneyUTest mwtA;       
        t1 = 1; t2 = 1; // counter (test file 1 and test file 2)
        fileCounter = 0; nTests = 0; // counter
        filename = "00 Statistics Summary "+addToFilename+"MWU 2.txt"; // output file
        tp = new TextPanel(); // content of output file
        double pValue, uStat;
        
        for (int i=0; i<files; i++) // compare all files with each other
        {
        fileCounter++;
        for (int j=0; j<(files-fileCounter); j++)
            {
            nTests++; // counts all tests
            t2++;
            tp.append("Mann-Whitney U-test of "+xTitle[t1-1]+" ("+IJ.d2s(t1,0)+") vs "+xTitle[t2-1]+" ("+IJ.d2s(t2,0)+"):");
            mwtA = new MannWhitneyUTest();
            pValue = mwtA.mannWhitneyUTest((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
            tp.append("Two-tailed exact p value: "+IJ.d2s(pValue,9));
            uStat = mwtA.mannWhitneyU((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
            tp.append("U statistic: "+IJ.d2s(uStat,0));
            tp.append("");
            }
        t1++;
        t2=t1;
        }
        // Bonferroni correction
        alpha = 0.05d;
        bon = alpha/nTests;
        tp.append("Bonferroni's adjustment to alpha ("+alpha+"): "+IJ.d2s(nTests,0)+" test(s) -> "+IJ.d2s(bon,9));
        tp.saveAs(dir+filename); // save file   
        
        
        // t-test
        t1 = 1; t2 = 1; // counter (test file 1 and test file 2)
        fileCounter = 0; nTests = 0; // counter
        filename = "00 Statistics Summary "+addToFilename+"T-Test.txt"; // output file
        tp = new TextPanel(); // content of output file
        
        for (int i=0; i<files; i++) // compare all files with each other
        {
        fileCounter++;
        for (int j=0; j<(files-fileCounter); j++)
            {
            nTests++; // counts all tests
            t2++;
            tp.append("T-test of "+xTitle[t1-1]+" ("+IJ.d2s(t1,0)+") vs "+xTitle[t2-1]+" ("+IJ.d2s(t2,0)+"):");
            pValue = TestUtils.tTest((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
            tp.append("Two-tailed exact p value: "+IJ.d2s(pValue,9));
            uStat = TestUtils.t((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
            tp.append("T statistic: "+IJ.d2s(uStat,0));
            if (TestUtils.tTest((double[])data.get(t1-1),(double[])data.get(t2-1), bon)) tp.append("Significant.");
            else tp.append("Not significant.");
            tp.append("");
            }
        t1++;
        t2=t1;
        }
        // Bonferroni correction
        tp.append("Bonferroni's adjustment to alpha ("+alpha+"): "+IJ.d2s(nTests,0)+" test(s) -> "+IJ.d2s(bon,9));
        tp.saveAs(dir+filename); // save file   
        
        return true;
        } // end of statTest()

    public boolean statTestMWU(ArrayList<ArrayList<Double>> dList, String addToFilename) // Mann-Whitney U-test on statistics of all files. Done using the jsc.jar library from A.J. Bertie,  http://www.jsc.nildram.co.uk/
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
	    int fileCounter = 0, nTests = 0; // counter
	    String filename = "00 Statistics Summary "+addToFilename+"MWU.txt"; // output file
	    TextPanel tp = new TextPanel(); // content of output file
	    double pValue, uStat;
	    
	    for (int i=0; i<files; i++) // compare all files with each other
	    {
	    fileCounter++;
	    for (int j=0; j<(files-fileCounter); j++)
	        {
	        nTests++; // counts all tests
	        t2++;
	        tp.append("Mann-Whitney U-test of "+xTitle[t1-1]+" ("+IJ.d2s(t1,0)+") vs "+xTitle[t2-1]+" ("+IJ.d2s(t2,0)+"):");
	        mwtA = new MannWhitneyUTest();
	        pValue = mwtA.mannWhitneyUTest((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
	        tp.append("Two-tailed exact p value: "+IJ.d2s(pValue,9));
	        uStat = mwtA.mannWhitneyU((double[])data.get(t1-1),(double[])data.get(t2-1)); // MWU test of two files
	        tp.append("U statistic: "+IJ.d2s(uStat,0));
	        tp.append("");
	        }
	    t1++;
	    t2=t1;
	    }

	    // Bonferroni correction
	    double alpha = 0.05d;
	    double bon = alpha/nTests;
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
   
    
    } 