//AlbumMode.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumMode {
	DoDir ("Dir", "Dir"),
	DoDup ("Dups", "Dup"),
	DoSampler ("Sampler", "Samp"),
	DoUpdateXml ("UpdateXml", "Ux");

	///////////////////////////////////////////////////////////////////////////
	AlbumMode (String name, String shortName) {
		value = new AlbumStringPair (name, "do" + name);
		this.shortName = shortName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName () {
		return value.getName ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getShortName () {
		return shortName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol () {
		return value.getSymbol ();
	}

	///////////////////////////////////////////////////////////////////////////
	private static void init () {
		if (values == null) {
			List<AlbumStringPair> arrayList = new ArrayList<> ();

			for (AlbumMode ff : values ()) {
				arrayList.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
			}

			values = arrayList.toArray (new AlbumStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumStringPair[] getValues () {
		init ();

		return values;
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumMode getValue (String symbol) {
		//brute-force method
		for (AlbumMode ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumMode.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private final AlbumStringPair value;
	private final String shortName;

	private static AlbumStringPair[] values;

//	private static Logger _log = LogManager.getLogger ();
}
