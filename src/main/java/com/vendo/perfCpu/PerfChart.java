//PerfChart.java

//JavaDoc http://jfree.org/jfreechart/api/javadoc/

package com.vendo.perfCpu;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnitSource;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class PerfChart
{
	///////////////////////////////////////////////////////////////////////////
	public PerfChart (PerfCpu.PlotType plotType, int maxThreads)
	{
		_plotType = plotType;
		_maxThreads = maxThreads;
	}

	///////////////////////////////////////////////////////////////////////////
	public JFreeChart getChart (String chartTitle)
	{
		JFreeChart chart = null;
		if (_plotType == PerfCpu.PlotType.Line) {
			chart = ChartFactory.createXYLineChart (chartTitle,
													"Number of Threads", //X-axis label
													"Relative Performance", //Y-axis label
													_seriesCollection,
													PlotOrientation.VERTICAL,
													true, //legend
													true, //tooltips
													false); //urls

		} else { //PlotType.Scatter
			chart = ChartFactory.createScatterPlot (chartTitle,
													"Number of Threads", //X-axis label
													"Relative Performance", //Y-axis label
													_seriesCollection);
		}

		XYPlot plot = chart.getXYPlot ();

		//set the domain (X) axis ticks and range
		NumberAxis xAxis = asNumberAxis (plot.getDomainAxis ());
		xAxis.setStandardTickUnits (new NumberTickUnitSource (true));
		xAxis.setRange (0.5, _maxThreads + 0.5);

		//set the range (Y) axis range
		NumberAxis yAxis = asNumberAxis (plot.getRangeAxis ());
//		yAxis.setRange (new Range (0, 100), /*turnOffAutoRange*/ false, /*notify*/ true);
		yAxis.setRangeType (RangeType.POSITIVE);
//		yAxis.setAutoRangeMinimumSize (100, true);
		yAxis.setAutoRangeMinimumSize (1, true);
		yAxis.setAutoRangeIncludesZero (true);

		XYLineAndShapeRenderer renderer = asXYLineAndShapeRenderer (plot.getRenderer ());
		renderer.setBaseShapesVisible (true);

		renderer.setBaseToolTipGenerator (new StandardXYToolTipGenerator ()
		{
			private static final long serialVersionUID = 1L;
			public String generateToolTip (XYDataset dataset, int series, int index)
			{
				XYSeries xySeries = asXYSeriesCollection (dataset).getSeries (series);

				if (series == 0) {
					return xySeries.getKey ().toString ();

				} else {
					int threads = (int) xySeries.getX (index).doubleValue ();
					double base = xySeries.getY (0).doubleValue ();
					double value = xySeries.getY (index).doubleValue ();

					StringBuffer sb = new StringBuffer ();
					sb.append (xySeries.getKey ());
					sb.append (": Ideal: ");
					sb.append (threads);
					sb.append ("x, Actual: ");
					sb.append (PerfCpu.formatDouble (value / base));
					sb.append ("x");

					return sb.toString ();
				}
			}
		});

		return chart;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean updateChartData (PerfChartResult perfChartResult, double xOffset)
	{
		System.out.println ("PerfChart.updateChartData: " + perfChartResult);

		String seriesKey = perfChartResult.getArraySizeStr ();

		XYSeries xySeries;
		try {
			xySeries = _seriesCollection.getSeries (seriesKey);
		} catch (UnknownKeyException ee) {
			xySeries = new XYSeries (seriesKey);
			_seriesCollection.addSeries (xySeries);
		}

		if (_plotType == PerfCpu.PlotType.Line) {
			try {
				//update value if it is already in the series, else drop into catch block to add it
				xySeries.getX (perfChartResult.getNumThreads () - 1);
				xySeries.update ((double) perfChartResult.getNumThreads (), perfChartResult.getPerf ());

			} catch (IndexOutOfBoundsException ee) {
				xySeries.add ((double) perfChartResult.getNumThreads (), perfChartResult.getPerf ());
			}

		} else { //PlotType.Scatter
			xySeries.add (perfChartResult.getNumThreads () + xOffset, perfChartResult.getPerf ());
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	@SuppressWarnings ("unchecked")
	private NumberAxis asNumberAxis (Axis axis) //cast
	{
		return axis instanceof NumberAxis ? (NumberAxis) axis : null;
	}

	///////////////////////////////////////////////////////////////////////////
	@SuppressWarnings ("unchecked")
	private XYSeriesCollection asXYSeriesCollection (XYDataset dataset) //cast
	{
		return dataset instanceof XYSeriesCollection ? (XYSeriesCollection) dataset : null;
	}

	///////////////////////////////////////////////////////////////////////////
	@SuppressWarnings ("unchecked")
	private XYLineAndShapeRenderer asXYLineAndShapeRenderer (XYItemRenderer renderer) //cast
	{
		return renderer instanceof XYLineAndShapeRenderer ? (XYLineAndShapeRenderer) renderer : null;
	}


	//private members
	private final PerfCpu.PlotType _plotType;
	private final int _maxThreads;
	private final XYSeriesCollection _seriesCollection = new XYSeriesCollection ();

	private static final Logger _log = LogManager.getLogger ();
}
