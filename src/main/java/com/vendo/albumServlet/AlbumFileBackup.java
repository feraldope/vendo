//AlbumFileBackup.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class AlbumFileBackup
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new VUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
//	static {
//		Runtime.getRuntime ().addShutdownHook (new Thread (() -> {
//			VendoUtils.sleepMillis (200);
//			//System.out.println ("Shutting down ...");
//			System.out.println ("*************************** Intercepted Ctrl+C ***************************");
//			//some cleaning up code...
//		}));
//	}

	///////////////////////////////////////////////////////////////////////////
	//entry point for "update database" feature
	public static void main (String args[])
	{
//TODO - change CLI to read properties file, too

		AlbumFormInfo.getInstance (); //call ctor to load class defaults

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
//		AlbumFormInfo._profileLevel = 0; //enable profiling just before calling run()
		AlbumFormInfo._profileLevel = 7;

		AlbumFileBackup albumFileBackup = new AlbumFileBackup ();

		if (!albumFileBackup.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

//		AlbumFormInfo._profileLevel = 5; //enable profiling just before calling run()

		AlbumImageFileDetails.setCompareType (AlbumImageFileDetails.CompareType.Full); //hack

		try {
			albumFileBackup.run ();

		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.main: ", ex);
		}

//		_log.debug ("AlbumFileBackup.main - leaving main");
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String[] args)
	{
		String filenamePatternString = ".*";
		String subFolderPatternString = ".*";
		String sourceRootName = null;
		String destRootName = null;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_debug = true;

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

				} else if (arg.equalsIgnoreCase ("pattern") || arg.equalsIgnoreCase ("ptn")) {
					try {
						filenamePatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("dest") || arg.equalsIgnoreCase ("dst")) {
					try {
						destRootName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("source") || arg.equalsIgnoreCase ("src")) {
					try {
						sourceRootName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("subFolders") || arg.equalsIgnoreCase ("sub")) {
					try {
						subFolderPatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("test") || arg.equalsIgnoreCase ("t")) {
					_testMode = true;

				} else if (arg.equalsIgnoreCase ("verbose") || arg.equalsIgnoreCase ("v")) {
					_verbose = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//check for required args and handle defaults
		_filenamePattern = Pattern.compile ("^" + filenamePatternString.replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling
		_subFolderPattern = Pattern.compile ("^" + subFolderPatternString.replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling

		if (sourceRootName == null) {
			displayUsage ("Must specify source root folder", true);
		} else {
			_sourceRootPath = FileSystems.getDefault ().getPath (sourceRootName);
		}

		if (destRootName == null) {
			displayUsage ("Must specify destination root folder", true);
		} else {
			_destRootPath = FileSystems.getDefault ().getPath (destRootName);
		}

		if (_debug) {
			_log.debug ("AlbumFileBackup.processArgs: filenamePatternString: " + filenamePatternString);
			_log.debug ("AlbumFileBackup.processArgs: subFolderPatternString: " + subFolderPatternString);
//			_log.debug ("AlbumFileBackup.processArgs: _filenamePattern: " + _filenamePattern.toString ());
//			_log.debug ("AlbumFileBackup.processArgs: _subFolderPattern: " + _subFolderPattern.toString ());

			_log.debug ("AlbumFileBackup.processArgs: _sourceRootPath: " + _sourceRootPath.toString ());
			_log.debug ("AlbumFileBackup.processArgs: _destRootPath: " + _destRootPath.toString ());

			_log.debug ("AlbumFileBackup.processArgs: _numThreads: " + _numThreads);
			_log.debug ("AlbumFileBackup.processArgs: _verbose: " + _verbose);
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

		msg += "Usage: " + _AppName + " [/debug] /source <source folder> /dest <destination folder> [/subFolders <wildName>] [/pattern <wildName>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumFileBackup ()
	{
		if (_debug) {
			_log.debug ("AlbumFileBackup ctor");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private void run ()
	{
		Instant startInstant = Instant.now ();

		Collection<String> sourceSubFolders = getSubFolders (_sourceRootPath, _subFolderPattern);

//		if (_Debug) {
//			_log.debug ("AlbumFileBackup.run: sourceSubFolders: " + sourceSubFolders);
//		}

		System.out.println ("sourceSubFolders = " + sourceSubFolders.stream ().sorted ().collect (Collectors.joining (",")));

		Collection<String> didNotCreate = createDestinationFoldersIfNotExist (_destRootPath, sourceSubFolders);
		if (didNotCreate.size () > 0) {
			_log.error("AlbumFileBackup.run: failed to create the following folders: " + didNotCreate);
		}

		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap = new ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> ();
		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> destMap = new ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> ();
		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> diffMap = new ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> ();

		getImageFileDetailsFromFileSystem (sourceSubFolders, sourceMap, destMap);

		if (_debug) {
			for (String subFolder : sourceMap.keySet ()) {
				_log.debug ("AlbumFileBackup.run: sourceSubFolder: " + subFolder + ": files = " + _decimalFormat2.format (sourceMap.get(subFolder).size ()));
			}
			for (String subFolder : destMap.keySet ()) {
				_log.debug ("AlbumFileBackup.run: destSubFolder: " + subFolder + ": files = " + _decimalFormat2.format (destMap.get(subFolder).size ()));
			}
		}

		long totalSourceFiles = sourceMap.values ().stream ().mapToLong (Collection::size).sum ();
		long totalDestFiles = destMap.values ().stream ().mapToLong (Collection::size).sum ();

		long totalSourceBytes = sourceMap.values ().stream ().flatMap (Collection::stream).mapToLong (AlbumImageFileDetails::getBytes).sum ();
		long totalDestBytes = destMap.values ().stream ().flatMap (Collection::stream).mapToLong (AlbumImageFileDetails::getBytes).sum ();

		System.out.println ("totalSourceFiles = " + _decimalFormat2.format (totalSourceFiles) + ", totalSourceBytes = " + VendoUtils.unitSuffixScale (totalSourceBytes));
		System.out.println ("totalDestFiles = " + _decimalFormat2.format (totalDestFiles) + ", totalDestBytes = " + VendoUtils.unitSuffixScale (totalDestBytes));

		System.out.println ("determining folders to backup...");

		getSourceDestFolderDiffs (sourceMap, destMap, diffMap);

		System.out.println ("folders to backup: " + diffMap.keySet ().stream ().sorted ().collect (Collectors.joining (",")));

		if (_debug) {
			for (String subFolder : diffMap.keySet ()) {
				Collection<AlbumImageFileDetails> diffColl = diffMap.get (subFolder);
				_log.debug("AlbumFileBackup.run: diffSubFolder: " + subFolder + ": files = " + _decimalFormat2.format (diffColl.size ()));
			}
		}

		long totalDiffFiles = diffMap.values ().stream ().mapToLong (Collection::size).sum ();
		long totalDiffBytes = diffMap.values ().stream ().flatMap (Collection::stream).mapToLong (AlbumImageFileDetails::getBytes).sum ();
		System.out.println ("totalDiffFiles = " + _decimalFormat2.format (totalDiffFiles) + ", totalDiffBytes = " + VendoUtils.unitSuffixScale (totalDiffBytes));

		//TODO - verify sufficient free disk space on dest

		copyFiles (diffMap);

		shutdownExecutor ();

		Instant endInstant = Instant.now ();
//		System.out.println (Duration.between (startInstant, endInstant)); //default ISO-8601 seconds-based representation
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format(_dateTimeFormatter));
	}

	///////////////////////////////////////////////////////////////////////////
	//get list of subfolders (relative paths only, i.e., 'aa', 'ab', etc.)
	public Collection<String> getSubFolders (Path rootPath, Pattern subFolderPattern)
	{
		Collection<String> subFolders;

		List<File> list = Arrays.asList (new File (rootPath.toString ()).listFiles (File::isDirectory));

		subFolders = list.stream ()
				.map (v -> v.getName ().toLowerCase ())
				.filter (subFolderPattern.asPredicate ())
				.sorted ()
				.collect (Collectors.toList ());

		return subFolders;
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<String> createDestinationFoldersIfNotExist (Path destRootPath, Collection<String> sourceSubFolders)
	{
		ConcurrentHashMap<String, String> didNotCreateMap = new ConcurrentHashMap<String, String> ();

		final CountDownLatch endGate = new CountDownLatch (sourceSubFolders.size ());

		for (String subFolder : sourceSubFolders) {
			Runnable task = () -> {
				if (!createDestinationFolderIfNotExist (destRootPath, subFolder)) {
					didNotCreateMap.put (subFolder, subFolder);
				}
				endGate.countDown ();
			};
			getExecutor().execute(task);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.createDestinationFoldersIfNotExist: endGate:", ex);
		}

		return new ArrayList<String> (didNotCreateMap.keySet ());
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean createDestinationFolderIfNotExist (Path destRootPath, String sourceSubFolder)
	{
		boolean status = true;

		Path destPath = FileSystems.getDefault ().getPath (destRootPath.toString(), sourceSubFolder);

		if (!Files.exists(destPath) || !Files.isDirectory (destPath)) {
			try {
				Files.createDirectories (destPath);

			} catch (Exception ex) {
				status = false;
//				_log.error ("AlbumFileBackup.createDestinationFolderIfNotExist: failed to create: " + destPath.toString (), ex);
				_log.error ("AlbumFileBackup.createDestinationFolderIfNotExist: " + ex);
			}
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean getImageFileDetailsFromFileSystem (Collection<String> sourceSubFolders,
													   ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap,
													   ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> destMap)
	{
		final CountDownLatch endGate = new CountDownLatch (2 * sourceSubFolders.size ());

		for (String subFolder : sourceSubFolders) {
			Runnable sourceTask = () -> {
				Path sourcePath = FileSystems.getDefault ().getPath (_sourceRootPath.toString (), subFolder);
				Collection<AlbumImageFileDetails> imageFileDetailsColl = getImageFileDetailsFromFileSystem (sourcePath, _filenamePattern);
				sourceMap.put (subFolder, imageFileDetailsColl);
				endGate.countDown ();
			};
			getExecutor ().execute (sourceTask);

			Runnable destTask = () -> {
				Path destPath = FileSystems.getDefault ().getPath (_destRootPath.toString(), subFolder);
				Collection<AlbumImageFileDetails> imageFileDetailsColl = getImageFileDetailsFromFileSystem (destPath, _filenamePattern);
				destMap.put (subFolder, imageFileDetailsColl);
				endGate.countDown ();
			};
			getExecutor ().execute (destTask);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.getImageFileDetailsFromFileSystem: endGate:", ex);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<AlbumImageFileDetails> getImageFileDetailsFromFileSystem (Path folder, Pattern filenamePattern)
	{
//		AlbumProfiling.getInstance ().enter (7, subFolder);

		List<AlbumImageFileDetails> list = new LinkedList<AlbumImageFileDetails>();

		try {
			Files.walkFileTree (folder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
				{
					String filename = file.getFileName ().toString ();
					if (filenamePattern.matcher (filename).matches ()) {
						long numBytes = attrs.size ();
						long modified = attrs.lastModifiedTime ().toMillis ();

						list.add (new AlbumImageFileDetails (filename, numBytes, modified));
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed (Path file, IOException ex)
				{
					if (ex != null) {
						_log.error ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): error: ", new Exception ("visitFileFailed", ex));
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory (Path dir, IOException ex)
				{
					if (ex != null) {
						_log.error ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): error: ", new Exception ("postVisitDirectory", ex));
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (Exception ex) {
			throw new AssertionError ("Files#walkFileTree will not throw IOException if the FileVisitor does not");
		}

//		Collections.sort (list);

//		_log.debug ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): list.size() = " + list.size ());

//		AlbumProfiling.getInstance ().exit (7, subFolder);

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean getSourceDestFolderDiffs (ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap,
											  ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> destMap,
											  ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> diffMap)
	{
//		final CountDownLatch endGate = new CountDownLatch ((_testMode ? 2 : 1) * sourceMap.keySet ().size ());
		final CountDownLatch endGate = new CountDownLatch (sourceMap.keySet ().size ());

		for (String subFolder : sourceMap.keySet ()) {
			final Collection<AlbumImageFileDetails> sourceColl = sourceMap.get (subFolder);
			final Collection<AlbumImageFileDetails> destColl = destMap.get (subFolder);
/*
			Runnable task = () -> {
				final Instant startInstant = Instant.now ();
				final Collection<AlbumImageFileDetails> diffColl = new HashSet<AlbumImageFileDetails> (sourceColl);
				diffColl.removeAll (destColl);
				if (diffColl.size () > 0) {
					diffMap.put(subFolder, diffColl);
				}
				Instant endInstant = Instant.now ();
				_log.debug("AlbumFileBackup.getSourceDestFolderDiffs: diffColl.size = " + diffColl.size ());
				_log.debug("AlbumFileBackup.getSourceDestFolderDiffs: elapsed@1: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format(_dateTimeFormatter));
				endGate.countDown ();
			};
			getExecutor ().execute (task);
*/
//			if (_testMode) {
				Runnable task2 = () -> {
//					final Instant startInstant = Instant.now ();

					final List<AlbumImageFileDetails> sourceList = new LinkedList<AlbumImageFileDetails> (sourceColl);
					final List<AlbumImageFileDetails> destList = new LinkedList<AlbumImageFileDetails> (destColl);
					sourceList.sort (AlbumImageFileDetails::compareTo);
					destList.sort (AlbumImageFileDetails::compareTo);

					final List<AlbumImageFileDetails> diffList = new LinkedList<AlbumImageFileDetails> ();

					VendoUtils.removeAll (sourceList, destList, diffList, null);

					final Collection<AlbumImageFileDetails> diffColl = new HashSet<AlbumImageFileDetails> (diffList);
					if (diffColl.size () > 0) {
						diffMap.put(subFolder, diffColl);
					}

//	public static <T extends Comparable> void removeAll (Collection<T> in0, Collection<T> in1, Collection<T> out0, Collection<T> out1)
//					final Collection<AlbumImageFileDetails> diffColl = new LinkedList<AlbumImageFileDetails> (sourceColl);
//					diffColl.removeAll (destColl);
//					if (diffColl.size () > 0) {
//						diffMap.put(subFolder, diffColl);
//					}
//					Instant endInstant = Instant.now ();
//					_log.debug("AlbumFileBackup.getSourceDestFolderDiffs: diffColl.size = " + diffList.size ());
//					_log.debug("AlbumFileBackup.getSourceDestFolderDiffs: elapsed@2: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format(_dateTimeFormatter));
					endGate.countDown ();
				};
				getExecutor ().execute (task2);
			}
//		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.getSourceDestFolderDiffs: endGate:", ex);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private long copyFiles (ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> diffMap)//, Collection<AlbumImageFileDetails>> failedMap)
	{
		final CountDownLatch endGate = new CountDownLatch (diffMap.keySet ().size ());

		final long numTotalFilesToCopy = diffMap.values ().stream ().mapToLong (Collection::size).sum ();

		AtomicLong numTotalFilesCopied = new AtomicLong ();
		for (String subFolder : diffMap.keySet ()) {
			final Collection<AlbumImageFileDetails> diffColl = diffMap.get (subFolder);
			Runnable task = () -> {
				if (_debug) {
					_log.debug("AlbumFileBackup.copyFiles: " + subFolder + ": files to copy: " + _decimalFormat2.format (diffColl.size ()));
				}

				long numSubfolderFilesCopied = 0;
				for (AlbumImageFileDetails imageFileDetails : diffColl) {
					Path sourcePath = FileSystems.getDefault().getPath(_sourceRootPath.toString(), subFolder, imageFileDetails.getName());
					Path destPath = FileSystems.getDefault().getPath(_destRootPath.toString(), subFolder, imageFileDetails.getName());

					if (_testMode) {

					} else {
						try {
							Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

							if (!setFileDateTime(sourcePath, destPath)) {
								_log.error("AlbumFileBackup.copyFiles: File.setLastModified failed for: " + destPath);
							}

							++numSubfolderFilesCopied;
							numTotalFilesCopied.getAndIncrement();

							if (_verbose) {
								String stats = "(" + _decimalFormat2.format(numTotalFilesCopied.get()) + " of " + _decimalFormat2.format(numTotalFilesToCopy) + ")";
								_log.debug("AlbumFileBackup.copyFiles: " + stats + " copied file: " + destPath);
							}

						} catch (Exception ex) {
							_log.error("AlbumFileBackup.copyFiles: Files.copy failed to write to '" + destPath + "'. " + ex.toString());
						}
					}
				}

				if(_debug) {
					_log.debug("AlbumFileBackup.copyFiles: " + subFolder + ": filesCopied: " + _decimalFormat2.format (numSubfolderFilesCopied));
				}

				endGate.countDown ();
			};
			getExecutor ().execute (task);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.copyFiles: endGate:", ex);
		}

		return numTotalFilesCopied.get ();
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean setFileDateTime (Path sourcePath, Path destPath)
	{
		File sourceFile = sourcePath.toFile ();
		File destFile = destPath.toFile ();

		long lastModified = sourceFile.lastModified ();
		boolean status = destFile.setLastModified (lastModified);

//		if (!status) {
//			_log.warn ("AlbumFileBackup.setFileDateTime: File.setLastModified failed for: " + destPath);
//		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	public synchronized ExecutorService getExecutor ()
	{
		if (_executor == null || _executor.isTerminated () || _executor.isShutdown ()) {
			_executor = Executors.newFixedThreadPool (_numThreads);
		}

		return _executor;
	}

	///////////////////////////////////////////////////////////////////////////
	public void shutdownExecutor ()
	{
		_log.debug ("AlbumFileBackup.shutdownExecutor: shutdown executor");
		getExecutor ().shutdownNow ();
	}


	//members
//	private static boolean _isCLI = false; //true if running as CLI, false for servlet

	private int _numThreads = 4;
	private boolean _verbose = false;
	private boolean _debug = false;
	private boolean _testMode = false;

	private Path _sourceRootPath = null;
	private Path _destRootPath = null;

	private Pattern _filenamePattern;
	private Pattern _subFolderPattern;

	private static ExecutorService _executor = null;

	private static final String NL = System.getProperty ("line.separator");

//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	private static Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumFileBackup";
}
