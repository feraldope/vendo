//CsvPortfolioPositionsBean.java -
//header from CSV file:
//Account Number,Account Name,Symbol,Description,Quantity,Last Price,Last Price Change,Current Value,Today's Gain/Loss Dollar,Today's Gain/Loss Percent,Total Gain/Loss Dollar,Total Gain/Loss Percent,Percent Of Account,Cost Basis Total,Average Cost Basis,Type

/* example scrubbed data
Account Number,Account Name,Symbol,Description,Quantity,Last Price,Last Price Change,Current Value,Today's Gain/Loss Dollar,Today's Gain/Loss Percent,Total Gain/Loss Dollar,Total Gain/Loss Percent,Percent Of Account,Cost Basis Total,Average Cost Basis,Type
X64000000,Individual - TOD,FCASH**,HELD IN FCASH,,,,$55.87,,,,,100.00%,,,Cash,
239000001,ROTH IRA,SPAXX**,HELD IN MONEY MARKET,,,,$0.60,,,,,0.00%,,,Cash,
239000001,ROTH IRA,SCHD,SCHWAB US DIVIDEND EQUITY ETF,565.896,$29.6099,+$0.3099,$16756.12,+$175.37,+1.05%,+$1449.16,+9.46%,6.02%,$15306.96,$27.05,Cash,
239000001,ROTH IRA,VYM,VANGUARD WHITEHALL FDS HIGH DIV YLD,339.155,$149.54,+$0.85,$50717.23,+$288.28,+0.57%,+$16309.63,+47.40%,18.24%,$34407.60,$101.45,Cash,
*/


package com.vendo.jRetirement;

import com.opencsv.bean.CsvBindByName;

import java.time.Instant;
import java.util.Objects;


public class CsvPortfolioPositionsBean extends CsvBaseBean {
    ///////////////////////////////////////////////////////////////////////////
    public CsvPortfolioPositionsBean() {
    }

