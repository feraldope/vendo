//AlbumImageFileDetails.java

package com.vendo.albumServlet;


import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;
import java.util.Objects;


public class AlbumImageFileDetails implements Comparable<AlbumImageFileDetails>
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageFileDetails (String name, long bytes, long modified)
	{
		_name = name;
		_bytes = bytes;
		_modified = modified;

		if (_name == null || _name.isEmpty() || _bytes <= 0 || _modified <= 0) {
			throw new IllegalArgumentException ("AlbumImageFileDetails.ctor: invalid values: + " + toString());

//			String message = "AlbumImageFileDetails.ctor: invalid values: " + toString();
//			_log.error(message, new IllegalArgumentException (message));
//			throw new IllegalArgumentException (message);
		}
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
		int compareNames = getName ().compareToIgnoreCase (other.getName ());
		if (compareNames != 0) {
			return compareNames;
		}

		int compareBytes = (int) (getBytes () - other.getBytes ());
		if (compareBytes != 0) {
			return compareBytes;
		}

		return (int) (getModified () - other.getModified ());
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
		return Objects.hash (getName (), getBytes (), getModified ());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		return getName () + ", " + getBytes () + ", " + _dateFormat.format (new Date(getModified ()));
	}


	//members
	private final String _name;
	private final long _bytes;
	private final long _modified;

	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

//	private static Logger _log = LogManager.getLogger ();
}
