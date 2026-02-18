//AccountsHistoryData.java

package com.vendo.jRetirement;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

public class AccountsHistoryData extends RetirementBaseData implements Comparator<AccountsHistoryData> {
	///////////////////////////////////////////////////////////////////////////
	public AccountsHistoryData(Instant runDate,
							   String account,
							   String accountNumber,
							   String action,
							   String symbol,
							   String description,
							   Double commission,
							   Double fees,
							   Double amount,
							   Instant settlementDate) {
		super(symbol, description);
		this.runDate = runDate;
		this.account = account;
		this.accountNumber = accountNumber;
		this.action = action;
		this.commission = commission;
		this.fees = fees;
		this.amount = amount;
		this.settlementDate = settlementDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public AccountsHistoryData(CsvAccountsHistoryBean bean) {
		super(bean.getSymbol(), bean.getDescription());
		this.runDate = bean.getRunDate();
		this.account = bean.getAccount();
		this.accountNumber = bean.getAccountNumber();
		this.action = bean.getAction();
		this.commission = bean.getCommission();
		this.fees = bean.getFees();
		this.amount = bean.getAmount();
		this.settlementDate = bean.getSettlementDate();
	}

	///////////////////////////////////////////////////////////////////////////
	//ctor to use for sorting/Comparator
	public AccountsHistoryData() {
		super(null, null);
		this.runDate = RetirementDao.AllDates;
		this.account = "dummy for sorting";
		this.accountNumber = null;
		this.action = null;
		this.commission = null;
		this.fees = null;
		this.amount = null;
		this.settlementDate = null;
	}

	///////////////////////////////////////////////////////////////////////////
	public Instant getRunDate() {
		return runDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getAccount() {
		return account;
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
				getAccount().equals(that.getAccount()) &&
				getAccountNumber().equals(that.getAccountNumber()) &&
				getAction().equals(that.getAction()) &&
				getSymbol().equals(that.getSymbol()) &&
				Objects.equals(getDescription(), that.getDescription()) &&
				Objects.equals(getCommission(), that.getCommission()) &&
				Objects.equals(getFees(), that.getFees()) &&
				getAmount().equals(that.getAmount()) &&
				Objects.equals(getSettlementDate(), that.getSettlementDate());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() { //includes fields from the base class
		return Objects.hash(getRunDate(),
							getAccount(),
							getAccountNumber(),
							getAction(),
							getSymbol(),
							getDescription(),
							getCommission(),
							getFees(),
							getAmount(),
							getSettlementDate());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() { //includes fields from the base class
		final StringBuffer sb = new StringBuffer("AccountsHistoryData{");
		sb.append("runDate=").append(getRunDate());
		sb.append(", account='").append(getAccount()).append('\'');
		sb.append(", accountNumber='").append(getAccountNumber()).append('\'');
		sb.append(", action='").append(getAction()).append('\'');
		sb.append(", symbol='").append(getSymbol()).append('\'');
		sb.append(", description='").append(getDescription()).append('\'');
		sb.append(", commission=").append(getCommission());
		sb.append(", fees=").append(getFees());
		sb.append(", amount=").append(getAmount());
		sb.append(", settlementDate=").append(getSettlementDate());
		sb.append('}');
		return sb.toString();
	}

	///////////////////////////////////////////////////////////////////////////
	public String toStringDistributionDetail() {
		final StringBuffer sb = new StringBuffer();
		sb.append(dateTimeFormatterMdy.format(getRunDate())).append(" ");
		sb.append(getAccount()).append(" ");
		sb.append(dollarFormat0.format(getAmount()));
		return sb.toString();
	}

	//private members
	//originally from CSV file
	private final Instant runDate;
	private final String account;
	private final String accountNumber;
	private final String action;
	private final Double commission;
	private final Double fees;
	private final Double amount;
	private final Instant settlementDate;
}
