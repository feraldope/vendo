//HarmAppFrame.java

package com.vendo.harmonograph;

import java.awt.*;
//import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
//import javax.swing.event.*;


public class HarmAppFrame extends JFrame
{
	///////////////////////////////////////////////////////////////////////////
	public HarmAppFrame (String title)
	{
		super (title);

		//allow then Enter key to 'press' buttons
		UIManager.put ("Button.defaultButtonFollowsFocus", Boolean.TRUE);

		_layout = new GridBagLayout ();
		setLayout (_layout);
		_constraints = new GridBagConstraints ();

		_graphicsPanel = new GraphicsPanel ();
		_model = new HarmModel (_graphicsPanel);
		_graphicsPanel.setModel (_model);

		//First row of controls
		int gridRow = 0;
		int gridColumn = 0;

		addLabel ("FunctionX", gridRow, gridColumn++, 1, 1);
		JComboBox<String> functionX = _model.getFunctionXControl ();
		addComponent (functionX, gridRow, gridColumn++, 1, 1, true);

		addLabel ("FunctionY", gridRow, gridColumn++, 1, 1);
		JComboBox<String> functionY = _model.getFunctionYControl ();
		addComponent (functionY, gridRow, gridColumn++, 1, 1, true);

		addLabel ("Red", gridRow, gridColumn++, 1, 1);
		JComboBox<String> colorRed = _model.getColorRedControl ();
		addComponent (colorRed, gridRow, gridColumn++, 1, 1, true);

		//Second row of controls
		gridRow++;
		gridColumn = 0;

		addLabel ("FrequencyX (Hz)", gridRow, gridColumn++, 1, 1);
		JSpinner frequencyX = _model.getFrequencyXControl ();
		addComponent (frequencyX, gridRow, gridColumn++, 1, 1, true);

		addLabel ("FrequencyY (Hz)", gridRow, gridColumn++, 1, 1);
		JSpinner frequencyY = _model.getFrequencyYControl ();
		addComponent (frequencyY, gridRow, gridColumn++, 1, 1, true);

		addLabel ("Green", gridRow, gridColumn++, 1, 1);
		JComboBox<String> colorGreen = _model.getColorGreenControl ();
		addComponent (colorGreen, gridRow, gridColumn++, 1, 1, true);

		//Third row of controls
		gridRow++;
		gridColumn = 0;

		addLabel ("AmplitudeX (%)", gridRow, gridColumn++, 1, 1);
		JSpinner amplitudeX = _model.getAmplitudeXControl ();
		addComponent (amplitudeX, gridRow, gridColumn++, 1, 1, true);

		addLabel ("AmplitudeY (%)", gridRow, gridColumn++, 1, 1);
		JSpinner amplitudeY = _model.getAmplitudeYControl ();
		addComponent (amplitudeY, gridRow, gridColumn++, 1, 1, true);

		addLabel ("Blue", gridRow, gridColumn++, 1, 1);
		JComboBox<String> colorBlue = _model.getColorBlueControl ();
		addComponent (colorBlue, gridRow, gridColumn++, 1, 1, true);

		//Fourth row of controls
		gridRow++;
		gridColumn = 0;

		addLabel ("PhaseX (degrees)", gridRow, gridColumn++, 1, 1);
		JSpinner phaseX = _model.getPhaseXControl ();
		addComponent (phaseX, gridRow, gridColumn++, 1, 1, true);

		addLabel ("PhaseY (degrees)", gridRow, gridColumn++, 1, 1);
		JSpinner phaseY = _model.getPhaseYControl ();
		addComponent (phaseY, gridRow, gridColumn++, 1, 1, true);

		addLabel ("Cycles", gridRow, gridColumn++, 1, 1);
		JSpinner cycles = _model.getCyclesControl ();
		addComponent (cycles, gridRow, gridColumn++, 1, 1, true);

		//Fifth row of controls
		gridRow++;
		gridColumn = 0;

		addLabel ("DecayX (%)", gridRow, gridColumn++, 1, 1);
		JSpinner decayX = _model.getDecayXControl ();
		addComponent (decayX, gridRow, gridColumn++, 1, 1, true);

		addLabel ("DecayY (%)", gridRow, gridColumn++, 1, 1);
		JSpinner decayY = _model.getDecayYControl ();
		addComponent (decayY, gridRow, gridColumn++, 1, 1, true);

		addLabel ("Rotate (degrees)", gridRow, gridColumn++, 1, 1);
		JSpinner rotate = _model.getRotateControl ();
		addComponent (rotate, gridRow, gridColumn++, 1, 1, true);

		//Sixth row of controls
		gridRow++;
		gridColumn = 0;

		gridColumn += 5; //move to right-hand side
		JButton reset = _model.getResetControl ();
		addComponent (reset, gridRow, gridColumn++, 1, 1, true);

		gridRow++;
		addComponent (_graphicsPanel, gridRow, 0, 6, 1, false);
	}

	///////////////////////////////////////////////////////////////////////////
	public Dimension getGraphicsPanelSize ()
	{
		return _graphicsPanel.getPreferredSize ();
	}

	///////////////////////////////////////////////////////////////////////////
	private void addLabel (String str, int row, int column, int width, int height)
	{
		final Border empty = BorderFactory.createEmptyBorder ();

		JTextField label = new JTextField (str, _widthLabel);
		label.setEditable (false);
		label.setFocusable (false);
//		label.setRequestFocusEnabled (false); //doesn't prevent focus when using, e.g., tab key
		label.setHorizontalAlignment (JTextField.RIGHT);
		label.setBorder(empty);
		addComponent (label, row, column, width, height, true);
	}

	///////////////////////////////////////////////////////////////////////////
	private void addComponent (Component component, int row, int column, int width, int height, boolean canGrow)
	{
		_constraints.fill = GridBagConstraints.BOTH;
		_constraints.gridx = column;
		_constraints.gridy = row;
		_constraints.gridwidth = width;
		_constraints.gridheight = height;

		if (canGrow) {
			//force the labels to be wider by giving them more weight
			if ((column % 2) == 0)
				_constraints.weightx = 4;
			else
				_constraints.weightx = 1;

		} else {
			_constraints.weightx = 0;
		}
		_constraints.weighty = 0;

		_layout.setConstraints (component, _constraints);
		add (component);
	}

	//members
	private HarmModel _model;
	private GraphicsPanel _graphicsPanel;

	private GridBagLayout _layout;
	private GridBagConstraints _constraints;

	private int _widthLabel = 20;
//	private int _widthEntry = 5;

	private static final long serialVersionUID = 1L;
}
