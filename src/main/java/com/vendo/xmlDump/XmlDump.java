//XmlDump.java

//Java API doc available from: http://www.jdom.org/docs/apidocs/

/*
This dumps XML files of the form (e.g., reportstudio_en.xml):

<!--
	Copyright (C) 2005 Cognos Incorporated. All Rights Reserved.
	Cognos (R) is a trademark of Cognos Incorporated.
-->
<stringTable>
	<component name="RST">
		<section name="RSA" type="Menus">
			<string id="IDS_MBI_FILE" type="Menu">Fichier</string>
			<string id="IDS_MBI_EDIT" type="Menu">Édition</string>
			<string id="IDS_MBI_VIEW" type="Menu">Affichage</string>
			<string id="IDS_MBI_ORGANIZE" type="Menu">Organisation</string>
		</section>
	</component>
</stringTable>
*/


package com.vendo.xmlDump;

import java.io.*;
import java.util.*;

import org.jdom2.*;
import org.jdom2.filter.*;
import org.jdom2.input.*;
//import org.jdom2.output.*;
//import org.jdom2.xpath.*;

//import org.w3c.dom.NodeList;

//import org.apache.logging.log4j.*;

import com.vendo.vendoUtils.VLogger;


public class XmlDump
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		XmlDump xmlDump = new XmlDump ();

		if (!xmlDump.processArgs (args))
			System.exit (1); //processArgs displays error

		xmlDump.run ();
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
				} else if (arg.equalsIgnoreCase ("xmlFile") || arg.equalsIgnoreCase ("xml")) {
					try {
						_xmlFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}
*/

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
				//check for other args
				if (_xmlFilename == null)
					_xmlFilename = arg;

				else
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		//handle defaults
/*
		if (_filename == null)
			_filename = "MyHDResCapList.mrl";
*/

		//verify required args
		if (_xmlFilename == null)
			displayUsage ("Incorrect usage", true);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] <XML file name>";
//		System.err.println ("Error: " + msg + NL);
		VLogger.error ("Error: " + msg);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		readXml ();

//		if (_doc == null)
//			_doc = new Document (new Element ("stringTable"));

		if (_doc != null)
			dumpXml ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readXml ()
	{
		try {
			File file = new File (_xmlFilename);
			if (!file.exists ()) {
				VLogger.error ("Error opening XML file \"" + _xmlFilename + "\"");
				return false;
			}

			SAXBuilder builder = new SAXBuilder ();
			_doc = builder.build (file);

		} catch (Exception ee) {
			VLogger.error ("Error parsing XML file \"" + _xmlFilename + "\"");
			VLogger.error (ee);
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean dumpXml ()
	{
		int size = 0;

		Element root = _doc.getRootElement ();
		Iterator<Element> iter = root.getDescendants (new ElementFilter ());

		while (iter.hasNext ()) {
			Element element = iter.next ();

//TODO - add items to collection to sort before printing
			String name = element.getName ();
			String id = element.getAttributeValue ("id");

			System.out.println (name + ", " + id);

			size++;
		}

		System.out.println ("XmlDump.dumpXml: size = " + size);

		return true;
	}


	//private members
	private String _xmlFilename = null;
	private Document _doc = null;

	//global members
	public static boolean _Debug = false;
	public static final String _AppName = "XmlDump";

	private static final String NL = System.getProperty ("line.separator");

//	private static Logger _log = LogManager.getLogger (XmlDump.class);
}
