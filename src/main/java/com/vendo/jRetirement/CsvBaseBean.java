//CsvBaseBean.java -

package com.vendo.jRetirement;

import com.opencsv.bean.CsvBindByName;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;


public class CsvBaseBean {
    ///////////////////////////////////////////////////////////////////////////
    public CsvBaseBean() {
    }

    ///////////////////////////////////////////////////////////////////////////
    protected static double parseNumberAmount(String stringValue) {
        if (StringUtils.isBlank(stringValue)) {
            return 0.;
        }

        return Double.parseDouble(stringValue.replaceFirst("\\$", ""));
    }

	///////////////////////////////////////////////////////////////////////////
	protected static Instant parseDateMmDdYyyy(String dateString) {
		final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.parseLenient()
				.appendOptional(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
				.toFormatter();

        Instant instant = null;

        if (!StringUtils.isBlank(dateString)) {
            try {
                LocalDate localDate = LocalDate.parse(dateString.trim(), formatter);
                instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

            } catch (Exception ex) {
                System.out.println("Error parsing date string: \"" + dateString + "\"");
                ex.printStackTrace();
            }
        }

		return instant;
	}

    ///////////////////////////////////////////////////////////////////////////
    protected static String stripTrailingCopyrightChar(String value) {
        final int copyrightChar = 0xAE;

        if (value.charAt(value.length() - 1) == copyrightChar) { //remove copyright character from end
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        symbol = symbol.trim();

        if ("315994103".equals(symbol)) {
            symbol = "SPAXX"; //HACK - used when parsing Account_History files for Contributions/Distributions

        } else if (symbol.endsWith("**")) { //HACK - cleanup for example 'SPAXX**'
            symbol = symbol.replaceAll("\\*", "");

        } else if (StringUtils.isBlank(symbol)) {
            symbol = FundsEnum.ContrDistr.getSymbol(); //HACK - for Account_History if the symbol is blank, use a hardcoded value

        } else if (FundsEnum.BTCUSD.getSymbol().startsWith(symbol)) {
            symbol = FundsEnum.BTCUSD.getSymbol(); //HACK - handle the case where the symbol value in Account_History does not match the value in Portfolio_Positions

        } else if (FundsEnum.ETHUSD.getSymbol().startsWith(symbol)) {
            symbol = FundsEnum.ETHUSD.getSymbol(); //HACK - handle the case where the symbol value in Account_History does not match the value in Portfolio_Positions
        }

        this.symbol = symbol;
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description.trim();
    }

    ///////////////////////////////////////////////////////////////////////////
    public String getType() {
        return typeUnused;
    }
    public void setType(String type) {
        this.typeUnused = type.trim();
    }


    //protected members
    @CsvBindByName (column = "Symbol")              protected String symbol;
    @CsvBindByName (column = "Description")         protected String description;
    @CsvBindByName (column = "Type")                protected String typeUnused; //seems to always be "Cash" in the CSV file
}
