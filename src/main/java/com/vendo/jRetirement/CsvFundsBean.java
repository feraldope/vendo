package com.vendo.jRetirement;

import com.opencsv.bean.CsvBindByName;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class CsvFundsBean {

    ///////////////////////////////////////////////////////////////////////////
    public CsvFundsBean() {
    }

    ///////////////////////////////////////////////////////////////////////////
    //ctor used by queryRecordsFromDatabase() to read records from database
    public CsvFundsBean(Instant dateDownloaded, String accountNumber, String accountName, String symbol, String description, Double currentValue, Double costBasisTotal) {
        this.dateDownloaded = dateDownloaded;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.symbol = symbol;
        this.description = description;
        this.currentValue = String.valueOf(currentValue);
        this.costBasisTotal = String.valueOf(costBasisTotal);
    }

    ///////////////////////////////////////////////////////////////////////////
    protected double parseNumberAmount(String stringValue) {
        if (StringUtils.isBlank(stringValue)) {
            return 0.;
        }

        return Double.parseDouble(stringValue.replaceFirst("\\$", ""));
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isRoth() {
        return RothAccountNames.contains(getAccountName());
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isPendingActivity() {
        return JRetirement.PendingActivityString.equals(getSymbol());
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
    public String getLastPriceChange() {
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
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        return "CsvFundsBean{" +
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
                ", costBasis='" + getCostBasisTotalString() + '\'' +
//                ", AverageCostBasis='" + getAverageCostBasis() + '\'' +
//                ", Type='" + getType() + '\'' +
                '}';
    }

    ///////////////////////////////////////////////////////////////////////////
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        CsvFundsBean that = (CsvFundsBean) o;
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
                getCurrentValue(),
//                getTodaysGainLossDollar(),
//                getTodaysGainLossPercent(),
//                getTotalGainLossDollar(),
//                getTotalGainLossPercent(),
//                getPercentOfAccount(),
                getCostBasisTotal());
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
    @CsvBindByName (column = "Type")                      private String type;

    private static final List<String> RothAccountNames = new ArrayList<>(Arrays.asList("ROTH IRA", "FIS 401(K) PLAN"));
}
