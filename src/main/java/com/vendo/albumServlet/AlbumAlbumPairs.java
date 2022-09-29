//AlbumAlbumPairs.java - class to hold (possibly) multiple AlbumAlbumPair objects that typically are related (like dups)

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static com.vendo.albumServlet.AlbumImagePair._alphanumComparator;


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
		boolean localDebug = false;

		if (localDebug) {
			_log.debug("AlbumAlbumPairs.addAlbumPair: adding pair: " + albumPair);
			_log.debug("AlbumAlbumPairs.addAlbumPair: all known sets before: ");
			int count = 0;
			for (Set<AlbumAlbumPair> albumSet : _albumSets) {
				_log.debug("AlbumAlbumPairs.addAlbumPair: set" + ++count + ": " + //albumSet);
						albumSet.stream()
								.map(AlbumAlbumPair::getImagePairs)
								.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
								.flatMap(Collection::stream)
								.map(i -> i.getBaseName(false))
								.sorted(_alphanumComparator)
								.distinct()
								.collect(Collectors.toList()));
			}
		}

		int added = 0;
		for (Set<AlbumAlbumPair> albumSet : _albumSets) {
			if (matchesAtLeastOneImage(albumSet, albumPair)) {
				added++;
				albumSet.add(albumPair);
			}
		}
		if (added == 0) {
			HashSet<AlbumAlbumPair> albumSetNew = new HashSet<>();
			albumSetNew.add(albumPair);
			_albumSets.add(albumSetNew);
		}

		if (localDebug) {
			_log.debug("AlbumAlbumPairs.addAlbumPair: all known sets after@1: ");
			int count = 0;
			for (Set<AlbumAlbumPair> albumSet : _albumSets) {
				_log.debug("AlbumAlbumPairs.addAlbumPair: set" + ++count + ": " + //albumSet);
						albumSet.stream()
								.map(AlbumAlbumPair::getImagePairs)
								.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
								.flatMap(Collection::stream)
								.map(i -> i.getBaseName(false))
								.sorted(_alphanumComparator)
								.distinct()
								.collect(Collectors.toList()));
			}
		}

//		_log.debug("AlbumAlbumPairs.addAlbumPair: all known albums: " + getAllAlbumsAcrossAllMatches());

		//go back over all sets and add any pairs we missed //hack??
//TODO - this can result in multiple identical Sets (that are later deduped by addServletError())
		for (Set<AlbumAlbumPair> albumSet1 : _albumSets) {
			for (AlbumAlbumPair albumPair1 : albumSet1) {
				for (Set<AlbumAlbumPair> albumSet2 : _albumSets) {
					if (matchesAtLeastOneImage(albumSet2, albumPair1)) {
						if (localDebug) { //debug logging
							if (!albumSet1.contains(albumPair1)) {
								_log.debug("AlbumAlbumPairs.addAlbumPair: adding pair: " + albumPair1 + " to existing set1: " +
									albumSet1.stream()
										.map(AlbumAlbumPair::getImagePairs)
										.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
//										.flatMap(Collection::stream)
//										.map(p -> Arrays.asList(p.getImage1(), p.getImage2()))
										.flatMap(Collection::stream)
										.map(i -> i.getBaseName(false))
										.sorted(_alphanumComparator)
										.distinct()
										.collect(Collectors.toList()));
//										+ " ************************************");
							}
							if (!albumSet2.contains(albumPair1)) {
								_log.debug("AlbumAlbumPairs.addAlbumPair: adding pair: " + albumPair1 + " to existing set2: " +
									albumSet2.stream()
										.map(AlbumAlbumPair::getImagePairs)
										.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
//										.flatMap(Collection::stream)
//										.map(p -> Arrays.asList(p.getImage1(), p.getImage2()))
										.flatMap(Collection::stream)
										.map(i -> i.getBaseName(false))
										.sorted(_alphanumComparator)
										.distinct()
										.collect(Collectors.toList()));
//										+ " ************************************");
							}
						}
						albumSet1.add(albumPair1);
						albumSet2.add(albumPair1);
					}
				}
			}
		}

		//dedup the EXISTING set contents
		Set<Set<AlbumAlbumPair>> newAlbumSets = new HashSet<>();
		newAlbumSets.addAll(_albumSets);
		if (newAlbumSets.size() != _albumSets.size()) {
			_log.debug("AlbumAlbumPairs.addAlbumPair: DEDUPING sets *****************************");
			_albumSets.clear();
			_albumSets.addAll(newAlbumSets);
		}

		if (localDebug) {
			_log.debug("AlbumAlbumPairs.addAlbumPair: all known sets after@2: ");
			int count = 0;
			for (Set<AlbumAlbumPair> albumSet : _albumSets) {
				_log.debug("AlbumAlbumPairs.addAlbumPair: set" + ++count + ": " + //albumSet);
						albumSet.stream()
								.map(AlbumAlbumPair::getImagePairs)
								.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
								.flatMap(Collection::stream)
								.map(i -> i.getBaseName(false))
								.sorted(_alphanumComparator)
								.distinct()
								.collect(Collectors.toList()));
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> getAllAlbumsAcrossAllMatches(boolean collapseGroups) {
		return getAllAlbumsAcrossAllMatches(collapseGroups, Integer.MAX_VALUE);
	}
	public List<String> getAllAlbumsAcrossAllMatches(boolean collapseGroups, int maxItemsToReturn) {
		 return _albumSets.stream()
						.flatMap(Collection::stream)
						.map(AlbumAlbumPair::getImagePairs)
				 		.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
						.flatMap(Collection::stream)
						.map(i -> i.getBaseName(collapseGroups))
				 		.limit(maxItemsToReturn)
						.sorted(_alphanumComparator)
						.distinct()
						.collect(Collectors.toList());
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
					baseNames.add(albumPair.getBaseName(0)
//TODO - clean this up
							+ " (" + albumPair.getNumberOfImagesInAlbum(0) + ")"
					);
					baseNames.add(albumPair.getBaseName(1)
							+ " (" + albumPair.getNumberOfImagesInAlbum(1) + ")"
					);
				}

				String filters = String.join(",", baseNames);
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
				detailString.add("[" + VendoUtils.dedupCollection(baseNames).size() + "] " + html
				);
			}
		}

		return detailString;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return "All known albums: " + _albumSets.stream().flatMap(Collection::stream)
			.map(AlbumAlbumPair::toString)
			.sorted(_alphanumComparator)
			.collect(Collectors.joining(", "));
	}

	//members
	protected final Set<Set<AlbumAlbumPair>> _albumSets = new HashSet<>();

	protected static Logger _log = LogManager.getLogger ();
}
