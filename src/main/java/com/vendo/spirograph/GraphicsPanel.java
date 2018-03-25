//GraphicsPanel.java

package com.vendo.spirograph;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Date;

import javax.swing.JPanel;

public class GraphicsPanel extends JPanel
{
	///////////////////////////////////////////////////////////////////////////
	public void paintComponent (Graphics g)
	{
		super.paintComponent (g);
		Graphics2D g2d = (Graphics2D) g;

		getParent ().getParent ().setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));

		long startMillis = new Date ().getTime ();

		Dimension panelSize = getSize (null);
		int currentWidth = (int) panelSize.getWidth ();
		int currentHeight = (int) panelSize.getHeight ();
		int currentSize = Math.min (currentWidth, currentHeight);

		g2d.translate (currentWidth / 2, currentHeight / 2);

		g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int prevX = 0;
		int prevY = 0;
		double scale = (double) currentSize;

		//calculate numberOfRevs
		int numberOfRevs = (int) (_radius2 / gcd ((int) _radius1, (int) _radius2)) * 2;
		if (numberOfRevs < 0)
			numberOfRevs = -numberOfRevs;
		if (_debug)
			System.out.println ("numberOfRevs = " + numberOfRevs);

		double finalTheta = Math.PI * (double) numberOfRevs;
		double thetaStep = Math.PI / (double) _pointsPerRev;

		boolean firstPass = true;
		for (double theta = 0; theta <= finalTheta; theta += thetaStep) {

//TODO - use GeneralPath

			//after transform, (-1 <= val <= +1)
			Point2D.Double point = (Point2D.Double) evaluate (theta, _radius1, _radius2, _offset);
			double xx = point.getX ();
			double yy = point.getY ();

			//after transform, -(scale / 2) <= val <= +(scale / 2)
			int currX = (int) (xx * scale / 2);
			int currY = (int) (yy * scale / 2);

			if (firstPass)
				firstPass = false;
			else {
				g2d.drawLine (prevX, prevY, currX, currY);
			}

			prevX = currX;
			prevY = currY;
		}

		if (true) { //_debug) {
			long elapsedMillis = new Date ().getTime () - startMillis;
			System.out.println ("GraphicsPanel.paintComponent: elapsed: " + elapsedMillis + " ms");
		}

		getParent ().getParent ().setCursor (Cursor.getDefaultCursor ());
	}

	///////////////////////////////////////////////////////////////////////////
	//from http://wordsmith.org/~anu/java/spirograph.html
	public int gcd (int x, int y)
	{
		if (x%y == 0)
			return y;

		return gcd (y, x%y);
	}

	///////////////////////////////////////////////////////////////////////////
	public Point2D evaluate (double theta, double radius1, double radius2, double pp)
	{
		radius1 /= 100.;
		radius2 /= 100.;
		pp /= 100.;

		double radiusSum = radius1 + radius2;
		double radiusFac = radiusSum * theta / radius2;

		double x = radiusSum * Math.cos (theta) - pp * Math.cos (radiusFac);
		double y = radiusSum * Math.sin (theta) - pp * Math.sin (radiusFac);

		Point2D point = new Point2D.Double (x, y);
		return point;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRadius1 (int radius1)
	{
		if (_radius1 != radius1) {
			_radius1 = radius1;
			repaint ();
		}
	}

	public int getRadius1 ()
	{
		return _radius1;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRadius2 (int radius2)
	{
		if (_radius2 != radius2) {
			_radius2 = radius2;
			repaint ();
		}
	}

	public int getRadius2 ()
	{
		return _radius2;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setOffset (int offset)
	{
		if (offset < 0)
			offset = 0;

		if (_offset != offset) {
			_offset = offset;
			repaint ();
		}
	}

	public int getOffset ()
	{
		return _offset;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setPointsPerRev (int pointsPerRev)
	{
		if (pointsPerRev < 0)
			pointsPerRev = 0;

		if (_pointsPerRev != pointsPerRev) {
			_pointsPerRev = pointsPerRev;
			repaint ();
		}
	}

	public int getPointsPerRev ()
	{
		return _pointsPerRev;
	}

	///////////////////////////////////////////////////////////////////////////
	public Dimension getPreferredSize ()
	{
		final int preferredSize = 600; //pixels

		return new Dimension (preferredSize, preferredSize);
	}

	//members
	boolean _debug = false; //true;

	//defaults for values that can be changed through GUI
	int _radius1 = 29;
	int _radius2 = 23;
	int _offset = 35;
	int _pointsPerRev = 60;
}
