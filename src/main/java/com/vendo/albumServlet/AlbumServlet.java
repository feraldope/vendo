//AlbumServlet.java

//Original from Core Servlets and JavaServer Pages 2nd Edition, http://www.coreservlets.com/

/*
http://localhost/servlet/com.vendo.apps.AlbumServlet
http://localhost/servlet/AlbumServlet.AlbumServlet
*/

package com.vendo.albumServlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class AlbumServlet extends HttpServlet
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void init () throws ServletException
	{
		super.init ();

//for debugging startup
		AlbumFormInfo._Debug = true;

		_log.debug ("--------------- AlbumServlet.init ---------------");

		AlbumProfiling.getInstance ().enter (1);

		_album = AlbumImages.getInstance ();

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ false);

		_log.debug ("--------------- AlbumServlet.init - done ---------------");
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void destroy ()
	{
		super.destroy ();

		AlbumImages.shutdownExecutor ();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void doPost (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		_log.debug ("--------------- AlbumServlet.doPost ---------------");
		doGet (request, response);
		_log.debug ("--------------- AlbumServlet.doPost - done ---------------");
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void doGet (HttpServletRequest request, HttpServletResponse response) //throws ServletException, IOException
	{
	try {
		_log.debug ("--------------- AlbumServlet.doGet ---------------");

		//debugging
//		_log.debug ("Request Headers:" + NL + VendoUtils.getRequestHeaders (request));

		//check the value of this property
		String property = "java.util.Arrays.useLegacyMergeSort";
		_log.debug (property + " = " + System.getProperty (property));

//TODO - change CLI to read properties file, too
		ServletContext context = getServletContext ();

		AlbumFormInfo form = AlbumFormInfo.newInstance (); //force a new instance on every request
		form.processRequest (request, context); //also checks for debug request
		AlbumBeanUtilities.populateBean (form, request);

		AlbumProfiling.getInstance ().enter (1);

//		HttpSession session = request.getSession ();
//		_log.debug ("AlbumServlet.doGet: session id = " + session.getId ());
//		AlbumDirList.getInstance ().setSessionId (session.getId ());

		_album.processParams (request); //rename files marked for deletion
		_album = AlbumImages.getInstance (); //force reload of instance in case rootFolder changed
		_album.setForm (form);
		_album.processRequest ();

		String bgColor = _album.getBgColor ();
		int stringFieldWidth1 = 40;
		int stringFieldWidth2 = 60;
		int numberFieldWidth = 2;

		//get tags from database, add blank selection to beginning of list
		Collection<String> allTags = new ArrayList<String> ();
		allTags.add ("");
		allTags.addAll (AlbumTags.getInstance ().getAllTags ());

		String title = _album.generateTitle ();

		String action = new String ();
		String method = form.getMethod ();
//		if (method.compareToIgnoreCase ("post") == 0) { //add appropriate action if method="post"
//			action = response.encodeURL (form.getServer ());
//			action = "action=\"" + action + "\" ";
//		}
		method = "method=\"" + method + "\" ";

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
		   .append ("	}").append (NL);

		List<Integer> fontSizes = Arrays.asList (8, 9, 10, 12, 24);
		for (Integer fontSize : fontSizes) {
			sb1.append ("	td.fontsize").append (fontSize).append (" {").append (NL)
			   .append ("		font-size: ").append (fontSize).append ("pt;").append (NL)
			   .append ("	}").append (NL);
		}
		sb1.append ("</style>").append (NL);

		sb1.append ("<A NAME=\"AlbumTop\"></A>").append (NL)
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
			//debugging navigator object
//		   .append ("		var uagent = navigator.userAgent.toLowerCase();").append (NL)
//		   .append ("		alert (uagent);").append (NL)
//		   .append ("		var nav = navigator;").append (NL)
//		   .append ("		var props = \"\";").append (NL)
//		   .append ("		for (var prop in nav) {").append (NL)
//		   .append ("			props += prop + \": \" + nav[prop] + \"\\n\";").append (NL)
//		   .append ("		}").append (NL)
//		   .append ("		alert (props);").append (NL)
		   .append ("	</script>").append (NL)
		   .append ("</div>").append (NL)

		   //javascript function handleSubmit
		   .append ("<SCRIPT TYPE=\"text/javascript\">").append (NL)
		   .append ("	function handleSubmit() {").append (NL)
			//avoid Firefox message when refreshing (when using method=post)
//		   .append ("		window.location = window.location;").append (NL)
		   //add display geometry
//		   .append ("		var screenWidth = document.getElementById (\"screenWidth\");").append (NL)
//		   .append ("		var screenHeight = document.getElementById (\"screenHeight\");").append (NL)
//		   .append ("		screenWidth.value = top.screen.width;").append (NL)
//		   .append ("		screenHeight.value = top.screen.height;").append (NL)
		   .append ("		var windowWidth = document.getElementById (\"windowWidth\");").append (NL)
		   .append ("		var windowHeight = document.getElementById (\"windowHeight\");").append (NL)
		   .append ("		windowWidth.value = top.window.innerWidth;").append (NL)
		   .append ("		windowHeight.value = top.window.innerHeight;").append (NL)
		   //add timestamp to URL to always causes server hit and avoid (reduce) caching
		   .append ("		var timestamp = document.getElementById (\"timestamp\");").append (NL)
		   .append ("		timestamp.value = (new Date ()).getTime ();").append (NL)
		   //add userAgent to URL for browser (and mobile) specific handling
		   .append ("		var userAgent = document.getElementById (\"userAgent\");").append (NL)
		   .append ("		userAgent.value = navigator.userAgent;").append (NL)
		   //.append ("		alert (userAgent);").append (NL)
		   .append ("		return true;").append (NL)
		   .append ("	}").append (NL)
//		   //javascript function setFocus
//		   .append ("	function setFocus() {").append (NL)
//		   .append ("		document.getElementById(\"filter1\").focus();").append (NL)
//		   .append ("		document.getElementById(\"filter1\").select();").append (NL)
//		   .append ("	}").append (NL)
		   .append ("</SCRIPT>").append (NL)

		   .append ("</HEAD>").append (NL)
//		   .append ("<BODY onload=\"setFocus()\" BGCOLOR=\"").append (bgColor).append ("\">").append (NL)
		   .append ("<BODY BGCOLOR=\"").append (bgColor).append ("\">").append (NL)
		   .append ("<CENTER>").append (NL)
//		   .append ("<H3>").append (title).append ("</H3>").append (NL)

		   .append ("<FORM name=\"AlbumForm\" onsubmit=\"return handleSubmit()\" autocomplete=\"off\" ").append (method).append (action).append ("\">").append (NL)
//		   .append ("	<input type=\"hidden\" name=\"screenWidth\" id=\"screenWidth\" />").append (NL)
//		   .append ("	<input type=\"hidden\" name=\"screenHeight\" id=\"screenHeight\" />").append (NL)
		   .append ("	<input type=\"hidden\" name=\"windowWidth\" id=\"windowWidth\" />").append (NL)
		   .append ("	<input type=\"hidden\" name=\"windowHeight\" id=\"windowHeight\" />").append (NL)
		   .append ("	<input type=\"hidden\" name=\"timestamp\" id=\"timestamp\" />").append (NL)
		   .append ("	<input type=\"hidden\" name=\"userAgent\" id=\"userAgent\" />").append (NL)

		   .append ("<TABLE WIDTH=100% CELLPADDING=0 CELLSPACING=0 BORDER=0>").append (NL)
		   .append ("<TR>").append (NL)

		   .append ("<TD ALIGN=RIGHT>").append (NL)
		   .append (inputElement ("Filter 1", "filter1", form.getFilter1 (), stringFieldWidth2)).append (_break).append (NL)
		   .append (inputElement ("Filter 2", "filter2", form.getFilter2 (), stringFieldWidth2)).append (_break).append (NL)
		   .append (inputElement ("Filter 3", "filter3", form.getFilter3 (), stringFieldWidth2)).append (_break).append (NL)
		   .append ("</TD>").append (NL);

		StringBuffer sb2 = new StringBuffer (4096);
		int rows = AlbumFormInfo._NumTagParams / 3;
		for (int ii = 0; ii < AlbumFormInfo._NumTagParams; ii++) {
			if ((ii % rows) == 0) {
				sb2.append ("<TD ALIGN=CENTER>").append (NL);
			}
			sb2.append (radioButtons ("Tag", "tagMode" + ii, AlbumTagMode.getValues (), form.getTagMode (ii).getSymbol (), 2)).append (_spacing).append (NL);
			sb2.append (dropDown ("", "tag" + ii, allTags, form.getTag (ii))).append (_break).append (NL);
			if ((ii % rows) == (rows - 1)) {
				sb2.append ("</TD>").append (NL);
			}
		}

		StringBuffer sb3 = new StringBuffer (4096);
		sb3.append ("<TD ALIGN=CENTER>").append (NL)
		   .append ("<TD ALIGN=LEFT>").append (NL)
		   .append (inputElement ("Exclude 1", "exclude1", form.getExclude1 (), stringFieldWidth1)).append (_break).append (NL)
		   .append (inputElement ("Exclude 2", "exclude2", form.getExclude2 (), stringFieldWidth1)).append (_break).append (NL)
		   .append (inputElement ("Exclude 3", "exclude3", form.getExclude3 (), stringFieldWidth1)).append (_break).append (NL)
		   .append ("</TD>").append (NL)

		   .append ("</TR>").append (NL)
		   .append ("</TABLE>").append (NL)

//		   .append (inputElement ("Extension", "extension", form.getExtension (), 10)) //could make this hidden??
		   .append (radioButtons ("Columns", "columns", form.getColumns (), 1, form.getMaxColumns (), 4)).append (_break).append (NL)
		   .append (radioButtons ("Sort By", "sortType", AlbumSortType.getValues (/*visibleInUi*/ true), form.getSortType ().getSymbol (), 4)).append (_spacing).append (NL)
		   .append (checkbox ("Reverse Sort", "reverseSort", form.getReverseSort ())).append (_spacing).append (NL)
		   .append (dropDown ("Orientation", "orientation", AlbumOrientation.getValues (), form.getOrientation ().getSymbol ())).append (_break).append (NL)

		   .append (inputElement ("Panels", "panels", form.getPanels (), numberFieldWidth)).append (_spacing).append (NL)
//		   .append (inputElement ("Since Days", "sinceDays", form.getSinceDays (), numberFieldWidth)).append (_spacing).append (NL)
		   .append (inputElement ("Max Filters", "maxFilters", form.getMaxFilters (), numberFieldWidth)).append (_spacing).append (NL)
		   .append (inputElement ("Highlight Days", "highlightDays", form.getHighlightDays (), numberFieldWidth)).append (_spacing).append (NL)
		   .append (inputElement ("EXIF Date Index", "exifDateIndex", form.getExifDateIndex (), numberFieldWidth)).append (_spacing).append (NL)
		   .append (inputElement ("Max RGB Diffs", "maxRgbDiffs", form.getMaxRgbDiffs (), numberFieldWidth)).append (_spacing).append (NL)
//		   .append (inputElement ("Folder", "rootFolder", form.getRootFolder (), 4)).append (_spacing).append (NL)

//		   .append (inputElement ("", "slice", 1, 0)) //form submit always sets slice to 1
		   .append (inputElement ("", "slice", form.getSlice (), 0))
		   .append (checkbox ("Tag Oper OR", "tagFilterOperandOr", form.getTagFilterOperandOr ())).append (_spacing).append (NL)
		   .append (checkbox ("Collapse Groups", "collapseGroups", form.getCollapseGroups ())).append (_spacing).append (NL)
		   .append (checkbox ("Limited Compare", "limitedCompare", form.getLimitedCompare ())).append (_spacing).append (NL)
		   .append (checkbox ("DB Compare", "dbCompare", form.getDbCompare ())).append (_spacing).append (NL)
		   .append (checkbox ("Loose Compare", "looseCompare", form.getLooseCompare ())).append (_spacing).append (NL)
		   .append (checkbox ("Ignore Bytes", "ignoreBytes", form.getIgnoreBytes ())).append (_spacing).append (NL)
		   .append (checkbox ("EXIF Dates", "useExifDates", form.getUseExifDates ())).append (_spacing).append (NL)
		   .append (checkbox ("Use Case", "useCase", form.getUseCase ())).append (_spacing).append (NL)
		   .append (checkbox ("Clear Cache", "clearCache", false /*form.getClearCache ()*/))//.append (_spacing).append (NL)
//		   .append (checkbox ("Debug", "debug", AlbumFormInfo._Debug))
		   .append ("<P>").append (NL)

		   //place dummy hidden submit first, to act as default action when pressing RETURN
		   .append ("<div style=\"display:none\">").append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (form.getMode ().getSymbol ())
		   .append ("\">").append (NL).append ("</div>").append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoDir.getSymbol ())
		   .append ("\">").append (NL).append (_spacing).append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoDup.getSymbol ())
		   .append ("\">").append (NL).append (_spacing).append (NL)
		   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoSampler.getSymbol ())
		   .append ("\">").append (NL)

		   .append ("</CENTER>").append (NL);

		StringBuffer sb4 = new StringBuffer ();
		sb4.append ("</FORM>").append (NL)
		   .append ("</BODY>").append (NL)
		   .append ("</HTML>").append (NL);

		response.setContentType ("text/html");
		PrintWriter out = response.getWriter ();
		out.println (sb1.toString ());
		out.println (sb2.toString ());
		out.println (sb3.toString ());
		out.println (_album.generateHtml ());
		out.println (sb4.toString ());

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (/*showMemoryUsage*/ true);

		_log.debug ("--------------- AlbumServlet.doGet - done ---------------");

	} catch (Exception ee) {
		_log.error ("AlbumServlet.doGet", ee);
	}
	}

	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, String value, int size)
	{
		//hide control if prompt is empty string
		boolean isHidden = (prompt.length () == 0 ? true : false);

		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append ("<INPUT TYPE=\"")
		  .append (isHidden ? "HIDDEN" : "TEXT")
		  .append ("\" NAME=\"")
		  .append (name)
		  .append ("\" ID=\"")
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
		  .append ("<INPUT TYPE=\"CHECKBOX\" NAME=\"")
		  .append (name)
		  .append ("\"")
		  .append (isChecked ? " CHECKED" : "")
		  .append (">")
		  .append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String radioButtons (String prompt, String name, int selectedValue, int min, int max, int size)
	{
		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (NL);

		for (int ii = min; ii <= max; ii++) {
			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" VALUE=\"")
			  .append (ii)
			  .append ("\"")
			  .append (selectedValue == ii ? " CHECKED" : "")
			  .append (">")
			  .append (ii)
			  .append (NL);
		}

		sb.append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String radioButtons (String prompt, String name, AlbumStringPair[] values, String selectedValue, int size)
	{
		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (NL);

		for (AlbumStringPair value : values) {
//			_log.debug (ii + " " + values[ii].getName () + "/" + values[ii].getSymbol () + ": " + selectedValue);

			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" VALUE=\"")
			  .append (value.getSymbol ())
			  .append ("\"")
			  .append (value.getSymbol ().equals (selectedValue) ? " CHECKED" : "")
			  .append (">")
			  .append (value.getName ())
			  .append (NL);
		}

		sb.append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String dropDown (String prompt, String name, Collection<String> values, String selectedValue)
	{
		StringBuffer sb = new StringBuffer (1024);
		sb.append (prompt)
		  .append (NL);

		sb.append ("<SELECT NAME=\"")
		  .append (name)
		  .append ("\">")
		  .append (NL);

		for (String value : values) {
			sb.append ("<OPTION VALUE=\"")
			  .append (value)
			  .append ("\"")
			  .append (value.compareTo (selectedValue) == 0 ? " SELECTED" : "")
			  .append (">")
			  .append (value)
			  .append ("</OPTION>")
			  .append (NL);
		}

		sb.append ("</SELECT>")
		  .append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String dropDown (String prompt, String name, AlbumStringPair[] values, String selectedValue)
	{
		StringBuffer sb = new StringBuffer (1024);
		sb.append (prompt)
		  .append (NL);

		sb.append ("<SELECT NAME=\"")
		  .append (name)
		  .append ("\">")
		  .append (NL);

		for (AlbumStringPair value : values) {
			sb.append ("<OPTION VALUE=\"")
			  .append (value.getSymbol ())
			  .append ("\"")
			  .append (value.getSymbol ().equals (selectedValue) ? " SELECTED" : "")
			  .append (">")
			  .append (value.getName ())
			  .append ("</OPTION>")
			  .append (NL);
		}

		sb.append ("</SELECT>")
		  .append (NL);

		return sb.toString ();
	}

	//members
	private AlbumImages _album = null;

	private static String _break = "<BR>";
	private static String _spacing = "&nbsp"; //"&nbsp&nbsp";
//	private static String _spacingBreak = _spacing + _break;

	private static final String NL = System.getProperty ("line.separator");
	private static final String DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";
	private static final long serialVersionUID = 1L;

	private static Logger _log = LogManager.getLogger ();
}
