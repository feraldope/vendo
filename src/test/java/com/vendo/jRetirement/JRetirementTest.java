package com.vendo.jRetirement;

import com.vendo.vendoUtils.VendoUtils;
import junit.framework.TestCase;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JRetirementTest extends TestCase {

    JRetirement objectUnderTest = new JRetirement();

    public void testParseDateDownloadedField() {
        List<String> testStrings = Arrays.asList(
                VendoUtils.quoteString("Date downloaded 05/17/2024 9:00 AM ET"),
                VendoUtils.quoteString("Date downloaded May-24-2024 12:35 PM ET"),
                VendoUtils.quoteString("Date downloaded May-24-2024 12:35 p.m ET"),
                VendoUtils.quoteString("Date downloaded May-24-2024 12:35 pm ET")
        );

        for (String testString : testStrings) {
            try {
                List<String> dateDownloadedList = Collections.singletonList(testString);
                Instant instant = objectUnderTest.parseDateDownloadedField(dateDownloadedList);
//                System.out.println ("<" + testString + "> parsed to: <" + JRetirement.dateTimeFormatter.format(instant) + ">");

            } catch (Exception ex) {
                System.err.println("Failed to parse string <" + testString + ">. " + ex);
                fail("Failed to parse string <" + testString + ">. " + ex);
            }
        }
    }
}
