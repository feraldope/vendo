package com.vendo.jRetirement;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.vendo.vendoUtils.VFileList;
import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vendo.jRetirement.FundsEnum.FundOwner;
import static com.vendo.jRetirement.RetirementDao.AllDates;


public class JRetirement {

	///////////////////////////////////////////////////////////////////////////
	static {
		Thread.setDefaultUncaughtExceptionHandler (new VUncaughtExceptionHandler());
	}

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
		String userName = System.getProperty ("user.name");
		String filenamePatternString = "*.csv";
		String sourceRootName = "C:/Users/" + userName + "/OneDrive/Documents/Fidelity/";

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

				} else if (arg.equalsIgnoreCase ("sourceFolder") || arg.equalsIgnoreCase ("src")) {
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
//				if (inputFilenameOverride == null) {
//					inputFilenameOverride = arg;
//
//				} else {
					displayUsage("Unrecognized argument '" + args[ii] + "'", true);
//				}
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

		msg += "Usage: " + AppName + " [/debug] [/test] [/deleteDuplicateRecords] [/generatePlotFile] [/pattern <pattern>] [/printHistoricalData] [/printTaxes] [/printUrls] [/sourceFolder <folder>]";
		System.err.println("Error: " + msg + NL);

		if (exit) {
			System.exit(1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	protected boolean run() throws Exception {
		if (updateFundsMetaDataInDatabase) {
			updateFundsMetaDataInDatabase();
		}

		List<Path> sourceFilePathList = new VFileList(sourceRootPath.toString(), Collections.singletonList(filenamePattern), false).getPathList();
//						.stream().sorted(new PortfolioFilenameComparatorByDateReverse()).collect(Collectors.toList());

		if (sourceFilePathList.isEmpty()) {
			System.err.println("JRetirement.run: no files found matching sourceRootName = \"" + sourceRootPath + "\", filenamePattern = \"" + filenamePattern);

		} else {
			updatePortfolioPositionsDataInDatabase(sourceFilePathList);
			updateAccountsHistoryDataInDatabase(sourceFilePathList);
		}

		printLatestPortfolioPositionsData();

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	protected boolean printLatestPortfolioPositionsData() throws Exception {
		final RetirementDao retirementDao = RetirementDao.getInstance();

//		final List<FundsMetaData> fundsMetaDataFromDb = retirementDao.queryFundsMetaDataFromDatabase();
		final Map<String, FundsMetaData> fundsMetaDataMap = retirementDao.queryFundsMetaDataFromDatabase().stream()
				.collect(Collectors.toMap(FundsMetaData::getSymbol, Function.identity()));

////Map<String, Integer> map = list.stream().collect(Collectors.toMap(AlbumImageCount::getBaseName, AlbumImageCount::getCount));

		final List<PortfolioPositionsData> portfolioPositionsDataFromDb = retirementDao.queryPortfolioPositionsDataFromDatabase(AllDates);

		Instant latestDateDownloadedFromDb = portfolioPositionsDataFromDb.stream()
				.map(PortfolioPositionsData::getDateDownloaded)
				.max(Instant::compareTo).orElse(null);

		final List<PortfolioPositionsData> records = portfolioPositionsDataFromDb.stream()
				.filter(p -> p.getDateDownloaded().equals(latestDateDownloadedFromDb))
//TODO - this should be done in DAO (adding FundsMetaData object to PortfolioPositionsData records
//				.filter(p -> {
//					FundsMetaData fundsMetaData = fundsMetaDataMap.get(p.getSymbol());
//					p.setFundsMetaData(fundsMetaData);
//					return true;
//				})
				.collect(Collectors.toList());

		System.out.println(NL + "Data for: " + dateTimeFormatterMdyHms.format(latestDateDownloadedFromDb));

//where should this live? Also, it needs to be reviewed/rewritten
//		if (deleteDuplicateRecords) {
//			int duplicateRecordsDeleted = deleteDuplicateRecords();
//			System.out.println("Duplicate records deleted: " + duplicateRecordsDeleted);
//		}

		System.out.println(NL + "Roth Accounts -----------------------------------------------------------");
		printDistribution(records, PortfolioPositionsData.rothAccounts, true, false);

		System.out.println(NL + "Traditional Accounts ----------------------------------------------------");
		printDistribution(records, PortfolioPositionsData.traditionalAccounts, true, true);

		System.out.println(NL + "All Accounts ------------------------------------------------------------");
		printDistribution(records, PortfolioPositionsData.allAccounts, true, true);

		if (printTaxes) {
			printTaxes();
		}

		if (printHistoricalData || generatePlotFile) {
			List<AggregateRecord> aggregateRecords = retirementDao.queryAggregateRecordsFromDatabase();

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
	protected List<CsvPortfolioPositionsBean> processPortfolioPositionsFile(Path inputCsvFilePath) {
		List<CsvPortfolioPositionsBean> records;
		try {
			records = generateFilteredPortfolioPositionsRecordList(inputCsvFilePath, true);
			VendoUtils.myAssert(records != null && !records.isEmpty(), "records != null && !records.isEmpty()", null); //do not use Java's assert as it is disabled by default

			//data integrity check - we should have 4 or more accounts in the file
			final int expectedAccounts = 4; //hardcoded
			Set<String> accounts = Objects.requireNonNull(records).stream()
					.map(CsvPortfolioPositionsBean::getAccountNumber)
					.collect(Collectors.toSet());
			if (accounts.size() < expectedAccounts) {
				System.out.println(NL + "Error: " + (expectedAccounts - accounts.size()) + " missing accounts. Found accounts: " + String.join(", ", accounts));
				return null;
			}

			if (printUrls) {
				printUrls();
			}

			if (false) {
				System.out.println(NL + "Filtered records (" + records.size() + "):");
				records.forEach(System.out::println);
			}

			if (false) {
				System.out.println(NL + "Read file: " + inputCsvFilePath);
				System.out.println("Records found: " + records.size());
			}

//			if (false) {
//				List<CsvPortfolioPositionsBean> pendingActivity = records.stream().filter(CsvPortfolioPositionsBean::isPendingActivity).collect(Collectors.toList());
//				if (!pendingActivity.isEmpty()) {
//					System.out.println(NL + "Pending Activity:");
//					pendingActivity.forEach(System.out::println);
//					System.out.println("");
//				}
//			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<CsvAccountsHistoryBean> processAccountsHistoryFile(Path inputCsvFilePath) {
		List<CsvAccountsHistoryBean> records;
		try {
			records = generateFilteredAccountsHistoryRecordList(inputCsvFilePath, true);
			VendoUtils.myAssert(records != null && !records.isEmpty(), "records != null && !records.isEmpty()", null); //do not use Java's assert as it is disabled by default

			if (false) {
				System.out.println(NL + "Filtered records (" + records.size() + "):");
				records.forEach(System.out::println);
			}

			if (false) {
				System.out.println(NL + "Read file: " + inputCsvFilePath);
				System.out.println("Records found: " + records.size());
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	protected boolean printDistribution(List<PortfolioPositionsData> records, /*FundsMetaData fundsMetaDataRecords,*/ Predicate<PortfolioPositionsData> predicate, boolean includeBreakOut, boolean includeWithdrawalAmounts) {
		try {
			double totalBond = 0;
			double totalCash = 0;
			double totalEquity = 0;
			double totalHealth = 0;
			double totalInternational = 0;
			double totalActive = 0;
			double totalIndex = 0;
			double totalRoth = 0;
			double totalPendingActivity = 0;
			double totalAllFunds = 0;
			boolean foundPendingActivity = false;

			Map<String, Double> balancesByGroupingMap = new HashMap<>();

			for (PortfolioPositionsData record : records) {
				if (!predicate.test(record)) {
					continue;
				}

				FundOwner fundOwner = record.getFundOwner();

//				FundsEnum fund = getValue(record.getSymbol());
				FundsMetaData fund = record.getFundsMetaData();


				//				String groupBy = fund.getFundFamily() + "." + fund.getStyle();
//				String groupBy = fund.getFundFamily() + "." + fund.getCategory();
//				String groupBy = fund.getFundFamily() + "." + fund.getFundType() + "." + fund.getCategory();

				String groupBy;

/*
				groupBy = ""
//						+ "[" + fund.getExpenseRatio() + "] "
//						+ "[" + fundOwner + "] "
///						+ StringUtils.rightPad(fund.getSymbolForGrouping(), 5, ' ')
//						+ " => " + fund.getFundFamily()
///						+ " => " + fund.getDescription()
//						+ "." + fundOwner
///						+ "." + fund.getFundType()
//						+ "." + fund.getManagementStyle()
						+ "." + fund.getCategory();
*/

				groupBy = ""
//						+ "[" + fund.getExpenseRatio() + "] "
//						+ "[" + fundOwner + "] "
						+ StringUtils.rightPad(fund.getSymbolForGrouping(), 5, ' ')
//						+ " => " + fund.getFundFamily()
//						+ "." + fundOwner
//						+ "." + fund.getFundType()
//						+ "." + fund.getManagementStyle()
						+ "." + fund.getCategory();

				groupBy = ""
//						+ "[" + fund.getExpenseRatio() + "] "
//						+ "[" + fundOwner + "] "
						+ StringUtils.rightPad(fund.getSymbolForGrouping(), 5, ' ')
						+ " => " + fund.getFundFamily()
//						+ "." + fundOwner
//						+ "." + fund.getFundType()
//						+ "." + fund.getManagementStyle()
						+ "." + fund.getCategory();

//				groupBy = fund.getFundTheme().toString();

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

				if (fund.isInternational()) {
					totalInternational += record.getCurrentValue();
				}

				if (fund.isActive()) {
					totalActive += record.getCurrentValue();
				} else if (fund.isIndex()) {
					totalIndex += record.getCurrentValue();
				}

				if (record.getTaxableType() == FundsEnum.TaxableType.ROTH) {
					totalRoth += record.getCurrentValue();
				}

				if (record.isPendingActivity()) {
					System.out.println("[DEBUG] *** current value for record where isPendingActivity=true: " + record.getCurrentValue());

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
				results.add(FundResult.BlankLine);
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
				double percentInternational = 100 * totalInternational / totalAllFunds;
				double percentActive = 100 * totalActive / totalAllFunds;
				double percentIndex = 100 * totalIndex / totalAllFunds;
				double percentRoth = 100 * totalRoth / totalAllFunds;

				totals.add(new FundResult("Total Bond", totalBond, percentBond));
				totals.add(new FundResult("Total Cash/CDs", totalCash, percentCash, foundPendingActivity ? " <<< adjusted for " + PendingActivityString : ""));
				totals.add(new FundResult("Total Equity", totalEquity, percentEquity));
				totals.add(new FundResult("Total Health", totalHealth, percentHealth));
				totals.add(new FundResult("Total International", totalInternational, percentInternational));
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
			return results.stream().map(r -> dollarFormat0.format(r.total).length() + lengthOfSymbol).max(Integer::compare).orElse(defaultFieldLength);
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
						: String.format("%" + longestTotal + "s", dollarFormat0.format(result.total)) + space2) +
						String.format("%" + longestPercent + "s", decimalFormat1.format(result.percent) + "%") +
						result.specialNote;
			}
		}

		final String label;
		final Double total;
		final double percent;
		final String specialNote;

		protected static final int lengthOfSymbol = 1; //hardcoded; i.e., "$" or "%" //TODO - fix this: "$" is now embedded in the format
		protected static final int defaultFieldLength = 20; //hardcoded
		protected static final String space2 = "  "; //hardcoded

		protected static final FundResult BlankLine = new FundResult("blank line", 0, 0);
	}

	///////////////////////////////////////////////////////////////////////////
	public Reader getReaderForStringList(List<String> list) {
		return new BufferedReader(new StringReader(String.join("\n", list)));
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<CsvPortfolioPositionsBean> generateFilteredPortfolioPositionsRecordList(Path inputCsvFilePath, final boolean printSkippedRecords) {
		final double minimumValueToBeIncluded = 500.; //hardcoded - skip funds that have less than this amount
//		final List<String> accountsToSkip = new ArrayList<>(Arrays.asList("Fixed Annuity", "Individual - 529 - TOD", "Individual - TOD"));
		final List<String> accountsToSkip = new ArrayList<>(Arrays.asList("Fixed Annuity", "Individual - 529 - TOD"));
		final List<String> skippedRecords = new ArrayList<>();

		List<CsvPortfolioPositionsBean> records;

		try {
			records = readPortfolioPositionsCsvFile(inputCsvFilePath, CsvPortfolioPositionsBean.class).stream()
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

		if (false && printSkippedRecords && !skippedRecords.isEmpty()) {
			System.out.println("JRetirement.generateFilteredPortfolioPositionsRecordList:");
			skippedRecords.stream().sorted().forEach(System.out::println);
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<CsvPortfolioPositionsBean> readPortfolioPositionsCsvFile(Path path, Class<? extends CsvPortfolioPositionsBean> clazz) {
		List<CsvPortfolioPositionsBean> records;

		try {
			Reader reader = getReaderForStringList(readAllValidPortfolioPositionsLines(path, true));
			CsvToBean<CsvPortfolioPositionsBean> cb	= new CsvToBeanBuilder<CsvPortfolioPositionsBean>(reader)
						.withType(clazz)
						.withIgnoreEmptyLine(true)
						.build();

			records = cb.parse();

		} catch (Exception ex) {
			System.out.println("JRetirement.readPortfolioPositionsCsvFile: error parsing file: " + path);
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> readAllValidPortfolioPositionsLines(Path filePath, final boolean printSkippedLines) throws Exception {
		final List<String> skippedLines = new ArrayList<>();

		List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8).stream()
			.map(this::stripLeadingBomUnicodeChar)
			.map(l -> !l.startsWith(AccountNumberString) ? repairDataLines(l, csvPortfolioPositionsExpectedCommas) : l)
			.map(l -> l.toLowerCase().contains(PendingActivityString.toLowerCase()) ? repairPendingActivityLine(l) : l) //HACK
			.filter(l -> {
				boolean keepLine = true;
				if (l.contains(DateDownloadedString)) {
					dateDownloadedList.add(l);
					keepLine = false;
				} else if (l.startsWith(CsvCommentDelimiter)) {
					skippedLines.add("skipped line (comment): " + l);
					keepLine = false;
				} else if (countCommas(l) < csvPortfolioPositionsExpectedCommas) {
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

			if (false) {
				System.out.println("JRetirement.readAllValidPortfolioPositionsLines:");
				skippedLines.stream().sorted().forEach(l -> {
					int charsToPrint = Math.min(l.length(), maxCharsToPrint);
					boolean truncated = l.length() > charsToPrint;
					System.out.println(l.substring(0, charsToPrint) + (truncated ? "*" : ""));
				});
			}

			final int maxExpectedSkippedLines = 3;
			if (skippedLines.size() > maxExpectedSkippedLines) { //hack
				throw new RuntimeException("Error: number of skipped lines (" + skippedLines.size() + ") is greater than expected (" + maxExpectedSkippedLines + ").");
			}
		}

		return lines;
	}

	///////////////////////////////////////////////////////////////////////////
	protected List<CsvAccountsHistoryBean> generateFilteredAccountsHistoryRecordList(Path inputCsvFilePath, final boolean printSkippedRecords) {
//		final List<String> accountsToSkip = new ArrayList<>(Arrays.asList("Fixed Annuity", "Individual - 529 - TOD", "Individual - TOD"));
		final List<String> accountsToSkip = new ArrayList<>(Arrays.asList("Fixed Annuity", "Individual - 529 - TOD"));
		final List<String> skippedRecords = new ArrayList<>();

		List<CsvAccountsHistoryBean> records;

		try {
			records = readAccountsHistoryCsvFile(inputCsvFilePath, CsvAccountsHistoryBean.class).stream()
					.filter(r -> {
						boolean keepRecord = true;
						if (accountsToSkip.contains(r.getAccount())) {
							skippedRecords.add("skipped record (account): " + r);
							keepRecord = false;
						}
						return keepRecord;
					})
					.collect(Collectors.toList());

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

		if (false && printSkippedRecords && !skippedRecords.isEmpty()) {
			System.out.println("JRetirement.generateFilteredAccountsHistoryRecordList:");
			skippedRecords.stream().sorted().forEach(System.out::println);
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<CsvAccountsHistoryBean> readAccountsHistoryCsvFile(Path path, Class<? extends CsvAccountsHistoryBean> clazz) {
		List<CsvAccountsHistoryBean> records;

		try {
			Reader reader = getReaderForStringList(readAllValidAccountsHistoryLines(path, true));
			CsvToBean<CsvAccountsHistoryBean> cb = new CsvToBeanBuilder<CsvAccountsHistoryBean>(reader)
						.withType(clazz)
						.withIgnoreEmptyLine(true)
						.build();

			records = cb.parse();

		} catch (Exception ex) {
			System.out.println("JRetirement.readAccountsHistoryCsvFile: error parsing file: " + path);
			ex.printStackTrace();
			return null;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> readAllValidAccountsHistoryLines(Path filePath, final boolean printSkippedLines) throws Exception {
		final List<String> skippedLines = new ArrayList<>();

		AtomicInteger requiredCommas = new AtomicInteger(0);
		List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8).stream()
//			.peek(l -> System.out.println("***DEBUG*** " + l)) //note this is written to file tomcat8-stdout.yyyy-mm-dd.log in tomcat log directory
			.map(this::stripLeadingBomUnicodeChar)
			.map(l -> {
				if (l.startsWith(RunDateString)) {
					requiredCommas.set(countCommas(l));
					return l;
				} else {
					return repairDataLines(l, requiredCommas.get());
				}
			})
			.filter(l -> {
				boolean keepLine = true;
				if (l.contains(DateDownloadedString)) {
					keepLine = false;
				} else if (l.startsWith(CsvCommentDelimiter)) {
					skippedLines.add("skipped line (comment): " + l);
					keepLine = false;
				} else if (countCommas(l) < csvAccountsHistoryExpectedCommas) {
					if (!StringUtils.isBlank(l)) {
						skippedLines.add("skipped line (not data): " + l);
					}
					keepLine = false;
				}
				return keepLine;
			})
			.collect(Collectors.toList());

		if (false && printSkippedLines && !skippedLines.isEmpty()) {
			final int maxCharsToPrint = 100; //hardcoded

			if (true) {
				System.out.println("JRetirement.readAllValidAccountsHistoryLines:");
				skippedLines.stream().sorted().forEach(l -> {
					int charsToPrint = Math.min(l.length(), maxCharsToPrint);
					boolean truncated = l.length() > charsToPrint;
					System.out.println(l.substring(0, charsToPrint) + (truncated ? "*" : ""));
				});
			}

			final int maxExpectedSkippedLines = 8;
			if (skippedLines.size() > maxExpectedSkippedLines) { //HACK
				throw new RuntimeException("Error: number of skipped lines (" + skippedLines.size() + ") is greater than expected (" + maxExpectedSkippedLines + ").");
			}
		}

		return lines;
	}

	///////////////////////////////////////////////////////////////////////////
	protected static int countCommas(String string) {
		return string.replaceAll("[^,]","").length();
	}

	///////////////////////////////////////////////////////////////////////////
	//remove unicode character at beginning of CSV file that prevents successful parsing
	protected String stripLeadingBomUnicodeChar(String line) {
		final int ZWNBSP = '\uFEFF'; //ZWNBSP (unicode zero-width no-break space) a.k.a. BOM (byte-order-mark) character

		if (!line.isEmpty() && line.charAt(0) == ZWNBSP) {
			return line.substring(1);
		}

		return line;
	}

	///////////////////////////////////////////////////////////////////////////
	//this "repair" hack is because sometimes the data lines have extra or missing commas
	//NOTE THIS CURRENTLY ONLY adds or removes *one* comma
	protected String repairDataLines(String line, int expectedCommas) {
		if (!line.isEmpty()) {
			int numCommas = countCommas(line);
			if (numCommas > expectedCommas) {
				if (line.endsWith(",")) {
					line = line.substring(0, line.length() - 1);
				}
			} else if (numCommas > 0 && numCommas < expectedCommas) {
				line += ",";
			}
		}

		return line;
	}

	///////////////////////////////////////////////////////////////////////////
	//this "repair" hack is because the pending activity amount was incorrectly in the "Last Price Change" column (see e.g., Portfolio_Positions_Feb-22-2025.csv)
	//but starting around Jun-2025, they seemed to have fixed this for some account types
	//in these cases the pending activity amount is now correctly found in the "Current Value" column (see e.g., Portfolio_Positions_Jul-14-2025.csv)
	//run these command with four and then five trailing commas to see the change:
	// search Portfolio_Positions*2025*csv "Pending Activity,,,,$"
	// search Portfolio_Positions*2025*csv "Pending Activity,,,,,$"
	protected String repairPendingActivityLine(String line) { //HACK
		if (line.contains(PendingActivityStringLower)) {
			line = line.replaceAll(PendingActivityStringLower, PendingActivityString); //HACK - it seems Fidelity is not consistent in the case, because occasionally it has lower case "a"
		}

		if (line.contains(PendingActivityString)) {
			if (!line.contains(PendingActivityString + ",,,,,")) { //five commas means it's already in the correct column
				line = line.replaceAll(PendingActivityString, PendingActivityString + ","); //add comma to shift columns right
			}
			int missingCommas = csvPortfolioPositionsExpectedCommas - countCommas(line);
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

		VendoUtils.myAssert(dateDownloadedList.size() == 1, "dateDownloadedList.size() == 1"); //there must be only one (do not use Java's assert as it is disabled by default)

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
		VendoUtils.myAssert(dateDownloadedList.size() == 1, "dateDownloadedList.size() == 1", null); //there must be only one (do not use Java's assert as it is disabled by default)
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
		VendoUtils.myAssert(records != null && !records.isEmpty(), "records != null && !records.isEmpty()", null); //do not use Java's assert as it is disabled by default

		final int yMax = 2_250_000; //TODO - calculate!
		final int yMin = yMax - 250_000; //TODO - calculate!

		final int days = 150; //180;

		String timestamp = dateTimeFormatterMdyHms.format (Instant.now());

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
			"xAxisLabel=Date (" + days + " days)",

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

			final Instant earliestDate = Instant.now().minus(days, ChronoUnit.DAYS);
			records.stream()
				   .filter(r -> earliestDate.compareTo(r.dateDownloaded) <= 0)
				   .filter(r -> {
					   if (false) {
						   System.out.println("generatePlotFile: debug stream: " + r);
					   }
					   return true;
				   })
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
		VendoUtils.myAssert(records != null && !records.isEmpty(), "records != null && !records.isEmpty()", null); //do not use Java's assert as it is disabled by default

		if (true) {
			final int lastN = 10;
			List<AggregateRecord> lastNRecords = records.subList(Math.max(records.size() - lastN, 0), records.size());

			System.out.println(NL + "Last " + lastN + " historical records (" + lastN + " of " + records.size() + "):");
			lastNRecords.forEach(System.out::println);
		}

		if (false) {
			final int topN = 10;
			List<AggregateRecord> topNRecords = records.stream().sorted(new AggregateRecord().reversed()).limit(topN).collect(Collectors.toList());

			System.out.println(NL + "Top " + topN + " historical records (" + topN + " of " + records.size() + "):");
			topNRecords.forEach(System.out::println);
		}

		final AggregateRecord maxRecord = records.stream().max(new AggregateRecord()).orElse(null);
		VendoUtils.myAssert(maxRecord != null, "maxRecord != null", null); //do not use Java's assert as it is disabled by default
		System.out.println(NL + "Max record:");
		System.out.println(maxRecord);

		final AggregateRecord lastRecord = records.get(records.size() - 1);

		if (true) {
			final int weeks = 52;
			final Instant earliestDate = Instant.now().minus(7 * weeks, ChronoUnit.DAYS);
			AggregateRecord filteredMax = records.stream()
					.filter(r -> earliestDate.compareTo(r.dateDownloaded) <= 0)
					.max(new AggregateRecord()).orElse(null);

			System.out.println(NL + "Max record (for previous " + weeks + " weeks):");
			if (filteredMax != null) {
				System.out.println(filteredMax);
				double diffFromMax = lastRecord.totalValue - filteredMax.totalValue;
				double percentDiff = 100. * (lastRecord.totalValue - filteredMax.totalValue) / filteredMax.totalValue;
				System.out.println("Diff from that max: " + dollarFormat0.format(diffFromMax) + " -> " + decimalFormat1.format(percentDiff) + "%");
			} else {
				System.out.println("No data available");
			}
		}

		if (true) {
			final int days = 90;
			final Instant earliestDate = Instant.now().minus(days, ChronoUnit.DAYS);
			AggregateRecord filteredMax = records.stream()
					.filter(r -> earliestDate.compareTo(r.dateDownloaded) <= 0)
					.max(new AggregateRecord()).orElse(null);

			System.out.println(NL + "Max record (for previous " + days + " days):");
			if (filteredMax != null) {
				System.out.println(filteredMax);
				double diffFromMax = lastRecord.totalValue - filteredMax.totalValue;
				double percentDiff = 100. * (lastRecord.totalValue - filteredMax.totalValue) / filteredMax.totalValue;
				System.out.println("Diff from that max: " + dollarFormat0.format(diffFromMax) + " -> " + decimalFormat1.format(percentDiff) + "%");
			} else {
				System.out.println("No data available");
			}
		}

		if (true) {
			final int days = 30;
			final Instant earliestDate = Instant.now().minus(days, ChronoUnit.DAYS);
			AggregateRecord filteredMax = records.stream()
					.filter(r -> earliestDate.compareTo(r.dateDownloaded) <= 0)
					.max(new AggregateRecord()).orElse(null);

			System.out.println(NL + "Max record (for previous " + days + " days):");
			if (filteredMax != null) {
				System.out.println(filteredMax);
				double diffFromMax = lastRecord.totalValue - filteredMax.totalValue;
				double percentDiff = 100. * (lastRecord.totalValue - filteredMax.totalValue) / filteredMax.totalValue;
				System.out.println("Diff from that max: " + dollarFormat0.format(diffFromMax) + " -> " + decimalFormat1.format(percentDiff) + "%");
			} else {
				System.out.println("No data available");
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	protected void printTaxes() throws Exception {
//		final int federalStandardDeduction2023 = 27700; //Married Filing Jointly
//		final List<TaxBracket> federalTaxBracket2023 = Arrays.asList( //Married Filing Jointly
//				//https://www.irs.gov/filing/federal-income-tax-rates-and-brackets
//				new TaxBracket(10, 0, 22000),
//				new TaxBracket(12, 22000, 89450),
//				new TaxBracket(22, 89450, 190750),
//				new TaxBracket(24, 190750, 364200)
//		);
//		final int federalStandardDeduction2024 = 29200; //Married Filing Jointly
//		final List<TaxBracket> federalTaxBracket2024 = Arrays.asList( //Married Filing Jointly
//				//https://www.irs.gov/newsroom/irs-provides-tax-inflation-adjustments-for-tax-year-2024
//				new TaxBracket(10,      0,  23200),
//				new TaxBracket(12,  23200,  94300),
//				new TaxBracket(22,  94300, 201050),
//				new TaxBracket(24, 201050, 383900)
//		);

		final int year = 2025;
		final int federalStandardDeduction2025 = 30000; //Married Filing Jointly
		final List<TaxBracket> federalTaxBracket2025 = Arrays.asList( //Married Filing Jointly
				//https://www.irs.gov/newsroom/irs-provides-tax-inflation-adjustments-for-tax-year-2025
				new TaxBracket(10,      0,  23850),
				new TaxBracket(12,  23850,  96950),
				new TaxBracket(22,  96950, 206700),
				new TaxBracket(24, 206700, 394600)
		);

		final List<TaxBracket> massTaxBracket202X = Collections.singletonList( //Married Filing Jointly
				new TaxBracket(5, 0, 999999)
		);
		final int massStandardDeduction202X = 2 * 4400; //Married Filing Jointly (from HRBlock 2023 tax software)
		//https://www.nerdwallet.com/article/taxes/massachusetts-state-tax-rates

		System.out.println(NL + "Taxes (using rates for " + year + "):");

		final int baseIncome = 100000;
		double totalTaxOnBaseIncome = 0.;
		for (int income = 100000; income <= 180000; income += 5000) {
			int federalIncomeTax = TaxBracket.calculateTax(income, federalStandardDeduction2025, federalTaxBracket2025);
			int massIncomeTax = TaxBracket.calculateTax(income, massStandardDeduction202X, massTaxBracket202X);
			int totalIncomeTax = federalIncomeTax + massIncomeTax;
			if (income == baseIncome) {
				totalTaxOnBaseIncome = totalIncomeTax;
			}
			double extraTax = totalIncomeTax - totalTaxOnBaseIncome;

			double federalTaxRate = 100. * (double) federalIncomeTax / (double) income;
			double massTaxRate = 100. * (double) massIncomeTax / (double) income;
			double totalTaxRate = 100. * (double) totalIncomeTax / (double) income;

			System.out.println("Pre-tax income: " + dollarFormat0.format(income) + ", Post-tax income: " + dollarFormat0.format(income - totalIncomeTax) +
								", Total tax: " + dollarFormat0.format(totalIncomeTax) + " (" + decimalFormat1.format(totalTaxRate) + "%)" +
								", Fed: " + dollarFormat0.format(federalIncomeTax) + " (" + decimalFormat1.format(federalTaxRate) + "%)" +
								", Mass: " + dollarFormat0.format(massIncomeTax) + " (" + decimalFormat1.format(massTaxRate) + "%)" +
								", Extra tax beyond base income: " + dollarFormat0.format(extraTax));
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
			VendoUtils.myAssert(remainingIncome == 0, "remainingIncome == 0", null); //do not use Java's assert as it is disabled by default

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
		final AtomicReference<String> suffix = new AtomicReference<>();

		final RetirementDao retirementDao = RetirementDao.getInstance();
		final List<FundsMetaData> fundsMetaDataFromDb = retirementDao.queryFundsMetaDataFromDatabase();

		if (true) {
			suffix.set("stars");

			List<FundsMetaData> funds = fundsMetaDataFromDb.stream()
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

			List<FundsMetaData> funds = fundsMetaDataFromDb.stream()
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
	protected static class PortfolioFilenameComparatorByDateReverse implements Comparator<Path> {

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compare(Path filename1, Path filename2) {
			long time1 = 0;
			long time2 = 0;
			try {
				time1 = parseDateTimeFromPortfolioPositionsFilename(filename1.toString());
				time2 = parseDateTimeFromPortfolioPositionsFilename(filename2.toString());

				return Long.compare(time1, time2); //sort newest first
			} catch(Exception ex) {
				System.err.println("PortfolioFilenameComparatorByDateReverse.compare: failed to parse date from filename: <" +
						(time1 == 0 ? filename1 : filename2) + ">");
			}
			return filename1.toString().compareToIgnoreCase(filename2.toString()); //fallback if date parsing fails
		}

		///////////////////////////////////////////////////////////////////////////
		protected long parseDateTimeFromPortfolioPositionsFilename(String filename) throws Exception {
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

		final RetirementDao retirementDao = RetirementDao.getInstance();

		final List<PortfolioPositionsData> portfolioPositionsDataFromDb = retirementDao.queryPortfolioPositionsDataFromDatabase(AllDates);

		Map<LocalDate, List<Instant>> dupMap = findDuplicateTimestamps(portfolioPositionsDataFromDb);

		if (!dupMap.isEmpty()) {
			List<Instant> instantsToBeDeleted = new ArrayList<>();
			dupMap.values().forEach(l -> {
				l.remove(0); //remove newest/latest timestamp (don't delete that one)
				instantsToBeDeleted.addAll(l); //do delete the rest
			});

			if (instantsToBeDeleted.size() > 0) {
				instantsToBeDeleted.forEach(i -> System.out.println("toBeDeleted: " + dateTimeFormatterMdyHms.format(i)));

				duplicateRecordsDeleted = retirementDao.deleteRecordsFromDatabase(instantsToBeDeleted);
			}
		}

		return duplicateRecordsDeleted;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns map with key = LocalDate, value = List of the instants that fall on that date IF MORE THAN ONE
	protected Map<LocalDate, List<Instant>> findDuplicateTimestamps(List<PortfolioPositionsData> records) {
		Map<String, List<PortfolioPositionsData>> dateMap1 = records.stream()
				.collect(Collectors.groupingBy(r ->
						"" + r.getDateDownloaded().atZone(ZoneId.systemDefault()).toLocalDate() +
						"|" + r.getAccountNumber() +
						"|" + r.getSymbol()
				));

		Map<LocalDate, List<Instant>> dateMap2 = dateMap1.values().stream()
				.filter(l -> l.size() > 1) //we only care about duplicates
				.collect(Collectors.toMap(
						r -> r.get(0).getDateDownloaded().atZone(ZoneId.systemDefault()).toLocalDate(),
						r -> r.stream().map(PortfolioPositionsData::getDateDownloaded)
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
	private boolean updateFundsMetaDataInDatabase() throws Exception {
		Instant startInstant = Instant.now ();

		RetirementDao retirementDao = RetirementDao.getInstance();

		List<FundsMetaData> fundsMetaDataFromEnum = Arrays.stream(FundsEnum.values())
				.map(FundsMetaData::new)
				.collect(Collectors.toList());

		List<FundsMetaData> fundsMetaDataFromDb = retirementDao.queryFundsMetaDataFromDatabase();

		List<FundsMetaData> toBeAdded = fundsMetaDataFromEnum.stream()
				.filter(i -> !fundsMetaDataFromDb.contains(i))
				.collect(Collectors.toList());

		int rowsPersisted = 0;
		if (!toBeAdded.isEmpty()) {
			rowsPersisted = retirementDao.persistFundsMetaDataToDatabase(toBeAdded);
		}
		System.out.println();
		System.out.println("New FundsMetaData rows persisted to database: " + rowsPersisted);
		System.out.println("Total FundsMetaData rows in database: " + fundsMetaDataFromDb.size());
		System.out.println("Elapsed: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()).format (dateTimeFormatterMmSs));

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean updatePortfolioPositionsDataInDatabase(List<Path> sourceFilePathList) throws Exception {
		Instant startInstant = Instant.now ();

		final Pattern portfolioPositionsPattern = Pattern.compile("^.*?[\\\\/]Portfolio_Positions_.*.csv$", Pattern.CASE_INSENSITIVE);

		final List<PortfolioPositionsData> portfolioPositionsDataFromCsvFiles = new ArrayList<>();

		AtomicInteger filesParsedSuccess = new AtomicInteger(0);
		AtomicInteger filesParsedFailed = new AtomicInteger(0);
		sourceFilePathList.stream()
				.filter(p -> portfolioPositionsPattern.matcher(p.toString()).matches())
				.forEach(p -> {
					dateDownloadedList = new ArrayList<>();
					List<CsvPortfolioPositionsBean> records = processPortfolioPositionsFile(p);
					if (records == null || records.isEmpty()) {
						System.out.println("Error: no valid data records found for: " + p);
						filesParsedFailed.incrementAndGet();

					} else {
						final Instant dateDownloaded = parseDateDownloadedField(dateDownloadedList);
						records.forEach(r -> r.setDateDownloaded(dateDownloaded));

						portfolioPositionsDataFromCsvFiles.addAll(records.stream()
								.map(PortfolioPositionsData::new)
								.collect(Collectors.toList()));

						filesParsedSuccess.incrementAndGet();
					}
				});

		final RetirementDao retirementDao = RetirementDao.getInstance();

		final List<PortfolioPositionsData> portfolioPositionsDataFromDb = retirementDao.queryPortfolioPositionsDataFromDatabase(AllDates);

		final List<PortfolioPositionsData> toBeAdded = portfolioPositionsDataFromCsvFiles.stream()
				.filter(i -> !portfolioPositionsDataFromDb.contains(i))
				.collect(Collectors.toList());

		int rowsPersisted = 0;
		if (!toBeAdded.isEmpty()) {
			rowsPersisted = retirementDao.persistPortfolioPositionsDataToDatabase(toBeAdded);
		}
		System.out.println();
		System.out.println("PortfolioPositions CSV files parsed/failed: " + filesParsedSuccess.get() + "/" + filesParsedFailed.get());
		System.out.println("New PortfolioPositionsData rows persisted to database: " + rowsPersisted);
		System.out.println("Total PortfolioPositionsData rows in database: " + portfolioPositionsDataFromDb.size());
//TODO - this does not include the possibility that we added a new date+time after the query was run above
		System.out.println("Total PortfolioPositionsData unique *date+times* in database: " + portfolioPositionsDataFromDb.stream().map(PortfolioPositionsData::getDateDownloaded).collect(Collectors.toSet()).size());
		System.out.println("Elapsed: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()).format (dateTimeFormatterMmSs));

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean updateAccountsHistoryDataInDatabase(List<Path> sourceFilePathList) throws Exception {
		Instant startInstant = Instant.now ();

		final Pattern accountsHistoryPattern = Pattern.compile("^.*?[\\\\/]Accounts_History_.*.csv$", Pattern.CASE_INSENSITIVE);
		final Set<AccountsHistoryData> accountsHistoryDataFromCsvFiles = new HashSet<>(); //use Set to avoid duplicates

		AtomicInteger filesParsedSuccess = new AtomicInteger(0);
		AtomicInteger filesParsedFailed = new AtomicInteger(0);
		sourceFilePathList.stream()
				.filter(p -> accountsHistoryPattern.matcher(p.toString()).matches())
				.forEach(p -> {
					List<CsvAccountsHistoryBean> records = processAccountsHistoryFile(p);
					if (records == null || records.isEmpty()) {
						System.out.println("Error: no valid data records found for: " + p);
						filesParsedFailed.incrementAndGet();

					} else {
						accountsHistoryDataFromCsvFiles.addAll(records.stream()
								.map(AccountsHistoryData::new)
								.collect(Collectors.toList()));

						filesParsedSuccess.incrementAndGet();
					}
				});

//		if (false) {
////TODO - implement compare
//			List<AccountsHistoryData> sorted = accountsHistoryDataFromCsvFiles.stream()
//					.sorted(new AccountsHistoryData())
//					.distinct()
//					.collect(Collectors.toList());
//		}

		if (true) { //debug
			List<AccountsHistoryData> nullSymbolField = accountsHistoryDataFromCsvFiles.stream()
					.filter(r -> StringUtils.isBlank(r.getSymbol()))
					.collect(Collectors.toList());

			if (!nullSymbolField.isEmpty()) {
				System.out.println("The following records have symbol=null: " + NL + nullSymbolField.stream().map(Object::toString).collect(Collectors.joining(NL)));
			}
		}

		if (false) { //debug
			List<AccountsHistoryData> tmp = accountsHistoryDataFromCsvFiles.stream()
					.filter(r -> Math.abs(r.getAmount()) == 284.45)
					.collect(Collectors.toList());

			if (!tmp.isEmpty()) {
				System.out.println("The following records have ***: " + NL + tmp.stream().map(Object::toString).collect(Collectors.joining(NL)));
			}
		}

		if (false) { //debug
			int longest = accountsHistoryDataFromCsvFiles.stream().map(r -> r.getAction().length()).max(Integer::compare).orElse(0);
			int bh = 1;
		}

		final RetirementDao retirementDao = RetirementDao.getInstance();

		final List<AccountsHistoryData> accountsHistoryDataFromDb = retirementDao.queryAccountsHistoryDataFromDatabase(AllDates);

		final List<AccountsHistoryData> toBeAdded = accountsHistoryDataFromCsvFiles.stream()
				.filter(i -> !accountsHistoryDataFromDb.contains(i))
				.collect(Collectors.toList());

		int rowsPersisted = 0;
		if (!toBeAdded.isEmpty()) {
			rowsPersisted = retirementDao.persistAccountsHistoryDataToDatabase(toBeAdded);
		}
		System.out.println();
		System.out.println("AccountsHistory CSV files parsed/failed: " + filesParsedSuccess.get() + "/" + filesParsedFailed.get());
		System.out.println("New AccountsHistoryData rows persisted to database: " + rowsPersisted);
		System.out.println("Total AccountsHistoryData rows in database: " + accountsHistoryDataFromDb.size());
//TODO - this does not include the possibility that we added a new date+time after the query was run above
		System.out.println("Total AccountsHistoryData unique *dates* in database: " + accountsHistoryDataFromDb.stream().map(AccountsHistoryData::getRunDate).collect(Collectors.toSet()).size());
		System.out.println("Elapsed: " + LocalTime.ofNanoOfDay(Duration.between(startInstant, Instant.now()).toNanos()).format (dateTimeFormatterMmSs));

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
				   dateTimeFormatterMdyHms.format(dateDownloaded) + "  " +
				   dollarFormat0.format(totalValue);
		}

		final int index;
		final Instant dateDownloaded;
		final int numRecords;
		final Double totalValue;
	}


	//private members
	private Path sourceRootPath = null;
	private Pattern filenamePattern = null;
	private boolean printHistoricalData = false;
	private boolean printTaxes = false;
	private boolean printUrls = false;
	private boolean generatePlotFile = false;
	private boolean deleteDuplicateRecords = false;
	private boolean updateFundsMetaDataInDatabase = true;

	private List<String> dateDownloadedList; //use List in case there is more than one matching record in the file

	private static final int csvAccountsHistoryExpectedCommas = 16; //hardcoded
	private static final int csvPortfolioPositionsExpectedCommas = 15; //hardcoded

	private static final DecimalFormat dollarFormat0 = new DecimalFormat ("$###,##0;($###,##0)"); //embedded "$"; format negative numbers with parenthesis
	private static final DecimalFormat decimalFormat1 = new DecimalFormat ("###,##0.0;-###,##0.0"); //format negative numbers with minus sign

	protected static final DateTimeFormatter dateTimeFormatterMdyHms = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss").withZone(ZoneId.systemDefault());
	protected static final DateTimeFormatter dateTimeFormatterMmSs = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)

	//global members
	public static final String CsvCommentDelimiter = "#";
	public static final String DateDownloadedString = "Date downloaded";
	public static final String RunDateString = "Run Date";
	public static final String AccountNumberString = "Account Number";
	public static final String PendingActivityString = "Pending Activity";
	public static final String PendingActivityStringLower = "Pending activity"; //HACK - it seems Fidelity is not consistent in the case, because occasionally it has lower case "a"
	public static final String PlotExecutable = "C:/Users/bin/plot/Release/plot.exe";
	public static final String PlotFileName = "C:/temp/retirement.gen.plt";

	public static boolean Debug = false;
	public static boolean Test = false;

	public static final String AppName = "JRetirement";
	public static final String NL = System.getProperty ("line.separator");
//	private static final Logger _log = LogManager.getLogger ();
}
