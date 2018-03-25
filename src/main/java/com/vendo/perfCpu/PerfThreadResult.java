//PerfThreadResult.java

package com.vendo.perfCpu;


public class PerfThreadResult
{
	///////////////////////////////////////////////////////////////////////////
	PerfThreadResult (long elapsedMillis, int arraySize, int loopCount, long value)
	{
		_elapsedMillis = elapsedMillis;
		_arraySize = arraySize;
		_loopCount = loopCount;
		_value = value;
	}

	///////////////////////////////////////////////////////////////////////////
	public long getElapsedMillis ()
	{
		return _elapsedMillis;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getArraySizeStr ()
	{
		return PerfCpu.unitSuffixScale (_arraySize);
	}

	///////////////////////////////////////////////////////////////////////////
	public int getArraySize ()
	{
		return _arraySize;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getLoopCount ()
	{
		return _loopCount;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ();
		sb.append ("PerfThreadResult");
		sb.append (": elapsedMillis: ");
		sb.append (getElapsedMillis ());
		sb.append (", arraySize: ");
		sb.append (getArraySizeStr ());
		sb.append (", loopCount: ");
		sb.append (getLoopCount ());
//		sb.append (", value: ");
//		sb.append (_value);

		return sb.toString ();
	}


	//private members
	private final long _elapsedMillis;
	private final int _arraySize;
	private final int _loopCount;
	private final long _value;
}
