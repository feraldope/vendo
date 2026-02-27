package com.vendo.jRetirement;

public enum FundsEnum {
    VBIAX  ("VBIAX",   "VANGUARD BALANCED INDEX ADMIRAL",                 0.070, FundTheme.Balanced,      FundType.StockFund, ManagementStyle.Index,  "Moder-Alloc",        "Large Growth"),
    VBTLX  ("VBTLX",   "VANGUARD TOTAL BOND MARKET INDEX ADMIRAL",        0.050, FundTheme.Bonds,         FundType.BondFund,  ManagementStyle.Index,  "Intermed Core Bond", "Bond Intermed"),
    SPAXX  ("SPAXX",   "HELD IN MONEY MARKET",                            0.420, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"),
    FCASH  ("FCASH",   "HELD IN FCASH",                                   0.420, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"), //expense ratio copied from SPAXX
    FDRXX  ("FDRXX",   "HELD IN MONEY MARKET",                            0.420, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"), //expense ratio copied from SPAXX
    FZFXX  ("FZFXX",   "HELD IN MONEY MARKET",                            0.420, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"), //expense ratio copied from SPAXX
    FRSXX  ("FRSXX",   "FIMM TREASURY ONLY PORTFOLIO: INSTL CL",          0.420, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"), //expense ratio copied from SPAXX
    VMFXX  ("VMFXX",   "VANGUARD FED RESERVE MMKT INVESTOR CL",           0.110, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"),
    VUSXX  ("VUSXX",   "VANGUARD TREASURY MMKT INV CL",                   0.090, FundTheme.Cash,          FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"),
    FXAIX  ("FXAIX",   "FIDELITY 500 INDEX FUND",                         0.015, FundTheme.SP500,         FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Growth"),
    FZROX  ("FZROX",   "FIDELITY ZERO TOTAL MARKET INDEX",                0.000, FundTheme.TotalMarket,   FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Growth"),
    FLCSX  ("FLCSX",   "FIDELITY LARGE CAP STOCK",                        0.760, FundTheme.LargeCap,      FundType.StockFund, ManagementStyle.Active, "Large Blend",        "Large Blend"),
    VOO    ("VOO",     "VANGUARD INDEX FUNDS S&P 500 ETF USD",            0.030, FundTheme.SP500,         FundType.StockETF,  ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    VIG    ("VIG",     "VANGUARD SPECIALIZED FUNDS DIV APP ETF",          0.060, FundTheme.Dividends,     FundType.StockETF,  ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    VTI    ("VTI",     "VANGUARD INDEX FDS VANGUARD TOTAL STK MKT ETF",   0.030, FundTheme.TotalMarket,   FundType.StockETF,  ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    VYM    ("VYM",     "VANGUARD WHITEHALL FDS HIGH DIV YLD",             0.060, FundTheme.Dividends,     FundType.StockETF,  ManagementStyle.Index,  "Large Value",        "Large Value"),
    VBR    ("VBR",     "VANGUARD SMALL CAP VALUE ETF",                    0.070, FundTheme.SmallCap,      FundType.StockETF,  ManagementStyle.Index,  "Small Value",        "Small Value"),
    VHT    ("VHT",     "VANGUARD WORLD FD HEALTH CAR ETF",                0.100, FundTheme.HealthCare,    FundType.StockETF,  ManagementStyle.Active, "Health",             "Large Growth"),
    VGHAX  ("VGHAX",   "VANGUARD HEALTH CARE ADMIRAL SHS",                0.034, FundTheme.HealthCare,    FundType.StockFund, ManagementStyle.Active, "Health",             "Large Growth"),
    VIMAX  ("VIMAX",   "VANGUARD MID CAP INDEX ADMIRAL SHS",              0.050, FundTheme.MidCap,        FundType.StockFund, ManagementStyle.Index,  "Mid-Cap Blend",      "Mid Blend"),
    VPMAX  ("VPMAX",   "VANGUARD PRIMECAP ADMIRAL CLASS",                 0.310, FundTheme.LargeCap,      FundType.StockFund, ManagementStyle.Active, "Large Blend",        "Large Blend"),
    VSMAX  ("VSMAX",   "VANGUARD SMALL-CAP INDEX ADMIRAL",                0.050, FundTheme.SmallCap,      FundType.StockFund, ManagementStyle.Index,  "Small Blend",        "Small Blend"),
    FLMVX  ("FLMVX",   "JPM MIDCAP VALUE L",                              0.750, FundTheme.MidCap,        FundType.StockFund, ManagementStyle.Active, "Mid-Cap Value",      "Mid Value"),
    VMGRX  ("VMGRX",   "VANG MIDCAP GRTH INV",                            0.370, FundTheme.MidCap,        FundType.StockFund, ManagementStyle.Active, "Mid-Cap Growth",     "Mid Growth"),
    VFIAX  ("VFIAX",   "VANGUARD 500 INDEX ADMIRAL",                      0.040, FundTheme.SP500,         FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Growth"),
    VGHCX  ("VGHCX",   "VANGUARD HEALTH CARE INVESTOR",                   0.340, FundTheme.HealthCare,    FundType.StockFund, ManagementStyle.Active, "Health",             "Large Growth"),
    FSPHX  ("FSPHX",   "FIDELITY SELECT HEALTH CARE",                     0.690, FundTheme.HealthCare,    FundType.StockFund, ManagementStyle.Active, "Health",             "Large Growth"),
    IYH    ("IYH",     "ISHARES U.S. HEALTHCARE ETF",                     0.400, FundTheme.HealthCare,    FundType.StockETF,  ManagementStyle.Index,  "Health",             "Large Growth"),
    SGOV   ("SGOV",    "ISHARES TR 0-3 MNTH TREASRY ETF",                 0.090, FundTheme.Bonds,         FundType.BondETF,   ManagementStyle.Index,  "Ultrashort Bond",    "Bond Ultrashort"),
    IXUS   ("IXUS",    "ISHARES TR CORE MSCI TOTAL ETF",                  0.070, FundTheme.International, FundType.StockETF,  ManagementStyle.Index,  "International",      "International"),
    FBND   ("FBND",    "FIDELITY TOTAL BOND ETF",                         0.450, FundTheme.Bonds,         FundType.BondETF,   ManagementStyle.Active, "Intermed Core Bond", "Bond Intermed"),
    FIGB   ("FIGB",    "FIDELITY INVESTMENT GRADE BOND ETF",              0.450, FundTheme.Bonds,         FundType.BondETF,   ManagementStyle.Active, "Intermed Core Bond", "Bond Intermed"),
    FLDR   ("FLDR",    "FIDELITY LOW DURATION BOND FACTOR ETF",           0.150, FundTheme.Bonds,         FundType.BondETF,   ManagementStyle.Index,  "Ultrashort Bond",    "Bond Ultrashort"),
    VWENX  ("VWENX",   "VANG WELLINGTON ADM",                             0.180, FundTheme.Balanced,      FundType.StockFund, ManagementStyle.Active, "Moder-Alloc",        "Large Blend"),
    SCHD   ("SCHD",    "SCHWAB US DIVIDEND EQUITY ETF",                   0.060, FundTheme.Dividends,     FundType.StockETF,  ManagementStyle.Index,  "Large Value",        "Large Value"),

    //used by Crypto
    USD    ("USD",     "US DOLLARS",                                      0.000, FundTheme.Cash,          FundType.Cash,      ManagementStyle.Index,  "Cash",               "Cash"),
    BTCUSD ("BTC/USD", "BITCOIN",                                         0.000, FundTheme.Crypto,        FundType.Crypto,    ManagementStyle.Index,  "Crypto",             "Crypto"),
    ETHUSD ("ETH/USD", "ETHEREUM",                                        0.000, FundTheme.Crypto,        FundType.Crypto,    ManagementStyle.Index,  "Crypto",             "Crypto"),

    //DO NOT DELETE - unusual symbol for CDs
    CD2025a ("06051XDD1", "BANK OF AMERICA NA CD 5.10000% 05/02/2025",         0.000, FundTheme.CD,       FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2026a ("06051XDB5", "BANK OF AMERICA NA CD 4.95000% 05/04/2026",         0.000, FundTheme.CD,       FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2027a ("05584CNB8", "BNY MELLON NA CD 4.40000% 05/06/2027",              0.000, FundTheme.CD,       FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2028a ("61690DPY8", "MORGAN STANLEY BK N A CD 4.65000% 05/01/2028",      0.000, FundTheme.CD,       FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2029a ("61690DPV4", "MORGAN STANLEY BK N A CD 4.55000% 05/01/2029",      0.000, FundTheme.CD,       FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2026b ("949764SC9", "WELLS FARGO BANK NATL ASSN CD 3.75000% 09/18/2026", 0.000, FundTheme.CD,       FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),

    //DO NOT DELETE - inactive; unusual symbol for FIS 401K
    N31617E778 ("31617E778", "FID BLUE CHP GR CP A",                     0.690, FundTheme.LargeCap,       FundType.StockFund, ManagementStyle.Active, "Large Growth",       "Large Growth"),
    N857444624 ("857444624", "SS S&P 500 INDEX II",                      0.160, FundTheme.SP500,          FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    N85744A687 ("85744A687", "SS GACEQ EXUS IDX II",                     0.050, FundTheme.International,  FundType.StockFund, ManagementStyle.Index,  "International",      "International"),
    N857480552 ("857480552", "SS RSL SMMDCP IDX II",                     0.030, FundTheme.SmallCap,       FundType.StockFund, ManagementStyle.Index,  "Mid-Cap Blend",      "Mid Blend"),

    //DO NOT DELETE - bend ENUM for FIS Stock
    FIS ("FIS", "FIDELITY NATL INFORMATION SERVICES COM",                0.000, FundTheme.MidCap,         FundType.StockFund, ManagementStyle.Active, "Mid-Cap Blend",      "Mid Blend"),

    //we need to be able to parse these lines
    PendingActivity ("Pending Activity", "Pending Activity",             0.000, FundTheme.Pending,        FundType.Cash,      ManagementStyle.NA,     "Pending Activity",   "Pending Activity"),
    //used when parsing Account_History files for Contributions/Distributions
    ContrDistr      ("Contr/Distr",      "Contr/Distr",                  0.000, FundTheme.Unknown,        FundType.Cash,      ManagementStyle.NA,     "Contr/Distr",        "Contr/Distr");

    public enum FundOwner {unknown, dr, mr}
    public enum FundType {Unknown, BondETF, BondFund, Cash, Crypto, StockETF, StockFund}
    public enum FundTheme {Unknown, CD, Cash, Crypto, Bonds, SP500, Dividends, TotalMarket, SmallCap, MidCap, LargeCap, HealthCare, Balanced, International, Pending}
    public enum ManagementStyle {NA, Active, Index}

    //used in PortfolioPositionsBean/Data
    public enum TaxableType {Unspecified, TaxFree /*ROTH*/, TaxDeferred /*Traditional*/, Taxable /*Non-retirement*/}

    //used in AccountsHistoryBean/Data
    public enum Activity {Unspecified, Contribution, Distribution, Redemption}


    ///////////////////////////////////////////////////////////////////////////
    FundsEnum (String symbol, String description, Double expenseRatio, FundTheme fundTheme, FundType fundType, ManagementStyle managementStyle, String category, String investmentStyle) {
        this.symbol = symbol;
        this.description = description;
        this.expenseRatio = expenseRatio;
        this.fundTheme = fundTheme;
        this.fundType = fundType;
        this.managementStyle = managementStyle;
        this.category = category;               //from MorningStar.com
        this.investmentStyle = investmentStyle; //from MorningStar.com

        //calculate fundFamily from description field in CSV file
        String[] descArray = description.split(" ");
        switch(descArray[0]) {
            default:
                fundFamily = descArray[0].charAt(0) + descArray[0].substring(1).toLowerCase();
                break;

            case "HELD": //e.g., "HELD IN MONEY MARKET"
            case "Fid":  //e.g., "FID BLUE CHP GR CP A"
            case "FIMM": //e.g., "FIMM TREASURY ONLY PORTFOLIO: INSTL CL"
            case "US":   //e.g., "US DOLLARS"
                fundFamily = "Fidelity";
                break;

            case "VANG":
                fundFamily = "Vanguard";
                break;

            case "ISHARES":
                fundFamily = "IShares";
                break;

            case "JPM":
                fundFamily = "JPMorgan";
                break;

            case "SCHWAB":
                fundFamily = "Schwab";
                break;

            case "BANK":
                fundFamily = "BankOfAmerica";
                break;

            case "BNY":
                fundFamily = "BNYMellon";
                break;

            case "MORGAN":
                fundFamily = "MorganStanley";
                break;

            case "Pending":
                fundFamily = "PendingActivity";
                break;

            case "SS":
                fundFamily = "StateStreet";
                break;
            }
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getSymbol () {
        return symbol;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getFundFamily () {
        return fundFamily;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundType getFundType () {
        return fundType;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundTheme getFundTheme () {
        return fundTheme;
    }

    ///////////////////////////////////////////////////////////////////////////
    public ManagementStyle getManagementStyle () {
        return managementStyle;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getDescription () {
        return description;
    }

    ///////////////////////////////////////////////////////////////////////////
    public Double getExpenseRatio () {
        return expenseRatio;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getCategory () {
        return category;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getInvestmentStyle () {
        return investmentStyle;
    }

    ///////////////////////////////////////////////////////////////////////////
    public static FundsEnum getValue (String symbol) {
        //brute-force method
        for (FundsEnum value : values ()) {
            if (value.getSymbol ().equals (symbol)) {
                return value;
            }
        }

        throw new RuntimeException ("FundsEnum.getValue: invalid symbol \"" + symbol + "\"");
    }


    //private members
    private final String symbol;
    private final String fundFamily;
    private final String description;
    private final Double expenseRatio; //adjusted expense ratio, if applies
    private final FundTheme fundTheme;
    private final FundType fundType;
    private final ManagementStyle managementStyle;
    private final String category;
    private final String investmentStyle;
}
