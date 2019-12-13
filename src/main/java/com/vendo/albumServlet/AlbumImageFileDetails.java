//AlbumImageFileDetails.java

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


import java.util.Objects;

public class AlbumImageFileDetails implements Comparable<AlbumImageFileDetails>
{
	public enum CompareType {NotSet, Full, Partial}

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
	public int compareTo (AlbumImageFileDetails other)
	{
		if (_compareType == CompareType.NotSet) {
			throw new RuntimeException ("AlbumImageFileDetails compareType not set");

		} else if (_compareType == CompareType.Full) {
			int compareNames = getName ().compareToIgnoreCase (other.getName ());
			if (compareNames != 0) {
				return compareNames;
			}

			int compareBytes = (int) (getBytes () - other.getBytes ());
			if (compareBytes != 0) {
				return compareBytes;
			}

			return (int) (getModified () - other.getModified ());

		} else { //_compareType == CompareType.Partial
			//compares only name field
			return getName ().compareToIgnoreCase (other.getName ());
		}
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
		return getName ().compareTo (other.getName ()) == 0 &&
				getBytes () == other.getBytes () &&
				getModified () == other.getModified ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode ()
	{
		return Objects.hash (_name, _bytes, _modified);
	}

	///////////////////////////////////////////////////////////////////////////
	public static void setCompareType (CompareType compareType) //hack
	{
		_compareType = compareType;
	}


	//members
	private final String _name;
	private final long _bytes;
	private final long _modified;

	private static CompareType _compareType = CompareType.NotSet;

//	private static Logger _log = LogManager.getLogger ();
}
