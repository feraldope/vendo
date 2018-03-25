//ReserViewer.java

package com.vendo.reserViewer;

import java.awt.Dimension;

import javax.swing.JOptionPane;

//public class ReserViewer extends JPanel
public class ReserViewer
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		ReserViewer reserViewer = new ReserViewer ();

		if (!reserViewer.processArgs (args))
			System.exit (1); //processArgs displays error

		reserViewer.run ();
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

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_filename == null) {
					_filename = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//handle defaults
		if (_filename == null)
			_filename = "MyHDResCapList.mrl";

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL + NL;

		msg += "Usage: " + _AppName + " [/debug] <reservation input file>";
		JOptionPane.showMessageDialog (null, msg, "Error", JOptionPane.ERROR_MESSAGE);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		String title = _AppName + " V2.12 - " + _filename;
		if (ReserViewer._Debug)
			title += "     --- DEBUG MODE ---";

		ReserAppFrame appFrame = new ReserAppFrame (title, getClassName (), _filename);

		ReserRecords records = new ReserRecords ();

		try {
			records.readFile (_filename);

		} catch (ReserException exception) {
			JOptionPane.showMessageDialog (null, exception.getMessage (), "Error", JOptionPane.ERROR_MESSAGE);
			System.exit (1);
		}

		appFrame.setRecords (records);

		appFrame.init ();

		Dimension size = appFrame.getPreferredSize ();
		int width = (int) size.getWidth ();
		int height = (int) size.getHeight ();
		appFrame.setSize (width, height);

		appFrame.setVisible (true);
	}

	///////////////////////////////////////////////////////////////////////////
	private String getClassName ()
	{
		String[] array = getClass ().toString ().split (" ");
		return array[1];
	}

	//private members
	private String _filename = null;

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "ReserViewer";
	public static final String NL = System.getProperty ("line.separator");
}
