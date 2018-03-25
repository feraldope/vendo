//GraphicsPanel.java

package com.vendo.harmonograph;

import java.util.*;
import java.awt.*;
import java.awt.geom.*;
//import java.awt.geom.Point2D.*;
import java.awt.image.*;
import javax.swing.*;

public class GraphicsPanel extends JPanel
{
	///////////////////////////////////////////////////////////////////////////
	public GraphicsPanel ()
	{
		super ();
		setBackground (Color.DARK_GRAY); //debug
	}

	///////////////////////////////////////////////////////////////////////////
	public void setModel (HarmModel model)
	{
		_model = model;
	}

	///////////////////////////////////////////////////////////////////////////
	public void paintComponent (Graphics g)
	{
		super.paintComponent (g);

		long startMillis = new Date ().getTime ();

		getParent ().getParent ().setCursor (Cursor.getPredefinedCursor (Cursor.WAIT_CURSOR));

		int currentWidth = getWidth ();
		int currentHeight = getHeight ();

		BufferedImage bi = new BufferedImage (currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D) bi.createGraphics ();

		createImage (g2d, currentWidth, currentHeight);

//TODO - only draw when image changes
		g.drawImage (bi, 0, 0, this);

		getParent ().getParent ().setCursor (Cursor.getDefaultCursor ());

		long elapsedMillis = new Date ().getTime () - startMillis;
		System.out.println ("GraphicsPanel.paintComponent: elapsed: " + elapsedMillis + " ms");
	}

	///////////////////////////////////////////////////////////////////////////
	public void createImage (Graphics2D g2d, int currentWidth, int currentHeight)
	{
		int currentSize = Math.min (currentWidth, currentHeight);

		ColorFunction colorRed   = _model.getColorRed ();
		ColorFunction colorGreen = _model.getColorGreen ();
		ColorFunction colorBlue  = _model.getColorBlue ();
		GraphFunction functionX = _model.getFunctionX ();
		GraphFunction functionY = _model.getFunctionY ();
		double frequencyX = _model.getFrequencyX ();
		double frequencyY = _model.getFrequencyY ();
		double amplitudeX = _model.getAmplitudeX ();
		double amplitudeY = _model.getAmplitudeY ();
		double phaseX = _model.getPhaseX ();
		double phaseY = _model.getPhaseY ();
		double decayX = _model.getDecayX ();
		double decayY = _model.getDecayY ();
		double rotate = _model.getRotate ();
		int cycles = _model.getCycles ();

 		g2d.setColor (Color.BLACK);
		g2d.fillRect (0, 0, currentWidth, currentHeight);

		g2d.translate (currentWidth / 2, currentHeight / 2); //center image

		g2d.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint (RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g2d.setRenderingHint (RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);

		double scaleX = (double) currentSize * ((double) amplitudeX / 100) * _sqrt2over2;
		double scaleY = (double) currentSize * ((double) amplitudeY / 100) * _sqrt2over2;

		int prevX = 0;
		int prevY = 0;

		//if no decay or rotation, there is no need to draw more than one cycle
		int overriddenCycles = (decayX == 0 && decayY == 0 && rotate == 0 ? 1 : cycles);

		for (int cycleCount = 1; cycleCount <= overriddenCycles; cycleCount++) {
			boolean firstPass = true;
			for (double theta = 0; theta <= 360; theta += _thetaStep) {

				//apply function - after transform, (-1 <= val <= +1)
				double xx = functionX.evaluate (theta, phaseX, frequencyX);
				double yy = functionY.evaluate (theta, phaseY, frequencyY);

				//set color
				double rr = colorRed.evaluate   (xx, yy, theta);
				double gg = colorGreen.evaluate (xx, yy, theta);
				double bb = colorBlue.evaluate  (xx, yy, theta);
				g2d.setColor (new Color ((float) rr, (float) gg, (float) bb));

				//rotate
				if (rotate != 0) {
					double phi = Math.toRadians (rotate / _stepsPerCycle);
					g2d.rotate (phi);
					Point2D point = GraphFunction.rotate (new Point2D.Double (xx, yy), phi);
					xx = point.getX ();
					yy = point.getY ();
				}

				//after transform, -(scale / 2) <= val <= +(scale / 2)
				int currX = (int) (xx * scaleX / 2);
				int currY = (int) (yy * scaleY / 2);

				//draw line
				if (firstPass) {
					firstPass = false;
				} else {
//					if (!skipThisLine (prevX, prevY, currX, currY, scaleX, scaleY))
						g2d.drawLine (prevX, prevY, currX, currY);
				}

				prevX = currX;
				prevY = currY;

				//apply decay
				scaleX /= (1 + (decayX / 100 / _stepsPerCycle));
				scaleY /= (1 + (decayY / 100 / _stepsPerCycle));
			}
		}
	}

/* unused
	///////////////////////////////////////////////////////////////////////////
	//skip drawing lines that 'wrap' (i.e., that are a significant portion of scale)
	public boolean skipThisLine (int prevX, int prevY, int currX, int currY,
								 double scaleX, double scaleY)
	{
		double limitX = 0.8 * scaleX;
		double limitY = 0.8 * scaleY;

		return (Math.abs (prevX - currX) > limitX ||
				Math.abs (prevY - currY) > limitY);
	}
*/

	///////////////////////////////////////////////////////////////////////////
	public Dimension getPreferredSize ()
	{
		return new Dimension (_preferredSize, _preferredSize);
	}

	public Dimension getMinimumSize ()
	{
		return getPreferredSize ();
	}

	//members
	private HarmModel _model;

//	private final boolean _debug = false; //true;

	private final int _preferredSize = 700;	//pixels

//	private final double _thetaStep = 0.5;
	private final double _thetaStep = 0.25;
	private final double _stepsPerCycle = 360 / _thetaStep;
	private final double _sqrt2over2 = Math.sqrt (2) / 2;

	private static final long serialVersionUID = 1L;
}
