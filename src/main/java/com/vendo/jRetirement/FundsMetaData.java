//FundsMetaData.java

package com.vendo.jRetirement;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Objects;

public class FundsMetaData implements Comparator<FundsMetaData> {
	///////////////////////////////////////////////////////////////////////////
	public FundsMetaData(String symbol,
						 String fundFamily,
						 String description,
						 Double expenseRatio,
						 FundsEnum.FundTheme fundTheme,
						 FundsEnum.FundType fundType,
						 FundsEnum.ManagementStyle managementStyle,
						 String category,
						 String investmentStyle) {
		this.symbol = symbol;
		this.fundFamily = fundFamily;
		this.description = description;
		this.expenseRatio = expenseRatio;
		this.fundTheme = fundTheme;
		this.fundType = fundType;
		this.managementStyle = managementStyle;
		this.category = category;
		this.investmentStyle = investmentStyle;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsMetaData(FundsEnum fundsEnum) {
		symbol = fundsEnum.getSymbol();
		fundFamily = fundsEnum.getFundFamily();
		description = fundsEnum.getDescription();
		expenseRatio = fundsEnum.getExpenseRatio();
		fundTheme = fundsEnum.getFundTheme();
		fundType = fundsEnum.getFundType();
		managementStyle = fundsEnum.getManagementStyle();
		category = fundsEnum.getCategory();
		investmentStyle = fundsEnum.getInvestmentStyle();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol() {
		return symbol;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFundFamily() {
		return fundFamily;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDescription() {
		return description;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getExpenseRatio() {
		return expenseRatio;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsEnum.FundTheme getFundTheme() {
		return fundTheme;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsEnum.FundType getFundType() {
		return fundType;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsEnum.ManagementStyle getManagementStyle() {
		return managementStyle;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getCategory() {
		return category;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getInvestmentStyle() {
		return investmentStyle;
	}


	///////////////////////////////////////////////////////////////////////////
	public boolean isBond () {
		return fundType == FundsEnum.FundType.BondETF || fundType == FundsEnum.FundType.BondFund;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isCash () {
		return fundType == FundsEnum.FundType.Cash;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isHealth () {
		return "Health".equals(getCategory());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isInternational () {
		return "International".equals(getCategory());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isActive () {
		return FundsEnum.ManagementStyle.Active.equals(getManagementStyle());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isIndex () {
		return FundsEnum.ManagementStyle.Index.equals(getManagementStyle());
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbolForGrouping () {
		String symbol = getSymbol();
		if (symbol.startsWith("61690DP") || symbol.startsWith("06051XD") || symbol.startsWith("05584CN") || symbol.startsWith("949764")) {
			symbol = "CD";
		}
		return symbol;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getURL (String suffix) {
		StringBuilder url = new StringBuilder("https://www.google.com/search?q=");
		url.append(getFundFamily())
				.append("+")
				.append(getSymbol());

		if (!StringUtils.isBlank(suffix)) {
			url.append("+")
					.append(suffix);
		}

		return url.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare(FundsMetaData o1, FundsMetaData o2) {
		throw new RuntimeException ("FundsMetaData.compare: not implemented"); //TODO - implement
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
		FundsMetaData that = (FundsMetaData) o;
		return getSymbol().equals(that.getSymbol()) &&
				Objects.equals(getFundFamily(), that.getFundFamily()) &&
				Objects.equals(getDescription(), that.getDescription()) &&
				Objects.equals(getExpenseRatio(), that.getExpenseRatio()) &&
				getFundTheme() == that.getFundTheme() &&
				getFundType() == that.getFundType() &&
				getManagementStyle() == that.getManagementStyle() &&
				Objects.equals(getCategory(), that.getCategory()) &&
				Objects.equals(getInvestmentStyle(), that.getInvestmentStyle());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		return Objects.hash(getSymbol(),
							getFundFamily(),
							getDescription(),
							getExpenseRatio(),
							getFundTheme(),
							getFundType(),
							getManagementStyle(),
							getCategory(),
							getInvestmentStyle());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("FundsMetaData{");
		sb.append("symbol='").append(symbol).append('\'');
		sb.append(", fundFamily='").append(fundFamily).append('\'');
		sb.append(", description='").append(description).append('\'');
		sb.append(", expenseRatio='").append(expenseRatio).append('\'');
		sb.append(", fundTheme=").append(fundTheme);
		sb.append(", fundType=").append(fundType);
		sb.append(", managementStyle=").append(managementStyle);
		sb.append(", category='").append(category).append('\'');
		sb.append(", investmentStyle='").append(investmentStyle).append('\'');
		sb.append('}');
		return sb.toString();
	}


	//members
	private final String symbol;
	private final String fundFamily;
	private final String description;
	private final Double expenseRatio; //adjusted expense ratio, if applies
	private final FundsEnum.FundTheme fundTheme;
	private final FundsEnum.FundType fundType;
	private final FundsEnum.ManagementStyle managementStyle;
	private final String category;
	private final String investmentStyle;
}
