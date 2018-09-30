//AlbumImage.java

package com.vendo.albumServlet;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.vendo.jpgUtils.JpgUtils;
import com.vendo.vendoUtils.VendoUtils;


public class AlbumImage
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImage (String name, int subFolderInt, long numBytes, int width, int height, long modified, String rgbData,
					   long exifDate0, long exifDate1, long exifDate2, long exifDate3)
	{
		String subFolder = subFolderFromByte (subFolderInt);
		String imagePath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ false) + subFolder;
		long[] exifDates = new long[] {exifDate0, exifDate1, exifDate2, exifDate3, 0, 0};

		_name = name;
		_numBytes = numBytes;
		_width = width;
		_height = height;
		_modified = modified;
		_rgbData = rgbData;
		setExifDates (exifDates);

		_subFolder = subFolder;
		_imagePath = VendoUtils.appendSlash (imagePath);

		_namePlus = null;
		_nameFirstLetterLower = null;
		_baseName1 = null;
		_baseName2 = null;
		_file = null;
		_pixels = -1;
//		_count = -1;
		_hash = -1;
		_random = -1;
		_modifiedString = null;
		_orientation = AlbumOrientation.ShowAny;
	}

//TODO - replace with clone?
	///////////////////////////////////////////////////////////////////////////
	public AlbumImage (AlbumImage image)
	{
		_name = image.getName ();
		_namePlus = image.getNamePlus ();
		_nameFirstLetterLower = image.getNameFirstLetterLower ();
		_numBytes = image.getNumBytes ();
		_width = image.getWidth ();
		_height = image.getHeight ();
		_modified = image.getModified ();
		_rgbData = image.getRgbData ();
		_modifiedString = image.getModifiedString ();
		setExifDates (image.getExifDates ());

		_subFolder = image.getSubFolder ();
		_imagePath = image.getImagePath ();

		_baseName1 = image.getBaseName (/*collapseGroups*/ false);
		_baseName2 = image.getBaseName (/*collapseGroups*/ true);
		_file = image.getFile ();
		_pixels = image.getPixels ();
//		_count = image.getCount ();
		_hash = image.getHash ();
		_random = image.getRandom ();
		_orientation = image.getOrientation ();

		_scaledWidth = image.getScaledWidth ();
		_scaledHeight = image.getScaledHeight ();
		_scale = image.getScale ();
	}

	///////////////////////////////////////////////////////////////////////////
	//this ctor optionally reads the attributes from the image file on the disk
	public AlbumImage (String name, String subFolder, boolean readAttrs)
	{
		AlbumProfiling.getInstance ().enter (7, subFolder, "ctor");

		String nameWithExt = name + AlbumFormInfo._ImageExtension;
		String imagePath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ false) + subFolder;

		_name = name;
		_subFolder = subFolder;
		_imagePath = VendoUtils.appendSlash (imagePath);
		_file = new File (imagePath, nameWithExt);

		if (readAttrs) {
			BasicFileAttributes attrs = null;
			try {
			    Path file = FileSystems.getDefault ().getPath (imagePath, nameWithExt);
				attrs = Files.readAttributes (file, BasicFileAttributes.class);

			} catch (Exception ee) {
				_log.error ("AlbumImage ctor: error reading file attributes \"" + nameWithExt + "\"");

			} finally {
				_numBytes = attrs.size ();
				_modified = attrs.lastModifiedTime ().toMillis (); //millisecs
			}

			//read (w x h) ints (int = 4 bytes) of data from image array starting at offset x, y
			final int w = 5;
			final int h = 1;
			int rgbIntArray[] = new int [w * h];

			BufferedImage image = JpgUtils.readImage (_file);
			_width = image.getWidth ();
			_height = image.getHeight ();
			image.getRGB (_width / 2, _height / 2, w, h, rgbIntArray, 0, w);

			//convert exact RGB data to String
			StringBuilder sb = new StringBuilder ();
			Formatter formatter = new Formatter (sb, Locale.US);
			for (int ii = 0; ii < rgbIntArray.length; ii++)
				formatter.format ("%08x", rgbIntArray[ii]); //note: writing integers
			_rgbData = sb.toString ();
			formatter.close ();

			setExifDates (readExifDates (_file));

		} else {
			_numBytes = -1;
			_modified = -1;
			_width = -1;
			_height = -1;
			_rgbData = null;
		}

		AlbumProfiling.getInstance ().exit (7, subFolder, "ctor");
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		return toString (/*full*/ false, /*collapseGroups*/ false);
	}
	public String toString (boolean full, boolean collapseGroups)
	{
		StringBuffer sb = new StringBuffer (256);
		sb.append (getName ()).append (", ")
		  .append (getWidth ()).append ("x").append (getHeight ()).append (", ")
		  .append (getNumBytes () / 1024).append ("KB, ")
		  .append (getModifiedString ());

		if (full) { //typically true for servlet, false for CLI

			int exifDateIndex = AlbumFormInfo.getInstance ().getExifDateIndex ();
			String exifDateString = getExifDateString (exifDateIndex);
			if (exifDateString.length () != 0) {
				sb.append (HtmlNewline)
				  .append (exifDateString);
			}

			String tagStr = AlbumTags.getInstance ().getTagsForBaseName (getBaseName (collapseGroups), collapseGroups);
			if (tagStr.length () != 0) {
				sb.append (HtmlNewline)
				  .append (tagStr);
			}

			if (AlbumFormInfo._showRgbData) { //debugging
				String string = getRgbData ();
//				String string = String.format ("0x%08x", getRgbData ().hashCode ());

				sb.append (", ")
				  .append (string);
			}
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	//allow inexact matches
	public boolean equalAttrs (AlbumImage image, boolean looseCompare, boolean ignoreBytes, boolean useExifDates, int exifDateIndex)
	{
		if (useExifDates) {
			return equalExifDates (image, exifDateIndex);
		}

		if (equalAttrsStrict (image, ignoreBytes)) {
			return true;
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	//check for exact match: bytes (optional), width, height, exact RGB data
	private boolean equalAttrsStrict (AlbumImage image, boolean ignoreBytes)
	{
		return (ignoreBytes || (!ignoreBytes && (getNumBytes () == image.getNumBytes ()))) &&
				getWidth () == image.getWidth () &&
				getHeight () == image.getHeight () &&
				getRgbData ().equals (image.getRgbData ());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean equalBase (AlbumImage image, boolean collapseGroups)
	{
		if (image.getBaseName (collapseGroups).equals (getBaseName (collapseGroups)))
			return true;
		else
			return false;
	}

	///////////////////////////////////////////////////////////////////////////
	//remove trailing digits (and optionally '-')
	public static String getBaseName (String name, boolean collapseGroups)
	{
		final String regex1 = "-\\d*$";			//match trailing [dash][digits]
		final String regex2 = "\\d*\\-\\d*$";	//match trailing [digits][dash][digits]

		return name.replaceAll (collapseGroups ? regex2 : regex1, "");
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized String getBaseName (boolean collapseGroups)
	{
		if (!collapseGroups) {
			if (_baseName1 == null) {
				_baseName1 = getBaseName (getName (), collapseGroups);
			}
			return _baseName1;

		} else { //collapseGroups
			if (_baseName2 == null) {
				_baseName2 = getBaseName (getName (), collapseGroups);
			}
			return _baseName2;
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
		return getName () + AlbumFormInfo._ImageExtension;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized String getNamePlus ()
	{
		if (_namePlus == null) {
			_namePlus = getName () + "." + getModified () + "." + getNumBytes ();
		}

		return _namePlus;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized String getNameFirstLetterLower ()
	{
		if (_nameFirstLetterLower == null) {
			_nameFirstLetterLower = getName ().substring (0, 1).toLowerCase ();
		}

		return _nameFirstLetterLower;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSubFolder ()
	{
		return _subFolder;
	}
	///////////////////////////////////////////////////////////////////////////
	public int getSubFolderInt ()
	{
		return subFolderToByte (_subFolder);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getImagePath ()
	{
		return _imagePath;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized File getFile ()
	{
		if (_file == null) {
			_file = new File (getImagePath (), getNameWithExt ());
		}

		return _file;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getNumBytes ()
	{
		return _numBytes;
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
	public String getScaleString ()
	{
		if (_scale < 0) {
			return "??";
		} else {
			return Integer.toString (_scale) + "%";
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public long getModified ()
	{
		return _modified;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized String getModifiedString ()
	{
		if (_modifiedString == null) {
			_modifiedString = _dateFormat.format (new Date (getModified ()));
		}

		return _modifiedString;
	}

/*TODO - fix this
	///////////////////////////////////////////////////////////////////////////
	public int getCount ()
	{
		return _count;
	}

	public void setCount (int count)
	{
		_count = count;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized long getPixels ()
	{
		if (_pixels < 0) {
			_pixels = getWidth () * getHeight ();
		}

		return _pixels;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized AlbumOrientation getOrientation ()
	{
		if (_orientation == AlbumOrientation.ShowAny) {
			final int squareSlop = 10; //allowable variation from exactly square that will still be considered square

			int diff = getWidth () - getHeight ();
			if (Math.abs (diff) <= squareSlop) {
				_orientation = AlbumOrientation.ShowSquare;
			} else if (diff > 0) {
				_orientation = AlbumOrientation.ShowLandScape;
			} else { //diff < 0
				_orientation = AlbumOrientation.ShowPortrait;
			}
		}

		return _orientation;
	}

	///////////////////////////////////////////////////////////////////////////
	//used for de-dup'ing
	//calculated on demand and cached
	public synchronized long getHash ()
	{
		//Note: looseCompare should not be a factor here since AlbumImageComparator does not sort by hash in that case
//		boolean looseCompare = AlbumFormInfo.getInstance ().getLooseCompare ();

		if (_hash < 0) {
//			if (false) { //"loose" hash
//				_hash = (getNumBytes () * 10000) ^ (getWidth () * 100) ^ getHeight ();
//
//			} else { // perfect hash?
				_hash = Math.abs (getRgbData ().hashCode ());

//				final long maxWidth  = 1 << 12; // 4K
//				final long maxHeight = 1 << 12; // 4K
//				final long maxBytes  = 1 << 22; // 4M
//
//				_hash = getWidth ();
//				_hash = _hash * maxHeight + getHeight ();
//				_hash = _hash * maxBytes + getNumBytes ();
//			}

//TODO - include getOrientation ()

//			if (_hash < 0)
//				throw new RuntimeException ("AlbumImage.getHash: hash overflowed long");

//			if (AlbumFormInfo._logLevel >= 10)
			if (_hash < 0)
				_log.debug ("AlbumImage.getHash: " + _hash + ", " + getName ());
		}

		return _hash;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized int getRandom ()
	{
		if (_random < 0) {
			_random = Math.abs (_randomGenerator.nextInt ());
		}

		return _random;
	}

	///////////////////////////////////////////////////////////////////////////
	//used for de-dup'ing
	public String getRgbData ()
	{
		if (_rgbData == null || _rgbData.length () == 0) {
			_log.debug ("AlbumImage.getRgbData: _rgbData not initialized for \"" + getName () + "\"");
//			throw new RuntimeException ("AlbumImage.getRgbData: _rgbData not initialized for \"" + getName () + "\"");
		}

		return _rgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isValidImageName (String name) //name without extension
	{
		final Pattern goodPattern = Pattern.compile ("^[A-Za-z]{2,}\\d{2,}\\-\\d{2,}$"); //match: [2 or more alpha][2 or more digits][dash][2 or more digits]

		return goodPattern.matcher (name).matches ();
	}

	///////////////////////////////////////////////////////////////////////////
	public /*synchronized*/ ByteBuffer readScaledImageData ()
	{
//		AlbumProfiling.getInstance ().enter (5); //don't profile; this is called by threads

		String nameWithExt = getImagePath () + getName () + AlbumFormInfo._RgbDataExtension;

		ByteBuffer scaledImageData = null;
		try (FileChannel fileChannel = new FileInputStream (nameWithExt).getChannel ()) {
//Bug in FileChannel#map() in Java 7 prevents file from closing: http://stackoverflow.com/questions/13065358/java-7-filechannel-not-closing-properly-after-calling-a-map-method
//			scaledImageData = fileChannel.map (FileChannel.MapMode.READ_ONLY, 0, fileChannel.size ());
			scaledImageData = ByteBuffer.allocate ((int) fileChannel.size ());
			fileChannel.read (scaledImageData, 0);

		} catch (Exception ee) {
			_log.error ("AlbumImage.readScaledImageData: error reading image data file \"" + nameWithExt + "\"", ee);
		}

		if (scaledImageData != null && scaledImageData.position () < 10000) {
			_log.error ("AlbumImage.readScaledImageData: error: unexpected size (" + scaledImageData.position () + " bytes) for " + nameWithExt);
		}

//		AlbumProfiling.getInstance ().exit (5);

		return scaledImageData;
	}

	///////////////////////////////////////////////////////////////////////////
	public static void removeRgbDataFileFromFileSystem (String imagePath)
	{
		String rgbDataFilePath = imagePath.replace (AlbumFormInfo._ImageExtension, AlbumFormInfo._RgbDataExtension);

		if (AlbumFormInfo._logLevel >= 8)
			_log.debug ("AlbumImage.removeRgbDataFileFromFileSystem: rgbDataFilePath = " + rgbDataFilePath);

		Path file = FileSystems.getDefault ().getPath (rgbDataFilePath);
		if (Files.exists (file)) { //rgbDataFile might not exist: e.g., if it was already renamed with the .jpg file by mov.exe
			try {
				Files.delete (file);

			} catch (Exception ee) {
				_log.error ("AlbumImage.removeRgbDataFileFromFileSystem: file delete failed: " + rgbDataFilePath, ee);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getScaledImageDiff (ByteBuffer buffer1, ByteBuffer buffer2, int maxRgbDiffs)
	{
//		AlbumProfiling.getInstance ().enter (5); //too expensive

		if (buffer1 == null || buffer2 == null) {
			return Integer.MAX_VALUE;
		}

		buffer1.rewind (); //always rewind before using
		buffer2.rewind (); //always rewind before using
		int numBytes = buffer1.remaining ();

		int maxAcceptableTotalDiff = maxRgbDiffs * numBytes;
		int totalDiff = 0;
		try {
			for (int ii = 0; ii < numBytes; ii++) {
				int b1 = buffer1.get (ii) & 0xFF;
				int b2 = buffer2.get (ii) & 0xFF;

				int diff = Math.abs (b2 - b1);
				totalDiff += diff;

				//drop out of loop once averageDiff will exceed maxRgbDiffs
				if (totalDiff > maxAcceptableTotalDiff) {
					throw new Exception ();
				}
			}

		} catch (Exception ee) { //this can occur if we failed to read/parse the image
			return Integer.MAX_VALUE;
		}

//		AlbumProfiling.getInstance ().exit (5);

		return totalDiff / numBytes; //averageDiff
	}

	///////////////////////////////////////////////////////////////////////////
	public synchronized void createRgbDataFile () //throws Exception
	{
		String nameWithExt = getImagePath () + getName () + AlbumFormInfo._RgbDataExtension;

		if (AlbumFormInfo._logLevel >= 8) {
			_log.debug ("AlbumImage.createRgbDataFile: " + nameWithExt);
		}

		BufferedImage image = JpgUtils.readImage (_file);

		int imageWidth = image.getWidth ();
		int imageHeight = image.getHeight ();

		int scaledWidth = 0;
		int scaledHeight = 0;
		if (imageWidth > imageHeight) {
			scaledWidth = 144;
			scaledHeight = 96;
		} else if (imageWidth < imageHeight) {
			scaledWidth = 96;
			scaledHeight = 144;
		} else { //imageWidth == imageHeight
			scaledWidth = scaledHeight = 120;
		}

		ByteBuffer scaledImageData = null;
		final int hints = BufferedImage.SCALE_FAST;
		try {
			BufferedImage scaledImage = toBufferedImage (image.getScaledInstance (scaledWidth, scaledHeight, hints));
			//get a subset of pixels from center of image
			int[] rawScaledImageData = scaledImage.getRGB (scaledWidth / 4, scaledHeight / 4, scaledWidth / 2, scaledHeight / 2, null, 0, scaledWidth / 2);

			//write thumbnail as PNG
//			String pngFilename = imageFile.getName (imageFile.getNameCount () - 1).toString ().replace (AlbumFormInfo._ImageExtension, ".png");
//			File pngFile = new File (pngFilename);
//			BufferedImage pngImage = new BufferedImage (scaledWidth / 2, scaledHeight / 2, image.getType ());
//			pngImage.setRGB (0, 0, scaledWidth / 2, scaledHeight / 2, rawScaledImageData, 0, scaledWidth / 2);
//			ImageIO.write (pngImage, "png", pngFile);

			scaledImageData = shrinkRgbData (rawScaledImageData);

		} catch (Exception ee) {
			_log.error ("AlbumImage.createRgbDataFile: error reading/scaling image file \"" + nameWithExt + "\"", ee);
		}

		try (FileChannel fileChannel = new FileOutputStream (nameWithExt).getChannel ()) {
			scaledImageData.rewind (); //always rewind before using
			fileChannel.write (scaledImageData);

		} catch (Exception ee) {
			_log.error ("AlbumImage.createRgbDataFile: error writing RGB data file \"" + nameWithExt + "\"", ee);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//only keep R, G, and B data, not A data
	private static ByteBuffer shrinkRgbData (int[] rawScaledImageData) throws Exception
	{
		int numBytes = ((rawScaledImageData.length * 4) * 3) / 4;
		ByteBuffer buffer = ByteBuffer.allocate (numBytes);

		for (int ii = 0; ii < rawScaledImageData.length; ii++) {
			int argb = rawScaledImageData[ii];
//			buffer.put ((byte) ((argb >> 24) & 0xFF)); //A - ignore A data
			buffer.put ((byte) ((argb >> 16) & 0xFF)); //R
			buffer.put ((byte) ((argb >>  8) & 0xFF)); //G
			buffer.put ((byte) ((argb      ) & 0xFF)); //B
		}

		return buffer;
	}

	///////////////////////////////////////////////////////////////////////////
	//Creating a Buffered Image from an Image:
	//original from: http://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
	public static BufferedImage toBufferedImage (Image image)
	{
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

//		AlbumProfiling.getInstance ().enter (5);

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage (image.getWidth (null), image.getHeight (null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D g2d = bimage.createGraphics ();
		g2d.drawImage (image, 0, 0, null);
		g2d.dispose ();

//		AlbumProfiling.getInstance ().exit (5);

		// Return the buffered image
		return bimage;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized void calculateScaledSize (int maxImageWidth, int maxImageHeight)
	{
		double maxImageScaleFactor = (double) AlbumFormInfo._maxImageScalePercent / 100;

//TODO - collapse these two cases into one
		if (_width < 0 || _height < 0) {
			//scale images to available space
			int width = 150;	//unknown image size, set a fake size for a 2:3 horizontally-oriented image
			int height = 100;

			double scaleForWidth = (double) maxImageWidth / (double) width;
			double scaleForHeight = (double) maxImageHeight / (double) height;

			double scale = Math.min (scaleForWidth, scaleForHeight);
//			scale = Math.min (scale, maxImageScaleFactor); //limit magnification to maxImageScaleFactor

			_scale = -1;//(int) (scale * 100);
			_scaledWidth = (int) (width * scale);
			_scaledHeight = (int) (height * scale);

		} else {
			//scale images to available space
			double scaleForWidth = (double) maxImageWidth / (double) _width;
			double scaleForHeight = (double) maxImageHeight / (double) _height;

			double scale = Math.min (scaleForWidth, scaleForHeight);
			scale = Math.min (scale, maxImageScaleFactor); //limit magnification to maxImageScaleFactor

			_scale = (int) (scale * 100);
			_scaledWidth = (int) (_width * scale);
			_scaledHeight = (int) (_height * scale);
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
*/
	}

	///////////////////////////////////////////////////////////////////////////
	//validates values: invalid values are set to 0
	//also calculates earliestExifDate and latestExifDate
	private void setExifDates (long[] exifDates)
	{
		_exifDates = exifDates;

		long earliestExifDate = Long.MAX_VALUE;
		long latestExifDate = 0;

		for (int ii = 0; ii < NumFileExifDates; ii++) {
			if (!isExifDateValid (_exifDates[ii])) {
				_exifDates[ii] = 0;
			}

			if (_exifDates[ii] > 0) {
				earliestExifDate = Math.min (_exifDates[ii], earliestExifDate);
			}
			latestExifDate = Math.max (_exifDates[ii], latestExifDate);
		}

		_exifDates[4] = (earliestExifDate < Long.MAX_VALUE ? earliestExifDate : 0);
		_exifDates[5] = latestExifDate; //(latestExifDate > 0 ? latestExifDate : 0);
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public boolean hasExifDate ()
	{
		if (!_hasExifDateChecked) {
			_hasExifDateChecked = true;

			for (int ii = 0; ii < NumFileExifDates; ii++) {
				if (_exifDates[ii] > 0) {
					_hasExifDate = true;
					break;
				}
			}
		}

		return _hasExifDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getExifDate (int exifDateIndex)
	{
		return _exifDates[exifDateIndex];
	}
	private long[] getExifDates ()
	{
		return _exifDates;
	}

	//following methods are used by mybatis/AlbumImageResultMap
	@SuppressWarnings("unused")
	private long getExifDate0 ()
	{
		return _exifDates[0];
	}
	@SuppressWarnings("unused")
	private long getExifDate1 ()
	{
		return _exifDates[1];
	}
	@SuppressWarnings("unused")
	private long getExifDate2 ()
	{
		return _exifDates[2];
	}
	@SuppressWarnings("unused")
	private long getExifDate3 ()
	{
		return _exifDates[3];
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean isExifDateValid (long exifDate)
	{
		final long maxValidExifDate = new GregorianCalendar ().getTimeInMillis () + (30 * 24 * 3600 * 1000); //30 days from now
		final long minValidExifDate = new GregorianCalendar (1980, 1, 1).getTimeInMillis (); //first second of 1980

		return (exifDate > minValidExifDate && exifDate < maxValidExifDate);
	}

	///////////////////////////////////////////////////////////////////////////
	//each string calculated on demand and cached
	//returns empty string if exifDate is not valid or 0 (i.e., not set)
	private synchronized String getExifDateString (int exifDateIndex)
	{
		if (_exifDateStrings == null) {
			_exifDateStrings = new String[NumExifDates];
		}

		if (_exifDateStrings[exifDateIndex] == null) {
			long exifDate = getExifDate (exifDateIndex);
			if (isExifDateValid (exifDate)) {
				_exifDateStrings[exifDateIndex] = _dateFormat.format (new Date (exifDate));
			} else {
				_exifDateStrings[exifDateIndex] = new String ();
			}
		}

		return _exifDateStrings[exifDateIndex];
	}

	///////////////////////////////////////////////////////////////////////////
	//ignore exifDates out of range
	public boolean equalExifDates (AlbumImage image, int exifDateIndex)
	{
		long exifDate1 = getExifDate (exifDateIndex);
		long exifDate2 = image.getExifDate (exifDateIndex);

		if (exifDate1 == 0 || exifDate2 == 0) {
			return false;
		}

		return (exifDate1 == exifDate2);
	}

	///////////////////////////////////////////////////////////////////////////
	//ignore exifDates out of range
	public int compareExifDates (AlbumImage image, int exifDateIndex)
	{
		long exifDate1 = getExifDate (exifDateIndex);
		long exifDate2 = image.getExifDate (exifDateIndex);

		if (exifDate1 == exifDate2) {
			return 0;

//		} else if (exifDate1 == 0 && exifDate2 != 0) {
//			return 1;
//
//		} else if (exifDate1 != 0 && exifDate2 == 0) {
//			return -1;

		} else {
			return (exifDate1 < exifDate2 ? 1 : -1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//no validation
	private long[] readExifDates (File file)
	{
		AlbumProfiling.getInstance ().enter (5);

		long[] exifDates = new long[NumExifDates];

		Metadata metadata = null;
		try {
			metadata = ImageMetadataReader.readMetadata (file);
		} catch (Exception ee) {
//TODO - ignore some, but not all exceptions?
			return exifDates;
		}

		try {
			ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType (ExifSubIFDDirectory.class);
			Date date = directory.getDate (ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
			exifDates[0] = date.getTime ();
		} catch (Exception ee) {
			//ignore
		}

		try {
			ExifIFD0Directory directory = metadata.getFirstDirectoryOfType (ExifIFD0Directory.class);
			Date date = directory.getDate (ExifIFD0Directory.TAG_DATETIME);
			exifDates[1] = date.getTime ();
		} catch (Exception ee) {
			//ignore
		}

		try {
			IptcDirectory directory = metadata.getFirstDirectoryOfType (IptcDirectory.class);
			Date date = directory.getDate (IptcDirectory.TAG_DATE_CREATED);
			int time = directory.getInt (IptcDirectory.TAG_TIME_CREATED);
			exifDates[2] = date.getTime () + (time * 1000); //this probably does not handle TZ correctly
		} catch (Exception ee) {
			//ignore
		}

		try {
			XmpDirectory directory = metadata.getFirstDirectoryOfType (XmpDirectory.class);
			Date date = directory.getDate (XmpDirectory.TAG_DATETIME_ORIGINAL);
			exifDates[3] = date.getTime ();
		} catch (Exception ee) {
			//ignore
		}

		AlbumProfiling.getInstance ().exit (5);

		return exifDates;
	}

	///////////////////////////////////////////////////////////////////////////
	//the subFolder column is stored as an TINYINT value instead of a string, (ostensibly) to improve database indexing performance
	//used by CLI and servlet
	public static byte subFolderToByte (String subFolderStr)
	{
		char subFolderChar = subFolderStr.charAt (0);
		byte subFolderByte = (byte) (subFolderChar - 'a' + 1); //1-based: 'a'=1, 'b'=2, etc.

		return subFolderByte;
	}

	///////////////////////////////////////////////////////////////////////////
	//the subFolder column is stored as an TINYINT value instead of a string, (ostensibly) to improve database indexing performance
	//used by CLI and servlet
	public static String subFolderFromByte (int subFolderInt)
	{
		char buf[] = {(char) (subFolderInt + 'a' - 1)}; //1-based: 'a'=1, 'b'=2, etc.
		return new String (buf);
	}


	//members

	//attributes read from database
	private final String _name; //name only, no file extension
	private final long _numBytes;
	private final int _width;
	private final int _height;
	private final long _modified;
	private final String _rgbData;
	private final String _subFolder;
	private final String _imagePath;
	private long[] _exifDates = null;

	private File _file = null;
	private String _namePlus = null;
	private String _baseName1 = null; //for collapseGroups = false
	private String _baseName2 = null; //for collapseGroups = true
	private String _nameFirstLetterLower = null;
	private long _pixels = -1;
	private int _scaledWidth = -1;
	private int _scaledHeight = -1;
	private int _scale = -1;
	private int _random = -1;
//	private int _count = -1;
	private long _hash = -1;
	private boolean _hasExifDateChecked = false;
	private boolean _hasExifDate = false;
	private String[] _exifDateStrings = null;
	private String _modifiedString = null;
	private AlbumOrientation _orientation = AlbumOrientation.ShowAny;

	public static final int NumExifDates = 6;		//4 exif dates read from database + 2 calculated
	public static final int NumFileExifDates = 4;	//4 exif dates read from database + 2 calculated

	public static final String HtmlNewline = "&#13;";

	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

//	private static final String NL = System.getProperty ("line.separator");
//	private static final String _marker = ":::";
	private static final Random _randomGenerator = new Random ();

	private static Logger _log = LogManager.getLogger ();
}
