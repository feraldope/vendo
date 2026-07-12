package com.vendo.vendoUtils;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VendoUtilsTest {

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testCalculateChunks() {
        //VPair<chunkSize, numChunks> = AlbumImages.calculateChunk (int maxChunks, int minPerChunk, int numItems)
        Assert.assertEquals(VPair.of(1, 0), VendoUtils.calculateChunks(10, 100, 0));
        Assert.assertEquals(VPair.of(1, 1), VendoUtils.calculateChunks(10, 100, 1));

        Assert.assertEquals(VPair.of(30, 1), VendoUtils.calculateChunks(10, 100, 30));

        Assert.assertEquals(VPair.of(99, 1), VendoUtils.calculateChunks(10, 100, 99));
        Assert.assertEquals(VPair.of(100, 1), VendoUtils.calculateChunks(10, 100, 100));
        Assert.assertEquals(VPair.of(101, 1), VendoUtils.calculateChunks(10, 100, 101));

        Assert.assertEquals(VPair.of(125, 4), VendoUtils.calculateChunks(10, 100, 499));
        Assert.assertEquals(VPair.of(100, 5), VendoUtils.calculateChunks(10, 100, 500));
        Assert.assertEquals(VPair.of(101, 5), VendoUtils.calculateChunks(10, 100, 501));

        Assert.assertEquals(VPair.of(111, 9), VendoUtils.calculateChunks(10, 100, 999));
        Assert.assertEquals(VPair.of(100, 10), VendoUtils.calculateChunks(10, 100, 1000));
        Assert.assertEquals(VPair.of(101, 10), VendoUtils.calculateChunks(10, 100, 1001));
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testDeEscapeString() {
        char closeQuote = (char) 39;
        String expected = "Verifying quote" + closeQuote + "s replacement";

        String result = VendoUtils.deEscapeUrlString("Verifying quote&#039;s replacement");
        Assert.assertEquals(expected, result);
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testUnitSuffixScaleBytes() {
        String result = VendoUtils.unitSuffixScaleBytes(1024);
        Assert.assertEquals(result, "1.00KB");

        result = VendoUtils.unitSuffixScaleBytes(-1);
        Assert.assertEquals(result, "-1.0");
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testUnitSuffixScale() {
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testConvertToRanges() {
        int fieldWidth = 3;
        List<Integer> numbers = Arrays.asList (8, 9, 10, 12, 16, 17, 28);
        String ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        Assert.assertEquals(ranges, "008-010, 012, 016-017, 028");

        fieldWidth = 2;
        numbers = Arrays.asList (3, 8, 9, 10, 11, 12, 16, 17, 18);
        ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        Assert.assertEquals(ranges, "03, 08-12, 16-18");

        fieldWidth = 2;
        numbers = Arrays.asList (3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        Assert.assertEquals(ranges, "03-15");

        fieldWidth = 2;
        numbers =  IntStream.rangeClosed(1000, 2000).boxed().collect(Collectors.toCollection(ArrayList::new));
        ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        Assert.assertEquals(ranges, "1000-2000");
    }
}
