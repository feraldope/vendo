//Original inspiration from
// http://www.javapractices.com/topic/TopicAction.do?Id=82
// http://www.coderanch.com/t/377833/java/java/listen-clipboard
// http://javarevisited.blogspot.com/2013/12/inter-thread-communication-in-java-wait-notify-example.html?_sm_au_=iVV5n6q6pSsPTZ0P

package com.vendo.jHistory;

import com.vendo.albumServlet.AlbumImages;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.vendoUtils.WatchDir;
import com.vendo.win32.Win32;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class JHistory
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new UncaughtExceptionHandler () {
			@Override
			public void uncaughtException (Thread thread, Throwable ex)
			{
				_log.error ("JHistory UncaughtExceptionHandler: ", ex);
				Thread.currentThread ().interrupt ();
			}
		});
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		JHistory app = new JHistory ();

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

				} else if (arg.equalsIgnoreCase ("outFile") || arg.equalsIgnoreCase ("out")) {
					try {
						_outFileName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args and handle defaults
		if (_destDir == null) {
			_destDir = VendoUtils.getCurrentDirectory ();
		}
		_destDir = VendoUtils.appendSlash (_destDir);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/dest <dest dir>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		if (_Debug) {
			_log.debug ("JHistory.run");
		}

		readHistoryFile ();
		/*Thread watchHistoryFileThread =*/ watchHistoryFile ();

		Queue<StringBuffer> listenerQueue = new LinkedList<StringBuffer> ();
		ClipboardListener clipboardListener = new ClipboardListener (listenerQueue);
		clipboardListener.start ();

		String string = new String ();
		while (true) {
			try {
				synchronized (listenerQueue) {
					//waiting condition - wait until Queue is not empty
					while (listenerQueue.size () == 0) {
						try {
							if (_trace) {
								System.out.println ("JHistory.run: queue is empty, waiting");
							}
							listenerQueue.wait ();
						} catch (InterruptedException ex) {
							ex.printStackTrace ();
						}
					}
					StringBuffer stringBuffer = listenerQueue.poll ();
					string = stringBuffer.toString ().trim ();

					if (_trace) {
						System.out.println("JHistory.run: consuming: " + string);
					}
					listenerQueue.notify ();
				}

				UrlData urlData = parseLine (string);
				if (urlData != null && urlData.isImage ()) {
					String urlNormalizedString = urlData.getNormalizedLine ();
					writeOutFile (urlNormalizedString + NL);

					System.out.println ("");

					List<MatchData> matchList = findInHistory (urlData.getPathBase ());
					if (matchList.size () > 0) {
						System.out.println ("Duplicate entry(s) found in history file: " + matchList.size ());
						System.out.println ("---- " + urlNormalizedString);

						int maxLines = (_Debug ? 20 : 6);
						int lineCount = Math.min (matchList.size (), maxLines);
						for (int ii = 0; ii < lineCount; ii++) {
							MatchData match = matchList.get (ii);
							short color = (match.getStrength () < 1000 ? _alertColor : _warningColor);
							System.out.print ("  ");
							VendoUtils.printWithColor (color, match.getLine (), /*includeNewLine*/ false);
							System.out.println (" (" + match.getReverseIndex () + ", " + match.getStrength () + ")");
						}

					} else {
						System.out.print ("Not found: ");
						VendoUtils.printWithColor (_highlightColor, urlNormalizedString);
					}
				}

			} catch (Exception ee) {
				_log.error ("JHistory.run: exception", ee);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//watch file and call readHistoryFile() whenever file changes on disk
	private Thread watchHistoryFile ()
	{
		Thread watchingThread = null;
		try {
			Path path = FileSystems.getDefault ().getPath (_destDir, _historyFilename);
			Path dir = path.getRoot ().resolve (path.getParent ());
			String filename = path.getFileName ().toString ();

			_log.info ("JHistory.watchHistoryFile: watching history file: " + path.normalize ().toString ());

			Pattern pattern = Pattern.compile (filename, Pattern.CASE_INSENSITIVE);
			boolean recurseSubdirs = false;

			WatchDir watchDir = new WatchDir (dir, pattern, recurseSubdirs)
			{
				@Override
				protected void notify (Path dir, WatchEvent<Path> pathEvent)
				{
					if (_Debug) {
						Path file = pathEvent.context ();
						Path path = dir.resolve (file);
						_log.debug ("JHistory.watchHistoryFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ().toString ());
					}

					if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_MODIFY) ||
						pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_CREATE)) {
						readHistoryFile ();
					}
				}

				@Override
				protected void overflow (WatchEvent<?> event)
				{
					_log.error ("JHistory.watchHistoryFile.overflow: received event: " + event.kind ().name () + ", count = " + event.count ());
					_log.error ("JHistory.watchHistoryFile.overflow: ", new Exception ("WatchDir overflow"));
				}
			};
			watchingThread = new Thread (watchDir);
			watchingThread.start ();

//			thread.join (); //wait for thread to complete

		} catch (Exception ee) {
			_log.error ("JHistory.watchHistoryFile: exception watching history file", ee);
		}

		return watchingThread;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readHistoryFile ()
	{
		Instant startInstant = Instant.now ();

		Path path = FileSystems.getDefault ().getPath (_destDir, _historyFilename);
		FileTime historyFileModified = FileTime.from (Instant.MAX);
		try {
			historyFileModified = Files.getLastModifiedTime (path);
		} catch (Exception ee) {
			_log.error ("JHistory.watchHistoryFile: exception calling getLastModifiedTime", ee);
		}

		/*
		try (Stream<String> stream = Files.lines (path)) {
    stream
        .filter(s -> s.endswith("/"))
        .sorted()
        .map(String::toUpperCase)
        .forEach(System.out::println);
}

    private static List readByJava8(String fileName) throws IOException {
        List<String> result;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            result = lines.collect(Collectors.toList());
        }
        return result;

    }
		*/



		boolean status = true;
		if (_historyFileModified.compareTo (historyFileModified) < 0) {



			List<UrlData> contents;
			try (Stream<String> stream = Files.lines (path)) {
				contents = stream.parallel ()
								 .map(v -> parseLine (v))
								 .filter(v -> v != null)
								 .collect (Collectors.toList ());

/*			List<UrlData> contents = new ArrayList<> ();
			try (BufferedReader reader = new BufferedReader (new FileReader (_destDir + _historyFilename))) {
				String line;
				while ((line = reader.readLine ()) != null) {
					UrlData urlData = parseLine (line);
					if (urlData != null) {
						contents.add (urlData);
					}
				}
*/
				synchronized (_historyFileContents) {
					_historyFileContents = contents;
					_historyFileModified = historyFileModified;
				}
			} catch (IOException ee) {
				_log.error ("JHistory.readHistoryFile: error reading history file \"" + _historyFilename + "\"", ee);
				status = false;
			}
		}
//		if (status) {
//			_historyFileModified = historyFileModified;
//		}

		System.out.println ("");
		printTiming (startInstant, "JHistory.readHistoryFile");

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<MatchData> findInHistory (String basePattern)
	{
		String pattern = VendoUtils.getUrlFileComponent (basePattern).toLowerCase ();
		if (_Debug) {
			_log.debug ("JHistory.findInHistory: basePattern = " + basePattern);
			_log.debug ("JHistory.findInHistory: pattern = " + pattern);
		}

		Instant startInstant = Instant.now ();

		final int maxThreads = VendoUtils.getLogicalProcessors () - 2;
		final int minPerThread = 100;
		final int chunkSize;
		List<List<UrlData>> chunks;
		final List<MatchData> matchList = new ArrayList<MatchData>();
		synchronized (_historyFileContents) {
//			/*final int*/ chunkSize = _historyFileContents.size () / 12;
			/*final int */
			chunkSize = AlbumImages.calculateChunks (maxThreads, minPerThread, _historyFileContents.size ()).getFirst ();
			/*List<List<UrlData>>*/
			chunks = ListUtils.partition (_historyFileContents, chunkSize);
//		}
			final int numChunks = chunks.size ();

//		final int maxThreads = VendoUtils.getLogicalProcessors ();
//		final int minPerThread = 100;
//		final int chunkSize = AlbumImages.calculateChunk (maxThreads, minPerThread, allCombos.size ()).getFirst ();

//		final int numChunks = chunks.size ();
			final CountDownLatch endGate = new CountDownLatch(numChunks);

			if (_Debug) {
				_log.debug("JHistory.findInHistory: _historyFileContents.size() = " + _historyFileContents.size());
				_log.debug("JHistory.findInHistory: numChunks = " + numChunks);
				_log.debug("JHistory.findInHistory: chunkSize = " + chunkSize);
			}

			for (final List<UrlData> chunk : chunks) {
				new Thread(() -> {
					final int numLines = chunk.size();

					//check for strong matches
					for (int ii = 0; ii < numLines; ii++) {
						UrlData urlData = chunk.get(ii);
						String baseLine = urlData.getPathBase();
						if (strongMatch(baseLine.toLowerCase(), pattern)) {
							synchronized (matchList) {
								matchList.add(new MatchData(urlData.getLine(), true, ii - numLines, 0));
							}
						}
					}

//TODO - this might be confusing: stronger weak matches are > 1000, but a strong match is 0.

					if (matchList.size() == 0) {
						//then check for stronger weak matches
						int minStrength = 1000;
						for (int ii = 0; ii < numLines; ii++) {
							UrlData urlData = chunk.get(ii);
							String baseLine = urlData.getPathBase();
							int strength = weakMatch(baseLine.toLowerCase(), pattern);
							if (strength > minStrength) {
								synchronized (matchList) {
									matchList.add(new MatchData(urlData.getLine(), false, ii - numLines, strength));
								}
							}
						}
					}

					if (matchList.size() == 0) {
						//then check for weaker weak matches
						int minStrength = 0;
						for (int ii = 0; ii < numLines; ii++) {
							UrlData urlData = chunk.get(ii);
							String baseLine = urlData.getPathBase();
							int strength = weakMatch(baseLine.toLowerCase(), pattern);
							if (strength > minStrength) {
								synchronized (matchList) {
									matchList.add(new MatchData(urlData.getLine(), false, ii - numLines, strength));
								}
							}
						}
					}
					endGate.countDown();
				}).start();
			}

			try {
				endGate.await();
			} catch (Exception ee) {
				_log.error("JHistory.findInHistory: endGate:", ee);
			}
		}
		printTiming (startInstant, "JHistory.findInHistory");

		return matchList;
	}

	///////////////////////////////////////////////////////////////////////////
	private UrlData parseLine (String line)
	{
		try {
			String urlStr = line;
			if (line.startsWith ("gu ")) {
				urlStr = line.split (" ")[1].trim (); //extract URL from line
			}

			URL url = new URL (urlStr);
			String path = url.getPath ();

			int lastSlash = path.lastIndexOf ('/');

			String pathBase = path.substring (0, lastSlash + 1)
								  .replaceAll("\\/\\/", "/")
								  .replaceAll("\\/thumbs\\/", "/")
								  .replaceAll("\\/tn\\/", "/")
								  .replaceAll("\\/tn_/", "/")
								  .replaceAll("\\/tn-/", "/");

			String fileName = path.substring (lastSlash + 1)
								  .replaceAll("\\.thumb\\.jpg", ".jpg");

			return new UrlData (line, url, pathBase, fileName);

		} catch (MalformedURLException ee) {
			//bad URL, return null below

		} catch (Exception ee) {
			_log.error ("JHistory.parseLine: error parsing line \"" + line + "\"", ee);
		}

		return null;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean strongMatch (String line1, String line2)
	{
		boolean matches = line2.length () >= 2 && line1.contains (line2);

		if (_Debug && matches) {
			_log.debug ("JHistory.strongMatch: line1: " + line1);
			_log.debug ("JHistory.strongMatch: line2: " + line2);
		}

		return matches;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean accept (String string)
	{
		if (string.length () < 2) {
			return false;
		}

		if (string.equalsIgnoreCase ("tn")) {
			return false;
		}

		if (string.endsWith (".com")) {
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private int weakMatch (String line1, String line2)
	{
		String array1[] = StringUtils.split (line1, '/'); //URL from history file
		String array2[] = StringUtils.split (line2, '/'); //pattern to match

		Set<String> set1 = Arrays.stream (array1).filter (s -> accept (s)).collect (Collectors.toSet ());
		Set<String> set2 = Arrays.stream (array2).filter (s -> accept (s)).collect (Collectors.toSet ());

		//make a set that has all of the common strings that are in both source Sets
		Set<String> set3 = set1.stream ().filter (s -> s.length () > 0 && set2.contains (s)).collect (Collectors.toSet ());

		List<String> matchList = new ArrayList<String> ();
		int nonMatchingNumberStrings = 0;
		int matchingNumberStrings = 0;
		int matchingNonNumberStrings = 0;
		for (String string : set3) {
//TODO: add test for hex numbers?
			if (VendoUtils.isDigits (string)) {
				if (string.length () >= 3) { //all-digit strings must have minimum length to count as match
					matchList.add (string);
					matchingNumberStrings++;
				} else {
					nonMatchingNumberStrings++;
				}

			} else {
				matchList.add (string);
				matchingNonNumberStrings++;
			}
		}

		int set1size = set1.size ();
		int set2size = set2.size ();
		int set3size = set3.size ();
		int matchListSize = matchList.size ();

		int strength = 0;

		//stronger matches

		if (set1size >= 2 && set1size == set2size && set1size == set3size) {
			strength |= 1000 + 1;
		}

		if (set1size >= 1 && set1size == set2size && set2size == matchListSize) {
			strength |= 1000 + 2;
		}

		if (set3size >= 3 && set3size == matchListSize) {
			strength |= 1000 + 4;
		}

		//weaker matches

		if (set2size >= 3 && set2size - matchListSize <= 1) {
			strength |= 8;
		}

		if (matchingNumberStrings + matchingNonNumberStrings >= 3) {
			strength |= 16;
		}

		if (matchingNumberStrings >= 2 && nonMatchingNumberStrings == 0) {
			strength |= 32;
		}

		if (matchingNumberStrings >= 1 && matchingNonNumberStrings >= 1 && nonMatchingNumberStrings == 0) {
			strength |= 64;
		}

		if (strength > 0 && _Debug) {
//		if (line1.contains ("/17151/")) {
			_log.debug ("JHistory.weakMatch: line1: " + line1);
			_log.debug ("JHistory.weakMatch: line2: " + line2);
			_log.debug ("JHistory.weakMatch: set1:  (" + set1size + ") " + StringUtils.join (set1.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: set2:  (" + set2size + ") " + StringUtils.join (set2.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: set3:  (" + set3size + ") " + StringUtils.join (set3.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: match: (" + matchListSize + ") " + StringUtils.join (matchList.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: strength: " + strength);
		}

		return strength;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeOutFile (String urlString)
	{
		if (_outFileName == null || _outFileSet.contains (urlString)) {
	 		return false;
		}

		try (FileOutputStream outputStream = new FileOutputStream (new File(_destDir + _outFileName), /*append*/ true)) {
			outputStream.write (urlString.getBytes ());
			outputStream.flush ();

			_outFileSet.add (urlString);

		} catch (IOException ee) {
			_log.error ("JHistory.writeOutFile: error writing output file \"" + _destDir + _outFileName + "\"");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void printTiming (Instant startInstant, String message)
	{
		if (!_printTiming) {
			return;
		}

		long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();
		_log.debug (message + ": elapsed: " + LocalTime.ofNanoOfDay (elapsedNanos));
	}

	///////////////////////////////////////////////////////////////////////////
	private class UrlData
	{
		///////////////////////////////////////////////////////////////////////////
		UrlData (String line, URL url, String pathBase, String fileName)
		{
			_line = line;
			_url = url;
			_pathBase = pathBase;
			_fileName = fileName;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getLine ()
 		{
			return _line;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getNormalizedLine ()
 		{
			return _line.replaceFirst ("://www\\.", "://");
		}

		///////////////////////////////////////////////////////////////////////////
		public URL getUrl ()
		{
			return _url;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getPathBase ()
 		{
			return _pathBase;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getFileName ()
 		{
			return _fileName;
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean isImage ()
		{
			String fileNameLower = getFileName ().toLowerCase ();

			return fileNameLower.endsWith ("jpg") || fileNameLower.endsWith ("jpeg");
		}

		//members
		private final String _line;
		private final URL _url;
		private final String _pathBase; //URL path with the everything before the last slash
		private final String _fileName; //URL path with the everything after the last slash
	}

	///////////////////////////////////////////////////////////////////////////
	private class MatchData
	{
		///////////////////////////////////////////////////////////////////////////
		MatchData (String line, boolean strongMatch, int reverseIndex, int strength)
		{
			_line = line;
			_strongMatch = strongMatch;
			_reverseIndex = reverseIndex;
			_strength = strength;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getLine ()
 		{
			return _line;
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean getStrongMatch ()
		{
			return _strongMatch;
		}

		///////////////////////////////////////////////////////////////////////////
		public int getReverseIndex ()
		{
			return _reverseIndex;
		}

		///////////////////////////////////////////////////////////////////////////
		public int getStrength ()
		{
			return _strength;
		}

		//members
		private final String _line;
		private final boolean _strongMatch;
		private final int _reverseIndex;
		private final int _strength;
	}


	//private members
	private static boolean _trace = false;
	private String _destDir = null;
	private String _outFileName = null;
	private List<UrlData> _historyFileContents = new ArrayList<UrlData> ();
	private FileTime _historyFileModified = FileTime.from (Instant.MIN);
	private Set<String> _outFileSet = new HashSet<String> ();

	private boolean _printTiming = true; //for performance timing
//	private Instant _startInstant = Instant.EPOCH; //for performance timing

	private static final String _historyFilename = "gu.history.txt";
	private static Logger _log = LogManager.getLogger ();

	private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
	private static final short _warningColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW;
	private static final short _highlightColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA;

	//global members
	public static boolean _Debug = false;
	public static boolean _TestMode = false;

	public static final String _AppName = "JHistory";
	public static final String NL = System.getProperty ("line.separator");
}
