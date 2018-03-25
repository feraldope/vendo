//FilesInfo.java

package com.vendo.treeDir2;

//import com.vendo.vendoUtils.*;

//import java.nio.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.*;

import org.apache.logging.log4j.*;


public class FilesInfo
{
	///////////////////////////////////////////////////////////////////////////
	public FilesInfo (Path file)
	{
		_file = file;
	}

/* todo?
	///////////////////////////////////////////////////////////////////////////
	public FilesInfo (String filename)
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
				_absolutePath = _file.toAbsolutePath ().toString ();
				_haveAbsolutePath = true;

			} catch (Exception ee) {
				_log.error ("FilesInfo.getAbsolutePath () failed - ", ee);
			}
		}

		return _absolutePath;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getRealPath () //This is SLOW
	{
		if (!_haveRealPath) {
			try {
				_realPath = _file.toRealPath ().toString (); //This is SLOW
				_haveRealPath = true;

			} catch (Exception ee) {
				_log.error ("FilesInfo.getRealPath () failed - ", ee);
			}
		}

		return _realPath;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getPrettyPath ()
	{
		final String relPath1 = "." + _slash;
		final String relPath2 = ".." + _slash;

		if (!_havePrettyPath) {
			_prettyPath = getAbsolutePath ();

			if (_prettyPath.contains (relPath1) || _prettyPath.contains (relPath2)) {
				_prettyPath = getRealPath ();
			}

			_havePrettyPath = true;
		}

		return _prettyPath;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		if (!_haveName) {
			_name = _file.getName (_file.getNameCount () - 1).toString ();

			_haveName = true;
		}

		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isDirectory ()
	{
		if (!_haveIsDirectory) {
			try {
				_isDirectory = Files.isDirectory (_file);
				_haveIsDirectory = true;

			} catch (Exception ee) {
				_log.error ("FilesInfo.isDirectory () failed - ", ee);
			}
		}

		return _isDirectory;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isRegularFile ()
	{
		if (!_haveIsRegularFile) {
			try {
				_isRegularFile = Files.isRegularFile (_file);
				_haveIsRegularFile = true;

			} catch (Exception ee) {
				_log.error ("FilesInfo.isRegularFile () failed - ", ee);
			}
		}

		return _isRegularFile;
	}

	///////////////////////////////////////////////////////////////////////////
	public FileTime lastModified ()
	{
		if (!_haveLastModified) {
			try {
				_lastModified = Files.getLastModifiedTime (_file);
				_haveLastModified = true;

			} catch (Exception ee) {
				_log.error ("FilesInfo.lastModified () failed - ", ee);
			}
		}

		return _lastModified;
	}

	///////////////////////////////////////////////////////////////////////////
	public long size ()
	{
		if (!_haveSize) {
			try {
				_size = Files.size (_file);
				_haveSize = true;

			} catch (Exception ee) {
				_log.error ("FilesInfo.size () failed - ", ee);
			}
		}

		return _size;
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		if (!_haveFormattedOutput) {

			boolean useBasicFileAttributes = false; //higher performance
//			boolean useBasicFileAttributes = true; //lower performance

			if (useBasicFileAttributes) {
				try {
					BasicFileAttributes attrs = Files.readAttributes (_file, BasicFileAttributes.class);

					String filename = getPrettyPath ();
//					String filename = getRealPath (); //This is SLOW
//					String filename = getAbsolutePath ();
					String modified = _dateFormat.format (attrs.lastModifiedTime ().toMillis ());
					String size;
					if (attrs.isDirectory ())
						size = String.format ("%-16s", "  <DIR>");
					else
						size = String.format ("%16s", _decimalFormat.format (attrs.size ()));

					_formattedOutput = modified + size + " " + filename;

				} catch (Exception ee) {
					_log.error ("FilesInfo.toString () failed - ", ee);
				}

			} else {
				String filename = getPrettyPath ();
//				String filename = getRealPath (); //This is SLOW
//				String filename = getAbsolutePath ();
				String modified = _dateFormat.format (lastModified ().toMillis ());
				String size;
				if (isDirectory ())
					size = String.format ("%-16s", "  <DIR>");
				else
					size = String.format ("%16s", _decimalFormat.format (size ()));

				_formattedOutput = modified + size + " " + filename;
			}
		}

		return _formattedOutput;
	}


	//private members
	private Path _file;
	private boolean _haveAbsolutePath;
	private boolean _haveRealPath;
	private boolean _havePrettyPath;
	private boolean _haveName;
	private boolean _haveIsDirectory;
	private boolean _haveIsRegularFile;
	private boolean _haveLastModified;
	private boolean _haveSize;
	private boolean _haveFormattedOutput;

	private String _absolutePath;
	private String _realPath;
	private String _prettyPath;
	private String _name;
	private boolean _isDirectory;
	private boolean _isRegularFile;
	private FileTime _lastModified;
	private long _size;
	private String _formattedOutput;

	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy  HH:mm:ss.SSS");
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0");

	private static Logger _log = LogManager.getLogger (FilesInfo.class);

	private static final String _slash = System.getProperty ("file.separator");
//	private static final String NL = System.getProperty ("line.separator");
}
