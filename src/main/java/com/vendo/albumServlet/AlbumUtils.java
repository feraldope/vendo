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
		String subFoldersOverrideStr = "";

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith("-") || arg.startsWith("/")) {
				arg = arg.substring(1);

				if (arg.equalsIgnoreCase("debug") || arg.equalsIgnoreCase("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase("test") || arg.equalsIgnoreCase("tst")) {
					_Test = true;

//				} else if (arg.equalsIgnoreCase("subFolders") || arg.equalsIgnoreCase("sub") || arg.equalsIgnoreCase("s")) {
//					try {
//						_subFolders = null; //force this to be recalculated
//						subFoldersOverrideStr = args[++ii];
//					} catch (ArrayIndexOutOfBoundsException exception) {
//						displayUsage("Missing value for /" + arg, true);
//					}

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

		String downloadFolder = properties.getProperty("downloadFolder").trim();
		String movHistoryFile = properties.getProperty("movHistoryFile").trim();

		//process baseNames
		List<String> baseNames = Arrays.stream(properties.getProperty("baseNames").split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		String baseName = baseNames.get(0);

		//process pairs
		Map<String, String> pairsMap = new HashMap<>();
		Arrays.stream(properties.getProperty("pairs").split(";"))
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.forEach(r -> {
					String[] items = r.split(":");
					pairsMap.put(items[0].trim(), items[1].trim());
			});

		if (!Files.exists (FileSystems.getDefault ().getPath (downloadFolder))) {
			_log.error ("AlbumUtils.run: error: download folder does not exist: " + downloadFolder);
			return false;
		}

		if (!Files.exists (FileSystems.getDefault ().getPath (movHistoryFile))) {
			_log.error ("AlbumUtils.run: error: mov history file does not exist: " + movHistoryFile);
			return false;
		}

		readAndProcessAllRecords(baseName, downloadFolder, movHistoryFile);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readAndProcessAllRecords(String baseName, String downloadFolder, String movHistoryFile) {
		String wildName = baseName + "*.zip";

		List<NameComponentsDownload> downloadRecords;
		try {
			downloadRecords = getAllValidDownloadRecords(downloadFolder, baseName, wildName, true);

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		_log.debug("AlbumUtils.readAndProcessAllRecords: found " + downloadRecords.size() + " download records");

		Path movHistoryPath = FileSystems.getDefault().getPath(movHistoryFile);

		List<NameComponentsMovHistory> movHistoryRecords;
		try {
			movHistoryRecords = getAllDistinctValidMovHistoryRecords(movHistoryPath, baseName, true);

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		_log.debug("AlbumUtils.readAndProcessAllRecords: found " + movHistoryRecords.size() + " mov history records");

		linkTheRecords(downloadRecords, movHistoryRecords);

		checkForRelativeDuplicateMovHistoryRecords(movHistoryRecords);

		checkForMisMatchesBetweenMovHistoryAndDownloads(downloadRecords, movHistoryRecords);

		checkForMisMatchesWithinMovHistoryForAlbumNames(movHistoryRecords);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<NameComponentsDownload> getAllValidDownloadRecords(String downloadFolder, String baseName, String wildName, final boolean printSkippedLines) {
		final List<String> skippedLines = new ArrayList<>();
		final List<NameComponentsDownload> nameComponents = new ArrayList<>();

		final Pattern downloadPatternStrict = Pattern.compile("(" + baseName + ")_(\\d{4}-\\d{2}-\\d{2})_(.*)-by-.*_(high|med)\\.zip");
		final Pattern downloadPatternLoose = Pattern.compile("(" + baseName + ")_.*\\.zip");

		List<String> fileList = new VFileList(downloadFolder, wildName, false).getFileList(VFileList.ListMode.FileOnly);
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
						NameComponentsDownload componentsDownload = new NameComponentsDownload(prefix, middle, dateString);
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

			System.out.println("AlbumUtils.readAllValidLines: SKIPPED LINES");
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

		List<NameComponentsDownload> sortedList = nameComponents.stream().sorted(new NameComponentsDownload("", "", "")).collect(Collectors.toList());
		return sortedList;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<NameComponentsMovHistory> getAllDistinctValidMovHistoryRecords(Path filePath, String baseName, final boolean printSkippedLines) throws Exception {
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
						NameComponentsMovHistory componentsMovHistory = new NameComponentsMovHistory(prefix, middle, albumName);
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

			System.out.println("AlbumUtils.getAllDistinctValidMovHistoryRecords: SKIPPED LINES");
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
					.map(NameComponentsMovHistory::getComponentsAsNormalizedString)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

//this syntax does not work
//			Map<List<String>, Long> movHistoryDupsByComponentMap = movHistoryRecords.stream()
//					.map(NameComponentsMovHistory::getComponents)
//					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

/* example that might work
https://www.geeksforgeeks.org/java/program-to-convert-list-to-map-in-java/
        // create multimap and store the value of list
        Map<Integer, List<String> >
            multimap = lt.stream()
                         .collect(Collectors.groupingBy(
                                       Student::getId,
                                       Collectors.mapping(
                                               Student::getName,
                                               Collectors.toList())));
*/

			dupsByComponent = movHistoryRecords.stream()
					.filter(r -> r.getNameComponentsMatching() != null) //only match linked records (is this correct??)
					.filter(r -> movHistoryComponentCountMap.get(r.getComponentsAsNormalizedString()) > 1)
					.collect(Collectors.toList());

			_log.debug("AlbumUtils.checkForRelativeDuplicateMovHistoryRecords: found " + dupsByComponent.size() + " duplicate mov history records BY COMPONENT");
		}

		{    //check for duplicates by albumName
			Map<String, Long> movHistoryAlbumNameCountMap = movHistoryRecords.stream()
					.map(NameComponentsMovHistory::getAlbumName)
					.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

			dupsByAlbumName = movHistoryRecords.stream()
					.filter(r -> r.getNameComponentsMatching() != null) //only match linked records (is this correct??)
					.filter(r -> movHistoryAlbumNameCountMap.get(r.getAlbumName()) > 1)
					.collect(Collectors.toList());

			_log.debug("AlbumUtils.checkForRelativeDuplicateMovHistoryRecords: found " + dupsByAlbumName.size() + " duplicate mov history records BY ALBUM NAME");
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean checkForMisMatchesBetweenMovHistoryAndDownloads(List<NameComponentsDownload> downloadRecords, List<NameComponentsMovHistory> movHistoryRecords) {
		List<String> listByDownloadComponents = downloadRecords.stream()
				.map(NameComponentsDownload::getComponentsAsNormalizedString)
				.collect(Collectors.toList());

		List<String> listByMovHistoryComponents = movHistoryRecords.stream()
				.map(NameComponentsMovHistory::getComponentsAsNormalizedString)
				.collect(Collectors.toList());

		Collection<String> setByDownloadComponents = new HashSet(listByDownloadComponents);
		Collection<String> setByMovHistoryComponents = new HashSet(listByMovHistoryComponents);

		Collection<String> missingFromDownload = VendoUtils.subtractCollections(setByMovHistoryComponents, setByDownloadComponents);
		Collection<String> missingFromMovHistory = VendoUtils.subtractCollections(setByDownloadComponents, setByMovHistoryComponents);

		_log.debug("AlbumUtils.checkForMisMatchesBetweenMovHistoryAndDownloads: found " + missingFromDownload.size() + " items missing from downloads");
		_log.debug("AlbumUtils.checkForMisMatchesBetweenMovHistoryAndDownloads: found " + missingFromMovHistory.size() + " items missing from mov history");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean checkForMisMatchesWithinMovHistoryForAlbumNames(List<NameComponentsMovHistory> movHistoryRecords) {
		List<NameComponentsMovHistory> listMisMatchesWithinMovHistory = movHistoryRecords.stream()
				.filter(r -> {
					return !r.getComponents().contains(r.getAlbumNameFirstName());
				})
				.collect(Collectors.toList());

		_log.debug("AlbumUtils.checkForMisMatchesWithinMovHistoryForAlbumNames: found " + listMisMatchesWithinMovHistory.size() + " mismatched items");

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	boolean linkTheRecords(List<NameComponentsDownload> downloadRecords, List<NameComponentsMovHistory> movHistoryRecords) {
		Map<String, NameComponentsMovHistory> movHistoryComponentsMap = movHistoryRecords.stream()
				.collect(Collectors.toMap(NameComponentsBase::getComponentsAsNormalizedString, k -> k, (oldValue, newValue) -> { //using merge function
//					_log.debug("AlbumUtils.linkTheRecords: mov history dups that need to be merged: " + oldValue.getAlbumName() + ", " + newValue.getAlbumName());
					return oldValue;
				}));

		Map<String, NameComponentsDownload> downloadComponentsMap = downloadRecords.stream()
				.collect(Collectors.toMap(NameComponentsBase::getComponentsAsNormalizedString, k -> k, (oldValue, newValue) -> { //using merge function
//					_log.debug("AlbumUtils.linkTheRecords: download dups that need to be merged: " + oldValue.getComponentsAsNormalizedString() + ", " + newValue.getComponentsAsNormalizedString());
					return oldValue;
				}));

		//link the two collections together - NOTE WE MERGED AWAY THE DUPS ABOVE and those dups won't get matched
		movHistoryComponentsMap.entrySet().forEach(i -> i.getValue().setNameComponentsMatching(downloadComponentsMap.get(i.getKey())));
		downloadComponentsMap.entrySet().forEach(i -> i.getValue().setNameComponentsMatching(movHistoryComponentsMap.get(i.getKey())));

		if (false) { //print the results
			movHistoryComponentsMap.entrySet().stream()
				.filter(i -> i.getValue().getNameComponentsMatching() == null)
				.forEach(i -> _log.debug("AlbumUtils.linkTheRecords: mov history: no matching download for: [" + i.getKey() + "] -> " + i.getValue()));

			downloadComponentsMap.entrySet().stream()
				.filter(i -> i.getValue().getNameComponentsMatching() == null)
				.forEach(i -> _log.debug("AlbumUtils.linkTheRecords: download: no matching mov history for: [" + i.getKey() + "] -> " + i.getValue()));
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class NameComponentsBase {
		///////////////////////////////////////////////////////////////////////////
		NameComponentsBase(String prefix, String middle) {
			this.prefix = prefix;
			components = Arrays.stream(middle.split("[_-]"))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(String::toLowerCase)
					.collect(Collectors.toList());

			if (components.contains("aneli")) {
				int bh = 1;
			}

			//some hacks - if the last two pairs of components are the same, remove the dups (i.e., [cat, dog, cat, dog] -> [cat, dog)
			int lastComponent = getComponents().size() - 1;
			if (lastComponent >= 3 && getComponents().get(lastComponent).equals(getComponents().get(lastComponent - 2)) && getComponents().get(lastComponent - 1).equals(getComponents().get(lastComponent - 3))) {
				getComponents().remove(lastComponent);
				getComponents().remove(lastComponent - 1);
			}

			//some hacks - if the last two components are the same, remove the dup (i.e., [cat, cat] -> [cat])
			lastComponent = getComponents().size() - 1;
			if (lastComponent >= 2 && getComponents().get(lastComponent - 1).equals(getComponents().get(lastComponent - 2))) {
				getComponents().remove(lastComponent - 1);
			}

			//some hacks - if the two components before the last are the same, remove the dup (i.e., [cat, cat, dog] -> [cat, dog])
			lastComponent = getComponents().size() - 1;
			if (lastComponent >= 1 && getComponents().get(lastComponent).equals(getComponents().get(lastComponent - 1))) {
				getComponents().remove(lastComponent);
			}
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
		protected String getComponentsAsNormalizedString() {
			return String.join("-", getComponents());
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

		protected final String prefix;
		protected final List<String> components;
		protected NameComponentsBase nameComponentsMatching;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class NameComponentsDownload extends NameComponentsBase implements Comparator<NameComponentsDownload> {
		///////////////////////////////////////////////////////////////////////////
		NameComponentsDownload() {
			this("uninitialized","uninitialized","uninitialized"); //ctor to be used only to initialize for Comparator
		}

		///////////////////////////////////////////////////////////////////////////
		NameComponentsDownload(String prefix, String middle, String dateString) {
			super(prefix, middle);
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
			if (nameComponentsMatching != null && nameComponentsMatching instanceof NameComponentsMovHistory) {
				matchingAlbumName = ((NameComponentsMovHistory) nameComponentsMatching).getAlbumName();
			}
			final StringBuffer sb = new StringBuffer("NameComponentsDownload{");
			sb.append("prefix='").append(prefix).append('\'');
			sb.append(", components=").append(components);
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
			this("uninitialized","uninitialized","uninitialized"); //ctor to be used only to initialize for Comparator
		}

		///////////////////////////////////////////////////////////////////////////
		NameComponentsMovHistory(String prefix, String middle, String albumName) {
			super(prefix, middle);
			this.albumName = albumName.toLowerCase();

			final Pattern firstNamePattern = Pattern.compile("([A-Z][a-z]+)([A-Z].*|)[0-9]+$");
			Matcher firstNameMatcher = firstNamePattern.matcher(albumName); //method param has mixed case, we need to operate on this param
			if (firstNameMatcher.find()) {
				albumNameFirstName = firstNameMatcher.group(1).toLowerCase();
			} else {
				albumNameFirstName = this.albumName;
			}
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getAlbumName() {
			return albumName;
		}

		///////////////////////////////////////////////////////////////////////////
		protected String getAlbumNameFirstName() {
			return albumNameFirstName;
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
			if (nameComponentsMatching != null && nameComponentsMatching instanceof NameComponentsDownload) {
				matchingDateString = ((NameComponentsDownload) nameComponentsMatching).getDateString();
			}
			final StringBuffer sb = new StringBuffer("NameComponentsMovHistory{");
			sb.append("prefix='").append(prefix).append('\'');
			sb.append(", components=").append(components);
			sb.append(", albumName='").append(albumName).append('\'');
			sb.append(", albumNameFirstName='").append(albumNameFirstName).append('\'');
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
			return getAlbumName().equals(that.getAlbumName()) && getAlbumNameFirstName().equals(that.getAlbumNameFirstName());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), getAlbumName(), getAlbumNameFirstName());
		}

		protected final String albumName;
		protected final String albumNameFirstName;
	}


	//members
//	private SqlSessionFactory _sqlSessionFactory = null;

	private static final String NL = System.getProperty ("line.separator");

	private static boolean _Debug = false;
	private static boolean _Test = false;

	private static final Logger _log = LogManager.getLogger ();
	private static final String _AppName = "AlbumUtils";
}
