//AlbumImageDiffer.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;
import com.vendo.vendoUtils.WatchDir;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AlbumImageDiffer
{
	public enum Mode {NotSet, Names, Names2, New, Tags, Tags2, Folders, Folders2, OnDemand} //OnDemand currently only used by servlet

	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args)
	{
		AlbumFormInfo.getInstance (); //call ctor to load class defaults

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
		AlbumFormInfo._profileLevel = 5;

		AlbumProfiling.getInstance ().enter/*AndTrace*/ (1);

		AlbumImageDiffer albumImageDiffer = AlbumImageDiffer.getInstance ();

		if (!albumImageDiffer.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		try {
			albumImageDiffer.run ();
		} catch (Exception ee) {
			ee.printStackTrace (System.err);
		}

		shutdownExecutor ();

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (true);
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String[] args)
	{
		String filterAStr = null;
		String filterBStr = null;
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1);

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("cleanup")) {
					_cleanUpObsoleteEntries = true;

				} else if (arg.equalsIgnoreCase ("filtersA")) {
					try {
						filterAStr = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("filtersB")) {
					try {
						filterBStr = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("maxRgbDiffs")) {
					try {
						_maxRgbDiffs = Integer.parseInt (args[++ii]);
						if (_maxRgbDiffs < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("maxStdDev")) {
					try {
						_maxStdDev = Integer.parseInt (args[++ii]);
						if (_maxStdDev < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("maxRows")) {
					try {
						_maxRows = Integer.parseInt (args[++ii]);
						if (_maxRows < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("modeA")) {
					_modeA = getMode(args[++ii]);
					if (_modeA == Mode.NotSet) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("modeB")) {
					_modeB = getMode(args[++ii]);
					if (_modeB == Mode.NotSet) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("numThreads") || arg.equalsIgnoreCase ("threads")) {
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

				} else if (arg.equalsIgnoreCase ("whereClauseA") || arg.equalsIgnoreCase ("whereA")) {
					try {
						_whereClauseA = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}


				} else if (arg.equalsIgnoreCase ("whereClauseB") || arg.equalsIgnoreCase ("whereB")) {
					try {
						_whereClauseB = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}


				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
/*
				if (_imageFilename1 == null) {
					_imageFilename1 = arg;

				} else {
*/
				displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		if (filterAStr != null) {
			_filtersA = Arrays.asList(filterAStr.split("\\s*,\\s*"));
		}
		if (filterBStr != null) {
			_filtersB = Arrays.asList(filterBStr.split("\\s*,\\s*"));
		}

		if (!_cleanUpObsoleteEntries) {
			//check for required args and handle defaults
			if (_whereClauseA == null || _whereClauseB == null) {
				displayUsage("Incorrect usage", true);
			}

			if (_modeA == Mode.NotSet || _modeB == Mode.NotSet) {
				displayUsage("Incorrect usage", true);
			}

			if (_Debug) {
				_log.debug ("AlbumImageDiffer.processArgs: filtersA = " + _filtersA);
				_log.debug ("AlbumImageDiffer.processArgs: filtersB = " + _filtersB);
				_log.debug ("AlbumImageDiffer.processArgs: maxRgbDiffs = " + _maxRgbDiffs);
				_log.debug ("AlbumImageDiffer.processArgs: maxStdDev = " + _maxStdDev);
				_log.debug ("AlbumImageDiffer.processArgs: maxRows = " + _decimalFormat.format (_maxRows));
				_log.debug ("AlbumImageDiffer.processArgs: modeA = " + _modeA);
				_log.debug ("AlbumImageDiffer.processArgs: modeB = " + _modeB);
				_log.debug ("AlbumImageDiffer.processArgs: numThreads = " + _numThreads);
				_log.debug ("AlbumImageDiffer.processArgs: whereClauseA = " + _whereClauseA);
				_log.debug ("AlbumImageDiffer.processArgs: whereClauseB = " + _whereClauseB);
			}
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

		msg += "Usage: " + _AppName + " [/debug] [/filtersA=<str>] [/filtersB=<str>] [/maxRgbDiffs=<n>] [/maxStdDev=<n>] [/maxRows=<n>] /modeA={names[2]|subFolders[2]|tags[2]} /modeB={names[2]|subFolders[2]|tags[2]} [/numThreads=<n>] [/whereClauseA=<str>] [/whereClauseB=<str>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public static AlbumImageDiffer getInstance()
	{
		if (_instance == null) {
			synchronized (AlbumImageDiffer.class) {
				if (_instance == null) {
					_instance = new AlbumImageDiffer ();
				}
			}
		}

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumImageDiffer ()
	{
		_log.debug ("AlbumImageDiffer ctor");

		String resource = "com/vendo/albumServlet/mybatis-image-server-config.xml";

		try (InputStream inputStream = Resources.getResourceAsStream (resource)) {
			_sqlSessionFactory = new SqlSessionFactoryBuilder ().build (inputStream);
			_log.debug ("AlbumImageDiffer ctor: loaded mybatis config from " + resource);

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer ctor: SqlSessionFactoryBuilder.build failed on " + resource, ee);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		AlbumProfiling.getInstance ().enter (5);

		if (_cleanUpObsoleteEntries) {
			_log.debug ("AlbumImageDiffer.run: cleanUpObsoleteEntries starting");
			Instant startInstant = Instant.now();

			long rowsDeleted = deleteFromImageDiffs();

			long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();
			_log.debug ("Summary:");
			_log.debug ("AlbumImageDiffer.run: rowsDeleted: " + _decimalFormat.format(rowsDeleted) + ", total elapsed: " + LocalTime.ofNanoOfDay (elapsedNanos));

			AlbumProfiling.getInstance ().exit (5);

			return;
		}

		_shutdownThread = watchShutdownFile ();

		if (_shutdownFlag.get ()) {
			return;
		}

//debugging
//		Map<String, AlbumImageDiffDetails> images = getImagesFromImageDiffs (IntStream.range(1, 10000).boxed().collect(Collectors.toList()));
//		System.out.println("*** images = " + images);
//		if (true) {
//			return;
//		}

		getImageIds ();

		_log.debug ("AlbumImageDiffer.run: _idListA.size: " + _decimalFormat.format (_idListA.size ()));
		_log.debug ("AlbumImageDiffer.run: _idListB.size: " + _decimalFormat.format (_idListB.size ()));

		if (_idListA.isEmpty () || _idListB.isEmpty ()) {
			return;
		}

		AlbumProfiling.getInstance ().enterAndTrace(5, "diff");

		final AtomicInteger numDiffs = new AtomicInteger (0);
		final AtomicInteger orientationMismatch = new AtomicInteger (0);
		final AtomicInteger sameBaseName = new AtomicInteger (0);
		final AtomicInteger duplicates = new AtomicInteger (0);
		final AtomicInteger rowsInserted = new AtomicInteger (0);
		final Set<String> toBeSkipped = new HashSet<>();

		if (_shutdownFlag.get ()) {
			return;
		}

		List<Integer> imageIds = Stream.of (_idListA, _idListB).flatMap (Collection::stream).map (AlbumImageData::getNameId).distinct ().collect (Collectors.toList ());
		Map<String, AlbumImageDiffDetails> existingDiffs = getImagesFromImageDiffs (imageIds);

		Map<String, ByteBuffer> scaledImageDataCache = readScaledImageDataIntoCache();

		final CountDownLatch endGate = new CountDownLatch (_idListA.size ());
		final Iterator<AlbumImageData> iterA = _idListA.iterator();
		while (iterA.hasNext () && !_shutdownFlag.get ()) {
			final AlbumImageData albumImageDataA = iterA.next ();
			synchronized (toBeSkipped) {
				if (toBeSkipped.contains (albumImageDataA.getName ())) {
					continue;
				}
			}
			final AlbumImage imageA = new AlbumImage (albumImageDataA.getName (), albumImageDataA.getSubFolder (), false);

			final ByteBuffer scaledImageDataA = scaledImageDataCache.get(albumImageDataA.getName());
			if (scaledImageDataA == null) {
				synchronized (toBeSkipped) {
					toBeSkipped.add(albumImageDataA.getName ());
				}
			}

			Runnable task = () -> {
				Thread.currentThread ().setName (albumImageDataA.getName ());

				Collection<AlbumImageDiffDetails> imageDiffDetails = new ArrayList<> ();

				Iterator<AlbumImageData> iterB = _idListB.iterator();
				while (iterB.hasNext () && !_shutdownFlag.get ()) {
					AlbumImageData albumImageDataB = iterB.next ();
					synchronized (toBeSkipped) {
						if (toBeSkipped.contains (albumImageDataB.getName ())) {
							continue;
						}
					}

					AlbumImage imageB = new AlbumImage (albumImageDataB.getName (), albumImageDataB.getSubFolder (), false);
					if (imageA.equalBase (imageB, false)) {
//						_log.debug ("AlbumImageDiffer.run: skipping images with same baseName: " + imageA.getBaseName (false));
						sameBaseName.incrementAndGet ();
						continue;
					}

					if (albumImageDataA.getOrientation () != albumImageDataB.getOrientation ()) {
//						_log.debug ("AlbumImageDiffer.run: skipping images with different orientation: " + imageA.getName () + "," + imageB.getName ());
						orientationMismatch.incrementAndGet ();
						continue;
					}

					AlbumImageDiffDetails existingDiff = existingDiffs.get (AlbumImageDiffDetails.getJoinedNameIds (albumImageDataA.getNameId (), albumImageDataB.getNameId ()));
					if (existingDiff != null) {
						//if this image pair already exists in database, skip diffing them, but still update record in database
						_log.debug ("AlbumImageDiffer.run: skipping duplicate: " + imageA.getName () + ", " + imageB.getName () + ", " + existingDiff);
						imageDiffDetails.add (new AlbumImageDiffDetails (existingDiff.getNameId1 (), existingDiff.getNameId2 (), existingDiff.getAvgDiff (), existingDiff.getStdDev (), 1, getMode (), existingDiff.getLastUpdate ()));
						duplicates.incrementAndGet ();
						continue;
					}

					final ByteBuffer scaledImageDataB = scaledImageDataCache.get(albumImageDataB.getName());
					if (scaledImageDataB == null) {
						synchronized (toBeSkipped) {
							toBeSkipped.add(albumImageDataB.getName ());
						}
					}

//					_log.debug ("AlbumImageDiffer.run: comparison: " + albumImageDataA.getName () + ", " + albumImageDataB.getName ());
					numDiffs.incrementAndGet ();

					VPair<Integer, Integer> diffPair = AlbumImage.getScaledImageDiff (scaledImageDataA, scaledImageDataB);
					int averageDiff = diffPair.getFirst ();
					int stdDev = diffPair.getSecond ();
					if (AlbumImage.acceptDiff (averageDiff, stdDev, _maxRgbDiffs, _maxStdDev)) {
						String goodMatchMarker = (averageDiff <= 10 || stdDev <= 10) ? " *" : "";
						if (!goodMatchMarker.isEmpty() && !imageA.equalBase(imageB, true)) {
							goodMatchMarker += "*"; //set to " **"
						}
						imageDiffDetails.add (new AlbumImageDiffDetails (albumImageDataA.getNameId (), albumImageDataB.getNameId (), averageDiff, stdDev, 1, getMode (), new Timestamp (new GregorianCalendar ().getTimeInMillis ())));
						_log.debug ("AlbumImageDiffer.run: " + String.format ("%2s", averageDiff) + " " + String.format ("%2s", stdDev) + " " + imageA.getName () + "," + imageB.getName () + goodMatchMarker);
					}

//					if (_shutdownFlag.get ()) {
//						System.err.println ("AlbumImageDiffer.run: got shutdown flag. Aborting.");
//						break;
//					}
				}

				if (imageDiffDetails.size () > 0) {
					rowsInserted.addAndGet (insertImageIntoImageDiffs (imageDiffDetails));
				}
//				_log.debug ("AlbumImageDiffer.run: thread/task complete for: " + albumImageDataA.getName ());

				endGate.countDown ();
			};
//			_log.debug ("AlbumImageDiffer.run: creating new thread/task for: " + albumImageDataA.getName ());
			getExecutor ().execute (task);
		}
		_log.debug ("AlbumImageDiffer.run: queued " + _decimalFormat.format (_idListA.size ()) + " threads");

		if (_shutdownFlag.get ()) {
			_log.debug ("AlbumImageDiffer.run: endGate.getCount = " + endGate.getCount ());
			while (endGate.getCount () > 0) {
				endGate.countDown ();
			}
			_log.debug ("AlbumImageDiffer.run: endGate.getCount = " + endGate.getCount ());
		}

		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.run: endGate:", ee);
		}

		if (_shutdownThread != null) {
			_shutdownThread.interrupt (); //trigger thread termination
		}

		AlbumProfiling.getInstance ().exit (5, "diff");

		_log.debug ("AlbumImageDiffer.run: numDiffs            = " + _decimalFormat.format (numDiffs.get ()));
		_log.debug ("AlbumImageDiffer.run: orientationMismatch = " + _decimalFormat.format (orientationMismatch.get ()));
		_log.debug ("AlbumImageDiffer.run: sameBaseName        = " + _decimalFormat.format (sameBaseName.get ()));
		_log.debug ("AlbumImageDiffer.run: duplicates          = " + _decimalFormat.format (duplicates.get ()));
		_log.debug ("AlbumImageDiffer.run: rowsInserted (new)  = " + _decimalFormat.format (rowsInserted.get () - 2L * duplicates.get ()));
		_log.debug ("AlbumImageDiffer.run: rowsInserted (all)  = " + _decimalFormat.format (rowsInserted.get () - duplicates.get ()));

		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	private Map<String, ByteBuffer> readScaledImageDataIntoCache() {
		AlbumProfiling.getInstance ().enter(5);

		Set<AlbumImageData> idSetAll = new HashSet<>(_idListA);
		idSetAll.addAll(_idListB);

		Map<String, ByteBuffer> scaledImageDataCache = new ConcurrentHashMap<>(idSetAll.size());

		_log.debug ("AlbumImageDiffer.readScaledImageDataIntoCache: idSetAll.size = " + _decimalFormat.format (idSetAll.size()));

		idSetAll.stream().parallel().forEach(d -> {
			final AlbumImage image = new AlbumImage (d.getName (), d.getSubFolder (), false);
			final ByteBuffer byteBuffer = image.readScaledImageData ();
			if (byteBuffer != null) {
				scaledImageDataCache.put(d.getName(), byteBuffer);
			}
		});

		_log.debug ("AlbumImageDiffer.readScaledImageDataIntoCache: scaledImageDataCache.size = " + _decimalFormat.format (scaledImageDataCache.size()));

		AlbumProfiling.getInstance ().exit (5);

		return scaledImageDataCache;
	}

	///////////////////////////////////////////////////////////////////////////
	private void getImageIds ()
	{
		AlbumProfiling.getInstance ().enter (5);

		final CountDownLatch endGate = new CountDownLatch (2);

		Runnable taskA = () -> {
			Thread.currentThread ().setName ("queryA");
			if (isQueryFromImagesTable(_modeA)) {
				_idListA = queryImageIdsNames(_whereClauseA);
			} else {
				_idListA = queryImageIdsTags(_whereClauseA, _filtersA);
			}
			endGate.countDown ();
		};
		getExecutor ().execute (taskA);

		Runnable taskB = () -> {
			Thread.currentThread ().setName ("queryB");
			if (isQueryFromImagesTable(_modeB)) {
				_idListB = queryImageIdsNames(_whereClauseB);
			} else {
				_idListB = queryImageIdsTags(_whereClauseB, _filtersB);
			}
			endGate.countDown ();
		};
		getExecutor ().execute (taskB);

		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.getImageIds: endGate:", ee);
		}

		_directoryCache = null; //we are done with cache

		AlbumProfiling.getInstance ().exit (5);

//		Collections.sort(_idListA, _idListA.get (0));
//		Collections.sort(_idListB, _idListB.get (0));
//		for (AlbumImageData item : _idListA) {
//			_log.debug ("AlbumImageDiffer.getImageIds: _idListA: " + item);
//		}
//		for (AlbumImageData item : _idListB) {
//			_log.debug ("AlbumImageDiffer.getImageIds: _idListB: " + item);
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<AlbumImageData> queryImageIdsNames (String whereClause)
	{
		String profileTag = extractQuotedStrings (whereClause).toString ();
		AlbumProfiling.getInstance ().enterAndTrace (5, profileTag);

		//example where clause: "where name_no_ext like 's%' or name_no_ext like 'b%'"
		String sql = "select name_id, name_no_ext, width, height from albumimages.images where name_id in " + NL +
				 	 "(select name_id from (select name_id from albumimages.images " + NL +
				 	 whereClause + NL +
				 	 "order by rand() limit " + _maxRows + ") t)";
		_log.debug ("AlbumImageDiffer.queryImageIdsNames: sql: " + NL + sql);

		Collection<AlbumImageData> items = new ArrayList<> ();

		try (Connection connection = getConnection ();
			 Statement statement = connection.createStatement ();
			 ResultSet rs = statement.executeQuery (sql)) {

			while (rs.next ()) {
				int nameId = rs.getInt ("name_id");
				String name = rs.getString ("name_no_ext");
				int width = rs.getInt ("width");
				int height = rs.getInt ("height");
				items.add (new AlbumImageData (nameId, name, AlbumOrientation.getOrientation (width, height)));
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.queryImageIdsNames:", ee);
			_log.error ("AlbumImageDiffer.queryImageIdsNames: sql:" + NL + sql);
		}

		if (items.isEmpty ()) {
			_log.error ("AlbumImageDiffer.queryImageIdsNames: no rows found for query:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5, profileTag);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<AlbumImageData> queryImageIdsTags (String whereClause, List<String> filters)
	{
		String profileTag = extractQuotedStrings (whereClause) + ", " + filters;
		AlbumProfiling.getInstance ().enterAndTrace (5, profileTag);

		boolean useFilters = (filters != null && filters.size () > 0);

		Collection<String> baseNames = new HashSet<> (_maxRows); //use Set to eliminate dups

		{ //---------------------------------------------------------------------
		AlbumProfiling.getInstance ().enterAndTrace (5, "part1");

		int maxRowsForQuery = _maxRows;
		if (useFilters) {
			maxRowsForQuery *= 10; //since filters will eliminate many results
		} else {
			maxRowsForQuery /= 4; //since each baseName will have multiple images
		}

		//example where clause: "where tag = 'blue'"
		String sql = "select distinct name from albumtags.base1_names where name_id in (" + NL +
					 "select name_id from (" + NL +
					 "(select name_id from albumtags.base1_names_tags" + NL +
					 "inner join albumtags.tags on base1_names_tags.tag_id = tags.tag_id" + NL +
					 "where tags.tag_id in (" + NL +
					 "select tag_id from albumtags.tags " + NL +
					 whereClause + NL +
					 ")) order by rand() limit " + maxRowsForQuery + ") t)";
		_log.debug ("AlbumImageDiffer.queryImageIdsTags: sql: " + NL + sql);

		try (Connection connection = getConnection ();
			 Statement statement = connection.createStatement ();
			 ResultSet rs = statement.executeQuery (sql)) {

			while (rs.next ()) {
				String name = rs.getString ("name");
				if (!useFilters || accept (name, filters)) {
					baseNames.add(name);
				}
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.queryImageIdsTags:", ee);
			_log.error ("AlbumImageDiffer.queryImageIdsTags: sql:" + NL + sql);
			return null;
		}
		_log.debug ("AlbumImageDiffer.queryImageIdsTags: baseNames.size(): " + baseNames.size ());

		if (baseNames.isEmpty ()) {
			_log.error ("AlbumImageDiffer.queryImageIdsTags: no rows found for query: sql:" + NL + sql);
			_log.error ("AlbumImageDiffer.queryImageIdsTags: no rows found for query: filters:" + NL + filters);
		}

		AlbumProfiling.getInstance ().exit (5, "part1");
		}

		Collection<String> names = new ArrayList<> ();
		Collection<AlbumImageData> items = new ArrayList<> ();

		boolean useFileSystem = true;
		if (useFileSystem) {

			AlbumProfiling.getInstance ().enterAndTrace (5, "part2");

			int maxRows = (_maxRows * 11) / 10;
			names = getRandomNamesForBaseNames (baseNames, maxRows);

			if (names.isEmpty ()) {
				_log.error ("AlbumImageDiffer.queryImageIdsTags: no names returned from getRandomNamesForBaseNames");
			}

			AlbumProfiling.getInstance ().exit (5, "part2");

			AlbumProfiling.getInstance ().enterAndTrace (5, "part3");

			items = selectNamesFromImagesTableFS (names);

			_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size ());

			if (items.isEmpty ()) {
				_log.error ("AlbumImageDiffer.queryImageIdsTags: no items returned from selectNamesFromImagesTableFS");
			}

			AlbumProfiling.getInstance ().exit (5, "part3");

		} else { //use database

			AlbumProfiling.getInstance ().enterAndTrace (5, "part2");

			items = selectNamesFromImagesTableDB (baseNames);

			_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size ());

			if (items.isEmpty ()) {
				_log.error ("AlbumImageDiffer.queryImageIdsTags: no items returned from selectNamesFromImagesTableDB");
			}

			AlbumProfiling.getInstance ().exit (5, "part2");
		}

		_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size () + " (original)");
		items = VendoUtils.shuffleAndTruncate (items, _maxRows, false);
		_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size () + " (truncated)");

		AlbumProfiling.getInstance ().exit (5, profileTag);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean accept (String base, List<String> filters)
	{
//		if (filters.stream().anyMatch(base::startsWith)) {
//			_log.debug("AlbumImageDiffer.accept(" + base + "," + filters + ")");
//		}
		return filters.stream ().anyMatch (base::startsWith);
	}

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	public Collection<AlbumImageData> selectNamesFromImagesTableFS (Collection<String> names)
	{
		AlbumProfiling.getInstance ().enter (5);

		Collection<AlbumImageData> items = new ArrayList<> ();

		if (names.isEmpty ()) {
			return items;
		}

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
			items = mapper.selectNamesFromImagesTable (names);

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectNamesFromImagesTableFS(\"" + names + "\"): ", ee);
		}

		AlbumProfiling.getInstance ().exit (5);

//		for (AlbumImageData item : items) {
//			_log.debug ("AlbumImageDiffer.selectNamesFromImagesTableFS: " + item);
//		}

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	private Collection<AlbumImageData> selectNamesFromImagesTableDB (Collection<String> baseNames)
	{
		AlbumProfiling.getInstance ().enter (5);

		Collection<AlbumImageData> items = new ArrayList<> ();

		if (baseNames.isEmpty ()) {
			return items;
		}

		String sqlBase = "select name_id, name_no_ext, width, height from images where name_no_ext rlike ";

		String sql = null;
		ResultSet rs = null;
		try (Connection connection = getConnection ();
			 Statement statement = connection.createStatement ()) {

			for (String baseName : baseNames) {
				sql = sqlBase + "'" + baseName + ".*'";
				rs = statement.executeQuery (sql);
				while (rs.next ()) {
					int nameId = rs.getInt ("name_id");
					String name = rs.getString ("name_no_ext");
					int width = rs.getInt ("width");
					int height = rs.getInt ("height");
					items.add (new AlbumImageData (nameId, name, AlbumOrientation.getOrientation (width, height)));
				}
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectNamesFromImagesTableDB:", ee);
			_log.error ("AlbumImageDiffer.selectNamesFromImagesTableDB: sql:" + NL + sql);

		} finally {
			if (rs != null) {
				try { rs.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			}
		}

		if (items.isEmpty ()) {
			_log.error ("AlbumImageDiffer.selectNamesFromImagesTableDB: no rows found for query:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	//returns a Map because I want to use get() on the result, and Set does not support get()
// not currently used
//	private Map<String, AlbumImageDiffDetails> getAllImagesFromImageDiffs ()
//	{
//		AlbumProfiling.getInstance ().enterAndTrace (5);
//
//		List<AlbumImageDiffDetails> list = new LinkedList<> ();
//
//		try (SqlSession session = _sqlSessionFactory.openSession ()) {
//			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
//			list = mapper.selectAllImagesFromImageDiffsTable ();
//
//		} catch (Exception ee) {
//			_log.error ("AlbumImageDiffer.getAllImagesFromImageDiffs(): ", ee);
//		}
//
//		Map<String, AlbumImageDiffDetails> map = list.stream ().collect (Collectors.toMap (i -> i.getNameId1 () + "," + i.getNameId2 (), i -> i));
//
//		AlbumProfiling.getInstance ().exit (5);
//
////		_log.debug ("AlbumImageDiffer.getAllImagesFromImageDiffs): map.size: " + map.size ());
//
//		return map;
//	}

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI and servlet
	public Map<String, AlbumImageDiffDetails> getImagesFromImageDiffs (Collection<Integer> nameIds)
	{
		return getImagesFromImageDiffs (nameIds, Integer.MAX_VALUE);
	}
	public Map<String, AlbumImageDiffDetails> getImagesFromImageDiffs (Collection<Integer> nameIds, int maxImagesToQuery)
	{
		_log.debug ("AlbumImageDiffer.getImagesFromImageDiffs: nameIds.size: " + _decimalFormat.format (nameIds.size ()));

		if (nameIds.isEmpty ()) {
			return new HashMap<> ();
//NO: too slow
//		} else if (nameIds.size() > maxImagesToQuery) {
//			return getAllImagesFromImageDiffs();
		} else if (nameIds.size() > maxImagesToQuery) {
			_log.warn ("AlbumImageDiffer.getImagesFromImageDiffs: nameIds.size (" + _decimalFormat.format (nameIds.size()) +
															") greater than max (" + _decimalFormat.format (maxImagesToQuery) + "): ***** SKIPPING *****");
			return new HashMap<> ();
		}

		AlbumProfiling.getInstance ().enter/*AndTrace*/ (5);

		List<AlbumImageDiffDetails> list = new LinkedList<> ();

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
			list = mapper.selectImagesFromImageDiffsTable (nameIds);

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.getImagesFromImageDiffs(): ", ee);
		}

		Map<String, AlbumImageDiffDetails> map = list.stream ().collect (Collectors.toMap (i -> i.getNameId1 () + "," + i.getNameId2 (), i -> i));

		AlbumProfiling.getInstance ().exit (5);

//		_log.debug ("AlbumImageDiffer.getImagesFromImageDiffs: map.size: " + map.size ());

		return map;
	}

	///////////////////////////////////////////////////////////////////////////
	//cleanup obsolete rows: rows in image_diffs that are no longer in images
	public long deleteFromImageDiffs ()
	{
		final int oneMillion = 1000 * 1000;
		int numMillions = 1 + getMaxNameIdFromImages () / oneMillion;

		final int nameIdMax = numMillions * oneMillion;
		final int nameIdNumSteps = numMillions;
		final int nameIdStepSize = nameIdMax / nameIdNumSteps;

		_log.debug("AlbumImageDiffer.deleteFromImageDiffs: nameIdMax rounded up to nearest million: " + _decimalFormat.format(nameIdMax));

		long totalRowsDeleted = 0;

		String nameIdColumnFormat = "name_id_%d";
		String sqlFormat = "delete from image_diffs where %s >= %d and %s < %d and %s not in" +
						   " (select name_id from images where name_id >= %d and name_id < %d)";

		for (int nameId = 0; nameId < nameIdMax; nameId += nameIdStepSize) {
			for (int nameIdColumnIndex = 1; nameIdColumnIndex <= 2; nameIdColumnIndex++) {
				AlbumProfiling.getInstance ().enter(5, "deleteFromImageDiffs");

				String nameIdColumn = String.format(nameIdColumnFormat, nameIdColumnIndex);
				String sql = String.format(sqlFormat, nameIdColumn, nameId, nameIdColumn, (nameId + nameIdStepSize), nameIdColumn,
																	nameId, (nameId + nameIdStepSize));

				_log.debug("AlbumImageDiffer.deleteFromImageDiffs: sql: " + sql);
				Instant startInstant = Instant.now();

				int rowsDeleted = 0;
				try (Connection connection = getConnection ();
					 Statement statement = connection.createStatement ()) {

					rowsDeleted = statement.executeUpdate (sql);

				} catch (Exception ee) {
					_log.error ("AlbumImageDiffer.deleteFromImageDiffs:", ee);
					_log.error ("AlbumImageDiffer.deleteFromImageDiffs: sql:" + NL + sql);
				}

				long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();
				_log.debug("AlbumImageDiffer.deleteFromImageDiffs: rowsDeleted: " + _decimalFormat.format(rowsDeleted) + ", elapsed: " + LocalTime.ofNanoOfDay (elapsedNanos));

				totalRowsDeleted += rowsDeleted;

				AlbumProfiling.getInstance ().exit(5, "deleteFromImageDiffs");
			}
		}

		return totalRowsDeleted;
	}

/* unused: currently being implemented by the other AlbumImageDiffer.insertImageIntoImageDiffs to handle 'on duplicate key' case
	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	private boolean insertImageIntoImageDiffs (AlbumImageDiffDetails imageDiffDetails)
	{
//		AlbumProfiling.getInstance ().enter (7);

		boolean status = true;

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
			int rowsAffected = mapper.insertImageIntoImageDiffs (imageDiffDetails);
			session.commit ();

			status = rowsAffected > 0;

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.insertImageIntoImageDiffs(\"" + imageDiffDetails + "\"): ", ee);

			status = false;
		}

//		AlbumProfiling.getInstance ().exit (7);

		return status;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	public int insertImageIntoImageDiffs (Collection<AlbumImageDiffDetails> items)
	{
//		AlbumProfiling.getInstance ().enter (5);

		final int maxUnsignedByte = 255; //max value that can be written to a TINYINT column

		String sqlBase = "insert into image_diffs (name_id_1, name_id_2, avg_diff, std_dev, count, source) values" + NL;
		String sqlValues = " (?, ?, ?, ?, ?, ?)" + NL;
		String sqlOnDup = " on duplicate key update avg_diff = values (avg_diff)," + NL +
												  " std_dev = values (std_dev)," + NL +
												  " count = count + values(count)," + NL +
												  " source = values (source)," + NL +
												  " last_update = now()";
		String sql = sqlBase + sqlValues + sqlOnDup;

		int rowsInserted = 0;
		try (Connection connection = getConnection ();
			 PreparedStatement statement = connection.prepareStatement (sql)) {

			connection.setAutoCommit (false);

			int batchCount = 0;
			for (AlbumImageDiffDetails item : items) {
				int index = 0;
				statement.setInt (++index, item.getNameId1 ());
				statement.setInt (++index, item.getNameId2 ());
				statement.setInt (++index, Math.min (maxUnsignedByte, item.getAvgDiff ()));
				statement.setInt (++index, Math.min (maxUnsignedByte, item.getStdDev ()));
				statement.setInt (++index, item.getCount ());
				statement.setString (++index, item.getSource ());
				statement.addBatch ();
				batchCount++;

				if (batchCount % _batchInsertSize == 0 || batchCount == items.size ()) {
					int [] updateCounts = statement.executeBatch ();
					rowsInserted += Arrays.stream (updateCounts).sum ();

					connection.commit ();
				}
			}

			connection.setAutoCommit(true); //TODO - this should be done in finally block, but try-with-resources does not allow it

//		} catch (MySQLIntegrityConstraintViolationException ee) {
		} catch (SQLIntegrityConstraintViolationException ee) {
			//ignore as this will catch any duplicate insertions

		} catch (Exception ee) {
			_log.error("AlbumImageDiffer.insertImageIntoImageDiffs:", ee);
			_log.error("AlbumImageDiffer.insertImageIntoImageDiffs: sql:" + NL + sql);
		}

//		_log.debug ("AlbumImageDiffer.insertImageIntoImageDiffs: rowsInserted: " + rowsInserted);

//		AlbumProfiling.getInstance ().exit (5);

		return rowsInserted;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	public Collection<AlbumImageDiffData> selectNamesFromImageDiffs (int maxRgbDiff, int maxStdDev, double sinceDays, boolean operatorOr)
	{
		AlbumProfiling.getInstance ().enter (5);

		Collection<AlbumImageDiffData> items = new ArrayList<> ();

		final String operatorStr = (operatorOr ? "OR" : "AND");
		final int sinceMinutes = (int) (sinceDays * 24. * 60.);

		AlbumSortType sortType = AlbumFormInfo.getInstance ().getSortType ();
		boolean reverseSort = AlbumFormInfo.getInstance ().getReverseSort ();
		String orderByClause;
		switch (sortType) {
		case ByName:
			orderByClause = " order by i1.name_no_ext, i2.name_no_ext, min_diff, max_diff" + (reverseSort ? " desc" : " asc");
			break;
		case ByDate:
			orderByClause = " order by d.last_update, min_diff, max_diff, i1.name_no_ext, i2.name_no_ext" + (reverseSort ? " asc" : " desc");
			break;
		default: //fall through
		case ByRgb:
			orderByClause = " order by min_diff, max_diff, i1.name_no_ext, i2.name_no_ext" + (reverseSort ? " asc" : " desc");
			break;
		}

//TODO - add where clause for includeFilters
		//for consistent behavior, this logic should be duplicated in both AlbumImageDiffer#selectNamesFromImageDiffs and AlbumImage#acceptDiff
		String acceptDiffClause = " (d.avg_diff <= " + maxRgbDiff + " " + operatorStr + " d.std_dev <= " + maxStdDev + ")" +
								  " and d.avg_diff <= (3 * " + maxRgbDiff + ") and d.std_dev <= (3 * " + maxStdDev + ")"; //hardcoded values

		String sql = "select i1.name_no_ext as name_no_ext1, i2.name_no_ext as name_no_ext2, d.avg_diff as avg_diff, d.std_dev as std_dev, d.source as source, d.last_update as last_update," + NL +
					 " case when avg_diff < std_dev then avg_diff else std_dev end as min_diff," + NL +
					 " case when avg_diff > std_dev then avg_diff else std_dev end as max_diff" + NL +
					 " from image_diffs d" + NL +
					 " join images i1 on i1.name_id = d.name_id_1" + NL +
					 " join images i2 on i2.name_id = d.name_id_2" + NL +
					 " where " + acceptDiffClause + NL +
					 " and d.last_update >= timestampadd(minute, -" + sinceMinutes + ", now())" + NL +
					 orderByClause;
		_log.debug ("AlbumImageDiffer.selectNamesFromImageDiffs: sql:" + NL + sql);

		try (Connection connection = getConnection ();
			 Statement statement = connection.createStatement ();
			 ResultSet rs = statement.executeQuery (sql)) {

			while (rs.next ()) {
				String name1 = rs.getString ("name_no_ext1");
				String name2 = rs.getString ("name_no_ext2");
				int averageDiff = rs.getInt ("avg_diff");
				int stdDev = rs.getInt ("std_dev");
				String source = rs.getString ("source");
				Date lastUpdate = new Date (rs.getTimestamp ("last_update").getTime ());
//				_log.debug (new AlbumImageDiffData (name1, name2, averageRgbDiff, stdDev, source, lastUpdate));
				items.add(new AlbumImageDiffData (name1, name2, averageDiff, stdDev, source, lastUpdate));
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectNamesFromImageDiffs:", ee);
			_log.error ("AlbumImageDiffer.selectNamesFromImageDiffs: sql:" + NL + sql);
		}

		if (items.isEmpty ()) {
			_log.error ("AlbumImageDiffer.selectNamesFromImageDiffs: no rows found for query:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	public int getMaxNameIdFromImages ()
	{
		AlbumProfiling.getInstance ().enter (5);

		int maxNameIdFromImages = 0;

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
			maxNameIdFromImages = mapper.selectMaxNameIdFromImagesTable ();

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectMaxNameIdFromImagesTable(): ", ee);
		}

		AlbumProfiling.getInstance ().exit (5);

		return maxNameIdFromImages;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns a random subset
	private Collection<String> getRandomNamesForBaseNames (Collection<String> baseNames, int maxRows)
	{
//		_log.debug ("AlbumImageDiffer.getRandomNamesForBaseNames: baseNames: " + baseNames);

		Collection<String> names = new ArrayList<> ();
		final int divisor = 4;

		for (String baseName : baseNames) {
			Collection<String> files = getMatchingImageNamesFromFileSystem (baseName);
			files = VendoUtils.shuffleAndTruncate (files, files.size () / divisor, false);
			names.addAll (files);

			if (names.size () >= maxRows) {
				break;
			}
		}

		_log.debug ("AlbumImageDiffer.getRandomNamesForBaseNames: names.size(): " + names.size ());

		return names;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns names only: no path or extension
	private Collection<String> getMatchingImageNamesFromFileSystem (String baseName)
	{
//		_log.debug ("AlbumImageDiffer.getMatchingImageNamesFromFileSystem: baseName: " + baseName);

		Collection<String> files = new ArrayList<> ();

		String subFolder = AlbumImageDao.getInstance ().getSubFolderFromImageName (baseName);
		Collection<String> filenames = getDirectoryCache (subFolder);
		for (String filename : filenames) {
			if (filename.startsWith (baseName)) {// && filename.endsWith (".jpg")) {
				files.add (filename.substring (0, filename.lastIndexOf ('.')));
			}
		}

		return files;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns a collection of image file names (without path, with extension)
	private synchronized Collection<String> getDirectoryCache (String subFolder)
	{
		if (_directoryCache.containsKey(subFolder)) {
			return _directoryCache.get(subFolder);
		}

//		AlbumProfiling.getInstance ().enterAndTrace(5, subFolder); //cache miss

		Collection<String> filenames = new ArrayList <> ();

		Path folder = FileSystems.getDefault ().getPath (_basePath, _defaultRootFolder, subFolder);
		try (DirectoryStream <Path> stream = Files.newDirectoryStream (folder, "*.jpg")) {
			for (Path file : stream) {
				filenames.add (file.getFileName ().toString ());
			}
			_directoryCache.put(subFolder, filenames);

		} catch (IOException ee) {
			_log.error ("AlbumImageDiffer.getDirectoryCache(\"" + subFolder + "\"): error reading file system", ee);
		}

//		_log.debug ("AlbumImageDiffer.getDirectoryCache(\"" + subFolder + "\"): filenames.size(): " + filenames.size ());

//		AlbumProfiling.getInstance ().exit (5, subFolder);

		return filenames;
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<String> extractQuotedStrings (String string)
	{
		Collection<String> list = new ArrayList<> ();

		final Pattern pattern = Pattern.compile ("'([^']*)'");
		Matcher matcher = pattern.matcher (string);
		while (matcher.find ()) {
			list.add(matcher.group (1));
		}

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	private String getMode ()
	{
		if (_modeA == _modeB) {
			return _modeA.toString ();
		} else {
			return _modeA.toString () + "/" + _modeB.toString ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Mode getMode (String modeStr) {
		switch(modeStr) {
			default: return Mode.NotSet;
			case "folders": return Mode.Folders;
			case "folders2": return Mode.Folders2;
			case "names": return Mode.Names;
			case "names2": return Mode.Names2;
			case "new": return Mode.New;
			case "tags": return Mode.Tags;
			case "tags2": return Mode.Tags2;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean isQueryFromImagesTable (Mode mode) {
		switch(mode) {
			default: return false;
			case Folders:
			case Folders2:
			case Names:
			case Names2:
			case New: return true;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Connection getConnection ()
	{
		Connection connection = null;

		try {
			connection = getDataSource ().getConnection ();

		} catch (Exception ee) {
//			connection = null;
			_log.error ("AlbumImageDiffer.getConnection: error connecting to database", ee);
		}

		return connection;
	}

	///////////////////////////////////////////////////////////////////////////
	private synchronized static BasicDataSource getDataSource ()
	{
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.cj.jdbc.Driver";
		final String dbUrl = "jdbc:mysql://localhost/albumimages";
		final String dbUser = "root";
		final String dbPass = "root";

		if (_dataSource == null) {
//			_log.debug ("AlbumImageDiffer.getDataSource: url = " + dbUrl);

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
	public synchronized static ExecutorService getExecutor ()
	{
		if (_executor == null || _executor.isTerminated () || _executor.isShutdown ()) {
			 _executor = Executors.newFixedThreadPool (_numThreads);
		}

		return _executor;
	}

	///////////////////////////////////////////////////////////////////////////
	public static void shutdownExecutor ()
	{
		_log.debug ("AlbumImageDiffer.shutdownExecutor: shutdown executor");
		/*List<Runnable> waitingThreads =*/ getExecutor ().shutdownNow ();
		//_log.debug ("AlbumImageDiffer.shutdownExecutor: waitingThreads.size = " + waitingThreads.size ());
	}

	///////////////////////////////////////////////////////////////////////////
	private Thread watchShutdownFile ()
	{
		Thread watchingThread = null;

		final Path path = FileSystems.getDefault ().getPath (_basePath, _shutdownFilename);

		if (VendoUtils.fileExists (path)) {
			System.err.println ("AlbumImageDiffer.watchShutdownFile.notify: file already exists: " + path.normalize ());
			_shutdownFlag.getAndSet (true);

			if (watchingThread != null) {
				watchingThread.interrupt (); //exit thread
			}

			return watchingThread;
		}

		try {
			Path dir = path.getRoot ().resolve (path.getParent ());
			String filename = path.getFileName ().toString ();

			_log.info ("AlbumImageDiffer.watchShutdownFile: watching for shutdown file: " + path.normalize ());

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
						_log.debug ("AlbumImageDiffer.watchShutdownFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ());
					}

					if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_MODIFY) ||
						pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_CREATE)) {
						System.err.println ("AlbumImageDiffer.watchShutdownFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ());
						_shutdownFlag.getAndSet (true);
						Thread.currentThread ().interrupt (); //exit thread
					}
				}

				@Override
				protected void overflow (WatchEvent<?> event)
				{
					_log.error ("AlbumImageDiffer.watchShutdownFile.overflow: received event: " + event.kind ().name () + ", count = " + event.count ());
					_log.error ("AlbumImageDiffer.watchShutdownFile.overflow: ", new Exception ("WatchDir overflow"));
				}
			};
			watchingThread = new Thread (watchDir);
			watchingThread.setName ("watchingThread");
			watchingThread.start ();

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.watchShutdownFile: exception watching shutdown file", ee);
		}

		return watchingThread;
	}


	//members from command line
	private static int _numThreads = 4;
	private int _maxRows = 10;
	private int _maxRgbDiffs = _maxAverageDiffUsedByBatFiles;
	private int _maxStdDev = _maxStdDevDiffUsedByBatFiles;
	private Mode _modeA = Mode.NotSet;
	private Mode _modeB = Mode.NotSet;
	private String _whereClauseA = null;
	private String _whereClauseB = null;
	private List<String> _filtersA = null;
	private List<String> _filtersB = null;
	private boolean _cleanUpObsoleteEntries = false;

	//members
	private SqlSessionFactory _sqlSessionFactory = null;
	private String _defaultRootFolder = "jroot";
	private int _batchInsertSize = 1000;

	private Collection<AlbumImageData> _idListB = null;
	private Collection<AlbumImageData> _idListA = null;

	private Thread _shutdownThread = null;
	private final AtomicBoolean _shutdownFlag = new AtomicBoolean ();
	private static final String _shutdownFilename = "shutdownImageDiffer.txt";

	private Map<String, Collection<String>> _directoryCache = new HashMap<> ();

	public static final int _maxAverageDiffUsedByBatFiles = 30; //see pr.bat: match value used by imageDiffer (id*.bat) BAT files: set MAX_RGB_DIFFS=/maxRgbDiffs 30
	public static final int _maxStdDevDiffUsedByBatFiles  = 35; //see pr.bat: match value used by imageDiffer (id*.bat) BAT files: set MAX_STD_DEV=/maxStdDev 35

	private static BasicDataSource _dataSource = null;
	private static ExecutorService _executor = null;
	private static volatile AlbumImageDiffer _instance = null;

	private static final String NL = System.getProperty ("line.separator");
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0"); //as int
	private static final String _basePath = "D:/Netscape/Program/"; //need trailing slash

	private static boolean _Debug = false;
	private static final Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumImageDiffer";
}
