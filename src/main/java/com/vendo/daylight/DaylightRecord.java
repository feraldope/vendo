//DaylightRecord.java

package com.vendo.daylight;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

//import org.apache.logging.log4j.*;


public class DaylightRecord
{
	///////////////////////////////////////////////////////////////////////////
	DaylightRecord (LocalDate localDate, LocalTime sunriseTime, LocalTime sunsetTime)
	{
		_localDate = localDate;
		_sunriseTime = sunriseTime;
		_sunsetTime = sunsetTime;

		_lengthOfDay = Duration.between (_sunriseTime, _sunsetTime);
		_isDst = isDst (_localDate);
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalDate getLocalDate ()
	{
		return _localDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalTime getSunriseTime ()
	{
		return getSunriseTime (true);
	}
	public LocalTime getSunriseTime (boolean honorDst)
	{
		return (_isDst && honorDst) ? _sunriseTime.plusHours (1) : _sunriseTime;
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalTime getSunsetTime ()
	{
		return getSunsetTime (true);
	}
	public LocalTime getSunsetTime (boolean honorDst)
	{
		return (_isDst && honorDst) ? _sunsetTime.plusHours (1) : _sunsetTime;
	}

	///////////////////////////////////////////////////////////////////////////
	public Duration getLengthOfDay ()
	{
		return _lengthOfDay;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append ("[localDate=" + _localDate);
		sb.append (", sunriseTime=" + _sunriseTime);
		sb.append (", sunsetTime=" + _sunsetTime);
		sb.append (", lengthOfDay=" + _lengthOfDay);
		sb.append (", isDst=" + new Boolean (_isDst));
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	private static boolean isDst (LocalDate localDate)
	{
//TODO - calculate DST dates instead of hardcoding
		final LocalDate _dstStart = LocalDate.of (2018, 03, 11); //hardcoded to actual value for 2018
		final LocalDate _dstEnd   = LocalDate.of (2018, 11, 04); //hardcoded to actual value for 2018

		return localDate.isAfter (_dstStart) && localDate.isBefore (_dstEnd);
	}


	//private members
	private LocalDate _localDate;
	private LocalTime _sunriseTime;
	private LocalTime _sunsetTime;
	private Duration _lengthOfDay;
	private boolean _isDst;

//	private static final Logger _log = LogManager.getLogger ();
}
