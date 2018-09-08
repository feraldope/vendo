//AlbumProfiling.java - class for profiling performance

package com.vendo.albumServlet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumProfiling
{
	///////////////////////////////////////////////////////////////////////////
	public synchronized static AlbumProfiling getInstance ()
	{
		if (_instance == null) {
			_instance = new AlbumProfiling (); // _instance is deleted in print()
		}

		return _instance;
	}

//TODO - add enableProfiling(boolean b) or setProfileLevel (int profileLevel)

	///////////////////////////////////////////////////////////////////////////
	private AlbumProfiling ()
	{
		_fieldWidths = new Integer [6];
		_fieldWidths[0] = 20;	//method, default value - actual width calculated in print()
		_fieldWidths[1] = 6;	//count
		_fieldWidths[2] = 9;	//total
		_fieldWidths[3] = 8;	//average
		_fieldWidths[4] = 7;	//min
		_fieldWidths[5] = 7;	//max

		_records = new HashMap<String, ProfileRecord> (32);
		_currentIndex = 0;
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

		StringBuilder tags = new StringBuilder (tag1).append (".").append (tag2);
		enter (tags.toString (), false);
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

		synchronized (_records) {
			ProfileRecord record = _records.get (method.toString ());

			if (record == null) {
//				_log.debug ("AlbumProfiling.enter: adding record for \"" + method + "\"");
				record = new ProfileRecord (method.toString (), _currentIndex++, startNano);

			} else {
				if (record._inProcess) {
//					throw new RuntimeException ("AlbumProfiling.enter: recursive entry for \"" + method + "\"");
					_log.error ("AlbumProfiling.enter: recursive entry for \"" + method.toString () + "\"");
				}

				record._startNano = startNano;
				record._inProcess = true;
			}

			_records.put (method.toString (), record);
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

		StringBuilder tags = new StringBuilder (tag1).append (".").append (tag2);
		exit (tags.toString ());
	}

	private void exit (String tag)
	{
		long endNano = System.nanoTime ();

		StringBuilder method = new StringBuilder (getCallerName (new Throwable (), 2));

		if (tag != null) {
			method.append (".").append (tag);
		}

		synchronized (_records) {
			ProfileRecord record = _records.get (method.toString ());
			if (record == null) {
//				throw new RuntimeException ("AlbumProfiling.exit: entry does not exist for \"" + method + "\"");
				_log.error ("AlbumProfiling.exit: entry does not exist for \"" + method.toString () + "\"");
				return;
			}

			long elapsedNano = endNano - record._startNano;

			if (record._minNano > elapsedNano) {
				record._minNano = elapsedNano;
			}
			if (record._maxNano < elapsedNano) {
				record._maxNano = elapsedNano;
			}

			record._elapsedNano += elapsedNano;
			record._count++;
			record._startNano = 0;
			record._inProcess = false;

			_records.put (method.toString (), record);
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
		print (showMemoryUsage, /*resetProfiling*/ true);
	}

	///////////////////////////////////////////////////////////////////////////
	//note this resets all the profiling data
	public synchronized void print (boolean showMemoryUsage, boolean resetProfiling)
	{
//		if (!AlbumFormInfo._Profiling)
//			return;

		if (_records == null || _records.size () == 0) {
			if (resetProfiling) {
				_instance = null;
			}

			return;
		}

		int numRecords = _records.size ();
		List<ProfileRecord> records = new ArrayList<ProfileRecord> (numRecords);
		records.addAll (_records.values ());
		Collections.sort (records, new ProfileRecordComparator ());

		int longestMethodName = 10; //min width
		for (ProfileRecord record : records) {
			if (record._inProcess) {
//				throw new RuntimeException ("AlbumProfiling.print: exit not called for \"" + record._method + "\"");
//TODO			if (!resetProfiling)
				_log.error ("AlbumProfiling.print: exit not called for \"" + record._method + "\"");
			}

			int length = record._method.length ();
			longestMethodName = Math.max (length, longestMethodName);
		}
		_fieldWidths[0] = longestMethodName;// + 5;

		_log.debug ("AlbumProfiling.print (millisecs) --------------------------------------");
		printRecord ("Method", "Count", "Total", "Average", "Min", "Max");

		for (ProfileRecord record : records) {
			if (record._count > 0) {
				//convert values from nanosecs to millisecs
				double total = (double) record._elapsedNano / 1000000;
				double average = total / record._count;
				double min = (double) record._minNano / 1000000;
				double max = (double) record._maxNano / 1000000;

				printRecord (record._method, record._count, total, average, min, max);
			}
		}

		if (showMemoryUsage) {
			final double mega = 1024 * 1024;
			final double giga = mega * 1024;

			double freeMem  = Runtime.getRuntime ().freeMemory ();
			double totalMem = Runtime.getRuntime ().totalMemory ();
			double maxMem   = Runtime.getRuntime ().maxMemory ();
			double usedMem  = totalMem - freeMem;
			double util = 100 * totalMem / maxMem;

			boolean useGiga = false;
//			if (freeMem >= giga && totalMem >= giga && maxMem >= giga && usedMem >= giga) {
			if (freeMem >= giga || totalMem >= giga || maxMem >= giga || usedMem >= giga) {
				useGiga = true;
			}

			String unitSuffixStr = (useGiga ? "GB" : "MB");
			double unitSuffix = (useGiga ? giga : mega);

			usedMem  /= unitSuffix;
			freeMem  /= unitSuffix;
			totalMem /= unitSuffix;
			maxMem   /= unitSuffix;

			_log.debug ("AlbumProfiling.print: used="  + _decimalFormat1.format (usedMem)  + unitSuffixStr +
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
	private void printRecord (String v0, Integer v1, Double v2, Double v3, Double v4, Double v5)
	{
		String s2 = _decimalFormat0.format (v2);
		String s3 = (v1 == 1 ? "--" : _decimalFormat0.format (v3));
		String s4 = (v1 == 1 ? "--" : _decimalFormat0.format (v4));
		String s5 = (v1 == 1 ? "--" : _decimalFormat0.format (v5));

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
	private static class ProfileRecord
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
	private static class ProfileRecordComparator implements Comparator<ProfileRecord>
	{
		@Override
		public int compare (ProfileRecord record1, ProfileRecord record2)
		{
			return record1._index - record2._index;
		}
	}

	//members
	private final Integer _fieldWidths[];
	private final HashMap<String, ProfileRecord> _records;
	private static int _currentIndex = 0;
	private static AlbumProfiling _instance = null;

	private final DecimalFormat _decimalFormat0 = new DecimalFormat ("###,##0");
	private final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0.0");

	private static Logger _log = LogManager.getLogger ();
}
