//ContentItem.java

package com.vendo.contentServlet;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VPair;


//	if (filename contains "-" or "_")
//		file = item00-01.ext
//		name = item00-01
//		base = item00
//		index = 01
//		extension = ext
//	else if (filename contains "[0-9]")
//		file = item00.ext
//		name = item00
//		base = item00
//		index = 00
//		extension = ext
//	else if (filename contains ".")
//		file = item.ext
//		name = item
//		base = item
//		index = ext
//		extension = ext
//	else
//		file = item
//		name = item
//		base = item
//		index = item
//		extension = ""

public class ContentItem
{
// unused
//	///////////////////////////////////////////////////////////////////////////
//	public ContentItem ()
//	{
//	}

	///////////////////////////////////////////////////////////////////////////
	//if filename contains "-" or "_", split on that, otherwise, split on extension
	public ContentItem (Path path)
	{
//		_isDirectory = Files.isDirectory (path);

		_file = path.getFileName ().toString ();
		_extension = getExtensionFromFilename (_file);

//		_log.debug ("ContentItem.ctor: _file: " + _file);

		try {
			if (_file.contains ("-") || _file.contains ("_")) {
				final String regex = "[\\-_]";

				_name = stripExtension (_file);

				VPair<String, String> pair = splitString (_name, regex, false); //split on last occurrence
				_base = pair.getFirst ();
				_index = pair.getSecond ();

			} else if (_file.matches (".*[0-9].*") && !_file.matches ("[0-9].*")) { //exclude files that start with a digit
				final String regex1 = "[0-9]";
				final String regex2 = "_";

				String temp = stripExtension (_file);

//				_log.debug ("ContentItem.ctor: _file: " + _file + ", temp: " + temp);

				_name = insertMarkerAtFirstMatch (temp, regex1, regex2); //insert "_" so we can split on it

				VPair<String, String> pair = splitString (_name, regex2, false); //split on last occurrence
				_base = pair.getFirst ();
				_index = pair.getSecond ();

			} else if (_file.contains (".")) {
				final String regex = "\\.";

				VPair<String, String> pair = splitString (_file, regex, true); //split on first occurrence
				_name = _base = pair.getFirst ();
				_index = pair.getSecond ();

			} else {
				//cause exception to handle this in catch block below
				new String ().substring (-1);
//				_name = _base = _index = _file;
			}

		} catch (Exception ee) {
			_name = _base = _index = _file;
			_log.debug ("ContentItem.ctor: unhandled case: _file: " + _file);// + ", _name: " + _name + ", _base: " + _base + ", index: " + _index);
		}

//		_log.debug ("ContentItem.ctor: _file: " + _file + ", _name: " + _name + ", _base: " + _base + ", index: " + _index);
	}

	///////////////////////////////////////////////////////////////////////////
	public ContentItem (ContentItem item)
	{
		_file = item.getFile ();
		_name = item.getName ();
		_base = item.getBase ();
		_index = item.getIndex ();
		_extension = item.getExtension ();
//		_isGrouped = item.getIsGrouped ();
		_isDirectory = item.getIsDirectory ();
	}

	///////////////////////////////////////////////////////////////////////////
	//note that this removes the component of the string that matches the regex
	public VPair<String, String> splitString (String string, String regex, boolean onFirstOccurrence) throws Exception
	{
		String parts[] = new String[] {};

		if (onFirstOccurrence) {
			final String marker = ":::";
			parts = string.replaceFirst (regex, marker).split (marker);

		} else {
			final String marker = ":::";
			String reversed = reverse (string).replaceFirst (regex, marker);
			parts = reverse (reversed).split (marker);
		}

		return VPair.of (parts[0], parts[1]);
	}

	///////////////////////////////////////////////////////////////////////////
	//splits filename on last occurrence of "." and returns everything after
	public static String getExtensionFromFilename (String filename)
	{
		final String marker = ":::";
		String reversed = reverse (filename).replaceFirst ("\\.", marker);
		String parts[] = reverse (reversed).split (marker);

		if (parts.length == 2)
			return parts[1];
		else
			return new String ();
	}

