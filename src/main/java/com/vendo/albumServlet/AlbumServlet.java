//AlbumServlet.java

//Original from Core Servlets and JavaServer Pages 2nd Edition, http://www.coreservlets.com/

/*
http://localhost/servlet/com.vendo.apps.AlbumServlet
http://localhost/servlet/AlbumServlet.AlbumServlet
*/

package com.vendo.albumServlet;

import com.vendo.vendoUtils.VendoUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


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

		AlbumProfiling.getInstance ().print (false);

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
	public void doPost (HttpServletRequest request, HttpServletResponse response) //throws ServletException, IOException
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

			synchronized (requestInProgress) {
				if (requestInProgress.getAndSet (true)) {
					String message = "Request already in progress. Please retry later.";
					_log.error(message);

					response.setContentType ("text/html");
					PrintWriter out = response.getWriter ();
					out.println (message);

					_log.debug ("--------------- AlbumServlet.doGet - aborted ---------------");
//					return;
				}
			}

			//debugging
//			_log.debug ("Request Headers:" + NL + VendoUtils.getRequestHeaders (request));

			//check the value of this property
//			String property = "java.util.Arrays.useLegacyMergeSort";
//			_log.debug (property + " = " + System.getProperty (property));

			String fullUrl = VendoUtils.getFullUrlFromRequest(request);
			_log.debug ("AlbumServlet.doGet: fullUrl.length = " + fullUrl.length() + " chars");

//TODO - change CLI to read properties file, too
			ServletContext context = getServletContext ();

			final AlbumFormInfo form = AlbumFormInfo.getInstance (true); //force a new instance on every request
			form.processRequest (request, context); //also checks for debug request
			AlbumBeanUtilities.populateBean (form, request);

			AlbumProfiling.getInstance ().enter (1);

//			HttpSession session = request.getSession ();
//			_log.debug ("AlbumServlet.doGet: session id = " + session.getId ());
//			AlbumDirList.getInstance ().setSessionId (session.getId ());

			_album.processParams (request, form); //rename files marked for deletion
			_album = AlbumImages.getInstance (); //force reload of instance in case rootFolder changed
			_album.setForm (form);
			_album.processRequest ();

			final boolean isAndroidDevice = form.isAndroidDevice ();

			final String bgColor = _album.getBgColor ();
			final int filterFieldWidth = !isAndroidDevice ? 80 : 20;
			final int excludeFieldWidth = !isAndroidDevice ? 40 : 10;
			final int numberFieldWidth = 2;

			//get tags from database, add blank selection to beginning of list
			Collection<String> allTags = new ArrayList<> ();
			allTags.add ("");
			allTags.addAll (AlbumTags.getInstance ().getAllTags ());

			String title = _album.generateTitle ();

			String action = "";
			String method = form.getMethod ();
