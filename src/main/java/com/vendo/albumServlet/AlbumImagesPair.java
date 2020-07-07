//AlbumImagePair.java - class to hold a pair of images that typically are related (like dups)

package com.vendo.albumServlet;

//import org.apache.logging.log4j.*;


import java.util.Comparator;

public class AlbumImagesPair implements Comparator<AlbumImage>
{
	///////////////////////////////////////////////////////////////////////////
	public AlbumImagesPair(AlbumImage image1, AlbumImage image2)
	{
//		if (image1.getBaseName (false).compareToIgnoreCase (image2.getBaseName (false)) < 0) {
		if (compare (image1, image2) < 0) {
			_image1 = image1;
			_image2 = image2;
		} else {
			_image1 = image2;
			_image2 = image1;
		}
		//TODO - shouldn't make these calls in ctor
		_numberOfMatchingImages1 = AlbumImages.getNumMatchingImages (image1.getBaseName (false), 0);
		_numberOfMatchingImages2 = AlbumImages.getNumMatchingImages (image2.getBaseName (false), 0);
		_numberOfDuplicateMatches = 1;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImage getImage1 ()
	{
		return _image1;
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumImage getImage2 ()
	{
		return _image2;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumberOfMatchingImages1 ()
	{
		return _numberOfMatchingImages1;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumberOfMatchingImages2 ()
	{
		return _numberOfMatchingImages2;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumberOfDuplicateMatches ()
	{
		return _numberOfDuplicateMatches;
	}

	///////////////////////////////////////////////////////////////////////////
	public int incrementNumberOfDuplicateMatches ()
	{
		return ++_numberOfDuplicateMatches;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare (AlbumImage image1, AlbumImage image2)
	{
		return image1.getName ().compareToIgnoreCase (image1.getName ());
	}

	///////////////////////////////////////////////////////////////////////////
    @Override
    public boolean equals (Object obj)
    {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AlbumImagesPair)) {
			return false;
		}

		AlbumImagesPair other = (AlbumImagesPair) obj;
		return getJoinedNames ().equals (other.getJoinedNames ());
	}

	///////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode ()
    {
		return getJoinedNames ().hashCode ();
    }

//currently only used by hashCode()
	///////////////////////////////////////////////////////////////////////////
	//calculated on demand and cached
	protected synchronized String getJoinedNames ()
	{
		if (_joinedNames == null) {
			_joinedNames = getJoinedNames (getImage1 (), getImage2 ());
		}

		return _joinedNames;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String getJoinedNames (AlbumImage image1, AlbumImage image2)
	{
		String baseName1 = image1.getBaseName (false);
		String baseName2 = image2.getBaseName (false);

		StringBuffer sb = new StringBuffer (48);
		if (baseName1.compareToIgnoreCase (baseName2) < 0) {
			sb.append (baseName1).append (".").append (baseName2);
		} else {
			sb.append (baseName2).append (".").append (baseName1);
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean pairsRepresentDuplicates ()
	{
		return getNumberOfMatchingImages1 () == getNumberOfDuplicateMatches () || getNumberOfMatchingImages2 () == getNumberOfDuplicateMatches ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ();
		sb.append (getImage1 ().getBaseName (false)).append (","); // no space
		sb.append (getImage2 ().getBaseName (false)).append (", ");
		sb.append (getNumberOfMatchingImages1 ()).append (", ");
		sb.append (getNumberOfMatchingImages2 ()).append (", ");
		sb.append (getNumberOfDuplicateMatches ());
//		if (pairsRepresentDuplicates ()) {
//			sb.append (" **********");
//		}

		return sb.toString ();
	}


	//members
	protected final AlbumImage _image1;
	protected final AlbumImage _image2;
	protected final int _numberOfMatchingImages1;
	protected final int _numberOfMatchingImages2;
	protected int _numberOfDuplicateMatches;
	protected String _joinedNames = null;

//	protected static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm:ss"); //Note SimpleDateFormat is not thread safe

//	protected static Logger _log = LogManager.getLogger ();
}
