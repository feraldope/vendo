//AlbumImagesTest.java

package com.vendo.albumServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertTrue;

public class AlbumImagePairTest {

	@Before
	public void setUp () throws Exception {
	}

	@After
	public void tearDown () throws Exception {
	}

	@Test
	public void testCompareTo2() {
		AlbumImage a1 = createAlbumImage("a01");
		AlbumImage a2 = createAlbumImage("a02");
		AlbumImage b1 = createAlbumImage("b01");
		AlbumImage b2 = createAlbumImage("b02");

		AlbumImagePair p1 = createAlbumImagePair(a1, b1, 10);
		AlbumImagePair p2 = createAlbumImagePair(a2, b2, 10);
		int i = p1.compareTo2(p2);
		assertTrue(i < 0); //p1 is "better"

		p1 = createAlbumImagePair(a2, b2, 10);
		p2 = createAlbumImagePair(a1, b1, 10);
		i = p1.compareTo2(p2);
		assertTrue(i > 0); //p2 is "better"

		p1 = createAlbumImagePair(a1, b1, 10);
		p2 = createAlbumImagePair(a2, b2, 8);
		i = p1.compareTo2(p2);
		assertTrue(i > 0); //p2 is "better"
	}

	private AlbumImage createAlbumImage(String name) {
		return new AlbumImage(101, name, "sub", 10, 10, 10, 10,
				"rbg", 10, 10, 10, 10);
	}

	private AlbumImagePair createAlbumImagePair(AlbumImage image1, AlbumImage image2, int averageDiff) {
		return new 	AlbumImagePair (image1, image2, averageDiff, 10, "source", new Date());
	}

}
