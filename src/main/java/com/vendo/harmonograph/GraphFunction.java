//GraphFunction.java

package com.vendo.harmonograph;

import java.util.*;

import java.awt.geom.*;
//import java.awt.geom.Point2D.*;

public enum GraphFunction
{
	COS ("cos"),
	SIN ("sin"),
	TAN ("tan"),
	UNITY ("unity"),
	COS_NEG ("-cos"),
	SIN_NEG ("-sin"),
	TAN_NEG ("-tan");

	//static members
//TODO - use a HashMap?
	private static final List<String> _functionNames = new ArrayList<String>();
	private static int _numItems = 0;

	///////////////////////////////////////////////////////////////////////////
	static
	{
		for (GraphFunction ff : values ()) {
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
	public static GraphFunction get (int index)
	{
		//brute-force method
		int ii = 0;
		for (GraphFunction ff : values ()) {
			if (index == ii)
				return ff;
			ii++;
		}

		return UNITY;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getLength ()
	{
		return _numItems;
//		return 6;
	}

	///////////////////////////////////////////////////////////////////////////
	GraphFunction (String name)
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
	public boolean isTrigFunction ()
	{
		switch (this) {
		case COS:
		case SIN:
		case TAN:
		case COS_NEG:
		case SIN_NEG:
		case TAN_NEG:
			return true;

		case UNITY:
			return false;

		default:
			throw new AssertionError ("Unknown enum: " + this);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//desired behavior is that (-1 <= result <= +1)
	public double evaluate (double theta, double phase, double frequency)
	{
		double newPhase = (theta + phase) * frequency;

		switch (this) {
		default:		throw new AssertionError ("Unknown enum: " + this);
		case COS:		return Math.cos (Math.toRadians (newPhase));
		case SIN:		return Math.sin (Math.toRadians (newPhase));
		case TAN:		return Math.tan (Math.toRadians (newPhase));
		case COS_NEG:	return -Math.cos (Math.toRadians (newPhase));
		case SIN_NEG:	return -Math.sin (Math.toRadians (newPhase));
		case TAN_NEG:	return -Math.tan (Math.toRadians (newPhase));
		case UNITY:		return ((newPhase % 360) / 180) - 1;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//convert coord to polar, rotate, convert back to rectangular
	public static Point2D rotate (Point2D point, double radians)
	{
		double xx = point.getX ();
		double yy = point.getY ();
		double theta = Math.atan2 (yy, xx);
		double radius = Math.sqrt (xx * xx + yy * yy);

		theta += radians;
		xx = radius * Math.cos (theta);
		yy = radius * Math.sin (theta);

		return new Point2D.Double (xx, yy);
	}

	//members
	private final String _name;
	private int _index;
}
