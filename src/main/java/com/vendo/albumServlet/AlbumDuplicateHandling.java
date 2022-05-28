//AlbumDuplicate.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumDuplicateHandling
{
	SelectNone ("Select None"),
	SelectFirst ("Select First"), //select first image in pair
	SelectSecond ("Select Second"), //select second image in pair
	SelectSmaller ("Select Smaller"), //select the smaller image
	SelectSmallerFirst ("Select Smaller / First"), //select the smaller image, or first in pair if same size
	SelectSmallerSecond ("Select Smaller / Second"); //select the smaller image, or second in pair if same size

	///////////////////////////////////////////////////////////////////////////
	AlbumDuplicateHandling(String name)
	{
		_value = new AlbumStringPair (name, "select" + name);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _value.getName ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol ()
	{
		return _value.getSymbol ();
	}

	///////////////////////////////////////////////////////////////////////////
	private static void init ()
	{
		if (_values == null) {
			List<AlbumStringPair> arrayList = new ArrayList<> ();

			for (AlbumDuplicateHandling ff : values ()) {
				arrayList.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
			}

			_values = arrayList.toArray (new AlbumStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumStringPair[] getValues ()
	{
		init ();

		return _values;
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumDuplicateHandling getValue (String symbol)
	{
		//brute-force method
		for (AlbumDuplicateHandling ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumDuplicateHandling.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private AlbumStringPair _value;

	private static AlbumStringPair[] _values;

//	private static Logger _log = LogManager.getLogger ();
}
