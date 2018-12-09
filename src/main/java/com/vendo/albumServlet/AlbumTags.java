//AlbumTags.java

package com.vendo.albumServlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.vendoUtils.WatchDir;
import com.vendo.win32.Win32;


public class AlbumTags
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	//entry point for "update tags database" feature
	public static void main (String args[])
	{
//TODO - change CLI to read properties file, too

		AlbumFormInfo.getInstance (); //call ctor to load class defaults

//debugging eclipse logging
//		System.out.println ("***AlbumTags.main testing System.out.println()");
//		_log.debug ("***AlbumTags.main testing _log.debug()");

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
		AlbumFormInfo._profileLevel = 0; //enable profiling just before calling run0()

//		AlbumProfiling.getInstance ().enter/*AndTrace*/ (1); //called in run1()

		AlbumTags albumTags = AlbumTags.getInstance ();

		if (!albumTags.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		AlbumFormInfo._profileLevel = 5; //enable profiling just before calling run0()

		try {
			albumTags.run0 ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.main: ", ee);
		}

//		AlbumProfiling.getInstance ().exit (1); //called in run1()

		AlbumImages.shutdownExecutor ();

//		if (_databaseNeedsUpdate || _checkForOrphanFilters) {
//			AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ true); //called in run1()
//		}
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

				} else if (arg.equalsIgnoreCase ("batchInsertSize") || arg.equalsIgnoreCase ("batch")) {
					try {
						_batchInsertSize = Integer.parseInt (args[++ii]);
						if (_batchInsertSize < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("dumpTagData") || arg.equalsIgnoreCase ("dump")) {
					_dumpTagData = true;

				} else if (arg.equalsIgnoreCase ("checkForOrphans") || arg.equalsIgnoreCase ("co")) {
					_checkForOrphanFilters = true;

				} else if (arg.equalsIgnoreCase ("continuous") || arg.equalsIgnoreCase ("cont")) {
					_runContinuous = true;

				} else if (arg.equalsIgnoreCase ("resetTables") || arg.equalsIgnoreCase ("reset")) {
					_resetTables = true;

				} else if (arg.equalsIgnoreCase ("tagFilename") || arg.equalsIgnoreCase ("tagFile")) {
					try {
						_tagFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("tagPatternString") || arg.equalsIgnoreCase ("p")) {
					try {
						_tagPatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_inPattern == null) {
					_inPattern = arg;

				} else if (_outPattern == null) {
					_outPattern = arg;

				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args and handle defaults
		if (_tagFilename == null)
			displayUsage ("<tagFilename> not specified", true);

		if (_tagPatternString == null)
			_tagPatternString = "*";

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " /tagFilename <file> [/debug] [/batchInsertSize <rows>] [/checkForOrphans] [/continuous] [/dumpTagData] [/resetTables] [/tagPatternString]";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumTags getInstance ()
	{
		if (_instance == null)
			_instance = new AlbumTags ();

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumTags () //singleton
	{
		if (AlbumFormInfo._logLevel >= 9)
			_log.debug ("AlbumTags ctor");

//		_rootPath = AlbumFormInfo.getInstance ().getRootPath (false);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run0 ()
	{
		run1 ();

		if (!_runContinuous) {
			return;
		}

		try {
			Path path = FileSystems.getDefault ().getPath (_tagFilename);
			Path dir = path.getRoot ().resolve (path.getParent ());
			String file = path.getFileName ().toString ();

			_log.debug ("AlbumTags.run: watching tag file: " + file);

			Pattern pattern = Pattern.compile (file, Pattern.CASE_INSENSITIVE);
			boolean recurseSubdirs = false;

			WatchDir watchDir = new WatchDir (dir, pattern, recurseSubdirs)
			{
				@Override
				protected void notify (Path dir, WatchEvent<Path> pathEvent)
				{
					if (AlbumFormInfo._logLevel >= 6) {
						Path file = pathEvent.context ();
						Path path = dir.resolve (file);
						_log.debug ("AlbumTags.WatchDir.notify: " + pathEvent.kind ().name () + ": " + path.normalize ().toString ());
					}

					if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_MODIFY) ||
						pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_CREATE)) {
						run1 ();
					}
				}

				@Override
				protected void overflow (WatchEvent<?> event)
				{
					_log.error ("AlbumTags.WatchDir.overflow: received event: " + event.kind ().name () + ", count = " + event.count ());
					_log.error ("AlbumTags.WatchDir.overflow: ", new Exception ("WatchDir overflow"));
				}
			};
			Thread thread = new Thread (watchDir);
			thread.start ();

			thread.join ();

		} catch (Exception ee) {
			_log.error ("AlbumTags run: exception in continuous mode", ee);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void run1 ()
	{
		AlbumProfiling.getInstance ().enterAndTrace (5);

		RowCounts initialCounts = new RowCounts ();

		if (!databaseNeedsUpdate () && !_dumpTagData && !_resetTables && !_checkForOrphanFilters) {
			_log.debug ("AlbumTags.run: skipping update");

			AlbumProfiling.getInstance ().exit (5);
			return;
		}

		generateAlbumBase1NameMap ();

		Map<String, Set<String>> tagFileMap = readTagFile (_tagFilename, _tagPatternString);
//		_log.debug ("AlbumTags.run: tagFileMap.size() = " + tagFileMap.size ());

		Map<String, Set<String>> tagDatabaseMap = null;
		if (!_resetTables) {
			tagDatabaseMap = readTagDatabase ();
//			_log.debug ("AlbumTags.run: tagDatabaseMap.size() = " + tagDatabaseMap.size ());

			//log new tags
			Collection<String> newTags = subtractCollections (tagFileMap.keySet (), tagDatabaseMap.keySet ());
			if (newTags.size () > 0) {
				printWithColor (_highlightColor, "found new tags: " + newTags);
			}

			//log removed tags
			Collection<String> oldTags = subtractCollections (tagDatabaseMap.keySet (), tagFileMap.keySet ());
			if (oldTags.size () > 0) {
				printWithColor (_warningColor, "removed old tags: " + oldTags);
			}
		}

		List<String> tagFileKeys = new ArrayList<String> (tagFileMap.keySet ());
		Collections.sort (tagFileKeys, VendoUtils.caseInsensitiveStringComparator);

		for (String tag : tagFileKeys) {
//			_log.debug ("AlbumTags.run: processing tag = \"" + tag + "\"");

			Collection<String> newFilters = tagFileMap.get (tag);

			if (!_resetTables) {
				//log removed filters
				Collection<String> oldFilters = tagDatabaseMap.get (tag);
				oldFilters = subtractCollections (oldFilters, tagFileMap.get (tag));
				if (oldFilters.size () > 0) {
					printWithColor (_warningColor, "removed filters for tag: " + tag + ": " + oldFilters);
				}

				//determine any new filters: if tag exists in database map, remove all the values it contains
				newFilters = subtractCollections (newFilters, tagDatabaseMap.get (tag));
				if (newFilters.size () > 0) {
					printWithColor (_highlightColor, "found new filters for tag: " + tag + ": " + newFilters);
				}
			}

			if (newFilters.size () > 0) {
				generateTagData (tag, newFilters);
			}
		}

		if (_dumpTagData) {
			dumpTagData (tagFileKeys);
			return;
		}

		if (_resetTables) {
			resetTables ();
		}

		checkForMismatchedFilters ();

		updateTagDatabase (tagFileMap);

		RowCounts finalCounts = new RowCounts ();
		RowCounts diffCounts = new RowCounts (finalCounts).subtract (initialCounts);

		_log.debug ("AlbumTags.run: final counts:");
		_log.debug ("AlbumTags.run: tags: " + _decimalFormat1.format (diffCounts._numTags) + " -> " + _decimalFormat2.format (finalCounts._numTags));
		_log.debug ("AlbumTags.run: base1_names: " + _decimalFormat1.format (diffCounts._numBase1Names) + " -> " + _decimalFormat2.format (finalCounts._numBase1Names));
		_log.debug ("AlbumTags.run: base2_names: " + _decimalFormat1.format (diffCounts._numBase2Names) + " -> " + _decimalFormat2.format (finalCounts._numBase2Names));
		_log.debug ("AlbumTags.run: raw_names: " + _decimalFormat1.format (diffCounts._numRawNames) + " -> " + _decimalFormat2.format (finalCounts._numRawNames));
		_log.debug ("AlbumTags.run: base1_names_tags: " + _decimalFormat1.format (diffCounts._numBase1NamesTags) + " -> " + _decimalFormat2.format (finalCounts._numBase1NamesTags));
		_log.debug ("AlbumTags.run: base2_names_tags: " + _decimalFormat1.format (diffCounts._numBase2NamesTags) + " -> " + _decimalFormat2.format (finalCounts._numBase2NamesTags));
		_log.debug ("AlbumTags.run: raw_names_tags: " + _decimalFormat1.format (diffCounts._numRawNamesTags) + " -> " + _decimalFormat2.format (finalCounts._numRawNamesTags));

		if (_checkForOrphanFilters) {
			checkForOrphanFilters (tagFileMap);
		}

		AlbumProfiling.getInstance ().exit (5);

//		if (_runContinuous && _Debug) {
		if (_Debug) {
			AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ true);
			_log.debug ("--------------- AlbumTags.run1 - done ---------------");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean databaseNeedsUpdate ()
	{
		String lastUpdateMillisStr = getStringFromConfig ("lastUpdateMillis", /*default*/ "0");
		long lastUpdateMillis = Long.valueOf (lastUpdateMillisStr);
//		_log.debug ("AlbumTags.databaseNeedsUpdate: lastUpdateMillisStr = " + _dateFormat.format (new java.util.Date (lastUpdateMillis)));

		if (lastUpdateMillis < getTagFileTime ()) {
			return true;
		}

		if (lastUpdateMillis < AlbumImageDao.getInstance ().getMaxLastUpdateFromImageFolder ()) {
			return true;
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	private long getTagFileTime ()
	{
		long tagFileTime = 0;

		Path tagFile = FileSystems.getDefault ().getPath (_tagFilename);
		try {
			FileTime fileTime = Files.getLastModifiedTime (tagFile);
			tagFileTime = fileTime.toMillis ();

		} catch (Exception ee) {
			_log.error ("AlbumTags.getTagFileTime: getLastModifiedTime failed for file \"" + _tagFilename + "\"", ee);
		}

//		_log.debug ("AlbumTags.getTagFileTime: tagFileTime = " + _dateFormat.format (new java.util.Date (tagFileTime)));

		return tagFileTime;
	}

	///////////////////////////////////////////////////////////////////////////
	private void generateAlbumBase1NameMap ()
	{
		AlbumProfiling.getInstance ().enter (5);

		//create map of subFolders and image baseNames for that subFolder
		Collection<String> subFolders = AlbumImageDao.getInstance ().getAlbumSubFolders ();
		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());
		for (final String subFolder : subFolders) {
			new Thread (() -> {
				final Collection<AlbumImage> images = AlbumImageDao.getInstance ().getImagesFromCache (subFolder);
				Set<String> base1NameSet = new HashSet<String> (); //use Set to eliminate duplicates
				for (AlbumImage image : images) {
					base1NameSet.add (image.getBaseName (/*collapseGroups*/ false));
				}

				_albumBase1NameMap.put (subFolder, base1NameSet); //no synchronized block necessary for ConcurrentHashMap

				endGate.countDown ();
			}).start ();
		}
		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.generateAlbumBase1NameMap: endGate:", ee);
		}

		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	private Map<String, Set<String>> readTagFile (String tagFilename, String tagPatternString)
	{
		AlbumProfiling.getInstance ().enter (5);

		final Map<String, Set<String>> tagFileMap = new HashMap<String, Set<String>> ();

		List<String> matchingTagLines = new ArrayList<String> ();
		StringBuffer wildTagBuffer = new StringBuffer (200);

		//open tag file (read-only, with read-sharing)
		Path tagFile = FileSystems.getDefault ().getPath (tagFilename);
		try (InputStream inputStream = Files.newInputStream (tagFile, StandardOpenOption.READ);
			 BufferedReader reader = new BufferedReader (new InputStreamReader (inputStream))) {

			//create pattern matcher
			if (!tagPatternString.endsWith ("*")) {
				tagPatternString += "*";
			}
			tagPatternString = tagPatternString.replace ("*", ".*");
			Pattern tagPattern = Pattern.compile (tagPatternString, Pattern.CASE_INSENSITIVE);

		//read file and find tag lines that match tagPatternString and one of the tag markers
			String line = new String();
			while ((line = reader.readLine()) != null) {
				Matcher matcher = tagPattern.matcher(line);
				if (matcher.matches()) {
					if (line.contains(_tagMarker1)) {
						matchingTagLines.add(line);
					}
					if (line.contains(_tagMarker2)) {
						wildTagBuffer.append(line.replaceAll("\\(.*\\)", ",")).append(",");
					}
				}
			}

		} catch (IOException ee) {
			_log.error ("AlbumTags.readTagFile: error reading tag file: " + tagFilename, ee);
			return null;
		}

		//process and add in the wild tag
		matchingTagLines.add ("*" + _tagMarker1 + processWildTag (wildTagBuffer.toString ()));

		Collections.sort (matchingTagLines, VendoUtils.caseInsensitiveStringComparator);
		int numMatchingLines = matchingTagLines.size ();
		_log.debug ("AlbumTags.readTagFile: found " + numMatchingLines + " matching lines");

		//add taglines to tagFileMap
		for (String tagLine : matchingTagLines) {
			String[] parts = tagLine.split (_tagMarker1);
			String tag = parts[0].trim ();
			String[] filterArray = /*VendoUtils.trimArrayItems*/ (parts[1].split (", ")); //require comma+space as separator

			checkForIncorrectSorting (tag, filterArray);
			checkForDuplicateEntries (tag, filterArray);

			Set<String> filterSet = new HashSet<String> (); //use Set to prevent duplicates in tag file from causing problems downstream

			//if tag already exists in tagFileMap, add to it
			if (tagFileMap.containsKey (tag)) {
				filterSet = tagFileMap.get (tag);
			}

			filterSet.addAll (Arrays.asList (filterArray));

			if (filterSet.size () > 0) {
				tagFileMap.put (tag, filterSet);
			}
		}

		//update "lastUpdateMillis" value in database
		long nowInMillis = new GregorianCalendar ().getTimeInMillis ();
		String nowInMillisStr = String.valueOf (nowInMillis);
		insertStringToConfig ("lastUpdateMillis", nowInMillisStr);

		_log.debug ("AlbumTags.readTagFile: set lastUpdateMillis to: " + nowInMillisStr + " (" + _dateFormat.format (new java.util.Date (nowInMillis)) + ")");

		AlbumProfiling.getInstance ().exit (5);

		return tagFileMap;
	}

	///////////////////////////////////////////////////////////////////////////
	private Map<String, Set<String>> readTagDatabase ()
	{
		AlbumProfiling.getInstance ().enter (5);

		final Map<String, Set<String>> tagDatabaseMap = new HashMap<String, Set<String>> ();

		String sql = "select tag, filters from tags_filters";
		final Map<String, String> tempMap = getStringMapFromDatabase (sql);

		for (String tag : tempMap.keySet ()) {
			String[] filterArray = /*VendoUtils.trimArrayItems*/ (tempMap.get (tag).split (", ")); //require comma+space as separator
			Set<String> filters = new HashSet<String> (Arrays.asList (filterArray));
			tagDatabaseMap.put (tag, filters);
		}

		AlbumProfiling.getInstance ().exit (5);

		return tagDatabaseMap;
	}

	///////////////////////////////////////////////////////////////////////////
	//note this is called once for each tag
	private void generateTagData (final String tag, final Collection<String> inFilterSet)
	{
		AlbumProfiling.getInstance ().enter (1);

//		_log.debug ("AlbumTags.generateTagData: tag = \"" + tag + "\", inFilterSet.size = " + inFilterSet.size ());
//		_log.debug ("AlbumTags.generateTagData: inFilterSet = \"" + AlbumImages.arrayToString (inFilterSet) + "\"");

		//add this tag to Collection
		_tags.add (tag);


//create map of subFolders and filter strings that match that subFolder
		Collection<String> subFolders = AlbumImageDao.getInstance ().getAlbumSubFolders ();
		final HashMap<String, Collection<String>> filterStringMap = new HashMap<String, Collection<String>> (subFolders.size ());
{
		AlbumProfiling.getInstance ().enter (5, "part1");
		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());
		for (final String subFolder : subFolders) {

			new Thread (() -> {
				final List<String> outFilterList = new ArrayList<String> ();
				for (String inFilter : inFilterSet) {
					if (inFilter.length () > 0) {
						String firstLower = inFilter.substring (0, 1).toLowerCase ();
						if (subFolder.compareTo (firstLower) == 0 || "*".compareTo (firstLower) == 0) {
							outFilterList.add (inFilter);
						}
					}
				}
//				_log.debug ("AlbumTags.generateTagData: " + subFolder + ": outFilterList.size() = " + outFilterList.size ());

				if (outFilterList.size () > 0) {
					Collections.sort (outFilterList, VendoUtils.caseInsensitiveStringComparator);

					synchronized (filterStringMap) {
						filterStringMap.put (subFolder, outFilterList);
					}
				}
				endGate.countDown ();
			}).start ();
		}
		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.generateTagData: endGate:", ee);
		}
		AlbumProfiling.getInstance ().exit (5, "part1");
}

//TODO skip folders that have no associated filters


//create map of subFolders and AlbumFileFilter objects that match that subFolder
		final HashMap<String, AlbumFileFilter> filterMap = new HashMap<String, AlbumFileFilter> (filterStringMap.size ());
{
		AlbumProfiling.getInstance ().enter (5, "part2");
		final CountDownLatch endGate = new CountDownLatch (filterStringMap.size ());
		for (final String subFolder : filterStringMap.keySet ()) {
			final Collection<String> filterStrings = filterStringMap.get (subFolder);

			if (filterStrings != null) {
				new Thread (() -> {
					final String filters[] = filterStrings.toArray (new String[] {});
					final AlbumFileFilter filter = new AlbumFileFilter (filters, null, /*useCase*/ false, /*sinceInMillis*/ 0);

					checkForMalformedFilters (filters); //prints any malformed filters

					synchronized (filterMap) {
						filterMap.put (subFolder, filter);
					}
					endGate.countDown ();
				}).start ();
			}
		}
		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.generateTagData: endGate:", ee);
		}
		AlbumProfiling.getInstance ().exit (5, "part2");
}
		filterStringMap.clear ();


//create map of subFolders and images that match the filters in filterMap
		final HashMap<String, Collection<String>> matchingBase1NameMap = new HashMap<String, Collection<String>> (filterMap.size ());
		final HashMap<String, Collection<String>> matchingBase2NameMap = new HashMap<String, Collection<String>> (filterMap.size ());
{
		AlbumProfiling.getInstance ().enter (5, "part3");
		final CountDownLatch endGate = new CountDownLatch (filterMap.size ());
		for (final String subFolder : filterMap.keySet ()) {
			final AlbumFileFilter filter = filterMap.get (subFolder);

//TODO - this looks broken: if filter IS null then endGate.countDown() is never called below
			if (filter != null) {
				new Thread (() -> {
					final Collection<String> folderBase1Names = _albumBase1NameMap.get (subFolder);
					final Set<String> outBase1Names = new HashSet<String> ();
					final Set<String> outBase2Names = new HashSet<String> ();
					for (String base1Name : folderBase1Names) {
						if (filter.accept (null, base1Name)) {
							outBase1Names.add (base1Name);

							//now convert the name from "collapseGroups = false" to "collapseGroups = true"
							String base2Name = base1Name.replaceAll ("[0-9]", "");
							outBase2Names.add (base2Name);
						}
					}

					if (outBase1Names.size () == 0 && !filter.isAllFolders ()) {
						printWithColor (_alertColor, "orphan filter @1 for " + subFolder + ": " + filter);
					}
					if ((filter.getMinItemCount ()) > outBase1Names.size () && !filter.isAllFolders ()) {
						printWithColor (_alertColor, "orphan filter @2 for " + subFolder + ": " + filter);
					}

					if (outBase1Names.size () > 0) {
						synchronized (matchingBase1NameMap) {
							matchingBase1NameMap.put (subFolder, outBase1Names);
						}
						synchronized (matchingBase2NameMap) {
							matchingBase2NameMap.put (subFolder, outBase2Names);
						}
					}
					endGate.countDown ();
				}).start ();
			}
		}

		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.generateTagData: endGate:", ee);
		}
		AlbumProfiling.getInstance ().exit (5, "part3");
}
		filterMap.clear ();

		//add all base1Name rows to Collection
		AlbumProfiling.getInstance ().enter (5, "part4");
		for (final String subFolder : matchingBase1NameMap.keySet ()) {
			_base1Names.addAll (matchingBase1NameMap.get (subFolder));
		}
		AlbumProfiling.getInstance ().exit (5, "part4");

		//add all base2Name rows to Collection
		AlbumProfiling.getInstance ().enter (5, "part5");
		for (final String subFolder : matchingBase2NameMap.keySet ()) {
			_base2Names.addAll (matchingBase2NameMap.get (subFolder));
		}
		AlbumProfiling.getInstance ().exit (5, "part5");

		//add all rawName rows to Collection
		AlbumProfiling.getInstance ().enter (5, "part6");
		_rawNames.addAll (inFilterSet);
		AlbumProfiling.getInstance ().exit (5, "part6");

		//add all base1Name tag rows to Collection
		AlbumProfiling.getInstance ().enter (5, "part7");
		for (final String subFolder : matchingBase1NameMap.keySet ()) {
			final Collection<String> base1Names = matchingBase1NameMap.get (subFolder);
			for (String base1Name : base1Names) {
				_base1NameTags.add (new NameTag (base1Name, tag));
			}
		}
		AlbumProfiling.getInstance ().exit (5, "part7");
		matchingBase1NameMap.clear ();

		//add all base2Name tag rows to Collection
		AlbumProfiling.getInstance ().enter (5, "part8");
		for (final String subFolder : matchingBase2NameMap.keySet ()) {
			final Collection<String> base2Names = matchingBase2NameMap.get (subFolder);
			for (String base2Name : base2Names) {
				_base2NameTags.add (new NameTag (base2Name, tag));
			}
		}
		AlbumProfiling.getInstance ().exit (5, "part8");
		matchingBase2NameMap.clear ();

		//add all rawName / tag rows to Collection
		AlbumProfiling.getInstance ().enter (5, "part9");
		for (String rawName : inFilterSet) {
			_rawNameTags.add (new NameTag (rawName, tag));
		}
		AlbumProfiling.getInstance ().exit (5, "part9");

		AlbumProfiling.getInstance ().exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private void dumpTagData (Collection<String> tagsIn)
	{
		final String dumpFile = "tagData.dump.txt"; //note relative path

		PrintStream out;
		try {
			out = new PrintStream (new File (dumpFile));

		} catch (Exception ee) {
			_log.error ("AlbumTags.dumpTagData: error accessing file \"" + dumpFile + "\"", ee);
			return;
		}

		//disable profiling for this task
		int saveLogLevel = AlbumFormInfo._logLevel;
		AlbumFormInfo._logLevel = 1;
		for (String tagIn : tagsIn) {
			Collection<String> rawNames = getNamesForTags (/*useCase*/ false, new String[] {tagIn});
			String string = VendoUtils.collectionToString (rawNames);
			out.println (tagIn + _tagMarker1 + string + ", ");
		}
		AlbumFormInfo._logLevel = saveLogLevel;

		String filename = FileSystems.getDefault ().getPath (dumpFile).toAbsolutePath ().toString ();
		System.out.println ("Output written to: " + filename);
	}

	///////////////////////////////////////////////////////////////////////////
	//process string and return sorted string
	private String processWildTag (String wildTagRawString)
	{
		wildTagRawString = wildTagRawString.replaceAll (" == ", ",").trim ()
										   .replaceAll (" = ", ",").trim ()
										   .replaceAll (",,", ",").trim ();
		String[] wildTags = VendoUtils.trimArrayItems (wildTagRawString.split (","));
		List<String> wildTagList = Arrays.asList (wildTags);
		wildTagList.sort ((s1, s2) -> s1.compareToIgnoreCase(s2));
		String wildTagString = VendoUtils.arrayToString (wildTagList.toArray (new String[] {}), ", "); //caller requires comma+space as separator

//		_log.debug ("AlbumTags.processWildTag: wildTag: " + wildTagString);

		return wildTagString;
	}

	///////////////////////////////////////////////////////////////////////////
	private void checkForIncorrectSorting (String tag, String[] filterArrayOrig)
	{
//		AlbumProfiling.getInstance ().enter/*AndTrace*/ (5);

		String filterStringOrig = Arrays.toString (filterArrayOrig);

		String[] filterArraySorted = Arrays.copyOf (filterArrayOrig, filterArrayOrig.length);
		Arrays.sort (filterArraySorted, VendoUtils.caseInsensitiveStringComparator);
		String filterStringSorted = Arrays.toString (filterArraySorted);

		if (filterStringOrig.compareTo (filterStringSorted) != 0) {
			printWithColor (_alertColor, "----------- " + tag + " -----------");

			int firstDiff = 0;
			for (int ii = 0; ii < filterStringOrig.length () && ii < filterStringSorted.length (); ii++) {
				if (filterStringOrig.charAt (ii) != filterStringSorted.charAt (ii)) {
					firstDiff = ii;
					while (filterStringOrig.charAt (firstDiff) != ' ' && firstDiff > 0) { //move back to most recent space char
						--firstDiff;
					}
					break;
				}
			}

			printWithColor (_alertColor, filterStringOrig.substring (firstDiff, Math.min (firstDiff + 60, filterStringOrig.length ())));
			printWithColor (_alertColor, filterStringSorted.substring (firstDiff, Math.min (firstDiff + 60, filterStringSorted.length ())));
		}

//		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	private void checkForDuplicateEntries (String tag, String[] filterArray)
	{
//		AlbumProfiling.getInstance ().enter/*AndTrace*/ (5);

		//complete list
		List<String> firstList = new ArrayList<String> (Arrays.asList (filterArray));
		Collections.sort (firstList, VendoUtils.caseInsensitiveStringComparator);

		//pass through set to deduplicate
		Set<String> set = new HashSet<String> (Arrays.asList (filterArray));
		List<String> secondList = new ArrayList<String> ();
		secondList.addAll (set);
		Collections.sort (secondList, VendoUtils.caseInsensitiveStringComparator);

//		firstList.removeAll (secondList); //can't use this because it removes dups, so do it by hand
		for (String item : secondList) {
			firstList.remove (item); //only removes first occurence
		}

		if (firstList.size () > 0) {
			printWithColor (_alertColor, "found duplicate filters for tag: " + tag + ": " + firstList);
		}

//		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	private void checkForMalformedFilters (String[] filters)
	{
//Note this process could be improved by de-duplicating the list before checking for patterns -- but it seems fast enough that it might not be worth it

//		_log.debug ("AlbumTags.checkForMalformedFilters: filters = " + VendoUtils.arrayToString (filters));

		final String whiteList = "[0-9A-Za-z\\+\\[\\]\\*]"; //all valid characters

		final Pattern requiredPattern1 = Pattern.compile (".*[0-9][0-9].*"); //at least two consecutive digits
		final Pattern requiredPattern2 = Pattern.compile ("[A-Z].*[A-Z].*"); //at least two uppercase letters

		final Pattern badPattern1 = Pattern.compile (".*[A-Z]$"); //bad: ends with uppercase letter
		final Pattern badPattern2 = Pattern.compile (".*[0-9].*[A-Za-z]"); //bad: any letters follow any numbers
		final Pattern badPattern3 = Pattern.compile (".*\\+.*\\+.*"); //bad: more than one plus sign
		final Pattern badPattern4 = Pattern.compile (".*\\+[0-9A-Za-z].*"); //bad: and alpha or number following plus sign

		for (String filter : filters) {
			if (filter.replaceAll (whiteList, "").length () > 0) {
				printWithColor (_alertColor, "malformed filter @0: " + filter);
			}

			boolean required1 = requiredPattern1.matcher (filter).matches ();
			boolean required2 = requiredPattern2.matcher (filter).matches ();
			boolean required3 = filter.endsWith ("+");
			boolean required4 = filter.endsWith ("*");
			boolean required5 = filter.startsWith ("xbf_");

			//at least one of these must be true
			if (! (required1 || required2 || required3 || required4 || required5)) {
				printWithColor (_alertColor, "malformed filter @1: " + filter);
			}

			boolean bad1 = badPattern1.matcher (filter).matches ();
			boolean bad2 = badPattern2.matcher (filter).matches ();
			boolean bad3 = badPattern3.matcher (filter).matches ();
			boolean bad4 = badPattern4.matcher (filter).matches ();

			//none of these must be true
			if (bad1 || bad2 || bad3 || bad4) {
				printWithColor (_alertColor, "malformed filter @2: " + filter);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void checkForMismatchedFilters ()
	{
//		AlbumProfiling.getInstance ().enterAndTrace (5);

		final Pattern pattern1 = Pattern.compile (".*[0-9]$"); //ends with digit
		final Pattern pattern2 = Pattern.compile (".*\\]$"); //ends with square bracket
		final Pattern pattern3 = Pattern.compile (".*\\+$"); //ends with plus sign
		final Pattern pattern4 = Pattern.compile (".*[A-Za-z]$"); //ends with alpha (case insensitive)

		List<String> misMatchedFilters1 = new ArrayList<String> ();
		Set<String> rawSet = new HashSet<String> ();
		for (String rawName : _rawNames) {
			boolean match1 = pattern1.matcher (rawName).matches ();
			boolean match2 = pattern2.matcher (rawName).matches ();
			boolean match3 = pattern3.matcher (rawName).matches ();
			boolean match4 = pattern4.matcher (rawName).matches ();

			if (match1 || match2) {
				continue;
			}

			if (match3) {
				String rawNameRemovePlus = rawName.substring (0, rawName.length () - 1);
				if (rawSet.contains (rawNameRemovePlus)) {
					misMatchedFilters1.add (rawNameRemovePlus);
				}

			} else if (match4) {
				String rawNameAddPlus = rawName + "+";
				if (rawSet.contains (rawNameAddPlus)) {
					misMatchedFilters1.add (rawName);
				}

			} else {
				//catches filters that end with e.g., "*"
//				misMatchedFilters1.add (rawName);
			}

			rawSet.add (rawName);
		}

		List<String> misMatchedFilters2 = misMatchedFilters1.stream ()
															.map (v -> v + "+, " + v)
															.sorted (VendoUtils.caseInsensitiveStringComparator)
															.distinct ()
															.collect (Collectors.toList ());

//		_log.debug ("AlbumTags.checkForMismatchedFilters: misMatchedFilters2.size() = " + misMatchedFilters2.size ());
		for (String misMatchedFilter : misMatchedFilters2) {
			printWithColor (_alertColor, "mismatched filter: " + misMatchedFilter);
		}

//		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	private void checkForOrphanFilters (Map<String, Set<String>> tagMap)
	{
		AlbumProfiling.getInstance ().enterAndTrace (5);

		List<TagFilter1> tagFilters = new ArrayList<TagFilter1> ();
		for (String tag : tagMap.keySet ()) {
			for (String filterString : tagMap.get (tag)) {
				AlbumFileFilter filter = new AlbumFileFilter (new String[] {filterString}, null, /*useCase*/ false, /*sinceInMillis*/ 0);
				tagFilters.add (new TagFilter1 (tag, filter));
			}
		}

		final int maxThreads = 10 * VendoUtils.getLogicalProcessors ();
		final int minPerThread = 1000;
		final int chunkSize = AlbumImages.calculateChunk (maxThreads, minPerThread, tagFilters.size ()).getFirst ();
		List<List<TagFilter1>> tagFilterChunks = ListUtils.partition (tagFilters, chunkSize);
		final int numChunks = tagFilterChunks.size ();
		_log.debug ("AlbumTags.checkForOrphanFilters: numChunks = " + numChunks + ", chunkSize = " + _decimalFormat2.format (chunkSize));

		List<String> allBase1Names = new ArrayList<String> ();
		for (String subFolder : _albumBase1NameMap.keySet ()) {
			allBase1Names.addAll (_albumBase1NameMap.get (subFolder));
		}

		final ExecutorService executor = AlbumImages.getExecutor ();
		final CountDownLatch endGate = new CountDownLatch (numChunks);

		HashMap<String, String> orphanMap = new HashMap<String, String> ();

		for (final List<TagFilter1> filterChunk : tagFilterChunks) {
			Runnable task = () -> {
				for (final TagFilter1 tagFilter : filterChunk) {
					AlbumFileFilter filter = tagFilter._filter;
					int minExpectedMatches = filter.getMinItemCount ();

					int foundMatches = 0;
					Set<String> remainingFiles = new HashSet<String> (); //use set to eliminate dups
					for (String base1Name : allBase1Names) {
						if (filter.accept (null, base1Name)) {
							foundMatches++;
							remainingFiles.add (base1Name);
							if (foundMatches >= minExpectedMatches) {
								break;
							}
						}
					}

					if (foundMatches < minExpectedMatches) {
						synchronized (orphanMap) {
							String value = orphanMap.get (tagFilter._filter.toString ());
							if (value == null) {
								value = tagFilter._tag;
							} else {
								value += ", " + tagFilter._tag;
							}

							orphanMap.put (tagFilter._filter.toString (), value);
							printWithColor (_warningColor, "orphan filter: " + tagFilter._filter.toString () + ", tag: " + value +
												", remaining files: " + Arrays.toString (remainingFiles.toArray ()));
						}
					}
				}
				endGate.countDown ();
			};
			executor.execute (task);
		}
		_log.debug ("AlbumTags.checkForOrphanFilters: queued " + _decimalFormat2.format (numChunks) + " threads");

		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.checkForOrphanFilters: endGate:", ee);
		}

		List<String> orphanList = orphanMap.keySet ().stream ()
													 .map (v -> orphanMap.get (v) + ": " + v)
													 .sorted (VendoUtils.caseInsensitiveStringComparator)
													 .collect (Collectors.toList ());

//		_log.debug ("AlbumTags.checkForOrphanFilters: orphanList.size() = " + orphanList.size ());
		for (String orphan : orphanList) {
			printWithColor (_alertColor, "orphan filter: " + orphan);
		}

		AlbumProfiling.getInstance ().exit (5);
	}


	///////////////////////////////////////////////////////////////////////////
	//remove everything in collection2 from collection1
	public static <T> Collection<T> subtractCollections (Collection<T> collection1, Collection<T> collection2)
	{
		Collection<T> newItems = new ArrayList<T> ();

		try { //handle case where collection is null
			newItems = new ArrayList<T> (collection1.size ());
			newItems.addAll (collection1); //copy values so we don't modify source collection
			newItems.removeAll (collection2);

		} catch (Exception ee) {
		}

		return newItems;
	}

	///////////////////////////////////////////////////////////////////////////
	private void updateTagDatabase (Map<String, Set<String>> tagFileMap)
	{
		///////////////////////////////////////////////////////////////////////
		//run first set of inserts
		AlbumProfiling.getInstance ().enterAndTrace (5, "updateTagDatabase.part1");

		final int numStepsPart1 = 5;
		final CountDownLatch endGate1 = new CountDownLatch (numStepsPart1);

		new Thread (() -> {
			writeTagDatabase (tagFileMap);
			endGate1.countDown ();
		}).start ();

		new Thread (() -> {
			insertTags (_tags);
			endGate1.countDown ();
		}).start ();

		new Thread (() -> {
			insertBase1Names (_base1Names);
			endGate1.countDown ();
		}).start ();

		new Thread (() -> {
			insertBase2Names (_base2Names);
			endGate1.countDown ();
		}).start ();

		new Thread (() -> {
			insertRawNames (_rawNames);
			endGate1.countDown ();
		}).start ();

		//wait for first set of inserts to complete
		try {
			endGate1.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.updateTagDatabase: endGate1:", ee);
		}
		AlbumProfiling.getInstance ().exit (5, "updateTagDatabase.part1");

		///////////////////////////////////////////////////////////////////////
		//run second set of inserts
		AlbumProfiling.getInstance ().enterAndTrace (5, "updateTagDatabase.part2");

		final int maxThreads = 3 * VendoUtils.getLogicalProcessors ();
		final int minPerThread = 1000;
		final int chunkSize = AlbumImages.calculateChunk (maxThreads, minPerThread, _base1NameTags.size ()).getFirst ();
		final List<List<NameTag>> base1NameTagsList = ListUtils.partition ((List<NameTag>) _base1NameTags, chunkSize);
		final int numChunks = base1NameTagsList.size ();
		_log.debug ("AlbumTags.updateTagDatabase: numChunks = " + numChunks + ", chunkSize = " + _decimalFormat2.format (chunkSize));

		final int numStepsPart2 = 2 + numChunks;
		final CountDownLatch endGate2 = new CountDownLatch (numStepsPart2);

		for (final List<NameTag> base1NameTags : base1NameTagsList) {
			new Thread (() -> {
				insertBase1NameTags (base1NameTags);
				endGate2.countDown ();
			}).start ();
		}

		new Thread (() -> {
			insertBase2NameTags (_base2NameTags);
			endGate2.countDown ();
		}).start ();

		new Thread (() -> {
			insertRawNameTags (_rawNameTags);
			endGate2.countDown ();
		}).start ();

		//wait for second set of inserts to complete
		try {
			endGate2.await ();
		} catch (Exception ee) {
			_log.error ("AlbumTags.updateTagDatabase: endGate2:", ee);
		}
		AlbumProfiling.getInstance ().exit (5, "updateTagDatabase.part2");
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertTags (Collection<String> tags)
	{
		String sql = "insert ignore into tags (tag) values";
		return insertStringsIntoTable (sql, "(?)", /*numColumns*/ 1, VendoUtils.dedupCollection (tags));
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertBase1Names (Collection<String> names)
	{
		String sql = "insert ignore into base1_names (name) values";
		return insertStringsIntoTable (sql, "(?)", /*numColumns*/ 1, VendoUtils.dedupCollection (names));
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertBase2Names (Collection<String> names)
	{
		String sql = "insert ignore into base2_names (name) values";
		return insertStringsIntoTable (sql, "(?)", /*numColumns*/ 1, VendoUtils.dedupCollection (names));
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertRawNames (Collection<String> names)
	{
		String sql = "insert ignore into raw_names (name) values";
		return insertStringsIntoTable (sql, "(?)", /*numColumns*/ 1, VendoUtils.dedupCollection (names));
	}

	///////////////////////////////////////////////////////////////////////////
	//insert _batchInsertSize rows at a time; caller should use 'insert ignore' to avoid any problems with duplicates
	private int insertStringsIntoTable (String sqlBase, String sqlValues, int numColumns, Collection<String> items)
	{
//TODO: validate that collection size is multiple of numColumns

//		AlbumProfiling.getInstance ().enter (5);

		String sql = sqlBase + sqlValues;
//		_log.debug ("AlbumTags.insertStringsIntoTable: sql: " + sql);

		int rowsInserted = 0;
		try (Connection connection = getConnection ();
			 PreparedStatement statement = connection.prepareStatement (sql)) {
//			_log.debug ("AlbumTags.insertStringsIntoTable: connection: " + connection);

			connection.setAutoCommit (false);

			int batchCount = 0;
 			Iterator<String> iter = items.iterator ();
			while (iter.hasNext ()) {
	        	for (int col = 0; col < numColumns; col++) {
		        	statement.setString (1 + col, iter.next ());
	        	}
	        	statement.addBatch ();
	        	batchCount++;

	        	if (batchCount % _batchInsertSize == 0 || !iter.hasNext ()) {
	        		int [] updateCounts = statement.executeBatch ();
	        		rowsInserted += Arrays.stream (updateCounts).sum ();

//        			AlbumProfiling.getInstance ().enter (5, "commit");
	       			connection.commit ();
//	       			AlbumProfiling.getInstance ().exit (5, "commit");
	        	}
	        }

	        connection.setAutoCommit (true); //TODO - should this be done in finally block?

		} catch (MySQLIntegrityConstraintViolationException ee) {
			//ignore as this will catch any duplicate insertions

		} catch (Exception ee) {
			_log.error ("AlbumTags.insertStringsIntoTable:", ee);
			_log.error ("AlbumTags.insertStringsIntoTable: sql:" + NL + sql);
		}

//		_log.debug ("AlbumTags.insertStringsIntoTable: rowsInserted: " + rowsInserted);

//		AlbumProfiling.getInstance ().exit (5);

		return rowsInserted;
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertBase1NameTags (Collection<NameTag> nameTags)
	{
		return insertNamesTags ("base1", nameTags);
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertBase2NameTags (Collection<NameTag> nameTags)
	{
		return insertNamesTags ("base2", nameTags);
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertRawNameTags (Collection<NameTag> nameTags)
	{
		return insertNamesTags ("raw", nameTags);
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertNamesTags (String tablePrefix, Collection<NameTag> nameTags)
	{
		if (nameTags.size () == 0)
			return 0;

//		_log.debug ("AlbumTags.insertNamesTags: nameTags.size = " + nameTags.size ());

//		AlbumProfiling.getInstance ().enter (5);

		String sql = "insert ignore into " + tablePrefix + "_names_tags (name_id, tag_id) values";

		String sqlValues = "((select name_id from " + tablePrefix + "_names where name = ?)," +
						   " (select tag_id from tags where tag = ?))";

		Collection<String> items = new ArrayList<String> (nameTags.size ());

		for (NameTag nameTag : nameTags) {
			items.add (nameTag._name);
			items.add (nameTag._tag);
		}

		int rowsInserted = insertStringsIntoTable (sql, sqlValues, /*numColumns*/ 2, items);

//		AlbumProfiling.getInstance ().exit (5);

		return rowsInserted;
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertTagsFilters (Collection<TagFilter2> tagFilters)
	{
		if (tagFilters.size () == 0)
			return 0;

//
		_log.debug ("AlbumTags.insertTagsFilters: tagFilters.size = " + tagFilters.size ());

//		AlbumProfiling.getInstance ().enter (5);

		String sql = "insert ignore into tags_filters (tag, filters) values";

		String sqlValues = "(?, ?)";

		Collection<String> items = new ArrayList<String> (tagFilters.size ());

		for (TagFilter2 tagFilter : tagFilters) {
			items.add (tagFilter._tag);
			items.add (tagFilter._filter);
		}

		int rowsInserted = insertStringsIntoTable (sql, sqlValues, /*numColumns*/ 2, items);

//		AlbumProfiling.getInstance ().exit (5);

		return rowsInserted;
	}

	///////////////////////////////////////////////////////////////////////////
	private int insertStringToConfig (String name, String value)
	{
		String sql = "replace into config (name, string_value) values";

		String sqlValues = "(?, ?)";

		Collection<String> items = new ArrayList<String> (2);

		items.add (name);
		items.add (value);

		int rowsInserted = insertStringsIntoTable (sql, sqlValues, /*numColumns*/ 2, items);

		return rowsInserted;
	}

	///////////////////////////////////////////////////////////////////////////
	private void writeTagDatabase (Map<String, Set<String>> tagMap)
	{
		AlbumProfiling.getInstance ().enter (5);

		resetTable ("tags_filters");

		Set<TagFilter2> tagFilters = new HashSet<TagFilter2> ();

		for (String tag : tagMap.keySet ()) {
			List<String> filterList = new ArrayList<String> (tagMap.get (tag));
			Collections.sort (filterList, VendoUtils.caseInsensitiveStringComparator);
			String filterStr = VendoUtils.arrayToString (filterList.toArray (new String[] {}));
			tagFilters.add (new TagFilter2 (tag, filterStr));
		}

		insertTagsFilters (tagFilters);

		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	//returns defaultValue on exception or failure
	private String getStringFromConfig (String name, String defaultValue)
	{
		String sql = "select string_value from config where name = '" + name + "'";

		String value = defaultValue;
		try {
			Collection<String> strings = getObjectsFromDatabase (sql, new String ());
			value = strings.toArray (new String[] {})[0];

		} catch (Exception ee) {
			//ignore
		}

		return value;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet: returns empty collection on error, or if no entries found
	public Collection<String> getAllTags ()
	{
		String sql = "select tag from tags order by lower(tag)";
		return getObjectsFromDatabase (sql, new String ());
	}

	///////////////////////////////////////////////////////////////////////////
	//assumes important data will be returned as type T in column 1
	//used by servlet (indirectly): returns empty collection on error, or if no entries found
	@SuppressWarnings("unchecked")
	private <T> Collection<T> getObjectsFromDatabase (String sql, T object)
	{
		AlbumProfiling.getInstance ().enter (5);

		Collection<T> items = new LinkedList<T> ();

		try (Connection connection = getConnection ();
			Statement statement = connection.createStatement ();
			ResultSet rs = statement.executeQuery (sql)) {

			while (rs.next ()) {
				if (object instanceof String) {
					items.add ((T) rs.getString (1));
				} else if (object instanceof Integer) {
					items.add ((T) new Integer (rs.getInt (1)));
				} else {
					throw new RuntimeException ("AlbumTags.getObjectsFromDatabase: unhandled type: " + object.getClass ().getName ());
				}
			}

		} catch (Exception ee) {
			_log.error ("AlbumTags.getObjectsFromDatabase:", ee);
			_log.error ("AlbumTags.getObjectsFromDatabase: sql:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns empty map on error, or if no entries found
	private Map<String, String> getStringMapFromDatabase (String sql)
	{
		String id = massageSqlToId (sql);

		AlbumProfiling.getInstance ().enter (5, id);

		Map<String, String> items = new HashMap<String, String> ();

		try (Connection connection = getConnection ();
			 Statement statement = connection.createStatement ();
			 ResultSet rs = statement.executeQuery (sql)) {

			while (rs.next ()) {
				String key = rs.getString (1);
				String value = rs.getString (2);
				items.put (key, value);
			}

		} catch (Exception ee) {
			_log.error ("AlbumTags.getStringMapFromDatabase:", ee);
			_log.error ("AlbumTags.getStringMapFromDatabase: sql:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5, id);

		_log.debug ("AlbumTags.getStringMapFromDatabase(\"" + id + "\"): items.size() = " + items.size ());

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	private int getCountFromDatabase (String table)
	{
		String sql = "select count(*) from " + table;
		Collection<Integer> items = getObjectsFromDatabase (sql, new Integer (0));

		int count = 0;
		if (items.size () > 0) {
			Integer[] integers = items.toArray (new Integer[] {});
			count = integers[0];
		}

		return count;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet: returns empty string on error, or if no entries found
	public String getTagsForBaseName (String baseName, boolean collapseGroups)
	{
		List<String> baseNames = new ArrayList<String> ();
		baseNames.add (baseName);
		return getTagsForBaseNames (baseNames, collapseGroups);
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet: returns empty string on error, or if no entries found
	public String getTagsForBaseNames (Collection<String> baseNames, boolean collapseGroups)
	{
		AlbumProfiling.getInstance ().enter (5);

		String tablePrefix = (collapseGroups ? "base2" : "base1");

		String sql1 = "select distinct tag" + NL +
					  "from " + tablePrefix + "_names bn" + NL +
					  "join " + tablePrefix + "_names_tags bnt on bn.name_id = bnt.name_id" + NL +
					  "join tags t on bnt.tag_id = t.tag_id" + NL +
					  "where bn.name in (?";
		String sql2 = new String ();
		String sql3 = ")" + NL + " order by lower(tag)";

		int numBaseNames = baseNames.size ();
		for (int ii = 1; ii < numBaseNames; ii++) {
			sql2 += ", ?";
		}

		String sql = sql1 + sql2 + sql3;
		List<String> tags = new ArrayList<String> ();

		ResultSet rs = null;
		try (Connection connection = getConnection ();
			 PreparedStatement ps = connection.prepareStatement (sql)) {

			int baseNameIndex = 0;
			for (String baseName : baseNames) {
				ps.setString (++baseNameIndex, baseName);
			}

//			String finalSql = ps.toString ().replaceFirst (" ", NL);
//			_log.debug ("AlbumTags.getTagsForBaseNames: sql: " + finalSql);

			rs = ps.executeQuery ();

			while (rs.next ()) {
				tags.add (rs.getString ("tag"));
			}

		} catch (Exception ee) {
			_log.error ("AlbumTags.getTagsForBaseNames:", ee);
			_log.error ("AlbumTags.getTagsForBaseNames: sql:" + NL + sql);

		} finally {
			if (rs != null) try { rs.close (); } catch (SQLException ex) { _log.error ("Error", ex); }
		}

		//truncate the list
		int maxTagsShown = 30;
		VendoUtils.truncateList (tags, maxTagsShown);
		String tagStr = VendoUtils.arrayToString (tags.toArray (new String[] {}));

		AlbumProfiling.getInstance ().exit (5);

		return tagStr;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet: returns empty string array on error, or if no entries found
	//if 'filters' are passed in, this will "OR" (union) all filters together, then "AND" (intersect) the result against all tags
	public Collection<String> getNamesForTags (boolean useCase, String[] tagsIn)
	{
		return getNamesForTags (useCase, tagsIn, new String[] {}, new String[] {});
	}
	public Collection<String> getNamesForTags (boolean useCase, String[] tagsIn, String[] tagsNotIn)
	{
		return getNamesForTags (useCase, tagsIn, tagsNotIn, new String[] {});
	}
	public Collection<String> getNamesForTags (boolean useCase, String[] tagsIn, String[] tagsNotIn, String[] filters)
	{
		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumTags.getNamesForTags: tagsIn = \"" + VendoUtils.arrayToString (tagsIn) + "\"");
			_log.debug ("AlbumTags.getNamesForTags: tagsNotIn = \"" + VendoUtils.arrayToString (tagsNotIn) + "\"");
			_log.debug ("AlbumTags.getNamesForTags: filters = \"" + VendoUtils.arrayToString (filters) + "\"");
		}

		Collection<String> baseNames = new ArrayList<String> ();

		int numTagsIn = tagsIn.length;
		int numTagsNotIn = tagsNotIn.length;
		int numFilters = filters.length;
		int numItemsIn = numTagsIn + (numFilters > 0 ? 1 : 0);

//TODO - fix this limitation
		//need to have at least one tagIn to proceed
		if (numTagsIn < 1) {
			return baseNames;
		}

		AlbumProfiling.getInstance ().enter (5);

		String tablePrefix = (numItemsIn == 1 ? "raw" : "base1");

		String sqlTagInStub = "		(select name_id from " + tablePrefix + "_names_tags" + NL +
							  "			inner join tags on " + tablePrefix + "_names_tags.tag_id = tags.tag_id" + NL +
							  "				where tags.tag_id = (" + NL +
							  "					select tag_id from tags where tag = ?))" + NL;

		String sqlTagNotInStub = ") and name_id not in (" + NL +
								 sqlTagInStub;

		String sqlFilterStub = "		(select name_id from base1_names where ";

		String sqlOrderByStub = ") order by " + (useCase ? "" : "lower") + "(name)" + NL;

		for (int ii = 0; ii < numFilters; ii++) {
			sqlFilterStub += (useCase ? "" : "lower") + "(name) regexp ?"; //MySQL regular expressions require "regexp" or "rlike" (can't use "like")
			if (ii < numFilters - 1) {
				sqlFilterStub += " or ";
			}
		}
		sqlFilterStub += ")" + NL;

		String sql = new String ();

		if (numItemsIn == 1) { //can only happen for numTagsIn = 1 and numFilters = 0
			sql = "select distinct name from " + tablePrefix + "_names where name_id in (" + NL;
			sql += sqlTagInStub;

			//add tagsNotIn
			for (int ii = 0; ii < numTagsNotIn; ii++) {
				sql += sqlTagNotInStub;
			}

			sql += sqlOrderByStub;

		} else {
			sql = "select distinct name from " + tablePrefix + "_names where name_id in (" + NL +
				  "	select name_id from (" + NL;

			//add tagsIn: each tagIn gets its own union
			for (int ii = 0; ii < numTagsIn; ii++) {
				sql += sqlTagInStub;
				if (ii < numItemsIn - 1) {
					sql += "		union all" + NL;
				}
			}

			//add filters: all filters are ANDed into one union
			if (numFilters > 0) {
				sql += sqlFilterStub;
			}

			sql += "	) as tbl group by tbl.name_id having count(*) = " + numItemsIn + NL;

			//add tagsNotIn
			for (int ii = 0; ii < numTagsNotIn; ii++) {
				sql += sqlTagNotInStub;
			}

			sql += sqlOrderByStub;
		}

		ResultSet rs = null;
		try (Connection connection = getConnection ();
			 PreparedStatement ps = connection.prepareStatement (sql)) {

			int index = 1;

			for (int ii = 0; ii < numTagsIn; ii++) {
				ps.setString (index++, tagsIn[ii]);
			}

			for (int ii = 0; ii < numFilters; ii++) {
				String filter = (useCase ? filters[ii] : filters[ii].toLowerCase ());
				ps.setString (index++, AlbumFormInfo.convertWildcardsToSqlRegex (filter));
			}

			for (int ii = 0; ii < numTagsNotIn; ii++) {
				ps.setString (index++, tagsNotIn[ii]);
			}

			rs = ps.executeQuery ();

			while (rs.next ()) {
				baseNames.add (rs.getString ("name"));
			}

		} catch (Exception ee) {
			_log.error ("AlbumTags.getNamesForTags:", ee);
			_log.error ("AlbumTags.getNamesForTags: sql:" + NL + sql);

		} finally {
			if (rs != null) try { rs.close (); } catch (SQLException ex) { _log.error ("Error", ex); }
		}

		AlbumProfiling.getInstance ().exit (5);

		return baseNames;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean resetTables ()
	{
		if (AlbumFormInfo._logLevel >= 5) {
			_log.trace ("AlbumTags.resetTables");
		}

		int errorCount = 0;

		if (!resetTable("config")) {
			errorCount++;
		}
		if (!resetTable("tags")) {
			errorCount++;
		}
		if (!resetTable("base1_names")) {
			errorCount++;
		}
		if (!resetTable("base2_names")) {
			errorCount++;
		}
		if (!resetTable("raw_names")) {
			errorCount++;
		}
		if (!resetTable("base1_names_tags")) {
			errorCount++;
		}
		if (!resetTable("base2_names_tags")) {
			errorCount++;
		}
		if (!resetTable("raw_names_tags")) {
			errorCount++;
		}
//		if (resetTable ("tags_filters")) {
//			errorCount++;
//		}

		return (errorCount > 0 ? false : true);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean resetTable (String tableName)
	{
		AlbumProfiling.getInstance ().enter (7);

		boolean status = true;

		String sql = "truncate table " + tableName;

		try (Connection connection = getConnection ();
			 Statement statement = connection.createStatement ()){

			statement.executeUpdate (sql);

		} catch (Exception ee) {
			_log.error ("AlbumTags.resetTable:", ee);
			_log.error ("AlbumTags.resetTable: sql:" + NL + sql);
			status = false;
		}

		AlbumProfiling.getInstance ().exit (7);

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private Connection getConnection ()
	{
		Connection connection = null;

		try {
			connection = getDataSource ().getConnection ();

		} catch (Exception ee) {
			connection = null;
			_log.error ("AlbumTags.getConnection: error connecting to database", ee);
		}

		return connection;
	}

	///////////////////////////////////////////////////////////////////////////
	private synchronized static BasicDataSource getDataSource ()
	{
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.jdbc.Driver";
		final String dbUrl = "jdbc:mysql://localhost/albumtags";
		final String dbUser = "root";
		final String dbPass = "root";

		if (_dataSource == null) {
//			_log.debug ("AlbumTags.getDataSource: url = " + dbUrl);

			BasicDataSource ds = new BasicDataSource ();
			ds.setDriverClassName (jdbcDriver);
			ds.setUrl (dbUrl);
			ds.setUsername (dbUser);
			ds.setPassword (dbPass);

//			ds.setMinIdle (5);
//			ds.setMaxIdle (10);
//			ds.setMaxOpenPreparedStatements (100);

			_dataSource = ds;
		}

		return _dataSource;
	}

	///////////////////////////////////////////////////////////////////////////
	private String massageSqlToId (String sql)
	{
		return sql.replaceAll ("select ", " ")
				  .replaceAll ("from ", ":")
				  .replaceAll ("order.*", " ")
				  .replaceAll ("where.*", " ")
				  .replaceAll (" ", "");
	}

	///////////////////////////////////////////////////////////////////////////
	//if no console available, use logger
	public static void printWithColor (Short fg, String line)
	{
		if (VendoUtils.hasConsole ()) {
			VendoUtils.printWithColor (fg, line);

		} else {
			String header = (fg == _alertColor ? ">>>>>>>>>>" : (fg == _warningColor ? "**********" : /*fg == _highlightColor*/ "++++++++++"));
			_log.debug (header + " " + line);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static class NameTag
	{
		public NameTag (String name, String tag)
		{
			_name = name;
			_tag = tag;
		}

		@Override
		public String toString ()
		{
			return _name + ", " + _tag;
		}

		private final String _name;
		private final String _tag;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class TagFilter1
	{
		public TagFilter1 (String tag, AlbumFileFilter filter)
		{
			_tag = tag;
			_filter = filter;
		}

		@Override
		public String toString ()
		{
			return _tag + ": " + _filter;
		}

		private final String _tag;
		private final AlbumFileFilter _filter;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class TagFilter2
	{
		public TagFilter2 (String tag, String filter)
		{
			_tag = tag;
			_filter = filter;
		}

		@Override
		public String toString ()
		{
			return _tag + ": " + _filter;
		}

		private final String _tag;
		private final String _filter;
	}

	///////////////////////////////////////////////////////////////////////////
	private class RowCounts
	{
		public RowCounts () //default ctor
		{
			_numTags = getCountFromDatabase ("tags");
			_numBase1Names = getCountFromDatabase ("base1_names");
			_numBase2Names = getCountFromDatabase ("base2_names");
			_numRawNames = getCountFromDatabase ("raw_names");
			_numBase1NamesTags = getCountFromDatabase ("base1_names_tags");
			_numBase2NamesTags = getCountFromDatabase ("base2_names_tags");
			_numRawNamesTags = getCountFromDatabase ("raw_names_tags");
		}

		public RowCounts (RowCounts c0) //copy ctor
		{
			_numTags = c0._numTags;
			_numBase1Names = c0._numBase1Names;
			_numBase2Names = c0._numBase2Names;
			_numRawNames = c0._numRawNames;
			_numBase1NamesTags = c0._numBase1NamesTags;
			_numBase2NamesTags = c0._numBase2NamesTags;
			_numRawNamesTags = c0._numRawNamesTags;
		}

		public RowCounts subtract (RowCounts c0)
		{
			_numTags -= c0._numTags;
			_numBase1Names -= c0._numBase1Names;
			_numBase2Names -= c0._numBase2Names;
			_numRawNames -= c0._numRawNames;
			_numBase1NamesTags -= c0._numBase1NamesTags;
			_numBase2NamesTags -= c0._numBase2NamesTags;
			_numRawNamesTags -= c0._numRawNamesTags;

			return this;
		}

		private int _numTags;
		private int _numBase1Names;
		private int _numBase2Names;
		private int _numRawNames;
		private int _numBase1NamesTags;
		private int _numBase2NamesTags;
		private int _numRawNamesTags;
	}


	//members
	private boolean _dumpTagData = false;
	private boolean _resetTables = false;

	private static boolean _runContinuous = false; //static so it can be accessed from main()
	private static boolean _checkForOrphanFilters = false; //static so it can be accessed from main()

	private static BasicDataSource _dataSource = null;

	private int _batchInsertSize = 1000;

//	private String _rootPath = null;
	private String _tagFilename = null;
	private String _tagPatternString = null;

//	private Map<String, Collection<String>> _albumBase1NameMap = new HashMap<String, Collection<String>> ();
	private Map<String, Collection<String>> _albumBase1NameMap = new ConcurrentHashMap<String, Collection<String>> ();

	private Collection<String> _tags = new LinkedList<String> ();
	private Collection<String> _base1Names = new LinkedList<String> (); //image names with collapseGroups = false
	private Collection<String> _base2Names = new LinkedList<String> (); //image names with collapseGroups = true
	private Collection<String> _rawNames = new LinkedList<String> ();
	private Collection<NameTag> _base1NameTags = new LinkedList<NameTag> ();
	private Collection<NameTag> _base2NameTags = new LinkedList<NameTag> ();
	private Collection<NameTag> _rawNameTags = new LinkedList<NameTag> ();

	private static AlbumTags _instance = null;

	private static final short _alertColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
	private static final short _warningColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW;
	private static final short _highlightColor = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA;
	private static final String _tagMarker1 = " := ";
	private static final String _tagMarker2 = " == ";

	private static final String NL = System.getProperty ("line.separator");
	private static final DecimalFormat _decimalFormat1 = new DecimalFormat ("+#;-#"); //format integer with +/- sign
	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //format as integer
	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm:ss");

	private static boolean _Debug = false;
	private static Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumTags";
}
