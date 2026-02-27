//PortfolioPositionsData.java

package com.vendo.jRetirement;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public class PortfolioPositionsData extends RetirementBaseData implements Comparator<PortfolioPositionsData> {
	///////////////////////////////////////////////////////////////////////////
	public PortfolioPositionsData(Instant dateDownloaded,
								  String accountNumber,
								  String accountName,
								  String symbol,
								  String description,
								  Double currentValue,
								  Double costBasis,
								  FundsEnum.TaxableType taxableType,
								  FundsEnum.FundOwner fundOwner,
								  FundsMetaData fundsMetaData) {
		super(symbol, description);
		this.dateDownloaded = dateDownloaded;
		this.accountNumber = accountNumber;
		this.accountName = accountName;
		this.currentValue = currentValue;
		this.costBasis = costBasis;
		this.taxableType = taxableType;
		this.fundOwner = fundOwner;
		this.fundsMetaData = fundsMetaData;
	}

	///////////////////////////////////////////////////////////////////////////
	public PortfolioPositionsData(CsvPortfolioPositionsBean bean) {
		super(bean.getSymbol(), bean.getDescription());
		this.dateDownloaded = bean.getDateDownloaded();
		this.accountNumber = bean.getAccountNumber();
		this.accountName = bean.getAccountName();
		this.currentValue = bean.getCurrentValue();
		this.costBasis = bean.getCostBasis();
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
	public Double getCurrentValue() {
		return currentValue;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getCostBasis() {
		return costBasis;
	}

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
	public boolean equals(Object o) { //includes fields from the base class
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
				getCurrentValue().equals(that.getCurrentValue()) &&
				Objects.equals(getCostBasis(), that.getCostBasis()) &&
				Objects.equals(getTaxableType(), that.getTaxableType()) &&
				Objects.equals(getFundOwner(), that.getFundOwner());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() { //includes fields from the base class
		return Objects.hash(getDateDownloaded(),
							getAccountNumber(),
							getAccountName(),
							getSymbol(),
							getDescription(),
							getCurrentValue(),
							getCostBasis(),
							getTaxableType(),
							getFundOwner());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() { //includes fields from the base class
		final StringBuffer sb = new StringBuffer("PortfolioPositionsData{");
		sb.append("dateDownloaded=").append(getDateDownloaded());
		sb.append(", accountNumber='").append(getAccountNumber()).append('\'');
		sb.append(", accountName='").append(getAccountName()).append('\'');
		sb.append(", symbol='").append(getSymbol()).append('\'');
		sb.append(", description='").append(getDescription()).append('\'');
		sb.append(", currentValue=").append(getCurrentValue());
		sb.append(", costBasis=").append(getCostBasis());
		sb.append(", taxableType='").append(getTaxableType()).append('\'');
		sb.append(", fundOwner='").append(getFundOwner()).append('\'');
		sb.append('}');

		return sb.toString();
	}


	//private members
	//originally from CSV file
	private final Instant dateDownloaded;
	private final String accountNumber;
	private final String accountName;
	private final Double currentValue;
	private final Double costBasis;

	//calculated
	private final FundsEnum.TaxableType taxableType;
	private final FundsEnum.FundOwner fundOwner;
	private final FundsMetaData fundsMetaData;

	public static final Predicate<PortfolioPositionsData> taxFreeAccounts = (p -> p.getTaxableType() == FundsEnum.TaxableType.TaxFree);
	public static final Predicate<PortfolioPositionsData> taxDeferredlAccounts = (p -> p.getTaxableType() == FundsEnum.TaxableType.TaxDeferred);
	public static final Predicate<PortfolioPositionsData> taxableAccounts = (p -> p.getTaxableType() == FundsEnum.TaxableType.Taxable);
	public static final Predicate<PortfolioPositionsData> allAccounts = p -> true;
}
