//FileFilter.java

package com.vendo.treeDir;

import com.vendo.vendoUtils.*;

import java.io.*;
//import java.util.Date;

import org.apache.logging.log4j.*;


public class FileFilter implements FilenameFilter
{
	///////////////////////////////////////////////////////////////////////////
	FileFilter (String[] includePatterns, String[] excludePatterns)//, long sinceInMillis)
	{
//		final boolean debugCtor = false;

		if (TreeDir._Debug) {
			String inc = new String ();
			if (includePatterns != null)
				for (int ii = 0; ii < includePatterns.length; ii++)
					inc += " '" + includePatterns[ii] + "'";
			String exc = new String ();
			if (excludePatterns != null)
				for (int ii = 0; ii < excludePatterns.length; ii++)
					exc += " '" + excludePatterns[ii] + "'";

			_log.debug ("FileFilter ctor: includePatterns = " + inc);
			_log.debug ("FileFilter ctor: excludePatterns = " + exc);
		}

		_includePatterns = (includePatterns != null ? includePatterns : new String[] {});
		_excludePatterns = (excludePatterns != null ? excludePatterns : new String[] {});

//		_sinceInMillis = sinceInMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean accept (File dir, String name)
	{
//		if (TreeDir._Debug)
//			_log.debug ("FileFilter.accept: dir = " + dir.getName () + ", name " + name);

		boolean status = false;

		if (_includePatterns.length == 0 && _excludePatterns.length == 0)
			return true;

		do {
			if (_includePatterns != null) {
				for (int ii = 0; ii < _includePatterns.length; ii++) {
					if (_includePatterns[ii].length () != 0) {
						if (VendoUtils.matchPattern (name, _includePatterns[ii])) {
							status = true;
							break;
						}
					}
				}
			}

			if (!status)
				break;

			if (_excludePatterns != null) {
				for (int ii = 0; ii < _excludePatterns.length; ii++) {
					if (_excludePatterns[ii].length () != 0) {
						if (VendoUtils.matchPattern (name, _excludePatterns[ii])) {
							status = false;
							break;
						}
					}
				}
			}

			if (!status)
				break;

/*
			if (_sinceInMillis != 0) {
				try {
					status = (_albumImages.getImageModified (name) > _sinceInMillis);

				} catch (Exception ee) {
					_log.error ("FileFilter.accept: _albumImages is null");
				}
			}
*/
		} while (false);

		return status;
	}


	//members
	private String[] _includePatterns = null;
	private String[] _excludePatterns = null;
//	private long _sinceInMillis = 0;

	private static Logger _log = LogManager.getLogger (FileFilter.class);
}
