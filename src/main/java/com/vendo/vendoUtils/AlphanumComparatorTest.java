// AlphanumComparatorTest.java

package com.vendo.vendoUtils;

//import java.io.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

//import org.apache.logging.log4j.*;


public class AlphanumComparatorTest
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (final String args[]) throws Exception
	{
		AlphanumComparatorTest app = new AlphanumComparatorTest ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () throws Exception
	{
		//this collection holds Strings, to test sorting of simple String comparator
		ArrayList<String> coll1 = new ArrayList<> ();

		//this collection holds an Object, to test sorting of AlphanumComparator
		ArrayList<TestRecord> coll2 = new ArrayList<> ();

		Random random = new Random ();

		int size = 15;
		for (int ii = 0; ii < size; ii++) {
	        int jj = random.nextInt (size);
			String base = ((ii & 1) == 0 ? "Name" : "name");
			String name = String.format ("%s%d.txt", base, jj);
			TestRecord record = new TestRecord (name, size - jj);

			//load both collections with similar data
			coll1.add (name);
			coll2.add (record);
		}

		System.out.println ("Original order:");
		for (String str : coll1) {
			System.out.println (str);
		}

		System.out.println ("");

		//sort once alphabetically
		Collections.sort (coll1, (s1, s2) -> s1.compareToIgnoreCase (s2));

		System.out.println ("Simple sort (case insensitive):");
		for (String str : coll1) {
			System.out.println (str);
		}

		System.out.println ("");

		//sort again with number-aware sorter
		Collections.sort (coll2, new AlphanumComparator ());

		System.out.println ("Better Sort (number-aware, case insensitive):");
		for (TestRecord record : coll2) {
			System.out.println (record);
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

//		msg += "Usage: " + _AppName + " [/debug] {/compress | /decompress} [/inFile] <input file> [/outFile] <output file>";
		msg += "Usage: TBD";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
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

/*
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
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_inFilename == null) {
					_inFilename = arg;

				} else if (_outFilename == null) {
					_outFilename = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args
/*
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
*/

		return true;
	}

	private class TestRecord
	{
		///////////////////////////////////////////////////////////////////////////
		//some object that contains something other than a String
		public TestRecord (String name, int number)
		{
			_name = name;
			_number = number;
		}

		///////////////////////////////////////////////////////////////////////////
		//required for AlphanumComparator
		@Override
		public String toString ()
		{
			return _name;
		}

/* I thought these might be required for AlphanumComparator, but apparently not
		///////////////////////////////////////////////////////////////////////////
		public int compareTo (TestRecord record)
		{
//			return _name.compareTo (record._name);
			return _name.compareToIgnoreCase (record._name);
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean isEquals (TestRecord record)
		{
//			return (_name.compareTo (record._name) == 0);
			return (_name.compareToIgnoreCase (record._name) == 0);
		}
*/

		public String _name;
		public int _number;
	}

	//private members
//	private Mode _mode = Mode.NotSet;
//	private String _inFilename = null;
//	private String _outFilename = null;

//	private static Logger _log = LogManager.getLogger (AlphanumComparatorTest.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "AlphanumComparatorTest";
	public static final String NL = System.getProperty ("line.separator");
}
