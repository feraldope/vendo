//ContentProfiling.java - class for profiling performance

package com.vendo.contentServlet;

import java.util.*;

import org.apache.logging.log4j.*;


public class ContentProfiling
{
	///////////////////////////////////////////////////////////////////////////
	public synchronized static ContentProfiling getInstance ()
	{
		if (_instance == null)
			_instance = new ContentProfiling (); // _instance is deleted in print()

		return _instance;
	}

//TODO - add enableProfiling(boolean b) or setProfileLevel (int profileLevel)

	///////////////////////////////////////////////////////////////////////////
	private ContentProfiling ()
	{
		_fieldWidths = new Integer [6];
		_fieldWidths[0] = 20;	//method, default value - actual width calculated in print()
		_fieldWidths[1] = 6;	//count
		_fieldWidths[2] = 8;	//total
		_fieldWidths[3] = 8;	//average
		_fieldWidths[4] = 7;	//min
		_fieldWidths[5] = 7;	//max

		_records = new HashMap<String, ProfileRecord> (32);
		_currentIndex = 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public void enter (int profileLevel)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		enter1 (null, false);
	}

	public void enter (int profileLevel, String tag)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		enter1 (tag, false);
	}

	public void enter (int profileLevel, String tag1, String tag2)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		enter1 (tag1 + "." + tag2, false);
	}

	public void enterAndTrace (int profileLevel)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		enter1 (null, true);
	}

	public void enterAndTrace (int profileLevel, String tag)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		enter1 (tag, true);
	}

	private void enter1 (String tag, boolean trace)
	{
		long startNano = System.nanoTime ();

		String method = getCallerName (new Throwable (), 2);

		if (tag != null)
			method += "." + tag;

		if (trace)
			_log.trace (method);

		synchronized (_records) {
			ProfileRecord record = _records.get (method);

			if (record == null) {
//				_log.debug ("ContentProfiling.enter: adding record for \"" + method + "\"");
				record = new ProfileRecord (method, _currentIndex++, startNano);

			} else {
				if (record._inProcess)
//					throw new RuntimeException ("ContentProfiling.enter: recursive entry for \"" + method + "\"");
					_log.error ("ContentProfiling.enter: recursive entry for \"" + method + "\"");

				record._startNano = startNano;
				record._inProcess = true;
			}

			_records.put (method, record);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void exit (int profileLevel)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		exit1 (null);
	}

	public void exit (int profileLevel, String tag)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		exit1 (tag);
	}

	public void exit (int profileLevel, String tag1, String tag2)
	{
		if (profileLevel > ContentFormInfo._profileLevel)
			return;

		exit1 (tag1 + "." + tag2);
	}

	private void exit1 (String tag)
	{
		long endNano = System.nanoTime ();

		String method = getCallerName (new Throwable (), 2);

		if (tag != null)
			method += "." + tag;

		synchronized (_records) {
			ProfileRecord record = _records.get (method);
			if (record == null) {
//				throw new RuntimeException ("ContentProfiling.exit: entry does not exist for \"" + method + "\"");
				_log.error ("ContentProfiling.exit: entry does not exist for \"" + method + "\"");
				return;
			}

			long elapsedNano = endNano - record._startNano;

			if (record._minNano > elapsedNano)
				record._minNano = elapsedNano;
			if (record._maxNano < elapsedNano)
				record._maxNano = elapsedNano;

			record._elapsedNano += elapsedNano;
			record._count++;
			record._startNano = 0;
			record._inProcess = false;

			_records.put (method, record);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static String getCallerName (Throwable throwable)
	{
		return getCallerName (throwable, 1);
	}

	private static String getCallerName (Throwable throwable, int level)
	{
		StackTraceElement[] stack = throwable.getStackTrace ();
		String line = stack[level].toString ();

		try {
			//string has the following format; extract the class and method name
			//org.vendo.AlbumServlet.doGet(AlbumServlet.java:84)
			String[] components = line.split("\\(")[0].split("\\.");
			int method = components.length - 1;
			line = components[method - 1] + "." + components[method];

		} catch (Exception ee) {
			//if our string processing failed, ignore and fall through and just return entire line
		}

		return line;
	}

	///////////////////////////////////////////////////////////////////////////
	//note this resets all the profiling data
	public synchronized void print (boolean showMemoryUsage)
	{
//		if (!ContentFormInfo._Profiling)
//			return;

		if (_records == null || _records.size () == 0) {
//todo? delete _instance here
			return;
		}

		int numRecords = _records.size ();
		List<ProfileRecord> records = new ArrayList<ProfileRecord> (numRecords);
		records.addAll (_records.values ());
		Collections.sort (records, new ProfileRecordComparator ());

		int longestMethodName = 10; //min width
		for (ProfileRecord record : records) {
			if (record._inProcess) {
//				throw new RuntimeException ("ContentProfiling.print: exit not called for \"" + record._method + "\"");
				_log.error ("ContentProfiling.print: exit not called for \"" + record._method + "\"");
			}

			int length = record._method.length ();
			longestMethodName = Math.max (length, longestMethodName);
		}
		_fieldWidths[0] = longestMethodName;// + 5;

		_log.debug ("ContentProfiling.print (millisecs) --------------------------------------");
		printRecord ("Method", "Count", "Total", "Average", "Min", "Max");

		for (ProfileRecord record : records) {
			//convert values from nanosecs to millisecs
			double total = (double) record._elapsedNano / 1000000;
			double average = total / (double) record._count;
			double min = (double) record._minNano / 1000000;
			double max = (double) record._maxNano / 1000000;

			printRecord (record._method, record._count, total, average, min, max);
		}

		if (showMemoryUsage) {
			final long mega = (1024 * 1024);

			long maxMem   = Runtime.getRuntime ().maxMemory () / mega;
			long freeMem  = Runtime.getRuntime ().freeMemory () / mega;
			long totalMem = Runtime.getRuntime ().totalMemory () / mega;
			long usedMem = totalMem - freeMem;
			_log.debug ("ContentProfiling.print: used=" + usedMem + "MB free=" + freeMem + "MB total=" + totalMem + "MB max=" + maxMem + "MB");
		}

		//effectively delete this ContentProfiling instance
		_instance = null;
	}

	///////////////////////////////////////////////////////////////////////////
	private void printRecord (String v0, Integer v1, Double v2, Double v3, Double v4, Double v5)
	{
		final String format = "%.1f";

		String s2 = String.format (format, v2);
		String s3 = (v1 == 1 ? "--" : String.format (format, v3));
		String s4 = (v1 == 1 ? "--" : String.format (format, v4));
		String s5 = (v1 == 1 ? "--" : String.format (format, v5));

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
	private class ProfileRecord
	{
		ProfileRecord (String method, int index, long startNano)
		{
			_method = method;
			_index = index;
			_startNano = startNano;
		}

		private String _method = "<???>";
		private int _index = 0;
		private long _startNano = 0;
		private long _elapsedNano = 0;
		private long _minNano = Long.MAX_VALUE;
		private long _maxNano = 0;
		private int _count = 0;
		private boolean _inProcess = true;
	}

	///////////////////////////////////////////////////////////////////////////
	private class ProfileRecordComparator implements Comparator<ProfileRecord>
	{
		public int compare (ProfileRecord record1, ProfileRecord record2)
		{
			return record1._index - record2._index;
		}
	}

	//members
	private Integer _fieldWidths[] = null;
	private HashMap<String, ProfileRecord> _records = null;
	private static int _currentIndex = 0;
	private static ContentProfiling _instance = null;

	private static Logger _log = LogManager.getLogger (ContentProfiling.class);
}
