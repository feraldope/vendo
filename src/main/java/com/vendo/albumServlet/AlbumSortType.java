//AlbumSortType.java

package com.vendo.albumServlet;

import java.util.*;

//import org.apache.logging.log4j.*;


public enum AlbumSortType
{
	ByName ("Name"),
	ByDate ("Date"),
	ByExif ("EXIF"),
	BySizeBytes ("Size (bytes)"),
	BySizePixels ("Size (pixels)"),
//TODO - fix this
//	ByCount ("Count"),
	ByHash ("Hash"),
	ByRgb ("RGB"),
	ByRandom ("Random");

	///////////////////////////////////////////////////////////////////////////
	AlbumSortType (String name)
	{
		_value = new AlbumStringPair (name, "by" + name);
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

			for (AlbumSortType ff : values ()) {
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
	public static AlbumSortType getValue (String symbol)
	{
		//brute-force method
		for (AlbumSortType ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumSortType.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private AlbumStringPair _value;

	private static AlbumStringPair[] _values;

//	private static Logger _log = LogManager.getLogger ();
}
