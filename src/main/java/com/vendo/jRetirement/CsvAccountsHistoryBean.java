//CsvAccountsHistoryBean.java -

/* Yuck! Three different (as of 01/26) Headers for this file:
search /q account*csv run date | sorti/u
Run Date,Account,Account Number,Action,Symbol,Description,Type,Exchange Quantity,Exchange Currency,Currency,Price,Quantity,Exchange Rate,Commission,Fees,Accrued Interest,Amount,Settlement Date
Run Date,Account,Account Number,Action,Symbol,Description,Type,Exchange Quantity,Exchange Currency,Quantity,Currency,Price,Exchange Rate,Commission,Fees,Accrued Interest,Amount,Settlement Date
Run Date,Account,               Action,Symbol,Description,Type,Exchange Quantity,Exchange Currency,Quantity,Currency,Price,Exchange Rate,Commission,Fees,Accrued Interest,Amount,Settlement Date

/* This is how IRA distributions are presented (scrubbed):
Example command to find them (for 2025)
search /q /o Accounts_History*csv TAX DISTR | grep 2025 | sorti /u

Accounts_History_2024-Q4.csv
10/01/2024,"Rollover IRA" 242000002," STATE TAX W/H MA STAT WTH (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-2000,
10/01/2024,"Rollover IRA" 242000002," FED TAX W/H FEDERAL TAX WITHHELD (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-4000,
10/01/2024,"Rollover IRA" 242000002," NORMAL DISTR PARTIAL VS X64-835730-1 CASH (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-34000,

Accounts_History_2025-Q1.csv
04/02/2025,"Traditional IRA","239000009"," STATE TAX W/H MA STAT WTH ED68394242 /WEB (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-1000,
04/02/2025,"Traditional IRA","239000009"," FED TAX W/H RET FED WTH ED68394242 /WEB (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-3000,
04/02/2025,"Traditional IRA","239000009"," NORMAL DISTR PARTIAL ED68394242 /WEB (Cash)", ," No Description",Cash,0,,0.000,USD,,0,,,,-16000,

Accounts_History_2025-Q3.csv
Run Date,Account,Account Number,Action,Symbol,Description,Type,Exchange Quantity,Exchange Currency,Quantity,Currency,Price,Exchange Rate,Commission,Fees,Accrued Interest,Amount,Settlement Date
09/02/2025,"ROTH IRA","239000003","NORMAL DISTRIBUTION VS X64-835730-1 RESIDUAL TFR (Cash)",,"No Description",Cash,0,,0.000,USD,,0,,,,-735.12,
08/26/2025,"ROTH IRA","239000001","NORMAL DISTR PARTIAL VS X64-835730-1 CASH (Cash)",,"No Description",Cash,0,,0.000,USD,,0,,,,-190000,
08/25/2025,"ROTH IRA","239000003","NORMAL DISTRIBUTION VS X64-835730-1 CASH (Cash)",,"No Description",Cash,0,,0.000,USD,,0,,,,-270854.35,
08/07/2025,"Rollover IRA","242000002","STATE TAX W/H MA STAT WTH (Cash)",,"No Description",Cash,0,,0.000,USD,,0,,,,-2500,
08/07/2025,"Rollover IRA","242000002","FED TAX W/H FEDERAL TAX WITHHELD (Cash)",,"No Description",Cash,0,,0.000,USD,,0,,,,-7500,
08/07/2025,"Rollover IRA","242000002","NORMAL DISTR PARTIAL VS X64-835730-1 CASH (Cash)",,"No Description",Cash,0,,0.000,USD,,0,,,,-40000,
*/

package com.vendo.jRetirement;

