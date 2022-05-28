//AlbumTagMode.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumTagMode
{
	TagIn ("In"),
	TagOut ("Out"),
	TagOff ("Off");

	///////////////////////////////////////////////////////////////////////////
	AlbumTagMode (String name)
	{
		_value = new AlbumStringPair (name, "tag" + name);
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

			for (AlbumTagMode ff : values ()) {
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
	public static AlbumTagMode getValue (String symbol)
	{
		//brute-force method
		for (AlbumTagMode ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumTagMode.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private AlbumStringPair _value;

	private static AlbumStringPair[] _values;

//	private static Logger _log = LogManager.getLogger ();
}