    ///////////////////////////////////////////////////////////////////////////
    public Instant getDateDownloaded() {
        return dateDownloaded;
    }
    public void setDateDownloaded(Instant dateDownloaded) {
        this.dateDownloaded = dateDownloaded;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = stripTrailingCopyrightChar(accountName);
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getQuantity() {
        return quantity;
    }
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getLastPrice() {
        return lastPrice;
    }
    public void setLastPrice(String lastPrice) {
        this.lastPrice = lastPrice;
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getLastPriceChange() {
        return parseNumberAmount(lastPriceChange);
    }
    public String getLastPriceChangeString() {
        return lastPriceChange;
    }
    public void setLastPriceChange(String lastPriceChange) {
        this.lastPriceChange = lastPriceChange;
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getCurrentValue() {
        return parseNumberAmount(currentValue);
    }
    public String getCurrentValueString() {
        return currentValue;
    }
    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getTodaysGainLossDollar() {
        return todaysGainLossDollar;
    }
    public void setTodaysGainLossDollar(String todaysGainLossDollar) {
        this.todaysGainLossDollar = todaysGainLossDollar;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getTodaysGainLossPercent() {
        return todaysGainLossPercent;
    }
    public void setTodaysGainLossPercent(String todaysGainLossPercent) {
        this.todaysGainLossPercent = todaysGainLossPercent;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getTotalGainLossDollar() {
        return totalGainLossDollar;
    }
    public void setTotalGainLossDollar(String totalGainLossDollar) {
        this.totalGainLossDollar = totalGainLossDollar;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getTotalGainLossPercent() {
        return totalGainLossPercent;
    }
    public void setTotalGainLossPercent(String totalGainLossPercent) {
        this.totalGainLossPercent = totalGainLossPercent;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getPercentOfAccount() {
        return percentOfAccount;
    }
    public void setPercentOfAccount(String percentOfAccount) {
        this.percentOfAccount = percentOfAccount;
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getCostBasis() {
        if ("--".equals(costBasis) || "n/a".equalsIgnoreCase(costBasis)) {
            return 0.; //TODO - probably not correct
        }

        return parseNumberAmount(costBasis);
    }
    public String getCostBasisString() {
        return costBasis;
    }
    public void setCostBasis(String costBasis) {
        this.costBasis = costBasis;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAverageCostBasis() {
        return averageCostBasis;
    }
    public void setAverageCostBasis(String averageCostBasis) {
        this.averageCostBasis = averageCostBasis;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundsEnum.FundOwner getFundOwner() { //calculate fundOwner from accountNumber field
        FundsEnum.FundOwner fundOwner = FundsEnum.FundOwner.unknown;

        String accountNumber = getAccountNumber();
        if (accountNumber.matches("23\\d+11") || //hardcoded values are scrubbed
                accountNumber.matches("24\\d+42") ||
                accountNumber.matches("24\\d+36") ||
                accountNumber.matches("8\\d+8") ||
                accountNumber.matches("X\\d+0") ||
                accountNumber.matches("16\\d+67")) {
            fundOwner = FundsEnum.FundOwner.dr;

        } else if (accountNumber.matches("23\\d+9[39]") || //hardcoded values are scrubbed
                accountNumber.matches("2BN\\d+55") ||
                accountNumber.matches("2BN\\d+63")) {
            fundOwner = FundsEnum.FundOwner.mr;

        } else {
            System.out.println("Warning: getFundOwner: unable to determine fundOwner for record: " + this);
        }

        return fundOwner;
    }

    ///////////////////////////////////////////////////////////////////////////
//TODO - this *could* be moved to base class (but one class uses getAccountName and the other uses getAccount
    public FundsEnum.TaxableType getTaxableType() {
        FundsEnum.TaxableType taxableType = FundsEnum.TaxableType.Unspecified;

        switch(getAccountName()) {
            case "Fidelity Crypto":
            case "Individual - TOD":
                taxableType = FundsEnum.TaxableType.Taxable;
                break;

            case "Rollover IRA":
            case "Traditional IRA":
                taxableType = FundsEnum.TaxableType.TaxDeferred;
                break;

            case "FIS 401(K) PLAN":
            case "Health Savings Account":
            case "ROTH IRA":
                taxableType = FundsEnum.TaxableType.TaxFree;
                break;

            default: //fall through
        }

        if (FundsEnum.TaxableType.Unspecified == taxableType) {
            throw new RuntimeException ("CsvPortfolioPositionsBean.getTaxableType: unhandled accountName: '" + getAccountName() + "'");
        }

        return taxableType;
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        return "CsvPortfolioPositionsBean{" +
                "date='" + getDateDownloaded() + '\'' +
                ", acctNum='" + getAccountNumber() + '\'' +
                ", acctName='" + getAccountName() + '\'' +
                ", symbol='" + getSymbol() + '\'' +
                ", desc='" + getDescription() + '\'' +
//                ", Quantity='" + getQuantity() + '\'' +
//                ", LastPrice='" + getLastPrice + '\'' +
//                ", LastPriceChange='" + getLastPriceChange + '\'' +
                ", value='" + getCurrentValueString() + '\'' +
//                ", TodaysGainLossDollar='" + getTodaysGainLossDollar + '\'' +
//                ", TodaysGainLossPercent='" + getTodaysGainLossPercent + '\'' +
//                ", TotalGainLossDollar='" + getTotalGainLossDollar + '\'' +
//                ", TotalGainLossPercent='" + getTotalGainLossPercent + '\'' +
//                ", PercentOfAccount='" + getPercentOfAccount() + '\'' +
                ", costBasis='" + getCostBasisString() + '\'' +
//                ", AverageCostBasis='" + getAverageCostBasis() + '\'' +
//                ", Type='" + getType() + '\'' +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        CsvPortfolioPositionsBean that = (CsvPortfolioPositionsBean) o;
        return Objects.equals(getDateDownloaded(), that.getDateDownloaded()) &&
                Objects.equals(getAccountNumber(), that.getAccountNumber()) &&
                Objects.equals(getAccountName(), that.getAccountName()) &&
                Objects.equals(getSymbol(), that.getSymbol()) &&
                Objects.equals(getDescription(), that.getDescription()) &&
//                Objects.equals(getQuantity(), that.getQuantity()) &&
//                Objects.equals(getLastPrice(), that.getLastPrice()) &&
//                Objects.equals(getLastPriceChange(), that.getLastPriceChange()) &&
                Objects.equals(getCurrentValue(), that.getCurrentValue()) &&
//                Objects.equals(getTodaysGainLossDollar(), that.getTodaysGainLossDollar()) &&
//                Objects.equals(getTodaysGainLossPercent(), that.getTodaysGainLossPercent()) &&
//                Objects.equals(getTotalGainLossDollar(), that.getTotalGainLossDollar()) &&
//                Objects.equals(getTotalGainLossPercent(), that.getTotalGainLossPercent()) &&
//                Objects.equals(getPercentOfAccount(), that.getPercentOfAccount()) &&
                Objects.equals(getCostBasis(), that.getCostBasis());
//                Objects.equals(getAverageCostBasis(), that.getAverageCostBasis()) &&
//                Objects.equals(getType(), that.getType());
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public int hashCode() {
        return Objects.hash(
                getDateDownloaded(),
                getAccountNumber(),
                getAccountName(),
                getSymbol(),
                getDescription(),
//                getQuantity(),
//                getLastPrice(),
//                getLastPriceChange(),
                getCurrentValue(),
//                getTodaysGainLossDollar(),
//                getTodaysGainLossPercent(),
//                getTotalGainLossDollar(),
//                getTotalGainLossPercent(),
//                getPercentOfAccount(),
                getCostBasis());
//                getAverageCostBasis(),
//                getType())
    }

    //private members
    Instant dateDownloaded;
    @CsvBindByName (column = "Account Number")            private String accountNumber;
    @CsvBindByName (column = "Account Name")              private String accountName;
    @CsvBindByName (column = "Quantity")                  private String quantity;
    @CsvBindByName (column = "Last Price")                private String lastPrice;
    @CsvBindByName (column = "Last Price Change")         private String lastPriceChange;
    @CsvBindByName (column = "Current Value")             private String currentValue;
    @CsvBindByName (column = "Today's Gain/Loss Dollar")  private String todaysGainLossDollar;
    @CsvBindByName (column = "Today's Gain/Loss Percent") private String todaysGainLossPercent;
    @CsvBindByName (column = "Total Gain/Loss Dollar")    private String totalGainLossDollar;
    @CsvBindByName (column = "Total Gain/Loss Percent")   private String totalGainLossPercent;
    @CsvBindByName (column = "Percent Of Account")        private String percentOfAccount;
    @CsvBindByName (column = "Cost Basis Total")          private String costBasis;
    @CsvBindByName (column = "Average Cost Basis")        private String averageCostBasis;
}
