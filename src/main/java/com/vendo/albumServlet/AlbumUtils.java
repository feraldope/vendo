//AlbumUtils.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VFileList;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class AlbumUtils {

	///////////////////////////////////////////////////////////////////////////
	static {
		Thread.setDefaultUncaughtExceptionHandler(new AlbumUncaughtExceptionHandler());
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumUtils() {
	}

	///////////////////////////////////////////////////////////////////////////
	//entry point for "update database" feature
	public static void main(String[] args) {
		_log.debug("AlbumUtils.main - entering main");

//		AlbumFormInfo.getInstance(); //call ctor to load class defaults

		//CLI overrides
//		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
//		AlbumFormInfo._profileLevel = 0; //enable profiling just before calling run()
		AlbumFormInfo._profileLevel = 7;

		AlbumUtils AlbumUtils = new AlbumUtils();

		if (!AlbumUtils.processArgs(args)) {
			System.exit(1); //processArgs displays error
		}

		try {
			AlbumUtils.run();

		} catch (Exception ee) {
			_log.error("AlbumUtils.main: ", ee);
		}

//		AlbumProfiling.getInstance ().exit (1); //called in run1()

		_log.debug("AlbumUtils.main - leaving main");
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs(String[] args) {
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith("-") || arg.startsWith("/")) {
				arg = arg.substring(1);

				if (arg.equalsIgnoreCase("debug") || arg.equalsIgnoreCase("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase("test") || arg.equalsIgnoreCase("tst")) {
					_Test = true;

				} else {
					displayUsage("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				displayUsage("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//check for required args and handle defaults
		//...

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage(String message, Boolean exit) {
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/test] **TBD**";
		System.err.println("Error: " + msg + NL);

		if (exit) {
			System.exit(1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run() {
		Path propertiesFile = Paths.get("D:/Netscape/Program/albumUtils.properties");
		Properties properties = new Properties ();
		try (InputStream inputStream = Files.newInputStream(propertiesFile)) {
			properties.load (inputStream);

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		String aliasesFile = getStringProperty(properties, "aliasesFile");
		String downloadZipFolder = getStringProperty(properties, "downloadZipFolder");
		String downloadMp4Folder = getStringProperty(properties, "downloadMp4Folder");
		String movHistoryFile = getStringProperty(properties, "movHistoryFile");

		if (null == aliasesFile || null == downloadZipFolder || null == downloadMp4Folder || null == movHistoryFile) {
			return false; //getStringProperty printed error
		}

		if (!VendoUtils.fileExists (aliasesFile)) {
			_log.error ("AlbumUtils.run: error: aliases file does not exist: " + aliasesFile);
			return false;
		}

		if (!VendoUtils.fileExists (downloadZipFolder)) {
			_log.error ("AlbumUtils.run: error: download zip folder does not exist: " + downloadZipFolder);
			return false;
		}

		if (!VendoUtils.fileExists (downloadMp4Folder)) {
			_log.error ("AlbumUtils.run: error: download mp4 folder does not exist: " + downloadMp4Folder);
			return false;
		}

		if (!VendoUtils.fileExists(movHistoryFile)) {
			_log.error ("AlbumUtils.run: error: mov history file does not exist: " + movHistoryFile);
			return false;
		}

		//process baseNames
		List<String> baseNames = Arrays.stream(properties.getProperty("baseNames").split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		String baseName = baseNames.get(0); //FOR NOW, JUST PROCESS FIRST ONE

		//process aliases
		final Path aliasesPath = FileSystems.getDefault().getPath(aliasesFile);
		final Pattern aliasesPattern = Pattern.compile("([A-Za-z]+) = ([A-Za-z]+) \\(" + baseName.toLowerCase() + "\\).*");
		final Aliases aliases = new Aliases(baseName);
		try {
			final List<String> matchingLines = Files.readAllLines(aliasesPath, StandardCharsets.ISO_8859_1).stream() //Note: using StandardCharsets.UTF_8 threw MalformedInputException; not sure why
					.filter(l -> {
						Matcher movMatcher = aliasesPattern.matcher(l);
						if (movMatcher.find()) {
							String nameInAlbum = movMatcher.group(1);
							String nameFromAlias = movMatcher.group(2);
							aliases.add(nameInAlbum, nameFromAlias);
							return true;
						}
						return false;
					})
					.collect(Collectors.toList());

		} catch (IOException ex) {
			_log.error ("AlbumUtils.run: error reading/parsing aliases file: " + aliasesFile, ex);
			return false;
		}

//old way to read aliases (KEEP - see properties file for reason)
//		Map<String, String> aliasesMap = new HashMap<>();
//		Arrays.stream(properties.getProperty("aliases").split(";"))
//			.map(String::trim)
//			.filter(s -> !s.isEmpty())
//			.forEach(r -> {
//					String[] items = r.split(":");
//					aliasesMap.put(items[0].trim(), items[1].trim());
//			});

		readAndProcessAllRecords(baseName, downloadZipFolder, downloadMp4Folder, movHistoryFile, aliases);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readAndProcessAllRecords(String baseName, String downloadZipFolder, String downloadMp4Folder, String movHistoryFile, Aliases aliases) {
		_log.debug("AlbumUtils.readAndProcessAllRecords: baseName: " + baseName);

		Instant startInstant = Instant.now ();
		List<NameComponentsDownload> downloadZipRecords;
		try {
			downloadZipRecords = getAllValidDownloadRecords(downloadZipFolder, baseName, "zip", true);

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		String elapsedString = "elapsed time: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()).format(_dateTimeFormatter);
		_log.debug("AlbumUtils.readAndProcessAllRecords: found " + downloadZipRecords.size() + " download zip records, " + elapsedString);

		startInstant = Instant.now ();
		List<NameComponentsDownload> downloadMp4Records;
		try {
			downloadMp4Records = getAllValidDownloadRecords(downloadMp4Folder, baseName, "mp4", true);

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		elapsedString = "elapsed time: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()).format(_dateTimeFormatter);
		_log.debug("AlbumUtils.readAndProcessAllRecords: found " + downloadMp4Records.size() + " download mp4 records, " + elapsedString);

		Path movHistoryPath = FileSystems.getDefault().getPath(movHistoryFile);
		startInstant = Instant.now ();
		List<NameComponentsMovHistory> movHistoryRecords;
		try {
			movHistoryRecords = getAllDistinctValidMovHistoryRecords(movHistoryPath, baseName, aliases, true);

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		elapsedString = "elapsed time: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()).format(_dateTimeFormatter);
		_log.debug("AlbumUtils.readAndProcessAllRecords: found " + movHistoryRecords.size() + " mov history records, " + elapsedString);

		linkTheRecords(downloadZipRecords, movHistoryRecords);

		checkForRelativeDuplicateMovHistoryRecords(movHistoryRecords);

		checkForDuplicateDownloads(downloadZipRecords);

		checkForDuplicateDownloads(downloadMp4Records);

		checkForMisMatchesBetweenMovHistoryAndDownloadZips(downloadZipRecords, movHistoryRecords);

		checkForMisMatchesWithinMovHistoryForAlbumNames(baseName, movHistoryRecords);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<NameComponentsDownload> getAllValidDownloadRecords(String downloadFolder, String baseName, String extension, final boolean printSkippedLines) {
		final List<String> skippedLines = new ArrayList<>();
		final List<NameComponentsDownload> nameComponents = new ArrayList<>();

		final Pattern downloadPatternStrict = "zip".equals(extension)
				? Pattern.compile("(" + baseName + ")_(\\d{4}-\\d{2}-\\d{2})_(.*)-by-.*_(high|med)\\." + extension)
				: Pattern.compile("(" + baseName + ")-(\\d{4}-\\d{2}-\\d{2})-(.*)(-1080|-1080-scaled|-4k)\\." + extension);
		final Pattern downloadPatternLoose = Pattern.compile("(" + baseName + ")[_-].*\\." + extension);

		final List<String> fileList = new VFileList(downloadFolder, baseName + "*." + extension, false).getFileList(VFileList.ListMode.FileOnly);
		if (fileList.isEmpty()) {
			throw new IllegalArgumentException("no matching files found in download folder \"" + downloadFolder + "\"");
		}

		final List<String> lines = fileList.stream()
				.filter(l -> {
					Matcher downloadMatcher = downloadPatternStrict.matcher(l);
					if (downloadMatcher.find()) {
						String prefix = downloadMatcher.group(1);
						String dateString = downloadMatcher.group(2);
						String middle = downloadMatcher.group(3);
						NameComponentsDownload componentsDownload = new NameComponentsDownload(l, prefix, middle, dateString);
						nameComponents.add(componentsDownload);
						return true;
					} else if (downloadPatternLoose.matcher(l).matches()) {
						skippedLines.add(l);
						return false;
					}
					return false;
				})
				.collect(Collectors.toList());

		if (printSkippedLines && !skippedLines.isEmpty()) {
			final int maxCharsToPrint = 100; //hardcoded

			System.out.println("AlbumUtils.getAllValidDownloadRecords: SKIPPED LINES (did not match pattern):");
			skippedLines.stream().sorted().forEach(l -> {
				int charsToPrint = Math.min(l.length(), maxCharsToPrint);
				boolean truncated = l.length() > charsToPrint;
				System.out.println(l.substring(0, charsToPrint) + (truncated ? "*" : ""));
			});

			final int maxExpectedSkippedLines = 100;
			if (skippedLines.size() > maxExpectedSkippedLines) { //hack
				throw new RuntimeException("Error: number of skipped lines (" + skippedLines.size() + ") is greater than expected (" + maxExpectedSkippedLines + ").");
			}
		}

		List<NameComponentsDownload> sortedList = nameComponents.stream().sorted(new NameComponentsDownload("", "", "", "")).collect(Collectors.toList());
		return sortedList;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<NameComponentsMovHistory> getAllDistinctValidMovHistoryRecords(Path filePath, String baseName, Aliases aliases, boolean printSkippedLines) throws Exception {
		final List<String> skippedLines = new ArrayList<>();
		final List<NameComponentsMovHistory> nameComponents = new ArrayList<>();

		final Pattern movPatternStrict = Pattern.compile("mov (" + baseName + ")_(.*)_(high|med)_0\\*(|g) (.*)-\\*(|g)");
		final Pattern movPatternLoose = Pattern.compile("mov (" + baseName + ")_.*_(high|med).*");

		final List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8).stream()
				.filter(l -> {
					Matcher movMatcher = movPatternStrict.matcher(l);
					if (movMatcher.find()) {
						String prefix = movMatcher.group(1);
						String middle = movMatcher.group(2);
						String albumName = movMatcher.group(5);
						NameComponentsMovHistory componentsMovHistory = new NameComponentsMovHistory(l, prefix, middle, albumName, aliases);
						nameComponents.add(componentsMovHistory);
						return true;
					} else if (movPatternLoose.matcher(l).matches()) {
						skippedLines.add(l);
						return false;
					}
					return false;
				})
			.collect(Collectors.toList());

		if (printSkippedLines && !skippedLines.isEmpty()) {
			final int maxCharsToPrint = 100; //hardcoded

			System.out.println("AlbumUtils.getAllDistinctValidMovHistoryRecords: SKIPPED LINES: ");
			skippedLines.stream().sorted().forEach(l -> {
				int charsToPrint = Math.min(l.length(), maxCharsToPrint);
				boolean truncated = l.length() > charsToPrint;
				System.out.println(l.substring(0, charsToPrint) + (truncated ? "*" : ""));
			});

			final int maxExpectedSkippedLines = 100; //hardcoded
			if (skippedLines.size() > maxExpectedSkippedLines) { //hack
				throw new RuntimeException("Error: number of skipped lines (" + skippedLines.size() + ") is greater than expected (" + maxExpectedSkippedLines + ").");
			}
		}

		//remove absolute dups (where every field is the same)
		Set<NameComponentsMovHistory> setNoDups = new HashSet<>((new ArrayList<>(nameComponents)));

		List<NameComponentsMovHistory> sortedList = setNoDups.stream().sorted(new NameComponentsMovHistory()).collect(Collectors.toList());

		_log.debug("AlbumUtils.getAllDistinctValidMovHistoryRecords: removed " + (nameComponents.size() - sortedList.size()) + " duplicate mov history records");

		return sortedList;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean checkForRelativeDuplicateMovHistoryRecords(List<NameComponentsMovHistory> movHistoryRecords) {
		List<NameComponentsMovHistory> dupsByComponent;
		List<NameComponentsMovHistory> dupsByAlbumName;

		{ 	//check for duplicates by component
			Map<String, Long> movHistoryComponentCountMap = movHistoryRecords.stream()
					.map(NameComponentsMovHistory::getComponentsDashed)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

//this syntax does not work
//			Map<List<String>, Long> movHistoryDupsByComponentMap = movHistoryRecords.stream()
//					.map(NameComponentsMovHistory::getComponents)
//					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

/* example that might work
        // create multimap and store the value of list
        Map<Integer, List<String>> multimap = lt.stream()
                         .collect(Collectors.groupingBy(
                                       Student::getId,
                                       Collectors.mapping(
                                               Student::getName,
                                               Collectors.toList())));
*/

			dupsByComponent = movHistoryRecords.stream()
					.filter(r -> null != r.getNameComponentsMatching()) //only match linked records (is this correct??)
					.filter(r -> movHistoryComponentCountMap.get(r.getComponentsDashed()) > 1)
					.collect(Collectors.toList());

			_log.debug("AlbumUtils.checkForRelativeDuplicateMovHistoryRecords: found " + dupsByComponent.size() + " duplicate mov history records BY COMPONENT");
		}

		{    //check for duplicates by albumName
			Map<String, Long> movHistoryAlbumNameCountMap = movHistoryRecords.stream()
					.map(NameComponentsMovHistory::getAlbumName)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

			dupsByAlbumName = movHistoryRecords.stream()
					.filter(r -> null != r.getNameComponentsMatching()) //only match linked records (is this correct??)
					.filter(r -> movHistoryAlbumNameCountMap.get(r.getAlbumName()) > 1)
					.collect(Collectors.toList());

			_log.debug("AlbumUtils.checkForRelativeDuplicateMovHistoryRecords: found " + dupsByAlbumName.size() + " duplicate mov history records BY ALBUM NAME");
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean checkForMisMatchesBetweenMovHistoryAndDownloadZips(List<NameComponentsDownload> downloadZipRecords, List<NameComponentsMovHistory> movHistoryRecords) {
		List<String> listByDownloadZipComponents = downloadZipRecords.stream()
				.map(NameComponentsDownload::getComponentsDashedLower)
				.collect(Collectors.toList());

		List<String> listByMovHistoryComponents = movHistoryRecords.stream()
				.map(NameComponentsMovHistory::getComponentsDashedLower)
				.collect(Collectors.toList());

		Collection<String> setByDownloadZipComponents = new HashSet(listByDownloadZipComponents);
		Collection<String> setByMovHistoryComponents = new HashSet(listByMovHistoryComponents);

		Collection<String> missingFromDownloadZips = VendoUtils.subtractCollections(setByMovHistoryComponents, setByDownloadZipComponents);
		Collection<String> missingFromMovHistory = VendoUtils.subtractCollections(setByDownloadZipComponents, setByMovHistoryComponents);

		_log.debug("AlbumUtils.checkForMisMatchesBetweenMovHistoryAndDownloadZips: found " + missingFromDownloadZips.size() + " items missing from zip downloads");
		_log.debug("AlbumUtils.checkForMisMatchesBetweenMovHistoryAndDownloadZips: found " + missingFromMovHistory.size() + " items missing from mov history");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean checkForMisMatchesWithinMovHistoryForAlbumNames(String baseName, List<NameComponentsMovHistory> movHistoryRecords) {
		List<NameComponentsMovHistory> listMisMatchesWithinMovHistory = movHistoryRecords.stream()
				.filter(r -> {
					//check name
					if (null != r.getNameInAlbumDashed() && r.getComponentsDashed().contains(r.getNameInAlbumDashed())) {
						return false;
					}
					//check alias
					if (null != r.getNameFromAliasDashed() && r.getComponentsDashed().contains(r.getNameFromAliasDashed())) {
						return false;
					}
					return true;
				})
				.collect(Collectors.toList());

		_log.debug("AlbumUtils.checkForMisMatchesWithinMovHistoryForAlbumNames: found " + listMisMatchesWithinMovHistory.size() + " mismatched items");

		if (true) {
			final int limit = 20; //hardcoded
			String limitString = listMisMatchesWithinMovHistory.size() > limit ? "(limited to first " + limit + " items)" : "";
			_log.debug("AlbumUtils.checkForMisMatchesWithinMovHistoryForAlbumNames: listMisMatchesWithinMovHistory: " + limitString + NL +
//					"CONSIDER ADDING THESE TO ALIASES FILE:" + NL +
					listMisMatchesWithinMovHistory.stream()
///*TEMP HACK*/ .filter(r -> !r.getNameInAlbumDashed().endsWith("-A") && !r.getNameInAlbumDashed().endsWith("-B")) //to help debugging, filter out any records that might match the case where Foo is saved as FooA or FooB (which we have not solved yet)
							.limit(limit)
							.map(NameComponentsMovHistory::toString)
							.collect(Collectors.joining(NL))
					+ NL);
		}

		if (true) {
			Map<String, String> suggestedAliasesMap = listMisMatchesWithinMovHistory.stream()
///*TEMP HACK*/ .filter(r -> !r.getNameInAlbumDashed().endsWith("-A") && !r.getNameInAlbumDashed().endsWith("-B")) //to help debugging, filter out any records that might match the case where Foo is saved as FooA or FooB (which we have not solved yet)
					.collect(Collectors.toMap(k -> k.getNameInAlbumDashed().replaceAll("-", ""),
											  k -> k.getComponentsDashed(),
											  (oldValue, newValue) -> { //using merge function
													return oldValue;
											  }));

			final int limit = 100; //hardcoded
			String limitString = suggestedAliasesMap.size() > limit ? "(limited to first " + limit + " items)" : "";
			_log.debug("AlbumUtils.checkForMisMatchesWithinMovHistoryForAlbumNames: suggestedAliasesMap: " + limitString + NL +
					"CONSIDER ADDING THESE TO ALIASES FILE:" + NL +
					suggestedAliasesMap.entrySet().stream()
							.limit(limit)
							.map(r -> r.getKey().replaceAll("-", "") + " = " + r.getValue() + " (" + baseName.toLowerCase() + ")")
							.sorted()
							.distinct()
							.collect(Collectors.joining(NL))
					+ NL);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean checkForDuplicateDownloads(List<NameComponentsDownload> downloadRecords) {
		Map<String, List<String>> multimap = downloadRecords.stream()
				.collect(Collectors.groupingBy(r -> r.getOriginalString().replaceAll("_(high|med).zip", "")
																		 .replaceAll("-(1080|4k).*", ""),
											   Collectors.mapping(NameComponentsDownload::getOriginalString,
																  Collectors.toList())));

		List<String> dups = multimap.entrySet().stream()
				.filter(e -> e.getValue().size() > 1)
				.map(e -> String.join(NL, e.getValue()))
				.collect(Collectors.toList());

		_log.debug("AlbumUtils.checkForDuplicateDownloads: found " + dups.size() + " dups: " + NL + String.join(NL, dups));

		if (dups.size() > 0) {
			multimap.entrySet().stream()
					.filter(e -> e.getValue().size() > 1)
					.map(e -> "dir " + e.getKey() + "*")
					.forEach(System.out::println);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean linkTheRecords(List<NameComponentsDownload> downloadZipRecords, List<NameComponentsMovHistory> movHistoryRecords) {
		Map<String, NameComponentsMovHistory> movHistoryComponentsMap = movHistoryRecords.stream()
				.collect(Collectors.toMap(NameComponentsBase::getComponentsDashedLower, k -> k, (oldValue, newValue) -> { //using merge function
//					_log.debug("AlbumUtils.linkTheRecords: mov history dups that need to be merged: " + oldValue.getAlbumName() + ", " + newValue.getAlbumName());
					return oldValue;
				}));

		Map<String, NameComponentsDownload> downloadZipComponentsMap = downloadZipRecords.stream()
				.collect(Collectors.toMap(NameComponentsBase::getComponentsDashedLower, k -> k, (oldValue, newValue) -> { //using merge function
//					_log.debug("AlbumUtils.linkTheRecords: download dups that need to be merged: " + oldValue.getComponentsDashed() + ", " + newValue.getComponentsDashed());
					return oldValue;
				}));

		//link the two collections together - NOTE WE MERGED AWAY THE DUPS ABOVE and those dups won't get matched
		movHistoryComponentsMap.entrySet().forEach(i -> i.getValue().setNameComponentsMatching(downloadZipComponentsMap.get(i.getKey())));
		downloadZipComponentsMap.entrySet().forEach(i -> i.getValue().setNameComponentsMatching(movHistoryComponentsMap.get(i.getKey())));

		if (true) { //print the results
			final int limit = 20; //hardcoded
			String limitString = movHistoryComponentsMap.size() > limit ? "(limited to first " + limit + " items)" : "";
			_log.debug("AlbumUtils.linkTheRecords: mov history: no matching zip download for: " + limitString + NL +
					movHistoryComponentsMap.entrySet().stream()
						.filter(i -> null == i.getValue().getNameComponentsMatching())
						.limit(limit)
						.map(i -> "[" + i.getKey() + "] -> " + i.getValue())
						.collect(Collectors.joining(NL))
					+ NL);

			limitString = downloadZipComponentsMap.size() > limit ? "(limited to first " + limit + " items)" : "";
			_log.debug("AlbumUtils.linkTheRecords: zip download: no matching mov history for: " + limitString + NL +
					downloadZipComponentsMap.entrySet().stream()
						.filter(i -> null == i.getValue().getNameComponentsMatching())
						.limit(limit)
						.map(i -> "[" + i.getKey() + "] -> " + i.getValue())
						.collect(Collectors.joining(NL))
					+ NL);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	String getStringProperty(Properties properties, String propertyName) {
		String propertyValue = properties.getProperty(propertyName);
		if (null == propertyValue || propertyValue.trim().isEmpty()) {
			_log.debug("AlbumUtils.getStringProperty: error: no value found for property named '" + propertyName + "'");
			return null;
		}

		return propertyValue.trim();
	}

	///////////////////////////////////////////////////////////////////////////
	private static class Aliases {
		///////////////////////////////////////////////////////////////////////////
		Aliases(String baseName) {
			this.baseName = baseName;
		}

		///////////////////////////////////////////////////////////////////////////
		public void add(String nameInAlbum, String nameFromAlias) {
			aliasesMap.put(nameInAlbum, nameFromAlias);
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getNameFromAlias(String nameInAlbum) {
			String nameFromAlias = aliasesMap.get(nameInAlbum);
			return nameFromAlias;
		}

		protected final String baseName;
		protected final Map<String, String> aliasesMap = new HashMap<>();
	}

	///////////////////////////////////////////////////////////////////////////
	private static class NameComponentsBase {
		///////////////////////////////////////////////////////////////////////////
		NameComponentsBase(String originalString, String prefix, String middle) {
			this.originalString = originalString;
			this.prefix = prefix;
			components = Arrays.stream(middle.split("[_-]"))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.collect(Collectors.toList());

			//some hacks - if the last two pairs of components are the same, remove the dups (i.e., [cat, dog, cat, dog] -> [cat, dog)
			int lastIndex = components.size() - 1;
			if (lastIndex >= 3 && components.get(lastIndex).equalsIgnoreCase(components.get(lastIndex - 2)) && components.get(lastIndex - 1).equalsIgnoreCase(components.get(lastIndex - 3))) {
				components.remove(lastIndex);
				components.remove(lastIndex - 1);
			}

			//some hacks - if the last two components are the same, remove the dup (i.e., [cat, cat] -> [cat])
			lastIndex = components.size() - 1;
			if (lastIndex >= 2 && components.get(lastIndex - 1).equalsIgnoreCase(components.get(lastIndex - 2))) {
				components.remove(lastIndex - 1);
			}

			//some hacks - if the two components before the last are the same, remove the dup (i.e., [cat, cat, dog] -> [cat, dog])
			lastIndex = components.size() - 1;
			if (lastIndex >= 1 && components.get(lastIndex).equalsIgnoreCase(components.get(lastIndex - 1))) {
				components.remove(lastIndex);
			}

			componentsDashed = String.join("-", getComponents());
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getOriginalString() {
			return originalString;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getPrefix() {
			return prefix;
		}

		///////////////////////////////////////////////////////////////////////////
		protected List<String> getComponents() {
			return components;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getComponentsDashed() {
			return componentsDashed;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getComponentsDashedLower() {
			return componentsDashed.toLowerCase();
		}

		///////////////////////////////////////////////////////////////////////////
		protected void setNameComponentsMatching(NameComponentsBase nameComponentsMatching) {
			this.nameComponentsMatching = nameComponentsMatching;
		}

		///////////////////////////////////////////////////////////////////////////
		protected NameComponentsBase getNameComponentsMatching() {
			return nameComponentsMatching;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			NameComponentsBase that = (NameComponentsBase) o;
			return getPrefix().equals(that.getPrefix()) && getComponents().equals(that.getComponents());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int hashCode() {
			return Objects.hash(getPrefix(), getComponents());
		}

		protected final String originalString;
		protected final String prefix;
		protected final List<String> components;
		protected final String componentsDashed;
		protected NameComponentsBase nameComponentsMatching;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class NameComponentsDownload extends NameComponentsBase implements Comparator<NameComponentsDownload> {
		///////////////////////////////////////////////////////////////////////////
		NameComponentsDownload() {
			this("uninitialized", "uninitialized", "uninitialized", "uninitialized"); //ctor to be used only to initialize for Comparator
		}

		///////////////////////////////////////////////////////////////////////////
		NameComponentsDownload(String originalString, String prefix, String middle, String dateString) {
			super(originalString, prefix, middle);
			this.dateString = dateString;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getDateString() {
			return dateString;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getNameSuggestion1() {
			return getComponents().get(getComponents().size() - 1);
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getNameSuggestion2() {
			return getComponents().get(getComponents().size() - 1) + getComponents().get(getComponents().size() - 1);
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compare(NameComponentsDownload o1, NameComponentsDownload o2) {
			return String.join(",", o1.getComponents()).compareTo(String.join(",", o2.getComponents()));
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString() {
			String matchingAlbumName = "";
			if (null != nameComponentsMatching && nameComponentsMatching instanceof NameComponentsMovHistory) {
				matchingAlbumName = ((NameComponentsMovHistory) nameComponentsMatching).getAlbumName();
			}
			final StringBuffer sb = new StringBuffer("NameComponentsDownload{");
			sb.append("prefix='").append(prefix).append('\'');
			sb.append(", componentsDashed=").append(componentsDashed);
			sb.append(", dateString='").append(dateString).append('\'');
			sb.append(", matchingAlbumName='").append(matchingAlbumName).append('\'');
			sb.append('}');
			return sb.toString();
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}
			NameComponentsDownload that = (NameComponentsDownload) o;
			return Objects.equals(getDateString(), that.getDateString());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), getDateString());
		}

		protected final String dateString;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class NameComponentsMovHistory extends NameComponentsBase implements Comparator<NameComponentsMovHistory> {
		///////////////////////////////////////////////////////////////////////////
		NameComponentsMovHistory() {
			this("uninitialized", "uninitialized", "uninitialized", "uninitialized", null); //ctor to be used only to initialize for Comparator
		}

		///////////////////////////////////////////////////////////////////////////
		NameComponentsMovHistory(String originalString, String prefix, String middle, String albumName, Aliases aliases) {
			super(originalString, prefix, middle);
			this.albumName = albumName;

//TODO - handle case where Foo is being saved as FooA, etc.

			if ("uninitialized".equals(albumName)) {
				nameInAlbumDashed = "uninitialized";  //used in ctor when initializing for Comparator
			} else {
				nameInAlbumDashed = dashTheName(albumName);
			}

			if (null != aliases) {
				String nameFromAlias = aliases.getNameFromAlias(albumName.replaceAll("\\d+", ""));
				if (null != nameFromAlias && !nameFromAlias.isEmpty()) {
					nameFromAliasDashed = dashTheName(nameFromAlias);
				}
			}
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getAlbumName() {
			return albumName;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getNameInAlbumDashed() {
			return nameInAlbumDashed;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getNameFromAliasDashed() {
			return nameFromAliasDashed;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String dashTheName(String undashedName) {
			final Pattern undashedNamePattern = Pattern.compile("([A-Z][a-z]+)([A-Z][a-z]+|[A-Z]|).*$");

			String dashedName;

			undashedName = undashedName.replaceAll("\\d+", "");
			Matcher undashedNameMatcher = undashedNamePattern.matcher(undashedName); //method param has mixed case, we need to operate on this param
			if (undashedNameMatcher.find()) {
				String lastName = undashedNameMatcher.group(2);
				dashedName = undashedNameMatcher.group(1) + (lastName.isEmpty() ? "" : "-" + lastName);
			} else {
				dashedName = undashedName; //failed to dash the name
				_log.warn("AlbumUtils.dashTheName: failed for undashedName: " + undashedName);
			}

			return dashedName;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compare(NameComponentsMovHistory o1, NameComponentsMovHistory o2) {
			return String.join(",", o1.getComponents()).compareTo(String.join(",", o2.getComponents()));
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString() {
			String matchingDateString = "";
			if (null != nameComponentsMatching && nameComponentsMatching instanceof NameComponentsDownload) {
				matchingDateString = ((NameComponentsDownload) nameComponentsMatching).getDateString();
			}
			final StringBuffer sb = new StringBuffer("NameComponentsMovHistory{");
			sb.append("prefix='").append(prefix).append('\'');
			sb.append(", componentsDashed=").append(componentsDashed);
			sb.append(", albumName='").append(albumName).append('\'');
			sb.append(", nameInAlbumDashed='").append(nameInAlbumDashed).append('\'');
			sb.append(", nameFromAliasDashed='").append(nameFromAliasDashed).append('\'');
			sb.append(", matchingDateString='").append(matchingDateString).append('\'');
			sb.append('}');
			return sb.toString();
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}
			NameComponentsMovHistory that = (NameComponentsMovHistory) o;
			return getAlbumName().equals(that.getAlbumName()) && getNameInAlbumDashed().equals(that.getNameInAlbumDashed());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), getAlbumName(), getNameInAlbumDashed());
		}

		protected final String albumName;
		protected final String nameInAlbumDashed;
		protected String nameFromAliasDashed;
	}


	//members
//	private SqlSessionFactory _sqlSessionFactory = null;

	private static final String NL = System.getProperty ("line.separator");
	private static final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("HH'h':mm'm':ss's'"); //for example: 01h:03m:12s (note this wraps values >= 24 hours)

	private static boolean _Debug = false;
	private static boolean _Test = false;

	private static final Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumUtils";
}
