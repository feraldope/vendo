//ReserRecords.java

package com.vendo.reserViewer;

import com.vendo.vendoUtils.TableSorter;

import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;


public class ReserRecords extends AbstractTableModel
{
	///////////////////////////////////////////////////////////////////////////
	public ReserRecords ()
	{
		_list = new ArrayList<ReserRecord> ();
	}

	///////////////////////////////////////////////////////////////////////////
	public List <ReserRecord> getRecords ()
	{
		return _list;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer size ()
	{
		return _list.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getInfoStr ()
	{
		int durationMins = 0;
		for (int ii = 0; ii < size (); ii++)
			durationMins += _list.get (ii).getDurationMins ();
		int gigaBytes = durationMins * 15 / 2 / 60; //aprox 7.5GB/hour

		Integer hour = durationMins / 60;
		Integer minute = durationMins % 60;
		StringBuffer sb = new StringBuffer ();
		sb.append (hour.toString ())
		  .append (":")
		  .append (String.format ("%02d", minute));
		String durationStr = new String (sb.toString ());

		sb = new StringBuffer ();
		sb.append (_list.size ())
		  .append (" reservations, ")
		  .append (durationStr)
		  .append (" hours, ~")
		  .append (gigaBytes)
		  .append (" GB") ;
		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int readFile (String filename) throws ReserException
	{
		Scanner input;
		try {
			File file = new File (filename);
			filename = file.getCanonicalPath ();

			input = new Scanner (file);

		} catch (IOException exception) {
			throw new ReserException ("Error opening file \"" + filename + "\"");
		}

		String line;
		ReserRecord record = null;
		while (input.hasNext ()) {
			line = input.nextLine ().trim ();

			//ignore these lines
			if (line.length () == 0 || line.startsWith (";")) {
			} else if (line.startsWith ("[ReservationCapture]")) {
			} else if (line.startsWith ("count = ")) {
//todo - compare count in file to number of records processed
			} else if (foundKeyword (line, "ChName")) {
			} else if (foundKeyword (line, "ChNum")) {
			} else if (foundKeyword (line, "DirStyle")) {
			} else if (foundKeyword (line, "FileName")) {
			} else if (foundKeyword (line, "FileNameStyle")) {
			} else if (foundKeyword (line, "PhyCh")) {
			} else if (foundKeyword (line, "UseDefaultFileName")) {

			} else if (line.startsWith ("[No. ")) {
				if (includeThisRecord (record))
					_list.add (record);			//add previous record
				record = new ReserRecord ();	//and get new record

			} else if (foundKeyword (line, "Title")) {
				record.setTitle (line);

			} else if (foundKeyword (line, "FileType")) {
				record.setFileType (line);

			} else if (foundKeyword (line, "Input")) {
				record.setInput (line);

			} else if (foundKeyword (line, "VirCh")) {
				record.setVirtualChannel (line);

			} else if (foundKeyword (line, "SubCh")) {
				record.setSubChannel (line);

			} else if (foundKeyword (line, "DateMode")) {
				record.setDateMode (line);

			} else if (foundKeyword (line, "Year")) {
				record.setYear (line);

			} else if (foundKeyword (line, "Month")) {
				record.setMonth (line);

			} else if (foundKeyword (line, "Day")) {
				record.setDay (line);

			} else if (foundKeyword (line, "Week")) {
				record.setWeek (line);

			} else if (foundKeyword (line, "StartTime_Hour")) {
				record.setStartHour (line);

			} else if (foundKeyword (line, "StartTime_Min")) {
				record.setStartMinute (line);

			} else if (foundKeyword (line, "EndTime_Hour")) {
				record.setEndHour (line);

			} else if (foundKeyword (line, "EndTime_Min")) {
				record.setEndMinute (line);

			} else {
				String msg = "unrecognized line ignored \"" + line + "\"";
				JOptionPane.showMessageDialog (null, msg, "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}

		//add the last record
		if (includeThisRecord (record))
			_list.add (record);

		input.close ();

		System.out.println ("readFile found " + _list.size () + " records");

		if (_list.size () == 0)
			throw new ReserException ("No applicable records found");

		return _list.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean foundKeyword (String line, String keyword)
	{
		return line.startsWith (keyword + " =");
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean includeThisRecord (ReserRecord record)
	{
		if (record == null)
			return false;

		if (record.isFutureRecording () || ReserViewer._Debug)
			return true;

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getCellWidth (int col)
	{
		switch (col) {
		default: return 0;
		case 0: return 16;	//Title
		case 1: return 3;	//Type
		case 2: return 4;	//Input
		case 3: return 5;	//Channel
		case 4: return 10;	//Date
		case 5: return 6;	//Start Time
		case 6: return 5;	//Duration
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//
	// Methods for TableSorter
	//
	///////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////
	public void initTableSorter (TableSorter sorter)
	{
		ReserRecord record = _list.get (0);
		sorter.setColumnComparator (String.class, _caseInsensitiveStringComparator);
		sorter.setColumnComparator (record.getChannel ().getClass (), _reserChannelComparator);
		sorter.setColumnComparator (record.getDate ().getClass (), _reserDateComparator);
		sorter.setColumnComparator (record.getStartTime ().getClass (), _reserTimeComparator);
		sorter.setColumnComparator (record.getDuration ().getClass (), _reserTimeComparator);
	}

	///////////////////////////////////////////////////////////////////////////
	public int getColumnCount ()
	{
		return 7;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getRowCount ()
	{
		return size ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getColumnName (int col)
	{
		switch (col) {
			default: return "unknown";
			case 0: return "Title";
			case 1: return "Type";
			case 2: return "Input";
			case 3: return "Channel";
			case 4: return "Date";
			case 5: return "Start Time";
			case 6: return "Duration";
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public Object getValueAt (int row, int col)
	{
		ReserRecord record = _list.get (row);
		switch (col) {
			default: return "unknown";
			case 0: return record.getTitle ();
			case 1: return record.getType ();
			case 2: return record.getInput ();
			case 3: return record.getChannel ();
			case 4: return record.getDate ();
			case 5: return record.getStartTime ();
			case 6: return record.getDuration ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public Class getColumnClass (int col)
	{
		return getValueAt (0, col).getClass ();
	}

	///////////////////////////////////////////////////////////////////////////
	private static final Comparator _caseInsensitiveStringComparator = new Comparator ()
	{
		public int compare (Object o1, Object o2)
		{
			return o1.toString ().compareToIgnoreCase (o2.toString ());
		}
	};

	///////////////////////////////////////////////////////////////////////////
	private static final Comparator _reserChannelComparator = new Comparator ()
	{
		public int compare (Object o1, Object o2)
		{
			Double d1 = new Double (o1.toString ());
			Double d2 = new Double (o2.toString ());

			return d1.compareTo (d2);
		}
	};

	///////////////////////////////////////////////////////////////////////////
	//note this does not handle year-wrap
	private static final Comparator _reserDateComparator = new Comparator ()
	{
		final SimpleDateFormat _dateFormat = new SimpleDateFormat ("E MM/dd"); //e.g., "Tuesday 11/18"

		public int compare (Object o1, Object o2)
		{
			ReserDate d1 = (ReserDate) o1;
			ReserDate d2 = (ReserDate) o2;

			return d1.compareTo (d2);
		}
	};

	///////////////////////////////////////////////////////////////////////////
	//used for sorting ReserTime and ReserDuration
	//note this does not handle shows with duration = 12 hours
	private static final Comparator _reserTimeComparator = new Comparator ()
	{
		final SimpleDateFormat _dateFormat = new SimpleDateFormat ("hh:mm a"); //e.g., "9:30 PM"

		public int compare (Object o1, Object o2)
		{
			String s1 = o1.toString ();
			String s2 = o2.toString ();

			//append AM to duration strings to use parseDateTime
			if (!s1.endsWith ("M")) {
				s1 += " AM";
				s2 += " AM";
			}

			Date d1 = parseDateTime (_dateFormat, s1);
			Date d2 = parseDateTime (_dateFormat, s2);

			return d1.compareTo (d2);
		}
	};

	///////////////////////////////////////////////////////////////////////////
	public static final Date parseDateTime (SimpleDateFormat dateFormat, String str)
	{
		Date date = new Date ();

		try {
			date = dateFormat.parse (str);
		} catch(ParseException ee) {
			System.out.println ("Error: could not parse string \"" + str + "\"");
		}

		return date;
	}

	//members
	private List<ReserRecord> _list = null;
}
