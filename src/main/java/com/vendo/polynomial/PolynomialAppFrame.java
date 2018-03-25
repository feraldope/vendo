//PolynomialAppFrame.java

package com.vendo.polynomial;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class PolynomialAppFrame extends JFrame
{
	///////////////////////////////////////////////////////////////////////////
	public PolynomialAppFrame (String title)
	{
		super (title);

		FocusHandler focusHandler = new FocusHandler ();
		SpinnerHandler spinnerHandler = new SpinnerHandler ();
//		TextFieldHandler textFieldHandler = new TextFieldHandler ();
		MouseHandler mouseHandler = new MouseHandler ();

		double spinnerMin1 = -30.;
		double spinnerMax1 = 30.;
		double spinnerStep1 = 0.05;
		double spinnerMin2 = 1.0;
		double spinnerMax2 = 30.;
		double spinnerStep2 = 1.0;

		_graphicsPanel = new GraphicsPanel ();
		_graphicsPanel.addMouseMotionListener (mouseHandler);
		_graphicsPanel.addMouseListener (mouseHandler);

		//"Equation Parameters" panel
		JPanel row1 = new JPanel (new GridLayout (1, 1));
		String str = "<html>y = ax<sup>4</sup> + bx<sup>3</sup> + cx<sup>2</sup> + dx + e</html>";
		row1.add (createLabel (str, JLabel.CENTER));

		Box row2 = Box.createHorizontalBox ();
		row2.add (Box.createHorizontalGlue ());

		_a = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getA (), spinnerMin1, spinnerMax1, spinnerStep1));
		_a.addChangeListener (spinnerHandler);
		_a.addFocusListener (focusHandler);
		row2.add (createLabel ("  a", JLabel.RIGHT));
		row2.add (_a);

		_b = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getB (), spinnerMin1, spinnerMax1, spinnerStep1));
		_b.addChangeListener (spinnerHandler);
		_b.addFocusListener (focusHandler);
		row2.add (createLabel ("  b", JLabel.RIGHT));
		row2.add (_b);

		_c = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getC (), spinnerMin1, spinnerMax1, spinnerStep1));
		_c.addChangeListener (spinnerHandler);
		_c.addFocusListener (focusHandler);
		row2.add (createLabel ("  c", JLabel.RIGHT));
		row2.add (_c);

		_d = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getD (), spinnerMin1, spinnerMax1, spinnerStep1));
		_d.addChangeListener (spinnerHandler);
		_d.addFocusListener (focusHandler);
		row2.add (createLabel ("  d", JLabel.RIGHT));
		row2.add (_d);

		_e = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getE (), spinnerMin1, spinnerMax1, spinnerStep1));
		_e.addChangeListener (spinnerHandler);
		_e.addFocusListener (focusHandler);
		row2.add (createLabel ("  e", JLabel.RIGHT));
		row2.add (_e);

		row2.add (Box.createHorizontalGlue ());

		JPanel leftPanel = new JPanel (new GridLayout (3, 1));
		leftPanel.setBorder (BorderFactory.createTitledBorder ("Equation Parameters"));
		leftPanel.add (row1);
		leftPanel.add (new JPanel ()); //vertical spacing
		leftPanel.add (row2);

		//"Graph Parameters" panel
		JPanel rows = new JPanel (new GridLayout (2, 2));

		_xMinMax = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getXMinMax (), spinnerMin2, spinnerMax2, spinnerStep2));
		_xMinMax.addChangeListener (spinnerHandler);
		_xMinMax.addFocusListener (focusHandler);
		rows.add (createLabel ("X min/max", JLabel.RIGHT));
		rows.add (_xMinMax);

		_yMinMax = new JSpinner (new SpinnerNumberModel ((double) _graphicsPanel.getYMinMax (), spinnerMin2, spinnerMax2, spinnerStep2));
		_yMinMax.addChangeListener (spinnerHandler);
		_yMinMax.addFocusListener (focusHandler);
		rows.add (createLabel ("Y min/max", JLabel.RIGHT));
		rows.add (_yMinMax);

		JPanel rightPanel = new JPanel ();
		rightPanel.setBorder (BorderFactory.createTitledBorder ("Graph Parameters"));
		rightPanel.add (rows);

		Box topPanel = Box.createHorizontalBox ();
		topPanel.add (Box.createHorizontalGlue ());
		topPanel.add (leftPanel);
		topPanel.add (rightPanel);
		topPanel.add (Box.createHorizontalGlue ());

		setLayout (new BorderLayout ());
		add (topPanel, BorderLayout.NORTH);
		add (_graphicsPanel, BorderLayout.CENTER);

	}

	///////////////////////////////////////////////////////////////////////////
	private JLabel createLabel (String str, int horizontalAlignment)
	{
		final Border empty = BorderFactory.createEmptyBorder ();

		JLabel label = new JLabel (str, horizontalAlignment);
		label.setBorder(empty);

		return label;
	}

	///////////////////////////////////////////////////////////////////////////
	public Dimension getGraphicsPanelSize ()
	{
		return _graphicsPanel.getPreferredSize ();
	}

	///////////////////////////////////////////////////////////////////////////
	private class FocusHandler extends FocusAdapter
	{
		public void focusLost (FocusEvent ee)
		{
			JTextField field = (JTextField) ee.getSource ();
			field.postActionEvent ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class SpinnerHandler implements ChangeListener
	{
		public void stateChanged (ChangeEvent ee)
		{
			JSpinner source = (JSpinner) ee.getSource ();
			String str = source.getValue ().toString ();

			double num;
			try {
				num = Double.parseDouble (str);

			} catch (NumberFormatException ex) {
				if (source == _a || source == _b || source == _c || source == _d || source == _e)
					num = 0;
				else
					num = 1;
			}

/*
			if (source == _xMinMax || source == _yMinMax) {
				if (num <= 0)
					num = 1;
			}
*/

			if (source == _a) {
				_graphicsPanel.setA (num);

			} else if (source == _b) {
				_graphicsPanel.setB (num);

			} else if (source == _c) {
				_graphicsPanel.setC (num);

			} else if (source == _d) {
				_graphicsPanel.setD (num);

			} else if (source == _e) {
				_graphicsPanel.setE (num);

			} else if (source == _xMinMax) {
				_graphicsPanel.setXMinMax (num);

			} else if (source == _yMinMax) {
				_graphicsPanel.setYMinMax (num);

			}
		}
	}

/* obsolete
	///////////////////////////////////////////////////////////////////////////
	private class TextFieldHandler implements ActionListener
	{
		public void actionPerformed (ActionEvent ee)
		{
			Object source = ee.getSource ();

			double num;
			try {
				num = Double.parseDouble (ee.getActionCommand ());

			} catch (NumberFormatException ex) {
				if (source == _a || source == _b || source == _c || source == _d || source == _e)
					num = 0;
				else
					num = 1;
			}

			if (source == _xMinMax || source == _yMinMax) {
				if (num <= 0)
					num = 1;
			}

			if (ee.getSource () == _a) {
				_graphicsPanel.setA (num);
				_a.setText (_graphicsPanel.getA ().toString ());

			} else if (ee.getSource () == _b) {
				_graphicsPanel.setB (num);
				_b.setText (_graphicsPanel.getB ().toString ());

			} else if (ee.getSource () == _c) {
				_graphicsPanel.setC (num);
				_c.setText (_graphicsPanel.getC ().toString ());

			} else if (ee.getSource () == _d) {
				_graphicsPanel.setD (num);
				_d.setText (_graphicsPanel.getD ().toString ());

			} else if (ee.getSource () == _e) {
				_graphicsPanel.setE (num);
				_e.setText (_graphicsPanel.getE ().toString ());

			} else if (ee.getSource () == _xMinMax) {
				_graphicsPanel.setXMinMax (num);
				_xMinMax.setText (_graphicsPanel.getXMinMax ().toString ());

			} else if (ee.getSource () == _yMinMax) {
				_graphicsPanel.setYMinMax (num);
				_yMinMax.setText (_graphicsPanel.getYMinMax ().toString ());
			}
		}
	}
*/

	///////////////////////////////////////////////////////////////////////////
	private class MouseHandler extends MouseInputAdapter
	{
		public void mouseMoved (MouseEvent ee)
		{
			_graphicsPanel.setMousePos (ee.getX ());
		}

		public void mouseEntered (MouseEvent ee)
		{
//			System.out.println ("mouseEntered");
			_graphicsPanel.getParent ().getParent ().setCursor (Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
		}

		public void mouseExited (MouseEvent ee)
		{
//			System.out.println ("mouseExited");
			_graphicsPanel.clearMousePos ();
			_graphicsPanel.getParent ().getParent ().setCursor (Cursor.getDefaultCursor ());
		}
	}

	//members
//	private JSplitPane _splitPane;

	public GraphicsPanel _graphicsPanel;

	private JSpinner _a;
	private JSpinner _b;
	private JSpinner _c;
	private JSpinner _d;
	private JSpinner _e;

	private JSpinner _xMinMax;
	private JSpinner _yMinMax;

//	private int _widthEntry = 3;
}
