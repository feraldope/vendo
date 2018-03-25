//VLogger.java

package com.vendo.vendoUtils;

//import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

import org.apache.logging.log4j.*;


public class VLogger
{
	//create singleton instance
	public synchronized static VLogger instance ()
	{
		if (_instance == null)
			_instance = new VLogger ();

		return _instance;
	}

	public static void trace ()
	{
		if (!isDebugLoggingEnabled ())
			return;

		String caller = getCallerName (new Throwable ());
		logMessage (caller, "");
	}

	public static void debug (String message)
	{
		if (!isDebugLoggingEnabled ())
			return;

		String caller = getCallerName (new Throwable ());
		logMessage (caller, message);
	}

	public static void error (String message)
	{
		String caller = getCallerName (new Throwable ());
		logMessage (caller, "Error: " + message);
	}

	public static void error (Throwable throwable)
	{
		StringBuffer sb = new StringBuffer ();
		getExceptionDebug (throwable, sb);
		String caller = getCallerName (new Throwable ());
		logMessage (caller, "Exception:" + NL + sb.toString ());
	}

	//original from RptCtrLogFormatter.java
	private static void getExceptionDebug (Throwable exception, StringBuffer sb)
	{
		Throwable cause = null;
		String causeTag = "";
		while (exception != null) {
			sb.append (_tab)
			  .append (causeTag)
			  .append (exception.toString ())
			  .append (NL);

			cause = exception.getCause ();

			if (cause == null)
				getStackDebug (exception, sb);

			causeTag = "Caused by: ";
			exception = cause;
		}
	}

	//original from RptCtrLogFormatter.java
	private static void getStackDebug (Throwable exception, StringBuffer sb)
	{
		if (exception != null) {
			sb.append (_tab)
			  .append (exception.getClass ())
			  .append (": ")
			  .append (exception.getMessage ())
			  .append (NL);

			StackTraceElement [] stack = exception.getStackTrace ();
			for (int ii = 0; ii < stack.length; ii++) {
				sb.append (_tab)
				  .append (_tab)
				  .append ("at ")
				  .append (stack[ii].toString ())
				  .append (NL);
			}
		}
	}

	private static String getCallerName (Throwable throwable)
	{
		StackTraceElement[] stack = throwable.getStackTrace ();
		String line = stack[1].toString ();

		try {
			//string has the following format; extract the class and method name
			//com.concord.apps.reporting.config.RptCtrListUsers.connectToDatabase(RptCtrListUsers.java:339)
			String[] components = line.split ("\\(")[0].split ("\\.");
			int method = components.length - 1;
			line = components[method - 1] + "." + components[method];

		} catch (Exception ee) {
			//if our string processing failed, ignore and fall through and just return entire line
		}

		return line;
	}

	private static void logMessage (String caller, String message)
	{
		final SimpleDateFormat dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

		StringBuffer sb = new StringBuffer ("[")
		  .append (dateFormat.format (new Date ()))
		  .append (" ")
		  .append (caller)
		  .append ("]");

		if (message != null && message.length () > 0) {
			sb.append (" ")
			.append (message);
		}

		_logger.debug (sb.toString ());
	}

	private static boolean isDebugLoggingEnabled ()
	{
		//force call to singleton ctor
		return instance ()._debugLoggingEnabled;
	}

	private VLogger ()
	{
		//determine if debug logging is enabled
//		String level = PropertyFactory.instance().getProperty(PropertyFactory.LOGGING_LEVEL);
//		if (level != null && level.compareTo ("debug") == 0)
			_debugLoggingEnabled = true;
	}

	private boolean _debugLoggingEnabled = false;

	private static final String _tab = "    ";
	private static final String NL = System.getProperty ("line.separator");

	private static VLogger _instance = null;
	private static final Logger _logger = LogManager.getLogger ("vlogger");
}
