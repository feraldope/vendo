//AlbumImageServer.java

package com.vendo.albumServlet;

import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumImageServer implements ServletContextListener
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void contextDestroyed (ServletContextEvent servletContextEvent)
	{
		_log.info ("AlbumImageServer.contextDestroyed");

		AlbumImageDao.getInstance ().cleanup ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void contextInitialized (ServletContextEvent servletContextEvent)
	{
		_log.info ("AlbumImageServer.contextInitialized");

		ServletContext context = servletContextEvent.getServletContext ();

		if (false) {
			_log.debug ("AlbumImageServer.contextInitialized: contextPath: " + context.getContextPath ());

			Enumeration<String> attributeNames = context.getAttributeNames ();
			while (attributeNames.hasMoreElements ()) {
				String attributeName = attributeNames.nextElement ();
				String attributeValue = context.getAttribute (attributeName).toString ();
				_log.debug ("AlbumImageServer.contextInitialized: " + attributeName + " = " + attributeValue);
			}
		}

		Thread thread = new Thread () {
			public void run () {
				AlbumImageDao.main (new String[] {
									"/debug"
				});
			}
		};
		thread.start ();
	}


	//members
	private static Logger _log = LogManager.getLogger ();
//	private static final String _AppName = "AlbumImageServer";
}