//			if (method.compareToIgnoreCase ("post") == 0) { //add appropriate action if method="post"
//				action = response.encodeURL (form.getServer ());
//				action = "action=\"" + action + "\" ";
//			}
			method = "method=\"" + method + "\" ";

			StringBuilder sb1 = new StringBuilder (4096);
			sb1.append (DOCTYPE).append (NL)
			   .append ("<HTML>").append (NL)
			   .append ("<HEAD>").append (NL)
			   .append ("<TITLE>").append(title).append("</TITLE>").append (NL)
			   .append ("</HEAD>").append (NL)
			   .append ("<BODY onload=\"handleSubmit()\" BGCOLOR=\"").append (bgColor).append ("\">").append (NL);

			if (!isAndroidDevice) {
				sb1.append("<style type='text/css'>").append(NL)
					.append("	body {").append(NL)
					.append("		font-family: Arial;").append(NL)
					.append("		font-size: 10pt;").append(NL)
					.append("	}").append(NL)
					.append("	h3 {").append(NL)
					.append("		font-size: 14pt;").append(NL)
					.append("	}").append(NL)
					.append("	td {").append(NL)
					.append("		font-size: 10pt;").append(NL)
					.append("	}").append(NL);
			} else {
				sb1.append("<style type='text/css'>").append(NL)
					.append("	body {").append(NL)
					.append("		font-family: Arial;").append(NL)
//					.append("		font-size: 20pt;").append(NL)
					.append("		font-size: 40pt;").append(NL)
					.append("	}").append(NL)

					//size the fields in the form
					.append("	form, input, label, textarea {").append(NL)
					.append("		font-family: Arial;").append(NL)
					.append("		font-size: 40pt;").append(NL)
					.append("	}").append(NL)

					//size the dropdowns in the form
					.append("	#tag0, #tag1, #tag2, #duplicateHandling, #orientation {").append(NL)
					.append("		font-family: Arial;").append(NL)
					.append("		font-size: 40pt;").append(NL)
					.append("		width: 150px;").append(NL)
					.append("	}").append(NL)

					//size the radiobuttons in the form
					.append("	#columns, #sortType {").append(NL)
					.append("		border: 0px;")
					.append("		width: 40px;")
					.append("		height: 40px;")
					.append("	}").append(NL)

					//size the checkboxes in the form
					.append("	input[type=checkbox] {")
					.append("		border: 0px;")
					.append("		width: 40px;")
					.append("		height: 40px;")
					.append("	}").append(NL)

					.append("	h3 {").append(NL)
//					.append("		font-size: 28pt;").append(NL)
					.append("		font-size: 40pt;").append(NL)
					.append("	}").append(NL)
					.append("	td {").append(NL)
//					.append("		font-size: 20pt;").append(NL)
					.append("		font-size: 40pt;").append(NL)
					.append("	}").append(NL);
			}

			List<Integer> fontSizes = Arrays.asList (8, 9, 10, 12, 16, 24, 28, 32, 36);
			double fonSizeMultiplier = !isAndroidDevice ? 1 : 1.5;
//			List<Integer> fontSizes = !isAndroidDevice ? Arrays.asList (8, 9, 10, 24, 36) : Arrays.asList (16, 18, 20, 48, 72);
			for (Integer fontSize : fontSizes) {
				sb1.append ("	td.fontsize").append (fontSize).append (" {").append (NL)
				   .append ("		font-size: ").append ((int) (fontSize * fonSizeMultiplier)).append ("pt;").append (NL)
				   .append ("	}").append (NL);
			}
			sb1.append ("</style>").append (NL);

			String tableWidthString = !isAndroidDevice ? "100%" : "2200"; //TODO - tablet hardcoded for portrait not landscape
			_log.debug ("AlbumServlet.doGet: tableWidthString = " + tableWidthString);

//attempt to turn off caching; copied from http://www.w3schools.com/tags/att_input_type.asp
//			sb1.append ("<META HTTP-EQUIV=\"pragma\" CONTENT=\"no-cache\" />").append (NL)
//			   .append ("<META HTTP-EQUIV=\"cache-control\" CONTENT=\"no-cache\" />").append (NL)
//			   .append ("<META HTTP-EQUIV=\"expires\" CONTENT=\"0\" />").append (NL);

			//check for javascript enabled
			sb1.append ("<div id='status'><span id='jscheck'>This page may not work correctly with JavaScript disabled</span>").append (NL)
			   .append ("	<script type='text/javascript'>").append (NL)
//			   .append ("		document.getElementById('jscheck').innerHTML = 'JavaScript enabled';").append (NL)
			   .append ("		document.getElementById('jscheck').innerHTML = '';").append (NL)
				//debugging navigator object
//			   .append ("		var uagent = navigator.userAgent.toLowerCase();").append (NL)
//			   .append ("		alert (uagent);").append (NL)
//			   .append ("		var nav = navigator;").append (NL)
//			   .append ("		var props = \"\";").append (NL)
//			   .append ("		for (var prop in nav) {").append (NL)
//			   .append ("			props += prop + \": \" + nav[prop] + \"\\n\";").append (NL)
//			   .append ("		}").append (NL)
//			   .append ("		alert (props);").append (NL)
			   .append ("	</script>").append (NL)
			   .append ("</div>").append (NL)

			   //javascript function handleSubmit
			   .append ("<SCRIPT TYPE=\"text/javascript\">").append (NL)
			   .append ("	function handleSubmit() {").append (NL)
				//avoid Firefox message when refreshing (when using method=post)
//			   .append ("		window.location = window.location;").append (NL)
			   //add display geometry
