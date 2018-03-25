//SaveRecords.java

package com.vendo.save;

import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

public class SaveRecords
{
	///////////////////////////////////////////////////////////////////////////
	public SaveRecords ()
	{
		_list = new ArrayList<SaveRecord> ();
	}

	///////////////////////////////////////////////////////////////////////////
	public List<SaveRecord> getRecords ()
	{
		return _list;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer size ()
	{
		return _list.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int readFile (String filename)
	{
		Scanner input;
		try {
			input = new Scanner (new File (filename));
		} catch (IOException ee) {
			JOptionPane.showMessageDialog (null, "Error opening file \"" + filename + "\"", "Fatal Error", JOptionPane.ERROR_MESSAGE);
			return 0;
		}

		String line;
		SaveRecord record = null;
		while (input.hasNext ()) {
			line = removeComment (input.nextLine ()).trim ();

			if (line.length () == 0) {
				//ignore

			} else if (line.startsWith ("[No. ")) {
				if (record != null)
					_list.add (record);				//add previous record
				record = new SaveRecord (this);		//and get new record

			} else if (foundKeyword (line, "Inflation")) {
				setInflationRate (line);

			} else if (foundKeyword (line, "BirthDate")) {
				setBirthDate (line);

			} else if (foundKeyword (line, "RetirementAge")) {
				setRetirementAge (line);

			} else if (foundKeyword (line, "Name")) {
				record.setName (line);

			} else if (foundKeyword (line, "Balance")) {
				record.setStartBalance (line);

			} else if (foundKeyword (line, "AnnualContribution")) {
				record.setAnnualContribution (line);

			} else if (foundKeyword (line, "GrowthRateOfInvestment")) {
				record.setGrowthRateOfInvestment (line);

			} else if (foundKeyword (line, "GrowthRateOfContribution")) {
				record.setGrowthRateOfContribution (line);

			} else {
//todo - these should be aggregated and displayed as one
				JOptionPane.showMessageDialog (null, "Unrecognized line ignored: \"" + line + "\"", "Warning", JOptionPane.ERROR_MESSAGE);
			}
		}

		//add the last record
		if (record != null)
			_list.add (record);

		input.close ();

		if (_list.size () < 1)
			JOptionPane.showMessageDialog (null, "No records found", "Fatal Error", JOptionPane.ERROR_MESSAGE);

		return _list.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String removeComment (String string)
	{
		final String Semicolon = "#";

		int index = string.indexOf (Semicolon);
		if (index > 0)
			return string.substring (0, index);
		else
			return string;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean foundKeyword (String line, String keyword)
	{
		//match strings that start with the keyword followed by optional whitespace followed by "="
		String regex = keyword + "[ \t]*=.*";
		return line.matches (regex);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setInflationRate (String string)
	{
		_inflationRate = SaveRecord.parseDouble (SaveRecord.extractString (string));
	}

	///////////////////////////////////////////////////////////////////////////
	public void setInflationRate (Double inflationRate)
	{
		_inflationRate = inflationRate;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setBirthDate (String string)
	{
		_birthDate = new SaveDate (SaveRecord.extractString (string));
//todo validate value
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRetirementAge (String string)
	{
		_retirementAge = SaveRecord.parseInt (SaveRecord.extractString (string));
//todo validate value
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRetirementAge (Integer retirementAge)
	{
		_retirementAge = retirementAge;
//todo validate value
	}

	///////////////////////////////////////////////////////////////////////////
	public String getInflationRate ()
	{
		return SaveRecord.formatNumber (_inflationRate);
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getInflationFactor (double fractionOfYear)
	{
		return (100 - (_inflationRate * fractionOfYear)) / 100.;
	}

	///////////////////////////////////////////////////////////////////////////
	public SaveDate getBirthDate ()
	{
		return _birthDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getRetirementAge ()
	{
		return SaveRecord.formatNumber (_retirementAge);
	}

	///////////////////////////////////////////////////////////////////////////
	public SaveDate getRetirementDate ()
	{
		return _birthDate.addYears (_retirementAge);
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getCurrentAge ()
	{
		SaveDate today = new SaveDate ();

		int adjust = (today.getMonth () < _birthDate.getMonth ()) ? -1 : 0;

		return today.getYear () - _birthDate.getYear () + adjust;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getYearsToRetirement ()
	{
		return _retirementAge - getCurrentAge () + 1;
	}

	//members
	private Double _inflationRate = new Double (3.); // annual inflation rate
	private SaveDate _birthDate = new SaveDate ("12/1970");
	private Integer _retirementAge = new Integer (65);

	private List<SaveRecord> _list = null;
}
