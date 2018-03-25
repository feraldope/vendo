//SaveAppFrame.java

package com.vendo.save;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class SaveAppFrame extends JFrame
{
	private final boolean DEBUG = false; //set to true to add debug output

	///////////////////////////////////////////////////////////////////////////
	public SaveAppFrame (String title)
	{
		super (title);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRecords (SaveRecords records)
	{
		_records = records;
	}

	///////////////////////////////////////////////////////////////////////////
	public void calculateData ()
	{
		int numYears = _records.getYearsToRetirement ();

		ListIterator<SaveRecord> iterator = _records.getRecords ().listIterator ();
		while (iterator.hasNext ()) {
			SaveRecord record = iterator.next ();
			record.calculateData (numYears);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void draw ()
	{
		_textFieldHandler = new TextFieldHandler ();
		_focusHandler = new FocusHandler ();
		_constraints = new GridBagConstraints ();
		_layout = new GridBagLayout ();
		setLayout (_layout);

		int numRecords = _records.size ();
		int tableColums = numRecords + 4;

		_name = new JTextField[numRecords];
		_startBalance = new JTextField[numRecords];
		_annualContribution = new JTextField[numRecords];
		_growthRateOfInvestment = new JTextField[numRecords];
		_growthRateOfContribution = new JTextField[numRecords];

		int gridRow = 0;
		int gridColumn = 0;
		addTextField ("Name", true, gridRow, gridColumn++, 1, 1, 15);
		addTextField ("Start Balance", true, gridRow, gridColumn++, 1, 1, 5);
		addTextField ("Annual Contribution", true, gridRow, gridColumn++, 1, 1, 5);
		addTextField ("Growth of Investment", true, gridRow, gridColumn++, 1, 1, 12);
		addTextField ("Growth of Contribution", true, gridRow, gridColumn++, 1, 1, 12);

		int ii = 0;
		ListIterator<SaveRecord> iterator = _records.getRecords ().listIterator ();
		while (iterator.hasNext ()) {
			gridRow++;
			gridColumn = 0;
			SaveRecord record = iterator.next ();

			_name[ii] = addTextField (record.getName (), false, gridRow, gridColumn++, 1, 1, 15);
			_startBalance[ii] = addTextField (record.getStartBalance (), false, gridRow, gridColumn++, 1, 1, 5);
			_annualContribution[ii] = addTextField (record.getAnnualContribution (), false, gridRow, gridColumn++, 1, 1, 5);
			_growthRateOfInvestment[ii] = addTextField (record.getGrowthRateOfInvestment (), false, gridRow, gridColumn++, 1, 1, 12);
			_growthRateOfContribution[ii] = addTextField (record.getGrowthRateOfContribution (), false, gridRow, gridColumn++, 1, 1, 12);
			ii++;
		}

		gridRow++;
		gridColumn = 0;
		addTextField ("Annual Inflation Rate", true, gridRow, gridColumn++, 1, 1, 12);
		_inflationRate = addTextField (_records.getInflationRate (), false, gridRow, gridColumn++, 1, 1, 12);
		addTextField ("Retirement Age", true, gridRow, gridColumn++, 1, 1, 12);
		_retirementAge = addTextField (_records.getRetirementAge (), false, gridRow, gridColumn++, 1, 1, 12);
		String birthDate = new String ("Birth Date: ") + _records.getBirthDate ().toString ();
		addTextField (birthDate, true, gridRow, gridColumn++, 1, 1, 12);

		gridRow++;
		gridColumn = 0;
		_tableHeaderArea = addTextArea (tableColums, 3, gridRow, gridColumn++, 5, 1, true, false);

		gridRow++;
		gridColumn = 0;
		_tableArea = addTextArea (tableColums, 3, gridRow, gridColumn++, 5, 1, false, true);

//System.out.println ("tab size=" + _tableArea.getTabSize ());

/* todo - add footer info
		gridRow++;
		gridColumn = 0;
		_tableFooterArea = addTextArea (tableColums, 1, gridRow, gridColumn++, 5, 1, false, false);
*/

		printTable ();
	}

	///////////////////////////////////////////////////////////////////////////
	private JTextField addTextField (String string, boolean isHeader, int gridRow, int gridColumn, int gridWidth, int gridHeight, int fieldWidth)
	{
//		final Border empty = BorderFactory.createEmptyBorder ();
		final Font headerFont = new Font (_fontFamily, Font.BOLD, 14);
		final Font normalFont = new Font (_fontFamily, Font.PLAIN, 14);

		JTextField textField = new JTextField (string, fieldWidth);

		textField.setFont (isHeader ? headerFont : normalFont);
		textField.setEditable (isHeader ? false : true);
		textField.setHorizontalAlignment (JTextField.RIGHT);
//		textField.setBorder(empty);

		textField.addActionListener (_textFieldHandler);
		textField.addFocusListener (_focusHandler);

		addComponent (textField, gridRow, gridColumn, gridWidth, gridHeight, false);

		return textField;
	}

	///////////////////////////////////////////////////////////////////////////
	private JTextArea addTextArea (int width, int height, int gridRow, int gridColumn, int gridWidth, int gridHeight, boolean isHeader, boolean addScrollBars)
	{
		final Border empty = BorderFactory.createEmptyBorder ();
		final Font headerFont = new Font (_fontFamily, Font.BOLD, 12);
		final Font normalFont = new Font (_fontFamily, Font.PLAIN, 12);
		final Color background = new Color (238, 238, 238);

		JTextArea textArea = new JTextArea (height, width);

		textArea.setBackground (background);
		textArea.setFont (isHeader ? headerFont : normalFont);
		textArea.setEditable (false);
		textArea.setBorder(empty);

		if (!addScrollBars)
			addComponent (textArea, gridRow, gridColumn, gridWidth, gridHeight, false);
		else
			addComponent (new JScrollPane (textArea), gridRow, gridColumn, gridWidth, gridHeight, true);

		return textArea;
	}

	///////////////////////////////////////////////////////////////////////////
	private void addComponent (Component component, int gridRow, int gridColumn, int gridWidth, int gridHeight, boolean canGrowVertically)
	{
		_constraints.fill = GridBagConstraints.BOTH;
		_constraints.gridx = gridColumn;
		_constraints.gridy = gridRow;
		_constraints.gridwidth = gridWidth;
		_constraints.gridheight = gridHeight;
		_constraints.weightx = 0;
		_constraints.weighty = (canGrowVertically ? 1 : 0);

		_layout.setConstraints (component, _constraints);
		add (component);
	}

	///////////////////////////////////////////////////////////////////////////
	private class FocusHandler implements FocusListener
	{
		public void focusGained (FocusEvent ee)
		{
		}

		public void focusLost (FocusEvent ee)
		{
			((JTextField) ee.getSource ()).postActionEvent ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private class TextFieldHandler implements ActionListener
	{
		public void actionPerformed (ActionEvent ee)
		{
			String string = ee.getActionCommand ();

			if (ee.getSource () == _inflationRate) {
				_records.setInflationRate (string);
				_inflationRate.setText (_records.getInflationRate ());
				printTable ();
				return;
			}

			if (ee.getSource () == _retirementAge) {
				_records.setRetirementAge (string);
				_retirementAge.setText (_records.getRetirementAge ());
				printTable ();
				return;
			}

			int ii = 0;
			ListIterator<SaveRecord> iterator = _records.getRecords ().listIterator ();
			while (iterator.hasNext ()) {
				SaveRecord record = iterator.next ();

				if (ee.getSource () == _name[ii]) {
					record.setName (string);
					_name[ii].setText (record.getName ());
					printTable ();
					return;
				}

				if (ee.getSource () == _startBalance[ii]) {
					record.setStartBalance (string);
					_startBalance[ii].setText (record.getStartBalance ());
					printTable ();
					return;
				}

				if (ee.getSource () == _annualContribution[ii]) {
					record.setAnnualContribution (string);
					_annualContribution[ii].setText (record.getAnnualContribution ());
					printTable ();
					return;
				}

				if (ee.getSource () == _growthRateOfInvestment[ii]) {
					record.setGrowthRateOfInvestment (string);
					_growthRateOfInvestment[ii].setText (record.getGrowthRateOfInvestment ());
					printTable ();
					return;
				}

				if (ee.getSource () == _growthRateOfContribution[ii]) {
					record.setGrowthRateOfContribution (string);
					_growthRateOfContribution[ii].setText (record.getGrowthRateOfContribution ());
					printTable ();
					return;
				}

				ii++;
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void printTable ()
	{
		calculateData ();

/* todo - add footer info
		_tableFooterArea.setText ("(footer)");
*/
		_tableHeaderArea.setText (NL + NL + "Date\tAge");
		ListIterator<SaveRecord> iterator = _records.getRecords ().listIterator ();
		while (iterator.hasNext ()) {
			SaveRecord record = iterator.next ();
			_tableHeaderArea.append ("\t" + record.getName ());
		}
		_tableHeaderArea.append ("\tTotal\tAdjusted");

		int currentAge = _records.getCurrentAge ();
		int numYears = _records.getYearsToRetirement ();
		double cumulativeInflation = 1.;
		String string;

		_tableArea.setText (""); //clear table area
		for (int ii = 0; ii < numYears; ii++) {
			boolean needDateAndAge = true;
			Double rowBalance = 0.;
			iterator = _records.getRecords ().listIterator ();
			while (iterator.hasNext ()) {
				SaveRecord record = iterator.next ();

				if (needDateAndAge) {
					needDateAndAge = false;
					_tableArea.append (record.getTableDate (ii).toString ());
					_tableArea.append ("\t" + (ii + currentAge) + "\t");
				}

				double balance = record.getTableBalance (ii);
				rowBalance += balance;

				string = SaveRecord.formatNumber (balance, true);
				_tableArea.append (string + "\t");
			}

			string = SaveRecord.formatNumber (rowBalance, true);
			_tableArea.append (string + "\t");

			string = SaveRecord.formatNumber (rowBalance * cumulativeInflation, true);
			_tableArea.append (string);

			if (ii < (numYears - 1))
				_tableArea.append (NL);

//todo - this adds a full year of inflation even though the difference between the
//		 first row (Today) and the next row is almost always less than a year
//			cumulativeInflation *= _records.getInflationFactor ();
//for now, during first year only add 1/2 the inflation
			cumulativeInflation *= _records.getInflationFactor (ii == 0 ? .5 : 1.);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void fm (int point, String string)
	{
		if (false) {
			Font font = new Font (_fontFamily, Font.PLAIN, point);
			FontRenderContext frc = new FontRenderContext (new AffineTransform (), false, false);

			Rectangle2D rect = font.getStringBounds (string, frc);
			System.out.println ("'" + string + "' at size " + point + ": w=" + rect.getWidth () + ", h=" + rect.getHeight ());
		}
	}

//debug
	///////////////////////////////////////////////////////////////////////////
	public Dimension getWindowSize (int yearsToLeaveRoomFor)
	{
		int point = 12;
		fm (point, "W");
		fm (point, "A");
		fm (point, "i");
		fm (point, "b");
		fm (point, "w");
		fm (point, "\t");
		point = 14;
		fm (point, "W");
		fm (point, "A");
		fm (point, "i");
		fm (point, "b");
		fm (point, "w");
		fm (point, "\t");

//14-point rows in the grid need 23 vertical pixels each
//12-point rows in the table need 16 vertical pixels each

		int fieldWidthTotal = 49;		 //sum of all fieldWidth's above
//todo - get char width and height values from system

		int width = 20 + fieldWidthTotal * 16;
		int height = 50 + (_records.size () + 2) * 23 + (yearsToLeaveRoomFor + 3) * 16;

		return new Dimension (width, height);
	}
/*
import java.awt.geom.Rectangle2D;

 Rectangle2D 	getStringBounds(char[] chars, int beginIndex, int limit, FontRenderContext frc)
          Returns the logical bounds of the specified array of characters in the specified FontRenderContext.
 Rectangle2D 	getStringBounds(CharacterIterator ci, int beginIndex, int limit, FontRenderContext frc)
          Returns the logical bounds of the characters indexed in the specified CharacterIterator in the specified FontRenderContext.
 Rectangle2D 	getStringBounds(String str, FontRenderContext frc)
          Returns the logical bounds of the specified String in the specified FontRenderContext.
 Rectangle2D 	getStringBounds(String str, int beginIndex, int limit, FontRenderContext frc)
          Returns the logical bounds of the specified String in the specified FontRenderContext.

getHeight ()
getWidth ()


import java.awt.font.LineMetrics;
import java.awt.font.FontRenderContext;

 LineMetrics 	getLineMetrics(char[] chars, int beginIndex, int limit, FontRenderContext frc)
          Returns a LineMetrics object created with the specified arguments.
 LineMetrics 	getLineMetrics(CharacterIterator ci, int beginIndex, int limit, FontRenderContext frc)
          Returns a LineMetrics object created with the specified arguments.
 LineMetrics 	getLineMetrics(String str, FontRenderContext frc)
          Returns a LineMetrics object created with the specified String and FontRenderContext.
 LineMetrics 	getLineMetrics(String str, int beginIndex, int limit, FontRenderContext frc)
          Returns a LineMetrics object created with the specified arguments.
 Rectangle2D 	getMaxCharBounds(FontRenderContext frc)
          Returns the bounds for the character with the maximum bounds as defined in the specified FontRenderContext.
*/

	///////////////////////////////////////////////////////////////////////////
	public Dimension getPreferredSize ()
	{
		return getWindowSize (_records.getYearsToRetirement ());
	}

	///////////////////////////////////////////////////////////////////////////
	public Dimension getMinimumSize ()
	{
		return getWindowSize (0);
	}

	//members
	private JTextArea _tableArea = null;
	private JTextArea _tableHeaderArea = null;
	private JTextArea _tableFooterArea = null;
	private JTextField _inflationRate = null;
	private JTextField _retirementAge = null;
	private JTextField[] _name = null;
	private JTextField[] _startBalance = null;
	private JTextField[] _annualContribution = null;
	private JTextField[] _growthRateOfInvestment = null;
	private JTextField[] _growthRateOfContribution = null;

	private SaveRecords _records = null;
	private TextFieldHandler _textFieldHandler = null;
	private FocusHandler _focusHandler = null;
	private GridBagConstraints _constraints = null;
	private GridBagLayout _layout = null;

	private static final String _fontFamily = "SansSerif";

	public static final String NL = System.getProperty ("line.separator");
}
