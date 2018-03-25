//FilesFilter.java

package com.vendo.treeDir2;

import com.vendo.vendoUtils.*;

//import java.io.*;
import java.io.IOException;
//import java.nio.*;
import java.nio.file.*;
//import java.util.Date;

import org.apache.logging.log4j.*;


//public class FilesFilter implements FilenameFilter
public class FilesFilter implements DirectoryStream.Filter<Path>
{
	///////////////////////////////////////////////////////////////////////////
	public FilesFilter (String[] includePatterns, String[] excludePatterns)//, long sinceInMillis)
	{
//		final boolean debugCtor = false;

		if (TreeDir2._Debug) {
			String inc = new String ();
			if (includePatterns != null)
				for (int ii = 0; ii < includePatterns.length; ii++)
					inc += " '" + includePatterns[ii] + "'";
			String exc = new String ();
			if (excludePatterns != null)
				for (int ii = 0; ii < excludePatterns.length; ii++)
					exc += " '" + excludePatterns[ii] + "'";

			_log.debug ("FilesFilter ctor: includePatterns = " + inc);
			_log.debug ("FilesFilter ctor: excludePatterns = " + exc);
		}

		_includePatterns = (includePatterns != null ? includePatterns : new String[] {});
		_excludePatterns = (excludePatterns != null ? excludePatterns : new String[] {});

//		_sinceInMillis = sinceInMillis;
	}

	///////////////////////////////////////////////////////////////////////////
//	public boolean accept (File dir, String name)
	public boolean accept (Path path) throws IOException
	{
//		if (TreeDir2._Debug)
//			_log.debug ("FilesFilter.accept: dir = " + dir.getName () + ", name " + name);

		boolean status = false;

		if (_includePatterns.length == 0 && _excludePatterns.length == 0)
			return true;

//		Path file = path.getName (path.getNameCount () - 1);
//		String name = file.getName (0);
		String name = path.getName (path.getNameCount () - 1).toString ();

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
					_log.error ("FilesFilter.accept: _albumImages is null");
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

	private static Logger _log = LogManager.getLogger (FilesFilter.class);
}
