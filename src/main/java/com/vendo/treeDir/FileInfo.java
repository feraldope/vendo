//FileInfo.java

package com.vendo.treeDir;

//import com.vendo.vendoUtils.*;

import java.io.*;
import java.text.*;

import org.apache.logging.log4j.*;


public class FileInfo
{
	///////////////////////////////////////////////////////////////////////////
	public FileInfo (File file)
	{
		_file = file;
	}

/* todo?
	///////////////////////////////////////////////////////////////////////////
	public FileInfo (String filename)
	{
		_file = new File (filename);
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//getAbsolutePath() tends to be quicker than the alternatives, but not as nice
	public String getAbsolutePath ()
	{
		if (!_haveAbsolutePath) {
			try {
				_absolutePath = _file.getAbsolutePath ();
				_haveAbsolutePath = true;

			} catch (Exception ee) {
				_log.error ("FileInfo.getAbsolutePath () failed - ", ee);
			}
		}

		return _absolutePath;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getCanonicalPath ()
	{
		if (!_haveCanonicalPath) {
			try {
				_canonicalPath = _file.getCanonicalPath ();
				_haveCanonicalPath = true;

			} catch (Exception ee) {
				_log.error ("FileInfo.getCanonicalPath () failed - ", ee);
			}
		}

		return _canonicalPath;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getPrettyPath ()
	{
		final String relPath1 = "." + _slash;
		final String relPath2 = ".." + _slash;

		if (!_havePrettyPath) {
			_prettyPath = getAbsolutePath ();

			if (_prettyPath.contains (relPath1) || _prettyPath.contains (relPath2)) {
				_prettyPath = getCanonicalPath ();
			}

			_havePrettyPath = true;
		}

		return _prettyPath;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		if (!_haveName) {
			_name = _file.getName ();
			_haveName = true;
		}

		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isDirectory ()
	{
		if (!_haveIsDirectory) {
			try {
				_isDirectory = _file.isDirectory ();
				_haveIsDirectory = true;

			} catch (Exception ee) {
				_log.error ("FileInfo.isDirectory () failed - ", ee);
			}
		}

		return _isDirectory;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isRegularFile ()
	{
		if (!_haveIsRegularFile) {
			try {
				_isRegularFile = _file.isFile ();
				_haveIsRegularFile = true;

			} catch (Exception ee) {
				_log.error ("FileInfo.isFile () failed - ", ee);
			}
		}

		return _isRegularFile;
	}

	///////////////////////////////////////////////////////////////////////////
	public long lastModified ()
	{
		if (!_haveLastModified) {
			try {
				_lastModified = _file.lastModified (); //millisecs
				_haveLastModified = true;

			} catch (Exception ee) {
				_log.error ("FileInfo.lastModified () failed - ", ee);
			}
		}

		return _lastModified;
	}

	///////////////////////////////////////////////////////////////////////////
	public long length ()
	{
		if (!_haveLength) {
			try {
				_length = _file.length ();
				_haveLength = true;

			} catch (Exception ee) {
				_log.error ("FileInfo.length () failed - ", ee);
			}
		}

		return _length;
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		if (!_haveFormattedOutput) {
			String filename = getPrettyPath ();
			String modified = _dateFormat.format (lastModified ());
			String size;
			if (isDirectory ())
				size = String.format ("%-16s", "  <DIR>");
			else
				size = String.format ("%16s", _decimalFormat.format (length ()));

			_formattedOutput = modified + size + " " + filename;
		}

		return _formattedOutput;
	}


	//private members
	private File _file;
	private boolean _haveAbsolutePath;
	private boolean _haveCanonicalPath;
	private boolean _havePrettyPath;
	private boolean _haveName;
	private boolean _haveIsDirectory;
	private boolean _haveIsRegularFile;
	private boolean _haveLastModified;
	private boolean _haveLength;
	private boolean _haveFormattedOutput;

	private String _absolutePath;
	private String _canonicalPath;
	private String _prettyPath;
	private String _name;
	private boolean _isDirectory;
	private boolean _isRegularFile;
	private long _lastModified;
	private long _length;
	private String _formattedOutput;

	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy  HH:mm:ss.SSS");
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0");

	private static Logger _log = LogManager.getLogger (FileInfo.class);

	private static final String _slash = System.getProperty ("file.separator");
//	private static final String NL = System.getProperty ("line.separator");
}
