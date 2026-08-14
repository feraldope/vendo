package com.vendo.vendoUtils;

import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class VendoUtilsTest {

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testCalculateChunks() {
        //VPair<chunkSize, numChunks> = AlbumImages.calculateChunk (int maxChunks, int minPerChunk, int numItems)
        assertEquals(VPair.of(1, 0), VendoUtils.calculateChunks(10, 100, 0));
        assertEquals(VPair.of(1, 1), VendoUtils.calculateChunks(10, 100, 1));

        assertEquals(VPair.of(30, 1), VendoUtils.calculateChunks(10, 100, 30));

        assertEquals(VPair.of(99, 1), VendoUtils.calculateChunks(10, 100, 99));
        assertEquals(VPair.of(100, 1), VendoUtils.calculateChunks(10, 100, 100));
        assertEquals(VPair.of(101, 1), VendoUtils.calculateChunks(10, 100, 101));

        assertEquals(VPair.of(125, 4), VendoUtils.calculateChunks(10, 100, 499));
        assertEquals(VPair.of(100, 5), VendoUtils.calculateChunks(10, 100, 500));
        assertEquals(VPair.of(101, 5), VendoUtils.calculateChunks(10, 100, 501));

        assertEquals(VPair.of(111, 9), VendoUtils.calculateChunks(10, 100, 999));
        assertEquals(VPair.of(100, 10), VendoUtils.calculateChunks(10, 100, 1000));
        assertEquals(VPair.of(101, 10), VendoUtils.calculateChunks(10, 100, 1001));
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testDeEscapeString() {
        char closeQuote = (char) 39;
        String expected = "Verifying quote" + closeQuote + "s replacement";

        String result = VendoUtils.deEscapeUrlString("Verifying quote&#039;s replacement");
        assertEquals(expected, result);
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testUnitSuffixScaleBytes() {
        String result = VendoUtils.unitSuffixScaleBytes(1024);
        assertEquals(result, "1.00KB");

        result = VendoUtils.unitSuffixScaleBytes(-1);
        assertEquals(result, "-1.0");
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
        assertEquals(ranges, "008-010, 012, 016-017, 028");

        fieldWidth = 2;
        numbers = Arrays.asList (3, 8, 9, 10, 11, 12, 16, 17, 18);
        ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        assertEquals(ranges, "03, 08-12, 16-18");

        fieldWidth = 2;
        numbers = Arrays.asList (3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        assertEquals(ranges, "03-15");

        fieldWidth = 2;
        numbers =  IntStream.rangeClosed(1000, 2000).boxed().collect(Collectors.toCollection(ArrayList::new));
        ranges = VendoUtils.NumberToRange.convertToRanges(numbers, fieldWidth);
        assertEquals(ranges, "1000-2000");
    }

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testExpandRegexRange() {
        List<String> result = VendoUtils.expandRegexRange("[1-5]");
        assertEquals("[1, 2, 3, 4, 5]", result.toString());

        result = VendoUtils.expandRegexRange("[a-f]");
        assertEquals("[a, b, c, d, e, f]", result.toString());

        result = VendoUtils.expandRegexRange("[a-f]a");
        assertEquals("[aa, ba, ca, da, ea, fa]", result.toString());

//TODO - should we prevent this? (mismatched ranges)
        result = VendoUtils.expandRegexRange("[0-F]");
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, :, ;, <, =, >, ?, @, A, B, C, D, E, F]", result.toString());
    }
}
