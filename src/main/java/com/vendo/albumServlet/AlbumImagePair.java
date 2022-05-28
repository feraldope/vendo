//AlbumImagePair.java - class to hold one pair (two only) of AlbumImage objects that typically are related (like dups)

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.*;
import java.util.stream.Collectors;

//import org.apache.logging.log4j.*;


public class AlbumImagePair implements Comparable<AlbumImagePair>
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2)
	{
		this (image1, image2, -1, -1, null, null);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2, int averageDiff, int stdDev, String source, Date lastUpdate)
	{
		_images = Arrays.stream(new AlbumImage[] {image1, image2})
						.sorted(_alphanumComparator)  //sort numerically
						.collect(Collectors.toList());
		_averageDiff = averageDiff;
		_stdDev = stdDev;
		_source = source;
		_lastUpdate = lastUpdate;
		_joinedNames = getJoinedNames (getImage1 (), getImage2 (), false);
		_joinedNamesPlusAttrs = getJoinedNames (getImage1 (), getImage2 (), true);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImage getImage1 ()
	{
		return _images.get(0);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImage getImage2 ()
	{
		return _images.get(1);
	}

	///////////////////////////////////////////////////////////////////////////
	public int getAverageDiff ()
	{
		return _averageDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getStdDev ()
	{
		return _stdDev;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getMinDiff ()
	{
		return Math.min (_averageDiff, _stdDev);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSource ()
	{
		return _source;
	}

	///////////////////////////////////////////////////////////////////////////
	public Date getLastUpdate ()
	{
		return _lastUpdate;
	}

	///////////////////////////////////////////////////////////////////////////
	protected String getJoinedNames ()
	{
		return _joinedNames;
	}

	///////////////////////////////////////////////////////////////////////////
	//currently only used by hashCode() ??
	protected String getJoinedNamesPlusAttrs ()
	{
		return _joinedNamesPlusAttrs;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getJoinedNames (AlbumImage image1, AlbumImage image2, boolean includeAttrs)
	{
		return Arrays.stream (new AlbumImage[] { image1, image2 })
					.map (i -> includeAttrs ? i.getNamePlusAttrs () : i.getBaseName (false))
					.sorted (_alphanumComparator) //sort numerically
					.collect (Collectors.joining (","));
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compareTo(AlbumImagePair other) {
		return getJoinedNamesPlusAttrs ().compareTo (other.getJoinedNamesPlusAttrs ());
	}

	///////////////////////////////////////////////////////////////////////////
    @Override
    public boolean equals (Object other)
    {
		if (other == this) {
			return true;
		}

		if (!(other instanceof AlbumImagePair)) {
			return false;
		}

		return getJoinedNamesPlusAttrs ().equals (((AlbumImagePair) other).getJoinedNamesPlusAttrs ());
	}

	///////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode ()
    {
		return getJoinedNamesPlusAttrs ().hashCode ();
    }

	///////////////////////////////////////////////////////////////////////////
	public static Collection<AlbumImage> getImages (Collection<AlbumImagePair> pairs1, AlbumSortType sortType)
	{
		//sort the list of pairs
		int numPairs = pairs1.size ();
		List<AlbumImagePair> pairs2 = new ArrayList<> (numPairs);
		pairs2.addAll (pairs1);

		if (sortType != AlbumSortType.ByNone) {
			pairs2.sort ((pair1, pair2) -> {
				switch (sortType) {
					case ByDate:
						//sort pairs by descending date (i.e., reverse)
						long pair1Latest = Math.max (pair1.getImage1 ().getModified (), pair1.getImage2 ().getModified ());
						long pair2Latest = Math.max (pair2.getImage1 ().getModified (), pair2.getImage2 ().getModified ());
						int diff = Long.compare (pair2Latest, pair1Latest);
						if (diff != 0) {
							return diff;
						}
						//if dates are identical fall through to sort by name

					case ByName:
						//this needs to be case-insensitive to achieve case-insensitive order in the browser
						return pair1.getImage1 ().getName ().compareToIgnoreCase (pair2.getImage1 ().getName ());

					default:
						throw new RuntimeException ("AlbumImagePair.getImages: invalid sortType \"" + sortType + "\"");
				}
			});
		}

		ArrayList<AlbumImage> images = new ArrayList<> (2 * numPairs);

		for (AlbumImagePair pair : pairs2) {
			images.add (pair.getImage1 ());
			images.add (pair.getImage2 ());
		}

		return images;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDetails1String ()
	{
		StringBuffer sb = new StringBuffer ();
		sb.append ("Avg/StdDev: ").append (getAverageDiff ()).append ("/").append (getStdDev ()).append (", ");
		sb.append ("Src: ").append (getSource ()).append (", ");
		sb.append ("Updated: ").append (getLastUpdate () != null ? _dateFormat.format (getLastUpdate ()) : "null");

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDetails2String ()
	{
		StringBuffer sb = new StringBuffer ();
		sb.append (getImage1 ().getName ()).append (", ");
		sb.append (getImage2 ().getName ()).append (", ");
		sb.append (getAverageDiff ()).append (", ");
		sb.append (getStdDev ()).append (", ");
		sb.append (getSource ()).append (", ");
		sb.append (getLastUpdate () != null ? _dateFormat.format (getLastUpdate ()) : "null");

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDetails3String ()
	{
		StringBuffer sb = new StringBuffer ();
		sb.append (getStdDev ()).append (" ");
		sb.append (getAverageDiff ()).append (" ");
		sb.append (getImage1 ().getName ()).append (" ");
		sb.append (getImage2 ().getName ()).append (" ");
		sb.append (getRelativeSizeIndicator ());

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String getRelativeSizeIndicator ()
	{
		int sizeCompare = getImage1 ().compareToByPixels (getImage2 ());
		return sizeCompare < 0 ? "<" : sizeCompare > 0 ? ">" : "=";
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		return getDetails2String ();
	}


	//members
	protected final List<AlbumImage> _images;
	protected final int _averageDiff;
	protected final int _stdDev;
	protected final String _source;
	protected final Date _lastUpdate;
	protected final String _joinedNames;
	protected final String _joinedNamesPlusAttrs;

	protected static final AlphanumComparator _alphanumComparator = new AlphanumComparator ();
	protected static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

//	protected static Logger _log = LogManager.getLogger ();
}
