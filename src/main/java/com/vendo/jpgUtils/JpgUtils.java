package com.vendo.jpgUtils;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.awt.image.codec.JPEGImageDecoderImpl;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class JpgUtils {

	///////////////////////////////////////////////////////////////////////////
	static {
		Thread.setDefaultUncaughtExceptionHandler (new VUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args) {
		JpgUtils app = new JpgUtils ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String[] args) {
		for (int ii = 0; ii < args.length; ii++) {
			System.out.println ("arg" + ii + " = <" + args[ii] + ">");
		}

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("height") || arg.equalsIgnoreCase ("h")) {
					try {
						_desiredHeight = Integer.parseInt (args[++ii]);
						if (_desiredHeight <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("megaPixels") || arg.equalsIgnoreCase ("mp")) {
					try {
						_desiredMegaPixels = Double.parseDouble (args[++ii]);
						if (_desiredMegaPixels <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("minimumHeight") || arg.equalsIgnoreCase ("minh")) {
					try {
						_minimumHeight = Integer.parseInt (args[++ii]);
						if (_minimumHeight <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("minimumWidth") || arg.equalsIgnoreCase ("minw")) {
					try {
						_minimumWidth = Integer.parseInt (args[++ii]);
						if (_minimumWidth <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("numthreads") || arg.equalsIgnoreCase ("threads")) {
					try {
						_numThreads = Integer.parseInt (args[++ii]);
						if (_numThreads < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("query") || arg.equalsIgnoreCase ("q")) {
					_queryMode = true;

				} else if (arg.equalsIgnoreCase ("scalePercent") || arg.equalsIgnoreCase ("sc")) {
					try {
						_scalePercent = Double.parseDouble (args[++ii]);
						if (_scalePercent <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("sourceDir") || arg.equalsIgnoreCase ("src")) {
					try {
						_sourceDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
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
						if (_desiredWidth <= 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_infilenameWild == null) {
					_infilenameWild = arg;

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
		if (_infilenameWild == null) {
			displayUsage ("<infile> not specified", true);
		} else {
			//hack - to avoid java wild card expansion problem
			if (!_infilenameWild.contains("*")) {
				_infilenameWild += "*";
			}
		}

		if (!_queryMode) {
			if (_scalePercent <= 0 && _desiredWidth <= 0 && _desiredHeight <= 0 && _desiredMegaPixels <= 0) {
				displayUsage ("must specify width, height, scale, or megaPixels", true);
			}
		}

//TODO? - extract sourceDir from input file spec??

		if (_sourceDir == null) {
			_sourceDir = getCurrentDirectory ();
		}
		_sourceDir = getRealPath (_sourceDir);
		_sourceDir = appendSlash (_sourceDir);
		if (!fileExists (_sourceDir)) {
			displayUsage ("<sourceDir> '" + _sourceDir + "' does not exist", true);
		}

		if (_destDir == null) {
			_destDir = getCurrentDirectory ();
		}
		_destDir = getRealPath (_destDir);
		_destDir = appendSlash (_destDir);
		if (!fileExists (_destDir)) {
			displayUsage ("<destDir> '" + _destDir + "' does not exist", true);
		}

//TODO - fail on illegal combos of /w /h /sc /mp

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit) {
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

//TODO
		msg += "Usage: " + _AppName + " [/debug] [/threads <num threads>] [/destDir] [/sourceDir] [/height] [/width] [/minimumHeight] [/minimumWidth] [/query] [/scalePercent] [/suffix] <wildname>";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			debugInfo ();
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean debugInfo () {
		List<String> formatNames = new ArrayList<> ();
		formatNames.addAll (Arrays.asList (ImageIO.getReaderFormatNames ()));
		formatNames.sort(VendoUtils.caseInsensitiveStringComparator);
//		System.out.println ("Output from ImageIO.getReaderFormatNames(): " + VendoUtils.arrayToString (formatNames.toArray (new String[] {})));
		System.out.println ("Output from ImageIO.getReaderFormatNames(): " + formatNames);

		formatNames.clear ();
		formatNames.addAll (Arrays.asList (ImageIO.getWriterFormatNames ()));
		formatNames.sort(VendoUtils.caseInsensitiveStringComparator);
//		System.out.println ("Output from ImageIO.getWriterFormatNames(): " + VendoUtils.arrayToString (formatNames.toArray (new String[] {})));
		System.out.println ("Output from ImageIO.getWriterFormatNames(): " + formatNames);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () {
		if (_Debug) {
			_log.trace ("JpgUtils.run");
		}

		String[] filenames = getFileList (_sourceDir, _infilenameWild);

		if (filenames.length == 0) {
			_log.error ("JpgUtils.run: no files matching '" + _sourceDir + _infilenameWild + "' found");
			return true;
		}

		if (_queryMode) {
			for (String filename : filenames) {
				String infilePath = _sourceDir + filename;
				ImageAttributes imageAttributes = getImageAttributes (infilePath);
				System.out.println (imageAttributes);
			}

			return true;
		}

		final CountDownLatch endGate = new CountDownLatch (filenames.length);
		final AtomicInteger numImagesCompressed = new AtomicInteger(0);

		for (final String filename : filenames) {
			Runnable task = () -> {
				Thread.currentThread ().setName (filename);
				if (compressFile(filename)) { //compressFile prints filename and image stats
					numImagesCompressed.getAndIncrement();
				}
				endGate.countDown();
			};
			getExecutor ().execute (task);
		}

		try {
			endGate.await();
		} catch (Exception ee) {
			_log.error("JpgUtils.run: endGate:", ee);
		}

		shutdownExecutor ();

		if (numImagesCompressed.get() == 0) {
			System.err.println (_AppName + ": Error: no images compressed");
			return false;
		}

		if (!_skippedImages.isEmpty()) {
			System.out.println();
			System.out.println("Skipped images:");
			_skippedImages.forEach(System.out::println);

			System.out.println();
			System.out.println ("Number of images total = " + filenames.length);
			System.out.println ("Number of images skipped = " + _skippedImages.size());
		} else {
			System.out.println ();
		}

		System.out.println ("Number of images compressed = " + numImagesCompressed.get());
		_inImageAttributesStats.print();
		_outImageAttributesStats.print();

		System.out.println ("Input bytes total = " + VendoUtils.unitSuffixScaleBytes(_inImageAttributesStats.getTotalBytes()));
		System.out.println ("Output bytes total = " + VendoUtils.unitSuffixScaleBytes(_outImageAttributesStats.getTotalBytes()));

		double totalCompression = (double) _inImageAttributesStats.getTotalBytes() / (double) _outImageAttributesStats.getTotalBytes();
		System.out.println ("Total compression = " + _decimalFormat1.get ().format (totalCompression) + ":1");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean compressFile (String filename) {
		if (_Debug) {
			_log.trace ("JpgUtils.compressFile(" + filename + ")");
		}

		String infilePath = _sourceDir + filename;
		String outfilePath = generateOutfilePath (_destDir + filename, _nameSuffix);

		if (outfilePath.compareToIgnoreCase (infilePath) == 0) {
			_log.error ("JpgUtils.compressFile: output file name '" + outfilePath + "' is the same as input file name (need to specify /suffix)");
			return false;
		}

		if (fileExists (outfilePath)) {
			_log.error ("JpgUtils.compressFile: output file '" + outfilePath + "' already exists");
			return false;
		}

		//if either one of these is less than 0, it directs the scaler to maintain the aspect ratio
		int newWidth = -1;
		int newHeight = -1;

		if (_desiredMegaPixels > 0) {
			ImageAttributes imageAttributes = getImageAttributes (infilePath);
			double currentMegaPixels =  (double) imageAttributes._width * (double) imageAttributes._height / 1e6;

			if (currentMegaPixels < (4 * _desiredMegaPixels / 3)) {
				_skippedImages.add(imageAttributes);
				_log.warn ("skip compressing '" + infilePath + "' because smaller than min desired MP");


				//skip file: if the src and dst files are not the same, copy the file
				Path srcPath = FileSystems.getDefault ().getPath (infilePath);
				Path dstPath = FileSystems.getDefault ().getPath (outfilePath);
				if (srcPath.compareTo (dstPath) != 0) {
					try {
						Files.copy (srcPath, dstPath);

					} catch (Exception ee) {
						_log.error ("JpgUtils.compressFile: Files.copy failed to copy '" + infilePath + "' to '" + outfilePath + "'");
						_log.error (ee);
					}
				}

				return false;
			}

			VPair<Integer, Integer> newDimensions = calculateDesiredDimensions(imageAttributes, _desiredMegaPixels);
			newWidth = newDimensions.getFirst();
			newHeight = newDimensions.getSecond();

		} else if (_desiredWidth > 0 || _desiredHeight > 0) {
			newWidth = _desiredWidth;
			newHeight = _desiredHeight;

		} else if (_scalePercent == 100) {
			newWidth = newHeight = -1; //maintain original dimensions

		} else if (_scalePercent > 0) {
			final double scaleFactor = _scalePercent / 100.;

			ImageAttributes imageAttributes = getImageAttributes (infilePath);

			if (imageAttributes._width < _minimumWidth || imageAttributes._height < _minimumHeight) {
				_log.warn ("skip scaling '" + infilePath + "' because smaller than min size");

				//skip file: if the src and dst files are not the same, copy the file
				Path srcPath = FileSystems.getDefault ().getPath (infilePath);
				Path dstPath = FileSystems.getDefault ().getPath (outfilePath);
				if (srcPath.compareTo (dstPath) != 0) {
					try {
						Files.copy (srcPath, dstPath);

					} catch (Exception ee) {
						_log.error ("JpgUtils.compressFile: Files.copy failed to copy '" + infilePath + "' to '" + outfilePath + "'");
						_log.error (ee);
					}
				}

				return false;
			}

			if (imageAttributes._width > imageAttributes._height) {
				newWidth = VendoUtils.roundUp (scaleFactor * imageAttributes._width);
			} else {
				newHeight = VendoUtils.roundUp (scaleFactor * imageAttributes._height);
			}
		}

		boolean status = generateScaledImage (infilePath, outfilePath, newWidth, newHeight);

		if (status) {
			setFileDateTime (infilePath, outfilePath);
		}

		if (status) {
			ImageAttributes inImageAttributes = getImageAttributes (infilePath);
			ImageAttributes outImageAttributes = getImageAttributes (outfilePath);
			double compression = (double) inImageAttributes.getNumBytesOnDisk() / (double) outImageAttributes.getNumBytesOnDisk();

			System.out.println (outImageAttributes + ", compression = " + _decimalFormat1.get ().format (compression) + ":1");

			synchronized (_inImageAttributesStats) {
				_inImageAttributesStats.add(inImageAttributes);
			}
			synchronized (_outImageAttributesStats) {
				_outImageAttributesStats.add(outImageAttributes);
			}
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String[] getFileList (String dirName, final String nameWild) {
		if (_Debug) {
			_log.trace ("JpgUtils.getFileList(" + dirName + ", " + nameWild + ")");
		}
		//for simple dir listings, it looks like java.io package is faster than java.nio
		FilenameFilter filenameFilter = (dir, name) -> VendoUtils.matchPattern (name, nameWild);

		File dir = new File (dirName);
		String[] filenames = dir.list (filenameFilter);

		return filenames;
	}

	///////////////////////////////////////////////////////////////////////////
	//tries to read image from file, using fastest (hopefully) to slowest method
	public static BufferedImage readImage (File file) {
		if (_Debug) {
			_log.trace ("JpgUtils.readImage(" + file + ")");
		}

		BufferedImage image = null;

		//first try faster, deprecated Sun class: sun.awt.image.codec.JPEGImageDecoderImpl (from jai-codec-1.1.3.jar)
		try (InputStream inputStream = Files.newInputStream (file.toPath (), StandardOpenOption.READ)) { //open file read-only, with read-sharing
			//note this prints errors directly to stderr; e.g., "Corrupt JPEG data: bad Huffman code" "Corrupt JPEG data: premature end of data segment"
			image = new JPEGImageDecoderImpl (inputStream).decodeAsBufferedImage ();
			return image;

		} catch (Exception ee) {
			_log.warn ("JpgUtils.readImage: JPEGImageDecoderImpl failed on " + file.getName () + ": (falling back to next method)");
			_log.warn (ee);
		}

		//try again with deprecated Sun class: com.sun.image.codec.jpeg.JPEGCodec (from jai-codec-1.1.3.jar)
		//note use of this method requires changes to pom.xml: <arg>-XDignore.symbol.file</arg>
		//                                       or build.xml: <compilerarg value="-XDignore.symbol.file"/>
		try (InputStream inputStream = Files.newInputStream (file.toPath (), StandardOpenOption.READ)) { //open file read-only, with read-sharing
			image = JPEGCodec.createJPEGDecoder (inputStream).decodeAsBufferedImage ();
			return image;

		} catch (Exception ee) {
			_log.warn ("JpgUtils.readImage: JPEGCodec.createJPEGDecoder failed on " + file.getName () + ": (falling back to next method)");
			_log.warn (ee);
		}

		//this returns null if no registered ImageReader claims to be able to read the resulting image
		//but throws IOException if an error occurs during reading
		try (InputStream inputStream = Files.newInputStream (file.toPath (), StandardOpenOption.READ)) { //open file read-only, with read-sharing
			image = ImageIO.read (inputStream);
			return image;

		} catch (Exception ee) {
			_log.warn ("JpgUtils.readImage: ImageIO.read failed on " + file.getName () + ": (no more tries)");
			_log.warn (ee);
//			System.out.println("del " + file.getName());
		}

		return image;
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean generateScaledImage (String inFilename, String outFilename, int desiredWidth, int desiredHeight) {
		if (_Debug) {
			_log.trace ("JpgUtils.generateScaledImage(" + inFilename + ", " + outFilename + ", " + desiredWidth + ", " + desiredHeight + ")");
		}

		BufferedImage image = null;
		final int hints = Image.SCALE_SMOOTH;
//		final int hints = Image.SCALE_REPLICATE;
//		final int hints = Image.SCALE_AREA_AVERAGING; //same as SCALE_SMOOTH ??

		try {
			image = readImage (new File (inFilename));

		} catch (Exception ee) {
			_log.error ("JpgUtils.generateScaledImage: failed to read '" + inFilename + "'");
			_log.error (ee);
			return false; //not an image
		}

		try {
			BufferedImage scaledImage = toBufferedImage (image.getScaledInstance (desiredWidth, desiredHeight, hints));
			ImageIO.write (scaledImage, _formatName, new File (outFilename));

		} catch (Exception ee) {
			_log.error ("JpgUtils.generateScaledImage: failed to write '" + outFilename + "'");
			_log.error (ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//Creating a Buffered Image from an Image
	//original from: http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
	//
	// This method returns a BufferedImage with the contents of an image
	public static BufferedImage toBufferedImage(Image image) {
		if (_Debug) {
			_log.trace ("JpgUtils.toBufferedImage(" + image + ")");
		}

		if (image instanceof BufferedImage) {
			return (BufferedImage)image;
		}

		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon(image).getImage();

		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent Pixels
		boolean hasAlpha = false; //hasAlpha(image);

		// Create a buffered image with a format that's compatible with the screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}

			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException ee) {
			// The system does not have a screen
		}

		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}

		// Copy image to buffered image
		Graphics g = bimage.createGraphics();

		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return bimage;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean setFileDateTime (String infilePath, String outfilePath) {
		File infile = new File (infilePath);
		File outfile = new File (outfilePath);

		long lastModified = infile.lastModified ();
		boolean status = outfile.setLastModified (lastModified);

		if (!status) {
			_log.warn ("setFileDateTime: File.setLastModified failed for: " + outfilePath);
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean fileExists (String filename) {
		Path file = FileSystems.getDefault ().getPath (filename);

		return Files.exists (file);
	}

	///////////////////////////////////////////////////////////////////////////
	private static String getRealPath (String filename) {
		Path path = FileSystems.getDefault ().getPath (filename);

		String file;

		try {
			file = path.toRealPath ().toString ();

		} catch (Exception ee) {
			_log.warn ("getRealPath: Path.toRealPath failed");
			_log.warn (ee);

			//fall back to use AbsolutePath
			file = path.toAbsolutePath ().toString ();
		}

		return file;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String getCurrentDirectory () {
		Path path = FileSystems.getDefault ().getPath ("");

		return getRealPath (path.toString ());
	}

	///////////////////////////////////////////////////////////////////////////
	private static String appendSlash (String dir) { //append slash if necessary
		int lastChar = dir.charAt (dir.length () - 1);
		if (lastChar != '/' && lastChar != '\\') {
			dir += _slash;
		}

		return dir;
	}

	///////////////////////////////////////////////////////////////////////////
	//split file name on dot before extension
	private static String[] splitFilename (String filename) {
		final String pattern = "\\."; //dot
		final String marker = "::::";
		final int blockNumber = 0; //counting from end, 0 means last

		//split string on nth group of digits (reverse string, find nth group, reverse back, split)
		String s1 = VendoUtils.reverse (filename);
		String s2 = VendoUtils.replacePattern (s1, pattern, marker, blockNumber);
		String s3 = VendoUtils.reverse (s2);
		String[] parts = s3.split (marker);

		return parts;
	}

	///////////////////////////////////////////////////////////////////////////
	//insert suffix before extension
	private static String generateOutfilePath (String filename, String suffix) {
		String[] parts = splitFilename (filename);
		String newFilename = parts[0] + suffix + "." + parts[1];
		return newFilename;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns false if the last part of the image contains the same invalid data
	// or if the image is not the size it claims
	///////////////////////////////////////////////////////////////////////////
	public static boolean validateImageData (String filename) {
		_log.trace ("getRealPath: Path.toRealPath failed");

		boolean validImage = false;

		BufferedImage image = null;
		try {
			image = JpgUtils.readImage (new File (filename));
			validImage = validateImageData (image);

		} catch (Exception ee) {
			_log.error ("JpgUtils.validateImageData: failed to read '" + filename + "'");
			_log.error (ee); //print exception, but no stack trace
		}

		return validImage;
	}
	public static boolean validateImageData (BufferedImage image) {
		int width = image.getWidth ();
		int height = image.getHeight ();

		//read (w x h) ints (int = 4 bytes) of data from image array starting at offset x, y
		final int w = 100;
		final int h = 10;
		int[] rgbIntArray = new int [w * h];
		image.getRGB (width - w - 1, height - h - 1, w, h, rgbIntArray, 0, w);

		final int invalidData = 0xFF808080;
		boolean invalidImage = Arrays.stream (rgbIntArray).allMatch (t -> t == invalidData);

		return !invalidImage;
	}

	///////////////////////////////////////////////////////////////////////////
	private ImageAttributes getImageAttributes (String filename) {
		if (_Debug) {
			_log.trace ("JpgUtils.getImageAttributes(" + filename + ")");
		}

		BufferedImage image = null;

		int width = -1;
		int height = -1;
		long bytes = -1;

		try {
			image = readImage (new File (filename));
			if (validateImageData (image)) {
				width = image.getWidth ();
				height = image.getHeight ();

				File file = new File (filename);
				bytes = file.length ();
			}

		} catch (Exception ee) {
			_log.error ("JpgUtils.getImageAttributes: failed on '" + filename + "'", ee);
		}

		ImageAttributes imageAttributes = new ImageAttributes (filename, width, height, bytes);

		return imageAttributes;
	}

	///////////////////////////////////////////////////////////////////////////
	public VPair<Integer, Integer> calculateDesiredDimensions (ImageAttributes imageAttributes, double desiredMegaPixels) {
		final double mega = 1e6; //1000-based, not 1024-based

		double currentMegaPixels =  (double) imageAttributes._width * (double) imageAttributes._height / mega;
		double scale = Math.sqrt(currentMegaPixels / desiredMegaPixels);

		int newHeight = (int) Math.round((double) imageAttributes._height / scale);
		int newWidth = (int) Math.round((double) imageAttributes._width / scale);

		return VPair.of(newWidth, newHeight);
	}

	///////////////////////////////////////////////////////////////////////////
	public synchronized ExecutorService getExecutor () {
		if (_executor == null || _executor.isTerminated () || _executor.isShutdown ()) {
			_executor = Executors.newFixedThreadPool (_numThreads);
		}

		return _executor;
	}

	///////////////////////////////////////////////////////////////////////////
	public void shutdownExecutor () {
		_log.debug ("JpgUtils.shutdownExecutor: shutdown executor");
		getExecutor ().shutdownNow ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static class ImageAttributes implements Comparable<ImageAttributes> {

		///////////////////////////////////////////////////////////////////////////
		public ImageAttributes (String name, int width, int height, long bytes) {
			_name = name;
			_width = width;
			_height = height;
			_bytes = bytes;
		}

		///////////////////////////////////////////////////////////////////////////
		public long getNumBytesOnDisk () { //ostensibly this is a better metric when aggregating many images
			final long bytesPerPhysicalSector = 4096; //hardcoded - to get this value on Windows, run: fsutil fsinfo ntfsinfo D:

			return bytesPerPhysicalSector * (_bytes + bytesPerPhysicalSector) / bytesPerPhysicalSector;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compareTo(ImageAttributes that) {
			int thisPixels = _width * _height;
			int thatPixels = that._width * that._height;
			int comparePixels = thisPixels - thatPixels;
			if (comparePixels != 0) {
				return comparePixels;
			}

			return _name.compareToIgnoreCase(that._name);
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString () {
			//copied from AlbumImage#toString
			double bytesPerPixel = (double) _bytes * (double) _bytes / ((double) _width  * (double) _height );

			StringBuffer sb = new StringBuffer(128);
			sb.append(_name).append(" = ");
			sb.append(_width).append("x").append(_height).append(", ");
			sb.append(VendoUtils.unitSuffixScale(_width * _height, "P")).append(", "); //"P" for Pixels
			sb.append(VendoUtils.unitSuffixScaleBytes(_bytes)).append(", ");
			sb.append("BPP=").append(VendoUtils.unitSuffixScale((long) bytesPerPixel, "B/P"));

			return sb.toString();
		}

		public final String _name;
		public final int _width;
		public final int _height;
		public final long _bytes;
	}

	///////////////////////////////////////////////////////////////////////////
	private class ImageAttributesStats {

		///////////////////////////////////////////////////////////////////////////
		public ImageAttributesStats (String name)  {
			_name = name;
		}

		///////////////////////////////////////////////////////////////////////////
		public void add (ImageAttributes imageAttributes) {
			_records.add (imageAttributes);
		}

		///////////////////////////////////////////////////////////////////////////
		public long getTotalBytes () {
			return _records.stream().mapToLong(ImageAttributes::getNumBytesOnDisk).sum();
		}

		///////////////////////////////////////////////////////////////////////////
		public void print () {
			int averageLargerDimension = (int) _records.stream().mapToLong(r -> Math.max(r._height, r._width)).sum() / _records.size();
			int averageSmallerDimension = (int) _records.stream().mapToLong(r -> Math.min(r._height, r._width)).sum() / _records.size();
			long averageBytes = _records.stream().mapToLong(ImageAttributes::getNumBytesOnDisk).sum() / _records.size();

			ImageAttributes imageAttributes = new ImageAttributes(_name, averageSmallerDimension, averageLargerDimension, averageBytes);

			System.out.println (imageAttributes);
		}

		private final String _name;
		private final ArrayList<ImageAttributes> _records = new ArrayList<> ();
	}


	//private members
	private int _numThreads = 8;
	private double _desiredMegaPixels = -1;
	private int _desiredHeight = -1;
	private int _desiredWidth = -1;
	private int _minimumWidth = -1;
	private int _minimumHeight = -1;
	private double _scalePercent = -1;
	private boolean _queryMode = false;
	private String _destDir = null;
	private String _sourceDir = null;
	private String _infilenameWild = null;
	private String _nameSuffix = "";

	private final ImageAttributesStats _inImageAttributesStats = new ImageAttributesStats("Input averages"); //write access is synchronized above
	private final ImageAttributesStats _outImageAttributesStats = new ImageAttributesStats("Output averages"); //write access is synchronized above

	private final Set<ImageAttributes> _skippedImages = new ConcurrentSkipListSet<>();

	private /*static*/ ExecutorService _executor = null;

	private static final String _formatName = "jpg";
	private static final ThreadLocal<NumberFormat> _decimalFormat1 = ThreadLocal.withInitial(() -> new DecimalFormat("###,##0.0")); //DecimalFormat is not thread safe

	private static final String _slash = System.getProperty ("file.separator");
	private static final Logger _log = LogManager.getLogger (JpgUtils.class);

	//global members
	public static boolean _Debug = false;
	public static boolean _TestMode = false;

	public static final String _AppName = "JpgUtils";
	public static final String NL = System.getProperty ("line.separator");
}
