//AlbumUncaughtExceptionHandler.java

package com.vendo.albumServlet;

import java.lang.Thread.*;

import org.apache.logging.log4j.*;


public class AlbumUncaughtExceptionHandler implements UncaughtExceptionHandler
{
	///////////////////////////////////////////////////////////////////////////
	@Override
	public void uncaughtException (Thread thread, Throwable ex)
	{
		_log.error ("AlbumUncaughtExceptionHandler: ", ex);

		Thread.currentThread ().interrupt ();

//		System.err.println ("UncaughtException: " + thread.getName () + ": ");
//		ex.printStackTrace ();

//		System.exit (1);
	}

	private static Logger _log = LogManager.getLogger ();
}
