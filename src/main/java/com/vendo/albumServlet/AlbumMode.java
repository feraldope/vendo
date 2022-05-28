//AlbumMode.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumMode
{
	DoDir ("Dir"),
	DoDup ("Dups"),
	DoSampler ("Sampler"),
	DoUpdateXml ("UpdateXml");

	///////////////////////////////////////////////////////////////////////////
	AlbumMode (String name)
	{
		_value = new AlbumStringPair (name, "do" + name);
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

			for (AlbumMode ff : values ()) {
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
	public static AlbumMode getValue (String symbol)
	{
		//brute-force method
		for (AlbumMode ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumMode.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private AlbumStringPair _value;

	private static AlbumStringPair[] _values;

//	private static Logger _log = LogManager.getLogger ();
}