//			   .append ("		var screenWidth = document.getElementById (\"screenWidth\");").append (NL)
//			   .append ("		var screenHeight = document.getElementById (\"screenHeight\");").append (NL)
//			   .append ("		screenWidth.value = top.screen.width;").append (NL)
//			   .append ("		screenHeight.value = top.screen.height;").append (NL)
			   .append ("		var windowWidth = document.getElementById (\"windowWidth\");").append (NL)
			   .append ("		var windowHeight = document.getElementById (\"windowHeight\");").append (NL)
			   .append ("		windowWidth.value = top.window.innerWidth;").append (NL)
			   .append ("		windowHeight.value = top.window.innerHeight;").append (NL)
			   //add timestamp to URL to always causes server hit and avoid (reduce) caching
			   .append ("		var timestamp = document.getElementById (\"timestamp\");").append (NL)
			   .append ("		timestamp.value = (new Date ()).getTime ();").append (NL)
//TODO - not sure if I need this, since I can get user-agent from the request header
			   //add userAgent to URL for browser (and mobile) specific handling
			   .append ("		var userAgent = document.getElementById (\"userAgent\");").append (NL)
			   .append ("		userAgent.value = navigator.userAgent;").append (NL)
//			   .append ("		alert (\"userAgent.value=\" + userAgent.value);").append (NL)
			   .append ("		return true;").append (NL)
			   .append ("	}").append (NL)
//			   //javascript function setFocus
//			   .append ("	function setFocus() {").append (NL)
//			   .append ("		document.getElementById(\"filter1\").focus();").append (NL)
//			   .append ("		document.getElementById(\"filter1\").select();").append (NL)
//			   .append ("	}").append (NL)
			   .append ("</SCRIPT>").append (NL)

//testing
//			   .append ("<FORM name=\"AlbumForm\" onDOMContentLoaded=\"return handleSubmit()\" autocomplete=\"off\" ").append (method).append (action).append (">").append (NL)
//			   .append ("<FORM name=\"AlbumForm\" onbeforeprint=\"return handleSubmit()\" autocomplete=\"off\" ").append (method).append (action).append (">").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"screenWidth\" id=\"screenWidth\" />").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"screenHeight\" id=\"screenHeight\" />").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"windowWidth\" id=\"windowWidth\" />").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"windowHeight\" id=\"windowHeight\" />").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"timestamp\" id=\"timestamp\" />").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"userAgent\" id=\"userAgent\" />").append (NL)
//			   .append ("</FORM>").append (NL)

//			window.addEventListener('DOMContentLoaded', (event) => {
//					console.log('DOM fully loaded and parsed');
//});

//testing
//				.append ("	<form method=\"GET\" action=\"/AlbumServlet\">").append(NL)// action="your/page/url">
//				.append ("		<input type=\"hidden\" name=\"someName\" value=\"someValue\" />").append(NL)
//				.append ("		<input type=\"submit\" />").append(NL)
//				.append ("	</form>").append(NL)

//			   .append ("</HEAD>").append (NL)
//			   .append ("<BODY onload=\"setFocus()\" BGCOLOR=\"").append (bgColor).append ("\">").append (NL)
//			   .append ("<CENTER>").append (NL)
//			   .append ("<H3>").append (title).append ("</H3>").append (NL)

			   .append ("<FORM name=\"AlbumForm\" onsubmit=\"return handleSubmit()\" autocomplete=\"off\" ").append (method).append (action).append (">").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"screenWidth\" id=\"screenWidth\" />").append (NL)
//			   .append ("	<input type=\"hidden\" name=\"screenHeight\" id=\"screenHeight\" />").append (NL)
			   .append ("	<input type=\"hidden\" name=\"windowWidth\" id=\"windowWidth\" />").append (NL)
			   .append ("	<input type=\"hidden\" name=\"windowHeight\" id=\"windowHeight\" />").append (NL)
			   .append ("	<input type=\"hidden\" name=\"timestamp\" id=\"timestamp\" />").append (NL)
			   .append ("	<input type=\"hidden\" name=\"userAgent\" id=\"userAgent\" />").append (NL)
