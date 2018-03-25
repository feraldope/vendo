//JDomTest.java

//Example usage:
// (optional) del album.xml*
// jr /xml album.xml /dir C:\WINDOWS\Web\Wallpaper /pattern *jpg
// jr /xml album.xml /dir C:\WINDOWS\Web\Wallpaper\Windows /pattern *jpg

package com.vendo.jDomTest;

import com.vendo.vendoUtils.*;

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;

import org.jdom2.*;
import org.jdom2.input.*;
import org.jdom2.output.*;
import org.jdom2.xpath.*;


public class JDomTest
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		JDomTest jDomTest = new JDomTest ();

		if (!jDomTest.processArgs (args))
			System.exit (1); //processArgs displays error

		jDomTest.run ();
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

				} else if (arg.equalsIgnoreCase ("xmlFile") || arg.equalsIgnoreCase ("xml")) {
					try {
						_xmlFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("directory") || arg.equalsIgnoreCase ("dir")) {
					try {
						_dirName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("pattern") || arg.equalsIgnoreCase ("p")) {
					try {
						_pattern = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_filename == null)
					_filename = arg;

				else
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//handle defaults
/*
		if (_filename == null)
			_filename = "MyHDResCapList.mrl";
*/

		//verify required args
		if (_xmlFilename == null || _dirName == null || _pattern == null)
			displayUsage ("Incorrect usage", true);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] TBD";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		readXml ();

		if (_Doc == null)
			_Doc = new Document (new Element ("files"));

		String[] files = doDir ();

		boolean modifiedDoc = false;
		if (files.length <= 0) {
			System.err.println ("No files found");
		} else {
			modifiedDoc = addFilesToDocument (files);
		}

		if (modifiedDoc)
			writeXml ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readXml ()
	{
		try {
			File file = new File (_xmlFilename);
			if (file.exists ()) {
				SAXBuilder builder = new SAXBuilder ();
				_Doc = builder.build (file);
			}

		} catch (Exception ee) {
			System.err.println ("Error parsing XML file \"" + _xmlFilename + "\"");
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeXml ()
	{
		String xmlBackupName = new String (_xmlFilename + ".bak");
		File xmlBackupFile = new File (xmlBackupName);

		if (xmlBackupFile.exists ()) {
			try {
				xmlBackupFile.delete ();

			} catch (Exception ee) {
				System.err.println ("Error deleting backup XML file \"" + xmlBackupName + "\"");
				return false;
			}
		}

		try {
			File xmlFile = new File (_xmlFilename);

			boolean status = xmlFile.renameTo (xmlBackupFile);
			if (!status) {
				System.err.println ("Rename failed (" + xmlFile.getCanonicalPath () + " to " + xmlBackupFile.getCanonicalPath () + ")");
//				return false;
			}

		} catch (Exception ee) {
			System.err.println ("Error renaming XML file to \"" + _xmlFilename + ".bak" + "\"");
//			return false;
		}

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream (_xmlFilename);

		} catch (IOException ee) {
			System.err.println ("Error opening output file \"" + _xmlFilename + "\"");
			return false;
		}

		try {
			new XMLOutputter (Format.getPrettyFormat ()).output (_Doc, outputStream);
//TODO - should this be in finally block?
			outputStream.close ();

		} catch (Exception ee) {
			System.err.println ("Error writing XML file \"" + _xmlFilename + "\"");
			return false;
		}

		System.err.println ("Wrote to XML file \"" + _xmlFilename + "\"");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private String[] doDir ()
	{
		FilenameFilter filter = new FilenameFilter () {
			public boolean accept (File dir, String name) {
				return VendoUtils.matchPattern (name, _pattern);
			}
		};

		File dir = new File (_dirName);
		String[] files = dir.list (filter);

		return files;
	}

	///////////////////////////////////////////////////////////////////////////
	@SuppressWarnings ("deprecation")
	private boolean addFilesToDocument (String[] files)
	{
		System.out.println (files.length + " files found");

		boolean modifiedDoc = false;
		Element root = _Doc.getRootElement ();

		for (int ii = 0; ii < files.length; ii++) {
			String filename = _dirName + _slash + files[ii];
			File file = new File (filename);
			filename = file.getAbsolutePath ();

			long bytes = file.length ();
			long modified = file.lastModified (); //millisecs

//TODO - we don't need to do this until we are sure the file is not already in the Document
			int width = -1;
			int height = -1;
			try {
				BufferedImage image = ImageIO.read (file);
				width = image.getWidth ();
				height = image.getHeight ();

			} catch (Exception ee) {
				System.err.println ("Error reading image file \"" + filename + "\"");
				System.err.println (ee);
			}

			String searchPath = "//file[@name='" + filename + "']";
			Element element1 = null;
			try {
				XPath path = XPath.newInstance (searchPath);
				element1 = (Element) path.selectSingleNode (_Doc);
			} catch (Exception ee) {
				System.err.println ("Error in XPath.selectSingleNode searching for \"" + searchPath + "\"");
			}

			boolean found = false;
			if (element1 != null) {
				if (bytes != extractLong (element1.getAttributeValue ("bytes")))
					break;
				if (width != extractInteger (element1.getAttributeValue ("width")))
					break;
				if (height != extractInteger (element1.getAttributeValue ("height")))
					break;
				if (modified != extractLong (element1.getAttributeValue ("modified")))
					break;
//TODO
//				if (checksum != extractInteger (element1.getAttributeValue ("checksum")))
//					break;

				found = true;
			}

			if (!found) {
				modifiedDoc = true;

				Element element2 = new Element ("file");
				element2.setAttribute ("name", filename);
				element2.setAttribute ("bytes", Long.toString (bytes));
				element2.setAttribute ("width", Integer.toString (width));
				element2.setAttribute ("height", Integer.toString (height));
				element2.setAttribute ("modified", Long.toString (modified));
//				element2.setAttribute ("checksum", "1");
				root.addContent (element2);
			}
		}

		return modifiedDoc;
	}

	///////////////////////////////////////////////////////////////////////////
	public static Integer extractInteger (String string)
	{
		int value = 0;

		try {
			value = Integer.parseInt (string);
		} catch (NumberFormatException exception) {
			System.err.println ("Error parsing integer \"" + string + "\", using value 0");
		}

		return value;
	}

	///////////////////////////////////////////////////////////////////////////
	public static Long extractLong (String string)
	{
		long value = 0;

		try {
			value = Long.parseLong (string);
		} catch (NumberFormatException exception) {
			System.err.println ("Error parsing long \"" + string + "\", using value 0");
		}

		return value;
	}


	//private members
	private String _xmlFilename = null;
	private String _dirName = null;
	private String _pattern = null;
	private Document _Doc = null;

	private static final String _slash = System.getProperty ("file.separator");

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "JDomTest";
	public static final String NL = System.getProperty ("line.separator");
}
