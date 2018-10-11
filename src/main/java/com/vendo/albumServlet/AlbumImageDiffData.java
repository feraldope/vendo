package com.vendo.albumServlet;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

public class AlbumImageDiffData // implements Comparator<AlbumImageDiffData> /*, Comparable<AlbumImageData>*/
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageDiffData(String nameNoExt1, String nameNoExt2, int averageDiff, Date lastUpdate) {
		_nameNoExt1 = nameNoExt1;
		_nameNoExt2 = nameNoExt2;
		_averageDiff = averageDiff;
		_lastUpdate = lastUpdate;
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumImageDiffData(AlbumImageDiffData imageData) {
		_nameNoExt1 = imageData.getNameNoExt1();
		_nameNoExt2 = imageData.getNameNoExt2();
		_averageDiff = imageData.getAverageDiff();
		_lastUpdate = imageData.getLastUpdate();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getNameNoExt1() {
		return _nameNoExt1;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getNameNoExt2() {
		return _nameNoExt2;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getAverageDiff() {
		return _averageDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public Date getLastUpdate() {
		return _lastUpdate;
	}

	/*
	 * @Override public int compare (AlbumImageDiffData data1, AlbumImageDiffData data2) { return 1;//data1._nameNoExt.compareToIgnoreCase(data2._nameNoExt); }
	 */
	/*
	 * ///////////////////////////////////////////////////////////////////////////
	 *
	 * @Override public int compareTo (AlbumImageData obj) { return (obj == null ? 1 : _nameNoExt.compareToIgnoreCase(obj._nameNoExt)); }
	 *
	 * ///////////////////////////////////////////////////////////////////////////
	 *
	 * @Override public int hashCode () { return 31 * _nameNoExt.hashCode() + _nameId; }
	 *
	 * ///////////////////////////////////////////////////////////////////////////
	 *
	 * @Override public boolean equals (Object obj) { if (!(obj instanceof AlbumImageData)) { return false; } if (this == obj) { return true; } return
	 * _nameNoExt.compareToIgnoreCase (((AlbumImageData) obj)._nameNoExt) == 0; // _nameId.equals (((AlbumImageData) obj)._nameId); }
	 */

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getSimpleName());
		sb.append(": ").append(getNameNoExt1());
		sb.append(", ").append(getNameNoExt2());
		sb.append(", ").append(getAverageDiff());
		sb.append(", ").append(_dateFormat.format(getLastUpdate()));

		return sb.toString();
	}

	// members
	private final String _nameNoExt1;
	private final String _nameNoExt2;
	private final int _averageDiff;
	private final Date _lastUpdate;

	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss"); // Note SimpleDateFormat is not thread safe
}
