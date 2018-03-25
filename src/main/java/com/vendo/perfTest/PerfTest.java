//PerfTest.java

package com.vendo.perfTest;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vendo.vendoUtils.VendoUtils;


public class PerfTest
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		PerfTest app = new PerfTest ();

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

				} else if (arg.equalsIgnoreCase ("count") || arg.equalsIgnoreCase ("c")) {
					try {
						_count = Integer.parseInt (args[++ii]);
						if (_count < 0)
							throw (new NumberFormatException ());
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

				} else if (_prefix == null) {
					_prefix = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args
		// -- none --

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] [/count <count>]";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
//		String folderRead  = "E:/Netscape/Program/jroot/a";
		String folderRead  = "/Windows/system32";

		String folderWrite = "/temp/testFiles";
		int numFilesToCreate = 60 * 1000;

//		testSleep ();
		testDateTimeFunctions ();
//		testRotateFunctions ();
//		testPatternMatching ();

//		testFileFunctions (folderWrite, int numFilesToCreate);
//		testListFiles (folderRead);
//		testFileExists (folderRead);
//		testFileGetAttrs (folderRead);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//how long does it take to get folder listing
	public void testListFiles (String folderRead)
	{
		System.out.println ("testListFiles, folderRead = \"" + folderRead + "\"");

		{ //using java.io package
		long startNano = System.nanoTime ();

		String files[] = listFiles1 (folderRead);
		int numFiles = files.length;

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = "found " + numFiles + " files";
		System.out.println ("using java.io package  (" + message + "): elapsed: " + elapsedMillis + " ms");
		}


		{ //using java.nio package
		long startNano = System.nanoTime ();

		Path path = FileSystems.getDefault ().getPath (folderRead);
		List<Path> files = listFiles2 (path);
		int numFiles = files.size ();

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = "found " + numFiles + " files";
		System.out.println ("using java.nio package (" + message + "): elapsed: " + elapsedMillis + " ms");
		}

		System.out.println ("");
	}

	///////////////////////////////////////////////////////////////////////////
	//how long does it take to get file information from a set of files
	public void testFileExists (String folderRead)
	{
		System.out.println ("testFileExists, folderRead = \"" + folderRead + "\"");

		{ //using java.io package
		long startNano = System.nanoTime ();

		String files[] = listFiles1 (folderRead);
		int numFiles = files.length;

		int numChecked = 0;
		for (int ii = 0; ii < numFiles; ii++) {
			if (fileExists1 (folderRead, files[ii]))
				numChecked++;
		}

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = "checked " + numChecked + " files";
		System.out.println ("using java.io package  (" + message + "): elapsed: " + elapsedMillis + " ms");
		}


		{ //using java.nio package
		long startNano = System.nanoTime ();

		Path path = FileSystems.getDefault ().getPath (folderRead);
		List<Path> files = listFiles2 (path);

		int numChecked = 0;
		for (Path file : files) {
			String filename = file.toAbsolutePath ().toString ();
			if (fileExists2 (filename))
				numChecked++;
		}

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = "checked " + numChecked + " files";
		System.out.println ("using java.nio package (" + message + "): elapsed: " + elapsedMillis + " ms");
		}

		System.out.println ("");
	}

	///////////////////////////////////////////////////////////////////////////
	//how long does it take to get file information from a set of files
	public void testFileGetAttrs (String folderRead)
	{
		System.out.println ("testFileGetAttrs, folderRead = \"" + folderRead + "\"");

		{ //using java.io package
		long startNano = System.nanoTime ();

		String files[] = listFiles1 (folderRead);
		int numFiles = files.length;

		int numChecked = 0;
		for (int ii = 0; ii < numFiles; ii++) {
			if (fileAttrs1 (folderRead, files[ii]))
				numChecked++;
		}

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = "checked " + numChecked + " files";
		System.out.println ("using java.io package  (" + message + "): elapsed: " + elapsedMillis + " ms");
		}


		{ //using java.nio package
		long startNano = System.nanoTime ();

		Path path = FileSystems.getDefault ().getPath (folderRead);
		List<Path> files = listFiles2 (path);

		int numChecked = 0;
		for (Path file : files) {
			String filename = file.toAbsolutePath ().toString ();
			if (fileAttrs2 (filename))
				numChecked++;
		}

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = "checked " + numChecked + " files";
		System.out.println ("using java.nio package (" + message + "): elapsed: " + elapsedMillis + " ms");
		}

		System.out.println ("");
	}

	///////////////////////////////////////////////////////////////////////////
	public void testFileFunctions (String folderWrite, int numFilesToCreate)
	{
		System.out.println ("testFileFunctions");
		System.out.println ("creating " + numFilesToCreate + " files");

		{
		long startNano = System.nanoTime ();

		int numFilesCreated = createTestFiles (folderWrite, numFilesToCreate);

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		String message = numFilesCreated + "/" + numFilesToCreate;
		System.out.println ("createTestFiles (" + message + "): elapsed: " + elapsedMillis + " ms");
		}

		{
		long startNano = System.nanoTime ();

		listFiles1 (folderWrite);

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println ("listFiles1: elapsed: " + elapsedMillis + " ms");
		}

		System.out.println ("");
	}

	///////////////////////////////////////////////////////////////////////////
	public int createTestFiles (String folderWrite, int numFilesToCreate)
	{
		int numFilesCreated = 0;
		int fileSize = 512;

		if (!fileExists1 (folderWrite)) {
			if (!createFolder (folderWrite)) {
				System.out.println ("Error: failed to create folder '" + folderWrite + "'\n");
				return 0;
			}
		}

		for (int ii = 0; ii < numFilesToCreate; ii++) {
			String filename = folderWrite + "/" + String.format ("perfTest.%06d.tmp", ii);
			if (!fileExists1 (folderWrite, filename))
				if (createFile (filename, fileSize))
					numFilesCreated++;
 		}

		System.out.println (numFilesCreated + " of " + numFilesToCreate + " files created\n");

		return numFilesCreated;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.io package
	private String[] listFiles1 (String folderRead)
	{
		String files[] = new String[] {};

		try {
			File file = new File (folderRead);
			files = file.list ();

		} catch (Exception ee) {
			System.out.println ("listFiles1 (" + folderRead + ") failed: ");
			System.out.println (ee);
		}

		return files;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.nio package
	private List<Path> listFiles2 (Path folderRead)
	{
		List<Path> result = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream (folderRead)) {
			for (Path entry : stream) {
				result.add (entry);
			}

		} catch (Exception ee) {
			System.out.println ("listFiles2 (" + folderRead + ") failed: ");
			System.out.println (ee);
		}

		return result;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.io package
	private boolean fileExists1 (String filename)
	{
		try {
			File file = new File (filename);
			if (file.exists ())
				return true;

		} catch (Exception ee) {
			//assume file does not exist (fall through)
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.io package
	private boolean fileExists1 (String folderRead, String filename)
	{
		try {
			File file = new File (folderRead, filename);
			if (file.exists ())
				return true;

		} catch (Exception ee) {
			//assume file does not exist (fall through)
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.nio package
	private boolean fileExists2 (String filename)
	{
		Path path = FileSystems.getDefault ().getPath (filename);

//		return Files.isReadable (path);
		return Files.exists (path);
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.nio package
	private boolean fileExists2 (String folderRead, String filename)
	{
		Path path = FileSystems.getDefault ().getPath (folderRead, filename);

//		return Files.isReadable (path);
		return Files.exists (path);
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.io package
	private boolean fileAttrs1 (String folderRead, String filename)
	{
		try {
			File file = new File (folderRead, filename);
			long bytes = file.length ();
			long modified = file.lastModified (); //millisecs

			return true;

		} catch (Exception ee) {
			//assume file does not exist (fall through)
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.nio package
	private boolean fileAttrs2 (/*String folderRead,*/ String filename)
	{
		Path path = FileSystems.getDefault ().getPath (/*folderRead,*/ filename);

		try {
			BasicFileAttributes attrs = Files.readAttributes (path, BasicFileAttributes.class);
			long bytes = attrs.size ();
			long modified = attrs.lastModifiedTime ().toMillis (); //millisecs

			return true;

		} catch (Exception ee) {
			//assume file does not exist (fall through)
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean createFile (String filename, int size)
	{
		try {
			FileOutputStream out = new FileOutputStream (filename);

			byte bytes[] = new byte [size];

			out.write (bytes, 0, size);
			out.close ();

		} catch (Exception ee) {
			System.out.println ("createFile (" + filename + ") failed: ");
			System.out.println (ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean createFolder (String folderWrite)
	{
		try {
			new File (folderWrite).mkdirs ();

		} catch (Exception ee) {
			System.out.println ("createFolder (" + folderWrite + ") failed: ");
			System.out.println (ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//which method of measuring elapsed time adds the least overhead?
	public void testDateTimeFunctions ()
	{
		System.out.println ("testDateTimeFunctions: count = " + _count);

		System.out.println ("Elapsed (ms)\tMethod");
		System.out.println ("-------------------------------------");

		//test using Calendar.getInstance
		double dummy = 0;
		long startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			dummy += Calendar.getInstance ().getTimeInMillis ();
		}
		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tCalendar.getInstance ().getTimeInMillis ()");

		//test using new GregorianCalendar
		startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			dummy += (new GregorianCalendar ()).getTimeInMillis ();
		}
		elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tnew GregorianCalendar ()).getTimeInMillis ()");

		//test using new Date
		startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			dummy += (new Date ()).getTime ();
		}
		elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tnew Date ()).getTime ()");

		//test using System.nanoTime
		startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			dummy += System.nanoTime ();
		}
		elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tSystem.nanoTime ()");

		//test using Instant.now
		startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			dummy += Instant.now ().toEpochMilli();
		}
		elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tInstant.now ()");

		System.out.println ("");
	}

	///////////////////////////////////////////////////////////////////////////
	//how long does a sleep of 1 nanosecond really last?
	public void testSleep ()
	{
		System.out.println ("testSleep");

		long minNano = Long.MAX_VALUE;
		long maxNano = 0;
		long totalNano = 0;

		for (int ii = 0; ii < _count; ii++) {
			long elapsedNano = testSleep1 (1);

			totalNano += elapsedNano;
			if (elapsedNano < minNano)
				minNano = elapsedNano;
			if (elapsedNano > maxNano)
				maxNano = elapsedNano;
		}
		long averageNano = totalNano / _count;

		System.out.println ("min = " + formatDouble3 ((double) minNano / 1000000.) + " ms");
		System.out.println ("max = " + formatDouble3 ((double) maxNano / 1000000.) + " ms");
		System.out.println ("avg = " + formatDouble3 ((double) averageNano / 1000000.) + " ms");
	}

	///////////////////////////////////////////////////////////////////////////
	public long testSleep1 (int nano)
	{
		long startNano = System.nanoTime ();

		try {
			Thread.sleep (0, nano);
		} catch (Exception ee) {
			System.out.println ("PerfTest.testSleep1 - Thread.sleep failed");
			System.out.println (ee);
		}

		long elapsedNano = System.nanoTime () - startNano;

		return elapsedNano;
	}

	///////////////////////////////////////////////////////////////////////////
	public void testPatternMatching ()
	{
		System.out.println ("testPatternMatching: count = " + _count);
		System.out.println ("");

		testPatternMatching1 ("foobar*", "foobar");
		testPatternMatching1 ("foobar*", "foobar.ext");
		testPatternMatching1 ("foo*", "foobar");
		testPatternMatching1 ("foo*", "foobar.ext");
		testPatternMatching1 ("f*", "foobar");
		testPatternMatching1 ("f*", "foobar.ext");
	}

	///////////////////////////////////////////////////////////////////////////
	public void testPatternMatching1 (String patternString, String searchString)
	{
		System.out.println ("testPatternMatching1: patternString = " + patternString + ", searchString = " + searchString);
		System.out.println ("");

		System.out.println ("Elapsed (ms)\tMethod");
		System.out.println ("-------------------------------------");

		//VendoUtils.matchPattern
		int matched = 0;
		long startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			if (VendoUtils.matchPattern (searchString, patternString))
				matched++;
		}
		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tVendoUtils.matchPattern ()" + " - " + matched + " matches");

		//adjust pattern for regex
		patternString = patternString.replace ("*", ".*");

		//Pattern.matches
		matched = 0;
		startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			if (Pattern.matches(patternString, searchString)) //non-compiled
				matched++;
		}
		elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tPattern.matches ()" + " - " + matched + " matches");

		//Matcher.matches
		final Pattern pattern = Pattern.compile (patternString);

		matched = 0;
		startNano = System.nanoTime ();
		for (int ii = 0; ii < _count; ii++) {
			Matcher matcher = pattern.matcher (searchString);
			if (matcher.matches ())
				matched++;
		}
		elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println (elapsedMillis + "\t\tMatcher.matches ()" + " - " + matched + " matches");

		System.out.println ("");
	}

	///////////////////////////////////////////////////////////////////////////
	public String formatDouble3 (double value)
	{
		return String.format ("%.3f", value);
	}

	///////////////////////////////////////////////////////////////////////////
	//how long do various 3D rotate methods take?
	public void testRotateFunctions ()
	{
		System.out.println ("testRotateFunctions");

		Point2D point = new Point2D.Double (.5, .5);

		long startMillis = (new Date ()).getTime ();
		for (int ii = 0; ii < _count; ii++) {
			point = rotate1 (point, .5);
		}
		long elapsedMillis = (new Date ()).getTime () - startMillis;
		System.out.println ("using rotate1: elapsed: " + elapsedMillis + " ms");

		startMillis = (new Date ()).getTime ();
		for (int ii = 0; ii < _count; ii++) {
			point = rotate2 (point, .5);
		}
		elapsedMillis = (new Date ()).getTime () - startMillis;
		System.out.println ("using rotate2: elapsed: " + elapsedMillis + " ms");

		startMillis = (new Date ()).getTime ();
		for (int ii = 0; ii < _count; ii++) {
			point = rotate3 (point, .5);
		}
		elapsedMillis = (new Date ()).getTime () - startMillis;
		System.out.println ("using rotate3: elapsed: " + elapsedMillis + " ms");
	}

	///////////////////////////////////////////////////////////////////////////
	//convert coord to polar, rotate, convert back to rectangular
	public static Point2D rotate1 (Point2D point, double radians)
	{
		double xx = point.getX ();
		double yy = point.getY ();
		double theta = Math.atan2 (yy, xx);
		double radius = Math.sqrt (xx * xx + yy * yy);

		theta += radians;
		xx = radius * Math.cos (theta);
		yy = radius * Math.sin (theta);

		return new Point2D.Double (xx, yy);
	}

	///////////////////////////////////////////////////////////////////////////
	//convert coord to polar, rotate, convert back to rectangular
	public static Point2D rotate2 (Point2D point, double radians)
	{
		double xx = point.getX ();
		double yy = point.getY ();
		double theta = Math.atan2 (yy, xx);
		double radius = Math.sqrt (xx * xx + yy * yy);

		theta += radians;
		xx = radius * Math.cos (theta);
		yy = radius * Math.sin (theta);

		point.setLocation (xx, yy);
		return point;
	}

	///////////////////////////////////////////////////////////////////////////
	//convert coord to polar, rotate, convert back to rectangular
	public static Point2D rotate3 (Point2D point, double radians)
	{
		//note this one doesn't work quite right

		double x1 = point.getX ();
		double y1 = point.getY ();

		double theta1 = Math.atan2 (y1, x1);
		double radius = x1 / Math.cos (theta1);

		double theta2 = theta1 + radians;
		double x2 = radius * Math.cos (theta2);
		double y2 = radius * Math.sin (theta2);

		return new Point2D.Double (x2, y2);
	}

	//private members
	private int _count = 1000;

//	private static Logger _log = LogManager.getLogger (PerfTest.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "PerfTest";
	public static final String NL = System.getProperty ("line.separator");
}
