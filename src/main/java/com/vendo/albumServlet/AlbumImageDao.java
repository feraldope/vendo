//AlbumImageDao.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VendoUtils;
import com.vendo.vendoUtils.WatchDir;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vendo.vendoUtils.VendoUtils.fileExists;
import static com.vendo.vendoUtils.VendoUtils.sleepMillis;


public class AlbumImageDao {
	///////////////////////////////////////////////////////////////////////////
	static {
		Thread.setDefaultUncaughtExceptionHandler(new AlbumImageDaoUncaughtExceptionHandler());
	}

	///////////////////////////////////////////////////////////////////////////
	//entry point for "update database" feature
	public static void main(String[] args) {
//TODO - change CLI to read properties file, too

		_log.debug("AlbumImageDao.main - entering main");

		AlbumFormInfo.getInstance(); //call ctor to load class defaults

//		_isCLI = true;

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
//		AlbumFormInfo._profileLevel = 0; //enable profiling just before calling run()
		AlbumFormInfo._profileLevel = 7;

//		AlbumProfiling.getInstance ().enter/*AndTrace*/ (1); //called in run1()

		AlbumImageDao albumImageDao = AlbumImageDao.getInstance();

		if (!albumImageDao.processArgs(args)) {
			System.exit(1); //processArgs displays error
		}

//		AlbumFormInfo._profileLevel = 5; //enable profiling just before calling run()

		//call this again after parsing args
//not currently necessary (was used for subFolderOverride)
//		albumImageDao.getAlbumSubFolders();

		try {
			albumImageDao.run();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.main: ", ee);
		}

//		AlbumProfiling.getInstance ().exit (1); //called in run1()

		_log.debug("AlbumImageDao.main - leaving main");
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs(String[] args)
	{
		String subFoldersOverrideStr = "";

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith("-") || arg.startsWith("/")) {
				arg = arg.substring(1);

				if (arg.equalsIgnoreCase("debug") || arg.equalsIgnoreCase("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase("server")) {
					_isServer = true;

				} else if (arg.equalsIgnoreCase("subFolders") || arg.equalsIgnoreCase("sub") || arg.equalsIgnoreCase("s")) {
					try {
						_subFolders = null; //force this to be recalculated
						subFoldersOverrideStr = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase("syncOnly") || arg.equalsIgnoreCase("sync")) {
					_syncOnly = true;

				} else {
					displayUsage("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				displayUsage("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//check for required args and handle defaults
		//...

		if (!subFoldersOverrideStr.isEmpty()) {
			List<String> subFolderOverrides = Arrays.stream(subFoldersOverrideStr.toLowerCase().split(",")).map(String::trim).collect(Collectors.toList());

			for (String subFolderOverride : subFolderOverrides) {
				boolean foundMatch = false;
				if (subFolderOverride.contains("*")) {
					Pattern pattern = Pattern.compile(subFolderOverride.replaceAll("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling

					for (final String subFolder : getAlbumSubFolders()) {
						Matcher matcher = pattern.matcher(subFolder);
						if (matcher.matches()) {
							foundMatch = true;
							_subFoldersOverrideSet.add(subFolder);
						}
					}

				} else { // no wildcard
					if (getAlbumSubFolders().contains(subFolderOverride)) {
						foundMatch = true;
						_subFoldersOverrideSet.add(subFolderOverride);
					}
				}

				if (!foundMatch) {
					displayUsage("No matching subfolder found for \"" + subFolderOverride + "\".", true);
				}
			}

			_log.debug("AlbumImageDao.processArgs: subFoldersOverrides = " + _subFoldersOverrideSet.stream().sorted().collect(Collectors.joining(",")));
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage(String message, Boolean exit)
	{
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/subFolders <comma separated list of subfolders; * wildcard allowed>] [/syncOnly]";
		System.err.println("Error: " + msg + NL);

		if (exit) {
			System.exit(1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public static AlbumImageDao getInstance()
	{
		if (_instance == null) {
			synchronized (AlbumImageDao.class) {
				if (_instance == null) {
					_instance = new AlbumImageDao();
				}
			}
		}

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumImageDao() //singleton
	{
		if (AlbumFormInfo._logLevel >= 9) {
			_log.debug("AlbumImageDao ctor");
		}

		String resource = "com/vendo/albumServlet/mybatis-image-server-config.xml";

		try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
			_sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
			_log.debug("AlbumImageDao ctor: loaded mybatis config from " + resource);

		} catch (Exception ee) {
			_log.error("AlbumImageDao ctor: SqlSessionFactoryBuilder.build failed on " + resource);
			_log.error(ee);
		}

		_rootPath = AlbumFormInfo.getInstance().getRootPath(false);

//		getAlbumSubFolders();

		if (getAlbumSubFolders().size() == 0) { //calls getAlbumSubFolders() as a side effect of this test
			_log.error("AlbumImageDao ctor: no subfolders found under " + _rootPath);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void cleanup()
	{
//		if (_pauseThread != null) {
//			_pauseThread.interrupt (); //trigger thread termination
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void cacheMaintenanceImageCache(boolean clearImageCache) {
		if (clearImageCache) {
			_albumImagesDataCache.clear();
			_albumImagesCountCache.clear();
			_albumAlbumsCountCache.clear();
			_albumImagesHasExifDataCache.clear();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private void run()
	{
		_log.debug("--------------- AlbumImageDao.run - syncFolders started ---------------");

		AlbumProfiling.getInstance().enter(1);

		int saveProfileLevel = AlbumFormInfo._profileLevel;
//		AlbumFormInfo._profileLevel = 4; //lower profile level during syncFolder
		AlbumFormInfo._profileLevel = 0; //disable profiling during syncFolder

		//cleanup image counts at startup
		//note that some mismatched entries might be recreated later in syncFolder() if there really are image files in the wrong folders
		deleteZeroCountsFromImageCounts();
		deleteMismatchedEntriesFromImageCounts();

		//sync all subfolders at startup
		final CountDownLatch endGate = new CountDownLatch(getAlbumSubFolders().size());
		for (final String subFolder : getAlbumSubFolders()) {
			Thread thread = new Thread(() -> {
				syncFolder(subFolder);
				endGate.countDown();
			});
			thread.setName("syncFolder:" + subFolder);
			thread.start();
		}
		try {
			endGate.await();
		} catch (Exception ee) {
			_log.error("AlbumImageDao.run: endGate:", ee);
		}

// this does not work for variable-length subfolders; e.g., it does not distinguish between subfolders "ka" and "kat"
//		Map<String, Integer> misMatchedMap = getMismatchedEntriesFromImageCounts();
//		for (String str : misMatchedMap.keySet()) {
//			_log.warn("AlbumImageDao.run: syncFolders: found image files in wrong folder (via query): " + str);
//		}

		AlbumFormInfo._profileLevel = saveProfileLevel; //disable profiling during syncFolder

		AlbumProfiling.getInstance().exit(1);
		AlbumProfiling.getInstance().print(true);

		_log.debug("--------------- AlbumImageDao.run - syncFolders done ---------------");

		if (_syncOnly) {
			return;
		}

//		_pauseThread = watchPauseFile ();

		//threads will run continuously watching for folder modifications
		final SortedSet<String> debugWatchingSubfolders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (String subFolder : getAlbumSubFolders()) {
			createWatcherThreadsForFolder(subFolder);
			debugWatchingSubfolders.add(subFolder);
		}
		_log.debug("AlbumImageDao.run: createWatcherThreadsForFolders(" + debugWatchingSubfolders.size() + "): " + debugWatchingSubfolders);

		List<Thread> allThreads = Stream.concat(_watcherThreadMap.values().stream(), _handlerThreadMap.values().stream()).collect(Collectors.toList());
		for (Thread thread : allThreads) {
			try {
				//wait for all threads to exit
				thread.join();
				//should never get here
				_log.debug("AlbumImageDao.run: thread.join completed for thread: " + thread.getName());

			} catch (Exception ee) {
				_log.error("AlbumImageDao.run: ", ee);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	//populate list of subfolders (relative paths only, i.e., 'aa', 'ab', etc.)
	//used by CLI and servlet
	public synchronized Collection<String> getAlbumSubFolders()
	{
		if (_subFolders == null) {
			List<File> list = Arrays.asList(Objects.requireNonNull(new File(_rootPath).listFiles(File::isDirectory)));
			_subFolders = list.stream()
								.map(v -> v.getName().toLowerCase())
								.collect(Collectors.toList());

			_subFolderByLengthMap = new HashMap<>();
			for (String subFolder : _subFolders) {
				int length = subFolder.length(); //1-based
				Set<String> set = _subFolderByLengthMap.computeIfAbsent(length, k -> new HashSet<>());
				set.add(subFolder);
			}
//			_log.debug("AlbumImageDao.getAlbumSubFolders: _subFolderByLengthMap: " + _subFolderByLengthMap.toString());
		}

		return _subFolders;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private boolean syncFolder(String subFolder)
	{
		if (_subFoldersOverrideSet != null && !_subFoldersOverrideSet.isEmpty()) {
			if (_subFoldersOverrideSet.contains(subFolder)) {
				_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): syncing subFolder = " + subFolder);
			} else {
				return false; //skip
			}
		}

		AlbumProfiling.getInstance().enter(4, "part 1");

		Collection<AlbumImageFileDetails> dbImageFileDetails = getImageFileDetailsFromImages(subFolder); //result is sorted
		if (/*dbImageFileDetails == null ||*/ dbImageFileDetails.isEmpty()) { //empty can happen when a new folder is created
//			throw new RuntimeException("AlbumImageDao.syncFolder(" + subFolder + "): getImageFileDetailsFromImages(" + subFolder + ") returned null/empty");
			_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): getImageFileDetailsFromImages(" + subFolder + ") returned null/empty");
		}
		Collection<AlbumImageFileDetails> fsImageFileDetails = getImageFileDetailsFromFileSystem(subFolder, ".jpg"); //result is sorted
		if (/*fsImageFileDetails == null ||*/ fsImageFileDetails.isEmpty()) { //empty can happen when a new folder is created
//			throw new RuntimeException("AlbumImageDao.syncFolder(" + subFolder + "): getImageFileDetailsFromFileSystem(" + subFolder + ") returned null/empty");
			_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): getImageFileDetailsFromFileSystem(" + subFolder + ") returned null/empty");
		}

		Set<AlbumImageFileDetails> missingFromFs = new HashSet<>(dbImageFileDetails);
		Set<AlbumImageFileDetails> missingFromDb = new HashSet<>(fsImageFileDetails);

		missingFromFs.removeAll(new HashSet<>(fsImageFileDetails));
		missingFromDb.removeAll(new HashSet<>(dbImageFileDetails));

		AlbumProfiling.getInstance().exit(4, "part 1");

		if (missingFromFs.size() > 0) {
			_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): missingFromFs.size() = " + missingFromFs.size() + " (to be removed from database)");

			List<String> sorted = missingFromFs.stream()
												.map(AlbumImageFileDetails::getName)
												.sorted(String.CASE_INSENSITIVE_ORDER)
												.collect(Collectors.toList());
			for (String str : sorted) {
//				_log.debug ("AlbumImageDao.syncFolder(" + subFolder + "): to be removed from database: " + str);
				handleFileDelete(subFolder, str);
			}
		}

		if (missingFromDb.size() > 0) {
			_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): missingFromDb.size() = " + missingFromDb.size() + " (to be added to database)");

			List<String> sorted = missingFromDb.stream()
												.map(AlbumImageFileDetails::getName)
												.sorted(String.CASE_INSENSITIVE_ORDER)
												.collect(Collectors.toList());
			for (String str : sorted) {
//				_log.debug ("AlbumImageDao.syncFolder(" + subFolder + "): to be added to database: " + str);
				handleFileCreate(subFolder, str);
			}
		}

		//sync image counts ---------------------------------------------------

		AlbumProfiling.getInstance().enter(4, "part 2");

		Map<String, Integer> dbImageCounts = getImageCountsFromImageCounts(subFolder);
		Map<String, Integer> fsImageCounts = calculateImageCountsFromFileSystem(subFolder, fsImageFileDetails);

		final Pattern endsWithDigitPattern = Pattern.compile(".*\\d$"); //ends with digit

		//propagate file system image counts into database
		for (Map.Entry<String, Integer> fsEntry : fsImageCounts.entrySet()) {
			String fsBaseName = fsEntry.getKey();
			Integer fsImageCount = fsEntry.getValue();
			Integer dbImageCount = dbImageCounts.get(fsBaseName);

			if (dbImageCount == null || !dbImageCount.equals(fsImageCount)) {
				boolean collapseGroups = !endsWithDigitPattern.matcher(fsBaseName).matches();
//				_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): updating image counts: (\"" + subFolder + "\", \"" + fsBaseName + "\", " + collapseGroups + ", " + fsImageCount + ")");
				updateImageCountsInImageCounts(subFolder, fsBaseName, collapseGroups, fsImageCount);
			}

			dbImageCounts.remove(fsBaseName); //remove this entry
		}
		//dbImageCounts should now only have orphans that need to be deleted from database
//		_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): dbImageCounts: " + dbImageCounts);
		for (Map.Entry<String, Integer> dbEntry : dbImageCounts.entrySet()) {
			String dbBaseName = dbEntry.getKey();
			int dbImageCount = 0; //set count to 0 to cause entry to be deleted

			boolean collapseGroups = !endsWithDigitPattern.matcher(dbBaseName).matches();
//			_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): updating image counts: (\"" + subFolder + "\", \"" + dbBaseName + "\", " + collapseGroups + ", " + dbImageCount + ")");
			updateImageCountsInImageCounts(subFolder, dbBaseName, collapseGroups, dbImageCount);
		}

		AlbumProfiling.getInstance().exit(4, "part 2");

		//update update time in image_folder table ----------------------------

		AlbumProfiling.getInstance().enter(4, "part 3");

		long nowInMillis = new GregorianCalendar().getTimeInMillis();
		insertLastUpdateIntoImageFolder(subFolder, nowInMillis);

		AlbumProfiling.getInstance().exit(4, "part 3");

		//validate image names ------------------------------------------------

		AlbumProfiling.getInstance().enter(4, "part 4");

		dbImageFileDetails = getImageFileDetailsFromImages(subFolder); //result is sorted
		for (AlbumImageFileDetails imageFileDetail : dbImageFileDetails) {
			String nameNoExt = imageFileDetail.getName();
			if (!AlbumImage.isValidImageName(nameNoExt)) {
				_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): warning: invalid image name: " + nameNoExt);
			}
		}

		AlbumProfiling.getInstance().exit(4, "part 4");

		//validate that there are no albums with same name but different case ------------------------------------------------

		AlbumProfiling.getInstance().enter(4, "part 5");

		Set<String> albumsOriginalCase = dbImageFileDetails.stream()
				.map(i -> AlbumImage.getBaseName(i.getName(), true))
				.collect (Collectors.toSet());
		Set<String> albumsLowerCase = dbImageFileDetails.stream()
				.map(i -> AlbumImage.getBaseName(i.getName(), true).toLowerCase())
				.collect (Collectors.toSet());
		int diff = albumsOriginalCase.size() - albumsLowerCase.size();
		if (diff != 0) {
//TODO - print offending albums
			_log.error("AlbumImageDao.syncFolder(" + subFolder + "): found " + diff + " albums with same name and different case in subFolder: " + subFolder);
		}

		AlbumProfiling.getInstance().exit(4, "part 5");

		//validate images are in correct folders ------------------------------------------------

		AlbumProfiling.getInstance().enter(4, "part 6");

		//NOTE: similar code exists in getImagesFromCache() and syncFolder()
		String misFiledStr = dbImageFileDetails.stream()
												.map(AlbumImageFileDetails::getName)
												.filter(s -> !subFolder.equalsIgnoreCase(getSubFolderFromImageName (s)))
												.sorted (VendoUtils.caseInsensitiveStringComparator)
												.collect (Collectors.joining (", "));
		AlbumProfiling.getInstance().exit(4, "part 6");

		if (!misFiledStr.isEmpty()) {
			_log.error("AlbumImageDao.syncFolder(" + subFolder + "): found image files in wrong folder (" + subFolder + "): " + misFiledStr);
		}

		//validate image names have consistent numbering ----------------------

		AlbumProfiling.getInstance().enter(4, "part 7");

		final int numDigitPatterns = 5;
		final List<Pattern> patterns1 = new ArrayList<>(numDigitPatterns);
		final List<Pattern> patterns2 = new ArrayList<>(numDigitPatterns);
		for (int ii = 0; ii < numDigitPatterns; ii++) {
			//note array index is 0-based, but pattern string is 1-based (note last pattern is a range: {n,99} to check for all other instances)
			String numberValue = (ii + 1) + (ii < numDigitPatterns - 1 ? "" : ",99");
			patterns1.add(ii, Pattern.compile("^[A-Za-z]*[0-9]*-[0-9]{" + numberValue + "}$")); //regex [letters][digits][dash][specific number of digits]
			patterns2.add(ii, Pattern.compile("^[A-Za-z]*[0-9]{" + numberValue + "}-[0-9]*$")); //regex [letters][specific number of digits][dash][digits]
		}

		String prevBaseName1 = "";
		String prevBaseName2 = "";
		List<Boolean> hasMatches1 = new ArrayList<>(Collections.nCopies(numDigitPatterns, false));
		List<Boolean> hasMatches2 = new ArrayList<>(Collections.nCopies(numDigitPatterns, false));
		dbImageFileDetails.add(new AlbumImageFileDetails("dummy", 1, 1)); //HACK - add dummy value to the end for loop processing
		for (AlbumImageFileDetails imageFileDetail : dbImageFileDetails) {
			String nameNoExt = imageFileDetail.getName();
			String baseName1 = AlbumImage.getBaseName(nameNoExt, false);
			String baseName2 = AlbumImage.getBaseName(nameNoExt, true);

			//skip some known offenders
//			if (nameNoExt.startsWith("q") || nameNoExt.startsWith("x")) {
			if (nameNoExt.startsWith("qt")) {
				continue;
			}

			if (baseName1.compareToIgnoreCase(prevBaseName1) != 0) {
				//complete processing for this baseName
				String matchList = getMatchList(hasMatches1);
				if (matchList.length() > 1) {
					_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): warning: inconsistent numbering: [" + matchList + "] digits: " + prevBaseName1 + " *******");
				}

				//reset for next baseName
				hasMatches1 = new ArrayList<>(Collections.nCopies(numDigitPatterns, false));
				prevBaseName1 = baseName1;
			}

			if (baseName2.compareToIgnoreCase(prevBaseName2) != 0) {
				//complete processing for this baseName
				String matchList = getMatchList(hasMatches2);
				if (matchList.length() > 1) {
					_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): warning: inconsistent numbering: [" + matchList + "] digits: " + prevBaseName2);
				}

				//reset for next baseName
				hasMatches2 = new ArrayList<>(Collections.nCopies(numDigitPatterns, false));
				prevBaseName2 = baseName2;
			}

			for (int ii = 0; ii < numDigitPatterns; ii++) {
				if (patterns1.get(ii).matcher(nameNoExt).matches()) {
					hasMatches1.set(ii, true);
				}
				if (patterns2.get(ii).matcher(nameNoExt).matches()) {
					hasMatches2.set(ii, true);
				}
			}
		}

		AlbumProfiling.getInstance().exit(4, "part 7");

		//validate every dat has jpg and vice versa ---------------------------

		AlbumProfiling.getInstance().enter(4, "part 8");

		Collection<AlbumImageFileDetails> fsDatFileDetails = getImageFileDetailsFromFileSystem(subFolder, ".dat"); //result is sorted

		Set<String> missingDatFiles = fsImageFileDetails.stream().map(AlbumImageFileDetails::getName).collect(Collectors.toSet());
		Set<String> missingJpgFiles = fsDatFileDetails.stream().map(AlbumImageFileDetails::getName).collect(Collectors.toSet());

		//note comparisons are done on filenames WITHOUT extensions
		missingDatFiles.removeAll(new HashSet<>(fsDatFileDetails.stream().map(AlbumImageFileDetails::getName).collect(Collectors.toSet())));
		missingJpgFiles.removeAll(new HashSet<>(fsImageFileDetails.stream().map(AlbumImageFileDetails::getName).collect(Collectors.toSet())));

		AlbumProfiling.getInstance().exit(4, "part 8");

		Collection<AlbumImage> imagesMissingOrBadDatFiles = new HashSet<>();

		if (missingDatFiles.size() > 0) {
			_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): missingDatFiles.size() = " + missingDatFiles.size() + " (mismatched jpg/dat pairs)");

			List<String> sorted = missingDatFiles.stream()
					.sorted(VendoUtils.caseInsensitiveStringComparator)
					.collect(Collectors.toList());
			for (String str : sorted) {
				String filePath = Paths.get(_rootPath, subFolder, str + ".jpg").toString();
				_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): warning: missing .dat for: " + filePath);
				imagesMissingOrBadDatFiles.add(new AlbumImage(str, subFolder, true)); //this AlbumImage ctor reads image from disk
			}
		}

		if (missingJpgFiles.size() > 0) {
			_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): missingJpgFiles.size() = " + missingJpgFiles.size() + " (mismatched jpg/dat pairs)");

			List<String> sorted = missingJpgFiles.stream()
					.sorted(VendoUtils.caseInsensitiveStringComparator)
					.collect(Collectors.toList());
			for (String str : sorted) {
				String filePath = Paths.get(_rootPath, subFolder, str + ".dat").toString();
				_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): warning: missing .jpg for: " + filePath);
			}
		}

		//validate dat files have correct size --------------------------------

		AlbumProfiling.getInstance().enter(4, "part 9");

		for (AlbumImageFileDetails fsDatFileDetail : fsDatFileDetails) {
			if (fsDatFileDetail.getBytes() != AlbumImage._RgbDatSizeRectagular && fsDatFileDetail.getBytes() != AlbumImage._RgbDatSizeSquare) {
				String filePath = Paths.get(_rootPath, subFolder, fsDatFileDetail.getName() + ".dat").toString();
				_log.warn("AlbumImageDao.syncFolder(" + subFolder + "): warning: invalid .dat file size (" + fsDatFileDetail.getBytes() + ") for: " + filePath);
				imagesMissingOrBadDatFiles.add(new AlbumImage(fsDatFileDetail.getName(), subFolder, true)); //this AlbumImage ctor reads image from disk
			}
		}

		AlbumProfiling.getInstance().exit(4, "part 9");

		// create missing or corrupted dat files --------------------------------

		if (imagesMissingOrBadDatFiles.size() > 0) {
			_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): subFolder = " + subFolder + ", imagesMissingOrBadDatFiles.size() = " + imagesMissingOrBadDatFiles.size() + " (missing or bad .dat files)");

			for (AlbumImage image : imagesMissingOrBadDatFiles) {
				_log.debug("AlbumImageDao.syncFolder(" + subFolder + "): subFolder = " + subFolder + ", creating .dat file: " + image.getName() + ".dat");
				try {
					image.createRgbDataFile();
				} catch (Exception ee) {
					_log.error("AlbumImageDao.syncFolder(" + subFolder + "): exception calling createRgbDataFile()", ee);
				}
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private Thread createWatcherThreadsForFolder(String subFolder1)
	{
		final BlockingQueue<AlbumImageEvent> queue = new LinkedBlockingQueue<>();

		//start thread to watch queue and handle events
		Thread handlerThread = new Thread(() -> {
			int numHandled = 0;
			while (true) {
				try {
					AlbumImageEvent albumImageEvent = queue.take(); //will block if queue is empty
					numHandled++;

					Path dir = albumImageEvent.getDir();
					WatchEvent<Path> pathEvent = albumImageEvent.getPathEvent();

					Path file = pathEvent.context();
					Path path = dir.resolve(file);
					String subFolder2 = dir.getName(dir.getNameCount() - 1).toString();

					if (subFolder1.compareToIgnoreCase(subFolder2) != 0) {
						_log.warn("AlbumImageDao.WatchDir.queueHandler(" + subFolder2 + ") subFolder mismatch: " + subFolder1 + " != " + subFolder2);
					}

					//give file system a chance to settle
					final long delayMillis = 5;
					int sleepMillis = (int) (albumImageEvent.getTimestamp() + delayMillis - new GregorianCalendar().getTimeInMillis());
					if (sleepMillis > 0) {
						sleepMillis(sleepMillis);
					}

//					if (_Debug) {
//						String message = "AlbumImageDao.WatchDir.queueHandler(\"" + subFolder2 + "\"/" + queue.size() + "): " +
//								pathEvent.kind().name() + ": " + path.normalize().toString();
//						if (sleepMillis > 0) {
//							message += ": slept " + sleepMillis + " ms";
//						}
//						_log.debug(message);
//					}

//THIS DOES NOT WORK
//					if (_pauseModeEnabled.get()) {
//						_log.debug("AlbumImageDao.WatchDir.queueHandler(\"" + subFolder2 + "\"): pause mode enabled: skipping event: " + pathEvent.kind().name() + " on file: " + path.normalize());
//						continue;
//					}

					if (pathEvent.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
						handleFileCreate(subFolder2, path);

					} else if (pathEvent.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
						handleFileModify(subFolder2, path);

					} else if (pathEvent.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
						handleFileDelete(subFolder2, path);

					} else {
						_log.warn("AlbumImageDao.WatchDir.queueHandler(" + subFolder2 + "/" + queue.size() + "): unhandled event: " + pathEvent.kind().name() + " on file: " + path.normalize());
					}

					final int intermediateRefreshCountMin = 200;
					final int intermediateRefreshCountMax = 300;
					if (queue.size() == 0 || (queue.size() > intermediateRefreshCountMin && numHandled > intermediateRefreshCountMax)) {
						numHandled = 0;
						updateImageCounts(subFolder2);
						if (queue.size() > 0) {
							_log.debug("AlbumImageDao.WatchDir.queueHandler(" + subFolder2 + ") update image counts: intermediate -----------------------------------------");
						} else {
							_log.debug("AlbumImageDao.WatchDir.queueHandler(" + subFolder2 + ") update image counts: queue empty ------------------------------------------");
						}
					}

				} catch (Exception ee) {
					_log.error("AlbumImageDao.WatchDir.queueHandler: ", ee);
				}
			}
		});
		handlerThread.setName("queueHandler:" + subFolder1);
		handlerThread.start();
		_handlerThreadMap.put(subFolder1, handlerThread);

		//start thread to watch for events, and put in queue
		Thread watcherThread = null;
		try {
			final Path dir = FileSystems.getDefault().getPath(_rootPath, subFolder1);

//			_log.debug ("AlbumImageDao.createWatcherThreadsForFolder: watching folder: " + dir.normalize ().toString ());

			final Pattern allPattern = Pattern.compile(".*");
			final Pattern imagePattern = Pattern.compile(".*\\" + AlbumFormInfo._ImageExtension, Pattern.CASE_INSENSITIVE);
			boolean recurseSubdirs = false;

			WatchDir watchDir = new WatchDir(dir, allPattern, recurseSubdirs) {
				@Override
				protected void notify(Path dir, WatchEvent<Path> pathEvent) {
					handlePauseAction(subFolder1);

					String filename = pathEvent.context().toString();
					if (imagePattern.matcher(filename).matches()) {
						try {
//							if (_Debug) {
//								Path file = pathEvent.context();
//								Path path = dir.resolve(file);
//								String subFolder = dir.getName(dir.getNameCount() - 1).toString();
//
//								_log.debug("AlbumImageDao.WatchDir.notify(\"" + subFolder + "\"/" + queue.size() + "): " + pathEvent.kind().name() + ": " + path.normalize().toString());
//							}
							queue.put(new AlbumImageEvent(dir, pathEvent));

						} catch (Exception ee) {
							_log.error("AlbumImageDao.WatchDir.notify(" + subFolder1 + "): ", ee);
						}
					} else if (!pathEvent.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
						if (!hasKnownFileExtension(filename)) {
							String filePath = dir.resolve(pathEvent.context()).toString();
							_log.warn("AlbumImageDao.WatchDir.notify(" + subFolder1 + "): unexpected file for " + pathEvent.kind() + ": " + filePath);
						}
					}
				}

				@Override
				protected void overflow(WatchEvent<?> event) {
					_log.error("AlbumImageDao.WatchDir.overflow(" + subFolder1 + "): received event: " + event.kind().name() + ", count = " + event.count());
					_log.error("AlbumImageDao.WatchDir.overflow(" + subFolder1 + "): ", new Exception("WatchDir overflow(" + subFolder1 + ")"));

					restartWatcherThreadsForFolder (subFolder1);
				}
			};
			watcherThread = new Thread(watchDir);
			watcherThread.setName("watcherThread:" + subFolder1);
			watcherThread.start();
			_watcherThreadMap.put(subFolder1, watcherThread);


		} catch (Exception ee) {
			_log.error("AlbumImageDao.createWatcherThreadsForFolder(" + subFolder1 + "): exception in continuous mode", ee);
		}

		return watcherThread;
	}

	///////////////////////////////////////////////////////////////////////////
	public void restartWatcherThreadsForFolder (String subFolder)
	{
		Thread currentThread = Thread.currentThread ();
		_log.debug ("AlbumImageDao.restartWatcherThreadsForFolder: subFolder: " + subFolder + ", thread: " + currentThread.getName ());

//		if (_isCLI) {
		if (_isServer) {
			Thread handlerThread = _handlerThreadMap.get(subFolder);
			Thread watcherThread = _watcherThreadMap.get(subFolder);

			_log.debug ("AlbumImageDao.restartWatcherThreadsForFolder: calling createWatcherThreadsForFolder for subFolder: " + subFolder);
			AlbumImageDao.getInstance ().createWatcherThreadsForFolder (subFolder);

			handlerThread.interrupt ();
			watcherThread.interrupt ();

		} else {
			_log.warn ("AlbumImageDao.restartWatcherThreadsForFolder: _isServer = false; not restarting threads for subFolder: " + subFolder + ", thread: " + currentThread.getName ());
		}
	}


	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	public Collection<AlbumImage> doDir(String subFolder, AlbumFileFilter filter, Set<String> debugNeedsChecking, Map<String, Integer> debugCacheMiss)
	{
		AlbumFormInfo form = AlbumFormInfo.getInstance();
		AlbumOrientation orientation = form.getOrientation();
		boolean sortByExifDate = (form.getSortType() == AlbumSortType.ByExif);

		Collection<AlbumImage> imageDisplayList = new LinkedList<>();

		if (filter.folderNeedsChecking(subFolder)) {
			debugNeedsChecking.add(subFolder);
			Collection<AlbumImage> imageList = getImagesFromCache(subFolder, debugCacheMiss);

			AlbumProfiling.getInstance().enter(7, subFolder, "accept loop");

			for (AlbumImage image : imageList) {
				String name = image.getName();

				if (filter.accept(null, name)) {
					//if sorting by exifDate, only include images that have at least one valid exifDate
					if (!sortByExifDate || image.hasExifDate()) {
						if (image.matchOrientation(orientation)) {
							imageDisplayList.add(image);
						}
					}
				}
			}

			AlbumProfiling.getInstance().exit(7, subFolder, "accept loop");
		}

		return imageDisplayList;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private boolean handleFileModify(String subFolder, Path path) //complete path plus extension
	{
		String nameNoExt = AlbumFormInfo.stripImageExtension(path.getFileName().toString());

		return handleFileModify(subFolder, nameNoExt);
	}

	private boolean handleFileModify(String subFolder, String nameNoExt) //image file name without extension
	{
//		_log.debug ("AlbumImageDao.handleFileModify: " + nameNoExt);

		handleFileDelete(subFolder, nameNoExt);
		handleFileCreate(subFolder, nameNoExt);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private boolean handleFileCreate(String subFolder, Path path) //complete path plus extension
	{
		String nameNoExt = AlbumFormInfo.stripImageExtension(path.getFileName().toString());

		return handleFileCreate(subFolder, nameNoExt);
	}

	private boolean handleFileCreate(String subFolder, String nameNoExt) //image file name without extension
	{
//		_log.debug ("AlbumImageDao.handleFileCreate: " + nameNoExt);

		boolean status = false;

		Path imageFile = FileSystems.getDefault().getPath(_rootPath, subFolder, nameNoExt + AlbumFormInfo._ImageExtension);
		if (!fileExists(imageFile)) {
//			_log.error("AlbumImageDao.handleFileCreate: image file does not exist \"" + imageFile.toString() + "\"");
			return status;
		}

		if (!AlbumImage.isValidImageName(nameNoExt)) {
			_log.error("AlbumImageDao.handleFileCreate: invalid image name: " + nameNoExt);
		}

		AlbumImage image = null;
//changed: catch exception in AlbumImage ctor
//		try {
			image = new AlbumImage(nameNoExt, subFolder, true); //this AlbumImage ctor reads image from disk
			image.createRgbDataFile();
			status = insertImageIntoImages(image);
//		} catch (Exception ee) {
//			_log.error("AlbumImageDao.handleFileCreate(" + subFolder + ", " + nameNoExt + "): ", ee);
//			System.out.println("del " + imageFile);
//		}

		_imagesNeedingCountUpdate.get().add(AlbumImage.getBaseName(nameNoExt, false));

		if (status) {
			_log.debug("AlbumImageDao.handleFileCreate(" + subFolder + "): created image: " + image);
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private boolean handleFileDelete(String subFolder, Path path) //complete path plus extension
	{
		String nameNoExt = AlbumFormInfo.stripImageExtension(path.getFileName().toString());

		return handleFileDelete(subFolder, nameNoExt);
	}

	private boolean handleFileDelete(String subFolder, String nameNoExt) //image file name without extension
	{
//		_log.debug ("AlbumImageDao.handleFileDelete: " + nameNoExt);

		Path rgbDataFile = FileSystems.getDefault().getPath(_rootPath, subFolder, nameNoExt + AlbumFormInfo._ImageExtension);

		boolean status = deleteImageFromImages(subFolder, nameNoExt);
		AlbumImage.removeRgbDataFileFromFileSystem(rgbDataFile.toString());

		_imagesNeedingCountUpdate.get().add(AlbumImage.getBaseName(nameNoExt, false));

		if (status) {
			_log.debug("AlbumImageDao.handleFileDelete(" + subFolder + "): deleted image: " + nameNoExt);
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private boolean updateImageCounts(String subFolder)
	{
		Set<String> baseNames1 = _imagesNeedingCountUpdate.get(); //baseNames with collapseGroups = false
//TODO temp debugging - why am I never clearing _imagesNeedingCountUpdate ??
// or is it being cleared below with this line: baseNames1.clear();

		_log.debug("AlbumImageDao.updateImageCounts(" + subFolder + "): " + baseNames1.stream().sorted().collect(Collectors.toList()));

		Set<String> baseNames2 = new HashSet<>(); //baseNames with collapseGroups = true

		int rowsAffected = 0;

		for (String baseName : baseNames1.stream().sorted().collect(Collectors.toList())) {
			baseNames2.add(AlbumImage.getBaseName(baseName + "-01", true)); //HACK - add extra for getBaseName()

			int count = getImageCountFromImages(subFolder, baseName + ".*"); //regex (e.g., Foo01 -> Foo01.*)
			rowsAffected += updateImageCountsInImageCounts(subFolder, baseName, false, count);
//			_log.debug("AlbumImageDao.updateImageCounts(" + subFolder + "): updated image count: " + baseName + (count == 0 ? " removed" : " = " + count));
		}
		baseNames1.clear(); //done with these - clear value in _imagesNeedingCountUpdate

		for (String baseName : baseNames2.stream().sorted().collect(Collectors.toList())) {
			int count = getImageCountFromImages(subFolder, baseName + "[0-9].*"); //regex  (e.g., Foo -> Foo[0-9].*)
			rowsAffected += updateImageCountsInImageCounts(subFolder, baseName, true, count);
//			_log.debug("AlbumImageDao.updateImageCounts(" + subFolder + "): updated image count: " + baseName + (count == 0 ? " removed" : " = " + count));
		}

		if (rowsAffected > 0) {
			long nowInMillis = new GregorianCalendar().getTimeInMillis();
			insertLastUpdateIntoImageFolder(subFolder, nowInMillis);
		}

		return rowsAffected > 0;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet and AlbumTags CLI
	public Collection<AlbumImage> getImagesFromCache(String subFolder, Map<String, Integer> debugCacheMiss)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		Collection<AlbumImage> images;

		AlbumImagesData imagesData = _albumImagesDataCache.get(subFolder);

		long databaseLastUpdateMillis = getLastUpdateFromImageFolder(subFolder);

		if (imagesData != null && imagesData.getLastUpdateMillis() > databaseLastUpdateMillis) {
//			_log.debug ("AlbumImageDao.getImagesFromCache: imageData cache hit for subFolder: " + subFolder);
			images = imagesData.getImages();

		} else {
//			_log.debug ("AlbumImageDao.getImagesFromCache: imageData cache miss for subFolder: " + subFolder);
			images = getImagesFromImages(subFolder);
			long nowInMillis = new GregorianCalendar().getTimeInMillis();
			imagesData = new AlbumImagesData(images, nowInMillis);
			_albumImagesDataCache.put(subFolder, imagesData);
			if (debugCacheMiss != null) {
				debugCacheMiss.put(subFolder, images.size());
			}

			if (!_isServer) { //server does not need these caches

				Map<String, Integer> imagesCountMap = getImageCountsFromImageCounts(subFolder);
				_albumImagesCountCache.put(subFolder, imagesCountMap);

				Map<String, Integer> albumsCountMap = new HashMap<>();
				for (String baseName : imagesCountMap.keySet()) {
					String baseNameNoDigits = baseName.replaceAll("\\d", "");

					Integer count = albumsCountMap.get(baseNameNoDigits);
					if (count == null) {
						albumsCountMap.put(baseNameNoDigits, 0); //start with 0 because the map has every album, and the collapsed-album name (Fh01, Fh02, and Fh)
					} else {
						albumsCountMap.put(baseNameNoDigits, ++count);
					}
				}
				_albumAlbumsCountCache.put(subFolder, albumsCountMap);

//new
				Map<String, Boolean> albumHasExifDataMap = new HashMap<>();
				for (AlbumImage image : images) {
					if (image.hasExifDate()) {
						albumHasExifDataMap.put(image.getBaseName(true), true);
						albumHasExifDataMap.put(image.getBaseName(false), true);
					}
				}
				_albumImagesHasExifDataCache.put(subFolder, albumHasExifDataMap);

				AlbumImages.duplicatesCacheMaintenance(subFolder);
			}

			//log mis-filed images

//temp timing for debugging
//			AlbumProfiling.getInstance ().enter (5, subFolder + ".misFiled");
//			Instant startInstant = Instant.now ();

			//NOTE: similar code exists in getImagesFromCache() and syncFolder()
			String messageForLog = "";
			String messageForServlet = "";
			_servletMisFiledImageErrorsMap.remove(subFolder);
			List<AlbumImage> misFiledImages = images.stream()
													.filter(i -> !subFolder.equalsIgnoreCase(AlbumImageDao.getInstance().getSubFolderFromImageName(i.getName ())))
													.collect (Collectors.toList ());

			if (!misFiledImages.isEmpty()) {
				String misFiledImagesStr = misFiledImages.stream()
															.map(AlbumImage::getName)
//															.filter(s -> !subFolder.equalsIgnoreCase(getSubFolderFromImageName(s)))
															.sorted(String.CASE_INSENSITIVE_ORDER)
															.collect(Collectors.joining(", "));

				List<String> moveCommands = misFiledImages.stream()
															.map(i -> i.getBaseName(true))
//															.filter(s -> !subFolder.equalsIgnoreCase(getSubFolderFromImageName(s)))
															.sorted(String.CASE_INSENSITIVE_ORDER)
															.distinct()
															.map(s -> "move jroot\\" + subFolder + "\\" + s + "*.jpg jroot\\" + getSubFolderFromImageName(s) + "\\" + NL)
															.collect(Collectors.toList());

				final StringBuilder sb = new StringBuilder("found image files (" + misFiledImages.size() + ") in wrong folder (" + subFolder + "): " + misFiledImagesStr + NL);
				messageForServlet = sb.toString();

				sb.append("To fix, run:").append(NL);
				moveCommands.forEach(sb::append);
				messageForLog = sb.toString();
			}

//			AlbumProfiling.getInstance().exit(5, subFolder + ".misFiled");
//			System.out.println("AlbumImageDao.getImagesFromCache(" + subFolder + ").misFiled: elapsed: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()));

			if (!messageForServlet.isEmpty()) {
				_log.error("AlbumImageDao.getImagesFromCache: " + messageForLog);
				_servletMisFiledImageErrorsMap.put(subFolder, "Error: " + messageForServlet);
			}
		}

		AlbumProfiling.getInstance().exit(7, subFolder);

		return images;
	}

//replaced by getNumMatchingImages()
	///////////////////////////////////////////////////////////////////////////
	//used by servlet
//	public Integer getImagesCount(String baseName)
//	{
//		String subFolder = AlbumImageDao.getInstance ().getSubFolderFromImageName(baseName);
//		Map<String, Integer> imagesCountMap = _albumImagesCountCache.get(subFolder);
//		return imagesCountMap.get(baseName);
//	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private int updateImageCountsInImageCounts(String subFolder, String baseName, boolean collapseGroups, int count) {
		_log.debug("AlbumImageDao.updateImageCountsInImageCounts(" + subFolder + "): updating image counts: " + baseName + (count == 0 ? " removed" : " = " + count));

		return count > 0 ?
				insertImageCountsIntoImageCounts(subFolder, baseName, collapseGroups, count) :
				deleteImageCountsFromImageCounts(subFolder, baseName);
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private int insertImageCountsIntoImageCounts(String subFolder, String nameNoExt, boolean collapseGroups, int value)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		String baseName = AlbumImage.getBaseName(nameNoExt, collapseGroups);
		int collapseGroupsInt = (collapseGroups ? 1 : 0);

		int rowsAffected = 0;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			rowsAffected = mapper.insertImageCountsIntoImageCounts(subFolder, baseName, collapseGroupsInt, value);
			session.commit();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.insertImageCountsIntoImageCounts(" + subFolder + ", " + nameNoExt + ", " + collapseGroups + ", " + value + "): ", ee);
		}

		AlbumProfiling.getInstance().exit(7, subFolder);

//		_log.debug ("AlbumImageDao.insertImageCountsIntoImageCounts(" + subFolder + "): nameNoExt: " + nameNoExt + ", value: " + value);

		return rowsAffected;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	public int getImageCountFromImages(String subFolder, String wildName)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		int count = 0;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			count = mapper.selectImageCountFromImages(subFolder, wildName);

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getImageCountFromImages(" + wildName + "): ", ee);
		}

		AlbumProfiling.getInstance().exit(7, subFolder);

//		_log.debug ("AlbumImageDao.getImageCountFromImages(\"" + wildName + "\"): count = " + count);

		return count;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	public int getNumMatchingImages(String baseName, long unused /*sinceInMillis*/) {
//TODO - this might? not work when image is in wrong subfolder (can happen when image is renamed prior to move)
		int numMatchingImages = 0;

		try {
			String subFolder = getSubFolderFromImageName(baseName);
			Map<String, Integer> imagesCountMap = _albumImagesCountCache.get(subFolder);
			numMatchingImages = imagesCountMap.get(baseName);

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getNumMatchingImages(" + baseName + "): ", ee);
		}

		return numMatchingImages;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	public int getNumMatchingAlbums(String baseName, long unused /*sinceInMillis*/)
	{
//TODO - this might? not work when image is in wrong subfolder (can happen when image is renamed prior to move)
		int numMatchingAlbums = 0;

		try {
			String subFolder = getSubFolderFromImageName(baseName);
			Map<String, Integer> albumsCountMap = _albumAlbumsCountCache.get(subFolder);
			numMatchingAlbums = albumsCountMap.get(baseName);

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getNumMatchingAlbums(" + baseName + "): ", ee);
		}

		return numMatchingAlbums;
	}
/* old way - save
//TODO - this does not work when image is in wrong subfolder (can happen when image is renamed prior to move)
		AlbumProfiling.getInstance().enter(5);

		int numMatchingAlbums = 0;

		String subFolder = AlbumImage.getSubFolderFromName(baseName);

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			numMatchingAlbums = mapper.selectAlbumCountFromImageCounts(subFolder, "^" + baseName + "[0-9]+$");

		} catch (Exception ee) {
//			if (ee instanceof org.apache.ibatis.binding.BindingException) {
//				//most likely cause
//				AlbumFormInfo.getInstance ().addServletError ("Error: found image files (" + baseName + ") in wrong folder");
//			}
			_log.error("AlbumImageDao.getNumMatchingAlbums(\"" + baseName + "\"): ", ee);
		}

		AlbumProfiling.getInstance().exit(5);

//		_log.debug ("AlbumImageDao.getAlbumCountFromImageCounts(\"" + wildName + "\"): count = " + count);

		return numMatchingAlbums;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	public boolean getAlbumHasExifData(String baseName, long unused /*sinceInMillis*/)
	{
//TODO - this might? not work when image is in wrong subfolder (can happen when image is renamed prior to move)
		boolean albumHasExifData = false;

		try {
			String subFolder = getSubFolderFromImageName(baseName);
			Map<String, Boolean> albumsHasExifDataMap = _albumImagesHasExifDataCache.get(subFolder);
			Boolean entry = albumsHasExifDataMap.get(baseName);
			albumHasExifData = entry != null && entry;

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getAlbumHasExifData(" + baseName + "): ", ee);
		}

		return albumHasExifData;
	}


	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private Map<String, Integer> getImageCountsFromImageCounts(String subFolder)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		List<AlbumImageCount> list = new LinkedList<>();

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			list = mapper.selectImageCountsFromImageCounts(subFolder);

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getImageCountsFromImageCounts(" + subFolder + "): ", ee);
		}

		Map<String, Integer> map = list.stream().collect(Collectors.toMap(AlbumImageCount::getBaseName, AlbumImageCount::getCount));

		AlbumProfiling.getInstance().exit(7, subFolder);

//		_log.debug ("AlbumTags.getImageCountsFromImageCounts(" + subFolder + "): map.size() = " + map.size ());

		return map;
	}

/* do not use: this does not work for variable-length subfolders; e.g., it does not distinguish between subfolders "ka" and "kat"
	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private Map<String, Integer> getMismatchedEntriesFromImageCounts()
	{
//		AlbumProfiling.getInstance ().enter (7);

		List<AlbumImageCount> list = new LinkedList<>();

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			list = mapper.selectMismatchedEntriesFromImageCounts();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getMismatchedEntriesFromImageCounts(): ", ee);
		}

		Map<String, Integer> map = list.stream().collect(Collectors.toMap(AlbumImageCount::getBaseName, AlbumImageCount::getCount));

		AlbumProfiling.getInstance().exit(7);

//		_log.debug ("AlbumTags.getMismatchedEntriesFromImageCounts(): map.size() = " + map.size ());

		return map;
	}
 */

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	//input must be sorted
	private Map<String, Integer> calculateImageCountsFromFileSystem(String subFolder, Collection<AlbumImageFileDetails> imageFileDetails)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		Map<String, List<AlbumImageFileDetails>> map1a = imageFileDetails.stream().collect(Collectors.groupingBy(i -> AlbumImage.getBaseName(i.getName(), true)));
		Map<String, List<AlbumImageFileDetails>> map2a = imageFileDetails.stream().collect(Collectors.groupingBy(i -> AlbumImage.getBaseName(i.getName(), false)));

 		Map<String, Integer> map1b = map1a.keySet().stream().collect(Collectors.toMap(i -> i, i -> map1a.get(i).size()));
 		Map<String, Integer> map2b = map2a.keySet().stream().collect(Collectors.toMap(i -> i, i -> map2a.get(i).size()));

//		_log.debug ("AlbumImageDao.calculateImageCountsFromFileSystem: subFolder: " + subFolder);
//		_log.debug ("AlbumImageDao.calculateImageCountsFromFileSystem: map1b: " + map1b);
//		_log.debug ("AlbumImageDao.calculateImageCountsFromFileSystem: map2b: " + map2b);

		map1b.putAll(map2b);

		AlbumProfiling.getInstance().exit(7, subFolder);

		return map1b;

/* old way
		Map<String, Integer> items = new HashMap<>();

		//set image counts
		String prevBaseName1 = "";
		String prevBaseName2 = "";
		int imageCount1 = 0;
		int imageCount2 = 0;
		for (AlbumImageFileDetails imageFileDetail : imageFileDetails) {
			String baseName1 = AlbumImage.getBaseName(imageFileDetail.getName(), false);
			String baseName2 = AlbumImage.getBaseName(imageFileDetail.getName(), true);

			if (baseName1.compareTo(prevBaseName1) == 0) {
				imageCount1++;

			} else {
				if (prevBaseName1.length() > 0) {
					items.put(prevBaseName1, imageCount1);
				}

				//reset
				prevBaseName1 = baseName1;
				imageCount1 = 1;
			}

			if (baseName2.compareTo(prevBaseName2) == 0) {
				imageCount2++;

			} else {
				if (prevBaseName2.length() > 0) {
					items.put(prevBaseName2, imageCount2);
				}

				//reset
				prevBaseName2 = baseName2;
				imageCount2 = 1;
			}
		}
		//now do last ones
		items.put(prevBaseName1, imageCount1);
		items.put(prevBaseName2, imageCount2);

//		if (true) {
//			for (String key : items.keySet ()) {
//				_log.debug ("AlbumImageDao.calculateImageCountsFromFileSystem: item: " + key + ": " + items.get (key));
//			}
//		}

		AlbumProfiling.getInstance().exit(7, subFolder);

		return items;
*/
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet and AlbumTags CLI
	public Collection<AlbumImage> getImagesFromImages(String subFolder)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		List<AlbumImage> list = new LinkedList<>();

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			list = mapper.selectImagesFromImages(subFolder);

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getImagesFromImages(" + subFolder + "): ", ee);
		}

		list.sort(new AlbumImageComparator(AlbumSortType.ByName));

		AlbumProfiling.getInstance().exit(7, subFolder);

//		_log.debug ("AlbumImageDao.getImagesFromImages(" + subFolder + "): list.size() = " + list.size ());

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private boolean insertLastUpdateIntoImageFolder(String subFolder, long updateTimeInMillis)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		boolean status = true;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			int rowsAffected = mapper.insertLastUpdateIntoImageFolder(subFolder, new Timestamp(updateTimeInMillis));
			session.commit();

			status = rowsAffected > 0;

		} catch (Exception ee) {
			_log.error("AlbumImageDao.insertLastUpdateIntoImageFolder(" + subFolder + "): ", ee);

			status = false;
		}

		AlbumProfiling.getInstance().exit(7, subFolder);

//		_log.debug ("AlbumImageDao.insertLastUpdateIntoImageFolder(" + subFolder + "): updateTimeInMillis = " + _dateFormat.format (new java.util.Date (updateTimeInMillis)) + " (" + updateTimeInMillis + ")");

		return status;
	}

/*not currently used
	///////////////////////////////////////////////////////////////////////////
	//used by CLI - queries images table
	public long getMaxInsertDateFromImages (String subFolder)
	{
		AlbumProfiling.getInstance ().enter (7, subFolder);

		long maxInsertMillis = Long.MAX_VALUE;

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			AlbumImageMapper mapper = session.getMapper (AlbumImageMapper.class);
			Timestamp maxInsertDate = mapper.selectMaxInsertDateFromImages (subFolder);
			maxInsertMillis = maxInsertDate.getTime ();

		} catch (Exception ee) {
			_log.error ("AlbumImageDao.getMaxInsertDateFromImages(" + subFolder + "): ", ee);
		}

		AlbumProfiling.getInstance ().exit (7, subFolder);

//		_log.debug ("AlbumImageDao.getMaxInsertDateFromImages: maxInsertMillis = " + _dateFormat.format (new java.util.Date (maxInsertMillis)) + " (" + maxInsertMillis + ")");

		return maxInsertMillis;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//used by AlbumTags CLI - queries image_folder table
	public long getMaxLastUpdateFromImageFolder()
	{
		AlbumProfiling.getInstance().enter(7);

		long maxLastUpdateMillis = Long.MAX_VALUE;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			Timestamp maxLastUpdateDate = mapper.selectMaxLastUpdateFromImageFolder();
			maxLastUpdateMillis = maxLastUpdateDate.getTime();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getMaxLastUpdateFromImageFolder: ", ee);
		}

		AlbumProfiling.getInstance().exit(7);

//		_log.debug ("AlbumImageDao.getMaxLastUpdateFromImageFolder: maxLastUpdateMillis = " + _dateFormat.format (new java.util.Date (maxLastUpdateMillis)) + " (" + maxLastUpdateMillis + ")");

		return maxLastUpdateMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	private long getLastUpdateFromImageFolder(String subFolder)
	{
		AlbumProfiling.getInstance().enter(7, subFolder);

		long lastUpdateMillis = Long.MAX_VALUE;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			Timestamp lastUpdateDate = mapper.selectLastUpdateFromImageFolder(subFolder);
			lastUpdateMillis = lastUpdateDate.getTime();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getLastUpdateFromImageFolder(" + subFolder + "): ", ee);
		}

		AlbumProfiling.getInstance().exit(7, subFolder);

//		_log.debug ("AlbumImageDao.getLastUpdateFromImageFolder(" + subFolder + "): lastUpdateMillis = " + _dateFormat.format (new java.util.Date (lastUpdateMillis)) + " (" + lastUpdateMillis + ")");

		return lastUpdateMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private Collection<AlbumImageFileDetails> getImageFileDetailsFromImages(String subFolder) //only retrieves nameNoExt, bytes, modified
	{
//		AlbumProfiling.getInstance ().enter (7, subFolder);

		List<AlbumImageFileDetails> list = new LinkedList<>();

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			list = mapper.selectImageFileDetailsFromImages(subFolder);

		} catch (Exception ee) {
			_log.error("AlbumImageDao.getImageFileDetailsFromImages(" + subFolder + "): ", ee);
		}

		Collections.sort(list);

//		AlbumProfiling.getInstance ().exit (7, subFolder);

//		_log.debug ("AlbumImageDao.getImageFileDetailsFromImages(" + subFolder + "): list.size() = " + list.size ());

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private Collection<AlbumImageFileDetails> getImageFileDetailsFromFileSystem(String subFolder, String extension)
	{
//		AlbumProfiling.getInstance ().enter (7, subFolder);

		List<AlbumImageFileDetails> list = new LinkedList<>();

		Path folder = FileSystems.getDefault().getPath(_rootPath, subFolder);

		try {
			Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					String nameWithExt = file.getFileName().toString();
					try {
						if (nameWithExt.endsWith(extension)) {
							String nameNoExt = nameWithExt.substring(0, nameWithExt.length() - extension.length());
							long numBytes = attrs.size();
							long modified = attrs.lastModifiedTime().toMillis();

							list.add(new AlbumImageFileDetails(nameNoExt, numBytes, modified));
						} else if (!hasKnownFileExtension(nameWithExt)) {
							_log.warn("AlbumImageDao.getImageFileDetailsFromFileSystem(" + subFolder + "): visitFile() unexpected file: " + file);
						}
					} catch (Exception ex) {
						_log.error("AlbumImageDao.getImageFileDetailsFromFileSystem(" + subFolder + "): visitFile() error on file: " + file, ex);
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException ex) {
					if (ex != null) {
						_log.error("AlbumImageDao.getImageFileDetailsFromFileSystem(" + subFolder + "): error: ", new Exception("visitFileFailed", ex));
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException ex) {
					if (ex != null) {
						_log.error("AlbumImageDao.getImageFileDetailsFromFileSystem(" + subFolder + "): error: ", new Exception("postVisitDirectory", ex));
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (Exception ex) {
			throw new AssertionError("Files#walkFileTree will not throw IOException if the FileVisitor does not");
		}

		Collections.sort (list);

//		AlbumProfiling.getInstance ().exit (7, subFolder);

//		_log.debug ("AlbumImageDao.getImageFileDetailsFromFileSystem(" + subFolder + "): list.size() = " + list.size ());

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private boolean insertImageIntoImages(AlbumImage image)
	{
//		AlbumProfiling.getInstance ().enter (7);

		boolean status = true;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			int rowsAffected = mapper.insertImageIntoImages(image);
			session.commit();

			status = rowsAffected > 0;

		} catch (Exception ee) {
			_log.error("AlbumImageDao.insertImageIntoImages(" + image + "): ", ee);

			status = false;
		}

//		AlbumProfiling.getInstance ().exit (7);

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private boolean deleteImageFromImages(String subFolder, String nameNoExt)
	{
//		AlbumProfiling.getInstance ().enter (7, subFolder);

		boolean status = true;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			int rowsAffected = mapper.deleteImageFromImages(subFolder, nameNoExt);
			session.commit();

			status = rowsAffected > 0;

		} catch (Exception ee) {
			_log.error("AlbumImageDao.deleteImageFromImages(" + subFolder + ", " + nameNoExt + "): ", ee);

			status = false;
		}

//		AlbumProfiling.getInstance ().exit (7, subFolder);

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private int deleteImageCountsFromImageCounts(String subFolder, String baseName)
	{
//		AlbumProfiling.getInstance ().enter (7);

		int rowsAffected = 0;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			rowsAffected = mapper.deleteImageCountsFromImageCounts(subFolder, baseName);
			session.commit();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.deleteImageCountsFromImageCounts(" + subFolder + ", " + baseName + "): ", ee);
		}

//		AlbumProfiling.getInstance ().exit (7);

//		_log.debug ("AlbumImageDao.deleteImageCountsFromImageCounts(\"" + baseName + "\"): " + rowsAffected + " rows deleted");

		return rowsAffected;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private int deleteZeroCountsFromImageCounts()
	{
//		AlbumProfiling.getInstance ().enter (7);

		int rowsAffected = 0;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			rowsAffected = mapper.deleteZeroCountsFromImageCounts();
			session.commit();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.deleteZeroCountsFromImageCounts(): ", ee);
		}

//		AlbumProfiling.getInstance ().exit (7);

		_log.debug("AlbumImageDao.deleteZeroCountsFromImageCounts(): " + rowsAffected + " rows deleted");

		return rowsAffected;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private int deleteMismatchedEntriesFromImageCounts()
	{
//		AlbumProfiling.getInstance ().enter (7);

		int rowsAffected = 0;

		try (SqlSession session = _sqlSessionFactory.openSession()) {
			AlbumImageMapper mapper = session.getMapper(AlbumImageMapper.class);
			rowsAffected = mapper.deleteMismatchedEntriesFromImageCounts();
			session.commit();

		} catch (Exception ee) {
			_log.error("AlbumImageDao.deleteMismatchedEntriesFromImageCounts(): ", ee);
		}

//		AlbumProfiling.getInstance ().exit (7);

		_log.debug("AlbumImageDao.deleteMismatchedEntriesFromImageCounts(): " + rowsAffected + " rows deleted");

		return rowsAffected;
	}

	///////////////////////////////////////////////////////////////////////////
	public Collection<String> getServletErrors()
	{
//		_log.trace ("AlbumImageDao.getServletErrors: " + _servletMisFiledImageErrorsMap);
		return new ArrayList<>(_servletMisFiledImageErrorsMap.values());
	}

	///////////////////////////////////////////////////////////////////////////
	public /*static*/ String getSubFolderFromImageName (String imageName)
	{
		if ("*".equals (imageName)) {
			return null;
		}

		getAlbumSubFolders (); //make sure this object has been initialized

		int maxLength = Math.min(_subFolderByLengthMap.size (), imageName.length());
		for (int ii = maxLength; ii > 0; --ii) { //start with the longest first, since it is most specific (i.e., "mar" is more specific than "ma")
			Set<String> subFolders = _subFolderByLengthMap.get (ii); //1-based
			String nameFirstLettersLower = imageName.toLowerCase ().substring (0, ii);
			if (subFolders.contains (nameFirstLettersLower)) {
				return nameFirstLettersLower;
			}
		}

		_log.error ("AlbumImageDao.getSubFolderFromImageName(" + imageName + "): failed to find subFolder for image name in map");
		return null;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean hasKnownFileExtension (String filename)
	{
		final Pattern pattern = Pattern.compile("(.*\\" + AlbumFormInfo._ImageExtension
													 + "|.*\\" + AlbumFormInfo._RgbDataExtension
													 + "|.*\\" + AlbumFormInfo._DeleteSuffix + ")",
												Pattern.CASE_INSENSITIVE);
		return pattern.matcher(filename).matches();
	}

		///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	//returns comma separated list of indexes that have true in the passed-in List
	private static String getMatchList (List<Boolean> hasMatches)
	{
		//note array index is 0-based, but pattern string is 1-based
		return IntStream.range (0, hasMatches.size ())
						.filter(hasMatches::get)
						.mapToObj (i -> String.valueOf (i + 1))
						.collect (Collectors.joining (","));
	}

	///////////////////////////////////////////////////////////////////////////
	public String getPauseFilename () {
		return Paths.get(_rootPath, _pauseFilename).toString();
	}

	///////////////////////////////////////////////////////////////////////////
	private void handlePauseAction (String subFolder)
	{
//NO; won't work: TODO rewrite this to have watcher on file that sets boolean

		final Path pauseFilePath = Paths.get(_rootPath, _pauseFilename);
		if (fileExists (pauseFilePath)) {
			_log.debug ("AlbumImageDao.handlePauseAction(" + subFolder + "): pause file found: entering pause mode...");
//			_pauseModeEnabled.set(true);

			while (fileExists (pauseFilePath)) {
				sleepMillis (_pauseSleepMillis);
			}

			_log.debug ("AlbumImageDao.handlePauseAction(" + subFolder + "): pause file not found: exiting pause mode");
//			_pauseModeEnabled.set(false);
		}
	}

	///////////////////////////////////////////////////////////////////////////
//	private Thread watchPauseFile ()
//	{
//		Thread watchingThread = null;
//
//		final Path path = FileSystems.getDefault ().getPath (_rootPath, _pauseFilename);
//
//		if (VendoUtils.fileExists (path)) {
//			System.err.println ("AlbumImageDao.watchPauseFile.notify: file already exists: " + path.normalize ());
//			_pauseModeEnabled.set (true);
//
//			if (watchingThread != null) {
//				watchingThread.interrupt (); //exit thread
//			}
//
//			return watchingThread;
//		}
//
//		try {
//			Path dir = path.getRoot ().resolve (path.getParent ());
//			String filename = path.getFileName ().toString ();
//
//			_log.info ("AlbumImageDao.watchPauseFile: watching for shutdown file: " + path.normalize ().toString ());
//
//			Pattern pattern = Pattern.compile (filename, Pattern.CASE_INSENSITIVE);
//			boolean recurseSubdirs = false;
//
//			WatchDir watchDir = new WatchDir (dir, pattern, recurseSubdirs)
//			{
//				@Override
//				protected void notify (Path dir, WatchEvent<Path> pathEvent)
//				{
//					if (_Debug) {
//						Path file = pathEvent.context ();
//						Path path = dir.resolve (file);
//						_log.debug ("AlbumImageDao.watchPauseFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ().toString ());
//					}
//
//					if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_MODIFY) ||
//							pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_CREATE)) {
//						System.err.println ("AlbumImageDao.watchPauseFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ().toString ());
//						_pauseModeEnabled.getAndSet (true);
//						Thread.currentThread ().interrupt (); //exit thread
//					}
//				}
//
//				@Override
//				protected void overflow (WatchEvent<?> event)
//				{
//					_log.error ("AlbumImageDao.watchPauseFile.overflow: received event: " + event.kind ().name () + ", count = " + event.count ());
//					_log.error ("AlbumImageDao.watchPauseFile.overflow: ", new Exception ("WatchDir overflow"));
//				}
//			};
//			watchingThread = new Thread (watchDir);
//			watchingThread.setName ("watchingThread");
//			watchingThread.start ();
//
//		} catch (Exception ee) {
//			_log.error ("AlbumImageDao.watchPauseFile: exception watching shutdown file", ee);
//		}
//
//		return watchingThread;
//	}

	///////////////////////////////////////////////////////////////////////////
	//used by servlet
	private static class AlbumImagesData
	{
		///////////////////////////////////////////////////////////////////////////
		AlbumImagesData (Collection<AlbumImage> images, long lastUpdateMillis)
		{
			_images = images;
			_lastUpdateMillis = lastUpdateMillis;
		}

		///////////////////////////////////////////////////////////////////////////
		public Collection<AlbumImage> getImages ()
		{
			return _images;
		}

		///////////////////////////////////////////////////////////////////////////
		public long getLastUpdateMillis ()
		{
			return _lastUpdateMillis;
		}

		//members
		private final Collection<AlbumImage> _images;
		private final long _lastUpdateMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI
	private static class AlbumImageEvent
	{
		///////////////////////////////////////////////////////////////////////////
		AlbumImageEvent (Path dir, WatchEvent<Path> pathEvent)
		{
			_timestamp = new GregorianCalendar ().getTimeInMillis ();
			_dir = dir;
			_pathEvent = pathEvent;
		}

		///////////////////////////////////////////////////////////////////////////
		public long getTimestamp () //millisecs
 		{
			return _timestamp;
		}

		///////////////////////////////////////////////////////////////////////////
		public Path getDir ()
		{
			return _dir;
		}

		///////////////////////////////////////////////////////////////////////////
		public WatchEvent<Path> getPathEvent ()
		{
			return _pathEvent;
		}


		//members
		private final long _timestamp; //millisecs
		private final Path _dir;
		private final WatchEvent<Path> _pathEvent;
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI and servlet
	private static class AlbumImageDaoUncaughtExceptionHandler implements UncaughtExceptionHandler
	{
		///////////////////////////////////////////////////////////////////////////
		@Override
		public void uncaughtException (Thread thread, Throwable ex)
		{
			_log.error ("AlbumImageDaoUncaughtExceptionHandler: thread: " + thread.getName () + ": ", ex);

//TODO - once we get restartWatcherThreadsForFolder() working, call it here, too
//			if (_isCLI) { //restart any watcher threads that die
//				String subFolder = _watcherThreadMap.get (thread);
//				if (subFolder != null) {
//					Thread watcherThread = AlbumImageDao.getInstance ().createWatcherThreadsForFolder (subFolder);
//					_watcherThreadMap.put (watcherThread, subFolder);
//					_log.debug ("AlbumImageDaoUncaughtExceptionHandler: createWatcherThreadsForFolder: " + subFolder);
//
//				} else {
//					_log.error ("AlbumImageDaoUncaughtExceptionHandler: thread not found in map: " + thread.getName ());
//				}
//			}
//
//			Thread.currentThread ().interrupt ();
		}
	}


	//members
//	private static boolean _isCLI = false; //true if running as CLI, false for servlet
	private static boolean _isServer = false; //true if running as (tomcat) server, false for CLI
	private /*static*/ boolean _syncOnly = false; //used by CLI: sync DB to FS then exit, do not start any watchers

	private SqlSessionFactory _sqlSessionFactory = null;

	private final String _rootPath;

	private Collection<String> _subFolders = null;
	private final Set<String> _subFoldersOverrideSet = new TreeSet<>();
//	private static final int _subFolderMinLength = 2;

	private static final int _pauseSleepMillis = 1000;
//	private Thread _pauseThread = null;
//	private final AtomicBoolean _pauseModeEnabled = new AtomicBoolean();
	private static final String _pauseFilename = "albumServer.pause.txt";

	private static volatile AlbumImageDao _instance = null;

	private static Map<Integer, Set<String>> _subFolderByLengthMap = null;

	private static final int _estimatedNumberOfSubFolders = 300;

	private static final Map<String, Thread> _handlerThreadMap = new ConcurrentHashMap<> (_estimatedNumberOfSubFolders);
	private static final Map<String, Thread> _watcherThreadMap = new ConcurrentHashMap<> (_estimatedNumberOfSubFolders);

	private static final Map<String, AlbumImagesData> _albumImagesDataCache = new HashMap<> (_estimatedNumberOfSubFolders);
	private static final Map<String, Map<String, Integer>> _albumImagesCountCache = new HashMap<> (_estimatedNumberOfSubFolders);
	private static final Map<String, Map<String, Integer>> _albumAlbumsCountCache = new HashMap<> (_estimatedNumberOfSubFolders);
	private static final Map<String, Map<String, Boolean>> _albumImagesHasExifDataCache = new HashMap<> (_estimatedNumberOfSubFolders);

	private static final Map<String, String> _servletMisFiledImageErrorsMap = new ConcurrentHashMap<> (_estimatedNumberOfSubFolders);

	private static final ThreadLocal<Set<String>> _imagesNeedingCountUpdate = ThreadLocal.withInitial(HashSet::new);

	private static final String NL = System.getProperty ("line.separator");
//	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int
//	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

	private static boolean _Debug = false;
	private static final Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumImageDao";
}
