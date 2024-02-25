//AlbumSortType.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumSortType
{
	//Enum (name, visibleInUi)
	ByNone ("None", false, false),
	ByName ("Name", true, false),
	ByDate ("Date", true, false),
	ByExif ("EXIF", true, false),
	BySizeBytes ("Size (bytes)", true, true),
	BySizePixels ("Size (pixels)", true, true),
	ByBytesPerPixel ("Bytes/pixel", true, true),
	ByCount ("Count", true, false),
	ByHash ("Hash", true, false),
	ByRgb ("RGB", true, false),
	ByRandom ("Random", true, false);
//	ByImageNumber ("Image Number", true, false);

	///////////////////////////////////////////////////////////////////////////
	AlbumSortType (String name, boolean isVisibleInUi, boolean propagateValueToDrillDowns)
	{
		_value = new AlbumStringPair (name, "by" + name);
		_isVisibleInUi = isVisibleInUi;
		_propagateValueToDrillDowns = propagateValueToDrillDowns;
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
	public boolean isPropagateValueToDrillDowns ()
	{
		return _propagateValueToDrillDowns;
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
	private final boolean _propagateValueToDrillDowns;

	private static AlbumStringPair[] _values;
	private static AlbumStringPair[] _uiValues;

//	private static Logger _log = LogManager.getLogger ();
}
