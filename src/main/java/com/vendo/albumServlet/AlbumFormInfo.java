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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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
		_subFolders = "";
		_filter1 = "";
		_filter2 = "";
		_filter3 = "";
		_servletErrors = new ArrayList<String> ();

		for (int ii = 0; ii < _NumTagParams; ii++) {
			_tagMode.add (AlbumTagMode.TagIn);
			_tagValue.add (new String ());
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
		_maxRgbDiffs = _defaultMaxRgbDiffs;
		_exifDateIndex = _defaultExifDateIndex;
		_tagFilterOperandOr = false;
		_collapseGroups = false;
		_limitedCompare = false;
		_looseCompare = false;
		_ignoreBytes = false;
		_useExifDates = false;
		_orientation = AlbumOrientation.ShowAny;
		_useCase = false;
		_clearCache = false;
		_reverseSort = false;
		_sortType = AlbumSortType.ByName;

		_highlightMinPixels = _defaultHighlightMinPixels;
		_highlightMaxKilobytes = _defaultHighlightMaxKilobytes;
		_logLevel = _defaultLogLevel;
		_maxImageScalePercent = _defaultMaxImageScalePercent;
		_profileLevel = _defaultProfileLevel;
		_showRgbData = _defaultShowRgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	public void processRequest (HttpServletRequest request, ServletContext context)
	{
		//booleans
		boolean gotDebugRequest = false;
		boolean gotFilterOperandOr = false;
		boolean gotCollapseGroups = false;
		boolean gotLimitedCompare = false;
		boolean gotLooseCompare = false;
		boolean gotIgnoreBytes = false;
		boolean gotUseExifDates = false;
		boolean gotUseCase = false;
		boolean gotClearCache = false;
		boolean gotReverseSort = false;
		boolean gotShowRgbData = false;

		if (_debugProperties) {
			List<String> headerNames = Collections.list (request.getHeaderNames ());
			Collections.sort (headerNames, AlbumFormInfo.caseInsensitiveStringComparator);
			for (String headerName : headerNames) {
				List<String> headerValues = Collections.list (request.getHeaders (headerName));
				Collections.sort (headerValues, AlbumFormInfo.caseInsensitiveStringComparator);
				for (String headerValue : headerValues) {
					_log.debug ("AlbumFormInfo.processRequest: header: " + headerName + " = " + headerValue);
				}
			}
		}

		//handle URL parameters and form checkbox/radiobutton parameters
		List<String> paramNames = Collections.list (request.getParameterNames ());
		Collections.sort (paramNames, AlbumFormInfo.caseInsensitiveStringComparator);
		for (String paramName : paramNames) {

			if (_debugProperties) { //debug - log form parameters
				List<String> paramValues = Arrays.asList (request.getParameterValues (paramName));
				Collections.sort (paramValues, AlbumFormInfo.caseInsensitiveStringComparator);
				for (String paramValue : paramValues) {
					_log.debug ("AlbumFormInfo.processRequest: param: " + paramName + " = " + paramValue);
				}
			}

			if (paramName.equals ("rootFolder")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got rootFolder = " + request.getParameterValues (paramName)[0]);
				_rootFolder = request.getParameterValues (paramName)[0];

			} else if (paramName.equals ("debug")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got debug");
				gotDebugRequest = true;

			} else if (paramName.equals ("filterOperandOr")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got filterOperandOr");
				gotFilterOperandOr = true;

			} else if (paramName.equals ("collapseGroups")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got collapseGroups");
				gotCollapseGroups = true;

			} else if (paramName.equals ("limitedCompare")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got limitedCompare");
				gotLimitedCompare = true;

			} else if (paramName.equals ("looseCompare")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got looseCompare");
				gotLooseCompare = true;

			} else if (paramName.equals ("ignoreBytes")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got ignoreBytes");
				gotIgnoreBytes = true;

			} else if (paramName.equals ("useExifDates")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got useExifDates");
				gotUseExifDates = true;

			} else if (paramName.equals ("useCase")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got useCase");
				gotUseCase = true;

			} else if (paramName.equals ("clearCache")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got clearCache");
				gotClearCache = true;

			} else if (paramName.equals ("reverseSort")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got reverseSort");
				gotReverseSort = true;

			} else if (paramName.equals ("orientation")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got orientation = " + request.getParameterValues (paramName)[0]);
				setOrientation (request.getParameterValues (paramName)[0]);

			} else if (paramName.equals ("sortType")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got sortType = " + request.getParameterValues (paramName)[0]);
				setSortType (request.getParameterValues (paramName)[0]);

			} else if (paramName.equals ("mode")) {
				if (_debugProperties)
					_log.debug ("AlbumFormInfo.processRequest: got mode = " + request.getParameterValues (paramName)[0]);
				setMode (request.getParameterValues (paramName)[0]);

			} else {
				for (int ii = 0; ii < _NumTagParams; ii++) {
					if (paramName.equals ("tagMode" + ii)) {
						if (_debugProperties)
							_log.debug ("AlbumFormInfo.processRequest: got tagMode" + ii + " = " + request.getParameterValues (paramName)[0]);
						setTagMode (ii, request.getParameterValues (paramName)[0]);
					}

					if (paramName.equals ("tag" + ii)) {
						if (_debugProperties)
							_log.debug ("AlbumFormInfo.processRequest: got tag" + ii + " = " + request.getParameterValues (paramName)[0]);
						setTag (ii, request.getParameterValues (paramName)[0]);
					}
				}
			}
		}

		//handle values in properties file
		Properties properties = new Properties ();
		InputStream inputStream = null;
		try {
			inputStream = context.getResourceAsStream (_propertiesFile);
			properties.load (inputStream);

		} catch (Exception ee) {
			_log.error ("AlbumFormInfo.processRequest: failed to read properties file \"" + _propertiesFile + "\"");
		} finally {
			try {
				inputStream.close ();
			} catch (Exception ee) {
				//ignore
			}
		}

		//string properties that are booleans
		String string = properties.getProperty ("debug", "no");
		if (string.compareToIgnoreCase ("yes") == 0) {
			gotDebugRequest = true;
			if (_debugProperties)
				_log.debug ("AlbumFormInfo.processRequest: property: got debug");
		}

		string = properties.getProperty ("showRgbData", "no");
		if (string.compareToIgnoreCase ("yes") == 0) {
			gotShowRgbData = true;
			if (_debugProperties)
				_log.debug ("AlbumFormInfo.processRequest: property: got showRgbData");
		}

//not currently working
//		//other string properties
//		_subFolders = getPropertyString (properties, "subFolders", "");
//		if (_debugProperties)
//			_log.debug ("AlbumFormInfo.processRequest: property: got subFolders = " + _subFolders);

//TODO - check for valid values

		//int properties
		_columns = _defaultColumns = getPropertyInt (properties, "defaultColumns", _defaultColumns);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultColumns = " + _defaultColumns);

		_maxFilters = _defaultMaxFilters = getPropertyInt (properties, "defaultMaxFilters", _defaultMaxFilters);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultMaxFilters = " + _defaultMaxFilters);
		
		_highlightDays = _defaultHighlightDays = getPropertyInt (properties, "defaultHighlightDays", _defaultHighlightDays);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultHighlightDays = " + _defaultHighlightDays);

		_highlightMinPixels = _defaultHighlightMinPixels = getPropertyInt (properties, "defaultHighlightMinPixels", _defaultHighlightMinPixels);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultHighlightMinPixels = " + _defaultHighlightMinPixels);

		_highlightMaxKilobytes = _defaultHighlightMaxKilobytes = getPropertyInt (properties, "defaultHighlightMaxKilobytes", _defaultHighlightMaxKilobytes);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultHighlightMaxKilobytes = " + _defaultHighlightMaxKilobytes);

		_maxImageScalePercent = _defaultMaxImageScalePercent = getPropertyInt (properties, "defaultMaxImageScalePercent", _defaultMaxImageScalePercent);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultMaxImageScalePercent = " + _defaultMaxImageScalePercent);

		_maxRgbDiffs = _defaultMaxRgbDiffs = getPropertyInt (properties, "defaultMaxRgbDiffs", _defaultMaxRgbDiffs);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultMaxRgbDiffs = " + _defaultMaxRgbDiffs);

		_panels = _defaultPanels = getPropertyInt (properties, "defaultPanels", _defaultPanels);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultPanels = " + _defaultPanels);

		_exifDateIndex = _defaultExifDateIndex = getPropertyInt (properties, "defaultExifDateIndex", _defaultExifDateIndex);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultExifDateIndex = " + _defaultExifDateIndex);

		_logLevel = _defaultLogLevel = getPropertyInt (properties, "defaultLogLevel", _defaultLogLevel);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultLogLevel = " + _defaultLogLevel);

		_profileLevel = _defaultProfileLevel = getPropertyInt (properties, "defaultProfileLevel", _defaultProfileLevel);
		if (_debugProperties)
			_log.debug ("AlbumFormInfo.processRequest: property: got defaultProfileLevel = " + _defaultProfileLevel);

		//booleans
		_Debug = gotDebugRequest;
		_tagFilterOperandOr = gotFilterOperandOr;
		_collapseGroups = gotCollapseGroups;
		_limitedCompare = gotLimitedCompare;
		_looseCompare = gotLooseCompare;
		_ignoreBytes = gotIgnoreBytes;
		_useExifDates = gotUseExifDates;
		_useCase = gotUseCase;
		_clearCache = gotClearCache;
		_reverseSort = gotReverseSort;
		_showRgbData = gotShowRgbData;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getPropertyInt (Properties properties, String propertyName, int defaultValue)
	{
		final String invalidString = new Integer (-1).toString ();

		//first try to get root-folder-specific value
		String propertyName2 = _rootFolder + "." + propertyName;
		String valueString = properties.getProperty (propertyName2, invalidString);

		//if that fails, try to get generic value
		if (valueString.equals (invalidString)) {
			valueString = properties.getProperty (propertyName, invalidString);
		}

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
	public String getPropertyString (Properties properties, String propertyName, String defaultValue)
	{
		final String invalidString = "invalid";//new Integer (-1).toString ();

		//first try to get root-folder-specific value
		String propertyName2 = _rootFolder + "." + propertyName;
		String valueString = properties.getProperty (propertyName2, invalidString);

		//if that fails, try to get generic value
		if (valueString.equals (invalidString)) {
			valueString = properties.getProperty (propertyName, invalidString);
		}

		//if that fails, use default
		if (valueString.equals (invalidString)) {
			valueString = defaultValue;
		}

		return valueString;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getServletErrorsHtml ()
	{
		StringBuffer sb = new StringBuffer ();

		if (_servletErrors.size () != 0) {
			sb.append ("<B>");
			for (String servletError : _servletErrors) {
				sb.append (servletError);
				sb.append ("<BR>");
			}
			sb.append ("</B>");
		}

		return sb.toString ();
	}

	public int getNumServletErrors ()
	{
		return _servletErrors.size ();
	}

	public void addServletError (String servletError)
	{
		_servletErrors.add (servletError);
	}

	///////////////////////////////////////////////////////////////////////////
	public String cleanFilter (String filter)
	{
		if (filter.isEmpty ()) {
			return filter;
		}

		//accept URL as input and try to create potentially useful filter from it
		try {
			//first try to parse as URL, which will throw exception if not valid, and continue below
			URL url = new URL (filter);

			//then parse as URI, which allows us to extract components
			URI uri = new URI (filter);

			filter = uri.getPath ();
			filter = filter.replaceAll ("[0-9]", ","); //regex
			filter = filter.replaceAll ("\\.", ","); //regex
			filter = filter.replaceAll ("/", ","); //regex
			filter = filter.replaceAll ("-", ""); //regex
			filter = filter.replaceAll ("_", ""); //regex
			filter = filter.replaceAll ("\\+", "\\*"); //regex
			filter = filter.replaceAll ("\\,.\\,", ","); //regex

			//drop through to continue processing

		} catch (Exception ee) {
			//not URL; ignore exception and drop through to continue processing
		}

		//convert " and " and " & " into a comma
		filter = filter.replaceAll (" and ", ","); //regex
		filter = filter.replaceAll ("\\*and\\*", ","); //regex
		filter = filter.replaceAll (" & ", ","); //regex

		//convert " aka " into a comma
		filter = filter.replaceAll (" aka ", ","); //regex
		filter = filter.replaceAll (" AKA ", ","); //regex

		//remove all white space
		filter = filter.replaceAll ("[ \t]*", ""); //regex

		//remove all periods
		filter = filter.replaceAll ("\\.", ""); //regex

		//remove all exclamation points
		filter = filter.replaceAll ("!", ""); //regex

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

		Collections.sort (filters, caseInsensitiveStringComparator);
		caseInsensitiveDedup (filters);

		return filters.toArray (new String[] {});
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

		Collections.sort (tags, caseInsensitiveStringComparator);
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

		Collections.sort (excludes, caseInsensitiveStringComparator);
		caseInsensitiveDedup (excludes);

		return excludes.toArray (new String[] {});
	}

//	///////////////////////////////////////////////////////////////////////////
//	public void setExtension (String extension)
//	{
//		_extension = extension.replaceAll ("[ \t]*", "");
//	}
//
//	public String getExtension ()
//	{
//		return _Extension;
//	}

	///////////////////////////////////////////////////////////////////////////
	public void setMaxRgbDiffs (int maxRgbDiffs)
	{
		_maxRgbDiffs = Math.max (maxRgbDiffs, 0);
	}

	public int getMaxRgbDiffs ()
	{
		return _maxRgbDiffs;
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

	//truncate to midnight boundary
	public long getSinceInMillis ()
	{
		if (getSinceDays () == 0) { //0 means disabled, 1 means last night at midnight, 2 means yesterday at midnight, etc.
			return 0;
		}

		if (_sinceInMillis < 0) {
			Calendar date = new GregorianCalendar ();
			date.set (Calendar.HOUR_OF_DAY, 0);
			date.set (Calendar.MINUTE, 0);
			date.set (Calendar.SECOND, 0);
			date.set (Calendar.MILLISECOND, 0);

			int days = (int) getSinceDays ();
			date.add (Calendar.DAY_OF_MONTH, 1 - days);
			_sinceInMillis = date.getTimeInMillis ();

			if (_Debug) {
				String sinceStr = _dateFormat.format (new Date (_sinceInMillis));
				_log.debug ("AlbumFormInfo.getSinceInMillis: since date: " + sinceStr);
			}
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
	public void setReverseSort (boolean reverseSort)
	{
		_reverseSort = reverseSort;
	}

	public boolean getReverseSort ()
	{
		return _reverseSort;
	}

//	///////////////////////////////////////////////////////////////////////////
//	public void setScreenWidth (int screenWidth)
//	{
//		if (screenWidth > 0) {
//			_screenWidth = screenWidth;
//		}
//	}
//
//	public int getScreenWidth ()
//	{
//		return (isAndroidDevice () ? 2 : 1) * _screenWidth; //hack - Nexus 7 uses Density Independent Pixel (commonly referred to as dp) and reports half as many pixels
//	}
//
//	///////////////////////////////////////////////////////////////////////////
//	public void setScreenHeight (int screenHeight)
//	{
//		if (screenHeight > 0) {
//			_screenHeight = screenHeight;
//		}
//	}
//
//	public int getScreenHeight ()
//	{
//		return (isAndroidDevice () ? 2 : 1) * _screenHeight; //hack - Nexus 7 uses Density Independent Pixel (commonly referred to as dp) and reports half as many pixels
//	}

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
		return _userAgent.toLowerCase ().contains ("android");
	}

	public boolean isNexus7Device ()
	{
		return _userAgent.toLowerCase ().contains ("nexus 7");
	}

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
		_rootFolder = rootFolder.replaceAll ("[ \t]*", "");
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
	public String getSubFolders ()
	{
		return _subFolders;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getServer ()
	{
		return _server;
	}

//	///////////////////////////////////////////////////////////////////////////
//	public boolean getShowRgbData ()
//	{
//		return _showRgbData;
//	}

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

	//truncate to midnight boundary
	public long getHighlightInMillis ()
	{
		if (getHighlightDays () == 0) { //0 means disabled, 1 means last night at midnight, 2 means yesterday at midnight, etc.
			return 0;
		}

		if (_highlightInMillis < 0) {
			Calendar date = new GregorianCalendar ();
			date.set (Calendar.HOUR_OF_DAY, 0);
			date.set (Calendar.MINUTE, 0);
			date.set (Calendar.SECOND, 0);
			date.set (Calendar.MILLISECOND, 0);

			int days = (int) getHighlightDays ();
			date.add (Calendar.DAY_OF_MONTH, 1 - days);
			_highlightInMillis = date.getTimeInMillis ();

			if (_Debug) {
				String highlightStr = _dateFormat.format (new Date (_highlightInMillis));
				_log.debug ("AlbumFormInfo.getHighlightInMillis: highlight date: " + highlightStr);
			}
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

		String prevString = new String ();
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

		if (name.toLowerCase ().endsWith (extension)) {
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

		string = string.replace ("*", ".*")
					   .replace (";", "[aeiouy]") //regex: semicolon here means vowel or 'y'
//this syntax doesn't work with MySQL? .replace (":", "[a-z&&[^aeiouy]]") //regex: colon here means any letter except vowel or 'y'
					   .replace (":", "[b-df-hj-np-tv-xz]") //regex: colon here means any letter except vowel or 'y'
					   .replace ("+", "[\\d]"); //regex: plus sign here means digit

		return string;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String convertRegexToWildcards (String string)
	{
		string = string.replace (".*", "*")
					   .replace ("[aeiouy]", ";") //regex: semicolon here means vowel or 'y'
//this syntax doesn't work with MySQL? .replace ("[a-z&&[^aeiouy]]", ":") //regex: colon here means any letter except vowel or 'y'
					   .replace ("[b-df-hj-np-tv-xz]", ":") //regex: colon here means any letter except vowel or 'y'
					   .replace ("[\\d]", "+"); //regex: plus sign here means digit

		if (string.endsWith ("*")) {
			string = string.substring (0, string.length () - 1); //remove trailing string
		}

		return string;
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

/* could not get this to compile
	///////////////////////////////////////////////////////////////////////////
	public static final <T> Comparator<T> caseInsensitiveStringComparator2 = new Comparator<T> ()
	{
		@Override
		public int compare (T o1, T o2)
		{
			return o1.toString ().compareToIgnoreCase (o2.toString ());
		}
	};
*/


	//parameters from properties file at "%CATALINA_HOME%"\webapps\AlbumServlet\WEB-INF\classes\album.properties
	private static final String _propertiesFile = "/WEB-INF/classes/album.properties";
	private int _defaultColumns = 3;
	private int _defaultMaxFilters = 500; 
	private int _defaultHighlightDays = 4;
	private int _defaultHighlightMinPixels = 640;
	private int _defaultHighlightMaxKilobytes = 1536;
	private int _defaultLogLevel = 5;
	private int _defaultMaxImageScalePercent = 150;
	private int _defaultMaxRgbDiffs = 5;
	private int _defaultPanels = 60;
	private int _defaultExifDateIndex = 4; //earliestExifDate
	private int _defaultProfileLevel = 5;
	private boolean _defaultShowRgbData = false;

	//private members
	private String _defaultRootFolder = "jroot";
	private String _subFolders = "";
	private int _highlightMinPixels = _defaultHighlightMinPixels;
	private int _highlightMaxKilobytes = _defaultHighlightMaxKilobytes;
	private int _maxColumns = 32;
	private List<String> _servletErrors = null;

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
	private int _maxRgbDiffs = _defaultMaxRgbDiffs;
	private int _exifDateIndex = _defaultExifDateIndex; //when sorting/comparing EXIF dates, specifies which date to use
	private boolean _tagFilterOperandOr = false;
	private boolean _collapseGroups = false;
	private boolean _limitedCompare = false; //don't include dups that share common base (name)
	private boolean _looseCompare = false; //compare dups with either loose or strict critera
	private boolean _ignoreBytes = false; //optionally tell compare dups to ignore number of bytes
	private boolean _useExifDates = false; //optionally tell compare dups to use the EXIF (date) data
	private boolean _useCase = false;
	private boolean _clearCache = false;
	private boolean _reverseSort = false;
	private AlbumOrientation _orientation = AlbumOrientation.ShowAny;
	private AlbumSortType _sortType = AlbumSortType.ByName;
	private String _userAgent = "";

	//hardcoded values for firefox at 1920 x 1200 resolution and reasonable width
	private int _windowWidth = (1880 * 55) / 100;
	private int _windowHeight = 980;
//	private int _screenWidth = _windowWidth;
//	private int _screenHeight = _windowHeight;

	public static final String _ImageExtension = ".jpg";
	public static final String _RgbDataExtension = ".dat";
	public static final String _DeleteSuffix = ".delete";

	public static final int _NumTagParams = 9;

	//global variables - NOT PART OF BEAN
	public static boolean _Debug = false;
	public static boolean _showRgbData = false;
	public static int _profileLevel = 5; //note this value is in use until processRequest() is called
	public static int _logLevel = 5; //note this value is in use until processRequest() is called
	public static int _maxFilesDir = 300000; //soft limit - used to init collections
	public static int _maxFilesSubdir = 40000; //soft limit - used to init collections
	public static int _maxFilePatterns = 3000; //soft limit - used to init collections
	public static int _maxImageScalePercent; //don't scale images over this size

	private static AlbumFormInfo _instance = null;

	private final boolean _debugProperties = false;

//	private static final String NL = System.getProperty ("line.separator");
	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm");

	private static final String _basePath = "E:/Netscape/Program/"; //need trailing slash
	private static final String _albumRoot = "/albumRoot/"; //should match tomcat's server.xml
	private static final String _server = "/AlbumServlet/AlbumServlet";

	private static Logger _log = LogManager.getLogger ();
}
