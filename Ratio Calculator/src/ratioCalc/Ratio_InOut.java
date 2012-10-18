package ratioCalc;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;

import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;

public class Ratio_InOut
    {   
//    private String saveError = "Couldn't save file.";
    private boolean chName = false; // for normalized statistics: use refFiles as input files
    // All of the following options can be set in chooseImages():
    private int nFiles = 0; // number of files to be opened
    private String directory = ""; // directory for saving files manually
    private String[] refFiles = new String[3]; // files for the normalized statistics (*currently disabled*)

    public Ratio_InOut(int files, String dir)
    	{
    	nFiles = files;
    	directory = dir;
    	}

    public Ratio_InOut(int files, String dir, boolean change, String[] references)
		{
		nFiles = files;
    	directory = dir;
		chName = change;
		refFiles = references;
		}    

    protected int[][] loadHistoFilesInt(int ch) // read the data from histogram files
	    { 
	    Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16); // content of each file 
	    String inputname = "";
	    int n = 0;
	    String part = "";
	
	    inputname = "01 Histogram "+ch+".xls";
	    list = readFile(inputname); // test whether files can be read           
	    if (list==null) return null;
	    if (list.size()==0) return null; 
	    n = list.size()-1; // determine file size (number of bins)
	    int[][] data = new int[n][nFiles]; // final data (returned)
	
	    for (int k=1;k<=nFiles;k++) // cycle through all files
	        {
	        if (k<10) inputname = "0"+k+" Histogram "+ch+".xls";
	        else inputname = k+" Histogram "+ch+".xls";          
	        list = readFile(inputname); // read data           
	        if (list.size()==0) return null; 
	        n = list.size()-1; 
	        StringTokenizer st;
	
	        try
	            {
	            st = (StringTokenizer)list.elementAt(1); // test whether there is any data in the file 
	            }
	        catch (ArrayIndexOutOfBoundsException e)
	            {
	            IJ.error("File \""+directory+inputname+"\" is too short.");
	            return null; 
	            }
	            
	        for (int i = 0; i < n; i++) // cycle through all bins
	            { 
	            st = (StringTokenizer)list.elementAt(i+1); 
	            part = st.nextToken(); // skip the index (line numbers)
	            part = st.nextToken(); 
	            try 
	                { 
	                data[i][k-1] = Integer.valueOf(part).intValue(); // read data
	                } 
	            catch (NumberFormatException e) 
	                { 
	                try 
	                    { 
	                    data[i][k-1] = (int)Math.round(Double.valueOf(part).doubleValue()); // read data
	                    } 
	                catch (NumberFormatException e2) 
	                    { 
	                    IJ.error("Unknown file format: \""+directory+inputname+"\"");
	                    return null; 
	                    } 
	                }
	            } // bin read 
	        } // all files read
	
	    return data; 
	    } // end of loadHistoFilesInt()
    
    
    protected int[][] loadHistoFiles() // read the data from histogram files
        { 
        Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16); // content of each file 
        String inputname = "";
        int n = 0;
        String part = "";

        inputname = "01 Histogram.xls";
        list = readFile(inputname); // test whether files can be read           
        if (list==null) return null;
        if (list.size()==0) return null; 
        n = list.size()-1; // determine file size (number of bins)
        int[][] data = new int[n][nFiles]; // final data (returned)

        for (int k=1;k<=nFiles;k++) // cycle through all files
            {
            if (k<10) inputname = "0"+k+" Histogram.xls";
            else inputname = k+" Histogram.xls";          
            list = readFile(inputname); // read data           
            if (list.size()==0) return null; 
            n = list.size()-1; 
            StringTokenizer st;
    
            try
                {
                st = (StringTokenizer)list.elementAt(1); // test whether there is any data in the file 
                }
            catch (ArrayIndexOutOfBoundsException e)
                {
                IJ.error("File \""+directory+inputname+"\" is too short.");
                return null; 
                }
                
            for (int i = 0; i < n; i++) // cycle through all bins
                { 
                st = (StringTokenizer)list.elementAt(i+1); 
                part = st.nextToken(); // skip the index (line numbers)
                part = st.nextToken(); 
                try 
                    { 
                    data[i][k-1] = Integer.valueOf(part).intValue(); // read data
                    } 
                catch (NumberFormatException e) 
	                { 
	                try 
	                    { 
	                    data[i][k-1] = (int)Math.round(Double.valueOf(part).doubleValue()); // read data
	                    } 
	                catch (NumberFormatException e2) 
	                    { 
	                    IJ.error("Unknown file format: \""+directory+inputname+"\"");
	                    return null; 
	                    } 
	                }
                } // bin read 
            } // all files read

        return data; 
        } // end of loadHistoFiles()


    private Vector<StringTokenizer> readFile(String inputname) // loads a file for loadHistoFiles() and loadStatFiles()
        {    
        Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16); 
        String line = ""; 

        try 
            { 
            FileReader fr = new FileReader(directory+inputname); 
            BufferedReader br = new BufferedReader(fr); 

            do 
                { 
                line = br.readLine(); 
                if (line != null)
                    { 
                    StringTokenizer st = new StringTokenizer(line); 
                    if(st.hasMoreTokens()) list.addElement(st); 
                    } 
                } 
            while (line != null); 

            fr.close(); 
            } 
        catch (FileNotFoundException e) 
            {
            IJ.error("File \""+directory+inputname+"\" not found."); 
            return null; 
            }
        catch (IOException e) 
            {
            IJ.error("Couldn't read from file \""+directory+inputname+"\"."); 
            return null; 
            } 
        return list;
        } // end of readFile()

    
    protected double[][] loadStatFilesInt(int ch) // extracts the specified line
        { 
        Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16); // content of each file
        String inputname = "";
        int nd = 6;
        String part = "";
        double[][] data = new double[nd][nFiles]; // final data (returned)
        int c = 0; // counter for the position within data[][]
        StringTokenizer st;
    	ch += ch-1; // go to the correct line for the channel
       
        for (int k=1;k<=nFiles;k++) // cycle through files
            {
            if (k<10) inputname = "0"+k+" Statistics.xls";
            else inputname = k+" Statistics.xls";          
            list = readFile(inputname); // read the file
            if (list==null) return null;           
            if (list.size()==0) return null; 
    
            try
                {
                st = (StringTokenizer)list.elementAt(1); // test whether there is any data in the file
                nd = st.countTokens();                
                }
            catch (ArrayIndexOutOfBoundsException e)
                {
                IJ.error("File \""+directory+inputname+"\" is too short.");
                return null; 
                }
                
            st = (StringTokenizer)list.elementAt(ch); 
            part = st.nextToken(); // skip the index (line numbers)
            for (int j = 0; j < nd-1; j++) // cycle through the columns
                { 
                part = st.nextToken(); // get the first column to read
                try 
                    { 
                    data[c][k-1] = Double.valueOf(part).doubleValue(); 
                    } 
                catch (NumberFormatException e) 
                    { 
                    IJ.error("Unknown file format: \""+directory+inputname+"\"");
                    return null; 
                    } 
                c++; // necessary for the position within data[][]
                } // file finished
            c=0;
            } // all files read

        return data; 
        } // end of loadStatFiles()


    protected double[][] loadStatFiles(int lines, int columns, boolean inverse, String addName, boolean extractMed) // extract data from statistics files
    // read LINES line per file, COLUMNS columns per file, INVERSE=false: output data = [data][files], ADDNAME: add a string to the input name, EXTRACTMED: get just the median and not all data (currently not used)
        { 
        Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16); // content of each file
        String inputname = "";
        int n = 0;
        int nd = columns;
        String part = "";
        double[][] data = new double[nFiles][nd*lines]; // final data (returned)     
        if (!inverse) data = new double[nd*lines][nFiles];
        int c = 0; // counter for the position within data[][]
        StringTokenizer st;
        
        for (int k=1;k<=nFiles;k++) // cycle through files
            {
            if (chName) inputname = refFiles[0]; // only needed for normalized statistics. Used when reading data from the reference file
            else if (k<10) inputname = "0"+k+" Statistics"+addName+".xls";
            else inputname = k+" Statistics"+addName+".xls";          
            list = readFile(inputname); // read the file
            if (list==null) return null;           
            if (list.size()==0) return null; 
            n = list.size()-1; // lines in the file
            if (extractMed && (k==1)) // read only the median or first file
                {
                lines = n;
                data = new double[nFiles][nd*lines]; // re-initialize data[][]     
                if (!inverse) data = new double[nd*lines][nFiles];
                }
    
            try
                {
                st = (StringTokenizer)list.elementAt(1); // test whether there is any data in the file
                nd = st.countTokens();                
                }
            catch (ArrayIndexOutOfBoundsException e)
                {
                IJ.error("File \""+directory+inputname+"\" is too short.");
                return null; 
                }
                
            for (int i = 0; i < lines; i++) // read all the lines
                { 
                st = (StringTokenizer)list.elementAt(i+1); 
                part = st.nextToken(); // skip the index (line numbers)
                if (extractMed) // skip MIN and Q1
                    {
                    part = st.nextToken(); 
                    part = st.nextToken(); 
                    nd = 2;
                    }
                for (int j = 0; j < nd-1; j++) // cycle through the columns
                    { 
                    part = st.nextToken(); // get the first column to read
                    try 
                        { 
                        if (inverse) data[k-1][c] = Double.valueOf(part).doubleValue(); 
                        else data[c][k-1] = Double.valueOf(part).doubleValue(); 
                        } 
                    catch (NumberFormatException e) 
                        { 
                        IJ.error("Unknown file format: \""+directory+inputname+"\"");
                        return null; 
                        } 
                    c++; // necessary for the position within data[][]
                    } // line finished 
                } // file finished
            c=0;
            } // all data read

        return data; 
        } // end of loadStatFiles()


    protected ArrayList<ArrayList<Double>> loadStatList(String addName) // read data for statTest() (comparative statistics). Based on loadStatFiles() but returns an ArrayList instead of an array. Reads only the median.
        { 
        Vector<StringTokenizer> list = new Vector<StringTokenizer>(0, 16); 
        String inputname = "";
        int n = 0, lines = 0, nd = 2;
        String part = "";

        ArrayList<ArrayList<Double>> data = new ArrayList<ArrayList<Double>>(nFiles); // final data (returned)
        ArrayList<Double> dList = new ArrayList<Double>(); // data from one file  
        StringTokenizer st;
        
        for (int k=1;k<=nFiles;k++) // cycle through files
            {
            if (k<10) inputname = "0"+k+" Statistics"+addName+".xls";
            else inputname = k+" Statistics"+addName+".xls";          
            list = readFile(inputname);            
            if (list==null) return null;           
            if (list.size()==0) return null; 
            n = list.size()-1; 
            lines = n;
            dList = new ArrayList<Double>(nd*lines); // contains the data from one file
    
            try
                {
                st = (StringTokenizer)list.elementAt(1); // test whether there is any data in the file 
                nd = st.countTokens();                
                }
            catch (ArrayIndexOutOfBoundsException e)
                {
                IJ.error("File \""+directory+inputname+"\" is too short.");
                return null; 
                }
                
            for (int i = 0; i < lines; i++) // read all lines
                { 
                st = (StringTokenizer)list.elementAt(i+1); 
                part = st.nextToken(); // read only the median; skip index, MIN and Q1
                part = st.nextToken(); 
                part = st.nextToken(); 
                nd = 2;
                for (int j = 0; j < nd-1; j++) // cycle through columns
                    { 
                    part = st.nextToken(); // get the data to read
                    try 
                        { 
                        dList.add(Double.valueOf(part).doubleValue());
                        } 
                    catch (NumberFormatException e) 
                        { 
                        IJ.error("Unknown file format: \""+directory+inputname+"\"");
                        return null; 
                        } 
                    } 
                } 
            data.add(dList); // add file to list
            }

        return data; 
        } // end of loadStatList()
    


    /**
     * Save a ResultsTable
     *
     * @param rt ResultsTable
     * @param tableTitle Filename
     */
    protected void saveTable(ResultsTable rt, String tableTitle) // Save a results table
        {
    	String saveError = "File "+tableTitle+" couldn't be saved.";
        try 
            { 
//            SaveDialog sd = new SaveDialog("Save "+tableTitle, tableTitle, ".xls");
            rt.saveAs(directory+tableTitle+".xls");
            } 
        catch (IOException e) 
            {
            IJ.error(saveError); 
            } 
        ij.text.TextWindow tw = ResultsTable.getResultsWindow(); 
        tw.close(false);
        }


    /**
     * Spectrum lut.
     *
     * @param img the img
     * @return the image plus
     */
    public static ImagePlus spectrumLUT(ImagePlus img, int maxPossibleValue, boolean displayLUT) // change the lookup table to ratio coding
        {
		byte[][] rgb = new byte[3][256]; // array of the rgb values for the LUT
		int r = 0;
		int g = 255;
        int b = 255;
        int i = 0;
        
		for (i=0; i<42; i++) // cyan to blue
            {
            rgb[0][i] = (byte)r;
            rgb[1][i] = (byte)g;
            rgb[2][i] = (byte)b;
            g-=6;
            }
        g=0;

		for (i=42; i<85; i++) // blue to magenta
            {
            rgb[0][i] = (byte)r;
            rgb[1][i] = (byte)g;
            rgb[2][i] = (byte)b;
            r+=6;
            }
        r=255;

		for (i=85; i<128; i++) // magenta to black
            {
            rgb[0][i] = (byte)r;
            rgb[1][i] = (byte)g;
            rgb[2][i] = (byte)b;
            r-=6;
            b-=6;
            }
        r=g=b=0;

		for (i=128; i<171; i++) // black to green
            {
            rgb[0][i] = (byte)r;
            rgb[1][i] = (byte)g;
            rgb[2][i] = (byte)b;
            g+=6;
            }
        g=255;

		for (i=171; i<213; i++) // green to yellow
            {
            rgb[0][i] = (byte)r;
            rgb[1][i] = (byte)g;
            rgb[2][i] = (byte)b;
            r+=6;
            }
        r=255;

		for (i=213; i<256; i++) // yellow to red
            {
            rgb[0][i] = (byte)r;
            rgb[1][i] = (byte)g;
            rgb[2][i] = (byte)b;
            g-=6;
            }
        
        if (displayLUT) // show the LUT as an extra image
            {
            int height = 10; // height of image
            String imp_out_title = "LUT";
            ImagePlus imp_out = NewImage.createRGBImage(imp_out_title, rgb[0].length, height, 1, 1);
            WindowManager.checkForDuplicateName = true; // add a number to the title if name already exists  

            ImageProcessor ip_out = imp_out.getProcessor();
            int bins = rgb[0].length; // number of colours
            int binWidth = 1; // width for each colour
            int picPos = 0; // counter within image
            int values[] = new int[3]; // red, green, blue
    
            for(i=0; i<bins; i++)
                {
                for(int j=0; j<height; j++)
                    {
                    for (int k=0; k<binWidth; k++)
                        {
                        values[0] = rgb[0][i]&0xff;
                        values[1] = rgb[1][i]&0xff;
                        values[2] = rgb[2][i]&0xff;
                        ip_out.putPixel(picPos+k,j,values);
                        }
                    }
                picPos = picPos + binWidth; // move to next colour
                }
            imp_out.show();
            }

		IndexColorModel icm = new IndexColorModel(8, 256, rgb[0], rgb[1], rgb[2]); // create the LUT
		img.getProcessor().setColorModel(icm); // apply the LUT
        img.getProcessor().setMinAndMax(0,maxPossibleValue); // scale the LUT

        return img; 
        }
    
    } 