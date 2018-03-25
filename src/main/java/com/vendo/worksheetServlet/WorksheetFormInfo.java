//WorksheetFormInfo.java

package com.vendo.worksheetServlet;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class WorksheetFormInfo
{
//TODO - make this a global enum
//	public enum Mode {DoDir, DoDup, DoSampler};

	///////////////////////////////////////////////////////////////////////////
	public WorksheetFormInfo ()
	{
		_log.debug ("WorksheetFormInfo: no-param ctor");
	}

	///////////////////////////////////////////////////////////////////////////
	public WorksheetFormInfo (HttpServletRequest request)
	{
		boolean gotDebugParam = false;
		boolean gotSuppressFormParam = false;

		//handle checkbox form parameters
		Enumeration paramNames = request.getParameterNames ();
		while (paramNames.hasMoreElements ()) {
			String paramName = (String) paramNames.nextElement ();

			if (true) { //debug - log form parameters
				String[] paramValues = request.getParameterValues (paramName);
				_log.debug ("WorksheetFormInfo ctor: param: " + paramName + " = " + paramValues[0]);
			}

			if (paramName.equals ("debug")) {
				_log.debug ("WorksheetFormInfo ctor: got debug");
				gotDebugParam = true;

			} else if (paramName.equals ("suppressForm")) {
				_log.debug ("WorksheetFormInfo ctor: got suppressForm");
				gotSuppressFormParam = true;

/*
			} else if (paramName.equals ("doDir")) {
				if (_Debug)
					_log.debug ("WorksheetFormInfo ctor: got doDir");
				setMode (Mode.DoDir);

			} else if (paramName.equals ("doDup")) {
				if (_Debug)
					_log.debug ("WorksheetFormInfo ctor: got doDup");
				setMode (Mode.DoDup);

			} else if (paramName.equals ("doSampler")) {
				if (_Debug)
					_log.debug ("WorksheetFormInfo ctor: got doSampler");
				setMode (Mode.DoSampler);
*/
			}
		}

		_Debug = gotDebugParam;
		_suppressForm = gotSuppressFormParam;
	}

	///////////////////////////////////////////////////////////////////////////
	public String toString1 ()
	{
/*
		StringBuffer sb = new StringBuffer (128);
		sb.append ("filter1 = ").append (getFilter1 ()).append (NL)
		  .append ("filter2 = ").append (getFilter2 ()).append (NL)
		  .append ("filter3 = ").append (getFilter3 ()).append (NL)
//		  .append ("extension = ").append (getExtension ()).append (NL)
		  .append ("columns = ").append (getColumns ()).append (NL)
		  .append ("panels = ").append (getPanels ()).append (NL)
		  .append ("sinceDays = ").append (getSinceDays ()).append (NL)
		  .append ("mode = ").append (getMode ());//.append (NL);

		return sb.toString ();
*/
		return "TBD";
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMinNumber1 (int minNumber1)
	{
		_minNumber1 = minNumber1;
	}

	public int getMinNumber1 ()
	{
		return _minNumber1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMaxNumber1 (int maxNumber1)
	{
		_maxNumber1 = maxNumber1;
	}

	public int getMaxNumber1 ()
	{
		return _maxNumber1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMinNumber2 (int minNumber2)
	{
		_minNumber2 = minNumber2;
	}

	public int getMinNumber2 ()
	{
		return _minNumber2;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMaxNumber2 (int maxNumber2)
	{
		_maxNumber2 = maxNumber2;
	}

	public int getMaxNumber2 ()
	{
		return _maxNumber2;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setOperator (String operator)
	{
		_operator = operator.trim ();
	}

	public String getOperator ()
	{
		return _operator;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setNumRows (int numRows)
	{
		_numRows = numRows;
	}

	public int getNumRows ()
	{
		return _numRows;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setNumCols (int numCols)
	{
		_numCols = numCols;
	}

	public int getNumCols ()
	{
		return _numCols;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public void setWindowWidth (int windowWidth)
	{
		if (windowWidth > 0)
			_windowWidth = windowWidth;
	}

	public int getWindowWidth ()
	{
		return _windowWidth;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setWindowHeight (int windowHeight)
	{
		if (windowHeight > 0)
			_windowHeight = windowHeight;
	}

	public int getWindowHeight ()
	{
		return _windowHeight;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	public void setSuppressForm (boolean suppressForm)
	{
		_suppressForm = suppressForm;
	}

	public boolean getSuppressForm ()
	{
		return _suppressForm;
	}

/*
	///////////////////////////////////////////////////////////////////////////
	public void setMode (Mode mode)
	{
		_mode = mode;
	}

	public Mode getMode ()
	{
		return _mode;
	}

	public boolean isModeDir () //helper since Mode enum is not global
	{
		return _mode == Mode.DoDir;
	}

	public boolean isModeDup () //helper since Mode enum is not global
	{
		return _mode == Mode.DoDup;
	}

	public boolean isModeSampler () //helper since Mode enum is not global
	{
		return _mode == Mode.DoSampler;
	}
*/


	//private members
	private String _operator = "-";
	private int _minNumber1 = 1;
	private int _maxNumber1 = 20;
	private int _minNumber2 = 1;
	private int _maxNumber2 = 10;

	private int _numRows = 7;
	private int _numCols = 7;

	public boolean _suppressForm = false;

/*
	//hardcoded values for firefox at 1600 x 1200 resolution at 66% wide
	private int _windowWidth = (1540 * 66) / 100;
	private int _windowHeight = 980;
*/

//	private Mode _mode = Mode.DoDir;

	public static boolean _Debug = false; //global variable - NOT PART OF BEAN

	private final String NL = System.getProperty ("line.separator");

	private static Logger _log = LogManager.getLogger (WorksheetFormInfo.class);
}
