package ratioCalc;
import ij.IJ;
import ij.plugin.PlugIn;


public class Show_Config implements PlugIn
    {
    public void run(String arg) 
        { 
    	Ratio_Config rc;
		rc = new Ratio_Config();
		if (rc.error) IJ.error("No configuration file \"ratio_calculator.txt\" found at "+rc.directory+".");
		else rc.showConfig();
        }
    } 
