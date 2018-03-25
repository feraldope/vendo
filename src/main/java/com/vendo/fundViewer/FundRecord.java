//FundRecord.java

package com.vendo.fundViewer;

public class FundRecord
{
	///////////////////////////////////////////////////////////////////////////
	public FundRecord ()
	{
		final String dashes = new String ("---");

		_fundName = dashes;
		_ticker = dashes;
		_category = dashes;
		_starRating = dashes;
		_ytdReturn = new FundYtdReturn ();
	}

/*
<tr valign="top" bgcolor="#effefe">
<tr valign="top" bgcolor="#dcffff">
<td align="left"><font face="Trebuchet MS, Arial, Helvetica" size="-1"><a href="http://quicktake.morningstar.com/Fund/MorningstarAnalysis.asp?Country=USA&amp;Symbol=FUSEX">Fidelity Spartan U.S. Equity Index</a></font></td><!---->
<td align="left"><font face="Trebuchet MS, Arial, Helvetica" size="-1">Large Blend</font></td>
<td valign="center" align="left"><img src="http://im.morningstar.com/im/3Stars.gif"></td>
<td align="left"><font face="Trebuchet MS, Arial, Helvetica" size="-1">2.23</font></td>
<td align="right"><font face="Trebuchet MS, Arial, Helvetica" size="-1">05/19/2006                    </font>
</td></tr>
*/

	///////////////////////////////////////////////////////////////////////////
	public boolean extractFundName (String line)
	{

		final String symbolKeyword = "Symbol=";

		int posHref = line.indexOf ("href=");
		int posSymbolStart = line.indexOf (symbolKeyword, posHref);
		if (posSymbolStart < 0)
			return false;
		posSymbolStart += symbolKeyword.length ();

		int posSymbolEnd = line.indexOf ("\"", posSymbolStart);
		if (posSymbolEnd < 0)
			return false;

		_ticker = line.substring (posSymbolStart, posSymbolEnd);

		int posNameStart = posSymbolEnd;
		if (posNameStart < 0)
			return false;
		posNameStart += 2;

		int posNameEnd = line.indexOf ("<", posNameStart);
		if (posNameEnd < 0)
			return false;

		_fundName = line.substring (posNameStart, posNameEnd).trim ();
		_fundName = _fundName.replace ("&amp;", "&");

//System.out.println ("ticker=" + ticker + ", fundName=" + fundName);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean extractCategory (String line)
	{
		int posMarker1 = line.indexOf ("<td align=");
		if (posMarker1 < 0)
			return false;
		int posMarker2 = line.indexOf ("font face=", posMarker1);
		if (posMarker2 < 0)
			return false;

		int posCategoryStart = line.indexOf (">", posMarker2);
		if (posCategoryStart < 0)
			return false;
		posCategoryStart++;

		int posCategoryEnd = line.indexOf ("<", posCategoryStart);
		if (posCategoryEnd < 0)
			return false;

		_category = line.substring (posCategoryStart, posCategoryEnd).trim ();

//System.out.println ("category=" + category);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean extractStarRating (String line)
	{
		final String marker = "im.morningstar.com/im/";

		int posMarkerStart = line.indexOf (marker);
		if (posMarkerStart < 0)
			return false;
		posMarkerStart += marker.length ();

		int posMarkerEnd = line.indexOf (".gif", posMarkerStart);
		if (posMarkerEnd < 0)
			return false;

		_starRating = line.substring (posMarkerStart, posMarkerEnd).trim ();

//System.out.println ("starRating=" + starRating);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean extractYtdReturn (String line)
	{
		int posMarker1 = line.indexOf ("<td align=");
		if (posMarker1 < 0)
			return false;
		int posMarker2 = line.indexOf ("font face=", posMarker1);
		if (posMarker2 < 0)
			return false;

		int posYtdReturnStart = line.indexOf (">", posMarker2);
		if (posYtdReturnStart < 0)
			return false;
		posYtdReturnStart++;

		int posYtdReturnEnd = line.indexOf ("<", posYtdReturnStart);
		if (posYtdReturnEnd < 0)
			return false;

		_ytdReturn = new FundYtdReturn (line.substring (posYtdReturnStart, posYtdReturnEnd).trim ());

//System.out.println ("ytdReturn=" + ytdReturn);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFundName ()
	{
		return _fundName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFundFamily ()
	{
		String[] specialCases = {
			"American Century",
			"American Beacon",
			"American Funds",
			"Cohen & Steers",
			"Dodge & Cox",
			"Eaton Vance",
			"Goldman Sachs",
			"Hotchkis and Wiley",
			"Legg Mason",
			"Lord Abbett",
			"Morgan Stanley",
			"Neuberger Berman",
			"Old Mutual",
			"T. Rowe Price",
			"Third Avenue",
			"Value Line",
			"Van Kampen",
			"Waddell & Reed",
			"Wells Fargo",
			"William Blair"
		};

		for (String specialCase : specialCases)
			if (_fundName.startsWith (specialCase))
				return specialCase;

		//family is first 'word' in fund name
		int index = _fundName.indexOf (_space);
		if (index > 0)
			return _fundName.substring (0, index);
		else
			return _fundName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getTicker ()
	{
		return _ticker;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getCategory ()
	{
		return _category;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getStarRating ()
	{
		return _starRating;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundYtdReturn getYtdReturn ()
	{
		return _ytdReturn;
	}

	//fields read from file
	private String _fundName;
	private String _ticker;
	private String _category;
	private String _starRating;
	private FundYtdReturn _ytdReturn;

	private static final String _space = " ";
}
