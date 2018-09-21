//AlbumImageFileDetails.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


public class AlbumImageDiffDetails implements Comparable<AlbumImageDiffDetails>
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageDiffDetails (int nameId1, int nameId2, int avgDiff, int maxDiff, String source)
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
	public String getSource ()
	{
		return _source;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compareTo (AlbumImageDiffDetails obj)
	{
		int cmp = obj == null ? 1 : _nameId1.compareTo (obj._nameId1);
		return cmp == 0 ? _nameId2.compareTo (obj._nameId2) : cmp;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode ()
	{
		return 31 * _nameId1 + _nameId2;
	}

	///////////////////////////////////////////////////////////////////////////
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
		StringBuffer sb = new StringBuffer (getClass ().getSimpleName ());
		sb.append (": ").append (getNameId1 ());
		sb.append (", ").append (getNameId2 ());
		sb.append (", ").append (getAvgDiff ());
		sb.append (", ").append (getMaxDiff ());
		sb.append (", ").append (getSource ());

		return sb.toString ();
	}

	//members
	private final Integer _nameId1;
	private final Integer _nameId2;
	private final Integer _avgDiff;
	private final Integer _maxDiff;
	private final String _source;

//	private static Logger _log = LogManager.getLogger ();
}
