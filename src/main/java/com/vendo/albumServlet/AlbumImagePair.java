//AlbumImagePair.java - class to hold a pair of images that typically are related (like dups)

package com.vendo.albumServlet;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.*;

//import org.apache.logging.log4j.*;


public class AlbumImagePair
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2)
	{
		this (image1, image2, 0, 0, null, null);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2, int averageDiff, int stdDev, String source)
	{
		this (image1, image2, averageDiff, stdDev, source, null);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2, int averageDiff, int stdDev, String source, Date lastUpdate)
	{
		if (image1.getName ().compareToIgnoreCase (image2.getName ()) < 0) {
			_image1 = image1;
			_image2 = image2;
		} else {
			_image1 = image2;
			_image2 = image1;
		}
		_averageDiff = averageDiff;
		_stdDev = stdDev;
		_source = source;
		_lastUpdate = lastUpdate;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImage getImage1 ()
	{
		return _image1;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImage getImage2 ()
	{
		return _image2;
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
    @Override
    public boolean equals (Object obj)
    {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AlbumImagePair)) {
			return false;
		}

		AlbumImagePair other = (AlbumImagePair) obj;
		return getJoinedNamesPlusAttrs ().equals (other.getJoinedNamesPlusAttrs ());
	}

	///////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode ()
    {
		return getJoinedNamesPlusAttrs ().hashCode ();
    }

//currently only used by hashCode()
	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	protected synchronized String getJoinedNamesPlusAttrs ()
	{
		if (_joinedNamesPlusAttrs == null) {
			_joinedNamesPlusAttrs = getJoinedNamesPlusAttrs (getImage1 (), getImage2 ());
		}

		return _joinedNamesPlusAttrs;
	}

	///////////////////////////////////////////////////////////////////////////
    //returns true if at least one image in each pair has the same base name
    public boolean matchesAtLeastOneImage (AlbumImagePair pair)
    {
		return getImage1 ().equalBase (pair.getImage1 (), false) ||
			   getImage1 ().equalBase (pair.getImage2 (), false) ||
			   getImage2 ().equalBase (pair.getImage1 (), false) ||
			   getImage2 ().equalBase (pair.getImage2 (), false);
	}

	///////////////////////////////////////////////////////////////////////////
    //returns true if either image in the pair has the same base name as any base name in the set
    public boolean matchesAtLeastOneImage (Set<String> baseNames)
    {
		for (String baseName : baseNames) {
			if (getImage1 ().getBaseName (false).equals (baseName) ||
				getImage2 ().getBaseName (false).equals (baseName)) {

				return true;
			}
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getJoinedNamesPlusAttrs (AlbumImage image1, AlbumImage image2)
	{
		String namePlusAttrs1 = image1.getNamePlusAttrs ();
		String namePlusAttrs2 = image2.getNamePlusAttrs ();

		StringBuffer sb = new StringBuffer (48);
		if (namePlusAttrs1.compareToIgnoreCase (namePlusAttrs2) < 0) {
			sb.append (namePlusAttrs1).append (".").append (namePlusAttrs2);
		} else {
			sb.append (namePlusAttrs2).append (".").append (namePlusAttrs1);
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static Collection<AlbumImage> getImages (Collection<AlbumImagePair> pairs1, AlbumSortType sortType)
	{
		//sort the list of pairs
		int numPairs = pairs1.size ();
		List<AlbumImagePair> pairs2 = new ArrayList<AlbumImagePair> (numPairs);
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

		ArrayList<AlbumImage> images = new ArrayList<AlbumImage> (2 * numPairs);

		for (AlbumImagePair pair : pairs2) {
			images.add (pair.getImage1 ());
			images.add (pair.getImage2 ());
		}

		return images;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDetailsString ()
	{
		StringBuffer sb = new StringBuffer ();
		sb.append ("Avg/StdDev: ").append (getAverageDiff ()).append ("/").append (getStdDev ()).append (", ");
		sb.append ("Src: ").append (getSource ()).append (", ");
		sb.append ("Updated: ").append (getLastUpdate () != null ? _dateFormat.format (getLastUpdate ()) : "null");

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getRelativeSizeIndicator ()
	{
		int sizeCompare = getImage1 ().compareToByPixels (getImage2 ());
		return sizeCompare < 0 ? "<" : sizeCompare > 0 ? ">" : "=";
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
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


	//members
	protected final AlbumImage _image1;
	protected final AlbumImage _image2;
	protected final int _averageDiff;
	protected final int _stdDev;
	protected final String _source;
	protected final Date _lastUpdate;
	protected String _joinedNamesPlusAttrs = null;

	protected static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

//	protected static Logger _log = LogManager.getLogger ();
}
