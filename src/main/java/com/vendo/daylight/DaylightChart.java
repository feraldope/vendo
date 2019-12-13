//DaylightChart.java

//JavaDoc http://jfree.org/jfreechart/api/javadoc/

package com.vendo.daylight;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;


public class DaylightChart //extends JFreeChart
{
	private enum AnnotationStyle {Day, Sunrise, Sunset}

	///////////////////////////////////////////////////////////////////////////
	public JFreeChart getChart (String chartTitle, DaylightData daylightData)
	{
		//create chart
		TimeSeries sunrise = new TimeSeries (_sunriseName);
		TimeSeries sunset = new TimeSeries (_sunsetName);

		RegularTimePeriod firstDay = null;
		RegularTimePeriod day = new Day ();

		for (DaylightRecord daylightRecord : daylightData.getDaylightDataList ()) {
			Date date = Date.from (daylightRecord.getLocalDate ().atStartOfDay (ZoneId.systemDefault ()).toInstant ());
			day = new Day (date);
			if (firstDay == null) {
				firstDay = day;
			}

			sunrise.add (day, getTimeAsMillis (daylightRecord.getSunriseTime ()));
			sunset.add (day, getTimeAsMillis (daylightRecord.getSunsetTime ()));
		}
		RegularTimePeriod lastDay = day;

		TimeSeriesCollection dataset = new TimeSeriesCollection ();
		dataset.addSeries (sunrise);
		dataset.addSeries (sunset);

		String yAxisLabel = _sunriseName + " / " + _sunsetName;
		JFreeChart chart = ChartFactory.createTimeSeriesChart (chartTitle,	//chart title
															   "Date",		//X-axis label
															   yAxisLabel,	//Y-axis label
															   dataset,		//data
															   true,		//show legend
															   true,		//show tool tips
															   false);		//show URLs

		final XYPlot plot = chart.getXYPlot ();

		//draw the domain (X) axis labels as month labels in the middle of the month (i.e., centered)
		DateAxis dateAxis = (DateAxis) plot.getDomainAxis ();
		dateAxis.setTickMarkPosition (DateTickMarkPosition.MIDDLE);
		DateTickUnit dateTickUnit = new DateTickUnit (DateTickUnitType.MONTH, 1, _dateFormatMonth);
		dateAxis.setTickUnit (dateTickUnit);
		dateAxis.setRange (firstDay.getStart (), lastDay.getStart ());
		if (Daylight._Debug) {
			System.out.println ("dateAxis: firstDay = " + firstDay.getStart () + " (" + firstDay.getFirstMillisecond () + " ms)");
			System.out.println ("dateAxis: lastDay  = " + lastDay.getStart () + " (" + lastDay.getFirstMillisecond () + " ms)");
		}

		//draw the range (Y) axis
		DateAxis timeAxis = new DateAxis ("Time");
//		timeAxis.setLowerMargin (0.15);
//		timeAxis.setUpperMargin (0.15);
		long startMilli = (new Minute (0, 0, 1, 1, 1970)).getFirstMillisecond ();
		long endMilli   = (new Minute (0, 0, 2, 1, 1970)).getFirstMillisecond () - 1;
		if (Daylight._Debug) {
			System.out.println ("timeAxis: startMilli = " + startMilli + ", endMilli = " + endMilli);
		}
		timeAxis.setRange (startMilli, endMilli);
		plot.setRangeAxis (timeAxis);

// it turns out this does not work with jfreechart-1.0.19.jar
//		plot.setRangeAxes (new DateAxis[] {timeAxis, timeAxis}); //draw Y-axis labels on both sides of chart

		//draw domain grid lines on the first of each month using markers
		//first disable default domain domain grid lines (which will be drawn by default in same place as labels, i.e., in the middle of the month)
		plot.setDomainGridlinesVisible (false);
		day = firstDay;
		for (int ii = 1; ii <= 365 + 1; ii++) { //add 1 day to get 13 grid lines
			Calendar calendar = Calendar.getInstance ();
			calendar.setTime (day.getStart ());
			if (calendar.get (Calendar.DAY_OF_MONTH) == 1) {
				if (Daylight._Debug) {
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

		//mark today and other interesting days
		plot.addAnnotation (getDayAnnotation (daylightData.getDaylightRecord (LocalDate.now ()), AnnotationStyle.Day, Color.green));
		plot.addAnnotation (getDayAnnotation (daylightData.getShortestDay (), AnnotationStyle.Day, Color.red));
		plot.addAnnotation (getDayAnnotation (daylightData.getLongestDay (), AnnotationStyle.Day, Color.red));
		plot.addAnnotation (getDayAnnotation (daylightData.getEarliestSunrise (), AnnotationStyle.Sunrise, Color.black));
		plot.addAnnotation (getDayAnnotation (daylightData.getLatestSunrise (), AnnotationStyle.Sunrise, Color.black));
		plot.addAnnotation (getDayAnnotation (daylightData.getEarliestSunset (), AnnotationStyle.Sunset, Color.black));
		plot.addAnnotation (getDayAnnotation (daylightData.getLatestSunset (), AnnotationStyle.Sunset, Color.black));

		//define custom tooltips; for reference, the default tooltip is: "Sunrise: (5/28/16 12:00 AM, 18,720)"
		XYItemRenderer renderer = plot.getRenderer ();
		renderer.setBaseToolTipGenerator (new StandardXYToolTipGenerator () {
			private static final long serialVersionUID = 1L;
			@Override
			public String generateToolTip (XYDataset dataset, int series, int index)
			{
				TimeSeries timeSeries = asTimeSeriesCollection (dataset).getSeries (0);
				Date date = timeSeries.getTimePeriod (index).getStart ();
				LocalDate localDate = date.toInstant ().atZone (ZoneId.systemDefault ()).toLocalDate ();
				DaylightRecord daylightRecord = daylightData.getDaylightRecord (localDate);

				String sunriseTimeString = _timeFormat.format (daylightRecord.getSunriseTime ().toSecondOfDay () * 1000);
				String sunsetTimeString = _timeFormat.format (daylightRecord.getSunsetTime ().toSecondOfDay () * 1000);
				String lengthOfDayString = LocalTime.ofSecondOfDay (daylightRecord.getLengthOfDay ().toMillis () / 1000).format (_lengthOfDayFormatter);

				StringBuffer sb = new StringBuffer ();
				sb.append (_dateFormatMonthDay.format (date)).append (": ")
				  .append (_sunriseName).append (" ").append (sunriseTimeString).append (", ")
				  .append (_sunsetName).append (" ").append (sunsetTimeString)
				  .append (" (").append (lengthOfDayString).append (")");

//				if (Daylight._Debug) {
//					System.out.println (sb.toString ());
//				}
				return sb.toString ();
			}
		});

		return chart;
	}

	///////////////////////////////////////////////////////////////////////////
	private XYLineAnnotation getDayAnnotation (DaylightRecord daylightRecord, AnnotationStyle annotationStyle, Paint paint)
	{
		final long lineHeightMillis = 30 * 60 * 1000;

		long dateMillis = getDateAsMillis (daylightRecord.getLocalDate ());
		long lowMillis = getTimeAsMillis (daylightRecord.getSunriseTime ());
		long highMillis = getTimeAsMillis (daylightRecord.getSunsetTime ());

		if (annotationStyle == AnnotationStyle.Sunrise) {
			highMillis = lowMillis + lineHeightMillis / 2;
			lowMillis -= lineHeightMillis / 2;
		}

		if (annotationStyle == AnnotationStyle.Sunset) {
			lowMillis = highMillis - lineHeightMillis / 2;
			highMillis += lineHeightMillis / 2;
		}

		return new XYLineAnnotation (dateMillis, lowMillis, dateMillis, highMillis, XYPlot.DEFAULT_OUTLINE_STROKE, paint);
	}

	///////////////////////////////////////////////////////////////////////////
	private long getDateAsMillis (LocalDate localDate)
	{
		return localDate.atStartOfDay (ZoneId.systemDefault ()).toEpochSecond () * 1000;
	}

	///////////////////////////////////////////////////////////////////////////
	private long getTimeAsMillis (LocalTime localTime)
	{
		Minute minute = new Minute (localTime.getMinute (), localTime.getHour (), 1, 1, 1970);
		return minute.getFirstMillisecond ();
	}

	///////////////////////////////////////////////////////////////////////////
//	@SuppressWarnings ("unchecked")
	private TimeSeriesCollection asTimeSeriesCollection (XYDataset dataset) //cast
	{
		return dataset instanceof TimeSeriesCollection ? (TimeSeriesCollection) dataset : null;
	}


	//private members
	private static final String _sunriseName = "Sunrise";
	private static final String _sunsetName = "Sunset";
	private static final SimpleDateFormat _dateFormatMonth = new SimpleDateFormat ("MMM"); //format date for domain axis
	private static final SimpleDateFormat _dateFormatMonthDay = new SimpleDateFormat ("MMM dd"); //format date fior tooltip
	private static final SimpleDateFormat _timeFormat = new SimpleDateFormat ("h:mm a"); //format time for tooltip
//	private static final DateTimeFormatter _lengthOfDayFormatter = DateTimeFormatter.ofPattern ("HH'h':mm'm'"); //for example: 09h:13m
	private static final DateTimeFormatter _lengthOfDayFormatter = DateTimeFormatter.ofPattern ("HH:mm"); //for example: 09:13

//	private static final Logger _log = LogManager.getLogger ();

	//global members
	public static final String NL = System.getProperty ("line.separator");
}
