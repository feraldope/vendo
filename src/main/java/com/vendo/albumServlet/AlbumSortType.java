//AlbumSortType.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumSortType
{
	//Enum (name, visibleInUi)
	ByNone ("None", false),
	ByName ("Name", true),
	ByDate ("Date", true),
	ByExif ("EXIF", true),
	BySizeBytes ("Size (bytes)", true),
	BySizePixels ("Size (pixels)", true),
	ByCount ("Count", true),
	ByHash ("Hash", true),
	ByRgb ("RGB", true),
	ByRandom ("Random", true);
//	ByImageNumber ("Image Number", true);

	///////////////////////////////////////////////////////////////////////////
	AlbumSortType (String name, boolean isVisibleInUi)
	{
		_value = new AlbumStringPair (name, "by" + name);
		_isVisibleInUi = isVisibleInUi;
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
	public boolean isVisibleInUi ()
	{
		return _isVisibleInUi;
	}

	///////////////////////////////////////////////////////////////////////////
	private static void init ()
	{
		if (_values == null || _uiValues == null) {
			List<AlbumStringPair> pairs = new ArrayList<> ();
			List<AlbumStringPair> uiPairs = new ArrayList<> ();

			for (AlbumSortType ff : values ()) {
				pairs.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
				if (ff._isVisibleInUi) {
					uiPairs.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
				}
			}

			_values = pairs.toArray (new AlbumStringPair[] {});
			_uiValues = uiPairs.toArray (new AlbumStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumStringPair[] getValues (boolean visibleInUi)
	{
		init ();

		if (visibleInUi) {
			return _uiValues;
		} else {
			return _values;
		}
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
	private final AlbumStringPair _value;
	private final boolean _isVisibleInUi;

	private static AlbumStringPair[] _values;
	private static AlbumStringPair[] _uiValues;

//	private static Logger _log = LogManager.getLogger ();
}
