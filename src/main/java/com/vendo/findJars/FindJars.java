//FindJars.java

/* example usage
findJar.bat /s "C:\Program Files\Java\jdk1.8.0_66" /fp rt*.jar JPEGImageDecoder
findJar.bat /s "C:\Program Files\Java\jdk1.8.0_66" /fp "*.jar,*.zip" JPEGImageDecoder
findJar.bat /s "C:\Program Files\Java\jdk1.8.0_66\src.zip" /fp "*.zip" JPEGImageDecoder
*/

package com.vendo.findJars;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VFileList;
import com.vendo.vendoUtils.VFileList.ListMode;
import com.vendo.vendoUtils.VendoUtils;


public class FindJars
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		FindJars app = new FindJars ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		boolean useCase = false;
		String filePatternsString = null;
		String searchPatternString = null;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

					if (true) { //dump all args
						_log.debug ("args.length = " + args.length);
						for (int jj = 0; jj < args.length; jj++)
							_log.debug ("args[" + ii + "] = '" + args[jj] + "'");
					}

				} else if (arg.equalsIgnoreCase ("case") || arg.equalsIgnoreCase ("c")) {
					useCase = true;

				} else if (arg.equalsIgnoreCase ("filePatterns") || arg.equalsIgnoreCase ("fp")) {
					try {
						filePatternsString = args[++ii];

						//print this out to avoid confusion when command-line Java resolves unquoted wildcards; e.g., *.jar might resolve to tools.jar for pwd
						System.err.println ("Using file pattern: " + filePatternsString);

					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("subdirs") || arg.equalsIgnoreCase ("s")) {
					_recurseSubdirs = true;

				} else if (arg.equalsIgnoreCase ("verbose") || arg.equalsIgnoreCase ("v")) {
					_verbose = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_folderName == null) {
					_folderName = arg;

				} else if (searchPatternString == null) {
					searchPatternString = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_folderName == null) {
			displayUsage ("No folder specified", true);
		}

		if (searchPatternString == null) {
			displayUsage ("No search pattern specified", true);
		}

		int searchPatternFlags = (useCase ? 0 : Pattern.CASE_INSENSITIVE);
		searchPatternString = "*" + searchPatternString + "*";
		searchPatternString = searchPatternString.replace ("*", ".*");
		_searchPattern = Pattern.compile (searchPatternString, searchPatternFlags);

		if (filePatternsString == null) {
			filePatternsString = "*.jar";
			System.err.println ("Using file pattern: " + filePatternsString);
		}

		String filePatternsArray[] = filePatternsString.split (","); //split on commas
		for (String filePatternString : filePatternsArray) {
			filePatternString = filePatternString.replace ("*", ".*").trim (); //regex
			_filePatterns.add (Pattern.compile (filePatternString, Pattern.CASE_INSENSITIVE));
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] [/case (only applies to search pattern)] [/verbose] [/subdirs] [/filePatterns <regex>[,<regex>]] <folder> <regex search pattern>";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		VFileList vFileList = new VFileList (_folderName, _filePatterns, _recurseSubdirs);

		if (_Debug) {
			_log.debug ("FindJars.run: " + vFileList);
		}

		List<String> fileList = vFileList.getFileList (ListMode.CompletePath);

		if (fileList.size () == 0) {
			System.out.println ("No files found");
			return true;
		}

		if (_Debug) {
			_log.debug ("FindJars.run: fileList.size() = " + fileList.size ());
		}

		if (_verbose) {
			System.out.println ("Found " + fileList.size () + " files matching pattern");
		}

		int matchedJars = 0;
		for (String filename : fileList) {
			if (_verbose) {
				System.out.println (filename);
			}

			List<String> matchList = searchJar (filename);

			if (matchList.size () > 0) {
				matchedJars++;

				Collections.sort (matchList, VendoUtils.caseInsensitiveStringComparator);

				for (String itemName : matchList) {
					System.out.println (filename + ": " + itemName);
				}
			}
		}

		System.out.println ("Results: " + matchedJars + " of " + fileList.size () + " jars had matches");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<String> searchJar (String zipFilename)
	{
		List<String> matchList = new ArrayList<String> ();

		FileInputStream fis = null;
		try {
			File zipFile = new File (zipFilename);
			fis = new FileInputStream (zipFile);

		} catch (FileNotFoundException ex) {
			System.err.println ("Error reading file '" + zipFilename + "'");
			System.err.println (ex.getMessage ());

			return matchList;
		}

		//read the zip file and add all entries to the map
		Map<String, byte[]> zipContents = new HashMap<String, byte[]> ();
		getZipContents (zipContents, fis);

		for (String name : zipContents.keySet ()) {
			if (_searchPattern.matcher (name).matches ()) {
				matchList.add (name);
			}
		}

		return matchList;
	}


	///////////////////////////////////////////////////////////////////////////
	//recursive method to extract zip entries from the input stream and add them to the map
	private void getZipContents (Map<String, byte[]> zipContents, InputStream fis)
	{
//		if (_Debug) {
//			_log.debug ("entering getZipContents()");
//		}

		Map<String, byte[]> tmpContents = ZipUtilities.read (fis);
		Set<String> names = new HashSet<String> (tmpContents.keySet ());

		for (String name : names) {
			byte[] bytes = tmpContents.get (name);
			if (name.endsWith (".zip") || name.endsWith (".jar")) {
				getZipContents (zipContents, new ByteArrayInputStream (bytes)); //recurse

			} else {
//				if (_Debug) {
//					_log.debug ("getZipContents: found entry named '" + name + "'");
//				}

				zipContents.put (name, bytes);
			}
		}
	}


	//private members
	private String _folderName = null;
	private List<Pattern> _filePatterns = new ArrayList<Pattern> ();
	private Pattern _searchPattern = null;
	private boolean _recurseSubdirs = false;
	private boolean _verbose = false;

//	private static final String _slash = System.getProperty ("file.separator");
	private static Logger _log = LogManager.getLogger (FindJars.class);

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "FindJars";
	public static final String NL = System.getProperty ("line.separator");
}
