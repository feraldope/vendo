/* CacheTest.java

http://ehcache.org/

ehCache v3 Java doc here:
http://www.ehcache.org/apidocs/3.1.0/index.html

ehCache v2 Java doc here:
http://www.ehcache.org/apidocs/2.8.4/index.html

Official ehcache.xml example:
http://ehcache.org/ehcache.xml

*/

package com.vendo.cacheTest;

import java.io.File;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// ehcache v2
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;


public class Cache2Test
{
	///////////////////////////////////////////////////////////////////////////
	public boolean writeInts (Collection<Integer> ints)
	{
		CacheManager cacheManager = CacheManager.create (getStoragePath () + File.separator + "ehcache2.xml");
		Cache myCache = cacheManager.getCache ("cache2test");

		{
		System.out.println (NL + "Loading " + _decimalFormat2.format (ints.size ()) + " items into Cache");

		Instant startInstant = Instant.now ();
		for (Integer ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Integer value = new Integer (ii);
			Element element = new Element (key, value);
			myCache.put (element);
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		myCache.flush ();
		CacheManager.getInstance ().shutdown ();

		System.gc (); //hint

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean readInts (Collection<Integer> ints)
	{
		CacheManager cacheManager = CacheManager.create (getStoragePath () + File.separator + "ehcache2.xml");
		Cache myCache = cacheManager.getCache ("cache2test");

		int maxValue = 0;
		{
		System.out.println ();
		System.out.println ("Reading " + _decimalFormat2.format (ints.size ()) + " items from Cache");

		Instant startInstant = Instant.now ();
		for (Integer ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Element element = myCache.get (key);
			Integer value = (Integer) element.getObjectValue ();
			if (value > maxValue) {
				maxValue = value;
			}
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		myCache.flush ();
		CacheManager.getInstance ().shutdown ();

		System.gc (); //hint

		return maxValue > 0;
	}

	///////////////////////////////////////////////////////////////////////////
	private String getStoragePath ()
	{
		return Paths.get ("C:/Users/java/CacheTest").toString ();
	}


	//private members
	private String _baseString = "abcdefghijklmnopqrstuvwxyz";

	//global members
	public static boolean _Debug = false;

	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 100 minutes)

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	public static final String _AppName = "Cache2Test";
	public static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger ();
}
