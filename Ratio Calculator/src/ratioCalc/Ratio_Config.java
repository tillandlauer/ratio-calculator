package ratioCalc;

import ij.text.TextPanel;
import ij.text.TextWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ratio_Config
    {   
	private HashMap<String, String> config;
	public boolean error=false;
	private ArrayList<String> errorKeys;
	public String directory;
	
    public Ratio_Config()
    	{
    	errorKeys = new ArrayList<String>();
    	directory = ij.Prefs.getHomeDir()+ij.Prefs.getFileSeparator()+"plugins"+ij.Prefs.getFileSeparator();
		config = Ratio_InOut.loadConfigFile(directory+"ratio_calculator.txt");
		if (config==null) error=true;
    	} 


    public boolean getError()
		{
    	return error;
		}
    

    public void setError()
		{
    	error=false;
		}


    public ArrayList<String> setErrorKeys()
		{
    	return errorKeys;
		}
    
    
    public String getValue(String value)
    	{
    	String result="";
    	if (config.containsKey(value)) result=config.get(value);
    	else 
    		{
    		error=true;
    		errorKeys.add(value);
    		}
    	return result;
    	}

    
    public int getInt(String value)
		{
		int result=0;
		if (config.containsKey(value))
			{
			try 
	        	{ 
				result = Integer.valueOf(config.get(value)).intValue();
	        	} 
		    catch (NumberFormatException e) 
		        { 
		        try 
		            { 
		            result = (int)Math.round(Double.valueOf(config.get(value)).doubleValue());
		            } 
		        catch (NumberFormatException e2) 
		            { 
		        	return result/0;
		            } 
		        }
			}
    	else 
			{
			error=true;
			errorKeys.add(value);
			}
		return result;
		}

    
    public double getDouble(String value)
		{
		double result = 0.0d;
		if (config.containsKey(value))
		{	
	        try 
	        	{ 
	        	result = Double.valueOf(config.get(value)).doubleValue();
	        	} 
	        catch (NumberFormatException e) 
	        	{ 
	        	return result/0;
	        	} 
			}
	    else 
			{
			error=true;
			errorKeys.add(value);
			}
		return result;
		}

    
    public boolean getBoolean(String value)
		{
		boolean result=false;
		if (config.containsKey(value)) 
			{
			int temp = getInt(value);
			if (temp==1) result=true;
			else result=false;
			}
    	else 
			{
			error=true;
			errorKeys.add(value);
			}
		return result;
		}
    
    
    public void showConfig()
		{
    	TextWindow tw = new TextWindow("Configuration", "", 500, 500);
    	TextPanel tp = tw.getTextPanel();
    	List<String> keys = new ArrayList<String>(config.keySet());
    	tp.append(directory+"ratio_calculator.txt");
    	tp.append("");
    	
    	for (int i=0; i<config.size(); i++)
    		{
        	tp.append(keys.get(i)+": "+config.get(keys.get(i)));
    		}
		}
    } 