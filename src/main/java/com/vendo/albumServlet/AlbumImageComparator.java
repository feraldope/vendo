//AlbumImageComparator.java

package com.vendo.albumServlet;

import java.util.Comparator;
import java.util.Random;

//import org.apache.logging.log4j.*;


public class AlbumImageComparator implements Comparator<AlbumImage>
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType)
	{
		_sortType = sortType;
		_sortFactor = 1;

		_exifDateIndex = 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType, int exifDateIndex)
	{
		_sortType = sortType;
		_sortFactor = 1;

		_exifDateIndex = exifDateIndex;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumFormInfo form)
	{
		_sortType = form.getSortType ();
		_sortFactor = (form.getReverseSort () ? -1 : 1);

		_exifDateIndex = form.getExifDateIndex ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare (AlbumImage image1, AlbumImage image2)
	{
		long value1 = 0;
		long value2 = 0;

		switch (_sortType) {
		default:
			throw new RuntimeException ("AlbumImageComparator.compare: invalid sortType \"" + _sortType + "\"");

		case ByDate: //descending
			value1 = image1.getModified ();
			value2 = image2.getModified ();
			break;

		case BySizeBytes: //descending
			value1 = image1.getNumBytes ();
			value2 = image2.getNumBytes ();
			break;

		case BySizePixels: //descending
		{
//			long w1 = image1.getWidth ();
//			long h1 = image1.getHeight ();
//			long w2 = image2.getWidth ();
//			long h2 = image2.getHeight ();
//
//			//ignore differences in orientation only
//			if (!(w1 == w2 && h1 == h2) && !(w1 == h2 && h1 == w2)) {
				value1 = image1.getPixels ();
				value2 = image2.getPixels ();
//			}
		}
			break;

		case ByCount: //descending
		{
			boolean collapseGroups = AlbumFormInfo.getInstance ().getCollapseGroups ();
			String imageName1 = image1.getBaseName (collapseGroups);
			String imageName2 = image2.getBaseName (collapseGroups);

			value1 = AlbumImageDao.getInstance().getImagesCount (imageName1);
			value2 = AlbumImageDao.getInstance().getImagesCount (imageName2);
		}
			break;

		case ByHash:
			value1 = image1.getHash ();
			value2 = image2.getHash ();
			break;

		case ByRgb:
//			if (true) {
				value1 = image1.getRgbData ().compareToIgnoreCase (image2.getRgbData ());
//
//			} else { // include orientation in comparison
//				value1 = 4 * image1.getRgbData ().compareToIgnoreCase (image2.getRgbData ());
//				value1 |= 2 * (image1.isLandscape () ? 1 : 0);
//				value1 |= 1 * (image2.isLandscape () ? 1 : 0);
//			}
			break;

		case ByRandom:
			value1 = _random ^ image1.getRandom ();
			value2 = _random ^ image2.getRandom ();
			break;

		case ByExif: //ascending
			value1 = image1.compareExifDates (image2, _exifDateIndex);
			break;

		case ByName: //nothing to do - will fall through to return comparison of names
		}

		if (value1 < value2) {
			return _sortFactor;
		} else if (value1 > value2) {
			return -_sortFactor;
		} else {
			//string compare needs to be case-insensitive to achieve case-insensitive order in the browser
			return _sortFactor * image1.getName ().compareToIgnoreCase (image2.getName ());
		}
	}

	//members
	private final AlbumSortType _sortType;
	private final int _sortFactor;

	private final int _exifDateIndex;
	private final int _random = new Random ().nextInt ();

//	private static Logger _log = LogManager.getLogger ();
}
