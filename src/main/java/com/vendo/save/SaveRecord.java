//SaveRecord.java

package com.vendo.save;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.JOptionPane;

public class SaveRecord
{
	///////////////////////////////////////////////////////////////////////////
	public SaveRecord (SaveRecords records)
	{
		_records = records;
	}

	///////////////////////////////////////////////////////////////////////////
	public void calculateData (int numYears)
	{
		_tableBalances = new Double[numYears + 1];
		_tableDates = new SaveDate[numYears + 1];

		Double balance = _startBalance;
		Double monthlyContribution = _annualContribution / 12;

		SaveDate date = new SaveDate ();
		SaveDate retirementDate = _records.getRetirementDate ();

		int ii = 0;
		setTableValues (balance, date, ii++);
		while (date.lessThanOrEqual (retirementDate)) {
			balance += monthlyContribution;
			balance *= 1 + (_growthRateOfInvestment / 1200);

			if (date.getMonth () == retirementDate.getMonth ()) {
				setTableValues (balance, date, ii++);
				monthlyContribution *= 1 + (_growthRateOfContribution / 1200);
			}

			date.nextMonth ();
		}

		if (ii == 0)
			JOptionPane.showMessageDialog (null, "Error in SaveRecord.calculateData, no data generated", "Warning", JOptionPane.ERROR_MESSAGE);
	}

	///////////////////////////////////////////////////////////////////////////
	public static Double parseDouble (String string)
	{
		if (string.length () == 0)
			return 0.;

		//accept strings with commas - and ignore the commas
		string = string.replace (",", "");

		return extractDouble (string);
	}

