//SpirographAppFrame.java

package com.vendo.spirograph;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class SpirographAppFrame extends JFrame
{
	///////////////////////////////////////////////////////////////////////////
	public SpirographAppFrame (String title)
	{
		super (title);

		SpinnerHandler spinnerHandler = new SpinnerHandler ();

		JPanel inputPanel = new JPanel (new GridLayout (2, 4, 2, 2));
		inputPanel.setBorder (BorderFactory.createTitledBorder ("Input"));

		Box topPanel = Box.createHorizontalBox ();
		topPanel.add (Box.createHorizontalGlue ());
		topPanel.add (inputPanel);
		topPanel.add (Box.createHorizontalGlue ());

		_graphicsPanel = new GraphicsPanel ();

		setLayout (new BorderLayout ());
		add (topPanel, BorderLayout.NORTH);
		add (_graphicsPanel, BorderLayout.CENTER);

		//input panel
		_radius1 = new JSpinner (new SpinnerNumberModel (_graphicsPanel.getRadius1 (), 1, 60, 1));
		_radius1.addChangeListener (spinnerHandler);
		inputPanel.add (createLabel ("Radius1"));
		inputPanel.add (_radius1);

		_radius2 = new JSpinner (new SpinnerNumberModel (_graphicsPanel.getRadius2 (), 1, 60, 1));
		_radius2.addChangeListener (spinnerHandler);
		inputPanel.add (createLabel ("Radius2"));
		inputPanel.add (_radius2);

		_offset = new JSpinner (new SpinnerNumberModel (_graphicsPanel.getOffset (), 1, 60, 1));
		_offset.addChangeListener (spinnerHandler);
		inputPanel.add (createLabel ("Offset"));
		inputPanel.add (_offset);

		_pointsPerRev = new JSpinner (new SpinnerNumberModel (_graphicsPanel.getPointsPerRev (), 20, 200, 10));
		_pointsPerRev.addChangeListener (spinnerHandler);
		inputPanel.add (createLabel ("Points/rev"));
		inputPanel.add (_pointsPerRev);

		_graphicsPanel.setBackground (Color.YELLOW);

		if (false) {
//TODO - trying to capture window resize events
			addComponentListener (
				new ComponentAdapter () {
					public void componentResized (ComponentEvent ee)
					{
						System.out.println (ee.paramString ());
//						System.out.println (ee.getComponent ());
					}
				}
			);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private JTextField createLabel (String str)
	{
		final Border empty = BorderFactory.createEmptyBorder ();
		final int width = 5;

		JTextField label = new JTextField (str, width);

		label.setBorder(empty);
		label.setEditable (false);
		label.setHorizontalAlignment (JTextField.RIGHT);

		return label;
	}

	///////////////////////////////////////////////////////////////////////////
	private class SpinnerHandler implements ChangeListener
	{
		public void stateChanged (ChangeEvent ee)
		{
			JSpinner source = (JSpinner) ee.getSource ();
			String str = source.getValue ().toString ();
			int num = (str.equals ("") ? 0 : Integer.parseInt (str));

			if (source == _radius1) {
				_graphicsPanel.setRadius1 (num);

			} else if (source == _radius2) {
				_graphicsPanel.setRadius2 (num);

			} else if (source == _offset) {
				_graphicsPanel.setOffset (num);

			} else if (source == _pointsPerRev) {
				_graphicsPanel.setPointsPerRev (num);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public Dimension getGraphicsPanelSize ()
	{
		return _graphicsPanel.getPreferredSize ();
	}

	//members
	public GraphicsPanel _graphicsPanel;

	private JSpinner _radius1;
	private JSpinner _radius2;
	private JSpinner _offset;
	private JSpinner _pointsPerRev;
}