//form ends at bottom of page		   .append ("</FORM>").append (NL)

			   .append ("<TABLE WIDTH=").append (tableWidthString).append (" CELLPADDING=0 CELLSPACING=0 BORDER=0>").append (NL)
			   .append ("<TR>").append (NL)

			   .append ("<TD ALIGN=RIGHT>").append (NL)
			   .append (inputElement ("Filter 1", "filter1", form.getFilter1 (), filterFieldWidth)).append (_break).append (NL)
			   .append (inputElement ("Filter 2", "filter2", form.getFilter2 (), filterFieldWidth)).append (_break).append (NL)
			   .append (inputElement ("Filter 3", "filter3", form.getFilter3 (), filterFieldWidth)).append (_break).append (NL)
			   .append ("</TD>").append (NL);

			StringBuilder sb2 = new StringBuilder (4096);
			if (!isAndroidDevice) {
				int numTagParamsToUse = !isAndroidDevice ? AlbumFormInfo._NumTagParams : AlbumFormInfo._NumTagParams / 3;
				int rows = !isAndroidDevice ? numTagParamsToUse / 3 : numTagParamsToUse;
				for (int ii = 0; ii < numTagParamsToUse; ii++) {
					if ((ii % rows) == 0) {
						sb2.append("<TD ALIGN=CENTER>").append(NL);
					}
					sb2.append(radioButtons("Tag", "tagMode" + ii, AlbumTagMode.getValues(), form.getTagMode(ii).getSymbol(), 2)).append(_spacing).append(NL);
					sb2.append(dropDown("", "tag" + ii, allTags, form.getTag(ii))).append(_break).append(NL);
					if ((ii % rows) == (rows - 1)) {
						sb2.append("</TD>").append(NL);
					}
				}
			}

			StringBuilder sb3 = new StringBuilder (4096);
			int maxColumns = !isAndroidDevice ? form.getMaxColumns () : 2; //hack - for android, only 1 or 2 columns (see also AlbumImages.java)

			sb3.append ("<TD ALIGN=CENTER>").append (NL)
//			   .append ("<TD ALIGN=LEFT>").append (NL)
			   .append (inputElement ("Exclude 1", "exclude1", form.getExclude1 (), excludeFieldWidth)).append (_break).append (NL)
			   .append (inputElement ("Exclude 2", "exclude2", form.getExclude2 (), excludeFieldWidth)).append (_break).append (NL)
			   .append (inputElement ("Exclude 3", "exclude3", form.getExclude3 (), excludeFieldWidth)).append (_break).append (NL)
			   .append ("</TD>").append (NL)
			   .append ("</TR>").append (NL)
			   .append ("</TABLE>").append (NL)

			   .append ("<TABLE WIDTH=").append (tableWidthString).append (" CELLPADDING=0 CELLSPACING=0 BORDER=0>").append (NL)
			   .append ("<TR>").append (NL)
			   .append ("<TD ALIGN=CENTER>").append (NL)

//			   .append (inputElement ("Extension", "extension", form.getExtension (), 10)) //could make this hidden??
			   .append (radioButtons ("Columns", "columns", form.getColumns (), 1, maxColumns, 4)).append (_break).append (NL)
			   .append ("</TD>").append (NL)
			   .append ("</TR>").append (NL)

			   .append ("<TR>").append (NL)
			   .append ("<TD ALIGN=CENTER>").append (NL)
			   .append (radioButtons ("Sort By", "sortType", AlbumSortType.getValues (true), form.getSortType ().getSymbol (), 4)).append (_spacing).append (NL)
			   .append (checkbox ("Reverse Sort", "reverseSort", form.getReverseSort ())).append (_spacing).append (NL)
			   .append (checkbox ("Interleave Sort", "interleaveSort", form.getInterleaveSort ())).append (_spacing).append (NL)
			   .append (dropDown ("Duplicate Handling", "duplicateHandling", AlbumDuplicateHandling.getValues (), AlbumDuplicateHandling.SelectNone.toString ())).append (_spacing).append (NL)
			   .append (dropDown ("Orientation", "orientation", AlbumOrientation.getValues (), form.getOrientation ().getSymbol ())).append (_break).append (NL)

			   .append (inputElement ("Panels", "panels", form.getPanels (), numberFieldWidth)).append (_spacing).append (NL)
