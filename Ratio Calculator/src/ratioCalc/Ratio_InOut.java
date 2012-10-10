package ratioCalc;
import ij.IJ;

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
    } 