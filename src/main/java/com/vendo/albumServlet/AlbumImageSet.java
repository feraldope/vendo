//AlbumImagePair.java - class to hold a pair of images that typically are related (like dups)

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//import org.apache.logging.log4j.*;


public class AlbumImageSet
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImageSet(String joinedNames)
	{
		this (joinedNames, null);
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageSet(String joinedNames, AlbumImagePair pair)
	{
		_joinedNames = joinedNames;

		if (pair != null) {
			addPair(pair);
		}

		_baseNames = Arrays.stream(_joinedNames.split(","))
							.sorted(_alphanumComparator)
							.collect(Collectors.toList());

		_numberOfImagesInAlbum1 = AlbumImages.getNumMatchingImages (_baseNames.get(0), 0);
		_numberOfImagesInAlbum2 = AlbumImages.getNumMatchingImages (_baseNames.get(1), 0);
	}

	///////////////////////////////////////////////////////////////////////////
	public void addPair(AlbumImagePair pair)
	{
		_pairs.add(pair);
		incrementNumberOfDuplicateMatches (pair.getImage1 ().compareToByPixels (pair.getImage2 ())); //pass pixelDiff
	}

	///////////////////////////////////////////////////////////////////////////
	public String getJoinedNames()
	{
		return _joinedNames;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getBaseName(int index)
	{
		return _baseNames.get(index);
	}

	///////////////////////////////////////////////////////////////////////////
	private int getNumberOfImagesInAlbum1 ()
	{
		return _numberOfImagesInAlbum1;
	}

	///////////////////////////////////////////////////////////////////////////
	private int getNumberOfImagesInAlbum2 ()
	{
		return _numberOfImagesInAlbum2;
	}

	///////////////////////////////////////////////////////////////////////////
	private int getNumberOfDuplicateMatches ()
	{
		return _numberOfDuplicateMatches;
	}

	///////////////////////////////////////////////////////////////////////////
	private void incrementNumberOfDuplicateMatches (int pixelDiff)
	{
		++_numberOfDuplicateMatches;

		if (pixelDiff == 0) {
			++_numberOfImagesOfEqualSize;
		} else if (pixelDiff > 0) {
			++_numberOfImagesWhereFirstIsLarger;
		} else { // pixelDiff < 0
			++_numberOfImagesWhereSecondIsLarger;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean pairRepresentsDuplicates ()
	{
		return getNumberOfDuplicateMatches () > 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean secondAlbumRepresentsExactDuplicate ()
	{
		return (getNumberOfImagesInAlbum2 () == getNumberOfDuplicateMatches ())
				&& (_numberOfImagesOfEqualSize + _numberOfImagesWhereFirstIsLarger == getNumberOfDuplicateMatches());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean secondAlbumRepresentsNearDuplicate ()
	{
		final int slop = 2;
		return ((Math.abs (getNumberOfImagesInAlbum2 () - getNumberOfDuplicateMatches ()) <= slop) &&
				(Math.abs (getNumberOfDuplicateMatches() - _numberOfImagesOfEqualSize - _numberOfImagesWhereFirstIsLarger) <= slop));

	}

	///////////////////////////////////////////////////////////////////////////
	//returns true if at least one image in either pair has the same base name as an pair in this set
	public boolean matchesAtLeastOneImage (AlbumImagePair pair2)
	{
		//TODO: improve this brute-force method
		for (AlbumImagePair pair1 : _pairs) {
			if (pair1.getImage1().equalBase(pair2.getImage1(), false) ||
				pair1.getImage1().equalBase(pair2.getImage2(), false) ||
				pair1.getImage2().equalBase(pair2.getImage1(), false) ||
				pair1.getImage2().equalBase(pair2.getImage2(), false)) {
				return true;
			}
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDetailString() {
		AlphanumComparator alphanumComparator = new AlphanumComparator();

		if (_pairs.isEmpty()) {
			return "<no pairs>";
		}

		String pixelDiffString;
		if (_numberOfImagesOfEqualSize == getNumberOfDuplicateMatches ()) {
			pixelDiffString = "=";

		} else if (_numberOfImagesWhereFirstIsLarger == getNumberOfDuplicateMatches ()) {
			pixelDiffString = ">";
		} else if ((_numberOfImagesWhereFirstIsLarger + _numberOfImagesOfEqualSize) == getNumberOfDuplicateMatches ()) {
			pixelDiffString = ">=";

		} else if (_numberOfImagesWhereSecondIsLarger == getNumberOfDuplicateMatches ()) {
			pixelDiffString = "<";

		} else if ((_numberOfImagesWhereSecondIsLarger + _numberOfImagesOfEqualSize) == getNumberOfDuplicateMatches ()) {
			pixelDiffString = "<=";

		} else {
			pixelDiffString = "?";
		}

		StringBuffer sb = new StringBuffer ();
		sb.append (getBaseName(0)).append (", ");
		sb.append (getBaseName(1)).append (", ");
		sb.append (getNumberOfImagesInAlbum1 ()).append (", ");
		sb.append (getNumberOfImagesInAlbum2 ()).append (", ");
		sb.append (_pairs.size()).append (", ");
		sb.append (pixelDiffString);
//		sb.append (getAverageDiff ()).append (", ");
//		sb.append (getStdDev ()).append (", ");
//		sb.append (getSource ()).append (", ");
//		sb.append (getLastUpdate () != null ? _dateFormat.format (getLastUpdate ()) : "null");
//
		return sb.toString ();
	}

	//members
	protected final String _joinedNames;
	protected final List<String> _baseNames;
	protected final int _numberOfImagesInAlbum1;
	protected final int _numberOfImagesInAlbum2;

	protected int _numberOfDuplicateMatches;
	protected int _numberOfImagesOfEqualSize;
	protected int _numberOfImagesWhereFirstIsLarger;
	protected int _numberOfImagesWhereSecondIsLarger;

	protected final Set<AlbumImagePair> _pairs = new HashSet<>();

	private static final AlphanumComparator _alphanumComparator = new AlphanumComparator();

//	protected static Logger _log = LogManager.getLogger ();
}
