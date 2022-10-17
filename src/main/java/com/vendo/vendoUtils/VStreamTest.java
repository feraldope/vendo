// VStreamTest.java - test stream.distinct with and without leading or trailing call to .sorted

package com.vendo.vendoUtils;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VStreamTest {
	public enum MyAction {DISTINCT_ONLY, DISTINCT_THEN_SORT, SORT_THEN_DISTINCT}

	private final AtomicInteger compareCount = new AtomicInteger(0);
	private final AtomicInteger compareToCount = new AtomicInteger(0);
	private final AtomicInteger equalsCount = new AtomicInteger(0);
	private final AtomicInteger hashCodeCount = new AtomicInteger(0);

	private static final DecimalFormat formatAsInt = new DecimalFormat ("###,##0"); //int


	///////////////////////////////////////////////////////////////////////////
	public static void main(final String[] args) throws Exception {
		VStreamTest app = new VStreamTest();

		app.run();
	}

	///////////////////////////////////////////////////////////////////////////
	public VStreamTest() {
	}

	///////////////////////////////////////////////////////////////////////////
//	@Test
	public void run() {

	//1. generate test data: first collection unsorted, second collection sorted

		final int maxRandom = 1000 * 1000;
		final long seed = 371823947L;
		final List<MyData> myDataList = new Random(seed)
				.ints(maxRandom, 1, maxRandom + 1)
				.mapToObj(i -> new MyData(String.valueOf(i)))
				.collect(Collectors.toList());

		//now add some duplicate values at the end
		myDataList.add(myDataList.get(11));
		myDataList.add(myDataList.get(22));
		myDataList.add(myDataList.get(33));
		myDataList.add(myDataList.get(44));

		final List<MyData2> myDataList2 = myDataList.stream()
				.map(MyData::getMyString)
				.map(MyData2::new)
				.collect(Collectors.toList());

	//2. test against class with inline Comparator

		//test with unsorted data
		myTest(myDataList, MyAction.DISTINCT_ONLY);
		myTest(myDataList, MyAction.DISTINCT_THEN_SORT);
		myTest(myDataList, MyAction.SORT_THEN_DISTINCT);

		final List<MyData> sortedDataList = myDataList.stream().sorted((o1, o2) -> {
			return o1.getMyString().compareTo(o2.getMyString());
		}).collect(Collectors.toList());

		//test with sorted data
		myTest(myDataList, MyAction.DISTINCT_ONLY);
		myTest(myDataList, MyAction.DISTINCT_THEN_SORT);
		myTest(myDataList, MyAction.SORT_THEN_DISTINCT);

	//3. test against class with defined Comparator

		//test with unsorted data
		myTest(myDataList2, MyAction.DISTINCT_ONLY);
		myTest(myDataList2, MyAction.DISTINCT_THEN_SORT);
		myTest(myDataList2, MyAction.SORT_THEN_DISTINCT);

		final List<MyData2> sortedDataList2 = myDataList2.stream().sorted(
			new MyData2Comparator()
		).collect(Collectors.toList());

		//test with sorted data
		myTest(myDataList2, MyAction.DISTINCT_ONLY);
		myTest(myDataList2, MyAction.DISTINCT_THEN_SORT);
		myTest(myDataList2, MyAction.SORT_THEN_DISTINCT);
	}

	///////////////////////////////////////////////////////////////////////////
	public <T extends MyData> void myTest(final List<T> myDataList, MyAction action) {
		compareCount.set(0);
		compareToCount.set(0);
		equalsCount.set(0);
		hashCodeCount.set(0);

		List<T> newList = new ArrayList<>();

		final Instant startInstant = Instant.now ();
		switch (action) {
			case DISTINCT_ONLY:
				newList = myDataList.stream().distinct().collect(Collectors.toList());
				break;
			case DISTINCT_THEN_SORT:
				if (myDataList.get(0) instanceof Comparable) {
					newList = myDataList.stream().distinct().sorted().collect(Collectors.toList());
				} else {
					newList = myDataList.stream().distinct().sorted((o1, o2) -> {
						return o1.getMyString().compareTo(o2.getMyString());
					}).collect(Collectors.toList());
				}
				break;
			case SORT_THEN_DISTINCT:
				if (myDataList.get(0) instanceof Comparable) {
					newList = myDataList.stream().sorted().distinct().collect(Collectors.toList());
				} else {
					newList = myDataList.stream().sorted((o1, o2) -> {
						return o1.getMyString().compareTo(o2.getMyString());
					}).distinct().collect(Collectors.toList());
				}
				break;
		}
		long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();

		System.out.println("Action: " + action + ", class: " + myDataList.get(0).getClass().getSimpleName());
		System.out.println("source: size = " + formatAsInt.format(myDataList.size()) + ", list = " + (myDataList.size() <= 100 ? myDataList : "[not shown]"));
		System.out.println("result: size = " + formatAsInt.format(newList.size()) + ", list = " + (newList.size() <= 100 ? newList : "[not shown]"));

		System.out.println("compareCount   = " + formatAsInt.format(compareCount.get()));
		System.out.println("compareToCount = " + formatAsInt.format(compareToCount.get()));
		System.out.println("equalsCount    = " + formatAsInt.format(equalsCount.get()));
		System.out.println("hashCodeCount  = " + formatAsInt.format(hashCodeCount.get()));

		System.out.println ("elapsed: " + formatAsInt.format(elapsedNanos) + " ns");
		System.out.println();
	}

	///////////////////////////////////////////////////////////////////////////
	private class MyData2Comparator implements Comparator<MyData2> {

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compare (MyData2 d1, MyData2 d2) {
			compareCount.incrementAndGet();

			return d1.getMyString().compareTo(d2.getMyString());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class MyData2 extends MyData implements Comparable<MyData> {

		///////////////////////////////////////////////////////////////////////////
		public MyData2(String myString) {
			super(myString);
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int compareTo(MyData other) {
			compareToCount.incrementAndGet();

			return getMyString ().compareTo (other.getMyString ());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class MyData {
		final String myString;

		///////////////////////////////////////////////////////////////////////////
		public MyData(String myString) {
			this.myString = myString;
		}

		///////////////////////////////////////////////////////////////////////////
		public String getMyString () {
			return myString;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public boolean equals (Object obj) {
			equalsCount.incrementAndGet();

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof MyData)) {
				return false;
			}

			MyData other = (MyData) obj;
			return getMyString().equals(other.getMyString());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public int hashCode () {
			hashCodeCount.incrementAndGet();

			return Objects.hash (getMyString());
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString() {
			return getMyString();
		}
	}
}
