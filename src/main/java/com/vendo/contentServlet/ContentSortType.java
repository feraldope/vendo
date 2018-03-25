//ContentSortType.java

package com.vendo.contentServlet;

import java.util.*;

import org.apache.logging.log4j.*;


public enum ContentSortType
{
	ByName ("Name"),
	ByDate ("Date"),
	BySize ("Size"),
//	BySizeBytes ("Size (bytes)"),
//	BySizePixels ("Size (pixels)"),
	ByCount ("Count"),
	ByHash ("Hash"),
	ByRandom ("Random");

	///////////////////////////////////////////////////////////////////////////
	ContentSortType (String name)
	{
		_value = new ContentStringPair (name, "by" + name);
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
			List<ContentStringPair> arrayList = new ArrayList<ContentStringPair> ();

			for (ContentSortType ff : values ())
				arrayList.add (new ContentStringPair (ff.getName (), ff.getSymbol ()));

			_values = arrayList.toArray (new ContentStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static ContentStringPair[] getValues ()
	{
		init ();

		return _values;
	}

	///////////////////////////////////////////////////////////////////////////
	public static ContentSortType getValue (String symbol)
	{
		//brute-force method
		for (ContentSortType ff : values ()) {
			if (ff.getSymbol ().equals (symbol))
				return ff;
		}

		throw new RuntimeException ("ContentSortType.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private ContentStringPair _value;

	private static ContentStringPair[] _values;

	private static Logger _log = LogManager.getLogger (ContentSortType.class);
}
