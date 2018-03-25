//ReserRecord.java

package com.vendo.reserViewer;

import java.util.*;
import javax.swing.*;

public class ReserRecord
{
	///////////////////////////////////////////////////////////////////////////
	public ReserRecord ()
	{
	}

	///////////////////////////////////////////////////////////////////////////
	private String extractString (String string)
	{
		int pos = string.indexOf (" = ");
		if (pos >= 0)
			return string.substring (pos + 3);

		return string;
	}

	///////////////////////////////////////////////////////////////////////////
	private Integer extractInteger (String string)
	{
		int pos = string.indexOf (" = ");
		if (pos >= 0)
			string = string.substring (pos + 3);

		Integer value = -1;

		try {
			value = Integer.parseInt (string);
		} catch (NumberFormatException exception) {
			String msg = "Error parsing number \"" + string + "\"";
			JOptionPane.showMessageDialog (null, msg, "Error", JOptionPane.ERROR_MESSAGE);
			System.exit (1);
		}

		return value;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setTitle (String title)
	{
		_title = extractString (title);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setFileType (String fileType)
	{
		_fileType = extractInteger (fileType);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setInput (String input)
	{
		_input = extractInteger (input);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setVirtualChannel (String virtualChannel)
	{
		_virtualChannel = extractInteger (virtualChannel);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setSubChannel (String subChannel)
	{
		_subChannel = extractInteger (subChannel);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setYear (String year)
	{
		_year = extractInteger (year);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setDateMode (String dateMode)
	{
		_dateMode = extractInteger (dateMode);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMonth (String month)
	{
		//convert 1-based month (MyHD) to 0-based (java)
		_month = extractInteger (month) - 1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setDay (String day)
	{
		_day = extractInteger (day);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setWeek (String week)
	{
		_week = extractInteger (week);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setStartHour (String startHour)
	{
		_startHour = extractInteger (startHour);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setStartMinute (String startMinute)
	{
		_startMinute = extractInteger (startMinute);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setEndHour (String endHour)
	{
		_endHour = extractInteger (endHour);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setEndMinute (String endMinute)
	{
		_endMinute = extractInteger (endMinute);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getTitle ()
	{
		return _title;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getType ()
	{
		switch (_fileType) {
		case 1:		return "AVI";
		case 2:		return "TP";
		default:	return "<" + _fileType.toString () + ">"; //unrecognized
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public String getInput ()
	{
		switch (_input) {
		case 0:		return "Ant1";
		case 1:		return "Ant2";
		default:	return "<" + _input.toString () + ">"; //unrecognized
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public ReserChannel getChannel ()
	{
		String channel = _virtualChannel.toString ();
		if (_subChannel != 0)
			channel += "." + _subChannel.toString ();
		return new ReserChannel (channel);
	}

	///////////////////////////////////////////////////////////////////////////
	public ReserDate getDate ()
	{
		if (_dateMode == 0) {					//specific date
			GregorianCalendar specificDate = new GregorianCalendar (_year, _month, _day, _startHour, _startMinute);
			return new ReserDate (specificDate.getTimeInMillis ());

		} else if (_dateMode == 1) {			//daily
			return new ReserDate ("Daily");

		} else if (_dateMode == 2) {			//weekly
			BitSet bitSet = new BitSet ();
			for (int ii = 0; ii < 7; ii++) {
				int mask = 1 << ii;
				if ((_week & mask) == mask)
					bitSet.set (ii);
			}

			String daysOfWeek = new String ();
			if (bitSet.cardinality() == 1) {	//use full day name
				if (bitSet.get (0))
					daysOfWeek = "Sunday";
				else if (bitSet.get (1))
					daysOfWeek = "Monday";
				else if (bitSet.get (2))
					daysOfWeek = "Tuesday";
				else if (bitSet.get (3))
					daysOfWeek = "Wednesday";
				else if (bitSet.get (4))
					daysOfWeek = "Thursday";
				else if (bitSet.get (5))
					daysOfWeek = "Friday";
				else if (bitSet.get (6))
					daysOfWeek = "Saturday";

			} else {							//use abbreviated day names
				if (bitSet.get (0))
					daysOfWeek += ",Su";
				if (bitSet.get (1))
					daysOfWeek += ",M";
				if (bitSet.get (2))
					daysOfWeek += ",Tu";
				if (bitSet.get (3))
					daysOfWeek += ",W";
				if (bitSet.get (4))
					daysOfWeek += ",Th";
				if (bitSet.get (5))
					daysOfWeek += ",F";
				if (bitSet.get (6))
					daysOfWeek += ",Sa";
				daysOfWeek = daysOfWeek.substring (1);	//skip leading comma
			}

			return new ReserDate ("Every " + daysOfWeek);

		} else {								//unrecognized
			return new ReserDate ("<" + _dateMode.toString () + ">");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public ReserTime getStartTime ()
	{
		String amPm = " AM";
		int hour = _startHour;
		if (hour >= 12) {
			hour -= 12;
			amPm = " PM";
		}

		if (hour == 0)
			hour = 12;

		return new ReserTime (String.format ("%d", hour) + ":" +
							  String.format ("%02d", _startMinute) + amPm);
	}

	///////////////////////////////////////////////////////////////////////////
	//also calculates _durationMins as a side effect
	public ReserDuration getDuration ()
	{
		_durationMins = (_endHour * 60 + _endMinute) - (_startHour * 60 + _startMinute);
		if (_durationMins < 0) //handle midnight wrap
			_durationMins += 24 * 60;

		Integer hour = _durationMins / 60;
		Integer minute = _durationMins % 60;

		StringBuffer sb = new StringBuffer ();
		sb.append (hour.toString ())
		  .append (":")
		  .append (String.format ("%02d", minute));

		return new ReserDuration (sb.toString ());
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getDurationMins ()
	{
		getDuration ();	//force calculation of _durationMins
		return _durationMins;
	}

//obsolete
//	///////////////////////////////////////////////////////////////////////////
//	public Integer getSortValue ()
//	{
//		int value;
//
//System.out.println ("called ReserRecord.getSortValue");
//		if (isRepeating ()) {
//			int base = 50000000; //force repeating recordings to end of list
//			value = base + _week * 10000 + _startHour * 100 + _startMinute;
//
//		} else {
//			GregorianCalendar startTimestamp = new GregorianCalendar (_year, _month, _day, _startHour, _startMinute);
//			long millis = startTimestamp.getTimeInMillis () / 60000; //convert to minutes
//			value = (int) millis;
//		}
//
//		return value;
//	}

	///////////////////////////////////////////////////////////////////////////
	public Boolean isRepeating ()
	{
		switch (_dateMode) {
		case 1: //daily
		case 2: //weekly
			return true;
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public Boolean isFutureRecording ()
	{
		if (isRepeating ())
			return true;

		getDuration (); //forces calculation of _durationMins

		//calculate end from start + duration
		GregorianCalendar endTimestamp = new GregorianCalendar ();
		endTimestamp.set (_year, _month, _day, _startHour, _startMinute, 0);
		endTimestamp.add (Calendar.MINUTE, _durationMins);

		return endTimestamp.after (new GregorianCalendar ());
	}

	//members
	private String _title = null;
	private Integer _fileType = null;
	private Integer _input = null;
	private Integer _virtualChannel = null;
	private Integer _subChannel = null;
	private Integer _dateMode = null;
	private Integer _year = null;
	private Integer _month = null;	//0 to 11
	private Integer _day = null;
	private Integer _week = null;
	private Integer _startHour = null;
	private Integer _startMinute = null;
	private Integer _endHour = null;
	private Integer _endMinute = null;
	private int _durationMins = (-1);
}
