/*Original inspiration from http://java.sun.com/docs/books/tutorial/networking/urls/readingURL.html

JavaDoc
https://jsoup.org/apidocs/index.html?org/jsoup/package-summary.html

testing:

gi /d "http://testing.com/index.shtml?page=0"
gi /d "http://testing.com/index.shtml?page=0" 0 4
gi /d /p foo /e .html "http://testing.com/index.shtml?page=0"
gi /d /p foo /e .html "http://testing.com/index.shtml?page=0" 0 4
gi /d /b 1 "http://testing.com/index.shtml?page=0&id=3456" 0 4 1

Test for User-Agent:
gi http://localhost/servlet/coreservlets.ShowRequestHeaders
*/

package com.vendo.getUri;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.win32.ProcessUtils;
import com.vendo.win32.Win32;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.util.Date;
import java.util.*;


public class GetUri
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		GetUri app = new GetUri ();

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

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

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
						_prefix = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_switches.add ("/prefix " + _prefix);

				} else if (arg.equalsIgnoreCase ("exten") || arg.equalsIgnoreCase ("e")) {
					try {
						_exten = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_switches.add ("/exten " + _exten);

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

				} else if (arg.equalsIgnoreCase ("extr1")) {// || arg.equalsIgnoreCase ("lo")) {
					_extr1 = true;

				} else if (arg.equalsIgnoreCase ("ignoreHistory") || arg.equalsIgnoreCase ("i")) {
					_ignoreHistory = true;

				} else if (arg.equalsIgnoreCase ("image") || arg.equalsIgnoreCase ("im")) {
					_image = true;

				} else if (arg.equalsIgnoreCase ("imageLinksOnly") || arg.equalsIgnoreCase ("ilo")) {
					_imageLinksOnly = true;

				} else if (arg.equalsIgnoreCase ("linksOnly") || arg.equalsIgnoreCase ("lo")) {
					_linksOnly = true;

				} else if (arg.equalsIgnoreCase ("checkHistoryOnly") || arg.equalsIgnoreCase ("co")) {
					_checkHistoryOnly = true;

				} else if (arg.equalsIgnoreCase ("noHistory") || arg.equalsIgnoreCase ("noh")) {
					_noHistory = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_startIndex < 0) {
					try {
						_startIndex = Integer.parseInt (arg);
						if (_startIndex < 0) {
							throw (new NumberFormatException ());
						}
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for starting index: '" + arg + "'", true);
					}

				} else if (_endIndex < 0) {
					try {
						_endIndex = Integer.parseInt (arg);
						if (_endIndex < 0) {
							throw (new NumberFormatException ());
						}
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for ending index", true);
					}

				} else if (_step < 0) {
					try {
						_step = Integer.parseInt (arg);
						if (_step < 0) {
							throw (new NumberFormatException ());
						}
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for step", true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_model == null) {
			displayUsage ("No URI specified", true);
		}
		_model = normalizeUrl2 (_model);

		try {
			//construct URI, which will decode any escaped chars in string
			URI uri = new URI (_model);

			_baseUri = new String ();
			if (uri.getScheme () != null) {
				_baseUri += uri.getScheme () + "://";
			}
			if (uri.getAuthority () != null) {
				_baseUri += uri.getAuthority ();
			}

		} catch (Exception ee) {
//TODO		displayUsage ("TBD", true);
			_log.error ("getUrl: error parsing URI '" + _model + "'");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

//TODO - verify _destDir exists, and is writable??
		if (_destDir == null) {
			_destDir = getCurrentDirectory ();
		}
		_destDir = appendSlash (_destDir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);

		if (_startIndex >= 0 && _endIndex >= 0) {
			if (_startIndex > _endIndex) {
				displayUsage("<start index> (" + _startIndex + ") + cannot be greater than <end index> (" + _endIndex + ")", true);
			}
		}

		if (_step < 0 && (_startIndex > 0 || _endIndex > 0)) {
			_step = 1;
		}

		if (_prefix == null) {
			_prefix = "";
		} else if (!validFileChars (_prefix)) {
			displayUsage ("Invalid value for <file prefix> '" + _prefix + "'", true);
		}

		if (_exten == null) {
			_exten = ".htm";
		} else if (!validFileChars (_exten)) {
			displayUsage ("Invalid value for <file extension> '" + _exten + "'", true);
		}

		if (false) { //dump all args
			_log.debug ("args.length = " + args.length);
			for (int ii = 0; ii < args.length; ii++) {
				_log.debug ("args[" + ii + "] = '" + args[ii] + "'");
			}
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

		msg += "Usage: " + _AppName + " [/debug] [/prefix <file prefix>] [/pad <number>] [/exten <file extension>] [<start index> <end index> [<step>]] [/destDir <folder>] [/checkHistoryOnly] [/block <block number>] [/ignoreHistory] [/noHistory] [/image] [/imageLinksOnly] [/linksOnly]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		if (_Debug) {
			_log.debug ("GetUri.run");
		}

		if (!parseModel ()) {
			return false;
		}

		if (!_noHistory) {
			if (findInHistory (_model)) {
				return false;
			}

			if (_checkHistoryOnly) {
				return true;
			}
		}

		buildTempString ();

		int count = 0;

		if (_startIndex < 0 || _endIndex < 0) {
			if (getUrl ()) {
				count++;
			}

		} else {
			for (_index = _startIndex; _index <= _endIndex; _index += _step) {
				if (getUrl ()) {
					count++;
				}
			}
		}

		if (count == 0 && !_TestMode) {
			_log.error ("No items downloaded");
			System.out.println ("");
			return false;
		}

		if (!_noHistory && !_TestMode) {
			writeHistory ();
		}

		_perfStats.print ();

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean getUrl ()
	{
		buildStrings ();

		if (fileExists (_filename)) {
			_log.debug ("File already exists: " + _filename);
			return false;
		}

		if (_TestMode) {
			_log.debug ("                ----> Debug mode: skip download of file <----");
			return true;
		}

		URL url = null;
		HttpURLConnection conn = null;
		try {
			url = new URL (_urlStr);
			conn = getConnection (_urlStr);

		} catch (Exception ee) {
			_log.error ("getUrl: error opening url '" + _urlStr + "'");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		_log.debug ("downloading: " + url);

		int totalBytesRead = 0;
		double elapsedSeconds = 0;

		//download URL into byte stream
		String wholeFile = new String ();
		try {
			BufferedInputStream in = new BufferedInputStream (conn.getInputStream ());
			OutputStream out = null;
			if (_image) {
				out = new FileOutputStream (_tempFilename);
			} else {
				out = new ByteArrayOutputStream ();
			}

			final int size = 64 * 1024;
			byte bytes[] = new byte [size];

			long startNano = System.nanoTime ();
			while (true) {
				int bytesRead = in.read (bytes, 0, size);
				if (bytesRead > 0) {
					totalBytesRead += bytesRead;
					out.write (bytes, 0, bytesRead);

				} else {
					break;
				}
			}
			elapsedSeconds = (double) (System.nanoTime () - startNano) / 1e9;

			in.close ();

			if (_image) {
				out.close ();
			} else {
				wholeFile = new String (((ByteArrayOutputStream) out).toByteArray (), "UTF-8");
			}

		} catch (Exception ee) {
			_log.error ("getUrl: error reading url '" + _urlStr + "'");
			_log.error (ee);
			return false;
		}

		closeConnection (_urlStr);

		if (!_image) {
			if (_imageLinksOnly) {
				Document doc = Jsoup.parse (wholeFile, _model);
				Elements linkElements = doc.select ("[src]");

				Set<String> linkSet = new HashSet<String> (); //use Set to eliminate duplicates
				for (Element link : linkElements) {
					if (link.tagName ().equals ("img")) {
						linkSet.add (link.attr ("abs:src"));
					}
				}
				List<String> linkList = new ArrayList<String> ();
				linkList.addAll (linkSet);
				Collections.sort (linkList, VendoUtils.caseInsensitiveStringComparator);

				wholeFile = new String ();
				for (String string  : linkList) {
					wholeFile += string + NL;
				}
			}

			if (_linksOnly) {
				Document doc = Jsoup.parse (wholeFile, _model);
				Elements linkElements = doc.select ("a[href]");

				Set<String> linkSet = new HashSet<String> (); //use Set to eliminate duplicates
				for (Element link : linkElements) {
					linkSet.add (link.attr ("abs:href"));
				}
				List<String> linkList = new ArrayList<String> ();
				linkList.addAll (linkSet);
				Collections.sort (linkList, VendoUtils.caseInsensitiveStringComparator);

				wholeFile = new String ();
				for (String string : linkList) {
					if (!string.contains ("/track/")) {
						wholeFile += string + NL;
					}
				}
			}

			if (_extr1) {
				Document doc = Jsoup.parse (wholeFile, _model);
				Element firstDiv = doc.select ("div").first ();
				if (firstDiv == null) {
					System.err.println ("Unable to find div");

				} else {
//					String protocol = "https:";
//					String imageHost = new String ();
					String imageHost = _baseUri;
					String imageDir = "/image/fl/";
					String imageName = new String ();

					List<Node> nodeList = new ArrayList<Node> ();
					NodeVisitor myNodeVisitor = new MyNodeVisitor (nodeList);
					NodeTraversor traversor = new NodeTraversor (myNodeVisitor);
					traversor.traverse (firstDiv);

					boolean localDebug = false;
					for (Node node : nodeList) {
						String nodeName = node.nodeName ();
						if (nodeName.equals ("input")) {
							Attributes attributes = node.attributes ();

							if (localDebug ) {
								System.err.println ("---------------------");
								System.err.println ("nodeName: " + nodeName);
								System.err.println ("attributes: " + attributes.size ());
								for (Attribute attribute : attributes) {
									System.err.println (attribute);
								}
								System.err.println ("---------------------");
							}


//							if (attributes.get ("id").equals ("imageHost")) {
//								imageHost = attributes.get ("value");
//							} else if (attributes.get ("id").equals ("imageDir")) {
//								imageDir = attributes.get ("value");
							/*} else*/ if (attributes.get ("id").equals ("imageName")) {
								imageName = attributes.get ("value");
							}
						}
					}
//					wholeFile = protocol + imageHost + imageDir + imageName + NL;
					wholeFile = imageHost + imageDir + imageName + NL;

					if (localDebug ) {
						System.err.println ("wholeFile: " + wholeFile);
					}
//					_log.debug ("wholeFile: " + wholeFile);
				}

/* old way
				Document doc = Jsoup.parse (wholeFile, _model);
				Elements divElements = doc.select ("div");
				for (Element div : divElements) {
					String divClass = div.attr ("class");
					if (divClass.equalsIgnoreCase ("photo")) {
						String imageDir = div.getElementById ("imageDir").attr("value");
						String imageName = div.getElementById ("imageName").attr("value");
						wholeFile = _baseUri + "/" + imageDir + "/" + imageName + NL;
					}
				}
*/
			}

			//write modified contents to file
			try {
				FileOutputStream out = new FileOutputStream (_tempFilename);

				out.write (wholeFile.getBytes ());
				out.close ();

			} catch (Exception ee) {
				_log.error ("getUrl: error writing output file  '" + _tempFilename + "'");
				_log.error (ee);
				return false;
			}
		}

		PerfStatsRecord record = new PerfStatsRecord (_filename, totalBytesRead , elapsedSeconds);
		_perfStats.add (record);

		_log.debug ("Downloaded: " + record.toString2 (), true);

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
				_httpURLConnection.setConnectTimeout (30 * 1000); //milliseconds

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
		int next = _index;
		do {
			String filenameNumberFormat = "%0" + _pad + "d";
			String filenameNumber = String.format (filenameNumberFormat, next++);

			_filename = _destDir + _prefix + filenameNumber + _exten;
		} while (fileExists (_filename));

		if (_startIndex < 0 || _endIndex < 0) {
			_urlStr = _model;

		} else {
			String urlNumberFormat = "%0" + _digits + "d";
			String urlNumber = String.format (urlNumberFormat, _index);

			_urlStr = _head + urlNumber + _tail;
		}

//		if (_Debug) {
//			_log.debug ("_urlStr = " + _urlStr);
//			_log.debug ("_filename = " + _filename);
//		}

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
			_log.error (ee);
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
			_log.error (ee);
			return false;
		}

		if (deleteSrc) {
			try {
				Files.delete (src);

			} catch (Exception ee) {
				_log.error ("copyFile: error deleting '" + srcName + "'");
				_log.error (ee);
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
//			_log.error (ee);
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
		int lastSlash = _model.lastIndexOf ('/');
		if (lastSlash < 0) {
			_log.error ("parseModel: no slash found in '" + _model + "'");
			return false;
		}

		String parts[] = splitLeaf (_model, _blockNumber);

		_head = parts[0];
		if (parts.length > 1) {
			_tail = parts[1];
		}

		_digits = _model.length () - _head.length () - _tail.length ();

		if (_Debug) {
			_log.debug ("_model = " + _model);
			_log.debug ("_baseUri = " + _baseUri);
			_log.debug ("_head = " + _head);
			_log.debug ("_tail = " + _tail);
			_log.debug ("_digits = " + _digits);
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
	//normalizeUrl2
	//	collapse multiple slashes into single slash (except for header "http://")
	//	convert URLs that start with "http://www." to "http://"
	//	convert URLs that end with ".ext/" to ".ext"
	private static String normalizeUrl2 (String url)
	{
		String header1 = "http://www.";
		String header2 = "http://";

		if (url.toLowerCase ().startsWith (header1)) {
			url = header2 + url.substring (header1.length (), url.length ());
//			System.out.println ("normalizeUrl2: url = '" + url + "'");
		}

		int headerIndex = url.indexOf (header2);
		if (headerIndex < 0) {
			return url;
		}

		int endOfHeader = headerIndex + header2.length ();

		int slashesIndex = url.lastIndexOf ("//");

		if (slashesIndex > endOfHeader) {
//			System.out.println ("normalizeUrl2: url = '" + url + "'");
			while (slashesIndex > endOfHeader) {
				url = url.replaceAll ("//", "/");
				slashesIndex = url.lastIndexOf ("//");
			}

			url = url.replaceFirst ("/", "//");
//			System.out.println ("normalizeUrl2: url = '" + url + "'");
		}

		if (url.endsWith (".com/") ||
			url.endsWith (".htm/") ||
			url.endsWith (".html/") ||
			url.endsWith (".name/") ||
			url.endsWith (".net/") ||
			url.endsWith (".org/") ||
			url.endsWith (".today/") ||
			url.endsWith (".us/") ||
			url.endsWith (".xxx/")) {
			url = url.substring (0, url.length () - 1); //strip trailing slash
//			System.out.println ("normalizeUrl2: url = '" + url + "'");
		}

		return url;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean findInHistory (String model)
	{
//		if (_Debug)
//			_log.debug ("findInHistory: model = " + model);

		try {
			connectDatabase ();

		} catch (Exception ee) {
			if (VendoUtils.isWorkEnvironment ()) {
				_log.warn ("database not available; history is disabled");

			} else {
				_log.error ("findInHistory: connectDatabase failed");//: error from Class.forName or DriverManager.getConnection (" + dbUrl + ")");
				_log.error (ee);
			}

			return false;
		}

		String sql = "select url, insert_date, timestampdiff (day, insert_date, curdate()) as days" + //no spaces allowed in "curdate()"?
					 " from history" +
					 " where url = ?" +
					 " order by insert_date desc";

		PreparedStatement ps = null;
		try {
			ps = _dbConnection.prepareStatement (sql);

			ps.setString (1, model);
//System.out.println ("findInHistory: ps='" + ps + "'");

		} catch (Exception ee) {
			_log.error ("findInHistory: error from Connection.prepareStatement (" + sql + ")");
			_log.error (ee);
			return false;
		}

		boolean found = false;

		ResultSet rs = null;
		try {
			rs = ps.executeQuery ();

			while (rs.next ()) {
				String url = rs.getString ("url");
				Timestamp insertDate = rs.getTimestamp ("insert_date");
				int days = rs.getInt ("days");

				if (days <= 7) {
					if (!found) { //print header
						found = true;

						System.out.println ("Recent entry(s) found in history file:");
					}

					String message = url + ", " + insertDate + ", days=" + days;
					VendoUtils.printWithColor (_alertColor, message);
//TODO:
//					break;

				} else {
//System.out.println ("findInHistory: url=" + url + ", insertDate=" + insertDate + ", days=" + days);
				}
			}

		} catch (Exception ee) {
			_log.error ("writeHistory: error from PreparedStatement.executeQuery");
			_log.error (ee);
			return false;
		}

		if (found) {
			System.out.println ("");
		}

		return (_ignoreHistory ? false : found);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeHistory ()
	{
		try {
			connectDatabase ();

		} catch (Exception ee) {
			//error already printed by findInHistory
			return false;
		}

		Collections.sort (_switches, VendoUtils.caseInsensitiveStringComparator);

		String args = new String ();
		for (String str : _switches) {
			args += str + " ";
		}
		if (_startIndex >= 0) {
			args += _startIndex + " ";
		}
		if (_endIndex >= 0) {
			args += _endIndex + " ";
		}
		if (_step > 1) { //only step values > 1 need to be included
			args += _step + " ";
		}
		args = args.trim ();

		//build prepared statement
		String sql1 = "insert into history" +
					  " (insert_date, url";
		String sql2 = ") values (?, ?";

		if (args.length () > 0) {
			sql1 += ", args";
			sql2 += ", ?";
		}

		if (_startIndex >= 0) {
			sql1 += ", start_index";
			sql2 += ", ?";
		}

		if (_endIndex >= 0) {
			sql1 += ", end_index";
			sql2 += ", ?";
		}

		if (_step > 1) { //only step values > 1 need to be included
			sql1 += ", step";
			sql2 += ", ?";
		}
		sql2 += ")";

		PreparedStatement ps = null;
		try {
			ps = _dbConnection.prepareStatement (sql1 + sql2);

			int index = 1;
			Timestamp now = new Timestamp (new GregorianCalendar ().getTimeInMillis ());
			ps.setTimestamp (index++, now);
			ps.setString (index++, _model);
			if (args.length () > 0) {
				ps.setString (index++, args);
			}
			if (_startIndex >= 0) {
				ps.setInt (index++, _startIndex);
			}
			if (_endIndex >= 0) {
				ps.setInt (index++, _endIndex);
			}
			if (_step > 1) {
				ps.setInt (index++, _step);
			}
//System.out.println ("writeHistory: ps='" + ps + "'");

			ps.executeUpdate ();

		} catch (Exception ee) {
			_log.error ("writeHistory: error from Connection.prepareStatement (" + sql1 + sql2 + ") or PreparedStatement.executeUpdate");
			_log.error (ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private Connection connectDatabase () throws Exception
	{
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.cj.jdbc.Driver";
		final String dbUrl = "jdbc:mysql://localhost/gihistory";
		final String dbUser = "root";
		final String dbPass = "root";

		if (_dbConnection == null) {
			Class.forName (jdbcDriver);

			_dbConnection = DriverManager.getConnection (dbUrl, dbUser, dbPass);
		}

		return _dbConnection;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean validFileChars (String str)
	{
		if (str.contains ("*") ||
			str.contains ("?") ||
			str.contains (":") ||
			str.contains ("/") ||
			str.contains ("\\")) {
			return false;
		}

		return true;
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

			if (false) { //debug
				_log.debug ("SizeDist.add - hash dump:");
				for (Map.Entry<Integer, Integer> entry : _dist.entrySet ()) {
					Integer key = entry.getKey ();
					Integer val = entry.getValue ();
					_log.debug (key + " bytes (" + val + " instances)");
				}

//old way uses iterator (save as interesting example)
//				for (Iterator<Map.Entry<Integer, Integer>> iter = _dist.entrySet ().iterator (); iter.hasNext ();) {
//					Map.Entry<Integer, Integer> entry = iter.next ();
//					Integer key = entry.getKey ();
//					Integer val = entry.getValue ();
//					_log.debug (key + " bytes (" + val + " instances)");
//				}
			}

			return count;
		}

		private final HashMap<Integer, Integer> _dist = new HashMap<Integer, Integer> (32);
	}

	private class PerfStats
	{
		///////////////////////////////////////////////////////////////////////////
		//holds performance statistics
		public void add (PerfStatsRecord record)
		{
			_records.add (record);
		}

		///////////////////////////////////////////////////////////////////////////
		public void print ()
		{
			Collections.sort (_records, new AlphanumComparator ());

			double totalBytes = 0;
			double totalSeconds = 0;
			for (PerfStatsRecord record : _records) {
				totalBytes += record._bytesRead;
				totalSeconds += record._elapsedSeconds;

				_log.debug (record.toString2 ());
			}

			double totalBitsPerSec = 8 * totalBytes / totalSeconds;

			_log.debug (_records.size () + " items downloaded, " +
						VendoUtils.unitSuffixScale (totalBytes, 0) + ", " +
						VendoUtils.unitSuffixScale (totalBitsPerSec, 0) + "ps average");

			System.out.println (""); //print blank line on output
		}

		private final Vector<PerfStatsRecord> _records = new Vector<PerfStatsRecord> ();
	}

	private class PerfStatsRecord
	{
		///////////////////////////////////////////////////////////////////////////
		//helper for performance statistics
		public PerfStatsRecord (String filename, double bytesRead, double elapsedSeconds)
		{
			File file = new File (filename);
			_filename = file.getName ();

			_bytesRead = bytesRead;
			_elapsedSeconds = elapsedSeconds;

			_bitsPerSec = 8 * _bytesRead / _elapsedSeconds;
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
				   VendoUtils.unitSuffixScale (_bytesRead, 0) + ", " +
				   VendoUtils.unitSuffixScale (_bitsPerSec, 0) + "ps";
		}

		public final String _filename;
		public final double _bytesRead;
		public final double _bitsPerSec;
		public final double _elapsedSeconds;
	}

	private static class MyNodeVisitor implements NodeVisitor
	{
		///////////////////////////////////////////////////////////////////////////
		public MyNodeVisitor (List<Node> nodeList)
		{
			if (nodeList == null) {
				throw new NullPointerException ("arg cannot be null.");
			}

			_nodeList = nodeList;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public void head (Node node, int depth)
		{
			if (node.childNodeSize () == 0) {
				_nodeList.add (node);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public void tail (Node node, int depth)
		{
		}

		private final List<Node> _nodeList;
	}

	//private members
	private int _digits = 1; //TODO make this a param?
	private int _index = 0;
	private int _pad = 2;
	private int _startIndex = -1;
	private int _endIndex = -1;
	private int _step = -1;
	private int _blockNumber = 0;
	private boolean _extr1 = false;
	private boolean _image = false;
	private boolean _ignoreHistory = false;
	private boolean _checkHistoryOnly = false;
	private boolean _linksOnly = false;
	private boolean _imageLinksOnly = false;
	private boolean _noHistory = false;
	private String _model = null;
	private String _baseUri = null;
	private String _prefix = null;
	private String _exten = null;
	private String _head = new String ();
	private String _tail = new String ();
	private String _urlStr = null;
	private String _filename = null;
	private String _destDir = null;
	private String _tempFilename = null;
	private SizeDist _sizeDist = new SizeDist ();
	private PerfStats _perfStats = new PerfStats ();
	private Vector<String> _switches = new Vector<String> ();
	private HttpURLConnection _httpURLConnection = null;

	private Connection _dbConnection = null;

	private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
//	private static final Short fgHighlightColor = Win32.CONSOLE_BACKGROUND_COLOR_BLACK;
//	private static final Short bgHighlightColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA;

//	private static final String _historyFilename = "gi.history.txt";
	private static final String _slash = System.getProperty ("file.separator");
	private static final Integer _pid = ProcessUtils.getWin32ProcessId ();
	private static Logger _log = LogManager.getLogger (GetUri.class);

	//global members
	public static boolean _Debug = false;
	public static boolean _TestMode = false;

	public static final String _AppName = "GetUri";
	public static final String NL = System.getProperty ("line.separator");
}
