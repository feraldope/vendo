package com.vendo.jpgUtils;

import com.vendo.jpgUtils.JpgUtils.ImageAttributes;
import com.vendo.vendoUtils.VPair;
import junit.framework.TestCase;
//import com.vendo.jpgUtils.calculateDesiredDimensions;

public class JpgUtilsTest extends TestCase {

    public void testCalculateDesiredDimensions() {
        calculateDesiredDimensionsTest("landscape", 8000, 5000);
        calculateDesiredDimensionsTest("portrait", 5000, 8000);
        calculateDesiredDimensionsTest("square", 6000, 6000);
    }

    public void calculateDesiredDimensionsTest(String filename, int width, int height) {
        final int fifteenMegaPixels = 15 * 1000 * 1000;

        //these values are not important for the test, just example display values
        final long fourMegaBytes = 4L * 1024 * 1024;
        final long fourHundredKiloBytes = 400L * 1024;

        ImageAttributes imageAttributes1 = new ImageAttributes(filename, width, height, fourMegaBytes);
        if (verbose) {
            System.out.println(imageAttributes1);
        }

        JpgUtils jpgUtils = new JpgUtils();
        VPair<Integer, Integer> pair = jpgUtils.calculateDesiredDimensions(imageAttributes1, 15);
        ImageAttributes imageAttributes2 = new ImageAttributes(filename, pair.getFirst(), pair.getSecond(), fourHundredKiloBytes);
        if (verbose) {
            System.out.println(imageAttributes2);
        }

        assertTrue(Math.abs(fifteenMegaPixels - pair.getFirst() * pair.getSecond()) < 1000); //slop

        if (width > height) {
            assertTrue(pair.getFirst() > pair.getSecond());
        } else if (width < height) {
            assertTrue(pair.getFirst() < pair.getSecond());
        } else {
            assertEquals(pair.getFirst(), pair.getSecond());
        }
    }

    final boolean verbose = false;
}
