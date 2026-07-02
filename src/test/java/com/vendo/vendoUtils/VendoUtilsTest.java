package com.vendo.vendoUtils;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VendoUtilsTest extends TestCase {

    ///////////////////////////////////////////////////////////////////////////
    public void testUnitSuffixScaleBytes() {
        String result = VendoUtils.unitSuffixScaleBytes(1024);
        assertEquals(result, "1.00KB");

        result = VendoUtils.unitSuffixScaleBytes(-1);
        assertEquals(result, "-1.0");
    }

    ///////////////////////////////////////////////////////////////////////////
    public void testUnitSuffixScale() {
    }

    ///////////////////////////////////////////////////////////////////////////
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
}
