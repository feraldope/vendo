package com.vendo.jRetirement;

import org.apache.commons.lang3.StringUtils;

import static com.vendo.jRetirement.JRetirement.PendingActivityString;

public enum FundsEnum {
    VBIAX ("VBIAX",   "VANGUARD BALANCED INDEX ADMIRAL",                 "0.070", FundTheme.Balanced,    FundType.StockFund, ManagementStyle.Index,  "Moder-Alloc",        "Large Growth"),
    VBTLX ("VBTLX",   "VANGUARD TOTAL BOND MARKET INDEX ADMIRAL",        "0.050", FundTheme.Bonds,       FundType.BondFund,  ManagementStyle.Index,  "Intermed Core Bond", "Bond Intermed"),
    FCASH ("FCASH**", "HELD IN FCASH",                                   "0.420", FundTheme.Cash,        FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"), //expense ratio copied from SPAXX
    SPAXX ("SPAXX**", "HELD IN MONEY MARKET",                            "0.420", FundTheme.Cash,        FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"),
    VMFXX ("VMFXX",   "VANGUARD FED RESERVE MMKT INVESTOR CL",           "0.110", FundTheme.Cash,        FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"),
    VUSXX ("VUSXX",   "VANGUARD TREASURY MMKT INV CL",                   "0.090", FundTheme.Cash,        FundType.Cash,      ManagementStyle.NA,     "Money Market",       "Money Market"),
    FXAIX ("FXAIX",   "FIDELITY 500 INDEX FUND",                         "0.015", FundTheme.SP500,       FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Growth"),
    FZROX ("FZROX",   "FIDELITY ZERO TOTAL MARKET INDEX",                "0.000", FundTheme.TotalMarket, FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Growth"),
    FLCSX ("FLCSX",   "FIDELITY LARGE CAP STOCK",                        "0.760", FundTheme.LargeCap,    FundType.StockFund, ManagementStyle.Active, "Large Blend",        "Large Blend"),
    VIG   ("VIG",     "VANGUARD SPECIALIZED FUNDS DIV APP ETF",          "0.060", FundTheme.Dividends,   FundType.StockETF,  ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    VTI   ("VTI",     "VANGUARD INDEX FDS VANGUARD TOTAL STK MKT ETF",   "0.030", FundTheme.TotalMarket, FundType.StockETF,  ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    VYM   ("VYM",     "VANGUARD WHITEHALL FDS HIGH DIV YLD",             "0.060", FundTheme.Dividends,   FundType.StockETF,  ManagementStyle.Index,  "Large Value",        "Large Value"),
    VBR   ("VBR",     "VANGUARD SMALL CAP VALUE ETF",                    "0.070", FundTheme.SmallCap,    FundType.StockETF,  ManagementStyle.Index,  "Small Value",        "Small Value"),
    VHT   ("VHT",     "VANGUARD WORLD FD HEALTH CAR ETF",                "0.100", FundTheme.HealthCare,  FundType.StockETF,  ManagementStyle.Active, "Health",             "Large Growth"),
    VGHAX ("VGHAX",   "VANGUARD HEALTH CARE ADMIRAL SHS",                "0.034", FundTheme.HealthCare,  FundType.StockFund, ManagementStyle.Active, "Health",             "Large Growth"),
    VIMAX ("VIMAX",   "VANGUARD MID CAP INDEX ADMIRAL SHS",              "0.050", FundTheme.MidCap,      FundType.StockFund, ManagementStyle.Index,  "Mid-Cap Blend",      "Mid Blend"),
    VPMAX ("VPMAX",   "VANGUARD PRIMECAP ADMIRAL CLASS",                 "0.310", FundTheme.LargeCap,    FundType.StockFund, ManagementStyle.Active, "Large Blend",        "Large Blend"),
    VSMAX ("VSMAX",   "VANGUARD SMALL-CAP INDEX ADMIRAL",                "0.050", FundTheme.SmallCap,    FundType.StockFund, ManagementStyle.Index,  "Small Blend",        "Small Blend"),
    FLMVX ("FLMVX",   "JPM MIDCAP VALUE L",                              "0.750", FundTheme.MidCap,      FundType.StockFund, ManagementStyle.Active, "Mid-Cap Value",      "Mid Value"),
    VMGRX ("VMGRX",   "VANG MIDCAP GRTH INV",                            "0.370", FundTheme.MidCap,      FundType.StockFund, ManagementStyle.Active, "Mid-Cap Growth",     "Mid Growth"),
    VFIAX ("VFIAX",   "VANGUARD 500 INDEX ADMIRAL",                      "0.040", FundTheme.SP500,       FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Growth"),
    VGHCX ("VGHCX",   "VANGUARD HEALTH CARE INVESTOR",                   "0.340", FundTheme.HealthCare,  FundType.StockFund, ManagementStyle.Active, "Health",             "Large Growth"),
    FSPHX ("FSPHX",   "FIDELITY SELECT HEALTH CARE",                     "0.690", FundTheme.HealthCare,  FundType.StockFund, ManagementStyle.Active, "Health",             "Large Growth"),
    IYH   ("IYH",     "ISHARES U.S. HEALTHCARE ETF",                     "0.400", FundTheme.HealthCare,  FundType.StockETF,  ManagementStyle.Index,  "Health",             "Large Growth"),
    FBND  ("FBND",    "FIDELITY TOTAL BOND ETF",                         "0.450", FundTheme.Bonds,       FundType.BondETF,   ManagementStyle.Active, "Intermed Core Bond", "Bond Intermed"),
    FIGB  ("FIGB",    "FIDELITY INVESTMENT GRADE BOND ETF",              "0.450", FundTheme.Bonds,       FundType.BondETF,   ManagementStyle.Active, "Intermed Core Bond", "Bond Intermed"),
    FLDR  ("FLDR",    "FIDELITY LOW DURATION BOND FACTOR ETF",           "0.150", FundTheme.Bonds,       FundType.BondETF,   ManagementStyle.Index,  "Ultrashort Bond",    "Bond Ultrashort"),
    VWENX ("VWENX",   "VANG WELLINGTON ADM",                             "0.180", FundTheme.Balanced,    FundType.StockFund, ManagementStyle.Active, "Moder-Alloc",        "Large Blend"),
    SCHD  ("SCHD",    "SCHWAB US DIVIDEND EQUITY ETF",                   "0.060", FundTheme.Dividends,   FundType.StockETF,  ManagementStyle.Index,  "Large Value",        "Large Value"),
    //unusual symbol for CDs
    CD2025 ("06051XDD1", "BANK OF AMERICA NA CD 5.10000% 05/02/2025",    "0.000", FundTheme.CD,          FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2026 ("06051XDB5", "BANK OF AMERICA NA CD 4.95000% 05/04/2026",    "0.000", FundTheme.CD,          FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2027 ("05584CNB8", "BNY MELLON NA CD 4.40000% 05/06/2027",         "0.000", FundTheme.CD,          FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2028 ("61690DPY8", "MORGAN STANLEY BK N A CD 4.65000% 05/01/2028", "0.000", FundTheme.CD,          FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    CD2029 ("61690DPV4", "MORGAN STANLEY BK N A CD 4.55000% 05/01/2029", "0.000", FundTheme.CD,          FundType.Cash,      ManagementStyle.NA,     "CD",                 "CD"),
    //inactive; unusual symbol for FIS 401K
    N31617E778 ("31617E778", "FID BLUE CHP GR CP A",                     "0.690", FundTheme.LargeCap,    FundType.StockFund, ManagementStyle.Active, "Large Growth",       "Large Growth"),
    N857444624 ("857444624", "SS S&P 500 INDEX II",                      "0.160", FundTheme.SP500,       FundType.StockFund, ManagementStyle.Index,  "Large Blend",        "Large Blend"),
    N85744A687 ("85744A687", "SS GACEQ EXUS IDX II",                     "0.050", FundTheme.LargeCap,    FundType.StockFund, ManagementStyle.Index,  "International",      "International"),
    N857480552 ("857480552", "SS RSL SMMDCP IDX II",                     "0.030", FundTheme.SmallCap,    FundType.StockFund, ManagementStyle.Index,  "Mid-Cap Blend",      "Mid Blend"),
    //we need to be able to parse these lines
    PendingActivity ("Pending Activity", "Pending Activity",             "-N/A-", FundTheme.Pending,     FundType.Cash,      ManagementStyle.NA,     "Pending Activity",   "Pending Activity");

    public enum FundOwner {unknown, dr, mr}
    public enum FundType {Unknown, BondETF, BondFund, Cash, StockETF, StockFund}
    public enum FundTheme {Unknown, CD, Cash, Bonds, SP500, Dividends, TotalMarket, SmallCap, MidCap, LargeCap, HealthCare, Balanced, Pending}
    public enum ManagementStyle {NA, Active, Index}


    ///////////////////////////////////////////////////////////////////////////
    FundsEnum (String symbol, String description, String expenseRatio, FundTheme fundTheme, FundType fundType, ManagementStyle managementStyle, String category, String investmentStyle)
    {
//        if (symbol.endsWith("**")) { //cleanup for example 'SPAXX**'
//            symbol = symbol.substring(0, symbol.length() - 2);
//        }

        this.symbol = symbol;
        this.description = description;
        this.expenseRatio = expenseRatio;
        this.fundTheme = fundTheme;
        this.fundType = fundType;
        this.managementStyle = managementStyle;
        this.category = category;               //from MorningStar.com
        this.investmentStyle = investmentStyle; //from MorningStar.com

        //generate fundFamily from description from CSV file
        String desc0 = description.split(" ")[0];
        switch (desc0) {
        default:
            fundFamily = desc0.charAt(0) + desc0.substring(1).toLowerCase();
            break;

        case "HELD":
        case "Fid":
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
    public String getSymbolCleaned () {
        String cleaned = getSymbol();
        if (cleaned.endsWith("*")) {
            cleaned = cleaned.replaceAll("\\**", ""); //remove trailing "*"
        }
        return cleaned;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getSymbolForGrouping () {
        String symbol = getSymbol();
        if (symbol.startsWith("61690DP") || symbol.startsWith("06051XD") || symbol.startsWith("05584CN")) {
            symbol = "CD";
        }
        return symbol;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getFundFamily ()
    {
        return fundFamily;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundType getFundType ()
    {
        return fundType;
    }

    ///////////////////////////////////////////////////////////////////////////
    public FundTheme getFundTheme ()
    {
        return fundTheme;
    }

    ///////////////////////////////////////////////////////////////////////////
    public ManagementStyle getManagementStyle ()
    {
        return managementStyle;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getDescription ()
    {
        return description;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getExpenseRatio ()
    {
        return expenseRatio;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getCategory ()
    {
        return category;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getInvestmentStyle ()
    {
        return investmentStyle;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getURL (String suffix)
    {
        StringBuilder url = new StringBuilder("https://www.google.com/search?q=");
        url.append(getFundFamily())
           .append("+")
           .append(getSymbolCleaned());

        if (!StringUtils.isBlank(suffix)) {
            url.append("+")
               .append(suffix);
        }

        return url.toString();
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isBond ()
    {
        return fundType == FundType.BondETF || fundType == FundType.BondFund;
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isCash ()
    {
        return fundType == FundType.Cash;
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isHealth ()
    {
        return "Health".equals(getCategory());
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isActive ()
    {
        return ManagementStyle.Active.equals(getManagementStyle());
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isIndex ()
    {
        return ManagementStyle.Index.equals(getManagementStyle());
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isPendingActivity ()
    {
        return PendingActivityString.equals(getSymbol());
    }

    ///////////////////////////////////////////////////////////////////////////
    public static FundsEnum getValue (String symbol)
    {
        //brute-force method
        for (FundsEnum value : values ()) {
            if (value.getSymbol ().equals (symbol)) {
                return value;
            }
        }

        throw new RuntimeException ("FundsEnum.getValue: invalid symbol \"" + symbol + "\"");
    }

    //members
    private final String symbol;
    private final String fundFamily;
    private final String description;
    private final String expenseRatio; //adjusted expense ratio, if applies
    private final FundTheme fundTheme;
    private final FundType fundType;
    private final ManagementStyle managementStyle;
    private final String category;
    private final String investmentStyle;
}
