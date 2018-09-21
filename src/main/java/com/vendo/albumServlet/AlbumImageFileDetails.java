//AlbumImageFileDetails.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


public class AlbumImageFileDetails implements Comparable<AlbumImageFileDetails>
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageFileDetails (String nameNoExt, long bytes, long modified)
	{
		_nameNoExt = nameNoExt;
		_bytes = bytes;
		_modified = modified;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getNameNoExt ()
	{
		return _nameNoExt;
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
		return _nameNoExt.compareTo (other.toString ()) == 0 && _bytes == other._bytes && _modified == other._modified;
	}

	///////////////////////////////////////////////////////////////////////////
	//required for VendoUtils#removeAll
	@Override
	public String toString ()
	{
		return _nameNoExt;
	}

	//members
	private final String _nameNoExt;
	private final long _bytes;
	private final long _modified;

//	private static Logger _log = LogManager.getLogger ();
}
