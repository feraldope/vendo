//Original inspiration from
// http://www.javapractices.com/topic/TopicAction.do?Id=82
// http://www.coderanch.com/t/377833/java/java/listen-clipboard
// http://javarevisited.blogspot.com/2013/12/inter-thread-communication-in-java-wait-notify-example.html?_sm_au_=iVV5n6q6pSsPTZ0P

package com.vendo.jHistory;

import com.vendo.albumServlet.AlbumImages;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.vendoUtils.WatchDir;
import com.vendo.win32.Win32;
import j2html.tags.ContainerTag;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static j2html.TagCreator.*;


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
	public static void main (String[] args)
	{
		JHistory app = new JHistory ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String[] args)
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

		_urlPathFragmentValue = System.getenv(_urlPathFragmentName);
		if (_urlPathFragmentValue == null || _urlPathFragmentValue.isEmpty()) {
			displayUsage ("Must specify environment variable '" + _urlPathFragmentName + "'", true);
		}

		_urlHostFragment1Value = System.getenv(_urlHostFragment1Name);
		if (_urlHostFragment1Value == null || _urlHostFragment1Value.isEmpty()) {
			displayUsage ("Must specify environment variable '" + _urlHostFragment1Name + "'", true);
		}

		_urlHostFragment2Value = System.getenv(_urlHostFragment2Name);
		if (_urlHostFragment2Value == null || _urlHostFragment2Value.isEmpty()) {
			displayUsage ("Must specify environment variable '" + _urlHostFragment2Name + "'", true);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = "";
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

		final Queue<StringBuffer> listenerQueue = new LinkedList<> ();
		ClipboardListener clipboardListener = new ClipboardListener (listenerQueue);
		clipboardListener.start ();

		String string;
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
				if (urlData != null) {
					if (urlData.isImage ()) {
						handleImageUrl(urlData);
					} else if (urlData.isUrl ()) {
						if (urlData.isInterestingUrl ()) {
							handleHtmlUrl(urlData);
						} else {
							System.out.println(NL + "JHistory.run: ignoring URL; try going to main page");
						}
					}
				}

			} catch (Exception ee) {
				_log.error ("JHistory.run: exception", ee);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//returns true if url found, false otherwise
	private boolean handleImageUrl (UrlData urlData) {
		boolean status = false;

		String urlNormalizedString = urlData.getNormalizedLine ();
		writeHistoryFile (urlNormalizedString + NL);

		System.out.println ("");

		List<MatchData> matchList = findInHistory (urlData.getPathBase (), urlNormalizedString);
		if (matchList.size () > 0) {
			System.out.println ("Duplicate entry(s) found in history file: " + matchList.size ());
			System.out.println ("---- " + urlNormalizedString);

			int maxLines = (_Debug ? 20 : 6);
			int lineCount = Math.min (matchList.size (), maxLines);
			for (int ii = 0; ii < lineCount; ii++) {
				MatchData match = matchList.get (ii);
				short color = (match.getStrength () < 1000 ? _alertColor : _warningColor);
				System.out.print ("  ");
				VendoUtils.printWithColor (color, match.getLine (), false);
				System.out.println (" (" + match.getReverseIndex () + ", " + match.getStrength () + ")");
			}
			status = true;

		} else {
			System.out.print ("Not found: ");
			VendoUtils.printWithColor (_highlightColor, urlNormalizedString);
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	//generates HTML; returns true if url found, false otherwise
	private boolean handleHtmlUrl (UrlData urlData1) {
		boolean status = false;

  		String urlNormalizedString = urlData1.getNormalizedLine ();
		writeHistoryFile (urlNormalizedString + NL);

		System.out.println ("");
		VendoUtils.printWithColor (_highlightColor, "Processing URL: " + urlNormalizedString);

		Collection<UrlLinkData> urlLinkDataAll = getUrlLinkData (urlData1, true);

		if (urlLinkDataAll == null || urlLinkDataAll.isEmpty()) {
			return status; //nothing to do
		}

		List<UrlLinkData> urlLinkDataNew = new ArrayList<> ();
		for (UrlLinkData urlLink : urlLinkDataAll) {
			String firstImageUrl = urlLink.getImageUrlStr (0);
			UrlData urlData2 = parseLine(firstImageUrl);
			boolean found = handleImageUrl(urlData2);
			if (!found) {
				urlLinkDataNew.add(urlLink);
			}
		}

//		final String htmlEscapedPlusSign = VendoUtils.escapeHtmlChars ("+");
		final String htmlEscapedModifier = VendoUtils.escapeHtmlChars ("[hmr+]");

		String name = "unknown";
		if (!urlLinkDataAll.isEmpty()) {
			name = urlLinkDataAll.iterator().next().getMainTitle().replaceAll("pictures.*", "").replaceAll("\\s", "");// + htmlEscapedPlusSign; //TODO hack
		}

		String albumServetLink = "http://localhost/AlbumServlet/AlbumServlet?mode=doSampler&filter1=" + name + "&filter2=" + name + htmlEscapedModifier +
								"&screenWidth=3840&screenHeight=2160&windowWidth=1863&windowHeight=2027" +
								"&panels=4000&columns=8&collapseGroups=on&limitedCompare=true&looseCompare=true&ignoreBytes=true&debug=on#topAnchor";
		String title = _htmlFilename + " (" + urlLinkDataNew.size () + " new of " + urlLinkDataAll.size () + " total)";

		String html = DOCTYPE + NL;
		if (!urlLinkDataNew.isEmpty ()) {
			AtomicInteger linkNumber = new AtomicInteger(1);
			final int totalLinkCount = urlLinkDataNew.size();
			html += html(
					head(
						title(title)
					),
					body(
						div(attrs("#div0"),
							h1(title)
						),
						div(attrs("#div1"),
							text("Existing albums: "),
							a().withText(albumServetLink)
								.withHref(albumServetLink).withTarget("_blank")
						),
						div(attrs("#div2"),
							urlLinkDataNew.stream().map(u1 ->
								div(attrs("#div3"),
									div(attrs("#div4"),
										h2("(" + linkNumber.getAndIncrement() + "/" + totalLinkCount + ") " + u1.getChildTitle()),
										a().withText(u1.getAlbumUrlStr())
											.withHref(u1.getAlbumUrlStr())
											.withTarget("_blank")
									),
									div(attrs("#div5"),
										u1.getImageUrlList().stream().map(u2 ->
											a(img().withSrc(u2)
													.attr("width=" + getImageDimensionFromName (u2, Dimension.Width, true))
													.attr("height=" + getImageDimensionFromName (u2, Dimension.Height, true))
											)
											.withHref(u2.replaceAll("p/\\d+x\\d+_", "m")) //convert thumbnail to image
											.withTarget("_blank")
										).toArray(ContainerTag[]::new)
									)
								)
							).toArray(ContainerTag[]::new)
						)
					)
			).renderFormatted();

		} else { //no new images found
			html += html(
					head(
						title(title)
					),
					body(
						div(attrs("#div0"),
								h1(title)
						),
						div(attrs("#div1"),
							text("No new links found for: "),
							a().withText(urlNormalizedString).withHref(urlNormalizedString).withTarget("_blank")
						),
						div(attrs("#div2"),
							br()
						),
						div(attrs("#div3"),
							text("Existing albums: "),
							a().withText(albumServetLink).withHref(albumServetLink).withTarget("_blank")
						)
					)
			).renderFormatted();
		}

		Path outputFilePath = FileSystems.getDefault().getPath(_destDir, _htmlFilename);
		if (writeOutputFile (html, outputFilePath)) {
			System.out.println ("");
			_log.debug("JHistory.handleHtmlUrl: wrote " + urlLinkDataNew.size () + " links to HTML output file: " + outputFilePath.toString());
			status = true;
		}

		String allNewLinks = urlLinkDataNew.stream ().map (u -> u.getImageUrlStr(0)).sorted ().collect (Collectors.joining (NL)) + NL;
		outputFilePath = FileSystems.getDefault().getPath(_destDir, _allNewLinksFilename);
		if (writeOutputFile (allNewLinks, outputFilePath)) {
			System.out.println ("");
			_log.debug("JHistory.handleHtmlUrl: wrote " + urlLinkDataNew.size () + " links to link output file: " + outputFilePath.toString());
			status = true;
		}

		VendoUtils.printWithColor (_highlightColor, "Total links: " + urlLinkDataAll.size ());
		VendoUtils.printWithColor (_alertColor,     "Old links:   " + (urlLinkDataAll.size () - urlLinkDataNew.size ()));
		VendoUtils.printWithColor (_highlightColor, "New links:   " + (urlLinkDataNew.isEmpty () ? "---" : urlLinkDataNew.size () + " *****"));

		return status;
	}
	///////////////////////////////////////////////////////////////////////////
	private Collection<UrlLinkData> getUrlLinkData (UrlData urlData1, boolean verbose) {
		Collection<UrlLinkData> urlLinkData = new ArrayList<> ();

//		String urlNormalizedString = urlData1.getNormalizedLine ();

		Document document1 = null;
		try {
			document1 = Jsoup.parse (urlData1.getUrl (), _timeoutMillis);
		} catch (Exception ee) {
			System.err.println (ee);
			ee.printStackTrace (System.err);
		}

		if (document1 == null) {
			_log.error ("JHistory.getUrlLinkData: error opening main URL: " + urlData1.getUrl () + NL);

		} else {
			String mainTitle = document1.select("title").text();
//old way
//			Elements linkElements1 = document1.select("title");
//			String mainTitle = "[main title not found]";
//			for (Element element : linkElements1) {
//				Elements elements = element.getElementsByTag ("title");
//				for (Element element2 : elements) {
//					Node node = element2.childNode(0);
//					mainTitle = node.toString();
//					break;
//				}
//			}

			List<String> mainLinksList = document1.select("a[href]").stream ()
												.map (e -> e.attr("abs:href"))
												.filter(s -> s.contains ("/galleries/"))
												.sorted(VendoUtils.caseInsensitiveStringComparator)
												.distinct()
												.collect(Collectors.toList());

			//skip if already checked during this session
//			Integer linkCount = _recentUrlResultsCache.get(urlNormalizedString);
//			if (linkCount != null && linkCount.equals(mainLinksList.size())) {
//				VendoUtils.printWithColor (_highlightColor, "****** Skipping URL as it was already processed during this session (" + mainLinksList.size () + " links)");
//				System.out.println ("");
//				return null;
// 			} else {
//				_recentUrlResultsCache.put(urlNormalizedString, mainLinksList.size());
//			}

			if (verbose) {
				VendoUtils.printWithColor (_highlightColor, "Processing " + mainLinksList.size () + " links");
			}

			for (String link : mainLinksList) {
				System.out.print (".");
				URL url = null;
				Document document2 = null;

				try {
					url = new URL (link);
					document2 = Jsoup.parse (url, _timeoutMillis);
				} catch (Exception ee) {
					System.err.println (ee);
					ee.printStackTrace (System.err);
				}

				if (document2 == null) {
					_log.error ("JHistory.getUrlLinkData: error opening child URL: " + url.toString () + NL);

				} else {
//					final String tagForChildTitle = "title"; //works OK
					final String tagForChildTitle = "h1"; //works better
					String childTitle = document2.select(tagForChildTitle).text();

					List<String> imageLinksList = document2.select ("[src]").stream ()
															.filter(e -> e.tagName ().equals ("img"))
															.map (e -> e.attr("abs:src"))
															.filter (s -> !s.contains ("/preview"))
															.sorted(ignoreImageSizeComparator)
															.distinct()
															.collect(Collectors.toList());
					urlLinkData.add (new UrlLinkData (url, mainTitle, childTitle, link, imageLinksList));
				}
			}

			if (!mainLinksList.isEmpty ()) {
				System.out.println(".");
			}
		}

		return urlLinkData;
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

		boolean status = true;
		if (_historyFileModified.compareTo (historyFileModified) < 0) {
			List<UrlData> contents;
			try (Stream<String> stream = Files.lines (path)) {
				contents = stream.parallel ()
								 .map(this::parseLine)
								 .filter(Objects::nonNull)
								 .collect (Collectors.toList ());

				synchronized (_historyFileContentsLock) {
					_historyFileContents = contents;
					_historyFileModified = historyFileModified;
				}
			} catch (IOException ee) {
				_log.error ("JHistory.readHistoryFile: error reading history file \"" + _historyFilename + "\"", ee);
				status = false;
			}
		}

//		System.out.println ("");
		printTiming (startInstant, NL + "JHistory.readHistoryFile");

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<MatchData> findInHistory (String basePattern, String sourceUrlIn)
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
		AtomicBoolean foundStrongMatch = new AtomicBoolean ();
		final List<MatchData> matchList = new ArrayList<MatchData> ();
		synchronized (_historyFileContentsLock) {
			chunkSize = AlbumImages.calculateChunks (maxThreads, minPerThread, _historyFileContents.size ()).getFirst ();
			chunks = ListUtils.partition (_historyFileContents, chunkSize);
			final int numChunks = chunks.size ();
			final CountDownLatch endGate = new CountDownLatch (numChunks);

			if (_Debug) {
				_log.debug("JHistory.findInHistory: _historyFileContents.size() = " + _historyFileContents.size());
				_log.debug("JHistory.findInHistory: numChunks = " + numChunks);
				_log.debug("JHistory.findInHistory: chunkSize = " + chunkSize);
			}

			for (final List<UrlData> chunk : chunks) {
				new Thread(() -> {
					final int numLines = chunk.size();
					final String sourceUrl = sourceUrlIn;

					//check for strong matches
					for (int ii = 0; ii < numLines; ii++) {
						UrlData urlData = chunk.get(ii);
						String baseLine = urlData.getPathBase();
						if (strongMatch(baseLine.toLowerCase(), pattern, sourceUrl, urlData)) {
							synchronized (matchList) {
								matchList.add(new MatchData(urlData.getLine(), true, ii - numLines, 0));
								foundStrongMatch.getAndSet (true);
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
							int strength = weakMatch(baseLine.toLowerCase(), pattern, sourceUrl, urlData);
							if (strength > minStrength) {
								synchronized (matchList) {
									matchList.add(new MatchData(urlData.getLine(), false, ii - numLines, strength));
									foundStrongMatch.getAndSet (true);
								}
							}
						}
					}

					if (matchList.size() == 0 && !foundStrongMatch.get ()) {
						//then check for weaker weak matches
						int minStrength = 0;
						for (int ii = 0; ii < numLines; ii++) {
							UrlData urlData = chunk.get(ii);
							String baseLine = urlData.getPathBase();
							int strength = weakMatch(baseLine.toLowerCase(), pattern, sourceUrl, urlData);
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

		// if we found any strong matches, remove all weak matches that might have been added
		if (foundStrongMatch.get ()) {
			matchList.removeIf (MatchData::isWeakMatch);
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
								  .replaceAll("/p/", "/")
								  .replaceAll("//", "/")
								  .replaceAll("/thumbs/", "/")
								  .replaceAll("/tn/", "/")
								  .replaceAll("/tn_/", "/")
								  .replaceAll("/tn-/", "/");

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
	private boolean isSpecialMatch (String sourceUrl, UrlData urlData)
	{
		return (sourceUrl.contains(_urlHostFragment2Value) && urlData.getLine().contains(_urlHostFragment1Value))
			|| (sourceUrl.contains(_urlHostFragment1Value) && urlData.getLine().contains(_urlHostFragment2Value));
	}

	//debugging
//	String debugFragment = "/fool/";

	///////////////////////////////////////////////////////////////////////////
	private boolean strongMatch (String historyFragment, String pattern, String sourceUrl, UrlData urlData)
	{
		//debugging
//		if (historyFragment.toLowerCase().contains(debugFragment) && pattern.toLowerCase().contains(debugFragment)) {
//			System.out.println("*************");
//		}

		boolean matches = pattern.length () >= 2 && historyFragment.contains (pattern);

		//this block allows redownload of what might be considered close matches
		if (matches && isSpecialMatch(sourceUrl, urlData)) {
			matches = false; //force not a match
		}

		if (_Debug && matches) {
			_log.debug ("JHistory.strongMatch: historyFragment: " + historyFragment);
			_log.debug ("JHistory.strongMatch: pattern: " + pattern);
		}

		return matches;
	}

	///////////////////////////////////////////////////////////////////////////
	private int weakMatch (String historyFragment, String pattern, String sourceUrl, UrlData urlData)
	{
		String[] array1 = StringUtils.split (historyFragment, '/'); //URL from history file
		String[] array2 = StringUtils.split (pattern, '/'); //pattern to match

		Set<String> set1 = Arrays.stream (array1).filter (this::accept).collect (Collectors.toSet ());
		Set<String> set2 = Arrays.stream (array2).filter (this::accept).collect (Collectors.toSet ());

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

		//debugging
//		if (sourceUrl.toLowerCase().contains(debugFragment) && historyFragment.toLowerCase().contains(debugFragment) && pattern.toLowerCase().contains(debugFragment)) {
//			System.out.println("*************");
//		}

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
			if (!isSpecialMatch(sourceUrl, urlData)) {
				strength |= 32;
			} // else ignore
		}

		if (matchingNumberStrings >= 1 && matchingNonNumberStrings >= 1 && nonMatchingNumberStrings == 0) {
			if (!isSpecialMatch(sourceUrl, urlData)) {
				strength |= 64;
			} else {
				strength = 0; //force not a match
			}
		}

		if (strength > 0 && _Debug) {
//		if (historyFragment.contains (debugFragment)) {
			_log.debug ("JHistory.weakMatch: historyFragment: " + historyFragment);
			_log.debug ("JHistory.weakMatch: pattern: " + pattern);
			_log.debug ("JHistory.weakMatch: set1:  (" + set1size + ") " + StringUtils.join (set1.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: set2:  (" + set2size + ") " + StringUtils.join (set2.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: set3:  (" + set3size + ") " + StringUtils.join (set3.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: match: (" + matchListSize + ") " + StringUtils.join (matchList.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: strength: " + strength);
		}

		return strength;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean accept (String string)
	{
		if (string.length () < 2) {
			return false;

		} else if (string.equalsIgnoreCase ("tn")) {
			return false;

		} else if (string.endsWith (".com")) {
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeHistoryFile (String urlString)
	{
		if (_outFileName == null || _outFileSet.contains (urlString)) {
	 		return false;
		}

		try (FileOutputStream outputStream = new FileOutputStream (new File(_destDir + _outFileName), true)) {
			outputStream.write (urlString.getBytes ());
			outputStream.flush ();

			_outFileSet.add (urlString);

		} catch (IOException ee) {
			_log.error ("JHistory.writeHistoryFile: error writing output file \"" + _destDir + _outFileName + "\"");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeOutputFile (String string, Path outputFilePath)
	{
		boolean status = true;

		try (FileOutputStream outputStream = new FileOutputStream (outputFilePath.toFile ());
			 PrintWriter printWriter = new PrintWriter (outputStream)) {
			printWriter.write (string);

		} catch (IOException ee) {
			_log.error ("JHistory.writeOutputFile: error writing HTML output file: " + outputFilePath.toString () + NL);
			_log.error (ee); //print exception, but no stack trace
			status = false;
		}

		return status;
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
	//neuters pixel values in image name for comparison
	public static final Comparator<String> ignoreImageSizeComparator = new Comparator<String> ()
	{
		@Override
		public int compare (String s1, String s2)
		{
			String s1a = s1.replaceAll ("\\d+x\\d+", "xxxNeuteredXxx");
			String s2a = s2.replaceAll ("\\d+x\\d+", "xxxNeuteredXxx");
			return s1a.compareToIgnoreCase (s2a);
		}
	};

	public enum Dimension { Width, Height }

	///////////////////////////////////////////////////////////////////////////
	//first determine image dimensions from name of form" 0752x1004_01.jpg"
	//then optionally scale to preferred max range
	private int getImageDimensionFromName (String imageName, Dimension dimension, boolean doScale)
	{
		int width = 240;
		int height = 360;

		final Pattern pattern = Pattern.compile ("(\\d+)x(\\d+)");
		Matcher matcher = pattern.matcher (imageName);
		if (matcher.find ()) {
			width = Integer.parseInt(matcher.group (1));
			height = Integer.parseInt(matcher.group (2));
		}

		if (doScale) {
			//scale images to preferred max
			double scaleForWidth = (double) _imageDimensionMax / (double) width;
			double scaleForHeight = (double) _imageDimensionMax / (double) height;
			double scaleMin = Math.min (scaleForWidth, scaleForHeight);

			double scale = scaleMin * 100.;
			double scaledWidth = width * scale;
			double scaledHeight = height * scale;

			width = (int) Math.round (scaledWidth / 100.);
			height = (int) Math.round (scaledHeight / 100);
		}

		return dimension == Dimension.Width ? width : height;
	}

	///////////////////////////////////////////////////////////////////////////
	private /*static*/ class UrlLinkData
	{
		///////////////////////////////////////////////////////////////////////////
		UrlLinkData (URL baseUrl, String mainTitle, String childTitle, String albumUrlStr, List<String> imageUrlStrList)
		{
			_baseUrl = baseUrl;
			_maintitle = mainTitle;
			_childTitle = childTitle;
			_albumUrlStr = albumUrlStr;
			_imageUrlStrList = imageUrlStrList;
		}

		///////////////////////////////////////////////////////////////////////////
		public URL getBaseUrl ()
		{
			return _baseUrl;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getMainTitle ()
		{
			return _maintitle;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getChildTitle ()
		{
			return _childTitle;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getAlbumUrlStr ()
		{
			return _albumUrlStr;
		}

		///////////////////////////////////////////////////////////////////////////
		public List<String> getImageUrlList ()
		{
			return _imageUrlStrList;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getImageUrlStr (int index)
		{
			String imageUrlStr = "[out of bounds]";
			try {
				imageUrlStr = _imageUrlStrList.get (index);
			} catch (Exception ignored) {
			}

			return imageUrlStr;
		}

/* TODO - this does not work at all - it only has the thumb's dimensions, not the actual image's
		///////////////////////////////////////////////////////////////////////////
		public String getAverageImageDimensionsStr ()
		{
//TODO - this does not account for mixes of portrait and landscape images
			double averageWidth = _imageUrlStrList.stream ()
					.map(u -> getImageDimensionFromName (u, Dimension.Width, false))
					.mapToDouble (i -> i)
					.average ()
					.getAsDouble ();

			double averageHeigth = _imageUrlStrList.stream ()
					.map(u -> getImageDimensionFromName (u, Dimension.Height, false))
					.mapToDouble (i -> i)
					.average ()
					.getAsDouble ();

			return Math.round(averageWidth) + "x" + Math.round(averageHeigth);
		}
*/
		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString ()
		{
			StringBuilder sb = new StringBuilder();
			sb.append (getMainTitle ()).append (", ");
			sb.append (getChildTitle ()).append (", ");
			sb.append (getBaseUrl ()).append (", ");
			sb.append (getAlbumUrlStr ()).append (", ");
			sb.append (getImageUrlStr (0)).append (", ");

			return sb.toString ();
		}

		//members
		private final URL _baseUrl;
		private final String _maintitle;
		private final String _childTitle;
		private final String _albumUrlStr;
		private final List<String> _imageUrlStrList;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class UrlData
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

		///////////////////////////////////////////////////////////////////////////
		public boolean isUrl ()
		{
			return getUrl ().getProtocol().toLowerCase ().startsWith ("http");
		}

		///////////////////////////////////////////////////////////////////////////
		public boolean isInterestingUrl ()
		{
			return getPathBase ().toLowerCase ().contains (_urlPathFragmentValue);
		}

		//members
		private final String _line;
		private final URL _url;
		private final String _pathBase; //URL path with the everything before the last slash
		private final String _fileName; //URL path with the everything after the last slash
	}

	///////////////////////////////////////////////////////////////////////////
	private static class MatchData
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
		public boolean isWeakMatch ()
		{
			return !_strongMatch;
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
		private final int _reverseIndex; //position in history file (line number), counting backwards from EOF
		private final int _strength;
	}


	//private members
	private static boolean _trace = false;
	private String _destDir = null;
	private String _outFileName = null;
	private FileTime _historyFileModified = FileTime.from (Instant.MIN);
	private List<UrlData> _historyFileContents = new ArrayList<UrlData> ();
	private final Object _historyFileContentsLock = new Object ();
	private final Set<String> _outFileSet = new HashSet<String> ();
	private final int _imageDimensionMax = 300;
	private final int _timeoutMillis = 10000;

	//must be specified in environment
	private static String _urlPathFragmentValue;
	private static String _urlPathFragmentName = "URL_PATH_FRAGMENT";
	private static String _urlHostFragment1Value;
	private static String _urlHostFragment1Name = "URL_HOST_FRAGMENT1";
	private static String _urlHostFragment2Value;
	private static String _urlHostFragment2Name = "URL_HOST_FRAGMENT2";

	private boolean _printTiming = false; //for performance timing
//	private Instant _startInstant = Instant.EPOCH; //for performance timing
//	private Map<String, Integer> _recentUrlResultsCache = new HashMap<>();

	private static final String _historyFilename = "gu.history.txt";
	private static final String _htmlFilename = "00html.html";
	private static final String _allNewLinksFilename = "00allNewLinks.dat";
	private static final Logger _log = LogManager.getLogger ();

	private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
	private static final short _warningColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW;
	private static final short _highlightColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA;

	//global members
	public static boolean _Debug = false;
//	public static boolean _TestMode = false;

	public static final String _AppName = "JHistory";
	public static final String NL = System.getProperty ("line.separator");
	private static final String DOCTYPE = "<!DOCTYPE HTML>";// PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";
}
