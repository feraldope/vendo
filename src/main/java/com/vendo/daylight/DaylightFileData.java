//DaylightFileData.java

package com.vendo.daylight;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

//import org.apache.logging.log4j.*;


public class DaylightFileData
{
	///////////////////////////////////////////////////////////////////////////
	//parse a data row; throws exception on any failure
	public DaylightFileData (String string) throws Exception
	{
		if (!_dataRowPattern.matcher (string).matches ()) {
			throw new DaylightNotADataRowException ();
		}

		//replace missing values (e.g., for non-existent dates like Feb 30th, April 31st, etc.) with pairs of "0"s
		string = _missingDataPattern.matcher (string).replaceAll (" 0 0 ");

		_dailyData = string.split ("\\s+");

		if (Daylight._Debug) {
			System.out.print ("DaylightFileData.ctor: ");
			for (int ii = 0; ii < _dailyData.length; ii++) {
				System.out.print (ii + ":" + _dailyData[ii] + " ");
			}
			System.out.println ();
		}

/* TODO - add exception handling
		} catch (DateTimeParseException | NoSuchElementException ex) {
			//catch known parsing errors and rethrow them; all other exceptions will be thrown as-is
			throw new DaylightNotADataRowException (ex);
		}
*/
	}

	///////////////////////////////////////////////////////////////////////////
	public int getDayOfMonth ()
	{
		return parseInt (_dailyData[0]);
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalDate getLocalDate (int month)
	{
		LocalDate localDate = null;

		try {
			localDate = LocalDate.of (LocalDate.now ().getYear (), month, getDayOfMonth ());

		} catch (Exception ex) {
			//ignore and fall through to return null below
		}

		return localDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalTime getSunriseTime (int month)
	{
		int sunriseIndex = month * 2 - 1;
		LocalTime localTime = LocalTime.parse (_dailyData[sunriseIndex], _hourMinuteTimeFormatter);

		return localTime;
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalTime getSunsetTime (int month)
	{
		int sunsetIndex = month * 2;
		LocalTime localTime = LocalTime.parse (_dailyData[sunsetIndex], _hourMinuteTimeFormatter);

		return localTime;
	}

	///////////////////////////////////////////////////////////////////////////
	private static Integer parseInt (String string)
	{
		int value = 0;

		if (string.length () > 0) {
			try {
				value = Integer.parseInt (string);

			} catch (NumberFormatException ex) {
				ex.printStackTrace ();
			}
		}

		return value;
	}


	//private members
	private String _dailyData[];

	//data rows start with the day-of-month (as a two-digit number) followed by whitespace
	private static final Pattern _dataRowPattern = Pattern.compile ("^\\d\\d\\s.*"); //regex

	//missing data has pattern: [non-whitespace char][13 whitespace chars][non-whitespace char]
	//example from http://stackoverflow.com/questions/9627988/java-regex-to-match-an-exact-number-of-digits-in-a-string
	private static final Pattern _missingDataPattern = Pattern.compile ("(?<!\\s)\\s{13}(?!\\s)"); //regex

	private static final DateTimeFormatter _hourMinuteTimeFormatter = DateTimeFormatter.ofPattern ("HHmm");

//	private static final Logger _log = LogManager.getLogger ();
}
