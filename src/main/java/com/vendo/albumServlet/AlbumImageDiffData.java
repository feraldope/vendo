package com.vendo.albumServlet;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

public class AlbumImageDiffData
{
	///////////////////////////////////////////////////////////////////////////
	AlbumImageDiffData(String name1, String name2, int averageDiff, Date lastUpdate) {
		_name1 = name1;
		_name2 = name2;
		_averageDiff = averageDiff;
		_lastUpdate = lastUpdate;
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumImageDiffData(AlbumImageDiffData imageData) {
		_name1 = imageData.getName1();
		_name2 = imageData.getName2();
		_averageDiff = imageData.getAverageDiff();
		_lastUpdate = imageData.getLastUpdate();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName1() {
		return _name1;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName2() {
		return _name2;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getAverageDiff() {
		return _averageDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public Date getLastUpdate() {
		return _lastUpdate;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getSimpleName());
		sb.append(": ").append(getName1());
		sb.append(", ").append(getName2());
		sb.append(", ").append(getAverageDiff());
		sb.append(", ").append(_dateFormat.format(getLastUpdate()));

		return sb.toString();
	}

	// members
	private final String _name1;
	private final String _name2;
	private final int _averageDiff;
	private final Date _lastUpdate;

	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss"); // Note SimpleDateFormat is not thread safe
}
