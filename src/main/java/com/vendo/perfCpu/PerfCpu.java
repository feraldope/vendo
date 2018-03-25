//PerfCpu.java

package com.vendo.perfCpu;

import java.text.DecimalFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.ui.ApplicationFrame;


public class PerfCpu
{
	public enum PlotType {Line, Scatter};

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		PerfCpu app = new PerfCpu ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
		System.out.println ("Done");
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());
				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("d")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("arraySize") || arg.equalsIgnoreCase ("a")) {
					try {
						_maxArraySize = Integer.parseInt (args[++ii]);
						if (_maxArraySize <= 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("lineChart") || arg.equalsIgnoreCase ("l")) {
					_plotType = PlotType.Line;

				} else if (arg.equalsIgnoreCase ("repeatCount") || arg.equalsIgnoreCase ("r")) {
					try {
						_repeatCount = Integer.parseInt (args[++ii]);
						if (_repeatCount <= 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("seconds") || arg.equalsIgnoreCase ("s")) {
					try {
						_workTimeSecs = Integer.parseInt (args[++ii]);
						if (_workTimeSecs <= 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else if (arg.equalsIgnoreCase ("threads") || arg.equalsIgnoreCase ("t")) {
					try {
						_maxThreads = Integer.parseInt (args[++ii]);
						if (_maxThreads <= 0)
							throw (new NumberFormatException ());
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					} catch (NumberFormatException exception) {
						displayUsage ("Invalid value for /" + arg + " '" + args[ii] + "'", true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (folderString == null) {
					folderString = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

		if (_Debug) {
			for (int ii = 0; ii < args.length; ii++) {
				String arg = args[ii];
				_log.debug ("processArgs: arg = '" + arg + "'");
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] [/lineChart] [/arraySize <number>] [/repeatCount <number>] [/seconds <number>] [/threads <number>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		System.out.println ("PerfCpu.run: availableProcessors: " + getNumLogicalProcessors ());

		createChart (_maxThreads);

		PerfChartResults perfChartResults = new PerfChartResults ();

		//create line for "ideal" processor
		double xOffset = 0;
		for (int numThreads = 1; numThreads <= _maxThreads; numThreads++) {
			PerfChartResult perfChartResult = new PerfChartResult (numThreads, 0, numThreads);
			_perfChart.updateChartData (perfChartResult, xOffset);
		}

		final int bytesPerInt = 4;
		double basis = -1;
		for (int jj = 0; jj < _repeatCount; jj++) {
			xOffset = (double) jj / (double) _repeatCount / 20.;
			for (int arraySize = _minArraySize; arraySize <= _maxArraySize; arraySize *= _arraySizeStep) {
				System.out.println (NL + "PerfCpu.run: arraySize: " + unitSuffixScale (arraySize * bytesPerInt) + ", repeat: " + (jj + 1) + "/" + _repeatCount);
				for (int numThreads = 1; numThreads <= _maxThreads; numThreads++) {
					BlockingQueue<PerfThreadResult> queue = new ArrayBlockingQueue<PerfThreadResult> (numThreads);
					runThreads (numThreads, _workTimeSecs, arraySize, _maxArraySize, queue);

					long totalElapsedMillis = 0;
					int totalLoopCount = 0;
					for (int ii = 0; ii < numThreads; ii++) {
						try {
							PerfThreadResult threadResult = queue.take ();
							totalLoopCount += threadResult.getLoopCount ();
							totalElapsedMillis += threadResult.getElapsedMillis ();

						} catch (Exception ee) {
							ee.printStackTrace ();
						}
					}
					double averageElapsedMillis = (double) totalElapsedMillis / (double) numThreads;
					double normalized = (double) totalLoopCount / averageElapsedMillis;

					if (basis < 0) {
						basis = normalized;
					}

					if (_Debug) {
						System.out.println ("totalLoopCount: " + totalLoopCount);
						System.out.println ("totalElapsedMillis: " + totalElapsedMillis);
						System.out.println ("averageElapsedMillis: " + formatDouble (averageElapsedMillis));
						System.out.println ("normalized: " + formatDouble (normalized));
					}

					PerfChartResult perfChartResult = new PerfChartResult (numThreads, arraySize, normalized / basis);

					if (_plotType == PlotType.Line) {
						perfChartResults.addResult (perfChartResult);
						_perfChart.updateChartData (perfChartResults.getWeightedResult (perfChartResult), 0);
					} else { //PlotType.Scatter
						_perfChart.updateChartData (perfChartResult, xOffset);
					}
				}
			}
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void runThreads (int numThreads, long workTimeSecs, int arraySize, int maxArraySize, BlockingQueue<PerfThreadResult> queue)
	{
		System.gc (); //hint

		CountDownLatch startGate = new CountDownLatch (1);
		CountDownLatch endGate = new CountDownLatch (numThreads);

		for (int ii = 0; ii < numThreads; ii++) {
			Thread thread = new Thread (new PerfTask (startGate, endGate, workTimeSecs, arraySize, maxArraySize, queue));
			thread.start ();
		}

		startGate.countDown (); //tell all threads to start processing

		try {
			endGate.await (); //wait for all threads to finish

		} catch (Exception ee) {
			ee.printStackTrace ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean createChart (int maxThreads)
	{
		_perfChart = new PerfChart (_plotType, maxThreads);

		ChartPanel chartPanel = new ChartPanel (_perfChart.getChart (_AppName));

		chartPanel.setDismissDelay (Integer.MAX_VALUE);
		chartPanel.setInitialDelay (0);
		chartPanel.setReshowDelay (0);

//		chartPanel.setBorder ((BorderFactory.createLineBorder (Color.black)));
		chartPanel.setPreferredSize (new java.awt.Dimension (700, 600));

		ApplicationFrame appFrame = new ApplicationFrame (_AppName);
		appFrame.setContentPane (chartPanel);
		appFrame.pack ();
//		RefineryUtilities.centerFrameOnScreen (appFrame);
		appFrame.setVisible (true);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getNumLogicalProcessors ()
	{
		return Runtime.getRuntime ().availableProcessors ();
	}

	///////////////////////////////////////////////////////////////////////////
	public static String unitSuffixScale (int value)
	{
		String unitSuffix = "";
		if (value >= 1000000) {
			value /= 1000000;
			unitSuffix = "M";

		} else if (value >= 1000) {
			value /= 1000;
			unitSuffix = "K";
		}

		return Integer.toString (value) + unitSuffix;
	}

	///////////////////////////////////////////////////////////////////////////
	public static String formatDouble (double value)
	{
		final DecimalFormat _decimalFormat = new DecimalFormat ("#,##0.00");
		return _decimalFormat.format (value);
	}


	//private members
	private int _maxThreads = getNumLogicalProcessors ();
	private long _workTimeSecs = 2; //recommended min value = 2
	private int _minArraySize = 1 * 1000;
	private int _maxArraySize = 1 * 1000 * 1000; //recommended max value = 10M
	private int _arraySizeStep = 10;
	private int _repeatCount = 2;
	private PlotType _plotType = PlotType.Scatter;

	private	PerfChart _perfChart = null;

	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;
	public static final String _AppName = "PerfCpu";
	public static final String NL = System.getProperty ("line.separator");
}
