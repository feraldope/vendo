//HarmModel.java

package com.vendo.harmonograph;

//import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
//import javax.swing.border.*;
import javax.swing.event.*;

public class HarmModel
{
	///////////////////////////////////////////////////////////////////////////
	public HarmModel (GraphicsPanel graphicsPanel)
	{
		_graphicsPanel = graphicsPanel;

		_comboBoxHandler = new ComboBoxHandler ();
		_buttonHandler = new ButtonHandler ();
		_focusHandler = new FocusHandler ();
		_spinnerHandler = new SpinnerHandler ();
//		_textFieldHandler = new TextFieldHandler ();
	}

	///////////////////////////////////////////////////////////////////////////
	public void resetModel ()
	{
		_functionXControl.setSelectedIndex (_functionXDefault.getIndex ());
		_functionYControl.setSelectedIndex (_functionYDefault.getIndex ());

		_colorRedControl.setSelectedIndex   (_colorRedDefault.getIndex ());
		_colorGreenControl.setSelectedIndex (_colorGreenDefault.getIndex ());
		_colorBlueControl.setSelectedIndex  (_colorBlueDefault.getIndex ());

		_frequencyXControl.setValue (_frequencyXDefault);
		_frequencyYControl.setValue (_frequencyYDefault);

		_amplitudeXControl.setValue (_amplitudeXDefault);
		_amplitudeYControl.setValue (_amplitudeYDefault);

		_phaseXControl.setValue (_phaseXDefault);
		_phaseYControl.setValue (_phaseYDefault);

		_decayXControl.setValue (_decayXDefault);
		_decayYControl.setValue (_decayYDefault);

		_cyclesControl.setValue (_cyclesDefault);
		_rotateControl.setValue (_rotateDefault);
	}

	public JComboBox<String> getColorRedControl ()
	{
		if (_colorRedControl == null) {
			_colorRedControl = new JComboBox<String> (ColorFunction.getNames ());
			_colorRedControl.setMaximumRowCount (ColorFunction.getLength ());
			_colorRedControl.setSelectedIndex (_colorRedDefault.getIndex ());
			_colorRedControl.addItemListener (_comboBoxHandler);
		}

		return _colorRedControl;
	}

	public JComboBox<String> getColorGreenControl ()
	{
		if (_colorGreenControl == null) {
			_colorGreenControl = new JComboBox<String> (ColorFunction.getNames ());
			_colorGreenControl.setMaximumRowCount (ColorFunction.getLength ());
			_colorGreenControl.setSelectedIndex (_colorGreenDefault.getIndex ());
			_colorGreenControl.addItemListener (_comboBoxHandler);
		}

		return _colorGreenControl;
	}

