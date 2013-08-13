package ratioCalc;
import ij.text.TextPanel;


public class Gnu_Scripts
    {   
    private boolean sd = true; // calculate/show standard errors
    private String plottitle = "";
    private String axislabel = "";
    private String terminal = ""; // output of data (aqua/x11/windows/wxt)
    private boolean screen = true; // show results on screen
    private boolean svg = true; // create .svg file
    private boolean png = true; // create .png file
    private int nFiles = 0; // number of files to be opened
    private String[] xTitle; // x-labels for the different files (statistics)

    public Gnu_Scripts(String imageTitle, String xLabel, String term, boolean showErrors, boolean showScreen, boolean createSvg, boolean createPng) 
        {   
    	plottitle = imageTitle;
    	axislabel = xLabel;
    	terminal = term;
    	sd = showErrors;
    	screen = showScreen;
    	svg = createSvg;
    	png = createPng;
        }

    
    public Gnu_Scripts(String imageTitle, String xLabel, String term, boolean showErrors, boolean showScreen, boolean createSvg, boolean createPng, int files, String[] names) 
        {   
    	plottitle = imageTitle;
    	axislabel = xLabel;
    	terminal = term;
    	sd = showErrors;
    	screen = showScreen;
    	svg = createSvg;
    	png = createPng;
    	nFiles = files;
    	xTitle = names;
        }

    
    private TextPanel initFile() 
        {
        TextPanel tp = new TextPanel();
        tp.append("#!/usr/local/bin/gnuplot -persist");
        tp.append("#");
        tp.append("reset");
        tp.append("set datafile missing \"-\"");
        tp.append("set key enhanced");
        return tp;
        }


    private void plotLine(TextPanel tp, int ch) // generates the plot command for the histogram GnuPlot script
    {
    if (nFiles==1) tp.append("plot \'01 Histogram "+ch+".xls\' using 1:2 notitle with boxes lc rgb \"#164191\"");
    else
        {
        String tempS1 = "";
        String tempS2 = "$2";
        int c=2;
        for (int i=1;i<=nFiles;i++)
            {
            if (i<10) tempS1 = tempS1+" \'0"+i+" Histogram "+ch+".xls\'";
            else tempS1 = tempS1+" \'"+i+" Histogram "+ch+".xls\'";
            if (i>1) tempS2 = tempS2+"+$"+c;
            c=c+2;
            }
        tp.append("plot \"< paste"+tempS1+"\" using 1:(("+tempS2+")/"+((c-2)/2)+") notitle with boxes lc rgb \"#164191\"");
        }
    }

    
    protected TextPanel histoScriptInt(int ch, String imagename)
        {
    	TextPanel tp = initFile();
        if (plottitle!="") tp.append("set title \""+plottitle+"\"");
        if (axislabel!="") tp.append("set xlabel \""+axislabel+"\" offset +0,1 tc rgb \"#062356\"");
        tp.append("set ylabel \"frequency\" offset +2,0 tc rgb \"#062356\"");
        tp.append("set xrange [1:128]");
        tp.append("set yrange [0:620000]");
        tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
        tp.append("set border 3 linestyle 10");
        tp.append("set xtics 4 tc rgb \"#113577\" nomirror rotate by -45");
        tp.append("set ytics 20000 tc rgb \"#113577\" nomirror");
        tp.append("set tics front out");
        tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
        tp.append("set style fill solid 0.9 border");
        
        if (sd) // calculate standard errors
	        {
            tp.append("set style data histogram");
            tp.append("set bars small back");
	        if (screen) // show output
	            {
	            tp.append("set term "+terminal+" title \"Intensities\"");
	            tp.append("set multiplot");
	            tp.append("set grid back xtics ytics linestyle 11");
	            tp.append("set style histogram errorbars gap 0 lw 2");
	            tp.append("plot \'00 Histogram Summary "+ch+".xls\' using 2:3 notitle lc rgb \"#808080\"");
	            tp.append("unset grid");
	            tp.append("set style histogram clustered gap 0");
	            tp.append("plot \'00 Histogram Summary "+ch+".xls\' using 2 notitle lc rgb \"#164191\"");
	            tp.append("unset multiplot");
	            }
	        if (svg) // save output as .svg
	            {
	            tp.append("set term svg dynamic enhanced");
	            tp.append("set output \""+imagename+".svg\"");
	            tp.append("set multiplot");
	            tp.append("set grid back xtics ytics linestyle 11");
	            tp.append("set style histogram errorbars gap 0 lw 2");
	            tp.append("plot \'00 Histogram Summary "+ch+".xls\' using 2:3 notitle lc rgb \"#808080\"");
	            tp.append("unset grid");
	            tp.append("set style histogram clustered gap 0");
	            tp.append("plot \'00 Histogram Summary "+ch+".xls\' using 2 notitle lc rgb \"#164191\"");
	            tp.append("unset multiplot");
	            }
	        if (png) // save output as .png
	            {
	            tp.append("set xlabel offset 0,0");
	            tp.append("set ylabel offset 0,0");
	            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	            tp.append("set output \""+imagename+".png\"");
	            tp.append("set multiplot");
	            tp.append("set grid back xtics ytics linestyle 11");
	            tp.append("set style histogram errorbars gap 0 lw 2");
	            tp.append("plot \'00 Histogram Summary "+ch+".xls\' using 2:3 notitle lc rgb \"#808080\"");
	            tp.append("unset grid");
	            tp.append("set style histogram clustered gap 0");
	            tp.append("plot \'00 Histogram Summary "+ch+".xls\' using 2 notitle lc rgb \"#164191\"");
	            tp.append("unset multiplot");
	            }
	        }
	        
	    else // no standard errors
	        {
	        tp.append("set grid back xtics ytics linestyle 11");
	        tp.append("set xtic rotate by -45 scale 0");
	        if (screen)
	            {
	            tp.append("set term "+terminal+" title \"Intensities\"");
	            plotLine(tp, ch); // creates the plot command
	            }
	        if (svg)
	            {
	            tp.append("set term svg dynamic enhanced");
	            tp.append("set output \""+imagename+".svg\"");
	            if (!screen) plotLine(tp, ch);
	            else tp.append("replot");
	            }
	        if (png)
	            {
	            tp.append("set xlabel offset 0,0");
	            tp.append("set ylabel offset 0,0");
	            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	            tp.append("set output \""+imagename+".png\"");
	            if (!screen && !svg) plotLine(tp, ch);
	            else tp.append("replot");
	            }
	        } // end of SD
	
	    tp.append("#EOF");
	    return tp;
        } // end of histoScriptInt


    protected TextPanel histoScript(String imagename, String filename, boolean both)
	    {
		TextPanel tp = initFile();
        if (plottitle!="") tp.append("set title \""+plottitle+"\"");
        if (axislabel!="") tp.append("set xlabel \""+axislabel+"\" offset +0,1 tc rgb \"#062356\"");
        tp.append("set ylabel \"frequency\" offset +2,0 tc rgb \"#062356\"");
        tp.append("set xrange [1:128]");
        if (both) tp.append("set yrange [-450000:450000]");
        else tp.append("set yrange [0:450000]");
        tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
        tp.append("set border 3 linestyle 10");
        tp.append("set xtics 4 tc rgb \"#113577\" nomirror rotate by -45");
        tp.append("set ytics 10000 tc rgb \"#113577\" nomirror");
        tp.append("set tics front out");
        tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
        tp.append("set style fill solid 0.9 border");
	    
	    if (sd) // calculate standard errors
	        {
	        tp.append("set style data histogram");
	        tp.append("set bars small back");
            if (screen) // show output
	            {
	            tp.append("set term "+terminal+" title \"Ratios\"");
	            tp.append("set multiplot");
	            tp.append("set grid back xtics ytics linestyle 11");
	            tp.append("set style histogram errorbars gap 0 lw 2");
	            tp.append("plot \'"+filename+"\' using 2:3 notitle lc rgb \"#808080\"");
	            tp.append("unset grid");
	            tp.append("set style histogram clustered gap 0");
	            tp.append("plot \'"+filename+"\' using 2 notitle lc rgb \"#164191\"");
	            tp.append("unset multiplot");
	            }
	        if (svg) // save output as .svg
	            {
	            tp.append("set term svg dynamic enhanced");
	            tp.append("set output \""+imagename+".svg\"");
	            tp.append("set multiplot");
	            tp.append("set grid back xtics ytics linestyle 11");
	            tp.append("set style histogram errorbars gap 0 lw 2");
	            tp.append("plot \'"+filename+"\' using 2:3 notitle lc rgb \"#808080\"");
	            tp.append("unset grid");
	            tp.append("set style histogram clustered gap 0");
	            tp.append("plot \'"+filename+"\' using 2 notitle lc rgb \"#164191\"");
	            tp.append("unset multiplot");
	            }
	        if (png) // save output as .png
	            {
	            tp.append("set xlabel offset 0,0");
	            tp.append("set ylabel offset 0,0");
	            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	            tp.append("set output \""+imagename+".png\"");
	            tp.append("set multiplot");
	            tp.append("set grid back xtics ytics linestyle 11");
	            tp.append("set style histogram errorbars gap 0 lw 2");
	            tp.append("plot \'"+filename+"\' using 2:3 notitle lc rgb \"#808080\"");
	            tp.append("unset grid");
	            tp.append("set style histogram clustered gap 0");
	            tp.append("plot \'"+filename+"\' using 2 notitle lc rgb \"#164191\"");
	            tp.append("unset multiplot");
	            }
	        }
	        
	    else // no standard errors
	        {
	        tp.append("set grid back xtics ytics linestyle 11");
	        tp.append("set xtic rotate by -45 scale 0");
            if (screen)
	            {
	            tp.append("set term "+terminal+" title \"Ratios\"");
	            tp.append("plot \'"+filename+"\' using 1:2 notitle with boxes lc rgb \"#164191\"");
	            }
	        if (svg)
	            {
	            tp.append("set term svg dynamic enhanced");
	            tp.append("set output \""+imagename+".svg\"");
	            if (!screen) tp.append("plot \'"+filename+"\' using 1:2 notitle with boxes lc rgb \"#164191\"");
	            else tp.append("replot");
	            }
	        if (png)
	            {
	            tp.append("set xlabel offset 0,0");
	            tp.append("set ylabel offset 0,0");
	            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	            tp.append("set output \""+imagename+".png\"");
	            if (!screen && !svg) tp.append("plot \'"+filename+"\' using 1:2 notitle with boxes lc rgb \"#164191\"");
	            else tp.append("replot");
	            }
	        } // end of SD
	
	    tp.append("#EOF");
	    return tp;
    } // end of histoScript

    
    protected TextPanel statScript1(String imagename)
    	{
    	TextPanel tp = initFile();
   
        if (plottitle!="") tp.append("set title \""+plottitle+"\"");
        tp.append("set xlabel \"\" ");
        tp.append("set ylabel \"ratio\" offset +2,0 tc rgb \"#062356\"");
        tp.append("set xrange [0:2]");
        tp.append("set yrange [0.00390625:256]");
        tp.append("set logscale y 2");
        tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
        tp.append("set border 3 linestyle 10");
        tp.append("set ytics (\"1/256\" 0.00390625, \"1/128\" 0.0078125, \"1/64\" 0.015625, \"1/32\" 0.03125, \"1/16\" 0.0625, \"1/8\" 0.125, \"1/4\" 0.25, \"1/2\" 0.5, \"1/1\" 1, \"2/1\" 2, \"4/1\" 4, \"8/1\" 8, \"16/1\" 16, \"32/1\" 32, \"64/1\" 64, \"128/1\" 128, \"256/1\" 256) tc rgb \"#113577\" nomirror");
        tp.append("set tics front out");
        tp.append("unset xtics");
        tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
        tp.append("set style fill solid 0.9 border");
        tp.append("set boxwidth 1.0");
        tp.append("set bars small");

        if (sd) // add error bars
            {
            if (screen)
                {
                tp.append("set term "+terminal+" title \"Ratios\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("unset multiplot");
                }
            if (svg)
                {
                tp.append("set term svg dynamic enhanced");
                tp.append("set output \""+imagename+".svg\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("unset multiplot");
                }
            if (png)
                {
                tp.append("set xlabel offset 0,0");
                tp.append("set ylabel offset 0,0");
                tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
                tp.append("set output \""+imagename+".png\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("unset multiplot");
                }
            }
        else // no error bars
            {
            if (screen)
                {
                tp.append("set term "+terminal+" title \"Ratios\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 4 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 5 lc rgb \"#1F497D\" notitle");
                tp.append("unset multiplot");
                }
            if (svg)
                {
                tp.append("set term svg dynamic enhanced");
                tp.append("set output \""+imagename+".svg\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 4 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 5 lc rgb \"#1F497D\" notitle");
                tp.append("unset multiplot");
                }
            if (png)
                {
                tp.append("set xlabel offset 0,0");
                tp.append("set ylabel offset 0,0");
                tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
                tp.append("set output \""+imagename+".png\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 4 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 5 lc rgb \"#1F497D\" notitle");
                tp.append("unset multiplot");
                }
            }

		tp.append("#EOF");
		return tp;
	    } // end of statscript1.plt
    
    
    protected TextPanel statScript2(String imagename)
		{
		TextPanel tp = initFile();
	
        if (plottitle!="") tp.append("set title \""+plottitle+"\"");
        tp.append("set xlabel tc rgb \"#062356\"");
        tp.append("set ylabel \"ratio\" offset +2,0 tc rgb \"#062356\"");
        tp.append("set xrange [0:4]");
/*            double yLow = 0.5; // automatic generation of yrange
        double yHigh = 2.0;
        tp.append("set yrange ["+IJ.d2s(yLow,1)+":"+IJ.d2s(yHigh,1)+"]"); */
        tp.append("set yrange [0.00390625:256]");
        tp.append("set logscale y 2");
        tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
        tp.append("set border 3 linestyle 10");
        tp.append("set xtics (\"\" 0, \"Quartile 1\" 1, \"Median\" 2, \"Quartile 3\" 3, \"\" 4) tc rgb \"#113577\" nomirror");
        tp.append("set ytics tc rgb \"#113577\" nomirror");
/*            String tics = IJ.d2s(yLow,1); // automatic generation of ytics
        long yMax = Math.round((yHigh-yLow)*10);
        for (long i=0; i<yMax; i++)
            {
            yLow = yLow + 0.1;
            tics = tics+","+yLow;
            }
        tp.append("set ytics ("+tics+") tc rgb \"#113577\" nomirror"); */
        tp.append("set tics front out");
        tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
        tp.append("set style fill solid 0.9 border");
        tp.append("set boxwidth 0.9");
        tp.append("set bars fullwidth");

        if (screen)
            {
            tp.append("set term "+terminal+" title \"Ratios\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::1 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::2::2 with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::3::3 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
            tp.append("unset multiplot");
            }
        if (svg)
            {
            tp.append("set term svg dynamic enhanced");
            tp.append("set output \""+imagename+".svg\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::1 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::2::2 with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::3::3 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
            tp.append("unset multiplot");
            }
        if (png)
            {
            tp.append("set xlabel offset 0,0");
            tp.append("set ylabel offset 0,0");
            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
            tp.append("set output \""+imagename+".png\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::1 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::2::2 with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::3::3 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
            tp.append("unset multiplot");
            }
	
		tp.append("#EOF");
		return tp;
		} // end of statscript2.plt


    protected TextPanel statScriptC1(String imagename)
	{
	TextPanel tp = initFile();
	
    String xLabel =""; // create the labels for the x-axis
    for (int i=0; i<(nFiles-1); i++)
        {
        xLabel += "\""+xTitle[i]+"\" "+(i+1)+", ";
        }
    xLabel += "\""+xTitle[(nFiles-1)]+"\" "+nFiles;

    if (plottitle!="") tp.append("set title \""+plottitle+"\"");
    tp.append("set xlabel \"\" ");
    tp.append("set ylabel \"ratio\" offset +2,0 tc rgb \"#062356\"");
    tp.append("set xrange [0:"+(nFiles+1)+"]");
    tp.append("set yrange [0.00390625:256]");
    tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
    tp.append("set border 3 linestyle 10");
    tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror");
    tp.append("set ytics (\"1/256\" 0.00390625, \"1/128\" 0.0078125, \"1/64\" 0.015625, \"1/32\" 0.03125, \"1/16\" 0.0625, \"1/8\" 0.125, \"1/4\" 0.25, \"1/2\" 0.5, \"1/1\" 1, \"2/1\" 2, \"4/1\" 4, \"8/1\" 8, \"16/1\" 16, \"32/1\" 32, \"64/1\" 64, \"128/1\" 128, \"256/1\" 256) tc rgb \"#113577\" nomirror");
    tp.append("set logscale y 2");
    tp.append("set tics front out");
    tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
    tp.append("set style fill solid 0.9 border");
    tp.append("set boxwidth 0.8");
    tp.append("set bars small");

    if (sd) // add error bars
        {
        if (screen) // display results
            {
            tp.append("set term "+terminal+" title \"Ratios\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
            tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("unset multiplot");
            }
        if (svg) // create .svg file
            {
            tp.append("set term svg dynamic enhanced");
            tp.append("set output \""+imagename+".svg\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
            tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("unset multiplot");
            }
        if (png) // create .png file
            {
            tp.append("set xlabel offset 0,0");
            tp.append("set ylabel offset 0,0");
            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
            tp.append("set output \""+imagename+".png\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
            tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
            tp.append("unset multiplot");
            }
        }
    else // no error bars
        {
        if (screen)
            {
            tp.append("set term "+terminal+" title \"Ratios\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
            tp.append("unset multiplot");
            }
        if (svg)
            {
            tp.append("set term svg dynamic enhanced");
            tp.append("set output \""+imagename+".svg\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
            tp.append("unset multiplot");
            }
        if (png)
            {
            tp.append("set xlabel offset 0,0");
            tp.append("set ylabel offset 0,0");
            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
            tp.append("set output \""+imagename+".png\"");
            tp.append("set multiplot");
            tp.append("set grid back ytics linestyle 11");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
            tp.append("unset grid");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
            tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
            tp.append("unset multiplot");
            }
        }
	
	tp.append("#EOF");
	return tp;
    } // end of statscriptC1.plt
    
    
    protected TextPanel statScriptC2(String imagename)
	{
	TextPanel tp = initFile();

    int c = 1; // create the labels for the x-axis
    String xLabel = "";
    for (int j=0; j<3; j++)
        {
        for (int i=0; i<nFiles; i++)
            {
            if (j==2 && i==(nFiles-1)) xLabel += "\""+xTitle[i]+"\" "+c;
            else xLabel += "\""+xTitle[i]+"\" "+c+", ";
            c++;
            }
        c++;
        }

    if (plottitle!="") tp.append("set title \""+plottitle+"\"");
    tp.append("set xlabel tc rgb \"#062356\"");
    tp.append("set ylabel \"ratio\" offset +2,0 tc rgb \"#062356\"");
    tp.append("set label 1 \"Quartile 1\" at graph 0.167,-0.08 center tc rgb \"#062356\"");
    tp.append("set label 2 \"Median\" at graph 0.5,-0.08 center tc rgb \"#062356\"");
    tp.append("set label 3 \"Quartile 3\" at graph 0.833,-0.08 center tc rgb \"#062356\"");
    tp.append("set xrange [0:"+((nFiles*3)+3)+"]");
/*            double yLow = 0.5; // automatic generation of y-range
    double yHigh = 2.0;
    tp.append("set yrange ["+IJ.d2s(yLow,1)+":"+IJ.d2s(yHigh,1)+"]");*/
    tp.append("set yrange [0.00390625:256]");
    tp.append("set logscale y 2");
    tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
    tp.append("set border 3 linestyle 10");
    tp.append("set ytics (\"1/256\" 0.00390625, \"1/128\" 0.0078125, \"1/64\" 0.015625, \"1/32\" 0.03125, \"1/16\" 0.0625, \"1/8\" 0.125, \"1/4\" 0.25, \"1/2\" 0.5, \"1/1\" 1, \"2/1\" 2, \"4/1\" 4, \"8/1\" 8, \"16/1\" 16, \"32/1\" 32, \"64/1\" 64, \"128/1\" 128, \"256/1\" 256) tc rgb \"#113577\" nomirror");
/*            String tics = IJ.d2s(yLow,1); // automatic generation of y-tics
    long yMax = Math.round((yHigh-yLow)*10);
    for (long i=0; i<yMax; i++)
        {
        if (yLow<=2) yLow = yLow + 0.1;
        else 
            {
            yLow = yLow + 0.2;
            i++;
            }
        tics = tics+","+yLow;
        }
    tp.append("set ytics ("+tics+") tc rgb \"#113577\" nomirror");*/
    tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror offset 0,0.5");
    tp.append("set tics front out");
    tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
    tp.append("set style fill solid 0.9 border");
    tp.append("set boxwidth 0.9");
    tp.append("set bars small back");

    if (screen)
        {
        tp.append("set term "+terminal+" title \"Ratios\"");
        tp.append("set multiplot");
        tp.append("set grid back ytics linestyle 11");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
        tp.append("unset grid");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles)+" with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles)+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
        tp.append("unset multiplot");
        }
    if (svg)
        {
        tp.append("set term svg dynamic enhanced");
        tp.append("set output \""+imagename+".svg\"");
        tp.append("set multiplot");
        tp.append("set grid back ytics linestyle 11");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
        tp.append("unset grid");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles)+" with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles)+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
        tp.append("unset multiplot");
        }
    if (png)
        {
        tp.append("set xlabel offset 0,0");
        tp.append("set ylabel offset 0,0");
        tp.append("set label 1 at graph 0.167,-0.035 center");
        tp.append("set label 2 at graph 0.5,-0.035 center");
        tp.append("set label 3 at graph 0.833,-0.035 center");
        tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
        tp.append("set output \""+imagename+".png\"");
        tp.append("set multiplot");
        tp.append("set grid back ytics linestyle 11");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
        tp.append("unset grid");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles)+" with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles)+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 every ::1::"+nFiles+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles-1)+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles-1)+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
        tp.append("unset multiplot");
        }

	tp.append("#EOF");
	return tp;
    } // end of statscriptC2.plt

    
    protected TextPanel statScriptC3(String imagename)
	{
	TextPanel tp = initFile();

    String xLabel =""; // create the labels for the x-axis
    for (int i=0; i<(nFiles-1); i++)
        {
        xLabel += "\""+xTitle[i]+"\" "+(i+1)+", ";
        }
    xLabel += "\""+xTitle[(nFiles-1)]+"\" "+nFiles;
    
    if (plottitle!="") tp.append("set title \""+plottitle+"\"");
    tp.append("set xlabel \"\" ");
    tp.append("set ylabel \"ratio\" offset +2,0 tc rgb \"#062356\"");
    tp.append("set xrange [0:"+(nFiles+1)+"]");
    tp.append("set yrange [0.00390625:256]");
    tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
    tp.append("set border 3 linestyle 10");
    tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror");
    tp.append("set ytics (\"1/256\" 0.00390625, \"1/128\" 0.0078125, \"1/64\" 0.015625, \"1/32\" 0.03125, \"1/16\" 0.0625, \"1/8\" 0.125, \"1/4\" 0.25, \"1/2\" 0.5, \"1/1\" 1, \"2/1\" 2, \"4/1\" 4, \"8/1\" 8, \"16/1\" 16, \"32/1\" 32, \"64/1\" 64, \"128/1\" 128, \"256/1\" 256) tc rgb \"#113577\" nomirror");
    tp.append("set logscale y 2");
    tp.append("set tics front out");
    tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
    tp.append("set style fill solid 0.9 border");
    tp.append("set boxwidth 0.8");
    tp.append("set bars small");

    if (screen)
        {
        tp.append("set term "+terminal+" title \"Ratios\"");
        tp.append("set multiplot");
        tp.append("set grid back ytics linestyle 11");
        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
        tp.append("unset grid");
        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
        tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("unset multiplot");
        }
    if (svg)
        {
        tp.append("set term svg dynamic enhanced");
        tp.append("set output \""+imagename+".svg\"");
        tp.append("set multiplot");
        tp.append("set grid back ytics linestyle 11");
        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
        tp.append("unset grid");
        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
        tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("unset multiplot");
        }
    if (png)
        {
        tp.append("set xlabel offset 0,0");
        tp.append("set ylabel offset 0,0");
        tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
        tp.append("set output \""+imagename+".png\"");
        tp.append("set multiplot");
        tp.append("set grid back ytics linestyle 11");
        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
        tp.append("unset grid");
        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
        tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
        tp.append("unset multiplot");
        }

	tp.append("#EOF");
	return tp;
    } // end of statscriptC3.plt

    
    protected TextPanel scatterScript(String imagename)
 		{
 		TextPanel tp = initFile();

        if (plottitle!="") tp.append("set title \""+plottitle+"\"");
        tp.append("set xlabel \"x\" offset +0,1 tc rgb \"#062356\"");
        tp.append("set ylabel \"y\" offset +3,1 tc rgb \"#062356\"");
        tp.append("set zlabel \"frequency\" offset +3,2 tc rgb \"#062356\"");
        tp.append("set zlabel rotate by +90");
        tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
        tp.append("set border 3 linestyle 10");
        tp.append("set xtics 20 tc rgb \"#113577\" nomirror rotate by +45");
        tp.append("set ytics 20 tc rgb \"#113577\" nomirror rotate by -45 offset -2,0");
        tp.append("set ztics 5000 tc rgb \"#113577\" nomirror rotate by +45 offset +1,0");
        tp.append("set cbtics 5000 tc rgb \"#113577\" nomirror");
        tp.append("set tics front out");
        tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
        tp.append("set grid xtics ytics ztics linestyle 11");
        tp.append("set ticslevel 0");
        tp.append("set xrange [0:255]");
        tp.append("set yrange [0:255]");
        tp.append("set zrange [0:65535]");
        tp.append("set cbrange [0:65535]");
        tp.append("set view 45, 135, 1, 1");
        tp.append("set isosample 100, 100");
        tp.append("set hidden3d");
        tp.append("set pm3d");
        tp.append("set palette defined (0 \"cyan\", 10921 \"blue\", 21842 \"magenta\", 32764 \"black\", 43689 \"green\", 54613 \"yellow\", 65535 \"red\")");

        if (screen)
            {
            tp.append("set term "+terminal+" title \"Scatter plot\"");
            tp.append("splot \'00 Scatter plot.txt\' matrix with lines notitle");
            }
        if (png) // .svg doesn't make sense here
            {
            tp.append("set ytics 20 tc rgb \"#113577\" nomirror rotate by -45 offset -3,0");
            tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
            tp.append("set output \""+imagename+".png\"");
            if (screen) tp.append("replot");
            else tp.append("splot \'00 Scatter plot.txt\' matrix with lines notitle");
            }
 	
 		tp.append("#EOF");
 		return tp;
 		} // end of scatterScript.plt
    
    
    protected TextPanel statScript1Int(int ch, String imagename)
	    {
		TextPanel tp = initFile();
		
		if (plottitle!="") tp.append("set title \""+plottitle+"\"");
		tp.append("set xlabel \"\" ");
		tp.append("set ylabel \"intensity\" offset +2,0 tc rgb \"#062356\"");
		tp.append("set xrange [0:2]");
		tp.append("set yrange [0:255]");
		tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
		tp.append("set border 3 linestyle 10");
		tp.append("set ytics 15 tc rgb \"#113577\" nomirror");
		tp.append("set tics front out");
		tp.append("unset xtics");
		tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
		tp.append("set style fill solid 0.9 border");
		tp.append("set boxwidth 1.0");
		tp.append("set bars small");

		if (sd) // add error bars
			{
			if (screen)
				{
				tp.append("set term "+terminal+" title \"Intensities\"");
				tp.append("set multiplot");
				tp.append("set grid back ytics linestyle 11");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
				tp.append("unset grid");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("unset multiplot");
				}
			if (svg)
				{
				tp.append("set term svg dynamic enhanced");
				tp.append("set output \""+imagename+".svg\"");
				tp.append("set multiplot");
				tp.append("set grid back ytics linestyle 11");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
				tp.append("unset grid");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("unset multiplot");
				}
			if (png)
				{
				tp.append("set xlabel offset 0,0");
				tp.append("set ylabel offset 0,0");
				tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
				tp.append("set output \""+imagename+".png\"");
				tp.append("set multiplot");
				tp.append("set grid back ytics linestyle 11");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
				tp.append("unset grid");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
				tp.append("unset multiplot");
				}
			}
		else // no error bars
			{
			if (screen)
				{
				tp.append("set term "+terminal+" title \"Intensities\"");
				tp.append("set multiplot");
				tp.append("set grid back ytics linestyle 11");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 4 lt 3 notitle");
				tp.append("unset grid");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 5 lc rgb \"#1F497D\" notitle");
				tp.append("unset multiplot");
				}
			if (svg)
				{
				tp.append("set term svg dynamic enhanced");
				tp.append("set output \""+imagename+".svg\"");
				tp.append("set multiplot");
				tp.append("set grid back ytics linestyle 11");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 4 lt 3 notitle");
				tp.append("unset grid");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 5 lc rgb \"#1F497D\" notitle");
				tp.append("unset multiplot");
				}
			if (png)
				{
				tp.append("set xlabel offset 0,0");
				tp.append("set ylabel offset 0,0");
				tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
				tp.append("set output \""+imagename+".png\"");
				tp.append("set multiplot");
				tp.append("set grid back ytics linestyle 11");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 4 lt 3 notitle");
				tp.append("unset grid");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
				tp.append("plot \'00 Statistics Summary "+ch+"-1-1.xls\' every ::1::1 using 1:4:4:4:4 with candlesticks lt -1 lw 5 lc rgb \"#1F497D\" notitle");
				tp.append("unset multiplot");
				}
			} // end of SD

		tp.append("#EOF");
		return tp;
	    } // end of statscript1Int.plt

    
    protected TextPanel statScript2Int(int ch, String imagename)
	    {
		TextPanel tp = initFile();

		if (plottitle!="") tp.append("set title \""+plottitle+"\"");
		tp.append("set xlabel tc rgb \"#062356\"");
		tp.append("set ylabel \"intensity\" offset +2,0 tc rgb \"#062356\"");
		tp.append("set xrange [0:4]");
		tp.append("set yrange [0:255]");
		tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
		tp.append("set border 3 linestyle 10");
		tp.append("set xtics (\"\" 0, \"Quartile 1\" 1, \"Median\" 2, \"Quartile 3\" 3, \"\" 4) tc rgb \"#113577\" nomirror");
		tp.append("set ytics 15 tc rgb \"#113577\" nomirror");
		tp.append("set tics front out");
		tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
		tp.append("set style fill solid 0.9 border");
		tp.append("set boxwidth 0.9");
		tp.append("set bars fullwidth");

		if (screen)
			{
			tp.append("set term "+terminal+" title \"Intensities\"");
			tp.append("set multiplot");
			tp.append("set grid back ytics linestyle 11");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::1::1 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
			tp.append("unset grid");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::2::2 with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::3::3 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
			tp.append("unset multiplot");
			}
		if (svg)
			{
			tp.append("set term svg dynamic enhanced");
			tp.append("set output \""+imagename+".svg\"");
			tp.append("set multiplot");
			tp.append("set grid back ytics linestyle 11");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::1::1 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
			tp.append("unset grid");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::2::2 with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::3::3 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
			tp.append("unset multiplot");
			}
		if (png)
			{
			tp.append("set xlabel offset 0,0");
			tp.append("set ylabel offset 0,0");
			tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
			tp.append("set output \""+imagename+".png\"");
			tp.append("set multiplot");
			tp.append("set grid back ytics linestyle 11");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::1::1 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
			tp.append("unset grid");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::2::2 with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2 every ::3::3 with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
			tp.append("plot \'00 Statistics Summary "+ch+"-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
			tp.append("unset multiplot");
			}
		
		tp.append("#EOF");
		return tp;
	    } // end of statscript2Int.plt


    protected TextPanel compScript1Int(String imagename)
    	{
    	TextPanel tp = initFile();

        String xLabel =""; // create the labels for the x-axis
        for (int i=0; i<(nFiles-1); i++)
            {
            xLabel += "\""+xTitle[i]+"\" "+(i+1)+", ";
            }
        xLabel += "\""+xTitle[(nFiles-1)]+"\" "+nFiles;

        if (plottitle!="") tp.append("set title \""+plottitle+"\"");
        tp.append("set xlabel \"\" ");
        tp.append("set ylabel \"intensity\" offset +2,0 tc rgb \"#062356\"");
        tp.append("set xrange [0:"+(nFiles+1)+"]");
        tp.append("set yrange [0:255]");
		tp.append("set ytics 15 tc rgb \"#113577\" nomirror");
		tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
        tp.append("set border 3 linestyle 10");
        tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror");
        tp.append("set tics front out");
        tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
        tp.append("set style fill solid 0.9 border");
        tp.append("set boxwidth 0.8");
        tp.append("set bars small");

        if (sd) // add error bars
            {
            if (screen) // display results
                {
                tp.append("set term "+terminal+" title \"Intensities\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("unset multiplot");
                }
            if (svg) // create .svg file
                {
                tp.append("set term svg dynamic enhanced");
                tp.append("set output \""+imagename+".svg\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("unset multiplot");
                }
            if (png) // create .png file
                {
                tp.append("set xlabel offset 0,0");
                tp.append("set ylabel offset 0,0");
                tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
                tp.append("set output \""+imagename+".png\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("plot \'00 Statistics Summary 1-2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-6.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#606060\" notitle");
                tp.append("unset multiplot");
                }
            }
        else // no error bars
            {
            if (screen)
                {
                tp.append("set term "+terminal+" title \"Intensities\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("unset multiplot");
                }
            if (svg)
                {
                tp.append("set term svg dynamic enhanced");
                tp.append("set output \""+imagename+".svg\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("unset multiplot");
                }
            if (png)
                {
                tp.append("set xlabel offset 0,0");
                tp.append("set ylabel offset 0,0");
                tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
                tp.append("set output \""+imagename+".png\"");
                tp.append("set multiplot");
                tp.append("set grid back ytics linestyle 11");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:2:6:5 with candlesticks whiskerbars 0.2 lc rgb \"#800080\" lw 6 lt 3 notitle");
                tp.append("unset grid");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
                tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
                tp.append("unset multiplot");
                }
            } // end of SD

        tp.append("#EOF");
        return tp;
    	} // end of compScript1Int.plt


    protected TextPanel compScript2Int(String imagename)
		{
		TextPanel tp = initFile();
	
	    int c = 1; // create the labels for the x-axis
        String xLabel =""; // create the labels for the x-axis
	    for (int j=0; j<3; j++)
	        {
	        for (int i=0; i<nFiles; i++)
	            {
	            if (j==2 && i==(nFiles-1)) xLabel += "\""+xTitle[i]+"\" "+c;
	            else xLabel += "\""+xTitle[i]+"\" "+c+", ";
	            c++;
	            }
	        c++;
	        }
	
	    if (plottitle!="") tp.append("set title \""+plottitle+"\"");
	    tp.append("set xlabel tc rgb \"#062356\"");
	    tp.append("set ylabel \"intensity\" offset +2,0 tc rgb \"#062356\"");
	    tp.append("set label 1 \"Quartile 1\" at graph 0.167,-0.08 center tc rgb \"#062356\"");
	    tp.append("set label 2 \"Median\" at graph 0.5,-0.08 center tc rgb \"#062356\"");
	    tp.append("set label 3 \"Quartile 3\" at graph 0.833,-0.08 center tc rgb \"#062356\"");
	    tp.append("set xrange [0:"+((nFiles*3)+3)+"]");
	/*            double yLow = 0.5; // automatic generation of y-range
	            double yHigh = 2.0;
	            tp.append("set yrange ["+IJ.d2s(yLow,1)+":"+IJ.d2s(yHigh,1)+"]");*/
        tp.append("set yrange [0:255]");
		tp.append("set ytics 15 tc rgb \"#113577\" nomirror");
	    tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
	    tp.append("set border 3 linestyle 10");
	/*            String tics = IJ.d2s(yLow,1); // automatic generation of y-tics
	            long yMax = Math.round((yHigh-yLow)*10);
	            for (long i=0; i<yMax; i++)
	                {
	                if (yLow<=2) yLow = yLow + 0.1;
	                else 
	                    {
	                    yLow = yLow + 0.2;
	                    i++;
	                    }
	                tics = tics+","+yLow;
	                }
	            tp.append("set ytics ("+tics+") tc rgb \"#113577\" nomirror");*/
	    tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror offset 0,0.5");
	    tp.append("set tics front out");
	    tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
	    tp.append("set style fill solid 0.9 border");
	    tp.append("set boxwidth 0.9");
	    tp.append("set bars small back");
	
	    if (screen)
	        {
	        tp.append("set term "+terminal+" title \"Intensities\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles)+" with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles)+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("unset multiplot");
	        }
	    if (svg)
	        {
	        tp.append("set term svg dynamic enhanced");
	        tp.append("set output \""+imagename+".svg\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles)+" with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles)+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("unset multiplot");
	        }
	    if (png)
	        {
	        tp.append("set xlabel offset 0,0");
	        tp.append("set ylabel offset 0,0");
	        tp.append("set label 1 at graph 0.167,-0.035 center");
	        tp.append("set label 2 at graph 0.5,-0.035 center");
	        tp.append("set label 3 at graph 0.833,-0.035 center");
	        tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	        tp.append("set output \""+imagename+".png\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles)+" with boxes lc rgb \"#164191\" lw 4 lt 3 notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles)+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 every ::1::"+nFiles+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 every ::"+(nFiles+2)+"::"+((nFiles+2)+nFiles-1)+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("plot \'00 Statistics Summary 2.xls\' using 1:2:3 every ::"+((nFiles*2)+3)+"::"+((nFiles*2)+3+nFiles-1)+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("unset multiplot");
	        }
	    
	    tp.append("#EOF");
	    return tp;
		} // end of compScript2Int.plt
	    
    protected TextPanel compScript3Int(String imagename)
		{
		TextPanel tp = initFile();

	    int c = 1; // create the labels for the x-axis
        String xLabel =""; // create the labels for the x-axis
	    for (int j=0; j<3; j++)
	        {
	        for (int i=0; i<nFiles; i++)
	            {
	            if (j==2 && i==(nFiles-1)) xLabel += "\""+xTitle[i]+"\" "+c;
	            else xLabel += "\""+xTitle[i]+"\" "+c+", ";
	            c++;
	            }
	        c++;
	        }
		
	    if (plottitle!="") tp.append("set title \""+plottitle+"\"");
	    tp.append("set xlabel \"\" ");
	    tp.append("set ylabel \"intensity\" offset +2,0 tc rgb \"#062356\"");
	    tp.append("set xrange [0:"+(nFiles+1)+"]");
        tp.append("set yrange [0:255]");
		tp.append("set ytics 15 tc rgb \"#113577\" nomirror");
	    tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
	    tp.append("set border 3 linestyle 10");
	    tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror");
	    tp.append("set tics front out");
	    tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
	    tp.append("set style fill solid 0.9 border");
	    tp.append("set boxwidth 0.8");
	    tp.append("set bars small");
	
	    if (screen)
	        {
	        tp.append("set term "+terminal+" title \"Intensities\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("unset multiplot");
	        }
	    if (svg)
	        {
	        tp.append("set term svg dynamic enhanced");
	        tp.append("set output \""+imagename+".svg\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("unset multiplot");
	        }
	    if (png)
	        {
	        tp.append("set xlabel offset 0,0");
	        tp.append("set ylabel offset 0,0");
	        tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	        tp.append("set output \""+imagename+".png\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:3:3:5:5 with candlesticks lc rgb \"#9BBB59\" lw 6 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary 1-1.xls\' using 1:4:4:4:4 with candlesticks lt -1 lw 6 lc rgb \"#1F497D\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-4.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-3.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("plot \'00 Statistics Summary 1-5.xls\' using 1:2:3 with errorbars pointtype 0 lt -1 lw 6 lc rgb \"#800080\" notitle");
	        tp.append("unset multiplot");
	        }
	
	    tp.append("#EOF");
	    return tp;
	    } // end of compScript3Int.plt


    protected TextPanel ratioScript(String imagename)
		{
		TextPanel tp = initFile();
	
	    int c = 1; // create the labels for the x-axis
	    String xLabel =""; // create the labels for the x-axis
	    for (int j=0; j<3; j++)
	        {
	        for (int i=0; i<nFiles; i++)
	            {
	            if (j==2 && i==(nFiles-1)) xLabel += "\""+xTitle[i]+"\" "+c;
	            else xLabel += "\""+xTitle[i]+"\" "+c+", ";
	            c++;
	            }
	        c++;
	        }
	
	    if (plottitle!="") tp.append("set title \""+plottitle+"\"");
	    tp.append("set xlabel tc rgb \"#062356\"");
	    tp.append("set ylabel \"intensity\" offset +2,0 tc rgb \"#062356\"");
	    tp.append("set label 2 \"Median\" at graph 0.5,-0.08 center tc rgb \"#062356\"");
	    tp.append("set xrange [0:"+(nFiles+1)+"]");
        tp.append("set yrange [0.00390625:256]");
        tp.append("set logscale y 2");
        tp.append("set ytics (\"1/256\" 0.00390625, \"1/128\" 0.0078125, \"1/64\" 0.015625, \"1/32\" 0.03125, \"1/16\" 0.0625, \"1/8\" 0.125, \"1/4\" 0.25, \"1/2\" 0.5, \"1/1\" 1, \"2/1\" 2, \"4/1\" 4, \"8/1\" 8, \"16/1\" 16, \"32/1\" 32, \"64/1\" 64, \"128/1\" 128, \"256/1\" 256) tc rgb \"#113577\" nomirror");
	    tp.append("set style line 10 lt 0 lc rgb \"#808080\"");
	    tp.append("set border 3 linestyle 10");
	    tp.append("set xtics ("+xLabel+") tc rgb \"#113577\" nomirror offset 0,0.5");
	    tp.append("set tics front out");
	    tp.append("set style line 11 lt 3 lc rgb \"#99bbf9\" lw 0.5");
	    tp.append("set style fill solid 0.9 border");
	    tp.append("set boxwidth 0.9");
	    tp.append("set bars small back");
	
	    if (screen)
	        {
	        tp.append("set term "+terminal+" title \"Intensities\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary Ratios 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary Ratios 2.xls\' using 1:2:3 every ::1::"+nFiles+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("unset multiplot");
	        }
	    if (svg)
	        {
	        tp.append("set term svg dynamic enhanced");
	        tp.append("set output \""+imagename+".svg\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary Ratios 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary Ratios 2.xls\' using 1:2:3 every ::1::"+nFiles+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("unset multiplot");
	        }
	    if (png)
	        {
	        tp.append("set xlabel offset 0,0");
	        tp.append("set ylabel offset 0,0");
	        tp.append("set label 1 at graph 0.167,-0.035 center");
	        tp.append("set label 2 at graph 0.5,-0.035 center");
	        tp.append("set label 3 at graph 0.833,-0.035 center");
	        tp.append("set term png font \"/Library/Fonts/Tahoma.ttf\" 14 size 2048,1536");
	        tp.append("set output \""+imagename+".png\"");
	        tp.append("set multiplot");
	        tp.append("set grid back ytics linestyle 11");
	        tp.append("plot \'00 Statistics Summary Ratios 2.xls\' using 1:2 every ::1::"+nFiles+" with boxes lc rgb \"#9BBB59\" lw 4 lt 3 notitle");
	        tp.append("unset grid");
	        tp.append("plot \'00 Statistics Summary Ratios 2.xls\' using 1:2:3 every ::1::"+nFiles+" with errorbars pointtype 0 lt -1 lw 8 lc rgb \"#808080\" notitle");
	        tp.append("unset multiplot");
	        }
	    
	    tp.append("#EOF");
	    return tp;
		} // end of ratioScript.plt
    
    } 