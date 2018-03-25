//PerfChartResults.java

package com.vendo.perfCpu;

import java.util.*;
import java.util.stream.*;


class PerfChartResults
{
	///////////////////////////////////////////////////////////////////////////
	PerfChartResults ()
	{
	}

	///////////////////////////////////////////////////////////////////////////
	boolean addResult (PerfChartResult perfChartResult)
	{
		_results.add (perfChartResult);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public PerfChartResult getWeightedResult (PerfChartResult perfChartResult)
	{
		return getMedian (perfChartResult);
//		return getWeightedAverage (perfChartResult);
	}

	///////////////////////////////////////////////////////////////////////////
	private PerfChartResult getMedian (PerfChartResult perfChartResult)
	{
		List<Double> values = getMatchingResultsSorted (perfChartResult);

		//calculate median
		int middle = values.size () / 2;
		double value = 0;
		if (values.size () % 2 == 1) {
			value = values.get (middle);
		} else {
			value = (values.get (middle - 1) + values.get (middle)) / 2;
		}

		return new PerfChartResult (perfChartResult.getNumThreads (), perfChartResult.getArraySize (), value);
	}

	///////////////////////////////////////////////////////////////////////////
	private PerfChartResult getWeightedAverage (PerfChartResult perfChartResult)
	{
		List<Double> values = getMatchingResultsSorted (perfChartResult);
		double sum = values.stream ().mapToDouble (Double::doubleValue).sum ();

		//calculate weighted average (if enough values, drop max and min)
//TODO - improve this: e.g., keep only middle 80% of values?
		int count = values.size ();
		if (count > 5) {
			sum -= values.get(0);
			sum -= values.get(count - 1);
			count -= 2;
		}

		return new PerfChartResult (perfChartResult.getNumThreads (), perfChartResult.getArraySize (), sum / count);
	}

	///////////////////////////////////////////////////////////////////////////
	private List<Double> getMatchingResultsSorted (PerfChartResult perfChartResult)
	{
		return _results.stream ()
					   .filter (v -> perfChartResult.equals (v))
					   .map (v -> v.getPerf ())
					   .sorted ()
					   .collect (Collectors.toList ());
	}


	//private members
	private final ArrayList<PerfChartResult> _results = new ArrayList<PerfChartResult> ();
}
