//AlbumImagesTest.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VPair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AlbumImagesTest {

	@Before
	public void setUp () throws Exception {
	}

	@After
	public void tearDown () throws Exception {
	}

	@Test
	public void testGetChunkSize () {
		//VPair<chunkSize, numChunks> = AlbumImages.calculateChunk (int maxChunks, int minPerChunk, int numItems)
		assertEquals (VPair.of (  1,  0), AlbumImages.calculateChunks (10, 100,    0));
		assertEquals (VPair.of (  1,  1), AlbumImages.calculateChunks (10, 100,    1));

		assertEquals (VPair.of ( 30,  1), AlbumImages.calculateChunks (10, 100,   30));

		assertEquals (VPair.of ( 99,  1), AlbumImages.calculateChunks (10, 100,   99));
		assertEquals (VPair.of (100,  1), AlbumImages.calculateChunks (10, 100,  100));
		assertEquals (VPair.of (101,  1), AlbumImages.calculateChunks (10, 100,  101));

		assertEquals (VPair.of (125,  4), AlbumImages.calculateChunks (10, 100,  499));
		assertEquals (VPair.of (100,  5), AlbumImages.calculateChunks (10, 100,  500));
		assertEquals (VPair.of (101,  5), AlbumImages.calculateChunks (10, 100,  501));

		assertEquals (VPair.of (111,  9), AlbumImages.calculateChunks (10, 100,  999));
		assertEquals (VPair.of (100, 10), AlbumImages.calculateChunks (10, 100, 1000));
		assertEquals (VPair.of (101, 10), AlbumImages.calculateChunks (10, 100, 1001));
	}
}
