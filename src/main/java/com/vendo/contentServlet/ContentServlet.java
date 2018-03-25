//ContentServlet.java

//Original from Core Servlets and JavaServer Pages 2nd Edition, http://www.coreservlets.com/

/*
http://localhost/servlet/ContentServlet.ContentServlet
http://localhost:8088/servlet/ContentServlet.ContentServlet
*/

//	Naming Convention for Paths and Folders:
//		path = absolute path
//		folder or dir = relative path
//		(paths should always have trailing slash, folders never do)
//
//	Examples:
//		basePath = E:/Netscape/Program/
//		rootPath = E:/Netscape/Program/jroot/
//		imagePath = E:/Netscape/Program/jroot/a/
//		rootFolder = jroot
//		subFolder = a
//		subDir = a

package com.vendo.contentServlet;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.logging.log4j.*;


public class ContentServlet extends HttpServlet
{
	///////////////////////////////////////////////////////////////////////////
	public void init () throws ServletException
	{
//for debugging startup
		ContentFormInfo._Debug = true;

		_log.debug ("--------------- ContentServlet.init ---------------");

		ContentProfiling.getInstance ().enter (1);

//		_album = AlbumImages.getInstance ();

//TODO - do I need this here?
//		AlbumDirList.getInstance ().getFileList (/*sinceInMillis*/ 0);

		ContentProfiling.getInstance ().exit (1);

		ContentProfiling.getInstance ().print (/*showMemoryUsage*/ false);

		_log.debug ("--------------- ContentServlet.init - done ---------------");
	}

