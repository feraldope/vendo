// CacheTestJCS.java - test ApacheJCS - Java Caching System
//
// Java doc
// https://commons.apache.org/proper/commons-jcs/commons-jcs3-core/apidocs/index.html
//
// performance test class
// https://commons.apache.org/proper/commons-jcs/JCSvsEHCache.html
//
// https://commons.apache.org/proper/commons-jcs/
// https://commons.apache.org/proper/commons-jcs/BasicJCSConfiguration.html
// https://commons.apache.org/proper/commons-jcs/ElementAttributes.html

package com.vendo.cacheTestJCS;

/*
import org.apache.commons.jcs3.JCS;
import org.apache.commons.jcs3.access.CacheAccess;
import org.apache.commons.jcs3.access.exception.CacheException;
import org.apache.commons.jcs3.engine.CacheElement;
import org.apache.commons.jcs3.engine.behavior.IElementAttributes;
import org.apache.commons.jcs3.engine.control.event.behavior.ElementEventType;
import org.apache.commons.jcs3.engine.control.event.behavior.IElementEvent;
import org.apache.commons.jcs3.engine.control.event.behavior.IElementEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
*/


//public class CacheTestJCS
public class CacheTestJCS //implements IElementEventHandler
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args)
	{
/*
		CacheTestJCS cacheTest = new CacheTestJCS();
		cacheTest.testCache1();

		JCS.shutdown();
*/
	}

