//VXmlFormatHandlerTest.java

package com.vendo.vendoUtils;

import java.io.*;

import org.xml.sax.*;
import javax.xml.parsers.*;

import org.apache.logging.log4j.*;


public class VXmlFormatHandlerTest
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (final String args[])
	{
		VXmlFormatHandlerTest app = new VXmlFormatHandlerTest ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		String inFilename = null;
		String outFilename = null;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("in") || arg.equalsIgnoreCase ("inFile")) {
					try {
						inFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("out") || arg.equalsIgnoreCase ("outFile")) {
					try {
						outFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (inFilename == null) {
					inFilename = arg;

				} else if (outFilename == null) {
					outFilename = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		if (inFilename == null) {
			_inStream = new InputStreamReader (System.in);

		} else {
			try {
				_inStream = new InputStreamReader (new BufferedInputStream (new FileInputStream (new File (inFilename))));

			} catch (Exception ee) {
				System.err.println ("Invalid input file: " + inFilename);
				return false;
			}
		}

		if (outFilename == null) {
			_outStream = new OutputStreamWriter (System.out);

		} else {
			try {
				_outStream = new OutputStreamWriter (new BufferedOutputStream (new FileOutputStream (new File (outFilename))));

			} catch (Exception ee) {
				System.err.println ("Invalid output file: " + outFilename);
				return false;
			}
		}

		if (inFilename != null && outFilename != null && inFilename.equalsIgnoreCase (outFilename)) {
			displayUsage ("Input and output files can not be the same", true);
		}

		if (_Debug) {
			if (inFilename != null) {
				System.out.println ("inFilename = '" + inFilename + "'");
			}
			if (outFilename != null) {
				System.out.println ("outFilename = '" + outFilename + "'");
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] [/inFile <input file>] [/outFile <output file>]";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		PrintWriter printWriter = null;
		boolean status = false;

		try {
			printWriter = new PrintWriter (_outStream);
			VXmlFormatHandler vXmlFormatHandler = new VXmlFormatHandler (printWriter);

			SAXParser parser = SAXParserFactory.newInstance ().newSAXParser ();
			parser.setProperty ("http://xml.org/sax/properties/lexical-handler", vXmlFormatHandler);
			parser.parse (new InputSource (_inStream), vXmlFormatHandler); //reads input and writes output at same time
			status = true;

		} catch (SAXParseException ee) {
			_log.error ("Parsing error: line " + ee.getLineNumber () + ", uri " + ee.getSystemId ());

		} catch (Exception ee) {
			_log.error ("Error in processing", ee);

		} finally {
			if (printWriter != null) {
				printWriter.flush ();
				printWriter.close ();
			}
		}

		return status;
	}

	//private members
	private InputStreamReader _inStream = null;
	private OutputStreamWriter _outStream = null;

	private static Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "VXmlFormatHandlerTest";
	public static final String NL = System.getProperty ("line.separator");
}
