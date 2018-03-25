//PerfChartResult.java

package com.vendo.perfCpu;

class PerfChartResult
{
	///////////////////////////////////////////////////////////////////////////
	PerfChartResult (int numThreads, int arraySize, double perf)
	{
		_numThreads = numThreads;
		_arraySize = arraySize;
		_perf = perf;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumThreads ()
	{
		return _numThreads;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getArraySize ()
	{
		return _arraySize;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getArraySizeStr ()
	{
		if (_arraySize == 0) {
			return "Ideal";

		} else {
			final int bytesPerInt = 4;
			return "Buffer=" + PerfCpu.unitSuffixScale (_arraySize * bytesPerInt);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public double getPerf ()
	{
		return _perf;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals (Object obj)
	{
		if (!(obj instanceof PerfChartResult)) {
			return false;
		}

		//note equality does not require all fields to be equal
		PerfChartResult result = (PerfChartResult) obj;
		return (getNumThreads () == result.getNumThreads () &&
				getArraySize () == result.getArraySize ());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ();
		sb.append ("PerfChartResult");
		sb.append (": numThreads: ");
		sb.append (getNumThreads ());
		sb.append (", arraySize: ");
		sb.append (getArraySizeStr ());
		sb.append (", perf: ");
		sb.append (PerfCpu.formatDouble (getPerf ()));

		return sb.toString ();
	}


	//private members
	private final int _numThreads;
	private final int _arraySize;
	private final double _perf;
}
