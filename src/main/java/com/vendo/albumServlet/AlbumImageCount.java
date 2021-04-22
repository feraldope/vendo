//AlbumImageCount.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


public class AlbumImageCount
{
	///////////////////////////////////////////////////////////////////////////
	//used by mybatis
	public AlbumImageCount (String baseName, Integer count)
	{
		_baseName = baseName;
		_count = count;

		if (_baseName == null || _baseName.isEmpty() || _count < 1) {
			throw new IllegalArgumentException ("AlbumImageCount.ctor1: invalid values: + " + toString());
		}
	}

	///////////////////////////////////////////////////////////////////////////
//	public AlbumImageCount (AlbumImageCount imageCount)
//	{
//		_baseName = imageCount.getBaseName ();
//		_count = imageCount.getCount ();
//
//		if (_baseName == null || _baseName.isEmpty() || _count < 1) {
//			throw new IllegalArgumentException ("AlbumImageCount.ctor2: invalid values: + " + toString());
//		}
//	}

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
