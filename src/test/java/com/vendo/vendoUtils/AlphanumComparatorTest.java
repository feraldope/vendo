package com.vendo.vendoUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlphanumComparatorTest {

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testSort () {
		String leading = "0";
		List<String> list = new ArrayList<> ();
		for (int ii = 0; ii< 10; ii++) {
//		for (int ii = 90; ii< 110; ii++) {
			list.add ("foo01-" + leading + ii + ".ext");
			list.add ("foo01-" + leading + ii + "5.ext");
		}

		Collections.sort (list, new AlphanumComparator ());
//		Collections.sort (list);

		for (String item : list) {
//			System.out.println (item);
		}
	}
}
