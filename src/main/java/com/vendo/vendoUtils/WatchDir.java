//original from
// https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *	 notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *	 notice, this list of conditions and the following disclaimer in the
 *	 documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *	 contributors may be used to endorse or promote products derived
 *	 from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.vendo.vendoUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;


public abstract class WatchDir implements Runnable
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args) throws IOException
	{
		String destDir = null;
		String patternString = null;
		Pattern pattern = null;
		boolean recurseSubdirs = false;

		//processArgs
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_debug = true;

				} else if (arg.equalsIgnoreCase ("sleepMillis") || arg.equalsIgnoreCase ("sleep")) {
					try {
						_sleepMillis = Integer.parseInt (args[++ii]);
						if (_sleepMillis < 0) {
							throw (new NumberFormatException ());
						}
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("subdirs") || arg.equalsIgnoreCase ("s")) {
					recurseSubdirs = true;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (destDir == null) {
					destDir = arg;

				} else if (patternString == null) {
					patternString = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (destDir == null) {
			displayUsage ("Must specify dir to watch", true);
		}
		Path dir = Paths.get (destDir);
		System.out.println ("Watching " + getRealPathString (dir) + " ...");

		if (patternString == null) {
			patternString = "*";
		}

		patternString = patternString.replace ("*", ".*");
		pattern = Pattern.compile (patternString, Pattern.CASE_INSENSITIVE);

		WatchDir watchDir = new WatchDir (dir, pattern, recurseSubdirs)
		{
			@Override
			protected void notify (Path dir, WatchEvent<Path> pathEvent)
			{
				VendoUtils.sleepMillis (_sleepMillis);

				Path file = pathEvent.context ();
				Path path = dir.resolve (file);

				long nowInMillis = new GregorianCalendar ().getTimeInMillis ();
				String nowString = _dateFormat.format (new Date (nowInMillis));

				System.out.format ("%s: WatchDir.notify: %s: %s\n", nowString, pathEvent.kind ().name (), getRealPathString (path));
			}

			@Override
			protected void overflow (WatchEvent<?> event)
			{
				long nowInMillis = new GregorianCalendar ().getTimeInMillis ();
				String nowString = _dateFormat.format (new Date (nowInMillis));

				System.err.format ("%s: WatchDir.overflow: %s, count = %d\n", nowString, event.kind ().name (), event.count ());
//				System.out.format ("%s: WatchDir.overflow: %s, count = %d\n", nowString, event.kind ().name (), event.count ());
			}
		};

		Thread thread = new Thread (watchDir);
		thread.start ();

		try {
			thread.join ();
		} catch (Exception ee) {
			ee.printStackTrace ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	protected static void displayUsage (String message, Boolean exit)
	{
		String msg = "";
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/subdirs] [/sleepMillis <testing delay>] <dir to watch> [<file pattern>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	protected WatchDir (Path dir, Pattern pattern, boolean recurseSubdirs) throws IOException
	{
		_watcher = FileSystems.getDefault ().newWatchService ();
		_keys = new HashMap<WatchKey, Path> ();

		_pattern = pattern;
		_recurseSubdirs = recurseSubdirs;

		if (_recurseSubdirs) {
			System.out.format ("Scanning %s ...\n", dir);
			registerAll (dir);
			System.out.println ("Done.");

		} else {
			register (dir);
		}

		// enable trace after initial registration
		_trace = true;
	}

	///////////////////////////////////////////////////////////////////////////
	protected <T> WatchEvent<T> asWatchEvent (WatchEvent<?> event) //cast
	{
		return event != null ? (WatchEvent<T>) event : null;
	}

	///////////////////////////////////////////////////////////////////////////
	// Register the given directory with the WatchService
	protected void register (Path dir) throws IOException
	{
		WatchKey key = dir.register (_watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		if (_trace) {
			Path prev = _keys.get (key);
			if (prev == null) {
				System.out.format ("register: %s\n", dir);

			} else {
				if (!dir.equals (prev)) {
					System.out.format ("update: %s -> %s\n", prev, dir);
				}
			}
		}
		_keys.put (key, dir);
	}

	///////////////////////////////////////////////////////////////////////////
	// Register the given directory, and all its sub-directories, with the WatchService.
	protected void registerAll (final Path start) throws IOException
	{
		// register directory and sub-directories
		Files.walkFileTree (start, new SimpleFileVisitor<Path> () {
			@Override
			public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) throws IOException {
				register (dir);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void run ()
	{
		processEvents ();
	}

	///////////////////////////////////////////////////////////////////////////
	// Process all events for keys queued to the watcher
	protected void processEvents ()
	{
		while (true) {
			// wait for key to be signalled
			WatchKey key;
			try {
				key = _watcher.take ();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = _keys.get (key);
			if (dir == null) {
				System.err.println ("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents ()) {
				WatchEvent.Kind<?> kind = event.kind ();

				if (kind == OVERFLOW) {
					overflow (event);
					continue;
				}

				WatchEvent<Path> pathEvent = asWatchEvent (event);
				String filename = pathEvent.context ().toString ();
				Matcher matcher = _pattern.matcher (filename);
				if (matcher.matches ()) {
					notify (dir, pathEvent);
				}

				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if (_recurseSubdirs && (kind == ENTRY_CREATE)) {
					Path file = pathEvent.context ();
					Path child = dir.resolve (file);

					try {
						if (Files.isDirectory (child, NOFOLLOW_LINKS)) {
							registerAll (child);
						}

					} catch (IOException ex) {
						ex.printStackTrace ();
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset ();
			if (!valid) {
				_keys.remove (key);

				// all directories are inaccessible
				if (_keys.isEmpty ()) {
					break;
				}
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// Override this in inheriting class
	protected abstract void notify (Path dir, WatchEvent<Path> pathEvent);

	///////////////////////////////////////////////////////////////////////////
	// Override this in inheriting class
	protected abstract void overflow (WatchEvent<?> event);

	///////////////////////////////////////////////////////////////////////////
	private static String getRealPathString (Path path)
	{
		try {
			return path.toRealPath ().toString ();

		} catch (IOException ee) {
//ignore this: if event is ENTRY_DELETE then call to toRealPath() will throw an exception
//			ee.printStackTrace ();
		}

//		return path.toAbsolutePath ().toString ();
		return path.normalize ().toString ();
	}


	//private members
	protected boolean _recurseSubdirs = false;
	protected Pattern _pattern = null;

	protected final WatchService _watcher;
	protected final Map<WatchKey, Path> _keys;
	protected boolean _trace = false;
	protected static boolean _debug = false;

	protected static int _sleepMillis = 0; //add artificial delay to notify() processing; attempt to trigger OVERFLOW event

	protected static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm:ss");

	//global members
	public static final String _AppName = "WatchDir";
	public static final String NL = System.getProperty ("line.separator");
}