/* TODO
How can I configure JCS with my own properties file?
You don't have to put the cache.ccf file in the classpath; instead you can do the following:

    Properties props = new Properties(); props.load("my resource file"");
    JCS.setConfigProperties(props);
*/

	///////////////////////////////////////////////////////////////////////////
	public CacheTestJCS()
	{
/*
		try {
			cache1 = JCS.getInstance("testCache1");
			//cache2 = JCS.getInstance("testCache2");

			// CacheAccess#getDefaultElementAttributes returns a copy not a reference
			IElementAttributes attributes = cache1.getDefaultElementAttributes();
			attributes.addElementEventHandler(this);
			cache1.setDefaultElementAttributes(attributes);

		} catch (CacheException e) {
			System.out.printf("Problem initializing cache: %s%n", e.getMessage());
		}
*/
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public void testCache1()
	{
//		final int maxBuffers = 1000 * 1000;
		final int maxBuffers = 500 * 1000;

		System.out.println(NL + "setup..." + NL);

		Instant startInstant = Instant.now ();
		final Map<String, ByteBuffer> buffers = generateByteBuffers(maxBuffers);
		System.out.println ("generateByteBuffers(" + _decimalFormat2.format(maxBuffers) + ") elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));

		// -----------------------------------------------------------------------------------------------

		System.out.println(NL + "testing with cache..." + NL);

		startInstant = Instant.now ();
		buffers.keySet().forEach(k -> putInCache1(k, buffers.get(k)));
		System.out.println ("putInCache1 elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));

		AtomicInteger numFound = new AtomicInteger(0);

		startInstant = Instant.now ();
		buffers.keySet().forEach(k -> {
			Object o = getFromCache1(k);
			if (o != null) {
				numFound.incrementAndGet();
			}
		});
		System.out.println ("getFromCache1 elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));

		Map<String, ByteBuffer> cache1Map = cache1.getMatching(".+");
		int cache1Size = cache1Map.size();

		System.out.println("cache1.size = " + _decimalFormat2.format(cache1Size));
		System.out.println("numFound = " + _decimalFormat2.format(numFound.get()));

		// -----------------------------------------------------------------------------------------------

		System.out.println(NL + "testing with map..." + NL);

		Map<String, ByteBuffer> map = new HashMap<> ();
		int maxToPutInMap = cache1Size; //use same size as cache1
		AtomicInteger itemsRemovedFromMap = new AtomicInteger(0);

		startInstant = Instant.now ();
		buffers.keySet().forEach(k -> {
			if (true) { //hack - not LRU or anything like it; just remove random items
				if (map.size() > maxToPutInMap) { //if map 'full', remove items
					int newSize = (3 * maxToPutInMap) / 4;
					int numItemsToRemove = map.size () - newSize;

					Iterator<String> iter = map.keySet ().iterator ();
					for (int ii = 0; ii < numItemsToRemove; ii++) {
						iter.next();
						iter.remove();
					}
					itemsRemovedFromMap.getAndAdd(numItemsToRemove);
				}
			}
			map.put(k, buffers.get(k));
		});
		System.out.println ("map.put elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));

		numFound.set(0);

		startInstant = Instant.now ();
		buffers.keySet().forEach(k -> {
			Object o = map.get(k);
			if (o != null) {
				numFound.incrementAndGet();
			}
		});
		System.out.println ("map.get elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, Instant.now ()).toNanos ()).format (_dateTimeFormatter));

		System.out.println("map.size = " + _decimalFormat2.format(map.size()));
		System.out.println("numFound = " + _decimalFormat2.format(numFound.get()));
		System.out.println("itemsRemovedFromMap = " + _decimalFormat2.format(itemsRemovedFromMap.get()));
		System.out.println();

		System.out.println("eventMap = " + _eventMap);
		System.out.println("Note: SPOOLED_NOT_ALLOWED seems to mean 'removed from cache'");
		System.out.println();
	}

	///////////////////////////////////////////////////////////////////////////
	private void putInCache1(String key, ByteBuffer byteBuffer)
	{
		try {
			cache1.put(key, byteBuffer);

		} catch (CacheException e) {
			System.out.printf("Problem putting byteBuffer %s in the cache, for key %s%n%s%n", key, key, e.getMessage());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private ByteBuffer getFromCache1(String key)
	{
		return cache1.get(key);
	}

	///////////////////////////////////////////////////////////////////////////
	private Map<String, ByteBuffer> generateByteBuffers(int numBuffers)
	{
		Map<String, ByteBuffer> buffers = new HashMap<> (numBuffers);

		for (int i1 = 0; i1 < numBuffers; i1++) {
			int i2 = Math.abs (_randomGenerator.nextInt ());
			ByteBuffer b1 = generateByteBuffer(i2);
			buffers.put(String.valueOf(i2), b1);
		}

		return buffers;
	}

	///////////////////////////////////////////////////////////////////////////
	private ByteBuffer generateByteBuffer(Integer i)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(10 * 1000);
		byteBuffer.putInt(i);

		return byteBuffer;
	}

	///////////////////////////////////////////////////////////////////////////
	// https://commons.apache.org/proper/commons-jcs/ElementEventHandling.html
	// Once you have an IElementEventHandler implementation, you can attach it to an element via the Element Attributes.
	// You can either add it to the element attributes when you put an item into the cache, add it to the attributes of an item that exist in the cache (which just results in a re-put),
	// or add the event handler to the default element attributes for a region. If you add it to the default attributes,
	// then all elements subsequently added to the region that do not define their own element attributes will be assigned the default event handlers.
	@Override
	public void handleElementEvent(IElementEvent event)
	{
		ElementEventType eventType = event.getElementEvent();
		String eventTypeName = eventType.name();

		// collect event metrics
		Integer count = _eventMap.get(eventTypeName);
		if (count == null) {
			count = 0;
		}
		_eventMap.put(eventTypeName, ++count);

		if (false) { //log all events
			CacheElement<String, ByteBuffer> element = (CacheElement<String, ByteBuffer>) ((EventObject) event).getSource();
			String elementKey = element.getKey().toString();
			System.out.println("handleElementEvent: eventTypeName = " + eventTypeName + " for key = " + elementKey);
		}
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
*

	private CacheAccess<String, ByteBuffer> cache1 = null;
	//private CacheAccess<String, ByteBuffer> cache2 = null;

//	private static Map<String, ByteBuffer> _nameScaledImageMap = null;
//	private static Map<String, AlbumImagePair> _looseCompareMap = null;

	private static final Random _randomGenerator = new Random ();

	private static final Map<String, Integer> _eventMap = new ConcurrentHashMap<>();

	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	public static final String _AppName = "CacheTestJCS";
	public static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger ();
*/
}
