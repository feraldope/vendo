//AlbumProfiling.java - class for profiling performance

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class AlbumProfiling
{
	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public static AlbumProfiling getInstance()
	{
		if (_instance == null) {
			synchronized (AlbumProfiling.class) {
				if (_instance == null) {
					_instance = new AlbumProfiling ();
				}
			}
		}

		return _instance;
	}

//TODO - add enableProfiling(boolean b) or setProfileLevel (int profileLevel)

	///////////////////////////////////////////////////////////////////////////
	private AlbumProfiling ()
	{
		_fieldWidths = new Integer [] {
				20,	//method, default value - actual width calculated in print()
				6,	//count
				9,	//total
				8,	//average
				7,	//min
				7	//max
				};

		_recordMap = new HashMap<String, ProfileRecord> (32);
		_ignorableRecords = new HashSet<ProfileIgnorableRecord> ();
		_recordsIgnoredCount = 0;
		_currentIndex = 0;

		//TODO - these should be set by caller, not hardcoded
		_ignorableRecords.add(new ProfileIgnorableRecord (".*doDir.*accept.*loop.*", 10));
		_ignorableRecords.add(new ProfileIgnorableRecord (".*getImagesFromCache.*", 10));
		_ignorableRecords.add(new ProfileIgnorableRecord (".*getImagesFromImages.*", 10));
		_ignorableRecords.add(new ProfileIgnorableRecord (".*getImageCountsFromImageCounts.*", 10));
		_ignorableRecords.add(new ProfileIgnorableRecord (".*getLastUpdateFromImageFolder.*", 10));
	}

	///////////////////////////////////////////////////////////////////////////
	public void enter (int profileLevel)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		enter (null, false);
	}

	public void enter (int profileLevel, String tag)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		enter (tag, false);
	}

	public void enter (int profileLevel, String tag1, String tag2)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		enter (tag1 + "." + tag2, false);
	}

	public void enterAndTrace (int profileLevel)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		enter (null, true);
	}

	public void enterAndTrace (int profileLevel, String tag)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		enter (tag, true);
	}

	private void enter (String tag, boolean trace)
	{
		long startNano = System.nanoTime ();

		StringBuilder method = new StringBuilder (getCallerName (new Throwable (), 2));

		if (tag != null) {
			method.append (".").append (tag);
		}

		if (trace) {
			_log.trace (method.toString ());
		}

		synchronized (_recordMap) {
			ProfileRecord record = _recordMap.get (method.toString ());

			if (record == null) {
//				_log.debug ("AlbumProfiling.enter: adding record for \"" + method + "\"");
				record = new ProfileRecord (method.toString (), _currentIndex++, startNano);

			} else {
				if (record.getInProcess()) {
//					throw new RuntimeException ("AlbumProfiling.enter: recursive entry for \"" + method + "\"");
					_log.error ("AlbumProfiling.enter: recursive entry for \"" + method.toString () + "\"");
				}

				record.setStartNano(startNano);
				record.setInProcess(true);
			}

			_recordMap.put (method.toString (), record);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void exit (int profileLevel)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		exit (null);
	}

	public void exit (int profileLevel, String tag)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		exit (tag);
	}

	public void exit (int profileLevel, String tag1, String tag2)
	{
		if (profileLevel > AlbumFormInfo._profileLevel) {
			return;
		}

		exit (tag1 + "." + tag2);
	}

	private void exit (String tag)
	{
		long endNano = System.nanoTime ();

		StringBuilder method = new StringBuilder (getCallerName (new Throwable (), 2));

		if (tag != null) {
			method.append (".").append (tag);
		}

		synchronized (_recordMap) {
			ProfileRecord record = _recordMap.get (method.toString ());
			if (record == null) {
//				throw new RuntimeException ("AlbumProfiling.exit: entry does not exist for \"" + method + "\"");
				_log.error ("AlbumProfiling.exit: entry does not exist for \"" + method.toString () + "\"");
				return;
			}

			long elapsedNano = endNano - record.getSetStartNano();

			if (record.getMinNano() > elapsedNano) {
				record.setMinNano(elapsedNano);
			}
			if (record.getMaxNano() < elapsedNano) {
				record.setMaxNano(elapsedNano);
			}

			record.setElapsedNanos(record.getElapsedNanos() + elapsedNano);
			record.setCount(record.getCount() + 1);
			record.setStartNano(0);
			record.setInProcess(false);

			_recordMap.put (method.toString (), record);
		}
	}

	///////////////////////////////////////////////////////////////////////////
