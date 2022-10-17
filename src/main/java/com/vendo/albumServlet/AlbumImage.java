//AlbumImage.java

package com.vendo.albumServlet;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import com.vendo.jpgUtils.JpgUtils;
import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
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
import java.util.*;
import java.util.regex.Pattern;


public class AlbumImage implements Comparable<AlbumImage>
{
	///////////////////////////////////////////////////////////////////////////
	//this ctor is used by mybatis
	public AlbumImage (int nameId, String name, String subFolder, long numBytes, int width, int height, long modified, String rgbData,
					   long exifDate0, long exifDate1, long exifDate2, long exifDate3)
	{
		String imagePath = AlbumFormInfo.getInstance ().getRootPath (false) + subFolder;
		long[] exifDates = new long[] {exifDate0, exifDate1, exifDate2, exifDate3, 0, 0};

		_nameId = nameId;
		_name = name;
		_numBytes = numBytes;
		_width = width;
		_height = height;
		_modified = modified;
		_rgbData = rgbData;
		setExifDates (exifDates);

		_subFolder = subFolder;
		_imagePath = VendoUtils.appendSlash (imagePath);

		_namePlusAttrs = null;
//		_nameFirstLettersLower = null;
		_baseName1 = null;
		_baseName2 = null;
		_tagString1 = null;
		_tagString2 = null;
		_file = null;
		_pixels = -1;
//		_count = -1;
		_rgbHash = -1;
		_random = -1;
		_modifiedString = null;
		_orientation = AlbumOrientation.ShowAny;
	}

//TODO - replace with clone?
/*unused?
	///////////////////////////////////////////////////////////////////////////
	public AlbumImage (AlbumImage image)
	{
		_nameId = image.getNameId ();
		_name = image.getName ();
		_namePlusAttrs = image.getNamePlusAttrs ();
		_nameFirstLettersLower = image.getNameFirstLettersLower ();
		_numBytes = image.getNumBytes ();
		_width = image.getWidth ();
		_height = image.getHeight ();
		_modified = image.getModified ();
		_rgbData = image.getRgbData ();
		_modifiedString = image.getModifiedString ();
		setExifDates (image.getExifDates ());

		_subFolder = image.getSubFolder ();
		_imagePath = image.getImagePath ();

		_baseName1 = image.getBaseName (false);
		_baseName2 = image.getBaseName (true);
		_tagString1 = image.getTagString (false);
		_tagString2 = image.getTagString (true);
		_file = image.getFile ();
		_pixels = image.getPixels ();
//		_count = image.getCount ();
		_rgbHash = image.getHash ();
		_random = image.getRandom ();
		_orientation = image.getOrientation ();

		_scaledWidth = image.getScaledWidth ();
		_scaledHeight = image.getScaledHeight ();
		_scale = image.getScale ();
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//this ctor optionally reads the attributes from the image file on the disk
	public AlbumImage (String name, String subFolder, boolean readAttrs)
	{
		AlbumProfiling.getInstance ().enter (7, subFolder, "ctor");

		String nameWithExt = name + AlbumFormInfo._ImageExtension;
		String imagePath = AlbumFormInfo.getInstance ().getRootPath (false) + subFolder;

		_nameId = -1;
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
			int[] rgbIntArray = new int [w * h];

			BufferedImage image = JpgUtils.readImage (_file);
			_width = image.getWidth ();
			_height = image.getHeight ();
			image.getRGB (_width / 2, _height / 2, w, h, rgbIntArray, 0, w);

			//convert exact RGB data to String
			StringBuilder sb = new StringBuilder ();
			Formatter formatter = new Formatter (sb, Locale.US);
			for (int i : rgbIntArray) {
				formatter.format("%08x", i); //note: writing integers
			}
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
	//required by Set<VPair<AlbumImage, AlbumImage>>
	//note compareTo only operates on name
	public int compareTo(AlbumImage other)
	{
		return getName().compareToIgnoreCase(other.getName()); //TODO - is this correct sense??
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	@Override
	public int hashCode ()
	{
		if (_hashCode == -1) {
			_hashCode = Objects.hash(getName(), getNumBytes(), getWidth(), getHeight(), getModified());
		}

		return _hashCode;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals (Object obj)
	{
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AlbumImage)) {
			return false;
		}

		AlbumImage other = (AlbumImage) obj;
		return getName ().compareTo (other.getName ()) == 0 &&
				getNumBytes () == other.getNumBytes () &&
				getWidth () == other.getWidth () &&
				getHeight () == other.getHeight () &&
				getModified () == other.getModified ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		return toString (false, false);
	}
	public String toString (boolean full, boolean collapseGroups)
	{
		StringBuffer sb = new StringBuffer (256);
		sb.append (getName ()).append (", ");
		sb.append (getWidth ()).append ("x").append (getHeight ()).append (", ");
		sb.append (getNumBytes () / 1024).append ("KB, ");
		if (AlbumFormInfo.getShowRgbData () && AlbumFormInfo.getInstance ().getMode () == AlbumMode.DoDup) { //debugging
			sb.append (String.format ("0x%08X", getRgbData ().hashCode ())).append (", ");
		}
		sb.append (getModifiedString ());

		if (full) { //typically true for servlet, false for CLI

			int exifDateIndex = AlbumFormInfo.getInstance ().getExifDateIndex ();
			String exifDateString = getExifDateString (exifDateIndex);
			if (exifDateString.length () != 0) {
				sb.append (HtmlNewline)
				  .append (exifDateString);
			}

//			String tagStr = AlbumTags.getInstance ().getTagsForBaseName (getBaseName (collapseGroups), collapseGroups);
			String tagStr = getTagString (collapseGroups);
			if (tagStr.length () != 0) {
				sb.append (HtmlNewline)
				  .append (tagStr);
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

		return equalAttrsStrict(image, ignoreBytes);
	}

	///////////////////////////////////////////////////////////////////////////
	//check for exact match: bytes (optional), width, height, exact RGB data
	private boolean equalAttrsStrict (AlbumImage image, boolean ignoreBytes)
	{
		return (ignoreBytes || getNumBytes() == image.getNumBytes()) &&
				getWidth() == image.getWidth() &&
				getHeight() == image.getHeight() &&
				getRgbData().equals(image.getRgbData());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean equalBase (AlbumImage image, boolean collapseGroups)
	{
		return image.getBaseName(collapseGroups).equals(getBaseName(collapseGroups));
	}

	///////////////////////////////////////////////////////////////////////////
	//remove trailing digits (and optionally '-')
	//expected formats:
	// Foo01-01 -> will return either Foo01 or Foo
	// Foo01 -> will return either Foo01 or Foo
	public static String getBaseName (String name) {
		return getBaseName(name, true);
	}
	public static String getBaseName (String name, boolean collapseGroups)
	{
		final String regex1 = "-\\d*$";			//match trailing [dash][digits]
//		final String regex2 = "\\d*-\\d*$";		//match trailing [digits][dash][digits]
		final String regex2 = "[\\d-].*$";		//match everything starting with first digit or dash

		return name.replaceAll (collapseGroups ? regex2 : regex1, "");
	}

/* old way
	///////////////////////////////////////////////////////////////////////////
	//remove trailing digits (and optionally '-')
	//expected format: Foo01-01; will return either Foo01 or Foo
	public static String getBaseName (String name, boolean collapseGroups)
	{
		final String regex1 = "-\\d*$";			//match trailing [dash][digits]
		final String regex2 = "\\d*-\\d*$";	//match trailing [digits][dash][digits]

		String result = name.replaceAll (collapseGroups ? regex2 : regex1, "");
		return result;
//		return name.replaceAll (collapseGroups ? regex2 : regex1, "");
	}
//BUG: note that the regex for collapseGroups = true is DIFFERENT between these two methods!
	///////////////////////////////////////////////////////////////////////////
	//remove trailing digits and any dashes '-'
	//this version simply removes everything starting with the first digit (collapseGroups = true is implied)
	public static String getBaseName (String name)
	{
		String result = name.replaceAll ("[\\d-].*$", "");
		return result;
//		return name.replaceAll ("[\\d-].*$", "");
	}
*/

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

