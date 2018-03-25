//ReserDate.java

package com.vendo.reserViewer;

import java.util.*;

//TODO - move date schedule logic from ReserRecord.getDate ()

public class ReserDate
{
	///////////////////////////////////////////////////////////////////////////
	public ReserDate (long millis)
	{
		_millis = millis;
		_isString = false;
	}

	///////////////////////////////////////////////////////////////////////////
	public ReserDate (String string)
	{
		_string = new String (string);
		_isString = true;
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		if (_isString)
			return _string;
		else {

//todo - cache this string?

			GregorianCalendar specificDate = new GregorianCalendar ();
			specificDate.setTimeInMillis (_millis);
			Integer weekDay = specificDate.get (Calendar.DAY_OF_WEEK);
			Integer month = specificDate.get (Calendar.MONTH);
			Integer date = specificDate.get (Calendar.DATE);

			String dayOfWeek;
			if (weekDay == Calendar.SUNDAY)
				dayOfWeek = "Sunday";
			else if (weekDay == Calendar.MONDAY)
				dayOfWeek = "Monday";
			else if (weekDay == Calendar.TUESDAY)
				dayOfWeek = "Tuesday";
			else if (weekDay == Calendar.WEDNESDAY)
				dayOfWeek = "Wednesday";
			else if (weekDay == Calendar.THURSDAY)
				dayOfWeek = "Thursday";
			else if (weekDay == Calendar.FRIDAY)
				dayOfWeek = "Friday";
			else if (weekDay == Calendar.SATURDAY)
				dayOfWeek = "Saturday";
			else								//unrecognized
				dayOfWeek = "<" + weekDay.toString () + ">";

			return dayOfWeek + " " + String.format ("%02d", month + 1)
							 + "/" + String.format ("%02d", date);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public int compareTo (ReserDate other) {

		//sort string values to front of list
		if (_isString && other._isString) {
			return toString ().compareToIgnoreCase (other.toString ());

		} else if (!_isString && other._isString) {
			return 1;

		} else if (_isString && !other._isString) {
			return -1;

		} else {
			long diff = _millis - other._millis;
			if (diff > 0)
				return 1;
			else if (diff < 0)
				return -1;
			else
				return 0;
		}
	}

	//members
	private boolean _isString = false;
	private long _millis = 0;
	private String _string = null;
}
