/* JpgRgbData.java

*/

package com.vendo.jpgRgbData;

import com.vendo.albumServlet.AlbumImage;
import com.vendo.albumServlet.AlbumOrientation;
import com.vendo.jpgInfo.FilenamePatternFilter;
import com.vendo.jpgUtils.JpgUtils;
import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class JpgRgbData
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new VUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (final String args[]) throws Exception
	{
		JpgRgbData app = new JpgRgbData ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		//set defaults for command line arguments
		String filePatternString = "*.jpg";
		String folderString = ".";

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("checkForOrphans") || arg.equalsIgnoreCase ("check")) {
					_checkForOrphans = true;

				} else if (arg.equalsIgnoreCase ("testMode") || arg.equalsIgnoreCase ("test")) {
					_testMode = true;

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

		System.out.println ("folderString = \"" + folderString + "\"");
		System.out.println ("filePatternString = \"" + filePatternString + "\"");

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
		if (_checkForOrphans) {
			runCheckForOrphans ();

		} else { //default behavior
			runProcessFiles ();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean runCheckForOrphans () throws Exception
	{
		long startNano = System.nanoTime ();

		if (!_recurseSubdirs) {
			checkForOrphanedFiles (_folder);

		} else {
			final ExecutorService executor = Executors.newFixedThreadPool (VendoUtils.getLogicalProcessors () - 1);

			//note this only gets the subfolders in this folder (it does not recurse)
			final Collection<Path> subFolders = getSubFolders (_folder);
			final CountDownLatch endGate = new CountDownLatch (subFolders.size ());

			for (final Path folder : subFolders) {
				Runnable task = () -> {
					checkForOrphanedFiles (folder);
					endGate.countDown ();
				};
				executor.execute (task);
			}

			try {
				endGate.await ();
			} catch (Exception ee) {
				_log.error ("JpgRgbData.runCheckForOrphans: endGate:", ee);
			}

			executor.shutdownNow ();
		}

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println ("JpgRgbData.runCheckForOrphans: checkForOrphanedFiles: elapsed: " + elapsedMillis + " ms");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean runProcessFiles () throws Exception
	{
		long startNano = System.nanoTime ();

//		final ExecutorService executor = Executors.newFixedThreadPool (VendoUtils.getLogicalProcessors () - 1);
		final ExecutorService executor = Executors.newFixedThreadPool (26);

		//note this only gets the subfolders in this folder (it does not recurse)
		Collection<Path> subFolders = getSubFolders (_folder);
		subFolders = VendoUtils.shuffleAndTruncate (subFolders, 0, true);
		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());

		for (final Path folder : subFolders) {
			Runnable task = () -> {
				try {
					processFiles (folder);
				} catch (Exception ee) {
					_log.error ("JpgRgbData.runProcessFiles: processFiles (" + folder.toString () + "):", ee);
				}
				endGate.countDown ();
			};
			executor.execute (task);
		}

		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("JpgRgbData.runProcessFiles: endGate:", ee);
		}

		executor.shutdownNow ();

		long elapsedMillis = (System.nanoTime () - startNano) / 1000000;
		System.out.println ("JpgRgbData.runProcessFiles: processFiles: elapsed: " + elapsedMillis + " ms");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//recursive call to process folders (and optionally) subfolders
	private boolean processFiles (Path folder) throws Exception
	{
        System.out.println ("begin processing folder: " + folder.toString ());

        try (DirectoryStream <Path> ds = Files.newDirectoryStream (folder)) {
			for (Path file : ds) {
				if (_filenamePatternFilter.accept (file)) {
					processFile (file);
				}

				if (_recurseSubdirs && isDirectory (file)) {
					processFiles (file); //recurse
				}
			}

		} catch (IOException ee) {
			_log.error ("JpgRgbData.processFiles() failed", ee);
		}

        System.out.println ("done processing folder: " + folder.toString ());
        System.out.println ("total images found:      " + _decimalFormat.format(_imagesFound.get ()));
        System.out.println ("total dat files written: " + _decimalFormat.format(_imagesWritten.get ()));

//        System.out.println ("readImage            elapsed: " + LocalTime.ofNanoOfDay (_readImageElapsedNanos.get ()));
//        System.out.println ("writeScaledImageData elapsed: " + LocalTime.ofNanoOfDay (_writeScaledImageDataElapsedNanos.get ()));
//        System.out.println ("readScaledImageData  elapsed: " + LocalTime.ofNanoOfDay (_readScaledImageDataElapsedNanos.get ()));

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean processFile (Path imageFile) throws Exception
	{
		String imageFilename = imageFile.toAbsolutePath ().normalize ().toString ();
		_imagesFound.incrementAndGet ();

		if (_testMode) {
			System.out.println ("JpgRgbData.processFile: " + imageFilename);
		}

		String dataFilename = imageFilename.replace (".jpg", ".dat");
		if (Files.exists (FileSystems.getDefault ().getPath (dataFilename))) {
			if (_testMode) {
				System.out.println ("File exists, skipping: " + dataFilename);
			}
			return true;
		}

		readImageFile (imageFile);

		writeScaledImageData (imageFile);
		_imagesWritten.incrementAndGet ();

		if (_testMode) {
			ByteBuffer readBuffer = readScaledImageDataFile (imageFile);

			boolean equals = compareByteBuffers (imageFile, readBuffer);
			System.out.println ("JpgRgbData.processFile: equals = " + equals);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readImageFile (Path imageFile) throws Exception
	{
		String imageFilename = imageFile.toAbsolutePath ().normalize ().toString ();

		Instant startInstant = Instant.now ();

		BufferedImage image = JpgUtils.readImage (imageFile.toFile ());

		int imageWidth = image.getWidth ();
		int imageHeight = image.getHeight ();
		AlbumOrientation orientation = AlbumOrientation.getOrientation (imageWidth, imageHeight);

		int scaledWidth = 0;
		int scaledHeight = 0;
		if (orientation == AlbumOrientation.ShowLandScape) {
			scaledWidth = AlbumImage.RgbDatDimRectLarge;
			scaledHeight = AlbumImage.RgbDatDimRectSmall;
		} else if (orientation == AlbumOrientation.ShowPortrait) {
			scaledWidth = AlbumImage.RgbDatDimRectSmall;
			scaledHeight = AlbumImage.RgbDatDimRectLarge;
		} else { //AlbumOrientation.ShowSquare
			scaledWidth = scaledHeight = AlbumImage.RgbDatDimSquare;
		}

		final int hints = BufferedImage.SCALE_FAST;
		try {
			BufferedImage scaledImage = AlbumImage.toBufferedImage (image.getScaledInstance (scaledWidth, scaledHeight, hints));
			//get a subset of pixels from center of image
			int[] rawScaledImageData = scaledImage.getRGB (scaledWidth / 4, scaledHeight / 4, scaledWidth / 2, scaledHeight / 2, null, 0, scaledWidth / 2);

			_readImageElapsedNanos.addAndGet(Duration.between (startInstant, Instant.now ()).toNanos ());

//System.out.println ("JpgRgbData.readImageFile: " + imageFilename + ": " + rawScaledImageData.length + " ints");

			if (_testMode) { //write thumbnail as PNG
				String pngFilename = imageFilename.replace (".jpg", ".png");
				File pngFile = new File (pngFilename);
				BufferedImage pngImage = new BufferedImage (scaledWidth / 2, scaledHeight / 2, image.getType ());
				pngImage.setRGB (0, 0, scaledWidth / 2, scaledHeight / 2, rawScaledImageData, 0, scaledWidth / 2);
				ImageIO.write (pngImage, "png", pngFile);
			}

			ByteBuffer scaledImageData = shrinkRgbData (rawScaledImageData);
			scaledImageData.rewind ();

//System.out.println ("JpgRgbData.readImageFile: " + imageFilename + ": " + scaledImageData.remaining () + " bytes");

			_nameScaledImageMap.put (imageFilename, scaledImageData);

		} catch (Exception ee) {
			_log.error ("JpgRgbData.readImageFile: error reading/scaling image file \"" + imageFilename + "\"");
			_log.error (ee);
		}

		return true;
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

		buffer.rewind ();

		return buffer;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean writeScaledImageData (Path imageFile) throws Exception
	{
		String imageFilename = imageFile.toAbsolutePath ().normalize ().toString ();

		ByteBuffer scaledImageData = _nameScaledImageMap.get (imageFilename);
		scaledImageData.rewind ();

		Instant startInstant = Instant.now ();

		String dataFilename = imageFilename.replace (".jpg", ".dat");

		try (FileChannel fileChannel = new FileOutputStream (dataFilename).getChannel ()) {
			fileChannel.write (scaledImageData);

		} catch (Exception ee) {
			_log.error ("JpgRgbData.writeScaledImageData: error writing RGB data file \"" + dataFilename + "\"");
			_log.error (ee);
		}

		_writeScaledImageDataElapsedNanos.addAndGet (Duration.between (startInstant, Instant.now ()).toNanos ());

		System.out.println ("JpgRgbData.writeScaledImageData: created file: " + dataFilename);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static ByteBuffer readScaledImageDataFile (Path imageFile) throws Exception
	{
		String imageFilename = imageFile.toAbsolutePath ().normalize ().toString ();
		String dataFilename = imageFilename.replace (".jpg", ".dat");

		Instant startInstant = Instant.now ();

		ByteBuffer buffer = null;

		try (FileChannel fileChannel = new FileInputStream (dataFilename).getChannel ()) {
//Bug in FileChannel#map() in Java 7 prevents file from closing: http://stackoverflow.com/questions/13065358/java-7-filechannel-not-closing-properly-after-calling-a-map-method
//			buffer = fileChannel.map (FileChannel.MapMode.READ_ONLY, 0, fileChannel.size ());
			buffer = ByteBuffer.allocate ((int) fileChannel.size ());
			fileChannel.read (buffer, 0);
			buffer.rewind ();

		} catch (Exception ee) {
			_log.error ("JpgRgbData.readScaledImageDataFile() failed");
			_log.error (ee);
		}

		_readScaledImageDataElapsedNanos.addAndGet (Duration.between (startInstant, Instant.now ()).toNanos ());

		return buffer;
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean compareByteBuffers (Path imageFile, ByteBuffer readBuffer)
	{
		String imageFilename = imageFile.toAbsolutePath ().normalize ().toString ();

		ByteBuffer scaledImageData = _nameScaledImageMap.get (imageFilename);

		readBuffer.rewind ();
		scaledImageData.rewind ();

		return readBuffer.equals (scaledImageData);
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isDirectory (Path file)
	{
		try {
			return Files.isDirectory (file);

		} catch (Exception ee) {
			_log.error ("JpgRgbData.isDirectory() failed", ee);
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns list of subFolders in given folder
	//note this only gets the subfolders in this folder (it does not recurse)
	public static Collection<Path> getSubFolders (Path folder)
	{
		Collection<Path> folders = new ArrayList<Path> ();

		try (DirectoryStream <Path> ds = Files.newDirectoryStream (folder)) {
			for (Path file : ds) {
				if (isDirectory (file)) {
					folders.add (file);
				}
			}

		} catch (IOException ee) {
			_log.error ("JpgRgbData.getSubFolders() failed", ee);
		}

		return folders;
	}

	///////////////////////////////////////////////////////////////////////////
	//recursive call to check folder for orphaned files (and optionally) subfolders
	private /*static*/ boolean checkForOrphanedFiles (Path folder) //throws Exception
	{
		Collection<String> datBaseNames = new ArrayList<String> ();
		Collection<String> jpgBaseNames = new ArrayList<String> ();
		Collection<String> otherNames = new ArrayList<String> ();

		FilenamePatternFilter datPattern = new FilenamePatternFilter ("*.dat");
		FilenamePatternFilter jpgPattern = new FilenamePatternFilter ("*.jpg");
		FilenamePatternFilter delPattern = new FilenamePatternFilter ("*.delete");
//		FilenamePatternFilter xmlPattern = new FilenamePatternFilter ("*.xml");
//		FilenamePatternFilter bakPattern = new FilenamePatternFilter ("*.xml.bak");

		try (DirectoryStream <Path> ds = Files.newDirectoryStream (folder)) {
			for (Path file : ds) {
				String filename = file.getName (file.getNameCount () - 1).toString (); //filename is last leaf

				if (delPattern.accept (file)) {// || xmlPattern.accept (file) || bakPattern.accept (file)) {
					//ignore

				} else if (datPattern.accept (file)) {
					datBaseNames.add (FilenameUtils.removeExtension (filename));

				} else if (jpgPattern.accept (file)) {
					jpgBaseNames.add (FilenameUtils.removeExtension (filename));

				} else if (!isDirectory (file)) {
					otherNames.add (filename);

				} else if (_recurseSubdirs) {
					checkForOrphanedFiles (file); //recurse
				}
			}

		} catch (IOException ee) {
			_log.error ("JpgRgbData.checkForOrphanedFiles() failed", ee);
		}

		//use Collection#equals to compare two Sets
		//"The HashSet.equals method already does comparisons to make sure there are the exact same elements in each set. The ArrayList.equals does the same except it also checks ordering."
		//http://stackoverflow.com/questions/12649178/how-does-java-quickly-compare-two-collection-are-exactly-the-same-in-java
		//Note this probably only works because String objects are immutable (so two string that have the same value should be the same object)
		boolean equal = datBaseNames.equals (jpgBaseNames);

		if (_Debug) {
			String folderName = folder.getName (folder.getNameCount () - 1).toString (); //folder is last leaf
			System.out.println ("JpgRgbData.checkForOrphanedFiles: folder: " + folderName + ", file count diff: " + Math.abs (datBaseNames.size() - jpgBaseNames.size ()) + ", Collections.equals(): " + equal);
		}

		if (!equal) {
			String folderName = folder.getName (folder.getNameCount () - 1).toString (); //folder is last leaf
			System.out.println ("JpgRgbData.checkForOrphanedFiles: found diffs in folder: " + folderName);

			Set<String> datDiff = new HashSet<String> (datBaseNames);
			Set<String> jpgDiff = new HashSet<String> (jpgBaseNames);

			datDiff.removeAll (new HashSet<String> (jpgBaseNames));
			jpgDiff.removeAll (new HashSet<String> (datBaseNames));

			if (datDiff.size () > 0) {
				List<String> sorted = datDiff.stream ().sorted (VendoUtils.caseInsensitiveStringComparator)
													   .map (v -> "REM del " + folder + "\\" + v + ".dat")
													   .collect (Collectors.toList ());
				for (String str : sorted) {
					System.out.println (str);
				}
			}

			if (jpgDiff.size () > 0) {
				List<String> sorted = jpgDiff.stream ().sorted (VendoUtils.caseInsensitiveStringComparator)
													   .map (v -> "REM dir " + folder + "\\" + v + ".jpg")
													   .collect (Collectors.toList ());
				for (String str : sorted) {
					System.out.println (str);
				}
			}
		}

		if (otherNames.size () > 0) {
		    List<String> sorted = otherNames.stream ().sorted (VendoUtils.caseInsensitiveStringComparator)
													  .map (v -> "REM dir " + folder + "\\" + v)
													  .collect (Collectors.toList ());
			for (String str : sorted) {
				System.out.println (str);
			}
		}

		if (_Debug) {
			String folderName = folder.getName (folder.getNameCount () - 1).toString (); //folder is last leaf
			System.out.println ("JpgRgbData.checkForOrphanedFiles: folder: " + folderName + ": done");
		}

		return true;
	}


	//private members
	private Path _folder;
	private Boolean _recurseSubdirs = false;
	private Boolean _checkForOrphans = false;
	private Boolean _testMode = false;
	private FilenamePatternFilter _filenamePatternFilter = null;

	private AtomicInteger _imagesFound = new AtomicInteger (0);
	private AtomicInteger _imagesWritten = new AtomicInteger (0);

	private static AtomicLong _readImageElapsedNanos = new AtomicLong (0);
	private static AtomicLong _writeScaledImageDataElapsedNanos = new AtomicLong (0);
	private static AtomicLong _readScaledImageDataElapsedNanos = new AtomicLong (0);

	private static final Map<String, ByteBuffer> _nameScaledImageMap = new ConcurrentHashMap<String, ByteBuffer> ();

	private static Logger _log = LogManager.getLogger (JpgRgbData.class);
//	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm:ss");
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0"); //int

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "JpgRgbData";
	public static final String NL = System.getProperty ("line.separator");
}
