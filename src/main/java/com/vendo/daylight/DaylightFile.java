//DaylightFile.java

package com.vendo.daylight;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

//import org.apache.logging.log4j.*;


public class DaylightFile
{
	///////////////////////////////////////////////////////////////////////////
	public DaylightFile (Path dataFile)
	{
		_dataFile = dataFile;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns sorted list of all data records in file
	public Collection<DaylightFileData> readFile ()
	{
		ArrayList<DaylightFileData> daylightFileDataList = new ArrayList<DaylightFileData> ();

		Scanner scanner = null;
		try {
			scanner = new Scanner (new FileInputStream (_dataFile.toFile ()));

		} catch (FileNotFoundException ex) {
			ex.printStackTrace ();
			return null;
		}

		while (scanner.hasNextLine ()) {
			String string = scanner.nextLine ();

			try {
				DaylightFileData DaylightFileData = new DaylightFileData (string);
				daylightFileDataList.add (DaylightFileData);

			} catch (DaylightNotADataRowException ex) {
				//exception indicates the string was not a valid data row
				//so try to extract year
				extractDataYear (string);

			} catch (Exception ex) {
				ex.printStackTrace ();
			}
		}
		scanner.close ();

		return daylightFileDataList;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean extractDataYear (String string)
	{
		if (_dataYear == 0) {
			if (Daylight._Debug) {
				System.out.println ("DaylightFile.extractDataYear: string = " + string);
			}

			try {
				Matcher matcher = _yearPattern.matcher (string);
				if (matcher.find ()) {
					_dataYear = Integer.valueOf (matcher.group (1));

					if (Daylight._Debug) {
						System.out.println ("DaylightFile.extractDataYear: _dataYear = " + _dataYear);
					}

					return true;
				}

			} catch (Exception ex) {
				//ignore; exception indicates this string is not the one we are looking for
			}
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer getDataYear ()
	{
		return _dataYear;
	}


	//private members
	private Path _dataFile;
	private Integer _dataYear = 0;

	//format of string from datafile that indicates year: " Rise and Set for the Sun for 2017 "
	//pattern for above: [Sun][whitespace][4-digit number starting with "2"][whitespace]
	//example from http://stackoverflow.com/questions/237061/using-regular-expressions-to-extract-a-value-in-java
	private static final Pattern _yearPattern = Pattern.compile (".*Sun.*\\s+(2\\d{3})\\s+"); //regex

//	private static final Logger _log = LogManager.getLogger ();
}
