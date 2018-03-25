//CsvData.java

package com.vendo.csvData;

/* Generate CSV data for DM/CCC in the following format:

Server Name,Time Stamp,Duration,Total CPU Utilization,Total Memory Utilization,
java8Test3.ca.com,2015-05-11 14:00:00,300.0,75.0,45.0,
java8Test3.ca.com,2015-05-11 14:05:00,300.0,75.0,45.0,
java8Test3.ca.com,2015-05-11 14:10:00,300.0,75.0,45.0,
[...]
*/

import java.io.*;
//import java.net.*;
//import java.nio.file.*;
import java.text.*;
import java.util.*;

import org.apache.logging.log4j.*;


public class CsvData
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[]) throws Exception
	{
		CsvData app = new CsvData ();

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

				} else if (arg.equalsIgnoreCase ("block") || arg.equalsIgnoreCase ("b")) {
					try {
						_blockNumber = Integer.parseInt (args[++ii]);
						if (_blockNumber < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/block " + _blockNumber);

				} else if (arg.equalsIgnoreCase ("checkOnly") || arg.equalsIgnoreCase ("co")) {
					_checkHistoryOnly = true;

*/
				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_startIndex < 0) {
					try {
						_startIndex = Integer.parseInt (arg);
						if (_startIndex < 0)
							throw (new NumberFormatException ());
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for starting index: '" + arg + "'", true);
					}

				} else if (_endIndex < 0) {
					try {
						_endIndex = Integer.parseInt (arg);
						if (_endIndex < 0)
							throw (new NumberFormatException ());
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for ending index", true);
					}

				} else if (_step < 0) {
					try {
						_step = Integer.parseInt (arg);
						if (_step < 0)
							throw (new NumberFormatException ());
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for step", true);
					}

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
/*
			}
*/
		}

/*
		//check for required args and handle defaults
		if (_model == null)
			displayUsage ("No URI specified", true);
		_model = normalizeUrl (_model);

//TODO - verify _destDir exists, and is writable??
		if (_destDir == null)
			_destDir = getCurrentDirectory ();
		_destDir = appendSlash (_destDir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);

		if (_startIndex >= 0 && _endIndex >= 0)
			if (_startIndex > _endIndex)
				displayUsage ("<start index> (" + _startIndex + ") + cannot be greater than <end index> (" + _endIndex + ")", true);

		if (_step < 0 && (_startIndex > 0 || _endIndex > 0))
			_step = 1;

		if (false) { //dump all args
			_log.debug ("args.length = " + args.length);
			for (int ii = 0; ii < args.length; ii++)
				_log.debug ("args[" + ii + "] = '" + args[ii] + "'");
		}
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] TBD...";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () throws Exception
	{
		if (_Debug)
			_log.debug ("CsvData.run");

		String header = "Server Name,Time Stamp,Duration,Total CPU Utilization,Total Memory Utilization,";
		_out.println (header);

		//            server				startDateString			elapsedHours	cpuUtil		memUtil
		generateDate ("java8Test3.ca.com",	"2015-05-11 14:00:00",	1,				60.,		60.);
		generateDate ("java8Test3.ca.com",	"2015-05-13 14:00:00",	4,				70.,		70.);
		generateDate ("java8Test3.ca.com",	"2015-05-15 14:00:00",	24,				80.,		80.);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void generateDate (String server, String startDateString, int elapsedHours, double cpuUtil, double memUtil) throws Exception
	{
		long curTimeInMillis = parseDateToMillis (startDateString);
		long endTimeInMillis = curTimeInMillis + elapsedHours * 3600 * 1000;

		final double duration = 300;
		final double durationInMillis = duration * 1000;

		for (; curTimeInMillis < endTimeInMillis; curTimeInMillis += durationInMillis) {
			_out.println (server + _comma +
						  _dateFormat.format (curTimeInMillis) + _comma +
						  _decimalFormat.format (duration) + _comma +
						  _decimalFormat.format (cpuUtil) + _comma +
						  _decimalFormat.format (memUtil) + _comma);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//convert a date string to the number of milliseconds since the epoch
	private long parseDateToMillis (String startDateString) throws Exception
	{
		Date startDate = _dateFormat.parse (startDateString);
		return startDate.getTime ();
	}


	private static final PrintStream _out = System.out;

	private static final DecimalFormat _decimalFormat = new DecimalFormat ("0.0");
	private static final DateFormat _dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

	private static final String _comma = ",";
//	private static final String _slash = System.getProperty ("file.separator");

	private static Logger _log = LogManager.getLogger (CsvData.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "CsvData";
	public static final String NL = System.getProperty ("line.separator");
}
