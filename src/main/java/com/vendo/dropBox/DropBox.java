//DropBox.java - encrypt and zip files for storage in dropbox

package com.vendo.dropBox;

import com.vendo.vendoUtils.VCrypto;
import com.vendo.vendoUtils.VZipper;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Comparator;


public class DropBox
{
	public enum Action {ListAction, PutAction, GetAction, DeleteAction}

	public enum Mode {EncodeMode, ClearMode}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		DropBox dropBox = new DropBox ();

		if (!dropBox.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		dropBox.run ();
	}

//TODO -- add /rename capability?

	///////////////////////////////////////////////////////////////////////////
	public DropBox ()
	{
		String userName = System.getProperty ("user.name");

//TODO - folder selection based on OS version might not be correct - may need to look at various folders until we find one that exists
		if (VendoUtils.isWindowsOs ()) {
			if (VendoUtils.getOsVersion () == 10) {
				_dropboxFolder = new String ("C:\\Users\\" + userName + "\\Dropbox");
			} else {
				_dropboxFolder = new String ("C:\\Documents and Settings\\" + userName + "\\My Documents\\My Dropbox");
			}

		} else if (VendoUtils.isMacOs ()) {
			_dropboxFolder = new String ("/Users/" + userName + "/Dropbox");

//		} else {
//			no default folder; user must specify on command line
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("folder") || arg.equalsIgnoreCase ("dropbox")) {
					try {
						_dropboxFolder = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("put") || arg.equalsIgnoreCase ("putFile")) {
					_action = Action.PutAction;
					try {
						_userFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("get") || arg.equalsIgnoreCase ("getFile")) {
					_action = Action.GetAction;
					try {
						_userFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("del") || arg.equalsIgnoreCase ("delete")) {
					_action = Action.DeleteAction;
					try {
						_userFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("list") || arg.equalsIgnoreCase ("listFiles")) {
					_action = Action.ListAction;

				} else if (arg.equalsIgnoreCase ("clear") || arg.equalsIgnoreCase ("clearMode")) {
					_mode = Mode.ClearMode;

				} else if (arg.equalsIgnoreCase ("force") || arg.equalsIgnoreCase ("f")) {
					_force = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_xmlFilename == null)
					_xmlFilename = arg;

				else
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//handle defaults
/*
		if (_filename == null)
			_filename = "dope.txt";
*/

		//verify required args

		if (_Debug) {
			System.out.println ("_dropboxFolder = " + quote (_dropboxFolder));
		}

		try {
			File folder = new File (_dropboxFolder);
			if (folder.exists ()) {
				_dropboxFolder = folder.getCanonicalPath ();
			} else {
				displayUsage ("DropBox folder '" + _dropboxFolder + "' does not exist", true);
			}

		} catch (Exception ee) {
			_log.error ("file.exists: failed on '" + _dropboxFolder + "'", ee);
		}

		if (_action == Action.ListAction) {
			return true; //done processing args
		}

		generateDropboxFilename ();

		if (_Debug) {
			System.out.println ("_userFileBase = " + quote (_userFileBase));
			System.out.println ("_userFilename = " + quote (_userFilename));
			System.out.println ("_dropboxFileBase = " + quote (_dropboxFileBase));
			System.out.println ("_dropboxFilename = " + quote (_dropboxFilename));
		}

		if (_action == Action.GetAction) {
			if (!fileExists (_dropboxFilename)) {
				displayUsage ("Source file '" + _userFileBase + "' does not exist in DropBox folder (" + _dropboxFilename + ")", true);
			}

			if (fileExists (_userFilename) && !_force) {
				displayUsage ("Destination file '" + _userFilename + "' exists; use /force to overwrite", true);
			}
		}

		if (_action == Action.PutAction) {
			if (!fileExists (_userFilename)) {
				displayUsage ("Source file '" + _userFilename + "' does not exist", true);
			}

			if (fileExists (_dropboxFilename) && !_force) {
				displayUsage ("Destination file '" + _userFileBase + "' exists in DropBox folder (" + _dropboxFilename + "); use /force to overwrite", true);
			}
		}

		if (_action == Action.DeleteAction) {
			if (!fileExists (_dropboxFilename)) {
				displayUsage ("File '" + _userFilename + "' does not exist in DropBox folder (" + _dropboxFilename + ")", true);
			}
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

		msg += "Usage: " + _AppName + " {/list | {/get | /put | /del} <filename> [/force] [/clear] [/folder <dropbox>] [/debug]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean fileExists (String filename)
	{
		try {
			File file = new File (filename);
			if (file.exists ()) {
				return true;
			}

		} catch (Exception ee) {
			_log.error ("fileExists: failed on '" + filename + "'", ee);
			return true;
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		try {
			if (_action == Action.ListAction) {
				listFiles ();
			} else if (_action == Action.GetAction) {
				getFile ();
			} else if (_action == Action.PutAction) {
				putFile ();
			} else if (_action == Action.DeleteAction) {
				deleteFile ();
			}

		} catch (InvalidKeyException ee) {
			//this happens when the security policy jars in "%JAVA_HOME%\jre\lib\security" are incorrect
			//go to http://java.sun.com/javase/downloads/index.jsp and download jce_policy-6.zip
			//JCE Unlimited Strength Jurisdiction Policy Files 6 Release Candidate
			// - OR -
			//the "%JAVA_HOME%\jre\lib\ext\bcprov-jdk16*.jar" is missing
			//download it from http://www.bouncycastle.org
			_log.error ("run: error: Illegal key size or default parameters");
			_log.error ("The security policy jars are not compatible", ee);

		} catch (Exception ee) {
			_log.error ("run: error in processing", ee);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void generateDropboxFilename ()
	{
		//remove path from _userFilename
		try {
			File file = new File (_userFilename);
			_userFilename = new String (file.getCanonicalPath ());
			_userFileBase = new String (file.getName ());

		} catch (Exception ee) {
			_log.error ("generateDropboxFilename: failed to remove path from '" + _userFilename + "'", ee);
		}

		if (_mode == Mode.EncodeMode) {
			_dropboxFileBase = _filePrefix;
			for (int ii = 0; ii < _userFileBase.length (); ii++) {
				int ch = (int) _userFileBase.charAt (ii);
				_dropboxFileBase += Integer.toHexString (ch);
			}
			_dropboxFileBase += _fileExtension;

		} else { //_mode == Mode.ClearMode
			_dropboxFileBase = _userFileBase;
		}

		_dropboxFilename = _dropboxFolder + _slash + _dropboxFileBase;
	}

	///////////////////////////////////////////////////////////////////////////
	private String translateDropboxFilename (String in)
	{
		if (_mode == Mode.ClearMode) {
			return new String (in);
		}

		byte[] bytes = new byte [256];
//TODO - verify string with String.startsWith, String.endsWith

		int start = _filePrefix.length ();
		int end = in.length () - _fileExtension.length ();

		int jj = 0;
		for (int ii = start; ii < end; ii += 2) {

			int i0 = (int) in.charAt (ii);
			if (i0 >= '0' && i0 <= '9') {
				i0 -= (char) '0';
			} else {
				i0 -= (char) 'a' - 10;
			}

			int i1 = (int) in.charAt (ii + 1);
			if (i1 >= '0' && i1 <= '9') {
				i1 -= (char) '0';
			} else {
				i1 -= (char) 'a' - 10;
			}

			int h1 = i0 * 16 + i1;
//			System.out.println ("i0 = " + i0 + ", i1 = " + i1 + ", h1 = " + h1);

			bytes[jj++] = (byte) h1;
		}

		return new String (bytes, 0, jj);
	}

	///////////////////////////////////////////////////////////////////////////
	private void listFiles ()
	{
		File dir = new File (_dropboxFolder);
		String[] files = dir.list ();

		if (files == null) {
			System.out.println ("No files found");
			return;
		}

		final String pattern = _filePrefix + "*" + _fileExtension;
		final String format = "%-25s";

		String[] sorted = new String [files.length];

		for (int ii = 0; ii < files.length; ii++) {
			String filename = files[ii];//.trim (); //remove carriage control chars in files (Mac)

			if (VendoUtils.matchPattern (filename, pattern)) {
				sorted[ii] = String.format (format, translateDropboxFilename (filename)) + "(" + filename + ")";

			} else {
				File file = new File (_dropboxFolder + _slash + filename);
				String description = (file.isDirectory () ? "(directory)" : "(clear file)");
				sorted[ii] = String.format (format, filename) + description;
			}
		}

		Arrays.sort (sorted, new Comparator<String> () {
			@Override
			public int compare (String s1, String s2) {
				return s1.compareToIgnoreCase (s2);
			}
		});

		System.out.println (NL + "Listing for " + quote (_dropboxFolder) + NL);

		for (int ii = 0; ii < sorted.length; ii++) {
			System.out.println (sorted[ii]);
		}

		System.out.println (NL + sorted.length + " files found" + NL);
	}

	///////////////////////////////////////////////////////////////////////////
	private void getFile () throws Exception
	{
		if (fileExists (_userFilename)) {
			backupFile (_userFilename);
		}

		File inFile = new File (_dropboxFilename);
		FileInputStream inStream = new FileInputStream (inFile);

//TODO - does this always represent the number of bytes in the stream??
		int available = inStream.available ();
		byte[] bytes = new byte [available];
		inStream.read (bytes, 0, available);
		inStream.close ();

		if (_mode == Mode.EncodeMode) {
			VCrypto crypto = new VCrypto ();
			VZipper zipper = new VZipper ();

			bytes = crypto.decryptBytes (bytes);
			bytes = zipper.decompressBytes (bytes);
		}

		File outFile = new File (_userFilename);
		FileOutputStream outStream = new FileOutputStream (outFile);

		outStream.write (bytes, 0, bytes.length);
		outStream.close ();
	}

	///////////////////////////////////////////////////////////////////////////
	private void putFile () throws Exception
	{
		File inFile = new File (_userFilename);
		FileInputStream inStream = new FileInputStream (inFile);

//TODO - does this always represent the number of bytes in the stream??
		int available = inStream.available ();
		if (_Debug) {
			System.out.println ("DropBox.putFile: inStream bytes = " + available);
		}

		byte[] bytes = new byte [available];
		inStream.read (bytes, 0, available);
		inStream.close ();

		if (_mode == Mode.EncodeMode) {
			VZipper zipper = new VZipper ();
			VCrypto crypto = new VCrypto ();

			bytes = zipper.compressBytes (bytes);
			bytes = crypto.encryptBytes (bytes);
		}

		File outFile = new File (_dropboxFilename);
		FileOutputStream outStream = new FileOutputStream (outFile);

		outStream.write (bytes, 0, bytes.length);
		outStream.close ();
	}

	///////////////////////////////////////////////////////////////////////////
	private void deleteFile ()
	{
		File delFile = new File (_dropboxFilename);

		boolean status = true;
		try {
			status = delFile.delete ();

		} catch (Exception ee) {
			status = false;
		}

		if (!status) {
			_log.error ("deleteFile: error deleting file '" + _userFilename + "'");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean backupFile (String filename)
	{
		String backupFilename = new String (filename + ".bak");
		File backupFile = new File (backupFilename);

		if (backupFile.exists ()) {
			try {
				boolean status = backupFile.delete ();
				if (!status) {
					_log.error ("backupFile: delete failed (" + backupFile.getCanonicalPath () + ")");
					return false;
				}

			} catch (Exception ee) {
				_log.error ("backupFile: error deleting backup file '" + backupFilename + "'", ee);
				return false;
			}
		}

//TODO - can't delete backup file from Windows (Error: Access is denied.)

		File file = new File (filename);
		boolean renameStatus = false;

		try {
			renameStatus = file.renameTo (backupFile);

		} catch (Exception ee) {
			_log.error ("backupFile: error renaming file to '" + backupFilename + "'", ee);
			return false;
		}

		//if rename failed, just copy the file
		if (!renameStatus) {
			try {
				BufferedInputStream in = new BufferedInputStream (new FileInputStream (file));
				BufferedOutputStream out = new BufferedOutputStream (new FileOutputStream (backupFile));

				int size = 0;
				byte[] buf = new byte[1024];

				while ((size = in.read (buf)) >= 0) {
					out.write (buf, 0, size);
				}

				in.close ();
				out.close ();
				file.delete (); //delete the old file

			} catch (Exception ee) {
				_log.error ("backupFile: error copying file to '" + backupFilename + "'", ee);
				return false;
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private String quote (String string)
	{
		if (string.contains (" ")) {
			return "\"" + string + "\"";
		} else {
			return string;
		}
	}


	//global members
	public static boolean _Debug = false;
	public static final String _AppName = "DropBox";

	//private members
	private Action _action = Action.ListAction;
	private Mode _mode = Mode.EncodeMode;

	private boolean _force = false;
	private String _dropboxFileBase = null;
	private String _dropboxFilename = null;
	private String _userFilename = null;
	private String _userFileBase = null;
	private String _dropboxFolder = null;

	private static final String _filePrefix = "zzz.";
	private static final String _fileExtension = ".bin";

	private static final String _slash = System.getProperty ("file.separator");
	private static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger (DropBox.class);
}
