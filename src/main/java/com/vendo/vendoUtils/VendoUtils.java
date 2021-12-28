//VendoUtils.java

package com.vendo.vendoUtils;

import com.vendo.win32.ConsoleUtil;
import com.vendo.win32.Win32;
import org.apache.commons.lang.ArrayUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//import org.apache.logging.log4j.*;


public class VendoUtils
{
	///////////////////////////////////////////////////////////////////////////
	private VendoUtils ()
	{
	}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		boolean runTest = true;
		boolean skipTest = true;

		//syntax test: create thread using lambda expression, then start it
		if (runTest) {
			for (int ii = 0; ii < 10; ii++) {
				new Thread (() -> System.out.println ("Thread name: " + Thread.currentThread ().getName ())).start ();
			}
		}

		//print system properties (sorted)
		if (skipTest) {
		    List<String> sorted = System.getProperties ().stringPropertyNames ().stream ()
																				.map (v -> v + "=" + System.getProperty (v).trim ())
																				.sorted (caseInsensitiveStringComparator)
																				.collect (Collectors.toList ());

			System.out.println ("--------------- System Properties (" + sorted.size () + ") ---------------");
			for (String str : sorted) {
				System.out.println (str);
			}
		}

		//print environment variables (sorted)
		if (skipTest) {
		    List<String> sorted = System.getenv ().entrySet ().stream ()
															  .map (v -> v.getKey () + "=" + v.getValue ())
															  .sorted (caseInsensitiveStringComparator)
															  .collect (Collectors.toList ());

			System.out.println ("--------------- Environment Variables (" + sorted.size () + ") ---------------");
			for (String str : sorted) {
				System.out.println (str);
			}
		}

		//print command line arguments to Java VM
		if (skipTest) {
		    List<String> sorted = ManagementFactory.getRuntimeMXBean ().getInputArguments ().stream ()
																							.sorted (caseInsensitiveStringComparator)
																							.collect (Collectors.toList ());

			System.out.println ("--------------- Java VM input arguments (" + sorted.size () + ") ---------------");
			for (String str : sorted) {
				System.out.println (str);
			}
		}

		//print available character sets (sorted)
		if (skipTest) {
		    List<String> sorted = Charset.availableCharsets ().keySet ().stream ()
																		.sorted (caseInsensitiveStringComparator)
																		.collect (Collectors.toList ());

			System.out.println ("--------------- Available Character Sets (" + sorted.size () + ") ---------------");
			for (String str : sorted) {
				System.out.println (str);
			}
		}

		//print available time zones (sorted)
		if (skipTest) {
		    List<String> sorted = ZoneId.getAvailableZoneIds ().stream ()
															   .sorted (caseInsensitiveStringComparator)
															   .collect (Collectors.toList ());

			System.out.println ("--------------- Available Time Zones (" + sorted.size () + ") ---------------");
			for (String str : sorted) {
				System.out.println (str);
			}
		}

		//print known logger names (!sorted)
		if (skipTest) {
//this fails with "error: cannot find symbol", symbol: method getLogManager(), location: class LogManager
//			org.apache.logging.log4j.LogManager logManager = org.apache.logging.log4j.LogManager.getLogManager ();

			java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager ();

			Enumeration<String> loggerNames = logManager.getLoggerNames ();
			System.out.println ("--------------- Known Logger Names ---------------");
			while (loggerNames.hasMoreElements ()) {
				System.out.println (loggerNames.nextElement ());
			}
		}

		//find jar/class
		if (skipTest) {
			System.out.println ("--------------- Find Jar/Class ---------------");
			try {
				Class<?> cls = Class.forName ("JpgInfo.JpgInfo");
				ClassLoader cLoader = cls.getClassLoader ();
				if (cLoader == null) {
					System.out.println ("The default system class was used.");

				} else {
					Class<? extends ClassLoader> loaderClass = cLoader.getClass ();
					System.out.println ("Class associated with ClassLoader = " + loaderClass.getName ());
				}
				System.out.println (cls.getClassLoader ()
									   .loadClass ("com.drew.metadata.Metadata")
									   .getResource ("/com/drew/metadata/Metadata.class"));
			}
			catch (ClassNotFoundException ee) {
				System.out.println ("Error: " + ee.toString ());
			}
		}

