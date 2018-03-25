//Listener.java

package com.vendo.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VendoUtils;


public class Listener
{
	private enum Mode {NotSet, Client, Server};

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		Listener app = new Listener ();

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
				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("client") || arg.equalsIgnoreCase ("c")) {
					_mode = Mode.Client;
					try {
						_firstPort = Integer.parseInt (args[++ii]);
						if (_firstPort < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("server") || arg.equalsIgnoreCase ("s")) {
					_mode = Mode.Server;
					try {
						_firstPort = Integer.parseInt (args[++ii]);
						_lastPort = Integer.parseInt (args[++ii]);
						if (_firstPort < 0 || _lastPort < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (inputFile == null) {
					inputFile = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		if (_Debug) {
			for (int ii = 0; ii < args.length; ii++) {
				String arg = args[ii];
				_log.debug ("processArgs: arg = '" + arg + "'");
			}
		}

/*
		//check for required args, set defaults
*/
		
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/client <port>] [/server <firstPort> <lastPort>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		if (_mode == Mode.Server) {
			server ();

//			while (true) {
				//Note this only shows the memory used by our java process,
				//it does not include the system memory used by all of the open ports
//				_log.debug ("Listener.run: " + VendoUtils.getMemoryStatistics ());
//				VendoUtils.sleepMillis (10000); //ms
//			}

		} else {
			client ();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean client ()
	{
		_log.debug ("client starting on port " + _firstPort);

		try (Socket socket = new Socket (_localhost, _firstPort);
			BufferedReader in = new BufferedReader (new InputStreamReader (socket.getInputStream ()))) {
			PrintWriter out = new PrintWriter (socket.getOutputStream (), true);
			out.println ("open");
			out.println ("close");
//

		} catch (IOException ex) {
//			_log.debug ("Failed to open/write socket on port " + _firstPort + ", will retry");
			ex.printStackTrace ();
		}
		
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean server ()
	{
//TODO - need a way to shut this down (cleanly?)
//TODO - read from properties file:
//	ports to skip (e.g., 1158)
//	port definitions

		_log.debug ("server: listening on ports " + _firstPort + " through " + _lastPort);

		int numPorts = _lastPort - _firstPort + 1;
		final CountDownLatch endGate = new CountDownLatch (numPorts);

		for (int ii = _firstPort; ii <= _lastPort; ii++) {
			final int port = ii;
			Thread thread = new Thread () {
				public void run () {
					ServerSocket serverSocket = null;
					while (true) {
						try {
							serverSocket = new ServerSocket (port);
							break;

						} catch (BindException ex) {
							_log.debug ("Port " + port + ": already in use, exiting thread");
							return;

						} catch (IOException ex) {
							_log.debug ("Port " + port + ": failed to open socket");
							ex.printStackTrace ();
							return;

						} finally {
							try {
								serverSocket.close ();
							} catch (IOException ex) {
								//ignore
							}
							endGate.countDown ();
						}
					}

					boolean firstTime = true;
					while (true) {
						Socket socket = null;
						try {
							if (firstTime) {
								firstTime = false;
							} else {
								_log.debug ("Port " + port + ": listening...");
							}

							socket = serverSocket.accept ();

						} catch (Exception ex) {
							_log.debug ("Port " + port + ": exception opening socket: " + ex.getMessage ());
							ex.printStackTrace ();
						}

						if (socket != null) {
							InetAddress remoteAddress = asInetSocketAddress (socket.getRemoteSocketAddress ()).getAddress ();
							_log.debug ("Port " + port + ": accepted connection from " + remoteAddress);

							try {
								BufferedReader in = new BufferedReader (new InputStreamReader (socket.getInputStream ()));

								String inputLine;
								_log.debug ("Port " + port + ": waiting for data");
								while ((inputLine = in.readLine()) != null) {
									_log.debug ("Port " + port + ": read: [" + inputLine + "]");
									_log.debug ("Port " + port + ": read: [" + VendoUtils.toHexString (inputLine) + "]");
								}
								_log.debug ("Port " + port + ": EOF");

							} catch (IOException ex) {
								_log.debug ("Port " + port + ": exception reading socket: " + ex.getMessage ());
//								ex.printStackTrace ();
							}
						}
					}
				}
			};
			thread.setName ("PortMonitor-" + port);
			thread.start ();
		}

		//wait for all threads to start before returning
		try {
			_log.debug ("Waiting for all threads to start...");
			endGate.await ();
			_log.debug ("Waiting for all threads to start... done");
		} catch (Exception ex) {
			ex.printStackTrace ();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	@SuppressWarnings ("unchecked")
	private InetSocketAddress asInetSocketAddress (SocketAddress address) //cast
	{
		return address instanceof InetSocketAddress ? (InetSocketAddress) address : null;
	}


	//private members
	private Mode _mode = Mode.Server;
	private int _firstPort = 1;
	private int _lastPort = 199;

	private static final String _localhost = "127.0.0.1";
	private static final String NL = System.getProperty ("line.separator");

	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "Listener";
}
