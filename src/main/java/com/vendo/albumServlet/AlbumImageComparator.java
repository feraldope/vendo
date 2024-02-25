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
	public AlbumImageComparator (AlbumSortType sortType, boolean reverseSort)
	{
		_sortType = sortType;
		_sortFactor = reverseSort ? -1 : 1;

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
		_sortFactor = form.getReverseSort () ? -1 : 1;

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

		case BySizePixels: //descending
			value1 = image1.getPixels ();
			value2 = image2.getPixels ();
			break;

		case BySizeBytes: //descending
			value1 = image1.getNumBytes ();
			value2 = image2.getNumBytes ();
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

			value1 = AlbumImageDao.getInstance().getNumMatchingImages (imageName1, 0);
			value2 = AlbumImageDao.getInstance().getNumMatchingImages (imageName2, 0);
		}
			break;

		case ByHash:
			value1 = image1.getRgbHash ();
			value2 = image2.getRgbHash ();
			break;

		case ByRgb:
			value1 = image1.getRgbData ().compareToIgnoreCase (image2.getRgbData ());
			break;

		case ByRandom:
			value1 = _random ^ image1.getRandom ();
			value2 = _random ^ image2.getRandom ();
			break;

		case ByExif: //ascending
			value1 = image1.compareExifDates (image2, _exifDateIndex);
			break;

//		case ByImageNumber: //ascending
//			value1 = image2.getImageNumber ();
//			value2 = image1.getImageNumber ();
//			break;

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
