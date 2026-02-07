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
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class CsvPortfolioPositionsBean {

    ///////////////////////////////////////////////////////////////////////////
    public CsvPortfolioPositionsBean() {
    }

    ///////////////////////////////////////////////////////////////////////////
    protected double parseNumberAmount(String stringValue) {
        if (StringUtils.isBlank(stringValue)) {
            return 0.;
        }

        return Double.parseDouble(stringValue.replaceFirst("\\$", ""));
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
        this.accountName = accountName;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        if (symbol.endsWith("**")) { //HACK - cleanup for example 'SPAXX**'
            symbol = symbol.substring(0, symbol.length() - 2);
        }

        this.symbol = symbol;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
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
    public Double getCostBasisTotal() {
        if ("--".equals(costBasisTotal) || "n/a".equalsIgnoreCase(costBasisTotal)) {
            return 0.;
        }
        return parseNumberAmount(costBasisTotal);
    }
    public String getCostBasisTotalString() {
        return costBasisTotal;
    }
    public void setCostBasisTotal(String costBasisTotal) {
        this.costBasisTotal = costBasisTotal;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getAverageCostBasis() {
        return averageCostBasis;
    }
    public void setAverageCostBasis(String averageCostBasis) {
        this.averageCostBasis = averageCostBasis;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getType() {
        return typeUnused;
    }
    public void setType(String type) {
        this.typeUnused = type;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundsEnum.FundOwner getFundOwner() {
        FundsEnum.FundOwner fundOwner = FundsEnum.FundOwner.unknown;

        String accountNumber = getAccountNumber();
        if (accountNumber.matches("23\\d+11") || accountNumber.matches("24\\d+42") || accountNumber.matches("8\\d+8") || accountNumber.matches("X\\d+0")) { //scrubbed
            fundOwner = FundsEnum.FundOwner.dr;
        } else if (accountNumber.matches("23\\d+9[39]")) { //scrubbed
            fundOwner = FundsEnum.FundOwner.mr;
        }

        return fundOwner;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundsEnum.TaxableType getTaxableType() {
        final List<String> RothAccountNames = new ArrayList<>(Arrays.asList("ROTH IRA", "FIS 401(K) PLAN"));

        return RothAccountNames.contains(getAccountName()) ? FundsEnum.TaxableType.ROTH : FundsEnum.TaxableType.Traditional;
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
//                ", costBasis='" + getCostBasisTotalString() + '\'' +
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
                Objects.equals(getCostBasisTotal(), that.getCostBasisTotal());
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
                getCurrentValue());
//                getTodaysGainLossDollar(),
//                getTodaysGainLossPercent(),
//                getTotalGainLossDollar(),
//                getTotalGainLossPercent(),
//                getPercentOfAccount(),
//                getCostBasisTotal(),
//                getAverageCostBasis(),
//                getType())
    }

    //private members
    Instant dateDownloaded;
    @CsvBindByName (column = "Account Number")            private String accountNumber;
    @CsvBindByName (column = "Account Name")              private String accountName;
    @CsvBindByName (column = "Symbol")                    private String symbol;
    @CsvBindByName (column = "Description")               private String description;
    @CsvBindByName (column = "Quantity")                  private String quantity;
    @CsvBindByName (column = "Last Price")                private String lastPrice;
    @CsvBindByName (column = "Last Price Change")         private String lastPriceChange;
    @CsvBindByName (column = "Current Value")             private String currentValue;
    @CsvBindByName (column = "Today's Gain/Loss Dollar")  private String todaysGainLossDollar;
    @CsvBindByName (column = "Today's Gain/Loss Percent") private String todaysGainLossPercent;
    @CsvBindByName (column = "Total Gain/Loss Dollar")    private String totalGainLossDollar;
    @CsvBindByName (column = "Total Gain/Loss Percent")   private String totalGainLossPercent;
    @CsvBindByName (column = "Percent Of Account")        private String percentOfAccount;
    @CsvBindByName (column = "Cost Basis Total")          private String costBasisTotal;
    @CsvBindByName (column = "Average Cost Basis")        private String averageCostBasis;
    @CsvBindByName (column = "Type")                      private String typeUnused; //seems to always be "Cash" in the CSV file
}
