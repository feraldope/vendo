//AlbumTagServer.java

package com.vendo.albumServlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumTagServer implements ServletContextListener
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
		_log.info ("AlbumTagServer.contextDestroyed");
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void contextInitialized (ServletContextEvent servletContextEvent)
	{
//		ServletContext context = servletContextEvent.getServletContext ();
		_log.info ("AlbumTagServer.contextInitialized");

		Thread thread = new Thread () {
			public void run () {
				AlbumTags.main (new String[] {
									"/tagFile", "E:/Netscape/Program/todo.dat",
									"/continuous",
//									"/checkForOrphans",
//									"/resetTables",
									"/debug"
				});
			}
		};
		thread.start ();
	}


	//members
	private static Logger _log = LogManager.getLogger ();
//	private static final String _AppName = "AlbumTagServer";
}
