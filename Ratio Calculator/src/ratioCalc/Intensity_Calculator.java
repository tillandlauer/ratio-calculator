package ratioCalc;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.util.ArrayList;

/**
 * Calculates average intensities.
 * @author Till Andlauer (till@andlauer.net)
 * @version 1.0 - 12-10-08
 */
public class Intensity_Calculator implements PlugIn
    {
	/** Title for dialogs and error messages. */
    private String title = "Intensity Calculator v1.0"; // title for dialogs and error messages
	/** Error message when out of memory */
    private String memoryError = "Out of memory...";
	/** Error message when file couldn't be saved */
    private String saveError = "Couldn't save file.";
	/** Threshold for the mask. Should be kept at <code>0</code> (-> all values above <code>0</code> are part of the mask).*/
    private int threshold = 0; // Threshold for the mask. Should be kept at 0 (-> all values above 0 are part of the mask).
	/** Counter for saturated pixels. @see maskCounter */
    private long satCounter = 0; // counter for saturated pixels
	/** Counter for masked pixels. @see satCounter */
    private long maskCounter = 0; // counter for masked pixels
	/**
	 * Maximum possible value (rank). (<code>39641</code> for gapless data, <code>65536</code> for original 8-bit data)
	 * @see centerValue
	 */
    private static int maxPossibleValue = 256; // maximum possible rank. (39641 for gapless data, 65536 for original data)
	/** Indicates whether data was added to the log window. */
    private boolean logWindow = false; // data was added to the log window
    // All of the following options can be set in chooseImages():
    /** Prefix added in front of image and table names; set in <code>chooseImages</code> */
    private String prefix = ""; // added in front of image and table names
    /** Keep source files open; set in <code>chooseImages</code> 
    * <br>If <code>false</code> ImageJ might occasionally crash
    */
    private boolean keepFiles = false; // Keep source files open
    /** Use three channels; set in <code>chooseImages</code> 
    */
    private boolean triple = false; // use three channels
    /** Save all generated files; set in <code>chooseImages</code>
    * @see saveDir
    */
    private boolean saveFiles = true; // Save all generated files
    /** Directory for saved files
    * @see saveFiles
    */
    private String saveDir = ""; // Directory for saved files
    /** Calculate histogram; set in <code>chooseImages</code> */
    private boolean histo = true; // Calculated histogram.
    /** Calculate statistics; set in <code>chooseImages</code> */
    private boolean statistics = true; // Calculate statistics.
    /** Calculate statistics without the (highest) value <code>255</code>; set in <code>chooseImages</code> 
    * <br>Useful with potentially oversaturated data 
    */
    private boolean stripStats = true; // Calculate statistics without the value 255.
    /** Use mask for statistics, histogram & scatter plot; set in <code>chooseImages</code> 
    * <br><i>It is always recommended to use a mask</i> 
    */
    private boolean mask = true; // Mask should be used for statistics, histogram, scatter plot.
    /** Show original, unbinned histogram data; set in <code>chooseImages</code> */
    private boolean showUnbinned = true; // Show original, unbinned histogram data
    /** Show parameters in the log window; set in <code>chooseImages</code> */
    private boolean logInfo = true; // Show parameters in the log window
    /** Number of histogram bins; set in <code>chooseImages</code>; default: <code>256</code> */
    private int histoBins = 128; // number of bins for the histogram
    /** Scaling factor for histograms (for scaleOptions <code>0</code> & <code>3</code>); set in <code>chooseImages</code>
    * <br><code>scaling factor * (frequency / total amount of data)</code>
    * <br>default for <code>scaleOption 3</code>: <code>10000000</code>
    * @see scaleOption 
    * @see drawMax 
    */
    private int defaultScale = 10000000; // Default scaling factor for histograms (options 1/4)   
    /** Height of histogram plots; set in <code>chooseImages</code> 
    * <br>(for <code>scaleOption 3</code>; max displayed frequency, not height in pixels)
    * <br>default: <code>130000</code>
    * @see scaleOption 
    * @see defaultScale 
    * @see normValue 
    */
    private int drawMax = 520000; // Default image height for histograms (option 4, normalize)
    /** Height of normalized histogram plots relative to other histograms 
    * @see drawMax 
    */
    private double normValue = 10.0d; // drawMax is divided by this value for normalized histograms
    /** How to scale histograms; set in <code>chooseImages</code> 
    * <br><code>0</code> = Scale the max value of data & image to a fixed value (<code>defaultScale</code>)  
    * <br><code>1</code> = Don't change the data; for display only scale the max value to the height of the image window  
    * <br><code>2</code> = Don't change the data; scale the height of the image window to the max value of the data  
    * <br><code>3</code> = Normalize the data by the total amount of data points; specify the max value displayed in the plot (<code>defaultScale</code>, <code>drawMax</code>)  
    * <br><code>scaleOption 3</code> is recommended if several plots shall be compared
 	* @see defaultScale
 	* @see drawMax
    */
    private int scaleOption = 3; // How to scale the histogram. 0 = Factor, 1 = Image, 2 = No, 3 = Normalize

	/**
	 * Runs the analysis
	 */
    public void run(String arg) 
        {  
        String parameters = Macro.getOptions();
        if (parameters!=null) prefix = parameters;
 
    	loadConfig();
        
        ImagePlus[] img = chooseImages(); // choose images and set variables.
        if (img == null) return;
        
        if (logInfo) showInfo(img); // Show the parameters
        
        double start_time = System.currentTimeMillis();
        IJ.showStatus("Beginning calculation...");

        boolean success = calcIntensities(img); // ratio calculation
        if (!success) return;

        if (!keepFiles)
            {
            img[0].close();
            img[1].close();
            if (triple) img[2].close();
            if (mask) img[3].close();
            }

        IJ.showStatus("The calculation took "+IJ.d2s(((double)System.currentTimeMillis()-start_time)/1000.0d, 2)+" seconds.");
        if (saveFiles) IJ.log("Files saved to: "+saveDir);
        if (logWindow) 
	    	{
	    	if (saveFiles)
	        	{
	        	TextWindow tw = new TextWindow("Log Temp", IJ.getLog(), 500, 500);
	        	TextPanel tp = tw.getTextPanel();
	        	tp.saveAs(saveDir+prefix+"Log.txt");
	        	tw.close();
	        	}
	    	
	    	if (keepFiles) IJ.log(""); // if the log window is open, add a blank line
	    	else
	        	{
	        	IJ.selectWindow("Log");
	        	IJ.run("Close");
	        	}
	    	}
        IJ.freeMemory();
        }

    
    private boolean loadConfig()
		{
		Ratio_Config rc;
		rc = new Ratio_Config();
		if (rc.error) return false;
		else
			{
			String[] ints = {"histoBins", "defaultScaleInt", "drawMaxInt", "scaleOption"};
			String[] doubles = {"normValueInt"};
			String[] strings = {"prefix"};
			String[] booleans = {"keepFiles", "saveFiles", "histo", "statistics", "stripStats", "mask", "showUnbinned", "logInfo", "triple"};

			int cInt = 0;
			double cDouble = 0.0d;
			String cString = "";
			boolean cBool = false;
			
	    	cInt = rc.getInt(ints[0]);
	    	if (!rc.error) histoBins=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[1]);
	    	if (!rc.error) defaultScale=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[2]);
	    	if (!rc.error) drawMax=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[3]);
	    	if (!rc.error) scaleOption=cInt;
	    	else rc.error=false;

	    	cDouble = rc.getDouble(doubles[0]);
	    	if (!rc.error) normValue=cDouble;
	    	else rc.error=false;

	    	cString = rc.getValue(strings[0]);
	    	if (!rc.error) prefix=cString;
	    	else rc.error=false;
			
	    	cBool = rc.getBoolean(booleans[0]);
	    	if (!rc.error) keepFiles=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[1]);
	    	if (!rc.error) saveFiles=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[2]);
	    	if (!rc.error) histo=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[3]);
	    	if (!rc.error) statistics=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[4]);
	    	if (!rc.error) stripStats=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[5]);
	    	if (!rc.error) mask=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[6]);
	    	if (!rc.error) showUnbinned=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[7]);
	    	if (!rc.error) logInfo=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[8]);
	    	if (!rc.error) triple=cBool;
	    	else rc.error=false;
			}
		return true;
		}
    
    
    /**
     * Calculate the average intensities
     * @param imp_in <code>ImagePlus[]</code> containing two (input) images
     * @return ImagePlus containing the result
     */
    public boolean calcIntensities(ImagePlus[] imp_in) // Calculating the average intensity - input: ImagePlus[] containing two (input) images. Output: Boolean success
        {
        //-- declare and initialize all the variables --//
        // variables for processing input and mask stacks
        ImageStack[] stacks = new ImageStack[4];
        stacks[0] = imp_in[0].getStack();
        stacks[1] = imp_in[1].getStack();
        stacks[2] = null;
        if (triple) stacks[2] = imp_in[2].getStack();
        stacks[3] = null;
        if (mask) stacks[3] = imp_in[3].getStack();       

        int width  = imp_in[0].getWidth(); 
        int height = imp_in[0].getHeight();
        int slices = imp_in[0].getStackSize();
        int size = width*height;
        ImageProcessor[] ip = new ImageProcessor[4];                 
        byte[][] pixel = new byte[4][size]; // image 1, image 2, mask
        int[] value = new int[4]; // pixel values       

        // arrays for statistics and histograms
        double[][] histoData = new double[3][maxPossibleValue]; // maxPossibleValue should be 256
        
         // histogram settings
        int histoX = 1024;
        int histoY = 512;
 
        //-- start actually doing stuff --//

        // count
        IJ.showStatus("Counting intensities...");
        for (int i=1; i<=slices; i++)
            {                
            ip[0] = stacks[0].getProcessor(i);
            ip[1] = stacks[1].getProcessor(i);
            pixel[0] = (byte[])ip[0].getPixels(); 
            pixel[1] = (byte[])ip[1].getPixels(); 
            if (triple)
	            {
	            ip[2] = stacks[2].getProcessor(i);
	            pixel[2] = (byte[])ip[2].getPixels(); 
	            }
            if (mask)
                {
                ip[3] = stacks[3].getProcessor(i);
                pixel[3] = (byte[])ip[3].getPixels(); 
                }
            
            for (int j=0; j<size; j++) 
                {
                value[0] = pixel[0][j]&0xff; // get the current pixel value of image 1
                value[1] = pixel[1][j]&0xff; // get the current pixel value of image 2                        
                if (triple) value[2] = pixel[2][j]&0xff; 
                if (mask) value[3] = pixel[3][j]&0xff; 

                if (!stripStats) // no stripStats = include 255
                    {
                    if (mask)
                        {
                        if (value[3]>threshold) 
                            {
                            histoData[0][value[0]]++; // sum the number of times each intensity occurred                   
                            histoData[1][value[1]]++;                   
                            if (triple) histoData[2][value[2]]++;                   
                            maskCounter++;
                            }
                        }
                    else // no mask 
                        {
                        histoData[0][value[0]]++;                   
                        histoData[1][value[1]]++;                   
                        if (triple) histoData[2][value[2]]++;                   
                        maskCounter++;
                        }
                    }
                else // stripStats = ignore 255
                    {
                    if (mask)
                        {
                        if (value[3]>threshold) 
                            {
                            if (triple)
	                            {
	                            if ((value[0]>254) || (value[1]>254) || (value[2]>254))
	                                {
	                                satCounter++; // count number of saturated pixels                       
	                                maskCounter++; // count total number of pixels in mask
	                                }
	                            else
	                                {
	                                histoData[0][value[0]]++;                   
	                                histoData[1][value[1]]++;                   
	                                histoData[2][value[2]]++;                   
	                                maskCounter++;
	                                }
	                            }
                            else
	                            {
	                            if ((value[0]>254) || (value[1]>254))
	                                {
	                                satCounter++; // count number of saturated pixels                       
	                                maskCounter++; // count total number of pixels in mask
	                                }
	                            else
	                                {
	                                histoData[0][value[0]]++;                   
	                                histoData[1][value[1]]++;                   
	                                maskCounter++;
	                                }
	                            }
                            }                      
                        }
                    else // no mask
                        {
                        histoData[0][value[0]]++;                   
                        histoData[1][value[1]]++;                   
                        if (triple) histoData[2][value[2]]++;                   
                        maskCounter++;
                        }
                    }
                }
            } // counting intensities is finished

        // process and display the results
        ResultsTable rt;
        Ratio_InOut rioT = new Ratio_InOut(saveError, saveDir);
        if (showUnbinned) // show original data
            {
            rt = ResultsTable.getResultsTable();        
            rt.reset();
            rt.setPrecision(9);
            for (int i=0; i<histoData[0].length; i++)
                {
                rt.incrementCounter();
                rt.addValue("frequency 1",histoData[0][i]);
                rt.addValue("frequency 2",histoData[1][i]);
                if (triple) rt.addValue("frequency 3",histoData[2][i]);
                }
            rt.show("Results");
            if (saveFiles) rioT.saveTable(rt, prefix+"Original Data");                      
            else IJ.renameResults("Results", prefix+"Original Data"); 
            }

        if (statistics) 
            {
            IJ.showStatus("Calculating statistics...");
            rt = calcStats(histoData);
            if (saveFiles) rioT.saveTable(rt, prefix+"Statistics");
            else IJ.renameResults("Results", prefix+"Statistics");                             
            }

        if (histo)
            {
            IJ.showStatus("Generating histogram...");
            HistoWrapper hw; // Object containing histo image and table
            Ratio_Statistics rs_histo = new Ratio_Statistics(drawMax, normValue, defaultScale, maxPossibleValue, scaleOption, maskCounter, satCounter, logWindow, prefix, memoryError, title);
            FileSaver fs;           
            hw = rs_histo.calcHisto(histoData[0], histoX, histoY, histoBins, false, logWindow);
    		logWindow = rs_histo.logWindow;
            ImagePlus img_histo = hw.getImage(); 
            if (img_histo == null) return false;
            img_histo.show();               
            if (saveFiles)
                {
                fs = new FileSaver(img_histo);
                fs.saveAsTiff(saveDir+img_histo.getTitle()+" 1.tif");
                img_histo.close();
                rioT.saveTable(hw.getTable(), prefix+"Histogram 1");
                }
            else IJ.renameResults("Results", prefix+"Histogram 1");               

            hw = rs_histo.calcHisto(histoData[1], histoX, histoY, histoBins, false, logWindow);
    		logWindow = rs_histo.logWindow;
            img_histo = hw.getImage(); 
            if (img_histo == null) return false;
            img_histo.show();               
            if (saveFiles)
                {
                fs = new FileSaver(img_histo);
                fs.saveAsTiff(saveDir+img_histo.getTitle()+" 2.tif");
                img_histo.close();
                rioT.saveTable(hw.getTable(), prefix+"Histogram 2");
                }
            else IJ.renameResults("Results", prefix+"Histogram 2");               

            if (triple)
            	{
                hw = rs_histo.calcHisto(histoData[2], histoX, histoY, histoBins, false, logWindow);
        		logWindow = rs_histo.logWindow;
                img_histo = hw.getImage(); 
                if (img_histo == null) return false;
                img_histo.show();               
                if (saveFiles)
                    {
                    fs = new FileSaver(img_histo);
                    fs.saveAsTiff(saveDir+img_histo.getTitle()+" 3.tif");
                    img_histo.close();
                    rioT.saveTable(hw.getTable(), prefix+"Histogram 3");
                    }
                else IJ.renameResults("Results", prefix+"Histogram 3");               
            	}
            
            histoData = new double[1][1];
            }

        if (stripStats)
            {
            if ((satCounter==0) && (maskCounter==0)) IJ.log("No saturated pixels.");
            else IJ.log("Total saturated pixels: "+IJ.d2s(satCounter,0)+" ("+IJ.d2s(100.0d*satCounter/maskCounter,2)+"% within the mask)");
            logWindow = true;
            }

        return true;
        } // end of calcIntensities


    /**
     * Calculate statistics of the ratio data
     * <br>The output table contains Minimum, Quartile 1, Median, Quartile 3, Maximum
     * <br>First line: standard results // second line: combined results // third line: inverted results
     * <br><i>Combined results:</i> 
     * <br>Show the original value if <code>0<=value<=1</code>, otherwise <code>1 + (1 - the inverted value)</code>; this way values are uniformly distributed around 1 
     * <br><code>Ratio Analysis</code> uses the original values for statistics (standard results, line 1)
     *
     * @param ratioResults <code>double[x][y]</code> containing the ratio data; <code>x</code>: all ranks (<code>39641</code> bins); <code>y=0</code>: histogram frequencies; <code>y=1</code>: ratio values
     * @return ResultsTable with basic statistics (Minimum, Quartile 1, Median, Quartile 3, Maximum)
	 * @see getHalf
	 * @see getMinMax
	 * @see getMedian
     */
    public ResultsTable calcStats(double[][] table_in) // Combine two/three results tables
        {
    	ResultsTable rt[] = new ResultsTable[3];
    	rt[0] = calcStats(table_in[0]);
    	double[][][] table = new double[1][1][1];
    	if (triple) table = new double[3][rt[0].getLastColumn()+1][rt[0].getCounter()];
    	else table = new double[2][rt[0].getLastColumn()+1][rt[0].getCounter()];
    	String[] heading = new String[rt[0].getLastColumn()+1];
		for (int col=0; col<table[0].length; col++)
			{
	    	table[0][col] = rt[0].getColumnAsDoubles(col);
	    	heading[col] = rt[0].getColumnHeading(col); 
	    	}

		rt[1] = calcStats(table_in[1]);
		for (int col=0; col<table[0].length; col++)
			{
	    	table[1][col] = rt[1].getColumnAsDoubles(col);
	    	}
    	if (triple) 
    		{
    		rt[2] = calcStats(table_in[2]);
    		for (int col=0; col<table[0].length; col++)
				{
		    	table[2][col] = rt[2].getColumnAsDoubles(col);
		    	}
    		}
    	
    	ResultsTable rt_out = ResultsTable.getResultsTable(); 
    	rt_out.reset();
        rt_out.setPrecision(9);   	
    	for (int file=0; file<table.length; file++)
			{
			for (int row=0; row<table[0][0].length; row++)
				{
				rt_out.incrementCounter();
				for (int col=0; col<table[0].length; col++)
					{
					rt_out.addValue(heading[col], table[file][col][row]);
					}
				}
			if (file<(table.length-1)) rt_out.incrementCounter();
			}
   	
    	rt_out.show("Results");
    	return rt_out;
        }

    /**
     * Calculate statistics of the ratio data
     * <br>The output table contains Minimum, Quartile 1, Median, Quartile 3, Maximum
     * <br>First line: standard results // second line: combined results // third line: inverted results
     * <br><i>Combined results:</i> 
     * <br>Show the original value if <code>0<=value<=1</code>, otherwise <code>1 + (1 - the inverted value)</code>; this way values are uniformly distributed around 1 
     * <br><code>Ratio Analysis</code> uses the original values for statistics (standard results, line 1)
     *
     * @param ratioResults <code>double[x][y]</code> containing the ratio data; <code>x</code>: all ranks (<code>39641</code> bins); <code>y=0</code>: histogram frequencies; <code>y=1</code>: ratio values
     * @return ResultsTable with basic statistics (Minimum, Quartile 1, Median, Quartile 3, Maximum)
	 * @see getHalf
	 * @see getMinMax
	 * @see getMedian
     */
    public ResultsTable calcStats(double[] table) // Calculate statistics of a table
        {
        // expand the histogram data back into an array of all values
        ArrayList<Integer> al = new ArrayList<Integer>(table.length);              
        for (int i=0; i<table.length; i++)
            {
            for (int j=0; j<table[i]; j++)
                {
                al.add(i);
                }  
            }

        int[] results = new int[al.size()];
        for (int i=0; i<al.size(); i++) 
            {
            results[i] = (int)al.get(i);
            }
        al = new ArrayList<Integer>(1);

        // calculate the statistics
        double median = Ratio_Statistics.getMedian(results, false); // sorting not necessary because this list already is sorted.
        int[][] halfs = Ratio_Statistics.getHalf(results);
        double q1 = Ratio_Statistics.getMedian(halfs[0], false);
        double q2 = Ratio_Statistics.getMedian(halfs[1], false);
        int[] minMax = Ratio_Statistics.getMinMax(results);
        double mean = Ratio_Statistics.getMean(results);
        
        ResultsTable rt = ResultsTable.getResultsTable();        
        rt.reset();
        rt.setPrecision(9);
        rt.incrementCounter();
        rt.addValue("Min", minMax[0]);    
        rt.addValue("Quartile 1", q1);    
        rt.addValue("Median", median);    
        rt.addValue("Quartile 3", q2);    
        rt.addValue("Max", minMax[1]);    
        rt.addValue("Mean", mean);    
        rt.show("Results");
        
        return rt;
        } // end of calcStats


    /**
     * Choose images.
     *
     * @return the image plus[]
     */
    private ImagePlus[] chooseImages() // choose images and parameters
        {
        int[] open_images = WindowManager.getIDList(); // get the IDs of the open images        
        if (open_images==null)
            {
            IJ.error(title, "No image is open.");
            return null;
            }                                    
        if (open_images.length<2)
            {
            IJ.error(title, "At least two images need to be open.");
            return null;
            }                                    
        ImagePlus img_temp;
        String[] image_titles = new String[open_images.length];
        for (int i=0; i<open_images.length; i++)  // get the titles of all of the open images
            {
            img_temp = WindowManager.getImage(open_images[i]);
            image_titles[i] = img_temp.getTitle();
            }

        String[] channelO = new String[2];
        channelO[0] = "Stack 1";
        channelO[1] = "Stack 2";

        String[] scaleOption_temp = new String[4];
        scaleOption_temp[0] = "Scale to fixed max. value (image & data)";
        scaleOption_temp[1] = "Scale to image height (image only)";
        scaleOption_temp[2] = "No scaling of data, scale image height";
        scaleOption_temp[3] = "Normalize by total amount of data, specify image height";

		int cgRows3=1, cgColumns3=2;
		String[] cgLabels3 = 
		    {
            "Keep input files open",
            "Save (and close) new files",
		    };
		boolean[] cgStates3 = {keepFiles,saveFiles};

		int cgRows1=1, cgColumns1=3;
		String[] cgLabels1 = 
		    {
            "Calculate statistics",
            "Generate histogram",
            "Calculate without the highest intensity value",
		    };
		boolean[] cgStates1 = {statistics,histo,stripStats};

        // create/show the dialog                
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Stack 1:", image_titles, image_titles[0]);
        gd.addChoice("Stack 2:", image_titles, image_titles[1]);
        if (image_titles.length > 2)
	        {
	        gd.addChoice("Stack 3:", image_titles, image_titles[2]);       	
    		gd.setInsets(20,20,0);
            gd.addCheckbox("Use three channels", triple);
	        }
        	
		gd.setInsets(20,20,0);
		gd.addCheckboxGroup(cgRows3, cgColumns3, cgLabels3, cgStates3);
        gd.addStringField("Prefix to filenames:", prefix, 10);

		gd.setInsets(20,20,0);
		gd.addCheckboxGroup(cgRows1, cgColumns1, cgLabels1, cgStates1);

        if (image_titles.length > 2)
            {
    		gd.setInsets(20,20,0);
            gd.addCheckbox("Use mask for statistics and plots", mask);
            if (image_titles.length == 3) gd.addChoice("Mask:", image_titles, image_titles[2]);
            else gd.addChoice("Mask:", image_titles, image_titles[3]);
            }
        gd.addMessage("Calculation of statistics or a histogram without a mask is not recommended.");

		gd.setInsets(20,0,0);
        gd.addNumericField("Number of bins (histograms):", histoBins, 0, 9, "bins"); // title, default value, digits, length, units
        gd.addChoice("Scaling of histograms:", scaleOption_temp, scaleOption_temp[scaleOption]);
        gd.addNumericField("Scaling factor (for options 1 & 4):", defaultScale, 0, 9, "factor * (frequency / total amount of data)");
        gd.addNumericField("Image height (for option 4):", drawMax, 0, 9, "(max. displayed frequency, not height in pixels)");
        gd.addCheckbox("Also show unbinned, original histogram data", showUnbinned);
        gd.addCheckbox("Show parameters in log window", logInfo);

        gd.showDialog();
        if (gd.wasCanceled()) return null;
        
        // process the input
        int[] choice_index = new int[4];      
        choice_index[0] = gd.getNextChoiceIndex(); // images
        choice_index[1] = gd.getNextChoiceIndex();

        triple = false;
        if (image_titles.length > 2)
        	{
        	triple = gd.getNextBoolean();
	        choice_index[2] = gd.getNextChoiceIndex();
        	}
	        
        histoBins = (int)gd.getNextNumber();
        defaultScale = (int)gd.getNextNumber();
        drawMax = (int)gd.getNextNumber();

        keepFiles = gd.getNextBoolean();
        saveFiles = gd.getNextBoolean();
        statistics = gd.getNextBoolean();
        histo = gd.getNextBoolean();
        stripStats = gd.getNextBoolean();

        mask = false;
        if (image_titles.length > 2)
            {
            mask = gd.getNextBoolean();
            choice_index[3] = gd.getNextChoiceIndex(); // mask
            }

        scaleOption = gd.getNextChoiceIndex(); // 0 = Factor, 1 = Image, 2 = No, 3 = Normalize
        
        showUnbinned = gd.getNextBoolean();
        logInfo = gd.getNextBoolean();

        prefix = gd.getNextString();

        // analyze the validity of the input images, create output array
        if (!statistics && !histo)
            {
            IJ.error(title, "No output has been chosen - aborting.");
            return null;
            }

        ImagePlus[] img_out = new ImagePlus[4];        
        img_out[0] = WindowManager.getImage(open_images[choice_index[0]]);
        img_out[1] = WindowManager.getImage(open_images[choice_index[1]]);

        if (img_out[0].getWidth() != img_out[1].getWidth() || img_out[0].getHeight() != img_out[1].getHeight() || img_out[0].getStackSize() != img_out[1].getStackSize())
            {         
            IJ.error(title, "The stacks are not compatible.");
            return null;
            }
        if (img_out[0].getType() != ImagePlus.GRAY8 || img_out[1].getType() != ImagePlus.GRAY8)
            {
            IJ.error(title, "8 bit stacks are necessary.");
            return null;
            }

        if (triple) 
        	{
        	img_out[2] = WindowManager.getImage(open_images[choice_index[2]]);
            if (img_out[0].getWidth() != img_out[2].getWidth() || img_out[0].getHeight() != img_out[2].getHeight() || img_out[0].getStackSize() != img_out[2].getStackSize())
		        {         
		        IJ.error(title, "The stacks are not compatible.");
		        return null;
		        }
            if (img_out[2].getType() != ImagePlus.GRAY8)
	            {
	            IJ.error(title, "8 bit stacks are necessary.");
	            return null;
	            }
        	}      
        
        if (mask) 
            {
            img_out[3] = WindowManager.getImage(open_images[choice_index[3]]);
            if (img_out[0].getWidth() != img_out[3].getWidth() || img_out[0].getHeight() != img_out[3].getHeight() || img_out[0].getStackSize() != img_out[3].getStackSize())
                {         
                IJ.error(title, "The mask is not compatible.");
                return null;
                }
            if (img_out[3].getType() != ImagePlus.GRAY8)
                {
                IJ.error(title, "8 bit mask is necessary.");
                return null;
                }
            }

        if (img_out[0].getStackSize()==1)
            {
            IJ.error(title, "Only image stacks are supported. If you want to use single-slice images, use the Mask Generator to convert your images into stacks.");
            return null;
            }

        if (saveFiles)
	        {
        	DirectoryChooser dc = new DirectoryChooser("Where do you want to save the files?");
            saveDir = dc.getDirectory();
	        }
        
        return img_out;
        }

    private void showInfo(ImagePlus[] img_out)
	    {
    	IJ.log("Stack 1: "+img_out[0].getTitle());
    	IJ.log("Stack 2: "+img_out[1].getTitle());
    	if (triple) IJ.log("Stack 3: "+img_out[2].getTitle());
    	if (mask) IJ.log("Mask: "+img_out[3].getTitle());
    	if (stripStats) IJ.log("Calculation without highest intensity value");
    	if (histo) 
    		{
            switch (scaleOption)
	            {
	            case 0: IJ.log("Scaling: Data to fixed max. value (1)");   
	                    break;
	            case 1: IJ.log("Scaling: Plot to image height (2)");   
	                    break;
	            case 2: IJ.log("Scaling: No scaling (3)");   
	                    break;
	            case 3: IJ.log("Scaling: Normalized by total amount of data (4)");   
	                    break;
	            }
    		IJ.log("Number of bins: "+IJ.d2s(histoBins,0));
    		IJ.log("Scaling factor: "+IJ.d2s(defaultScale,0));
    		IJ.log("Plot height: "+IJ.d2s(drawMax,0));
    		}
	    }
    
    }