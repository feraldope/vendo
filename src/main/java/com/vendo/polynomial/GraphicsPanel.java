//GraphicsPanel.java

package com.vendo.polynomial;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class GraphicsPanel extends JPanel
{
	///////////////////////////////////////////////////////////////////////////
	public GraphicsPanel ()
	{
		super ();
//		setBackground (Color.RED); //debug
//		setBackground (_backgroundColor);
	}

	///////////////////////////////////////////////////////////////////////////
	public void paintComponent (Graphics g)
	{
		super.paintComponent (g);

//		long startMillis = new Date ().getTime ();

		Dimension panelSize = getSize (null);
		int w = (int) panelSize.getWidth ();
		int h = (int) panelSize.getHeight ();

		if (w != _currentWidth || h != _currentHeight) {
			_currentWidth = w;
			_currentHeight = h;
			_dirty = true;
		}

		_currentSize = Math.min (_currentWidth, _currentHeight);

		if (_currentWidth > _currentHeight) {
			_xOffset = (_currentWidth - _currentHeight) / 2;
			_yOffset = 0;

//graph is only centered in x-direction
//		} else if (h > w) {
//			_xOffset = 0;
//			_yOffset = (h - w) / 2;

		} else {
			_xOffset = 0;
			_yOffset = 0;
		}

		if (_dirty) {
			_xScale = (double) _currentSize / (_xMax - _xMin);
			_yScale = (double) _currentSize / (_yMin - _yMax); //flip y direction (_yScale is negative)

			_transform.setToTranslation (_xOffset + _currentSize / 2, _yOffset + _currentSize / 2);
			_transform.scale (_xScale, _yScale);

			_image = new BufferedImage (_currentWidth, _currentHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D) _image.createGraphics ();
			createImage (g2d);
		}

		g.drawImage (_image, 0, 0, this);

		drawHighlight ((Graphics2D) g);

//		long elapsedMillis = new Date ().getTime () - startMillis;
//		System.out.println ("GraphicsPanel.paintComponent: elapsed: " + elapsedMillis + " ms");
	}

	///////////////////////////////////////////////////////////////////////////
	public void createImage (Graphics2D g2d)
	{
		g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

 		g2d.setColor (_backgroundColor);
		g2d.fillRect (0, 0, _currentWidth, _currentHeight);

		g2d.setColor (_axesColor);
		g2d.draw (generateAxes ());

		g2d.setColor (_graphColor);
		g2d.draw (generateCurve ());

		_dirty = false;
	}

	///////////////////////////////////////////////////////////////////////////
	public double evaluate (double x)
	{
		return (((_a*x + _b)*x + _c)*x + _d)*x + _e;
	}

	///////////////////////////////////////////////////////////////////////////
	public Shape generateCurve ()
	{
		float xStep = (float) (_xMax - _xMin) / (float) (_numPoints - 1);

		GeneralPath path = new GeneralPath ();
		for (int ii = 0; ii < _numPoints; ii++) {
			float x = (float) _xMin + (float) ii * xStep;
			float y = (float) evaluate (x);

			if (ii == 0)
				path.moveTo (x, y);
			else
				path.lineTo (x, y);
		}

		return _transform.createTransformedShape (path);
	}

	///////////////////////////////////////////////////////////////////////////
	public Shape generateAxes ()
	{
		GeneralPath axes = new GeneralPath ();

		int numXTicks = (int) (10 * _xMax);
		int numYTicks = (int) (10 * _yMax);

		float xTickLength = (float) (6 / _xScale);
		float yTickLength = (float) (6 / Math.abs (_yScale));

		//x-axis ticks
		for (int ii = 0; ii < numXTicks; ii++) {
			float x = 0.1f * (ii + 1f);
			axes.moveTo ( x, -yTickLength);
			axes.lineTo ( x,  yTickLength);
			axes.moveTo (-x, -yTickLength);
			axes.lineTo (-x,  yTickLength);
		}

		//y-axis ticks
		for (int ii = 0; ii < numYTicks; ii++) {
			float y = 0.1f * (ii + 1f);
			axes.moveTo (-xTickLength,  y);
			axes.lineTo ( xTickLength,  y);
			axes.moveTo (-xTickLength, -y);
			axes.lineTo ( xTickLength, -y);
		}

		//axes
		axes.moveTo ((float) _xMin, 0);
		axes.lineTo ((float) _xMax, 0);
		axes.moveTo (0, (float) _yMin);
		axes.lineTo (0, (float) _yMax);

		return _transform.createTransformedShape (axes);
	}

	///////////////////////////////////////////////////////////////////////////
	private void drawHighlight (Graphics2D g2d)
	{
		//private reusable objects (to reduce object creation and garbage collection)
		final Point2D absPos = new Point2D.Double ();
		final Point2D relPos = new Point2D.Double ();
		final Ellipse2D absDot = new Ellipse2D.Double ();

		if (_xMousePos == _NotSet)
			return;

		//calculate relative (graph) and absolute (window) coordinates
		absPos.setLocation (_xMousePos, 0);
		try {
			_transform.inverseTransform (absPos, relPos);
		} catch (NoninvertibleTransformException ee) {
			System.out.println ("NoninvertibleTransformException");
		}
		relPos.setLocation (relPos.getX (), evaluate (relPos.getX ()));
		_transform.transform (relPos, absPos);

		double xDiam = 6 / _xScale;
		double yDiam = 6 / Math.abs (_yScale);
		absDot.setFrame (relPos.getX () - xDiam / 2, relPos.getY () - yDiam / 2, xDiam, yDiam);
		Shape relDot = _transform.createTransformedShape (absDot);

		g2d.setColor (_highlightColor);
		g2d.fill (relDot);

		String details = format3 (relPos.getX ()) + ", " + format3 (relPos.getY ());

		double x0 = absPos.getX ();
		double y0 = absPos.getY ();
		final int xShift = 12;
		final int margin = 3;

		FontRenderContext frc = g2d.getFontRenderContext ();
		TextLayout text = new TextLayout (details, _highlighFont, frc);
		Rectangle2D rect = text.getBounds ();

		double xTextPos, yTextPos, xRectPos, yRectPos;

		boolean onRight = (x0 + rect.getX () + rect.getWidth () + xShift + margin < _currentWidth);
		if (onRight) {
			xTextPos = x0 + xShift;
			yTextPos = y0;
			xRectPos = xTextPos + rect.getX () - margin;
			yRectPos = yTextPos + rect.getY () - margin;

		} else {
			xTextPos = (x0 - rect.getWidth () - xShift);
			yTextPos = y0;
			xRectPos = xTextPos - rect.getX () - margin;
			yRectPos = yTextPos + rect.getY () - margin;
		}
		rect.setRect (xRectPos, yRectPos, rect.getWidth () + 2*margin, rect.getHeight () + 2*margin);

		g2d.setColor (_semiTransparent);
		g2d.fill (rect);

		g2d.setColor (_highlightColor);
		g2d.draw (rect);
		text.draw (g2d, (float) xTextPos, (float) yTextPos);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setA (double a)
	{
		if (_a != a) {
			_a = a;
			setDirty ();
		}
	}

	public Double getA ()
	{
		return _a;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setB (double b)
	{
		if (_b != b) {
			_b = b;
			setDirty ();
		}
	}

	public Double getB ()
	{
		return _b;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setC (double c)
	{
		if (_c != c) {
			_c = c;
			setDirty ();
		}
	}

	public Double getC ()
	{
		return _c;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setD (double d)
	{
		if (_d != d) {
			_d = d;
			setDirty ();
		}
	}

	public Double getD ()
	{
		return _d;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setE (double e)
	{
		if (_e != e) {
			_e = e;
			setDirty ();
		}
	}

	public Double getE ()
	{
		return _e;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setXMinMax (double xMinMax)
	{
		if (_xMax != xMinMax) {
			_xMax = xMinMax;
			_xMin = -_xMax;
			setDirty ();
		}
	}

	public Double getXMinMax ()
	{
		return _xMax;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setYMinMax (double yMinMax)
	{
		if (_yMax != yMinMax) {
			_yMax = yMinMax;
			_yMin = -_yMax;
			setDirty ();
		}
	}

	public Double getYMinMax ()
	{
		return _yMax;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setMousePos (int xMousePos)
	{
		if (_xMousePos != xMousePos) {
			_xMousePos = xMousePos;

			repaint ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void clearMousePos ()
	{
		_xMousePos = _NotSet;
		repaint ();
	}

	///////////////////////////////////////////////////////////////////////////
	private void setDirty ()
	{
		_dirty = true;
		repaint ();
	}

	///////////////////////////////////////////////////////////////////////////
	public Dimension getPreferredSize ()
	{
		return new Dimension (_preferredSize, _preferredSize);
	}

	///////////////////////////////////////////////////////////////////////////
	public String format3 (double d)
	{
		return String.format ("%.3f", d);
	}

	//members
	final boolean _debug = true;

	final int _preferredSize = 500; //pixels
	int _currentSize;	//pixels
	int _currentWidth;	//pixels
	int _currentHeight;	//pixels

	//specified by user
	double _a = 2;
	double _b = 2;
	double _c = -2;
	double _d = -1; //-2;
	double _e = 0;
	double _xMax = 1; //2;
	double _yMax = 1; //2;

	//calculated
	double _xMin = -_xMax;
	double _yMin = -_yMax;
	double _xScale;
	double _yScale; //note this value will be negative
	int _xOffset;
	int _yOffset;

	int _numPoints = 250;

	//mouse/highlight
	final int _NotSet = -123456;
	int _xMousePos = _NotSet;	//absolute

	boolean _dirty = true; //true when image needs to be redrawn

	BufferedImage _image;
	Font _highlighFont = new Font ("SansSerif", Font.BOLD, 12);

	//shared reusable objects (to reduce object creation and garbage collection)
	AffineTransform _transform = new AffineTransform ();

	Color _backgroundColor = Color.LIGHT_GRAY;
//	Color _backgroundColor = Color.GRAY;
	Color _graphColor = Color.WHITE;
//	Color _highlightColor = Color.GRAY;
	Color _highlightColor = Color.RED; //debug
//	Color _highlightColor = Color.LIGHT_GRAY;
	Color _axesColor = Color.BLACK;
	Color _semiTransparent = new Color (200, 200, 200, 175);
}
