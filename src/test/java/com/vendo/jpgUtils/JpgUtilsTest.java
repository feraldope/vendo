package com.vendo.jpgUtils;

import com.vendo.jpgUtils.JpgUtils.ImageAttributes;
import com.vendo.vendoUtils.VPair;
import junit.framework.TestCase;
//import com.vendo.jpgUtils.calculateDesiredDimensions;

public class JpgUtilsTest extends TestCase {

    public void testCalculateDesiredDimensions() {
        if (false) { //when enabled, generates unnecessary output during java build
            calculateDesiredDimensionsTest("landscape", 8000, 5000);
            calculateDesiredDimensionsTest("portrait", 5000, 8000);
            calculateDesiredDimensionsTest("square", 6000, 6000);
        }
    }

    public void calculateDesiredDimensionsTest(String filename, int width, int height) {
        JpgUtils jpgUtils = new JpgUtils();
        ImageAttributes imageAttributes1 = new ImageAttributes(filename, width, height, (long) (4 * 1e6));
        System.out.println(imageAttributes1);

        VPair<Integer, Integer> pair = jpgUtils.calculateDesiredDimensions(imageAttributes1, 15);
        ImageAttributes imageAttributes2 = new ImageAttributes(filename, pair.getFirst(), pair.getSecond(), (long) (4 * 1e5));
        System.out.println(imageAttributes2);
    }
}
