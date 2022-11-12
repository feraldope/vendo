//AlbumFileRename.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VFileList;
import com.vendo.vendoUtils.VFileList.ListMode;
import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class AlbumFileRename
{
	private enum Mode {RenameAlbum, RenameImage}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args)
	{
		AlbumFileRename app = new AlbumFileRename ();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		try {
			app.run();
		} catch (Exception ex) {
			_log.error("Exception caught in main", ex);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String[] args)
	{
		for (int ii = 0; ii < args.length; ii++) {
			System.out.println ("arg" + ii + " = <" + args[ii] + ">");
		}

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("force")) {
					_force = true;

				} else if (arg.equalsIgnoreCase ("test")) {
					_testMode = true;

				} else if (arg.equalsIgnoreCase ("ma")) {
					_mode = Mode.RenameAlbum;

				} else if (arg.equalsIgnoreCase ("mi")) {
					_mode = Mode.RenameImage;

				} else if (arg.equalsIgnoreCase ("rootPath")) {
					try {
						_rootPath = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("ext")) {
					try {
						_extension = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("digits") || arg.equalsIgnoreCase ("d")) {
					try {
						_exactDigits = Integer.parseInt (args[++ii]);
						if (_exactDigits < 0) {
							throw (new NumberFormatException());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing values for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("rangeInclusive") || arg.equalsIgnoreCase ("ri")) {
					try {
						_startIndex = Integer.parseInt (args[++ii]);
						_endIndex = Integer.parseInt (args[++ii]);
						if (_startIndex < 0 || _endIndex < 0) {
							throw (new NumberFormatException());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing values for /" + arg, true);
					} catch (NumberFormatException exception) {
						//TODO this message is not quite right for parsing two integers
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("renum")) {
					try {
						_renumDigits = Integer.parseInt (args[++ii]);
						if (_renumDigits < 0) {
							throw (new NumberFormatException());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing values for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_inPattern == null) {
					_inPattern = arg;

				} else if (_outPattern == null) {
					_outPattern = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_inPattern == null) {
			displayUsage ("<inPattern> not specified", true);
		}

		if (_outPattern == null) {
			displayUsage("<outPattern> not specified", true);
		}

		if (_inPattern.equalsIgnoreCase(_outPattern)) {
			displayUsage("<inPattern> and <outPattern> are the same", true);
		}

		if (_extension == null) {
			_extension = _DefaultExtension;
		}

		_inPattern += _extension;
		_outPattern += _extension;

		String message = validatePattern(_inPattern);
		if (message != null) {
			displayUsage (message, true);
		}

		message = validatePattern(_outPattern);
		if (message != null) {
			displayUsage (message, true);
		}

//TODO - will these work for files NOT in or under PR_ROOT? _startIndex, _endIndex, _exactDigits

//TODO - verify dir exists, and is writable??
		if (_rootPath == null) {
			_rootPath = VendoUtils.getCurrentDirectory ();
		}
		_rootPath = appendSlash (_rootPath);

		if (_Debug) {
			_log.debug("_rootPath = " + _rootPath);
			_log.debug("_inPattern = " + _inPattern);
			_log.debug("_outPattern = " + _outPattern);
			_log.debug("_extension = " + _extension);
			_log.debug("_startIndex = " + _startIndex);
			_log.debug("_endIndex = " + _endIndex);
			_log.debug("_exactDigits = " + _exactDigits);
			_log.debug("_mode = " + _mode);
//			System.out.println();
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
		msg += "Usage: " + _AppName + " <inPattern> <outPattern> [/debug] [/rootPath <root folder>] [/ri|/rangeInclusive <start index> <end index>] " +
				"[/ext <extension>] [mi|ma (mode=album or mode=image)] [/d|/digits <exact number of image/album digits>] [/renum <digits>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		final int maxToPrint = 10;

		_sourceSubFolder = appendSlash(_rootPath + "jroot/" + AlbumImageDao.getInstance ().getSubFolderFromImageName (_inPattern));
		_destSubFolder = appendSlash(_rootPath + "jroot/" + AlbumImageDao.getInstance ().getSubFolderFromImageName (_outPattern));

		_log.debug("_sourceSubFolder = " + _sourceSubFolder);
		_log.debug("_destSubFolder = " + _destSubFolder);

		List<String> sourceFileListInRoot = new VFileList(_rootPath, _inPattern, false).getFileList (ListMode.CompletePath);
		List<String> sourceFileListInSubFolder = new VFileList(_sourceSubFolder, _inPattern, false).getFileList (ListMode.CompletePath);

		List<String> destFileListInRoot = new VFileList(_rootPath, _outPattern, false).getFileList (ListMode.CompletePath);
		List<String> destFileListInSubFolder = new VFileList(_destSubFolder, _outPattern, false).getFileList (ListMode.CompletePath);

		_log.debug(printListSortedWithLimit("sourceFileListInRoot", sourceFileListInRoot, maxToPrint, NL));
		_log.debug(printListSortedWithLimit("sourceFileListInSubFolder", sourceFileListInSubFolder, maxToPrint, NL));

		_log.debug(printListSortedWithLimit("destFileListInRoot", destFileListInRoot, maxToPrint, NL));
		_log.debug(printListSortedWithLimit("destFileListInSubFolder", destFileListInSubFolder, maxToPrint, NL));

		boolean sourceFilesInRoot = sourceFileListInRoot.size() > 0;
		boolean sourceFilesInSubFolder = sourceFileListInSubFolder.size() > 0;

		if (!sourceFilesInRoot && !sourceFilesInSubFolder) {
			System.out.println("Error: no source files found in any of:" + NL
					+ _rootPath + NL + _sourceSubFolder);
			return false;

		} else if (sourceFilesInRoot && sourceFilesInSubFolder) {
//TODO improve this message
			System.out.println("Error: source files exist in both root and subFolders:" + NL
					+ sourceFileListInRoot.stream().sorted().collect(Collectors.joining(NL)) + NL
					+ sourceFileListInSubFolder.stream().sorted().collect(Collectors.joining(NL)));
			return false;
		}

		if (destFileListInRoot.size() > 0 || destFileListInSubFolder.size() > 0) {
//TODO improve this message
			System.out.println("Error: destination files already exist:" + NL
					+ destFileListInRoot.stream().sorted().collect(Collectors.joining(NL)) + NL
					+ destFileListInSubFolder.stream().sorted().collect(Collectors.joining(NL)));
			if (!_force) {
				return false;
			}
		}

		List<String> sourceFileListToUse = sourceFilesInRoot ? sourceFileListInRoot : sourceFileListInSubFolder;

		//filter based on _exactDigits and _startIndex and _endIndex
		List<String> filteredSourceFileList = filterSourceFileList(sourceFileListToUse);

		if (filteredSourceFileList.size() != sourceFileListToUse.size()) {
			Set<String> files = new TreeSet<>(sourceFileListToUse);
			files.removeAll(new TreeSet<>(filteredSourceFileList));
			_log.debug(printListSortedWithLimit("The following source files were removed by filter", files, maxToPrint, NL));
		}

		_log.debug(printListSortedWithLimit("filteredSourceFileList", filteredSourceFileList, maxToPrint, NL));

		if (filteredSourceFileList.size() == 0) {
//TODO improve this message
			System.out.println("Error: *filtered* source file list is empty: filter removed all files");
			return false;
		}

		if (filteredSourceFileList.size() == sourceFileListToUse.size()) {
			_log.info("No files filtered from original source file list");
		}

		List<VPair<String, String>> fileNamePairs = generateFileNamePairs (filteredSourceFileList);

		_log.info("fileNamePairs: [showing first " + maxToPrint + " of " + fileNamePairs.size() + "]");
		fileNamePairs.stream().sorted().limit(maxToPrint).forEach(System.out::println);

		try {
			moveFilesAndUpdateUndoCommands (fileNamePairs);

		} catch (Exception ee) {
			_log.error("Error from moveFilesAndUpdateUndoCommands, " + _filesProcessed  + " of " + fileNamePairs.size() + " files processed.", ee);
		}

		if (!_testMode) {
			_log.debug(printListSortedWithLimit("undoCommands", _undoCommands, maxToPrint, NL));

			writeUndoCommandsToFile();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<VPair<String, String>> generateFileNamePairs (List<String> inFileList) {
		ArrayList<VPair<String, String>> results = new ArrayList<>();

		List<String> inPatternParts = Arrays.asList (_inPattern.split ("\\*"));
		List<String> outPatternParts = Arrays.asList (_outPattern.split ("\\*"));

		if (inPatternParts.size() != 2 || outPatternParts.size() != 2) {
			throw new RuntimeException("Not implemented yet: more than one '*' in patterns");
		}

		int destIndexInt = 0;
		String format = _renumDigits != null ? "%0" + _renumDigits + "d" : "%d";

		final Pattern middlePattern = Pattern.compile(inPatternParts.get(0) + "(.*)" + inPatternParts.get(1));

		for (String inFile : inFileList) {
			Matcher middleMatcher = middlePattern.matcher(inFile);
			String middle = null;
			if (middleMatcher.find()) {
				middle = middleMatcher.group(1);
			}
			String outFile = _destSubFolder + outPatternParts.get(0) + middle + outPatternParts.get(1);

			if (_renumDigits != null) {
				if (_mode == Mode.RenameAlbum) {
					throw new RuntimeException("Not implemented yet: combination of /renum and /ma");
				}

				String destIndexString = String.format(format, ++destIndexInt);
				Matcher renumMatcher = _imageNamePattern.matcher(outFile);
				int groupToReplace = _mode == Mode.RenameAlbum ? 1 : 2;
				if (renumMatcher.find()) {
					outFile = new StringBuilder(outFile).replace(renumMatcher.start(groupToReplace), renumMatcher.end(groupToReplace), destIndexString).toString();
				} else {
					throw new RuntimeException("generateFileNamePairs: failed to match pattern for <" + outFile + ">");
				}
			}

			results.add(VPair.of(inFile, outFile));
		}

		return results;
	}

	///////////////////////////////////////////////////////////////////////////
	public int moveFilesAndUpdateUndoCommands (List<VPair<String, String>> fileNamePairs) throws Exception
	{
		for (VPair<String, String> fileNamePair : fileNamePairs) {
			boolean status = moveFile(fileNamePair.getFirst(), fileNamePair.getSecond());

			if (status) {
				_undoCommands.add(fixSlashes("move " + fileNamePair.getSecond() + " " + fileNamePair.getFirst()));
				_filesProcessed++;
			}
		}

		return _filesProcessed;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean moveFile (String srcName, String destName) throws Exception
	{
		Path src = FileSystems.getDefault ().getPath (srcName);
		Path dest = FileSystems.getDefault ().getPath (destName);

		if (Files.exists (dest)) {
			_log.error ("moveFile: destination file already exists: " + dest);
			return false;
		}

		if (_testMode) {
			System.out.println("TestMode: move " + srcName + " " + destName);
		} else {
			Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> filterSourceFileList (List<String> inList)
	{
		return inList.stream()
				.filter(s -> {
					String numberAsString = extractNumberFromFileName(s);
					if (_exactDigits != null && numberAsString.length() != _exactDigits) {
						return false;
					}
					int numberAsInt = Integer.parseInt(numberAsString);
					if (numberAsInt < _startIndex || numberAsInt > _endIndex) {
						return false;
					}
					return true;
				})
				.collect(Collectors.toList());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean writeUndoCommandsToFile () {
		String outFileName = "mov." + _dateFormat.format (new Date()) + ".bat";
		Path outFilePath = Paths.get(_rootPath, "tmp", outFileName);

		try (FileOutputStream outputStream = new FileOutputStream (outFilePath.toFile ())) {
			for (String undoCommand : _undoCommands) {
				outputStream.write((undoCommand + NL).getBytes());
			}
			outputStream.flush ();
			//outputStream.close (); will be closed by try-with-resources

		} catch (IOException ee) {
			_log.error ("writeUndoCommandsToFile: error writing undo file: " + outFilePath + NL);
			_log.error (ee); //print exception, but no stack trace
		}

		_log.debug ("writeUndoCommandsToFile: undo commands (" + _undoCommands.size() + ") written to file: " + outFilePath + NL);
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	// fileName is string in form: <image name><album number>-<image number>.<extension>
	public String extractNumberFromFileName (String fileName)
	{
		int group = _mode == Mode.RenameAlbum ? 1 : 2;
		Matcher matcher = _imageNamePattern.matcher(fileName);

		return matcher.find () ? matcher.group(group) : null;
	}

	///////////////////////////////////////////////////////////////////////////
	// returns null on success; otherwise error message
	private String validatePattern (String pattern)
	{
		if (pattern.split("\\*").length != 2) {
			return "Pattern <" + pattern + "> must have exactly one asterisk (*)";
		}

		return null;
	}

	///////////////////////////////////////////////////////////////////////////
	private String printListSortedWithLimit (String header, Collection<String> list, int limit, CharSequence delimiter)
	{
		if (list == null || list.isEmpty()) {
			return header + ": [empty]";
		}

		String string = list.stream().sorted(new AlphanumComparator()).limit(limit).collect(Collectors.joining(delimiter));
		return header + ": [showing first " + limit + " of " + list.size() + "]" + NL + string;
	}

	///////////////////////////////////////////////////////////////////////////
	//replace all forward slashes with backslashes
	private String fixSlashes (String path)
	{
		if (path.contains("/")) {
			return path.replaceAll("/", "\\\\"); //regex
		} else {
			return path;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private String appendSlash (String path)
	{
		if (path.endsWith("/") || path.endsWith("\\")) {
			return path;
		} else {
			return path + _slash;
		}
	}


	//members from command line
	private boolean _testMode = false;
	private boolean _force = false;
	private String _inPattern = null;
	private String _outPattern = null;
	private String _rootPath = null;
	private String _extension = null;
	private Integer _exactDigits = null;
	private Integer _renumDigits = null;
	private int _startIndex = 0;
	private int _endIndex = Integer.MAX_VALUE;
	private Mode _mode = Mode.RenameImage;

	public static boolean _Debug = false;

	//members NOT from command line
	private String _sourceSubFolder = null;
	private String _destSubFolder = null;
	private final Pattern _imageNamePattern = Pattern.compile ("\\D+(\\d+)-(\\d+)\\D+"); //imageName is string in form: <image name><album number>-<image number>.<extension>
//	private final Pattern _imagePattern = Pattern.compile ("-(\\d+)");
//	private final Pattern _albumPattern = Pattern.compile ("(\\d+)-");

	private int _filesProcessed = 0;
	private List<String> _undoCommands = new ArrayList<>();

	public static final String _AppName = "AlbumFileRename";
	public static final String _DefaultExtension = ".jpg";
	public static final String _slash = System.getProperty ("file.separator");
	public static final String NL = System.getProperty ("line.separator");

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //format as integer
	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("yyyyMMdd.HHmmss"); // Note SimpleDateFormat is not thread safe

	private static Logger _log = LogManager.getLogger ();
}
