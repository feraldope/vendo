//OilUsage.java

package com.vendo.oilUsage;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.vendo.vendoUtils.VendoUtils;


public class OilUsage
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		OilUsage app = new OilUsage ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		String inputFile = null;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());
				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (inputFile == null) {
					inputFile = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		if (_Debug) {
			for (int ii = 0; ii < args.length; ii++) {
				String arg = args[ii];
				_log.debug ("processArgs: arg = '" + arg + "'");
			}
		}

		//check for required args, set defaults
		if (inputFile == null) {
			inputFile = "C:/Users/doc/pers/oil.txt";
		}

		try {
			_dataFile = FileSystems.getDefault ().getPath (VendoUtils.appendSlash (inputFile));
		} catch (Exception ee) {
			System.err.println ("Invalid input file: " + inputFile);
			return false;
		}

		if (!Files.exists (_dataFile)) {
			System.err.println ("Invalid input file: " + inputFile);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] <input file>";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		OilFile oilFile = new OilFile (_dataFile);
		Collection<OilFileData> oilFileDataList = oilFile.readFile ();
		System.out.println ("Read " + oilFileDataList.size () + " records");

		if (_Debug) {
			long totalDays = 0;
			for (OilFileData oilFileData : oilFileDataList) {
				oilFileData.getPeriodInDays ();
				totalDays += oilFileData.getPeriodInDays ();
				System.out.println (oilFileData.getStartDate () + " -> " +
									oilFileData.getEndDate () + " = " +
									oilFileData.getPeriodInDays () + " days, gallons/day = " +
									_decimalFormat.format (oilFileData.getGallonsPerDay ()));
			}
			System.out.println ("totalDays = " + totalDays);
		}

		OilUsageData oilUsageData = new OilUsageData (oilFileDataList);

		if (_Debug) {
			System.out.print ("Daily averages: ");
			for (int ii = 1; ii <= 365; ii++) {
				System.out.print (ii + ": " + _decimalFormat.format (oilUsageData.getDailyAverage (ii)) + ", ");
			}
			System.out.println ("");
		}

		OilUsageChart oilUsageChart = new OilUsageChart ();
		ChartPanel chartPanel = new ChartPanel (oilUsageChart.getChart (_AppName, oilUsageData));

		chartPanel.setDismissDelay (Integer.MAX_VALUE);
		chartPanel.setInitialDelay (0);
		chartPanel.setReshowDelay (0);

//		chartPanel.setBorder ((BorderFactory.createLineBorder (Color.black)));
		chartPanel.setPreferredSize (new java.awt.Dimension (1200, 800));

		ApplicationFrame appFrame = new ApplicationFrame (_AppName);
		appFrame.setContentPane (chartPanel);
		appFrame.pack ();
		RefineryUtilities.centerFrameOnScreen (appFrame);
		appFrame.setVisible (true);

		return true;
	}


	//private members
	private Path _dataFile;

	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0.00");
	private static final String NL = System.getProperty ("line.separator");

	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "OilUsage";
}
