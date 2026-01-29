package com.vendo.vendoUtils;

import junit.framework.TestCase;

public class VendoUtilsTest extends TestCase {

    public void testUnitSuffixScaleBytes() {

        assertEquals("1.00KB",  VendoUtils.unitSuffixScaleBytes(1024));
//        String result = VendoUtils.unitSuffixScaleBytes(1024);

        assertEquals("-1.0",  VendoUtils.unitSuffixScaleBytes(-1));
//        result = VendoUtils.unitSuffixScaleBytes(-1);
    }

    public void testUnitSuffixScale() {
    }
}