	///////////////////////////////////////////////////////////////////////////
	public void doGet (HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException
	{
		_log.debug ("--------------- ContentServlet.doGet ---------------");

//TODO - change CLI to read properties file, too
		ServletContext context = getServletContext ();

//		ContentFormInfo form = ContentFormInfo.getInstance ().init ();
		_form = ContentFormInfo.getInstance ();//.init ();
		_form.processRequest (request, context); //checks for debug request
		ContentBeanUtilities.populateBean (_form, request);

		ContentProfiling.getInstance ().enter (1);

		HttpSession session = request.getSession ();
		_log.debug ("ContentServlet.doGet: session id = " + session.getId ());

		_contentList = getItemList ();
		_groups = getGroups (_contentList);

/*
		AlbumDirList.getInstance ().setSessionId (session.getId ());

		_album.processParams (request); //rename files marked for deletion
		_album = AlbumImages.getInstance (); //force reload of instance in case rootFolder changed
		_album.setForm (form);
		_album.loadImageLists ();
		_album.processRequest ();
*/
		String bgColor = getBgColor ();
/*
		int numSlices = _album.getNumSlices ();
		int charFieldWidth = 30;

		String title = _album.generateTitle ();
*/
		String title = generateTitle ();

		StringBuffer sb1 = new StringBuffer (4096);
		sb1.append (DOCTYPE)
		   .append ("<HTML>").append (NL)

		   .append ("<style type='text/css'>").append (NL)
		   .append ("	body {").append (NL)
		   .append ("		font-family: Arial;").append (NL)
		   .append ("		font-size: 10pt;").append (NL)
		   .append ("	}").append (NL)
		   .append ("	h3 {").append (NL)
		   .append ("		font-size: 14pt;").append (NL)
		   .append ("	}").append (NL)
		   .append ("	td {").append (NL)
		   .append ("		font-size: 10pt;").append (NL)
		   .append ("	}").append (NL)
		   .append ("	td.fontsize8 {").append (NL)
		   .append ("		font-size: 8pt;").append (NL)
		   .append ("	}").append (NL)
		   .append ("	td.fontsize9 {").append (NL)
		   .append ("		font-size: 9pt;").append (NL)
		   .append ("	}").append (NL)
		   .append ("	td.fontsize10 {").append (NL)
		   .append ("		font-size: 10pt;").append (NL)
		   .append ("	}").append (NL)
		   .append ("</style>").append (NL)

		   .append ("<A NAME=\"ContentTop\"></A>").append (NL)

//attempt to turn off caching; copied from http://www.w3schools.com/tags/att_input_type.asp
//		   .append ("<META HTTP-EQUIV=\"pragma\" CONTENT=\"no-cache\" />").append (NL)
//		   .append ("<META HTTP-EQUIV=\"cache-control\" CONTENT=\"no-cache\" />").append (NL)
//		   .append ("<META HTTP-EQUIV=\"expires\" CONTENT=\"0\" />").append (NL)

		   .append ("<HEAD><TITLE>" + title + "</TITLE>").append (NL)

		   //check for javascript enabled
		   .append ("<div id='status'><span id='jscheck'>This page may not work correctly with JavaScript disabled</span>").append (NL)
		   .append ("	<script type='text/javascript'>").append (NL)
//		   .append ("		document.getElementById('jscheck').innerHTML = 'JavaScript enabled';").append (NL)
		   .append ("		document.getElementById('jscheck').innerHTML = '';").append (NL)
		   .append ("	</script>").append (NL)
		   .append ("</div>").append (NL)

		   //javascript function handleSubmit
		   .append ("<SCRIPT TYPE=\"text/javascript\">").append (NL)
		   .append ("	function handleSubmit() {").append (NL)
		   .append ("		var windowWidth = document.getElementById (\"windowWidth\");").append (NL)
		   .append ("		var windowHeight = document.getElementById (\"windowHeight\");").append (NL)
		   .append ("		windowWidth.value = top.window.innerWidth;").append (NL)
		   .append ("		windowHeight.value = top.window.innerHeight;").append (NL)

		   //add timeStamp to URL to always causes server hit and avoid caching
		   .append ("		var timeStamp = document.getElementById (\"timeStamp\");").append (NL)
		   .append ("		timeStamp.value = (new Date ()).getTime ();").append (NL)

		   .append ("		return true;").append (NL)
		   .append ("	}").append (NL)
		   .append ("</SCRIPT>").append (NL)

		   .append ("</HEAD>").append (NL)

		   .append ("<BODY BGCOLOR=\"").append (bgColor).append ("\">").append (NL)
//		   .append ("<CENTER>").append (NL)
		   .append ("<LEFT>").append (NL)
		   ;
//		   .append ("<H3>").append (title).append ("</H3>").append (NL)

/*
		   .append ("<FORM name=\"AlbumForm\" onsubmit=\"return handleSubmit()\" autocomplete=\"off\">").append (NL)
		   .append ("	<input type=\"hidden\" name=\"windowWidth\" id=\"windowWidth\" />").append (NL)
		   .append ("	<input type=\"hidden\" name=\"windowHeight\" id=\"windowHeight\" />").append (NL)
		   .append ("	<input type=\"hidden\" name=\"timeStamp\" id=\"timeStamp\" />").append (NL)

		   .append ("<TABLE WIDTH=100% CELLPADDING=0 CELLSPACING=0 BORDER=0>").append (NL)
		   .append ("<TR>").append (NL)

		   .append ("<TD ALIGN=RIGHT>").append (NL)
		   .append (inputElement ("Filter 1", "filter1", form.getFilter1 (), charFieldWidth)).append (_spacing3Break).append (NL)
		   .append (inputElement ("Filter 2", "filter2", form.getFilter2 (), charFieldWidth)).append (_spacing3Break).append (NL)
		   .append (inputElement ("Filter 3", "filter3", form.getFilter3 (), charFieldWidth)).append (_spacing3Break).append (NL)
//		   .append (inputElement ("Filter 4", "filter4", form.getFilter4 (), charFieldWidth)).append (_spacing3Break).append (NL)
		   .append ("</TD>").append (NL)

		   .append ("<TD ALIGN=LEFT>").append (NL)
		   .append (inputElement ("Exclude 1", "exclude1", form.getExclude1 (), charFieldWidth)).append (_break).append (NL)
		   .append (inputElement ("Exclude 2", "exclude2", form.getExclude2 (), charFieldWidth)).append (_break).append (NL)
		   .append (inputElement ("Exclude 3", "exclude3", form.getExclude3 (), charFieldWidth)).append (_break).append (NL)
//		   .append (inputElement ("Exclude 4", "exclude4", form.getExclude4 (), charFieldWidth)).append (_break).append (NL)
		   .append ("</TD>").append (NL)

		   .append ("</TR>").append (NL)
		   .append ("</TABLE>").append (NL)

//		   .append (inputElement ("Extension", "extension", form.getExtension (), 10)) //could make this hidden??
		   .append (radioButtons ("Columns", "columns", form.getColumns (), 1, form.getMaxColumns (), 4)).append (_break).append (NL)
		   .append (radioButtons ("Sort By", "sortType", AlbumSortType.getValues (), form.getSortType ().getSymbol (), 4)).append (_break).append (NL)
		   .append (inputElement ("Panels", "panels", form.getPanels (), 4)).append (_spacing3).append (NL)
		   .append (inputElement ("SinceDays", "sinceDays", form.getSinceDays (), 4)).append (_spacing3).append (NL)
		   .append (inputElement ("Folder", "rootFolder", form.getRootFolder (), 4)).append (_spacing3).append (NL)

//		   .append (inputElement ("", "slice", 1, 0)) //form submit always sets slice to 1
		   .append (inputElement ("", "slice", form.getSlice (), 0))
		   .append (checkbox ("Collapse Groups", "collapseGroups", form.getCollapseGroups ())).append (_spacing3).append (NL)
		   .append (checkbox ("Reverse Sort", "reverseSort", form.getReverseSort ()))/* .append (_spacing3).append (NL)
		   .append (checkbox ("Debug", "debug", ContentFormInfo._Debug))*
		   .append ("<P>").append (NL)

		   //place dummy hidden submit first, to act as default action when pressing RETURN
		   .append ("<div style=\"display:none\">").append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (form.getMode ().getSymbol ())
		   .append ("\">").append (NL).append ("</div>").append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoDir.getSymbol ())
		   .append ("\">").append (NL).append (_spacing3).append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoDup.getSymbol ())
		   .append ("\">").append (NL).append (_spacing3).append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoSampler.getSymbol ())
		   .append ("\">").append (NL)

		   .append ("</CENTER>").append (NL);
*/

		StringBuffer sb2 = new StringBuffer ();
		sb2.append ("</FORM>").append (NL)
		   .append ("</BODY>").append (NL)
		   .append ("</HTML>").append (NL);

		response.setContentType ("text/html");
		PrintWriter out = response.getWriter ();
		out.println (sb1.toString ());
//		out.println (_album.generateHtml ());
		out.println (generateHtml ());
		out.println (sb2.toString ());

		ContentProfiling.getInstance ().exit (1);

		ContentProfiling.getInstance ().print (/*showMemoryUsage*/ true);

		_log.debug ("--------------- ContentServlet.doGet - done ---------------");
	}

	///////////////////////////////////////////////////////////////////////////
	private String generateTitle ()
	{
		return "ContentServlet";
	}

	///////////////////////////////////////////////////////////////////////////
	private String generateHtml ()
	{
		StringBuffer sb = new StringBuffer (4096);

		String itemUrlPath = ContentFormInfo.getInstance ().getRootPath (true);

		sb.append ("<TABLE")// WIDTH=")
//		  .append ("1%")//tableWidth)
		  .append (" CELLSPACING=0 CELLPADDING=")
		  .append ("0")//padding)
		  .append (" BORDER=")
		  .append ("0 ")//_tableBorder)
//		  .append ("ALIGN=LEFT")
		  .append (">").append (NL);

		sb.append (TAB).append ("<TD class=\"fontsize10\" style=\"font-weight:normal;color:black\"\" VALIGN=BOTTOM ALIGN=LEFT>").append (NL);

		sb.append ("<TABLE")// WIDTH=")
//		  .append ("1%")//tableWidth)
		  .append (" CELLSPACING=0 CELLPADDING=")
		  .append ("2")//padding)
		  .append (" BORDER=")
		  .append ("1 ")//_tableBorder)
//		  .append ("ALIGN=LEFT")
		  .append (">").append (NL);

// name = item00-01
// file = item00-01.ext
// base = item00
// index = 01

		int count = 1;
		ContentItem prevItem = null;
		for (ContentItem item : _contentList) {
//			if (item.getIsDirectory ())
//				continue;
//			if (item.getExtension ().compareToIgnoreCase (_form.getExtension ()) != 0)
//				continue;

			boolean newBase = !item.isSameBase (prevItem);
			boolean newGroup = !item.isSameFirstLetter (prevItem); //group items by first letter of name

			if (newBase) {
				sb.append ("</TR>").append (NL);

				sb.append ("</TABLE>").append (NL);

				sb.append ("<TABLE")// WIDTH=")
//				  .append ("1%")//tableWidth)
				  .append (" CELLSPACING=0 CELLPADDING=")
				  .append ("2")//padding)
				  .append (" BORDER=")
				  .append ("1 ")//_tableBorder)
//				  .append ("ALIGN=LEFT")
				  .append (">").append (NL);

				sb.append ("<TR>").append (NL);
			}

//			sb.append ("<TR>").append (NL);

			sb.append (TAB).append ("<TD class=\"fontsize10\" style=\"font-weight:normal;color:black\"\" VALIGN=BOTTOM ALIGN=LEFT>").append (NL);

			if (newBase) {
				sb.append ("<NOBR>").append (item.getBase ()).append ("</NOBR>").append (NL);
				sb.append (TAB).append ("</TD>").append (NL);
				sb.append (TAB).append ("<TD class=\"fontsize10\" style=\"font-weight:normal;color:black\"\" VALIGN=BOTTOM ALIGN=LEFT>").append (NL);
			}

//			sb.append (count + " / " + total).append (_spacing3);

			String href = itemUrlPath + item.getFile ();
			String name = item.getIndex ();

//			sb.append (TAB2).append ("<A HREF=\"").append (href).append ("\" target=_blank>"); //TODO - may still need this
			sb.append (TAB2).append ("<A HREF=\"").append (href).append ("\" >");
			sb.append ("<NOBR>").append (name).append ("</NOBR>").append (NL);
			sb.append (TAB2).append ("</A>").append (NL);


			sb.append (TAB).append ("</TD>").append (NL);
//			sb.append ("</TR>").append (NL);

			if (newGroup) {
				String firstLetter = item.getBase ().substring (0, 1).toUpperCase ();
				sb.append (TAB).append ("<A NAME=\"" + firstLetter + "\"></A>");

				sb.append (generateLinks (_groups));
			}

			prevItem = item;
			count++;
		}

		if (count == 1)
			sb.append ("No matching items found").append (NL);

		sb.append ("</TABLE>").append (NL);
		sb.append ("</TD>").append (NL);
		sb.append ("</TABLE>");

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String generateLinks (Collection<String> groups)
	{
		StringBuffer sb = new StringBuffer (1024);

		sb.append (_spacing3);
		for (String group : groups) {
			sb.append ("<A HREF=\"#" + group + "\"><NOBR>" + group + "</NOBR></A>");
			sb.append (_spacing3).append (NL);
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String getBgColor ()
	{
		return "#FDF5E5";
	}

	///////////////////////////////////////////////////////////////////////////
	private Collection<ContentItem> getItemList ()
	{
		Path folder = FileSystems.getDefault ().getPath (_form.getRootPath (false));
		Collection<Path> fileList = getFileList (folder);
//TODO error check here

		List<ContentItem> result = new ArrayList<ContentItem> ();
		for (Path entry : fileList) {
			result.add (new ContentItem (entry));
		}

		Collections.sort (/*(ArrayList<ContentItem>)*/ result, new ContentItemComparator (_form));

		return result;
	}

	///////////////////////////////////////////////////////////////////////////
	//using java.nio package
	private Collection<Path> getFileList (Path folder)
	{
		String glob = new String ("*.{") + _form.getExtension () + "}";

		Collection<Path> result = new ArrayList<Path> ();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream (folder, glob)) {
			for (Path entry : stream) {
				result.add (entry);
			}
		} catch (Exception ee) {
			System.out.println ("listFiles2 (" + folder + ") failed: ");
			System.out.println (ee);
		}

		return result;
	}

	///////////////////////////////////////////////////////////////////////////
	//items are grouped by the first letter of their name
	private Collection<String> getGroups (Collection<ContentItem> contentList)
	{
		HashSet<String> set = new HashSet<String> (); //a collection that contains no duplicate elements

		for (ContentItem item : contentList) {
			String firstLetter = item.getBase ().substring (0, 1).toUpperCase ();
			set.add (firstLetter);
		}

		List<String> result = new ArrayList<String> ();
		result.addAll (set);
		Collections.sort (result);

		return result;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, String value, int size)
	{
		//hide control if prompt is empty string
		boolean isHidden = (prompt.length () == 0 ? true : false);

		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (isHidden ? "" : ": ")
		  .append ("<INPUT TYPE=\"")
		  .append (isHidden ? "HIDDEN" : "TEXT")
		  .append ("\" NAME=\"")
		  .append (name)
		  .append ("\" SIZE=\"")
		  .append (size)
		  .append ("\" VALUE=\"")
		  .append (value)
		  .append ("\">")
		  .append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, int value, int size)
	{
		String num = "";
		if (value != 0)
			num = String.valueOf (value);

		return (inputElement (prompt, name, num, size));
	}

	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, double value, int size)
	{
		String num = "";
		if (value != 0)
			num = String.valueOf (value);

		//strip trailing zeros (assumes decimal point is always included)
		while (num.endsWith ("0"))
			num = num.substring (0, num.length () - 1);
		if (num.endsWith ("."))
			num = num.substring (0, num.length () - 1);

		return (inputElement (prompt, name, num, size));
	}

	///////////////////////////////////////////////////////////////////////////
	private String checkbox (String prompt, String name, boolean isChecked)
	{
		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (": ")
		  .append ("<INPUT TYPE=\"CHECKBOX\" NAME=\"")
		  .append (name)
		  .append ("\"")
		  .append (isChecked ? " CHECKED" : "")
		  .append (">")
		  .append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String radioButtons (String prompt, String name, int defaultValue, int min, int max, int size)
	{
		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (": ")
		  .append (NL);

		for (int ii = min; ii <= max; ii++) {
			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" VALUE=\"")
			  .append (ii)
			  .append ("\"")
			  .append (defaultValue == ii ? " CHECKED" : "")
			  .append (">")
			  .append (ii)
			  .append (NL);
		}

		sb.append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String radioButtons (String prompt, String name, AlbumStringPair[] values, String defaultValue, int size)
	{
		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (": ")
		  .append (NL);

		for (AlbumStringPair value : values) {
//			_log.debug (ii + " " + values[ii].getName () + "/" + values[ii].getSymbol () + ": " + defaultValue);

			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" VALUE=\"")
			  .append (value.getSymbol ())
			  .append ("\"")
			  .append (value.getSymbol ().equals (defaultValue) ? " CHECKED" : "")
			  .append (">")
			  .append (value.getName ())
			  .append (NL);
		}

		sb.append (NL);

		return sb.toString ();
	}
*/

	//members
	private ContentFormInfo _form = null;
	private Collection<ContentItem> _contentList = null;
	private Collection<String> _groups = null;

	private static String _break = "<BR>";
	private static String _spacing3 = "&nbsp&nbsp&nbsp";
	private static String _spacing3Break = _spacing3 + _break;

	private static final String TAB = "\t";
	private static final String TAB2 = "\t\t";
	private static final String NL = System.getProperty ("line.separator");
	private static final String DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";

	private static Logger _log = LogManager.getLogger (ContentServlet.class);
}
