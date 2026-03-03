//FundsMetaData.java

package com.vendo.jRetirement;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Objects;

public class FundsMetaData extends RetirementBaseData implements Comparator<FundsMetaData> {
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
		super("not used", symbol, description);
		this.fundFamily = fundFamily;
		this.expenseRatio = expenseRatio;
		this.fundTheme = fundTheme;
		this.fundType = fundType;
		this.managementStyle = managementStyle;
		this.category = category;
		this.investmentStyle = investmentStyle;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsMetaData(FundsEnum fundsEnum) {
		super("not used", fundsEnum.getSymbol(), fundsEnum.getDescription());
		fundFamily = fundsEnum.getFundFamily();
		expenseRatio = fundsEnum.getExpenseRatio();
		fundTheme = fundsEnum.getFundTheme();
		fundType = fundsEnum.getFundType();
		managementStyle = fundsEnum.getManagementStyle();
		category = fundsEnum.getCategory();
		investmentStyle = fundsEnum.getInvestmentStyle();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFundFamily() {
		return fundFamily;
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
	public boolean isBond() {
		return fundType == FundsEnum.FundType.BondETF || fundType == FundsEnum.FundType.BondFund;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isCash() {
		return fundType == FundsEnum.FundType.Cash;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isCrypto() {
		return fundType == FundsEnum.FundType.Crypto;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isHealth() {
		return "Health".equals(getCategory());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isInternational() {
		return "International".equals(getCategory());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isActive() {
		return FundsEnum.ManagementStyle.Active.equals(getManagementStyle());
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isIndex() {
		return FundsEnum.ManagementStyle.Index.equals(getManagementStyle());
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbolForGrouping() {
		String symbol = getSymbol();
		if (symbol.startsWith("61690DP") || symbol.startsWith("06051XD") || symbol.startsWith("05584CN") || symbol.startsWith("949764")) {
			symbol = "CD";
		}
		return symbol;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getURL(String suffix) {
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
	public boolean equals(Object o) { //includes fields from the base class
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
	public int hashCode() { //includes fields from the base class
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
	public String toString() { //includes fields from the base class
		final StringBuffer sb = new StringBuffer("FundsMetaData{");
		sb.append("symbol='").append(getSymbol()).append('\'');
		sb.append(", fundFamily='").append(getFundFamily()).append('\'');
		sb.append(", description='").append(getDescription()).append('\'');
		sb.append(", expenseRatio='").append(getExpenseRatio()).append('\'');
		sb.append(", fundTheme=").append(getFundTheme());
		sb.append(", fundType=").append(getFundType());
		sb.append(", managementStyle=").append(getManagementStyle());
		sb.append(", category='").append(getCategory()).append('\'');
		sb.append(", investmentStyle='").append(getInvestmentStyle()).append('\'');
		sb.append('}');
		return sb.toString();
	}


	//members
	private final String fundFamily;
	private final Double expenseRatio; //adjusted expense ratio, if applies
	private final FundsEnum.FundTheme fundTheme;
	private final FundsEnum.FundType fundType;
	private final FundsEnum.ManagementStyle managementStyle;
	private final String category;
	private final String investmentStyle;
}
