//AlbumImagesTest.java

package com.vendo.albumServlet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AlbumImagesTest {

	///////////////////////////////////////////////////////////////////////////
	@Before
	public void setUp() throws Exception {
	}

	///////////////////////////////////////////////////////////////////////////
	@After
	public void tearDown() throws Exception {
	}

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testGenerateRangesOfMissingImageNumbersFromSortedList() {
		String baseName = "FooBar01";

		List<String> numbers = Arrays.asList ("02", "08", "09", "10", "12", "16", "17", "28");
		List<String> imageNamesNoExt = numbers.stream()
				.map(s -> baseName + "-" + s)
				.collect(Collectors.toList());

		AtomicInteger numMissingImagesReturn = new AtomicInteger(0);
		String ranges = AlbumImages.generateRangesOfMissingImageNumbersFromSortedList(imageNamesNoExt, numMissingImagesReturn);
		Assert.assertEquals(ranges, "01, 03-07, 11, 13-15, 18-27");
		Assert.assertEquals(20, numMissingImagesReturn.get());
	}
}
