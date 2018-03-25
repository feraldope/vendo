//Original from http://www.java2s.com/Tutorials/Java/Thread/How_to_use_Java_BlockingQueue_with_multiple_threads.htm

/*
This program is a part of the companion code for Core Java 8th ed.
(http://horstmann.com/corejava)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vendo.search1;

import java.io.File;
import java.util.concurrent.BlockingQueue;

/**
 * @version 1.0 2004-08-01
 * @author Cay Horstmann
 */

/**
 * This task enumerates all files in a directory and its subdirectories.
 */
public class FileEnumerationTask implements Runnable
{
	/**
	 * Constructs a FileEnumerationTask.
	 * @param queue the blocking queue to which the enumerated files are added
	 * @param startingDirectory the directory in which to start the enumeration
	 */
	public FileEnumerationTask (BlockingQueue<File> queue, File startingDirectory)
	{
		this.queue = queue;
		this.startingDirectory = startingDirectory;
	}

	public void run ()
	{
		try {
			enumerate (startingDirectory);
			queue.put (DONE_MARKER);

		} catch (InterruptedException ex) {
			ex.printStackTrace ();
		}
	}

	/**
	 * Recursively enumerates all files in a given directory and its subdirectories
	 * @param directory the directory in which to start
	 */
	public void enumerate (File directory) throws InterruptedException
	{
		File[] files = directory.listFiles ();

		if (files == null) { //possibly a junction
			Search1._log.debug ("directory: " + directory + ": files is null");
		} else {

		for (File file : files) {
			if (file.isDirectory ()) {
//				Search1._log.debug ("enumerate: " + file);
				enumerate (file);
			} else {
				queue.put (file);
			}
		}
		}
	}

	public static File DONE_MARKER = new File (""); //used to indicate last entry

	private BlockingQueue<File> queue;
	private File startingDirectory;
}
