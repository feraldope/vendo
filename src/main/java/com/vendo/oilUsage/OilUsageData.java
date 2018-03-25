//OilUsageData.java

package com.vendo.oilUsage;

//import com.vendo.vendoUtils.*;

import java.text.*;
import java.util.*;
import java.time.*;

//import org.apache.logging.log4j.*;


public class OilUsageData
{
	///////////////////////////////////////////////////////////////////////////
	//contains one OilUsageRecord object for each day of the year
	public OilUsageData (Collection<OilFileData> oilFileDataList)
	{
		for (int ii = 0; ii <= 365; ii++) { //notes: record 0 is unused
			_oilUsageRecords.add (new OilUsageRecord ());
		}

		for (OilFileData oilFileData : oilFileDataList) {
			LocalDate day = oilFileData.getStartDate ();
			while (day.isBefore (oilFileData.getEndDate ())) {
//				System.out.println ("day: " + day + ", day of year: " + day.getDayOfYear ());
				addData (day.getDayOfYear (), oilFileData.getGallonsPerDay (), oilFileData.getDollarsPerGallon ());
				day = day.plusDays (1);
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void addData (int dayOfYear, double gallonsPerDay, double dollarsPerGallons)
	{
		//note this does not handle leap years: Feb 29th and all days after are shifted by 1 day
		//and the real Dec 31st is dropped
		if (dayOfYear > 365) {
			return;
		}

		_oilUsageRecords.get (dayOfYear).addData (gallonsPerDay, dollarsPerGallons);
	}

	///////////////////////////////////////////////////////////////////////////
	public double getDailyAverage (int dayOfYear)
	{
		return _oilUsageRecords.get (dayOfYear).getAverageGallonsPerDay ();
	}

	///////////////////////////////////////////////////////////////////////////
	public double getDailyMin (int dayOfYear)
	{
		return _oilUsageRecords.get (dayOfYear).getMinGallonsPerDay ();
	}

	///////////////////////////////////////////////////////////////////////////
	public double getDailyMax (int dayOfYear)
	{
		return _oilUsageRecords.get (dayOfYear).getMaxGallonsPerDay ();
	}

	///////////////////////////////////////////////////////////////////////////
	public double getDailyCount (int dayOfYear)
	{
		return _oilUsageRecords.get (dayOfYear).getCount ();
	}

	///////////////////////////////////////////////////////////////////////////
	public double getOverallAverage ()
	{
		double total = 0;

		for (int ii = 1; ii <= 365; ii++) {
			total += getDailyAverage (ii);
		}

		return total / 365;
	}


	//private members
	private ArrayList<OilUsageRecord> _oilUsageRecords = new ArrayList<OilUsageRecord> ();

	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0.00");

//	private static final Logger _log = LogManager.getLogger ();
}
