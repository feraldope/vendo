//ContentItemComparator.java

package com.vendo.contentServlet;

import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ContentItemComparator implements Comparator<ContentItem>
{
	///////////////////////////////////////////////////////////////////////////
	//unused and hidden (private)
	private ContentItemComparator ()
	{
		_sortType = ContentSortType.ByName;
	}

	///////////////////////////////////////////////////////////////////////////
	public ContentItemComparator (ContentSortType sortType)
	{
		_sortType = sortType;
		_sortFactor = 1;

		init ();
	}

	///////////////////////////////////////////////////////////////////////////
	public ContentItemComparator (ContentFormInfo form)
	{
		_sortType = form.getSortType ();

		if (form.getReverseSort ())
			_sortFactor = -1;

		init ();
	}

	///////////////////////////////////////////////////////////////////////////
	private void init ()
	{
//		_log.debug ("ContentItemComparator ctor: sortType = " + sortType);

//		Random randomGenerator = new Random ();
//		_random = Math.abs (randomGenerator.nextLong ());

//		if (ContentFormInfo._logLevel >= 9)
//			_log.debug ("ContentItemComparator.init: _random = " + _random);
	}

	///////////////////////////////////////////////////////////////////////////
	public int compare (ContentItem item1, ContentItem item2)
	{
		long value1 = 0;
		long value2 = 0;

/*
		switch (_sortType) {
		default:
			throw new RuntimeException ("ContentItemComparator.compare: invalid sortType \"" + _sortType + "\"");

		case ByDate:
			value1 = image1.getModified ();
			value2 = image2.getModified ();
			break;

		case BySizeBytes:
			value1 = image1.getBytes ();
			value2 = image2.getBytes ();
			break;

		case BySizePixels:
		{
			long w1 = image1.getWidth ();
			long h1 = image1.getHeight ();
			long w2 = image2.getWidth ();
			long h2 = image2.getHeight ();

			//if images have same dimensions (independent of orientation), fall through to sort by name
			if ((w1 == w2 && h1 == h2) || (w1 == h2 && h1 == w2)) {
				value1 = value2 = 0;

			} else {
				//otherwise sort by image area
				value1 = image1.getPixels ();
				value2 = image2.getPixels ();
			}
		}
			break;

		case ByCount:
			value1 = image1.getCount ();
			value2 = image2.getCount ();
//TOCO - sort ByCount doesn't work the first time because the counts aren't init'ed before doDir
_log.debug (image1.getName () + ", " + image2.getName () + ", " + value1 + ", " + value2);
			break;

		case ByHash:
			value1 = image1.getHash ();
			value2 = image2.getHash ();
			break;

		case ByRandom:
			value1 = image1.getRandom ();
			value2 = image2.getRandom ();

//			value1 ^= _random;
//			value2 ^= _random;
			break;

		case ByName: //nothing to do - will fall through to return comparison of names
		}

		if (value1 < value2)
			return _sortFactor;
		else if (value1 > value2)
			return -_sortFactor;
		else
			//string compare needs to be case-insensitive to achieve case-insensitive order in the browser
			return _sortFactor * image1.getName ().compareToIgnoreCase (image2.getName ());
*/
			return _sortFactor * item1.getName ().compareToIgnoreCase (item2.getName ());
	}

	//members
	private ContentSortType _sortType = ContentSortType.ByName;
	private int _sortFactor = 1; //used to reverse sort
//	private long _random;

	private static Logger _log = LogManager.getLogger (ContentItemComparator.class);
}
