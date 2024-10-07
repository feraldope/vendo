//AlbumFileBackup.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
	//entry point for "image backup" feature
	public static void main (String[] args)
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

//		AlbumImageFileDetails.setCompareType (AlbumImageFileDetails.CompareType.Full); //hack

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
		String defaultSubFolderPatternString = "*";
		String subFolderPatternString = defaultSubFolderPatternString;
		String filenamePatternString = "*";
		String sourceRootName = null;
		String destRootName = null;
		double sinceInDays = -1.;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_debug = true;

				} else if (arg.equalsIgnoreCase ("threads") || arg.equalsIgnoreCase ("th")) {
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
					_commandLineArguments.add("/threads " + _numThreads);

				} else if (arg.equalsIgnoreCase ("pattern") || arg.equalsIgnoreCase ("pat")) {
					try {
						filenamePatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_commandLineArguments.add("/pattern " + filenamePatternString);

				} else if (arg.equalsIgnoreCase ("dest") || arg.equalsIgnoreCase ("dst")) {
					try {
						destRootName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_commandLineArguments.add("/dest " + destRootName);

				} else if (arg.equalsIgnoreCase ("sinceDays") || arg.equalsIgnoreCase ("since")) {
					try {
						sinceInDays = Double.parseDouble (args[++ii]);
						if (sinceInDays < 0.) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_commandLineArguments.add("/sinceDays " + sinceInDays);

				} else if (arg.equalsIgnoreCase ("source") || arg.equalsIgnoreCase ("src")) {
					try {
						sourceRootName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_commandLineArguments.add("/source " + sourceRootName);

				} else if (arg.equalsIgnoreCase ("subFolders") || arg.equalsIgnoreCase ("sub")) {
					try {
						subFolderPatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
					_commandLineArguments.add("/subFolders " + subFolderPatternString);

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
		if (!filenamePatternString.toLowerCase ().endsWith ("jpg")) {
			filenamePatternString += "jpg";
		}
		_isPartialBackup = !subFolderPatternString.equals(defaultSubFolderPatternString);
		_subFolderPattern = Pattern.compile ("^" + subFolderPatternString.replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling
		_filenamePattern = Pattern.compile ("^" + filenamePatternString.replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling

		if (sourceRootName == null) {
			displayUsage ("Must specify source root folder", true);
		} else {
			_sourceRootPath = FileSystems.getDefault ().getPath (sourceRootName);
			if (!Files.exists (_sourceRootPath)) {
				_log.error ("AlbumFileRename.processArgs: error source path does not exist: " + _sourceRootPath);
				return false;
			}
		}

		if (destRootName == null) {
			displayUsage ("Must specify destination root folder", true);
		} else {
			_destRootPath = FileSystems.getDefault ().getPath (destRootName);
			if (!Files.exists (_destRootPath)) {
				_log.error ("AlbumFileRename.processArgs: error destination path does not exist: " + _destRootPath);
				return false;
			}
		}

		if (sinceInDays > 0.) {
			_sinceInMillis = AlbumFormInfo.getMillisFromDays(sinceInDays, false);
		}

		if (_debug) {
			String startDateStr = (_sinceInMillis > 0 ? sinceInDays + " (since: " + _dateFormat.format (new Date (_sinceInMillis)) + ")" : "(all days)");
			_log.debug ("AlbumFileBackup.processArgs: sinceInDays: " + startDateStr);
			_log.debug ("AlbumFileBackup.processArgs: filenamePatternString: " + filenamePatternString + " => pattern: " + _filenamePattern.toString());
			_log.debug ("AlbumFileBackup.processArgs: subFolderPatternString: " + subFolderPatternString + " => pattern: " + _subFolderPattern.toString());

			_log.debug ("AlbumFileBackup.processArgs: _sourceRootPath: " + _sourceRootPath.toString ());
			_log.debug ("AlbumFileBackup.processArgs: _destRootPath: " + _destRootPath.toString ());

			_log.debug ("AlbumFileBackup.processArgs: _numThreads: " + _numThreads);
			_log.debug ("AlbumFileBackup.processArgs: _verbose: " + _verbose);
			_log.debug ("");
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

		msg += "Usage: " + _AppName + " [/debug] [/verbose] /source <source folder> /dest <destination folder> [/subFolders <wildName>] [/pattern <wildName>] [/threads <num threads>]";
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
	private boolean run ()
	{
		Instant startInstant = Instant.now ();

		Collection<String> sourceSubFolders = getSubFolders (_sourceRootPath, _subFolderPattern, _sinceInMillis);

//		if (_Debug) {
//			_log.debug ("AlbumFileBackup.run: sourceSubFolders: " + sourceSubFolders);
//		}

		System.out.println ("sourceSubFolders(" + sourceSubFolders.size () + ") = " + sourceSubFolders.stream ().sorted ().collect (Collectors.joining (",")));

		Collection<String> didNotCreate = createDestinationFoldersIfNotExist (_destRootPath, sourceSubFolders);
		if (didNotCreate.size () > 0) {
			_log.error ("AlbumFileBackup.run: failed to create the following folders: " + didNotCreate);
			return false;
		}

		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap = new ConcurrentHashMap<> ();
		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> destMap = new ConcurrentHashMap<> ();
		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> diffMap = new ConcurrentHashMap<> ();
		ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> orphanMap = new ConcurrentHashMap<> ();

		getImageFileDetailsFromFileSystem (sourceSubFolders, sourceMap, destMap);

//		if (_debug) {
//			for (String subFolder : sourceMap.keySet ()) {
//				_log.debug ("AlbumFileBackup.run: sourceSubFolder: " + subFolder + ": files = " + _decimalFormat2.format (sourceMap.get (subFolder).size ()));
//			}
//			for (String subFolder : destMap.keySet ()) {
//				_log.debug ("AlbumFileBackup.run: destSubFolder: " + subFolder + ": files = " + _decimalFormat2.format (destMap.get (subFolder).size ()));
//			}
//		}

		long totalSourceFiles = sourceMap.values ().stream ().mapToLong (Collection::size).sum ();
		long totalDestFiles = destMap.values ().stream ().mapToLong (Collection::size).sum ();

		long totalSourceBytes = sourceMap.values ().stream ().flatMap (Collection::stream).mapToLong (AlbumImageFileDetails::getBytes).sum ();
		long totalDestBytes = destMap.values ().stream ().flatMap (Collection::stream).mapToLong (AlbumImageFileDetails::getBytes).sum ();

		System.out.println ("totalSourceFiles = " + _decimalFormat2.format (totalSourceFiles) + ", totalSourceBytes = " + VendoUtils.unitSuffixScaleBytes(totalSourceBytes));
		System.out.println ("totalDestFiles = " + _decimalFormat2.format (totalDestFiles) + ", totalDestBytes = " + VendoUtils.unitSuffixScaleBytes(totalDestBytes));
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));
		System.out.println ("");

		System.out.println ("determining folders to backup...");

		getSourceDestFolderDiffs (sourceMap, destMap, diffMap, orphanMap);

		if (diffMap.isEmpty ()) {
			System.out.println ("-> no folders to backup");
		}
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));
		System.out.println ("");

		System.out.println ("writing file lists...");

		String timestamp = new SimpleDateFormat ("yyyyMMdd.HHmmss").format (new Date ());
		String partialStub = _isPartialBackup ? ".partial" : "";
//		String orphan1Filename = "fileList." +  timestamp + ".orphan1.log";
//		String orphan2Filename = "fileList." +  timestamp + ".orphan2.log";
//		String orphan3Filename = "fileList." +  timestamp + ".orphan3.log";

		List<String> sourceFileList = getSourceFileList (sourceMap, null);
		String sourceFilename = "fileList." +  timestamp + partialStub + ".sources." + sourceFileList.size () + "rows.log";
		Path outputFilePath = FileSystems.getDefault ().getPath (_destRootPath.toString (), sourceFilename);
		int linesWritten = writeSourceFileList (sourceFileList, outputFilePath);
		System.out.println (_decimalFormat2.format (linesWritten) + " lines written to " + outputFilePath);

		List<String> orphanFileList = getSourceFileList (orphanMap, _destRootPath);
		String orphanFilename = "fileList." +  timestamp + partialStub + ".orphans." + orphanFileList.size () + "rows.log";
		outputFilePath = FileSystems.getDefault ().getPath (_destRootPath.toString (), orphanFilename);
		linesWritten = writeSourceFileList (orphanFileList, outputFilePath);
		System.out.println (_decimalFormat2.format (linesWritten) + " lines written to " + outputFilePath);

//		List<String> orphan2FileList = reduceSourceFileList (orphanMap, _destRootPath, false);
//		outputFilePath = FileSystems.getDefault ().getPath (_destRootPath.toString (), orphan2Filename);
//		linesWritten = writeSourceFileList (orphan2FileList, outputFilePath);
//		System.out.println (_decimalFormat2.format (linesWritten) + " lines written to " + outputFilePath.toString ());

//		List<String> orphan3FileList = reduceSourceFileList (orphanMap, _destRootPath, true);
//		outputFilePath = FileSystems.getDefault ().getPath (_destRootPath.toString (), orphan3Filename);
//		linesWritten = writeSourceFileList (orphan3FileList, outputFilePath);
//		System.out.println (_decimalFormat2.format (linesWritten) + " lines written to " + outputFilePath.toString ());

		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));
		System.out.println ("");

		System.out.println ("folders to backup(" + diffMap.size () + ") = " + diffMap.keySet ().stream ().sorted ().collect (Collectors.joining (",")));

		if (_debug) {
			for (String subFolder : diffMap.keySet ()) {
				Collection<AlbumImageFileDetails> diffColl = diffMap.get (subFolder);
				long numBytesToCopy = diffColl.stream ().mapToLong (AlbumImageFileDetails::getBytes).sum ();
				_log.debug ("AlbumFileBackup.run: diffSubFolder: " + subFolder + ": files = " + _decimalFormat2.format (diffColl.size ()) + " (" + VendoUtils.unitSuffixScaleBytes(numBytesToCopy) + ")");
			}
		}

		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));
		System.out.println ("");

		long totalDiffFiles = diffMap.values ().stream ().mapToLong (Collection::size).sum ();
		long totalDiffBytes = diffMap.values ().stream ().flatMap (Collection::stream).mapToLong (AlbumImageFileDetails::getBytes).sum ();
		long usableBytes = getUsableBytesOnDrive (_destRootPath);

		System.out.println ("totalDiffFiles = " + _decimalFormat2.format (totalDiffFiles) +
							", totalDiffBytes = " + VendoUtils.unitSuffixScaleBytes(totalDiffBytes) +
							", usableBytes on destination = " + VendoUtils.unitSuffixScaleBytes(usableBytes));

		if (usableBytes < addPercentPadding (totalDiffBytes, 10)) {
			System.out.println ("Error: not enough space on destination drive. Aborting.");
			return false;
		}

		copyFiles (diffMap);

		shutdownExecutor ();

//		System.out.println (Duration.between (startInstant, Instant.now ())); //default ISO-8601 seconds-based representation
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//get list of subfolders (relative paths only, i.e., 'aa', 'ab', etc.)
	public Collection<String> getSubFolders (Path rootPath, Pattern subFolderPattern, long sinceInMillis)
	{
		Collection<String> subFolders;

		List<File> list = Arrays.asList (new File (rootPath.toString ()).listFiles (File::isDirectory));

		subFolders = list.stream ()
						 .filter (f -> f.lastModified() >= sinceInMillis)
						 .map (f -> f.getName ().toLowerCase ())
						 .filter (subFolderPattern.asPredicate ())
						 .sorted (VendoUtils.caseInsensitiveStringComparator)
						 .collect (Collectors.toList ());

		return subFolders;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<String> getSourceFileList (ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap, Path rootPath)
	{
		String rootFolder = rootPath != null ? rootPath + "\\" : "";
		List<String> list = new ArrayList<> ();
		for (String subFolder : new TreeSet<> (sourceMap.keySet ())) {
			list.addAll (sourceMap.get (subFolder).stream ()
												  .map(v -> rootFolder + subFolder + "\\" + v.getName())
												  .sorted(VendoUtils.caseInsensitiveStringComparator)
												  .collect(Collectors.toList()));
		}

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
//	private List<String> reduceSourceFileList (ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap, Path rootPath, boolean collapseGroups)
//	{
//		String rootFolder = rootPath != null ? rootPath + "\\" : "";
//		Map<String, Integer> reduceMap = new HashMap<> ();
//		for (String subFolder : new TreeSet<> (sourceMap.keySet ())) {
//			Collection<AlbumImageFileDetails> files = sourceMap.get (subFolder);
//			for (AlbumImageFileDetails file : files) {
//				String imageBaseName = rootFolder + subFolder + "\\" + getBaseName (file.getName (), collapseGroups);
//				int count = 0;
//				if (reduceMap.get (imageBaseName) != null) {
//					count = reduceMap.get (imageBaseName);
//				}
//				reduceMap.put (imageBaseName, ++count);
//			}
//		}
//
//		List<String> reduceList = new ArrayList<> ();
//		for (String filename : new TreeSet<> (reduceMap.keySet ())) {
//			reduceList.add (filename + " " + reduceMap.get (filename));
//		}
//
//		return reduceList;
//	}

	///////////////////////////////////////////////////////////////////////////
	private int writeSourceFileList (List<String> sourceFileList, Path outputFilePath)
	{
		int linesWritten = 0;

		String commandLineArguments = _commandLineArguments.stream().sorted(VendoUtils.caseInsensitiveStringComparator).collect(Collectors.joining(" "));

		try (FileOutputStream outputStream = new FileOutputStream (outputFilePath.toFile ());
			 PrintWriter printWriter = new PrintWriter (outputStream)) {
			printWriter.write("REM command line args: " + commandLineArguments + NL);
			for (String sourceFile : sourceFileList) {
				printWriter.write (sourceFile + NL);
				++linesWritten;
			}

		} catch (IOException ee) {
			_log.error ("AlbumFileBackup.writeSourceFileList: error writing output file: " + outputFilePath + NL);
			_log.error (ee); //print exception, but no stack trace
		}

		return linesWritten;
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<String> createDestinationFoldersIfNotExist (Path destRootPath, Collection<String> sourceSubFolders)
	{
		ConcurrentHashMap<String, String> didNotCreateMap = new ConcurrentHashMap<> ();

		final CountDownLatch endGate = new CountDownLatch (sourceSubFolders.size ());

		for (String subFolder : sourceSubFolders) {
			Runnable task = () -> {
				Thread.currentThread ().setName (subFolder);
				if (!createDestinationFolderIfNotExist (destRootPath, subFolder)) {
					didNotCreateMap.put (subFolder, subFolder);
				}
				endGate.countDown ();
			};
			getExecutor ().execute (task);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.createDestinationFoldersIfNotExist: endGate: ", ex);
		}

		return new ArrayList<> (didNotCreateMap.keySet ());
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean createDestinationFolderIfNotExist (Path destRootPath, String sourceSubFolder)
	{
		boolean status = true;

		Path destPath = FileSystems.getDefault ().getPath (destRootPath.toString (), sourceSubFolder);

		if (!Files.exists (destPath) || !Files.isDirectory (destPath)) {
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
		_remainingFoldersToBeRead.set (2 * sourceSubFolders.size ());

		System.out.print (NL + "remaining folders to be read: ");

		final CountDownLatch endGate = new CountDownLatch (2 * sourceSubFolders.size ());

		for (String subFolder : sourceSubFolders) {
			Runnable sourceTask = () -> {
				Thread.currentThread ().setName ("source: " + subFolder);
				Path sourcePath = FileSystems.getDefault ().getPath (_sourceRootPath.toString (), subFolder);
				Collection<AlbumImageFileDetails> imageFileDetailsColl = getImageFileDetailsFromFileSystem (sourcePath, _filenamePattern);
				sourceMap.put (subFolder, imageFileDetailsColl);
				endGate.countDown ();
			};
			getExecutor ().execute (sourceTask);

			Runnable destTask = () -> {
				Thread.currentThread ().setName ("source: " + subFolder);
				Path destPath = FileSystems.getDefault ().getPath (_destRootPath.toString (), subFolder);
				Collection<AlbumImageFileDetails> imageFileDetailsColl = getImageFileDetailsFromFileSystem (destPath, _filenamePattern);
				destMap.put (subFolder, imageFileDetailsColl);
				endGate.countDown ();
			};
			getExecutor ().execute (destTask);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.getImageFileDetailsFromFileSystem: endGate: ", ex);
		}

		System.out.println(""); //print a blank line after the _remainingFoldersToBeRead output printed below

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<AlbumImageFileDetails> getImageFileDetailsFromFileSystem (Path folder, Pattern filenamePattern)
	{
//		AlbumProfiling.getInstance ().enter (7, subFolder);

		final Instant startInstant = Instant.now ();
//		if (_verbose) {
//			_log.debug ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): starting...");
//		}

//TODO use AtomicReference? - is this thread-safe?
		final List<String> lastPathHandled = new ArrayList<>(Collections.singletonList("")); //for debugging (final List<> is hack

		Set<AlbumImageFileDetails> coll = new HashSet<> ();

		try {
			Files.walkFileTree (folder, new SimpleFileVisitor<Path> () {
				@Override
				public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
				{
					lastPathHandled.set(0, file.toString());

					String filename = file.getFileName ().toString ();
					if (filenamePattern.matcher (filename).matches ()) {
						long numBytes = attrs.size ();
						long modified = attrs.lastModifiedTime ().toMillis ();

						coll.add (new AlbumImageFileDetails (filename, numBytes, modified));
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed (Path file, IOException ex)
				{
					lastPathHandled.set(0, file.toString());

					if (ex != null) {
						_log.error ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): error: ", new Exception ("visitFileFailed", ex));
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory (Path dir, IOException ex)
				{
					lastPathHandled.set(0, dir.toString());

					if (ex != null) {
						_log.error ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): error: ", new Exception ("postVisitDirectory", ex));
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (Exception ex) {
//			throw new AssertionError ("Files#walkFileTree(\"" + folder + "\") will not throw IOException if the FileVisitor does not");
			String hightlight = "******************************************************************************************";
			_log.error (NL + hightlight + NL + "AlbumFileBackup.getImageFileDetailsFromFileSystem: lastPathHandled = '" + lastPathHandled.get(0) + "'" + NL + hightlight + NL, ex);
		}

//		_log.debug ("AlbumFileBackup.getImageFileDetailsFromFileSystem (\"" + folder + "\"): coll.size () = " + coll.size ());

		System.out.print (_remainingFoldersToBeRead.decrementAndGet () + ",");
		System.out.flush();

//		AlbumProfiling.getInstance ().exit (7, subFolder);

//		if (_verbose) {
//			Duration duration = Duration.between (startInstant, Instant.now ());
//			_log.debug ("AlbumFileBackup.getImageFileDetailsFromFileSystem(\"" + folder + "\"): complete" +
//							", elapsed: " + LocalTime.ofNanoOfDay (duration.toNanos ()).format (_dateTimeFormatter) +
//							", remaining folders: " + _remainingFoldersToBeRead.get ());
//		}

		return coll;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean getSourceDestFolderDiffs (ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> sourceMap,
											  ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> destMap,
											  ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> diffMap,
											  ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> orphanMap)
	{
		final CountDownLatch endGate = new CountDownLatch (sourceMap.keySet ().size ());

//TODO - handle case where sourceMap.keySet() is not the same as destMap.keySet()) {

		for (String subFolder : sourceMap.keySet ()) {
			final Collection<AlbumImageFileDetails> sourceColl = sourceMap.get (subFolder);
			final Collection<AlbumImageFileDetails> destColl = destMap.get (subFolder);

			Runnable task = () -> {
				final Instant startInstant = Instant.now ();

				Thread.currentThread ().setName ("diff: " + subFolder);

				final Collection<AlbumImageFileDetails> diffColl = new HashSet<> (sourceColl);
				diffColl.removeAll (destColl);
				if (diffColl.size () > 0) {
					diffMap.put (subFolder, diffColl);
				}

				final Collection<AlbumImageFileDetails> orphanColl = new HashSet<> (destColl);
				orphanColl.removeAll (sourceColl);
				if (orphanColl.size () > 0) {
					orphanMap.put (subFolder, orphanColl);
				}

				Duration duration = Duration.between (startInstant, Instant.now ());

				if (diffColl.size () > 0 || duration.getSeconds () > 0) {
					_log.debug ("AlbumFileBackup.getSourceDestFolderDiffs: " + subFolder + ": diffColl.size: " + diffColl.size () +
								", elapsed: " + LocalTime.ofNanoOfDay (duration.toNanos ()).format (_dateTimeFormatter));
				}

				endGate.countDown ();
			};
			getExecutor ().execute (task);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.getSourceDestFolderDiffs: endGate: ", ex);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private long copyFiles (ConcurrentHashMap<String, Collection<AlbumImageFileDetails>> diffMap)
	{
		final CountDownLatch endGate = new CountDownLatch (diffMap.keySet ().size ());
		final long numTotalFoldersToCopy = diffMap.keySet ().size ();
		final long numTotalFilesToCopy = diffMap.values ().stream ().mapToLong (Collection::size).sum ();

		Map<String, Long> diffBytesMap = new HashMap<> ();
		for (String subFolder : diffMap.keySet ()) {
			final Collection<AlbumImageFileDetails> diffColl = diffMap.get(subFolder);
			long diffCollBytes = diffColl.stream ().mapToLong (AlbumImageFileDetails::getBytes).sum ();
			diffBytesMap.put (subFolder, diffCollBytes);
		}

		//NOTE this is the order we create the tasks, but not necessarily the order they will start (or run??)
		List<String> foldersSortedByNumBytes = diffBytesMap.keySet ().stream ()
						.sorted ((f1, f2) -> {
							return diffBytesMap.get (f2).compareTo(diffBytesMap.get (f1)); //sort in descending order
						})
						.collect (Collectors.toList ());

		AtomicLong numTotalFilesCopied = new AtomicLong ();
		for (String subFolder : foldersSortedByNumBytes) {
			final Collection<AlbumImageFileDetails> diffColl = diffMap.get (subFolder);
			Runnable task = () -> {
				Thread.currentThread ().setName ("copy: " + subFolder);

				if (_debug) {
					long numBytesToCopy = diffColl.stream ().mapToLong (AlbumImageFileDetails::getBytes).sum ();
					_log.debug ("AlbumFileBackup.copyFiles: " + subFolder + ": files to copy: " + _decimalFormat2.format (diffColl.size ()) + " (" + VendoUtils.unitSuffixScaleBytes(numBytesToCopy) + ")");
				}

				long numSubfolderFilesCopied = 0;
				for (AlbumImageFileDetails imageFileDetails : diffColl) {
					Path sourcePath = FileSystems.getDefault ().getPath (_sourceRootPath.toString (), subFolder, imageFileDetails.getName ());
					Path destPath = FileSystems.getDefault ().getPath (_destRootPath.toString (), subFolder, imageFileDetails.getName ());

					if (_testMode) {
						//do not copy
					} else {
						try {
							Files.copy (sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

							++numSubfolderFilesCopied;
							numTotalFilesCopied.incrementAndGet ();

							if (_verbose) {
								String stats = "(" + _decimalFormat2.format (numTotalFilesCopied.get ()) + " of " + _decimalFormat2.format (numTotalFilesToCopy) + ")";
								_log.debug ("AlbumFileBackup.copyFiles: " + stats + " copied file: " + destPath);
							}

						} catch (Exception ex) {
							_log.error ("AlbumFileBackup.copyFiles: Files.copy failed to write to '" + destPath + "'. " + NL + ex);
						}
					}
				}

				if (_debug) {
					_log.debug ("AlbumFileBackup.copyFiles: " + subFolder + ":  filesCopied: " + _decimalFormat2.format (numSubfolderFilesCopied)
							+ " (" + (endGate.getCount() - 1) + " of " + numTotalFoldersToCopy + " folders remain)");
				}

				endGate.countDown ();
			};
			getExecutor ().execute (task);
		}

		try {
			endGate.await ();
		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.copyFiles: endGate: ", ex);
		}

		return numTotalFilesCopied.get ();
	}

	///////////////////////////////////////////////////////////////////////////
	private long getUsableBytesOnDrive (Path path)
	{
		long usableBytes = 0;

		try {
			FileStore store = Files.getFileStore (path);
			usableBytes = store.getUsableSpace ();

		} catch (Exception ex) {
			_log.error ("AlbumFileBackup.getUsableBytesOnDrive: ", ex);
		}

		return usableBytes;
	}

	///////////////////////////////////////////////////////////////////////////
	long addPercentPadding (double value, double percentPadding)
	{
		return (long) Math.ceil (value * (1 + percentPadding / 100));
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getBaseName (String name, boolean collapseGroups)
	{
		final String regex1 = "-\\d.*$";		//match trailing [dash][digit][anything else]
		final String regex2 = "\\d*\\-\\d.*$";	//match trailing [digits][dash][digit][anything else]

		return name.replaceAll (collapseGroups ? regex2 : regex1, "");
	}

//not necessary: functionality provided by StandardCopyOption.COPY_ATTRIBUTES
	///////////////////////////////////////////////////////////////////////////
//	private static boolean setFileDateTime (Path sourcePath, Path destPath)
//	{
//		File sourceFile = sourcePath.toFile ();
//		File destFile = destPath.toFile ();
//
//		long lastModified = sourceFile.lastModified ();
//		boolean status = destFile.setLastModified (lastModified);
//
//		return status;
//	}

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
	private int _numThreads = 4;
	private long _sinceInMillis = -1;
	private boolean _verbose = false;
	private boolean _debug = false;
	private boolean _testMode = false;
	private boolean _isPartialBackup = false;

	private Path _sourceRootPath = null;
	private Path _destRootPath = null;

	private Pattern _filenamePattern;
	private Pattern _subFolderPattern;

	private final AtomicInteger _remainingFoldersToBeRead = new AtomicInteger ();
	private final List<String> _commandLineArguments = new ArrayList<> ();

	private /*static*/ ExecutorService _executor = null;

	private static final String NL = System.getProperty ("line.separator");

//	private static final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
//	private static final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)
	private static final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("HH'h':mm'm':ss's'"); //for example: 01h:03m:12s (note this wraps values >= 24 hours)

	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm"); //Note SimpleDateFormat is not thread safe
	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	private static final Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumFileBackup";
}
