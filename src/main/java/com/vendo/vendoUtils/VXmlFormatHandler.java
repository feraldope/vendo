//VXmlFormatHandler.java - generates XML output where each attribute is printed on its own line
//when used with a SAX parser, this writes the output as the parser is reading/parsing the input

package com.vendo.vendoUtils;

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;

import org.apache.logging.log4j.*;


public class VXmlFormatHandler extends DefaultHandler implements LexicalHandler
{
	public VXmlFormatHandler (Writer out)
	{
		_out = out;
	}

	public void startDocument () throws SAXException
	{
		_indentLevel = 0;
		_outputWritten = false;
		_state.clear ();

		emit ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	}

	public void endDocument () throws SAXException
	{
		try {
			emitNewLine ();
			_out.flush ();

		} catch (Exception ee) {
			throw new SAXException ("I/O error", ee);
		}
	}

	public void startElement (String namespaceURI,
							  String simpleName,
							  String qualifiedName,
							  Attributes attrs) throws SAXException
	{
		handleStartTag (/*closeTag*/ false);
		_state.push (new ParseState (qualifiedName, new AttributesImpl (attrs)));
	}

	public void endElement (String namespaceURI,
						    String simpleName,
						    String qualifiedName) throws SAXException
	{
		boolean noChildren = handleStartTag (/*closeTag*/ true);
		_indentLevel--;
		if (!noChildren)
			emitEndTag (qualifiedName);
		_state.pop ();
	}

	public void error (SAXParseException ee) throws SAXParseException
	{
		throw ee; //treat validation errors as fatal
	}

	public void warning (SAXParseException ee) throws SAXParseException
	{
		_log.warn ("Parsing error: line " + ee.getLineNumber () + ", uri " + ee.getSystemId (), ee);
//		System.out.println ("** Warning"
//						    + ", line " + err.getLineNumber ()
//						    + ", uri " + err.getSystemId ());
//		System.out.println ("   " + err.getMessage ());
	}

	public void characters (char buf[], int offset, int len) throws SAXException
	{
		handleStartTag (/*closeTag*/ false);

		String s = new String (buf, offset, len).replaceAll ("[\t]","");
		StringTokenizer st = new StringTokenizer (s, NL);
		int tokenCount = st.countTokens ();
		boolean multiline = (tokenCount > 1);
		boolean first = true;

		while (st.hasMoreTokens ()) {
			if (multiline && _state.peek ()._hasChars && !first) {
				if (_outputWritten)
					emitNewLine ();
				emitIndent ();
			}
			first = false;
			emit (safe (st.nextToken ()));

			if (s.trim ().length () > 0) {
				_state.peek ()._hasChars = true;
			}
			if (multiline) {
				_state.peek ()._multiline = true;
			}
		}
	}

	public void comment (char buf[], int offset, int len) throws SAXException
	{
		handleStartTag (/*closeTag*/ false);

		String s = new String (buf, offset, len).replaceAll ("[\t]","");
		if (_outputWritten)
			emitNewLine ();
		emitIndent ();
		emit ("<!--");
		StringTokenizer st = new StringTokenizer (s, NL);
		boolean first = true;
		if (st.countTokens () > 1) {
			while (st.hasMoreTokens ()) {
				if (first)
					first = false;
				else {
					emitNewLine ();
					emitIndent ();
				}
				emit (safe (st.nextToken ()));
			}
			emitNewLine ();
			emitIndent ();
		} else {
			emit (safe (s));
		}
		emit ("-->");
	}

	public void startCDATA () {
		System.err.println ("Warning: CDATA Handler not implemented.");
	}
	public void startDTD (String name, String publicId, String systemId) {
		System.err.println ("Warning: DTD Handler not implemented.");
	}
	public void startEntity (String name) {}
	public void endCDATA () {}
	public void endEntity (String name) {}
	public void endDTD () {}

	private void emit (String s) throws SAXException
	{
		try {
			_out.write (s);
			_out.flush ();
			_outputWritten = true;

		} catch (Exception ee) {
			throw new SAXException ("I/O error", ee);
		}
	}

	private void emitNewLine () throws SAXException
	{
		try {
			_out.write (NL);

		} catch (Exception ee) {
			throw new SAXException ("I/O error", ee);
		}
	}

	public static String safe (String s)
	{
		return s;
//		return s.replaceAll ("&", "&amp;").
//				 replaceAll ("'", "&apos;").
//				 replaceAll ("<", "&lt;").
//				 replaceAll (">", "&gt;").
//				 replaceAll ("\"", "&quot;").
//				 replaceAll ("\u00B0", "&#x00B0;");
	}

	private void emitEndTag (String name) throws SAXException
	{
		if (!_state.peek ()._hasChars ||
			_state.peek ()._multiline) {
			emitNewLine ();
			emitIndent ();
		}
		emit ("</" + name + ">");
	}

	//special handling - sort the attributes and print each on its own line
	private void emitTagWithAttr (String name, Attributes attrs, boolean closeTag) throws SAXException
	{
		if (_outputWritten)
			emitNewLine ();
		emitIndent ();
		emit ("<" + name);
		if (attrs != null) {
			_indentLevel++;

			String[] attrStrings = new String [attrs.getLength ()];
			for (int ii = 0; ii < attrs.getLength (); ii++) {
				attrStrings[ii] = attrs.getQName (ii) + "=\"" + safe (attrs.getValue (ii)) + "\"";
			}

			Arrays.sort (attrStrings, _caseInsensitiveStringComparator);

			for (int ii = 0; ii < attrStrings.length; ii++) {
				emitNewLine ();
				emitIndent ();
				emit (attrStrings[ii]);
			}
			_indentLevel--;
		}
		if (closeTag) {
			emitNewLine ();
			emitIndent ();
			emit ("/");
		}
		emit (">");
	}

	private void emitIndent () throws SAXException
	{
		try {
			for (int ii = 0; ii < _indentLevel; ii++) {
				_out.write ("\t");
			}

		} catch (Exception ee) {
			throw new SAXException ("I/O error", ee);
		}
	}

	private boolean handleStartTag (boolean closeTag) throws SAXException
	{
		if (!_state.empty ()) {
			ParseState ps = _state.peek ();
			if (!ps._startTagWritten) {
				emitTagWithAttr (ps._tagName, ps._attrs, closeTag);
				_indentLevel++;
				ps._startTagWritten = true;
				return true;
			}
		}
		return false;
	}

	private static final Comparator<String> _caseInsensitiveStringComparator = new Comparator<String> ()
	{
		public int compare (String s1, String s2) {
			return s1.compareToIgnoreCase (s2);
		}
	};

	protected class ParseState
	{
		public ParseState (String n, Attributes a) {_tagName = n; _attrs = a;}
		public boolean _hasChars = false;
		public boolean _multiline = false;
		public String _tagName = null;
		public Attributes _attrs = null;
		public boolean _startTagWritten = false;
	}

	//private members
	private Writer _out;
	private int _indentLevel = 0;
	private boolean _outputWritten = false;
	private Stack<ParseState> _state = new Stack<ParseState> ();
	private final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger (VXmlFormatHandler.class);
}
