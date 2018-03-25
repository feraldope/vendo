//ReserChannel.java - simply a class that holds a String
//					  created to allow use of time-specific setColumnComparator

//Note that the channel comparison for sorting is done by ReserRecords._reserChannelComparator ()

package com.vendo.reserViewer;

public class ReserChannel
{
	///////////////////////////////////////////////////////////////////////////
	public ReserChannel (String string)
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