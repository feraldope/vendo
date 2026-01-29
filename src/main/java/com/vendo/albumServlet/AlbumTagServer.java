//AlbumTagServer.java

package com.vendo.albumServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class AlbumTagServer implements ServletContextListener
{
	///////////////////////////////////////////////////////////////////////////
	static {
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void contextDestroyed (ServletContextEvent servletContextEvent) {
		_log.info ("AlbumTagServer.contextDestroyed");
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void contextInitialized (ServletContextEvent servletContextEvent) {
//		ServletContext context = servletContextEvent.getServletContext ();
		_log.info ("AlbumTagServer.contextInitialized");

		new Thread (new Runnable() {
			@Override
			public void run() {
				AlbumTags.main(new String[]{
								"/tagFile", "D:/Netscape/Program/todo.dat",
								"/continuous",
								"/startupDelayInSeconds", "150",  //HACK - try to give AlbumImageDao some time to finish starting
//								"/checkForOrphans",
//								"/resetTables",
								"/debug"
				});
			}
		}).start ();
	}


	//members
	private static final Logger _log = LogManager.getLogger ();
//	private static final String _AppName = "AlbumTagServer";
}
