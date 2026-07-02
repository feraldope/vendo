//AlbumSkipType.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumSkipType {

	//Enum (name, propagateValueToDrillDowns)
	SkipNone ("None", false),
	SkipFirst ("First", false),
	SkipLast ("Last", false),
	SkipFirstAndLast ("First And Last", false);

	///////////////////////////////////////////////////////////////////////////
	AlbumSkipType (String name, boolean propagateValueToDrillDowns) {
		value = new AlbumStringPair (name, "skip" + name);
		this.propagateValueToDrillDowns = propagateValueToDrillDowns;
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
	public boolean isPropagateValueToDrillDowns () {
		return propagateValueToDrillDowns;
	}

	///////////////////////////////////////////////////////////////////////////
	private static void init () {
		if (values == null) {
			List<AlbumStringPair> pairs = new ArrayList<> ();

			for (AlbumSkipType ff : values ()) {
				pairs.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
			}

			values = pairs.toArray (new AlbumStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumStringPair[] getValues (/*boolean visibleInUi*/) {
		init ();

		return values;
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumSkipType getValue (String symbol) {
		//brute-force method
		for (AlbumSkipType ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumSkipType.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private final AlbumStringPair value;
	private final boolean propagateValueToDrillDowns;

	private static AlbumStringPair[] values;

//	private static Logger _log = LogManager.getLogger ();
}
