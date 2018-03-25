//FilenamePatternFilter.java - filename filter that supports regex

package com.vendo.jpgInfo;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

//import org.apache.logging.log4j.*;

public class FilenamePatternFilter implements DirectoryStream.Filter<Path> {
	///////////////////////////////////////////////////////////////////////////
	public FilenamePatternFilter() {
		this("*"); // default ctor matches all files
	}

	///////////////////////////////////////////////////////////////////////////
	public FilenamePatternFilter(String filenamePatternString) {
		filenamePatternString = filenamePatternString.replace("*", ".*");
		filenamePatternString = filenamePatternString.replace("+", "[\\d]"); // plus sign here means digit
		filenamePatternString = filenamePatternString.toLowerCase(); // any regular expressions themselves need to be in lower case

		try {
			_filenamePattern = Pattern.compile(filenamePatternString, Pattern.CASE_INSENSITIVE);

		} catch (PatternSyntaxException ee) {
			throw new RuntimeException("FilenamePatternFilter ctor: invalid pattern = \"" + filenamePatternString + "\": " + ee);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean accept(Path path) throws IOException {
		String name = path.getName(path.getNameCount() - 1).toString(); // filename is last leaf
		Matcher matcher = _filenamePattern.matcher(name);
		if (matcher.matches()) {
			return true;
		}

		return false;
	}

	// private members
	final protected Pattern _filenamePattern;

	// protected static Logger _log = LogManager.getLogger (FilenamePatternFilter.class);

	// global members
	public static boolean _Debug = false;
}