	///////////////////////////////////////////////////////////////////////////
	public static Integer parseInt (String string)
	{
		if (string.length () == 0)
			return 0;

		//accept strings with commas - and ignore the commas
		string = string.replace (",", "");

		return extractInteger (string);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String extractString (String string)
	{
		int pos = string.indexOf ("=");
		if (pos >= 0)
			return string.substring (pos + 1).trim ();

		return string;
	}

	///////////////////////////////////////////////////////////////////////////
	public static Integer extractInteger (String string)
	{
		int value = 0;

		string = extractString (string);
		try {
			value = Integer.parseInt (string);
		} catch (NumberFormatException ee) {
			JOptionPane.showMessageDialog (null, "Error parsing number \"" + string + "\", using value 0", "Warning", JOptionPane.ERROR_MESSAGE);
		}

		if (value < 0) {
			JOptionPane.showMessageDialog (null, "Unexpected negative number \"" + value + "\", using value 0", "Warning", JOptionPane.ERROR_MESSAGE);
			value = 0;
		}

		return value;
	}

	///////////////////////////////////////////////////////////////////////////
	public static Double extractDouble (String string)
	{
		double value = 0;

		string = extractString (string);
		try {
			value = Double.parseDouble (string);
		} catch (NumberFormatException ee) {
			JOptionPane.showMessageDialog (null, "Error parsing number \"" + string + "\", using value 0", "Warning", JOptionPane.ERROR_MESSAGE);
		}

		if (value < 0) {
			JOptionPane.showMessageDialog (null, "Unexpected negative number \"" + value + "\", using value 0", "Warning", JOptionPane.ERROR_MESSAGE);
			value = 0;
		}

		return value;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String execute (String command)
	{
		int quote = command.indexOf ("\"");
		if (quote == -1)
			return command;
		command = command.substring (quote + 1, command.length ());

		quote = command.indexOf ("\"");
		if (quote == -1)
			return command;
		command = command.substring (0, quote);

		Process process = null;
		try {
			process = Runtime.getRuntime ().exec (command);
		} catch (Exception ee) {
			JOptionPane.showMessageDialog (null, "Error executing \"" + command + "\", using value 0", "Warning", JOptionPane.ERROR_MESSAGE);
			return "0";
		}

		BufferedReader reader = new BufferedReader (new InputStreamReader (process.getInputStream ()));
		String pattern = "Total";
		String line = new String ();
		while (!line.startsWith (pattern)) {
			try {
				line = reader.readLine ();
				if (line == null) //EOF
					throw new Exception ("End of file");

			} catch (Exception ee) {
				JOptionPane.showMessageDialog (null, "Error reading stream, using value 0", "Warning", JOptionPane.ERROR_MESSAGE);
				return "0";
			}
		}

		line = stripFraction (line.substring (pattern.length (), line.length ()).trim ());
		return line;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String stripFraction (String string)
	{
		int dot = string.indexOf (".");
		if (dot == -1)
			return string;

		return string.substring (0, dot);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String stripTrailingZeroes (String string)
	{
		int dot = string.indexOf (".");
		if (dot == -1)
			return string;

		int end = string.length ();
		while (end > 0 && string.charAt (end - 1) == '0')
			string = string.substring (0, --end); //strip trailing zeroes
		if (end > 0 && string.charAt (end - 1) == '.')
			string = string.substring (0, --end); //strip trailing decimal

		return string;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String formatNumber (Integer value)
	{
		return String.format ("%,d", value); //print with commas
	}

	///////////////////////////////////////////////////////////////////////////
	public static String formatNumber (Double value) //stripFraction = false
	{
		return formatNumber (value, false);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String formatNumber (Double value, boolean stripFraction)
	{
		String string = String.format ("%,f", value); //print with commas

		if (stripFraction)
			return (stripFraction (string));

		return stripTrailingZeroes (string);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setName (String string)
	{
		_name = extractString (string);
	}

	///////////////////////////////////////////////////////////////////////////
	public void setStartBalance (String string)
	{
		if (string.contains ("execute"))
			string = execute (extractString (string));

		_startBalance = parseDouble (extractString (string));
	}

	///////////////////////////////////////////////////////////////////////////
	public void setStartBalance (Double startBalance)
	{
		_startBalance = startBalance;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setAnnualContribution (String string)
	{
		_annualContribution = parseDouble (extractString (string));
	}

	///////////////////////////////////////////////////////////////////////////
	public void setAnnualContribution (Double annualContribution)
	{
		_annualContribution = annualContribution;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setGrowthRateOfInvestment (String string)
	{
		_growthRateOfInvestment = parseDouble (extractString (string));
	}

	///////////////////////////////////////////////////////////////////////////
	public void setGrowthRateOfInvestment (Double growthRateOfInvestment)
	{
		_growthRateOfInvestment = growthRateOfInvestment;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setGrowthRateOfContribution (String string)
	{
		_growthRateOfContribution = parseDouble (extractString (string));
	}

	///////////////////////////////////////////////////////////////////////////
	public void setGrowthRateOfContribution (Double growthRateOfContribution)
	{
		_growthRateOfContribution = growthRateOfContribution;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName ()
	{
		return _name;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getStartBalance ()
	{
		return formatNumber (_startBalance);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getAnnualContribution ()
	{
		return formatNumber (_annualContribution);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getGrowthRateOfInvestment ()
	{
		return formatNumber (_growthRateOfInvestment);
	}

	///////////////////////////////////////////////////////////////////////////
	public String getGrowthRateOfContribution ()
	{
		return formatNumber (_growthRateOfContribution);
	}

	///////////////////////////////////////////////////////////////////////////
	private void setTableValues (Double balance, SaveDate date, int index)
	{
		_tableBalances[index] = balance;
		_tableDates[index] = new SaveDate (date);
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getTableBalance (int index)
	{
//aaa - catch array-out-of-bounds exception
		return _tableBalances[index];
	}

	///////////////////////////////////////////////////////////////////////////
	public SaveDate getTableDate (int index)
	{
//aaa - catch array-out-of-bounds exception
		return _tableDates[index];
	}

	//members
	private String _name = new String ("Investment");
	private Double _startBalance = new Double (0.);
	private Double _annualContribution = new Double (0.);
	private Double _growthRateOfInvestment = new Double (0.);
	private Double _growthRateOfContribution = new Double (0.);

	private Double[] _tableBalances = null;
	private SaveDate[] _tableDates = null;
	private SaveRecords _records = null;
}
