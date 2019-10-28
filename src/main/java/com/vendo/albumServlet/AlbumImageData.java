package com.vendo.albumServlet;

import java.util.Comparator;

public class AlbumImageData implements Comparator<AlbumImageData>
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageData (int nameId, String name, AlbumOrientation orientation)
	{
		_nameId = nameId;
		_name = name;
		_orientation = orientation;
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumImageData (int nameId, String name, int width, int height)
	{
		_nameId = nameId;
		_name = name;
		_orientation = AlbumOrientation.getOrientation (width, height);
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumImageData (AlbumImageData imageData)
	{
		_nameId = imageData.getNameId ();
		_name = imageData.getName ();
		_orientation = imageData.getOrientation ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNameId ()
	{
		return _nameId;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumOrientation getOrientation ()
	{
		return _orientation;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSubFolder ()
	{
		return AlbumImage.getSubFolderFromName (getName ());
	}

	@Override
	public int compare (AlbumImageData data1, AlbumImageData data2)
	{
		return data1._name.compareToIgnoreCase(data2.getName ());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer (getClass ().getSimpleName ());
		sb.append (": ").append (getName ());
		sb.append (", ").append (getNameId ());
		sb.append (", ").append (getOrientation ());

		return sb.toString ();
	}

	//members
	private final int _nameId;
	private final String _name;
	private final AlbumOrientation _orientation;
}
