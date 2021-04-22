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

		if (_nameId <= 0 || _name == null || _name.isEmpty()) {
			throw new IllegalArgumentException ("AlbumImageData.ctor1: invalid values: + " + toString());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//this ctor is used by mybatis
	AlbumImageData (int nameId, String name, int width, int height)
	{
		_nameId = nameId;
		_name = name;
		_orientation = AlbumOrientation.getOrientation (width, height);

		if (_nameId <= 0 || _name == null || _name.isEmpty()) {
			throw new IllegalArgumentException ("AlbumImageData.ctor2: invalid values: + " + toString());
		}
	}

	///////////////////////////////////////////////////////////////////////////
//	AlbumImageData (AlbumImageData imageData)
//	{
//		_nameId = imageData.getNameId ();
//		_name = imageData.getName ();
//		_orientation = imageData.getOrientation ();
//	}

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
	//calculated on demand and cached
	public synchronized String getSubFolder () {
		if (_subFolder == null) {
			_subFolder = AlbumImageDao.getInstance ().getSubFolderFromImageName(getName ());
		}

		return _subFolder;
	}

	@Override
	public int compare (AlbumImageData data1, AlbumImageData data2)
	{
		return data1.getName ().compareToIgnoreCase (data2.getName ());
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

	private String _subFolder;
}
