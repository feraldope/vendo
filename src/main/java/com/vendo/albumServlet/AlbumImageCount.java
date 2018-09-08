//AlbumImageCount.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


public class AlbumImageCount
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImageCount (String baseName, Integer count)
	{
		_baseName = baseName;
		_count = count;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageCount (AlbumImageCount employee)
	{
		_baseName = employee.getBaseName ();
		_count = employee.getCount ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getBaseName ()
	{
		return _baseName;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getCount ()
	{
		return _count;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer (getClass ().getSimpleName ());
		sb.append (": ").append (getBaseName ());
		sb.append (", ").append (getCount ());

		return sb.toString ();
	}


	//members
	private final String _baseName;
	private final Integer _count;

//	private static Logger _log = LogManager.getLogger ();
}
