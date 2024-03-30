package com.vendo.jRetirement;

public enum FundsEnum {
    PendingActivity ("Pending Activity", "Pending Activity",           FundType.Other,     ManagementStyle.NA,     "Pending Activity",       "Pending Activity"),

    VBIAX ("VBIAX",   "VANGUARD BALANCED INDEX ADMIRAL",               FundType.StockFund, ManagementStyle.Index,  "Moder-Alloc",            "Large Growth"),
    VBTLX ("VBTLX",   "VANGUARD TOTAL BOND MARKET INDEX ADMIRAL",      FundType.BondFund,  ManagementStyle.Index,  "Intermed Core Bond",     "Bond Intermed"),
    FCASH ("FCASH**", "HELD IN FCASH",                                 FundType.Cash,      ManagementStyle.NA,     "Money Market",           "Money Market"),
    SPAXX ("SPAXX**", "HELD IN MONEY MARKET",                          FundType.Cash,      ManagementStyle.NA,     "Money Market",           "Money Market"),
    VMFXX ("VMFXX",   "VANGUARD FED RESERVE MMKT INVESTOR CL",         FundType.Cash,      ManagementStyle.NA,     "Money Market",           "Money Market"),
    VUSXX ("VUSXX",   "VANGUARD TREASURY MMKT INV CL",                 FundType.Cash,      ManagementStyle.NA,     "Money Market",           "Money Market"),
    FXAIX ("FXAIX",   "FIDELITY 500 INDEX FUND",                       FundType.StockFund, ManagementStyle.Index,  "Large Blend",            "Large Growth"),
    FZROX ("FZROX",   "FIDELITY ZERO TOTAL MARKET INDEX",              FundType.StockFund, ManagementStyle.Index,  "Large Blend",            "Large Growth"),
    FLCSX ("FLCSX",   "FIDELITY LARGE CAP STOCK",                      FundType.StockFund, ManagementStyle.Active, "Large Blend",            "Large Blend"),
    VIG   ("VIG",     "VANGUARD SPECIALIZED FUNDS DIV APP ETF",        FundType.StockETF,  ManagementStyle.Index,  "Large Blend",            "Large Blend"),
    VTI   ("VTI",     "VANGUARD INDEX FDS VANGUARD TOTAL STK MKT ETF", FundType.StockETF,  ManagementStyle.Index,  "Large Blend",            "Large Blend"),
    VYM   ("VYM",     "VANGUARD WHITEHALL FDS HIGH DIV YLD",           FundType.StockETF,  ManagementStyle.Index,  "Large Value",            "Large Value"),
    VBR   ("VBR",     "VANGUARD SMALL CAP VALUE ETF",                  FundType.StockETF,  ManagementStyle.Index,  "Small Value",            "Small Value"),
    VHT   ("VHT",     "VANGUARD WORLD FD HEALTH CAR ETF",              FundType.StockETF,  ManagementStyle.Active, "Health",                 "Large Growth"),
    VGHAX ("VGHAX",   "VANGUARD HEALTH CARE ADMIRAL SHS",              FundType.StockFund, ManagementStyle.Active, "Health",                 "Large Growth"),
    VIMAX ("VIMAX",   "VANGUARD MID CAP INDEX ADMIRAL SHS",            FundType.StockFund, ManagementStyle.Index,  "Mid-Cap Blend",          "Mid Blend"),
    VPMAX ("VPMAX",   "VANGUARD PRIMECAP ADMIRAL CLASS",               FundType.StockFund, ManagementStyle.Active, "Large Blend",            "Large Blend"),
    VSMAX ("VSMAX",   "VANGUARD SMALL-CAP INDEX ADMIRAL",              FundType.StockFund, ManagementStyle.Index,  "Small Blend",            "Small Blend"),
    FLMVX ("FLMVX",   "JPM MIDCAP VALUE L",                            FundType.StockFund, ManagementStyle.Active, "Mid-Cap Value",          "Mid Value"),
    VMGRX ("VMGRX",   "VANG MIDCAP GRTH INV",                          FundType.StockFund, ManagementStyle.Active, "Mid-Cap Growth",         "Mid Growth"),
    VFIAX ("VFIAX",   "VANGUARD 500 INDEX ADMIRAL",                    FundType.StockFund, ManagementStyle.Index,  "Large Blend",            "Large Growth"),
    VGHCX ("VGHCX",   "VANGUARD HEALTH CARE INVESTOR",                 FundType.StockFund, ManagementStyle.Active, "Health",                 "Large Growth"),
    FSPHX ("FSPHX",   "FIDELITY SELECT HEALTH CARE",                   FundType.StockFund, ManagementStyle.Active, "Health",                 "Large Growth"),
    IYH   ("IYH",     "ISHARES U.S. HEALTHCARE ETF",                   FundType.StockFund, ManagementStyle.Index,  "Health",                 "Large Growth"),
    FBND  ("FBND",    "FIDELITY TOTAL BOND ETF",                       FundType.BondETF,   ManagementStyle.Active, "Intermed Core Bond",     "Bond Intermed"),
    FIGB  ("FIGB",    "FIDELITY INVESTMENT GRADE BOND ETF",            FundType.BondETF,   ManagementStyle.Active, "Intermed Core Bond",     "Bond Intermed"),
    FLDR  ("FLDR",    "FIDELITY LOW DURATION BOND FACTOR ETF",         FundType.BondETF,   ManagementStyle.Index,  "Ultrashort Bond",        "Bond Ultrashort"),
//unusual naming from FIS 401K
    N31617E778 ("31617E778", "FID BLUE CHP GR CP A",                   FundType.StockFund, ManagementStyle.Active, "Large Growth",           "Large Growth"),
    N857444624 ("857444624", "SS S&P 500 INDEX II",                    FundType.StockFund, ManagementStyle.Index,  "Large Blend",            "Large Blend"),
    N85744A687 ("85744A687", "SS GACEQ EXUS IDX II",                   FundType.StockFund, ManagementStyle.Index,  "International",          "International"),
    N857480552 ("857480552", "SS RSL SMMDCP IDX II",                   FundType.StockFund, ManagementStyle.Index,  "Mid-Cap Blend",          "Mid Blend");

    public enum FundType {Other, BondETF, BondFund, Cash, StockETF, StockFund}
    public enum ManagementStyle {Other, NA, Active, Index}

    final String morningStarUrlFormat = "https://www.morningstar.com/funds/xnas/%s/quote";

    ///////////////////////////////////////////////////////////////////////////
    FundsEnum (String symbol, String description, FundType fundType, ManagementStyle managementStyle, String category, String investmentStyle)
    {
        this.symbol = symbol;
        this.description = description;
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

        case "Pending":
            fundFamily = "PendingActivity";
            break;

        case "SS":
            fundFamily = "StateStreet";
            break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getSymbol ()
    {
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
    public String getURL ()
    {
        if (getSymbol().startsWith("857")) { //hack - special handling for some 401K entries
            return String.format(morningStarUrlFormat, getDescription());
        } else {
            return String.format(morningStarUrlFormat, getSymbol());
        }
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
    private final FundType fundType;
    private final ManagementStyle managementStyle;
    private final String category;
    private final String investmentStyle;
}
