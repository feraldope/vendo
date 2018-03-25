//ContentFormInfo.java

//	Naming Convention for Paths and Folders:
//		path = absolute path
//		folder or dir = relative path
//		(paths should always have trailing slash, folders never do)
//
//	Examples:
//		basePath = E:/Netscape/Program/
//		rootPath = E:/Netscape/Program/jroot/
//		imagePath = E:/Netscape/Program/jroot/a/
//		rootFolder = jroot
//		subFolder = a
//		subDir = a

package com.vendo.contentServlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VendoUtils;


public class ContentFormInfo
{
	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static ContentFormInfo getInstance ()
	{
		if (_instance == null)
			_instance = new ContentFormInfo ();

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private ContentFormInfo ()
	{
		init ();
	}

	///////////////////////////////////////////////////////////////////////////
	private ContentFormInfo init ()
	{
//		if (true || VendoUtils.isWorkEnvironment ()) {
		if (VendoUtils.isWorkEnvironment ()) {
			_basePath = "C:/";
			_contentRoot = "/contentRootC/";

			_rootFolder = "users/bin";
			_extension = "bat,C,H,mak,txt";
		} else {
			_basePath = "E:/";
			_contentRoot = "/contentRootE/";

//			_rootFolder = "Netscape/Program/v";
			_rootFolder = "Netscape/Program/";
//			_extension = "mpg";
			_extension = "mpg,wmv";
//			_extension = "mpg,avi";
/*
			_rootFolder = "Netscape/Program/g";
			_extension = "gif";
*/
		}
/*
		_mode = ContentMode.DoDir;
		_filter1 = "";
		_filter2 = "";
		_filter3 = "";
		_filter4 = "";
		_exclude1 = "";
		_exclude2 = "";
		_exclude3 = "";
		_exclude4 = "";
		_rootFolder = "";
		_columns = _defaultColumns;
		_panels = _defaultPanels;
		_slice = 1;
		_sinceDays = 0; //0 means show all
		_sinceInMillis = 0;
		_collapseGroups = false;
		_reverseSort = false;
		_sortType = ContentSortType.ByName;

		_highlightDays = _defaultHighlightDays;
		_highlightPixels = _defaultHighlightPixels;
		_logLevel = _defaultLogLevel;
		_maxImageScalePercent = _defaultMaxImageScalePercent;
		_profileLevel = _defaultProfileLevel;
		_testRgbData = _defaultTestRgbData;
*/

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	public void processRequest (HttpServletRequest request, ServletContext context)
	{
/*
		final boolean debugCtor = false;

		boolean gotDebugRequest = false;
		boolean gotCollapseGroups = false;
		boolean gotReverseSort = false;
		boolean gotTestRgbData = false;

		//handle values in properties file
		Properties properties = new Properties ();
		try {
			InputStream inputStream = context.getResourceAsStream (_propertiesFile);
			properties.load (inputStream);

		} catch (Exception ee) {
			_log.error ("ContentFormInfo ctor: failed to read properties file \"" + _propertiesFile + "\"");
		}

		String string = properties.getProperty ("debug", "no");
		if (string.compareToIgnoreCase ("yes") == 0) {
			gotDebugRequest = true;
			if (debugCtor)
				_log.debug ("ContentFormInfo ctor: property: got debug");
		}

		string = properties.getProperty ("testRgbData", "no");
		if (string.compareToIgnoreCase ("yes") == 0) {
			gotTestRgbData = true;
			if (debugCtor)
				_log.debug ("ContentFormInfo ctor: property: got testRgbData");
		}

		string = properties.getProperty ("defaultColumns", (new Integer (_defaultColumns)).toString ());
		try {
			_defaultColumns = Integer.parseInt (string);
			if (_defaultColumns > 0) {
				_columns = _defaultColumns;
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: property: got defaultColumns = " + _defaultColumns);
			}
		} catch (NumberFormatException exception) {
			_log.error ("ContentFormInfo ctor: error parsing defaultColumns value \"" + string + "\", using value " + _defaultColumns);
		}

		string = properties.getProperty ("defaultMaxImageScalePercent", (new Integer (_defaultMaxImageScalePercent)).toString ());
		try {
			_defaultMaxImageScalePercent = Integer.parseInt (string);
			if (_defaultMaxImageScalePercent > 0) {
				_maxImageScalePercent = _defaultMaxImageScalePercent;
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: property: got defaultMaxImageScalePercent = " + _defaultMaxImageScalePercent);
			}
		} catch (NumberFormatException exception) {
			_log.error ("ContentFormInfo ctor: error parsing defaultMaxImageScalePercent value \"" + string + "\", using value " + _defaultMaxImageScalePercent);
		}

		_rootFolder = properties.getProperty ("defaultRootFolder", _defaultRootFolder);
		if (debugCtor)
			_log.debug ("ContentFormInfo ctor: property: using rootFolder = " + _rootFolder);

		string = properties.getProperty ("defaultLogLevel", (new Integer (_defaultLogLevel)).toString ());
		try {
			_defaultLogLevel = Integer.parseInt (string);
			if (_defaultLogLevel > 0) {
				_logLevel = _defaultLogLevel;
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: property: got defaultLogLevel = " + _defaultLogLevel);
			}
		} catch (NumberFormatException exception) {
			_log.error ("ContentFormInfo ctor: error parsing defaultLogLevel value \"" + string + "\", using value " + _defaultLogLevel);
		}
		_log.debug ("ContentFormInfo ctor: _logLevel = " + _logLevel);

		string = properties.getProperty ("defaultProfileLevel", (new Integer (_defaultProfileLevel)).toString ());
		try {
			_defaultProfileLevel = Integer.parseInt (string);
			if (_defaultProfileLevel > 0) {
				_profileLevel = _defaultProfileLevel;
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: property: got defaultProfileLevel = " + _defaultProfileLevel);
			}
		} catch (NumberFormatException exception) {
			_log.error ("ContentFormInfo ctor: error parsing defaultProfileLevel value \"" + string + "\", using value " + _defaultProfileLevel);
		}
		_log.debug ("ContentFormInfo ctor: _profileLevel = " + _profileLevel);

		//handle form checkbox/radiobutton parameters
		Enumeration paramNames = request.getParameterNames ();
		while (paramNames.hasMoreElements ()) {
			String paramName = (String) paramNames.nextElement ();

			if (true) { //debug - log form parameters
				String[] paramValues = request.getParameterValues (paramName);
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: param: " + paramName + " = " + paramValues[0]);
			}

			if (paramName.equals ("debug")) {
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: got debug");
				gotDebugRequest = true;

			} else if (paramName.equals ("collapseGroups")) {
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: got collapseGroups");
				gotCollapseGroups = true;

			} else if (paramName.equals ("reverseSort")) {
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: got reverseSort");
				gotReverseSort = true;

			} else if (paramName.equals ("sortType")) {
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: got sortType = " + request.getParameterValues (paramName)[0]);
				setSortType (request.getParameterValues (paramName)[0]);

			} else if (paramName.equals ("mode")) {
				if (_Debug && debugCtor)
					_log.debug ("ContentFormInfo ctor: got mode = " + request.getParameterValues (paramName)[0]);
				setMode (request.getParameterValues (paramName)[0]);
			}
		}

		_Debug = gotDebugRequest;
		_collapseGroups = gotCollapseGroups;
		_reverseSort = gotReverseSort;
		_testRgbData = gotTestRgbData;
*/
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public String cleanFilter (String filter)
	{
		//remove all white space
		filter = filter.replaceAll ("[ \t]*", "");

		//collapse multiple consecutive commas
		while (filter.contains (",,"))
			filter = filter.replaceAll (",,", ",");

		//remove leading and trailing commas
		if (filter.startsWith (","))
			filter = filter.substring (1, filter.length ());
		if (filter.endsWith (","))
			filter = filter.substring (0, filter.length () - 1);

		return filter;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setFilter1 (String filter)
	{
		_filter1 = cleanFilter (filter);
	}

	public String getFilter1 ()
	{
		return _filter1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setFilter2 (String filter)
	{
		_filter2 = cleanFilter (filter);
	}

	public String getFilter2 ()
	{
		return _filter2;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setFilter3 (String filter)
	{
		_filter3 = cleanFilter (filter);
	}

	public String getFilter3 ()
	{
		return _filter3;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setFilter4 (String filter)
	{
		_filter4 = cleanFilter (filter);
	}

	public String getFilter4 ()
	{
		return _filter4;
	}

	///////////////////////////////////////////////////////////////////////////
	public String[] getFilters ()
	{
		List<String> filters = new ArrayList<String> ();

		if (_filter1.length () > 0)
			filters.addAll (splitString (_filter1));
		if (_filter2.length () > 0)
			filters.addAll (splitString (_filter2));
		if (_filter3.length () > 0)
			filters.addAll (splitString (_filter3));
		if (_filter4.length () > 0)
			filters.addAll (splitString (_filter4));

		return filters.toArray (new String[] {});
	}

	///////////////////////////////////////////////////////////////////////////
	public void setExclude1 (String exclude)
	{
		_exclude1 = cleanFilter (exclude);
	}

	public String getExclude1 ()
	{
		return _exclude1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setExclude2 (String exclude)
	{
		_exclude2 = cleanFilter (exclude);
	}

	public String getExclude2 ()
	{
		return _exclude2;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setExclude3 (String exclude)
	{
		_exclude3 = cleanFilter (exclude);
	}

	public String getExclude3 ()
	{
		return _exclude3;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setExclude4 (String exclude)
	{
		_exclude4 = cleanFilter (exclude);
	}

	public String getExclude4 ()
	{
		return _exclude4;
	}

	///////////////////////////////////////////////////////////////////////////
	public String[] getExcludes ()
	{
		List<String> excludes = new ArrayList<String> ();

		if (_exclude1.length () > 0)
			excludes.addAll (splitString (_exclude1));
		if (_exclude2.length () > 0)
			excludes.addAll (splitString (_exclude2));
		if (_exclude3.length () > 0)
			excludes.addAll (splitString (_exclude3));
		if (_exclude4.length () > 0)
			excludes.addAll (splitString (_exclude4));

		return excludes.toArray (new String[] {});
	}

//	///////////////////////////////////////////////////////////////////////////
//	public void setExtension (String extension)
//	{
//		_extension = extension.replaceAll ("[ \t]*", "");
//	}
*/

	public String getExtension ()
	{
		return _extension;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public void setPanels (int panels)
	{
		_panels = Math.max (panels, 1);
	}

	public int getPanels ()
	{
		return _panels;
	}

	public int getDefaultPanels ()
	{
		return _defaultPanels;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setColumns (int Columns)
	{
		_columns = Math.max (Columns, 1);
		_columns = Math.min (_columns, 32); //avoid issues with doDup
	}

	public int getColumns ()
	{
		return _columns;
	}

	public int getDefaultColumns ()
	{
		return _defaultColumns;
	}

	public int getMaxColumns ()
	{
		return _maxColumns;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setSlice (int slice)
	{
		_slice = Math.max (slice, 1);
	}

	public int getSlice ()
	{
		return _slice;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setSinceDays (double sinceDays)
	{
		_sinceDays = Math.max (sinceDays, 0);
		_sinceInMillis = 0; //cause this to be recalculated
	}

	public double getSinceDays ()
	{
		return _sinceDays;
	}

	public long getSinceInMillis ()
	{
		if (getSinceDays () == 0)
			return 0;

		if (_sinceInMillis == 0) {
			int sinceHours = (int) (-24. * getSinceDays ()); //do math in hours to minimize loss of precision
			GregorianCalendar now = new GregorianCalendar ();
			now.add (Calendar.HOUR, sinceHours);
			_sinceInMillis = now.getTimeInMillis ();

			//truncate to hour boundary
			final long oneHourInMillis = 3600 * 1000;
			_sinceInMillis -= _sinceInMillis % oneHourInMillis;

			if (_Debug) {
				Date since = new Date (_sinceInMillis);
				_log.debug ("ContentFormInfo.getSinceInMillis: since: " + since.toString () + " (truncated to hour boundary)");
			}
		}

		return _sinceInMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setCollapseGroups (boolean collapseGroups)
	{
		_collapseGroups = collapseGroups;
	}

	public boolean getCollapseGroups ()
	{
		return _collapseGroups;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setReverseSort (boolean reverseSort)
	{
		_reverseSort = reverseSort;
	}
*/

	public boolean getReverseSort ()
	{
		return _reverseSort;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public void setWindowWidth (int windowWidth)
	{
		if (windowWidth > 0)
			_windowWidth = windowWidth;
	}

	public int getWindowWidth ()
	{
		return _windowWidth;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setWindowHeight (int windowHeight)
	{
		if (windowHeight > 0)
			_windowHeight = windowHeight;
	}

	public int getWindowHeight ()
	{
		return _windowHeight;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMode (String symbol)
	{
		_mode = ContentMode.getValue (symbol);
	}

	public ContentMode getMode ()
	{
		return _mode;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setSortType (String symbol)
	{
		_sortType = ContentSortType.getValue (symbol);
	}
*/

	public ContentSortType getSortType ()
	{
		return _sortType;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public void setRootFolder (String rootFolder)
	{
		_rootFolder = rootFolder.replaceAll ("[ \t]*", "");
	}

	public String getRootFolder ()
	{
		return _rootFolder;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	public String getRootPath (boolean asUrl)
	{
		if (asUrl)
			return _contentRoot + _rootFolder + "/";
		else
			return _basePath + _rootFolder + "/";
	}

	///////////////////////////////////////////////////////////////////////////
	public String getServer ()
	{
		return _server;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public boolean getTestRgbData ()
	{
		return _testRgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getHighlightDays ()
	{
		return _highlightDays;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getHighlightPixels ()
	{
		return _highlightPixels;
	}

	///////////////////////////////////////////////////////////////////////////
	//handle comma-separated list
	public Collection<String> splitString (String string) //split on commas
	{
		String parts[] = string.split (",");
		List<String> list = Arrays.asList (parts);
		return list;
	}
*/

	//parameters from properties file at %CATALINA_HOME%\webapps\ROOT\WEB-INF\content.properties
	private static final String _propertiesFile = "/WEB-INF/content.properties";
	private int _defaultColumns = 3;
	private int _defaultHighlightDays = 4;
	private int _defaultHighlightPixels = 640;
	private int _defaultLogLevel = 5;
	private int _defaultMaxImageScalePercent = 150;
	private int _defaultPanels = 60;
	private int _defaultProfileLevel = 5;
	private String _defaultRootFolder = "jroot";
	private boolean _defaultTestRgbData = false;

	//private members
	private int _highlightDays = _defaultHighlightDays;
	private int _highlightPixels = _defaultHighlightPixels;
	private int _maxColumns = 24;
	private boolean _testRgbData = _defaultTestRgbData;

	//parameters from URL
	public static String _extension = "mpg"; //note does not include "."
//	public static String _folder = "E:/";
	public static String _rootFolder = "Netscape/Program/v";
//	public static String _rootFolder = "Netscape/Program";
//	public static String _rootFolder = "Netscape/Program/jroot/x";
	public static String _pattern = "*";
/*
	private ContentMode _mode = ContentMode.DoDir;
	private String _filter1 = "";
	private String _filter2 = "";
	private String _filter3 = "";
	private String _filter4 = "";
	private String _exclude1 = "";
	private String _exclude2 = "";
	private String _exclude3 = "";
	private String _exclude4 = "";
	private String _rootFolder = "jroot"; //relative path from root
	private int _columns = _defaultColumns;
	private int _panels = _defaultPanels;
	private int _slice = 1;
	private double _sinceDays = 0; //0 means show all
	private long _sinceInMillis = 0;
	private boolean _collapseGroups = false;
*/
	private boolean _reverseSort = false;
	private ContentSortType _sortType = ContentSortType.ByName;

	//hardcoded values for firefox at 1920 x 1200 resolution and reasonable width
	private int _windowWidth = (1880 * 55) / 100;
	private int _windowHeight = 980;

	//global variables - NOT PART OF BEAN
	public static boolean _Debug = false;
	public static int _profileLevel = 5; //note this value is in use until processRequest() is called
	public static int _logLevel = 5; //note this value is in use until processRequest() is called
//	public static int _maxFilesDir = 300000; //soft limit - used to init collections
//	public static int _maxFilesSubdir = 40000; //soft limit - used to init collections
//	public static int _maxFilePatterns = 3000; //soft limit - used to init collections
//	public static int _maxImageScalePercent; //don't scale images over this size

	private static ContentFormInfo _instance = null;

	private static final String NL = System.getProperty ("line.separator");

//	private static final String _basePath = "E:/Netscape/Program/"; //need trailing slash
	private static /*final*/ String _basePath = "E:/"; //need trailing slash
	private static /*final*/ String _contentRoot = "/contentRootE/"; //should match tomcat's server.xml
	private static final String _server = "/servlet/ContentServlet.ContentServlet";

	private static Logger _log = LogManager.getLogger (ContentFormInfo.class);
}
