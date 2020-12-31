//AlbumFormInfo.java

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

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class AlbumFormInfo
{
	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumFormInfo getInstance ()
	{
		if (_instance == null) {
			_instance = new AlbumFormInfo ();
		}

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumFormInfo newInstance ()
	{
		_instance = null; //force creation of new instance
		_instance = new AlbumFormInfo ();

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumFormInfo ()
	{
		_mode = AlbumMode.DoDir;
//		_subFolders = "";
		_filter1 = "";
		_filter2 = "";
		_filter3 = "";

		for (int ii = 0; ii < _NumTagParams; ii++) {
			_tagMode.add (AlbumTagMode.TagIn);
			_tagValue.add ("");
		}

		_exclude1 = "";
		_exclude2 = "";
		_exclude3 = "";
		_rootFolder = _defaultRootFolder;
		_columns = _defaultColumns;
		_panels = _defaultPanels;
		_slice = 1;
		_sinceDays = 0; //0 means show all
		_maxFilters = _defaultMaxFilters;
		_highlightDays = _defaultHighlightDays;
		_maxStdDev = _defaultMaxStdDev;
		_exifDateIndex = _defaultExifDateIndex;
		_tagFilterOperandOr = false;
		_collapseGroups = false;
		_limitedCompare = false;
		_dbCompare = false;
		_looseCompare = false;
		_ignoreBytes = false;
		_useExifDates = false;
		_duplicateHandling = AlbumDuplicateHandling.SelectNone;
		_orientation = AlbumOrientation.ShowAny;
		_useCase = false;
		_clearCache = false;
		_reverseSort = false;
		_sortType = AlbumSortType.ByName;
		_forceBrowserCacheRefresh = false;

		_highlightMinPixels = _defaultHighlightMinPixels;
		_highlightMaxKilobytes = _defaultHighlightMaxKilobytes;
		_logLevel = _defaultLogLevel;
		_maxImageScalePercent = _defaultMaxImageScalePercent;
		_profileLevel = _defaultProfileLevel;
		_showRgbData = _defaultShowRgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	//handle non-primitive params and defaults from the properties file
	//Note that primitive params are handled after this, by the call to AlbumBeanUtilities.populateBean() in AlbumServlet.doGet()
	public void processRequest (HttpServletRequest request, ServletContext context)
	{
		_isServlet = true;

		if (_debugProperties) {
			List<String> headerNames = Collections.list (request.getHeaderNames ()).stream ().sorted (VendoUtils.caseInsensitiveStringComparator).collect(Collectors.toList ());
			for (String headerName : headerNames) {
				List<String> headerValues = Collections.list (request.getHeaders (headerName)).stream ().sorted (VendoUtils.caseInsensitiveStringComparator).collect(Collectors.toList ());
				for (String headerValue : headerValues) {
					_log.debug ("AlbumFormInfo.processRequest: header: " + headerName + " = " + headerValue);
				}
			}
		}

		List<String> paramNames = Collections.list (request.getParameterNames ()).stream ().sorted (VendoUtils.caseInsensitiveStringComparator).collect(Collectors.toList ());
		for (String paramName : paramNames) {
			if (_debugProperties) { //debug - log form parameters
				List<String> paramValues = Arrays.asList (request.getParameterValues (paramName)).stream ().sorted (VendoUtils.caseInsensitiveStringComparator).collect(Collectors.toList ());
				for (String paramValue : paramValues) {
					_log.debug ("AlbumFormInfo.processRequest: param: " + paramName + " = " + paramValue);
				}
			}

			if (paramName.equals ("rootFolder")) {
				if (_debugProperties) {
					_log.debug ("AlbumFormInfo.processRequest: got rootFolder = " + request.getParameterValues (paramName)[0]);
				}
				_rootFolder = request.getParameterValues (paramName)[0];

			} else if (paramName.equals ("duplicateHandling")) {
				if (_debugProperties) {
					_log.debug ("AlbumFormInfo.processRequest: got duplicateHandling = " + request.getParameterValues (paramName)[0]);
				}
				setDuplicateHandling (request.getParameterValues (paramName)[0]);

			} else if (paramName.equals ("orientation")) {
				if (_debugProperties) {
					_log.debug ("AlbumFormInfo.processRequest: got orientation = " + request.getParameterValues (paramName)[0]);
				}
				setOrientation (request.getParameterValues (paramName)[0]);

			} else if (paramName.equals ("sortType")) {
				if (_debugProperties) {
					_log.debug ("AlbumFormInfo.processRequest: got sortType = " + request.getParameterValues (paramName)[0]);
				}
				setSortType (request.getParameterValues (paramName)[0]);

			} else if (paramName.equals ("mode")) {
				if (_debugProperties) {
					_log.debug ("AlbumFormInfo.processRequest: got mode = " + request.getParameterValues (paramName)[0]);
				}
				setMode (request.getParameterValues (paramName)[0]);

			} else {
				for (int ii = 0; ii < _NumTagParams; ii++) {
					if (paramName.equals ("tagMode" + ii)) {
						if (_debugProperties) {
							_log.debug ("AlbumFormInfo.processRequest: got tagMode" + ii + " = " + request.getParameterValues (paramName)[0]);
						}
						setTagMode (ii, request.getParameterValues (paramName)[0]);

					} else if (paramName.equals ("tag" + ii)) {
						if (_debugProperties) {
							_log.debug ("AlbumFormInfo.processRequest: got tag" + ii + " = " + request.getParameterValues (paramName)[0]);
						}
						setTag (ii, request.getParameterValues (paramName)[0]);
					}
				}
			}
		}

		//handle values in properties file
		Properties properties = new Properties ();
		try (InputStream inputStream = context.getResourceAsStream (_propertiesFile)) {
			properties.load (inputStream);

		} catch (Exception ee) {
			_log.error ("AlbumFormInfo.processRequest: failed to read properties file \"" + _propertiesFile + "\"", ee);
		}

		//boolean properties
		_Debug = getPropertyBoolean (properties, "debug", false);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: debug = " + _Debug);
		}

		_showRgbData = getPropertyBoolean (properties, "showRgbData", false);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: showRgbData = " + _showRgbData);
		}

		//int properties
		_columns = _defaultColumns = getPropertyInt (properties, "defaultColumns", _defaultColumns);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultColumns = " + _defaultColumns);
		}

		_maxFilters = _defaultMaxFilters = getPropertyInt (properties, "defaultMaxFilters", _defaultMaxFilters);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultMaxFilters = " + _defaultMaxFilters);
		}

		_highlightDays = _defaultHighlightDays = getPropertyInt (properties, "defaultHighlightDays", _defaultHighlightDays);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultHighlightDays = " + _defaultHighlightDays);
		}

		_highlightMinPixels = _defaultHighlightMinPixels = getPropertyInt (properties, "defaultHighlightMinPixels", _defaultHighlightMinPixels);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultHighlightMinPixels = " + _defaultHighlightMinPixels);
		}

		_highlightMaxKilobytes = _defaultHighlightMaxKilobytes = getPropertyInt (properties, "defaultHighlightMaxKilobytes", _defaultHighlightMaxKilobytes);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultHighlightMaxKilobytes = " + _defaultHighlightMaxKilobytes);
		}

		_maxImageScalePercent = _defaultMaxImageScalePercent = getPropertyInt (properties, "defaultMaxImageScalePercent", _defaultMaxImageScalePercent);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultMaxImageScalePercent = " + _defaultMaxImageScalePercent);
		}

		_maxStdDev = _defaultMaxStdDev = getPropertyInt (properties, "defaultMaxStdDev", _defaultMaxStdDev);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultMaxStdDev = " + _defaultMaxStdDev);
		}

		_panels = _defaultPanels = getPropertyInt (properties, "defaultPanels", _defaultPanels);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultPanels = " + _defaultPanels);
		}

		_exifDateIndex = _defaultExifDateIndex = getPropertyInt (properties, "defaultExifDateIndex", _defaultExifDateIndex);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultExifDateIndex = " + _defaultExifDateIndex);
		}

		_logLevel = _defaultLogLevel = getPropertyInt (properties, "defaultLogLevel", _defaultLogLevel);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultLogLevel = " + _defaultLogLevel);
		}

		_profileLevel = _defaultProfileLevel = getPropertyInt (properties, "defaultProfileLevel", _defaultProfileLevel);
		if (_debugProperties) {
			_log.debug ("AlbumFormInfo.processRequest: property: defaultProfileLevel = " + _defaultProfileLevel);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public int getPropertyInt (Properties properties, String propertyName, int defaultValue)
	{
		final String invalidString = new Integer (-1).toString ();

		String valueString = properties.getProperty (propertyName, invalidString);

		//now try to parse value
		int value = defaultValue; //default value if parsing fails
		try {
			int intValue = Integer.parseInt (valueString);
			if (intValue >= 0) { //ignore negative values
				value = intValue;
			}

		} catch (NumberFormatException exception) {
			_log.error ("AlbumFormInfo.getPropertyInt: error parsing property \"" + propertyName + "\" value \"" + valueString + "\", using value " + defaultValue);
		}

		return value;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean getPropertyBoolean (Properties properties, String propertyName, boolean defaultValue)
	{
		final String invalidString = "invalid";

		String valueString = properties.getProperty (propertyName, invalidString);
		Boolean booleanValue = BooleanUtils.toBooleanObject (valueString);

		if (booleanValue == null) {
			booleanValue = defaultValue;
			_log.error ("AlbumFormInfo.getPropertyBoolean: error parsing property \"" + propertyName + "\" value \"" + valueString + "\", using value " + defaultValue);
		}

		return booleanValue;
	}

	///////////////////////////////////////////////////////////////////////////
	public synchronized void addServletError (String servletError)
	{
		if (_isServlet) {
			_servletErrors.add (servletError);
		}
	}
	public synchronized int getNumServletErrors ()
	{
		return getServletErrors ().size ();
//		return _servletErrors.size ();
	}

	public Collection<String> getServletErrors ()
	{
		Collection<String> servletErrors = new TreeSet<> (new AlphanumComparator()); //use set to avoid dups
		servletErrors.addAll (_servletErrors);
		servletErrors.addAll (AlbumImageDao.getInstance ().getServletErrors ());

		return servletErrors;
	}

	///////////////////////////////////////////////////////////////////////////
	public String cleanFilter (String filter)
	{
		if (filter.isEmpty ()) {
			return filter;
		}

		//accept URL as input and try to create potentially useful filter from it
		try {
			//first try to parse as URL, which will throw exception if not valid, then continue below
			/*URL url =*/ new URL (filter);

			//then parse as URI, which allows us to extract components
			URI uri = new URI (filter);

			filter = uri.getPath ().toLowerCase ();

			filter = filter.replaceAll ("\\.jpg$", ""); //regex - remove trailing extension
			filter = filter.replaceAll ("[0-9]+", "/"); //regex - replace all digits
			filter = filter.replaceAll ("\\.", "/"); //regex - replace all dots "."
			filter = filter.replaceAll ("-", ""); //regex - remove all dashes
			filter = filter.replaceAll ("_", ""); //regex - remove all underscores
			filter = filter.replaceAll ("\\+", "\\*"); //regex - replace all pluses "+"
			filter = filter.replaceAll ("/[a-zA-z]{1,2}/", "/"); //regex - replace all single-char components
			filter = filter.replaceAll ("/", ","); //regex - replace all slashes

			//sort and de-dup the list
			filter = VendoUtils.collectionToString (
					Arrays.stream (StringUtils.split (filter, ','))
							.sorted (VendoUtils.caseInsensitiveStringComparator)
							.distinct ()
							.filter (s -> !s.equalsIgnoreCase ("thumb") && !s.equalsIgnoreCase ("thumbs") && !s.equalsIgnoreCase ("kha"))
							.collect (Collectors.toList ()), ",");

			//drop through to continue processing

		} catch (Exception ee) {
			//not URL; ignore exception and drop through to continue processing
		}

		//convert strings " and " and " & " into a comma
		filter = filter.replaceAll (" and ", ","); //regex
		filter = filter.replaceAll ("\\*and\\*", ","); //regex
		filter = filter.replaceAll (" & ", ","); //regex

		//convert " aka " into a comma
		filter = filter.replaceAll (" aka ", ","); //regex
		filter = filter.replaceAll (" AKA ", ","); //regex

		//remove all white space
		filter = filter.replaceAll ("[ \t]*", ""); //regex

		//remove all periods
//		filter = filter.replaceAll ("\\.", ""); //regex

		//remove all exclamation points
		filter = filter.replaceAll ("!", ""); //regex

		//remove all single quotes
		filter = filter.replaceAll ("'", ""); //regex

		//convert equals signs to commas
		filter = filter.replaceAll ("\\=", ","); //regex

		//convert colons to commas
//		filter = filter.replaceAll (":", ","); //regex

		//remove filters that are only "+"
		if (filter.startsWith ("+")) {
			filter = filter.substring (1, filter.length ());
		}
		while (filter.contains (",+")) { //not regex
			filter = filter.replaceAll (",\\+", ","); //regex
		}

		//convert strings enclosed in parentheses to commas
		if (filter.contains ("(") && filter.contains (")")) {
			filter = filter.replaceAll ("\\s*\\([^\\)]*\\)\\s*", ","); //regex
		}

		//remove partial filters like [145] and [3-5]
		String numberRangeRegex = "\\[[0-9-]*\\]";
		if (filter.contains (",[")) {
			filter = filter.replaceAll ("," + numberRangeRegex, ","); //regex
		}
		if (filter.startsWith ("[")) {
			filter = filter.replaceFirst (numberRangeRegex, ","); //regex
		}

		//collapse multiple consecutive commas
		while (filter.contains (",,")) {
			filter = filter.replaceAll (",,", ","); //regex
		}

		//remove leading and trailing commas
		if (filter.startsWith (",")) {
			filter = filter.substring (1, filter.length ());
		}
		if (filter.endsWith (",")) {
			filter = filter.substring (0, filter.length () - 1);
		}

		_log.debug ("AlbumFormInfo.cleanFilter: filter: \"" + filter + "\"");

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
	//sorts (case insensitive) and dedups the list before returning it
	public String[] getFilters ()
	{
		return getFilters (0); //index = 0 means get all filters
	}

	///////////////////////////////////////////////////////////////////////////
	//sorts (case insensitive) and dedups the list before returning it
	//index specifies which filter, 0 means all
	public String[] getFilters (int index)
	{
		List<String> filters = new ArrayList<String> ();

		if ((index == 0 || index == 1) && _filter1.length () > 0) {
			filters.addAll (splitString (_filter1));
		}
		if ((index == 0 || index == 2) && _filter2.length () > 0) {
			filters.addAll (splitString (_filter2));
		}
		if ((index == 0 || index == 3) && _filter3.length () > 0) {
			filters.addAll (splitString (_filter3));
		}

		filters.sort (VendoUtils.caseInsensitiveStringComparator);
		caseInsensitiveDedup (filters);

		return filters.toArray (new String[] {});
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean filtersHaveWildCards ()
	{
		final Predicate<String> wildcards = Pattern.compile ("[+*\\[\\]]").asPredicate (); //regex: prevent: '+', '*', '[', or ']'

		return Arrays.stream (getFilters ()).anyMatch (wildcards);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setTagMode (int index, String symbol)
	{
		_tagMode.set (index, AlbumTagMode.getValue (symbol));
	}

	public AlbumTagMode getTagMode (int index)
	{
		return _tagMode.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setTag (int index, String tagValue)
	{
		_tagValue.set (index, tagValue);
	}

	public String getTag (int index)
	{
		return _tagValue.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	public String[] getTags (AlbumTagMode tagMode)
	{
		List<String> tags = new ArrayList<String> ();

		for (int ii = 0; ii < AlbumFormInfo._NumTagParams; ii++) {
			if (_tagMode.get (ii) == tagMode && _tagValue.get (ii).length () > 0) {
				tags.addAll (splitString (_tagValue.get (ii)));
			}
		}

		tags.sort (VendoUtils.caseInsensitiveStringComparator);
		caseInsensitiveDedup (tags);

		return tags.toArray (new String[] {});
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
	//sorts (case insensitive) and dedups the list before returning it
	public String[] getExcludes ()
	{
		List<String> excludes = new ArrayList<String> ();

		if (_exclude1.length () > 0) {
			excludes.addAll (splitString (_exclude1));
		}
		if (_exclude2.length () > 0) {
			excludes.addAll (splitString (_exclude2));
		}
		if (_exclude3.length () > 0) {
			excludes.addAll (splitString (_exclude3));
		}

		excludes.sort (VendoUtils.caseInsensitiveStringComparator);
		caseInsensitiveDedup (excludes);

		return excludes.toArray (new String[] {});
	}

//	///////////////////////////////////////////////////////////////////////////
//	public void setExtension (String extension)
//	{
//		_extension = extension.replaceAll ("[ \t]*", ""); //regex
//	}
//
//	public String getExtension ()
//	{
//		return _Extension;
//	}

	///////////////////////////////////////////////////////////////////////////
	public void setMaxStdDev (int maxStdDev)
	{
		_maxStdDev = Math.max (maxStdDev, 0);
	}

	public int getMaxStdDev ()
	{
		return _maxStdDev;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setPanels (int panels)
	{
		_panels = Math.max (panels, 1);
	}

	public int getPanels ()
	{
		return _panels;
	}

//not currently used
//	public int getDefaultPanels ()
//	{
//		return _defaultPanels;
//	}

	///////////////////////////////////////////////////////////////////////////
	public void setExifDateIndex (int exifDateIndex)
	{
		if (exifDateIndex < 0 || exifDateIndex >= AlbumImage.NumExifDates) {
			exifDateIndex = _defaultExifDateIndex;
		}

		_exifDateIndex = exifDateIndex;
	}

	public int getExifDateIndex ()
	{
		return _exifDateIndex;
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
		_sinceInMillis = -1; //cause this to be recalculated
	}

	public double getSinceDays ()
	{
		return _sinceDays;
	}

	public long getSinceInMillis (boolean truncateToMidnightBoundary)
	{
		if (getSinceDays () == 0) { //0 means disabled
			return 0;
		}

		if (_sinceInMillis < 0) {
			_highlightInMillis = getMillisFromDays (getHighlightDays (), truncateToMidnightBoundary);
		}

		return _sinceInMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setTagFilterOperandOr (boolean tagFilterOperandOr)
	{
		_tagFilterOperandOr = tagFilterOperandOr;
	}

	public boolean getTagFilterOperandOr ()
	{
		return _tagFilterOperandOr;
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
	public void setLimitedCompare (boolean limitedCompare)
	{
		_limitedCompare = limitedCompare;
	}

	public boolean getLimitedCompare ()
	{
		return _limitedCompare;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setDbCompare (boolean dbCompare)
	{
		_dbCompare = dbCompare;
	}

	public boolean getDbCompare ()
	{
		return _dbCompare;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setLooseCompare (boolean looseCompare)
	{
		_looseCompare = looseCompare;
	}

	public boolean getLooseCompare ()
	{
		return _looseCompare;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setIgnoreBytes (boolean ignoreBytes)
	{
		_ignoreBytes = ignoreBytes;
	}

	public boolean getIgnoreBytes ()
	{
		return _ignoreBytes;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setUseExifDates (boolean useExifDates)
	{
		_useExifDates = useExifDates;
	}

	public boolean getUseExifDates ()
	{
		return _useExifDates;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setUseCase (boolean useCase)
	{
		_useCase = useCase;
	}

	public boolean getUseCase ()
	{
		return _useCase;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setClearCache (boolean clearCache)
	{
		_clearCache = clearCache;
	}

	public boolean getClearCache ()
	{
		return _clearCache;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setForceBrowserCacheRefresh (boolean forceBrowserCacheRefresh)
	{
		_forceBrowserCacheRefresh = forceBrowserCacheRefresh;
	}

	public boolean getForceBrowserCacheRefresh ()
	{
		return _forceBrowserCacheRefresh;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setReverseSort (boolean reverseSort)
	{
		_reverseSort = reverseSort;
	}

	public boolean getReverseSort ()
	{
		return _reverseSort;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setScreenWidth (int screenWidth)
	{
		if (screenWidth > 0) {
			_screenWidth = screenWidth;
		}
	}

	public int getScreenWidth ()
	{
//		return (isAndroidDevice () ? 2 : 1) * _screenWidth; //hack - Nexus 7 uses Density Independent Pixel (commonly referred to as dp) and reports half as many pixels
		return _screenWidth;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setScreenHeight (int screenHeight)
	{
		if (screenHeight > 0) {
			_screenHeight = screenHeight;
		}
	}

	public int getScreenHeight ()
	{
//		return (isAndroidDevice () ? 2 : 1) * _screenHeight; //hack - Nexus 7 uses Density Independent Pixel (commonly referred to as dp) and reports half as many pixels
		return _screenHeight;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setWindowWidth (int windowWidth)
	{
		if (windowWidth > 0) {
			_windowWidth = windowWidth;
		}
	}

	public int getWindowWidth ()
	{
		return _windowWidth;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setWindowHeight (int windowHeight)
	{
		if (windowHeight > 0) {
			_windowHeight = windowHeight;
		}
	}

	public int getWindowHeight ()
	{
		return _windowHeight;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setUserAgent (String userAgent)
	{
		_userAgent = userAgent;
	}

	public String getUserAgent ()
	{
		return _userAgent;
	}

	public boolean isAndroidDevice ()
	{
//		_log.debug ("AlbumFormInfo.isAndroidDevice: _userAgent = " + _userAgent);
		return _userAgent.toLowerCase ().contains ("android");
	}

//	public boolean isNexus7Device ()
//	{
//		return _userAgent.toLowerCase ().contains ("nexus 7");
//	}

	///////////////////////////////////////////////////////////////////////////
	public void setMode (String symbol)
	{
		_mode = AlbumMode.getValue (symbol);
	}

	public AlbumMode getMode ()
	{
		return _mode;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setDuplicateHandling (String symbol)
	{
		_duplicateHandling = AlbumDuplicateHandling.getValue (symbol);
	}

	public AlbumDuplicateHandling getDuplicateHandling ()
	{
		return _duplicateHandling;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setOrientation (String symbol)
	{
		_orientation = AlbumOrientation.getValue (symbol);
	}

	public AlbumOrientation getOrientation ()
	{
		return _orientation;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setSortType (String symbol)
	{
		_sortType = AlbumSortType.getValue (symbol);
	}

	public AlbumSortType getSortType ()
	{
		return _sortType;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRootFolder (String rootFolder)
	{
		_rootFolder = rootFolder.replaceAll ("[ \t]*", ""); //regex
	}

	public String getRootFolder ()
	{
		return _rootFolder;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getRootPath (boolean asUrl)
	{
		if (asUrl) {
			return _albumRoot + _rootFolder + "/";
		} else {
			return _basePath + _rootFolder + "/";
		}
	}

	///////////////////////////////////////////////////////////////////////////
//	public String getSubFolders ()
//	{
//		return _subFolders;
//	}

	///////////////////////////////////////////////////////////////////////////
	public String getServer ()
	{
		return _server;
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean getShowRgbData ()
	{
		return _showRgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMaxFilters (int maxFilters)
	{
		_maxFilters = Math.max (maxFilters, 0);
	}

	public int getMaxFilters ()
	{
		return _maxFilters;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setHighlightDays (double highlightDays)
	{
		_highlightDays = Math.max (highlightDays, 0);
		_highlightInMillis = -1; //cause this to be recalculated
	}

	public double getHighlightDays ()
	{
		return _highlightDays;
	}

	public long getHighlightInMillis (boolean truncateToMidnightBoundary)
	{
		if (getHighlightDays () == 0) { //0 means disabled
			return 0;
		}

		if (_highlightInMillis < 0) {
			_highlightInMillis = getMillisFromDays (getHighlightDays (), truncateToMidnightBoundary);
		}

		return _highlightInMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getHighlightMinPixels ()
	{
		return _highlightMinPixels;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getHighlightMaxKilobytes ()
	{
		return _highlightMaxKilobytes;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getMethod ()
	{
		return "get";
	}

	///////////////////////////////////////////////////////////////////////////
	public static long getMillisFromDays (double days, boolean truncateToMidnightBoundary)
	{
		Calendar date = new GregorianCalendar (); //now

		if (truncateToMidnightBoundary) {
			//truncate to last night at midnight; days = 1 means last night at midnight, 2 means yesterday at midnight, etc.
			date.set (Calendar.HOUR_OF_DAY, 0);
			date.set (Calendar.MINUTE, 0);
			date.set (Calendar.SECOND, 0);
			date.set (Calendar.MILLISECOND, 0);

			date.add (Calendar.DAY_OF_MONTH, 1 - (int) days);

		} else {
			double minutes = 60 * 24 * days;

			date.add (Calendar.MINUTE, (int) -minutes);
		}

		long millis = date.getTimeInMillis ();

//		if (_Debug) {
//			String highlightStr = _dateFormat.format (new Date (millis));
//			_log.debug ("AlbumFormInfo.getMillisFromDays(" + days + ", " + truncateToMidnightBoundary + "): highlight date: " + highlightStr);
//		}

		return millis;
	}

	///////////////////////////////////////////////////////////////////////////
	//handle comma-separated list
	public static Collection<String> splitString (String string)
	{
		String parts[] = string.split (","); //split on commas
		List<String> list = Arrays.asList (parts);
		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	public static void caseInsensitiveDedup (Collection<String> strings)
	{
		List<String> toBeRemoved = new ArrayList<String> ();

		String prevString = "";
		for (String string : strings) {
			if (prevString.length () != 0 && prevString.compareToIgnoreCase (string) == 0) {
				toBeRemoved.add (string);
			}
			prevString = string;
		}

		for (String string : toBeRemoved) {
			strings.remove (string);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static String stripImageExtension (String name)
	{
		final String extension = AlbumFormInfo._ImageExtension;
		final int length = extension.length ();

		if (name.endsWith (extension)) {
			name = name.substring (0, name.length () - length);
		}

		return name;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String convertWildcardsToSqlRegex (String string)
	{
		//Note: SQL regex assumes leading and trailing wildcards (matches regex anywhere in string)

		if (string.startsWith ("*")) {
			string = string.substring (1); //strip leading "*"
		} else {
			string = "^" + string; //anchor match to start of string
		}

		return convertWildcardsToRegex (string);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String convertWildcardsToRegex (String string)
	{
		if (!string.endsWith ("*")){
			string += "*"; //add trailing string
		}

		string = string.replace (".", "[a-z]") //dot/period here means any alpha
					   .replace ("*", ".*")
					   .replace (";", "[aeiouy]") //semicolon here means vowel or 'y'
//this syntax doesn't work with MySQL? .replace (":", "[a-z&&[^aeiouy]]") //colon here means any letter except vowel or 'y'
					   .replace (":", "[b-df-hj-np-tv-xz]") //colon here means any letter except vowel or 'y'
					   .replace ("+", "[\\d]"); //plus sign here means digit

		return string;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String convertRegexToWildcards (String string)
	{
		string = string.replace (".*", "*")
					   .replace ("[a-z]", ".") //dot/period here means any alpha
					   .replace ("[aeiouy]", ";") //semicolon here means vowel or 'y'
//this syntax doesn't work with MySQL? .replace ("[a-z&&[^aeiouy]]", ":") //colon here means any letter except vowel or 'y'
					   .replace ("[b-df-hj-np-tv-xz]", ":") //colon here means any letter except vowel or 'y'
					   .replace ("[\\d]", "+"); //plus sign here means digit

		if (string.endsWith ("*")) {
			string = string.substring (0, string.length () - 1); //remove trailing string
		}

		return string;
	}

	//parameters from properties file at "%CATALINA_HOME%"\webapps\AlbumServlet\WEB-INF\classes\album.properties
	private static final String _propertiesFile = "/WEB-INF/classes/album.properties";
	private int _defaultColumns = 3;
	private int _defaultMaxFilters = 500;
	private int _defaultHighlightDays = 4;
	private int _defaultHighlightMinPixels = 640;
	private int _defaultHighlightMaxKilobytes = 1536;
	private int _defaultLogLevel = 5;
	private int _defaultMaxImageScalePercent = 150;
	private int _defaultMaxStdDev = 15;
	private int _defaultPanels = 60;
	private int _defaultExifDateIndex = 4; //earliestExifDate
	private int _defaultProfileLevel = 5;
	private boolean _defaultShowRgbData = false;

	//private members
	private boolean _isServlet = false;
	private boolean _forceBrowserCacheRefresh = false;
	private String _defaultRootFolder = "jroot";
//	private String _subFolders = "";
	private int _highlightMinPixels = _defaultHighlightMinPixels;
	private int _highlightMaxKilobytes = _defaultHighlightMaxKilobytes;
	private int _maxColumns = 32;
	private Collection<String> _servletErrors = new HashSet<String> (); //use set to avoid dups

	//parameters from URL
	private AlbumMode _mode = AlbumMode.DoDir;
	private String _filter1 = "";
	private String _filter2 = "";
	private String _filter3 = "";
	private List<AlbumTagMode> _tagMode = new ArrayList<AlbumTagMode> ();
	private List<String> _tagValue = new ArrayList<String> ();
	private String _exclude1 = "";
	private String _exclude2 = "";
	private String _exclude3 = "";
	private String _rootFolder = _defaultRootFolder; //relative path from root
	private int _columns = _defaultColumns;
	private int _panels = _defaultPanels;
	private int _slice = 1;
	private double _sinceDays = 0; //0 means show all
	private long _sinceInMillis = -1;
	private int _maxFilters = _defaultMaxFilters;
 	private double _highlightDays = _defaultHighlightDays;
	private long _highlightInMillis = -1;
	private int _maxStdDev = _defaultMaxStdDev;
	private int _exifDateIndex = _defaultExifDateIndex; //when sorting/comparing EXIF dates, specifies which date to use
	private boolean _tagFilterOperandOr = false;
	private boolean _collapseGroups = false;
	private boolean _limitedCompare = false; //don't include dups that share common basename
	private boolean _dbCompare = false; //compare dups retrieved from imgage_diffs table
	private boolean _looseCompare = false; //compare dups with either loose or strict critera
	private boolean _ignoreBytes = false; //optionally tell compare dups to ignore number of bytes
	private boolean _useExifDates = false; //optionally tell compare dups to use the EXIF date
	private boolean _useCase = false;
	private boolean _clearCache = false;
	private boolean _reverseSort = false;
	private AlbumDuplicateHandling _duplicateHandling = AlbumDuplicateHandling.SelectNone;
	private AlbumOrientation _orientation = AlbumOrientation.ShowAny;
	private AlbumSortType _sortType = AlbumSortType.ByName;
	private String _userAgent = "";

	//hardcoded values for firefox at 1920 x 1200 resolution and reasonable width
	private int _windowWidth = (1880 * 55) / 100;
	private int _windowHeight = 980;
	private int _screenWidth = _windowWidth;
	private int _screenHeight = _windowHeight;

	public static final String _ImageExtension = ".jpg";
	public static final String _RgbDataExtension = ".dat";
	public static final String _DeleteParam1 = "DeleteImage:";
	public static final String _DeleteParam2 = "DeleteImages:";
	public static final String _DeleteSuffix = ".delete";

	public static final int _NumTagParams = 9;

	//global variables - NOT PART OF BEAN
	public static boolean _Debug = false;
	public static int _profileLevel = 5; //note this value is in use until processRequest() is called
	public static int _logLevel = 5; //note this value is in use until processRequest() is called
	public static int _maxFilesDir = 300000; //soft limit - used to init collections
	public static int _maxFilesSubdir = 40000; //soft limit - used to init collections
	public static int _maxFilePatterns = 3000; //soft limit - used to init collections
	public static int _maxImageScalePercent; //don't scale images over this size

	private static boolean _showRgbData = false;

	private static AlbumFormInfo _instance = null;

	private final boolean _debugProperties = false;

	private static final String _basePath = "D:/Netscape/Program/"; //need trailing slash
	private static final String _albumRoot = "/albumRoot/"; //should match tomcat's server.xml
	private static final String _server = "/AlbumServlet/AlbumServlet";

	private static Logger _log = LogManager.getLogger ();
}
