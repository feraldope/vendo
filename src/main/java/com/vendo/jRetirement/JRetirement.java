package com.vendo.jRetirement;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VFileList;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public final class JRetirement {

	///////////////////////////////////////////////////////////////////////////
	public static void main(String[] args) {
		JRetirement app = new JRetirement();

		if (!app.processArgs(args)) {
			System.exit(1); //processArgs displays error
		}

		app.run();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs(String[] args) {
		String filenamePatternString = "Portfolio_Positions_*.csv";
		String sourceRootName = "C:\\Users\\david\\OneDrive\\Documents\\";

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith("-") || arg.startsWith("/")) {
				arg = arg.substring(1);

				if (arg.equalsIgnoreCase("debug") || arg.equalsIgnoreCase("dbg")) {
					_Debug = true;

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

//				} else if (arg.equalsIgnoreCase("inputCsvFileName") || arg.equalsIgnoreCase("in")) {
//					try {
//						_inputCsvFileName = args[++ii];
//					} catch (ArrayIndexOutOfBoundsException exception) {
//						displayUsage("Missing value for /" + arg, true);
//					}

				} else {
					displayUsage("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

				} else {
*/
				displayUsage("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		_filenamePattern = Pattern.compile ("^" + filenamePatternString.replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling

		if (sourceRootName == null) {
			displayUsage ("Must specify source root folder", true);
		} else {
			_sourceRootPath = FileSystems.getDefault ().getPath (sourceRootName);
			if (!Files.exists (_sourceRootPath)) {
				System.err.println("JRetirement.processArgs: error source path does not exist: " + _sourceRootPath);
				return false;
			}
		}

		//check for required args and handle defaults
//		if (_inputCsvFileName == null || !VendoUtils.fileExists(_inputCsvFileName)) {
//			displayUsage("Must specify valid CSV input file '" + _inputCsvFileName + "'", true);
//		}

		if (_Debug || true) {
			System.out.println("JRetirement.processArgs: filenamePatternString: " + filenamePatternString + " => pattern: " + _filenamePattern.toString());
			System.out.println("JRetirement.processArgs: _sourceRootPath: " + _sourceRootPath.toString ());
			System.out.println("");
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage(String message, Boolean exit) {
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] TBD [/dest <dest dir>]";
		System.err.println("Error: " + msg + NL);

		if (exit) {
			System.exit(1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run() {
		List<String> sourceFileList = new VFileList(_sourceRootPath.toString(), Collections.singletonList(_filenamePattern), false).getFileList (VFileList.ListMode.CompletePath);

		if (sourceFileList.isEmpty()) {
			System.err.println("JRetirement.run: no files found matching sourceRootName = \"" + _sourceRootPath + "\", filenamePattern = \"" + _filenamePattern);
			return false;
		}

		//TODO add sort by name to get newest, with this date format in file name: Portfolio_Positions_Feb-24-2024.csv
		String newestFile = sourceFileList.stream().min(new AlphanumComparator(AlphanumComparator.SortOrder.Reverse)).orElse("");
		Path sourcePath = FileSystems.getDefault().getPath(newestFile);

		return processFile(sourcePath);
	}

	private static final List<String> symbolsToSkip = new ArrayList<>(Arrays.asList(
			"N/A",      //Annuity
			"MAX100907" //529
	));

	///////////////////////////////////////////////////////////////////////////
	private boolean processFile(Path inputCsvFilePath) {
		try {
			List<CsvFundsBean> records = csvBeanBuilder(inputCsvFilePath, CsvFundsBean.class);

			records.forEach(System.out::println);
			System.out.println("Read file: " + inputCsvFilePath);
			System.out.println("Records found: " + records.size());
			System.out.println("");

			double totalBond = 0;
			double totalCash = 0;
			double totalRoth = 0;
			double totalAllFunds = 0;

			Map<String, Double> balanceByStyleMap = new HashMap<>();

			for (CsvFundsBean record : records) {
				if (symbolsToSkip.contains(record.getSymbol())) {
					continue;
				}

				FundsEnum fundsEnum = FundsEnum.getValue(record.getSymbol());
				String style = fundsEnum.getStyle();

				Double balance = balanceByStyleMap.get(style);
				if (balance == null) {
					balance = 0.;
				}
				balance += record.getCurrentValue();
				balanceByStyleMap.put(style, balance);

				totalAllFunds += record.getCurrentValue();

				if (fundsEnum.isBond()) {
					totalBond += record.getCurrentValue();
				}

				if (fundsEnum.isCash()) {
					totalCash += record.getCurrentValue();
				}

				if (record.isRoth()) {
					totalRoth += record.getCurrentValue();
				}
			}

			for (Map.Entry<String, Double> entry : balanceByStyleMap.entrySet().stream()
					.sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
				String style = entry.getKey();
				double total = entry.getValue();
				double percent = 100. * total / totalAllFunds;

				System.out.println(style + " total = $" + _decimalFormat2.format(total) + ", percent = " + _decimalFormat1.format(percent) + "%");
			}

			System.out.println("");

			double bondPercent = 100 * totalBond / totalAllFunds;
			double cashPercent = 100 * totalCash / totalAllFunds;
			double rothPercent = 100 * totalRoth / totalAllFunds;

			System.out.println("totalAllFunds = $" + _decimalFormat2.format(totalAllFunds));
			System.out.println("totalBond = $" + _decimalFormat2.format(totalBond) + ", bondPercent = " + _decimalFormat1.format(bondPercent) + "%");
			System.out.println("totalCash = $" + _decimalFormat2.format(totalCash) + ", cashPercent = " + _decimalFormat1.format(cashPercent) + "%");
			System.out.println("totalRoth = $" + _decimalFormat2.format(totalRoth) + ", rothPercent = " + _decimalFormat1.format(rothPercent) + "%");

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<CsvFundsBean> csvBeanBuilder(Path path, Class<? extends CsvFundsBean> clazz) {
		List<CsvFundsBean> records = new ArrayList<>();

		try {
			Reader reader = getReaderFromStringList(readAllValidLines(path));
			CsvToBean<CsvFundsBean> cb = new CsvToBeanBuilder<CsvFundsBean>(reader)
					.withType(clazz)
					.build();
			records = cb.parse();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	Reader getReaderFromStringList(List<String> list) {
		return new BufferedReader(new StringReader(String.join("\n", list)));
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> readAllValidLines(Path filePath) throws Exception {
		final int expectedCommas = 15; //hardcoded

		List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8)
								  .stream()
								  .filter(s -> countCommas(s) >= expectedCommas)
								  .collect(Collectors.toList());

		return lines;
	}

	///////////////////////////////////////////////////////////////////////////
	public int countCommas(String string) {
		String[] parts = string.split(",");
		return parts.length;
	}


	//private members
//	private String _inputCsvFolder = null;
//	private String _inputCsvPattern = null;

	private Path _sourceRootPath = null;
	private Pattern _filenamePattern;

	private static final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0.0");
	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0.00");

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "JRetirement";
	public static final String NL = System.getProperty ("line.separator");
//	private static final Logger _log = LogManager.getLogger ();
}
