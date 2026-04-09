//AccountsHistoryData.java

package com.vendo.jRetirement;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

public class AccountsHistoryData extends RetirementBaseData implements Comparator<AccountsHistoryData> {
	///////////////////////////////////////////////////////////////////////////
	public AccountsHistoryData(Instant runDate,
							   String accountName,
							   String accountNumber,
							   String action,
							   String symbol,
							   String description,
							   Double commission,
							   Double fees,
							   Double amount,
							   Instant settlementDate,
							   FundsEnum.Activity activity) {
		super(accountName, symbol, description);
		this.runDate = runDate;
		this.accountNumber = accountNumber;
		this.action = action;
		this.commission = commission;
		this.fees = fees;
		this.amount = amount;
		this.settlementDate = settlementDate;
		this.activity = activity;
	}

	///////////////////////////////////////////////////////////////////////////
	public AccountsHistoryData(CsvAccountsHistoryBean bean) {
		super(bean.getAccountName(), bean.getSymbol(), bean.getDescription());
		this.runDate = bean.getRunDate();
		this.accountNumber = bean.getAccountNumber();
		this.action = bean.getAction();
		this.commission = bean.getCommission();
		this.fees = bean.getFees();
		this.amount = bean.getAmount();
		this.settlementDate = bean.getSettlementDate();
		this.activity = bean.getActivity();
	}

	///////////////////////////////////////////////////////////////////////////
	//ctor to use for sorting/Comparator
	public AccountsHistoryData() {
		super("dummy for sorting", null, null);
		this.runDate = RetirementDao.AllDates;
		this.accountNumber = null;
		this.action = null;
		this.commission = null;
		this.fees = null;
		this.amount = null;
		this.settlementDate = null;
		this.activity = null;
	}

	///////////////////////////////////////////////////////////////////////////
	public Instant getRunDate() {
		return runDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getAccountNumber() {
		return accountNumber;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getAction() {
		return action;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getCommission() {
		return commission;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getFees() {
		return fees;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getAmount() {
		return amount;
	}

	///////////////////////////////////////////////////////////////////////////
	public Instant getSettlementDate() {
		return settlementDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public FundsEnum.Activity getActivity() {
		return activity;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int compare(AccountsHistoryData o1, AccountsHistoryData o2) {
		throw new RuntimeException ("AccountsHistoryData.compare: not implemented"); //TODO - implement
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
		AccountsHistoryData that = (AccountsHistoryData) o;
		return getRunDate().equals(that.getRunDate()) &&
				getAccountName().equals(that.getAccountName()) &&
				getAccountNumber().equals(that.getAccountNumber()) &&
				getAction().equals(that.getAction()) &&
				getSymbol().equals(that.getSymbol()) &&
				Objects.equals(getDescription(), that.getDescription()) &&
				Objects.equals(getCommission(), that.getCommission()) &&
				Objects.equals(getFees(), that.getFees()) &&
				getAmount().equals(that.getAmount()) &&
				Objects.equals(getSettlementDate(), that.getSettlementDate()) &&
				Objects.equals(getActivity(), that.getActivity());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() { //includes fields from the base class
		return Objects.hash(getRunDate(),
							getAccountName(),
							getAccountNumber(),
							getAction(),
							getSymbol(),
							getDescription(),
							getCommission(),
							getFees(),
							getAmount(),
							getSettlementDate(),
							getActivity());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() { //includes fields from the base class
		final StringBuffer sb = new StringBuffer("AccountsHistoryData{");
		sb.append("runDate=").append(getRunDate());
		sb.append(", accountName='").append(getAccountName()).append('\'');
		sb.append(", accountNumber='").append(getAccountNumber()).append('\'');
		sb.append(", action='").append(getAction()).append('\'');
		sb.append(", symbol='").append(getSymbol()).append('\'');
		sb.append(", description='").append(getDescription()).append('\'');
		sb.append(", commission=").append(getCommission());
		sb.append(", fees=").append(getFees());
		sb.append(", amount=").append(getAmount());
		sb.append(", settlementDate=").append(getSettlementDate());
		sb.append(", activity=").append(getActivity());
		sb.append('}');
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	public String toStringDistributionDetail() {
		final StringBuffer sb = new StringBuffer();
		sb.append(dateTimeFormatterMdy.format(getRunDate())).append(" ");
		sb.append(getAccountName()).append(" ");
		sb.append(dollarFormat0.format(getAmount()));
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	public String toStringRedemptionDetail() {
		return toStringDistributionDetail(); //for now, the same
	}

	///////////////////////////////////////////////////////////////////////////
	public String toStringContributionDetail() {
		return toStringDistributionDetail(); //for now, the same
	}

	//private members
	//originally from CSV file
	private final Instant runDate;
	private final String accountNumber;
	private final String action;
	private final Double commission;
	private final Double fees;
	private final Double amount;
	private final Instant settlementDate;

	//calculated
	private final FundsEnum.Activity activity;
}