		//print class path (not sorted!)
		if (skipTest) {
			ClassLoader classLoader = ClassLoader.getSystemClassLoader ();
			URL[] urls = ((URLClassLoader) classLoader).getURLs ();

			System.out.println ("--------------- Class path (" + urls.length + ") ---------------");
			for(URL url : urls) {
				System.out.println (url);
			}
		}

		//test findPattern and replacePattern
		if (skipTest) {
			final String strings[] = {
				"foo23bar456baz8put",
				"012dog3cat7bird78"
			};
			final String pattern = "[0-9]+"; //sequence of digits
			final String replaceString = "::::"; //marker

			for (String str : strings) {
				System.out.println ("string = '" + str + "'");

				System.out.println ("testing findPattern ()");
				for (int ii = 0; ii < 4; ii++) {
					VPair<Integer, Integer> pair = findPattern (str, pattern, ii);
					System.out.println ("ii = " + ii + ", pair = " + pair);
				}
				System.out.println ("");

				System.out.println ("testing replacePattern ()");
				for (int ii = 0; ii < 4; ii++) {
					String newString = replacePattern (str, pattern, replaceString, ii);
					System.out.println ("ii = " + ii + ", newString = " + newString);
				}
				System.out.println ("");
			}
		}

		//test matchPattern ()
		if (skipTest) {
			System.out.println ("");
			System.out.println ("These should match:");
			matchTest ("foobar", "foobar");
			matchTest ("foobar", "foobar*");
			matchTest ("foobar", "foo*");
			matchTest ("foobar", "Foo*");
			matchTest ("foobar", "?O*");
			matchTest ("foobar", "?O?*");
			matchTest ("foobar", "?O*?");
			matchTest ("foobar", "*bar");
			matchTest ("foobar", "*O*A*");
			matchTest ("foobar", "*a?");
			matchTest ("foobar", "*ooba*");
			matchTest ("foobar", "?ooba?");
			matchTest ("foobar", "fo??ar");
			matchTest ("foobar", "fo?*ar");
//			matchTest ("foobar", "fo%%ar");
//			matchTest ("foobar", "fo%*ar");
			matchTest ("foobar", "??????");

			System.out.println ("");
			System.out.println ("These should not match:");
			matchTest ("foobar", "foobar?");
			matchTest ("foobar", "foo");
			matchTest ("foobar", "o*a");
			matchTest ("foobar", "ooba");
			matchTest ("foobar", "*a??");
			matchTest ("foobar", "?O?");
			matchTest ("foobar", "fo?ar");
//			matchTest ("foobar", "fo%ar");
			matchTest ("foobar", "?????");

			System.out.println ("");
		}

		//test unitSuffixScale ()
		if (skipTest) {
			{ //test KB
				System.out.println ("");

				final int KB = 1024;
				final int start = 990 * KB;
				final int stop = 1050 * KB;
				final int step = 4 * KB;
				final int fieldWidth = 10;

				for (int ii = start; ii <= stop; ii += step) {
					System.out.println (ii + " = " + unitSuffixScale (ii, fieldWidth));
				}
			}
			{ //test MB
				System.out.println ("");

				final int MB = 1024 * 1024;
				final int start = 990 * MB;
				final int stop = 1050 * MB;
				final int step = 4 * MB;
				final int fieldWidth = 10;

				for (int ii = start; ii <= stop; ii += step) {
					System.out.println (ii + " = " + unitSuffixScale (ii, fieldWidth));
				}
			}
		}

		//test listAllThreads ()
		if (skipTest) {
			List<String> threadDetails = new ArrayList<String> ();
			listAllThreads (threadDetails);

			for (String threadDetail : threadDetails) {
				System.out.println (threadDetail);
			}
		}

		//test the speed of various methods for getting the caller *class* name (not method name)
		if (skipTest) {
			testMethod (new ReflectionMethod ());
			testMethod (new ThreadStackTraceMethod ());
			testMethod (new ThrowableStackTraceMethod ());
			testMethod (new SecurityManagerMethod ());
		}

