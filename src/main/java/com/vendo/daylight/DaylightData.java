//DaylightData.java

package com.vendo.daylight;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

//import org.apache.logging.log4j.*;


public class DaylightData
{
	private enum Mode {Sunrise, Sunset};

	///////////////////////////////////////////////////////////////////////////
	//contains one DaylightRecord object for each day of the year
	public DaylightData (Collection<DaylightFileData> daylightFileDataList)
	{
		for (DaylightFileData daylightFileData : daylightFileDataList) {
//			int dayOfMonth = daylightFileData.getDayOfMonth ();
			for (int month = 1; month <= 12; month++) {
				LocalDate localDate = daylightFileData.getLocalDate (month);

				if (localDate != null) {
					LocalTime sunriseTime = daylightFileData.getSunriseTime (month);
					LocalTime sunsetTime = daylightFileData.getSunsetTime (month);

					_daylightRecordMap.put (localDate, new DaylightRecord (localDate, sunriseTime, sunsetTime));
				}
			}
		}

//TODO - this is not very precise since the data file only has minute granularity, and multiple days have the same sunrise or sunset
		Duration shortestDuration = Duration.ofDays (1);
		Duration longestDuration = Duration.ZERO;
		for (DaylightRecord daylightRecord : getDaylightDataList ()) {

			Duration lengthOfDay = daylightRecord.getLengthOfDay ();
			if (shortestDuration.compareTo (lengthOfDay) > 0) {
				shortestDuration = lengthOfDay;
				_shortestDay = daylightRecord.getLocalDate ().plusDays (4); //hack!
			}
			if (longestDuration.compareTo (lengthOfDay) < 0) {
				longestDuration = lengthOfDay;
				_longestDay = daylightRecord.getLocalDate ().plusDays (4); //hack!
			}
		}

		_earliestSunriseDate = getDateOfEarliest (Mode.Sunrise);
		_latestSunriseDate = getDateOfLatest (Mode.Sunrise);
		_earliestSunsetDate = getDateOfEarliest (Mode.Sunset);
		_latestSunsetDate = getDateOfLatest (Mode.Sunset);

		if (Daylight._Debug) {
			System.out.println ("shortestDay: " + _shortestDay +
								", longestDay: " + _longestDay +
								", earliestSunriseDate: " + _earliestSunriseDate +
								", latestSunriseDate: " + _latestSunriseDate +
								", earliestSunsetDate: " + _earliestSunsetDate +
								", latestSunsetDate: " + _latestSunsetDate);

		}
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalDate getDateOfEarliest (Mode mode)
	{
		final boolean honorDst = false; //ignore DST for this processing

		//find earliest time and index of the first instance of that time
		LocalTime earliestTime = LocalTime.MAX;
		int startIndex = 0;
		int ii = 0;
		for (ii = 0; ii < getDaylightDataList ().size (); ii++) {
			DaylightRecord daylightRecord = getDaylightDataList ().get (ii);
			LocalTime localTime = (mode == Mode.Sunrise ? daylightRecord.getSunriseTime (honorDst) : daylightRecord.getSunsetTime (honorDst));
			if (earliestTime.compareTo (localTime) > 0) {
				earliestTime = localTime;
				startIndex = ii;
			}
		}

		//find last instance of earliest time
		int endIndex = 0;
		for (ii = startIndex; ii < getDaylightDataList ().size (); ii++) {
			DaylightRecord daylightRecord = getDaylightDataList ().get (ii);
			LocalTime localTime = (mode == Mode.Sunrise ? daylightRecord.getSunriseTime (honorDst) : daylightRecord.getSunsetTime (honorDst));
			if (earliestTime.compareTo (localTime) != 0) {
				endIndex = ii - 1;
				break;
			}
		}

		int middleIndex = (startIndex + endIndex + 1) / 2;

		return getDaylightDataList ().get (middleIndex).getLocalDate ();
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalDate getDateOfLatest (Mode mode)
	{
		final boolean honorDst = false; //ignore DST for this processing

		//find latest time and index of the first instance of that time
		LocalTime latestTime = LocalTime.MIN;
		int startIndex = 0;
		int ii = 0;
		for (ii = 0; ii < getDaylightDataList ().size (); ii++) {
			DaylightRecord daylightRecord = getDaylightDataList ().get (ii);
			LocalTime localTime = (mode == Mode.Sunrise ? daylightRecord.getSunriseTime (honorDst) : daylightRecord.getSunsetTime (honorDst));
			if (latestTime.compareTo (localTime) < 0) {
				latestTime = localTime;
				startIndex = ii;
			}
		}

		//find last instance of latest time
		int endIndex = 0;
		for (ii = startIndex; ii < getDaylightDataList ().size (); ii++) {
			DaylightRecord daylightRecord = getDaylightDataList ().get (ii);
			LocalTime localTime = (mode == Mode.Sunrise ? daylightRecord.getSunriseTime (honorDst) : daylightRecord.getSunsetTime (honorDst));
			if (latestTime.compareTo (localTime) != 0) {
				endIndex = ii - 1;
				break;
			}
		}

		int middleIndex = (startIndex + endIndex + 1) / 2;

		return getDaylightDataList ().get (middleIndex).getLocalDate ();
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getDaylightRecord (LocalDate localDate)
	{
		return _daylightRecordMap.get (localDate);
	}

	///////////////////////////////////////////////////////////////////////////
	public ArrayList<DaylightRecord> getDaylightDataList ()
	{
		ArrayList<DaylightRecord> list = new ArrayList<DaylightRecord> ();
		list.addAll (_daylightRecordMap.values ());
		Collections.sort (list, _daylightDataComparator);

		return list;
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getShortestDay ()
	{
		return _daylightRecordMap.get (_shortestDay);
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getLongestDay ()
	{
		return _daylightRecordMap.get (_longestDay);
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getEarliestSunrise ()
	{
		return _daylightRecordMap.get (_earliestSunriseDate);
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getLatestSunrise ()
	{
		return _daylightRecordMap.get (_latestSunriseDate);
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getEarliestSunset ()
	{
		return _daylightRecordMap.get (_earliestSunsetDate);
	}

	///////////////////////////////////////////////////////////////////////////
	public DaylightRecord getLatestSunset ()
	{
		return _daylightRecordMap.get (_latestSunsetDate);
	}

	///////////////////////////////////////////////////////////////////////////
	private static final Comparator<DaylightRecord> _daylightDataComparator = new Comparator<DaylightRecord> ()
	{
		public int compare (DaylightRecord o1, DaylightRecord o2)
		{
			return o1.getLocalDate ().compareTo (o2.getLocalDate ());
		}
	};


	//private members
	private LocalDate _shortestDay;
	private LocalDate _longestDay;
	private LocalDate _earliestSunriseDate;
	private LocalDate _latestSunriseDate;
	private LocalDate _earliestSunsetDate;
	private LocalDate _latestSunsetDate;
	private Map<LocalDate, DaylightRecord> _daylightRecordMap = new HashMap<LocalDate, DaylightRecord> ();

//	private static final Logger _log = LogManager.getLogger ();
}
