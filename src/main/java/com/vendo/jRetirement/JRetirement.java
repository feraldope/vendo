package com.vendo.jRetirement;


/* This is how an IRA distribution is presented
Accounts_History_2024-Q4.csv
 10/01/2024,"Rollover IRA" 242813142," STATE TAX W/H MA STAT WTH (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-2000,
 10/01/2024,"Rollover IRA" 242813142," FED TAX W/H FEDERAL TAX WITHHELD (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-4000,
 10/01/2024,"Rollover IRA" 242813142," NORMAL DISTR PARTIAL VS X64-835730-1 CASH (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-34000,
* */

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.vendo.vendoUtils.VFileList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vendo.jRetirement.FundsEnum.FundOwner;
import static com.vendo.jRetirement.FundsEnum.getValue;


public /*final*/ class JRetirement {

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
	protected Boolean processArgs(String[] args) {
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

				} else if (arg.equalsIgnoreCase("deleteDuplicateRecords") || arg.equalsIgnoreCase("dedup")) {
					deleteDuplicateRecords = true;

				} else if (arg.equalsIgnoreCase("generatePlotFile") || arg.equalsIgnoreCase("plot")) {
					generatePlotFile = true;

				} else if (arg.equalsIgnoreCase("printHistoricalData") || arg.equalsIgnoreCase("hist")) {
					printHistoricalData = true;

				} else if (arg.equalsIgnoreCase("printTaxes") || arg.equalsIgnoreCase("taxes")) {
					printTaxes = true;

				} else if (arg.equalsIgnoreCase("printUrls") || arg.equalsIgnoreCase("urls")) {
					printUrls = true;

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
	protected void displayUsage(String message, Boolean exit) {
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
	protected boolean run() throws Exception {
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
		assert records != null && !records.isEmpty();

		final Instant dateDownloaded = parseDateDownloadedField(dateDownloadedList);
		records.forEach(r -> r.setDateDownloaded(dateDownloaded));
		System.out.println ("Date Downloaded: " + dateTimeFormatter.format(dateDownloaded));

		int rowsMissingFromDb = 0;
		{ //TODO clean this up
			List<CsvFundsBean> matchingRecordsFromDb = queryRecordsFromDatabase(dateDownloaded);
			System.out.println("Existing rows read from database: " + matchingRecordsFromDb.size());

			Set<CsvFundsBean> recordsMissingFromFile = new HashSet<>(matchingRecordsFromDb);
			recordsMissingFromFile.removeAll(new HashSet<>(records));

			Set<CsvFundsBean> recordsMissingFromDb = new HashSet<>(records);
			recordsMissingFromDb.removeAll(new HashSet<>(matchingRecordsFromDb));

			rowsMissingFromDb = recordsMissingFromDb.size();

			System.out.println("recordsMissingFromFile.size() = " + recordsMissingFromFile.size());
			System.out.println("recordsMissingFromDb.size() = " + recordsMissingFromDb.size());
		}

		int rowsPersisted = 0;
		if (rowsMissingFromDb > 0) {
			rowsPersisted = persistRecordsToDatabase(records);
		}
		System.out.println("New rows persisted to database: " + rowsPersisted);

		if (deleteDuplicateRecords) {
			int duplicateRecordsDeleted = deleteDuplicateRecords();
			System.out.println("Duplicate records deleted: " + duplicateRecordsDeleted);
		}

		System.out.println(NL + "Roth Accounts -----------------------------------------------------------");
		printDistribution(records, rothAccounts, true, false);

		System.out.println(NL + "Traditional Accounts ----------------------------------------------------");
		printDistribution(records, traditionalAccounts, true, true);

		System.out.println(NL + "All Accounts ------------------------------------------------------------");
		printDistribution(records, allAccounts, true, true);

		if (printTaxes) {
			printTaxes();
		}

		if (printHistoricalData || generatePlotFile) {
			List<AggregateRecord> aggregateRecords = queryAggregateRecordsFromDatabase();

			if (printHistoricalData) {
				printHistoricalData(aggregateRecords);
			}

			if (generatePlotFile) {
				generatePlotFile(aggregateRecords);

				if (false) {
					String command = PlotExecutable + " " + PlotFileName;
					try {
						Runtime.getRuntime().exec(command);
					} catch (Exception ex) {
						String msg = "Error executing \"" + command + "\"";
						System.out.println(msg);
					}
				}
			}
		}

		System.out.println();

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<CsvFundsBean> processFile(Path inputCsvFilePath) {
		List<CsvFundsBean> records;
		try {
			records = generateFilteredRecordList(inputCsvFilePath, true);
			assert records != null && !records.isEmpty();

			//data integrity check - we should have 4 or more accounts in the file
			final int expectedAccounts = 4; //hardcoded
			Set<String> accounts = Objects.requireNonNull(records).stream()
					.map(CsvFundsBean::getAccountName)
					.collect(Collectors.toSet());
			if (accounts.size() < expectedAccounts) {
				System.out.println(NL + "Error: " + (expectedAccounts - accounts.size()) + " missing accounts. Found accounts: " + String.join(", ", accounts));
				return null;
			}

			if (printUrls) {
				printUrls();
			}

			System.out.println(NL + "Filtered records (" + records.size() + "):");
			records.forEach(System.out::println);

			System.out.println(NL + "Read file: " + inputCsvFilePath);
			System.out.println("Records found: " + records.size());

			List <CsvFundsBean> pendingActivity = records.stream().filter(CsvFundsBean::isPendingActivity).collect(Collectors.toList());
			if (!pendingActivity.isEmpty()) {
				System.out.println(NL + "Pending Activity:");
				pendingActivity.forEach(System.out::println);
				System.out.println("");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	protected boolean printDistribution(List<CsvFundsBean> records, Predicate<CsvFundsBean> predicate, boolean includeBreakOut, boolean includeWithdrawalAmounts) {
		try {
			double totalBond = 0;
			double totalCash = 0;
			double totalEquity = 0;
			double totalHealth = 0;
			double totalActive = 0;
			double totalIndex = 0;
			double totalRoth = 0;
			double totalPendingActivity = 0;
			double totalAllFunds = 0;
			boolean foundPendingActivity = false;

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

//				String groupBy = fund.getFundTheme().toString();

				String groupBy = ""
//						+ "[" + fund.getExpenseRatio() + "] "
//						+ "[" + fundOwner + "] "
///						+ StringUtils.rightPad(fund.getSymbolForGrouping(), 5, ' ')
//						+ " => " + fund.getFundFamily()
///						+ " => " + fund.getDescription()
//						+ "." + fundOwner
///						+ "." + fund.getFundType()
//						+ "." + fund.getManagementStyle()
						+ "." + fund.getCategory();

/*
				groupBy = ""
//						+ "[" + fund.getExpenseRatio() + "] "
//						+ "[" + fundOwner + "] "
//						+ StringUtils.rightPad(fund.getSymbolForGrouping(), 5, ' ')
//						+ " => " + fund.getFundFamily()
//						+ "." + fundOwner
//						+ "." + fund.getFundType()
//						+ "." + fund.getManagementStyle()
						+ "." + fund.getCategory();
*/

				Double balance = balancesByGroupingMap.computeIfAbsent(groupBy, k -> 0.); //if key not present, balance is 0.

				balance += record.getCurrentValue();
				balancesByGroupingMap.put(groupBy, balance);

				totalAllFunds += record.getCurrentValue();

				if (fund.isBond()) {
					totalBond += record.getCurrentValue();
				} else if (fund.isCash()) {
					totalCash += record.getCurrentValue();
				} else {
					totalEquity += record.getCurrentValue();
				}

				if (fund.isHealth()) {
					totalHealth += record.getCurrentValue();
				}

				if (fund.isActive()) {
					totalActive += record.getCurrentValue();
				} else if (fund.isIndex()) {
					totalIndex += record.getCurrentValue();
				}

				if (record.isRoth()) {
					totalRoth += record.getCurrentValue();
				}

				if (record.isPendingActivity()) {
					totalPendingActivity += record.getCurrentValue();
				}
			}

			totalCash += totalPendingActivity; //pending activity should be negative

			List<FundResult> results = new ArrayList<>();
			for (Map.Entry<String, Double> entry : balancesByGroupingMap.entrySet().stream()
					.sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
				String grouping = entry.getKey();
				double total = entry.getValue();
				double percent = 100. * total / totalAllFunds;
				results.add(new FundResult(grouping, total, percent, grouping.contains(PendingActivityString) ? " <<< " + PendingActivityString : ""));

//TODO - this does not work for all values of groupBy - specifically those that do not include the symbol
				if (grouping.contains(PendingActivityString)) {
					foundPendingActivity = true;
				}
			}

			//sort results
			results = results.stream().sorted(Comparator.comparing(r -> r.total)).collect(Collectors.toList());

			results.add(new FundResult("Total", totalAllFunds, 100.));

			if (includeWithdrawalAmounts) {
				results.add(BlankLine);
				List<Integer> percents = Arrays.asList(2, 3, 4);
				for (Integer percent : percents) {
					double withdrawalPercent = percent * totalAllFunds / 100;
					results.add(new FundResult("Annual withdrawal at " + percent + " percent", withdrawalPercent, percent /*, " <<< divide by 2 for 2024"*/));
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
				double percentEquity = 100 * totalEquity / totalAllFunds;
				double percentHealth = 100 * totalHealth / totalAllFunds;
				double percentActive = 100 * totalActive / totalAllFunds;
				double percentIndex = 100 * totalIndex / totalAllFunds;
				double percentRoth = 100 * totalRoth / totalAllFunds;

				totals.add(new FundResult("Total Bond", totalBond, percentBond));
				totals.add(new FundResult("Total Cash/CDs", totalCash, percentCash, foundPendingActivity ? " <<< adjusted for " + PendingActivityString : ""));
				totals.add(new FundResult("Total Equity", totalEquity, percentEquity));
				totals.add(new FundResult("Total Health", totalHealth, percentHealth));
				totals.add(new FundResult("Total Active", totalActive, percentActive));
				totals.add(new FundResult("Total Index", totalIndex, percentIndex));
				if (percentRoth != 0. && percentRoth != 100.) { //hack
					totals.add(new FundResult("Total Roth", totalRoth, percentRoth));
				}

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
			return results.stream().map(r -> decimalFormat0.format(r.total).length() + lengthOfSymbol).max(Integer::compare).orElse(defaultFieldLength);
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
						(Test ? ""
						: String.format("%" + longestTotal + "s", "$" + decimalFormat0.format(result.total)) + space2) +
						String.format("%" + longestPercent + "s", decimalFormat1.format(result.percent) + "%") +
						result.specialNote;
			}
		}

		final String label;
		final Double total;
		final double percent;
		final String specialNote;

		protected static final int lengthOfSymbol = 1; //hardcoded; i.e., "$" or "%"
		protected static final int defaultFieldLength = 20; //hardcoded
		protected static final String space2 = "  "; //hardcoded
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<CsvFundsBean> generateFilteredRecordList(Path inputCsvFilePath, final boolean printSkippedRecords) {
		final double minimumValueToBeIncluded = 500.; //hardcoded - skip funds that have less than this amount
//		final List<String> accountsToSkip = new ArrayList<>(Arrays.asList("Fixed Annuity", "Individual - 529 - TOD", "Individual - TOD"));
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
			Reader reader = getReaderForStringList(readAllValidLines(path, true));
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
	public Reader getReaderForStringList(List<String> list) {
		return new BufferedReader(new StringReader(String.join("\n", list)));
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> readAllValidLines(Path filePath, final boolean printSkippedLines) throws Exception {
		final String commentDelimiter = "#";
		final List<String> skippedLines = new ArrayList<>();

		AtomicInteger currentLineNumber = new AtomicInteger(0);
		List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8).stream()
			.map(l -> currentLineNumber.incrementAndGet() == 1 ? repairFileHeaderLine(l) : l)
			.map(l -> l.contains(PendingActivityString) ? repairPendingActivityLine(l) : l)
			.filter(l -> {
				boolean keepLine = true;
				if (l.contains(DateDownloadedString)) {
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
			final int maxCharsToPrint = 100; //hardcoded

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
	protected static FundOwner determineFundOwner(String accountNumber) {
		FundOwner fundOwner = FundOwner.unknown;

		if (accountNumber.matches("23\\d+11") || accountNumber.matches("24\\d+42") || accountNumber.matches("8\\d+8") || accountNumber.matches("X\\d+0")) {
			fundOwner = FundOwner.dr;
		} else if (accountNumber.matches("23\\d+9[39]")) {
			fundOwner = FundOwner.mr;
		}

		return fundOwner;
	}

	///////////////////////////////////////////////////////////////////////////
	protected static int countCommas(String string) {
		return string.replaceAll("[^,]","").length();
	}

	///////////////////////////////////////////////////////////////////////////
	//remove unicode character at beginning of CSV file that prevents successful parsing of first column
	protected String repairFileHeaderLine(String line) {
		final int ZWNBSP = '\uFEFF'; //ZWNBSP (unicode zero-width no-break space) a.k.a. BOM (byte-order-mark) character
		if (!line.isEmpty() && line.charAt(0) == ZWNBSP) {
			line = line.substring(1);
		}
		return line;
	}

	///////////////////////////////////////////////////////////////////////////
	//for some reason, these lines have fewer commas than the rest of the data lines, and the value needs to be shifted one column to the right
	protected String repairPendingActivityLine(String line) {
		if (line.contains(PendingActivityString)) {
			line = line.replaceAll(PendingActivityString, PendingActivityString + ","); //add comma to shift columns right
			int missingCommas = csvExpectedCommas - countCommas(line);
			if (missingCommas > 0) {
				line += StringUtils.rightPad("", missingCommas, ','); //pad with missing commas so parser can parse
			}
		}
		return line;
	}

/* original - keep for now
	///////////////////////////////////////////////////////////////////////////
	protected long parseDateDownloaded() throws Exception {
		final Pattern datePattern = Pattern.compile(".*(\\d{2}/\\d{2}/\\d{4} \\d{1,2}:\\d{2} [A-Z]{2}).*");
		final FastDateFormat dateFormat = FastDateFormat.getInstance("MM/dd/yyyy hh:mm aa"); // Note SimpleDateFormat is not thread safe

		assert dateDownloadedList.size() == 1; //must be only one

		//go ahead and throw an exception if any of this fails
		Matcher dateMatcher = datePattern.matcher(dateDownloadedList.get(0));
		dateMatcher.find();
		String dateString = dateMatcher.group(1);
		Date date = dateFormat.parse(dateString);
		return date.getTime ();
	}
*/

	///////////////////////////////////////////////////////////////////////////
	protected Instant parseDateDownloadedField(List<String> dateDownloadedList) {
		final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.parseLenient()
				.appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a zz"))
				.appendOptional(DateTimeFormatter.ofPattern("MMM-dd-yyyy h:mm a zz"))
				.toFormatter();

		//go ahead and throw an exception if any of this fails
		assert dateDownloadedList.size() == 1; //must be only one
		String dateString = dateDownloadedList.get(0)
				.replaceAll(DateDownloadedString, "")
				.replaceAll("[\".]", "") //remove quotes and any errant periods
				.trim();

		LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);
		Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

		return instant;
	}

	///////////////////////////////////////////////////////////////////////////
	protected void generatePlotFile(List<AggregateRecord> records) throws Exception {
		assert records != null && !records.isEmpty();

		final int yMin = 2000000; //TODO - calculate!
		final int yMax = 2500000; //TODO - calculate!

		String timestamp = dateTimeFormatter.format (Instant.now());

		List<String> headers = Arrays.asList(
			"# do not edit: file auto generated by JRetirement on: " + timestamp,
			"",
			"[PlotList]",
			"Total=1",
			"",
			"[PlotFileOptions]",
			"xTransform=linear",
//			"xAxisDataType=monthOfDecade", //TODO
			"xAxisDataType=normal",
			"xAxisSkipTicks=30", //30 days ~ one month
			"xAxisLabel=Date",

			"yTransform=linear",
			"yAxisDataType=normal",
			"yAxisLabel=($)",
			"yMin=" + yMin,	//hardcoded
			"yMax=" + yMax  //hardcoded
		);

		try (PrintWriter out = new PrintWriter(Files.newOutputStream(new File(PlotFileName).toPath()))) {
			headers.forEach(out::println);

			out.println();
			out.println("[Total]");

			final int maxItemsToPrint = 180; //about 6 months
			records.stream()
				   .skip(Math.max(0, records.size() - maxItemsToPrint))
				   .forEach(r -> out.println("" + r.index + "=" + r.totalValue));

		} catch (Exception ex) {
			System.out.println("generatePlotFile: error writing to output file \"" + PlotFileName + "\"");
			System.out.println(ex.getMessage()); //print exception, but no stack trace
			return;
		}

		System.out.println(NL + "generatePlotFile: historical data written to plot file \"" + PlotFileName + "\"");
	}

	///////////////////////////////////////////////////////////////////////////
	protected void printHistoricalData(List<AggregateRecord> records) throws Exception {
		assert records != null && !records.isEmpty();

		{
			final int lastN = 10;
			List<AggregateRecord> lastNRecords = records.subList(Math.max(records.size() - lastN, 0), records.size());

			System.out.println(NL + "Last " + lastN + " historical records (" + lastN + " of " + records.size() + "):");
			lastNRecords.forEach(System.out::println);
		}

		{
			final int topN = 10;
			List<AggregateRecord> topNRecords = records.stream().sorted(new AggregateRecord().reversed()).limit(topN).collect(Collectors.toList());

			System.out.println(NL + "Top " + topN + " historical records (" + topN + " of " + records.size() + "):");
			topNRecords.forEach(System.out::println);
		}

//		AggregateRecord maxRecord = records.stream().max(new AggregateRecord()).orElse(null);
//		if (maxRecord != null) {
//			System.out.println(NL + "Max record:");
//			System.out.println(maxRecord);
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	protected void printTaxes() throws Exception {
//		final List<TaxBracket> federalTaxBracket2023 = Arrays.asList( //Married Filing Jointly
//				//https://www.irs.gov/filing/federal-income-tax-rates-and-brackets
//				new TaxBracket(10, 0, 22000),
//				new TaxBracket(12, 22000, 89450),
//				new TaxBracket(22, 89450, 190750),
//				new TaxBracket(24, 190750, 364200)
//		);
//		final int federalStandardDeduction2023 = 27700; //Married Filing Jointly
		final List<TaxBracket> federalTaxBracket2024 = Arrays.asList( //Married Filing Jointly
				//https://www.irs.gov/newsroom/irs-provides-tax-inflation-adjustments-for-tax-year-2024
				new TaxBracket(10,      0,  23200),
				new TaxBracket(12,  23200,  94300),
				new TaxBracket(22,  94300, 201050),
				new TaxBracket(24, 201050, 383900)
		);
		final int federalStandardDeduction2024 = 29200; //Married Filing Jointly

		final List<TaxBracket> massTaxBracket2024 = Arrays.asList( //Married Filing Jointly
				new TaxBracket(5, 0, 999999)
		);
		final int massStandardDeduction2023 = 2 * 4400; //Married Filing Jointly (from HRBlock 2023 tax software)
		//https://www.nerdwallet.com/article/taxes/massachusetts-state-tax-rates

		final int year = 2024;
		System.out.println(NL + "Taxes (for " + year + "):");
		for (int income = 120000; income <= 180000; income += 5000) {
			int federalIncomeTax = TaxBracket.calculateTax(income, federalStandardDeduction2024, federalTaxBracket2024);
			int massIncomeTax = TaxBracket.calculateTax(income, massStandardDeduction2023, massTaxBracket2024);
			int totalIncomeTax = federalIncomeTax + massIncomeTax;
			double effectiveTaxRate = 100. * (double) totalIncomeTax / (double) income;
			double federalTaxRate = 100. * (double) federalIncomeTax / (double) income;
			double massTaxRate = 100. * (double) massIncomeTax / (double) income;
			double totalTaxRate = 100. * (double) totalIncomeTax / (double) income;

			System.out.println("Pre-tax income: $" + decimalFormat0.format(income) + ", Post-tax income: $" + decimalFormat0.format(income - totalIncomeTax) +
								", Total tax: $" + decimalFormat0.format(totalIncomeTax) + " (" + decimalFormat1.format(totalTaxRate) + "%)" +
								", Fed: $" + decimalFormat0.format(federalIncomeTax) + " (" + decimalFormat1.format(federalTaxRate) + "%)" +
								", Mass: $" + decimalFormat0.format(massIncomeTax) + " (" + decimalFormat1.format(massTaxRate) + "%)");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	protected static class TaxBracket {

		///////////////////////////////////////////////////////////////////////////
		TaxBracket(int percentRate, int minValue, int maxValue) {
			this.percentRate = percentRate;
			this.minValue = minValue;
			this.maxValue = maxValue;
		}

		///////////////////////////////////////////////////////////////////////////
		static int calculateTax(int income, int deduction, List<TaxBracket> taxBrackets) {
			int tax = 0;
			int remainingIncome = income - deduction;

			for (TaxBracket taxBracket : taxBrackets) {
				if (remainingIncome > 0) {
					int incomeForThisBracket = Math.min(remainingIncome, taxBracket.maxValue - taxBracket.minValue);
					int taxForThisBracket = incomeForThisBracket * taxBracket.percentRate / 100;
					tax += taxForThisBracket;
					remainingIncome -= incomeForThisBracket;
				}
			}
			assert remainingIncome == 0;

			return tax;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString() {
			return "TaxBracket{" +
					"percentRate=" + percentRate +
					", minValue=" + minValue +
					", maxValue=" + maxValue +
					'}';
		}

		final int percentRate;
		final int minValue;
		final int maxValue;
	}

	///////////////////////////////////////////////////////////////////////////
	protected void printUrls() throws Exception {
		AtomicReference<String> suffix = new AtomicReference<>();

		if (true) {
			suffix.set("stars");

			List<FundsEnum> funds = new ArrayList<>(Arrays.asList(FundsEnum.values())).stream()
					.filter(f -> !PendingActivityString.equals(f.getSymbol()))
					.sorted((f1, f2) -> f1.getSymbol().compareToIgnoreCase(f2.getSymbol()))
					.collect(Collectors.toList());

			System.out.println(NL + "URLs for " + suffix + " (" + funds.size() + "):");
			funds.forEach(f -> System.out.println(
					"[" + f.getExpenseRatio() + "] " +
							f.getSymbol() + " -> " + f.getURL(suffix.get())));
		}

		if (true) {
			suffix.set("yield");

			List<FundsEnum> funds = new ArrayList<>(Arrays.asList(FundsEnum.values())).stream()
					.filter(f -> !PendingActivityString.equals(f.getSymbol()))
					.filter(f -> f.isCash() || f.isBond())
					.sorted((f1, f2) -> f1.getSymbol().compareToIgnoreCase(f2.getSymbol()))
					.collect(Collectors.toList());

			System.out.println(NL + "URLs for " + suffix + " (" + funds.size() + "):");
			funds.forEach(f -> System.out.println(
					"[" + f.getExpenseRatio() + "] " +
							f.getSymbol() + " -> " + f.getURL(suffix.get())));
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//to sort filenames by embedded date (newest first), with this date format in file name: path1\path2\Portfolio_Positions_Feb-24-2024.csv
	protected static class PortfolioFilenameComparatorByDateReverse implements Comparator<String> {

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
		protected long parseDateTimeFromFilename(String filename) throws Exception {
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
	//if we have more than one set of records for any given day, we can generally delete all but the newest/latest
	protected int deleteDuplicateRecords() throws Exception {
		int duplicateRecordsDeleted = 0;

		List<CsvFundsBean> allRecordsFromDb = queryRecordsFromDatabase(AllDates);

		Map<LocalDate, List<Instant>> dupMap = findDuplicateTimestamps(allRecordsFromDb);

		if (!dupMap.isEmpty()) {
			List<Instant> instantsToBeDeleted = new ArrayList<>();
			dupMap.values().forEach(l -> {
				l.remove(0); //remove newest/latest timestamp (don't delete that one)
				instantsToBeDeleted.addAll(l); //do delete the rest
			});

			if (instantsToBeDeleted.size() > 0) {
				instantsToBeDeleted.forEach(i -> System.out.println("toBeDeleted: " + dateTimeFormatter.format(i)));

				duplicateRecordsDeleted = deleteRecordsFromDatabase(instantsToBeDeleted);
			}
		}

		return duplicateRecordsDeleted;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns map with key = LocalDate, value = List of the instants that fall on that date IF MORE THAN ONE
	protected Map<LocalDate, List<Instant>> findDuplicateTimestamps(List<CsvFundsBean> records) {
		Map<String, List<CsvFundsBean>> dateMap1 = records.stream()
				.collect(Collectors.groupingBy(r ->
						"" + r.getDateDownloaded().atZone(ZoneId.systemDefault()).toLocalDate() +
						"|" + r.getAccountNumber() +
						"|" + r.getSymbol()
				));

		Map<LocalDate, List<Instant>> dateMap2 = dateMap1.values().stream()
				.filter(l -> l.size() > 1) //we only care about duplicates
				.collect(Collectors.toMap(
						r -> r.get(0).getDateDownloaded().atZone(ZoneId.systemDefault()).toLocalDate(),
						r -> r.stream().map(CsvFundsBean::getDateDownloaded)
									   .sorted(Comparator.reverseOrder()) //sort so newest/latest timestamp is first in each list
									   .collect(Collectors.toList()),
						(r1, r2) -> { //merge function, added to avoid: java.lang.IllegalStateException: Duplicate key <key>
							if (!r2.equals(r1)) {
								System.out.println("findDuplicateTimestamps: record1: " + r1);
								System.out.println("findDuplicateTimestamps: record2: " + r2);
								throw new RuntimeException("oops, merge found unequal values; for now, delete from DB by hand");
							}
							return r2;
						}
				));

		return dateMap2;
	}

	///////////////////////////////////////////////////////////////////////////
	protected int persistRecordsToDatabase(List<CsvFundsBean> records) throws Exception {
		int rowsPersisted = 0;

		if (records != null) {
			try (Connection connection = connectDatabase()) {
				for (CsvFundsBean record : records) {
					if (persistRecordToDatabase(connection, record)) {
						++rowsPersisted;
					}
				}
			}
		}

		return rowsPersisted;
	}

	///////////////////////////////////////////////////////////////////////////
	protected boolean persistRecordToDatabase(Connection connection, CsvFundsBean record) {
		final String sql = "insert into retirement (downloaded_timestamp, account_number, account_name, symbol, description, value, cost_basis)" + NL +
						   " values (?, ?, ?, ?, ?, ?, ?)";

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			int index = 0;
			assert null != record.getDateDownloaded(); //catch programming error
			stmt.setTimestamp(++index, java.sql.Timestamp.from(record.getDateDownloaded()));
			stmt.setString(++index, record.getAccountNumber());
			stmt.setString(++index, record.getAccountName());
			stmt.setString(++index, record.getSymbol());
			stmt.setString(++index, record.getDescription());
			stmt.setDouble(++index, record.getCurrentValue());
			stmt.setDouble(++index, record.getCostBasisTotal());

			stmt.executeUpdate ();

//		} catch (SQLIntegrityConstraintViolationException ex) {
//			if (!ex.getMessage().matches("Duplicate entry.*PRIMARY.*")) { //we expect to get duplicate entries because we aren't checking before persisting (which is a TODO)
//				System.err.println("persistRecordToDatabase: error persisting record <" + record + ">");
//				System.err.println(ex.getMessage());
//			}
//			return false;

		} catch (Exception ex) {
			System.err.println("persistRecordToDatabase: error persisting record <" + record + ">");
			System.err.println(ex.getMessage());
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	static class AggregateRecord implements Comparator<AggregateRecord> {
		AggregateRecord() {
			this(0, null, 0, 0.);
		}
		AggregateRecord(int index, Instant dateDownloaded, int numRecords, Double totalValue) {
			this.index = index;
			this.dateDownloaded = dateDownloaded;
			this.numRecords = numRecords;
			this.totalValue = totalValue;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compare (AggregateRecord r1, AggregateRecord r2) {
			return r1.totalValue.compareTo(r2.totalValue);
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString() {
			return index + "  " +
				   numRecords + "  " +
				   dateTimeFormatter.format(dateDownloaded) + "  " +
				   "$" + decimalFormat0.format(totalValue);
		}

		final int index;
		final Instant dateDownloaded;
		final int numRecords;
		final Double totalValue;
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<AggregateRecord> queryAggregateRecordsFromDatabase() {
		String sql = "select downloaded_timestamp, count(*) records, sum(value) total_value" + NL +
					 " from retirement" +
					 " group by downloaded_timestamp" +
					 " order by downloaded_timestamp";

		List<AggregateRecord> records = new ArrayList<>();

		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			int index = 0;
			while (rs.next()) {
				Timestamp timestamp = rs.getTimestamp("downloaded_timestamp");
				int count = rs.getInt("records");
				Double totalValue = rs.getDouble("total_value");

				AggregateRecord record = new AggregateRecord(++index, timestamp.toInstant(), count, totalValue);
				records.add(record);
			}

		} catch (Exception ex) {
			System.err.println("queryAggregateRecordsFromDatabase: error running sql <" + sql + ">");
			System.err.println(ex.getMessage());
			return records;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<CsvFundsBean> queryRecordsFromDatabase(Instant dateDownloaded) {
		String sql = "select downloaded_timestamp, account_number, account_name, symbol, description, value, cost_basis" + NL +
					 " from retirement";
		if (!dateDownloaded.equals(AllDates)) {
			sql += " where downloaded_timestamp = ?";
		}

		List<CsvFundsBean> records = new ArrayList<>();

		ResultSet rs = null;
		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {

			if (!dateDownloaded.equals(AllDates)) {
				java.sql.Timestamp timestamp = java.sql.Timestamp.from(dateDownloaded);
				stmt.setTimestamp(1, timestamp);
			}

			rs = stmt.executeQuery ();

			while (rs.next ()) {
				java.sql.Timestamp timestamp = rs.getTimestamp("downloaded_timestamp");
				String accountNumber = rs.getString ("account_number");
				String accountName = rs.getString ("account_name");
				String symbol = rs.getString ("symbol");
				String description = rs.getString ("description");
				Double currentValue = rs.getDouble ("value");
				Double costBasisTotal = rs.getDouble ("cost_basis");

				CsvFundsBean record = new CsvFundsBean(timestamp.toInstant(), accountNumber, accountName, symbol, description, currentValue, costBasisTotal);
				records.add(record);
			}

		} catch (Exception ex) {
			System.err.println("queryRecordsFromDatabase: error running sql <" + sql + "> for dateDownloaded <" + dateDownloaded + ">");
			System.err.println(ex.getMessage());
			return records;

		} finally {
			if (rs != null) {
				try { rs.close (); } catch (SQLException ignored) {}
			}
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	protected int deleteRecordsFromDatabase(List<Instant> instants) {
		final String sql = "delete from retirement where downloaded_timestamp = ?";

		int totalRowsDeleted = 0;
		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {

			for (Instant instant : instants) {
				stmt.setTimestamp(1, java.sql.Timestamp.from(instant));
				stmt.addBatch();
			}

			int [] rowsDeleted = stmt.executeBatch();
			totalRowsDeleted += Arrays.stream (rowsDeleted).sum ();

		} catch (Exception ex) {
			System.err.println("deleteRecordsFromDatabase: error deleting records <" + instants + ">");
			System.err.println(ex.getMessage());
			return totalRowsDeleted;
		}

		return totalRowsDeleted;
	}

	///////////////////////////////////////////////////////////////////////////
	protected Connection connectDatabase () throws Exception {
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.cj.jdbc.Driver";
		final String dbUrl = "jdbc:mysql://localhost/retirement";
		final String dbUser = "root";
		final String dbPass = "root";

		Class.forName (jdbcDriver);
		return DriverManager.getConnection (dbUrl, dbUser, dbPass);
	}


	//private members
	private Path sourceRootPath = null;
	private Pattern filenamePattern = null;
	private String inputFilenameOverride = null; //to specify a specific input file
	private boolean printHistoricalData = false;
	private boolean printTaxes = false;
	private boolean printUrls = false;
	private boolean generatePlotFile = false;
	private boolean deleteDuplicateRecords = false;

	private static final int csvExpectedCommas = 15; //hardcoded

	private static final Predicate<CsvFundsBean> rothAccounts = CsvFundsBean::isRoth;
	private static final Predicate<CsvFundsBean> traditionalAccounts = rothAccounts.negate();
	private static final Predicate<CsvFundsBean> allAccounts = r -> true;

	private final List<String> dateDownloadedList = new ArrayList<>(); //use List in case there is more than one matching record in the file

	private static final FundResult BlankLine = new FundResult("blank line", 0, 0);
	private static final Instant AllDates = Instant.ofEpochSecond(9999); //some fixed, hopefully unique time

	private static final DecimalFormat decimalFormat0 = new DecimalFormat ("###,##0");
	private static final DecimalFormat decimalFormat1 = new DecimalFormat ("###,##0.0");
	private static final DecimalFormat decimalFormat2 = new DecimalFormat ("###,##0.00");

	protected static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss").withZone(ZoneId.systemDefault());

	//global members
	public static final String DateDownloadedString = "Date downloaded";
	public static final String PendingActivityString = "Pending Activity";
	public static final String PlotExecutable = "C:/Users/bin/plot/Release/plot.exe";
	public static final String PlotFileName = "C:/temp/retirement.gen.plt";

	public static boolean Debug = false;
	public static boolean Test = false;

	public static final String AppName = "JRetirement";
	public static final String NL = System.getProperty ("line.separator");
//	private static final Logger _log = LogManager.getLogger ();
}
