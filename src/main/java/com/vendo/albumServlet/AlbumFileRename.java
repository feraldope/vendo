//AlbumFileRename.java

package com.vendo.albumServlet;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;


public class AlbumFileRename
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		AlbumFileRename app = new AlbumFileRename ();

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

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("dir")) {
					try {
						_dir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

/*
				} else if (arg.equalsIgnoreCase ("height") || arg.equalsIgnoreCase ("h")) {
					try {
						_desiredHeight = Integer.parseInt (args[++ii]);
						if (_desiredHeight < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("query") || arg.equalsIgnoreCase ("q")) {
					_queryMode = true;

				} else if (arg.equalsIgnoreCase ("scalePercent") || arg.equalsIgnoreCase ("sc")) {
					try {
						_scalePercent = Integer.parseInt (args[++ii]);
						if (_scalePercent < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("suffix") || arg.equalsIgnoreCase ("su")) {
					try {
						_nameSuffix = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("width") || arg.equalsIgnoreCase ("w")) {
					try {
						_desiredWidth = Integer.parseInt (args[++ii]);
						if (_desiredWidth < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
*/
/*
				} else if (arg.equalsIgnoreCase ("max") || arg.equalsIgnoreCase ("m")) {
					try {
						_maxMissedFiles = Integer.parseInt (args[++ii]);
						if (_maxMissedFiles < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/max " + _maxMissedFiles);

				} else if (arg.equalsIgnoreCase ("strip")) { // || arg.equalsIgnoreCase ("strip")) {
					_stripHead = true;
					_switches.add ("/strip");

				} else if (arg.equalsIgnoreCase ("ignore") || arg.equalsIgnoreCase ("i")) {
					_ignoreHistory = true;

				} else if (arg.equalsIgnoreCase ("checkOnly") || arg.equalsIgnoreCase ("co")) {
					_checkHistoryOnly = true;
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_inPattern == null) {
					_inPattern = arg;

				} else if (_outPattern == null) {
					_outPattern = arg;

/*
				} else if (_outputPrefix == null) {
					_outputPrefix = arg;
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_inPattern == null)
			displayUsage ("<inPattern> not specified", true);

		if (_outPattern == null)
			displayUsage ("<outPattern> not specified", true);

/*
		if (!_queryMode) {
			if (_scalePercent < 0 && _desiredWidth < 0 && _desiredHeight < 0)
				displayUsage ("must specify width, height, or scale", true);
		}

*/

//TODO - verify _destDir exists, and is writable??
		if (_dir == null)
			_dir = VendoUtils.getCurrentDirectory ();
		_dir = appendSlash (_dir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

//ju <infile> [/subDirs] [/outFile <outfile>] [/width <width (possibly negative)>] [/height <height (possibly negative)>]

//		msg += "Usage: " + _AppName + " [/debug] [/strip] [/ignore] [/dest <dest dir>] [/start <start index>] [/block <block number>] [/maxMissedFiles <count>] [/digits <number>] [/pad <number>] [/prefix <numberPrefix>] <URL> <output prefix>";
		msg += "Usage: " + _AppName + " [/debug] ... TBD";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
//		if (_Debug)
//			_log.debug ("AlbumFileRename.run");

		_log.debug ("_inPattern = " + _inPattern);
		_log.debug ("_outPattern = " + _outPattern);

		ArrayList<VPair<String, String>> results = moveFiles (_inPattern, _outPattern);

		for (VPair<String, String> result : results) {
			System.out.println (result);
		}

/*
		if (_queryMode) {
			for (String filename : filenames) {
				ImageAttributes imageAttributes = getImageAttributes (filename);
				System.out.println (imageAttributes);
//				return imageAttributes;
//				String results = queryImage (filename);
			}

			return true;
		}

		if (_desiredWidth > 0 || _desiredHeight > 0) {
			for (String filename : filenames) {
				System.out.println (filename);
				String outFilename = generateOutFilename (filename, _nameSuffix);

				if (outFilename.compareToIgnoreCase (filename) == 0) {
					_log.error ("run: output file name '" + outFilename + "' is the same as input file name (need to specify /suffix)");
					continue;
				}

				if (fileExists (outFilename)) {
					_log.error ("run: output file '" + outFilename + "' already exists");
					continue;
				}

				System.out.println (outFilename);

				generateScaledImage (filename, outFilename, _desiredWidth, _desiredHeight);
			}
		}

		if (_scalePercent > 0) {
			double scaleFactor = _scalePercent / 100.;

			for (String filename : filenames) {
				System.out.println (filename);
				String outFilename = generateOutFilename (filename, _nameSuffix);

				if (outFilename.compareToIgnoreCase (filename) == 0) {
					_log.error ("run: output file name '" + outFilename + "' is the same as input file name (need to specify /suffix)");
					continue;
				}

				if (fileExists (outFilename)) {
					_log.error ("run: output file '" + outFilename + "' already exists");
					continue;
				}

				System.out.println (outFilename);

				ImageAttributes imageAttributes = getImageAttributes (filename);

				int width = -1;
				int height = -1;

				if (imageAttributes._width > imageAttributes._height)
					width = (int) ((double)(imageAttributes._width + 1) * scaleFactor);
				else
					height = (int) ((double)(imageAttributes._height + 1) * scaleFactor);

				generateScaledImage (filename, outFilename, width, height);
			}
		}
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
//	public VPair<String, String>[] moveFiles (String inPattern, String outPattern)
	public ArrayList<VPair<String, String>> moveFiles (String inPattern, String outPattern)
	{
//		String[] imageFiles = new String[] {};

//		VPair<String, String>[] results = new VPair<String, String>[] { VPair.of ("", "") };
//		VPair<String, String>[] results = new VPair<String, String>[] {};
//		VPair<String, String>[] results = null;
//		VPair<?, ?>[] results = new VPair<String, String>[] {};
		ArrayList<VPair<String, String>> results = new ArrayList<VPair<String, String>> ();


//see:
//http://stackoverflow.com/questions/7131652/generic-array-creation-error


/*
		String[] infilenames = getFileList (_dir, _inPattern);

		if (infilenames.length == 0) {
			_log.error ("run: no files matching '" + _inPattern + "' found");
			return true;
		}

		for (String infilename : infilenames) {
			System.out.println (infilename);
		}
*/
		return results;
	}

	///////////////////////////////////////////////////////////////////////////
	public String[] getFileList (String dirName, final String nameWild)
	{
		//for simple dir listings, it looks like java.io package is faster than java.nio
		FilenameFilter filenameFilter = new FilenameFilter () {
			public boolean accept (File dir, String name) {
				return VendoUtils.matchPattern (name, nameWild);
			}
		};

		File dir = new File (dirName);
		String[] filenames = dir.list (filenameFilter);

		dirName = appendSlash (dirName);
		for (int ii = 0; ii < filenames.length; ii++)
			filenames[ii] = dirName + filenames[ii];

		return filenames;
	}

//	///////////////////////////////////////////////////////////////////////////
//	private boolean fileExists (String filename)
//	{
//		Path file = FileSystems.getDefault ().getPath (filename);
//
//		return Files.exists (file);
//	}

	///////////////////////////////////////////////////////////////////////////
	private String appendSlash (String dir) //append slash if necessary
	{
		int lastChar = dir.charAt (dir.length () - 1);
		if (lastChar != '/' && lastChar != '\\')
			dir += _slash;

		return dir;
	}


	//members
	private String _inPattern = null;
	private String _outPattern = null;
	private String _dir = null;

	public static boolean _Debug = false;

	public static final String _AppName = "AlbumFileRename";
	public static final String _slash = System.getProperty ("file.separator");
	public static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger ();
}
