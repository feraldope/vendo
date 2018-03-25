//JFreeChartDemo.java

/*
http://jfree.org/
http://www.jfree.org/jfreechart/
http://www.jfree.org/jfreechart/api/javadoc/index.html
*/

package com.vendo.jFreeChartDemo;

import java.text.DecimalFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class JFreeChartDemo
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		JFreeChartDemo app = new JFreeChartDemo ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

/*
				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

/*
		//check for required args
		if (_model == null)
			displayUsage ("<TBD>", true);
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] TBD";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		_log.debug ("JFreeChartDemo.run");

		XYSeriesCollection dataset = new XYSeriesCollection ();

		final int numGraphs = 5;

		for (int ii = 0; ii < numGraphs; ii++) {
			createAndAddSeries (dataset, ii + 1);
		}

		JFreeChart chart = ChartFactory.createXYLineChart ("Inverse", "X", "Y", dataset, PlotOrientation.VERTICAL, true, true, false);

		ChartFrame frame1 = new ChartFrame ("Chart1", chart);
		frame1.setVisible (true);
		frame1.setSize (600, 600);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void createAndAddSeries (XYSeriesCollection dataset, int factor)
	{
		final int numPoints = 100;

		XYSeries series = new XYSeries ("y=" + factor + "/x");
		for (int ii = -numPoints  / 2; ii <= numPoints / 2; ii++) {
			if (ii != 0) {
				double x = (double) ii / (double) (numPoints / 2);
				double y = (double) factor / x;
				series.add (x, y);

				if (_Debug) {
					System.out.println (ii + ": " + "(" + _decimalFormat.format (x) + ", " + _decimalFormat.format (y) + ")");
				}
			}
		}
		dataset.addSeries (series);
	}


	//private members
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0.00");

	private static Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "JFreeChartDemo";
	public static final String NL = System.getProperty ("line.separator");
}
