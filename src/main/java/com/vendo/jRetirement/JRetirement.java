package com.vendo.jRetirement;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.vendo.vendoUtils.VFileList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vendo.jRetirement.FundsEnum.*;


public final class JRetirement {

	///////////////////////////////////////////////////////////////////////////
	public static void main(String[] args) {
		JRetirement app = new JRetirement();

		if (!app.processArgs(args)) {
			System.exit(1); //processArgs displays error
		}

		try {
			app.run();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs(String[] args) {
		String filenamePatternString = "Portfolio_Positions_*.csv";
		String sourceRootName = "C:\\Users\\david\\OneDrive\\Documents\\Fidelity\\";

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith("-") || arg.startsWith("/")) {
				arg = arg.substring(1);

				if (arg.equalsIgnoreCase("debug") || arg.equalsIgnoreCase("dbg")) {
					Debug = true;

				} else if (arg.equalsIgnoreCase("test") || arg.equalsIgnoreCase("tst")) {
					Test = true;

				} else if (arg.equalsIgnoreCase ("pattern") || arg.equalsIgnoreCase ("pat")) {
					try {
						filenamePatternString = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("source") || arg.equalsIgnoreCase ("src")) {
					try {
						sourceRootName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (inputFilenameOverride == null) {
					inputFilenameOverride = arg;

				} else {
					displayUsage("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		filenamePattern = Pattern.compile ("^" + filenamePatternString.replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling

		if (sourceRootName == null) {
			displayUsage ("Must specify source root folder", true);
		} else {
			sourceRootPath = FileSystems.getDefault().getPath (sourceRootName);
			if (!Files.exists (sourceRootPath)) {
				System.err.println("JRetirement.processArgs: error source path does not exist: " + sourceRootPath);
				return false;
			}
		}

		if (Debug || true) {
			System.out.println("JRetirement.processArgs: filenamePatternString: " + filenamePatternString + " => pattern: " + filenamePattern.toString());
			System.out.println("JRetirement.processArgs: sourceRootPath: " + sourceRootPath.toString ());
			System.out.println("JRetirement.processArgs: inputFilenameOverride: " + inputFilenameOverride);
			System.out.println();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage(String message, Boolean exit) {
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + AppName + " [/debug] [/test] TBD [/dest <dest dir>]";
		System.err.println("Error: " + msg + NL);

		if (exit) {
			System.exit(1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run() throws Exception {
		Path sourcePath;
		if (inputFilenameOverride != null) {
			if (inputFilenameOverride.matches("[A-Za-z]:.*")) { //check if we got a complete path
				sourcePath = sourceRootPath.getFileSystem().getPath(inputFilenameOverride);
			} else { //partial path or filename only
				sourcePath = FileSystems.getDefault().getPath(sourceRootPath.toString(), inputFilenameOverride);
			}

		} else {
			List<String> sourceFileList = new VFileList(sourceRootPath.toString(), Collections.singletonList(filenamePattern), false).getFileList (VFileList.ListMode.CompletePath);

			if (sourceFileList.isEmpty()) {
				System.err.println("JRetirement.run: no files found matching sourceRootName = \"" + sourceRootPath + "\", filenamePattern = \"" + filenamePattern);
				return false;
			}

			String newestFile = sourceFileList.stream().max(new PortfolioFilenameComparatorByDateReverse()).orElse("");
			sourcePath = FileSystems.getDefault().getPath(newestFile);
		}

		List<CsvFundsBean> records = processFile(sourcePath);

		final Instant dateDownloaded = parseDateDownloadedField();
		System.out.println ("Date Downloaded: " + dateTimeFormatter.format(dateDownloaded));

		int rowsPersisted = persistRecordsToDatabase(records, dateDownloaded);
		System.out.println ("Rows Persisted to Database: " + rowsPersisted);

		System.out.println(NL + "Roth Accounts -----------------------------------------------------------");
		printDistribution(records, rothAccounts, false, false);

		System.out.println(NL + "Traditional Accounts ----------------------------------------------------");
		printDistribution(records, traditionalAccounts, false, true);

		System.out.println(NL + "All Accounts ------------------------------------------------------------");
		printDistribution(records, allAccounts, true, true);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<CsvFundsBean> processFile(Path inputCsvFilePath) {
		List<CsvFundsBean> records;
		try {
			records = generateFilteredRecordList(inputCsvFilePath, true);

			//data integrity check - we should have 4 or more accounts in the file
			final int expectedAccounts = 4; //hardcoded
			Set<String> accounts = Objects.requireNonNull(records).stream()
					.map(CsvFundsBean::getAccountName)
					.collect(Collectors.toSet());
			if (accounts.size() < expectedAccounts) {
				System.out.println(NL + "Error: " + (expectedAccounts - accounts.size()) + " missing accounts. Found accounts: " + String.join(", ", accounts));
				return null;
			}

			if (false) { //print URLs
				System.out.println(NL + "URLs:");
				List<FundsEnum> funds = new ArrayList<>(Arrays.asList(values())).stream()
						.filter(f -> !pendingActivityString.equals(f.getSymbol()))
						.sorted ((f1, f2) -> f1.getSymbol().compareToIgnoreCase(f2.getSymbol()))
						.collect (Collectors.toList ());
				funds.forEach(f -> System.out.println("[" + f.getExpenseRatio() + "] " + f.getSymbol() + " -> " + f.getURL()));
			}

			System.out.println(NL + "Filtered records:");
			records.forEach(System.out::println);

			System.out.println(NL + "Read file: " + inputCsvFilePath);
			System.out.println("Records found: " + records.size());

			List <CsvFundsBean> pendingActivity = records.stream().filter(CsvFundsBean::isPendingActivity).collect(Collectors.toList());
			if (!pendingActivity.isEmpty()) {
				System.out.println(NL + "Pending Activity:");
				pendingActivity.forEach(System.out::println);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean printDistribution(List<CsvFundsBean> records, Predicate<CsvFundsBean> predicate, boolean includeBreakOut, boolean includeWithdrawalAmounts) {
		try {
			double totalBond = 0;
			double totalCash = 0;
			double totalHealth = 0;
			double totalActive = 0;
			double totalIndex = 0;
			double totalRoth = 0;
			double totalAllFunds = 0;

			Map<String, Double> balancesByGroupingMap = new HashMap<>();

			for (CsvFundsBean record : records) {
				if (!predicate.test(record)) {
					continue;
				}

				FundOwner fundOwner = determineFundOwner(record.getAccountNumber());

				FundsEnum fund = getValue(record.getSymbol());
//				String groupBy = fund.getFundFamily() + "." + fund.getStyle();
//				String groupBy = fund.getFundFamily() + "." + fund.getCategory();
//				String groupBy = fund.getFundFamily() + "." + fund.getFundType() + "." + fund.getCategory();
				String groupBy = ""
						+ "[" + fund.getExpenseRatio() + "] "
						+ "[" + fundOwner + "] "
						+ StringUtils.rightPad(fund.getSymbol(), 5, ' ')
						+ " => " + fund.getFundFamily()
//						+ "." + fundOwner
						+ "." + fund.getFundType()
						+ "." + fund.getManagementStyle()
						+ "." + fund.getCategory();

				Double balance = balancesByGroupingMap.computeIfAbsent(groupBy, k -> 0.); //if key not present, balance is 0.

				balance += record.getCurrentValue();
				balancesByGroupingMap.put(groupBy, balance);

				totalAllFunds += record.getCurrentValue();

				if (fund.isBond()) {
					totalBond += record.getCurrentValue();
				}

				if (fund.isCash()) {
					totalCash += record.getCurrentValue();
				}

				if (fund.isHealth()) {
					totalHealth += record.getCurrentValue();
				}

				if (fund.isActive()) {
					totalActive += record.getCurrentValue();
				}

				if (fund.isIndex()) {
					totalIndex += record.getCurrentValue();
				}

				if (record.isRoth()) {
					totalRoth += record.getCurrentValue();
				}
			}

			List<FundResult> results = new ArrayList<>();
			for (Map.Entry<String, Double> entry : balancesByGroupingMap.entrySet().stream()
					.sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
				String grouping = entry.getKey();
				double total = entry.getValue();
				double percent = 100. * total / totalAllFunds;
				results.add(new FundResult(grouping, total, percent,
							grouping.contains(pendingActivityString) ? " <<< " + pendingActivityString : ""));
			}
			results.add(new FundResult("Total", totalAllFunds, 100.));

			if (includeWithdrawalAmounts) {
				results.add(BlankLine);
				List<Integer> percents = Arrays.asList(2, 3, 4);
				for (Integer percent : percents) {
					double withdrawalPercent = percent * totalAllFunds / 100;
					results.add(new FundResult("Annual withdrawal at " + percent + " percent", withdrawalPercent, percent, " <<< divide by 2 for 2024"));
				}
			}

			int longestLabel = FundResult.getMaxLabelLength(results);
			int longestTotal = FundResult.getMaxTotalLength(results);
			int longestPercent = FundResult.getMaxPercentLength(results);

			for (FundResult result : results) {
				System.out.println(FundResult.generateString(result, longestLabel, longestTotal, longestPercent));
			}

			if (includeBreakOut) {
				List<FundResult> totals = new ArrayList<>();
				double percentBond = 100 * totalBond / totalAllFunds;
				double percentCash = 100 * totalCash / totalAllFunds;
				double percentHealth = 100 * totalHealth / totalAllFunds;
				double percentActive = 100 * totalActive / totalAllFunds;
				double percentIndex = 100 * totalIndex / totalAllFunds;
				double percentRoth = 100 * totalRoth / totalAllFunds;

				totals.add(new FundResult("Total Bond", totalBond, percentBond));
				totals.add(new FundResult("Total Cash", totalCash, percentCash));
				totals.add(new FundResult("Total Health", totalHealth, percentHealth));
				totals.add(new FundResult("Total Active", totalActive, percentActive));
				totals.add(new FundResult("Total Index", totalIndex, percentIndex));
				totals.add(new FundResult("Total Roth", totalRoth, percentRoth));

				longestLabel = FundResult.getMaxLabelLength(totals);
				longestTotal = FundResult.getMaxTotalLength(totals);
				longestPercent = FundResult.getMaxPercentLength(totals);

				System.out.println();
				for (FundResult total : totals) {
					System.out.println(FundResult.generateString(total, longestLabel, longestTotal, longestPercent));
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	static class FundResult {
		FundResult(String label, double total, double percent) {
			this(label, total, percent, "");
		}
		FundResult(String label, double total, double percent, String specialNote) {
			this.label = label;
			this.total = total;
			this.percent = percent;
			this.specialNote = specialNote;
		}

		///////////////////////////////////////////////////////////////////////////
		public static int getMaxLabelLength(List<FundResult> results) {
			return results.stream().map(r -> r.label.length()).max(Integer::compare).orElse(defaultFieldLength);
		}

		///////////////////////////////////////////////////////////////////////////
		public static int getMaxTotalLength(List<FundResult> results) {
			return results.stream().map(r -> decimalFormat2.format(r.total).length() + lengthOfSymbol).max(Integer::compare).orElse(defaultFieldLength);
		}

		///////////////////////////////////////////////////////////////////////////
		public static int getMaxPercentLength(List<FundResult> results) {
			return results.stream().map(r -> decimalFormat1.format(r.percent).length() + lengthOfSymbol).max(Integer::compare).orElse(defaultFieldLength);
		}

		///////////////////////////////////////////////////////////////////////////
		public static String generateString(FundResult result, int longestLabel, int longestTotal, int longestPercent) {
			if (BlankLine.equals(result)) {
				return "";
			} else {
				return  String.format("%-" + longestLabel + "s", result.label) + space2 +
						(Test ? "" :
						String.format("%" + longestTotal + "s", "$" + decimalFormat2.format(result.total)) + space2) +
						String.format("%" + longestPercent + "s", decimalFormat1.format(result.percent) + "%") +
						result.specialNote;
			}
		}

		final String label;
		final double total;
		final double percent;
		final String specialNote;

		private static final int lengthOfSymbol = 1; //hardcoded; i.e., "$" or "%"
		private static final int defaultFieldLength = 20; //hardcoded
		private static final String space2 = "  "; //hardcoded
	}

	///////////////////////////////////////////////////////////////////////////
	private List<CsvFundsBean> generateFilteredRecordList(Path inputCsvFilePath, final boolean printSkippedRecords) {
		final double minimumValueToBeIncluded = 100.; //hardcoded - skip funds that have less than this amount
		final List<String> accountsToSkip = new ArrayList<>(Arrays.asList("Fixed Annuity", "Individual - 529 - TOD"));
		final List<String> skippedRecords = new ArrayList<>();

		List<CsvFundsBean> records;

		try {
			records = csvBeanBuilder(inputCsvFilePath, CsvFundsBean.class).stream()
					.filter(r -> {
						boolean keepRecord = true;
						if (accountsToSkip.contains(r.getAccountName())) {
							skippedRecords.add("skipped record (account): " + r);
							keepRecord = false;
						} else if (Math.abs(r.getCurrentValue()) < minimumValueToBeIncluded) {
							skippedRecords.add("skipped record (amount): " + r);
							keepRecord = false;
						}
						return keepRecord;
					})
					.collect(Collectors.toList());

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		if (printSkippedRecords && !skippedRecords.isEmpty()) {
			System.out.println("JRetirement.generateFilteredRecordList:");
			skippedRecords.stream().sorted().forEach(System.out::println);
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<CsvFundsBean> csvBeanBuilder(Path path, Class<? extends CsvFundsBean> clazz) {
		List<CsvFundsBean> records;

		try {
			Reader reader = getReaderFromStringList(readAllValidLines(path, true));
			CsvToBean<CsvFundsBean> cb = new CsvToBeanBuilder<CsvFundsBean>(reader)
					.withType(clazz)
					.build();
			records = cb.parse();

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public Reader getReaderFromStringList(List<String> list) {
		return new BufferedReader(new StringReader(String.join("\n", list)));
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> readAllValidLines(Path filePath, final boolean printSkippedLines) throws Exception {
		final String commentDelimiter = "#";
		final List<String> skippedLines = new ArrayList<>();

		List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8).stream()
			.map(l -> {
				if (!l.isEmpty() && l.charAt(0) > 'Z') { //hack - remove unicode (?) garbage at beginning of file that prevents successful parsing of first column
					l = l.substring(1);
				}
				return l;
			})
			.map(l -> l.contains(pendingActivityString) ? repairPendingActivityLine(l) : l)
			.filter(l -> {
				boolean keepLine = true;
				if (l.contains(dateDownloadedString)) {
					dateDownloadedList.add(l);
					keepLine = false;
				} else if (l.startsWith(commentDelimiter)) {
					skippedLines.add("skipped line (comment): " + l);
					keepLine = false;
				} else if (countCommas(l) < csvExpectedCommas) {
					if (!StringUtils.isBlank(l)) {
						skippedLines.add("skipped line (not data): " + l);
					}
					keepLine = false;
				}
				return keepLine;
			})
			.collect(Collectors.toList());

		if (printSkippedLines && !skippedLines.isEmpty()) {
			final int maxCharsToPrint = 80; //hardcoded

			System.out.println("JRetirement.readAllValidLines:");
			skippedLines.stream().sorted().forEach(l -> {
				int charsToPrint = Math.min(l.length(), maxCharsToPrint);
				boolean truncated = l.length() > charsToPrint;
				System.out.println(l.substring(0, charsToPrint) + (truncated ? "*" : ""));
			});
		}

		return lines;
	}

	///////////////////////////////////////////////////////////////////////////
	private static FundOwner determineFundOwner(String accountNumber) {
		FundOwner fundOwner;

		switch (accountNumber) {
			default:
				fundOwner = FundOwner.Other;
				break;

			case "239929111":
			case "242813142":
			case "85918":
				fundOwner = FundOwner.Primary;
				break;

			case "239971093":
			case "239971099":
				fundOwner = FundOwner.Secondary;
				break;
		}

		return fundOwner;
	}

	///////////////////////////////////////////////////////////////////////////
	private static int countCommas(String string) {
		return string.replaceAll("[^,]","").length();
	}

	///////////////////////////////////////////////////////////////////////////
	//for some reason, these lines have fewer commas than the rest of the data lines, and the value needs to be shifted one column to the right
	private String repairPendingActivityLine(String line) {
		if (line.contains(pendingActivityString)) {
			line = line.replaceAll(pendingActivityString, pendingActivityString + ","); //add comma to shift columns right
			int missingCommas = csvExpectedCommas - countCommas(line);
			if (missingCommas > 0) {
				line += StringUtils.rightPad("", missingCommas, ','); //pad with missing commas so parser can parse
			}
		}
		return line;
	}

/* original - keep for now
	///////////////////////////////////////////////////////////////////////////
	private long parseDateDownloaded() throws Exception {
		final Pattern datePattern = Pattern.compile(".*(\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2} [A-Z]{2}).*");
		final FastDateFormat dateFormat = FastDateFormat.getInstance("MM/dd/yyyy hh:mm aa"); // Note SimpleDateFormat is not thread safe

		assert(dateDownloadedList.size() == 1); //must be only one

		//go ahead and throw an exception if any of this fails
		Matcher dateMatcher = datePattern.matcher(dateDownloadedList.get(0));
		dateMatcher.find();
		String dateString = dateMatcher.group(1);
		Date date = dateFormat.parse(dateString);
		return date.getTime ();
	}
*/

	///////////////////////////////////////////////////////////////////////////
	private Instant parseDateDownloadedField() throws Exception {
		final Pattern datePattern = Pattern.compile(".*(\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2} [A-Z]{2}).*");
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a");

		assert(dateDownloadedList.size() == 1); //must be only one

		//go ahead and throw an exception if any of this fails
		Matcher dateMatcher = datePattern.matcher(dateDownloadedList.get(0));
		dateMatcher.find();
		String dateString = dateMatcher.group(1);
		LocalDateTime localDateTime = LocalDateTime.parse(dateString, dateTimeFormatter);
		Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
		return instant;
	}

	///////////////////////////////////////////////////////////////////////////
	//to sort filenames by embedded date (newest first), with this date format in file name: path1\path2\Portfolio_Positions_Feb-24-2024.csv
	private static class PortfolioFilenameComparatorByDateReverse implements Comparator<String>
	{
		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compare(String filename1, String filename2) {
			long time1 = 0;
			long time2 = 0;
			try {
				time1 = parseDateTimeFromFilename(filename1);
				time2 = parseDateTimeFromFilename(filename2);

				return Long.compare(time1, time2); //sort newest first
			} catch(Exception ex) {
				System.err.println("PortfolioFilenameComparatorByDateReverse.compare: failed to parse date from filename: <" +
						(time1 == 0 ? filename1 : filename2) + ">");
			}
			return filename1.compareToIgnoreCase(filename2); //fallback if date parsing fails
		}

		///////////////////////////////////////////////////////////////////////////
		private long parseDateTimeFromFilename(String filename) throws Exception {
			if (filenameToDateMap.get(filename) != null) {
				return filenameToDateMap.get(filename);
			}

			//go ahead and throw an exception if any of this fails
			Matcher dateMatcher = datePattern.matcher(filename);
			dateMatcher.find();
			String dateString = dateMatcher.group(1);
			Date date = dateFormat.parse(dateString);
			long time = date.getTime();
			filenameToDateMap.put(filename, time);

			return time;
		}

		final Map<String, Long> filenameToDateMap = new HashMap<>();
		final Pattern datePattern = Pattern.compile("Portfolio.*_([A-Z][a-z]{2}-\\d{2}-\\d{4}).*");
		final FastDateFormat dateFormat = FastDateFormat.getInstance ("MMM-dd-yyyy"); // Note SimpleDateFormat is not thread safe
	}

	///////////////////////////////////////////////////////////////////////////
	private int persistRecordsToDatabase(List<CsvFundsBean> records, Instant dateDownloaded) throws Exception {
		int rowsPersisted = 0;

		try (Connection connection = connectDatabase()) {
			if (records != null) {
				for (CsvFundsBean record : records) {
					if (persistRecordToDatabase(connection, record, dateDownloaded)) {
						++rowsPersisted;
					}
				}
			}
		}

		return rowsPersisted;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean persistRecordToDatabase(Connection connection, CsvFundsBean record, Instant dateDownloaded)
	{
		final String sql = "insert into retirement (downloaded_timestamp, account_number, account_name, symbol, description, value, cost_basis)" + NL +
						   " values (?, ?, ?, ?, ?, ?, ?)";

		final java.sql.Timestamp timestamp = java.sql.Timestamp.from(dateDownloaded);

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			int index = 1;
			stmt.setTimestamp(index++, timestamp);
			stmt.setString(index++, record.getAccountNumber());
			stmt.setString(index++, record.getAccountName());
			stmt.setString(index++, record.getSymbol());
			stmt.setString(index++, record.getDescription());
			stmt.setDouble(index++, record.getCurrentValue());
			stmt.setDouble(index++, record.getCostBasisTotal());

			stmt.executeUpdate ();

		} catch (SQLIntegrityConstraintViolationException ex) {
			if (!ex.getMessage().matches("Duplicate entry.*PRIMARY.*")) { //we expect to get duplicate entries because we aren't checking before persisting (which is a TODO)
				System.out.println("persistRecordToDatabase: ignoring duplicate record: " + record);
			}
			return false;

		} catch (Exception ex) {
			System.err.println("persistRecordToDatabase: error from Connection.prepareStatement(" + sql + ") or PreparedStatement.executeUpdate");
			System.err.println(ex);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private Connection connectDatabase () throws Exception
	{
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.cj.jdbc.Driver";
		final String dbUrl = "jdbc:mysql://localhost/retirement";
		final String dbUser = "root";
		final String dbPass = "root";

		Class.forName (jdbcDriver);

		Connection dbConnection = DriverManager.getConnection (dbUrl, dbUser, dbPass);

		return dbConnection;
	}


	//private members
	private Path sourceRootPath = null;
	private Pattern filenamePattern = null;
	private String inputFilenameOverride = null; //to specify a specific input file

	private static final int csvExpectedCommas = 15;
	private static final String dateDownloadedString = "Date downloaded";

	private static final Predicate<CsvFundsBean> rothAccounts = CsvFundsBean::isRoth;
	private static final Predicate<CsvFundsBean> traditionalAccounts = r -> !r.isRoth();
	private static final Predicate<CsvFundsBean> allAccounts = r -> true;

	private final List<String> dateDownloadedList = new ArrayList<>(); //List in case there is more than one matching record in the file

	private static final FundResult BlankLine = new FundResult("blank line", 0, 0);

	private static final DecimalFormat decimalFormat1 = new DecimalFormat ("###,##0.0");
	private static final DecimalFormat decimalFormat2 = new DecimalFormat ("###,##0.00");

	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss").withZone(ZoneId.systemDefault());

	//global members
	public static String pendingActivityString = "Pending Activity";

	public static boolean Debug = false;
	public static boolean Test = false;

	public static final String AppName = "JRetirement";
	public static final String NL = System.getProperty ("line.separator");
//	private static final Logger _log = LogManager.getLogger ();
}
