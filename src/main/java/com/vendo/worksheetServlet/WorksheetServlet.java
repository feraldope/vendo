//WorksheetServlet.java

//Original from Core Servlets and JavaServer Pages 2nd Edition, http://www.coreservlets.com/

/*
http://localhost/servlet/com.vendo.apps.WorksheetServlet
http://localhost/servlet/WorksheetServlet.WorksheetServlet
*/

package com.vendo.worksheetServlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WorksheetServlet extends HttpServlet
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		WorksheetFormInfo._Debug = true;

		WorksheetServlet servlet = new WorksheetServlet ();

//		if (!servlet.processArgs (args))
//			System.exit (1); //processArgs displays error

//		if (args.length != 1) {
//			System.err.println ("Usage: java WorksheetServlet <TBD>");
//			return;
//		}

		servlet.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		_log.debug ("WorksheetServlet.run");

		WorksheetData data = new WorksheetData ();
		WorksheetFormInfo form = new WorksheetFormInfo ();
		data.init (form);
//		data.run ();

		System.out.println (generateHeader (form, false));
		System.out.println (data.generateHtml ());

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public void init () throws ServletException
	{
		_log.debug ("WorksheetServlet.init");
	}

	///////////////////////////////////////////////////////////////////////////
	public void doGet (HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException
	{
		WorksheetFormInfo form = new WorksheetFormInfo (request); //checks for debug param
		WorksheetBeanUtilities.populateBean (form, request);

		_log.debug ("--------------- WorksheetServlet.doGet ---------------");

//		HttpSession session = request.getSession ();
//		_log.debug ("WorksheetServlet.doGet: session id = " + session.getId ());

		WorksheetData data = new WorksheetData ();
		data.init (form);
		data.run ();

		boolean showForm = !form.getSuppressForm ();

		StringBuffer sb = new StringBuffer ();
		sb.append ("</FORM>").append (NL)
		  .append ("</BODY>").append (NL)
		  .append ("</HTML>").append (NL);

		response.setContentType ("text/html");
		PrintWriter out = response.getWriter ();

		out.println (generateHeader (form, showForm));
		out.println (data.generateHtml ());
		out.println (sb.toString ());

		_log.debug ("--------------- WorksheetServlet.doGet - done ---------------");
	}

	///////////////////////////////////////////////////////////////////////////
	private String generateHeader (WorksheetFormInfo form, boolean showForm)
	{
		String title = "Worksheet";
//		int fontSize = (WorksheetFormInfo._Debug ? 10 : 24);
		int fontSize = 19;

		StringBuffer sb = new StringBuffer (4096);
		sb.append (DOCTYPE).append (NL)
		  .append ("<HTML>").append (NL)
//		  .append ("<A NAME=\"AlbumTop\"></A>").append (NL)

		  .append ("<HEAD><TITLE>" + title + "</TITLE>").append (NL)

/*
		  //check for javascript enabled
		  .append ("<div id='status'><span id='jscheck'>This page may not work correctly with JavaScript disabled</span>").append (NL)
		  .append ("	<script type='text/javascript'>").append (NL)
//		  .append ("		document.getElementById('jscheck').innerHTML = 'JavaScript enabled';").append (NL)
		  .append ("		document.getElementById('jscheck').innerHTML = '';").append (NL)
		  .append ("	</script>").append (NL)
		  .append ("</div>").append (NL)

		  //javascript function handleSubmit
		  .append ("<SCRIPT TYPE=\"text/javascript\">").append (NL)
		  .append ("	function handleSubmit() {").append (NL)
		  .append ("		var widthHidden = document.getElementById (\"windowWidth\");").append (NL)
		  .append ("		var heightHidden = document.getElementById (\"windowHeight\");").append (NL)
		  .append ("		widthHidden.value = top.window.innerWidth;").append (NL)
		  .append ("		heightHidden.value = top.window.innerHeight;").append (NL)
		  .append ("		return true;").append (NL)
		  .append ("	}").append (NL)
		  .append ("</SCRIPT>").append (NL)
*/

		  //style sheet
		  .append ("<style type='text/css'>").append (NL)
		  .append ("	body {").append (NL)
		  .append ("		font-family: 'Comic Sans MS';").append (NL)
		  .append ("		font-size: 14px;").append (NL)
		  .append ("	}").append (NL)
		  .append ("	h1 {").append (NL)
		  .append ("		font-family: 'Comic Sans MS';").append (NL)
		  .append ("	}").append (NL)
		  .append ("	.table1 {").append (NL)
		  .append ("		font-size: " + fontSize + "px;").append (NL)
//		  .append ("		border-width: 3px 3px 3px 3px;").append (NL)
//		  .append ("		border-spacing: 1px;").append (NL)
//		  .append ("		border-color: red green blue yellow;").append (NL)
		  .append ("		padding: 0px;").append (NL)
		  .append ("		margin: 0px;").append (NL)
//		  .append ("		height: 40px;").append (NL)
		  .append ("		width: 100%").append (NL)
		  .append ("	}").append (NL)
		  .append ("	.table1 td {").append (NL)
		  .append ("		font-size: " + fontSize + "px;").append (NL)
		  .append ("		text-align: right;").append (NL)
//		  .append ("		border-width: 3px 3px 3px 3px;").append (NL)
//		  .append ("		border-spacing: 0px;").append (NL)
		  .append ("		padding: 0px;").append (NL)
		  .append ("		margin: 0px;").append (NL)
		  .append ("	}").append (NL)
		  .append ("</style>").append (NL)

		  .append ("</HEAD>").append (NL)

		  .append ("<BODY><CENTER>").append (NL);
//		  .append ("<H3>" + title + "</H3>").append (NL);

		if (showForm) {
			sb.append ("<FORM name=\"Album\" onsubmit=\"return handleSubmit()\" autocomplete=\"off\">").append (NL)
			  .append ("	<input type=\"hidden\" name=\"windowWidth\" id=\"windowWidth\" />").append (NL)
			  .append ("	<input type=\"hidden\" name=\"windowHeight\" id=\"windowHeight\" />").append (NL)

			  .append (inputElement ("Minimum number 1", "minNumber1", form.getMinNumber1 (), 5))
			  .append (inputElement ("Maximum number 1", "maxNumber1", form.getMaxNumber1 (), 5))
			  .append (inputElement ("Minimum number 2", "minNumber2", form.getMinNumber2 (), 5))
			  .append (inputElement ("Maximum number 2", "maxNumber2", form.getMaxNumber2 (), 5))
			  .append (inputElement ("Operator", "operator", form.getOperator (), 5))
			  .append (inputElement ("Number of rows", "numRows", form.getNumRows (), 5))
			  .append (inputElement ("Number of columns", "numCols", form.getNumCols (), 5))

			  .append (checkbox ("suppressForm", "suppressForm", true))
			  .append (checkbox ("Debug", "debug", WorksheetFormInfo._Debug))
			  .append ("<P>").append (NL)

			  .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"generate\" VALUE=\"Generate Worksheet\">").append (NL)
//			  .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"doDir\" VALUE=\"Generate Worksheet\">").append (NL)
//			  .append (_spacing3).append (NL)
//			  .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"doDup\" VALUE=\"Dup All\">").append (NL)
//			  .append (_spacing3).append (NL)
//			  .append ("<INPUT TYPE=\"SUBMIT\" NAME=\"doSampler\" VALUE=\"Sampler\">").append (NL)

			  .append ("</CENTER>").append (NL);
		}

		return sb.toString ();
	}

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
		  .append (isHidden ? "" : "<BR>")
		  .append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String inputElement (String prompt, String name, int value, int size)
	{
		String num;

		if (value == 0)
			num = "";
		else
			num = String.valueOf (value);

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
		  .append ("><BR>").append (NL);

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String radioButtons (String prompt, String name, int value, int min, int max, int size)
	{
		StringBuffer sb = new StringBuffer (128);
		sb.append (prompt)
		  .append (": ");

		for (int ii = min; ii <= max; ii++) {
			sb.append ("<INPUT TYPE=\"RADIO\" NAME=\"")
			  .append (name)
			  .append ("\" SIZE=\"")
			  .append (size)
			  .append ("\" VALUE=\"")
			  .append (ii)
			  .append ("\"")
			  .append (value == ii ? " CHECKED" : "")
			  .append (">")
			  .append (ii)
			  .append (NL);
		}

		sb.append ("<BR>").append (NL);

		return sb.toString ();
	}

	//members
	private static String _spacing3 = "&nbsp&nbsp&nbsp";

	private final String NL = System.getProperty ("line.separator");
	private final String DOCTYPE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">";

	private static Logger _log = LogManager.getLogger (WorksheetServlet.class);
}
