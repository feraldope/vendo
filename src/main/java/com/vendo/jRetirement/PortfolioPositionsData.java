//PortfolioPositionsData.java

package com.vendo.jRetirement;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public class PortfolioPositionsData implements Comparator<PortfolioPositionsData> {
	///////////////////////////////////////////////////////////////////////////
	public PortfolioPositionsData(Instant dateDownloaded,
								  String accountNumber,
								  String accountName,
								  String symbol,
								  String description,
//								  Double quantity,
//								  Double lastPrice,
//								  Double lastPriceChange,
								  Double currentValue,
//								  Double todaysGainLossDollar,
//								  Double todaysGainLossPercent,
//								  Double totalGainLossDollar,
//								  Double totalGainLossPercent,
//								  Double percentOfAccount,
//								  Double costBasisTotal,
//								  Double averageCostBasis,
//								  String type,
								  FundsEnum.TaxableType taxableType,
								  FundsEnum.FundOwner fundOwner,
								  FundsMetaData fundsMetaData) {
		this.dateDownloaded = dateDownloaded;
		this.accountNumber = accountNumber;
		this.accountName = accountName;
		this.symbol = symbol;
		this.description = description;
//		this.quantity = quantity;
//		this.lastPrice = lastPrice;
//		this.lastPriceChange = lastPriceChange;
		this.currentValue = currentValue;
//		this.todaysGainLossDollar = todaysGainLossDollar;
//		this.todaysGainLossPercent = todaysGainLossPercent;
//		this.totalGainLossDollar = totalGainLossDollar;
//		this.totalGainLossPercent = totalGainLossPercent;
//		this.percentOfAccount = percentOfAccount;
//		this.costBasisTotal = costBasisTotal;
//		this.averageCostBasis = averageCostBasis;
//		this.type = type;
		this.taxableType = taxableType;
		this.fundOwner = fundOwner;
		this.fundsMetaData = fundsMetaData;
	}

	///////////////////////////////////////////////////////////////////////////
	public PortfolioPositionsData(CsvPortfolioPositionsBean bean) {
		this.dateDownloaded = bean.getDateDownloaded();
		this.accountNumber = bean.getAccountNumber();
		this.accountName = bean.getAccountName();
		this.symbol = bean.getSymbol();
		this.description = bean.getDescription();
		this.currentValue = bean.getCurrentValue();
		this.taxableType = bean.getTaxableType();
		this.fundOwner = bean.getFundOwner();
		this.fundsMetaData = null;
	}

	///////////////////////////////////////////////////////////////////////////
	public Instant getDateDownloaded() {
		return dateDownloaded;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getAccountNumber() {
		return accountNumber;
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
//	public Double getQuantity() {
//		return quantity;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getLastPrice() {
//		return lastPrice;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getLastPriceChange() {
//		return lastPriceChange;
//	}

	///////////////////////////////////////////////////////////////////////////
	public Double getCurrentValue() {
		return currentValue;
	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getTodaysGainLossDollar() {
//		return todaysGainLossDollar;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getTodaysGainLossPercent() {
//		return todaysGainLossPercent;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getTotalGainLossDollar() {
//		return totalGainLossDollar;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getTotalGainLossPercent() {
//		return totalGainLossPercent;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getPercentOfAccount() {
//		return percentOfAccount;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getCostBasisTotal() {
//		return costBasisTotal;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getAverageCostBasis() {
//		return averageCostBasis;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public String getType() {
//		return type;
//	}

	///////////////////////////////////////////////////////////////////////////
	public FundsEnum.TaxableType getTaxableType() {
		return taxableType;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsEnum.FundOwner getFundOwner() {
		return fundOwner;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsMetaData getFundsMetaData() {
		return fundsMetaData;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isPendingActivity() {
		return JRetirement.PendingActivityString.equalsIgnoreCase(getSymbol());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare(PortfolioPositionsData o1, PortfolioPositionsData o2) {
		throw new RuntimeException ("PortfolioPositionsData.compare: not implemented"); //TODO - implement
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
		PortfolioPositionsData that = (PortfolioPositionsData) o;
		return getDateDownloaded().equals(that.getDateDownloaded()) &&
				getAccountNumber().equals(that.getAccountNumber()) &&
				getAccountName().equals(that.getAccountName()) &&
				getSymbol().equals(that.getSymbol()) &&
				getDescription().equals(that.getDescription()) &&
//				Objects.equals(getQuantity(), that.getQuantity()) &&
//				Objects.equals(getLastPrice(), that.getLastPrice()) &&
//				Objects.equals(getLastPriceChange(), that.getLastPriceChange()) &&
				getCurrentValue().equals(that.getCurrentValue()) &&
//				Objects.equals(getTodaysGainLossDollar(), that.getTodaysGainLossDollar()) &&
//				Objects.equals(getTodaysGainLossPercent(), that.getTodaysGainLossPercent()) &&
//				Objects.equals(getTotalGainLossDollar(), that.getTotalGainLossDollar()) &&
//				Objects.equals(getTotalGainLossPercent(), that.getTotalGainLossPercent()) &&
//				Objects.equals(getPercentOfAccount(), that.getPercentOfAccount()) &&
//				Objects.equals(getCostBasisTotal(), that.getCostBasisTotal()) &&
//				Objects.equals(getAverageCostBasis(), that.getAverageCostBasis()) &&
//				Objects.equals(getType(), that.getType()) &&
				Objects.equals(getTaxableType(), that.getTaxableType()) &&
				Objects.equals(getFundOwner(), that.getFundOwner());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		return Objects.hash(getDateDownloaded(),
							getAccountNumber(),
							getAccountName(),
							getSymbol(),
							getDescription(),
//							getQuantity(),
//							getLastPrice(),
//							getLastPriceChange(),
							getCurrentValue(),
//							getTodaysGainLossDollar(),
//							getTodaysGainLossPercent(),
//							getTotalGainLossDollar(),
//							getTotalGainLossPercent(),
//							getPercentOfAccount(),
//							getCostBasisTotal(),
//							getAverageCostBasis(),
//							getType(),
							getTaxableType(),
							getFundOwner());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("PortfolioPositionsData{");
		sb.append("dateDownloaded=").append(dateDownloaded);
		sb.append(", accountNumber='").append(accountNumber).append('\'');
		sb.append(", accountName='").append(accountName).append('\'');
		sb.append(", symbol='").append(symbol).append('\'');
		sb.append(", description='").append(description).append('\'');
//		sb.append(", quantity=").append(quantity);
//		sb.append(", lastPrice=").append(lastPrice);
//		sb.append(", lastPriceChange=").append(lastPriceChange);
		sb.append(", currentValue=").append(currentValue);
//		sb.append(", todaysGainLossDollar=").append(todaysGainLossDollar);
//		sb.append(", todaysGainLossPercent=").append(todaysGainLossPercent);
//		sb.append(", totalGainLossDollar=").append(totalGainLossDollar);
//		sb.append(", totalGainLossPercent=").append(totalGainLossPercent);
//		sb.append(", percentOfAccount=").append(percentOfAccount);
//		sb.append(", costBasisTotal=").append(costBasisTotal);
//		sb.append(", averageCostBasis=").append(averageCostBasis);
//		sb.append(", type='").append(type).append('\'');
		sb.append(", taxableType='").append(taxableType).append('\'');
		sb.append(", fundOwner='").append(fundOwner).append('\'');
		sb.append('}');

		return sb.toString();
	}


	//members
	//originally from CSV file
	private final Instant dateDownloaded;
	private final String accountNumber;
	private final String accountName;
	private final String symbol;
	private final String description;
//	private final Double quantity;
//	private final Double lastPrice;
//	private final Double lastPriceChange;
	private final Double currentValue;
//	private final Double todaysGainLossDollar;
//	private final Double todaysGainLossPercent;
//	private final Double totalGainLossDollar;
//	private final Double totalGainLossPercent;
//	private final Double percentOfAccount;
//	private final Double costBasisTotal;
//	private final Double averageCostBasis;
//	private final String type;

	//calculated
	private final FundsEnum.TaxableType taxableType;
	private final FundsEnum.FundOwner fundOwner;
	private final FundsMetaData fundsMetaData;

	public static final Predicate<PortfolioPositionsData> rothAccounts = (p -> p.getTaxableType() == FundsEnum.TaxableType.ROTH);
	public static final Predicate<PortfolioPositionsData> traditionalAccounts = (p -> p.getTaxableType() == FundsEnum.TaxableType.Traditional);
	public static final Predicate<PortfolioPositionsData> allAccounts = p -> true;
}
