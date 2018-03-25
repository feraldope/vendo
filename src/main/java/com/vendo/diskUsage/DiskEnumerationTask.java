//DiskEnumerationTask.java

//Original from http://www.java2s.com/Tutorials/Java/Thread/How_to_use_Java_BlockingQueue_with_multiple_threads.htm

package com.vendo.diskUsage;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VendoUtils;
import com.vendo.win32.FileUtils;


class DiskEnumerationTask implements Runnable
{
	///////////////////////////////////////////////////////////////////////////
	public DiskEnumerationTask (BlockingQueue<Path> queue, Path rootFolder)
	{
		_queue = queue;
		_rootFolder = rootFolder;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void run ()
	{
		enumerate (_rootFolder);

		try {
			_queue.put (DiskUsage.DONE_MARKER);

		} catch (InterruptedException ex) {
			ex.printStackTrace ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//recursive method to enumerate folder and subfolders
	public void enumerate (Path folder)
	{
		if (DiskUsage._Debug) {
			_log.debug ("enumerate: " + folder);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream (folder)) {
			for (Path path : stream) {
				if (path.toFile ().isDirectory ()) {
					try {
						_queue.put (path); //will block if queue is full

					} catch (InterruptedException ex) {
						ex.printStackTrace ();
					}
					enumerate (path); //recurse
				}
			}

		} catch (IOException ee) {
			if (!DiskUsage._Quiet) { //optionally ignore failures
				String name = VendoUtils.getRealPathString (folder);
				if (FileUtils.isJunctionOrSymlink2 (folder)) {
					System.out.println ("----------  Skip Junction:  " + name);
				} else {
					System.out.println ("----------  Unable to read: " + name);
				}
			}
		}
	}


	//private members
	private BlockingQueue<Path> _queue;
	private Path _rootFolder;

	private static final Logger _log = LogManager.getLogger ();
}