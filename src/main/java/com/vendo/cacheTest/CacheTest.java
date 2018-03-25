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

import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/* ehcache v3
import org.ehcache.*;
import org.ehcache.config.*;
import org.ehcache.config.builders.*;
import org.ehcache.config.units.*;
//import org.ehcache.xml.*;
*/
/*
// ehcache v2
import net.sf.ehcache.*;
import net.sf.ehcache.store.*;
*/
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CacheTest
{
	private enum Mode {NotSet, Read, Write};

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		CacheTest app = new CacheTest ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		int numItems = 0;

		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

					//dump all args
					System.err.println ("args.length = " + args.length);
					for (int jj = 0; jj < args.length; jj++)
						System.err.println ("args[" + jj + "] = '" + args[jj] + "'");

				} else if (arg.equalsIgnoreCase ("numItems") || arg.equalsIgnoreCase ("n")) {
					try {
						numItems = Integer.parseInt (args[++ii]);
						if (numItems < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("read") || arg.equalsIgnoreCase ("r")) {
					_mode = Mode.Read;

				} else if (arg.equalsIgnoreCase ("write") || arg.equalsIgnoreCase ("w")) {
					_mode = Mode.Write;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_inName == null) {
					_inName = arg;

				} else if (_oldString == null) {
					_oldString = arg;

				} else if (_newString == null) {
					_newString = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		//check for required args and handle defaults
		if (_mode == Mode.NotSet)
			displayUsage ("No action specified", true);

		if (numItems > 0) {
			_numItems = numItems * 1000;// * 1000;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] [/numItems <number of items in millions>] {/read | /write}";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
/* old way
		Random random = new Random ();
		IntStream intStream = random.ints (_numItems);
		int ints[] = intStream.toArray ();
*/

		List<Integer> ints = new ArrayList<> (_numItems);
		for (int ii = 0; ii < _numItems; ii++) {
			ints.add (ii);
		}
		Collections.shuffle (ints);

		testMap (ints);

		Cache2Test cache2Test = new Cache2Test ();

		if (_mode == Mode.Write) {
			cache2Test.writeInts (ints);
		}

		//if (_mode == Mode.Write || _mode == Mode.Read)
		cache2Test.readInts (ints);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean testMap (Collection<Integer> ints)
	{
		Map<String, Integer> testMap = new ConcurrentHashMap<String, Integer> (_numItems);

		{
		System.out.println (NL + "Loading " + _decimalFormat2.format (ints.size ()) + " items into ConcurrentHashMap");

		Instant startInstant = Instant.now ();
		for (Integer ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Integer value = new Integer (ii);
			testMap.put (key, value);
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		int maxValue = 0;
		{
		System.out.println ();
		System.out.println ("Reading " + _decimalFormat2.format (ints.size ()) + " items from ConcurrentHashMap");

		Instant startInstant = Instant.now ();
		for (Integer ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Integer value = testMap.get (key);
			if (value > maxValue) {
				maxValue = value;
			}
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		testMap.clear ();
		System.gc (); //hint

		return maxValue > 0;
	}

	///////////////////////////////////////////////////////////////////////////
	protected String getStoragePath ()
	{
		return Paths.get ("C:/Users/java/CacheTest").toString ();
	}

/*
	///////////////////////////////////////////////////////////////////////////
	private boolean testEhCache (int ints[])
	{
*/
/*		PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder ()
			.with (CacheManagerBuilder.persistence (getStoragePath () + File.separator + "myData"))
			.withCache ("ehCache",
				CacheConfigurationBuilder.newCacheConfigurationBuilder (String.class, Integer.class,
					ResourcePoolsBuilder.newResourcePoolsBuilder ()
						.heap (_numItems, EntryUnit.ENTRIES)
//						.offheap (_numItems, MemoryUnit.KB)
//						.disk (20, MemoryUnit.MB)
					)
			).build (true);

		Cache<String, Integer> myCache = persistentCacheManager.getCache ("ehCache", String.class, Integer.class);
*/
/*
if (true) {
		CacheManager cacheManager = CacheManager.create (getStoragePath () + File.separator + "ehcache2.xml");
		Cache myCache = cacheManager.getCache ("cache2test");

		{
		System.out.println (NL + "Loading " + _decimalFormat2.format (_numItems) + " items into Cache");

		Instant startInstant = Instant.now ();
		for (int ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Integer value = new Integer (ii);
			Element element = new Element (key, value);
			myCache.put (element);
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		int maxValue = 0;
		{
		System.out.println ();
		System.out.println ("Reading " + _decimalFormat2.format (_numItems) + " items from Cache");

		Instant startInstant = Instant.now ();
		for (int ii : ints) {
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
//		cacheManager.shutdown ();
		CacheManager.getInstance ().shutdown ();

		System.gc (); //hint

		return maxValue > 0;

} else { // v3 code
*/
/*		PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder ()
			.with (CacheManagerBuilder.persistence (getStoragePath () + File.separator + "myData"))
			.withCache ("ehCache",
				CacheConfigurationBuilder.newCacheConfigurationBuilder (String.class, Integer.class,
					ResourcePoolsBuilder.newResourcePoolsBuilder ()
						.heap (_numItems, EntryUnit.ENTRIES)
//						.offheap (_numItems, MemoryUnit.KB)
//						.disk (20, MemoryUnit.MB)
					)
			).build (true);

		Cache<String, Integer> myCache = persistentCacheManager.getCache ("ehCache", String.class, Integer.class);
*/
/*
		CacheManager cacheManager = CacheManager.create (getStoragePath () + File.separator + "ehcache.xml");
		Cache myCache = cacheManager.getCache ("cachetest");

		{
		System.out.println (NL + "Loading " + _decimalFormat2.format (_numItems) + " items into Cache");

		Instant startInstant = Instant.now ();
		for (int ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Integer value = new Integer (ii);
			myCache.put (key, value);
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		int maxValue = 0;
		{
		System.out.println ();
		System.out.println ("Reading " + _decimalFormat2.format (_numItems) + " items from Cache");

		Instant startInstant = Instant.now ();
		for (int ii : ints) {
			String key = _baseString + String.valueOf (ii);
			Integer value = myCache.get (key);
			if (value > maxValue) {
				maxValue = value;
			}
		}
		Instant endInstant = Instant.now ();
		System.out.println ("Elapsed: " + LocalTime.ofNanoOfDay (Duration.between (startInstant, endInstant).toNanos ()).format (_dateTimeFormatter));
		}

		persistentCacheManager.close ();
		System.gc (); //hint

		return maxValue > 0;
*/
/*
}
		return false;
	}
*/


	//private members
	private Mode _mode = Mode.NotSet;
	private int _numItems = 1 * 1000;// * 1000;
	private String _baseString = "abcdefghijklmnopqrstuvwxyz";

	//global members
	public static boolean _Debug = false;

	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME;
//	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 100 minutes)

	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	public static final String _AppName = "CacheTest";
	public static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger ();
}
