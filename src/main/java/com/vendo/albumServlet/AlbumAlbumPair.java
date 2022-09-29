//AlbumAlbumPair.java - class to hold one imagePair (two albums) of albums (collection of AlbumImage objects) that typically are related (like dups)

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

//import org.apache.logging.log4j.*;


public class AlbumAlbumPair
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumAlbumPair(String joinedNames, AlbumImagePair imagePair)
	{
		_joinedNames = joinedNames;

		if (imagePair != null) {
			addImagePair(imagePair);
		}

		_baseNames = Arrays.stream(_joinedNames.split(","))
							.sorted(_alphanumComparator)
							.collect(Collectors.toList());

		_numberOfImagesInAlbum[0] = AlbumImageDao.getInstance ().getNumMatchingImages (_baseNames.get(0), 0);
		_numberOfImagesInAlbum[1] = AlbumImageDao.getInstance ().getNumMatchingImages (_baseNames.get(1), 0);
	}

	///////////////////////////////////////////////////////////////////////////
	public void addImagePair(AlbumImagePair imagePair)
	{
		_imagePairs.add(imagePair);
		incrementNumberOfDuplicateMatches (imagePair.getImage1 ().compareToByPixels (imagePair.getImage2 ())); //pass pixelDiff
	}

	///////////////////////////////////////////////////////////////////////////
	public Set<AlbumImagePair> getImagePairs ()
	{
		return _imagePairs;
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
	public List<String> getBaseNames()
	{
		return _baseNames;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumberOfImagesInAlbum (int index)
	{
		return _numberOfImagesInAlbum[index];
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
		return (getNumberOfImagesInAlbum (1) == getNumberOfDuplicateMatches ())
				&& (_numberOfImagesOfEqualSize + _numberOfImagesWhereFirstIsLarger == getNumberOfDuplicateMatches());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean secondAlbumRepresentsNearDuplicate ()
	{
		final int slop = 2;
		return ((Math.abs (getNumberOfImagesInAlbum (1) - getNumberOfDuplicateMatches ()) <= slop) &&
				(Math.abs (getNumberOfDuplicateMatches() - _numberOfImagesOfEqualSize - _numberOfImagesWhereFirstIsLarger) <= slop));
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDetailString(boolean enableHighlight)
	{
		if (_imagePairs.isEmpty()) {
			return "<no image pairs>";
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
			enableHighlight = false;

		} else if ((_numberOfImagesWhereSecondIsLarger + _numberOfImagesOfEqualSize) == getNumberOfDuplicateMatches ()) {
			pixelDiffString = "<=";
			enableHighlight = false;

		} else {
			pixelDiffString = "?";
		}

		String filters = getBaseName(0) + "," + getBaseName(1);

		AlbumFormInfo form = AlbumFormInfo.getInstance();
		String href = AlbumImages.getInstance().generateImageLink(filters, filters, AlbumMode.DoSampler,  form.getColumns(), form.getSinceDays(), false, true);
		StringBuilder html = new StringBuilder();
//TODO - move to helper class/method
		html.append ("<A HREF=\"")
				.append (href)
				.append ("\" ")
				.append ("title=\"").append (filters)
				.append ("\" target=_blank>")
				.append (filters)
				.append ("</A>");

		if (_imagePairs.size() < 2) {
			enableHighlight = false;
		}

		StringBuffer sb = new StringBuffer ();
//		sb.append (getBaseName(0)).append (", ");
//		sb.append (getBaseName(1)).append (", ");
		sb.append (html).append (", ");
		sb.append (enableHighlight ? "<B>" : "");
		sb.append (getNumberOfImagesInAlbum (0)).append (", ");
		sb.append (getNumberOfImagesInAlbum (1)).append (", ");
		sb.append (_imagePairs.size()).append (", ");
		sb.append (pixelDiffString);
		sb.append (enableHighlight ? "</B>" : "");
//		sb.append (getAverageDiff ()).append (", ");
//		sb.append (getStdDev ()).append (", ");
//		sb.append (getSource ()).append (", ");
//		sb.append (getLastUpdate () != null ? _dateFormat.format (getLastUpdate ()) : "null");
//
		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return _baseNames.stream().sorted(_alphanumComparator).collect(Collectors.joining(", "));
	}

	//members
	protected final String _joinedNames;
	protected final List<String> _baseNames;
	protected final Integer[] _numberOfImagesInAlbum = new Integer[2];

	protected int _numberOfDuplicateMatches;
	protected int _numberOfImagesOfEqualSize;
	protected int _numberOfImagesWhereFirstIsLarger;
	protected int _numberOfImagesWhereSecondIsLarger;

	protected final Set<AlbumImagePair> _imagePairs = new HashSet<>();

	private static final AlphanumComparator _alphanumComparator = new AlphanumComparator();

//	protected static Logger _log = LogManager.getLogger ();
}
