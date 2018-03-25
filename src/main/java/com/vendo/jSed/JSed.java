package com.vendo.jSed;

/* Originally developed to address RTC Defect 318466: TELKOM SAF - 21850523-1 - TZ issue? Planned Downtime Does Not Work

REM build without ant/my build environment (note "package" line at top of JSed.java must be commented out)
"C:\Program Files\Java\jdk1.7.0_51\bin\javac.exe" JSed.java

REM run (runs JSed.class in current/source folder, not one under build)
java -cp . JSed reports.orig.txt "tz est5edt" "tz sast-2Johannesburg"
$NH_HOME/jre/bin/java -cp . JSed reports.orig.txt "tz est5edt" "tz sast-2Johannesburg"

*/

import java.io.*;
//import java.net.*;
import java.nio.file.*;
//import java.text.*;
import java.util.*;

//import org.apache.logging.log4j.*;


public class JSed
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		JSed app = new JSed ();

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

					//dump all args
					System.err.println ("args.length = " + args.length);
					for (int jj = 0; jj < args.length; jj++)
						System.err.println ("args[" + jj + "] = '" + args[jj] + "'");

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_inName == null) {
					_inName = arg;

				} else if (_oldString == null) {
					_oldString = arg;

				} else if (_newString == null) {
					_newString = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_inName == null)
			displayUsage ("No input file name specified", true);

		if (_oldString == null)
			displayUsage ("No old string specified", true);

		if (_newString == null)
			displayUsage ("No new string specified", true);

		if (!fileExists (_inName))
			displayUsage ("Input file not found: " + _inName, true);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] <input file name> <old string> <new string>";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		BufferedReader reader;
		try {
			reader = new BufferedReader (new FileReader (_inName));

		} catch (IOException ee) {
			System.err.println ("JSed.run: error opening input file \"" + _inName + "\"");
			System.err.println (ee);
			return false;
		}

		Vector<String> fileContents = new Vector<String> ();
		try {
			String line = new String ();
			while ((line = reader.readLine ()) != null)
				fileContents.add (line);

		} catch (IOException ee) {
			System.err.println ("JSed.run: error reading input file \"" + _inName + "\"");
			System.err.println (ee);
			return false;
		} finally {
			try {
				reader.close ();
			} catch (Exception ee) {
				//ignore
			}
		}

		int numLines = fileContents.size ();
		for (int ii = 0; ii < numLines; ii++) {
			String baseLine = fileContents.get (ii);
			String parts[] = baseLine.split ("\\|");

			if (parts.length == 2) {
				String jobId = parts[0];
				String stepArgs = parts[1];

				stepArgs = stepArgs.replace (_oldString, _newString);
				stepArgs = stepArgs.replace ("'", "''"); //escape single quotes for Oracle

				//create SQL update command from components
				String update = "update nh_job_step set step_args='";
				update += stepArgs;
				update += "' where job_id=";
				update += jobId;
				update += ";";

				System.out.println (update);

			} else { //parts.length != 2
				if (_Debug)
					System.err.println ("JSed.run: unexpected line: " + baseLine);
//					System.err.println ("JSed.run: parts.length: " + parts.length);
//					System.err.println ("JSed.run: parts[0]: " + parts[0]);
//					System.err.println ("JSed.run: parts[1]: " + parts[1]);
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean fileExists (String filename)
	{
		Path file = FileSystems.getDefault ().getPath (filename);

		return Files.exists (file);
	}


	//private members
	private String _inName = null;
	private String _oldString = null;
	private String _newString = null;

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "JSed";
	public static final String NL = System.getProperty ("line.separator");
}
