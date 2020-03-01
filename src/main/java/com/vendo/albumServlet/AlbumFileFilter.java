//AlbumFileFilter.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AlbumFileFilter implements FilenameFilter
{
	///////////////////////////////////////////////////////////////////////////
	AlbumFileFilter (String[] includeFilters, String[] excludeFilters, boolean useCase, long sinceInMillis)
	{
		boolean debugCtor = false;

		int patternFlags = (useCase ? 0 : Pattern.CASE_INSENSITIVE);

		if (debugCtor && AlbumFormInfo._Debug) {
			String inc = new String ();
			if (includeFilters != null) {
				for (String includeFilter : includeFilters) {
					inc += " \"" + includeFilter + "\"";
				}
			}
			String exc = new String ();
			if (excludeFilters != null) {
				for (String excludeFilter : excludeFilters) {
					exc += " \"" + excludeFilter + "\"";
				}
			}

			_log.debug ("AlbumFileFilter ctor: includeFilters =" + inc);
			_log.debug ("AlbumFileFilter ctor: excludeFilters =" + exc);
		}

		if (includeFilters != null && includeFilters.length > 0) {
			_includePatterns = new ArrayList<Pattern> (includeFilters.length);

			for (String includeFilter : includeFilters) {
				if (includeFilter.length () != 0) {
					String orinalIncludeFilter = includeFilter; //in case of error
					if (includeFilter.startsWith ("*") || includeFilter.startsWith ("[")) {
						_includeAllFolders = true;
					}
					if (includeFilter.equals ("*")) {
						_includeAllFiles = true;
						break;
					}

					if (!useCase) {
						includeFilter = includeFilter.toLowerCase ();
					}

					includeFilter = AlbumFormInfo.convertWildcardsToRegex (includeFilter);

					if (debugCtor && AlbumFormInfo._Debug) {
						_log.debug ("AlbumFileFilter ctor: includeFilter = " + includeFilter);
					}

					try {
						Pattern pattern = Pattern.compile (includeFilter, patternFlags);
						_includePatterns.add (pattern);

					} catch (Exception ee) {
						AlbumFormInfo.getInstance ().addServletError ("Warning: ignoring invalid includeFilter: \"" + orinalIncludeFilter + "\": " + ee);
					}
				}
			}
		}

		if (excludeFilters != null && excludeFilters.length > 0) {
			_excludePatterns = new ArrayList<Pattern> (excludeFilters.length);

			for (String excludeFilter : excludeFilters) {
				if (excludeFilter.length () != 0) {
					String orinalExcludeFilter = excludeFilter; //in case of error

					if (!useCase) {
						excludeFilter = excludeFilter.toLowerCase ();
					}

					excludeFilter = AlbumFormInfo.convertWildcardsToRegex (excludeFilter);

					if (debugCtor && AlbumFormInfo._Debug) {
						_log.debug ("AlbumFileFilter ctor: excludeFilter = " + excludeFilter);
					}

					try {
						Pattern pattern = Pattern.compile (excludeFilter, patternFlags);
						_excludePatterns.add (pattern);

					} catch (Exception ee) {
						AlbumFormInfo.getInstance ().addServletError ("Warning: ignoring invalid excludeFilter: \"" + orinalExcludeFilter + "\": " + ee);
					}
				}
			}
		}

//		_sinceInMillis = sinceInMillis;
//		_albumImages = AlbumImages.getInstance ();

		if (debugCtor && AlbumFormInfo._Debug) {
			_log.debug ("AlbumFileFilter ctor: _includeAllFolders = " + new Boolean (_includeAllFolders) + ", _includeAllFiles = " + new Boolean (_includeAllFiles));
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//note this does not check the exclude filters to eliminate any folders
	public boolean folderNeedsChecking (String folder)
	{
		boolean status = false;

		do {
			if (_includeAllFolders) {
				status = true;

			} else if (_includePatterns != null) {
				for (Pattern includePattern : _includePatterns) {
					String leadingNonNumericChars = includePattern.pattern ().replaceFirst ("[0-9\\[\\.\\*].*", "");
					if (leadingNonNumericChars.startsWith(folder) || folder.startsWith(leadingNonNumericChars)) {
//						_log.debug ("AlbumFileFilter folderNeedsChecking: folder \"" + folder + "\" matches pattern \"" + includePattern + "\"");
						status = true;
						break;
//					} else {
//						_log.debug ("AlbumFileFilter folderNeedsChecking: folder \"" + folder + "\" DOES NOT match pattern \"" + includePattern + "\"");
					}
				}

/*
				//handle regular expression ranges (e.g., "[a-d]")
				//TODO - note if you pass in e.g., "[a-d]a", this ignores everything after the []; i.e., it will include folders aa, ab, ad, af, etc.
				for (Pattern includePattern : _includePatterns) {
					String regexRange = includePattern.pattern ();
					if (regexRange.startsWith ("[")) {
						//strip everything after closing ']' (pattern must evaluate to one character to match folder, which is clipped to one character)
						int close = regexRange.indexOf (']');
						regexRange = regexRange.substring (0, close + 1);
						if (Pattern.matches (regexRange, folder.substring (0, 1))) {
							status = true;
							break;
						}
					}
				}
*/
			}
		} while (false);

//		if (status && AlbumFormInfo._logLevel >= 5) {
//			_log.debug ("AlbumFileFilter.folderNeedsChecking: folder = " + folder + ", return = " + new Boolean (status));
//		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean accept (File dir, String name)
	{
//		_log.debug ("AlbumFileFilter.accept: name = " + name + ", pattern = " + _includePatterns + _extension);

		if (_profileAccept) {
			AlbumProfiling.getInstance ().enter (7);
		}

		//optimization: AlbumXml.doDir calls this with null dir and names with no extensions
		//for other callers, reject any file without an image extension
		if (dir != null) {
			if (!name.toLowerCase ().endsWith (AlbumFormInfo._ImageExtension)) {
				if (AlbumFormInfo._logLevel >= 10) {
					_log.debug ("AlbumFileFilter.accept: rejecting non-image, name = " + name);
				}

				return false; //not an image file
			}

			name = AlbumFormInfo.stripImageExtension (name);
		}

		boolean status = false;

		do {
			if (_includeAllFiles) {
				status = true;

			} else if (_includePatterns != null) {
				for (Pattern includePattern : _includePatterns) {
					Matcher matcher = includePattern.matcher (name);
					if (matcher.matches ()) {
						status = true;
						break;
					}
				}
			}
			if (!status) {
				break;
			}

			if (_excludePatterns != null) {
				for (Pattern excludePattern : _excludePatterns) {
					Matcher matcher = excludePattern.matcher (name);
					if (matcher.matches ()) {
						status = false;
						break;
					}
				}
			}
			if (!status) {
				break;
			}

/*TODO - fix this
			if (_sinceInMillis > 0) { //0 means disabled
				try {
					status = (_albumImages.getImageModified (name) > _sinceInMillis);

				} catch (Exception ee) {
					_log.error ("AlbumFileFilter.accept: _albumImages is null");
				}
			}
*/
		} while (false);

		if (_profileAccept) {
			AlbumProfiling.getInstance ().exit (7);
		}

		return status;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isAllFolders ()
	{
		return _includeAllFolders;
	}

	///////////////////////////////////////////////////////////////////////////
	//note this will be fooled if a regular expression uses the range character '-' (e.g., [1-4] instead of [1234])
	public int getMinItemCount ()
	{
		int itemCount = 0;

		if (_includePatterns != null) {
			for (Pattern includePattern : _includePatterns) {
				String patternString = includePattern.pattern ().toString ();
				int openBracket = patternString.indexOf ('[');
				if (openBracket > 0) {
					if (patternString.charAt (openBracket + 1) == '\\' && patternString.charAt (openBracket + 2) == 'd') { // handle [\d]
						itemCount++; //possibly/likely more than 1, but we can't know for sure

					} else {
						int closeBracket = patternString.indexOf (']');
						itemCount += closeBracket - openBracket - 1;
					}

				} else {
					itemCount ++;
				}
			}
		}

		return itemCount;
	}

	///////////////////////////////////////////////////////////////////////////
	//note this does not show the exclude filters
	@Override
	public String toString ()
	{
		final String separator = ",";
		final StringBuilder sb = new StringBuilder ();

		if (_includeAllFiles) {
			sb.append (separator).append ("<allFiles>");
		}

		if (_includeAllFolders) {
			sb.append (separator).append ("<allFolders>");
		}

		if (_includePatterns != null) {
			_includePatterns.stream ()
							.map (p -> AlbumFormInfo.convertRegexToWildcards (p.toString ()))
							.sorted (VendoUtils.caseInsensitiveStringComparator)
							.forEach (s -> sb.append (separator).append (s));
		}

		int startIndex = (sb.length () > separator.length () ? separator.length () : 0); //step over initial separator, if there

		return sb.substring (startIndex);
	}


	//members
	private boolean _includeAllFiles = false;
	private boolean _includeAllFolders = false;
//	private long _sinceInMillis = 0;
	private Collection<Pattern> _includePatterns = null;
	private Collection<Pattern> _excludePatterns = null;
//	private AlbumImages _albumImages = null;

	private final boolean _profileAccept = false;

	private static Logger _log = LogManager.getLogger ();
}