		if (skipTest) {
			System.out.println ("--------------- Test color printing ---------------");

			final short fg = Win32.CONSOLE_FOREGROUND_COLOR_LIGHT_RED;
			final short bg = Win32.CONSOLE_BACKGROUND_COLOR_LIGHT_AQUA;

			System.out.println ("Has Console = " + (hasConsole () ? "true" : "false"));
			System.out.println ("");

			System.out.print ("Normal ");
			printWithColor (fg, " Light Red "); //prints newline by default
			System.out.println ("");

			System.out.print ("Normal ");
			printWithColor (fg, " Light Red ", /*includeNewLine*/ false);
			System.out.println (" Normal");
			System.out.println ("");

			System.out.print ("Normal ");
			printWithColor (bg, fg, " Light Red on Light Aqua "); //prints newline by default
			System.out.println ("");

			System.out.print ("Normal ");
			printWithColor (bg, fg, " Light Red on Light Aqua ", /*includeNewLine*/ false);
			System.out.println (" Normal");
			System.out.println ("");
		}

		if (skipTest) {
			System.out.println ("--------------- Compare Lists ---------------");
			compareLists ();
		}
	}

	// Test the speed of various methods for getting the caller *class* name (not method name)
	// http://stackoverflow.com/questions/421280/how-do-i-find-the-caller-of-a-method-using-stacktrace-or-reflection
	// Abstract class for testing different methods of getting the caller class name
	private static abstract class GetCallerClassNameMethod
	{
		public abstract String getCallerClassName (int callStackDepth);
		public abstract String getMethodName ();
	}
	// Uses the internal Reflection class
	private static class ReflectionMethod extends GetCallerClassNameMethod
	{
		@Override
		public String getCallerClassName (int callStackDepth)
		{
			return sun.reflect.Reflection.getCallerClass (callStackDepth).getName ();
		}
		@Override
		public String getMethodName ()
		{
			return "Reflection";
		}
	}
	// Get a stack trace from the current thread
	private static class ThreadStackTraceMethod extends GetCallerClassNameMethod
	{
		@Override
		public String  getCallerClassName (int callStackDepth)
		{
			return Thread.currentThread ().getStackTrace ()[callStackDepth].getClassName ();
		}
		@Override
		public String getMethodName ()
		{
			return "Current Thread StackTrace";
		}
	}
	// Get a stack trace from a new Throwable
	private static class ThrowableStackTraceMethod extends GetCallerClassNameMethod
	{
		@Override
		public String getCallerClassName (int callStackDepth)
		{
			return new Throwable ().getStackTrace ()[callStackDepth].getClassName ();
		}
		@Override
		public String getMethodName ()
		{
			return "Throwable StackTrace";
		}
	}
	// Use the SecurityManager.getClassContext ()
	private static class SecurityManagerMethod extends GetCallerClassNameMethod
	{
		@Override
		public String  getCallerClassName (int callStackDepth)
		{
			return _mySecurityManager.getCallerClassName (callStackDepth);
		}
		@Override
		public String getMethodName ()
		{
			return "SecurityManager";
		}
		// A custom security manager that exposes the getClassContext () information
		static class MySecurityManager extends SecurityManager
		{
			public String getCallerClassName (int callStackDepth)
			{
				return getClassContext ()[callStackDepth].getName ();
			}
		}
		private final static MySecurityManager _mySecurityManager = new MySecurityManager ();
	}
	private static void testMethod (GetCallerClassNameMethod method)
	{
		long startTime = System.nanoTime ();
		String className = null;
		for (int i = 0; i < 1000000; i++) {
			className = method.getCallerClassName (2);
		}
		printElapsedTime (method.getMethodName (), startTime);
	}
	private static void printElapsedTime (String title, long startTime)
	{
		System.out.println (title + ": " + ((double) (System.nanoTime () - startTime))/1000000 + " ms.");
	}

	///////////////////////////////////////////////////////////////////////////
	// List all threads and recursively list all subgroup
	// http://stackoverflow.com/questions/1323408/get-a-list-of-all-threads-currently-running-in-java
	public static void listAllThreads (List<String> threadDetails)
	{
		ThreadGroup rootGroup = Thread.currentThread ().getThreadGroup ();
		ThreadGroup parent;
		while ((parent = rootGroup.getParent ()) != null) {
			rootGroup = parent;
		}

		listThreads (threadDetails, rootGroup, "");
	}

	///////////////////////////////////////////////////////////////////////////
	// List all threads and recursively list all subgroup
	private static void listThreads (List<String> threadDetails, ThreadGroup group, String indent)
	{
		threadDetails.add (indent + "Group[" + group.getName () + ":" + group.getClass () + "]");
		int numThreads = group.activeCount ();
		Thread[] threads = new Thread[numThreads * 2 + 10]; //numThreads is not accurate
		numThreads = group.enumerate (threads, false);

		// List every thread in the group
		for (int ii = 0; ii < numThreads; ii++) {
			Thread thread = threads[ii];
			threadDetails.add (indent + "   Thread[" + thread.getName () + ":" + thread.getClass () + "]");
		}

		// Recursively list all subgroups
		int numGroups = group.activeGroupCount ();
		ThreadGroup[] groups = new ThreadGroup[numGroups * 2 + 10];
		numGroups = group.enumerate (groups, false);

		for (int ii = 0; ii < numGroups; ii++) {
			listThreads (threadDetails, groups[ii], indent + "   ");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//compare performance or ArrayList vs LinkedList when adding many items
	public static void compareLists ()
	{
		final int count = 1000 * 1000;
		System.out.println ("compareLists: count: " + count);

		{
		long startMillis = new Date ().getTime ();
		compareListsHelper (new ArrayList<String> (), count);
		long elapsedMillis = new Date ().getTime () - startMillis;
		System.out.println ("compareLists: ArrayList: elapsed = " + elapsedMillis + " ms");
		}

		{
		long startMillis = new Date ().getTime ();
		compareListsHelper (new ArrayList<String> (count), count);
		long elapsedMillis = new Date ().getTime () - startMillis;
		System.out.println ("compareLists: ArrayList: elapsed = " + elapsedMillis + " ms (with initial size specified)");
		}

		{
		long startMillis = new Date ().getTime ();
		compareListsHelper (new LinkedList<String> (), count);
		long elapsedMillis = new Date ().getTime () - startMillis;
		System.out.println ("compareLists: LinkedList: elapsed = " + elapsedMillis + " ms");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//helper used by compareLists
	private static void compareListsHelper (Collection<String> coll, int count)
	{
		for (int ii = 0; ii < count; ii++) {
			coll.add (String.valueOf (ii));
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static void matchTest (String searchPath, String pattern)
	{
		boolean matches = matchPattern (searchPath, pattern);
		System.out.println ("matchPattern (" + searchPath + ", " + pattern + ") " +
							 (matches ? "matches" : "does not match"));
	}

	///////////////////////////////////////////////////////////////////////////
	// Wildcard matching routine
	// '?' matches any single char, '*' matches 0 or more chars
	public static boolean matchPattern (String path, String pattern)
	{
		while (true) {
			if (pattern.compareTo (_asterisk) == 0) {
				return true;
			}

			if (pattern.length () == 0 && path.length () == 0) {
				return true;
			} else if (pattern.length () != 0 ^ path.length () != 0) {
				return false;
			}

			String pattChar = pattern.substring (0, 1);
			pattern = pattern.substring (1, pattern.length ());

			if (pattChar.compareTo (_asterisk) == 0) {
				while (true) {
					if (matchPattern (path, pattern)) {
						return true;
					}
					if (path.length () == 0) {
						return false;
					}
					path = path.substring (1, path.length ());
				}

			} else if ((pattChar.compareTo (_question) != 0) /*&&
					   (pattChar.compareTo (_percent) != 0)*/) {
				if (path.length () == 0) {
					return false;
				}
				String pathChar = path.substring (0, 1);
//NOTE - this should match the file system of the server (case-insensitive on Windows; case-sensitive on unix)
				if (pathChar.compareToIgnoreCase (pattChar) != 0)
//				if (pathChar.compareTo (pattChar) != 0)
				{
					return false;
				}
			}

			path = path.substring (1, path.length ());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//returns original string if matchCount exceeds number of times pattern found in string
	public static String replacePattern (String string, String patternString, String replaceString, int matchCount)
	{
		VPair<Integer, Integer> pair = findPattern (string, patternString, matchCount);

		//numbered match not found - return original string
		if (pair.equals (VPair.of (0, 0))) {
			return string;
		}

		int i1 = pair.getFirst ();
		int i2 = pair.getSecond ();

		return string.substring (0, i1) + replaceString + string.substring (i2, string.length ());
	}

	///////////////////////////////////////////////////////////////////////////
	//returns (0, 0) if matchCount exceeds number of times pattern found in string
	public static VPair<Integer, Integer> findPattern (String string, String patternString, int matchCount)
	{
		final Pattern pattern = Pattern.compile (patternString);
		final Matcher matcher = pattern.matcher (string);

		VPair<Integer, Integer> pair = VPair.of (0, 0);

		int ii = 0;
		while (matcher.find ()) {
			if (ii == matchCount) {
				pair = VPair.of (matcher.start (), matcher.end ());
				break;
			}

			ii++;
		}

		if (ii == matchCount) {
			return pair;
		} else {
			return VPair.of (0, 0);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isMacOs ()
	{
		return _osName.startsWith ("Mac OS");
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isWindowsOs ()
	{
		return _osName.startsWith ("Windows");
	}

	///////////////////////////////////////////////////////////////////////////
	public static double getOsVersion ()
	{
		double version = 0;

		try {
			version = Double.parseDouble(_osVersion);
		} catch (Exception ee) {
			System.out.println ("VendoUtils.getOsVersion: " + ee);
		}

		return version;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getLogicalProcessors ()
	{
		return Runtime.getRuntime ().availableProcessors ();
	}

	///////////////////////////////////////////////////////////////////////////
	//simple test to determine if running at home or work
	public static boolean isWorkEnvironment ()
	{
		return _user.equalsIgnoreCase ("ricda13");
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isDigit (char ch)
	{
		return ch >= '0' && ch <= '9';
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isHexDigit (char ch)
	{
		return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
	}

	///////////////////////////////////////////////////////////////////////////
	//return true if entire string is made up of digits, otherwise false
	public static boolean isDigits (String string)
	{
		for (int ii = 0; ii < string.length (); ii++) {
			if (!isDigit (string.charAt (ii))) {
				return false;
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//return true if entire string is made up of hex digits, otherwise false
	public static boolean isHexDigits (String string)
	{
		for (int ii = 0; ii < string.length (); ii++) {
			if (!isHexDigit (string.charAt (ii))) {
				return false;
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int roundUp (double x)
	{
		return (int) Math.ceil (x);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String reverse (String string)
	{
		return new StringBuffer (string).reverse ().toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getUserAgent (boolean ie)
	{
		// Note: these are the values displayed in the browser on swine (Windows 7, 64-bit) when viewing this URL:
		//		http://localhost/servlet/coreservlets.ShowRequestHeaders
		// OR
		// AlbumServlet also has (debug) code to print them

		if (ie) {
			return "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko"; //IE 11
		} else {
			return "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko"; //Firefox 56
		}

		//old values
		//String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0"; //Firefox 29
		//String userAgent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727; .NET CLR 1.1.4322; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET4.0C; .NET4.0E; MS-RTC LM 8)";
		//String userAgent = "Mozilla/5.0 (Windows NT 5.1; rv:13.0) Gecko/20100101 Firefox/13.0.1";
		//String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401"; // Do as if you're using Firefox 3.6.3.
		//String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.1.2) Gecko/20090729 Firefox/3.5.2 (.NET CLR 3.5.30729)";
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getRequestHeaders (HttpServletRequest request)
	{
		StringBuilder sb = new StringBuilder(1000);

		sb.append("Request Method: ").append(request.getMethod()).append(NL);
		sb.append("Request Protocol: ").append(request.getProtocol()).append(NL);
		sb.append("Request URI: ").append(request.getRequestURI()).append(NL);

		Enumeration<String> headerNames = request.getHeaderNames ();
		sb.append("Request Headers:").append(NL);
		while (headerNames.hasMoreElements ()) {
			String headerName = headerNames.nextElement ();
			sb.append(headerName).append(": ").append(request.getHeader(headerName)).append(NL);
		}

		Cookie[] cookies = request.getCookies ();
		sb.append("Request Cookies:").append(NL);
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				sb.append (cookie);
			}
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getMemoryStatistics ()
	{
		final long mega = (1024 * 1024);

		long maxMem   = Runtime.getRuntime ().maxMemory () / mega;
		long freeMem  = Runtime.getRuntime ().freeMemory () / mega;
		long totalMem = Runtime.getRuntime ().totalMemory () / mega;
		long usedMem = totalMem - freeMem;

		return "Memory used=" + usedMem + "MB free=" + freeMem + "MB total=" + totalMem + "MB max=" + maxMem + "MB";
	}

	///////////////////////////////////////////////////////////////////////////
	public static void sleepMillis (int milliseconds)
	{
		if (milliseconds > 0) {
			try {
				Thread.sleep (milliseconds);

			} catch (Exception ee) {
				ee.printStackTrace ();
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//http://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java
	public static String toHexString (String string)
	{
		return toHexString (string.getBytes ());
	}
	public static String toHexString (byte[] byteArray)
	{
		StringBuilder sb = new StringBuilder ();

		for (byte b : byteArray) {
			sb.append(String.format("%02x", b));
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	//note this always returns a List
	public static <T> Collection<T> shuffleAndTruncate (Collection<T> items, int newSize, boolean sort)
	{
		List<T> list;
		if (items instanceof List) {
			list = (List<T>) items;

		} else {
			//convert to list, so we can shuffle
			list = new ArrayList<T> (items.size ());
			list.addAll(items);
		}
		Collections.shuffle (list);

		if (newSize > 0 && newSize < list.size ()) {
			list = list.subList (0, newSize);
		}

		if (sort) {
			list.sort((o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
		}

		return list;
	}

//TODO: note that most (all?) uses of arrayToString() can likely be replaced with a stream solution using Collectors.joining(), like:
//		String string = set.stream ().sorted (VendoUtils.caseInsensitiveStringComparator).collect (Collectors.joining (", "));

	///////////////////////////////////////////////////////////////////////////
	//use this version for primitive type (int)
	public static String arrayToString (int[] items)
	{
		return arrayToString (ArrayUtils.toObject (items), ", ");
	}

	///////////////////////////////////////////////////////////////////////////
	//use this version for primitive type long
	public static String arrayToString (long[] items)
	{
		return arrayToString (ArrayUtils.toObject (items), ", ");
	}

	///////////////////////////////////////////////////////////////////////////
	//use this version for primitive type double
	public static String arrayToString (double[] items)
	{
		return arrayToString (ArrayUtils.toObject (items), ", ");
	}

	///////////////////////////////////////////////////////////////////////////
	//use these version for Collections
	public static <T> String collectionToString (Collection<T> collection)
	{
		return collectionToString (collection, ", ");
	}
	public static <T> String collectionToString (Collection<T> collection, String separator)
	{
		return arrayToString (collection.toArray (new Object[] {}), separator);
	}

	///////////////////////////////////////////////////////////////////////////
	//Note these are very similar to Arrays.toString ()
	public static <T> String arrayToString (T[] items)
	{
		return arrayToString (items, ", ");
	}
	public static <T> String arrayToString (T[] items, String separator)
	{
		StringBuilder sb = new StringBuilder(items.length * 10);

		for (T item : items) {
			sb.append (separator)
			  .append (item.toString ().trim ());
		}

		int startIndex = (sb.length () > separator.length () ? separator.length () : 0); //step over initial separator, if there

		return sb.substring (startIndex);
	}

//do we need this?  use Arrays.asList () instead
//	///////////////////////////////////////////////////////////////////////////
//	public static <T> ArrayList<T> arrayToList (T[] items)
//	{
//		ArrayList<T> list = new ArrayList<T> (items.length);
//		for (T item : items) {
//			list.add (item);
//		}
//
//		return list;
//	}

	///////////////////////////////////////////////////////////////////////////
	//calls trim () on each item, also eliminates empty items
	public static String[] trimArrayItems (String[] items1)
	{
		String[] items2 = new String[items1.length];

		int i1 = 0;
		int i2 = 0;
		for (i1 = 0; i1 < items1.length; i1++) {
			String item1 = items1[i1].trim ();
			if (item1.length () > 0) {
				items2[i2++] = items1[i1].trim ();
			}
		}

		if (i1 == i2) {
			return items2;
		} else {
			return Arrays.copyOf (items2, i2);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static List<String> truncateList (List<String> list, int maxItems)
	{
		int numItems = list.size ();
		if (numItems > maxItems) {
			list.subList (maxItems, numItems).clear ();
			list.add ("..."); //indicate list has been truncated
		}

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	public static <T> Collection<T> dedupCollection (Collection<T> items)
	{
		//deduplicate Collection by adding everything to Set
		return new HashSet<T>(items);
	}

	///////////////////////////////////////////////////////////////////////////
	public static List<String> caseInsensitiveSortAndDedup (List<String> strings) {
		Set<String> deduped = new HashSet<>(strings);

		TreeSet<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		deduped.removeIf(s -> !seen.add(s));

		return new ArrayList<>(deduped);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getStackTrace (Throwable ex)
	{
	    PrintWriter writer = new PrintWriter (new StringWriter ());
	    ex.printStackTrace (writer);
	    return writer.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	// only for positive values
	public static String unitSuffixScale (double value)
	{
		return unitSuffixScale (value, 0);
	}
	public static String unitSuffixScale (double value, int fieldWidth)
	{
		final String[] unitSuffixArray = new String[] { "B ", "KB", "MB", "GB", "TB", "PB" };
		final DecimalFormat decimalFormat = new DecimalFormat ("#,##0.00");
		final double base = 1024;

		int digitGroups = 0;
		if (value < 1) {
			return "0";
		} else {
			digitGroups = (int) (Math.log10 (value) / Math.log10 (base));
		}

		String valueString = "??";
		try {
			valueString = decimalFormat.format(value / Math.pow(base, digitGroups)) + " " + unitSuffixArray[digitGroups];

		} catch (ArrayIndexOutOfBoundsException ex) {
			//TODO - print/log error
		}

		if (fieldWidth > 0) {
			String format = "%" + fieldWidth + "s";
			valueString = String.format (format, valueString);
		}

		return valueString;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String appendSlash (String dir) //append slash if necessary
	{
		if (!dir.endsWith ("\\") || !dir.endsWith ("/")) {
			dir += _slash;
		}

		return dir;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String quoteString (String string)
	{
		return quoteString (string, true);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String quoteString (String string, boolean always)
	{
		if (always || string.contains (" ")) {
			return "\"" + string + "\"";
		} else {
			return string;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getRealPathString (Path file)
	{
		String realPath;
		try {
			realPath = file.toRealPath ().toString ();

		} catch (IOException ee) {
			ee.printStackTrace ();

			realPath = file.toAbsolutePath ().toString ();
		}

		return realPath;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getCurrentDirectory ()
	{
		Path file = FileSystems.getDefault ().getPath ("");
		return file.toAbsolutePath ().toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean isDirectory (String filename)
	{
		boolean isDirectory = false;
		try {
			Path file = FileSystems.getDefault ().getPath (filename);
			isDirectory = Files.isDirectory (file, LinkOption.NOFOLLOW_LINKS);

		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		return isDirectory;
	}

	///////////////////////////////////////////////////////////////////////////
	//expects full path with wildcard "*" in the name only
	//returns true if it finds any matches
	public static boolean fileExistsWild (String wildname)
	{
		boolean fileExists = false;

		try {
			final String marker = "QyUuZeQa"; //temporarily remove any "*" since getPath does not accept them
			Path fileWithMarker = FileSystems.getDefault ().getPath (wildname.replace("*", marker));
			Path parent = fileWithMarker.getParent();
			List<File> list = Arrays.asList((parent.toFile()).listFiles(File::isFile));

			String wildnameOnly = fileWithMarker.toFile().getName().replace(marker, ".*");
			Pattern pattern = Pattern.compile(wildnameOnly, Pattern.CASE_INSENSITIVE);
			fileExists = list.stream().map(File::getName).anyMatch(pattern.asPredicate());

		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		return fileExists;
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean fileExists (String filename)
	{
		boolean fileExists = false;
		try {
			Path file = FileSystems.getDefault ().getPath (filename);
			fileExists = Files.exists (file);

		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		return fileExists;
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean fileExists (Path path)
	{
		boolean fileExists = false;
		try {
			fileExists = Files.exists (path);

		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		return fileExists;
	}

	///////////////////////////////////////////////////////////////////////////
	public static double getDiskPercentFull (String filename)
	{
		long freeBytes = VendoUtils.getFreeDiskSpace (filename);
		long totalBytes = VendoUtils.getTotalDiskSpace (filename);
		double percentFull = (double) (100 * (totalBytes - freeBytes)) / (double) totalBytes;

		return percentFull;
	}

	///////////////////////////////////////////////////////////////////////////
	public static long getFreeDiskSpace (String filename)
	{
		long freeSpace = (-1);

		try {
			File file = new File (filename);
	    	freeSpace = file.getFreeSpace ();

		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		return freeSpace;
	}

	///////////////////////////////////////////////////////////////////////////
//	public static long getUsableDiskSpace (String filename)
//	{
//		long usableSpace = (-1);
//
//		try {
//			File file = new File (filename);
//	    	usableSpace = file.getUsableSpace ();
//
//		} catch (Exception ee) {
//			ee.printStackTrace ();
//		}
//
//		return usableSpace;
//	}

	///////////////////////////////////////////////////////////////////////////
	public static long getTotalDiskSpace (String filename)
	{
		long totalSpace = (-1);

		try {
			File file = new File (filename);
	    	totalSpace = file.getTotalSpace ();

		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		return totalSpace;
	}

	///////////////////////////////////////////////////////////////////////////
	//see https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
	//if url = "http://example.com:80/docs/books/tutorial/index.html?name=networking#DOWNLOADING
	// protocol = http
	// authority = example.com:80
	// host = example.com
	// port = 80
	// path = /docs/books/tutorial/index.html
	// query = name=networking
	// filename = /docs/books/tutorial/index.html?name=networking
	// ref = DOWNLOADING
	public static String getUrlFileComponent (String urlIn)
	{
		String fileComponent = urlIn;
		try {
			URL url = new URL (urlIn);
			fileComponent = url.getFile ();
		} catch (Exception ee) {
		}

		return fileComponent;
	}

	///////////////////////////////////////////////////////////////////////////
	//strip leading 'www.', collapse multiple slashes (after protocol) into single slash
	public static String normalizeUrl (String url)
	{
		url = url.replaceFirst ("://www\\.", "://");

		while (url.contains ("//")) {
			url = url.replaceAll ("//", "/");
		}

		url = url.replaceFirst (":/", "://");

		return url;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String escapeHtmlChars (String urlFragment)
	{
		return urlFragment.replace ("+", "%2b")
							.replace ("[", "%5b")
							.replace ("]", "%5d");
	}

	///////////////////////////////////////////////////////////////////////////
	public static final Comparator<String> caseInsensitiveStringComparator = new Comparator<String> ()
	{
		@Override
		public int compare (String s1, String s2)
		{
			return s1.compareToIgnoreCase (s2);
		}
	};

	///////////////////////////////////////////////////////////////////////////
//	public static final Comparator<String> caseSensitiveStringComparator = new Comparator<String> ()
//	{
//		@Override
//		public int compare (String s1, String s2)
//		{
//			return s1.compareTo (s2);
//		}
//	};

	///////////////////////////////////////////////////////////////////////////
	public static boolean hasConsole ()
	{
		return ConsoleUtil.has_console ();
	}
	public static void printWithColor (Short fg, String line)
	{
		final Short bg = Win32.CONSOLE_BACKGROUND_COLOR_BLACK;
		final boolean includeNewLine = true;
		printWithColor (bg, fg, line, includeNewLine);
	}
	public static void printWithColor (Short fg, String line, boolean includeNewLine)
	{
		final Short bg = Win32.CONSOLE_BACKGROUND_COLOR_BLACK;
		printWithColor (bg, fg, line, includeNewLine);
	}
	public static void printWithColor (Short bg, Short fg, String line)
	{
		final boolean includeNewLine = true;
		printWithColor (bg, fg, line, includeNewLine);
	}
	public synchronized static void printWithColor (Short bg, Short fg, String line, boolean includeNewLine)
	{
		try {
			if (includeNewLine) {
				ConsoleUtil.static_color_println (System.out, line, bg, fg);
			} else {
				ConsoleUtil.static_color_print (System.out, line, bg, fg);
			}

		} catch (Exception ee) {
			ee.printStackTrace ();
		}
	}


	//members
	private static final String _asterisk = "*";
	private static final String _question = "?";
//	private static final String _percent = "%";

	private static final String _osName = System.getProperty ("os.name");
	private static final String _osVersion = System.getProperty ("os.version"); //e.g., will be "10.0" on Windows 10
	private static final String _user = System.getProperty ("user.name");
	private static final String _slash = System.getProperty ("file.separator");

//	private static final Logger _log = LogManager.getLogger ();

	private static final String NL = System.getProperty ("line.separator");
}
