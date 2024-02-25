package com.vendo.jRetirement;

public enum FundsEnum {

    VBIAX ("VBIAX",   "VANGUARD BALANCED INDEX ADMIRAL",               FundType.Bond,  "Balanced"),
    VBTLX ("VBTLX",   "VANGUARD TOTAL BOND MARKET INDEX ADMIRAL",      FundType.Bond,  "IT Bond"), //Intermediate-Term Bond
    FCASH ("FCASH**", "HELD IN FCASH",                                 FundType.Cash,  "Money Market"),
    SPAXX ("SPAXX**", "HELD IN MONEY MARKET",                          FundType.Cash,  "Money Market"),
    VMFXX ("VMFXX",   "VANGUARD FED RESERVE MMKT INVESTOR CL",         FundType.Cash,  "Money Market"),
    VUSXX ("VUSXX",   "VANGUARD TREASURY MMKT INV CL",                 FundType.Cash,  "Money Market"),
    FXAIX ("FXAIX",   "FIDELITY 500 INDEX FUND",                       FundType.Stock, "Large Blend"),
    FLCSX ("FLCSX",   "FIDELITY LARGE CAP STOCK",                      FundType.Stock, "Large Blend"),
    VIG   ("VIG",     "VANGUARD SPECIALIZED FUNDS DIV APP ETF",        FundType.Stock, "Large Blend"),
    VTI   ("VTI",     "VANGUARD INDEX FDS VANGUARD TOTAL STK MKT ETF", FundType.Stock, "Large Blend"),
    VYM   ("VYM",     "VANGUARD WHITEHALL FDS HIGH DIV YLD",           FundType.Stock, "Large Value"),
    VBR   ("VBR",     "VANGUARD SMALL CAP VALUE ETF",                  FundType.Stock, "Small Value"),
    VGHAX ("VGHAX",   "VANGUARD HEALTH CARE ADMIRAL SHS",              FundType.Stock, "Health Care"),
    VIMAX ("VIMAX",   "VANGUARD MID CAP INDEX ADMIRAL SHS",            FundType.Stock, "Mid Blend"),
    VPMAX ("VPMAX",   "VANGUARD PRIMECAP ADMIRAL CLASS",               FundType.Stock, "Large Growth"),
    VSMAX ("VSMAX",   "VANGUARD SMALL-CAP INDEX ADMIRAL",              FundType.Stock, "Small Blend"),
    FLMVX ("FLMVX",   "JPM MIDCAP VALUE L",                            FundType.Stock, "Mid Value"),
    VMGRX ("VMGRX",   "VANG MIDCAP GRTH INV",                          FundType.Stock, "Mid Growth"),
    VFIAX ("VFIAX",   "VANGUARD 500 INDEX ADMIRAL",                    FundType.Stock, "Large Blend"),
    VGHCX ("VGHCX",   "VANGUARD HEALTH CARE INVESTOR",                 FundType.Stock, "Health Care"),
    FSPHX ("FSPHX",   "FIDELITY SELECT HEALTH CARE",                   FundType.Stock, "Health Care"),
//unusual naming from FIS 401K
    N857444624 ("857444624", "SS S&P 500 INDEX II",                    FundType.Stock, "Large Blend"),
    N85744A687 ("85744A687", "SS GACEQ EXUS IDX II",                   FundType.Stock, "International"),
    N857480552 ("857480552", "SS RSL SMMDCP IDX II",                   FundType.Stock, "Mid Blend"); //Small Growth

    public enum FundType {Bond, Cash, Stock}

    final String exampleMorningStarUrl = "https://www.morningstar.com/funds/xnas/vfiax/quote";

    ///////////////////////////////////////////////////////////////////////////
    FundsEnum (String symbol, String description, FundType fundType, /*boolean isBond, boolean isCash,*/ String style)
    {
        _symbol = symbol;
        _description = description;
        _fundType = fundType;
        _style = style;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getSymbol ()
    {
        return _symbol;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getDescription ()
    {
        return _description;
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isBond ()
    {
        return _fundType == FundType.Bond;
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isCash ()
    {
        return _fundType == FundType.Cash;
    }

    ///////////////////////////////////////////////////////////////////////////
    public boolean isStock ()
    {
        return _fundType == FundType.Stock;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getStyle ()
    {
        return _style;
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
    private String _symbol;
    private String _description;
    private FundType _fundType;
    private String _style;
}
