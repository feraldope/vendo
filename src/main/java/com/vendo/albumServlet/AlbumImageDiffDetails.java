//AlbumImageFileDetails.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


public class AlbumImageDiffDetails implements Comparable<AlbumImageDiffDetails>
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageDiffDetails (int nameId1, int nameId2, int avgDiff, int maxDiff, int count, String source)
	{
		if (nameId1 < nameId2) {
			_nameId1 = nameId1;
			_nameId2 = nameId2;
		} else {
			_nameId1 = nameId2;
			_nameId2 = nameId1;
		}
		_avgDiff = avgDiff;
		_maxDiff = maxDiff;
		_count = count;
		_source = source;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getNameId1 ()
	{
		return _nameId1;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getNameId2 ()
	{
		return _nameId2;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getAvgDiff ()
	{
		return _avgDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getMaxDiff ()
	{
		return _maxDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getCount ()
	{
		return _count;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSource ()
	{
		return _source;
	}

	///////////////////////////////////////////////////////////////////////////
	//note compareTo, equals, and hashCode only operate on _nameId1 and _nameId2
	@Override
	public int hashCode ()
	{
		int hash = _nameId1;
		hash = 31 * hash + _nameId2;
//		hash = 31 * hash + _avgDiff;
		return hash;
	}

	///////////////////////////////////////////////////////////////////////////
	//note compareTo, equals, and hashCode only operate on _nameId1 and _nameId2
	@Override
	public int compareTo (AlbumImageDiffDetails obj)
	{
		int cmp = obj == null ? 1 : _nameId1.compareTo (obj._nameId1);
		return cmp == 0 ? _nameId2.compareTo (obj._nameId2) : cmp;
	}

	///////////////////////////////////////////////////////////////////////////
	//note compareTo, equals, and hashCode only operate on _nameId1 and _nameId2
	@Override
	public boolean equals (Object obj)
	{
		if (!(obj instanceof AlbumImageDiffDetails)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		return _nameId1.equals (((AlbumImageDiffDetails) obj)._nameId1) &&
			   _nameId2.equals (((AlbumImageDiffDetails) obj)._nameId2);
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ();
		sb.append (getNameId1 ()).append (", ");
		sb.append (getNameId2 ()).append (", ");
		sb.append (getAvgDiff ()).append (", ");
		sb.append (getMaxDiff ()).append (", ");
		sb.append (getCount ()).append (", ");
		sb.append (getSource ());

		return sb.toString ();
	}

	//members
	private final Integer _nameId1;
	private final Integer _nameId2;
	private final Integer _avgDiff;
	private final Integer _maxDiff;
	private final Integer _count;
	private final String _source;

//	private static Logger _log = LogManager.getLogger ();
}
