//TreeDir.java

//Original inspiration from http://www.javapractices.com/topic/TopicAction.do?Id=68

package com.vendo.treeDir;

//import com.vendo.vendoUtils.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class TreeDir
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		TreeDir app = new TreeDir ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
/*
		if (false && args.length >= 1) {
			try {
				File file = new File (args[0]);
				String parent = file.getParent ();
				String name = file.getName ();
				String path = file.getPath ();
				String absolutePath = file.getAbsolutePath ();
				String canonicalPath = file.getCanonicalPath ();

				System.out.println ("       parent = " + parent);
				System.out.println ("         name = " + name);
				System.out.println ("         path = " + path);
				System.out.println (" absolutePath = " + absolutePath);
				System.out.println ("canonicalPath = " + canonicalPath);

			} catch (Exception ee) {
				_log.error ("Exception accessing file '" + args[0] + "' - ", ee);
			}
		}
*/

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());
				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("include") || arg.equalsIgnoreCase ("i")) {
					try {
						_includePatterns.add (args[++ii].trim ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("exclude") || arg.equalsIgnoreCase ("e")) {
					try {
						_excludePatterns.add (args[++ii].trim ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("perf") || arg.equalsIgnoreCase ("p")) {
					_perfTesting = true;

				} else if (arg.equalsIgnoreCase ("subdirs") || arg.equalsIgnoreCase ("s")) {
					_recurseSubdirs = true;

/*
				} else if (arg.equalsIgnoreCase ("block") || arg.equalsIgnoreCase ("b")) {
					try {
						_blockNumber = Integer.parseInt (args[++ii]);
						if (_blockNumber < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/block " + _blockNumber);
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_folderString == null) {
					_folderString = arg;

/*
				} else if (_prefix == null) {
					_prefix = arg;
*/

				} else {
//					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);

//TODO - check for invalid characters
					_includePatterns.add (arg.trim ());

				}
			}
		}

		if (_Debug) {
			for (int ii = 0; ii < args.length; ii++) {
				String arg = args[ii];
				_log.debug ("processArgs: arg = '" + arg + "'");
			}
		}

		//check for required args, set defaults
		if (_folderString == null) {
			_folderString = ".";
		}
		_folder = new File (_folderString);

//		if (_includePatterns.isEmpty ())
//			_includePatterns.add ("*");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/subdirs] [/include <file pattern> ...] [/exclude <file pattern> ...] [<folder> [<file pattern> ...]] ";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
//		_log.debug ("TreeDir.run");

		long startNano = System.nanoTime ();

		String[] includes = _includePatterns.toArray (new String[] {});
		String[] excludes = _excludePatterns.toArray (new String[] {});
		_filter = new FileFilter (includes, excludes);

		_filesFound = 0;
		boolean status = printFiles (_folder);

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;

		if (_filesFound == 0) {
			System.out.println ("No files found");
		} else if (_filesFound == 1) {
			System.out.println (_filesFound + " file found");
		} else {
			System.out.println (_filesFound + " files found");
		}

		if (_perfTesting) {
			System.out.println ("elapsed: " + elapsedMillis + " ms");
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean printFiles (File folder)
	{
		File[] files = folder.listFiles ();

		if (files != null) {
			for (File file : files) {
				FileInfo fileInfo = new FileInfo (file);

				if (_filter.accept (folder, file.getName ())) {
					_filesFound++;
					String info = fileInfo.toString ();
					if (!_perfTesting) {
						System.out.println (info);
					}
				}

				if (_recurseSubdirs && fileInfo.isDirectory ()) {
					printFiles (file);
				}
			}
		}

		return true;
	}


	//private members
	private boolean _recurseSubdirs;
	private boolean _perfTesting;
	private String _folderString = null;
	private File _folder;
	private List<String> _includePatterns = new ArrayList<String> ();
	private List<String> _excludePatterns = new ArrayList<String> ();
	private FileFilter _filter;
	private int _filesFound = 0;

//	private static final String _slash = System.getProperty ("file.separator");
	private static Logger _log = LogManager.getLogger (TreeDir.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "TreeDir";
	public static final String NL = System.getProperty ("line.separator");
}
