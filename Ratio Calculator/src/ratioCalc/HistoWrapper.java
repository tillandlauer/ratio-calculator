package ratioCalc;
import ij.ImagePlus;
import ij.measure.ResultsTable;

public class HistoWrapper 
	{
	protected ImagePlus img;
	protected ResultsTable rt;
	
	public HistoWrapper()
		{
		img = new ImagePlus();
		rt = new ResultsTable();
		}
	
	public ImagePlus getImage()
		{
		return img;
		}

	public ResultsTable getTable()
		{
		return rt;
		}

	public void setImage(ImagePlus img_in)
		{
		this.img = img_in;
		return;
		}
	
	public void setTable(ResultsTable rt_in)
		{
		this.rt = rt_in;
		return;
		}
	}
