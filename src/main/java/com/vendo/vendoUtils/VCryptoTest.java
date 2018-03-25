// VCryptoTest.java

package com.vendo.vendoUtils;

//import java.io.*;
//import java.util.*;

import org.apache.logging.log4j.*;

import java.security.InvalidKeyException;


public class VCryptoTest
{
	private enum Mode {NotSet, Encrypt, Decrypt};

	///////////////////////////////////////////////////////////////////////////
	public static void main (final String args[]) throws Exception
	{
		VCryptoTest app = new VCryptoTest ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () throws Exception
	{
		VCrypto crypto = new VCrypto ();

		try {
			if (_mode == Mode.Encrypt)
				crypto.encryptFile (_inFilename, _outFilename);
			else
				crypto.decryptFile (_inFilename, _outFilename);

		} catch (InvalidKeyException ee) {
			//this happens when the security policy jars in "%JAVA_HOME%\jre\lib\security" are incorrect
			//go to http://java.sun.com/javase/downloads/index.jsp and download jce_policy-6.zip
			//JCE Unlimited Strength Jurisdiction Policy Files 6 Release Candidate
			_log.error ("run: error: Illegal key size or default parameters");
			_log.error ("The security policy jars are not compatible", ee);

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

		msg += "Usage: " + _AppName + " [/debug] {/encrypt | /decrypt} [/inFile] <input file> [/outFile] <output file>";
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
					VCrypto._Debug = true;

				} else if (arg.equalsIgnoreCase ("en") || arg.equalsIgnoreCase ("encrypt")) {
					_mode = Mode.Encrypt;

				} else if (arg.equalsIgnoreCase ("de") || arg.equalsIgnoreCase ("decrypt")) {
					_mode = Mode.Decrypt;

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

	private static Logger _log = LogManager.getLogger (VCryptoTest.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "VCryptoTest";
	public static final String NL = System.getProperty ("line.separator");
}
