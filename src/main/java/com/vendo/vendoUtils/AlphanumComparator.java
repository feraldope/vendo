//original from:
// http://www.davekoelle.com/alphanum.html
// http://www.davekoelle.com/files/AlphanumComparator.java

/*
 * The Alphanum Algorithm is an improved sorting algorithm for strings
 * containing numbers.  Instead of sorting numbers in ASCII order like
 * a standard sort, this algorithm sorts numbers in numeric order.
 *
 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.vendo.vendoUtils;

import java.util.Comparator;

/**
 * This is an updated version with enhancements made by Daniel Migowski,
 * Andre Bogus, and David Koelle
 *
 * To convert to use Templates (Java 1.5+):
 *   - Change "implements Comparator" to "implements Comparator<String>"
 *   - Change "compare(Object o1, Object o2)" to "compare(String s1, String s2)"
 *   - Remove the type checking and casting in compare().
 *
 * To use this class:
 *   Use the static "sort" method from the java.util.Collections class:
 *   Collections.sort(your list, new AlphanumComparator());
 */

///////////////////////////////////////////////////////////////////////////
//
// drich: this version works on Objects instead of Strings, but the Object
//		  must have a toString method that can be used for sorting
//
// Note it also ignores case
//
///////////////////////////////////////////////////////////////////////////

//public class AlphanumComparator implements Comparator<String>
public class AlphanumComparator implements Comparator<Object>
{
	public enum SortOrder {Normal, Reverse}

	private final int _sortFactor;

	public AlphanumComparator(SortOrder sortOrder) {
		_sortFactor = sortOrder == SortOrder.Reverse ? -1 : 1;
	}

	public AlphanumComparator() {
		_sortFactor = 1;
	}

	private final boolean isDigit(char ch)
	{
		return ch >= 48 && ch <= 57;
	}

	/** Length of string is passed in for improved efficiency (only need to calculate it once) **/
	private final String getChunk(String s, int slength, int marker)
	{
		StringBuilder chunk = new StringBuilder();
		char c = s.charAt(marker);
		chunk.append(c);
		marker++;
		if (isDigit(c))
		{
			while (marker < slength)
			{
				c = s.charAt(marker);
				if (!isDigit(c)) {
					break;
				}
				chunk.append(c);
				marker++;
			}
		} else
		{
			while (marker < slength)
			{
				c = s.charAt(marker);
				if (isDigit(c)) {
					break;
				}
				chunk.append(c);
				marker++;
			}
		}
		return chunk.toString();
	}

	@Override
//	public int compare(String s1, String s2)
	public int compare(Object o1, Object o2)
	{
		String s1 = o1.toString ();
		String s2 = o2.toString ();

		int thisMarker = 0;
		int thatMarker = 0;
		int s1Length = s1.length();
		int s2Length = s2.length();

		while (thisMarker < s1Length && thatMarker < s2Length)
		{
			String thisChunk = getChunk(s1, s1Length, thisMarker);
			thisMarker += thisChunk.length();

			String thatChunk = getChunk(s2, s2Length, thatMarker);
			thatMarker += thatChunk.length();

			// If both chunks contain numeric characters, sort them numerically
			int result = 0;
			if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0)))
			{
				// Simple chunk comparison by length.
				int thisChunkLength = thisChunk.length();
				result = thisChunkLength - thatChunk.length();
				// If equal, the first different number counts
				if (result == 0)
				{
					for (int i = 0; i < thisChunkLength; i++)
					{
						result = thisChunk.charAt(i) - thatChunk.charAt(i);
						if (result != 0)
						{
							return _sortFactor * result;
						}
					}
				}
			} else
			{
//				result = thisChunk.compareTo(thatChunk);
				result = thisChunk.compareToIgnoreCase(thatChunk);
			}

			if (result != 0) {
				return _sortFactor * result;
			}
		}

		return _sortFactor * (s1Length - s2Length);
	}
}
