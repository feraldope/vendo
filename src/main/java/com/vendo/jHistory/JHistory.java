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
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

				} else if (arg.equalsIgnoreCase ("timeout") || arg.equalsIgnoreCase ("to")) {
					try {
						_timeoutSeconds = Integer.parseInt (args[++ii]);
						if (_timeoutSeconds < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}


//				} else if (arg.equalsIgnoreCase ("outFile") || arg.equalsIgnoreCase ("out")) {
//					try {
//						_outFileName = args[++ii];
//					} catch (ArrayIndexOutOfBoundsException exception) {
//						displayUsage ("Missing value for /" + arg, true);
//					}

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

		//NOTE: similar code exists in JHistory.java and GetUrl.java
		_urlPathFragmentsValues = Arrays.stream(System.getenv (_urlPathFragmentsName).toLowerCase ().split (",")).map(String::trim).collect(Collectors.toList());
		if (/*_urlPathFragmentsValues == null ||*/ _urlPathFragmentsValues.isEmpty ()) {
			displayUsage ("Must specify environment variable '" + _urlPathFragmentsName + "'", true);
		}
		_urlHostsToBeSkippedValues = Arrays.stream(System.getenv (_urlHostsToBeSkippedName).toLowerCase ().split (",")).map(String::trim).collect(Collectors.toList());
		if (/*_urlHostsToBeSkippedValues == null ||*/ _urlHostsToBeSkippedValues.isEmpty ()) {
			displayUsage ("Must specify environment variable '" + _urlHostsToBeSkippedName + "'", true);
		}
		//DO NOT SORT - DO NOT CHANGE ORDER FROM FILE
		_urlKnownHostFragmentsValues = Arrays.stream(System.getenv (_urlKnownHostFragmentsName).toLowerCase ().split (",")).map(String::trim).collect(Collectors.toList());
		if (/*_urlKnownHostFragmentsValues == null ||*/ _urlKnownHostFragmentsValues.isEmpty ()) {
			displayUsage ("Must specify environment variable '" + _urlKnownHostFragmentsName + "'", true);
		}
		_urlDeadHostFragmentsValues = Arrays.stream(System.getenv (_urlDeadHostFragmentsName).toLowerCase ().split (",")).map(String::trim).collect(Collectors.toList());
		if (_urlDeadHostFragmentsValues == null || _urlDeadHostFragmentsValues.isEmpty ()) {
			displayUsage ("Must specify environment variable '" + _urlDeadHostFragmentsName + "'", true);
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

		readGuHistoryFile(true);

		readJhHistoryFile();

		Set<String> missingValues = verifyUrlKnownHostFragmentsValues();
		if (!missingValues.isEmpty()) {
			_log.error("JHistory.run: verifyUrlKnownHostFragmentsValues() found " + missingValues.size() + " missing values: " + missingValues.toString());
		}

		_interestingUrls = loadInterestingUrlsFromJhHistory (true);

		/*Thread watchGuHistoryFileThread =*/ watchGuHistoryFile ();

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
	private boolean handleImageUrl (UrlData urlData)
	{
		boolean status = false;

		String urlNormalizedString = urlData.getNormalizedLine ();
		writeAppendJhHistoryFile(urlNormalizedString);

		System.out.println ("");

		List<MatchData> matchList = findInGuHistory (urlData);//.getPathBase (), urlNormalizedString);
		if (matchList.size () > 0) {
			System.out.println ("Duplicate entry(s) found in history file: " + matchList.size ());
//			System.out.println ("---- " + urlNormalizedString);
			System.out.print ("---- ");// + urlNormalizedString);
			short color = isKnownDeadHost (_urlDeadHostFragmentsValues, urlNormalizedString) ? _warningColor : _defaultColor;
			VendoUtils.printWithColor (color, urlNormalizedString);

			int maxLines = (_Debug ? 20 : 6);
			int lineCount = Math.min (matchList.size (), maxLines);
			for (int ii = 0; ii < lineCount; ii++) {
				MatchData match = matchList.get (ii);
				/*short*/ color = (match.getStrength () < 1000 ? _alertColor : _warningColor);
				System.out.print ("  ");
				VendoUtils.printWithColor (color, match.getLine (), false);
				System.out.println (" (" + match.getReverseIndex () + ", " + match.getStrength () + ")");
			}
			status = true;

		} else {
			System.out.print ("Not found: ");
			short color = isKnownDeadHost (_urlDeadHostFragmentsValues, urlNormalizedString) ? _warningColor : _highlightColor;
			VendoUtils.printWithColor (color, urlNormalizedString);
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	//generates HTML; returns true if url found, false otherwise
	private boolean handleHtmlUrl (UrlData urlData1)
	{
		boolean status = false;

		String urlNormalizedString = urlData1.getNormalizedLine();

		addToInterestingUrls(urlNormalizedString);

		System.out.println("");
		VendoUtils.printWithColor(_highlightColor, "Processing URL: " + urlNormalizedString);

		findInJhHistory(urlNormalizedString).stream()
											.sorted(String.CASE_INSENSITIVE_ORDER)
											.forEach(s -> VendoUtils.printWithColor(_warningColor, "Previous match: " + s));

		List<UrlLinkData> urlLinkDataAll = getUrlLinkData(urlData1, true);

		if (urlLinkDataAll == null || urlLinkDataAll.isEmpty()) {
			return status; //nothing to do
		}

		List<UrlLinkData> urlLinkDataNew = new ArrayList<>();
		for (UrlLinkData urlLink : urlLinkDataAll) {
			String firstImageUrl = urlLink.getImageUrlStr(0);
			if (!urlLinkDataNew.contains(urlLink)) {
				UrlData urlData2 = parseLine(firstImageUrl);
				boolean found = handleImageUrl(urlData2);
				if (!found) {
					urlLinkDataNew.add(urlLink);
				}
			} else {
				int bh = 1;
			}
		}

		urlLinkDataNew = urlLinkDataNew.stream()
										.sorted((u1, u2) -> u1.getBaseUrl().getFile().compareToIgnoreCase(u2.getBaseUrl().getFile()))
										.collect(Collectors.toList());

		final String htmlEscapedModifier = VendoUtils.escapeHtmlChars("[bhlmr+]");

		final String regex = "(?i)(picture.*|\\sin\\s.*|\\saka\\s.*)";
		final String nameStr = urlLinkDataAll.iterator().next().getMainTitle().replaceAll(regex, "").replaceAll("\\s", ""); //TODO hack - string processing, and just uses first item in collection

		String albumServetLink = "http://localhost/AlbumServlet/AlbumServlet?mode=doSampler&filter1=" + nameStr + "&filter2=" + nameStr + htmlEscapedModifier +
									"&windowWidth=2200&windowHeight=2050&panels=4000&columns=8&collapseGroups=on&limitedCompare=true&looseCompare=true&ignoreBytes=true&debug=on#topAnchor";
		String titleStr = _htmlFilename + " - " + nameStr + " (" + urlLinkDataNew.size() + " new of " + urlLinkDataAll.size() + " total)";

		String html = DOCTYPE + NL;
		if (!urlLinkDataNew.isEmpty()) {
			AtomicInteger linkNumber = new AtomicInteger(1);
			final int totalLinkCount = urlLinkDataNew.size();
			html += html(
					head(
						title(titleStr)
					),
					body(
						div(attrs("#div0"),
							h1(titleStr)
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
										h2("(" + linkNumber.getAndIncrement() + "/" + totalLinkCount + ") " + nameStr + " - " + u1.getChildTitle()),
										a().withText(u1.getAlbumUrlStr())
											.withHref(u1.getAlbumUrlStr())
											.withTarget("_blank"),
										h4("call gu.bat " + u1.getImageUrlStr(0) + " " + nameStr + " && call push.bat")
									),
									div(attrs("#div5"),
										u1.getImageUrlList().stream().map(u2 ->
											a(img().withSrc(u2)
													.attr("width=" + getImageDimensionFromName(u2, Dimension.Width, true))
													.attr("height=" + getImageDimensionFromName(u2, Dimension.Height, true))
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
						title(titleStr)
					),
					body(
						div(attrs("#div0"),
							h1(titleStr)
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

		//never overwrite .BAT file while it is running
		//if the BAT file is run more than once simultaneously, each instance will get its own lock file
		Path outputFilePath = FileSystems.getDefault().getPath(_destDir, "tmp", nameStr + ".bat");
		Path lockFileBase = FileSystems.getDefault().getPath(_destDir, "tmp", nameStr + ".lock");
		if (!VendoUtils.fileExistsWild(lockFileBase.toString() + "*")) {
			String batFileCommands = "";
			if (!urlLinkDataNew.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				sb.append("echo off").append(NL)
				  .append("REM autogenerated by " + _AppName).append(NL)
//				  .append("title " + nameStr).append(NL)
				  .append("setlocal").append(NL)
				  .append("SET DO_INTERMEDIATE_PUSH=YES").append(NL)
				  .append("IF EXIST ").append(lockFileBase.toString()).append("* SET DO_INTERMEDIATE_PUSH=NO").append(NL)
				  .append("SET LOCKFILE=").append(lockFileBase.toString()).append(".%RANDOM%").append(NL)
				  .append("SET do").append(NL)
				  .append("SET lock").append(NL)
				  .append("echo running at %DATE% %TIME% ... >> %LOCKFILE%").append(NL)
				  .append("set NAME=%1").append(NL)
				  .append("if %NAME%@==@ set NAME=").append(nameStr).append(NL);
				int count = 0;
				for (UrlLinkData urlLinkData : urlLinkDataNew) {
					sb.append(NL)
					  .append("echo ").append(++count).append(" of ").append(urlLinkDataNew.size()).append(NL)
					  .append("call gu.bat ").append(urlLinkData.getImageUrlStr(0)).append(" %NAME%").append(NL)
					  .append("REM echo on").append(NL);
//					if (count != urlLinkDataNew.size()) {
						sb.append("IF %DO_INTERMEDIATE_PUSH%==YES echo calling push.bat").append(NL)
						  .append("IF %DO_INTERMEDIATE_PUSH%==YES call push.bat").append(NL);
//					}
				}
				sb.append(NL)
				  .append("IF %DO_INTERMEDIATE_PUSH%==NO echo calling push.bat").append(NL)
				  .append("IF %DO_INTERMEDIATE_PUSH%==NO call push.bat").append(NL)
				  .append("REM echo on").append(NL)
				  .append("del %LOCKFILE%").append(NL);
				batFileCommands = sb.toString();
			}

			if (writeOutputFile(batFileCommands, outputFilePath)) {
				System.out.println("");
				_log.debug("JHistory.handleHtmlUrl: wrote " + urlLinkDataNew.size() + " links to BAT output file: " + outputFilePath.toString());
				status = true;
			}
		} else {
			_log.error("JHistory.handleHtmlUrl: BAT FILE IN USE; NOT OVERWRITING: " + outputFilePath.toString());
		}

		outputFilePath = FileSystems.getDefault().getPath(_destDir, _htmlFilename);
		if (writeOutputFile (html, outputFilePath)) {
			System.out.println ("");
			_log.debug("JHistory.handleHtmlUrl: wrote " + urlLinkDataNew.size () + " links to HTML output file: " + outputFilePath.toString());
			status = true;
		}

		String allNewLinks = urlLinkDataNew.stream ().map (u -> u.getImageUrlStr(0))/*.sorted ()*/.collect (Collectors.joining (NL)) + NL;
		outputFilePath = FileSystems.getDefault().getPath(_destDir, _allNewLinksFilename);
		if (writeOutputFile (allNewLinks, outputFilePath)) {
			System.out.println ("");
			_log.debug("JHistory.handleHtmlUrl: wrote " + urlLinkDataNew.size () + " links to link output file: " + outputFilePath.toString());
			status = true;
		}

		List<UrlLinkData> urlLinkDataNewDead = urlLinkDataNew.stream().filter(u -> isKnownDeadHost (_urlDeadHostFragmentsValues, u.getImageUrlList().get(0))).collect(Collectors.toList());
		List<UrlLinkData> urlLinkDataNewNotDead = urlLinkDataNew.stream().filter(u -> !isKnownDeadHost (_urlDeadHostFragmentsValues, u.getImageUrlList().get(0))).collect(Collectors.toList());

		VendoUtils.printWithColor (_highlightColor, "Total links: " + urlLinkDataAll.size ());
		VendoUtils.printWithColor (_alertColor,     "Old links:   " + (urlLinkDataAll.size () - urlLinkDataNew.size ()));
		VendoUtils.printWithColor (_warningColor,   "New links:   " + (urlLinkDataNewDead.isEmpty () ? "---" : urlLinkDataNewDead.size ()) + " (known dead)");
		VendoUtils.printWithColor (_highlightColor, "New links:   " + (urlLinkDataNewNotDead.isEmpty () ? "---" : urlLinkDataNewNotDead.size () + " *****") + " (excluding any known dead)");

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<String> getUrlLinkDataHelper (UrlData urlData1, boolean verbose)
	{
		List<String> mainLinksList = new ArrayList<>();

		Document document = null;
		try {
			document = Jsoup.parse(urlData1.getUrl(), _timeoutSeconds * 1000);
		} catch (Exception ee) {
			//will return empty list below
//			_log.error("JHistory.getUrlLinkDataHelper: error opening main URL: " + urlData1.getUrl());
			VendoUtils.printWithColor (_alertColor, "JHistory.getUrlLinkDataHelper: " + getParseError(ee) + " opening main URL: " + urlData1.getUrl());

			//try again with denormalized url (if it wasn't already a denormalized url)
			String deNormalizedUrlStr = urlData1.getDeNormalizedUrl().toString();
			if (!deNormalizedUrlStr.equalsIgnoreCase(urlData1.getNormalizedLine())) {
				try {
					document = Jsoup.parse(urlData1.getDeNormalizedUrl(), _timeoutSeconds * 1000);
				} catch (Exception e2) {
					//will return empty list below
	//				_log.error("JHistory.getUrlLinkDataHelper: error opening main URL: " + urlData1.getUrl());
					VendoUtils.printWithColor(_alertColor, "JHistory.getUrlLinkDataHelper: " + getParseError(ee) + " opening main URL: " + urlData1.getDeNormalizedUrl());
			}
			}
		}

		if (document != null) {
//TODO fix _mainTitle
			/*String*/
			_mainTitle = document.select("title").text();

			mainLinksList = document.select("a[href]").stream()
					.map(e -> e.attr("href"))
//					.map(e -> e.attr("abs:href"))
					.filter(s -> s.contains("/galleries/") || s.contains("/picz/"))
					.filter(s -> !s.contains("/287586/")) //HACK - this gets rid of one who will not be named
					.filter(s -> !s.contains("/116335/")) //HACK - this gets rid of one who will not be named
					.map(s -> {
						if (!s.startsWith("http")) {
							String fragment = _urlPathFragmentsValues.get(0); //HACK
							String string = urlData1.getNormalizedLine().replaceAll(Pattern.quote(fragment) + ".*", "");
							return string + s;
						} else {
							return s;
						}
					})
					.distinct()
					.collect(Collectors.toList());

			String urlNormalizedString = urlData1.getNormalizedLine();
			if (verbose) {
//				VendoUtils.printWithColor(_highlightColor, "Processed URL: " + urlNormalizedString + ", found " + mainLinksList.size() + " links");
				System.out.println("Processed URL: " + urlNormalizedString + (mainLinksList.size() > 0 ? " --> found " + mainLinksList.size() + " links" : ""));
			}

			if (mainLinksList.size() > 0) {
				writeAppendJhHistoryFile(urlNormalizedString + " " + mainLinksList.size());
			}
		}

		return mainLinksList;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<String> getMainLinksList (UrlData urlData1, boolean verbose)
	{
//TODO ?? - do I still need this?
//		List<String> baseLeafs = Arrays.asList("/galleries/v2/", "/picz/");

		String leaf = urlData1.getUrl().getFile();

		List<String> mainLinksList = new ArrayList<>();
		_interestingUrls.stream()
				.parallel()
				.forEach(u -> {
//TODO
//					for (String fragment : _urlPathFragmentsValues) {
					final UrlData urlData2 = parseLine(u + leaf);
					final List<String> mainLinksList1 = getUrlLinkDataHelper(urlData2, verbose);
					synchronized (mainLinksList) {
						mainLinksList.addAll(mainLinksList1);
					}
//					}
				});

		return mainLinksList;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<UrlLinkData> getUrlLinkData (UrlData urlData1, boolean verbose)
	{
		List<String> mainLinksList = getMainLinksList(urlData1, verbose);

		List<UrlLinkData> urlLinkData = getChildLinkData (mainLinksList, verbose);

		return urlLinkData;
	}

	///////////////////////////////////////////////////////////////////////////
	private UrlLinkData getChildLinkDataHelper (String mainLink, boolean verbose)
	{
		UrlLinkData urlLinkData = null;

		if (verbose) {
			System.out.print("."); //no linefeed
		}

		URL url = null;
		Document document = null;
		try {
			url = new URL (mainLink);
			document = Jsoup.parse (url, _timeoutSeconds * 1000);
		} catch (Exception ee) {
			if (verbose) {
				System.out.println(""); //just linefeed
			}
//			_log.error ("JHistory.getChildLinkDataHelper: error opening child URL: " + url.toString ());
			VendoUtils.printWithColor (_alertColor, "JHistory.getChildLinkDataHelper: " + getParseError(ee) + " opening child URL: " + url.toString ());
//			System.err.println (ee);
//			ee.printStackTrace (System.err);
		}

		if (document != null) {
//			final String tagForChildTitle = "title"; //works OK
			final String tagForChildTitle = "h1"; //works better
			String childTitle = document.select(tagForChildTitle).text();

			List<String> imageLinksList = document.select ("[src]").stream ()
													.filter(e -> e.tagName ().equals ("img"))
													.map (e -> e.attr("abs:src"))
													.filter (s -> !s.contains ("/preview"))
//													.sorted(ignoreImageSizeComparator)
													.distinct()
													.collect(Collectors.toList());
			urlLinkData = new UrlLinkData(url, _mainTitle, childTitle, mainLink, imageLinksList);
		}

		return urlLinkData;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<UrlLinkData> getChildLinkData (List<String> mainLinksList, boolean verbose)
	{
		if (verbose) {
			long distinctLeafs = mainLinksList.stream()
												.map(l -> {
													try {
														return new URL (l).getFile();
													} catch (Exception ignored) {}
													return null;
//													String leaf = null;
//													try {
//														URL url = new URL (l);
//														return url.getFile();
//													} catch (Exception ignored) {}
//													return leaf;
												})
												.filter(Objects::nonNull)
												.distinct()
												.count();
			VendoUtils.printWithColor(_highlightColor, "Processing " + mainLinksList.size() + " links");
			VendoUtils.printWithColor(_highlightColor, "Found " + distinctLeafs + " distinct leafs");

			if (false) { //debugging
				System.out.println(mainLinksList.stream()
						.map(l -> {
							String leaf = null;
							try {
								URL url = new URL (l);
								return url.getFile();
							} catch (Exception ignored) {}
							return leaf;
						})
						.filter(Objects::nonNull)
						.distinct()
						.sorted(String.CASE_INSENSITIVE_ORDER)
						.collect(Collectors.joining(NL)) + NL);
			}
		}

		//use map to avoid duplicate image links
		ConcurrentHashMap<String, UrlLinkData> urlLinkDataMap = new ConcurrentHashMap<>();
		mainLinksList.stream()
					.parallel()
					.forEach(l -> {
						if (verbose) {
							System.out.print("."); //no linefeed
						}
						final UrlLinkData urlLinkData1 = getChildLinkDataHelper (l, verbose);
						if (urlLinkData1 != null) {
							final List<String> imageLinksList = urlLinkData1.getImageUrlList();
							if (imageLinksList != null && imageLinksList.get(0) != null && !urlLinkDataMap.containsKey(imageLinksList.get(0))) {
								urlLinkDataMap.put(imageLinksList.get(0), urlLinkData1);
							}
						}
					});

		if (false) { //debugging
			System.out.println(urlLinkDataMap.values().stream()
					.map(u -> u.getImageUrlStr(0))
					.filter(Objects::nonNull)
					.distinct()
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.collect(Collectors.joining(NL)) + NL);
		}

		return new ArrayList<>(urlLinkDataMap.values());
	}

	///////////////////////////////////////////////////////////////////////////
	//watch file and call readGuHistoryFile() whenever file changes on disk
	private Thread watchGuHistoryFile ()
	{
		Thread watchingThread = null;
		try {
			Path path = FileSystems.getDefault ().getPath (_destDir, _guHistoryFilename);
			Path dir = path.getRoot ().resolve (path.getParent ());
			String filename = path.getFileName ().toString ();

			_log.info ("JHistory.watchGuHistoryFile: watching history file: " + path.normalize ().toString ());

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
						_log.debug ("JHistory.watchGuHistoryFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ().toString ());
					}

					if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_MODIFY) ||
						pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_CREATE)) {
						readGuHistoryFile(false);
					}
				}

				@Override
				protected void overflow (WatchEvent<?> event)
				{
					_log.error ("JHistory.watchGuHistoryFile.overflow: received event: " + event.kind ().name () + ", count = " + event.count ());
					_log.error ("JHistory.watchGuHistoryFile.overflow: ", new Exception ("WatchDir overflow"));
				}
			};
			watchingThread = new Thread (watchDir);
			watchingThread.start ();

//			thread.join (); //wait for thread to complete

		} catch (Exception ee) {
			_log.error ("JHistory.watchGuHistoryFile: exception watching history file", ee);
		}

		return watchingThread;
	}

	///////////////////////////////////////////////////////////////////////////
	//only called at startup
	private boolean readJhHistoryFile() {
		Instant startInstant = Instant.now();

		Path path = FileSystems.getDefault().getPath(_destDir, _jhHistoryFilename);

		boolean status = true;
		try (Stream<String> stream = Files.lines (path)) {
			_jhHistoryFileContents = stream
//										.filter(s -> !s.startsWith("#"))
										.map(String::trim)
										.collect (Collectors.toSet ());

		} catch (IOException ee) {
			_log.error ("JHistory.readJhHistoryFile: error reading JH history file \"" + _jhHistoryFilename + "\"", ee);
			status = false;
		}

		printTiming(startInstant, "JHistory.readJhHistoryFile");

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readGuHistoryFile(boolean printTiming)
	{
		Instant startInstant = Instant.now ();

		Path path = FileSystems.getDefault ().getPath (_destDir, _guHistoryFilename);
		FileTime historyFileModified = FileTime.from (Instant.MAX);
		try {
			historyFileModified = Files.getLastModifiedTime (path);
		} catch (Exception ee) {
			_log.error ("JHistory.readGuHistoryFile: exception calling getLastModifiedTime", ee);
		}

		boolean status = true;
		if (_guHistoryFileModified.compareTo (historyFileModified) < 0) {
			List<UrlData> contents;
			try (Stream<String> stream = Files.lines (path)) {
				contents = stream.parallel ()
								 .map(this::parseLine)
								 .filter(Objects::nonNull)
								 .collect (Collectors.toList ());

				synchronized (_guHistoryFileContentsLock) {
					_guHistoryFileContents = contents;
					_guHistoryFileModified = historyFileModified;
				}
			} catch (IOException ee) {
				_log.error ("JHistory.readGuHistoryFile: error GU reading history file \"" + _guHistoryFilename + "\"", ee);
				status = false;
			}
		}

		if (printTiming) {
			printTiming(startInstant, "JHistory.readGuHistoryFile");
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<String> findInJhHistory (String sourceUrl)
	{
//call this on every URL for temp debugging
//		Set<String> forDebugging = loadInterestingUrlsFromJhHistory (true);

		List<String> foundInJhHistory = new ArrayList<>();
		for (String fragment : _urlPathFragmentsValues) {
			foundInJhHistory.addAll(
				_jhHistoryFileContents.stream()
					.filter(s -> s.contains(fragment))
					.filter(s -> {
						String sourceUrlFragment = sourceUrl.replaceAll(".*" + fragment, fragment).replaceAll("\\s+.*", "");
						String jhHistoryUrlFragment = s.replaceAll(".*" + fragment, fragment).replaceAll("\\s+.*", "");
						return jhHistoryUrlFragment.equalsIgnoreCase(sourceUrlFragment);
					})
//					.sorted(String.CASE_INSENSITIVE_ORDER)
					.collect(Collectors.toList()));
		}

		return foundInJhHistory;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<MatchData> findInGuHistory (UrlData urlDataIn)
	{
		String basePattern = urlDataIn.getPathBase();
		String sourceUrlIn = urlDataIn.getNormalizedLine();

		String patternStr = VendoUtils.getUrlFileComponent (basePattern).toLowerCase ();
		if (_Debug) {
			_log.debug ("JHistory.findInGuHistory: basePattern = " + basePattern);
			_log.debug ("JHistory.findInGuHistory: patternStr = " + patternStr);
		}

//		Instant startInstant = Instant.now ();

		final int maxThreads = VendoUtils.getLogicalProcessors () - 2;
		final int minPerThread = 100;
		final int chunkSize;
		List<List<UrlData>> chunks;
		AtomicBoolean foundStrongMatch = new AtomicBoolean ();
		final List<MatchData> matchList = new ArrayList<MatchData> ();
		synchronized (_guHistoryFileContentsLock) {
			chunkSize = AlbumImages.calculateChunks (maxThreads, minPerThread, _guHistoryFileContents.size ()).getFirst ();
			chunks = ListUtils.partition (_guHistoryFileContents, chunkSize);
			final int numChunks = chunks.size ();
			final CountDownLatch endGate = new CountDownLatch (numChunks);

			if (_Debug) {
				_log.debug("JHistory.findInGuHistory: _historyFileContents.size() = " + _guHistoryFileContents.size());
				_log.debug("JHistory.findInGuHistory: numChunks = " + numChunks);
				_log.debug("JHistory.findInGuHistory: chunkSize = " + chunkSize);
			}

			for (final List<UrlData> chunk : chunks) {
				new Thread(() -> {
					final int numLines = chunk.size();
					final String sourceUrl = sourceUrlIn;

					//check for strong matches
					for (int ii = 0; ii < numLines; ii++) {
						final UrlData urlData = chunk.get(ii);
						final String baseLine = urlData.getPathBase();
//TODO - ignore if extensions don't match
						if (strongMatch(baseLine.toLowerCase(), patternStr, sourceUrl, urlData)) {
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
							final UrlData urlData = chunk.get(ii);
							final String baseLine = urlData.getPathBase();
							int strength = weakMatch(baseLine.toLowerCase(), patternStr, sourceUrl, urlData);
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
						final int minStrength = 0;
						for (int ii = 0; ii < numLines; ii++) {
							final UrlData urlData = chunk.get(ii);
							final String baseLine = urlData.getPathBase();
							final int strength = weakMatch(baseLine.toLowerCase(), patternStr, sourceUrl, urlData);
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
				_log.error("JHistory.findInGuHistory: endGate:", ee);
			}
		}

		// if we found any strong matches, remove all weak matches that might have been added
		if (foundStrongMatch.get ()) {
			matchList.removeIf (MatchData::isWeakMatch);
		}

//		printTiming (startInstant, "JHistory.findInGuHistory");

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
			else if (line.contains("\n")) {
				urlStr = line.split ("\n")[0].trim (); //extract URL from line
			}
			else if (line.matches(".*\\s+.*")) {
				urlStr = line.split ("\\s+")[0].trim (); //extract URL from line
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

	//debugging
	static String debugFragment = "flower".toLowerCase ();

	///////////////////////////////////////////////////////////////////////////
	//NOTE: this method used by JHistory.java and GetUrl.java
	//returns true if this is a special match that we can ignore
	public static boolean ignoreThisSpecialMatch (List<String> urlKnownHostFragmentsValues, String sourceUrl, String historyUrl)
	{
		//debugging
//		if (sourceUrl.toLowerCase().contains(debugFragment) || historyUrl.toLowerCase().contains(debugFragment)) {
//			System.out.println("************* - found match for \"" + debugFragment + "\": " + urlData1.getNormalizedLine());
//		}

		String sourceFragment = urlKnownHostFragmentsValues.stream().filter(sourceUrl::contains).findAny().orElse(null);
		String urlDataFragment = urlKnownHostFragmentsValues.stream().filter(historyUrl::contains).findAny().orElse(null);

//TODO - what to do about ip addresses?
		final Pattern ipAddressPattern = Pattern.compile(".*//\\d+\\.\\d+\\.\\d+\\.\\d+\\/.*");
		boolean b1 = ipAddressPattern.matcher (sourceUrl).matches ();
		boolean b2 = ipAddressPattern.matcher (historyUrl).matches ();

		return (sourceFragment != null &&
				urlDataFragment != null &&
				!sourceFragment.equalsIgnoreCase(urlDataFragment)) ||
					ipAddressPattern.matcher (historyUrl).matches ();
	}

	///////////////////////////////////////////////////////////////////////////
	//NOTE: this method used by JHistory.java and GetUrl.java
	//returns true if this is a know dead host
	public static boolean isKnownDeadHost (List<String> urlDeadHostFragmentsValues, String sourceUrl)
	{
//		_log.debug("JHistory.isKnownDeadHost: urlDeadHostFragmentsValues: " + urlDeadHostFragmentsValues + ", sourceUrl: " + sourceUrl);
		String sourceFragment = urlDeadHostFragmentsValues.stream().filter(sourceUrl::contains).findAny().orElse(null);
//		_log.debug("JHistory.isKnownDeadHost: sourceFragment: " + sourceFragment);
		return sourceFragment != null;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns set of any values in _urlKnownHostFragmentsValues that do not exist in _historyFileContents
	private Set<String> verifyUrlKnownHostFragmentsValues() {
		Instant startInstant = Instant.now();

//TODO - stream the inner block

//		Set<String> missingValues = new ConcurrHashSet<>(_urlKnownHostFragmentsValues);
		Set<String> missingValues = ConcurrentHashMap.newKeySet();
		_guHistoryFileContents.stream()
				.parallel()
				.map(u -> u.getUrl().toString().toLowerCase())
				.forEach(s -> {
					for (String urlFragment : _urlKnownHostFragmentsValues) {
						if (s.contains(urlFragment)) {
							missingValues.remove(urlFragment);
							return;
						}
					}
				});

		printTiming (startInstant, "JHistory.verifyUrlKnownHostFragmentsValues");

		return missingValues;
	}

	///////////////////////////////////////////////////////////////////////////
	private Set<String> loadInterestingUrlsFromJhHistory (boolean verbose)
	{
		Instant startInstant = Instant.now();

		Map<String, Long> interestingUrlDistribution = _jhHistoryFileContents.stream()
						.filter(s -> _urlPathFragmentsValues.stream().anyMatch(s::contains))
						.filter(s -> _urlHostsToBeSkippedValues.stream().noneMatch(s::contains))
						.map(s -> {
							final String[] string = {s}; //HACK variable must be final for lambda, so use array instead of string
							_urlPathFragmentsValues.forEach(f -> string[0] = string[0].replaceAll(Pattern.quote(f) + ".*", ""));
							return string[0];
//old way
//							for (String fragment : _urlPathFragmentsValues) {
//								string = string.replaceAll(Pattern.quote(fragment) + ".*", "");
//							}
//							return string;
						})
						.collect(Collectors.groupingBy(e -> e, Collectors.counting()));

		printTiming(startInstant, "JHistory.loadInterestingUrlsFromJhHistory");

		if (verbose) {
			System.out.println("URLs (" + interestingUrlDistribution.size() + ")" + NL +
					interestingUrlDistribution.keySet().stream()
					.map(u -> u + " (" + interestingUrlDistribution.get(u) + ")")
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.collect(Collectors.joining(NL)) + NL);
		}

		return new HashSet<>(interestingUrlDistribution.keySet());
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean addToInterestingUrls (String urlStrOrig)
	{
		//debugging
//		_log.debug ("JHistory.addToInterestingUrls: urlStr: " + urlStr);

		int numAdded = 0;
		for (String fragment : _urlPathFragmentsValues) {
			String urlStrBase = urlStrOrig.replaceAll(Pattern.quote(fragment) + ".*", "");
			boolean added = _interestingUrls.add(urlStrBase);
			if (added) {
				numAdded++;
				writeAppendJhHistoryFile(urlStrOrig);

				_log.debug("JHistory.addToInterestingUrls: added: " + urlStrBase);
			}
		}

		return numAdded > 0;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean strongMatch (String historyFragment, String patternStr, String sourceUrl, UrlData urlData)
	{
		boolean matches = patternStr.length () >= 2 && historyFragment.contains (patternStr);

		//this block allows redownload of what might be considered close matches
		if (matches && ignoreThisSpecialMatch(_urlKnownHostFragmentsValues, sourceUrl, urlData.getLine())) {
			matches = false; //force not a match
		}

		if (_Debug && matches) {
			_log.debug ("JHistory.strongMatch: historyFragment: " + historyFragment);
			_log.debug ("JHistory.strongMatch: patternStr: " + patternStr);
		}

		return matches;
	}

	///////////////////////////////////////////////////////////////////////////
	private int weakMatch (String historyFragment, String patternStr, String sourceUrl, UrlData urlData)
	{
		String[] array1 = StringUtils.split (historyFragment, '/'); //URL from history file
		String[] array2 = StringUtils.split (patternStr, '/'); //pattern to match

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
				if (string.length () >= 4) { //all-digit strings must have minimum length to count as match
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
			if (!ignoreThisSpecialMatch(_urlKnownHostFragmentsValues, sourceUrl, urlData.getLine())) {
				strength |= 32;
			} // else ignore
		}

		if (matchingNumberStrings >= 1 && matchingNonNumberStrings >= 1 && nonMatchingNumberStrings == 0) {
			if (!ignoreThisSpecialMatch(_urlKnownHostFragmentsValues, sourceUrl, urlData.getLine())) {
				strength |= 64;
			} else {
				strength = 0; //force not a match
			}
		}

		if (strength > 0 && _Debug) {
//		if (historyFragment.contains (debugFragment)) {
			_log.debug ("JHistory.weakMatch: historyFragment: " + historyFragment);
			_log.debug ("JHistory.weakMatch: patternStr: " + patternStr);
			_log.debug ("JHistory.weakMatch: set1:  (" + set1size + ") " + StringUtils.join (set1.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: set2:  (" + set2size + ") " + StringUtils.join (set2.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: set3:  (" + set3size + ") " + StringUtils.join (set3.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: match: (" + matchListSize + ") " + StringUtils.join (matchList.stream ().sorted ().collect (Collectors.toList ()), ','));
			_log.debug ("JHistory.weakMatch: strength: " + strength);
		}

		return strength;
	}

	///////////////////////////////////////////////////////////////////////////
	private String getParseError (Throwable exception)
	{
		if (exception instanceof SocketTimeoutException) {
			return "timeout";
		} else if (exception instanceof UnknownHostException) {
			return "unknown host error";
		} else if (exception instanceof HttpStatusException) {
			return "error " + ((HttpStatusException) exception).getStatusCode();
		}
		return "error";
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
	//do not write lines that have already been written
	private synchronized boolean writeAppendJhHistoryFile(String urlString)
	{
		if (_jhHistoryFileContents.contains (urlString)) {
	 		return false;
		}

		try (FileOutputStream outputStream = new FileOutputStream (new File(_destDir + _jhHistoryFilename), true)) {
			outputStream.write ((urlString + NL).getBytes ());
			outputStream.flush ();

			_jhHistoryFileContents.add (urlString);

		} catch (IOException ee) {
			_log.error ("JHistory.writeAppendJhHistoryFile: error writing output file \"" + _destDir + _jhHistoryFilename + "\"");
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
//		if (!_printTiming) {
//			return;
//		}

		long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();
//		_log.debug (message + ": elapsed: " + LocalTime.ofNanoOfDay (elapsedNanos));
		System.out.println(message + ": elapsed: " + LocalTime.ofNanoOfDay (elapsedNanos));
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

//		private static final String _title;
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
//			if (_line.contains("www.bdsmirl.com")) {
//				return _line;
//			}
 			return _line.replaceFirst ("://www\\.", "://");
		}

		///////////////////////////////////////////////////////////////////////////
		public URL getDeNormalizedUrl () //throws Exception
 		{
			String line = _line;
			if (!_line.contains("://www.")) {
				line = _line.replaceFirst("://", "://www.");
			}
			URL deNormalizedUrl = _url;
			try {
				deNormalizedUrl = new URL(line);
			} catch (Exception ignore) {
			}
			return deNormalizedUrl;
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
			return _urlPathFragmentsValues.stream().anyMatch(s -> getPathBase ().toLowerCase ().contains (s));
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
//	private String _outFileName = null;
	private FileTime _guHistoryFileModified = FileTime.from (Instant.MIN);
	private Set<String> _jhHistoryFileContents = new HashSet<> ();
	private Set<String> _interestingUrls = new HashSet<>();
	private List<UrlData> _guHistoryFileContents = new ArrayList<> ();
	private final Object _guHistoryFileContentsLock = new Object ();
	private int _timeoutSeconds = 5;
	private final int _imageDimensionMax = 300;

	private String _mainTitle; //HACK

	//must be specified in environment
	//NOTE: similar code exists in JHistory.java and GetUrl.java
	private final static String _urlPathFragmentsName = "URL_PATH_FRAGMENTS";
	private static List<String> _urlPathFragmentsValues;
	private final static String _urlHostsToBeSkippedName = "URL_HOSTS_TO_BE_SKIPPED";
	private static List<String> _urlHostsToBeSkippedValues;
	private final static String _urlKnownHostFragmentsName = "URL_KNOWN_HOST_FRAGMENTS";
	private static List<String> _urlKnownHostFragmentsValues;
	private final static String _urlDeadHostFragmentsName = "URL_DEAD_HOST_FRAGMENTS";
	private static List<String> _urlDeadHostFragmentsValues;

//	private boolean _printTiming = true; //for performance timing
//	private Instant _startInstant = Instant.EPOCH; //for performance timing
//	private Map<String, Integer> _recentUrlResultsCache = new HashMap<>();

	private static final String _guHistoryFilename = "gu.history.txt";
	private static final String _jhHistoryFilename = "jHistory.log";
	private static final String _htmlFilename = "00html.html";
	private static final String _allNewLinksFilename = "00allNewLinks.dat";
	private static final Logger _log = LogManager.getLogger ();

	private static final short _defaultColor = Win32.CONSOLE_FOREGROUND_COLOR_WHITE;
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
