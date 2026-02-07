//AccountsHistoryData.java

package com.vendo.jRetirement;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

public class AccountsHistoryData implements Comparator<AccountsHistoryData> {
	///////////////////////////////////////////////////////////////////////////
	public AccountsHistoryData(Instant runDate,
							   String account,
							   String accountNumber,
							   String action,
							   String symbol,
							   String description,
//							   String typeUnused,
//							   Double exchangeQuantity,
//							   String exchangeCurrency,
//							   Double quantity,
//							   String currency,
//							   Double price,
//							   Double exchangeRate,
							   Double commission,
							   Double fees,
//							   Double accruedInterest,
							   Double amount,
							   Instant settlementDate) {
		this.runDate = runDate;
		this.account = account;
		this.accountNumber = accountNumber;
		this.action = action;
		this.symbol = symbol;
		this.description = description;
//		this.typeUnused = typeUnused;
//		this.exchangeQuantity = exchangeQuantity;
//		this.exchangeCurrency = exchangeCurrency;
//		this.quantity = quantity;
//		this.currency = currency;
//		this.price = price;
//		this.exchangeRate = exchangeRate;
		this.commission = commission;
		this.fees = fees;
//		this.accruedInterest = accruedInterest;
		this.amount = amount;
		this.settlementDate = settlementDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public AccountsHistoryData(CsvAccountsHistoryBean bean) {
		this.runDate = bean.getRunDate();
		this.account = bean.getAccount();
		this.accountNumber = bean.getAccountNumber();
		this.action = bean.getAction();
		this.symbol = bean.getSymbol();
		this.description = bean.getDescription();
		this.commission = bean.getCommission();
		this.fees = bean.getFees();
		this.amount = bean.getAmount();
		this.settlementDate = bean.getSettlementDate();
	}

	///////////////////////////////////////////////////////////////////////////
	//ctor to use for sorting/Comparator
	public AccountsHistoryData() {
		this.runDate = RetirementDao.AllDates;
		this.account = "dummy for sorting";
		this.accountNumber = null;
		this.action = null;
		this.symbol = null;
		this.description = null;
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
	public String getSymbol() {
		return symbol;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getDescription() {
		return description;
	}

	///////////////////////////////////////////////////////////////////////////
//	public String getTypeUnused() {
//		return typeUnused;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getExchangeQuantity() {
//		return exchangeQuantity;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public String getExchangeCurrency() {
//		return exchangeCurrency;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getQuantity() {
//		return quantity;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public String getCurrency() {
//		return currency;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getPrice() {
//		return price;
//	}

	///////////////////////////////////////////////////////////////////////////
//	public Double getExchangeRate() {
//		return exchangeRate;
//	}

	///////////////////////////////////////////////////////////////////////////
	public Double getCommission() {
		return commission;
	}

	///////////////////////////////////////////////////////////////////////////
	public Double getFees() {
		return fees;
	}
	///////////////////////////////////////////////////////////////////////////
//	public Double getAccruedInterest() {
//		return accruedInterest;
//	}

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
	public boolean equals(Object o) {
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
//				Objects.equals(getTypeUnused(), that.getTypeUnused()) &&
//				Objects.equals(getExchangeQuantity(), that.getExchangeQuantity()) &&
//				Objects.equals(getExchangeCurrency(), that.getExchangeCurrency()) &&
//				Objects.equals(getQuantity(), that.getQuantity()) &&
//				Objects.equals(getCurrency(), that.getCurrency()) &&
//				Objects.equals(getPrice(), that.getPrice()) &&
//				Objects.equals(getExchangeRate(), that.getExchangeRate()) &&
				Objects.equals(getCommission(), that.getCommission()) &&
				Objects.equals(getFees(), that.getFees()) &&
//				Objects.equals(getAccruedInterest(), that.getAccruedInterest()) &&
				getAmount().equals(that.getAmount()) &&
				Objects.equals(getSettlementDate(), that.getSettlementDate());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		return Objects.hash(getRunDate(),
							getAccount(),
							getAccountNumber(),
							getAction(),
							getSymbol(),
							getDescription(),
//							getTypeUnused(),
//							getExchangeQuantity(),
//							getExchangeCurrency(),
//							getQuantity(),
//							getCurrency(),
//							getPrice(),
//							getExchangeRate(),
							getCommission(),
							getFees(),
//							getAccruedInterest(),
							getAmount(),
							getSettlementDate());
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("AccountsHistoryData{");
		sb.append("runDate=").append(runDate);
		sb.append(", account='").append(account).append('\'');
		sb.append(", accountNumber='").append(accountNumber).append('\'');
		sb.append(", action='").append(action).append('\'');
		sb.append(", symbol='").append(symbol).append('\'');
		sb.append(", description='").append(description).append('\'');
//		sb.append(", typeUnused='").append(typeUnused).append('\'');
//		sb.append(", exchangeQuantity=").append(exchangeQuantity);
//		sb.append(", exchangeCurrency='").append(exchangeCurrency).append('\'');
//		sb.append(", quantity=").append(quantity);
//		sb.append(", currency='").append(currency).append('\'');
//		sb.append(", price=").append(price);
//		sb.append(", exchangeRate=").append(exchangeRate);
		sb.append(", commission=").append(commission);
		sb.append(", fees=").append(fees);
//		sb.append(", accruedInterest=").append(accruedInterest);
		sb.append(", amount=").append(amount);
		sb.append(", settlementDate=").append(settlementDate);
		sb.append('}');
		return sb.toString();
	}

	//private members
	private final Instant runDate;
	private final String account;
	private final String accountNumber;
	private final String action;
	private final String symbol;
	private final String description;
//	private final String typeUnused; //seems to always be "Cash" in the CSV file
//	private final Double exchangeQuantity;
//	private final String exchangeCurrency;
//	private final Double quantity;
//	private final String currency;
//	private final Double price;
//	private final Double exchangeRate;
	private final Double commission;
	private final Double fees;
//	private final Double accruedInterest;
	private final Double amount;
	private final Instant settlementDate;
}
