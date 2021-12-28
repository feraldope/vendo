//AlbumAlbumPairs.java - class to hold (possibly) multiple AlbumAlbumPair objects that typically are related (like dups)

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;

import java.util.*;

//import org.apache.logging.log4j.*;


public class AlbumAlbumPairs
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumAlbumPairs()
	{
		int bh = 1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void addAlbumPairs(Collection<AlbumAlbumPair> albumPairs)
	{
		albumPairs.forEach(this::addAlbumPair);
	}

	///////////////////////////////////////////////////////////////////////////
	public void addAlbumPair(AlbumAlbumPair albumPair)
	{
		Set<AlbumAlbumPair> foundSet = null;
		for (Set<AlbumAlbumPair> albumSet1 : _albumSets) {
			if (matchesAtLeastOneImage(albumSet1, albumPair)) {
				foundSet = albumSet1;
				albumSet1.add(albumPair);
				break;
			}
		}
		if (foundSet == null) {
			HashSet<AlbumAlbumPair> albumSet2 = new HashSet<>();
			albumSet2.add(albumPair);
			_albumSets.add(albumSet2);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//returns true if at least one image in either albumPair has the same base name as an albumPair in this set
	private boolean matchesAtLeastOneImage (Set<AlbumAlbumPair> albumSet1, AlbumAlbumPair albumPair2)
	{
//TODO: improve this brute-force method
		Set<String> baseNames1 = new HashSet<>();
		for (AlbumAlbumPair albumpair1 : albumSet1) {
			baseNames1.add(albumpair1.getBaseName(0));
			baseNames1.add(albumpair1.getBaseName(1));
		}
		Set<String> baseNames2 = new HashSet<>();
		baseNames2.add(albumPair2.getBaseName(0));
		baseNames2.add(albumPair2.getBaseName(1));

		boolean found = baseNames1.stream().anyMatch(new HashSet<>(baseNames2)::contains);
		return found;
	}

/* old version copied (more recently copied from AlbumAlbumPair)
	///////////////////////////////////////////////////////////////////////////
	//returns true if at least one image in either imagePair has the same base name as an imagePair in this set
	private boolean matchesAtLeastOneImage (AlbumAlbumPair set2)
	{
		//TODO: improve this brute-force method
		Set<AlbumImage> images1 = new HashSet<>();
		for (AlbumImagePair imagePair : _imagePairs) {
			images1.add(imagePair.getImage1());
			images1.add(imagePair.getImage2());
		}
		Set<AlbumImage> images2 = new HashSet<>();
		for (AlbumImagePair imagePair : set2.getPairs()) {
			images2.add(imagePair.getImage1());
			images2.add(imagePair.getImage2());
		}
		for (AlbumImage image1 : images1) {
			for (AlbumImage image2 : images2) {
				if (image1.getBaseName(false).equalsIgnoreCase(image2.getBaseName(false))) {
					return true;
				}
			}

		}

		return false;
*/
/* old way
		//TODO: improve this brute-force method
		for (AlbumImagePair pair1 : _imagePairs) {
			if (pair1.getImage1().equalBase(pair2.getImage1(), false) ||
				pair1.getImage1().equalBase(pair2.getImage2(), false) ||
				pair1.getImage2().equalBase(pair2.getImage1(), false) ||
				pair1.getImage2().equalBase(pair2.getImage2(), false)) {
				return true;
			}
		}
		return false;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	public List<String> getDetailsStrings(int minimumPairsToShow)
	{
		if (_albumSets.isEmpty()) {
			return Collections.singletonList("<no album pairs>");
		}

		List<String> detailString = new ArrayList<>();
		for (Set<AlbumAlbumPair> albumPairs : _albumSets) {
			if (albumPairs.size() >= minimumPairsToShow) {
				Collection<String> baseNames = new TreeSet<> (new AlphanumComparator()); //use set to avoid dups
				for (AlbumAlbumPair albumPair : albumPairs) {
					baseNames.add(albumPair.getBaseName(0));
					baseNames.add(albumPair.getBaseName(1));
				}

				String filters = String.join(",", baseNames);
				AlbumFormInfo form = AlbumFormInfo.getInstance();
				String href = AlbumImages.getInstance().generateImageLink(filters, filters, AlbumMode.DoSampler,  form.getColumns(), form.getSinceDays(), false, true);
				StringBuilder html = new StringBuilder();
//TODO - move to help class/method
				html.append ("<A HREF=\"")
					.append (href)
					.append ("\" ")//.append(NL)
					.append ("title=\"").append (filters)
					.append ("\" target=_blank>")//.append (NL)
					.append (filters)//.append (NL)
					.append ("</A>");//.append (NL);
				detailString.add("[" + VendoUtils.dedupCollection(baseNames).size() + "] " + html.toString()
				);
			}
		}

		return detailString;
	}

	//members
	protected final Set<Set<AlbumAlbumPair>> _albumSets = new HashSet<>();

	private static final AlphanumComparator _alphanumComparator = new AlphanumComparator();

//	protected static Logger _log = LogManager.getLogger ();
}
