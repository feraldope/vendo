//AlbumFileMigrate.java - migrate album image files to new folders (for example, from 2-char folder like "zz" to 3-char folder like "zzz")

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VUncaughtExceptionHandler;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class AlbumFileMigrate {
	///////////////////////////////////////////////////////////////////////////
	public static void main(String[] args) {
		AlbumFileMigrate albumFileMigrate = new AlbumFileMigrate();

		if (!albumFileMigrate.processArgs(args)) {
			System.exit(1); //processArgs displays error
		}

		String error = albumFileMigrate.checkForRunningTomcatProcess();
		if (error != null) {
			System.out.println(error);
			System.exit(1);
		}

		error = albumFileMigrate.checkForJunctions();
		if (error != null) {
			System.out.println(error);
			System.exit(1);
		}

		try {
			albumFileMigrate.run();

		} catch (Exception ee) {
			log.error("AlbumFileMigrate.main: ", ee);
		}

		log.debug("AlbumFileMigrate.main - leaving main");
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs(String[] args) {
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					Debug = true;

				} else if (arg.equalsIgnoreCase ("subFolder")) {
					try {
						subFolder = args[++ii].toLowerCase();
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("prefix")) {
					try {
						prefix = args[++ii].toLowerCase();
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

//				} else if (arg.equalsIgnoreCase ("migrate")) {
//					try {
//						_migrate = true;
//					} catch (ArrayIndexOutOfBoundsException exception) {
//						displayUsage ("Missing value for /" + arg, true);
//					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

//			} else {
//				//check for other args
//				if (_inPattern == null) {
//					_inPattern = arg;
//
//				} else {
//					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args and handle defaults

		final Predicate<String> validString = Pattern.compile ("^[a-z]{1,4}$").asPredicate (); //valid: between 3 and 5 lower case letters

		if (subFolder == null || !validString.test(subFolder)) {
			displayUsage ("Invalid value for <subFolder>: " + subFolder, true);
		}

		if (prefix == null || !validString.test(prefix)) {
			displayUsage ("Invalid value for <prefix>: " + prefix, true);
		}

		if (prefix == null || !prefix.startsWith(subFolder) || prefix.length() <= subFolder.length() ) {
			displayUsage ("prefix and folder do not meet requirements", true);
		}

		log.debug("subFolder = " + subFolder);
		log.debug("prefix = " + prefix);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage(String message, Boolean exit) {
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + AppName + " [/debug] /subFolder <source subFolder> /prefix <prefix to match image file names>" + NL +
				"Example: albumFileMigrate ca can";
		System.err.println("Error: " + msg + NL);

		if (exit) {
			System.exit(1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//returns error String on error, null on success
	private String checkForRunningTomcatProcess() {
		if (!VendoUtils.isWindowsOs()) {
			return "Error: this currently only runs on windows due to windows-specific command";
		}

		String getProcessCommand = System.getenv("windir") + "\\system32\\tasklist.exe";
		List<String> commandOutput = VendoUtils.executeCommand(getProcessCommand, log);
		if (commandOutput == null) {
			return "Error: unable to get list of processes";
		}

		final Predicate<String> servletProcessPattern = Pattern.compile (".*Tomcat8.exe.*", Pattern.CASE_INSENSITIVE).asPredicate ();
		List<String> matchingProcesses = commandOutput.stream()
				.filter(servletProcessPattern)
				.collect(Collectors.toList());

		if (!matchingProcesses.isEmpty()) {
			return "Error: Tomcat servlet process is running: " + NL + String.join(NL, matchingProcesses);
		}

		return null; //process not running
	}

	///////////////////////////////////////////////////////////////////////////
	//if the source is a JUNCTION and the dest is not, then we can't *move* the files, they will need to be copied, which we want to avoid
	//returns error String on error, null on success
	private String checkForJunctions() {
		if (!VendoUtils.isWindowsOs()) {
			return "Error: this currently only runs on windows due to windows-specific command";
		}

		//example output of following "dir" command for junctions and regular folders:
		// 10/19/2025  03:34 PM    <JUNCTION>     ka [E:\Netscape.secondary\Program\jroot\ka]
		// 12/15/2025  08:16 AM    <DIR>          kar
		// 10/19/2025  03:34 PM    <JUNCTION>     kat [E:\Netscape.secondary\Program\jroot\kat]
		// 12/31/2025  03:40 PM    <DIR>          kate
		// 12/31/2025  04:24 PM    <JUNCTION>     kati [E:\Netscape.secondary\Program\jroot\kati]

		String getProcessCommand = "cmd.exe /c dir /ad D:\\Netscape\\Program\\jroot";
		List<String> commandOutput = VendoUtils.executeCommand(getProcessCommand, log);
		if (commandOutput == null) {
			return "Error: unable to get dir list";
		}

		if (commandOutput.size() < 300) { //hardcoded
			return "Error: dir list likely incomplete, only " + commandOutput.size() + " items";
		}

		final Predicate<String> subFolderJunctionPattern = Pattern.compile (".*<JUNCTION>\\s+" + subFolder + "\\s.*", Pattern.CASE_INSENSITIVE).asPredicate ();
		final Predicate<String> prefixJunctionPattern = Pattern.compile (".*<JUNCTION>\\s+" + prefix + "\\s.*", Pattern.CASE_INSENSITIVE).asPredicate ();
		boolean subFolderIsJunction = commandOutput.stream().anyMatch(subFolderJunctionPattern);
		boolean prefixIsJunction = commandOutput.stream().anyMatch(prefixJunctionPattern);

		if (subFolderIsJunction && !prefixIsJunction) {
			StringBuilder sb = new StringBuilder(NL);
			sb.append("Error: source subFolder is a JUNCTION but destination is not.").append(NL);
			sb.append("Fix this by running the following commands:").append(NL);
			sb.append("md E:\\Netscape.secondary\\Program\\jroot\\").append(prefix).append(NL);
			sb.append("mklink /J D:\\Netscape\\Program\\jroot\\").append(prefix).append(" E:\\Netscape.secondary\\Program\\jroot\\").append(prefix).append(NL);
			return sb.toString();
		}

		if (!subFolderIsJunction && prefixIsJunction) {
			return "Error: prefix is a JUNCTION but subFolder is not";
			//how to fix?
		}

		return null; //success
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run () {
//		if (Debug) {
//			log.debug("AlbumFileMigrate.run");
//		}

		boolean status = moveFiles();

		if (status) {
			System.out.println();
			System.out.println(">>> You must update DB with new folder, with command like the following:");
			System.out.println("mysql -u root -proot albumimages -e \"update images set sub_folder = '" + prefix + "' where lower(name_no_ext) like '" + prefix + "%' AND sub_folder = '" + subFolder + "'\"");
			System.out.println();
			System.out.println("AND");
			System.out.println();
			System.out.println("uds.bat " + subFolder + "*");
			System.out.println();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean moveFiles() {
		if (Debug) {
			log.debug("AlbumFileMigrate.moveFiles");
		}

		final Path sourcePathForJpgFilesonD = FileSystems.getDefault().getPath("D:/Netscape/Program/jroot", subFolder);
		final Path destPathForJpgFilesonD   = FileSystems.getDefault().getPath("D:/Netscape/Program/jroot", prefix);
		final Path sourcePathForDatFilesonD = FileSystems.getDefault().getPath(sourcePathForJpgFilesonD.toString(), "dat");
		final Path destPathForDatFilesonD   = FileSystems.getDefault().getPath(destPathForJpgFilesonD.toString(), "dat");

		final Path sourcePathForJpgFilesonB = FileSystems.getDefault().getPath("B:/Netscape.backup/jroot", subFolder);
		final Path destPathForJpgFilesonB   = FileSystems.getDefault().getPath("B:/Netscape.backup/jroot", prefix);

		//do B: first, since it is the most likely to fail
		if (!createSubfolder(destPathForJpgFilesonB)) {
			return false; //createSubfolder prints error
		}
		if (!createSubfolder(destPathForJpgFilesonD)) {
			return false; //createSubfolder prints error
		}
		if (!createSubfolder(destPathForDatFilesonD)) {
			return false; //createSubfolder prints error
		}

		final Collection<AlbumImageFileDetails> albumImageFileDetails = AlbumImageDao.getInstance().getImageFileDetailsFromFileSystem(subFolder, "", ".jpg"); //result is sorted

		final Predicate<String> prefixPattern = Pattern.compile ("^" + prefix, Pattern.CASE_INSENSITIVE).asPredicate ();

		final List<String> matchingFiles = albumImageFileDetails.stream()
																.map(AlbumImageFileDetails::getName)
																.filter(prefixPattern)
																.collect(Collectors.toList());
		final int expectedFileCount = matchingFiles.size();
		log.debug("AlbumFileMigrate.moveFiles: " + decimalFormat0.format(expectedFileCount) + " source files found");
		if (expectedFileCount == 0) {
			return false;
		}

		List<Thread> threads = new ArrayList<>();

		//start three threads for the three folders

		Thread moveFilesThread1 = new Thread (new MoveFilesTask(new ArrayList<>(matchingFiles), sourcePathForJpgFilesonD, destPathForJpgFilesonD, ".jpg", expectedFileCount));
		moveFilesThread1.start ();
		threads.add (moveFilesThread1);

		Thread moveFilesThread2 = new Thread (new MoveFilesTask(new ArrayList<>(matchingFiles), sourcePathForDatFilesonD, destPathForDatFilesonD, ".dat", expectedFileCount));
		moveFilesThread2.start ();
		threads.add (moveFilesThread2);

		Thread moveFilesThread3 = new Thread (new MoveFilesTask(new ArrayList<>(matchingFiles), sourcePathForJpgFilesonB, destPathForJpgFilesonB, ".jpg", expectedFileCount));
		moveFilesThread3.start ();
		threads.add (moveFilesThread3);

		//wait for all threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();

			} catch(Exception ex) {
				log.error("AlbumFileMigrate.moveFiles: thread.join failed");
				log.error(ex);
			}
		}

		for (Thread thread : threads) {
			if (thread.isAlive()) {
				log.error("AlbumFileMigrate.moveFiles: thread is still running: " + thread.getName());
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private synchronized boolean createSubfolder(Path fullPath) {
		if (Files.exists (fullPath)) {
			if (Files.isDirectory(fullPath)) {
				return true;
			} else {
				log.error ("AlbumFileMigrate.createSubfolder: subFolder exists, but is not a directoy: " + fullPath);
				return false;
			}
		}

		try {
		    Files.createDirectories(fullPath);

		} catch (Exception ee) {
			log.error ("AlbumFileMigrate.createSubfolder: error creating subFolder: " + fullPath, ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class MoveFilesTask implements Runnable {
		///////////////////////////////////////////////////////////////////////////
		public MoveFilesTask(List<String> matchingFiles, Path sourcePath, Path destPath, String extension, int expectedFileCount) {
			this.matchingFiles = matchingFiles;
			this.sourcePath = sourcePath;
			this.destPath = destPath;
			this.extension = extension;
			this.expectedFileCount = expectedFileCount;

			Thread.currentThread().setName("source: " + sourcePath);
			Thread.currentThread().setUncaughtExceptionHandler (new VUncaughtExceptionHandler());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public void run() {
			Instant startInstant = Instant.now ();

			AtomicInteger filesMoved = new AtomicInteger(0);
			matchingFiles.forEach(f -> {
				final Path fullSourcePath = FileSystems.getDefault().getPath(sourcePath.toString(), f + extension);
				final Path fullDestPath = FileSystems.getDefault().getPath(destPath.toString(), f + extension);
				if (moveFile(fullSourcePath, fullDestPath)) {
					filesMoved.incrementAndGet();
				}
			});

			String elapsed = "Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (dateTimeFormatter) + " for source: " + sourcePath;

			if (filesMoved.get() != expectedFileCount) {
				log.error("AlbumFileMigrate.moveFiles: files moved (" + filesMoved.get() + ") is not equal to expected file count (" + expectedFileCount + ") for source " + sourcePath + NL + elapsed);
			} else {
				System.out.println(decimalFormat0.format(filesMoved) + " " + extension + " files moved from source: " + sourcePath + NL + elapsed);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		private boolean moveFile(Path fullSourcePath, Path fullDestPath) {
			if (Files.exists (fullDestPath)) {
				log.error ("AlbumFileMigrate.moveFile: destination file already exists: " + fullDestPath);
				return false;
			}

			try {
				Files.move (fullSourcePath, fullDestPath, StandardCopyOption.ATOMIC_MOVE); //note to future: if ATOMIC_MOVE causes an exception, make sure the source (or dest) isn't a junction
//				System.out.println("move: " + fullSourcePath + " -> " + fullDestPath);

			} catch (Exception ee) {
				log.error ("AlbumFileMigrate.moveFile: error moving file (" + fullSourcePath + " to " + fullDestPath + ")");
				return false;
			}

			return true;
		}

		//members
		final private List<String> matchingFiles;
		final private Path sourcePath;
		final private Path destPath;
		final private String extension;
		final private int expectedFileCount;
	}


	//members
	private String subFolder = null;
	private String prefix = null;

	private ExecutorService executor = null;

	private static final DecimalFormat decimalFormat0 = new DecimalFormat ("###,##0"); //format as integer
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern ("HH'h':mm'm':ss's'"); //for example: 01h:03m:12s (note this wraps values >= 24 hours)

	public static boolean Debug = false;

	public static final String AppName = "AlbumFileMigrate";
	public static final String NL = System.getProperty ("line.separator");

	private static final Logger log = LogManager.getLogger ();
}