//			   .append (inputElement ("Since Days", "sinceDays", form.getSinceDays (), numberFieldWidth)).append (_spacing).append (NL)
			   .append (inputElement ("Max Filters", "maxFilters", form.getMaxFilters (), numberFieldWidth)).append (_spacing).append (NL)
			   .append (inputElement ("Highlight Days", "highlightDays", form.getHighlightDays (), numberFieldWidth)).append (_spacing).append (NL)
			   .append (inputElement ("EXIF Date Index", "exifDateIndex", form.getExifDateIndex (), numberFieldWidth)).append (_spacing).append (NL)
			   .append (inputElement ("Max StdDev", "maxStdDev", form.getMaxStdDev (), numberFieldWidth)).append (_spacing).append (NL)
			   .append (inputElement ("Large Album*", "minImagesToFlagAsLargeAlbum", form.getMinImagesToFlagAsLargeAlbum (), numberFieldWidth)).append (_spacing).append (NL)
//			   .append (inputElement ("Folder", "rootFolder", form.getRootFolder (), 4)).append (_spacing).append (NL)

//			   .append (inputElement ("", "slice", 1, 0)) //form submit always sets slice to 1
			   .append (inputElement ("", "slice", form.getSlice (), 0))
			   .append (checkbox ("Tag Oper OR", "tagFilterOperandOr", form.getTagFilterOperandOr ())).append (_spacing).append (NL)
			   .append (checkbox ("Collapse Groups", "collapseGroups", form.getCollapseGroups ())).append (_spacing).append (NL)
			   .append (checkbox ("Limited Compare", "limitedCompare", form.getLimitedCompare ())).append (_spacing).append (NL)
			   .append (checkbox ("DB Compare", "dbCompare", form.getDbCompare ())).append (_spacing).append (NL)
			   .append (checkbox ("Loose Compare", "looseCompare", form.getLooseCompare ())).append (_spacing).append (NL)
			   .append (checkbox ("Ignore Bytes", "ignoreBytes", form.getIgnoreBytes ())).append (_spacing).append (NL)
			   .append (checkbox ("EXIF Dates", "useExifDates", form.getUseExifDates ())).append (_spacing).append (NL)
			   .append (checkbox ("Use Case", "useCase", form.getUseCase ())).append (_spacing).append (NL)
			   .append (checkbox ("Clear Cache", AlbumFormInfo._ClearCacheParam, false /*form.getClearCache ()*/))//.append (_spacing).append (NL)
//			   .append (checkbox ("Debug", "debug", AlbumFormInfo._Debug))
			   .append ("</TD>").append (NL)
			   .append ("</TR>").append (NL)
//			   .append ("<P>").append (NL)

			   //place dummy hidden submit first, to act as default action when pressing RETURN
			   .append ("<TR>").append (NL)
			   .append ("<TD ALIGN=CENTER>").append (NL)
			   .append ("<div style=\"display:none\">").append (NL)
			   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (form.getMode ().getSymbol ())
			   .append ("\">").append (NL).append ("</div>").append (NL)
			   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoDir.getSymbol ())
			   .append ("\">").append (NL).append (_spacing).append (NL)
			   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoDup.getSymbol ())
			   .append ("\">").append (NL).append (_spacing).append (NL)
			   .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"mode\" VALUE=\"").append (AlbumMode.DoSampler.getSymbol ())
			   .append ("\">").append (NL)
			   .append ("</TD>").append (NL)
			   .append ("</TR>").append (NL)

//			   .append ("</CENTER>").append (NL)
			   .append ("</TABLE>").append (NL);
