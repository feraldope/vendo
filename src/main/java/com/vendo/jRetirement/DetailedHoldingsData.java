//DetailedHoldingsData.java

package com.vendo.jRetirement;

import java.util.*;

public class DetailedHoldingsData /*extends RetirementBaseData*/ implements Comparator<DetailedHoldingsData> {
	///////////////////////////////////////////////////////////////////////////
	public DetailedHoldingsData(int rowNumberInFile,
								String symbol,
								String description,
								String account,
								String investmentType,
								String morningstarCategory,
								String stockStyleCategory,
								String bondStyleCategory) {
		this.rowNumberInFile = rowNumberInFile;
		this.symbol = symbol;
		this.description = description;
		this.account = account;
		this.investmentType = investmentType;
		this.morningstarCategory = morningstarCategory;
		this.stockStyleCategory = stockStyleCategory;
		this.bondStyleCategory = bondStyleCategory;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getrowNumberInFile() {
		return rowNumberInFile;
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
	public String getAccount() {
		return account;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getInvestmentType() {
		return investmentType;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getMorningstarCategory() {
		return morningstarCategory;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getStockStyleCategory() {
		return stockStyleCategory;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getBondStyleCategory() {
		return bondStyleCategory;
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

		//do not compare rowNumberInFile
		DetailedHoldingsData that = (DetailedHoldingsData) o;
		return getSymbol().equals(that.getSymbol()) &&
				getDescription().equals(that.getDescription()) &&
				getAccount().equals(that.getAccount()) &&
				getInvestmentType().equals(that.getInvestmentType()) &&
				getMorningstarCategory().equals(that.getMorningstarCategory()) &&
				Objects.equals(getStockStyleCategory(), that.getStockStyleCategory()) &&
				Objects.equals(getBondStyleCategory(), that.getBondStyleCategory());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		//do not hash rowNumberInFile
		return Objects.hash(getSymbol(),
							getDescription(),
							getAccount(),
							getInvestmentType(),
							getMorningstarCategory(),
							getStockStyleCategory(),
							getBondStyleCategory());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("DetailedHoldingsData{");
		sb.append("rowNumberInFile='").append(rowNumberInFile).append('\'');
		sb.append(", symbol='").append(symbol).append('\'');
		sb.append(", description='").append(description).append('\'');
		sb.append(", account='").append(account).append('\'');
		sb.append(", investmentType='").append(investmentType).append('\'');
		sb.append(", morningstarCategory='").append(morningstarCategory).append('\'');
		sb.append(", stockStyleCategory='").append(stockStyleCategory).append('\'');
		sb.append(", bondStyleCategory='").append(bondStyleCategory).append('\'');
		sb.append('}');
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare(DetailedHoldingsData o1, DetailedHoldingsData o2) {
		throw new RuntimeException ("DetailedHoldingsData.compare: not implemented"); //TODO - implement
	}


	//private members
	//originally from XLS file
	private final int rowNumberInFile;
	private final String symbol;
	private final String description;
	private final String account;
	private final String investmentType;
	private final String morningstarCategory;
	private final String stockStyleCategory;
	private final String bondStyleCategory;
}
