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
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.StackConverter;

/**
 * Calculates ratios between all pixels of two image stacks.
 * @author Till Andlauer (till@andlauer.net)
 * @version 1.30a - 12-09-26
 */
public class Identity_Calculator implements PlugIn
    {
	/** Title for dialogs and error messages. */
    private String title = "Ratio Calculator v1.30a"; // title for dialogs and error messages
	/** Error message when out of memory */
    private String memoryError = "Out of memory...";
	/** Threshold for the mask. Should be kept at <code>0</code> (-> all values above <code>0</code> are part of the mask).*/
    private int threshold = 0; // Threshold for the mask. Should be kept at 0 (-> all values above 0 are part of the mask).
	/** Indicates whether data was added to the log window. */
    private boolean logWindow = false; // data was added to the log window
    // All of the following options can be set in chooseImages():
    /** Prefix added in front of image and table names; set in <code>chooseImages</code> */
    private String prefix = ""; // added in front of image and table names
    /** Keep source files open; set in <code>chooseImages</code> 
    * <br>If <code>false</code> ImageJ might occasionally crash
    */
    private boolean keepFiles = true; // Keep source files open
    /** Save all generated files; set in <code>chooseImages</code>
    * @see saveDir
    */
    private boolean saveFiles = false; // Save all generated files
    /** Directory for saved files
    * @see saveFiles
    */
    private String saveDir = ""; // Directory for saved files
    /** Show parameters in the log window; set in <code>chooseImages</code> */
    private boolean logInfo = true; // Show parameters in the log window
    private int[] ratioThresh = new int[2];
    
	/**
	 * Runs the analysis
	 */
    public void run(String arg) 
        {  
        String parameters = Macro.getOptions();
        if (parameters!=null) prefix = parameters;
    
        ratioThresh[0] = 23123;
        ratioThresh[1] = 15857;       
        ImagePlus[] img = chooseImages(); // choose images and set variables.
        if (img == null) return;
        
        if (logInfo) showInfo(img); // Show the parameters
        
        double start_time = System.currentTimeMillis();
        IJ.showStatus("Beginning calculation...");
        ImagePlus img_ratio = calcRatio(img); // ratio calculation
        if (img_ratio == null) return;

        StackConverter ratioConv = new StackConverter(img_ratio);
        ratioConv.convertToGray8();           
        img_ratio.show();

        if (!keepFiles)
            {
            img[0].close();
            img[1].close();
            }

        if (saveFiles)
            {
            FileSaver fs = new FileSaver(img_ratio);
            fs.saveAsTiffStack(saveDir+img_ratio.getTitle()+".tif");
            img_ratio.close();
            }

        IJ.showStatus("The calculation took "+IJ.d2s(((double)System.currentTimeMillis()-start_time)/1000.0d, 2)+" seconds.");
        if (saveFiles) IJ.log("Files saved to: "+saveDir);
        if (!logWindow) logWindow = RankGenerator.getLog();
        if (logWindow) IJ.log(""); // if the log window is open, add a blank line
        IJ.freeMemory();
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
        ImageStack[] stacks = new ImageStack[5];
        stacks[0] = imp_in[0].getStack();
        stacks[1] = imp_in[1].getStack();
        stacks[2] = imp_in[2].getStack();
        stacks[3] = imp_in[3].getStack();
        int width  = imp_in[0].getWidth(); 
        int height = imp_in[0].getHeight();
        int slices = imp_in[0].getStackSize();
        int size = width*height;

        FloatProcessor[] fp = new FloatProcessor[4]; // for historical reasons, the input image data is handled has float.                 
        float[][] pixel = new float[4][size]; // image 1, image 2, mask
        int x = 0, y = 0; // position within image
        float[] value = new float[4]; // pixel values       

        // variables for processing the output (image)
        ImagePlus imp_out = null;
        ImageProcessor ip_out = null;
        String imp_out_title = prefix+"Ratio";
        imp_out = NewImage.createShortImage(imp_out_title, width, height, slices, 1); // create a new image with the same dimensions as the first chosen image
        if (imp_out == null) // produce an error if this fails
            {
            IJ.error(title, memoryError);
            return null;
            }
        WindowManager.checkForDuplicateName = true; // add a number to the title if name already exists  
        stacks[4] = imp_out.getStack();

        // variables for calculating the ratio
        int[] ratio = new int[2];

        //-- start actually doing stuff --//
        // create the rank matrix
        double[][][] ranks = RankGenerator.generate(); // 0 = original rank, 1 = final rank, 2 = counter, 3 = ratio                  

        // calculate the ratio
        IJ.showStatus("Calculating ratio...");
        for (int i=1; i<=slices; i++)
            {                
            ip_out = stacks[4].getProcessor(i);
            fp[0] = stacks[0].getProcessor(i).toFloat(0, fp[0]);
            fp[1] = stacks[1].getProcessor(i).toFloat(0, fp[1]);
            fp[2] = stacks[2].getProcessor(i).toFloat(0, fp[2]);
            fp[3] = stacks[3].getProcessor(i).toFloat(0, fp[3]);
            pixel[0] = (float[])fp[0].getPixels(); 
            pixel[1] = (float[])fp[1].getPixels(); 
            pixel[2] = (float[])fp[2].getPixels(); 
            pixel[3] = (float[])fp[3].getPixels(); 
            
            for (int j=0; j<size; j++) 
                {
                value[0] = pixel[0][j]; // get the current pixel value of image 1
                value[1] = pixel[1][j]; // get the current pixel value of image 2                        
                value[2] = pixel[2][j]; // get the current pixel value of image 3                        
                value[3] = pixel[3][j]; // get the current pixel value of the mask                        
                ratio[0] = (int)ranks[Math.round(value[1])][Math.round(value[0])][1]; // get the uniformly distributed ratio of these two pixel values (=rank)                     
                ratio[1] = (int)ranks[Math.round(value[2])][Math.round(value[0])][1]; // get the uniformly distributed ratio of these two pixel values (=rank)                     

                if (value[3]>threshold)
                	{
	                y = (j / height);
	                x = (j % height);
	                if ((ratio[0]>ratioThresh[0]) && (ratio[1]<ratioThresh[1]))
	                    {
	                    ip_out.putPixelValue(x, y, 65535);
	                    }
	                else
	                    {
	                    ip_out.putPixelValue(x, y, 0);
	                    }
                	}
                }
            } // calculation of ratios is finished

        // process and display the results
        imp_out.copyScale(imp_in[0]);

        return imp_out;
        } // end of calcRatio


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
        if (open_images.length<4)
            {
            IJ.error(title, "At least four images need to be open.");
            return null;
            }                                    
        ImagePlus img_temp;
        String[] image_titles = new String[open_images.length];
        for (int i=0; i<open_images.length; i++)  // get the titles of all of the open images
            {
            img_temp = WindowManager.getImage(open_images[i]);
            image_titles[i] = img_temp.getTitle();
            }

		int cgRows3=1, cgColumns3=2;
		String[] cgLabels3 = 
		    {
            "Keep input files open",
            "Save (and close) new files",
		    };
		boolean[] cgStates3 = {keepFiles,saveFiles};

        // create/show the dialog                
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Stack 1:", image_titles, image_titles[0]);
        gd.addChoice("Stack 2:", image_titles, image_titles[1]);
        gd.addChoice("Stack 3:", image_titles, image_titles[2]);
        gd.addChoice("Mask:", image_titles, image_titles[3]);
		gd.setInsets(20,20,0);
        gd.addMessage("Ratio 1: Stack 2 vs Stack 1; Ratio 2: Stack 3 vs Stack 1");
        gd.addNumericField("Threshold for ratio 1:", ratioThresh[0], 0, 9, ""); // title, default value, digits, length, units
        gd.addNumericField("Threshold for ratio 2:", ratioThresh[1], 0, 9, ""); // title, default value, digits, length, units
        gd.addMessage("-> ratios have to be \'>= threshold 1\' and \'<= threshold 2\'");

		gd.setInsets(20,20,0);
		gd.addCheckboxGroup(cgRows3, cgColumns3, cgLabels3, cgStates3);
        gd.addStringField("Prefix to filenames:", prefix, 10);

		gd.setInsets(20,20,0);
        gd.addCheckbox("Show parameters in log window", logInfo);

        gd.showDialog();
        if (gd.wasCanceled()) return null;
        
        // process the input
        int[] choice_index = new int[4];      
        choice_index[0] = gd.getNextChoiceIndex(); // images
        choice_index[1] = gd.getNextChoiceIndex();
        choice_index[2] = gd.getNextChoiceIndex();
        choice_index[3] = gd.getNextChoiceIndex();

        keepFiles = gd.getNextBoolean();
        saveFiles = gd.getNextBoolean();
        logInfo = gd.getNextBoolean();

        ratioThresh[0] = (int)gd.getNextNumber();
        ratioThresh[1] = (int)gd.getNextNumber();
        prefix = gd.getNextString();

        ImagePlus[] img_out = new ImagePlus[4];        
        img_out[0] = WindowManager.getImage(open_images[choice_index[0]]);
        img_out[1] = WindowManager.getImage(open_images[choice_index[1]]);
        img_out[2] = WindowManager.getImage(open_images[choice_index[2]]);
        img_out[3] = WindowManager.getImage(open_images[choice_index[3]]);

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
    	IJ.log("Stack 3: "+img_out[2].getTitle());
    	IJ.log("Mask: "+img_out[3].getTitle());
    	IJ.log("Threshold 1: "+IJ.d2s(ratioThresh[0],0));
    	IJ.log("Threshold 2: "+IJ.d2s(ratioThresh[1],0));
	    }
    
    }