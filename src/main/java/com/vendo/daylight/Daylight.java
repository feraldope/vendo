//Daylight.java

//For new year:
//1. Download Sunrise/Sunset data for Daylight.dat from here:
//   http://aa.usno.navy.mil/data/docs/RS_OneYear.php
//2. update method DaylightRecord#isDst with DST dates

package com.vendo.daylight;

import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.TimeZone;


public class Daylight
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		//set TZ for entire app into GMT to avoid issues with dates/times being relative to our local TZ
		TimeZone.setDefault (TimeZone.getTimeZone ("GMT"));
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		Daylight app = new Daylight ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

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
			Integer thisYear = LocalDate.now ().getYear ();
			inputFile = "C:/Users/java/vendo/src/main/java/com/vendo/daylight/Daylight." + thisYear + ".dat";
		}

		try {
			_dataFile = FileSystems.getDefault ().getPath (VendoUtils.appendSystemSlash(inputFile));
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
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] <input file>";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		DaylightFile daylightFile = new DaylightFile (_dataFile);
		Collection<DaylightFileData> daylightFileDataList = daylightFile.readFile ();
		System.out.println ("Read " + daylightFileDataList.size () + " records from " + _dataFile.getFileName ());

		if (LocalDate.now ().getYear () != daylightFile.getDataYear ()) {
			System.out.println ("Warning: the sunrise/sunset data is specific to " + daylightFile.getDataYear ());
		}

		DaylightData daylightData = new DaylightData (daylightFileDataList);

		if (_Debug) {
			System.out.println ("Daylight.run:");
			for (DaylightRecord daylightRecord : daylightData.getDaylightDataList ()) {
				System.out.println (daylightRecord.getLocalDate () + ": " +
									daylightRecord.getSunriseTime () + " -> " +
									daylightRecord.getSunsetTime () + " (" +
									daylightRecord.getLengthOfDay () + ")");
			}
		}

		String chartTitle = _AppName + " " + daylightFile.getDataYear ();
		DaylightChart daylightChart = new DaylightChart ();
		int width = 1200;
		int height = 800;
		ChartPanel chartPanel = new ChartPanel (daylightChart.getChart (chartTitle, daylightData),
                  width, height, //width, height
                  width, height, //minimumDrawWidth, minimumDrawHeight
                  width, height, //maximumDrawWidth, maximumDrawHeight
                  true, //useBuffer
                  true, //properties
                  true, //save
                  true, //print
                  true, //zoom
                  true); //tooltips)

		chartPanel.setDismissDelay (Integer.MAX_VALUE);
		chartPanel.setInitialDelay (0);
		chartPanel.setReshowDelay (0);

//		chartPanel.setBorder ((BorderFactory.createLineBorder (Color.black)));
//		chartPanel.setPreferredSize (new java.awt.Dimension (1200, 800));

		ApplicationFrame appFrame = new ApplicationFrame (_AppName);
		appFrame.setContentPane (chartPanel);
		appFrame.pack ();
		RefineryUtilities.centerFrameOnScreen (appFrame);
		appFrame.setVisible (true);

		return true;
	}


	//private members
	private Path _dataFile;

//	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0.00");
	private static final String NL = System.getProperty ("line.separator");

	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "Daylight";
}