//	private static String getCallerName (Throwable throwable)
//	{
//		return getCallerName (throwable, 1);
//	}

	private static String getCallerName (Throwable throwable, int level)
	{
		StackTraceElement[] stack = throwable.getStackTrace ();
		StringBuilder line = new StringBuilder (stack[level].toString ());

		try {
			//string has the following format; extract the class and method name
			//org.vendo.AlbumServlet.doGet(AlbumServlet.java:84)
			String[] components = line.toString ().split("\\(")[0].split("\\.");
			int method = components.length - 1;
			line = new StringBuilder (components[method - 1]).append (".").append (components[method]);

		} catch (Exception ee) {
			//if our string processing failed, ignore and fall through and just return entire line
		}

		//also show current thread (number)
		line.append ("(").append (Thread.currentThread ().getName ().replace ("Thread-", "")).append (")");

		return line.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	//note this resets all the profiling data
	public synchronized void print (boolean showMemoryUsage)
	{
		print (showMemoryUsage, true);
	}

	///////////////////////////////////////////////////////////////////////////
	//note this resets all the profiling data
	public synchronized void print (boolean showMemoryUsage, boolean resetProfiling)
	{
//		if (!AlbumFormInfo._Profiling)
//			return;

		if (_recordMap == null || _recordMap.size () == 0) {
			if (resetProfiling) {
				_instance = null;
			}

			return;
		}

		markIgnorableRecords ();

		//sort by index for printing
		List<ProfileRecord> records = _recordMap.values ().stream ().sorted (new ProfileRecordComparatorByIndex ()).collect (Collectors.toList ());

		int longestMethodName = 10; //min width
		for (ProfileRecord record : records) {
			if (record.getInProcess() && !record.getIsFake()) {
				_log.error ("AlbumProfiling.print: exit not called for \"" + record.getMethod() + "\"");
			}

			int length = record.getMethod().length ();
			longestMethodName = Math.max (length, longestMethodName);
		}
		_fieldWidths[0] = longestMethodName;// + 5;

		_log.debug ("AlbumProfiling.print (millisecs) --------------------------------------");
		printRecord ("Method", "Count", "Total", "Average", "Min", "Max");

		for (ProfileRecord record : records) {
			if (record.getCount () > 0 && !record.getIgnore ()) {
				//convert values from nanosecs to millisecs
				double total = (double) record.getElapsedNanos() / 1e6;
				double average = total / record.getCount();
				double min = (double) record.getMinNano() / 1e6;
				double max = (double) record.getMaxNano() / 1e6;

				printRecord (record.getMethod(), record.getCount(), record.getIsFake(), total, average, min, max);
			}
		}

		if (_recordsIgnoredCount > 0) {
			_log.debug ("AlbumProfiling.print: " + _recordsIgnoredCount + " of " + records.size () + " records ignored/collapsed");
		}

		if (showMemoryUsage) {
			final double mega = 1024 * 1024;
			final double giga = mega * 1024;

			double freeMem  = Runtime.getRuntime ().freeMemory ();
			double totalMem = Runtime.getRuntime ().totalMemory ();
			double maxMem   = Runtime.getRuntime ().maxMemory ();
			double usedMem  = totalMem - freeMem;
			double util = 100 * totalMem / maxMem;

			boolean useGiga = (freeMem >= giga || totalMem >= giga || maxMem >= giga || usedMem >= giga);
			String unitSuffixStr = (useGiga ? "GB" : "MB");
			double unitSuffix = (useGiga ? giga : mega);

			usedMem  /= unitSuffix;
			freeMem  /= unitSuffix;
			totalMem /= unitSuffix;
			maxMem   /= unitSuffix;

			_log.debug ("AlbumProfiling.print: memory used="  + _decimalFormat1.format (usedMem)  + unitSuffixStr +
													   " free="  + _decimalFormat1.format (freeMem)  + unitSuffixStr +
													   " total=" + _decimalFormat1.format (totalMem) + unitSuffixStr +
													   " max="   + _decimalFormat1.format (maxMem)   + unitSuffixStr +
													   " util="  + _decimalFormat1.format (util) + "%");
		}

		//optionally delete this AlbumProfiling instance
		if (resetProfiling) {
			_instance = null;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void printRecord (String v0, Integer v1, boolean isFake, Double v2, Double v3, Double v4, Double v5)
	{
		String s2 = (isFake ? "**" : _decimalFormat0.format (v2));
		String s3 = (v1 == 1 || isFake ? "--" : _decimalFormat0.format (v3));
		String s4 = (v1 == 1 || isFake ? "--" : _decimalFormat0.format (v4));
		String s5 = (v1 == 1 || isFake ? "--" : _decimalFormat0.format (v5));

		printRecord (v0, v1.toString (), s2, s3, s4, s5);
	}

	///////////////////////////////////////////////////////////////////////////
	private void printRecord (String v0, String v1, String v2, String v3, String v4, String v5)
	{
		String format = "%-" + _fieldWidths[0] + "s";
		v0 = String.format (format, v0);

		format = "%" + _fieldWidths[1] + "s";
		v1 = String.format (format, v1);

		format = "%" + _fieldWidths[2] + "s";
		v2 = String.format (format, v2);

		format = "%" + _fieldWidths[3] + "s";
		v3 = String.format (format, v3);

		format = "%" + _fieldWidths[4] + "s";
		v4 = String.format (format, v4);

		format = "%" + _fieldWidths[5] + "s";
		v5 = String.format (format, v5);

		_log.debug (v0 + " " + v1 + " " + v2 + " " + v3 + " " + v4 + " " + v5);
	}

	///////////////////////////////////////////////////////////////////////////
	private void markIgnorableRecords ()
	{
		for (ProfileIgnorableRecord ignorableRecord : _ignorableRecords) {
			List<ProfileRecord> matchingRecords = _recordMap.keySet ().stream ()
															.map (_recordMap::get)
															.filter (v -> v.getCount () == 1)
															.filter (v -> v.matchesPattern (ignorableRecord.getMethodPattern ()))
															.collect (Collectors.toList ());

//TODO there is only one value for neuteredKey each loop; no need for Map
			Map<String, Integer> map = new HashMap<>();
			for (ProfileRecord record : matchingRecords) {
				String neuteredKey = record.getMethod().replaceAll("\\(.*\\)", "(*)").replaceAll("\\)\\.[a-z\\-]+", ").*");
				Integer count = map.get(neuteredKey);
				if (count != null) {
					count += record.getCount();
				} else {
					count = record.getCount();
				}
				map.put(neuteredKey, count);
			}

			for (Map.Entry<String, Integer> entry : map.entrySet()) {
				ProfileRecord record = new ProfileRecord(entry.getKey(), _currentIndex++, 0);
				record.setCount(entry.getValue());
				record.setIsFake(true);
				_recordMap.put(entry.getKey(), record);
			}

			int numToBeIgnored = matchingRecords.size () - ignorableRecord.getMinCount ();
			if (numToBeIgnored > 0) {
				_recordsIgnoredCount += numToBeIgnored;
				matchingRecords.stream () //will sort ascending, so first numToBeIgnored items will be set to ignore, leaving worst (slowest) offenders to be printed
							   .sorted (new ProfileRecordComparatorByElapsedNanos ())
							   .limit (numToBeIgnored)
							   .forEach (v -> v.setIgnore (true));
			}
		}

/* old way
		for (ProfileIgnorableRecord ignorableRecord : _ignorableRecords) {
//			_log.debug ("removeIgnorableRecords: ignorableRecord: " + ignorableRecord);
			List<ProfileRecord> matchingRecords = new ArrayList<ProfileRecord> ();
			for (String methodName : _recordMap.keySet ()) {
				ProfileRecord record = _recordMap.get (methodName);
				if (record.getCount () == 1) {
					Matcher matcher = ignorableRecord.getMethodPattern ().matcher (record.getMethod ());
					if (matcher.matches ()) {
//						_log.debug("removeIgnorableRecords: found matching record: " + record);
						matchingRecords.add (record);
					}
				}
			}

			int minCount = ignorableRecord.getMinCount ();
			if (matchingRecords.size () >= minCount) {
				matchingRecords.sort (new ProfileRecordComparatorByElapsedNanos ());
				List<ProfileRecord> recordsToBeIgnored = matchingRecords.subList (minCount, matchingRecords.size ());
				for (ProfileRecord recordToBeIgnored : recordsToBeIgnored) {
					recordToBeIgnored.setIgnore (true);
					_recordsIgnoredCount++;
				}
			}
		}
*/
	}

	///////////////////////////////////////////////////////////////////////////
	private class ProfileRecord
	{
		ProfileRecord (String method, int index, long startNano)
		{
			_method = method;
			_index = index;
			_startNano = startNano;
		}

		///////////////////////////////////////////////////////////////////////
		public String getMethod ()
		{
			return _method;
		}

		///////////////////////////////////////////////////////////////////////
		public int getIndex ()
		{
			return _index;
		}

		///////////////////////////////////////////////////////////////////////
		public long getElapsedNanos ()
		{
			return _elapsedNano;
		}
		public void setElapsedNanos (long elapsedNano)
		{
			_elapsedNano = elapsedNano;
		}

		///////////////////////////////////////////////////////////////////////
		public long getElapsedMillis ()
		{
			return (long) ((double) _elapsedNano / 1000000);
		}

		///////////////////////////////////////////////////////////////////////
		public long getSetStartNano ()
		{
			return _startNano;
		}
		public void setStartNano (long startNano)
		{
			_startNano = startNano;
		}

		///////////////////////////////////////////////////////////////////////
		public long getMinNano ()
		{
			return _minNano;
		}
		public void setMinNano (long minNano)
		{
			_minNano = minNano;
		}

		///////////////////////////////////////////////////////////////////////
		public long getMaxNano ()
		{
			return _maxNano;
		}
		public void setMaxNano (long maxNano)
		{
			_maxNano = maxNano;
		}

		///////////////////////////////////////////////////////////////////////
		public int getCount ()
		{
			return _count;
		}
		///////////////////////////////////////////////////////////////////////
		public void setCount (int count)
		{
			_count = count;
		}

		///////////////////////////////////////////////////////////////////////
		public boolean getIgnore ()
		{
			return _ignore;
		}
		public void setIgnore (boolean ignore)
		{
			_ignore = ignore;
		}

		///////////////////////////////////////////////////////////////////////
		public boolean getInProcess ()
		{
			return _inProcess;
		}
		public void setInProcess (boolean inProcess)
		{
			_inProcess = inProcess;
		}

		///////////////////////////////////////////////////////////////////////
		public boolean getIsFake ()
		{
			return _isFake;
		}
		public void setIsFake (boolean isFake)
		{
			_isFake = isFake;
		}

		///////////////////////////////////////////////////////////////////////
		boolean matchesPattern (Pattern pattern)
		{
			return pattern.matcher (getMethod ()).matches ();
		}

		///////////////////////////////////////////////////////////////////////
		@Override
		public String toString ()
		{
			StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
			sb.append (": ").append (VendoUtils.quoteString(getMethod ()));
			sb.append (", ").append (getElapsedMillis ());
			sb.append (", ").append (getIgnore ());
			return sb.toString ();
		}

		private String _method = "<???>";
		private int _index = 0;
		private long _startNano = 0;
		private long _elapsedNano = 0;
		private long _minNano = Long.MAX_VALUE;
		private long _maxNano = 0;
		private int _count = 0;
		private boolean _inProcess = true;
		private boolean _ignore = false;
		private boolean _isFake = false;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class ProfileRecordComparatorByIndex implements Comparator<ProfileRecord>
	{
		@Override
		public int compare (ProfileRecord record1, ProfileRecord record2)
		{
			return record1.getIndex () - record2.getIndex (); //sort in ascending order
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static class ProfileRecordComparatorByElapsedNanos implements Comparator<ProfileRecord>
	{
		@Override
		public int compare (ProfileRecord record1, ProfileRecord record2)
		{
			long diff = record1.getElapsedNanos () - record2.getElapsedNanos (); //sort in ascending order
			return diff == 0 ? 0 : diff > 0 ? 1 : -1;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static class ProfileIgnorableRecord
	{
		ProfileIgnorableRecord (String methodRegex, int minCount)
		{
			//TODO - Pattern.compile can throw exception
			_methodPattern = Pattern.compile (methodRegex);
			_minCount = minCount;
		}

		///////////////////////////////////////////////////////////////////////
		public Pattern getMethodPattern ()
		{
			return _methodPattern;
		}

		///////////////////////////////////////////////////////////////////////
		public int getMinCount ()
		{
			return _minCount;
		}

		///////////////////////////////////////////////////////////////////////
		@Override
		public String toString ()
		{
			StringBuilder sb = new StringBuilder (getClass ().getSimpleName ());
			sb.append (": ").append (VendoUtils.quoteString(getMethodPattern ().toString ()));
			sb.append (", ").append (getMinCount ());
			return sb.toString ();
		}

		private final Pattern _methodPattern;
		private final int _minCount;
	}


	//members
	private final Integer _fieldWidths[];
	private final HashMap<String, ProfileRecord> _recordMap;
	private static HashSet<ProfileIgnorableRecord> _ignorableRecords;
	private static int _recordsIgnoredCount;
	private static int _currentIndex = 0;
	private static volatile AlbumProfiling _instance = null;

	private final DecimalFormat _decimalFormat0 = new DecimalFormat ("###,##0");
	private final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0.0");

	private static Logger _log = LogManager.getLogger ();
}
