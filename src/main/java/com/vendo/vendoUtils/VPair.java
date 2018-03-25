//VPair.java
//original from: http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java

package com.vendo.vendoUtils;


public class VPair<A extends Comparable<? super A>, B extends Comparable<? super B>> implements Comparable<VPair<A, B>>
{
	private final A _first;
	private final B _second;

	///////////////////////////////////////////////////////////////////////////
	private VPair (A first, B second)
	{
		_first = first;
		_second = second;
	}

	///////////////////////////////////////////////////////////////////////////
	public static <A extends Comparable<? super A>, B extends Comparable<? super B>> VPair<A, B> of (A first, B second)
	{
		return new VPair<A, B> (first, second);
	}

	///////////////////////////////////////////////////////////////////////////
	public A getFirst ()
	{
		return _first;
	}

	///////////////////////////////////////////////////////////////////////////
	public B getSecond ()
	{
		return _second;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compareTo (VPair<A, B> obj) {
		int cmp = obj == null ? 1 : _first.compareTo (obj._first);
		return cmp == 0 ? _second.compareTo (obj._second) : cmp;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode ()
	{
		return 31 * hashcode (_first) + hashcode (_second);
	}

	///////////////////////////////////////////////////////////////////////////
	// TODO : move to a helper class.
	private static int hashcode (Object obj)
	{
		return obj == null ? 0 : obj.hashCode ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals (Object obj)
	{
		if (!(obj instanceof VPair)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		return equal (_first, ((VPair<?, ?>) obj)._first) &&
			   equal (_second, ((VPair<?, ?>) obj)._second);
	}

	///////////////////////////////////////////////////////////////////////////
	// TODO : move this to a helper class.
	private boolean equal (Object o1, Object o2)
	{
		return o1 == o2 || (o1 != null && o1.equals (o2));
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		return "(" + _first + ", " + _second + ")";
	}
}
