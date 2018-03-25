//OilFileData.java

package com.vendo.oilUsage;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

//import org.apache.logging.log4j.*;


public class OilFileData
{
	///////////////////////////////////////////////////////////////////////////
	//parse a string in this form: "12/04/15  116.5  1.999"
	//throws exception on failure
	public OilFileData (String string) throws Exception
	{
		try (Scanner scanner = new Scanner (string)) {
			scanner.useDelimiter ("\\s+");
			_startDate = _endDate = LocalDate.parse (scanner.next (), _dateTimeFormatter);
			_gallons = scanner.nextDouble ();
			_dollarsPerGallon = scanner.nextDouble ();

		} catch (DateTimeParseException | NoSuchElementException ex) {
			//catch known parsing errors and rethrow them; all other exceptions will be thrown as-is
			throw new OilUsageNotADataRowException (ex);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalDate getStartDate ()
	{
		return _startDate;
	}
	public void setStartDate (LocalDate startDate)
	{
		assert _startDate.isBefore (_endDate) || _startDate.isEqual (_endDate);

		_startDate = startDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public LocalDate getEndDate ()
	{
		return _endDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public double getGallons ()
	{
		return _gallons;
	}

	///////////////////////////////////////////////////////////////////////////
	public double getDollarsPerGallon ()
	{
		return _dollarsPerGallon;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getPeriodInDays ()
	{
		return ChronoUnit.DAYS.between (_startDate, _endDate);
 	}

	///////////////////////////////////////////////////////////////////////////
	public double getGallonsPerDay ()
	{
		if (getPeriodInDays () == 0) {
			return 0;
		}

		return _gallons / getPeriodInDays ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static final Comparator<OilFileData> oilFileDataComparator = new Comparator<OilFileData> ()
	{
		public int compare (OilFileData o1, OilFileData o2)
		{
			return o1.getEndDate ().compareTo (o2.getEndDate ());
		}
	};


	//private members
	private LocalDate _startDate;
	private LocalDate _endDate;
	private double _gallons;
	private double _dollarsPerGallon;

	private final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

//	private static final Logger _log = LogManager.getLogger ();
}
