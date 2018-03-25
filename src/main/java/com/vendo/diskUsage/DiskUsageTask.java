//DiskUsageTask.java

//Original inspirations from
//http://www.java2s.com/Tutorials/Java/Thread/How_to_use_Java_BlockingQueue_with_multiple_threads.htm
//http://stackoverflow.com/questions/2149785/get-size-of-folder-or-file

package com.vendo.diskUsage;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VendoUtils;


class DiskUsageTask implements Runnable
{
	///////////////////////////////////////////////////////////////////////////
	public DiskUsageTask (BlockingQueue<Path> queue, long threshold, AtomicInteger numMatchingFolders)
	{
		_queue = queue;
		_threshold = threshold;
		_numMatchingFolders = numMatchingFolders;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void run ()
	{
		try {
			boolean done = false;
			while (!done) {
				Path folder = _queue.take (); //will block if queue is empty
				if (folder == DiskUsage.DONE_MARKER) {
					_queue.put (folder); //put done marker back in queue for other threads
					done = true;

				} else {
					if (DiskUsage._Debug) {
						_log.debug ("process: " + folder);
					}

					long size = size (folder);
					if (size >= _threshold) {
						_numMatchingFolders.getAndIncrement ();
						System.out.println (VendoUtils.unitSuffixScale (size, _fieldWidth) + "  " + VendoUtils.getRealPathString (folder));
					}
				}
			}

		} catch (InterruptedException ex) {
			ex.printStackTrace ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public long size (Path folder)
	{
		final AtomicLong size = new AtomicLong (0); //accumulate size of all files in folder and its subfolders

		try {
			Files.walkFileTree (folder, new SimpleFileVisitor<Path> () {
				@Override
				public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
				{
					size.addAndGet (attrs.size ());
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed (Path file, IOException ex)
				{
//System.out.println ("----------  visitFileFailed: " + file);
/* we should not need to print these here, since they will be caught by the enumerate step
					if (!DiskUsage._Quiet) { //optionally ignore failures
						String name = VendoUtils.getRealPathString (file);
						if (FileUtils.isJunctionOrSymlink2 (folder)) {
//							System.out.println ("----------  Skip Junction:  " + name + " *");
						} else {
							System.out.println ("----------  Unable to read: " + name + " *");
						}
					}
*/
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory (Path dir, IOException ex)
				{
					if (ex != null) {
						ex.printStackTrace ();
//						System.out.println ("had trouble traversing: " + dir + " (" + ex + ")");
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (IOException ex) {
			throw new AssertionError ("Files#walkFileTree will not throw IOException if the FileVisitor does not");
		}

		return size.get ();
	}


	//private members
	private BlockingQueue<Path> _queue;
	private long _threshold;
	private AtomicInteger _numMatchingFolders;
	private int _fieldWidth = 10;

	private static final Logger _log = LogManager.getLogger ();
}