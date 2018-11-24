//AlbumImageDifferGen.java

package com.vendo.albumServlet;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumImageDifferGen
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
//TODO - change CLI to read properties file, too

		AlbumFormInfo.getInstance (); //call ctor to load class defaults

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
		AlbumFormInfo._profileLevel = 5;

		AlbumProfiling.getInstance ().enter/*AndTrace*/ (1);

		AlbumImageDifferGen albumImageDifferGen = AlbumImageDifferGen.getInstance ();

		if (!albumImageDifferGen.processArgs (args))
			System.exit (1); //processArgs displays error

		try {
			albumImageDifferGen.run ();
		} catch (Exception ee) {
			ee.printStackTrace (System.err);
		}

		AlbumProfiling.getInstance ().exit (1);

//		AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ true);
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

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_imageFilename1 == null) {
					_imageFilename1 = arg;

				} else if (_imageFilename2 == null) {
					_imageFilename2 = arg;

				} else if (_imageFilename3 == null) {
					_imageFilename3 = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_imageFilename1 == null || _imageFilename2 == null || _imageFilename3 == null) {
			displayUsage ("Incorrect usage", true);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] <input image filename 1>  <input image filename 2>  <output image filename>";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumImageDifferGen getInstance ()
	{
		if (_instance == null)
			_instance = new AlbumImageDifferGen ();

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumImageDifferGen ()
	{
		_log.debug ("AlbumImageDifferGen ctor");

//		_rootPath = AlbumFormInfo.getInstance ().getRootPath (false);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run () throws Exception
	{
		BufferedImage image1Orig = null;
		BufferedImage image2Orig = null;
		try {
			image1Orig = ImageIO.read (new File (_imageFilename1));
		} catch (Exception ee) {
			_log.error ("AlbumImageDifferGen.run: failed to read image \"" + _imageFilename1 + "\"");
		}
		try {
			image2Orig = ImageIO.read (new File (_imageFilename2));
		} catch (Exception ee) {
			_log.error ("AlbumImageDifferGen.run: failed to read image \"" + _imageFilename2 + "\"");
		}

		int image1Width = image1Orig.getWidth ();
		int image1Height = image1Orig.getHeight ();

		int image2Width = image2Orig.getWidth ();
		int image2Height = image2Orig.getHeight ();

		if (_Debug) {
			_log.debug ("AlbumImageDifferGen.run: image1Orig: " + image1Width + " x " + image1Height);
			_log.debug ("AlbumImageDifferGen.run: image2Orig: " + image2Width + " x " + image2Height);
		}

		int hints = BufferedImage.SCALE_FAST;
		BufferedImage image1Scaled = image1Orig;
		BufferedImage image2Scaled = image2Orig;
		if (image1Width < image2Width && image1Height < image2Height) {
			image2Scaled = toBufferedImage (image2Orig.getScaledInstance (image1Width, image1Height, hints));
		} else if (image2Width < image1Width && image2Height < image1Height) {
			image1Scaled = toBufferedImage (image1Orig.getScaledInstance (image2Width, image2Height, hints));
		}

		if (_Debug) {
			_log.debug ("AlbumImageDifferGen.run: image1Scaled: " + image1Scaled.getWidth () + " x " + image1Scaled.getHeight () );
			_log.debug ("AlbumImageDifferGen.run: image2Scaled: " + image2Scaled.getWidth () + " x " + image2Scaled.getHeight () );
		}

		createImageDiff (image1Scaled, image2Scaled, new File (_imageFilename3));
	}

	///////////////////////////////////////////////////////////////////////////
	public static int createImageDiff (String filename1, String filename2)
	{
		AlbumProfiling.getInstance ().enter (5, "1");

		BufferedImage image1Orig = null;
		BufferedImage image2Orig = null;

		AlbumProfiling.getInstance ().enter (5, "reading");
		try {
			image1Orig = ImageIO.read (new File (filename1));
		} catch (Exception ee) {
			_log.error ("AlbumImageDifferGen.createImageDiff: failed to read image \"" + filename1 + "\"");
		}
		try {
			image2Orig = ImageIO.read (new File (filename2));
		} catch (Exception ee) {
			_log.error ("AlbumImageDifferGen.createImageDiff: failed to read image \"" + filename2 + "\"");
		}
		AlbumProfiling.getInstance ().exit (5, "reading");

		int image1Width = image1Orig.getWidth ();
		int image1Height = image1Orig.getHeight ();

		int image2Width = image2Orig.getWidth ();
		int image2Height = image2Orig.getHeight ();

		if (_Debug) {
			_log.debug ("AlbumImageDifferGen.createImageDiff: image1Orig: " + image1Width + " x " + image1Height);
			_log.debug ("AlbumImageDifferGen.createImageDiff: image2Orig: " + image2Width + " x " + image2Height);
		}

		int hints = BufferedImage.SCALE_FAST;
		BufferedImage image1Scaled = image1Orig;
		BufferedImage image2Scaled = image2Orig;

		AlbumProfiling.getInstance ().enter (5, "scaling");
		if (image1Width < image2Width && image1Height < image2Height) {
			image2Scaled = toBufferedImage (image2Orig.getScaledInstance (image1Width, image1Height, hints));
		} else if (image2Width < image1Width && image2Height < image1Height) {
			image1Scaled = toBufferedImage (image1Orig.getScaledInstance (image2Width, image2Height, hints));
		}
		AlbumProfiling.getInstance ().exit (5, "scaling");

		if (_Debug) {
			_log.debug ("AlbumImageDifferGen.createImageDiff: image1Scaled: " + image1Scaled.getWidth () + " x " + image1Scaled.getHeight () );
			_log.debug ("AlbumImageDifferGen.createImageDiff: image2Scaled: " + image2Scaled.getWidth () + " x " + image2Scaled.getHeight () );
		}

		int averageDiff = createImageDiff (image1Scaled, image2Scaled, null);//new File (_imageFilename3));

		_log.debug ("AlbumImageDifferGen.createImageDiff: " + averageDiff + " " + filename1 + " " + filename2);

		AlbumProfiling.getInstance ().exit (5, "1");

		return averageDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int createImageDiff (BufferedImage image1, BufferedImage image2, File imageDiff)
	{
		AlbumProfiling.getInstance ().enter (5, "2");

		BufferedImage image3 = null;
		if (imageDiff != null) {
			image3 = new BufferedImage (image1.getWidth (), image1.getHeight (), image1.getType ());
		}

		int image1Width = image1.getWidth ();
		if (image1.getWidth () != image2.getWidth ()) {
			_log.error ("AlbumImageDifferGen.createImageDiff: widths not equal (" + image1.getWidth () + " != " + image2.getWidth () + ")");
			return 10000;
		}
		int image1Height = image1.getHeight ();
		if (image1.getHeight () != image2.getHeight ()) {
			_log.error ("AlbumImageDifferGen.createImageDiff: heights not equal (" + image1.getHeight () + " != " + image2.getHeight () + ")");
			return 10000;
		}

		AlbumProfiling.getInstance ().enter (5, "getRGB loop");
		boolean a0a1NotEqual = false;
		long totalDiff = 0;
		for (int x = 0; x < image1.getWidth (); x++) {
			for (int y = 0; y < image1.getHeight (); y++) {
				int argb0 = image1.getRGB (x, y);
				int argb1 = image2.getRGB (x, y);

				int a0 = (argb0 >> 24) & 0xFF;
				int r0 = (argb0 >> 16) & 0xFF;
				int g0 = (argb0 >>  8) & 0xFF;
				int b0 = (argb0      ) & 0xFF;

				int a1 = (argb1 >> 24) & 0xFF;
				int r1 = (argb1 >> 16) & 0xFF;
				int g1 = (argb1 >>  8) & 0xFF;
				int b1 = (argb1      ) & 0xFF;

//				int aDiff = Math.abs (a1 - a0); //aDiff should always be 0
				int rDiff = Math.abs (r1 - r0);
				int gDiff = Math.abs (g1 - g0);
				int bDiff = Math.abs (b1 - b0);

				if (a0 != a1) {
					a0a1NotEqual = true;
				}

//_log.debug ("AlbumImageDifferGen.createImageDiff: a0: " + a0 + ", a1: " + a1 + ", aDiff: " + aDiff);
//_log.debug ("AlbumImageDifferGen.createImageDiff: r0: " + r0 + ", r1: " + r1 + ", rDiff: " + rDiff);

				if (imageDiff != null) {
					int pixel = (a0 << 24) | (rDiff << 16) | (gDiff << 8) | bDiff;
					image3.setRGB (x, y, pixel);
				}

				totalDiff += rDiff + gDiff + bDiff;
			}
		}
		AlbumProfiling.getInstance ().exit (5, "getRGB loop");

//		if (a0a1NotEqual) {
			_log.error ("AlbumImageDifferGen.createImageDiff: a0a1NotEqual: " + a0a1NotEqual);
//		}

		double averageDiff = (double) totalDiff / (image1Width * image1Height);
		if (_Debug) {
			_log.debug ("AlbumImageDifferGen.createImageDiff: image3: totalDiff: " + totalDiff);
			_log.debug ("AlbumImageDifferGen.createImageDiff: image3: averageDiff: " + averageDiff);
		}

		if (imageDiff != null) {
			int image3Width = image3.getWidth ();
			int image3Height = image3.getHeight ();
			if (_Debug) {
				_log.debug ("AlbumImageDifferGen.createImageDiff: image3: " + image3Width + " x " + image3Height);
			}

			try {
				ImageIO.write (image3, "jpg", imageDiff);

			} catch (Exception ee) {
				_log.error ("AlbumImageDifferGen.createImageDiff: failed to write image \"" + imageDiff + "\"");
			}
		}

		AlbumProfiling.getInstance ().exit (5, "2");

		return (int) averageDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	//Creating a Buffered Image from an Image:
	//original from: http://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
	public static BufferedImage toBufferedImage (Image img)
	{
		if (img instanceof BufferedImage)
		{
			return (BufferedImage) img;
		}

		AlbumProfiling.getInstance ().enter (5);

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage (img.getWidth (null), img.getHeight (null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics ();
		bGr.drawImage (img, 0, 0, null);
		bGr.dispose ();

		AlbumProfiling.getInstance ().exit (5);

		// Return the buffered image
		return bimage;
	}


	//members
	private String _imageFilename1 = null;
	private String _imageFilename2 = null;
	private String _imageFilename3 = null;

//	private String _rootPath = null;

	private static AlbumImageDifferGen _instance = null;

	private static final String NL = System.getProperty ("line.separator");
//	private static final DecimalFormat _decimalFormat = new DecimalFormat ("+#;-#"); //print integer with +/- sign
//	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm:ss");

	private static boolean _Debug = false;
	private static Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumImageDifferGen";
}
