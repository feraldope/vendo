//FundRecordComparator.java

package com.vendo.fundViewer;

import java.util.Comparator;

public class FundRecordComparator implements Comparator<FundRecord>
{
	public int compare (FundRecord record1, FundRecord record2)
	{
		String name1 = record1.getFundName ();
		String name2 = record2.getFundName ();

		int compare = name1.compareToIgnoreCase (name2);
		return compare;
	}
}
