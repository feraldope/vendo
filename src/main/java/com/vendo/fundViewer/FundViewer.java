//FundViewer.java

/*
	This parses the file that results from opening this link,
	clicking All (the link marked "(may be slower to load)")
	and doing a SaveAs (note: requires javascript):

		http://www.morningstar.com/allanalyses/analysesLists.html?type=FO

	Then process the resulting HTML file into CSV like this:

		jr analysesLists.html analysesLists.csv

	Then run anZip to create a zip backup
*/

package com.vendo.fundViewer;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.vendo.vendoUtils.TableSorter;

public class FundViewer extends JPanel
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		FundViewer fundViewer = new FundViewer ();

		if (!fundViewer.processArgs (args))
			System.exit (1); //processArgs displays error

		fundViewer.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	public FundViewer ()
	{
		super (new GridLayout (1, 0));
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d"))
					_Debug = true;

				else
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);

			} else {
				//check for other args
				if (_inFilename == null)
					_inFilename = arg;

				else if (_outFilename == null)
					_outFilename = arg;

				else
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//check for required args
		if (_inFilename == null)
			displayUsage ("No input filename specified", true);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL + NL;

		msg += "Usage: " + _AppName + " [/debug] <input file> [<output file>]";
		JOptionPane.showMessageDialog (null, msg, "Error", JOptionPane.ERROR_MESSAGE);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		String title = _AppName + " V1.05 - " + _inFilename;

//		JFrame.setDefaultLookAndFeelDecorated (true);

		JFrame frame = new JFrame (title);
		frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		FundRecords records = new FundRecords ();
		if (records.readFile (_inFilename) == 0)
			System.exit (1);					//readFile () writes any error

		records.sort ();

		if (_outFilename != null)
			records.writeFile (_outFilename);	//writeFile () writes any error

		TableSorter sorter = new TableSorter (records);
		JTable table = new JTable (sorter);
		sorter.setTableHeader (table.getTableHeader ());
		table.setPreferredScrollableViewportSize (new Dimension (800, 800));
		records.initTableSorter (sorter);

		table.getTableHeader ().setToolTipText ("Click to specify sorting; Control-Click to specify secondary sorting");

		JScrollPane scrollPane = new JScrollPane (table);
		add (scrollPane);

		setOpaque (true);
		frame.setContentPane (this);
		frame.pack ();
		frame.setVisible (true);
	}

	//private members
	private String _inFilename = null;
	private String _outFilename = null;

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "FundViewer";
	public static final String NL = System.getProperty ("line.separator");
}
