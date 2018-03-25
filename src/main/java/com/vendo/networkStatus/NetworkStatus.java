//NetworkStatus.java

//Useful information collected from:
// http://stackoverflow.com/questions/9286861/get-ip-address-with-url-string-java
// http://stackoverflow.com/questions/11506321/java-code-to-ping-an-ip-address
// http://stackoverflow.com/questions/11930/how-can-i-determine-the-ip-of-my-router-gateway-in-java

//Usage:
//jr https://www.google.com/


/*
suggested:
netstat -rn

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        BufferedReader buffer = null;
        try {
            URL url = new URL("http://www.whatismyip.com/tools/ip-address-lookup.asp");
            InputStreamReader in = new InputStreamReader(url.openStream());
            buffer = new BufferedReader(in);
            String line = buffer.readLine();
            Pattern pattern = Pattern.compile("(.*)value=\"(\\d+).(\\d+).(\\d+).(\\d+)\"(.*)");
            Matcher matcher;
            while (line != null) {
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    line = matcher.group(2) + "." + matcher.group(3) + "." + matcher.group(4) + "." + matcher.group(5);
                    System.out.println(line);
                }
                line = buffer.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                if (buffer != null) {
                    buffer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
*/

package com.vendo.networkStatus;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class NetworkStatus
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		NetworkStatus app = new NetworkStatus ();

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

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_model == null) {
					_model = arg;

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_model == null) {
			_model = "https://www.google.com/";
//			displayUsage ("No URI specified", true);
		}
		_model = collapseSlashes (_model);

/*
//TODO - verify _destDir exists, and is writable??
		if (_destDir == null)
			_destDir = getCurrentDirectory ();
		_destDir = appendSlash (_destDir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);

		if (_startIndex >= 0 && _endIndex >= 0)
			if (_startIndex > _endIndex)
				displayUsage ("<start index> (" + _startIndex + ") + cannot be greater than <end index> (" + _endIndex + ")", true);

		if (_step < 0 && (_startIndex > 0 || _endIndex > 0))
			_step = 1;

		if (_prefix == null)
			_prefix = "";
		else if (!validFileChars (_prefix))
			displayUsage ("Invalid value for <file prefix> '" + _prefix + "'", true);

		if (_exten == null)
			_exten = ".htm";
		else if (!validFileChars (_exten))
			displayUsage ("Invalid value for <file extension> '" + _exten + "'", true);
*/

		if (false) { //dump all args
			_log.debug ("args.length = " + args.length);
			for (int ii = 0; ii < args.length; ii++)
				_log.debug ("args[" + ii + "] = '" + args[ii] + "'");
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " <URL>";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		if (_Debug)
			_log.debug ("NetworkStatus.run");

//		if (!parseModel ())
//			return false;

//		buildStrings ();

		_urlStr = _model;

		try {
			_log.debug ("URL: " + _urlStr);
			URL url = new URL (_urlStr);

			String host = url.getHost ();
			_log.debug ("host: " + host);

			InetAddress inetAddress = InetAddress.getByName (host);
			_log.debug ("inetAddress: " + inetAddress);

			String hostAddress = inetAddress.getHostAddress ();
			_log.debug ("hostAddress: " + hostAddress);

			boolean isReachable = inetAddress.isReachable (4000);
			_log.debug ("isReachable: " + isReachable);

			URLConnection conn = url.openConnection ();
			String contentType = conn.getContentType ();
			_log.debug ("contentType: " + contentType);

		} catch (Exception ee) {
//TBD
			_log.error ("error in run()");
			_log.error (ee); //print exception, but no stack trace
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//collapse multiple slashes into single slash (except for header "http://")
	private String collapseSlashes (String url)
	{
		String header = "http://";

		int headerIndex = url.indexOf (header);
		if (headerIndex < 0)
			return url;

		int endOfHeader = headerIndex + header.length ();

		int slashesIndex = url.lastIndexOf ("//");

		if (slashesIndex > endOfHeader) {
//			System.out.println ("collapseSlashes: url = '" + url + "'");
			while (slashesIndex > endOfHeader) {
				url = url.replaceAll ("//", "/");
				slashesIndex = url.lastIndexOf ("//");
			}

			url = url.replaceFirst ("/", "//");
//			System.out.println ("collapseSlashes: url = '" + url + "'");
		}

		return url;
	}


	//private members
	private String _model = null;
	private String _urlStr = null;

	private static Logger _log = LogManager.getLogger (NetworkStatus.class);

	//global members
	public static boolean _Debug = false;
	public static boolean _TestMode = false;

	public static final String _AppName = "NetworkStatus";
	public static final String NL = System.getProperty ("line.separator");
}
