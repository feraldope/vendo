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

import com.vendo.vendoUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.logging.log4j.*;

/**
 * @version 1.0 2004-08-01
 * @author Cay Horstmann
 */

public class Search1
{
	public static void main (String[] args)
	{
		String userName = System.getProperty ("user.name");
//		String directory = "C:/users/" + userName;
		String directory = "C:/users/java";
//		String directory = "C:/";
		String keyword = "Java";

		_log.debug ("directory: " + directory);
		_log.debug ("keyword: " + keyword);

		final int FILE_QUEUE_SIZE = 10;
		final int SEARCH_THREADS = 50;

		BlockingQueue<File> queue = new ArrayBlockingQueue<File> (FILE_QUEUE_SIZE);

		FileEnumerationTask enumerator = new FileEnumerationTask (queue, new File (directory));
		new Thread (enumerator).start ();

		for (int i = 1; i <= SEARCH_THREADS; i++) {
			new Thread (new SearchTask (queue, keyword)).start ();
		}

		//debugging
		if (false) {
			List<String> threadDetails = new ArrayList<String> ();
			VendoUtils.listAllThreads (threadDetails);
			for (String threadDetail : threadDetails) {
				System.out.println (threadDetail);
			}
		}
	}

	public static Logger _log = LogManager.getLogger (Search1.class);
	public static boolean Trace = true;
}