//			   .append ("</FORM>").append (NL);

			StringBuilder sb4 = new StringBuilder ();
			sb4.append ("</FORM>").append (NL)
			   .append ("</BODY>").append (NL)
			   .append ("</HTML>").append (NL);

			String html = _album.generateHtml ();

			response.setContentType ("text/html");
			PrintWriter out = response.getWriter ();

			//TODO - this isn't quite right
			if (form.getForceBrowserCacheRefresh ()) {
				_log.debug ("AlbumServlet.doGet: force browser cache refresh");
				response.setHeader("Cache-Control", "no-cache"); //Forces caches to obtain a new copy of the page from the origin server
				response.setHeader("Cache-Control", "max-age=0");
				response.setHeader("Cache-Control", "must-revalidate");
//				response.setHeader("Cache-Control","no-store"); //Directs caches not to store the page under any circumstance
				response.setDateHeader("Expires", -1); //Causes the proxy cache to see the page as "stale"
//				response.setHeader("Pragma","no-cache"); //HTTP 1.0 backward compatibility
			}

			out.println (sb1);
			out.println (sb2);
			out.println (sb3);
			out.println (html);
			out.println (sb4);

			AlbumProfiling.getInstance ().exit (1);

			AlbumProfiling.getInstance ().print (true);

			if (false) { //debugging
				Path rootPath = FileSystems.getDefault ().getPath (AlbumFormInfo.getInstance ().getRootPath (false));
				Path htmlFile = FileSystems.getDefault ().getPath (rootPath.toString (), "page.html");
				try (FileOutputStream outputStream = new FileOutputStream (htmlFile.toFile ())) {
					outputStream.write (sb1.toString ().getBytes ());
					outputStream.write (sb2.toString ().getBytes ());
					outputStream.write (sb3.toString ().getBytes ());
					outputStream.write (html.getBytes ());
					outputStream.write (sb4.toString ().getBytes ());
					outputStream.flush ();
//					outputStream.close (); //try-with-resources closes automatically

				} catch (IOException ee) {
					_log.error ("AlbumServlet.doGet: error writing html file: " + htmlFile.toString () + NL);
					_log.error (ee); //print exception, but no stack trace
				}
			}

			requestInProgress.set (false);

			_log.debug ("--------------- AlbumServlet.doGet - done ---------------");

		} catch (Exception ee) {
			_log.error ("AlbumServlet.doGet", ee);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, String value, int size)
	{
		//hide control if prompt is empty string
		boolean isHidden = prompt.length () == 0;

		StringBuilder sb = new StringBuilder (128);
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
		if (value != 0) {
			num = String.valueOf (value);
		}

		return (inputElement (prompt, name, num, size));
	}

	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, double value, int size)
	{
		String num = "";
		if (value != 0) {
			num = String.valueOf (value);
		}

		//strip trailing zeros (assumes decimal point is always included)
		while (num.endsWith ("0")) {
			num = num.substring (0, num.length () - 1);
		}
		if (num.endsWith (".")) {
			num = num.substring (0, num.length () - 1);
		}

		return (inputElement (prompt, name, num, size));
	}

	///////////////////////////////////////////////////////////////////////////
	private String checkbox (String prompt, String name, boolean isChecked)
	{
		StringBuilder sb = new StringBuilder (128);
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
		StringBuilder sb = new StringBuilder (128);
		sb.append (prompt)
		  .append (NL);

		for (int ii = min; ii <= max; ii++) {
			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" ID=\"")
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
		StringBuilder sb = new StringBuilder (128);
		sb.append (prompt)
		  .append (NL);

		for (AlbumStringPair value : values) {
//			_log.debug (ii + " " + values[ii].getName () + "/" + values[ii].getSymbol () + ": " + selectedValue);

			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" ID=\"")
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
		StringBuilder sb = new StringBuilder (1024);
		sb.append (prompt)
		  .append (NL);

		sb.append ("<SELECT NAME=\"")
		  .append (name)
		  .append ("\" ID=\"")
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
		StringBuilder sb = new StringBuilder (1024);
		sb.append (prompt)
		  .append (NL);

		sb.append ("<SELECT NAME=\"")
		  .append (name)
		  .append ("\" ID=\"")
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

	public static final AtomicBoolean requestInProgress = new AtomicBoolean (false);

	private static final String _break = "<BR>";
	private static final String _spacing = "&nbsp;"; //"&nbsp;&nbsp;";
//	private static final String _spacingBreak = _spacing + _break;
	private static final String NL = System.getProperty ("line.separator");
	private static final String DOCTYPE = "<!DOCTYPE HTML>";// PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n";
	private static final long serialVersionUID = 1L;

	private static final Logger _log = LogManager.getLogger ();
}