	///////////////////////////////////////////////////////////////////////////
	//splits filename on first occurrence of "."
	public static String stripExtension (String filename)
	{
		String parts[] = filename.split ("\\."); //regex
		try {
			filename = parts[0];
		} catch (Exception ee) {
			_log.error ("ContentItem.stripExtension: split failed on filename = \"" + filename + "\"");
//			_log.error (ee);
		}

		return filename;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String insertMarkerAtFirstMatch (String string, String regex, String marker)
	{
		String temp = string.replaceFirst (regex, marker);
		int markerAt = temp.indexOf (marker);

//		_log.debug ("ContentItem.insertMarkerAtFirstMatch: string: " + string);

		int length = string.length ();
		String newString = string.substring (0, markerAt) + marker + string.substring (markerAt, length);

//		_log.debug ("ContentItem.insertMarkerAtFirstMatch: string: " + string + ", newString: " + newString);

		return newString;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String reverse (String string)
	{
		return new StringBuffer (string).reverse ().toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFile ()
	{
		return _file;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getBase ()
	{
		return _base;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getIndex ()
	{
		return _index;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getExtension ()
	{
		return _extension;
	}

	///////////////////////////////////////////////////////////////////////////
//	public boolean getIsGrouped ()
//	{
//		return _isGrouped;
//	}

	///////////////////////////////////////////////////////////////////////////
	public boolean getIsDirectory ()
	{
		return _isDirectory;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isSameFirstLetter (ContentItem item)
	{
		if (item == null)
			return false;

		return getBase ().substring (0, 1).compareToIgnoreCase (item.getBase ().substring (0, 1)) == 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isSameBase (ContentItem item)
	{
		if (item == null)
			return false;

//		return getBase ().compareTo (item.getBase ()) == 0;
		return getBase ().compareToIgnoreCase (item.getBase ()) == 0;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		String modified = _dateFormat.format (new Date (getModified ()));

		StringBuffer sb = new StringBuffer (256);
		sb.append (getName ()).append (", ")
		  .append (getWidth ()).append ("x").append (getHeight ()).append (", ")
		  .append (getBytes () / 1024).append ("KB, ")
		  .append (modified);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean equalAttrs (ContentItem image, boolean testRgbData)
	{
		if (getHash () == image.getHash () &&
			getBytes () == image.getBytes () &&
			getWidth () == image.getWidth () &&
			getHeight () == image.getHeight ()) {

			if (testRgbData)
				return getRgbData ().equals (image.getRgbData ());
			else
				return true;

		} else {
			return false;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getNameWithExt ()
	{
		return _name + ContentFormInfo._DotExtension;
	}

	///////////////////////////////////////////////////////////////////////////
	//lazy evaluation
	public String getNameFirstLetterLower ()
	{
		if (_nameFirstLetterLower == null)
			_nameFirstLetterLower = _name.substring (0, 1).toLowerCase ();

		return _nameFirstLetterLower;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSubFolder ()
	{
		return _subFolder;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getImagePath ()
	{
		return _imagePath;
	}

	///////////////////////////////////////////////////////////////////////////
	public File getFile ()
	{
		if (_file == null) {
			_file = new File (getImagePath (), getName ());

//			_fileExists = _file.exists ();
		}

		return _file;
	}

	///////////////////////////////////////////////////////////////////////////
	//note this needs to be tested actively, since the file could have been deleted since the last time we checked
	public boolean getFileExists ()
	{
//this optimization breaks the .delete functionality
//		if (_fileExists)
//			return _fileExists;

//TODO - temp perf testing
//		_fileExists = getFile ().exists ();
		_fileExists = true;

/* new way
		Path file = FileSystems.getDefault ().getPath (getImagePath (), getName ());

		_fileExists = Files.exists (file);
*

		return _fileExists;
	}

	///////////////////////////////////////////////////////////////////////////
	public ContentItem getPartner ()
	{
		return _partner;
	}

	public void setPartner (ContentItem partner)
	{
		_partner = partner;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getBytes ()
	{
		return _bytes;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getWidth ()
	{
		return _width;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getHeight ()
	{
		return _height;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getScaledWidth ()
	{
		return _scaledWidth;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getScaledHeight ()
	{
		return _scaledHeight;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getScale ()
	{
		return _scale;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getScaleStr ()
	{
		if (_scale < 0)
			return "(??)";
		else
			return "(" + Integer.toString (_scale) + "%)";
	}

	///////////////////////////////////////////////////////////////////////////
	public long getModified ()
	{
		return _modified;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getCount ()
	{
		return _count;
	}

	public void setCount (int count)
	{
		_count = count;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand
	public long getPixels ()
	{
		if (_pixels < 0)
			_pixels = getWidth () * getHeight ();

		return _pixels;
	}

	///////////////////////////////////////////////////////////////////////////
	//used for de-dup'ing; calculated on demand
	public long getHash ()
	{
		if (_hash < 0) {
			_hash = (getBytes () * 10000) ^ (getWidth () * 100) ^ getHeight ();

			if (_hash < 0)
				throw new RuntimeException ("ContentItem.getHash: hash overflowed long");

			if (ContentFormInfo._logLevel >= 10)
				_log.debug (_hash + ": " + getName ());
		}

		return _hash;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand
	public long getRandom ()
	{
		if (_random < 0) {
			_random = Math.abs (_randomGenerator.nextLong ());

			if (ContentFormInfo._logLevel >= 10)
				_log.debug (_random + ": " + getName ());
		}

		return _random;
	}

	///////////////////////////////////////////////////////////////////////////
	//used for de-dup'ing
	public String getRgbData ()
	{
		if (_rgbData == null || _rgbData.length () == 0)
			throw new RuntimeException ("ContentItem.getRgbData: _rgbData not initialized");

		return _rgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand
	public void calculateScaledSize (int maxImageWidth, int maxImageHeight)
	{
		double maxImageScaleFactor = (double) ContentFormInfo._maxImageScalePercent / 100;

//		_log.debug ("ContentItem.calculateScaledSize: maxImageWidth = " + maxImageWidth + ", maxImageHeight = " + maxImageHeight);

//TODO - collapse these two cases into one
		if (_width < 0 || _height < 0) {
			//scale images to available space
//			int width = 100;	//unknown image size, set a fake size for a 3:2 vertically-oriented image
//			int height = 150;
			int width = 150;	//unknown image size, set a fake size for a 2:3 horizontally-oriented image
			int height = 100;

			double scaleForWidth = maxImageWidth / (double) width;
			double scaleForHeight = maxImageHeight / (double) height;

			double scale = Math.min (scaleForWidth, scaleForHeight);
//			scale = Math.min (scale, maxImageScaleFactor);

			_scale = -1;//(int) (scale * 100);
			_scaledWidth = (int) ((double) width * scale);
			_scaledHeight = (int) ((double) height * scale);

		} else {
			//scale images to available space
			double scaleForWidth = maxImageWidth / (double) _width;
			double scaleForHeight = maxImageHeight / (double) _height;

			double scale = Math.min (scaleForWidth, scaleForHeight);
			scale = Math.min (scale, maxImageScaleFactor); //limit magnification to maxImageScaleFactor

			_scale = (int) (scale * 100);
			_scaledWidth = (int) ((double) _width * scale);
			_scaledHeight = (int) ((double) _height * scale);
		}

/* works except images grow vertically beyond the window size
		if (_width < 0 || _height < 0) {
			_scaledWidth = maxImageWidth;
			_scaledHeight = (3 * _scaledWidth) / 2; //this only works for vertically-oriented images

		} else {
			//scale images to available space
			double scaleForWidth = maxImageWidth / (double) _width;
			double scaleForHeight = maxImageHeight / (double) _height;

			double scale = Math.min (scaleForWidth, scaleForHeight);
			scale = Math.min (scale, maxImageScaleFactor);

			_scale = (int) (scale * 100);
			_scaledWidth = (int) ((double) _width * scale);
			_scaledHeight = (int) ((double) _height * scale);
		}
*
	}
*/

	//members
	private String _name;		//item00-01
	private String _file;		//item00-01.ext
	private String _base;		//item00
	private String _index;		//01
	private String _extension;	//ext
//	private boolean _isGrouped;	//does this name support groups (i.e., name contains "-" or "_")
	private boolean _isDirectory;

	//objects read from xml file
/*
	private String _name = null;
	private String _nameFirstLetterLower = null;
	private long _bytes = -1;
	private int _width = -1;
	private int _height = -1;
	private long _modified = -1;
	private String _rgbData = null;

	private String _subFolder = null;
	private String _imagePath = null;
	private File _file = null;
	private boolean _fileExists = false;
	private long _pixels = -1;
	private int _scaledWidth = -1;
	private int _scaledHeight = -1;
	private int _scale = -1;
	private long _hash = -1;
	private long _random = -1;
	private int _count = -1;
	private ContentItem _partner = null;

	private static HashMap<String, String> _imageBaseNameMap = new HashMap<String, String> (2 * ContentFormInfo._maxFilesDir);
	private static Random _randomGenerator = new Random ();

	private static final String NL = System.getProperty ("line.separator");
	private static final String _marker = ":::";
	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm:ss");
*/

	private static Logger _log = LogManager.getLogger (ContentItem.class);
}
