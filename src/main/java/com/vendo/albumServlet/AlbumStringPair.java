//AlbumStringPair.java - class to hold a pair of strings to represent a name and a symbol

package com.vendo.albumServlet;

//import java.util.*;

//import org.apache.logging.log4j.*;


public class AlbumStringPair
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumStringPair (String name, String symbol)
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

//	private static Logger _log = LogManager.getLogger ();
}