	public JComboBox<String> getColorBlueControl ()
	{
		if (_colorBlueControl == null) {
			_colorBlueControl = new JComboBox<String> (ColorFunction.getNames ());
			_colorBlueControl.setMaximumRowCount (ColorFunction.getLength ());
			_colorBlueControl.setSelectedIndex (_colorBlueDefault.getIndex ());
			_colorBlueControl.addItemListener (_comboBoxHandler);
		}

		return _colorBlueControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JComboBox<String> getFunctionXControl ()
	{
		if (_functionXControl == null) {
			_functionXControl = new JComboBox<String> (GraphFunction.getNames ());
			_functionXControl.setMaximumRowCount (GraphFunction.getLength ());
			_functionXControl.setSelectedIndex (_functionXDefault.getIndex ());
			_functionXControl.addItemListener (_comboBoxHandler);
		}

		return _functionXControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JComboBox<String> getFunctionYControl ()
	{
		if (_functionYControl == null) {
			_functionYControl = new JComboBox<String> (GraphFunction.getNames ());
			_functionYControl.setMaximumRowCount (GraphFunction.getLength ());
			_functionYControl.setSelectedIndex (_functionYDefault.getIndex ());
			_functionYControl.addItemListener (_comboBoxHandler);
		}

		return _functionYControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getFrequencyXControl ()
	{
		if (_frequencyXControl == null) {
			_frequencyXControl = new JSpinner (new SpinnerNumberModel (_frequencyXDefault, 1, 30, 1));
			_frequencyXControl.addChangeListener (_spinnerHandler);
			_frequencyXControl.addFocusListener (_focusHandler);
		}

		return _frequencyXControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getFrequencyYControl ()
	{
		if (_frequencyYControl == null) {
			_frequencyYControl = new JSpinner (new SpinnerNumberModel (_frequencyYDefault, 1, 30, 1));
			_frequencyYControl.addChangeListener (_spinnerHandler);
			_frequencyYControl.addFocusListener (_focusHandler);
		}

		return _frequencyYControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getPhaseXControl ()
	{
		if (_phaseXControl == null) {
			_phaseXControl = new JSpinner (new SpinnerNumberModel (_phaseXDefault, -360, 360, 1));
			_phaseXControl.addChangeListener (_spinnerHandler);
			_phaseXControl.addFocusListener (_focusHandler);
		}

		return _phaseXControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getPhaseYControl ()
	{
		if (_phaseYControl == null) {
			_phaseYControl = new JSpinner (new SpinnerNumberModel (_phaseYDefault, -360, 360, 1));
			_phaseYControl.addChangeListener (_spinnerHandler);
			_phaseYControl.addFocusListener (_focusHandler);
		}

		return _phaseYControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getAmplitudeXControl ()
	{
		if (_amplitudeXControl == null) {
			_amplitudeXControl = new JSpinner (new SpinnerNumberModel (_amplitudeXDefault, 5, 125, 5));
			_amplitudeXControl.addChangeListener (_spinnerHandler);
			_amplitudeXControl.addFocusListener (_focusHandler);
		}

		return _amplitudeXControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getAmplitudeYControl ()
	{
		if (_amplitudeYControl == null) {
			_amplitudeYControl = new JSpinner (new SpinnerNumberModel (_amplitudeYDefault, 5, 125, 5));
			_amplitudeYControl.addChangeListener (_spinnerHandler);
			_amplitudeYControl.addFocusListener (_focusHandler);
		}

		return _amplitudeYControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getDecayXControl ()
	{
		if (_decayXControl == null) {
			_decayXControl = new JSpinner (new SpinnerNumberModel (_decayXDefault, 0, 100, 1));
			_decayXControl.addChangeListener (_spinnerHandler);
			_decayXControl.addFocusListener (_focusHandler);
		}

		return _decayXControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getDecayYControl ()
	{
		if (_decayYControl == null) {
			_decayYControl = new JSpinner (new SpinnerNumberModel (_decayYDefault, 0, 100, 1));
			_decayYControl.addChangeListener (_spinnerHandler);
			_decayYControl.addFocusListener (_focusHandler);
		}

		return _decayYControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getCyclesControl ()
	{
		if (_cyclesControl == null) {
			_cyclesControl = new JSpinner (new SpinnerNumberModel (_cyclesDefault, 1, 100, 1));
			_cyclesControl.addChangeListener (_spinnerHandler);
			_cyclesControl.addFocusListener (_focusHandler);
		}

		return _cyclesControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JSpinner getRotateControl ()
	{
		if (_rotateControl == null) {
			_rotateControl = new JSpinner (new SpinnerNumberModel (_rotateDefault, -360, 360, 1));
			_rotateControl.addChangeListener (_spinnerHandler);
			_rotateControl.addFocusListener (_focusHandler);
		}

		return _rotateControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public JButton getResetControl ()
	{
		if (_resetControl == null) {
			_resetControl = new JButton ("Reset");
			_resetControl.addActionListener (_buttonHandler);
		}

		return _resetControl;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getPhaseX ()
	{
		Integer value = (Integer) _phaseXControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getPhaseY ()
	{
		Integer value = (Integer) _phaseYControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getAmplitudeX ()
	{
		Integer value = (Integer) _amplitudeXControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getAmplitudeY ()
	{
		Integer value = (Integer) _amplitudeYControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getFrequencyX ()
	{
		Integer value = (Integer) _frequencyXControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getFrequencyY ()
	{
		Integer value = (Integer) _frequencyYControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public GraphFunction getFunctionX ()
	{
		int index = _functionXControl.getSelectedIndex ();
		return GraphFunction.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	public GraphFunction getFunctionY ()
	{
		int index = _functionYControl.getSelectedIndex ();
		return GraphFunction.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	public int getDecayX ()
	{
		Integer value = (Integer) _decayXControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getDecayY ()
	{
		Integer value = (Integer) _decayYControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getCycles ()
	{
		Integer value = (Integer) _cyclesControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getRotate ()
	{
		Integer value = (Integer) _rotateControl.getValue ();
		return value.intValue ();
	}

	///////////////////////////////////////////////////////////////////////////
	public ColorFunction getColorRed ()
	{
		int index = _colorRedControl.getSelectedIndex ();
		return ColorFunction.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	public ColorFunction getColorGreen ()
	{
		int index = _colorGreenControl.getSelectedIndex ();
		return ColorFunction.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	public ColorFunction getColorBlue ()
	{
		int index = _colorBlueControl.getSelectedIndex ();
		return ColorFunction.get (index);
	}

	///////////////////////////////////////////////////////////////////////////
	private class FocusHandler implements FocusListener
	{
//TODO - this doesn't seem to work (the debug messages are never printed)
//		 plus it incorrectly casts everything to a JTextField

		public void focusGained (FocusEvent ee)
		{
			JTextField field = (JTextField) ee.getSource ();
System.out.println ("HarmModel.FocusHandler: focusGained: " + field);
			field.select (0, _widthEntry);
		}

		public void focusLost (FocusEvent ee)
		{
			JTextField field = (JTextField) ee.getSource ();
System.out.println ("HarmModel.FocusHandler: focusLost: " + field);
			field.postActionEvent ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class ComboBoxHandler implements ItemListener
	{
		public void itemStateChanged (ItemEvent ee)
		{
			_graphicsPanel.repaint ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class SpinnerHandler implements ChangeListener
	{
		public void stateChanged (ChangeEvent ee)
		{
			_graphicsPanel.repaint ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class ButtonHandler implements ActionListener
	{
		public void actionPerformed (ActionEvent ee)
		{
			JButton source = (JButton) ee.getSource ();

			if (source == _resetControl) {
				resetModel ();
				_graphicsPanel.repaint ();
			}
		}
	}

/* unused
	///////////////////////////////////////////////////////////////////////////
	private class TextFieldHandler implements ActionListener
	{
		public void actionPerformed (ActionEvent ee)
		{
			String str = ee.getActionCommand ();

			if (ee.getSource () == _frequencyX) {
				double num = (str.equals ("") ? 0 : Double.parseDouble (str));
				setFrequencyX (num);
				_frequencyX.select (0, _widthEntry);

			} else if (ee.getSource () == _frequencyY) {
				double num = (str.equals ("") ? 0 : Double.parseDouble (str));
				setFrequencyY (num);
				_frequencyY.select (0, _widthEntry);
			}
		}
	}
*/

	//members
	private GraphicsPanel _graphicsPanel;

	private ComboBoxHandler _comboBoxHandler;
	private ButtonHandler _buttonHandler;
	private FocusHandler _focusHandler;
	private SpinnerHandler _spinnerHandler;

	private JComboBox<String> _functionXControl;
	private JComboBox<String> _functionYControl;
	private JComboBox<String> _colorRedControl;
	private JComboBox<String> _colorGreenControl;
	private JComboBox<String> _colorBlueControl;
	private JSpinner _frequencyXControl;
	private JSpinner _frequencyYControl;
	private JSpinner _amplitudeXControl;
	private JSpinner _amplitudeYControl;
	private JSpinner _phaseXControl;
	private JSpinner _phaseYControl;
	private JSpinner _decayXControl;
	private JSpinner _decayYControl;
	private JSpinner _cyclesControl;
	private JSpinner _rotateControl;
	private JButton _resetControl;

	ColorFunction _colorRedDefault   = ColorFunction.X_ASC;
	ColorFunction _colorGreenDefault = ColorFunction.Y_ASC;
	ColorFunction _colorBlueDefault  = ColorFunction.X_DESC;

	GraphFunction _functionXDefault = GraphFunction.SIN;
	GraphFunction _functionYDefault = GraphFunction.COS;

	int _frequencyXDefault = 2;		//Hz
	int _frequencyYDefault = 3;
	int _amplitudeXDefault = 100;	//percent
	int _amplitudeYDefault = 100;
	int _phaseXDefault = 0;			//degrees
	int _phaseYDefault = 0;
	int _decayXDefault = 0;			//percent (occurs on every drawLine, (360 / _thetaStep) times per cycle)
	int _decayYDefault = 0;
	int _rotateDefault = 0;			//degrees (occurs on every drawLine, (360 / _thetaStep) times per cycle)
	int _cyclesDefault = 10;

//	private int _widthLabel = 20;
	private int _widthEntry = 5;
}
