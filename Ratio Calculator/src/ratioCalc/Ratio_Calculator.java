package ratioCalc;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.util.ArrayList;

/**
 * Calculates ratios between all pixels of two image stacks.
 * @author Till Andlauer (till@andlauer.net)
 * @version 1.31 - 13-08-21
 */
public class Ratio_Calculator implements PlugIn
    {
	/** Title for dialogs and error messages. */
    private String title = "Ratio Calculator v1.31"; // title for dialogs and error messages
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
    private static int maxPossibleValue = 39641; // maximum possible rank. (39641 for gapless data, 65536 for original data)
	/**
	 * Median rank, half of <code>maxPossibleValue</code>. (<code>19821</code> for gapless data, <code>32768</code> for original data) 
	 * @see maxPossibleValue
	 */
    private static int centerValue = 19821; // median rank. (19821 for gapless data, 32768 for original data)
	/** Indicates whether data was added to the log window. */
    private boolean logWindow = false; // data was added to the log window
    // All of the following options can be set in chooseImages():
    /** Prefix added in front of image and table names; set in <code>chooseImages</code> */
    private String prefix = ""; // added in front of image and table names
    /** Bitdepth of the output image. <code>0</code> = 8 bit, <code>1</code> = 16 bit, <code>2</code> = 32 bit; set in <code>chooseImages</code> 
    * <br>Use 8 bit to save disk space, 16 bit for the original values
    */
    private int bitdepth = 0; // bitdepth of the output image. 0 = 8 bit, 1 = 16 bit, 2 = 32 bit.
    /** Keep source files open; set in <code>chooseImages</code> 
    * <br>If <code>false</code> ImageJ might occasionally crash
    */
    private boolean keepFiles = false; // Keep source files open
    /** Save all generated files; set in <code>chooseImages</code>
    * @see saveDir
    */
    private boolean saveFiles = true; // Save all generated files
    /** Directory for saved files
    * @see saveFiles
    */
    private String saveDir = ""; // Directory for saved files
    /** Generate an output image; set in <code>chooseImages</code> */
    private boolean image = true; // Generated output image.
    /** Show the LUT for the ratio image; set in <code>chooseImages</code> */
    private static boolean displayLUT = false; // Show the LUT.
    /** Calculate scatter plot; set in <code>chooseImages</code> */
    private boolean scatter = true; // Calculate scatter plot.
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
    /** Use original values for histogram/statistics; set in <code>chooseImages</code>
    * @see normHisto
    */
    private boolean standHisto = true; // Show histogram/statistics with original values
    /** Use values normalized by statistical frequency for histogram/statistics; set in <code>chooseImages</code> 
    * @see standHisto
    */
    private boolean normHisto = true; // Show histogram/statistics with values normalized by statistical frequency
    /** Show original, unbinned histogram data; set in <code>chooseImages</code> */
    private boolean showUnbinned = true; // Show original, unbinned histogram data
    /** Scale scatter plot to the max value; set in <code>chooseImages</code>
    * <br>Only relevant if the max frequency exceeds the max 32 bit value; otherwise higher values are cut off
    * <br>Usually ok when <code>false</code>
    * @see scatterScale
    */
    private boolean maxScatter = false; // Scale scatter plot to max frequency
    /** Scale scatter plot to total amount of data; set in <code>chooseImages</code>
    * <br><code>true</code> is recommended if several plots shall be compared
    * @see maxScatter
    * @see drawMax 
    * @see defaultScale 
    * @see scaleOption
    */
    private boolean scatterScale = true; // Scale scatter plot to total amount of data (like scaleOption = 3 for histograms)
    /** Show the statistical ratio/rank distribution; set in <code>chooseImages</code> */
    private boolean showRanks = false; // Show statistical rank distribution
    /** The use uniform ratio <code>(a-b)/(a+b)</code> instead of <code>a/b</code>; set in <code>chooseImages</code> 
    * <br>Usually unnecessary because ranks transform also the <code>a/b</code> ratio to a uniform distribution
    */
    private boolean useUniRatio = false; // (a-b)/(a+b)
    /** Generate statistics files in the old format; set in <code>chooseImages</code> 
    * <br>Option exists only for compatibility. Should be <code>false</code>. 
    */
    private boolean oldFormat = false; // Use the old format for statistics files.
    /** Calculate ratio image separately to save memory; set in <code>chooseImages</code> */
    private boolean extraRatio = false; // Calculate ratio image separately
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
    * <br>default: <code>300000</code>
    * @see scaleOption 
    * @see defaultScale 
    * @see normValue 
    */
    private int drawMax = 300000; // Default image height for histograms (option 4, normalize)
    /** Height of normalized histogram plots relative to other histograms 
    * @see drawMax 
    */
    private double normValue = 10.0d; // drawMax is divided by this value for normalized histograms
    /** Multiplication factor for scatter plot scaling; set in <code>chooseImages</code> 
    * <br>Only used if <code>scatterScale=true</code>
    * <br><code>scatterMulti * defaultScale</code>
    * <br>default: <code>20</code>
    * @see scatterScale 
    * @see defaultScale 
    */
    private double scatterMulti = 20.0d; // Default multiplication factor during scatter plot scaling
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

        if (showRanks) // ignore the input images. Show the statistical rank distribution instead.
            {
            showRankDist();
            return;
            }

        ImagePlus img_ratio = null;
        if (image || statistics || histo) 
            {
            boolean temp = false; // if the ratio image should be generated independent from the other calculations 
            if (extraRatio && image) 
                {
                temp = image;
                image = false;
                }
            img_ratio = calcRatio(img); // ratio calculation
            if (img_ratio == null) return;
            if (extraRatio && temp) // if the ratio image should be generated independent from the other calculations 
                {
                extraRatio = false;
                image = true;
                statistics = false;
                histo = false;
                img_ratio = calcRatio(img);
                if (img_ratio == null) return;
                }
            }

        ImagePlus img_scatter = null;
        if (scatter)
            {
            IJ.showStatus("Generating scatter plot...");
            img_scatter = generate_Scatter(img);
            if (img_scatter == null) return;

            // find the max value, apply LUT and scale it
            ImageStatistics iSt = new ImageStatistics();
            iSt = ImageStatistics.getStatistics(img_scatter.getProcessor(), 16, null); // Get raw MIN_MAX
            WindowManager.setTempCurrentImage(img_scatter); 
            IJ.run("Fire"); // change the lookup table
            img_scatter.getProcessor().setMinAndMax(0,iSt.max);

            img_scatter.show();
            }

        if (image) // show the ratio image
            {
            IJ.showStatus("Applying LUT...");
            img_ratio = Ratio_InOut.spectrumLUT(img_ratio, maxPossibleValue, displayLUT); // change the lookup table
            if (img_ratio == null) return;
            if (image && bitdepth==0) // conversion to 8 bit
                {
                StackConverter ratioConv = new StackConverter(img_ratio);
                ratioConv.convertToGray8();           
                }           
            img_ratio.show();
            }

        if (!keepFiles)
            {
            img[0].close();
            img[1].close();
            if (mask) img[2].close();
            }

        if (saveFiles)
            {
            FileSaver fs;
            if (image)
                {
            	fs = new FileSaver(img_ratio);
                fs.saveAsTiffStack(saveDir+img_ratio.getTitle()+".tif");
                img_ratio.close();
                }
            if (scatter)
                {
            	fs = new FileSaver(img_scatter);
                fs.saveAsTiff(saveDir+img_scatter.getTitle()+".tif");
                img_scatter.close();
                }
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
			String[] ints = {"bitdepth", "histoBins", "defaultScale", "drawMax", "scaleOption"};
			String[] doubles = {"normValue", "scatterMulti"};
			String[] strings = {"prefix"};
			String[] booleans = {"keepFiles", "saveFiles", "image", "displayLUT", "scatter", "histo", "statistics", "stripStats", "mask", "standHisto", "normHisto", "showUnbinned", "maxScatter", "scatterScale", "showRanks", "useUniRatio", "extraRatio", "logInfo"};

			int cInt = 0;
			double cDouble = 0.0d;
			String cString = "";
			boolean cBool = false;
			
	    	cInt = rc.getInt(ints[0]);
	    	if (!rc.error) bitdepth=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[1]);
	    	if (!rc.error) histoBins=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[2]);
	    	if (!rc.error) defaultScale=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[3]);
	    	if (!rc.error) drawMax=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[4]);
	    	if (!rc.error) scaleOption=cInt;
	    	else rc.error=false;

	    	cDouble = rc.getDouble(doubles[0]);
	    	if (!rc.error) normValue=cDouble;
	    	else rc.error=false;
	    	cDouble = rc.getDouble(doubles[1]);
	    	if (!rc.error) scatterMulti=cDouble;
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
	    	if (!rc.error) image=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[3]);
	    	if (!rc.error) displayLUT=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[4]);
	    	if (!rc.error) scatter=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[5]);
	    	if (!rc.error) histo=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[6]);
	    	if (!rc.error) statistics=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[7]);
	    	if (!rc.error) stripStats=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[8]);
	    	if (!rc.error) mask=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[9]);
	    	if (!rc.error) standHisto=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[10]);
	    	if (!rc.error) normHisto=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[11]);
	    	if (!rc.error) showUnbinned=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[12]);
	    	if (!rc.error) maxScatter=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[13]);
	    	if (!rc.error) scatterScale=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[14]);
	    	if (!rc.error) showRanks=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[15]);
	    	if (!rc.error) useUniRatio=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[16]);
	    	if (!rc.error) extraRatio=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[17]);
	    	if (!rc.error) logInfo=cBool;
	    	else rc.error=false;
			}
		return true;
		}
    
    
    /**
     * Calculate the ratios
     * @param imp_in <code>ImagePlus[]</code> containing two (input) images
     * @return ImagePlus containing the result
     */
    public ImagePlus calcRatio(ImagePlus[] imp_in) // Calculate the ratios - input: ImagePlus[] containing two (input) images. Output: ImagePlus containing the result
        {
        //-- declare and initialize all the variables --//
        // variables for processing input and mask stacks
        ImageStack[] stacks = new ImageStack[4];
        stacks[0] = imp_in[0].getStack();
        stacks[1] = imp_in[1].getStack();
        int width  = imp_in[0].getWidth(); 
        int height = imp_in[0].getHeight();
        int slices = imp_in[0].getStackSize();
        int size = width*height;

        FloatProcessor[] fp = new FloatProcessor[3]; // for historical reasons, the input image data is handled as float.                 
        float[][] pixel = new float[3][size]; // image 1, image 2, mask
        int x = 0, y = 0; // position within image
        float[] value = new float[3]; // pixel values
        
        stacks[3] = null;
        if (mask) stacks[3] = imp_in[2].getStack();       

        // arrays for statistics and histograms
        double[] histoDataS = new double[1]; // original data
        double[] histoDataN = new double[1]; // normalized data (by frequency)
        double[][] ratioData = new double[1][1]; // combination of histogram frequencies and actual ratios for statistics

         // histogram settings
        int histoX = 1024;
        int histoY = 512;

        if (histo || statistics)
            {
            if (useUniRatio) // (a-b)/(a+b)
                {
                if (standHisto) histoDataS = new double[maxPossibleValue+2];
                if (normHisto) histoDataN = new double[maxPossibleValue+2];
                if (statistics) ratioData = new double[maxPossibleValue+2][2];
                }
            else // a/b
                {
                if (standHisto) histoDataS = new double[maxPossibleValue];
                if (normHisto) histoDataN = new double[maxPossibleValue];
                if (statistics) ratioData = new double[maxPossibleValue][2];
                }
            }
 
        // variables for processing the output (image)
        ImagePlus imp_out = null;
        ImageProcessor ip_out = null;
        if (image)
            {
            String imp_out_title = prefix+"Ratio";
            imp_out = NewImage.createShortImage(imp_out_title, width, height, slices, 1); // create a new image with the same dimensions as the first chosen image
            if (imp_out == null) // produce an error if this fails
                {
                IJ.error(title, memoryError);
                return null;
                }
            WindowManager.checkForDuplicateName = true; // add a number to the title if name already exists  
            stacks[2] = imp_out.getStack();
            }

        // variables for calculating the ratio
        int ratio = 0;
        int counts = 0;

        //-- start actually doing stuff --//
        // create the rank matrix
        double[][][] ranks = rankGenerator(); // 0 = original rank, 1 = final rank, 2 = counter, 3 = ratio                  

        // calculate the ratio
        IJ.showStatus("Calculating ratio...");
        for (int i=1; i<=slices; i++)
            {                
            if (image) ip_out = stacks[2].getProcessor(i);
            fp[0] = stacks[0].getProcessor(i).toFloat(0, fp[0]);
            fp[1] = stacks[1].getProcessor(i).toFloat(0, fp[1]);
            pixel[0] = (float[])fp[0].getPixels(); 
            pixel[1] = (float[])fp[1].getPixels(); 
            if (mask)
                {
                fp[2] = stacks[3].getProcessor(i).toFloat(0, fp[2]);
                pixel[2] = (float[])fp[2].getPixels(); 
                }
            
            for (int j=0; j<size; j++) 
                {
                value[0] = pixel[0][j]; // get the current pixel value of image 1
                value[1] = pixel[1][j]; // get the current pixel value of image 2                        
                if (mask) value[2] = pixel[2][j]; 

                if (image)
                    {
                    ratio = (int)ranks[Math.round(value[0])][Math.round(value[1])][1]; // get the uniformly distributed ratio of these two pixel values (=rank)                     
                    if (mask)
                        {
                        if (value[2]<=threshold) ratio=centerValue; 
                        }
                    y = (j / height);
                    x = (j % height);
                    ip_out.putPixelValue(x, y, ratio);
                    }

                if (histo || statistics)
                    {
                    ratio = ((int)ranks[Math.round(value[0])][Math.round(value[1])][1])-1; // '-1' because the value is from now on used as an index for arrays. arrays start at 0, thus x[n-1] to address the value at position n.
                    counts = (int)ranks[Math.round(value[0])][Math.round(value[1])][2]; // needed for normalization and statistics

                    if (!stripStats) // no stripStats = include 255
                        {
                        if (mask)
                            {
                            if (value[2]>threshold) 
                                {
                                if (standHisto) histoDataS[ratio]++; // sum the number of times each ratio occurred                   
                                if (normHisto) histoDataN[ratio] = histoDataN[ratio] + (1.0d/counts); // normalize the numbers by the statistical frequency 
                                if (statistics && (ratioData[ratio][1]==0)) ratioData[ratio][1] = ranks[Math.round(value[0])][Math.round(value[1])][3]; // add the real ratio x/y to the array (for statistics)                    
                                maskCounter++;
                                }
                            }
                        else // no mask 
                            {
                            if (standHisto) histoDataS[ratio]++;                      
                            if (normHisto) histoDataN[ratio] = histoDataN[ratio] + (1.0d/counts);                      
                            if (statistics && (ratioData[ratio][1]==0)) ratioData[ratio][1] = ranks[Math.round(value[0])][Math.round(value[1])][3];                    
                            maskCounter++;
                            }
                        }
                    else // stripStats = ignore 255
                        {
                        if (mask)
                            {
                            if (value[2]>threshold) 
                                {
                                if ((value[0]>254) || (value[1]>254))
                                    {
                                    satCounter++; // count number of saturated pixels                       
                                    maskCounter++; // count total number of pixels in mask
                                    }
                                else
                                    {
                                    if (standHisto) histoDataS[ratio]++;                      
                                    if (normHisto) histoDataN[ratio] = histoDataN[ratio] + (1.0d/counts);                      
                                    maskCounter++;
                                    if (statistics && (ratioData[ratio][1]==0)) ratioData[ratio][1] = ranks[Math.round(value[0])][Math.round(value[1])][3];                    
                                    }
                                }                      
                            }
                        else // no mask
                            {
                            if (standHisto) histoDataS[ratio]++;                      
                            if (normHisto) histoDataN[ratio] = histoDataN[ratio] + (1.0d/counts);                      
                            if (statistics && (ratioData[ratio][1]==0)) ratioData[ratio][1] = ranks[Math.round(value[0])][Math.round(value[1])][3];                    
                            maskCounter++;
                            }
                        }
                    }
                }
            } // calculation of ratios is finished

        // process and display the results
        ResultsTable rt;
        Ratio_InOut rioT = new Ratio_InOut(saveError, saveDir);
        if (showUnbinned && (histo || statistics)) // show original data
            {
            rt = ResultsTable.getResultsTable();        
            rt.reset();
            rt.setPrecision(9);
            if (standHisto)
                {
                for (int i=0; i<histoDataS.length; i++)
                    {
                    rt.incrementCounter();
                    if (statistics) rt.addValue("ratio",ratioData[i][1]);
                    rt.addValue("frequency",histoDataS[i]);
                    }
                rt.show("Results");
                if (saveFiles) rioT.saveTable(rt, prefix+"Original Data");                      
                else IJ.renameResults("Results", prefix+"Original Data"); 
                }
            if (normHisto)
                {
                rt.reset();
                for (int i=0; i<histoDataN.length; i++)
                    {
                    rt.incrementCounter();
                    if (statistics) rt.addValue("ratio",ratioData[i][1]);
                    rt.addValue("frequency",histoDataN[i]);
                    }
                rt.show("Results");
                if (saveFiles) rioT.saveTable(rt, prefix+"Original Data (normalized)");                      
                else IJ.renameResults("Results", prefix+"Original Data (normalized)");                      
                }
            }

        if (statistics) 
            {
            IJ.showStatus("Calculating statistics...");
            
            if (standHisto)
                {
                for (int i=0; i<ratioData.length; i++)
                    {
                    ratioData[i][0] = histoDataS[i];
                    }
                rt = calcStats(ratioData);
                if (saveFiles) rioT.saveTable(rt, prefix+"Statistics");
                else IJ.renameResults("Results", prefix+"Statistics");                             
                }
            if (normHisto)
                {
                for (int i=0; i<ratioData.length; i++)
                    {
                    ratioData[i][0] = histoDataN[i];
                    }
                rt = calcStats(ratioData);
                if (saveFiles) rioT.saveTable(rt, prefix+"Statistics (normalized)");
                else IJ.renameResults("Results", prefix+"Statistics (normalized)");                      
                }
           ratioData = new double[1][1];
           }

        if (histo)
            {
            IJ.showStatus("Generating histogram...");
            HistoWrapper hw; // Object containing histo image and table
            Ratio_Statistics rs_histo = new Ratio_Statistics(drawMax, normValue, defaultScale, maxPossibleValue, scaleOption, maskCounter, satCounter, logWindow, prefix, memoryError, title);
            FileSaver fs;
            
            if (standHisto)
                {
            	hw = rs_histo.calcHisto(histoDataS, histoX, histoY, histoBins, false, logWindow);
        		logWindow = rs_histo.logWindow;
                histoDataS = new double[1];
                ImagePlus img_histoS = hw.getImage(); 
                if (img_histoS == null) return null;
                img_histoS.show();               
                if (saveFiles)
                    {
                    fs = new FileSaver(img_histoS);
                    fs.saveAsTiff(saveDir+img_histoS.getTitle()+".tif");
                    img_histoS.close();
                    rioT.saveTable(hw.getTable(), prefix+"Histogram");
                    }
                else IJ.renameResults("Results", prefix+"Histogram");               
                }

            if (normHisto)
                {
                hw = rs_histo.calcHisto(histoDataN, histoX, histoY, histoBins, true, logWindow);
        		logWindow = rs_histo.logWindow;
        		histoDataN = new double[1];
                ImagePlus img_histoN = hw.getImage();
                if (img_histoN == null) return null;
                img_histoN.setTitle(img_histoN.getTitle()+" (normalized)");                      
                img_histoN.show();
                if (saveFiles)
                    {
                    fs = new FileSaver(img_histoN);
                    fs.saveAsTiff(saveDir+img_histoN.getTitle()+".tif");
                    img_histoN.close();
                    rioT.saveTable(hw.getTable(), prefix+"Histogram (normalized)");
                    }
                else IJ.renameResults("Results", prefix+"Histogram (normalized)");                      
                }
            }

        if (statistics || histo) IJ.log("Number of pixels in mask: "+maskCounter);
        
        if ((stripStats && statistics) || (stripStats && histo))
            {
            if ((satCounter==0) && (maskCounter==0)) IJ.log("No saturated pixels."); // if no mask was used.
            else IJ.log("Saturated pixels: "+IJ.d2s(satCounter,0)+" ("+IJ.d2s(100.0d*satCounter/maskCounter,2)+"% of the mask)");
            logWindow = true;
            }

        if (image) imp_out.copyScale(imp_in[0]);
        else imp_out = imp_in[0];

        return imp_out;
        } // end of calcRatio


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
    public ResultsTable calcStats(double[][] ratioResults) // Calculate statistics of a ratio table
        {
        // expand the histogram data back into an array of all values
        ArrayList<Double> ratioList = new ArrayList<Double>(ratioResults.length);              
        for (int i=0; i<ratioResults.length; i++)
            {
            for (int j=0; j<(int)Math.round(ratioResults[i][0]); j++)
                {
                ratioList.add(ratioResults[i][1]);
                }  
            }

        double[] results = new double[ratioList.size()];
        for (int i=0; i<ratioList.size(); i++) 
            {
            results[i] = (double)ratioList.get(i);
            }
        ratioList = new ArrayList<Double>(1);

        // calculate the statistics
        double median = Ratio_Statistics.getMedian(results, false); // sorting not necessary because this list already is sorted.
        double[][] halfs = Ratio_Statistics.getHalf(results);
        double q1 = Ratio_Statistics.getMedian(halfs[0], false);
        double q2 = Ratio_Statistics.getMedian(halfs[1], false);
        double[] minMax = Ratio_Statistics.getMinMax(results);

        double[] value = new double[5];
        value[0] = minMax[0];
        value[1] = q1;
        value[2] = median;
        value[3] = q2;
        value[4] = minMax[1];

        double[] invValue = new double[5];
        invValue[0] = 1/value[0];
        invValue[1] = 1/value[1];
        invValue[2] = 1/value[2];
        invValue[3] = 1/value[3];
        invValue[4] = 1/value[4];

        // old format:
        // first line: combined results // second line: standard results // third line: inverted results
        // new format:
        // first line: standard results // second line: combined results // third line: inverted results
        // combined results: 
        // show the original value if 0<=value<=1, otherwise 1 + (1 - the inverted value)
        // this way values are uniformly distributed around 1
        // Ratio Analysis uses the original values for statistics (standard results, line 1)
        
        ResultsTable rt = ResultsTable.getResultsTable();        
        rt.reset();
        rt.setPrecision(9);
        if (oldFormat) // only for compatibility    
            {
            rt.incrementCounter();
            if (value[0]>1) rt.addValue("Min", 1+(1-invValue[0]));
            else rt.addValue("Min", value[0]);    
            if (value[1]>1) rt.addValue("Quartile 1", 1+(1-invValue[1]));
            else rt.addValue("Quartile 1", value[1]);    
            if (value[2]>1) rt.addValue("Median", 1+(1-invValue[2]));
            else rt.addValue("Median", value[2]);    
            if (value[3]>1) rt.addValue("Quartile 3", 1+(1-invValue[3]));
            else rt.addValue("Quartile 3", value[3]);    
            if (value[4]>1) rt.addValue("Max", 1+(1-invValue[4]));
            else rt.addValue("Max", value[4]);    
            rt.incrementCounter();
            rt.addValue("Min", value[0]);    
            rt.addValue("Quartile 1", value[1]);    
            rt.addValue("Median", value[2]);    
            rt.addValue("Quartile 3", value[3]);    
            rt.addValue("Max", value[4]);    
            rt.incrementCounter();
            rt.addValue("Min", invValue[0]);    
            rt.addValue("Quartile 1", invValue[1]);    
            rt.addValue("Median", invValue[2]);    
            rt.addValue("Quartile 3", invValue[3]);    
            rt.addValue("Max", invValue[4]);    
            }
        else
            {
            rt.incrementCounter();
            rt.addValue("Min", value[0]);    
            rt.addValue("Quartile 1", value[1]);    
            rt.addValue("Median", value[2]);    
            rt.addValue("Quartile 3", value[3]);    
            rt.addValue("Max", value[4]);    
            rt.incrementCounter();
            if (value[0]>1) rt.addValue("Min", 1+(1-invValue[0]));
            else rt.addValue("Min", value[0]);    
            if (value[1]>1) rt.addValue("Quartile 1", 1+(1-invValue[1]));
            else rt.addValue("Quartile 1", value[1]);    
            if (value[2]>1) rt.addValue("Median", 1+(1-invValue[2]));
            else rt.addValue("Median", value[2]);    
            if (value[3]>1) rt.addValue("Quartile 3", 1+(1-invValue[3]));
            else rt.addValue("Quartile 3", value[3]);    
            if (value[4]>1) rt.addValue("Max", 1+(1-invValue[4]));
            else rt.addValue("Max", value[4]);    
            rt.incrementCounter();
            rt.addValue("Min", invValue[0]);    
            rt.addValue("Quartile 1", invValue[1]);    
            rt.addValue("Median", invValue[2]);    
            rt.addValue("Quartile 3", invValue[3]);    
            rt.addValue("Max", invValue[4]);    
            }
        rt.show("Results");
        
        return rt;
        } // end of calcStats


   /**
    * Generate_ scatter.
    *
    * @param imp_in the imp_in
    * @return the image plus
    */
   public ImagePlus generate_Scatter(ImagePlus[] imp_in) // Generate the scatter plot. 
        {
        ImageStack[] stacks = new ImageStack[3];
        stacks[0] = imp_in[0].getStack();
        stacks[1] = imp_in[1].getStack();
        int width  = imp_in[0].getWidth(); 
        int height = imp_in[0].getHeight();
        int slices = imp_in[0].getStackSize();
        int size = width*height;

        byte[][] pixel = new byte[2][size]; // image 1 and 2
        ImageProcessor[] ip = new ImageProcessor[2];
        int x, y = 0; // position
        long maxValue = 4294967295L; // max float image pixel value
        boolean overflow = false; // values were cut off
 
        // variables for the mask
        int valueMask = 0; // pixel value of the mask
        ImageProcessor ip_mask = null;                    
        byte[] pixels_mask = new byte[size];
        stacks[2] = null;
        if (mask) stacks[2] = imp_in[2].getStack();      

        // variables for the output image
        String imp_out_title = prefix+"Scatter plot";
        ImagePlus imp_out = NewImage.createFloatImage(imp_out_title, 256, 256, 1, 1);
        if (imp_out == null)
            {
            IJ.error(title, memoryError);
            return null;
            }
        WindowManager.checkForDuplicateName = true; // add a number to the title if name already exists  
        ImageProcessor ip_out = imp_out.getProcessor();

        double[][] ratioCount = new double[256][256]; // counter for ratio frequencies
        
        // generate the scatter plot      
        // maxScatter: re-scales the plot if a single ratio has a frequency > maxValue. This is hardly ever the case in normal applications.
        // if maxScatter is false, overflowing frequencies are cut off at maxValue.
        for (int i=1; i<=slices; i++) // cycle through the stack
            {                
            ip[0] = stacks[0].getProcessor(i); 
            ip[1] = stacks[1].getProcessor(i);
            pixel[0] = (byte[])ip[0].getPixels(); 
            pixel[1] = (byte[])ip[1].getPixels();
            if (mask)
                {
                ip_mask = stacks[2].getProcessor(i);
                pixels_mask = (byte[])ip_mask.getPixels();
                }
            
            for (int j=0; j<size; j++) // go through all pixels of each slice
                {
                x = pixel[0][j]&0xff; // convert byte to int
                y = pixel[1][j]&0xff;                      

                if (mask)
                    {
                    valueMask = pixels_mask[j]&0xff;
                    if (valueMask>threshold) 
                        {
                        if (maxScatter) ratioCount[x][y]++;
                        else if (ratioCount[x][y]!=maxValue) ratioCount[x][y]++; // don't increase counter further if max. possible frequency has been reached. 
                        else overflow = true; // values will be cut off
                        }
                    }
                else
                    { 
                    if (maxScatter) ratioCount[x][y]++;
                    else if (ratioCount[x][y]!=maxValue) ratioCount[x][y]++; 
                    else overflow = true; // values will be cut off
                    }
                }
            }

        if (scatterScale) // normalize plot by total amount of data
            {
            overflow = false; // reset cut-off indicator; condition gets reevaluated below
            long pixels = size*slices; // total amount of data
            double maxScale = (double)defaultScale * scatterMulti; // scale by this factor after normalization to get values back >1

            for (int i=0; i<256; i++)
                {  
                for (int j=0; j<256; j++)
                    {  
                    ratioCount[i][j] = maxScale * (ratioCount[i][j] / pixels); // normalize and scale the data
                    if (ratioCount[i][j] > maxValue) overflow = true; // values will be cut off
                    }
                }  
            }

        if (overflow)
            {
            IJ.log("Caution: Scatter plot was cut off, consider to apply scaling.");
            logWindow = true;
            }

        double scaleFactor = 1; // adjustment of max. value
        if (maxScatter)
            {
            double max = ratioCount[0][0]; // find the maximum frequency (for scaling)
            for (int i=0; i<256; i++)
                {  
                for (int j=0; j<256; j++)
                    {  
                    if (ratioCount[i][j] > max) max = ratioCount[i][j];
                    }
                }  

            if ((int)Math.round(max) > maxValue) // if max. frequency is higher than the max. possible frequency (due to image type) 
                {
                scaleFactor = max / maxValue; // maxValue = max. possible value; only scale if max. possible value would have been exceeded
                IJ.log("Scatter plot was scaled, max. frequency: "+IJ.d2s(max,0)+".");
                logWindow = true;
                }
            }

        // create the output image    
        for (int i=0; i<256; i++) // write values to image
            {  
            for (int j=0; j<256; j++)
                {  
                if (maxScatter) ratioCount[i][j] = ratioCount[i][j] / scaleFactor; // scale to max. possible value
                ip_out.putPixelValue(i, j, ratioCount[i][j]); // write the frequency as a pixel
                }
            }  

        ip_out.flipVertical();
        return imp_out;
        }


    /**
     * Show rank dist.
     */
    public void showRankDist() // visualizes the rank matrix
        {
        double[] histoDataS = new double[1];
        double[] histoDataN = new double[1];
        double[][] ratioData = new double[1][1];
        int histoX = 1024;
        int histoY = 512;
        int ratio = 0;
        int counts = 0;
        ResultsTable rt = ResultsTable.getResultsTable();        
        rt.reset();
        rt.setPrecision(9);
        if (scatter) image=true;

        if (useUniRatio) // (a-b)/(a+b)
            {
            if (standHisto) histoDataS = new double[maxPossibleValue+2];
            if (normHisto) histoDataN = new double[maxPossibleValue+2];
            if (statistics) ratioData = new double[maxPossibleValue+2][2];
            }
        else // a/b
            {
            if (standHisto) histoDataS = new double[maxPossibleValue];
            if (normHisto) histoDataN = new double[maxPossibleValue];
            if (statistics) ratioData = new double[maxPossibleValue][2];
            }

        double[][][] ranks = rankGenerator(); // 0 = original rank, 1 = final rank, 2 = counter, 3 = ratio                  

        ImagePlus img_ratio = null; // prepare output image
        ImageProcessor ip = null;
        if (image)
            {
            img_ratio = NewImage.createShortImage("Ratios", ranks.length, ranks.length, 1, 1); // create a new image with the same dimensions as the first chosen image
            WindowManager.checkForDuplicateName = true; // add a number to the title if name already exists  
            ip = img_ratio.getProcessor();
            }
            
        for (int i=0; i<ranks.length; i++) // create the output data
            {
            for (int j=0; j<ranks.length; j++)
                {
                counts = (int)ranks[i][j][2];

                rt.incrementCounter();
                rt.addValue("x",i);
                rt.addValue("y",j);
                rt.addValue("ratio",ranks[i][j][3]);                    
                rt.addValue("rank",ranks[i][j][1]);                    
                rt.addValue("frequency",counts);                    

                ratio = ((int)ranks[i][j][1])-1; // '-1' because the index of arrays starts at '0'.
                if (standHisto) histoDataS[ratio]++;                      
                if (normHisto) histoDataN[ratio] = histoDataN[ratio] + (1.0d/counts); // normalized by frequency  
                if (statistics && (ratioData[ratio][1]==0)) ratioData[ratio][1] = ranks[i][j][3]; // counts needed for statistics                   
                if (image) ip.putPixelValue(i, j, ((int)ranks[i][j][1]));
                }
            }

        rt.show("Results");
        IJ.renameResults("Results", "Statistical ratio distribution");                      

        // Note: The lower and upper quartiles fall on the boundaries between '0.498039216' and '0.5' and between '2' and '2.007874016', respectively. 
        // During the calculation of medians, each time the lower value is chosen.
        // Because 0/0 is treated as a ratio of 1, this distribution is skewed by one value towards the middle.
 
        if (showUnbinned && (histo || statistics)) // show original data
	        {
	        rt = ResultsTable.getResultsTable();        
	        rt.reset();
	        rt.setPrecision(9);
	        if (standHisto)
	            {
	            for (int i=0; i<histoDataS.length; i++)
	                {
	                rt.incrementCounter();
	                if (statistics) rt.addValue("ratio",ratioData[i][1]);
	                rt.addValue("frequency",histoDataS[i]);
	                }
	            rt.show("Results");
	            IJ.renameResults("Results", prefix+"Original Data"); 
	            }
	        if (normHisto)
	            {
	            rt.reset();
	            for (int i=0; i<histoDataN.length; i++)
	                {
	                rt.incrementCounter();
	                if (statistics) rt.addValue("ratio",ratioData[i][1]);
	                rt.addValue("frequency",histoDataN[i]);
	                }
	            rt.show("Results");
	            IJ.renameResults("Results", prefix+"Original Data (normalized)");                      
	           }
	        }
        
        if (statistics)
            {
            if (standHisto) // add frequency data to the ratio table
                {
                for (int i=0; i<ratioData.length; i++)
                    {
                    ratioData[i][0] = histoDataS[i];
                    }
                calcStats(ratioData);
                IJ.renameResults("Results", "Statistics");                             
                }
            if (normHisto) // normalized data
                {
                for (int i=0; i<ratioData.length; i++)
                    {
                    ratioData[i][0] = histoDataN[i];
                    }
                calcStats(ratioData);
                IJ.renameResults("Results", "Statistics (normalized)");                             
                }
            ratioData = new double[1][1]; // free memory
            }
        
        if (histo)
            {
            HistoWrapper hw; // Object containing histo image and table
            Ratio_Statistics rs_histo = new Ratio_Statistics(drawMax, normValue, defaultScale, maxPossibleValue, scaleOption, maskCounter, satCounter, logWindow, prefix, memoryError, title);
       	
            if (standHisto)
                {
                hw = rs_histo.calcHisto(histoDataS, histoX, histoY, histoBins, false, logWindow);
        		logWindow = rs_histo.logWindow;
         		histoDataS = new double[1];
                ImagePlus img_histoS = hw.getImage(); 
                if (img_histoS == null) return;
                img_histoS.show();
                IJ.renameResults("Results", "Histogram");               
                }

            if (normHisto) // normalized data
                {
                hw = rs_histo.calcHisto(histoDataN, histoX, histoY, histoBins, true, logWindow);
        		logWindow = rs_histo.logWindow;
                histoDataN = new double[1];
                ImagePlus img_histoN = hw.getImage();
                if (img_histoN == null) return;
                img_histoN.setTitle(img_histoN.getTitle()+" (normalized)");                      
                img_histoN.show();
                IJ.renameResults("Results", "Histogram (normalized)");                      
                }
            }

        if (image)
            {
            img_ratio = Ratio_InOut.spectrumLUT(img_ratio, maxPossibleValue, displayLUT); // change the lookup table
            img_ratio.show();
            }

        if (scatter)
            {
            // prepare data for generate_Scatter()
            mask = false; // otherwise the generate_scatter crashes
            ImagePlus img_dupl = new Duplicator().run(img_ratio); // change a copy of the ratio image to 8 bit
            ImageConverter ic = new ImageConverter(img_dupl);
            ImageConverter.setDoScaling(true);
            ic.convertToGray8();
            ImagePlus[] img_temp = new ImagePlus[2]; // generate_scatter needs an array
            img_temp[0] = img_dupl;
            img_temp[1] = img_dupl;            
            ImagePlus img_scatter = generate_Scatter(img_temp);
            // find the max value, apply LUT and scale it
            ImageStatistics iSt = new ImageStatistics();
            iSt = ImageStatistics.getStatistics(img_scatter.getProcessor(), 16, null); // Get raw MIN_MAX
            WindowManager.setTempCurrentImage(img_scatter); 
            IJ.run("Fire"); // change the lookup table
            img_scatter.getProcessor().setMinAndMax(0,iSt.max);
            img_scatter.show();
            }
        }


    /**
     * Rank generator.
     *
     * @return the double[][][]
     */
    public double[][][] rankGenerator() // generates the rank matrix
        {        
        IJ.showStatus("Calculating the ranks...");
        double[][][] matrix = createMatrix();
        return matrix;
        }


    /**
     * Creates the matrix.
     *
     * @return the double[][][]
     */
    private double[][][] createMatrix() // really generates the rank matrix
        {
        // a symmetric matrix will be created that contains all possible values for the division of two values running from 0 two 255.
        int size = 256;
        double[][] matrix = new double[7][size*size]; // counter x, counter y, total counter, ratio, final rank without repetitions, counter for repetitions, initial rank (has to be the last value!)
        double di = 0, dj = 0; // for the calculation of "ratio"
        double ratio;
        int counter = 0;
        if (useUniRatio) // instead of x/y
            {
            IJ.log("Ratio (x-y)/(x+y) is used for calculations.");
            logWindow = true;
            }
        
        // generate all possible values, including repetitions, ranks are not yet correct.
        for (int i=0; i<size; i++)
            {
            for (int j=0; j<size; j++)
                {
                if (useUniRatio) ratio = (di - dj) / (Math.abs(di) + Math.abs(dj)); // calculate a ratio of these two pixel values
                else
                    {
                    if ((di==0) && (dj==0)) ratio = 1.0d; // catch 0/0
                    else if (di==0) ratio=(1.0d/255.0d)/4.0d; // catch 0/y
                    else if (dj==0) ratio=255.0d*4.0d; // catch x/0
                    else ratio = di / dj;
                    }

                matrix[0][counter]=i; // counter x                
                matrix[1][counter]=j; // counter y                 
                matrix[2][counter]=counter; // total counter                 
                matrix[3][counter]=ratio; // ratio                 
                matrix[6][counter]=counter+1; // preliminary rank                 
                counter++;
                dj++;
                }
            dj=0;                
            di++;
            }

        matrix = sortMatrix(matrix, 3, 4, 5, 2); // correctly assign ranks, and equal ranks to equal ratio values
        // 3 = ratios, 4 = final rank, 5 = repetition counter, 2 = total counter

        double ranks[][][] = new double[size][size][4]; // write values into output matrix
        for (int i=0; i<size*size; i++)
            { 
            ranks[(int)Math.round(matrix[0][i])][(int)Math.round(matrix[1][i])][0] = Math.round(matrix[6][i]); // original rank            
            ranks[(int)Math.round(matrix[0][i])][(int)Math.round(matrix[1][i])][1] = Math.round(matrix[4][i]); // final rank            
            ranks[(int)Math.round(matrix[0][i])][(int)Math.round(matrix[1][i])][2] = Math.round(matrix[5][i]); // counter            
            ranks[(int)Math.round(matrix[0][i])][(int)Math.round(matrix[1][i])][3] = matrix[3][i]; // ratio            
            }
        return ranks;        
        }


     /**
      * Sort matrix.
      *
      * @param matrix the matrix
      * @param ratioID the ratio id
      * @param finalRankID the final rank id
      * @param counterID the counter id
      * @param totalID the total id
      * @return the double[][]
      */
     private double[][] sortMatrix(double[][] matrix, int ratioID, int finalRankID, int counterID, int totalID) // assign the same rank to equal ratio values // RANK HAS TO BE IN THE LAST POSITION, OTHERWISE IT DOESN'T WORK
        // 3 = ratios, 4 = final rank, 5 = repetition counter, 2 = total counter
        {  
        int rankID = matrix.length-1;
        
        matrix = quicksort(matrix, ratioID, true); // assign the ranks to their correct values (ascending ratios)

        boolean sorted = false;  
        while (!sorted) // if ratios are equal, assign the highest rank (= last column) to all 
            {  
            sorted = true;  
            for (int i=0; i<matrix[0].length-1; i++)
                {  
                if ((matrix[ratioID][i] == matrix[ratioID][i+1]) && (matrix[rankID][i] != matrix[rankID][i+1]))
                    {  
                    matrix[rankID][i] = matrix[rankID][i+1];  
                    sorted = false;  
                    }  
                }  
            }  

        matrix = quicksort(matrix, rankID, false); // sort by rank

        for (int i=0; i<matrix[0].length; i++) // count how many time each rank occurs, identify gaps
            {
		    matrix[counterID][(int)Math.round(matrix[rankID][i])-1]++;
            }
            
        int newRankCounter = 1; // count the number of new ranks without gaps
        
        for (int i=0; i<matrix[0].length; i++) // generate the new ranks without gaps
            {
            if (matrix[counterID][i]>0) 
                {
                matrix[finalRankID][(int)Math.round(matrix[rankID][i])-1]=newRankCounter; // add a new rank if it's not a gap
                newRankCounter++;
                }
            else matrix[finalRankID][(int)Math.round(matrix[rankID][i])-1]=0; // assign rank 0 to gaps          
            }

        int nwc = 0; // temp counter

        for (int i=0; i<matrix[0].length; i++)
            {
            if (matrix[finalRankID][i]==0) // assign the same final rank to all equal ranks
                { 
                while (matrix[finalRankID][i+nwc]==0) // find the last one
                    {
                    nwc++;
                    }
                matrix[finalRankID][i]=matrix[finalRankID][i+nwc]; // the final rank is always at the position where the repetition occurs for the last time
                }
            nwc=0;

            if (matrix[counterID][i]==0) // assign the same gap count to all equal ranks
                { 
                while (matrix[counterID][i+nwc]==0)
                    {
                    nwc++;
                    }
                matrix[counterID][i]=matrix[counterID][i+nwc];
                }
            nwc=0;
          }

        matrix = quicksort(matrix, totalID, false); // sort back into the original sequence

        return matrix;  
        }  


    /**
     * Quicksort.
     *
     * @param a the a
     * @param compareID the compare id
     * @param skip the skip
     * @return the double[][]
     */
    public double[][] quicksort(double[][] a, int compareID, boolean skip) // sort [column][row] after [compareID][row]; if skip=true, skip the last column
        {
        a = quicksort(a, 0, a[0].length-1, compareID, skip);
        return a;
        }


    /**
     * Quicksort.
     *
     * @param a the a
     * @param lo the lo
     * @param hi the hi
     * @param compareID the compare id
     * @param skip the skip
     * @return the double[][]
     */
    private double[][] quicksort (double[][] a, int lo, int hi, int compareID, boolean skip) // modified from: http://www.inf.fh-flensburg.de/lang/algorithmen/sortieren/quick/quicken.htm
        {
        //  lo is the lower index, hi is the upper index of the region of array a that is to be sorted
        int i=lo, j=hi;
        double[] h = new double[a.length];
        double x=a[compareID][(lo+hi)/2]; // comparison element x
    
        do //  partition
            {    
            while (a[compareID][i]<x) i++; 
            while (a[compareID][j]>x) j--;
            if (i<=j)
                {
                if (skip)
                    {
                    for (int k=0;k<a.length-1;k++) // the last element in the array is not modified (rank)
                        {
                        h[k]=a[k][i]; 
                        a[k][i]=a[k][j]; 
                        a[k][j]=h[k];
                        }
                    }
                else
                    {
                    for (int k=0;k<a.length;k++) // the last element in the array is included (rank)
                        {
                        h[k]=a[k][i]; 
                        a[k][i]=a[k][j]; 
                        a[k][j]=h[k];
                        }
                    }
                i++; j--;
                }
            } while (i<=j);
    
        //  recursion
        if (lo<j) quicksort(a, lo, j, compareID, skip);
        if (i<hi) quicksort(a, i, hi, compareID, skip);
        
        return a;
        }
        

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

        String[] bitdepth_temp = new String[2];
        bitdepth_temp[0] = "8 bit";
        bitdepth_temp[1] = "16 bit";

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

		int cgRows2=1, cgColumns2=2;
		String[] cgLabels2 = 
		    {
            "Generate ratio image",
            "Display LUT",
		    };
		boolean[] cgStates2 = {image,displayLUT};

		int cgRows1=2, cgColumns1=2;
		String[] cgLabels1 = 
		    {
            "Generate scatter plot",
            "Calculate statistics",
            "Generate histogram",
            "Calculate without the highest intensity value",
		    };
		boolean[] cgStates1 = {scatter,statistics,histo,stripStats};

        // create/show the dialog                
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Stack 1:", image_titles, image_titles[0]);
        gd.addChoice("Stack 2:", image_titles, image_titles[1]);

		gd.setInsets(20,20,0);
		gd.addCheckboxGroup(cgRows3, cgColumns3, cgLabels3, cgStates3);
        gd.addStringField("Prefix to filenames:", prefix, 10);

		gd.setInsets(20,20,0);
		gd.addCheckboxGroup(cgRows2, cgColumns2, cgLabels2, cgStates2);
        gd.addChoice("Bit depth of output image:", bitdepth_temp, bitdepth_temp[0]);

		gd.setInsets(20,20,0);
		gd.addCheckboxGroup(cgRows1, cgColumns1, cgLabels1, cgStates1);

        if (image_titles.length > 2)
            {
    		gd.setInsets(20,20,0);
            gd.addCheckbox("Use mask for statistics, plots and ratio image", mask);
            gd.addChoice("Mask:", image_titles, image_titles[2]);
            }
        gd.addMessage("Calculation of statistics, a histogram or a scatter plot without a mask is not recommended.");

		gd.setInsets(20,0,0);
        gd.addNumericField("Number of bins (histograms):", histoBins, 0, 9, "bins"); // title, default value, digits, length, units
        gd.addChoice("Scaling of histograms:", scaleOption_temp, scaleOption_temp[scaleOption]);
        gd.addNumericField("Scaling factor (for options 1 & 4):", defaultScale, 0, 9, "factor * (frequency / total amount of data)");
        gd.addNumericField("Image height (for option 4):", drawMax, 0, 9, "(max. displayed frequency, not height in pixels)");
        gd.addCheckbox("Show histograms/statistics with original values", standHisto);
        gd.addCheckbox("Show histograms/statistics with values normalized by statistical frequency", normHisto);
        gd.addCheckbox("Also show unbinned, original histogram data", showUnbinned);

		gd.setInsets(20,20,0);
        gd.addCheckbox("Scale scatter plot if max. frequency exceeds max. representable number (otherwise cut off higher values)", maxScatter);
        gd.addCheckbox("Normalize scatter plot to the total amount of data (use scaling factor for histograms)", scatterScale);
        gd.addNumericField("Multipl. factor for scatter plot scaling during normalization:", scatterMulti, 1, 9, "* histogram scaling factor");

		gd.setInsets(20,20,0);
        gd.addCheckbox("Show the rank matrix and histogram instead of processing image data (don't use scaling option 4!)", showRanks);
        gd.addCheckbox("Use the ratio (x-y)/(x+y) instead of x/y", useUniRatio);
        gd.addCheckbox("Use old format for statistics files.", oldFormat);
        gd.addCheckbox("Calculate ratio image separately to save memory (takes more time)", extraRatio);
        gd.addCheckbox("Show parameters in log window", logInfo);

        gd.showDialog();
        if (gd.wasCanceled()) return null;
        
        // process the input
        int[] choice_index = new int[3];      
        choice_index[0] = gd.getNextChoiceIndex(); // images
        choice_index[1] = gd.getNextChoiceIndex();
        bitdepth = gd.getNextChoiceIndex(); // 0 = 8 bit, 1 = 16 bit, 2 = 32 bit
        if (image_titles.length > 2) choice_index[2] = gd.getNextChoiceIndex(); // mask
        scaleOption = gd.getNextChoiceIndex(); // 0 = Factor, 1 = Image, 2 = No, 3 = Normalize

        histoBins = (int)gd.getNextNumber();
        defaultScale = (int)gd.getNextNumber();
        drawMax = (int)gd.getNextNumber();
        scatterMulti = (double)gd.getNextNumber();

        keepFiles = gd.getNextBoolean();
        saveFiles = gd.getNextBoolean();
        image = gd.getNextBoolean();
        displayLUT = gd.getNextBoolean();
        scatter = gd.getNextBoolean();
        statistics = gd.getNextBoolean();
        histo = gd.getNextBoolean();
        stripStats = gd.getNextBoolean();

        mask = false;
        if (image_titles.length > 2)
            {
            mask = gd.getNextBoolean();
            }

        standHisto = gd.getNextBoolean();
        normHisto = gd.getNextBoolean();
        if ((!standHisto) && (!normHisto)) standHisto=true;
        showUnbinned = gd.getNextBoolean();
        maxScatter = gd.getNextBoolean();
        scatterScale = gd.getNextBoolean();
        showRanks = gd.getNextBoolean();
        useUniRatio = gd.getNextBoolean();
        oldFormat = gd.getNextBoolean();
        extraRatio = gd.getNextBoolean();
        logInfo = gd.getNextBoolean();
        if (!image) extraRatio = false;
        if (!statistics && !histo)
            {
            extraRatio = false;
            stripStats = false;
            }

        prefix = gd.getNextString();

        // analyze the validity of the input images, create output array
        if (!image && !statistics && !scatter && !histo)
            {
            IJ.error(title, "No output has been chosen - aborting.");
            return null;
            }

        ImagePlus[] img_out = new ImagePlus[3];        
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

        if (mask) 
            {
            img_out[2] = WindowManager.getImage(open_images[choice_index[2]]);
            if (img_out[0].getWidth() != img_out[2].getWidth() || img_out[0].getHeight() != img_out[2].getHeight() || img_out[0].getStackSize() != img_out[2].getStackSize())
                {         
                IJ.error(title, "The mask is not compatible.");
                return null;
                }
            if (img_out[2].getType() != ImagePlus.GRAY8)
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
    	if (mask) IJ.log("Mask: "+img_out[2].getTitle());
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
    		IJ.log("Plot height (normalized): "+IJ.d2s(drawMax/normValue,0));
    		}
    	if (scatter) 
    		{
	    	if (maxScatter) IJ.log("Scatter plot scaled");
	    	if (scatterScale) IJ.log("Scatter plot normalized");
    		IJ.log("Multiplication factor for scatter plot: "+IJ.d2s(scatterMulti,2));
    		}
    	if (useUniRatio) IJ.log("Ratio (x-y)/(x+y) was used");
    	else IJ.log("Ratio x/y was used");
    	if (oldFormat) IJ.log("Old format was used for statistics");
	    }
    
    }