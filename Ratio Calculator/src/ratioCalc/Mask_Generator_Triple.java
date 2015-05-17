package ratioCalc;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackConverter;

// _____________________________________________________________________________________________________________
//
// ------------------------------------
// Mask Generator v1.12a
// ------------------------------------
//
// Till Andlauer
// Date: 11-03-01 - 12-09-27
//
// The thresholding algorithm was adapted from Gabriel Landini's AutoThresholder class, part of the ImageJ code
// Masking uses a conversion adapted from the function "convertShortToByte" of the ImageJ class "TypeConverter" 
//
// Version history
// xxxxxxxxxxxxxxx
//
// v1.0
// Mask generator combines two images, where the first is the input (8- or 16-bit) and the second one a mask
//
// v1.10
// Support for automatic thresholding, based on the entire stack or substacks (or slices)
// Optional merge of two input images prior to masking
// Masking is optional (plugin can also be used just for thresholding)
//
// v1.11
// Interpolation of thresholds
//
// v1.12
// Workaround for single-slice images (no stacks) 
//
// v1.12a - final version used for ratio analysis
// Minor adaptations and debugging
//
// _____________________________________________________________________________________________________________


public class Mask_Generator_Triple implements PlugIn
    {
    private String title = "Mask Generator v1.12a"; // title for dialogs and error messages
    private String memoryError = "Out of memory...";
    private int threshold = -1; // thresholding value, re-set in execThresh() 
    // All of the following options can be set in chooseImages():
    private double defaultThreshold = 0.8d; // default threshold for dialog (-> factor)
    private boolean merge = true; // merge stack and (Amira) segmentation mask. If "false" only thresholding is performed.
    private boolean thresh = true; // threshold
    private boolean tSlice = true; // calculate threshold based on substacks
    private boolean iThresh = true; // interpolate thresholds
    private boolean showRes = false; // show results table with thresholds
    private boolean manual = false; // manually define the number of substacks
    private boolean ratio = false; // input image is a ratio image from ratio calculator
    private boolean mask = false; // keep non-thresholded mask
    private boolean keepAM = false; // keep (Amira) segmentation mask file. "false" can lead to crashes 
    private boolean perc = false; // percentile thresholding (otherwise isodata); set by choice:Method
    private boolean logInfo = true; // Show parameters in the log window
    private double factor = 0; // thresholding factor (see default threshold)
    private int nBins = 0; // number of substacks
    private int cutOff = 0; // min. number of slices per substack
    
    public void run(String arg) 
        {        
    	loadConfig();
        // Create and show user dialog to select images and set options
        ImagePlus[] img = chooseImages();
        if (img == null) return;

        double start_time = System.currentTimeMillis(); // get the current system time
        
        // If the input image is not a stack, create a 'virtual' stack
        // A 2nd, black slice is added. Because the mask is also black (=0 =blank) here, this second slice will be automatically excluded in the ratio calculator.
        ImageStack tempStack;               
        ImagePlus tempImg;
        if (img[0].getStackSize()==1)
            {
            ImagePlus blackImg = IJ.createImage("", "8 black", img[0].getWidth(), img[0].getHeight(), 1);
            ImageProcessor blackIP = blackImg.getProcessor();
            
            for (int i=0; i<img.length; i++)
                {
                if (img[i]!=null)
                    {
                    tempStack = new ImageStack(img[i].getWidth(), img[i].getHeight());               
                    tempStack.addSlice(null, img[i].getProcessor());                  
                    tempStack.addSlice(null, blackIP);                  
                    tempImg = new ImagePlus(img[i].getTitle()+" Stack", tempStack);
                    tempImg.copyScale(img[i]);
                    img[i] = tempImg;
                    img[i].show();
                    }
                }
            }

        ImageStack[] stacks = new ImageStack[3];
        stacks[0] = img[0].getStack();
        stacks[1] = img[1].getStack();
        stacks[2] = img[2].getStack();
        int w  = img[0].getWidth(); 
        int h = img[0].getHeight();
        int s = img[0].getStackSize();

        RGBStackMerge rsm = new RGBStackMerge();
        ImageStack s_rgb = rsm.mergeStacks(w, h, s, stacks[0], stacks[1], stacks[2], true);
        ImagePlus img_rgb = new ImagePlus("RGB", s_rgb);          
        StackConverter sc = new StackConverter(img_rgb);
        sc.convertToGray8();

        img[0]=img_rgb; 
        img[1]=null; 
        img[2]=null; 
        IJ.freeMemory();

        // Mask image
        ImagePlus img_mask = null;
        if (merge)
            {
            img_mask = createMask(img);
            if (img_mask == null) return;
            if (!thresh) // the job is finished then
                {
                img_mask.show();
                IJ.run("Fire");
                }
            if (mask) // show a copy of the mask without thresholding
                {
                ImagePlus img_show = new Duplicator().run(img_mask);
                img_show.setTitle(img_mask.getTitle());
                img_show.show();
                IJ.run("Fire");
                }            
            }

        // Threshold the (masked) image
        // The thresholding algorithm was adapted from Gabriel Landini's AutoThresholder class, part of the ImageJ code
        if (thresh)
            {
            IJ.showStatus("Thresholding...");
            ImagePlus img_thresh = null;
            
            if (!manual && (cutOff>=img[0].getNSlices())) tSlice=false; // no substacks if the complete stack contains too few slices
            if (tSlice) // divide the stack into substacks; find a threshold for each substack
                {
                ResultsTable rt = ResultsTable.getResultsTable(); 
                rt.reset();
                rt.setPrecision(9);
                ImageStack inputStack = new ImageStack();               
                if (merge) inputStack = img_mask.getStack();
                else inputStack = img[0].getStack();               
                int size = inputStack.getSize();
                ImageStack finalStack = new ImageStack(inputStack.getWidth(), inputStack.getHeight());               
                ImageProcessor tempIP;

                // Empty slices shouldn't be part of the substacks. Therefore empty slice at the start and end of the stack are excluded from the substacks.
                int eSlice[] = new int[2]; // contains the positions of the first/last slice with data
                eSlice[0] = 0;
                eSlice[1] = 0;
                ImageStatistics iSt = new ImageStatistics();

                for (int i=1; i<=size; i++) // search for empty slices at the start
                    {                
                    tempIP = inputStack.getProcessor(i);
                    iSt = ImageStatistics.getStatistics(tempIP, 16, null); // Get raw MIN_MAX
                    if (iSt.max==0) eSlice[0]++;
                    else i=size+1;
                    }
                for (int i=size; i>0; i--) // search for empty slices at the end
                    {                
                    tempIP = inputStack.getProcessor(i);
                    iSt = ImageStatistics.getStatistics(tempIP, 16, null);
                    if (iSt.max==0) eSlice[1]++;
                    else i=0;
                    }
                   
                // Calculate substack size
                if (!manual) nBins = (size-eSlice[0]-eSlice[1]) / cutOff; // Integer division to round down
                long bins = Math.round(1.0d*(size-eSlice[0]-eSlice[1])/(double)nBins);
                int corrFactor = (size-eSlice[0]-eSlice[1])-((int)bins * nBins);

                // Add empty slices at the beginning to the final stack                        
                if (eSlice[0]>0)
                    {    
                    for (int i=1; i<=eSlice[0]; i++)
                        {
                        finalStack.addSlice(null, inputStack.getProcessor(i));
                        }
                    }

                // Create the substacks                        
                int[][] stackPos = new int[(int)Math.round(nBins)][3];
                int binC = 0;
                int sC=0;
                if (nBins==1) iThresh = false;

                for (int i=(1+eSlice[0]); i<=(size-eSlice[1]); i+=bins)
                    {
                    stackPos[binC][0] = sC+eSlice[0];
                    tempStack = new ImageStack(inputStack.getWidth(), inputStack.getHeight());               
                    for (int j=1; j<=bins; j++)
                        {
                        if ((i+j-1)<=(size-eSlice[1]))
                            {
                            tempIP = inputStack.getProcessor(i+j-1);
                            tempStack.addSlice(null, tempIP);
                            sC++;
                            }
                        }
                    // Add additional slices if the last substack needs to be longer (avoids rounding errors)
                    if (((binC+1)==nBins) && (sC<(size-eSlice[0]-eSlice[1])))
                        {
                        for (int j=size-eSlice[1]-corrFactor+1; j<=size-eSlice[1]; j++)
                            {
                            tempIP = inputStack.getProcessor(j);
                            tempStack.addSlice(null, tempIP);
                            sC++;
                            }
                        i=size-eSlice[1]+1;
                        }
                    stackPos[binC][1] = sC+eSlice[0]-1;
                    IJ.log("Substack "+IJ.d2s(binC+1,0)+": "+IJ.d2s(stackPos[binC][0]+1,0)+"-"+IJ.d2s(stackPos[binC][1]+1,0));
                        
                    // Calculate thresholds                        
                    tempImg  = new ImagePlus(null, tempStack);
                    if (iThresh) 
                        {
                        tempImg = execThresh(tempImg, true, false);   
                        stackPos[binC][2] = threshold;
                        }
                    else // if no interpolation, the calculation of thresholds is finished here
                        {
                        tempImg = execThresh(tempImg, true, true);   
                    
                        // Re-combine the substacks                        
                        for (int j=1; j<=tempStack.getSize(); j++)
                            {
                            finalStack.addSlice(null, tempStack.getProcessor(j));
                            }
                        }
                    binC++;
                    }

                // Interpolate the thresholds from one centre of a substack to the next
                if (iThresh)
                    {
                    double threshDiff = 0.0d;
                    int[] binSize = new int[2];
                    int[] middle = new int[2];
                    double addFactor = 0.0d;
                    int[] finalThreshs = new int[size];
                    double currThresh = 0.0d;

                    for (int i=0; i<nBins-1; i++)
                        {
                        threshDiff = stackPos[i+1][2]-stackPos[i][2];
                        binSize[0] = stackPos[i][1]-stackPos[i][0]+1;
                        binSize[1] = stackPos[i+1][1]-stackPos[i+1][0]+1;
                        middle[0] = binSize[0] / 2; // Integer division to round down
                        middle[1] = binSize[1] / 2;
                        addFactor = threshDiff / (middle[0]+middle[1]);
                        middle[0] = stackPos[i][0]+middle[0];
                        middle[1] = stackPos[i+1][0]+middle[1];

                        if (i==0)
                            {
                            for (int j=stackPos[i][0]; j<middle[0]; j++)
                                {
                                finalThreshs[j] = stackPos[i][2];
                                }
                            }

                        finalThreshs[middle[0]] = stackPos[i][2];
                        currThresh = stackPos[i][2];
                        for (int j=middle[0]+1; j<middle[1]; j++)
                            {
                            currThresh+=addFactor;
                            finalThreshs[j] = (int)Math.round(currThresh);
                            }
                        finalThreshs[middle[1]] = stackPos[i+1][2];

                        if (i==nBins-2)
                            {
                            for (int j=middle[1]+1; j<=stackPos[i+1][1]; j++)
                                {
                                finalThreshs[j] = stackPos[i+1][2];
                                }
                            }
                        } // interpolation done.
                    
                    for (int i=0; i<size; i++) // show the thresholds
                        {
                        rt.incrementCounter();
                        rt.addValue("threshold", finalThreshs[i]);    
                        }

                    // Apply the interpolated thresholds
                    for (int i=eSlice[0]+1; i<=size-eSlice[1]; i++)
                        {
                        threshold = finalThreshs[i-1];
                        tempIP = inputStack.getProcessor(i);
                        tempImg  = new ImagePlus(null, tempIP);
                        tempImg = execThresh(tempImg, false, true);   
                        finalStack.addSlice(null, tempImg.getProcessor()); // re-combine the stack
                        }
                    }

                // Add empty slices at the end to the final stack                        
                if (eSlice[1]>0)
                    {    
                    for (int i=(size-eSlice[1]+1); i<=size; i++)
                        {
                        finalStack.addSlice(null, inputStack.getProcessor(i));
                        }
                    }

                img_thresh = new ImagePlus("Mask", finalStack);
                img_thresh.copyScale(img[0]);

                // Calculate the average threshold (for display only)
                double value = 0;
                double mean = 0;
                int c = 0;
                for (int i=0; i<rt.getCounter(); i++)
                    {
                    value = rt.getValueAsDouble(0,i);
                    if (value>0)
                        {
                        mean+=value;
                        c++;
                        }                    
                    }
                mean = mean / c;
                IJ.log("Mean threshold: "+IJ.d2s(Math.round(mean),0));                
                IJ.log("");
                img_thresh.setTitle("Mask mean "+IJ.d2s(Math.round(mean),0));  
                if (showRes) rt.show("Results");
                }                
            // Threshold with a value based on the entire stack, not for substacks
            else // tSlice = false
                {
                if (merge) img_thresh = execThresh(img_mask, true, true);
                else img_thresh = execThresh(img[0], true, true);
                }
            // Display the thresholded image
            if (img_thresh == null) return;
            img_thresh.show();
            if (!ratio) IJ.run("Fire");
            }

        if (merge && !keepAM) img[3].close();
        IJ.showStatus("The calculation took "+IJ.d2s((System.currentTimeMillis()-start_time)/1000.0d, 2)+" seconds."); // display the amount of time used.
        }

    
    private boolean loadConfig()
		{
		Ratio_Config rc;
		rc = new Ratio_Config();
		if (rc.error) return false;
		else
			{
			String[] ints = {"nBins", "cutOff"};
			String[] doubles = {"defaultThreshold", "factor"};
			String[] booleans = {"merge", "thresh", "tSlice", "iThresh", "showRes", "manual", "ratio", "maskM", "keepAM", "perc"};
			int cInt = 0;
			double cDouble = 0.0d;
			boolean cBool = false;
			
	    	cInt = rc.getInt(ints[0]);
	    	if (!rc.error) nBins=cInt;
	    	else rc.error=false;
	    	cInt = rc.getInt(ints[1]);
	    	if (!rc.error) cutOff=cInt;
	    	else rc.error=false;

	    	cDouble = rc.getDouble(doubles[0]);
	    	if (!rc.error) defaultThreshold=cDouble;
	    	else rc.error=false;
	    	cDouble = rc.getDouble(doubles[1]);
	    	if (!rc.error) factor=cDouble;
	    	else rc.error=false;
	    	
	    	cBool = rc.getBoolean(booleans[0]);
	    	if (!rc.error) merge=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[1]);
	    	if (!rc.error) thresh=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[2]);
	    	if (!rc.error) tSlice=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[3]);
	    	if (!rc.error) iThresh=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[4]);
	    	if (!rc.error) showRes=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[5]);
	    	if (!rc.error) manual=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[6]);
	    	if (!rc.error) ratio=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[7]);
	    	if (!rc.error) mask=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[8]);
	    	if (!rc.error) keepAM=cBool;
	    	else rc.error=false;
	    	cBool = rc.getBoolean(booleans[9]);
	    	if (!rc.error) perc=cBool;
	    	else rc.error=false;
			}
		return true;
		}
    

     public ImagePlus execThresh(ImagePlus imp, boolean calc, boolean apply) // Adapted from Gabriel Landini's AutoThresholder class
        {
        ImageProcessor ip = imp.getProcessor();
        int nI = 256; // size of histogram (gets overwritten below)

        // Calculate the threshold value
        if (calc)
            {
            threshold=-1; // global variable
            int currentSlice = imp.getCurrentSlice();
            int[] data = (ip.getHistogram());
            nI = data.length;
            int[] temp = new int[nI];
            ResultsTable rt = ResultsTable.getResultsTable(); 
    
            // Get the stack histogram into the data[] array
            temp=data;
            for (int i=1; i<=imp.getStackSize(); i++)
                {
                if (i==currentSlice) continue; // Ignore the slice that has already been included 
                imp.setSliceWithoutUpdate(i);
                ip = imp.getProcessor();
                temp = ip.getHistogram();
                for(int j=0; j<nI; j++) 
                    {
                    data[j]+=temp[j];
                    }
                }   
            imp.setSliceWithoutUpdate(currentSlice);
     
            // Ignore lowest and highest values
            data[0] = 0;
            data[nI-1] = 0;
     
            // Bracket the histogram to the range that holds data to make the calculation quicker
            int minbin=-1, maxbin=-1;
            for (int i=0; i<nI; i++)
                {
                if (data[i]>0) maxbin = i;
                }
            for (int i=nI-1; i>=0; i--)
                {
                if (data[i]>0) minbin = i;
                }
            int [] data2 = new int [(maxbin-minbin)+1];
            if (minbin!=-1)
                {
                for (int i=minbin; i<=maxbin; i++)
                    {
                    data2[i-minbin]= data[i];;
                    }
                // Calculate thresholds 
                if (perc) // percentile threshold
                    {
                    threshold = percentile(data2);
                    threshold+=minbin; // Add the offset of the histogram to the threshold
                    if (tSlice && !iThresh)
                        {
                        rt.incrementCounter();
                        rt.addValue("threshold", threshold);    
                        }
                    else if (!tSlice) IJ.log("Percentile: "+threshold);
                    }
                else // isodata threshold
                    {
                    threshold = isoData(data2);
                    threshold+=minbin;
                    if (tSlice && !iThresh)
                        {
                        rt.incrementCounter();
                        rt.addValue("threshold", threshold);    
                        }
                    else if (!tSlice) IJ.log("IsoData: "+threshold);
                    }
                }
            else // no threshold
                {
                threshold=-1;
                if (tSlice && !iThresh)
                    {
                    rt.incrementCounter();
                    rt.addValue("threshold", 0);    
                    }
                } 
            }
    
        // Threshold the image
        if (apply && (threshold>-1))
            {
            int xe = ip.getWidth();
            int ye = ip.getHeight();
            int x, y, b = 0;
            int c = imp.getBitDepth()==8?255:65535;

            imp.setDisplayRange(0, Math.max(b,c)); // Otherwise we can never set the threshold 

            for(int j=1; j<=imp.getStackSize(); j++) 
                {
                imp.setSlice(j);
                ip=imp.getProcessor();
                for (y=0; y<ye; y++) 
                    {
                    for(x=0; x<xe; x++)
                        {
                        if(ip.getPixel(x,y)<=threshold)
                            ip.putPixel(x,y,b);
                        }
                    }
                }
            imp.getProcessor().setThreshold(nI-1, nI-1, ImageProcessor.NO_LUT_UPDATE);
            imp.setTitle("Mask "+threshold);
            }
            
        return imp;
        }


    private int percentile(int[] data) 
        {
        // W. Doyle, "Operation useful for similarity-invariant pattern recognition,"
        // Journal of the Association for Computing Machinery, vol. 9,pp. 259-267, 1962.
        // ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
        // Original Matlab code Copyright (C) 2004 Antti Niemisto
        // See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
        // and the original Matlab code.
 
        int threshold = -1;
        double ptile = factor;
        double [] avec = new double [data.length];
 
        for (int i=0; i<data.length; i++)
            avec[i]=0.0d;
 
        double total = partialSum(data, data.length-1);
        double temp = 1.0d;
        for (int i=0; i<data.length; i++)
            {
            avec[i]=Math.abs((partialSum(data, i)/total)-ptile);
            if (avec[i]<temp) 
                {
                temp = avec[i];
                threshold = i;
                }
            }
        return threshold;
        }


    private static double partialSum(int[] y, int j) // needed by percentile()
        {
        double x = 0.0d;
        for (int i=0;i<=j;i++)
            x+=y[i];
        return x;
        }


    private int isoData(int[] data) 
        {
        // Also called intermeans
        // Iterative procedure based on the isodata algorithm [T.W. Ridler, S. Calvard, Picture 
        // thresholding using an iterative selection method, IEEE Trans. System, Man and 
        // Cybernetics, SMC-8 (1978) 630-632.] 
        // The procedure divides the image into objects and background by taking an initial threshold,
        // then the averages of the pixels at or below the threshold and pixels above are computed. 
        // The averages of those two values are computed, the threshold is incremented and the 
        // process is repeated until the threshold is larger than the composite average. That is,
        //  threshold = (average background + average objects)/2
        // The code in ImageJ that implements this function is the getAutoThreshold() method in the ImageProcessor class. 
        //
        // L = the average grey value of pixels with intensities < G
        // H = the average grey value of pixels with intensities > G

        int i, l, h, sumLow, sumHigh, threshold = 0;
        
        for (i=1; i<data.length; i++) // search for the first value where the histogram has a frequency > 0
            {
            if (data[i] > 0)
                {
                threshold = i+1;
                break;
                }
            }
            
        while (true)
            {
            l = 0;
            sumLow = 0;
            h = 0;
            sumHigh = 0;

            for (i=0; i<threshold; i++) 
                {
                sumLow += data[i];
                l += (data[i]*i);
                }
                
            for (i=(threshold+1); i<data.length; i++)
                {
                sumHigh += data[i];
                h += (data[i]*i);
                }
                
            if (sumLow>0 && sumHigh>0)
                {
                l /= sumLow;
                h /= sumHigh;               
                if (threshold == (int) Math.round((l + h) / factor)) break;
                }
                
            threshold++;
            if (threshold>(data.length-2))
                {
                IJ.log("IsoData Threshold not found.");
                return -1;
                }
            }
        return threshold;
        }


    private ImageProcessor byteCompare(ImageProcessor imp_in, ImageProcessor imp_mask) // Used by createMask() to do the actual masking. This conversion is adapted from the function "convertShortToByte" of the ImageJ class "TypeConverter".     
        {
        int threshold = 0;
        int width  = imp_in.getWidth(); 
        int height = imp_in.getHeight();
        int size = width*height;
        byte[] pixels_in = (byte[])imp_in.getPixels();
        byte[] pixels_mask = (byte[])imp_mask.getPixels();
        byte[] pixels_out = new byte[size];
        int value;
        for (int i=0; i<size; i++) 
            {
            value = (pixels_mask[i])&0xff;
            if (value>threshold) pixels_out[i] = pixels_in[i];
            else // pixel not part of the mask
                {
                if (ratio) pixels_out[i] = (byte)128; // black in ratio files
                else pixels_out[i] = 0;
                }
            }
        ImageProcessor imp_out = new ByteProcessor(width, height, pixels_out, imp_in.getCurrentColorModel());
        return imp_out;            
        }


    public ImagePlus createMask(ImagePlus[] imp_in) // Mask an image - Input: ImagePlus array containing two/three (input) images. output: ImagePlus containing the result
        {
        ImageStack[] stacks = new ImageStack[3];
        stacks[0] = imp_in[0].getStack();
        stacks[1] = imp_in[3].getStack();
        int width  = imp_in[0].getWidth(); 
        int height = imp_in[0].getHeight();
        int slices = imp_in[0].getStackSize();
        
        ImageProcessor ip_out = null;

        String imp_out_title = "Mask of "+imp_in[0].getTitle();
        ImagePlus imp_out = null;
        stacks[2] = new ImageStack (width, height);
        ImageProcessor[] ip = new ImageProcessor[2];

        // For 8-bit images
        if (imp_in[0].getType() == ImagePlus.GRAY8)
            {
            imp_out = NewImage.createByteImage(imp_out_title, width, height, slices, 1);
            if (imp_out == null)
                {
                IJ.error(title, memoryError);
                return null;
                }
            WindowManager.checkForDuplicateName = true; // Add a number to the title if name already exists  

            // Mask each slice
            for (int i=1; i<=slices; i++)
                {
                ip[0] = stacks[0].getProcessor(i); 
                ip[1] = stacks[1].getProcessor(i);
                ip_out = byteCompare(ip[0], ip[1]); // do the actual masking
                stacks[2].addSlice(null, ip_out);
                }
            imp_out.setStack(null, stacks[2]);
            imp_out.copyScale(imp_in[0]);
            }
        // Unsupported image type
        else
            {
            IJ.error("Image type not supported.");
            return null;
            }             
        return imp_out;
        }


    private ImagePlus[] chooseImages() // Let the user choose the images to work on.
        {
        int[] open_images = WindowManager.getIDList(); // Get the IDs of the open images        
        if (open_images==null)
            {
            IJ.error(title, "No image is open.");
            return null;
            }                                   
        
        // Get image titles            
        ImagePlus img_temp;
        String[] image_titles = new String[open_images.length]; // Array of (open) images
        for (int i=0; i<open_images.length; i++)  // Get the titles of all of the open images
            {
            img_temp = WindowManager.getImage(open_images[i]);
            image_titles[i] = img_temp.getTitle();
            }

        String[] method_titles = {"Percentiles", "IsoData (Iterative intermeans)"};

        if (open_images.length<3)
	        {
            IJ.error(title, "At least three images need to be open.");
        	return null;
	        }
        
        // Create/show the dialog                
        GenericDialog gd = new GenericDialog(title);

        gd.addChoice("Stack 1:", image_titles, image_titles[0]);
        gd.addChoice("Stack 2:", image_titles, image_titles[1]);
        if (open_images.length>3)
            {
            gd.addChoice("Stack 3:", image_titles, image_titles[2]);
            gd.addChoice("Mask:", image_titles, image_titles[3]);
            }
        else
            {
            gd.addChoice("Stack 3:", image_titles, image_titles[2]);
            }
        gd.addCheckbox("Merge stacks and segmentation mask", merge);

        gd.addCheckbox("Threshold", thresh);
		gd.setInsets(0,20,0);
        gd.addNumericField("Thresholding factor:", defaultThreshold, 2);
		gd.setInsets(0,20,0);
        gd.addChoice("Method:", method_titles, method_titles[0]);
        gd.addCheckbox("Calculate thresholds based on substacks", tSlice);
        gd.addCheckbox("Interpolate thresholds", iThresh);
        gd.addCheckbox("Show results table with thresholds", showRes);
        gd.addCheckbox("Manually define the number of substacks", manual);
		gd.setInsets(0,20,0);
        gd.addMessage("If manual:");
		gd.setInsets(0,20,0);
        gd.addNumericField("How many substacks:", 3, 0);
		gd.setInsets(0,20,0);
        gd.addMessage("If automatic:");
		gd.setInsets(0,20,0);
        gd.addNumericField("Min # of slices per substack:", 30, 0);
        gd.addMessage("To calculate the ratio for each slice individually, choose \"manual\" and enter the total number of slices as the number of substacks.");

        if (open_images.length>3)
            {
    		gd.setInsets(20,20,0);
            gd.addCheckbox("Keep non-thresholded mask", mask);
            gd.addCheckbox("Keep Amira (segmentation mask) file", keepAM);
            }

		gd.setInsets(20,20,0);
        gd.addCheckbox("Show parameters in log window", logInfo);
        gd.addMessage("If the images aren't image stacks, new image stacks will be created that contain a second, blank slice. These images can be used for Ratio/Intensity Calculator.");
		gd.setInsets(0,20,0);
        gd.addMessage("In this case, you have to use a mask in Ratio/Intensity Calculator. If you don't really wish to use a mask, use a merge of the two channels as a mask.");

        gd.showDialog();
        if (gd.wasCanceled())  return null;
        
        // Process the input
        merge = false; mask = false; keepAM = false; // important in case open_images.length<2
        
        if (open_images.length>3)
            {
            merge = gd.getNextBoolean();
            }
        else merge = false;
        thresh = gd.getNextBoolean();
        tSlice = gd.getNextBoolean();
        iThresh = gd.getNextBoolean();
        showRes = gd.getNextBoolean();
        manual = gd.getNextBoolean();
        if (open_images.length>3)
            {
            mask = gd.getNextBoolean();
            keepAM = gd.getNextBoolean();
            }
        logInfo = gd.getNextBoolean();

        factor = (double)gd.getNextNumber();
        nBins = (int)gd.getNextNumber();
        cutOff = (int)gd.getNextNumber();

        // Load the images
        ImagePlus[] img_out = new ImagePlus[4];
        img_out[0] = WindowManager.getImage(open_images[gd.getNextChoiceIndex()]);
        img_out[1] = WindowManager.getImage(open_images[gd.getNextChoiceIndex()]);
        img_out[2] = WindowManager.getImage(open_images[gd.getNextChoiceIndex()]);
        if (merge)
            {
            img_out[3] = WindowManager.getImage(open_images[gd.getNextChoiceIndex()]);
            if (img_out[0].getWidth() != img_out[3].getWidth() || img_out[0].getHeight() != img_out[3].getHeight() || img_out[0].getStackSize() != img_out[3].getStackSize())
                {         
                IJ.error(title, "The stacks are not compatible.");
                return null;
                }
            }
        if (img_out[0].getWidth() != img_out[1].getWidth() || img_out[0].getHeight() != img_out[1].getHeight() || img_out[0].getStackSize() != img_out[1].getStackSize())
        {         
        IJ.error(title, "The stacks are not compatible.");
        return null;
        }
        if (img_out[0].getWidth() != img_out[2].getWidth() || img_out[0].getHeight() != img_out[2].getHeight() || img_out[0].getStackSize() != img_out[2].getStackSize())
	        {         
	        IJ.error(title, "The stacks are not compatible.");
	        return null;
	        }

        if (gd.getNextChoiceIndex()==0) perc = true;

        if (logInfo)
        	{
        	IJ.log("Stack 1: "+img_out[0].getTitle());
        	IJ.log("Stack 2: "+img_out[1].getTitle());
        	IJ.log("Stack 3: "+img_out[2].getTitle());
        	if (merge) IJ.log("Mask: "+img_out[3].getTitle());
        	if (thresh) 
        		{
        		if (perc) IJ.log("Percentile thresholding, factor "+IJ.d2s(factor,2));
        		else IJ.log("IsoData thresholding, factor "+IJ.d2s(factor,2));
            	if (tSlice) IJ.log("Substacks used");
            	if (iThresh) IJ.log("Interpolation of thresholds");
            	if (manual) IJ.log("Manual substacks: "+IJ.d2s(nBins,0));
            	else IJ.log("Automatic substacks, minimum: "+IJ.d2s(cutOff,0));
        		}
        	}
        
        return img_out;
        }
    } 
