//AlbumImagesTest.java

package com.vendo.albumServlet;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class AlbumImagesTest {

	///////////////////////////////////////////////////////////////////////////
//	@Before
//	public void setUp() throws Exception {
//	}

	///////////////////////////////////////////////////////////////////////////
//	@After
//	public void tearDown() throws Exception {
//	}

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
		assertEquals("01, 03-07, 11, 13-15, 18-27", ranges);
		assertEquals(20, numMissingImagesReturn.get());
	}

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testCalculateColumns() {

		//test AlbumSkipType.SkipAllButFirstAndLast
		assertEquals(4, AlbumImages.calculateColumns(3, 6, false, AlbumMode.DoDir, AlbumSkipType.SkipAllButFirstAndLast));

		//test interleaveSort
		assertEquals(4, AlbumImages.calculateColumns(3, 6, true, AlbumMode.DoDir, AlbumSkipType.SkipNone));

		//test AlbumMode.DoDup
		assertEquals(4, AlbumImages.calculateColumns(3, 6, false, AlbumMode.DoDup, AlbumSkipType.SkipNone));

	}
}
