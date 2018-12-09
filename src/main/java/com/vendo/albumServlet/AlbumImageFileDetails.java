//AlbumImageFileDetails.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


public class AlbumImageFileDetails implements Comparable<AlbumImageFileDetails>
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageFileDetails (String name, long bytes, long modified)
	{
		_name = name;
		_bytes = bytes;
		_modified = modified;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getBytes ()
	{
		return _bytes;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getModified ()
	{
		return _modified;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compareTo (AlbumImageFileDetails obj)
	{
		return toString ().compareToIgnoreCase (obj.toString ());
	}

	//TODO: implement hashCode

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals (Object obj)
	{
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AlbumImageFileDetails)) {
			return false;
		}

		AlbumImageFileDetails other = (AlbumImageFileDetails) obj;
		return _name.compareTo (other.toString ()) == 0 && _bytes == other._bytes && _modified == other._modified;
	}

	///////////////////////////////////////////////////////////////////////////
	//required for VendoUtils#removeAll
	@Override
	public String toString ()
	{
		return _name;
	}

	//members
	private final String _name;
	private final long _bytes;
	private final long _modified;

//	private static Logger _log = LogManager.getLogger ();
}
