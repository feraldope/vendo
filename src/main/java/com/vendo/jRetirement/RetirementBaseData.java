//RetirementBaseData.java

package com.vendo.jRetirement;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public abstract class RetirementBaseData /*implements Comparator<RetirementBaseData>*/ {
	///////////////////////////////////////////////////////////////////////////
	public RetirementBaseData(String accountName, String symbol, String description) {
		this.accountName = accountName;
		this.symbol = symbol;
		this.description = description;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getAccountName() {
		return accountName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol() {
		return symbol;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDescription() {
		return description;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RetirementBaseData that = (RetirementBaseData) o;
		return getAccountName().equals(that.getAccountName()) &&
				getSymbol().equals(that.getSymbol()) &&
				Objects.equals(getDescription(), that.getDescription());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		return Objects.hash(getAccountName(),
							getSymbol(),
							getDescription());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("RetirementBaseData{");
		sb.append("accountName='").append(getAccountName()).append('\'');
		sb.append("symbol='").append(getSymbol()).append('\'');
		sb.append(", description='").append(getDescription()).append('\'');
		sb.append('}');
		return sb.toString();
	}

	//protected members
	protected final String accountName;
	protected final String symbol;
	protected final String description;

	protected static final DecimalFormat dollarFormat0 = new DecimalFormat ("$###,##0;($###,##0)"); //embedded "$"; format negative numbers with parenthesis
	protected static final DateTimeFormatter dateTimeFormatterMdy = DateTimeFormatter.ofPattern("MM/dd/yy").withZone(ZoneId.systemDefault());

	public static final String NL = System.getProperty ("line.separator");
}
