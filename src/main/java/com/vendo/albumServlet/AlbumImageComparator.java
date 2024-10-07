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
		this.sortType = sortType;
		sortFactor = 1;

		exifDateIndex = 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType, boolean reverseSort)
	{
		this.sortType = sortType;
		sortFactor = reverseSort ? -1 : 1;

		exifDateIndex = 0;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumSortType sortType, int exifDateIndex)
	{
		this.sortType = sortType;
		sortFactor = 1;

		this.exifDateIndex = exifDateIndex;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImageComparator (AlbumFormInfo form)
	{
		sortType = form.getSortType ();
		sortFactor = form.getReverseSort () ? -1 : 1;

		exifDateIndex = form.getExifDateIndex ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare (AlbumImage image1, AlbumImage image2)
	{
		long value1 = 0;
		long value2 = 0;

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

		case BySizeBytes: //descending
			if (AlbumFormInfo.getInstance().getMode() == AlbumMode.DoSampler) {
				boolean collapseGroups = AlbumFormInfo.getInstance().getCollapseGroups();
				String imageName1 = image1.getBaseName(collapseGroups);
				String imageName2 = image2.getBaseName(collapseGroups);

				value1 = AlbumImageDao.getInstance().getAlbumSizeInBytesFromCache(imageName1, 0);
				value2 = AlbumImageDao.getInstance().getAlbumSizeInBytesFromCache(imageName2, 0);
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

			value1 = AlbumImageDao.getInstance().getNumMatchingImagesFromCache (imageName1, 0);
			value2 = AlbumImageDao.getInstance().getNumMatchingImagesFromCache (imageName2, 0);
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
			value1 = random ^ image1.getRandom ();
			value2 = random ^ image2.getRandom ();
			break;

		case ByExif: //ascending
			value1 = image1.compareExifDates (image2, exifDateIndex);
			break;

//		case ByImageNumber: //ascending
//			value1 = image2.getImageNumber ();
//			value2 = image1.getImageNumber ();
//			break;

		case ByName: //nothing to do - will fall through to return comparison of names
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

//	private static Logger log = LogManager.getLogger ();
}
