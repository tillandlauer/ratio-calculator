package ratioCalc;
import ij.IJ;
import ij.plugin.PlugIn;


public class About implements PlugIn
    {
    public void run(String arg) 
        { 
    	IJ.showMessage("Ratio Calculator", "More info at: http://ratios.andlauer.net");
        }
    } 
