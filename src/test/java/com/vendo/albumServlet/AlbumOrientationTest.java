//AlbumOrientationTest.java

package com.vendo.albumServlet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlbumOrientationTest {
	@Test
	public void testGetOrientation()
	{
		assertEquals (AlbumOrientation.ShowAny, AlbumOrientation.getOrientation (-1, -1));
		assertEquals (AlbumOrientation.ShowAny, AlbumOrientation.getOrientation (-1, 1));
		assertEquals (AlbumOrientation.ShowAny, AlbumOrientation.getOrientation (1, -1));
		assertEquals (AlbumOrientation.ShowAny, AlbumOrientation.getOrientation (0, 0));

		for (int ii = -50; ii < 50; ii++) {
			double percent = (double) ii / 10;
			int imageWidth = 1024;
			int imageHeight = addPercent (imageWidth, percent);

			AlbumOrientation expected = AlbumOrientation.ShowSquare;
			if (percent > 2.) {
				expected = AlbumOrientation.ShowPortrait;
			} else if (percent < -2.) {
				expected = AlbumOrientation.ShowLandScape;
			}

			String message = "w:" + imageWidth + ", h:" + imageHeight;
			assertEquals (message, expected, AlbumOrientation.getOrientation (imageWidth, imageHeight));
		}
	}

	public int addPercent (int value, double percent)
	{
		return (int) Math.round (value * (1. + percent / 100.));
	}
}
