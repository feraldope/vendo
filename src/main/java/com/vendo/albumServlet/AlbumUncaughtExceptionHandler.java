//AlbumUncaughtExceptionHandler.java

package com.vendo.albumServlet;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VendoUtils;


public class AlbumUncaughtExceptionHandler implements UncaughtExceptionHandler
{
	///////////////////////////////////////////////////////////////////////////
	@Override
	public void uncaughtException (Thread thread, Throwable ex)
	{
		_log.error ("AlbumUncaughtExceptionHandler in thread " + thread.getName () + ": ", ex);
		//the previous line should print the stack trace, but the JVM might swallow it, so force it:
		_log.error (VendoUtils.getStackTrace (ex));

		Thread.currentThread ().interrupt ();
//		System.exit (1);
	}

	private static Logger _log = LogManager.getLogger ();
}