	/////////////////////////////////////////////////////////////////////////
	public int getNameId ()
	{
		return _nameId;
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
	public synchronized String getNamePlusAttrs ()
	{
		if (_namePlusAttrs == null) {
			_namePlusAttrs = getName () + "." + getModified () + "." + getNumBytes ();
		}

		return _namePlusAttrs;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
//	public synchronized String getNameFirstLettersLower ()
//	{
//		if (_nameFirstLettersLower == null) {
//			_nameFirstLettersLower = AlbumImageDao.getInstance ().getSubFolderFromImageName (getName ());
//		}
//
//		return _nameFirstLettersLower;
//	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized String getTagString (boolean collapseGroups)
	{
		if (!collapseGroups) {
			if (_tagString1 == null) {
				_tagString1 = String.join(", ", AlbumTags.getInstance ().getTagsForBaseName (getBaseName (collapseGroups), collapseGroups));
			}
			return _tagString1;

		} else { //collapseGroups
			if (_tagString2 == null) {
				_tagString2 = String.join(", ", AlbumTags.getInstance ().getTagsForBaseName (getBaseName (collapseGroups), collapseGroups));
			}
			return _tagString2;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSubFolder ()
	{
		return _subFolder;
	}

//remove "helper" method; just have callers call DAO method directly
	///////////////////////////////////////////////////////////////////////////
//	public static String getSubFolderFromName (String name)
//	{
//		return AlbumImageDao.getInstance ().getSubFolderFromImageName (name);
//	}

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
//	public int getScale ()
//	{
//		return _scale;
//	}

	///////////////////////////////////////////////////////////////////////////
	public String getScaleString ()
	{
		return _scale < 0 ? "??" : _scale + "%";
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
		if (_pixels == -1) {
			_pixels = (long) getWidth () * (long) getHeight ();
		}

		return _pixels;
	}

	///////////////////////////////////////////////////////////////////////////
	public int compareToByPixels (AlbumImage other)
	{
		return AlbumImages.compareToWithSlop (getPixels(), other.getPixels(), true, 0.5);
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized AlbumOrientation getOrientation ()
	{
		if (_orientation == AlbumOrientation.ShowAny) {
			_orientation = AlbumOrientation.getOrientation (getWidth (), getHeight ());
		}

		return _orientation;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean matchOrientation (AlbumOrientation orientation)
	{
		return orientation == AlbumOrientation.ShowAny || orientation == getOrientation ();
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isRgbDatExpectedSize (int size)
	{
		if (getOrientation () == AlbumOrientation.ShowAny) {
			//if we do not know the orientation, verify that the size is one of the valid values
			return size == RgbDatSizeSquare || size == RgbDatSizeRectagular;
		} else if (getOrientation () == AlbumOrientation.ShowSquare) {
			return size == RgbDatSizeSquare;
		} else {
			return size == RgbDatSizeRectagular;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	//currently only used by AlbumImageComparator#compare, for sorting for doDup
	public synchronized long getRgbHash ()
	{
		if (_rgbHash == -1) {
			_rgbHash = getRgbData ().hashCode ();
		}

		return _rgbHash;
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	public synchronized int getRandom ()
	{
		if (_random == -1) {
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
		final Pattern goodPattern = Pattern.compile ("^[A-Za-z]{2,}\\d{2,}\\-\\d{2,}$"); //regex - match: [2 or more alpha][2 or more digits][dash][2 or more digits]

		return goodPattern.matcher (name).matches ();
	}

	///////////////////////////////////////////////////////////////////////////
	public /*synchronized*/ ByteBuffer readScaledImageData ()
	{
//		AlbumProfiling.getInstance ().enter (5); //don't profile; this is called by threads

		String nameWithExt = getImagePath () + getName () + AlbumFormInfo._RgbDataExtension;

//TODO - convert to try-with-resources
		ByteBuffer scaledImageData = null;
		FileInputStream inputStream = null;
		FileChannel fileChannel = null;
		try {
			inputStream = new FileInputStream (nameWithExt);
			fileChannel = inputStream.getChannel ();
			scaledImageData = ByteBuffer.allocate ((int) fileChannel.size ());
			fileChannel.read (scaledImageData, 0);

		} catch (Exception ee) {
			_log.error ("AlbumImage.readScaledImageData: error reading image data file \"" + nameWithExt + "\"", ee);

		} finally {
			if (fileChannel != null) {
				try { fileChannel.close (); } catch (Exception ex) { _log.error (ex); ex.printStackTrace (); }
			}
			if (inputStream != null) {
				try { inputStream.close (); } catch (Exception ex) { _log.error (ex); ex.printStackTrace (); }
			}
		}

// old way - thread hangs in some cases when file does not exist
//		ByteBuffer scaledImageData = null;
//		try (FileChannel fileChannel = new FileInputStream (nameWithExt).getChannel ()) {
////Bug in FileChannel#map() in Java 7 prevents file from closing: http://stackoverflow.com/questions/13065358/java-7-filechannel-not-closing-properly-after-calling-a-map-method
////			scaledImageData = fileChannel.map (FileChannel.MapMode.READ_ONLY, 0, fileChannel.size ());
//			scaledImageData = ByteBuffer.allocate ((int) fileChannel.size ());
//			fileChannel.read (scaledImageData, 0);
//
//		} catch (Exception ee) {
//			_log.error ("AlbumImage.readScaledImageData: error reading image data file \"" + nameWithExt + "\"", ee);
//		}

//		AlbumProfiling.getInstance ().exit (5);

		validateScaledImageData (scaledImageData, nameWithExt); //emits errors

		return scaledImageData;
	}

	///////////////////////////////////////////////////////////////////////////
	public void validateScaledImageData (ByteBuffer scaledImageData, String nameWithExt)
	{
//		AlbumProfiling.getInstance ().enter (5); //don't profile; this is called by threads

		//basic validations
		if (scaledImageData != null) {
			scaledImageData.rewind (); //always rewind before using
			int numBytes = scaledImageData.remaining ();

			//check for valid size
			if (!isRgbDatExpectedSize (numBytes)) {
				String message = "unexpected size (" + numBytes + " bytes) for " + nameWithExt;
				_log.error ("AlbumImage.validateScaledImageData: " + message);
				AlbumFormInfo.getInstance ().addServletError ("Error: " + message);
			}

			//check for average value out of range
			int sum = 0;
			for (int ii = 0; ii < numBytes; ii++) {
				sum += scaledImageData.get (ii) & 0xFF;
			}
			int average = (int) Math.round ((double) sum / numBytes);
			if (average < 5 || average > 255 - 5) { //hardcoded values
				String message = "average (" + average + ") out of range for " + nameWithExt;
				_log.error ("AlbumImage.validateScaledImageData: " + message);
				AlbumFormInfo.getInstance ().addServletError ("Error: " + message);
			}
		}

//		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	public static void removeRgbDataFileFromFileSystem (String imagePath)
	{
		String rgbDataFilePath = imagePath.replace (AlbumFormInfo._ImageExtension, AlbumFormInfo._RgbDataExtension);

		if (AlbumFormInfo._logLevel >= 8) {
			_log.debug ("AlbumImage.removeRgbDataFileFromFileSystem: rgbDataFilePath = " + rgbDataFilePath);
		}

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
	public static VPair<Integer, Integer> getScaledImageDiff (ByteBuffer buffer1, ByteBuffer buffer2)
	{
//		AlbumProfiling.getInstance ().enter (5); //too expensive

		final VPair<Integer, Integer> errorPair = VPair.of (Integer.MAX_VALUE, Integer.MAX_VALUE);

		if (buffer1 == null || buffer2 == null) {
			return errorPair;
		}

		buffer1.rewind (); //always rewind before using
		buffer2.rewind (); //always rewind before using
		final int numBytes = Math.min (buffer1.remaining (), buffer2.remaining ());

		double[] diffs = new double[numBytes];
		try {
			for (int ii = 0; ii < numBytes; ii++) {
				int b1 = buffer1.get (ii) & 0xFF;
				int b2 = buffer2.get (ii) & 0xFF;
				diffs[ii] = b2 - b1;
			}
		} catch (Exception ee) { //this can occur if we failed to read/parse the image
			return errorPair;
		}

		double mean = StatUtils.mean (diffs);

		double totalAbsDiff = 0;
		double totalSquDiff = 0;
		for (int ii = 0; ii < numBytes; ii++) {
			totalAbsDiff += Math.abs (diffs[ii]);
			totalSquDiff += Math.pow (diffs[ii] - mean, 2.0);
		}

		int averageAbsDiff = (int) Math.round (totalAbsDiff / numBytes);
		int stdDev = (int) Math.round (Math.sqrt (totalSquDiff / numBytes));

		final boolean testStdDevFormula = false; //debug
		if (testStdDevFormula) {
			int stdDev2 = (int) Math.round (new StandardDeviation ().evaluate (diffs));
			if (Math.abs (stdDev2 - stdDev) > 2) { //hardcoded value
				_log.error ("AlbumImage.getScaledImageDiff: stdDev (" + stdDev + ") does not match stdDev2 (" + stdDev2 + ")");
			}
		}

		//		AlbumProfiling.getInstance ().exit (5);

		return VPair.of (averageAbsDiff, stdDev);
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean acceptDiff (int averageDiff, int stdDev, int maxRgbDiffs, int maxStdDev)
	{
		//for consistent behavior, this logic should be duplicated in both AlbumImageDiffer#selectNamesFromImageDiffs and AlbumImage#acceptDiff
		return (averageDiff >= 0 && stdDev >= 0) && //avoid accepting AlbumImagePair objects that have not been inited with diff values
				(averageDiff <= maxRgbDiffs || stdDev <= maxStdDev) && averageDiff <= 3 * maxRgbDiffs && stdDev <= 3 * maxStdDev; //hardcoded values
	}

	///////////////////////////////////////////////////////////////////////////
	public synchronized void createRgbDataFile () //throws Exception
	{
		String nameWithExt = getImagePath () + getName () + AlbumFormInfo._RgbDataExtension;

		if (AlbumFormInfo._logLevel >= 8) {
			_log.debug ("AlbumImage.createRgbDataFile: " + nameWithExt);
		}

		BufferedImage image = JpgUtils.readImage (_file);

		int scaledWidth = 0;
		int scaledHeight = 0;
		if (getOrientation () == AlbumOrientation.ShowLandScape) {
			scaledWidth = RgbDatDimRectLarge;
			scaledHeight = RgbDatDimRectSmall;
		} else if (getOrientation () == AlbumOrientation.ShowPortrait) {
			scaledWidth = RgbDatDimRectSmall;
			scaledHeight = RgbDatDimRectLarge;
		} else { //AlbumOrientation.ShowSquare
			scaledWidth = scaledHeight = RgbDatDimSquare;
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

			validateScaledImageData (scaledImageData, nameWithExt); //emits errors

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
	private static ByteBuffer shrinkRgbData (int[] rawScaledImageData) //throws Exception
	{
		int numBytes = ((rawScaledImageData.length * 4) * 3) / 4;
		ByteBuffer buffer = ByteBuffer.allocate (numBytes);

		for (int argb : rawScaledImageData) {
//			buffer.put ((byte) ((argb >> 24) & 0xFF)); //A - ignore A data
			buffer.put ((byte) ((argb >> 16) & 0xFF)); //R
			buffer.put ((byte) ((argb >> 8)  & 0xFF)); //G
			buffer.put ((byte) ((argb)       & 0xFF)); //B
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
	private long getExifDate0 ()
	{
		return _exifDates[0];
	}
	private long getExifDate1 ()
	{
		return _exifDates[1];
	}
	private long getExifDate2 ()
	{
		return _exifDates[2];
	}
	private long getExifDate3 ()
	{
		return _exifDates[3];
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean isExifDateValid (long exifDate)
	{
		final long maxValidExifDate = new GregorianCalendar ().getTimeInMillis () + (30L * 24 * 3600 * 1000); //30 days from now
		final long minValidExifDate = new GregorianCalendar (1980, Calendar.JANUARY, 1).getTimeInMillis (); //first second of 1980

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
				_exifDateStrings[exifDateIndex] = "";
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
			exifDates[2] = date.getTime () + (time * 1000L); //this probably does not handle TZ correctly
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


	//members

	//attributes read from database
	private final int _nameId;
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
	private String _namePlusAttrs = null;
	private String _baseName1 = null; //for collapseGroups = false
	private String _baseName2 = null; //for collapseGroups = true
	private String _tagString1 = null; //for collapseGroups = false
	private String _tagString2 = null; //for collapseGroups = true
//	private String _nameFirstLettersLower = null;
	private long _pixels = -1;
	private int _scaledWidth = -1;
	private int _scaledHeight = -1;
	private int _scale = -1;
	private int _random = -1;
//	private int _count = -1;
	private int _hashCode = -1;
	private long _rgbHash = -1;
	private boolean _hasExifDateChecked = false;
	private boolean _hasExifDate = false;
	private String[] _exifDateStrings = null;
	private String _modifiedString = null;
	private AlbumOrientation _orientation = AlbumOrientation.ShowAny;

	//for rgbDatFile
	public static final int RgbDatDimRectSmall = 96;
	public static final int RgbDatDimRectLarge = 144;
	public static final int RgbDatSizeRectagular = (RgbDatDimRectSmall * RgbDatDimRectLarge * 3) / 4;
	public static final int RgbDatDimSquare = 120;
	public static final int RgbDatSizeSquare = (RgbDatDimSquare * RgbDatDimSquare * 3) / 4;

	public static final int NumExifDates = 6;		//4 exif dates read from database + 2 calculated
	public static final int NumFileExifDates = 4;	//4 exif dates read from database + 2 calculated

//	public static final int SubFolderLength = 2;	//expected length of sub_folder column in various database tables

	public static final String HtmlNewline = "&#13;";

	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

//	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0"); //format as integer

//	private static final String NL = System.getProperty ("line.separator");
	private static final Random _randomGenerator = new Random ();

	private static final Logger _log = LogManager.getLogger ();
}
