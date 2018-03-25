//ColorFunction.java

package com.vendo.harmonograph;

import java.util.*;

public enum ColorFunction
{
	ON ("on"),
	OFF ("off"),
	X_ASC ("X (asc)"),
	X_DESC ("X (desc)"),
	Y_ASC ("Y (asc)"),
	Y_DESC ("Y (desc)"),
	THETA_ASC ("theta (asc)"),
	THETA_DESC ("theta (desc)");

	//static members
//TODO - use a HashMap?
	private static final List<String> _functionNames = new ArrayList<String>();
	private static int _numItems = 0;

	///////////////////////////////////////////////////////////////////////////
	static
	{
		for (ColorFunction ff : values ()) {
			_functionNames.add (ff.getName ());
			ff._index = _numItems++;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static String[] getNames ()
	{
		return _functionNames.toArray (new String[_functionNames.size ()]);
	}

	///////////////////////////////////////////////////////////////////////////
	public static ColorFunction get (int index)
	{
		//brute-force method
		int ii = 0;
		for (ColorFunction ff : values ()) {
			if (index == ii)
				return ff;
			ii++;
		}

		return ON;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getLength ()
	{
		return _numItems;
//		return 3;
	}

	///////////////////////////////////////////////////////////////////////////
	ColorFunction (String name)
	{
		_name = name;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getIndex ()
	{
		return _index;
	}

	///////////////////////////////////////////////////////////////////////////
	public double evaluate (double xx, double yy, double theta)
	{
		switch (this) {
		default:			throw new AssertionError ("Unknown enum: " + this);
		case ON:			return 1;
		case OFF:			return 0;
		case X_ASC:			return Math.max (Math.min ((1 + xx) / 2, 1), 0);
		case X_DESC:		return Math.max (Math.min ((1 - xx) / 2, 1), 0);
		case Y_ASC:			return Math.max (Math.min ((1 - yy) / 2, 1), 0);
		case Y_DESC:		return Math.max (Math.min ((1 + yy) / 2, 1), 0);
		case THETA_ASC:		return theta / 360;
		case THETA_DESC:	return 1 - theta / 360;
		}
	}

	//members
	private final String _name;
	private int _index;
}
