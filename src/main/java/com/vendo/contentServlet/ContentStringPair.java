//ContentStringPair.java - class to hold a pair of strings to represent a name and a symbol

package com.vendo.contentServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ContentStringPair
{
	///////////////////////////////////////////////////////////////////////////
	public ContentStringPair (String name, String symbol)
	{
		_name = name;
		_symbol = symbol;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol ()
	{
		return _symbol;
	}


	//members
	private String _name;
	private String _symbol;

	private static Logger _log = LogManager.getLogger (ContentStringPair.class);
}
