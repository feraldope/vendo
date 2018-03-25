//SaveDate.java

package com.vendo.save;

import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.swing.JOptionPane;

public class SaveDate
{
	///////////////////////////////////////////////////////////////////////////
	public SaveDate ()
	{
		GregorianCalendar today = new GregorianCalendar ();
		init (today.get (Calendar.MONTH) + 1, today.get (Calendar.YEAR));
	}

	///////////////////////////////////////////////////////////////////////////
	public SaveDate (SaveDate date)
	{
		init (date.getMonth (), date.getYear ());
	}

	///////////////////////////////////////////////////////////////////////////
	//format = mm/yyyy
	public SaveDate (String date)
	{
		boolean valid = false;

		try {
			int slash = date.indexOf ("/");
			int month = Integer.parseInt (date.substring (0, slash));
			int year = Integer.parseInt (date.substring (slash + 1, date.length ()));
			valid = init (month, year);

		} catch (NumberFormatException ee) {
			valid = false;
		}

		if (!valid)
			JOptionPane.showMessageDialog (null, "Error parsing date \"" + date + "\", using value \"" + toString () + "\"", "Warning", JOptionPane.ERROR_MESSAGE);
	}

	///////////////////////////////////////////////////////////////////////////
	public SaveDate (int month, int year)
	{
		if (!init (month, year))
			JOptionPane.showMessageDialog (null, "Invalid date \"" + month + "/" + year + "\", using value \"" + toString () + "\"", "Warning", JOptionPane.ERROR_MESSAGE);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean init (int month, int year)
	{
		if (month < 1 || month > 12 || year < 1900)
			return false;

		_month = month;
		_year = year;

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean lessThanOrEqual (SaveDate endDate)
	{
		int date = (_year * 12) + _month;
		int end = (endDate.getYear () * 12) + endDate.getMonth ();

		return date <= end;
	}

	///////////////////////////////////////////////////////////////////////////
	public SaveDate addYears (int years)
	{
		return new SaveDate (_month, _year + years);
	}

	///////////////////////////////////////////////////////////////////////////
	public void nextMonth ()
	{
		_month++;
		if (_month > 12) {
			_month = 1;
			_year++;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public int getMonth ()
	{
		return _month;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getYear ()
	{
		return _year;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isToday ()
	{
		SaveDate today = new SaveDate ();
		return (today.getMonth () == _month && today.getYear () == _year);
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		if (isToday ())
			return "Today";

		return String.format ("%02d/%04d", _month, _year);
	}

	//members
	private int _month = 1;			//1-based (Jan = 1)
	private int _year = 1970;		//4 digit
}
