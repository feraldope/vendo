//AlbumOrientation.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumOrientation
{
	ShowAny ("Any"),
	ShowLandScape ("LandScape"),
	ShowPortrait ("Portrait"),
	ShowSquare ("Square");

	///////////////////////////////////////////////////////////////////////////
	AlbumOrientation (String name)
	{
		_value = new AlbumStringPair (name, "show" + name);
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
			List<AlbumStringPair> arrayList = new ArrayList<AlbumStringPair> ();

			for (AlbumOrientation ff : values ()) {
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
	public static AlbumOrientation getValue (String symbol)
	{
		//brute-force method
		for (AlbumOrientation ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumOrientation.getValue: invalid symbol \"" + symbol + "\"");
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumOrientation getOrientation (int imageWidth, int imageHeight)
	{
		final int squareSlop = 10; //allowable variation from exactly square that will still be considered square

		int diff = imageWidth - imageHeight;
		if (diff > squareSlop) {
			return AlbumOrientation.ShowLandScape;
		} else if (diff < -squareSlop) {
			return AlbumOrientation.ShowPortrait;
		} else {
			return AlbumOrientation.ShowSquare;
		}
	}


	//members
	private AlbumStringPair _value;

	private static AlbumStringPair[] _values;

//	private static Logger _log = LogManager.getLogger ();
}
