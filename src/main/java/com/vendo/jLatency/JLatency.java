package com.vendo.jLatency;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public final class JLatency
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new UncaughtExceptionHandler () {
			@Override
			public void uncaughtException (Thread thread, Throwable ex)
			{
				_log.error ("JLatency UncaughtExceptionHandler: ", ex);
				Thread.currentThread ().interrupt ();
			}
		});
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		JLatency app = new JLatency ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

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

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("cycle") || arg.equalsIgnoreCase ("c")) {
					try {
						_cycleInMinutes = Integer.parseInt(args[++ii]);
						if (_cycleInMinutes < 0) {
							throw (new NumberFormatException());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase("port") || arg.equalsIgnoreCase("p")) {
					try {
						_port = Integer.parseInt(args[++ii]);
						if (_port < 0) {
							throw (new NumberFormatException());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("url") || arg.equalsIgnoreCase ("u")) {
					try {
						_url = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args and handle defaults
		if (_destDir == null) {
			_destDir = "./";
		}

		if (_url == null) {
			_url = "www.google.com";
		}

		if (_port == 0) {
			_port = 80;
		}

		if (_historyFilename == null) {
			String timestamp = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
			_historyFilename = "jLatency." + _url + "." + timestamp + ".csv";
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/url <URL>] [/port <port>] [/cycle <cycle in minutes]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () {
		if (_Debug) {
			_log.debug("JLatency.run");
		}

		TimerTask repeatedTask = new TimerTask() {
			@Override
			public void run() {
				String timestamp = new SimpleDateFormat ("yyyy/MM/dd HH:mm:ss").format (new Date ());
				double milliSeconds = getLatencyInMilliseconds ();
				if (milliSeconds > 0.) {
					String record = timestamp + "," + _url + "," + milliSeconds;
					writeHistory(record);
				}
			}
		};

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor ();
		long delay = 0L;
		long period = _cycleInMinutes * 60L * 1000L;
		executor.scheduleAtFixedRate (repeatedTask, delay, period, TimeUnit.MILLISECONDS);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private double getLatencyInMilliseconds () {
		double milliSeconds = -1.;

		final Instant startInstant = Instant.now ();

		Socket socket = new Socket ();
		SocketAddress address = new InetSocketAddress (_url, _port);
		try {
			socket.connect (address, _timeoutInMillis);
			Duration duration = Duration.between (startInstant, Instant.now ());
			milliSeconds = (double) duration.toNanos () / 1e6;

		} catch (SocketTimeoutException ex) {
			_log.error("getLatencyInMilliseconds: socket timeout");
			_log.error(ex); //print exception, but no stack trace
		} catch (IOException ex) {
			_log.error("getLatencyInMilliseconds: error");
			_log.error(ex); //print exception, but no stack trace
		}

		try {
			socket.close();
		} catch (IOException ex) {
			// closing failed
		}

		return milliSeconds;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeHistory(String line) {
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream (new File (_destDir + _historyFilename), true);

		} catch (IOException ee) {
			_log.error("writeHistory: error opening output file \"" + _historyFilename + "\"");
			_log.error(ee); //print exception, but no stack trace
			return false;
		}

		line += NL;
		try {
			outputStream.write (line.getBytes ());
			outputStream.flush ();
			outputStream.close ();

		} catch (IOException ex) {
			_log.error("writeHistory: error writing to output file \"" + _historyFilename + "\"");
			_log.error(ex); //print exception, but no stack trace
			return false;
		}

		return true;
	}


	//private members
//	private static boolean _trace = false;
	private String _historyFilename = null;
	private String _destDir = null;
	private String _url = null;
	private int _port;
	private int _timeoutInMillis = 5000;
	private int _cycleInMinutes;

	private static Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;
	public static boolean _TestMode = false;

	public static final String _AppName = "JLatency";
	public static final String NL = System.getProperty ("line.separator");
}
