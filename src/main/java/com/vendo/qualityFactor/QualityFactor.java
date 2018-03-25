//QualityFactor.java

/*
http://jfree.org/
http://www.jfree.org/jfreechart/
http://www.jfree.org/jfreechart/api/javadoc/index.html
*/

package com.vendo.qualityFactor;

import java.text.DecimalFormat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class QualityFactor
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		QualityFactor app = new QualityFactor ();

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
		_log.debug ("QualityFactor.run");

		XYSeriesCollection dataset = new XYSeriesCollection ();

		final int numGraphs = 10;

		for (int ii = 0; ii <= numGraphs; ii++) {
			double rSquared = (double) ii / (double) numGraphs;
			createAndAddSeries (dataset, rSquared);
		}

		JFreeChart chart = ChartFactory.createXYLineChart ("Quality Factor", "Data Coverage (Percent)", "Quality Factor",
														   dataset, PlotOrientation.VERTICAL, true, true, false);

		Range range100 = new Range (0, 100);
		XYPlot plot = chart.getXYPlot ();

		NumberAxis xAxis = asNumberAxis (plot.getDomainAxis ());
		xAxis.setRange (range100, /*turnOffAutoRange*/ false, /*notify*/ true);

		NumberAxis yAxis = asNumberAxis (plot.getRangeAxis ());
		yAxis.setRange (range100, /*turnOffAutoRange*/ false, /*notify*/ true);

		final int size = 800;
		ChartFrame frame1 = new ChartFrame ("Chart1", chart);
		frame1.setVisible (true);
		frame1.setSize (size, size);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void createAndAddSeries (XYSeriesCollection dataset, double rSquared)
	{
		final int numPoints = 100;

		XYSeries series = new XYSeries ("R2=" + rSquared);

		for (int ii = 0; ii <= numPoints; ii++) {
			double coverageFactor = (double) ii / (double) numPoints;

			//variations of sqrt or squaring the values before combining
//			double qualityFactor = coverageFactor * rSquared;
//			double qualityFactor = coverageFactor * coverageFactor * rSquared;
//			double qualityFactor = coverageFactor * rSquared * rSquared;
//			double qualityFactor = Math.sqrt (coverageFactor * rSquared);
			double qualityFactor = Math.sqrt (coverageFactor) * rSquared;

			series.add (100 * coverageFactor, 100 * qualityFactor);

			if (_Debug) {
				System.out.println (ii + ": " + _decimalFormat.format (rSquared) + ", " +
												_decimalFormat.format (coverageFactor) + ", " +
												_decimalFormat.format (qualityFactor));
			}
		}
		dataset.addSeries (series);
	}

	///////////////////////////////////////////////////////////////////////////
	@SuppressWarnings ("unchecked")
	private NumberAxis asNumberAxis (Axis axis) //cast
	{
		return axis instanceof NumberAxis ? (NumberAxis) axis : null;
	}


	//private members
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0.00");

	private static Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "QualityFactor";
	public static final String NL = System.getProperty ("line.separator");
}
