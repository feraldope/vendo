//WorksheetData.java

package com.vendo.worksheetServlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WorksheetData
{
	///////////////////////////////////////////////////////////////////////////
	public int init (WorksheetFormInfo form)
	{
		if (WorksheetFormInfo._Debug)
			_log.debug ("WorksheetData.init");

		_form = form;

		return 1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void processParams (HttpServletRequest request)
	{
	}

	///////////////////////////////////////////////////////////////////////////
	public int run ()
	{
		return 1;
	}

	///////////////////////////////////////////////////////////////////////////
	public String generateHtml ()
	{
		String font = "face='Comic Sans MS' size=4";

		StringBuffer blankRow = new StringBuffer (128);
		blankRow.append ("					<tr>" + NL)
				.append ("						<td>&nbsp</td>" + NL)
				.append ("					</tr>" + NL);

		StringBuffer block = new StringBuffer (1024);
		block.append ("			<td>" + NL);
		block.append ("				<table class='table1'>" + NL);
		block.append (blankRow.toString ());
		block.append ("					<tr>" + NL);
		block.append ("						<td>$(NUM1)</td>" + NL);
		block.append ("					</tr>" + NL);
		block.append ("					<tr>" + NL);
		block.append ("						<td><u>$(OPERATOR)$(NUM2)</u></td>" + NL);
		block.append ("					</tr>" + NL);
//		for (int ii = 0; ii < 2; ii++) {
			block.append (blankRow.toString ());
//		}
		block.append ("				</table>" + NL);
		block.append ("			</td>" + NL);

		StringBuffer html = new StringBuffer (10 * 1024);

		html.append ("<DL><p>" + NL);

		String border = (WorksheetFormInfo._Debug ? "1" : "0");
		html.append ("	<table class='table1'>" + NL);

		for (int row = 0; row < _form.getNumRows (); row++) {
			html.append ("		<tr>" + NL);

			for (int col = 0; col < _form.getNumCols (); col++) {
				int num1 = random (_form.getMaxNumber1 (), _form.getMinNumber1 ());
				int num2 = random (_form.getMaxNumber2 (), _form.getMinNumber2 ());

				//for subtraction, avoid results that are <= 0
				while (num2 >= num1) {
					if (WorksheetFormInfo._Debug)
						_log.debug ("equation rejected: " + num1 + " - " + num2);
					num1 = random (_form.getMaxNumber1 (), _form.getMinNumber1 ());
					num2 = random (_form.getMaxNumber2 (), _form.getMinNumber2 ());
				}

				String text = block.toString ().replace ("$(NUM1)", String.valueOf (num1) + _spacing1)
											   .replace ("$(NUM2)", String.valueOf (num2) + _spacing1)
											   .replace ("$(OPERATOR)", _spacing2 + " " + _form.getOperator ());

				html.append (text);
			}

			html.append ("		</tr>" + NL);
		}

		html.append ("	</table>" + NL);

		return html.toString ();

	}

	///////////////////////////////////////////////////////////////////////////
	private int random (int min, int max)
	{
		double d = Math.random () * (max - min) + min;
		return (int) (d + 0.5);
	}


	//members
	private WorksheetFormInfo _form = null;

//	public int _border = 0; //set to 0 normally, set to 1 for debugging

	private static String _spacing1 = "&nbsp";
	private static String _spacing2 = "&nbsp&nbsp";
	private static String _spacing3 = "&nbsp&nbsp&nbsp";
	private static String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger (WorksheetData.class);
}
