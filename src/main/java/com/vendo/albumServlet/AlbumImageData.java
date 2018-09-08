package com.vendo.albumServlet;

import java.util.Comparator;

public class AlbumImageData implements Comparator<AlbumImageData> /*, Comparable<AlbumImageData>*/
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageData (int nameId, String nameNoExt)
	{
		_nameId = nameId;
		_nameNoExt = nameNoExt;
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumImageData (AlbumImageData imageData)
	{
		_nameId = imageData.getNameId ();
		_nameNoExt = imageData.getNameNoExt ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNameId ()
	{
		return _nameId;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getNameNoExt ()
	{
		return _nameNoExt;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSubFolder ()
	{
		return getNameNoExt ().substring (0, 1).toLowerCase ();
	}

	@Override
	public int compare (AlbumImageData data1, AlbumImageData data2)
	{
		return data1._nameNoExt.compareToIgnoreCase(data2._nameNoExt);
	}

/*
	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compareTo (AlbumImageData obj)
	{
		return (obj == null ? 1 : _nameNoExt.compareToIgnoreCase(obj._nameNoExt));
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode ()
	{
		return 31 * _nameNoExt.hashCode() + _nameId;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals (Object obj)
	{
		if (!(obj instanceof AlbumImageData)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		return _nameNoExt.compareToIgnoreCase (((AlbumImageData) obj)._nameNoExt) == 0;
//			   _nameId.equals (((AlbumImageData) obj)._nameId);
	}
*/

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer (getClass ().getSimpleName ());
		sb.append (": ").append (getNameNoExt ());
		sb.append (", ").append (getNameId ());
//		sb.append (", ").append (getSubFolder ());

		return sb.toString ();
	}

	//members
	private final int _nameId;
	private final String _nameNoExt;
}
