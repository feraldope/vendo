//AlbumFileMigrate.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


public class AlbumFileMigrate
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args)
	{
		AlbumFileMigrate app = new AlbumFileMigrate();

		if (!app.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String[] args)
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("dir")) {
					try {
						_dir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("migrate")) {
					try {
						_migrate = true;
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

/*
				} else if (arg.equalsIgnoreCase ("height") || arg.equalsIgnoreCase ("h")) {
					try {
						_desiredHeight = Integer.parseInt (args[++ii]);
						if (_desiredHeight < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("query") || arg.equalsIgnoreCase ("q")) {
					_queryMode = true;

				} else if (arg.equalsIgnoreCase ("scalePercent") || arg.equalsIgnoreCase ("sc")) {
					try {
						_scalePercent = Integer.parseInt (args[++ii]);
						if (_scalePercent < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("suffix") || arg.equalsIgnoreCase ("su")) {
					try {
						_nameSuffix = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("width") || arg.equalsIgnoreCase ("w")) {
					try {
						_desiredWidth = Integer.parseInt (args[++ii]);
						if (_desiredWidth < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
*/
/*
				} else if (arg.equalsIgnoreCase ("max") || arg.equalsIgnoreCase ("m")) {
					try {
						_maxMissedFiles = Integer.parseInt (args[++ii]);
						if (_maxMissedFiles < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
					_switches.add ("/max " + _maxMissedFiles);

				} else if (arg.equalsIgnoreCase ("strip")) { // || arg.equalsIgnoreCase ("strip")) {
					_stripHead = true;
					_switches.add ("/strip");

				} else if (arg.equalsIgnoreCase ("ignore") || arg.equalsIgnoreCase ("i")) {
					_ignoreHistory = true;

				} else if (arg.equalsIgnoreCase ("checkOnly") || arg.equalsIgnoreCase ("co")) {
					_checkHistoryOnly = true;
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_inPattern == null) {
					_inPattern = arg;

				} else if (_outPattern == null) {
					_outPattern = arg;

/*
				} else if (_outputPrefix == null) {
					_outputPrefix = arg;
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (!_migrate) {
			if (_inPattern == null) {
				displayUsage ("<inPattern> not specified", true);
			}

			if (_outPattern == null) {
				displayUsage ("<outPattern> not specified", true);
			}
		}

//TODO - verify _destDir exists, and is writable??
		if (_dir == null) {
			_dir = VendoUtils.getCurrentDirectory ();
		}
		_dir = VendoUtils.appendSystemSlash (_dir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

//ju <infile> [/subDirs] [/outFile <outfile>] [/width <width (possibly negative)>] [/height <height (possibly negative)>]

//		msg += "Usage: " + _AppName + " [/debug] [/strip] [/ignore] [/dest <dest dir>] [/start <start index>] [/block <block number>] [/maxMissedFiles <count>] [/digits <number>] [/pad <number>] [/prefix <numberPrefix>] <URL> <output prefix>";
		msg += "Usage: " + _AppName + " [/debug] ... TBD";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
//		if (_Debug)
//			_log.debug ("AlbumFileRename.run");

		if (_migrate) {
			return doMigrate ();
		}

		_log.debug ("_inPattern = " + _inPattern);
		_log.debug ("_outPattern = " + _outPattern);

		ArrayList<VPair<String, String>> results = moveFiles (_inPattern, _outPattern);

		for (VPair<String, String> result : results) {
			System.out.println (result);
		}

/*
		if (_queryMode) {
			for (String filename : filenames) {
				ImageAttributes imageAttributes = getImageAttributes (filename);
				System.out.println (imageAttributes);
//				return imageAttributes;
//				String results = queryImage (filename);
			}

			return true;
		}

		if (_desiredWidth > 0 || _desiredHeight > 0) {
			for (String filename : filenames) {
				System.out.println (filename);
				String outFilename = generateOutFilename (filename, _nameSuffix);

				if (outFilename.compareToIgnoreCase (filename) == 0) {
					_log.error ("run: output file name '" + outFilename + "' is the same as input file name (need to specify /suffix)");
					continue;
				}

				if (fileExists (outFilename)) {
					_log.error ("run: output file '" + outFilename + "' already exists");
					continue;
				}

				System.out.println (outFilename);

				generateScaledImage (filename, outFilename, _desiredWidth, _desiredHeight);
			}
		}

		if (_scalePercent > 0) {
			double scaleFactor = _scalePercent / 100.;

			for (String filename : filenames) {
				System.out.println (filename);
				String outFilename = generateOutFilename (filename, _nameSuffix);

				if (outFilename.compareToIgnoreCase (filename) == 0) {
					_log.error ("run: output file name '" + outFilename + "' is the same as input file name (need to specify /suffix)");
					continue;
				}

				if (fileExists (outFilename)) {
					_log.error ("run: output file '" + outFilename + "' already exists");
					continue;
				}

				System.out.println (outFilename);

				ImageAttributes imageAttributes = getImageAttributes (filename);

				int width = -1;
				int height = -1;

				if (imageAttributes._width > imageAttributes._height)
					width = (int) ((double)(imageAttributes._width + 1) * scaleFactor);
				else
					height = (int) ((double)(imageAttributes._height + 1) * scaleFactor);

				generateScaledImage (filename, outFilename, width, height);
			}
		}
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean doMigrate () {
		//CLI overrides
		AlbumFormInfo._Debug = true;
		AlbumFormInfo._logLevel = 5;
		AlbumFormInfo._profileLevel = 5;

		AlbumProfiling.getInstance ().enterAndTrace (1);

		List<AlbumImage> imageDisplayList = getImageDisplayList ();

		int oldSubFolderLength = 1;
		int newSubFolderLength = 2;
		Map<String, String> subFolderMap = getSubFolderMap (imageDisplayList, oldSubFolderLength, newSubFolderLength);

		moveImages1 (subFolderMap, imageDisplayList);

		AlbumProfiling.getInstance ().exit (1);

		if (_Debug) {
			AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ true);
			_log.debug ("--------------- AlbumFileRename.doMigrate - done ---------------");
		}

		return true;
}

	///////////////////////////////////////////////////////////////////////////
	//note: not sorted
	private List<AlbumImage> getImageDisplayList () {
		AlbumProfiling.getInstance ().enterAndTrace (1);

		final String[] filters = new String[] {"*"};
		final AlbumFileFilter filter = new AlbumFileFilter (filters, null, /*useCase*/ false, /*sinceInMillis*/ 0);

		List<AlbumImage> imageDisplayList = new LinkedList<> ();

		Collection<String> subFolders = AlbumImageDao.getInstance ().getAlbumSubFolders ();
		//debug override
//		subFolders = Arrays.asList ("u", "w", "o"); //smallest folders
//		subFolders = Arrays.asList ("u");

		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());
		final Set<String> debugNeedsChecking = new ConcurrentSkipListSet<> ();
		final Map<String, Integer> debugCacheMiss = new ConcurrentHashMap<>();

		AlbumProfiling.getInstance ().enter (5, "getImageDisplayList.doDir");
		for (final String subFolder : subFolders) {
			new Thread (() -> {
				final Collection<AlbumImage> imageDisplayList1 = AlbumImageDao.getInstance ().doDir (subFolder, filter, debugNeedsChecking, debugCacheMiss);
				if (imageDisplayList1.size () > 0) {
					synchronized (imageDisplayList) {
						imageDisplayList.addAll (imageDisplayList1);
					}
				}
				endGate.countDown ();
			}).start ();
		}
		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumFileRename.getImageDisplayList: endGate:", ee);
		}
		AlbumProfiling.getInstance ().exit (5, "getImageDisplayList.doDir");

		_log.debug ("AlbumFileRename.getImageDisplayList: _imageDisplayList.size = " + _decimalFormat2.format (imageDisplayList.size ()));

		AlbumProfiling.getInstance ().exit (1);

		return imageDisplayList;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns a map of {newSubfolderName -> oldSubfolderName}
	private Map<String, String> getSubFolderMap (List<AlbumImage> imageDisplayList, int oldSubFolderLength, int newSubFolderLength) {
		AlbumProfiling.getInstance ().enterAndTrace (1);

		Set<String> subFolderSet = //use set to eliminate duplicates
				imageDisplayList.stream ()
								.map (i -> i.getName ().substring (0, newSubFolderLength).toLowerCase ())
								.collect (Collectors.toSet ());

		Map<String, String> subFolderMap =
				subFolderSet.stream ()
							.collect (Collectors.toMap (j -> j, j -> j.substring (0, oldSubFolderLength)));

		_log.debug ("AlbumFileRename.getSubFolderMap: subFolderMap = " + subFolderMap);
		_log.debug ("AlbumFileRename.getSubFolderMap: subFolderMap.size = " + subFolderMap.size ());

		AlbumProfiling.getInstance ().exit (1);

		return subFolderMap;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean moveImages1 (final Map<String, String> subFolderMap, final List<AlbumImage> imageDisplayList) {
		AlbumProfiling.getInstance ().enterAndTrace (1);

		final CountDownLatch endGate = new CountDownLatch (subFolderMap.keySet ().size ());

		for (final String newSubFolder : subFolderMap.keySet ()) {
			new Thread (() -> {
				final List<String> imageNames =
						imageDisplayList.stream ()
//										.map (i -> i.getBaseName(/*collapseGroups*/ false))
										.map (i -> i.getName())
										.filter (i -> i.substring (0, newSubFolder.length ()).equalsIgnoreCase (newSubFolder))
										.collect (Collectors.toList ());

				String oldSubFolder = subFolderMap.get (newSubFolder);
				moveImages2 (oldSubFolder, newSubFolder, imageNames);
				endGate.countDown ();
			}).start ();
		}
		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumFileRename.moveImages1: endGate:", ee);
		}

		AlbumProfiling.getInstance ().exit (1);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean moveImages2 (String oldSubFolder, String newSubFolder, List<String> imageNames) {
//		AlbumProfiling.getInstance ().enterAndTrace (1);

		_log.debug ("AlbumFileRename.moveImages2: oldSubFolder = " + oldSubFolder + ", newSubFolder = " + newSubFolder);// + ", imageNames = " + imageNames);

		final String rootPath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ false);// + subFolder;

		Path newSubfolderPath = FileSystems.getDefault ().getPath (rootPath, newSubFolder);
		if (!createSubfolder (newSubfolderPath.toString ())) {
			return false;
		}

		for (String imageName : imageNames) {
			Path srcPath = FileSystems.getDefault ().getPath (rootPath, oldSubFolder, imageName);
			Path destPath = FileSystems.getDefault ().getPath (rootPath, newSubFolder, imageName);

			_log.debug ("AlbumFileRename.moveImages2: srcPath = " + srcPath + ", destPath = " + destPath);// + ", imageNames = " + imageNames);

//			if (imageName.contains("UmaA01-0")) {
				moveFile (srcPath + ".jpg", destPath + ".jpg");
				moveFile (srcPath + ".dat", destPath + ".dat");
//			}
		}

//		AlbumProfiling.getInstance ().exit (1);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean moveFile (String srcName, String destName)
	{
		Path src = FileSystems.getDefault ().getPath (srcName);
		Path dest = FileSystems.getDefault ().getPath (destName);

		if (Files.exists (dest)) {
			_log.error ("AlbumFileRename.moveFile: destination file already exists: " + dest.toString ());
			return false;
		}

		try {
			Files.move (src, dest);

		} catch (Exception ee) {
			_log.error ("AlbumFileRename.moveFile: error moving file (" + src.toString () + " to " + dest.toString () + ")", ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private synchronized static boolean createSubfolder (String fullPath)
	{
		Path path = Paths.get(fullPath);

		if (Files.exists (path)) {
			return true;
		}

		try {
		    Files.createDirectories(path);

		} catch (Exception ee) {
			_log.error ("AlbumFileRename.createSubfolder: error creating subFolder: " + fullPath, ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
//	public VPair<String, String>[] moveFiles (String inPattern, String outPattern)
	public ArrayList<VPair<String, String>> moveFiles (String inPattern, String outPattern)
	{
//		String[] imageFiles = new String[] {};

//		VPair<String, String>[] results = new VPair<String, String>[] { VPair.of ("", "") };
//		VPair<String, String>[] results = new VPair<String, String>[] {};
//		VPair<String, String>[] results = null;
//		VPair<?, ?>[] results = new VPair<String, String>[] {};
		ArrayList<VPair<String, String>> results = new ArrayList<> ();

		throw new RuntimeException("moveFiles: not implemented");
//see:
//http://stackoverflow.com/questions/7131652/generic-array-creation-error

/*
		String[] infilenames = getFileList (_dir, _inPattern);

		if (infilenames.length == 0) {
			_log.error ("run: no files matching '" + _inPattern + "' found");
			return true;
		}

		for (String infilename : infilenames) {
			System.out.println (infilename);
		}

		return results;
*/
	}

	///////////////////////////////////////////////////////////////////////////
	public String[] getFileList (String dirName, final String nameWild)
	{
		//for simple dir listings, it looks like java.io package is faster than java.nio
		FilenameFilter filenameFilter = new FilenameFilter () {
			@Override
			public boolean accept (File dir, String name) {
				return VendoUtils.matchPattern (name, nameWild);
			}
		};

		File dir = new File (dirName);
		String[] filenames = dir.list (filenameFilter);

		dirName = VendoUtils.appendSystemSlash (dirName);
		for (int ii = 0; ii < filenames.length; ii++) {
			filenames[ii] = dirName + filenames[ii];
		}

		return filenames;
	}


	//members
	private String _inPattern = null;
	private String _outPattern = null;
	private String _dir = null;

	private boolean _migrate = false;

	public static boolean _Debug = false;

	public static final String _AppName = "AlbumFileRename";
//	public static final String _slash = System.getProperty ("file.separator");
	public static final String NL = System.getProperty ("line.separator");

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //format as integer

	private static Logger _log = LogManager.getLogger ();
}
