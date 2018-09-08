//AlbumProfilingTest.java - class for profiling performance

package com.vendo.albumServlet;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumProfilingTest
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		AlbumProfilingTest tester = new AlbumProfilingTest ();
		tester.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	public AlbumProfilingTest ()
	{
		AlbumFormInfo._Debug = true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean run ()
	{
//		if (true) {
//			_log.debug ("_log.getName () = " + _log.getName ());
//			_log.debug ("_log.getLevel () = " + _log.getLevel ());
//			_log.debug ("_log.getParent () = " + _log.getParent ());
//		}

//		if (true) {
//			//test for error conditions: recursive entry, exit with no entry, etc.
//			AlbumProfiling.getInstance ().enter (1, "foo1");
//			AlbumProfiling.getInstance ().enter (1, "foo1");
//			AlbumProfiling.getInstance ().exit (1, "foo2");
//		}

		System.out.println ("------------------------------------------------------------");
		run1 (/*count*/ 10000);

		System.out.println ("------------------------------------------------------------");
		run2 (/*enableProfiling*/ true);

		System.out.println ("------------------------------------------------------------");
		run2 (/*enableProfiling*/ false);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean run1 (int count)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		for (int ii = 0; ii < count; ii++) {
			test1 ();
		}

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ false);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean test1 ()
	{
		AlbumProfiling.getInstance ().enter (1);
		AlbumProfiling.getInstance ().exit (1);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean run2 (boolean enableProfiling)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		_log.debug ("enableProfiling = " + new Boolean (enableProfiling));

		int loops = 5;
		int step = 4;
		int iterations = 1;
		double duration = 32;

		for (int ii = 0; ii < loops; ii++) {
			test2 ("loop" + ii, iterations, duration, enableProfiling);
			iterations *= step;
			duration /= step;
		}

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ false);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void test2 (String tag, int iterations, double millis, boolean enableProfiling)
	{
		long startMillis = new Date ().getTime ();

		for (int ii = 0; ii < iterations; ii++) {
			if (enableProfiling)
				AlbumProfiling.getInstance ().enter (1, tag);

			long endNano = System.nanoTime () + (long) (millis * 1000000);
			do {
				try {
					Thread.sleep (0, 1); //sleep 1ns
				} catch (Exception ee) {
					_log.debug ("AlbumProfilingTest.test2 - Thread.sleep exception", ee);
				}
			} while (System.nanoTime () < endNano);

			if (enableProfiling)
				AlbumProfiling.getInstance ().exit (1, tag);
		}

		long elapsedMillis = new Date ().getTime () - startMillis;
		int duration = new Integer ((int) (iterations * millis));
		_log.debug (tag + ": requested total duration " + duration + " ms, elapsed " + elapsedMillis + " ms, iterations: " + iterations);
	}

	private static Logger _log = LogManager.getLogger ();
}
