//XmlParse.java

//JDOM API doc available from: http://www.jdom.org/docs/apidocs/

/*
This reads and writes XML files of the form (e.g., elementTypeId.xml):

<fileInfo version=number
		lastModified=string
/>

<elementType elementTypeId=number
		symbol=string
		name=string
		subTypes=comma separated list of numbers, possibly empty
		version=number
/>

<objects>
	<object objectId=number
		objectType=class/object
		symbol=string (may be "_empty" if no symbol, e.g., for "Bytes/sec" and the weighted measures)
		name=string
		parentName=string
		select=string
		where=string
		dataType=number/string/date/??
		columnNames=comma separated list of strings, possibly empty
		aggregate=average/count/max/min/sum/etc.
		qualification=dimension/measure/detail
		show=yes/no
	/>
</objects>

*/

package com.vendo.xmlParse;

import java.io.*;
import java.util.*;

import org.jdom2.*;
import org.jdom2.filter.*;
import org.jdom2.input.*;
import org.jdom2.output.*;
//import org.jdom2.xpath.*;

//import org.w3c.dom.NodeList;

import org.apache.logging.log4j.*;


public class XmlParse
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		XmlParse xmlParse = new XmlParse ();

		if (!xmlParse.processArgs (args))
			System.exit (1); //processArgs displays error

		xmlParse.run ();
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

				} else if (arg.equalsIgnoreCase ("xmlFile") || arg.equalsIgnoreCase ("xml")) {
					try {
						_xmlFilename = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("firstObjectID") || arg.equalsIgnoreCase ("first")) {
					try {
						_nextObjectID = parseInt (args[++ii]);
//todo handle NumberFormatException
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

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
			_filename = "elementType.xml";
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
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		readXml ();

		if (_document == null) {
			int elementTypeId = 104052;
			String symbol = "LanWan";
			String label = "LAN/WAN";
			int[] subTypes = { 104051, 104050 };

			_document = newDefaultXmlDocument (elementTypeId, symbol, label, subTypes);
		}

		if (_document != null) {
			dumpXml ();

//temp testing
			addFakeObjectToDocument (_nextObjectID + 1);
			modifyObjectInDocument (_nextObjectID + 1);

			writeXml ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private Document newDefaultXmlDocument (int elementTypeId, String symbol, String label, int[] subTypes)
	{
		Element root = new Element ("root");
		Document document = new Document (root);

		Element fileInfo = new Element ("fileInfo");
		fileInfo.setAttribute ("version", "1");
		fileInfo.setAttribute ("lastModified", new Date ().toString ());
		root.addContent (fileInfo);

		Element elementType = new Element ("elementType");
		elementType.setAttribute ("elementTypeId", Integer.toString (elementTypeId));
		elementType.setAttribute ("symbol", symbol);
		elementType.setAttribute ("name", label);
		elementType.setAttribute ("subTypes", arrayToString (subTypes));
		elementType.setAttribute ("version", "1"); //new file - use version 1
		root.addContent (elementType);

		Element objects = new Element ("objects");
		root.addContent (objects);

		String initialObjects[] = { "Prompt Measurements",
									"Base Measurements",
									"Hourly Measurements",
									"Daily Measurements",
									"Fast Poll Measurements"
								  };

		objects.addContent (newClassObject ("Measurements", ""));

		for (int ii = 0; ii < initialObjects.length; ii++) {
			objects.addContent (newClassObject (initialObjects[ii], "Measurements"));
		}

		for (int ii = 0; ii < initialObjects.length; ii++) {
			objects.addContent (newClassObject ("Weighted " + initialObjects[ii], initialObjects[ii]));
		}

		return document;
	}

	///////////////////////////////////////////////////////////////////////////
	private Element newClassObject (String name, String parentName)
	{
		Element object = new Element ("object");
		object.setAttribute ("objectId", Integer.toString (_nextObjectID++));
		object.setAttribute ("name", name);
		object.setAttribute ("parentName", parentName);

		return object;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean readXml ()
	{
		try {
			File file = new File (_xmlFilename);
			if (!file.exists ()) {
				System.err.println ("Error opening XML file \"" + _xmlFilename + "\"");
				return false;
			}

			SAXBuilder builder = new SAXBuilder ();
			_document = builder.build (file);

		} catch (Exception ee) {
			System.err.println ("Error parsing XML file \"" + _xmlFilename + "\"");
			ee.printStackTrace();
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean dumpXml ()
	{
		dumpFileInfo ();
		dumpElementType ();
		dumpObjects ();

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean dumpFileInfo ()
	{
		int size = 0;

		Element root = _document.getRootElement ();
		Iterator<Element> iter = root.getDescendants (new ElementFilter ("fileInfo"));

		while (iter.hasNext ()) {
			Element element = iter.next ();

			String xmlName = element.getName ();
			String version = element.getAttributeValue ("version");

			System.out.println (xmlName + ", version = " + version);

			size++;
		}

		System.out.println ("XmlParse.dumpXml: size = " + size);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean dumpElementType ()
	{
		int size = 0;

		Element root = _document.getRootElement ();
		Iterator<Element> iter = root.getDescendants (new ElementFilter ("elementType"));

		while (iter.hasNext ()) {
			Element element = iter.next ();

			String xmlName = element.getName ();
			String elementTypeId = element.getAttributeValue ("elementTypeId");
			String symbol = element.getAttributeValue ("symbol");
			String name = element.getAttributeValue ("name");
			String subTypes = element.getAttributeValue ("subTypes");

			System.out.println (xmlName + ", " + elementTypeId + ", " + symbol  + ", " + name  + ", " + subTypes);

			size++;
		}

		System.out.println ("XmlParse.dumpXml: size = " + size);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean dumpObjects ()
	{
		int size = 0;

		Element root = _document.getRootElement ();
		Iterator<Element> iter = root.getDescendants (new ElementFilter ("object"));

		while (iter.hasNext ()) {
			Element element = iter.next ();

			String xmlName = element.getName ();
			String objectId = element.getAttributeValue ("objectId");
			String objectType = element.getAttributeValue ("objectType");
			String symbol = element.getAttributeValue ("symbol");
			String name = element.getAttributeValue ("name");
//			String parentName = element.getAttributeValue ("parentName");
//			String select = element.getAttributeValue ("select");
//			String where = element.getAttributeValue ("where");

			System.out.println (xmlName + ", " + objectId + ", " + objectType  + ", " + symbol  + ", " + name);

			int id = Integer.valueOf (objectId);
			if (id > _nextObjectID)
				_nextObjectID = id;

			size++;
		}

		System.out.println ("XmlParse.dumpXml: size = " + size);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean addFakeObjectToDocument (int objectId)
	{
		Element element = new Element ("object");
		element.setAttribute ("objectId", Integer.toString (objectId));
		element.setAttribute ("objectType", "class");
		element.setAttribute ("symbol", "discards");
		element.setAttribute ("name", "Discards");
		element.setAttribute ("select", "select sum(DISCARDS) from NH_TABLE");

		Element root = _document.getRootElement ();
		Element objects = root.getChild ("objects");

		objects.addContent (element);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean modifyObjectInDocument (int objectId)
	{
		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeXml ()
	{
		String xmlBackupName = new String (_xmlFilename + ".bak");
		File xmlBackupFile = new File (xmlBackupName);

		if (xmlBackupFile.exists ()) {
			try {
				xmlBackupFile.delete ();

			} catch (Exception ee) {
				_log.error ("writeXml: error deleting backup XML file \"" + xmlBackupName + "\"");
				return false;
			}
		}

		try {
			File xmlFile = new File (_xmlFilename);

			boolean status = xmlFile.renameTo (xmlBackupFile);
			if (!status) {
				_log.error ("Rename failed (" + xmlFile.getCanonicalPath () + " to " + xmlBackupFile.getCanonicalPath () + ")");
				return false;
			}

		} catch (Exception ee) {
			_log.error ("writeXml: error renaming XML file to \"" + _xmlFilename + ".bak" + "\"");
			return false;
		}

		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream (_xmlFilename);

		} catch (IOException ee) {
			_log.error ("writeXml: error opening output file \"" + _xmlFilename + "\"");
			return false;
		}

		try {
			new XMLOutputter (Format.getPrettyFormat ()).output (_document, outputStream);
//TODO - should this be in finally block?
			outputStream.close ();

		} catch (Exception ee) {
			_log.error ("writeXml: error writing XML file \"" + _xmlFilename + "\"");
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private static String arrayToString (int[] array)
	{
		StringBuffer sb = new StringBuffer (array.length * 8);
		for (int ii = 0; ii < array.length; ii++) {
			if (ii != 0)
				sb.append (",");
			sb.append (Integer.toString (array[ii]));
		}

		return sb.toString ();
	}

//	///////////////////////////////////////////////////////////////////////////
//	private static String arrayToString (String[] array)
//	{
//		StringBuffer sb = new StringBuffer (array.length * 20);
//		for (int ii = 0; ii < array.length; ii++) {
//			if (ii != 0)
//				sb.append (",");
//			sb.append (array[ii]);
//		}
//
//		return sb.toString ();
//	}

	///////////////////////////////////////////////////////////////////////////
	public static int parseInt (String string)
	{
		int value = 0;

		try {
			value = Integer.parseInt (string);

		} catch (NumberFormatException ee) {
			_log.error ("parseInteger: error parsing number \"" + string + "\", using value 0");
		}

		if (value < 0) {
			_log.error ("parseInteger: unexpected negative number \"" + value + "\", using value 0");
			value = 0;
		}

		return value;
	}



	//private members
	private int _nextObjectID = 1000;
	private String _xmlFilename = null;
	private Document _document = null;

	//global members
	public static boolean _Debug = false;
	public static final String _AppName = "XmlParse";

	private static final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger (XmlParse.class);
}
