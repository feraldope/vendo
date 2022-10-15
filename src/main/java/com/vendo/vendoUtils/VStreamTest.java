// VStreamTest.java

package com.vendo.vendoUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class VStreamTest {
	final AtomicInteger compareToCount = new AtomicInteger(0);
	final AtomicInteger equalsCount = new AtomicInteger(0);
	final AtomicInteger hashCodeCount = new AtomicInteger(0);

	///////////////////////////////////////////////////////////////////////////
	public static void main (final String[] args) throws Exception
	{
		VStreamTest app = new VStreamTest();

		app.run();
	}

	///////////////////////////////////////////////////////////////////////////
	public VStreamTest() {
	}

	///////////////////////////////////////////////////////////////////////////
//	@Test
	public void run() {
		final int maxRandom = 100;
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

		//test stream.distinct with unsorted data
		myTest(myDataList, true);
		myTest(myDataList, false);

		final List<MyData> sortedDataList = myDataList.stream().sorted((o1, o2) -> {
			return o1.getMyString().compareTo(o2.getMyString());
		}).collect(Collectors.toList());

		//test stream.distinct with sorted data
		myTest(sortedDataList, true);
		myTest(sortedDataList, false);
	}

	///////////////////////////////////////////////////////////////////////////
	public void myTest (final List<MyData> myDataList, boolean performSort) {
		compareToCount.set(0);
		equalsCount.set(0);
		hashCodeCount.set(0);

		//use stream().sorted().distinct() OR stream().distinct()
		List<MyData> dedupedList = performSort ?
				myDataList.stream().sorted((o1, o2) -> {
					return o1.getMyString().compareTo(o2.getMyString());
				}).distinct().collect(Collectors.toList())
				:
				myDataList.stream().distinct().collect(Collectors.toList());

		System.out.println("source: size = " + myDataList.size() + ", list = " + myDataList);
		System.out.println("result: size = " + dedupedList.size() + ", list = " + dedupedList);

		System.out.println("compareToCount = " + compareToCount.get());
		System.out.println("equalsCount    = " + equalsCount.get());
		System.out.println("hashCodeCount  = " + hashCodeCount.get());
		System.out.println();
	}

	///////////////////////////////////////////////////////////////////////////
	private class MyData implements Comparable<MyData> {
		String myString;

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
		public int compareTo(MyData other) {
			compareToCount.incrementAndGet();

			return getMyString ().compareTo (other.getMyString ());
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
