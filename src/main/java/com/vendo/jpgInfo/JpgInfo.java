/* JpgInfo.java

https://drewnoakes.com/code/exif/
https://github.com/drewnoakes/metadata-extractor/releases

Java doc available at:
http://javadoc.metadata-extractor.googlecode.com/git/2.7.0/index.html

Getting started:
https://code.google.com/p/metadata-extractor/wiki/GettingStarted

Example code
http://code.google.com/p/metadata-extractor/source/browse/Samples/com/drew/metadata/SampleUsage.java

Currently version:
C:\Program Files\Java\jdk1.7.0_45\jre\lib\ext\metadata-extractor-2.8.1.jar
C:\Program Files\Java\jdk1.7.0_45\jre\lib\ext\xmpcore-5.1.2.jar

Examples (see also JpgInfoTest.bat):
jr.bat /subdirs /folder .. /file "*jpg" | egrep -i "(.jpg|date|time)"
jr.bat /subdirs /folder .. /file "*jpg" /dateOnly

*/

package com.vendo.jpgInfo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.vendo.jpgUtils.JpgUtils;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.win32.Win32;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;


public class JpgInfo
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (final String args[]) throws Exception
	{
		JpgInfo app = new JpgInfo ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		//set defaults for command line arguments
		String filePatternString = "*";
		String folderString = ".";

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("dateOnly") || arg.equalsIgnoreCase ("date")) {
					_dateOnly = true;

				} else if (arg.equalsIgnoreCase ("file") || arg.equalsIgnoreCase ("fi")) {
					try {
						filePatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("folder") || arg.equalsIgnoreCase ("fo")) {
					try {
						folderString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("subdirs") || arg.equalsIgnoreCase ("s")) {
					_recurseSubdirs = true;

				} else if (arg.equalsIgnoreCase ("imageInfo") || arg.equalsIgnoreCase ("image")) {
					_imageInfo = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
/*
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

		//check for required args, set defaults
//		if (folderString == null) { //note some defaults set above
//			folderString = ".";
//		}

		_folder = FileSystems.getDefault ().getPath (folderString);

		_filenamePatternFilter = new FilenamePatternFilter (filePatternString);

		if (_Debug) {
			System.out.println ("folderString = \"" + folderString + "\"");
			System.out.println ("filePatternString = \"" + filePatternString + "\"");
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

		msg += "Usage: " + _AppName + " [/debug] [/subdirs] [/folder <folder>] [/file <filename pattern>] [/dateOnly]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () throws Exception
	{
		processFiles (_folder);

/* old way: iterate through all data
		File jpgFile = new File (_inFilename);
		Metadata metadata = JpegMetadataReader.readMetadata (jpgFile);

		int dirCount = metadata.getDirectoryCount ();
		System.out.println ("dir count = " + dirCount);

		//iterate through metadata directories
		Iterable<Directory> iterable = metadata.getDirectories ();
		Iterator<Directory> directories = iterable.iterator ();

		while (directories.hasNext ()) {
			Directory directory = directories.next ();

			int tagCount = directory.getTagCount ();
			System.out.println ("tag count = " + tagCount);

			Collection<Tag> tags = directory.getTags ();
			for (Tag tag : tags) {
				System.out.println (tag);
			}
		}
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//recursive call to process folders (and optionally) subfolders
	private boolean processFiles (Path folder) throws Exception
	{
		try (DirectoryStream <Path> ds = Files.newDirectoryStream (folder)) {
			for (Path file : ds) {
				if (file.toString ().toLowerCase ().endsWith (".jpg") && _filenamePatternFilter.accept (file)) {
					processFile (file);
				}

				if (_recurseSubdirs && isDirectory (file)) {
					processFiles (file); //recurse
				}
			}

		} catch (IOException ee) {
//TODO - better error?
			ee.printStackTrace ();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean processFile (Path file) throws Exception
	{
		if (_dateOnly) {
			return processFileDateOnly(file);
		} else if (_imageInfo) {
			return processImageInfo (file);
		} else {
			return processFileFull (file);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean processFileDateOnly (Path file) throws Exception
	{
		String filename = file.toAbsolutePath ().normalize ().toString ();

//TODO - heed this:
//[ERROR] JpgInfo.processFileDateOnly: error reading attributes from "E:\Netscape\Program\jroot\..."
//[ERROR] com.drew.metadata.MetadataException: Tag 'Digital Time Created' has not been set -- check using containsTag() first

		long exifSubIFDOrigMillis = 0;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata (file.toFile ());

			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType (ExifSubIFDDirectory.class);
			Date date = directory.getDate (ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			exifSubIFDOrigMillis = date.getTime ();

//			System.out.println (filename + " " + exifSubIFDOrigMillis);

		} catch (Exception ee) {
//			_log.error ("JpgInfo.processFileDateOnly: error reading exifSubIFDOrigMillis from \"" + filename + "\"", ee);
		}

		long exifIFD0DateMillis = 0;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata (file.toFile ());

			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType (ExifIFD0Directory.class);
			Date date = directory.getDate (ExifIFD0Directory.TAG_DATETIME);
			exifIFD0DateMillis = date.getTime ();

//			System.out.println (filename + " " + exifIFD0DateMillis);

		} catch (Exception ee) {
//			_log.error ("JpgInfo.processFileDateOnly: error reading exifIFD0DateMillis from \"" + filename + "\"", ee);
		}

		long iptcDateMillis = 0;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata (file.toFile ());

			IptcDirectory directory = metadata.getFirstDirectoryOfType (IptcDirectory.class);
			Date date = directory.getDate (IptcDirectory.TAG_DATE_CREATED);
			int time = directory.getInt (IptcDirectory.TAG_TIME_CREATED);
			iptcDateMillis = date.getTime () + (time * 1000);

//			System.out.println (filename + " " + iptcDateMillis);

		} catch (Exception ee) {
//			_log.error ("JpgInfo.processFileDateOnly: error reading iptcDateMillis from \"" + filename + "\"", ee);
		}

		long xmpDateMillis = 0;
		try {
			Metadata metadata = ImageMetadataReader.readMetadata (file.toFile ());

			XmpDirectory directory = metadata.getFirstDirectoryOfType (XmpDirectory.class);
			Date date = directory.getDate (XmpDirectory.TAG_DATETIME_ORIGINAL);
			xmpDateMillis = date.getTime ();

//			System.out.println (filename + " " + iptcDateMillis);

		} catch (Exception ee) {
//			_log.error ("JpgInfo.processFileDateOnly: error reading xmpDateMillis from \"" + filename + "\"", ee);
		}

//TODO - add XmpDirectory.TAG_DATETIME_ORIGINAL

		String dates = new String ();
		if (exifSubIFDOrigMillis != 0) {
			dates += ", D0=" + _dateFormat.format (new Date (exifSubIFDOrigMillis));
		}
		if (exifIFD0DateMillis != 0) {
			dates += ", D1=" + _dateFormat.format (new Date (exifIFD0DateMillis));
		}
		if (iptcDateMillis != 0) {
			dates += ", D2=" + _dateFormat.format (new Date (iptcDateMillis));
		}
		if (xmpDateMillis != 0) {
			dates += ", D3=" + _dateFormat.format (new Date (xmpDateMillis));
		}

		System.out.println (filename + dates);

		if (_Debug) {
			dates = new String ();
			if (exifSubIFDOrigMillis != 0) {
				dates += ", D0=" + exifSubIFDOrigMillis;
			}
			if (exifIFD0DateMillis != 0) {
				dates += ", D1=" + exifIFD0DateMillis;
			}
			if (iptcDateMillis != 0) {
				dates += ", D2=" + iptcDateMillis;
			}
			if (xmpDateMillis != 0) {
				dates += ", D3=" + xmpDateMillis;
			}

			System.out.println (filename + dates);
		}

		System.out.println ("");

/*
		if (exifSubIFDOrigMillis == 0 && exifIFD0DateMillis == 0 && iptcDateMillis == 0) {
			System.out.println (filename + " :NoDates:");
		} else if (exifSubIFDOrigMillis != 0 && exifIFD0DateMillis == 0 && iptcDateMillis == 0) {
			System.out.println (filename + " :D0Only:");
		} else if (exifSubIFDOrigMillis == 0 && exifIFD0DateMillis != 0 && iptcDateMillis == 0) {
			System.out.println (filename + " :D1Only:");
		} else if (exifSubIFDOrigMillis == 0 && exifIFD0DateMillis == 0 && iptcDateMillis != 0) {
			System.out.println (filename + " :D2Only:");
		} else if (exifSubIFDOrigMillis != 0 && exifIFD0DateMillis != 0 && iptcDateMillis == 0) {
			if (exifSubIFDOrigMillis == exifIFD0DateMillis) {
				System.out.println (filename + " :D0D1:same");
			} else {
				System.out.println (filename + " :D0D1:diff");
			}
		} else if (exifSubIFDOrigMillis != 0 && exifIFD0DateMillis == 0 && iptcDateMillis != 0) {
			if (exifSubIFDOrigMillis == iptcDateMillis) {
				System.out.println (filename + " :D0D2:same");
			} else {
				System.out.println (filename + " :D0D2:diff");
			}
		} else if (exifSubIFDOrigMillis == 0 && exifIFD0DateMillis != 0 && iptcDateMillis != 0) {
			if (exifIFD0DateMillis == iptcDateMillis) {
				System.out.println (filename + " :D1D2:same");
			} else {
				System.out.println (filename + " :D1D2:diff");
			}
//test for D0=D1 with or without D2?
		} else if (exifSubIFDOrigMillis != 0 && exifIFD0DateMillis != 0 && iptcDateMillis != 0) {
			if (exifSubIFDOrigMillis == exifIFD0DateMillis && exifSubIFDOrigMillis == iptcDateMillis) {
				System.out.println (filename + " :D0D1D2:same");
			} else {
				System.out.println (filename + " :D0D1D2:diff");
			}
		}
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean processImageInfo (Path file) throws Exception {
//		String filename = file.toAbsolutePath ().normalize ().toString ();
		String filename = file.getFileName ().toString ();

		BufferedImage image = null;
		long numBytes = 0;
		int width = 0;
		int height = 0;
		short color = _warningColor;
		String orientation = "";

		try {
			BasicFileAttributes attrs = Files.readAttributes (file, BasicFileAttributes.class);
			image = JpgUtils.readImage (new File (filename));

			numBytes = attrs.size ();
			width = image.getWidth();
			height = image.getHeight();
			color = _highlightColor;
			if (width < _alertPixels || height < _alertPixels) {
				color = _warningColor;
			}
			orientation = (width > height ? "-" : width < height ? "|" : "+"); //TODO - does not include any slop when identifying square images
		} catch (Exception ex) {
			//ignore
		}

		String details = filename + " " + orientation + " " + width + "x" + height + ", " + VendoUtils.unitSuffixScale (numBytes);
		VendoUtils.printWithColor (color, details);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean processFileFull (Path file) throws Exception
	{
		String filename = file.toAbsolutePath ().normalize ().toString ();
		System.out.println (filename);

		boolean foundTags = false;

		try {
			Metadata metadata = ImageMetadataReader.readMetadata (file.toFile ());

			//iterate through metadata directories
			Iterable<Directory> iterable = metadata.getDirectories ();
			Iterator<Directory> directories = iterable.iterator ();
			int numDirectories = metadata.getDirectoryCount ();

			int ii = 0;
			while (directories.hasNext ()) {
				Directory directory = directories.next ();
				String directoryName = directory.getName ();

				int tagCount = directory.getTagCount ();
				System.out.println ("Directory \"" + directoryName + "\" " + (++ii) + "/" + numDirectories + " (tag count = " + tagCount + ")");

				Collection<Tag> tags = directory.getTags ();
				for (Tag tag : tags) {
					foundTags = true;
					System.out.println (tag);
				}
			}

		} catch (Exception ee) {
//TODO - ignore some, but not all exceptions
			//ignore
		}

		if (!foundTags) {
			System.out.println ("[no metadata tags found]");
		}

		System.out.println ("");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isDirectory (Path file)
	{
		try {
			return Files.isDirectory (file);

		} catch (Exception ee) {
			_log.error ("JpgInfo.isDirectory () failed - ", ee);
		}

		return false;
	}


	//private members
	private Path _folder;
	private Boolean _recurseSubdirs = false;
	private Boolean _dateOnly = false;
	private Boolean _imageInfo = false;
	private FilenamePatternFilter _filenamePatternFilter = null;

	private static final int _alertPixels = 640;
//	private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
	private static final short _warningColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW;
	private static final short _highlightColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA;

	private static Logger _log = LogManager.getLogger (JpgInfo.class);
    private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yyyy HH:mm:ss");

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "JpgInfo";
	public static final String NL = System.getProperty ("line.separator");
}
