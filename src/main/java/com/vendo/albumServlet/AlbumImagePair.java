//AlbumImagePair.java - class to hold a pair of images that typically are related (like dups)

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

//import org.apache.logging.log4j.*;


public class AlbumImagePair
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2)
	{
		this (image1, image2, 0);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImagePair (AlbumImage image1, AlbumImage image2, int averageDiff)
	{
		if (image1.getName ().compareToIgnoreCase (image2.getName ()) < 0) {
			_image1 = image1;
			_image2 = image2;
		} else {
			_image1 = image2;
			_image2 = image1;
		}
		_averageDiff = averageDiff;
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
	public void setAverageDiff (int averageDiff)
	{
		_averageDiff = averageDiff;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getAverageDiff ()
	{
		return _averageDiff;
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
		return getJoinedNames ().equals (other.getJoinedNames ());
	}

	///////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode ()
    {
		return getJoinedNames ().hashCode ();
    }

//currently only used by hashCode()
	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	private synchronized String getJoinedNames (String... extras)
	{
		if (_joinedNames == null) {
			_joinedNames = getJoinedNames (getImage1 (), getImage2 (), extras);
		}

		return _joinedNames;
	}

	///////////////////////////////////////////////////////////////////////////
    //returns true if at least one image in each pair has the same base name
    public boolean matchesAtLeastOneImage (AlbumImagePair pair)
    {
		return getImage1 ().equalBase (pair.getImage1 (), /*collapseGroups*/ false) ||
			   getImage1 ().equalBase (pair.getImage2 (), /*collapseGroups*/ false) ||
			   getImage2 ().equalBase (pair.getImage1 (), /*collapseGroups*/ false) ||
			   getImage2 ().equalBase (pair.getImage2 (), /*collapseGroups*/ false);
	}

	///////////////////////////////////////////////////////////////////////////
    //returns true if either image in the pair has the same base name as any base name in the set
    public boolean matchesAtLeastOneImage (Set<String> baseNames)
    {
		for (String baseName : baseNames) {
			if (getImage1 ().getBaseName (/*collapseGroups*/ false).equals (baseName) ||
				getImage2 ().getBaseName (/*collapseGroups*/ false).equals (baseName)) {

				return true;
			}
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getJoinedNames (AlbumImage image1, AlbumImage image2, String... extras)
	{
		String namePlus1 = image1.getNamePlus ();
		String namePlus2 = image2.getNamePlus ();

		StringBuffer sb = new StringBuffer (100);
		if (namePlus1.compareToIgnoreCase (namePlus2) < 0) {
			sb.append (namePlus1).append (".").append (namePlus2);
		} else {
			sb.append (namePlus2).append (".").append (namePlus1);
		}

		for (String extra : extras) {
			sb.append (".").append (extra);
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
			Collections.sort (pairs2, new Comparator<AlbumImagePair> () {
				@Override
				public int compare (AlbumImagePair pair1, AlbumImagePair pair2) {
					if (sortType == AlbumSortType.ByName) {
						//this needs to be case-insensitive to achieve case-insensitive order in the browser
						return pair1.getImage1 ().getName ().compareToIgnoreCase (pair2.getImage1 ().getName ());

					} else if (sortType == AlbumSortType.ByDate) {
						//sort pairs by descending date (i.e., reverse)
						long pair1Latest = Math.max (pair1.getImage1 ().getModified (), pair1.getImage2 ().getModified ());
						long pair2Latest = Math.max (pair2.getImage1 ().getModified (), pair2.getImage2 ().getModified ());

						return (pair1Latest < pair2Latest ? 1 : pair1Latest > pair2Latest ? -1 : 0);

					} else {
						throw new RuntimeException ("AlbumImagePair.getImages: invalid sortType \"" + sortType + "\"");
					}
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


	//members
	private final AlbumImage _image1;
	private final AlbumImage _image2;
	private String _joinedNames = null;
	private int _averageDiff = Integer.MAX_VALUE;

//	private static Logger _log = LogManager.getLogger ();
}
