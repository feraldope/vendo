package com.vendo.vendoUtils;

import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AlphanumComparatorTest {

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testSortAndReverseSort () {
		final int numItems = 100;
		final int maxRandValue = 10 * 1000;

		//create list of random ints
		List<Integer> listInt =  new Random().ints(0,maxRandValue).limit(numItems).boxed().collect(Collectors.toList());

		//expected: first sort as INTS, then wrap with alpha strings
		List<String> listSortedAsIntsNormal = listInt.stream().sorted(Comparator.naturalOrder()).map(this::wrap).collect(Collectors.toList());
		List<String> listSortedAsIntsReverse = listInt.stream().sorted(Comparator.reverseOrder()).map(this::wrap).collect(Collectors.toList());

		//actual: first wrap with alpha strings, then sort as ALPHANUM
		List<String> listSortedAsAlphaNumNormal = listInt.stream().map(this::wrap).sorted(new AlphanumComparator()).collect(Collectors.toList());
		List<String> listSortedAsAlphaNumReverse = listInt.stream().map(this::wrap).sorted(new AlphanumComparator(AlphanumComparator.SortOrder.Reverse)).collect(Collectors.toList());

		if (false) {
			System.out.println("listSortedAsIntsNormal: " + NL + listSortedAsIntsNormal);
			System.out.println("listSortedAsIntsReverse: " + NL + listSortedAsIntsReverse);
			System.out.println("listSortedAsAlphaNumNormal: " + NL + listSortedAsAlphaNumNormal);
			System.out.println("listSortedAsAlphaNumReverse: " + NL + listSortedAsAlphaNumReverse);
		}

		//now make sure they sorted in the same order
		assertEquals(listSortedAsIntsNormal, listSortedAsAlphaNumNormal);
		assertEquals(listSortedAsIntsReverse, listSortedAsAlphaNumReverse);

		assertNotEquals(listSortedAsIntsNormal, listSortedAsAlphaNumReverse);
		assertNotEquals(listSortedAsIntsReverse, listSortedAsAlphaNumNormal);
	}

	private String wrap(Integer ii) {
		return "alpha" + ii + ".tail";
	}

	public static final String NL = System.getProperty ("line.separator");
}
