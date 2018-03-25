//OilUsageRecord.java

package com.vendo.oilUsage;

//import org.apache.logging.log4j.*;


public class OilUsageRecord
{
	///////////////////////////////////////////////////////////////////////////
	//exactly one record per day, used to accumulate all of the values for that day (only)
	public void addData (double gallonsPerDay, double dollarsPerGallons)
	{
		_count++;
		_gallonsPerDay += gallonsPerDay;
		_dollarsPerGallons += dollarsPerGallons;

		if (gallonsPerDay > _maxGallonsPerDay) {
			_maxGallonsPerDay = gallonsPerDay;
		}

		if (gallonsPerDay < _minGallonsPerDay) {
			_minGallonsPerDay = gallonsPerDay;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public double getAverageGallonsPerDay ()
	{
		//if we have a minimum number of samples, discard the two extremes
		if (_count >= 10) {
			return (_gallonsPerDay - _minGallonsPerDay - _maxGallonsPerDay) / (_count - 2);
		} else {
			return _gallonsPerDay / _count;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public double getMinGallonsPerDay ()
	{
		return _minGallonsPerDay;
	}

	///////////////////////////////////////////////////////////////////////////
	public double getMaxGallonsPerDay ()
	{
		return _maxGallonsPerDay;
	}

	///////////////////////////////////////////////////////////////////////////
	public double getCount ()
	{
		return _count;
	}


	//private members
	private int _count;
	private double _gallonsPerDay; //sum of all individual values for the day this record represents
	private double _dollarsPerGallons; //sum of all individual values for the day this record represents
	private double _maxGallonsPerDay = Double.MIN_VALUE;
	private double _minGallonsPerDay = Double.MAX_VALUE;
}
