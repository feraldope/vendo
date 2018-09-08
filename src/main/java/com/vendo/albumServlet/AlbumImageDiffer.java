//AlbumImageDiffer.java

package com.vendo.albumServlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumImageDiffer
{
	private enum Mode {NotSet, Names, Tags};

	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		AlbumFormInfo.getInstance (); //call ctor to load class defaults

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
		AlbumFormInfo._profileLevel = 5;

		AlbumProfiling.getInstance ().enter/*AndTrace*/ (1);

		AlbumImageDiffer albumImageDiffer = AlbumImageDiffer.getInstance ();

		if (!albumImageDiffer.processArgs (args))
			System.exit (1); //processArgs displays error

		try {
			albumImageDiffer.run ();
		} catch (Exception ee) {
			ee.printStackTrace (System.err);
		}

		shutdownExecutor ();

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ true);
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		String filterAStr = null;
		String filterBStr = null;
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

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
						if (_maxRgbDiffs < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("maxRows")) {
					try {
						_maxRows = Integer.parseInt (args[++ii]);
						if (_maxRows < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("modeA")) {
					String modeStr = args[++ii];
					if (modeStr.compareTo("names") == 0) {
						_modeA = Mode.Names;
					} else if (modeStr.compareTo("tags") == 0) {
						_modeA = Mode.Tags;
					}
					if (_modeA == Mode.NotSet) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("modeB")) {
					String modeStr = args[++ii];
					if (modeStr.compareTo("names") == 0) {
						_modeB = Mode.Names;
					} else if (modeStr.compareTo("tags") == 0) {
						_modeB = Mode.Tags;
					}
					if (_modeB == Mode.NotSet) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("numThreads") || arg.equalsIgnoreCase ("threads")) {
					try {
						_numThreads = Integer.parseInt (args[++ii]);
						if (_numThreads < 0)
							throw (new NumberFormatException ());
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

		//check for required args and handle defaults
		if (_whereClauseA == null || _whereClauseB == null) {
			displayUsage ("Incorrect usage", true);
		}

		if (_modeA == Mode.NotSet || _modeB == Mode.NotSet) {
			displayUsage ("Incorrect usage", true);
		}

		if (_Debug) {
			_log.debug ("AlbumImageDiffer.processArgs: filtersA = " + _filtersA);
			_log.debug ("AlbumImageDiffer.processArgs: filtersB = " + _filtersB);
			_log.debug ("AlbumImageDiffer.processArgs: maxRgbDiffs = " + _maxRgbDiffs);
			_log.debug ("AlbumImageDiffer.processArgs: maxRows = " + _decimalFormat.format (_maxRows));
			_log.debug ("AlbumImageDiffer.processArgs: modeA = " + _modeA);
			_log.debug ("AlbumImageDiffer.processArgs: modeB = " + _modeB);
			_log.debug ("AlbumImageDiffer.processArgs: numThreads = " + _numThreads);
			_log.debug ("AlbumImageDiffer.processArgs: whereClauseA = " + _whereClauseA);
			_log.debug ("AlbumImageDiffer.processArgs: whereClauseB = " + _whereClauseB);
			}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] [/filtersA=<str>] [/filtersB=<str>] [/maxRgbDiffs=<n>] [/maxRows=<n>] /modeA={names|tags} /modeB={names|tags} [/numThreads=<n>] [/whereClauseA=<str>] [/whereClauseB=<str>]";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumImageDiffer getInstance ()
	{
		if (_instance == null)
			_instance = new AlbumImageDiffer ();

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
	private void run () throws Exception
	{
		AlbumProfiling.getInstance ().enter (5);

		getImageIds ();

		_log.debug ("AlbumImageDiffer.run: _idListA.size: " + _decimalFormat.format (_idListA.size ()));
		_log.debug ("AlbumImageDiffer.run: _idListB.size: " + _decimalFormat.format (_idListB.size ()));

		if (_idListA.size () == 0 || _idListB.size () == 0) {
			return;
		}

		AlbumProfiling.getInstance ().enterAndTrace(5, "diff");

		final AtomicInteger rowsInserted = new AtomicInteger (0);
		final AtomicInteger numDiffs = new AtomicInteger (0);
		final Set<String> toBeSkipped = new HashSet<String>();

		final CountDownLatch endGate = new CountDownLatch (_idListA.size ());
		final Iterator<AlbumImageData> iterA = _idListA.iterator();
		while (iterA.hasNext ()) {
			final AlbumImageData albumImageDataA = iterA.next ();
			synchronized (toBeSkipped) {
				if (toBeSkipped.contains (albumImageDataA.getNameNoExt ())) {
					continue;
				}
			}
			final AlbumImage imageA = new AlbumImage (albumImageDataA.getNameNoExt (), albumImageDataA.getSubFolder (), false);

			final ByteBuffer scaledImageDataA = imageA.readScaledImageData ();
			if (scaledImageDataA == null) {
				synchronized (toBeSkipped) {
					toBeSkipped.add(albumImageDataA.getNameNoExt ());
				}
			}

			Runnable task = () -> {
				Collection<AlbumImageDiffDetails> imageDiffDetails = new ArrayList<AlbumImageDiffDetails> ();

				Iterator<AlbumImageData> iterB = _idListB.iterator();
				while (iterB.hasNext ()) {
					AlbumImageData albumImageDataB = iterB.next ();
					synchronized (toBeSkipped) {
						if (toBeSkipped.contains (albumImageDataB.getNameNoExt ())) {
							continue;
						}
					}

					AlbumImage imageB = new AlbumImage (albumImageDataB.getNameNoExt (), albumImageDataB.getSubFolder (), false);
					if (imageA.equalBase (imageB, false)) {
						continue;
					}

					ByteBuffer scaledImageDataB = imageB.readScaledImageData ();
					if (scaledImageDataB == null) {
						synchronized (toBeSkipped) {
							toBeSkipped.add(albumImageDataB.getNameNoExt ());
						}
					}

//					_log.debug ("AlbumImageDiffer.run: comparison: " + albumImageDataA.getNameNoExt () + ", " + albumImageDataB.getNameNoExt ());
					numDiffs.incrementAndGet ();

					int averageDiff = AlbumImage.getScaledImageDiff (scaledImageDataA, scaledImageDataB, _maxRgbDiffs);
					if (averageDiff <= _maxRgbDiffs) {
						imageDiffDetails.add (new AlbumImageDiffDetails (albumImageDataA.getNameId(), albumImageDataB.getNameId(), averageDiff, _maxRgbDiffs));
						_log.debug ("AlbumImageDiffer.run: " + String.format("%-2s", averageDiff) + " " + imageA.getName () + "," + imageB.getName () + ",");
					}
				}

				if (imageDiffDetails.size () > 0) {
					rowsInserted.addAndGet (insertImageIntoImageDiffs (imageDiffDetails));
				}
//				_log.debug ("AlbumImageDiffer.run: thread/task complete for: " + albumImageDataA.getNameNoExt ());

				endGate.countDown ();
			};
//			_log.debug ("AlbumImageDiffer.run: creating new thread/task for: " + albumImageDataA.getNameNoExt ());
			getExecutor ().execute (task);
		}
		_log.debug ("AlbumImageDiffer.run: started " + endGate.getCount () + " threads");

		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.run: endGate:", ee);
		}

		AlbumProfiling.getInstance ().exit (5, "diff");

		_log.debug ("AlbumImageDiffer.run: numDiffs     = " + _decimalFormat.format (numDiffs.get ()));
		_log.debug ("AlbumImageDiffer.run: rowsInserted = " + _decimalFormat.format (rowsInserted.get ()));

		AlbumProfiling.getInstance ().exit (5);
	}

	///////////////////////////////////////////////////////////////////////////
	private void getImageIds ()
	{
		AlbumProfiling.getInstance ().enter (5);

		final CountDownLatch endGate = new CountDownLatch (2);

		Runnable taskA = () -> {
			if (_modeA == Mode.Names) {
				_idListA = queryImageIdsNames(_whereClauseA);
			} else {
				_idListA = queryImageIdsTags(_whereClauseA, _filtersA);
			}
			endGate.countDown ();
		};
		getExecutor ().execute (taskA);

		Runnable taskB = () -> {
			if (_modeB == Mode.Names) {
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

		Collections.sort(_idListA, _idListA.get (0));
		Collections.sort(_idListB, _idListB.get (0));

		AlbumProfiling.getInstance ().exit (5);

//		for (AlbumImageData item : _idListA) {
//			_log.debug ("AlbumImageDiffer.getImageIds: _idListA: " + item);
//		}
//		for (AlbumImageData item : _idListB) {
//			_log.debug ("AlbumImageDiffer.getImageIds: _idListB: " + item);
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	private List<AlbumImageData> queryImageIdsNames (String whereClause)
	{
		String profileTag = extractQuotedStrings (whereClause).toString ();
		AlbumProfiling.getInstance ().enterAndTrace (5, profileTag);

		//example where clause: "where name_no_ext like 's%' or name_no_ext like 'b%'"
		String sql = "select name_id, name_no_ext from images where name_id in " + NL +
			    	 "(select name_id from (select name_id from images " + NL +
			    	 whereClause + NL +
			    	 "order by rand() limit " + _maxRows + ") t)";
//		_log.debug ("AlbumImageDiffer.queryImageIdsNames: sql: " + NL + sql);

		List<AlbumImageData> items = new ArrayList<AlbumImageData> ();

		Connection connection = getConnection ();
		Statement statement = null;
		try {
			statement = connection.createStatement ();

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.queryImageIdsNames: error from Connection.createStatement", ee);

			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			return items;
		}

		ResultSet rs = null;
		try {
			rs = statement.executeQuery (sql);
			while (rs.next ()) {
				int nameId = rs.getInt ("name_id");
				String nameNoExt = rs.getString ("name_no_ext");
				items.add(new AlbumImageData (nameId, nameNoExt));
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.queryImageIdsNames: error from Statement.executeQuery");
			_log.error ("AlbumImageDiffer.queryImageIdsNames: sql:" + NL + sql);
			_log.error (ee);
			return items;

		} finally {
			if (rs != null) try { rs.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
		}

		if (items.size () == 0) {
			_log.error ("AlbumImageDiffer.queryImageIdsNames: no rows found for query:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5, profileTag);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<AlbumImageData> queryImageIdsTags (String whereClause, List<String> filters)
	{
		String profileTag = extractQuotedStrings (whereClause) + ", " + filters;
		AlbumProfiling.getInstance ().enterAndTrace (5, profileTag);

		boolean useFilters = (filters != null && filters.size () > 0);

		List<String> baseNames = new ArrayList<String> ();

		{ //---------------------------------------------------------------------
		AlbumProfiling.getInstance ().enterAndTrace (5, "part1");

		int maxRowsForQuery = _maxRows;
		if (useFilters) {
			maxRowsForQuery *= 10; //since filters will eliminate many results
		} else {
			maxRowsForQuery /= 4; //since each basename will have multiple images
		}

		//example where clause: "where tag = 'blue'"
		String sql1 = "select distinct name from albumtags.base1_names where name_id in (" + NL +
					  "select name_id from (" + NL +
					  "(select name_id from albumtags.base1_names_tags" + NL +
					  "inner join albumtags.tags on base1_names_tags.tag_id = tags.tag_id" + NL +
					  "where tags.tag_id = (" + NL +
					  "select tag_id from albumtags.tags " + NL +
					  whereClause + NL +
					  ")) order by rand() limit " + maxRowsForQuery + ") t)";
//		_log.debug ("AlbumImageDiffer.queryImageIdsTags: sql: " + NL + sql1);

		Connection connection = getConnection ();
		Statement statement = null;
		try {
			statement = connection.createStatement ();

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.queryImageIdsTags: error from Connection.createStatement", ee);

			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			return null;
		}

		ResultSet rs = null;
		try {
			rs = statement.executeQuery (sql1);
			while (rs.next ()) {
				String name = rs.getString ("name");
				if (!useFilters || (useFilters && accept (name, filters))) {
					baseNames.add(name);
				}
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.queryImageIdsTags: error from Statement.executeQuery");
			_log.error ("AlbumImageDiffer.queryImageIdsTags: sql1:" + NL + sql1);
			_log.error (ee);
			return null;

		} finally {
			if (rs != null) try { rs.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
		}
		_log.debug ("AlbumImageDiffer.queryImageIdsTags: baseNames.size(): " + baseNames.size ());

		if (baseNames.size () == 0) {
			_log.error ("AlbumImageDiffer.queryImageIdsTags: no rows found for query: sql:" + NL + sql1);
			_log.error ("AlbumImageDiffer.queryImageIdsTags: no rows found for query: filters:" + NL + filters);
		}

		AlbumProfiling.getInstance ().exit (5, "part1");
		}

		List<String> names = new ArrayList<String> ();
		List<AlbumImageData> items = new ArrayList<AlbumImageData> ();

		boolean useFileSystem = true;
		if (useFileSystem) {

			AlbumProfiling.getInstance ().enterAndTrace (5, "part2");

			int maxRows = (_maxRows * 11) / 10;
			names = getRandomNamesForBaseNames (baseNames, maxRows);

			if (names.size () == 0) {
				_log.error ("AlbumImageDiffer.queryImageIdsTags: no names returned from getRandomNamesForBaseNames");
			}

			AlbumProfiling.getInstance ().exit (5, "part2");

			AlbumProfiling.getInstance ().enterAndTrace (5, "part3");

			items = selectNamesFromImages1 (names);

			_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size ());

			if (items.size () == 0) {
				_log.error ("AlbumImageDiffer.queryImageIdsTags: no items returned from selectNamesFromImages");
			}

			AlbumProfiling.getInstance ().exit (5, "part3");

		} else { //use database

			AlbumProfiling.getInstance ().enterAndTrace (5, "part2");

			items = selectNamesFromImages2 (baseNames);

			_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size ());

			if (items.size () == 0) {
				_log.error ("AlbumImageDiffer.queryImageIdsTags: no items returned from selectNamesFromImages");
			}

			AlbumProfiling.getInstance ().exit (5, "part2");
		}

		Collections.shuffle (items);
		_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size () + " (original)");
		items = items.subList (0, Math.min (items.size (), _maxRows));
		_log.debug ("AlbumImageDiffer.queryImageIdsTags: items.size(): " + items.size () + " (truncated)");

		AlbumProfiling.getInstance ().exit (5, profileTag);

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean accept (String base, List<String> filters) {
		for (String filter : filters) {
			if (base.startsWith (filter)) {
//				_log.debug ("AlbumImageDiffer.accept(" + base + "," + filter + "): " + true);
				return true;
			}
		}

		return false;
	}

/* unused
	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	private Collection<AlbumImageDiffDetails> getImagesFromImageDiffs ()
	{
		AlbumProfiling.getInstance ().enter (7);

		List<AlbumImageDiffDetails> list = new LinkedList<AlbumImageDiffDetails> ();

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.getImagesFromImageDiffs(): ", ee);
		}

		Collections.sort (list);

		AlbumProfiling.getInstance ().exit (7);

//		_log.debug ("AlbumImageDiffer.getImagesFromImageDiffs): list.size: " + list.size ());

		return list;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	private List<AlbumImageData> selectNamesFromImages1 (List<String> names)
	{
		AlbumProfiling.getInstance ().enter (5);

		List<AlbumImageData> items = new ArrayList<AlbumImageData> ();

		if (names.size () == 0) {
			return items;
		}

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
			items = mapper.selectNamesFromImages (names);

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectNamesFromImages1(\"" + names + "\"): ", ee);
		}

		AlbumProfiling.getInstance ().exit (5);

//		for (AlbumImageData item : items) {
//			_log.debug ("AlbumImageDiffer.selectNamesFromImages1: " + item);
//		}

		return items;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumImageDiffer CLI
	private List<AlbumImageData> selectNamesFromImages2 (List<String> names)
	{
		AlbumProfiling.getInstance ().enter (5);

		List<AlbumImageData> items = new ArrayList<AlbumImageData> ();

		if (names.size () == 0) {
			return items;
		}

		String sqlBase = "select name_id, name_no_ext from images where name_no_ext rlike ";

		Connection connection = getConnection ();
		Statement statement = null;
		try {
			statement = connection.createStatement ();

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectNamesFromImages2: error from Connection.createStatement", ee);

			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			return items;
		}

		String sql = null;
		ResultSet rs = null;
		try {
			for (String name : names) {
				sql = sqlBase + "'" + name + ".*'";
				rs = statement.executeQuery (sql);
				while (rs.next ()) {
					int nameId = rs.getInt ("name_id");
					String nameNoExt = rs.getString ("name_no_ext");
					items.add(new AlbumImageData (nameId, nameNoExt));
				}
			}

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.selectNamesFromImages2: error from Statement.executeQuery");
			_log.error ("AlbumImageDiffer.selectNamesFromImages2: sql:" + NL + sql);
			_log.error (ee);
			return items;

		} finally {
			if (rs != null) try { rs.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
		}

		if (items.size () == 0) {
			_log.error ("AlbumImageDiffer.selectNamesFromImages2: no rows found for query:" + NL + sql);
		}

		AlbumProfiling.getInstance ().exit (5);

		return items;
	}


/* obsolete
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

//TODO - convert this to mybatis
	private int insertImageIntoImageDiffs (Collection<AlbumImageDiffDetails> items)
	{
//		AlbumProfiling.getInstance ().enter (5);

		String sqlBase = "insert into image_diffs (name_id_1, name_id_2, avg_diff, max_diff) values";
		String sqlValues = " (?, ?, ?, ?)";
		String sqlOnDup = " on duplicate key update avg_diff = values (avg_diff), max_diff = values (max_diff), last_update = now()";
		String sql = sqlBase + sqlValues + sqlOnDup;

		Connection connection = getConnection ();
		PreparedStatement statement = null;

		int rowsInserted = 0;
		try {
			connection.setAutoCommit (false);
			int ii = 0;

			statement = connection.prepareStatement (sql);
	        for (AlbumImageDiffDetails item : items) {
	        	statement.setInt (1, item.getNameId1 ());
	        	statement.setInt (2, item.getNameId2 ());
	        	statement.setInt (3, item.getAvgDiff ());
	        	statement.setInt (4, item.getMaxDiff ());
	        	statement.addBatch ();

	        	ii++;
	        	if (ii % 1000 == 0 || ii == items.size ()) {
	        		int [] updateCounts = statement.executeBatch ();
	        		rowsInserted += Arrays.stream (updateCounts).sum ();

//	        		AlbumProfiling.getInstance ().enter (5, "commit");
	        		connection.commit ();
//	        		AlbumProfiling.getInstance ().exit (5, "commit");
	        	}
	        }

//		} catch (MySQLIntegrityConstraintViolationException ee) {
		} catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException ee) {
			//ignore as this will catch any duplicate insertions
//			_log.debug ("Ignoring exception from database while adding ??");// + name);
			_log.error (ee);

		} catch (Exception ee) {
			_log.error ("AlbumImageDiffer.insertImageIntoImageDiffs: error from Connection.prepareStatement or PreparedStatement.executeUpdate");
			_log.error ("AlbumImageDiffer.insertImageIntoImageDiffs: sql:" + NL + sql);
			_log.error (ee);
//			return rowsInserted;

		} finally {
			if (connection != null) try { connection.setAutoCommit (true); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (statement != null) try { statement.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
			if (connection != null) try { connection.close (); } catch (SQLException ex) { _log.error (ex); ex.printStackTrace (); }
		}

//		_log.debug ("AlbumImageDiffer.insertImageIntoImageDiffs: rowsInserted: " + rowsInserted);

//		AlbumProfiling.getInstance ().exit (5);

		return rowsInserted;
	}

	///////////////////////////////////////////////////////////////////////////
	//actually only get one random name from each set
	private List<String> getRandomNamesForBaseNames (List<String> baseNames, int maxRows) {
//		_log.debug ("AlbumImageDiffer.getRandomNamesForBaseNames: baseNames: " + baseNames);

		List<String> names = new ArrayList<String> ();
		final int divisor = 4;

		for (String baseName : baseNames) {
	        List<String> files = getMatchingImageNamesFromFileSystem (baseName);
	        int numRandom = files.size () / divisor;
	        for (int ii = 0; ii < numRandom; ii++) {
		        names.add (files.get(_random.nextInt (files.size ())));
	        }
	        if (names.size () >= maxRows) {
	        	break;
	        }
		}

		_log.debug ("AlbumImageDiffer.getRandomNamesForBaseNames: names.size(): " + names.size ());

		return names;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns names only: no path or extension
	private List<String> getMatchingImageNamesFromFileSystem (String baseName) {
//		_log.debug ("AlbumImageDiffer.getMatchingImageNamesFromFileSystem: baseName: " + baseName);

		List<String> files = new ArrayList<String> ();

		Path folder = FileSystems.getDefault ().getPath (_basePath, _defaultRootFolder, baseName.substring (0, 1).toLowerCase());
		try (DirectoryStream <Path> ds = Files.newDirectoryStream (folder)) {
			for (Path file : ds) {
				String filename = file.getFileName ().toString ();
//				_log.debug ("AlbumImageDiffer.getMatchingImageNamesFromFileSystem: filename: " + filename);
				if (filename.startsWith (baseName) && filename.endsWith (".jpg")) {
					files.add (filename.substring (0, filename.lastIndexOf ('.')));
				}
			}

		} catch (IOException ee) {
			_log.error ("AlbumImageDiffer.getMatchingImageNamesFromFileSystem(\"" + baseName + "\"): error reading file system", ee);
		}

//		_log.debug ("AlbumImageDiffer.getMatchingImageNamesFromFileSystem: files.size(): " + files.size ());

		return files;
	}

	private List<String> extractQuotedStrings (String string)
	{
		List<String> list = new ArrayList<String> ();

		final Pattern pattern = Pattern.compile ("'([^']*)'");
        Matcher matcher = pattern.matcher (string);
        while (matcher.find ()) {
            list.add(matcher.group (1));
        }

        return list;
	}

	///////////////////////////////////////////////////////////////////////////
	private Connection getConnection ()
	{
		Connection connection = null;

		try {
			connection = getDataSource ().getConnection ();

		} catch (Exception ee) {
			connection = null;
			_log.error ("AlbumImageDiffer.getConnection: error connecting to database", ee);
		}

		return connection;
	}

	///////////////////////////////////////////////////////////////////////////
	private synchronized static BasicDataSource getDataSource ()
	{
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.jdbc.Driver";
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
		_log.debug ("AlbumImages.shutdownExecutor: shutdown executor");
		getExecutor ().shutdownNow ();
	}

	//members from command line
	private static int _numThreads = 4;
	private int _maxRows = 10;
	private int _maxRgbDiffs = 25;
	private Mode _modeA = Mode.NotSet;
	private Mode _modeB = Mode.NotSet;
	private String _whereClauseA = null;
	private String _whereClauseB = null;
	private List<String> _filtersA = null;
	private List<String> _filtersB = null;

	//members
	private Random _random = new Random ();
	private SqlSessionFactory _sqlSessionFactory = null;
	private String _defaultRootFolder = "jroot";

	private List<AlbumImageData> _idListB = null;
	private List<AlbumImageData> _idListA = null;

	private static BasicDataSource _dataSource = null;
	private static AlbumImageDiffer _instance = null;
	private static ExecutorService _executor = null;

	private static final String NL = System.getProperty ("line.separator");
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0"); //as int
	private static final String _basePath = "E:/Netscape/Program/"; //need trailing slash

	private static boolean _Debug = false;
	private static Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumImageDiffer";
}
