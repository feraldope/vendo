package com.vendo.getUrl;


import com.vendo.vendoUtils.VendoUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class GetUrlTest
{
	GetUrl objectUnderTest;

	///////////////////////////////////////////////////////////////////////////
	@Before
	public void setUp() throws Exception {
		objectUnderTest = new GetUrl ();
	}

	///////////////////////////////////////////////////////////////////////////
	@After
	public void tearDown() throws Exception {
	}

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testGetUrl () {
	}

	///////////////////////////////////////////////////////////////////////////
//TODO - this should be in VendoUtilsTest.java (but that does not currently exist)
	@Test
	public void testDeEscapeString () {
		char closeQuote = (char) 39;
		String expected = "Verifying quote" + closeQuote + "s replacement";

		String result = VendoUtils.deEscapeUrlString("Verifying quote&#039;s replacement");

		assertEquals(expected, result);
	}
}
