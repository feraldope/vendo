//OilUsageChart.java

package com.vendo.oilUsage;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;


public class OilUsageChart //extends JFreeChart
{
	///////////////////////////////////////////////////////////////////////////
//	public OilUsageChart (String chartTitle, OilUsageData oilUsageData)
//	{
//	}

	///////////////////////////////////////////////////////////////////////////
	public JFreeChart getChart (String chartTitle, OilUsageData oilUsageData)
	{
		//create chart
		RegularTimePeriod day = _firstConcreteDayToPlot;
		TimeSeries dailyAverage = new TimeSeries ("Daily Average");
		TimeSeries dailyMin = new TimeSeries ("Daily Min");
		TimeSeries dailyMax = new TimeSeries ("Daily Max");
		TimeSeries overallAverage = new TimeSeries ("Overall Average");
		for (int ii = 1; ii <= 365; ii++) {
			dailyAverage.add (day, oilUsageData.getDailyAverage (ii));
			dailyMin.add (day, oilUsageData.getDailyMin (ii));
			dailyMax.add (day, oilUsageData.getDailyMax (ii));
			overallAverage.add (day, oilUsageData.getOverallAverage ());
			day = day.next ();
		}
		RegularTimePeriod lastDay = day.previous ();

		TimeSeriesCollection dataset = new TimeSeriesCollection ();
		dataset.addSeries (dailyAverage);
		dataset.addSeries (dailyMin);
		dataset.addSeries (dailyMax);
		dataset.addSeries (overallAverage);

		JFreeChart chart = ChartFactory.createTimeSeriesChart (chartTitle,		//chart title
															   "Date",			//X-axis label
															   "Gallons/day",	//Y-axis label
															   dataset,			//data
															   true,			//show legend
															   true,			//show tool tips
															   false);			//show URLs

		XYPlot plot = chart.getXYPlot ();

		//draw the domain axis labels as month labels in the middle of the month (i.e., centered)
		DateAxis dateAxis = (DateAxis) plot.getDomainAxis ();
		dateAxis.setTickMarkPosition (DateTickMarkPosition.MIDDLE);
		DateTickUnit dateTickUnit = new DateTickUnit (DateTickUnitType.MONTH, 1, _dateFormatMonth);
		dateAxis.setTickUnit (dateTickUnit);
		dateAxis.setRange (_firstConcreteDayToPlot.getStart (), lastDay.getStart ());

		ValueAxis valueAxis = (ValueAxis) plot.getRangeAxis ();
		valueAxis.setLowerBound (0);

		//draw domain grid lines on the first of each month using markers
		//first disable default domain domain grid lines (which will be drawn by default in same place as labels, i.e., in the middle of the month)
		plot.setDomainGridlinesVisible (false);
		day = _firstConcreteDayToPlot;
		for (int ii = 1; ii <= 365 + 1; ii++) { //add 1 day to get 13 grid lines
			Calendar calendar = Calendar.getInstance ();
			calendar.setTime (day.getStart ());
			if (calendar.get (Calendar.DAY_OF_MONTH) == 1) {
				if (OilUsage._Debug) {
					System.out.println ("adding domain marker for: " + calendar.getTime ());
				}

				//surprisingly, XYPlot.DEFAULT_GRIDLINE_PAINT = grey (which is the same as the default background)
//				System.out.println (XYPlot.DEFAULT_GRIDLINE_PAINT);
//				ValueMarker domainMarker = new ValueMarker (day.getFirstMillisecond (), XYPlot.DEFAULT_GRIDLINE_PAINT, XYPlot.DEFAULT_GRIDLINE_STROKE);
				ValueMarker domainMarker = new ValueMarker (day.getFirstMillisecond (), Color.white, XYPlot.DEFAULT_GRIDLINE_STROKE);
				plot.addDomainMarker (domainMarker);
			}
			day = day.next ();
		}

/*unused: decided to show average as series instead
		//add marker for overall average
		ValueMarker rangeMarker = new ValueMarker (oilUsageData.getOverallAverage (), Color.red, XYPlot.DEFAULT_GRIDLINE_STROKE);
		plot.addRangeMarker (rangeMarker);
*/

		//for reference, the default tooltip is: "OilUsage: (3/2/15 12:00 AM, 2.335)"
		XYItemRenderer renderer = plot.getRenderer ();
		renderer.setBaseToolTipGenerator (new StandardXYToolTipGenerator () {
			private static final long serialVersionUID = 1L;
			public String generateToolTip (XYDataset dataset, int series, int index)
			{
				TimeSeriesCollection timeSeriesCollection = (TimeSeriesCollection) dataset;
				TimeSeries timeSeries = timeSeriesCollection.getSeries (series);
				Number number = timeSeries.getValue (index);
				String name = timeSeries.getKey ().toString ();

				if (series != 3) {
					RegularTimePeriod regularTimePeriod = timeSeries.getTimePeriod (index);
					Date date = regularTimePeriod.getStart ();

					return name + ": " + _dateFormatMonthDay.format (date) + ": " + _decimalFormat.format (number);
				} else {
					return name + ": " + _decimalFormat.format (number);
				}
			}
		});

		return chart;
	}


	//private members
	private static final RegularTimePeriod _firstConcreteDayToPlot = new Day (1, 1, 2015); //1-Jan-2015: convert logical dayOfYear into concrete day/year (but avoid a leap year)

	private static final SimpleDateFormat _dateFormatMonth = new SimpleDateFormat ("MMM");
	private static final SimpleDateFormat _dateFormatMonthDay = new SimpleDateFormat ("MMM dd");
	private static final DecimalFormat _decimalFormat = new DecimalFormat ("###,##0.00");

	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static final String NL = System.getProperty ("line.separator");
}
