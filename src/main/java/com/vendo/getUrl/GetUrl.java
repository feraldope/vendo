//Original inspiration from http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html

package com.vendo.getUrl;

import com.vendo.albumServlet.AlbumImage;
import com.vendo.jpgUtils.JpgUtils;
import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.win32.ProcessUtils;
import com.vendo.win32.Win32;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;


public class GetUrl
{
	public enum FileType {JPG, MPG, WMV, MP4, AVI, Other}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		GetUrl app = new GetUrl ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
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

				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("digits") || arg.equalsIgnoreCase ("d")) {
					try {
						_overrideDigits = Integer.parseInt (args[++ii]);
						if (_overrideDigits < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/digits " + _overrideDigits);

				} else if (arg.equalsIgnoreCase ("block") || arg.equalsIgnoreCase ("b")) {
					try {
						_blockNumber = Integer.parseInt (args[++ii]);
						if (_blockNumber < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/block " + _blockNumber);

				} else if (arg.equalsIgnoreCase ("fromFile") || arg.equalsIgnoreCase ("from")) {
					try {
						_fromFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("maxMissedFiles") || arg.equalsIgnoreCase ("max") || arg.equalsIgnoreCase ("m")) {
					try {
						_maxMissedFiles = Integer.parseInt (args[++ii]);
						if (_maxMissedFiles < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/max " + _maxMissedFiles);

				} else if (arg.equalsIgnoreCase ("pad")) { // || arg.equalsIgnoreCase ("p")) {
					try {
						_pad = Integer.parseInt (args[++ii]);
						if (_pad < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/pad " + _pad);

				} else if (arg.equalsIgnoreCase ("prefix") || arg.equalsIgnoreCase ("pre")) {
					try {
						_numberPrefix = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_switches.add ("/prefix " + _numberPrefix);

				} else if (arg.equalsIgnoreCase ("start") || arg.equalsIgnoreCase ("s")) {
					try {
						_startIndex = Long.parseLong (args[++ii]);
						if (_startIndex < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_overrideStartIndex = true;
					_switches.add ("/start " + _startIndex);

				} else if (arg.equalsIgnoreCase ("sleep") || arg.equalsIgnoreCase ("sl")) {
					try {
						_sleepMillis = Integer.parseInt (args[++ii]);
						if (_sleepMillis < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/sleep " + _sleepMillis);

				} else if (arg.equalsIgnoreCase ("strip")) { // || arg.equalsIgnoreCase ("strip")) {
					_stripHead = true;
					_switches.add ("/strip");

				} else if (arg.equalsIgnoreCase ("noAutoPrefix") || arg.equalsIgnoreCase ("noa")) {
					_autoPrefix = false;

				} else if (arg.equalsIgnoreCase ("ignoreHistory") || arg.equalsIgnoreCase ("i")) {
					_ignoreHistory = true;

				} else if (arg.equalsIgnoreCase ("checkHistoryOnly") || arg.equalsIgnoreCase ("co")) {
					_checkHistoryOnly = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
//TODO - verify _destDir exists, and is writable??
		if (_destDir == null) {
			_destDir = getCurrentDirectory ();
		}
		_destDir = appendSlash (_destDir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);

		if (_fromFilename != null) {
			if (!_checkHistoryOnly) {
				displayUsage ("Must specify check if specifying fromFile", true);
			}

			if (!fileExists (_destDir + _fromFilename)) {
				displayUsage ("fromFile not found: '" + _fromFilename + "'", true);
			}

		} else {
			if (_model == null) {
				displayUsage ("No URL specified", true);
			}
			_model = VendoUtils.normalizeUrl (_model);

			if (_outputPrefix == null) {
				displayUsage ("No output file (prefix) specified", true);

			} else {
//				final String whiteList = "[0-9A-Za-z_\\-\\.]"; //all valid characters
				final String whiteList = "[0-9A-Za-z_\\-\\.\\*]"; //all valid characters
				if (_outputPrefix.replaceAll (whiteList, "").length () > 0) {
					displayUsage ("Invalid value for <output prefix> '" + _outputPrefix + "'", true);
				}

				_stripHead = true; //automatically strip header when prefix is specified
				_switches.add ("/strip");
			}

			if (_numberPrefix == null) {
				_numberPrefix = "";
			}

			setFileType ();

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

		msg += "Usage: " + _AppName + " [/debug] [/sleep <millis>] [/strip] [/ignore] [/checkHistoryOnly] [/fromFile <file>] [/dest <dest dir>] [/start <start index>] [/block <block number>] [/maxMissedFiles <count>] [/digits <number>] [/pad <number>] [/prefix <numberPrefix>] <URL> <output prefix>";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		_perfStats = new PerfStats (); //ctor starts stats timing

		if (_Debug) {
			_log.debug ("GetUrl.run");
		}

		if (_checkHistoryOnly && _fromFilename != null) {
			findInHistoryFromFile ();
			return true;
		}

		if (!parseModel ()) {
			return false;
		}

		if (!parseOutputPrefix ()) {
			return false;
		}

		if (findInHistory (_base + _headOrig, /*printResults*/ true)) {
			return false;
		}

		if (_checkHistoryOnly) {
			return true;
		}

		_index = _startIndex;

		long lastGoodIndex = _startIndex - 1;

		buildTempString ();

		boolean knowDigits = false;
		int minDigits = String.valueOf (_startIndex).length ();
		int maxDigits = (minDigits == 1 ? 4 : minDigits + 1);

		if (_digits >= 4) {
			knowDigits = true;
			minDigits = maxDigits = _digits;
		}

		if (_overrideDigits > 0) {
			knowDigits = true;
			_digits = minDigits = maxDigits = _overrideDigits;
		}

		int count = 0;
		while (true) {
			if (!knowDigits) {
				for (int ii = minDigits; ii <= maxDigits; ii++) {
					_digits = ii;
					if (getUrl ()) {
						if (_Debug) {
							_log.debug ("_digits = " + _digits);
						}
						knowDigits = true;
						if (!_overrideStartIndex) {
//							_index = _startIndex - 1; //start over at startIndex (after incrementing below)
//						} else {
							_index = -1; //start over at 0 (after incrementing below)
						}

						break;
					}
				}

				if (knowDigits) {
					count++;
					_index++;
				}
			}

			if (!knowDigits) {
				_log.error ("run: digit examination failed");

				if ((_index - _startIndex) > _maxMissedFiles) {
					break;
				} else {
					_index++;
				}
			}

			if (count > 0 && !_TestMode) {
				writeHistory ();
			}

			if (knowDigits) {
				if (getUrl ()) {
					lastGoodIndex = _index;
					count++;
				}
			}

			if (knowDigits) {
				_index++;
			}

			if ((_index - lastGoodIndex) > _maxMissedFiles) {
				break;
			}
		}

		if (count == 0 && !_TestMode) {
			_log.error ("No items downloaded");
			return false;
		}

//		if (!_TestMode) {
//			writeHistory ();
//		}

		_perfStats.print (); //print ends stats timing

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean getUrl ()
	{
		buildStrings ();

		//if we have already attempted to get this URL, just return previous results
		if (_resultsMap.contains (_urlStr)) {
//			if (_Debug)
//				_log.debug ("found '" + _urlStr + "'");
			return _resultsMap.getStatus (_urlStr);
		}

		if (fileExists (_filename)) {
			VendoUtils.printWithColor (_alertColor, "File already exists: " + _filename);
			return false;
		}

		if (_TestMode) {
			_log.debug ("                ----> Debug mode: skip download of file <----");
			if (_index < _maxMissedFiles) {
				return true;
			} else {
				return false;
			}
		}

		_log.debug ("downloading: " + _urlStr);

		int totalBytesRead = 0;
		double elapsedSeconds = 0;

		int retryCount = 250;
		int retrySleepStepMillis = 250; //increase sleep time on each failure
		int retrySleepMillis = retrySleepStepMillis;

		HttpURLConnection conn = null;
		while (true) {
			try {
				conn = getConnection (_urlStr);

				BufferedInputStream in = new BufferedInputStream (conn.getInputStream ());
				FileOutputStream out = new FileOutputStream (_tempFilename);

				final int size = 64 * 1024;
				byte bytes[] = new byte [size];

				long startNano = System.nanoTime ();
				while (true) {
					int bytesRead = in.read (bytes, 0, size);
					if (bytesRead > 0) {
						totalBytesRead += bytesRead;
						out.write (bytes, 0, bytesRead);
//testing force invalid image
//						out.write (bytes, 0, bytesRead - 1);

					} else {
						break;
					}
				}
				elapsedSeconds = (System.nanoTime () - startNano) / 1e9;

				in.close ();
				out.close ();

				break; //success

			} catch (Exception ee) {
				// HTTP Error Codes
				// 1xx: Informational
				// 2xx: Success
				// 3xx: Redirection
				// 4xx: Client Error
				// 5xx: Server Error

				// 500: Internal Server Error
				// 502: Bad Gateway
				// 503: Service Unavailable
				// 504: Gateway Timeout

//				final int errorServiceUnavailable = 503;
//				final int errorGatewayTimeout = 504;

				int responseCode = getResponseCode (conn);
				_log.error ("getUrl: error (" + responseCode + ") reading url: " + _urlStr);
				_log.error (ee); //print exception, but no stack trace

				boolean retryableCondition = ee instanceof ConnectException ||
											 ee instanceof SocketException ||
											 ee instanceof SocketTimeoutException ||
											 ee instanceof UnknownHostException ||
//											(ee instanceof IOException && responseCode == errorServiceUnavailable) ||
//											(ee instanceof IOException && responseCode == errorGatewayTimeout);
											(ee instanceof IOException && responseCode >= 500 && responseCode < 510);

				if (retryableCondition && retryCount >= 0) {
					_log.debug ("******** retries left = " + retryCount + ", sleepMillis = " + retrySleepMillis + " ********");

					closeConnection (_urlStr); //force reconnection on next pass

					sleepMillis (retrySleepMillis);

					retryCount--;
					retrySleepMillis += retrySleepStepMillis;

					continue; //retry
				}

				_resultsMap.add (_urlStr, false);
				return false;
			}
		}

		ImageSize imageSize = new ImageSize ();
		if (!validFile (_tempFilename, imageSize)) {
			_resultsMap.add (_urlStr, false);
			return false;
		}

		PerfStatsRecord record = new PerfStatsRecord (_filename, totalBytesRead , elapsedSeconds, imageSize._width, imageSize._height);
		_perfStats.add (record);

//TODO
//		private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
//		private static final short _warningColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW;

		short color = _highlightColor;
		if (imageSize._width < _alertPixels || imageSize._height < _alertPixels) {
			color = _warningColor;
		}

		VendoUtils.printWithColor (color, "Downloaded: " + record.toString2 ());

		int dist = _sizeDist.add (totalBytesRead);
		if (dist >= 3) {
			_log.warn ("*********************************************************");
			_log.warn ("Possible problem: duplicate file sizes");
			_log.warn ("*********************************************************");
		}

		boolean status = moveFile (_tempFilename, _filename);

		if (!status) { //move failed, try copying file
			status = copyFile (_tempFilename, _filename, true);
		}

//		if (status)
//			_log.debug ("downloaded file: " + _filename);

		_resultsMap.add (_urlStr, status);

		sleepMillis (_sleepMillis);

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private HttpURLConnection getConnection (String urlStr)
	{
		if (_httpURLConnection == null || (_httpURLConnection.getURL ().toString ().compareTo (_urlStr) != 0)) {
			_httpURLConnection = null;

			try {
				URL url = new URL (urlStr);
				String protocol = url.getProtocol ();

				if (protocol.compareToIgnoreCase ("https") == 0) {
					allowAllCertificates ();
					_httpURLConnection = (HttpsURLConnection) url.openConnection ();

				} else {
					_httpURLConnection = (HttpURLConnection) url.openConnection ();
				}

				String userAgent = VendoUtils.getUserAgent (true);
				_httpURLConnection.setRequestProperty ("User-Agent", userAgent);
//				_httpURLConnection.setConnectTimeout (30 * 1000); //milliseconds

			} catch (Exception ee) {
				_log.error ("getConnection: error opening url: " + urlStr);
				_log.error (ee); //print exception, but no stack trace
			}
		}

		return _httpURLConnection;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean closeConnection (String urlStr)
	{
		_httpURLConnection = null;
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//UNSAFE! allows all certificates and all hosts over https
	private static void allowAllCertificates () throws Exception
	{
		//Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager ()	{
				@Override
				public X509Certificate[] getAcceptedIssuers () {
					return null;
				}
				@Override
				public void checkClientTrusted (X509Certificate[] certs, String authType) {
				}
				@Override
				public void checkServerTrusted (X509Certificate[] certs, String authType) {
				}
			}
		};

		//Install the all-trusting trust manager
//		final SSLContext context = SSLContext.getInstance ("SSL");
		final SSLContext context = SSLContext.getInstance ("TLS");
		context.init (null, trustAllCerts, new java.security.SecureRandom ());
		HttpsURLConnection.setDefaultSSLSocketFactory (context.getSocketFactory ());

		//Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier () {
			@Override
			public boolean verify (String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier (allHostsValid);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean buildStrings ()
	{
		String urlNumberFormat = "%0" + _digits + "d";
		String urlNumber = String.format (urlNumberFormat, _index);

		String filenameNumberFormat = "%0" + _pad + "d";
		String filenameNumber = String.format (filenameNumberFormat, _index);

		_urlStr = _base  + _headOrig + _numberPrefix + urlNumber + _tailOrig;
		_filename = _destDir + _outputPrefix + _headUsed + filenameNumber + _tailUsed;

//		if (_Debug) {
//			_log.debug ("_urlStr = " + _urlStr);
//			_log.debug ("_filename = " + _filename);
//		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean validFile (String filename, ImageSize imageSize)
	{
		switch (_fileType) {
			default:
			case Other:	return true;
			case JPG:	return validImage (filename, imageSize);
			case MPG:
			case WMV:
			case MP4:
			case AVI:	return validVideo (filename);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean validImage (String filename, ImageSize imageSize)
	{
		try {
			BufferedImage image = JpgUtils.readImage (new File (filename));

			if (!JpgUtils.validateImageData (image)) {
				VendoUtils.printWithColor (_alertColor, "Image corrupt");
				return false; //image corrupt
			}

			int width = image.getWidth ();
			int height = image.getHeight ();
//			if (_Debug)
//				_log.debug ("w = " + width + ", h = " + height);
			imageSize._width = width;
			imageSize._height = height;

			if (width <= 200 || height <= 200) {
				VendoUtils.printWithColor (_warningColor, "Image too small (" + width + " x " + height + ")");
				return false; //not an image
			}

// TODO - write thumbnail -------------------------------
//			if (false) {
//				BufferedImage thumbnail = toBufferedImage (image.getScaledInstance (width / -3, height / 3, 0));
//				ImageIO.write (thumbnail, "jpg", new File (filename + "_tn.jpg"));
//			}

		} catch (Exception ee) {
			_log.error ("validImage: failed to read '" + filename + "'");
			_log.error (ee); //print exception, but no stack trace
			return false; //not an image
		}

//		if (_Debug)
//			_log.debug ("valid image: " + filename);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean validVideo (String filename)
	{
		final int compareLen = 8;
		byte[] bytes = new byte [compareLen];

		try (FileInputStream stream = new FileInputStream (new File (filename))) {
			//TODO - does this always represent the number of bytes in the stream??
			int available = stream.available ();

			if (available < 100 * 1024) {
				_log.error ("Video too small (" + available + " bytes)");
				return false;
			}

			//signature test is not always correct, so accept any sufficiently large file
			if (available > 1024 * 1024) {
				stream.close ();
				return true;
			}

			stream.read (bytes, 0, compareLen);

		} catch (Exception ee) {
			_log.error ("validVideo: failed to read '" + filename + "'");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		boolean isValid = false;

		final byte[] signatureMPG = { 0x00, 0x00, 0x01, (byte) 0xBA, 0x21, 0x00, 0x01, 0x00 };
		final byte[] signatureWMV = { 0x30, 0x26, (byte) 0xB2, 0x75, (byte) 0x8E, 0x66, (byte) 0xCF, 0x11 };

		if (_fileType == FileType.MPG) {
			isValid = compareBytes (bytes, signatureMPG);

		} else if (_fileType == FileType.WMV) {
			isValid = compareBytes (bytes, signatureWMV);

			//try again with other signature
			if (!isValid) {
				isValid = compareBytes (bytes, signatureMPG);
			}

		} else {
			_log.error ("validVideo: unhandled file type in file '" + filename + "'");
			return true; //unknown type, let it pass
		}

//		if (_Debug && isValid)
//			_log.debug ("valid video: " + filename);

		return isValid;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean compareBytes (byte[] b0, byte[] b1)
	{
		int len = Math.min (b0.length, b1.length);

		for (int ii = 0; ii < len; ii++) {
			if (b0[ii] != b1[ii]) {
				return false;
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean moveFile (String srcName, String destName)
	{
		Path src = FileSystems.getDefault ().getPath (srcName);
		Path dest = FileSystems.getDefault ().getPath (destName);

		if (Files.exists (dest)) {
			_log.error ("moveFile: destination file already exists: " + dest.toString ());
			return false;
		}

		try {
			Files.move (src, dest);

		} catch (Exception ee) {
			_log.error ("moveFile: error moving file (" + src.toString () + " to " + dest.toString () + ")");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean copyFile (String srcName, String destName, boolean deleteSrc)
	{
		Path src = FileSystems.getDefault ().getPath (srcName);
		Path dest = FileSystems.getDefault ().getPath (destName);

		if (Files.exists (dest)) {
			_log.error ("copyFile: destination file already exists: " + dest.toString ());
			return false;
		}

		try {
			Files.copy (src, dest);

		} catch (Exception ee) {
			_log.error ("copyFile: error copying file (" + src.toString () + " to " + dest.toString () + ")");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		if (deleteSrc) {
			try {
				Files.delete (src);

			} catch (Exception ee) {
				_log.error ("copyFile: error deleting '" + srcName + "'");
				_log.error (ee); //print exception, but no stack trace
//				return false;
			}
		}

// old way - keep as example
//		try {
//			BufferedInputStream in = new BufferedInputStream (new FileInputStream (new File (srcName)));
//			BufferedOutputStream out = new BufferedOutputStream (new FileOutputStream (new File (destName)));
//
//			int size = 0;
//			byte[] buf = new byte[1024];
//
//			while ((size = in.read (buf)) >= 0)
//				out.write (buf, 0, size);
//
//			in.close ();
//			out.close ();
//
//		} catch (Exception ee) {
//			_log.error ("copyFile: error copying '" + srcName + "' to '" + destName + "'");
//			_log.error (ee); //print exception, but no stack trace
//			return false;
//		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean fileExists (String filename)
	{
		Path file = FileSystems.getDefault ().getPath (filename);

		return Files.exists (file);
	}

	///////////////////////////////////////////////////////////////////////////
	private static String getCurrentDirectory ()
	{
		Path file = FileSystems.getDefault ().getPath ("");
		String dir = file.toAbsolutePath ().toString ();

		return dir;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String appendSlash (String dir) //append slash if necessary
	{
		int lastChar = dir.charAt (dir.length () - 1);
		if (lastChar != '/' && lastChar != '\\') {
			dir += _slash;
		}

		return dir;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean parseModel ()
	{
		_model = _model.replaceAll("\\.thumb\\.jpg", ".jpg");
		_model = _model.replaceAll("\\/thumbs\\/", "/");
		_model = _model.replaceAll("\\/tn\\/", "/");
		_model = _model.replaceAll("\\/tn_/", "/");
		_model = _model.replaceAll("\\/tn-/", "/");

		int lastSlash = _model.lastIndexOf ('/');
		if (lastSlash < 0) {
			_log.error ("parseModel: no slash found in '" + _model + "'");
			return false;
		}

		_base = _model.substring (0, lastSlash + 1);
		String remainder = _model.substring (lastSlash + 1);

		String parts1[] = splitLeaf (remainder, _blockNumber);

		if (_Debug) {
			_log.debug ("parts1[" + parts1.length + "] = " + VendoUtils.arrayToString (parts1));
		}

		if (parts1.length != 2) {
			_log.error ("parseModel: splitLeaf() failed for '" + remainder + "'");
			return false;
		}

		//process tail
		_tailUsed = _tailOrig = parts1[1];
		String parts2[] = _tailOrig.toLowerCase ().split ("\\."); //regex
		if (parts2.length == 2) {
			_tailUsed = "." + parts2[1];
		}

		if (_tailUsed.equals (".jpeg")) {
			_tailUsed = ".jpg";
		}

		//process head
		if (parts1.length == 2) {
			_headOrig = parts1[0];
		} else {
			_headOrig = "";
		}

		if (_stripHead) {
			_headUsed = "";
		} else {
			_headUsed = _headOrig;
		}

		//how many digits did we remove? (subtract length of _numberPrefix)
		_digits = remainder.length () - (_headOrig.length () + _tailOrig.length ()) - _numberPrefix.length ();

		if (_Debug) {
			_log.debug ("_base = " + _base);
			_log.debug ("_headOrig = " + _headOrig + ", _headUsed = " + _headUsed);
			if (!".jpg".equalsIgnoreCase (_tailOrig)) {
				_log.debug ("_tailOrig = " + _tailOrig);
			}
			if (!".jpg".equalsIgnoreCase (_tailUsed)) {
				_log.debug ("_tailUsed = " + _tailUsed);
			}
			_log.debug ("_digits = " + _digits);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean parseOutputPrefix ()
	{
		if (!_model.toLowerCase ().endsWith (".jpg")) { //this processing only applies to specific file types
			return true;
		}

		boolean gotNewName = false;
		short color = _highlightColor;
		if (_autoPrefix) {
			ImageBaseNames imageBaseNames = new ImageBaseNames (_outputPrefix);

			if (imageBaseNames.hasInvalidMatch ()) {
				VendoUtils.printWithColor (_alertColor, "Error: invalid basename: \"" + _outputPrefix + "\"");
				return false;

			} else if (imageBaseNames.hasMultipleMatches ()) {
				VendoUtils.printWithColor (_alertColor, "Error: multiple matching basenames found for \"" + _outputPrefix + "\": " + imageBaseNames.getMatchingNames ());
				return false;

			} else if (imageBaseNames.hasNoMatches ()) {
				if (imageBaseNames.usingWildcards ()) {
					VendoUtils.printWithColor (_alertColor, "Error: no matching basenames found for \"" + _outputPrefix + "\"");
					return false;
				}

				gotNewName = true;
				color = _warningColor;
				_outputPrefix = imageBaseNames.getNewName ();

			} else {
				color = _highlightColor;
				_outputPrefix = imageBaseNames.getNextName ();
			}
		}

		if (!_outputPrefix.contains ("-")) {
			_outputPrefix += "-"; //add trailing dash if none found
		}

		VendoUtils.printWithColor (color, "_outputPrefix = " + _outputPrefix);

		if (gotNewName) {
			sleepMillis (1000); //pause here to allow user to react
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String[] splitLeaf (String leaf, int blockNumber)
	{
		final String pattern = "[0-9]+"; //sequence of digits
		final String marker = "::::";

		//split string on nth group of digits (reverse string, find nth group, reverse back, split)
		String s1 = VendoUtils.reverse (leaf);
		String s2 = VendoUtils.replacePattern (s1, pattern, marker, blockNumber);
		String s3 = VendoUtils.reverse (s2);
		String parts[] = s3.split (marker);

		return parts;
	}

	///////////////////////////////////////////////////////////////////////////
	private void buildTempString ()
	{
		_tempFilename = _destDir + "000000." + new Date ().getTime () + "." + _pid + ".tmp";
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean findInHistory (String basePattern, boolean printResults)
	{
//		String pattern = stripHttpHeader (basePattern).toLowerCase ();
		String pattern = VendoUtils.getUrlFileComponent (basePattern).toLowerCase ();
		if (_Debug) {
			_log.debug ("findInHistory: basePattern = " + basePattern);
			_log.debug ("findInHistory: pattern = " + pattern);
		}

		if (pattern.length () == 0 || pattern.equals ("/")) {
			return false;
		}

		if (_historyFileContents.size () == 0) {
			try (BufferedReader reader = new BufferedReader (new FileReader (_destDir + _historyFilename))) {
				String line = new String ();
				while ((line = reader.readLine ()) != null) {
					_historyFileContents.add (line);
				}

			} catch (IOException ee) {
				_log.error ("findInHistory: error reading history file \"" + _historyFilename + "\"");
				_log.error (ee); //print exception, but no stack trace
				return false;
			}
		}

		boolean found = false;

		int maxLines = 6;	//TODO - come up with better solution??
		int numLines = _historyFileContents.size ();
		for (int ii = 0; ii < numLines; ii++) {
			String baseLine = _historyFileContents.get (ii);
			String line = /*stripHttpHeader*/ (baseLine).toLowerCase ();
			if (line.contains (pattern)) {
				if (!found) { //print header
					found = true;

					if (printResults) { //print header
						System.out.println ("Duplicate entry(s) found in history file:");
						System.out.println ("---- " + basePattern);
					}
				}

				if (printResults) {
					System.out.print ("  ");
					VendoUtils.printWithColor (_alertColor, baseLine, /*includeNewLine*/ false);
//					System.out.println (" (" + (ii + 1) + "/" + numLines + ")");
					System.out.println (" (" + (ii + 1 - numLines) + ")");
					if (--maxLines == 0) {	//TODO - come up with better solution??
						System.out.println (" ... omitting rest");
						break;
					}
				}
			}
		}

		if (found && printResults) {
			System.out.println ("");
		}

		return (_ignoreHistory ? false : found);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean findInHistoryFromFile ()
	{
		Vector<String> fromFileContents = new Vector<String> ();
		try (BufferedReader reader = new BufferedReader (new FileReader (_destDir + _fromFilename))) {
			String line = new String ();
			while ((line = reader.readLine ()) != null) {
				fromFileContents.add (line);
			}

		} catch (IOException ee) {
			_log.error ("findInHistoryFromFile: error reading from file \"" + _fromFilename + "\"");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		int numLines = fromFileContents.size ();
		for (int ii = 0; ii < numLines; ii++) {
			_model = fromFileContents.get (ii);

			if (!parseModel ()) {
				continue; //parseModel prints error
			}

			if (!findInHistory (_base + _headOrig, false)) {
				System.out.println (VendoUtils.normalizeUrl (_model)); //print all lines not found in history
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeHistory ()
	{
		if (_wroteHistory) {
			return true;
		}

		Collections.sort (_switches, VendoUtils.caseInsensitiveStringComparator);

		String command = "gu " + _model;
		if (!_outputPrefix.isEmpty ()) {
			command += " " + _outputPrefix;
		}
		for (String str : _switches) {
			command += " " + str;
		}
		command += NL;

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream (new File (_destDir + _historyFilename), /*append*/ true);

		} catch (IOException ee) {
			_log.error ("writeHistory: error opening output file \"" + _historyFilename + "\"");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		try {
			outputStream.write (command.getBytes ());
			outputStream.flush ();
			outputStream.close ();

		} catch (IOException ee) {
			_log.error ("writeHistory: error writing to output file \"" + _historyFilename + "\"");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		_wroteHistory = true;

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private FileType setFileType ()
	{
		String extension = _model.toLowerCase ();

		if (extension.endsWith (".jpg") || extension.endsWith (".jpeg")) {
			_fileType = FileType.JPG;
		} else if (extension.endsWith (".mpg")) {
			_fileType = FileType.MPG;
		} else if (extension.endsWith (".wmv")) {
			_fileType = FileType.WMV;
		} else if (extension.endsWith (".mp4")) {
			_fileType = FileType.MP4;
		} else if (extension.endsWith (".avi")) {
			_fileType = FileType.AVI;
		} else {
			_fileType = FileType.Other;
		}

		return _fileType;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getResponseCode (HttpURLConnection conn)
	{
		int responseCode = 0;

		try {
			responseCode = conn.getResponseCode ();

		} catch (Exception ee) {
//			_log.error ("getResponseCode: exception:");
//			_log.error (ee); //print exception, but no stack trace
		}

		return responseCode;
	}

	///////////////////////////////////////////////////////////////////////////
	public static void sleepMillis (int milliseconds)
	{
		if (milliseconds > 0) {
			try {
				Thread.sleep (milliseconds);

			} catch (Exception ee) {
				_log.debug ("Thread.sleep exception", ee);
				_log.error (ee); //print exception, but no stack trace
			}
		}
	}

	private class ImageSize
	{
		///////////////////////////////////////////////////////////////////////////
		//holder for image size data
		public int _width;
		public int _height;
	}

	private class SizeDist
	{
		///////////////////////////////////////////////////////////////////////////
		//returns the number of times this value is in the hash (including the instance just added)
		public int add (Integer bytes)
		{
			Integer value = _dist.get (bytes);

			int count = (value != null ? value.intValue () : 0);

			_dist.put (bytes, new Integer (++count));

//			if (false) { //debug
//				_log.debug ("SizeDist.add - hash dump:");
//				for (Map.Entry<Integer, Integer> entry : _dist.entrySet ()) {
//					Integer key = entry.getKey ();
//					Integer val = entry.getValue ();
//					_log.debug (key + " bytes (" + val + " instances)");
//				}
//			}

//old way uses iterator (save as interesting example)
//			if (false) { //debug
//				for (Iterator<Map.Entry<Integer, Integer>> iter = _dist.entrySet ().iterator (); iter.hasNext ();) {
//					Map.Entry<Integer, Integer> entry = iter.next ();
//					Integer key = entry.getKey ();
//					Integer val = entry.getValue ();
//					_log.debug (key + " bytes (" + val + " instances)");
//				}
//			}

			return count;
		}

		private final HashMap<Integer, Integer> _dist = new HashMap<Integer, Integer> (32);
	}

	private class PerfStats
	{
		///////////////////////////////////////////////////////////////////////////
//		public PerfStats () //ctor starts timing
//		{
//		}

		///////////////////////////////////////////////////////////////////////////
		//holds performance statistics
		public void add (PerfStatsRecord record)
		{
			_records.add (record);
		}

		///////////////////////////////////////////////////////////////////////////
		public void print () //print ends timing
		{
			Instant endInstant = Instant.now ();
//			String elapsedTimeString = Duration.between (_startInstant, endInstant).toString (); //default ISO-8601 seconds-based representation
			String elapsedTimeString = LocalTime.ofNanoOfDay (Duration.between (_startInstant, endInstant).toNanos ()).format (_dateTimeFormatter);

			Collections.sort (_records, new AlphanumComparator ());

			double totalBytes = 0;
			double totalSeconds = 0;
			for (PerfStatsRecord record : _records) {
				totalBytes += record._bytesRead;
				totalSeconds += record._elapsedSeconds;

				short color = _highlightColor;
				if (record._width < _alertPixels || record._height < _alertPixels) {
					color = _warningColor;
				}

				VendoUtils.printWithColor (color, record.toString2 ());
			}

			double totalBitsPerSec = 8 * totalBytes / totalSeconds;

			System.out.println (_records.size () + " items downloaded, " +
								elapsedTimeString + " elapsed, " +
								VendoUtils.unitSuffixScale (totalBytes, 0) + ", " +
								VendoUtils.unitSuffixScale (totalBitsPerSec, 0) + "ps average");
		}

		private final Instant _startInstant = Instant.now (); //ctor starts timing
		private final Vector<PerfStatsRecord> _records = new Vector<PerfStatsRecord> ();
	}

	private class PerfStatsRecord
	{
		///////////////////////////////////////////////////////////////////////////
		//helper for performance statistics
		public PerfStatsRecord (String filename, double bytesRead, double elapsedSeconds, int width, int height)
		{
			File file = new File (filename);
			_filename = file.getName ();

			_bytesRead = bytesRead;
			_elapsedSeconds = elapsedSeconds;

			_bitsPerSec = 8 * _bytesRead / _elapsedSeconds;

			_width = width;
			_height = height;
		}

		///////////////////////////////////////////////////////////////////////////
		//required for AlphanumComparator
		@Override
		public String toString ()
		{
			return _filename;
		}

		///////////////////////////////////////////////////////////////////////////
		public String toString2 ()
		{
			return _filename + ", " +
				   _width + "x" + _height + ", " +
				   VendoUtils.unitSuffixScale (_bytesRead, 0) + ", " +
				   VendoUtils.unitSuffixScale (_bitsPerSec, 0) + "ps";
		}

		public final String _filename;
		public final double _bytesRead;
		public final double _bitsPerSec;
		public final double _elapsedSeconds;
		public final int _width;
		public final int _height;
	}

	private class ResultsMap
	{
		///////////////////////////////////////////////////////////////////////////
		//holds attempts and results
		public void add (String urlStr, Boolean status)
		{
			_results.put (urlStr, status);
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean contains (String urlStr)
		{
			return _results.containsKey (urlStr);
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean getStatus (String urlStr)
		{
			return _results.get (urlStr);
		}

		private final HashMap<String, Boolean> _results = new HashMap<String, Boolean> (64);
	}

	private class ImageBaseNames
	{
		///////////////////////////////////////////////////////////////////////////
		public ImageBaseNames (String outputPrefixOrig)
		{
			_outputPrefixOrig = outputPrefixOrig;
			_outputPrefixBase = outputPrefixOrig.replaceAll ("[\\-\\._]", ""); //regex
			String outputPrefix = _outputPrefixBase;

			if (_classDebug) {
				System.out.println ("GetUrl.ImageBaseNames: outputPrefix @1 = " + outputPrefix);
			}

			outputPrefix = outputPrefix.replaceAll ("\\d+.*", ""); //regex: digits and anything after
			if (outputPrefix.contains ("*")) {
				outputPrefix = outputPrefix.replaceAll ("\\*", ".*"); //regex
			}

			if (_classDebug) {
				System.out.println ("GetUrl.ImageBaseNames: outputPrefix @2 = " + outputPrefix);
			}

			String subdir = "/jroot/" + AlbumImage.getSubFolderFromName (outputPrefix);

			_filenameList.addAll (getFileList (_destDir, outputPrefix));
			_filenameList.addAll (getFileList (_destDir + subdir, outputPrefix));
			Collections.sort (_filenameList, VendoUtils.caseInsensitiveStringComparator);

//			if (_classDebug) {
//				System.out.println ("GetUrl.ImageBaseNames: filenameList = " + _filenameList);
//			}

			for (String filename : _filenameList) {
				_sortedBaseNameSet.add (filename.replaceAll ("\\d+.*", "")); //regex: digits and anything after
			}

			if (_classDebug) {
				System.out.println ("GetUrl.ImageBaseNames: baseNames = " + StringUtils.join (_sortedBaseNameSet, ','));
			}
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean usingWildcards ()
		{
			if (_classDebug) {
				System.out.println ("GetUrl.usingWildcards: " + _outputPrefixBase.contains ("*"));
			}

			return _outputPrefixBase.contains ("*");
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean hasInvalidMatch ()
		{
			boolean invalidMatch = !usingWildcards () && fileExists (_destDir + _outputPrefixOrig) && !_outputPrefixOrig.toLowerCase ().endsWith (".jpg");

			if (_classDebug) {
				System.out.println ("GetUrl.hasInvalidMatch: " + invalidMatch);
			}

			return invalidMatch;
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean hasMultipleMatches ()
		{
			if (_classDebug) {
				System.out.println ("GetUrl.hasMultipleMatches: " + (_sortedBaseNameSet.size () > 1));
			}

			return _sortedBaseNameSet.size () > 1;
		}


		///////////////////////////////////////////////////////////////////////////
		public boolean hasNoMatches ()
		{
			if (_classDebug) {
				System.out.println ("GetUrl.hasNoMatches: " + (_filenameList.size () == 0));
			}

			return _filenameList.size () == 0;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getMatchingNames ()
		{
			if (!hasMultipleMatches ()) {
				throw new RuntimeException ("invalid call to getMatchingNames()");
			}

			return StringUtils.join (_sortedBaseNameSet, ',');
		}

		///////////////////////////////////////////////////////////////////////////
		public String getNewName ()
		{
			if (!hasNoMatches ()) {
				throw new RuntimeException ("invalid call to getNewName()");
			}

			return _outputPrefixBase.replaceAll ("\\d+", "").replaceAll ("-", "") + "01"; //remove any/all digits and dashes, then append "01"
		}

		///////////////////////////////////////////////////////////////////////////
		public String getNextName ()
		{
			if (hasNoMatches () || hasMultipleMatches ()) {
				throw new RuntimeException ("invalid call to getNextName()");
			}

			String lastFileName = _filenameList.get (_filenameList.size () - 1);

			if (_classDebug) {
				System.out.println ("GetUrl.getNextName: lastFileName = " + lastFileName);
			}

			String[] parts1 = splitLeaf (lastFileName, /*blockNumber*/ 0);

			String outputPrefix = parts1[0].replaceAll ("\\d+-", ""); //replace all digits and dashes (with empty string)
			String numberStr = parts1[0].replaceAll ("\\D+", ""); //replace all non-digits (with empty string)

			int nextNumber = 1 + Integer.valueOf (numberStr);
			if ((nextNumber % 10) == 0) {
				nextNumber++;
			}

			String format = "%0" + numberStr.length () + "d";

			return outputPrefix + String.format (format, nextNumber);
		}

		///////////////////////////////////////////////////////////////////////////
		private List<String> getFileList (String dirName, final String nameWild)
		{
			//digits in following pattern prevent e.g. "Foo01" from matching "FooBar01"
			Pattern pattern = Pattern.compile (nameWild + "[0-9][0-9].*" + "\\" + _tailUsed, Pattern.CASE_INSENSITIVE);

			if (_classDebug) {
				System.out.println ("GetUrl.getFileList: dirName = " + dirName + ", nameWild = " + nameWild + ", pattern = " + pattern.pattern ());
			}

			//for simple dir listings, it looks like java.io package is faster than java.nio
			FilenameFilter filenameFilter = new FilenameFilter () {
				@Override
				public boolean accept (File dir, String name) {
					return pattern.matcher (name).matches ();
				}
			};

			File dir = new File (dirName);
			String[] files = dir.list (filenameFilter);
//			if (_classDebug) {
//				System.out.println ("GetUrl.getFileList: files = " + files);
//			}
			return (files != null ? Arrays.asList (dir.list (filenameFilter)) : new ArrayList<String> ());
		}

		private final boolean _classDebug = false;
		private final String _outputPrefixOrig;
		private final String _outputPrefixBase;
		private final List<String> _filenameList = new ArrayList<String> ();
		private final TreeSet<String> _sortedBaseNameSet = new TreeSet<String> (VendoUtils.caseInsensitiveStringComparator);
	}

/*
	///////////////////////////////////////////////////////////////////////////
	//Creating a Buffered Image from an Image:
	//original from: http://www.exampledepot.com/egs/java.awt.image/Image2Buf.html
	//
	// This method returns a BufferedImage with the contents of an image
	public static BufferedImage toBufferedImage (Image image)
	{
		if (image instanceof BufferedImage) {
			return (BufferedImage)image;
		}

		// This code ensures that all the pixels in the image are loaded
		image = new ImageIcon (image).getImage ();

		// Determine if the image has transparent pixels; for this method's
		// implementation, see e661 Determining If an Image Has Transparent Pixels
		boolean hasAlpha = false; //hasAlpha (image);

		// Create a buffered image with a format that's compatible with the screen
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment ();
		try {
			// Determine the type of transparency of the new buffered image
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}

			// Create the buffered image
			GraphicsDevice gs = ge.getDefaultScreenDevice ();
			GraphicsConfiguration gc = gs.getDefaultConfiguration ();
			bimage = gc.createCompatibleImage (image.getWidth (null), image.getHeight (null), transparency);
		} catch (HeadlessException ee) {
			// The system does not have a screen
		}

		if (bimage == null) {
			// Create a buffered image using the default color model
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage (image.getWidth (null), image.getHeight (null), type);
		}

		// Copy image to buffered image
		Graphics g = bimage.createGraphics ();

		// Paint the image onto the buffered image
		g.drawImage (image, 0, 0, null);
		g.dispose ();

		return bimage;
	}
*/

	//private members
	private int _digits = 1;
	private int _overrideDigits = -1;
	private int _pad = 2;
	private int _sleepMillis = 0;
	private long _index = 0;
	private long _startIndex = 1;
	private int _maxMissedFiles = 3;
	private int _blockNumber = 0;
	private boolean _overrideStartIndex = false;
	private boolean _stripHead = false;
	private boolean _autoPrefix = true;
	private boolean _ignoreHistory = false;
	private boolean _checkHistoryOnly = false;
	private boolean _wroteHistory = false;
	private String _fromFilename = null;
	private String _model = null;
	private String _numberPrefix = null;
	private String _outputPrefix = null;
	private String _base = null;
	private String _headOrig = null;
	private String _headUsed = null;
	private String _tailOrig = null;
	private String _tailUsed = null;
	private String _urlStr = null;
	private String _filename = null;
	private String _destDir = null;
	private String _tempFilename = null;
	private FileType _fileType = FileType.Other;
	private SizeDist _sizeDist = new SizeDist ();
	private PerfStats _perfStats = null;
	private ResultsMap _resultsMap = new ResultsMap ();
	private Vector<String> _switches = new Vector<String> ();
	private Vector<String> _historyFileContents = new Vector<String> ();
	private HttpURLConnection _httpURLConnection = null;
	private static final String _historyFilename = "gu.history.txt";
	private static final String _slash = System.getProperty ("file.separator");
	private static final Integer _pid = ProcessUtils.getWin32ProcessId ();
	private static Logger _log = LogManager.getLogger (GetUrl.class);

	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm:ss"); //note this wraps values >= 60 minutes

	private static final int _alertPixels = 640;
	private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
	private static final short _warningColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW;
	private static final short _highlightColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA;

	//global members
	public static boolean _Debug = false;
	public static boolean _TestMode = false;

	public static final String _AppName = "GetUrl";
	public static final String NL = System.getProperty ("line.separator");
}
