//FundRecords.java

package com.vendo.fundViewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;

import javax.swing.table.AbstractTableModel;

import com.vendo.vendoUtils.TableSorter;

public class FundRecords extends AbstractTableModel
{
	///////////////////////////////////////////////////////////////////////////
	public FundRecords ()
	{
		_list = new ArrayList<FundRecord> ();
	}

	///////////////////////////////////////////////////////////////////////////
	public List<FundRecord> getRecords ()
	{
		return _list;
	}

	///////////////////////////////////////////////////////////////////////////
	public void sort ()
	{
		Collections.sort (_list, new FundRecordComparator ());
	}

	///////////////////////////////////////////////////////////////////////////
	public Integer size ()
	{
		return _list.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int readFile (String filename)
	{
		Scanner input;
		try {
			input = new Scanner (new File (filename));
		} catch (IOException exception) {
			System.err.println ("Error opening file \"" + filename + "\"");
			return 0;
		}

		final String firstLine = "<tr valign=\"top\" bgcolor=\"#";

		String line;
		FundRecord record = null;
		while (input.hasNext ()) {
			line = input.nextLine ();
			if (line.indexOf (firstLine) == 0) {
				if (record != null && includeThisRecord (record))
					_list.add (record);			//add previous record

				record = new FundRecord ();		//get new record

				line = input.nextLine ();
				record.extractFundName (line);

				line = input.nextLine ();
				record.extractCategory (line);

				line = input.nextLine ();
				record.extractStarRating (line);

				line = input.nextLine ();
				record.extractYtdReturn (line);
			}
		}

		//add the last record
		if (record != null && includeThisRecord (record))
			_list.add (record);

		input.close ();

System.out.println ("readFile included " + _list.size () + " records");

		return _list.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String quote (String string)
	{
		final String quote = new String ("\"");

		return quote + string + quote;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean writeFile (String filename)
	{
		if (filename != null) {
			StringBuffer sb = new StringBuffer ();

			sb.append ("Name,Ticker,Category,Stars")
			  .append (NL);

			ListIterator<FundRecord> iterator = getRecords ().listIterator ();
			while (iterator.hasNext ()) {
				FundRecord record = iterator.next ();
				sb.append (quote (record.getFundName ()))
				  .append (_comma)
				  .append (quote (record.getTicker ()))
				  .append (_comma)
				  .append (quote (record.getCategory ()))
				  .append (_comma)
				  .append (record.getStarRating ())
				  .append (NL);
			}

			writeFile (sb.toString (), filename);
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeFile (String string, String filename)
	{
		FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream (new File (filename));
		} catch (IOException ee) {
			System.err.println ("Error opening output file \"" + filename + "\"");
			return false;
		}

		try {
			outputStream.write (string.getBytes ());
			outputStream.flush ();
			outputStream.close ();
		} catch (IOException ee) {
			System.err.println ("Error writing to output file \"" + filename + "\"");
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean includeThisRecord (FundRecord record)
	{
		String category = record.getCategory ();

		if (category.startsWith ("Bank Loan"))
			return false;
		else if (category.startsWith ("Bear Market"))
			return false;
		else if (category.startsWith ("Convertibles"))
			return false;
		else if (category.startsWith ("Muni "))
			return false;
		else if (category.startsWith ("Target-Date"))
			return false;

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	//
	// Methods for TableSorter
	//
	///////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////////////////
	public void initTableSorter (TableSorter sorter)
	{
		FundRecord record = _list.get (0);
		sorter.setColumnComparator (record.getFundName ().getClass (), _caseInsensitiveStringComparator);
		sorter.setColumnComparator (record.getYtdReturn ().getClass (), _ytdReturnComparator);
	}

	///////////////////////////////////////////////////////////////////////////
	public int getColumnCount ()
	{
		return 6;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getRowCount ()
	{
		return size ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getColumnName (int col)
	{
		switch (col) {
			default: return "unknown";
			case 0: return "Family";
			case 1: return "Name";
			case 2: return "Ticker";
			case 3: return "Category";
			case 4: return "Rating";
			case 5: return "YTD Return (%)";
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public Object getValueAt (int row, int col)
	{
		FundRecord record = _list.get (row);
		switch (col) {
			default: return "unknown";
			case 0: return record.getFundFamily ();
			case 1: return record.getFundName ();
			case 2: return record.getTicker ();
			case 3: return record.getCategory ();
			case 4: return record.getStarRating ();
			case 5: return record.getYtdReturn ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public Class getColumnClass (int col)
	{
		return getValueAt (0, col).getClass ();
	}

	private static final Comparator<Object> _caseInsensitiveStringComparator = new Comparator<Object> ()
	{
		public int compare (Object o1, Object o2)
		{
			return o1.toString ().compareToIgnoreCase (o2.toString ());
		}
	};

	private static final Comparator<Object> _ytdReturnComparator = new Comparator<Object> ()
	{
		public int compare (Object o1, Object o2)
		{
			FundYtdReturn y1 = (FundYtdReturn) o1;
			FundYtdReturn y2 = (FundYtdReturn) o2;

			return y2.compareTo (y1);
		}
	};

	//private members
	private List<FundRecord> _list = null;
	private static final String _comma = new String (",");

	public static final String NL = System.getProperty ("line.separator");
}
