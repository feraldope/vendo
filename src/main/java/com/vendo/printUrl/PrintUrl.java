//Original inspiration from http://docs.oracle.com/javase/tutorial/networking/urls/readingWriting.html

//see also:
// http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests

/*
file:///C:/Program%20Files/Java/jdk-8u60-docs/docs/api/index.html

Identities

For any URI u, it is always the case that

    new URI(u.toString()).equals(u) . 

For any URI u that does not contain redundant syntax such as two slashes before an empty authority (as in file:///tmp/ )
or a colon following a host name but no port (as in http://java.sun.com: ), and that does not encode characters except 
those that must be quoted, the following identities also hold:

     new URI(u.getScheme(),
             u.getSchemeSpecificPart(),
             u.getFragment())
     .equals(u)

in all cases,

     new URI(u.getScheme(),
             u.getUserInfo(), u.getAuthority(),
             u.getPath(), u.getQuery(),
             u.getFragment())
     .equals(u)

if u is hierarchical, and

     new URI(u.getScheme(),
             u.getUserInfo(), u.getHost(), u.getPort(),
             u.getPath(), u.getQuery(),
             u.getFragment())
     .equals(u)

if u is hierarchical and has either no authority or a server-based authority. 
*/

package com.vendo.printUrl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import com.vendo.vendoUtils.VendoUtils;


public class PrintUrl
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		PrintUrl app = new PrintUrl ();

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
					_debug = true;

				} else if (arg.equalsIgnoreCase ("verbose") || arg.equalsIgnoreCase ("v")) {
					_verbose = true;

				} else if (arg.equalsIgnoreCase ("checkOnly") || arg.equalsIgnoreCase ("co")) {
					_checkOnly = true;

				} else if (arg.equalsIgnoreCase ("timeout") || arg.equalsIgnoreCase ("to")) {
					try {
						_timeoutSeconds = Integer.parseInt (args[++ii]);
						if (_timeoutSeconds < 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

/*
				} else if (arg.equalsIgnoreCase ("destDir") || arg.equalsIgnoreCase ("dest")) {
					try {
						_destDir = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

*/
				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_urlString == null) {
					_urlString = arg;

/*
				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

*/
				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}
			}
		}

		//check for required args and handle defaults
		if (_urlString == null) {
			displayUsage ("Must specify URL", true);

/*
		} else {
			if (_model == null)
				displayUsage ("No URL specified", true);
			_model = normalizeUrl (_model);
*/
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " TBD"; //" [/debug] [/sleep(millis)] [/strip] [/ignore] [/checkOnly] [/fromFile <file>] [/dest <dest dir>] [/start <start index>] [/block <block number>] [/maxMissedFiles <count>] [/digits <number>] [/pad <number>] [/prefix <numberPrefix>] <URL> <output prefix>";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		try {
			printUrl ();

		} catch (Exception ee) {
			System.err.println (ee);
			if (_verbose) {
				ee.printStackTrace (System.err);
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean printUrl () throws Exception
	{
//		String string = "http://ricda13wd01.ca.com/cgi-bin/nhWeb?func=getList&listType=clientversion&contents=6.3.0.0.0";
//		String string = "http://localhost/servlet/coreservlets.ShowRequestHeaders";

		if (_verbose)
			System.err.println ("_urlString = '" + _urlString + "'");

		//construct URI, which will decode any escaped chars in string
		URI uri = new URI (_urlString);

		if (_verbose) {
			System.err.println ("uri.getScheme () = '" + uri.getScheme () + "'");
			System.err.println ("uri.getSchemeSpecificPart () = '" + uri.getSchemeSpecificPart () + "'");
			System.err.println ("uri.getHost () = '" + uri.getHost () + "'");
			System.err.println ("uri.getPort () = '" + uri.getPort () + "'");
			System.err.println ("uri.getUserInfo () = '" + uri.getUserInfo () + "'");
			System.err.println ("uri.getAuthority () = '" + uri.getAuthority () + "'");
			System.err.println ("uri.getPath () = '" + uri.getPath () + "'");
			System.err.println ("uri.getQuery () = '" + uri.getQuery () + "'");
			System.err.println ("uri.getFragment () = '" + uri.getFragment () + "'");
		}

		//reassemble URL string from components
		String urlString = new String ();
		if (uri.getScheme () != null)
			urlString += uri.getScheme () + "://";
		if (uri.getUserInfo () != null)
			urlString += uri.getUserInfo ();
		if (uri.getAuthority () != null)
			urlString += uri.getAuthority ();
		if (uri.getPath () != null)
			urlString += uri.getPath ();
		if (uri.getQuery  () != null)
			urlString += "?" + uri.getQuery ();
		if (uri.getFragment () != null)
			urlString += "#" + uri.getFragment ();

		if (_verbose)
			System.err.println ("urlString = '" + urlString + "'");

		URL url = new URL (urlString);

		if (_debug)
			printProperties ();

		if (_verbose)
			System.err.println ("url = '" + url + "'");

		if (_verbose)
			System.err.println ("calling: url.openConnection");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection ();

//note when running this program against http://localhost/servlet/coreservlets.ShowRequestHeaders it returns "Java/1.6.0_29" when no "User-Agent" is set
		if (true) {
			String userAgent = VendoUtils.getUserAgent (true);

			if (_verbose)
				System.err.println ("calling: conn.setRequestProperty");
			conn.setRequestProperty ("User-Agent", userAgent);
		}

		if (_verbose)
			System.err.println ("calling: conn.setConnectTimeout");
		conn.setConnectTimeout (_timeoutSeconds * 1000); //milliseconds

		if (_verbose)
			System.err.println ("calling: conn.getInputStream");

		InputStream inputStream = null;
		try {
			inputStream = conn.getInputStream ();

		} catch (Exception ee) {
//			System.err.println ("Error " + conn.getResponseCode () + " = " + conn.getResponseMessage ());
			System.err.println (ee);
		}

		System.err.println ("HttpURLConnection response: " + conn.getResponseCode () + " = " + conn.getResponseMessage ());

		if (_verbose)
			System.err.println ("calling: 'new InputStreamReader'");
		InputStreamReader in1 = new InputStreamReader (inputStream);

		if (_verbose)
			System.err.println ("calling: 'new BufferedReader'");
		BufferedReader in2 = new BufferedReader (in1);

		String inputLine;

		if (_verbose)
			System.err.println ("calling: 'in2.readLine' in loop");

		while ((inputLine = in2.readLine ()) != null) {
			if (!_checkOnly) {
				System.out.println (inputLine);
			}
		}

		in2.close ();

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//print properties (sorted)
	public static void printProperties ()
	{
		Vector<String> sorted = new Vector<String> ();

		Enumeration propEnum = System.getProperties ().propertyNames ();
		while (propEnum.hasMoreElements ()) {
			String key = (String) propEnum.nextElement ();
			sorted.add (key + " = " + System.getProperty (key).trim ());
		}

		Collections.sort (sorted, new Comparator<String> () {
			public int compare (String s1, String s2) {
				return s1.compareToIgnoreCase (s2);
			}
		});

		System.err.println ("Properties:");
		for (String str : sorted) {
			System.err.println (str);
		}
		System.err.println ("");
	}

	//private members
	private boolean _checkOnly = false;
	private boolean _debug = false;
	private boolean _verbose = false;
	private int _timeoutSeconds = 10;
	private String _urlString = null;

	//global members
	public static final String _AppName = "PrintUrl";
	public static final String NL = System.getProperty ("line.separator");
}
