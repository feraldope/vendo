//AlbumImageComparator.java

package com.vendo.albumServlet;

import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class AlbumImageComparator implements Comparator<AlbumImage> {

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType) {
		this.sortType = sortType;
		sortFactor = 1;

		exifDateIndex = 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType, boolean reverseSort) {
		this.sortType = sortType;
		sortFactor = reverseSort ? -1 : 1;

		exifDateIndex = 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType, int exifDateIndex) {
		this.sortType = sortType;
		sortFactor = 1;

		this.exifDateIndex = exifDateIndex;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumFormInfo form) {
		sortType = form.getSortType ();
		sortFactor = form.getReverseSort () ? -1 : 1;

		exifDateIndex = form.getExifDateIndex ();
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getNumberOfCallsToCompare () {
		if (debugNumberOfCallsToCompare) {
			return numberOfCallsToCompare.get();
		} else {
			return null;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare (AlbumImage image1, AlbumImage image2) {
		if (debugNumberOfCallsToCompare) {
			numberOfCallsToCompare.incrementAndGet();
		}

		Long value1 = 0L;
		Long value2 = 0L;
		if (sortType.isComparatorUsesCache()) {
			value1 = cache.get(image1);
			value2 = cache.get(image2);
		}

		switch (sortType) {
		default:
			throw new RuntimeException ("AlbumImageComparator.compare: invalid sortType \"" + sortType + "\"");

		case ByDate: //descending
			value1 = image1.getModified ();
			value2 = image2.getModified ();
			break;

		case BySizePixels: //descending
			value1 = image1.getPixels ();
			value2 = image2.getPixels ();
			break;

		case BySizeBytes: //descending - USES CACHE
			if (AlbumFormInfo.getInstance().getMode() == AlbumMode.DoSampler) {
				boolean collapseGroups = AlbumFormInfo.getInstance().getCollapseGroups();

				if (!image1.equals(image2)) {
					if (value1 == null) {
						try {
							String imageName1 = image1.getBaseName(collapseGroups);
							value1 = AlbumImageDao.getInstance().getAlbumSizeInBytesFromCache(imageName1, 0);
							cache.put(image1, value1);
						} catch (Exception ex) {
							value1 = 0L; //not much we can do here
						}
					}
					if (value2 == null) {
						try {
							String imageName2 = image2.getBaseName(collapseGroups);
							value2 = AlbumImageDao.getInstance().getAlbumSizeInBytesFromCache(imageName2, 0);
							cache.put(image2, value2);
						} catch (Exception ex) {
							value2 = 0L; //not much we can do here
						}
					}
				}
			} else {
				value1 = image1.getNumBytes ();
				value2 = image2.getNumBytes ();
			}
			break;

		case BySizeAvgBytes: //descending - USES CACHE
			if (AlbumFormInfo.getInstance().getMode() == AlbumMode.DoSampler) {
				boolean collapseGroups = AlbumFormInfo.getInstance().getCollapseGroups();

				if (!image1.equals(image2)) {
					if (value1 == null) {
						try {
							String imageName1 = image1.getBaseName(collapseGroups);
							long bytes1 = AlbumImageDao.getInstance().getAlbumSizeInBytesFromCache(imageName1, 0);
							long count1 = AlbumImageDao.getInstance().getNumMatchingImagesFromCache(imageName1, 0);
							value1 = bytes1 / count1;
							cache.put(image1, value1);
						} catch (Exception ex) {
							value1 = 0L; //not much we can do here
						}
					}
					if (value2 == null) {
						try {
							String imageName2 = image2.getBaseName(collapseGroups);
							long bytes2 = AlbumImageDao.getInstance().getAlbumSizeInBytesFromCache(imageName2, 0);
							long count2 = AlbumImageDao.getInstance().getNumMatchingImagesFromCache(imageName2, 0);
							value2 = bytes2 / count2;
							cache.put(image2, value2);
						} catch (Exception ex) {
							value2 = 0L; //not much we can do here
						}
					}
				}
			} else {
				value1 = image1.getNumBytes ();
				value2 = image2.getNumBytes ();
			}
			break;

		case ByBytesPerPixel: //descending
			value1 = image1.getBytesPerPixel ();
			value2 = image2.getBytesPerPixel ();
			break;

		case ByCount: //descending
		{
			boolean collapseGroups = AlbumFormInfo.getInstance ().getCollapseGroups ();
			String imageName1 = image1.getBaseName (collapseGroups);
			String imageName2 = image2.getBaseName (collapseGroups);

			if (!imageName1.equals(imageName2)) { //we only need actual values if the inputs are different
				value1 = (long) AlbumImageDao.getInstance().getNumMatchingImagesFromCache(imageName1, 0);
				value2 = (long) AlbumImageDao.getInstance().getNumMatchingImagesFromCache(imageName2, 0);
			}
		}
			break;

		case ByHash:
			value1 = image1.getRgbHash ();
			value2 = image2.getRgbHash ();
			break;

		case ByRgb:
			value1 = (long) image1.getRgbData ().compareToIgnoreCase (image2.getRgbData ());
			break;

		case ByRandom:
			value1 = (long) random ^ image1.getRandom ();
			value2 = (long) random ^ image2.getRandom ();
			break;

		case ByExif: //ascending
			value1 = (long) image1.compareExifDates (image2, exifDateIndex);
			break;

//		case ByImageNumber: //ascending
//			value1 = image2.getImageNumber ();
//			value2 = image1.getImageNumber ();
//			break;

		case ByName: //nothing to do - will fall through to return comparison of names
		}

		if (sortType.isComparatorUsesCache()) {
			if (value1 == null) {
				value1 = 0L;
			}
			if (value2 == null) {
				value2 = 0L;
			}
		}

		if (value1 < value2) {
			return sortFactor;
		} else if (value1 > value2) {
			return -sortFactor;
		} else {
			//string compare needs to be case-insensitive to achieve case-insensitive order in the browser
			return sortFactor * image1.getName ().compareToIgnoreCase (image2.getName ());
		}
	}

	//members
	private final AlbumSortType sortType;
	private final int sortFactor;

	private final int exifDateIndex;
	private final int random = new Random ().nextInt ();

	private final ConcurrentHashMap<AlbumImage, Long> cache = new ConcurrentHashMap<>();

	//DEBUG
	private final boolean debugNumberOfCallsToCompare = false;
	private final AtomicInteger numberOfCallsToCompare = new AtomicInteger(0);

//	private static final Logger _log = LogManager.getLogger ();
}
