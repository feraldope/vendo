//FundYtdReturn.java

package com.vendo.fundViewer;

public class FundYtdReturn
{
	private Boolean _isSet;
	private Double _ytdReturn;

	///////////////////////////////////////////////////////////////////////////
	public FundYtdReturn ()
	{
		_isSet = false;
		_ytdReturn = 0.;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundYtdReturn (String string)
	{
		try {
			_ytdReturn = Double.parseDouble (string);
			_isSet = true;

		} catch (NumberFormatException exception) {
			_ytdReturn = 0.;
			_isSet = false;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString ()
	{
		final String dashes = new String ("---");

		if (_isSet)
			return _ytdReturn + "%";
		else
			return dashes;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isSet ()
	{
		return _isSet;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getDoubleValue ()
	{
		return _ytdReturn;
	}

	///////////////////////////////////////////////////////////////////////////
	public int compareTo (FundYtdReturn that)
	{
		if (this.isSet () && that.isSet ())
			return this.getDoubleValue ().compareTo (that.getDoubleValue ());
		else if (!this.isSet ())
			return -1;
		else
			return 1;
	}
}
