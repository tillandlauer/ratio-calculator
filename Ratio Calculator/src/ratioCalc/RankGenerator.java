package ratioCalc;
import ij.IJ;
import ij.measure.ResultsTable;

public class RankGenerator 
	{
	private static boolean useUniRatio = false;
	private static boolean logWindow = false;

    public RankGenerator() // generates the rank matrix
    	{        
    	useUniRatio = false;
    	logWindow = false;
    	}

    public RankGenerator(boolean uniformRatio) // generates the rank matrix
		{        
    	useUniRatio = uniformRatio;
    	logWindow = false;
		}
    
    
	/**
     * Rank generator.
     *
     * @return the double[][][]
     */
    public static double[][][] generate() // generates the rank matrix
        {        
        IJ.showStatus("Calculating the ranks...");
        double[][][] matrix = createMatrix();
        return matrix;
        }
    
    
    public static void show() // generates the rank matrix
	    {        
    	double[][][] matrix = createMatrix();
	    ResultsTable rt = ResultsTable.getResultsTable();        
	    rt.reset();
	    rt.setPrecision(9);
	    for (int x=0;x<matrix.length;x++)
	    	{
	    	for (int y=0;y<matrix[0].length;y++)
		    	{
		    	rt.incrementCounter();
		    	rt.addValue("X",  x);
		    	rt.addValue("Y",  y);
		    	rt.addValue("original rank",  matrix[x][y][0]);
		    	rt.addValue("final rank",  matrix[x][y][1]);
		    	rt.addValue("frequency",  matrix[x][y][2]);
		    	rt.addValue("ratio",  matrix[x][y][3]);
		    	}
	    	}
	    rt.show("Results");
	    }   
    
    
    /**
     * Creates the matrix.
     *
     * @return the double[][][]
     */
    private static double[][][] createMatrix() // really generates the rank matrix
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
     private static double[][] sortMatrix(double[][] matrix, int ratioID, int finalRankID, int counterID, int totalID) // assign the same rank to equal ratio values // RANK HAS TO BE IN THE LAST POSITION, OTHERWISE IT DOESN'T WORK
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
    public static double[][] quicksort(double[][] a, int compareID, boolean skip) // sort [column][row] after [compareID][row]; if skip=true, skip the last column
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
    private static double[][] quicksort (double[][] a, int lo, int hi, int compareID, boolean skip) // modified from: http://www.inf.fh-flensburg.de/lang/algorithmen/sortieren/quick/quicken.htm
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


    public static boolean getLog() 
		{
		return logWindow;
		}
	
	}
