// VZipperTest.java

package com.vendo.vendoUtils;

//import java.io.*;
//import java.util.*;

import org.apache.logging.log4j.*;


public class VZipperTest
{
	private enum Mode {NotSet, Compress, Decompress};

	///////////////////////////////////////////////////////////////////////////
	public static void main (final String args[]) throws Exception
	{
		VZipperTest app = new VZipperTest ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () throws Exception
	{
		VZipper zipper = new VZipper ();

		try {
			if (_mode == Mode.Compress)
				zipper.compressFile (_inFilename, _outFilename);
			else
				zipper.decompressFile (_inFilename, _outFilename);

		} catch (Exception ee) {
			_log.error ("Error in processing", ee);
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

		msg += "Usage: " + _AppName + " [/debug] {/compress | /decompress} [/inFile] <input file> [/outFile] <output file>";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
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
					VZipper._Debug = true;

				} else if (arg.equalsIgnoreCase ("comp") || arg.equalsIgnoreCase ("compress")) {
					_mode = Mode.Compress;

				} else if (arg.equalsIgnoreCase ("decomp") || arg.equalsIgnoreCase ("decompress")) {
					_mode = Mode.Decompress;

				} else if (arg.equalsIgnoreCase ("in") || arg.equalsIgnoreCase ("inFile")) {
					try {
						_inFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("out") || arg.equalsIgnoreCase ("outFile")) {
					try {
						_outFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_inFilename == null) {
					_inFilename = arg;

				} else if (_outFilename == null) {
					_outFilename = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args
		if (_mode == Mode.NotSet)
			displayUsage ("No action specified", true);

		if (_inFilename == null)
			displayUsage ("No input file specified", true);

		if (_outFilename == null)
			displayUsage ("No output file specified", true);

		if (_inFilename.equalsIgnoreCase (_outFilename))
			displayUsage ("Input and output files can not be the same", true);

		if (_Debug) {
			System.out.println ("_inFilename = '" + _inFilename + "'");
			System.out.println ("_outFilename = '" + _outFilename + "'");
		}

		return true;
	}

	//private members
	private Mode _mode = Mode.NotSet;
	private String _inFilename = null;
	private String _outFilename = null;

	private static Logger _log = LogManager.getLogger (VZipperTest.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "VZipperTest";
	public static final String NL = System.getProperty ("line.separator");
}
