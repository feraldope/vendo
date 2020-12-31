//DiskUsage.java

//Original inspirations from
//http://stackoverflow.com/questions/2149785/get-size-of-folder-or-file
//http://stackoverflow.com/questions/266825/how-to-format-a-duration-in-java-e-g-format-hmmss

package com.vendo.diskUsage;

import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class DiskUsage
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		DiskUsage app = new DiskUsage ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		double thresholdGB = -1; //threshold in gigabytes
		String folderString = null;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());
				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("quiet") || arg.equalsIgnoreCase ("q")) {
					_Quiet = true;

				} else if (arg.equalsIgnoreCase ("queueSize") || arg.equalsIgnoreCase ("qs")) {
					try {
						_queueSize = Integer.parseInt (args[++ii]);
						if (_queueSize <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("threads") || arg.equalsIgnoreCase ("th")) {
					try {
						_numUsageThreads = Integer.parseInt (args[++ii]);
						if (_numUsageThreads <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("threshold") || arg.equalsIgnoreCase ("t")) {
					try {
						thresholdGB = Double.parseDouble (args[++ii]);
						if (thresholdGB < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (folderString == null) {
					folderString = arg;

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
		if (folderString == null) {
			folderString = ".";
		}

		try {
			_rootFolder = FileSystems.getDefault ().getPath (VendoUtils.appendSlash (folderString));
		} catch (Exception ee) {
			System.err.println ("Invalid folder: " + folderString);
			return false;
		}

		if (!Files.exists (_rootFolder)) {
			System.err.println ("Invalid folder: " + folderString);
			return false;
		}

		if (thresholdGB >= 0) {
			_threshold = (long) (thresholdGB * GB);
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

		msg += "Usage: " + _AppName + " [/debug] [/quiet] [/threshold <threshold in gigabytes>] <folder> [/threads <number>] [/queueSize <number>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		System.out.println ("Root folder: " + VendoUtils.getRealPathString (_rootFolder));
		System.out.println ("Threshold: " + VendoUtils.unitSuffixScale (_threshold));

		Instant startInstant = Instant.now ();

		BlockingQueue<Path> queue = new ArrayBlockingQueue<Path> (_queueSize);
		List<Thread> threads = new ArrayList<Thread> ();
		AtomicInteger numMatchingFolders = new AtomicInteger (0);

		//start enumerator thread
		Thread enumeratorThread = new Thread (new DiskEnumerationTask (queue, _rootFolder));
		enumeratorThread.setUncaughtExceptionHandler (new VUncaughtExceptionHandler ());
		enumeratorThread.start ();
		threads.add (enumeratorThread);

		//start usage threads
		for (int ii = 0; ii < _numUsageThreads; ii++) {
			Thread usageThread = new Thread (new DiskUsageTask (queue, _threshold, numMatchingFolders));
			usageThread.setUncaughtExceptionHandler (new VUncaughtExceptionHandler ());
			usageThread.start ();
			threads.add (usageThread);
		}

		//wait for all threads (enumerator and usage) to finish
		for (Thread thread : threads) {
			try {
				thread.join ();

			} catch (Exception ee) {
				ee.printStackTrace ();
			}
		}

		System.out.println (NL + "Found " + numMatchingFolders.get () + " folders that exceed the threshold");

//		System.out.println (Duration.between (startInstant, Instant.now ())); //default ISO-8601 seconds-based representation
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format(_dateTimeFormatter));

		if (_Debug) {
			_log.debug ("DiskUsage.run: exiting");
		}

		return true;
	}


	//private members
	private Path _rootFolder;
	private long _threshold = 1 * GB; //default to 1 GB
	private int _queueSize = 50;
	private int _numUsageThreads = 50; //note there is also one enumerate thread

//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)

	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static final Path DONE_MARKER = FileSystems.getDefault ().getPath (""); //used to indicate last entry

	public static boolean _Quiet = false;
	public static boolean _Debug = false;

	public static final String _AppName = "DiskUsage";
	public static final String NL = System.getProperty ("line.separator");
	public static final long GB = 1024 * 1024 * 1024;
}