import com.opencsv.bean.CsvBindByName;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CsvAccountsHistoryBean extends CsvBaseBean {
    ///////////////////////////////////////////////////////////////////////////
    public CsvAccountsHistoryBean() {
    }

    ///////////////////////////////////////////////////////////////////////////
    //Prior to 2025 or so, the Account field includes the Account Number, and the Account Number field is empty
    //For examples (scrubbed and wrapped in curly braces): Account = {Rollover IRA" 242000002}, {Individual - TOD" X64000002}
    protected void handleCombinedAccountAndAccountNumber(String combinedString) {
        final Pattern pattern = Pattern.compile("^([^\"]+)\" (\\w+)"); //Note there is only one double quote in the combined string

        if (StringUtils.isBlank(accountNumber)) {
            Matcher matcher = pattern.matcher(combinedString.trim());
            if (matcher.find()) {
                accountName = matcher.group(1);
                accountNumber = matcher.group(2);
//            } else {
//TODO - print message
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //The 401(k) records are not unique enough in the fields I chose for the database PRIMARY KEY, so make them unique
    protected String handle401kActionField(String action) {
        if ("FIS 401(K) PLAN".equals(accountName) && accountNumber.startsWith("8591")) {
            return action + " : " + description; //HACK
        }

        return action;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundsEnum.Activity getActivity() { //calculate activity from action field
        FundsEnum.Activity activity = FundsEnum.Activity.Unspecified;

        String action = getAction();

        if (action.contains("CONTR")) {
            activity = FundsEnum.Activity.Contribution;

        } else if (action.contains("DISTR") || action.contains("TAX")) {
            activity = FundsEnum.Activity.Distribution;

        } else if (action.contains("REDEMPTION FROM CORE ACCOUNT")) {
            activity = FundsEnum.Activity.Redemption;
        }

        return activity;
    }

    ///////////////////////////////////////////////////////////////////////////
    public Instant getRunDate() {
        return parseDateMmDdYyyy(runDate);
    }
    public String getRunDateString() {
        return runDate;
    }
    public void setRunDate(String runDate) {
        this.runDate = runDate.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAccountName() {
        handleCombinedAccountAndAccountNumber(accountName);
        return accountName;
    }
    public void setAccountName(String accountName) { //Note: the setter used by the CSV layer is based on the Java name (accountName), not the CSV file name (account)
        this.accountName = stripTrailingCopyrightChar(accountName.trim());
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAccountNumber() {
        handleCombinedAccountAndAccountNumber(accountName);
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAction() {
        return handle401kActionField(action);
    }
    public void setAction(String action) {
        this.action = action.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getExchangeQuantity() {
        return exchangeQuantity;
    }
    public void setExchangeQuantity(String exchangeQuantity) {
        this.exchangeQuantity = exchangeQuantity.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getExchangeCurrency() {
        return exchangeCurrency;
    }
    public void setExchangeCurrency(String exchangeCurrency) {
        this.exchangeCurrency = exchangeCurrency.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getQuantity() {
        return quantity;
    }
    public void setQuantity(String quantity) {
        this.quantity = quantity.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getPrice() {
        return price;
    }
    public void setPrice(String price) {
        this.price = price.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getExchangeRate() {
        return exchangeRate;
    }
    public void setExchangeRate(String exchangeRate) {
        this.exchangeRate = exchangeRate.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getCommission() {
        return parseNumberAmount(commission);
    }
    public String getCommissionString() {
        return commission;
    }
    public void setCommission(String commission) {
        this.commission = commission.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getFees() {
        return parseNumberAmount(fees);
    }
    public String getFeesString() {
        return fees;
    }
    public void setFees(String fees) {
        this.fees = fees.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAccruedInterest() {
        return accruedInterest;
    }
    public void setAccruedInterest(String accruedInterest) {
        this.accruedInterest = accruedInterest.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getAmount() {
        return parseNumberAmount(amount);
    }
    public String getAmountString() {
        return amount;
    }
    public void setAmount(String amount) {
        this.amount = amount.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public Instant getSettlementDate() {
        return parseDateMmDdYyyy(settlementDate);
    }
    public String getSettlementDateString() {
        return settlementDate;
    }
    public void setSettlementDate(String settlementDate) {
        this.settlementDate = settlementDate.trim();
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
        CsvAccountsHistoryBean that = (CsvAccountsHistoryBean) o;
        return getRunDate().equals(that.getRunDate()) &&
                getAccountName().equals(that.getAccountName()) &&
                Objects.equals(getAccountNumber(), that.getAccountNumber()) &&
                getAction().equals(that.getAction()) &&
                getSymbol().equals(that.getSymbol()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
//                Objects.equals(getType(), that.getType()) &&
//                Objects.equals(getExchangeQuantity(), that.getExchangeQuantity()) &&
//                Objects.equals(getExchangeCurrency(), that.getExchangeCurrency()) &&
//                Objects.equals(getQuantity(), that.getQuantity()) &&
//                Objects.equals(getCurrency(), that.getCurrency()) &&
//                Objects.equals(getPrice(), that.getPrice()) &&
//                Objects.equals(getExchangeRate(), that.getExchangeRate()) &&
                Objects.equals(getCommission(), that.getCommission()) &&
                Objects.equals(getFees(), that.getFees()) &&
//                Objects.equals(getAccruedInterest(), that.getAccruedInterest()) &&
                getAmount().equals(that.getAmount()) &&
                getSettlementDate().equals(that.getSettlementDate());
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode() {
        return Objects.hash(getRunDate(),
                            getAccountName(),
                            getAccountNumber(),
                            getAction(),
                            getSymbol(),
                            getDescription(),
//                            getType(),
//                            getExchangeQuantity(),
//                            getExchangeCurrency(),
//                            getQuantity(),
//                            getCurrency(),
//                            getPrice(),
//                            getExchangeRate(),
                            getCommission(),
                            getFees(),
//                            getAccruedInterest(),
                            getAmount(),
                            getSettlementDate());
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CsvAccountsHistoryBean{");
        sb.append("runDate='").append(getRunDate()).append('\'');
        sb.append(", accountName='").append(getAccountName()).append('\'');
        sb.append(", accountNumber='").append(getAccountNumber()).append('\'');
        sb.append(", action='").append(getAction()).append('\'');
        sb.append(", symbol='").append(getSymbol()).append('\'');
        sb.append(", description='").append(getDescription()).append('\'');
//        sb.append(", type='").append(typeUnused).append('\'');
//        sb.append(", exchangeQuantity='").append(exchangeQuantity).append('\'');
//        sb.append(", exchangeCurrency='").append(exchangeCurrency).append('\'');
//        sb.append(", quantity='").append(quantity).append('\'');
//        sb.append(", currency='").append(currency).append('\'');
//        sb.append(", price='").append(price).append('\'');
//        sb.append(", exchangeRate='").append(exchangeRate).append('\'');
        sb.append(", commission='").append(getCommission()).append('\'');
        sb.append(", fees='").append(getFees()).append('\'');
//        sb.append(", accruedInterest='").append(accruedInterest).append('\'');
        sb.append(", amount='").append(getAmount()).append('\'');
        sb.append(", settlementDate='").append(getSettlementDate()).append('\'');
        sb.append('}');
        return sb.toString();
    }


    //private members
    @CsvBindByName (column = "Run Date")            private String runDate;
    @CsvBindByName (column = "Account")             private String accountName; //Note: this is "Account" in the CSV file, but accountName everywhere else (java, DB)
    @CsvBindByName (column = "Account Number")      private String accountNumber;
    @CsvBindByName (column = "Action")              private String action;
    @CsvBindByName (column = "Exchange Quantity")   private String exchangeQuantity;
    @CsvBindByName (column = "Exchange Currency")   private String exchangeCurrency;
    @CsvBindByName (column = "Quantity")            private String quantity;
    @CsvBindByName (column = "Currency")            private String currency;
    @CsvBindByName (column = "Price")               private String price;
    @CsvBindByName (column = "Exchange Rate")       private String exchangeRate;
    @CsvBindByName (column = "Commission")          private String commission;
    @CsvBindByName (column = "Fees")                private String fees;
    @CsvBindByName (column = "Accrued Interest")    private String accruedInterest;
    @CsvBindByName (column = "Amount")              private String amount;
    @CsvBindByName (column = "Settlement Date")     private String settlementDate;
}
