//AlbumAlbumPairs.java - class to hold (possibly) multiple AlbumAlbumPair objects that typically are related (like dups)

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VendoUtils;

import java.util.*;
import java.util.stream.Collectors;

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
	public boolean matchesAtLeastOneImage (Set<AlbumAlbumPair> albumSet1, AlbumAlbumPair albumPair2) {
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
	public List<String> getDetailsStrings(int minimumPairsSoShow) {
		if (_albumSets.isEmpty()) {
			return Arrays.asList("<no album pairs>");
		}

		List<String> detailString = new ArrayList<>();
		for (Set<AlbumAlbumPair> albumPairs : _albumSets) {
			if (albumPairs.size() >= minimumPairsSoShow) {
				StringBuffer sb = new StringBuffer();
				List<String> baseNames = new ArrayList<>();
				for (AlbumAlbumPair albumPair : albumPairs) {
					baseNames.add(albumPair.getBaseName(0));
					baseNames.add(albumPair.getBaseName(1));
				}
				detailString.add(VendoUtils.dedupCollection(baseNames).stream().sorted(_alphanumComparator).collect(Collectors.joining(", ")));
			}
		}

		return detailString;
	}

	//members
	protected final Set<Set<AlbumAlbumPair>> _albumSets = new HashSet<>();

	private static final AlphanumComparator _alphanumComparator = new AlphanumComparator();

//	protected static Logger _log = LogManager.getLogger ();
}
