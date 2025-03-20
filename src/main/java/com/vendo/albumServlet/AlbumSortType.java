//AlbumSortType.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumSortType {

	//Enum (name, visibleInUi)
	ByNone ("None", false, false, false),
	ByName ("Name", true, false, false),
	ByDate ("Date", true, false, false),
	ByExif ("EXIF", true, false, false),
	BySizeBytes ("Size (bytes)", true, true, true),
	BySizeAvgBytes ("Size (avg bytes)", true, true, true),
	BySizePixels ("Size (pixels)", true, true, false),
	ByBytesPerPixel ("Bytes/pixel", true, true, false),
	ByCount ("Count", true, false, false),
	ByHash ("Hash", true, false, false),
	ByRgb ("RGB", true, false, false),
	ByRandom ("Random", true, false, false);
//	ByImageNumber ("Image Number", true, false);

	///////////////////////////////////////////////////////////////////////////
	AlbumSortType (String name, boolean isVisibleInUi, boolean propagateValueToDrillDowns, boolean comparatorUsesCache) {
		value = new AlbumStringPair (name, "by" + name);
		this.isVisibleInUi = isVisibleInUi;
		this.propagateValueToDrillDowns = propagateValueToDrillDowns;
		this.comparatorUsesCache = comparatorUsesCache;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName () {
		return value.getName ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol () {
		return value.getSymbol ();
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isVisibleInUi () {
		return isVisibleInUi;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isPropagateValueToDrillDowns () {
		return propagateValueToDrillDowns;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isComparatorUsesCache () {
		return comparatorUsesCache;
	}

	///////////////////////////////////////////////////////////////////////////
	private static void init () {
		if (values == null || uiValues == null) {
			List<AlbumStringPair> pairs = new ArrayList<> ();
			List<AlbumStringPair> uiPairs = new ArrayList<> ();

			for (AlbumSortType ff : values ()) {
				pairs.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
				if (ff.isVisibleInUi) {
					uiPairs.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
				}
			}

			values = pairs.toArray (new AlbumStringPair[] {});
			uiValues = uiPairs.toArray (new AlbumStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumStringPair[] getValues (boolean visibleInUi) {
		init ();

		if (visibleInUi) {
			return uiValues;
		} else {
			return values;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumSortType getValue (String symbol) {
		//brute-force method
		for (AlbumSortType ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumSortType.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private final AlbumStringPair value;
	private final boolean isVisibleInUi;
	private final boolean propagateValueToDrillDowns;
	private final boolean comparatorUsesCache;

	private static AlbumStringPair[] values;
	private static AlbumStringPair[] uiValues;

//	private static Logger _log = LogManager.getLogger ();
}
