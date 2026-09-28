//AlbumAlbumPairs.java - class to hold (possibly) multiple AlbumAlbumPair objects that typically are related (like dups)

package com.vendo.albumServlet;

import com.mysql.cj.x.protobuf.MysqlxDatatypes;
import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.vendo.albumServlet.AlbumImagePair._alphanumComparator;
import static com.vendo.albumServlet.AlbumImages.compareToWithSlop;


public class AlbumAlbumPairs {

	///////////////////////////////////////////////////////////////////////////
	public AlbumAlbumPairs() {
//		int bh = 1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void addAlbumPairs(Collection<AlbumAlbumPair> albumPairs) {
		albumPairs.forEach(this::addAlbumPair);
	}

	///////////////////////////////////////////////////////////////////////////
	public void addAlbumPair(AlbumAlbumPair albumPair) {
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
//	public List<String> getAllAlbumsAcrossAllMatches(boolean collapseGroups) {
//		return getAllAlbumsAcrossAllMatches(collapseGroups, Integer.MAX_VALUE);
//	}
//	public List<String> getAllAlbumsAcrossAllMatches(boolean collapseGroups, int maxItemsToReturn) {
	public List<String> getAllAlbumsAcrossAllMatches(boolean collapseGroups) {
		AlbumProfiling.getInstance ().enter/*AndTrace*/ (5);

		List<String> allAlbumsAcrossAllMatches =
		 /*return*/ _albumSets.stream()
						.flatMap(Collection::stream)
						.map(AlbumAlbumPair::getImagePairs)
				 		.map(p -> AlbumImagePair.getImages(p, AlbumSortType.ByName))
						.flatMap(Collection::stream)
						.map(i -> i.getBaseName(collapseGroups))
						.sorted(_alphanumComparator)
						.distinct()
//				 		.limit(maxItemsToReturn) //should be last operation before collect
						.collect(Collectors.toList());

		_log.debug ("AlbumAlbumPairs.getAllAlbumsAcrossAllMatches(" + collapseGroups + "): allAlbumsAcrossAllMatches.size = " + allAlbumsAcrossAllMatches.size());

		AlbumProfiling.getInstance ().exit (5);

		return allAlbumsAcrossAllMatches;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns true if at least one image in either albumPair has the same base name as an albumPair in this set
	private boolean matchesAtLeastOneImage (Set<AlbumAlbumPair> albumSet1, AlbumAlbumPair albumPair2) {
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

	///////////////////////////////////////////////////////////////////////////
	public List<String> generateCopyCommandsForMisMatchedDuplicateImages () { //mis-matched by pixels
		//get all images, then look for instances where the image is in multiple pairs
		//these should not be copied, so comment out (via "REM") those copy commands below in generateSingleCopyCommand()
		List<AlbumImage> allImages = new ArrayList<>();
		Map<AlbumImage, Long> imageCounts = new HashMap<>();

		for (Set<AlbumAlbumPair> albumPairs : _albumSets) {
			for (AlbumAlbumPair albumPair : albumPairs) {
				Set<AlbumImagePair> imagePairs = albumPair.getImagePairs();
				allImages.addAll(imagePairs.stream().map(AlbumImagePair::getImage1).collect(Collectors.toList()));
				allImages.addAll(imagePairs.stream().map(AlbumImagePair::getImage2).collect(Collectors.toList()));
				imageCounts.putAll(allImages.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));
			}
		}

		final Predicate<AlbumImagePair> misMatchByPixelsLeftGtRight = p -> compareToWithSlop(p.getImage1().getPixels(), p.getImage2().getPixels(), true, AlbumFormInfo._slopPercent) > 0; //keep where L>R
		final Predicate<AlbumImagePair> misMatchByPixelsRightGtLeft = p -> compareToWithSlop(p.getImage1().getPixels(), p.getImage2().getPixels(), true, AlbumFormInfo._slopPercent) < 0; //keep where R>L

		List<String> copyCommands = new ArrayList<>();
		for (Set<AlbumAlbumPair> albumPairs : _albumSets) {
			for (AlbumAlbumPair albumPair : albumPairs) {
				Set<AlbumImagePair> imagePairs = albumPair.getImagePairs();

				//generate LEFT->RIGHT copy commands
				List<String> copyCommandsLtoR = imagePairs.stream()
						.filter(misMatchByPixelsLeftGtRight)
						.map(p -> {
							boolean commentOutThisCopyCommand = imageCounts.computeIfAbsent(p.getImage1(), k -> 0L) > 1 ||
																imageCounts.computeIfAbsent(p.getImage2(), k -> 0L) > 1;
							return generateSingleCopyCommand(p.getImage1(), p.getImage2(), commentOutThisCopyCommand);
						})
						.sorted()
						.collect(Collectors.toList());

				if (!copyCommandsLtoR.isEmpty()) {
					AlbumImagePair firstPair = imagePairs.iterator().next();
					copyCommands.add("REM copy larger images L->R (" + copyCommandsLtoR.size() + "): " + firstPair.getImage1().getBaseName(false) + " -> " + firstPair.getImage2().getBaseName(false) + "    REM");
					copyCommands.addAll(copyCommandsLtoR);
				}
			}

			//generate RIGHT->LEFT copy commands
			for (AlbumAlbumPair albumPair : albumPairs) {
				Set<AlbumImagePair> imagePairs = albumPair.getImagePairs();

				List<String> copyCommandsRtoL = imagePairs.stream()
						.filter(misMatchByPixelsRightGtLeft)
						.map(p -> {
							boolean commentOutThisCopyCommand = imageCounts.computeIfAbsent(p.getImage1(), k -> 0L) > 1 ||
																imageCounts.computeIfAbsent(p.getImage2(), k -> 0L) > 1;
							return generateSingleCopyCommand(p.getImage2(), p.getImage1(), commentOutThisCopyCommand);
						})
						.sorted()
						.collect(Collectors.toList());

				if (!copyCommandsRtoL.isEmpty()) {
					AlbumImagePair firstPair = imagePairs.iterator().next();
					copyCommands.add("REM copy larger images R->L (" + copyCommandsRtoL.size() + "): " + firstPair.getImage2().getBaseName(false) + " -> " + firstPair.getImage1().getBaseName(false) + "    REM");
					copyCommands.addAll(copyCommandsRtoL);
				}
			}
		}

		copyCommands.add("REM copies end ------------------------------------------------");

		return copyCommands;
	}

	///////////////////////////////////////////////////////////////////////////
	private String generateSingleCopyCommand (AlbumImage i1, AlbumImage i2, boolean commentOutThisCopyCommand) {
		return (commentOutThisCopyCommand ? "REM " : "") + "copy /y " + i1.getSubFolder(true) + "\\" + i1.getName() + ".jpg " +
																		i2.getSubFolder(true) + "\\" + i2.getName() + ".jpg";
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> getDetailsStrings(int minimumPairsToShow) {
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
				String html = AlbumImages.generateGenericLink(filters, filters, filters, AlbumMode.DoSampler, -1, -1, false, true);

				detailString.add("[" + VendoUtils.dedupCollection(baseNames).size() + "] " + html);
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
