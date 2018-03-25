//PerfTask.java

package com.vendo.perfCpu;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


class PerfTask implements Runnable
{
	///////////////////////////////////////////////////////////////////////////
	public PerfTask (CountDownLatch startGate, CountDownLatch endGate, long workTimeSecs, int arraySize, int maxArraySize, BlockingQueue<PerfThreadResult> queue)
	{
		_startGate = startGate;
		_endGate = endGate;
		_workTimeNanos = workTimeSecs * (1000 * 1000 * 1000);
		_arraySize = arraySize;
		_repeatCount = maxArraySize / arraySize;
		_queue = queue;

		_intArray = new int [_arraySize];
		for (int ii = 0; ii < _arraySize; ii++) {
			_intArray[ii] = _random.nextInt ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void run ()
	{
		int loopCount = 0;
		long elapsedNanos = 0;
		long value = _random.nextLong ();

		try {
			_startGate.await ();
		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		long startNano = System.nanoTime ();
		do {
			value = doWork (value);
			loopCount++;
			elapsedNanos = System.nanoTime () - startNano;
		} while (elapsedNanos < _workTimeNanos);

		try {
			_queue.put (new PerfThreadResult (elapsedNanos / (1000 * 1000), _arraySize, loopCount, value));
		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		_endGate.countDown ();
	}

	///////////////////////////////////////////////////////////////////////////
	public long doWork (long value)
	{
		for (int jj = 0; jj < _repeatCount; jj++) {
			for (int ii = 0; ii < _arraySize; ii++) {
				value ^= _intArray[ii];
			}
		}

		return value;
	}

/* math-intensive version
	///////////////////////////////////////////////////////////////////////////
	public long doWork (long value)
	{
		final int count = 1000 * 1000;

		double x = value;
		for (int ii = 0; ii < count; ii++) {
			x = Math.sqrt (x) + 1;
		}

		return (long) x;
	}
*/


	//private members
	private final CountDownLatch _startGate;
	private final CountDownLatch _endGate;
	private final long _workTimeNanos;
	private final int _arraySize;
	private final int _repeatCount;
	private final BlockingQueue<PerfThreadResult> _queue;

	private final int _intArray[];
	private final Random _random = new Random ();

	private static final Logger _log = LogManager.getLogger ();
}
