//AlbumImageDuplicateDetails.java

package com.vendo.albumServlet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.vendo.albumServlet.AlbumImages.compareToWithSlop;

///////////////////////////////////////////////////////////////////////////
public class AlbumImageDuplicateDetails implements Comparable<AlbumImageDuplicateDetails>
{
    ///////////////////////////////////////////////////////////////////////////
    //filter is an actual filter, with proper case
    AlbumImageDuplicateDetails (String filter, String needsCleanupDigits)
    {
        _filter = filter;
        _needsCleanupDigits = needsCleanupDigits;

        if (_filter == null || _filter.isEmpty()) {
            throw new IllegalArgumentException ("AlbumImageDuplicateDetails.ctor: invalid values: + " + this);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public void init (Collection<AlbumImage> imageDisplayList)
    {
        _firstMatchingImage = findFirstMatchingImage(_filter, imageDisplayList);
        _imageBaseName = _firstMatchingImage.getBaseName(false);
        _subFolder = _firstMatchingImage.getSubFolder();
        _digitsInImageBaseName = _imageBaseName.chars().filter(Character::isDigit).count();
        _count = countMatchingImages(_filter, imageDisplayList);
        _averageAspectRatio = getAverageAspectRatio(_filter, imageDisplayList);
        _averagePixels = getAveragePixels(_filter, imageDisplayList);
        _medianPixels = getMedianPixels(_filter, imageDisplayList, AlbumOrientation.ShowAny);
        _medianPixelsPortraitOrientation = getMedianPixels(_filter, imageDisplayList, AlbumOrientation.ShowPortrait);
        _percentPortrait = getPercentPortrait(_filter, imageDisplayList);
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public int compareTo (AlbumImageDuplicateDetails i2)
    {
        AlbumImageDuplicateDetails i1 = this;

        //compare median pixel size of only the portrait-oriented images
        //TODO - is comparing median pixel size (of only the portrait oriented images) always right?
        long medianPixelDiff = compareToWithSlop (i1._medianPixelsPortraitOrientation, i2._medianPixelsPortraitOrientation, false, 0.5); //sort in descending order
        if (medianPixelDiff != 0) {
            return medianPixelDiff > 0 ? 1 : -1;
        }

        //compare percent orientation
        long percentPortraitDiff = i2._percentPortrait - i1._percentPortrait; //sort in descending order
        final long maxPreferredPercentPortraitDiff = 6; //5; //allowable variation from exact that will still be considered equal
        if (Math.abs (percentPortraitDiff) >= maxPreferredPercentPortraitDiff) {
            return percentPortraitDiff > 0 ? 1 : -1;
        }

        //if everything is landscape, prefer album with more-square aspect ratio
        if (i1._percentPortrait == 0 && i2._percentPortrait == 0) {
            long averageAspectRatioDiff = i1._averageAspectRatio - i2._averageAspectRatio;  //sort in ascending order
            if (averageAspectRatioDiff != 0) {
                return averageAspectRatioDiff > 0 ? 1 : -1;
            }
        }

        //compare average pixel size - we only get here if there are no portrait oriented images
        long averagePixelDiff = compareToWithSlop (i1._averagePixels, i2._averagePixels, false, 0.5); //sort in descending order
        if (averagePixelDiff != 0) {
            return averagePixelDiff > 0 ? 1 : -1;
        }

        //compare count
        long countDiff = i2._count - i1._count; //sort in descending order
//		if (countDiff != 0) {
//			return countDiff > 0 ? 1 : -1;
//		}

        return countDiff == 0 ? 0 : countDiff > 0 ? 1 : -1;
    }

    ///////////////////////////////////////////////////////////////////////////
    //filter is an actual filter, with proper case
    public static List<AlbumImageDuplicateDetails> splitMultipleSubAlbums (String filter, Collection<AlbumImage> imageDisplayList)
    {
        List<AlbumImageDuplicateDetails> items = new ArrayList<>();

        AlbumImage firstImage = findFirstMatchingImage(filter, imageDisplayList);
        if (firstImage == null) {
            return null;
        }

        String digits = firstImage.getName().split("-")[1];
        int numDigits = digits.length();
        if (numDigits == 2) {
            items.add(new AlbumImageDuplicateDetails(filter, null));
            return items;

        } else if (numDigits == 3) {
            if (digits.startsWith("0")) {
                return null;

            } else {
                for (int ii = 0; ii < 10; ii++) {
                    String albumName = String.format("%s-%d", filter, ii);
                    if (findFirstMatchingImage(albumName, imageDisplayList) != null) {
                        items.add(new AlbumImageDuplicateDetails(albumName, String.format("%d", ii)));
                    }
                }
            }

        } else if (numDigits == 4) {
            for (int ii = 0; ii < 100; ii++) {
                String albumName = String.format("%s-%02d", filter, ii);
                if (findFirstMatchingImage(albumName, imageDisplayList) != null) {
                    items.add(new AlbumImageDuplicateDetails(albumName, String.format("%02d", ii)));
                }
            }
        }

        return items;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static AlbumImage findFirstMatchingImage (String filter, Collection<AlbumImage> imageDisplayList)
    {
        return imageDisplayList.stream()
                .filter(i -> i.getName().startsWith(filter))
                .findFirst()
                .orElse(null);
    }

    ///////////////////////////////////////////////////////////////////////////
    private Long countMatchingImages (String filter, Collection<AlbumImage> imageDisplayList)
    {
        return imageDisplayList.stream()
                .filter(i -> i.getName().startsWith(filter))
                .count();
    }

    ///////////////////////////////////////////////////////////////////////////
    private Long getAverageAspectRatio (String filter, Collection<AlbumImage> imageDisplayList)
    {
        return Math.round (imageDisplayList.stream ()
                .filter(i -> i.getName().startsWith(filter))
                .map (i -> 100. * (double) i.getWidth() / (double) i.getHeight())
                .mapToDouble (i -> i)
                .average ()
                .orElse(0));
    }

    ///////////////////////////////////////////////////////////////////////////
    private Long getAveragePixels (String filter, Collection<AlbumImage> imageDisplayList)
    {
        return Math.round (imageDisplayList.stream ()
                .filter(i -> i.getName().startsWith(filter))
                .map (AlbumImage::getPixels)
                .mapToDouble (i -> i)
                .average ()
                .orElse(0));
    }

    ///////////////////////////////////////////////////////////////////////////
    private Long getMedianPixels (String filter, Collection<AlbumImage> imageDisplayList, AlbumOrientation orientation) {
        List<Double> pixels = imageDisplayList.stream()
                .filter(i -> i.getName().startsWith(filter))
                .filter(i -> i.matchOrientation(orientation))
                .map (AlbumImage::getPixels)
                .mapToDouble (i -> i)
                .sorted ()
                .boxed ()
                .collect (Collectors.toList ());

        double median = 0;
        if (!pixels.isEmpty()) {
            int halfSize = pixels.size() / 2;
            median = pixels.get(halfSize);
            if (halfSize > 1 && halfSize % 2 == 0) {
                median = (median + pixels.get(halfSize - 1)) / 2;
            }
        }

        return Math.round(median);
    }

    ///////////////////////////////////////////////////////////////////////////
    private Long getPercentPortrait (String filter, Collection<AlbumImage> imageDisplayList)
    {
        List<AlbumOrientation> orientations = imageDisplayList.stream()
                .filter(i -> i.getName().startsWith(filter))
                .map(AlbumImage::getOrientation)
                .collect(Collectors.toList());

        long numPortrait = orientations.stream()
                .filter(p -> p == AlbumOrientation.ShowPortrait)
                .count();

        return Math.round(100. * (double) numPortrait / (double) orientations.size());
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean doesNumberOfDigitsInBaseNamesMatch (Collection<AlbumImageDuplicateDetails> dups)
    {
        return dups.stream()
                .map(d -> d._digitsInImageBaseName)
                .distinct()
                .count() != 1;
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public String toString ()
    {
//        StringBuilder sb = new StringBuilder(getClass ().getSimpleName ());
        StringBuilder sb = new StringBuilder("dupDetails");
        sb.append (": ").append (_filter);
        sb.append (", ").append (_firstMatchingImage.getName());
        sb.append (", ").append (_imageBaseName);
        sb.append (", ").append (_subFolder);
        sb.append (", ").append (_digitsInImageBaseName);
        sb.append (", ").append ("-").append(_needsCleanupDigits == null ? "" : _needsCleanupDigits);
        sb.append (", ").append (_decimalFormat2.format (_averagePixels / 1e6)).append("MP");
        sb.append (", ").append (_decimalFormat2.format (_medianPixels / 1e6)).append("MP");
        sb.append (", ").append (_decimalFormat2.format (_medianPixelsPortraitOrientation / 1e6)).append("MP");
        sb.append (", ").append (_percentPortrait).append("%");
        sb.append (", ").append (_decimalFormat2.format (_averageAspectRatio / 100.)).append(":1");
        sb.append (", ").append (_count);

        return sb.toString ();
    }

    //members
    final String _filter;
    AlbumImage _firstMatchingImage = null;
    String _imageBaseName;
    String _subFolder;
    String _needsCleanupDigits;
    Long _digitsInImageBaseName;
    Long _count;
    Long _averageAspectRatio;
    Long _averagePixels;
    Long _medianPixels;
    Long _medianPixelsPortraitOrientation;
    Long _percentPortrait;

    private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0.00");

//    private static final Logger _log = LogManager.getLogger ();
}
