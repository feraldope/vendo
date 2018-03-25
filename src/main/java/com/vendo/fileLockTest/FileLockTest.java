/* Original inspiration from
https://stackoverflow.com/questions/128038/how-can-i-lock-a-file-using-java-if-possible
https://examples.javacodegeeks.com/core-java/nio/channels/filelock-channels/java-nio-channels-filelock-example/

Usage:
start one process as master and then start one or more as slaves
*/

package com.vendo.fileLockTest;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VendoUtils;


public class FileLockTest
{
	private enum Mode {NotSet, Master, Slave};

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		FileLockTest app = new FileLockTest ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

/*
				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("thresholdPct") || arg.equalsIgnoreCase ("t")) {
					try {
						_thresholdPercent = Double.parseDouble (args[++ii]);
						if (_thresholdPercent < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}
*/
				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (arg.equalsIgnoreCase ("master")) {
					_mode = Mode.Master;

				} else if (arg.equalsIgnoreCase ("slave")) {
					_mode = Mode.Slave;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_mode == Mode.NotSet) {
			displayUsage ("No action specified", true);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] {master | slave}";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		if (_Debug) {
			_log.debug ("FileLockTest.run: mode: " + _mode);
		}

		FileChannel fileChannel = null;
		try {
			Path path = Paths.get (_filename);
			fileChannel = FileChannel.open (path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);

		} catch (Exception ee) {
			_log.error ("FileLockTest.run: failed to get channel on file: " + _filename);
			_log.error (ee);
			return false;
		}

		if (_mode == Mode.Master) {
/* currently unused, but keep as example
			try {
				Integer processId = ProcessUtils.getWin32ProcessId ();
				String processIdStr = String.valueOf (processId);
				ByteBuffer buffer = ByteBuffer.allocate (2 * processIdStr.length ());
				buffer.asCharBuffer ().put (processIdStr); //written as unicode
//				buffer.rewind (); //always rewind before using
				fileChannel.write (buffer);
				fileChannel.force (true);

			} catch (Exception ee) {
				_log.error ("FileLockTest.run: failed to write buffer to file: " + _filename);
				_log.error (ee);
				ee.printStackTrace ();
				return false;
			}
*/

			while (true) {
				FileLock lock = null;

				try {
					Instant startInstant = Instant.now ();
					lock = fileChannel.lock (); //BLOCKING!!!
					long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();
					_log.debug ("File lock acquired: " + lock + ", blocked time: " + LocalTime.ofNanoOfDay (elapsedNanos));


				} catch (Exception ee) {
					_log.error ("FileLockTest.run: failed to acquire lock on file: " + _filename);
					_log.error (ee);
				}

				VendoUtils.sleepMillis (_masterLockedSleepMillis);

				if (lock != null) {
					try {
						lock.release ();
						_log.debug ("File lock released");

					} catch (Exception ee) {
						_log.error ("FileLockTest.run: failed to release lock on file: " + _filename);
						_log.error (ee);
					}
				}

				VendoUtils.sleepMillis (_masterReleasedSleepMillis);
			}

//Note: the slave can block the master, which is not desired

		} else if (_mode == Mode.Slave) {
			while (true) {
				FileLock lock = null;

				try {
					lock = fileChannel.tryLock (); //non-blocking
					//note THIS line will throw NullPointerException if it tries to dereference "lock" and the lock was not acquired:
					_log.debug ("File lock acquired: " + lock);// + " (valid = " + lock.isValid () + ", shared = " + lock.isShared () + ")");

				} catch (Exception ee) {
					_log.error ("FileLockTest.run: failed to acquire lock on file: " + _filename);
					_log.error (ee);
				}

				VendoUtils.sleepMillis (_slaveLockedSleepMillis);

				if (lock != null) {
					try {
						lock.release ();
						_log.debug ("File lock released");

					} catch (Exception ee) {
						_log.error ("FileLockTest.run: failed to release lock on file: " + _filename);
						_log.error (ee);
					}
				}

				VendoUtils.sleepMillis (_slaveReleasedSleepMillis);
			}
		}

		return true;
	}


	//private members
	private Mode _mode = Mode.NotSet;
	private String _filename = "C:/Users/java/FileLockTest/file.lck";
	private final int _masterLockedSleepMillis = 5 * 1000;
	private final int _masterReleasedSleepMillis = 2 * 1000;
	private final int _slaveLockedSleepMillis = 100;
	private final int _slaveReleasedSleepMillis = 250;

	private static Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = true;//false;

	public static final String _AppName = "FileLockTest";
	public static final String NL = System.getProperty ("line.separator");
}
