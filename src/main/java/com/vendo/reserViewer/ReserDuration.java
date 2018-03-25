//ReserDuration.java - simply a class that holds a String
//					   created to allow use of time-specific setColumnComparator

//Note that the time comparison for sorting is done by ReserRecords._reserTimeComparator ()

package com.vendo.reserViewer;

public class ReserDuration
{
	///////////////////////////////////////////////////////////////////////////
	public ReserDuration (String string)
	{
		_string = new String (string);
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		return _string;
	}

	//members
	private String _string = null;
}