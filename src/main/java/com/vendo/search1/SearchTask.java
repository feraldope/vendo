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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

/**
 * @version 1.0 2004-08-01
 * @author Cay Horstmann
 */

/**
 * This task searches files for a given keyword.
 */
public class SearchTask implements Runnable
{
	/**
	 * Constructs a SearchTask.
	 * @param queue the queue from which to take files
	 * @param keyword the keyword to look for
	 */
	public SearchTask (BlockingQueue<File> queue, String keyword)
	{
		this.queue = queue;
		this.keyword = keyword;
	}

	public void run ()
	{
		try {
			boolean done = false;
			while (!done) {
				File file = queue.take ();
				if (file == FileEnumerationTask.DONE_MARKER) {
					queue.put (file); //put back in queue for other threads
					done = true;
				} else {
//					Search1._log.debug ("search: " + file);
					search (file);
				}
			}

		} catch (IOException ex) {
			ex.printStackTrace ();

		} catch (InterruptedException ex) {
			ex.printStackTrace ();
		}
	}

	/**
	 * Searches a file for a given keyword and prints all matching lines.
	 * @param file the file to search
	 */
	public void search (File file) throws IOException
	{
		Scanner in = null;
		try {
			in = new Scanner (new FileInputStream (file));

		} catch (FileNotFoundException ex) {
//try to recognize files that are locked, or no premission
			Search1._log.debug ("search: exception on: " + file + ", cause: " + ex.getCause ());
//			ex.printStackTrace ();
			return;
		}

		int lineNumber = 0;
		while (in.hasNextLine ()) {
			lineNumber++;
			String line = in.nextLine ();

			if (!line.matches (asciiRegex)) {
//				System.out.printf ("%s:%d: Skipping non-ASCII file\n", file.getPath (), lineNumber);
				break; //skip rest of file
			}

			if (line.contains (keyword)) {
				System.out.printf ("%s:%d:%s\n", file.getPath (), lineNumber, line);
				break; //stop after first match
			}
		}
		in.close ();
	}

	//http://stackoverflow.com/questions/3585053/in-java-is-it-possible-to-check-if-a-string-is-only-ascii
	private String asciiRegex = "\\A\\p{ASCII}*\\z"; //regex = \\A - Beginning of input ... \\p{ASCII}* - Any ASCII character any times ...\\z - End of input

	private BlockingQueue<File> queue;
	private String keyword;
}
