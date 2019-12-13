//Original inspiration from http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html

package com.vendo.fillDisk;

import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;


public class FillDisk
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		FillDisk app = new FillDisk ();

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

				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("thresholdPct") || arg.equalsIgnoreCase ("t")) {
					try {
						_thresholdPercent = Double.parseDouble (args[++ii]);
						if (_thresholdPercent < 0) {
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
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args and handle defaults
		if (_destDir == null) {
			displayUsage ("Must specify destination drive/folder", true);

		} else if (!VendoUtils.fileExists (_destDir) || !VendoUtils.isDirectory (_destDir)) {
			displayUsage ("Invalid destination folder: '" + _destDir + "'", true);
		}

		if (_thresholdPercent < 0) {
			displayUsage ("Must specify disk full threshold", true);
		}

		if (_filenamePrefix == null) {
			_filenamePrefix = "tmp";
		}

		if (_Debug) {
			_log.debug ("_destDir: " + _destDir);
			_log.debug ("_thresholdPercent: " + _thresholdPercent);
			_log.debug ("_filenamePrefix: " + _filenamePrefix);
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

		msg += "Usage: " + _AppName + " [/debug] [TBD]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		if (_Debug) {
			_log.debug ("FillDisk.run");

			_log.debug ("Free bytes:   " + _decimalFormat1.format (VendoUtils.getFreeDiskSpace (_destDir)));
			_log.debug ("Total bytes:  " + _decimalFormat1.format (VendoUtils.getTotalDiskSpace (_destDir)));
			_log.debug ("Percent Full: " + _decimalFormat2.format (VendoUtils.getDiskPercentFull (_destDir)));
		}

		System.out.println ("Percent full: " + _decimalFormat2.format (VendoUtils.getDiskPercentFull (_destDir)));

		int filesWritten = 0;
		Instant startInstant = Instant.now ();
		try {
			byte bits = (byte) 0xAA;
			ByteBuffer buffer = generateByteBuffer (bits, _byteBufferSize);

			while (VendoUtils.getDiskPercentFull (_destDir) < _thresholdPercent) {
				String filePath = getNextFilepath (_destDir, _filenamePrefix);
				writeFile (filePath, buffer, _count);
				filesWritten++;

				Instant endInstant = Instant.now ();
//				String elapsedTimeString = Duration.between (_startInstant, endInstant).toString (); //default ISO-8601 seconds-based representation
				String elapsedTimeString = LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format(_dateTimeFormatter);

				System.out.print ("Percent full: " + _decimalFormat2.format (VendoUtils.getDiskPercentFull (_destDir)));
				System.out.print (", elapsed time: " + elapsedTimeString);
				System.out.print (" (" + filesWritten + " files written)");
				System.out.println ("");
			}

		} catch (Exception ee) {
//			_log.error ("FillDisk.run: error writing file \"" + filepath + "\"");
			_log.error (ee);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private ByteBuffer generateByteBuffer (byte bits, int numBytes) throws Exception
	{
		ByteBuffer buffer = ByteBuffer.allocate (numBytes);

		for (int ii = 0; ii < numBytes; ii++) {
			buffer.put (bits);
		}

		buffer.rewind (); //always rewind before using

		return buffer;
	}

	///////////////////////////////////////////////////////////////////////////
	private long writeFile (String filepath, ByteBuffer buffer, int count) throws Exception
	{
		if (_Debug) {
			_log.debug ("FillDisk.writeFile: writing file: " + filepath);
		}

		long bytesWritten = 0;
		try (FileChannel fileChannel = new FileOutputStream (filepath).getChannel ()) {
			for (int ii = 0; ii < count; ii++) {
				buffer.rewind (); //always rewind before using
				bytesWritten += fileChannel.write (buffer);
			}

			} catch (Exception ee) {
				_log.error ("FillDisk.writeFile: error writing file \"" + filepath + "\"");
				_log.error (ee);
		}

		return bytesWritten;
	}	

	///////////////////////////////////////////////////////////////////////////
	private String getNextFilepath (String folder, String filenamePrefix)
	{
		final String ext = ".tmp";

		long millis = (new Date ()).getTime ();
		Path file = null;

		do {
			String millisStr = Long.toString (millis);
			file = FileSystems.getDefault ().getPath (folder, filenamePrefix + millisStr + ext);

		} while (Files.exists (file));

		return file.toString ();
	}


	//private members
	private double _thresholdPercent = (-1);
	private String _destDir = null;
	private String _filenamePrefix = null;

	private int _count = 50;
	private int _byteBufferSize = 10 * 1024 * 1024;

//	private static final String _slash = System.getProperty ("file.separator");
	private static Logger _log = LogManager.getLogger ();

//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("HH'h':mm'm'"); //for example: 09h:13m
	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("HH:mm:ss"); //for example: 09:13:15

    private static final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0"); //int
    private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0.00"); //float/double

	//global members
	public static boolean _Debug = false;
//	public static boolean _TestMode = false;

	public static final String _AppName = "FillDisk";
	public static final String NL = System.getProperty ("line.separator");
}
