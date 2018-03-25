//VUncaughtExceptionHandler.java

package com.vendo.vendoUtils;

import java.lang.Thread.*;


public class VUncaughtExceptionHandler implements UncaughtExceptionHandler
{
	///////////////////////////////////////////////////////////////////////////
	@Override
	public void uncaughtException (Thread thread, Throwable ex)
	{
		System.err.println ("UncaughtException: " + thread.getName () + ": ");
		ex.printStackTrace ();

		Thread.currentThread ().interrupt ();
//		System.exit (1);
	}
}
