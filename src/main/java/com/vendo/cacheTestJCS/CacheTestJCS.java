// CacheTestJCS.java - test ApacheJCS - Java Caching System
//
// https://commons.apache.org/proper/commons-jcs/
// https://commons.apache.org/proper/commons-jcs/BasicJCSConfiguration.html
// https://commons.apache.org/proper/commons-jcs/ElementAttributes.html

package com.vendo.cacheTestJCS;

import org.apache.commons.jcs3.JCS;
import org.apache.commons.jcs3.access.CacheAccess;
import org.apache.commons.jcs3.access.exception.CacheException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class CacheTestJCS
{

	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args)
	{
		CacheTestJCS cacheTest = new CacheTestJCS();
		cacheTest.testCache();
	}

//	private CacheAccess<String, City> cache = null;
	private CacheAccess<String, ByteBuffer> cache1 = null;
	private CacheAccess<String, ByteBuffer> cache2 = null;

//	private static Map<String, ByteBuffer> _nameScaledImageMap = null;
//	private static Map<String, AlbumImagePair> _looseCompareMap = null;

	public CacheTestJCS()
	{
		try {
			cache1 = JCS.getInstance("testCache1");
			cache2 = JCS.getInstance("testCache2");

		} catch (CacheException e) {
			System.out.printf("Problem initializing cache: %s%n", e.getMessage());
		}
	}

	public void putInCache(String key, ByteBuffer byteBuffer)
	{
		try {
			cache1.put(key, byteBuffer);
			cache2.put(key, byteBuffer);

		} catch (CacheException e) {
			System.out.printf("Problem putting byteBuffer %s in the cache, for key %s%n%s%n", key, key, e.getMessage());
		}
	}

	public ByteBuffer retrieveFromCache(String key)
	{
		return cache1.get(key);
	}

	public ByteBuffer generateByteBuffer(Integer i)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(10000);
		byteBuffer.putInt(i);

		return byteBuffer;
	}

	public void testCache()
	{
		List<ByteBuffer> buffers = new ArrayList<>();

		final int maxBuffers = 100 * 1000;

		for (int i1 = 0; i1 < maxBuffers; i1++) {
			int i2 = Math.abs (_randomGenerator.nextInt ());
			ByteBuffer b1 = generateByteBuffer(i2);
			putInCache(String.valueOf(i2), b1);
		}

		Map map1 = cache1.getMatching(".+");
		Map map2 = cache2.getMatching(".+");

		int bh = 1;
	}

/*
	// defined as a nested inner class to reduce number of .java files in the example
	public class City implements Serializable
	{
		private static final long serialVersionUID = 6332772176164580346L;
		public String name;
		public String country;
		public int population;

		public City(String name, String country, int population)
		{
			this.name = name;
			this.country = country;
			this.population = population;
		}

		@Override
		public String toString()
		{
			return String.format("%s is a city in the country %s with a population of %d", name, country, population);
		}
	}
*/

/*
		Instant startInstant = Instant.now ();

		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
	}
*/

	private static final Random _randomGenerator = new Random ();

	//global members
	public static boolean _Debug = false;

	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	public static final String _AppName = "CacheTestJCS";
	public static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger ();
}
