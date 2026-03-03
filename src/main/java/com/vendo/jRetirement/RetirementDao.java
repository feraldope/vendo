//RetirementDao.java

package com.vendo.jRetirement;

import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RetirementDao {
	///////////////////////////////////////////////////////////////////////////
	private RetirementDao() { //singleton
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public static RetirementDao getInstance() {
		if (instance == null) {
			synchronized (RetirementDao.class) {
				if (instance == null) {
					instance = new RetirementDao();
				}
			}
		}

		return instance;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<FundsMetaData> queryFundsMetaDataFromDatabase() {
		String sql = "select symbol, fund_family, description, expense_ratio, fund_theme, fund_type, management_style, category, investment_style" + NL +
					 " from funds_meta_data" + NL +
					 " order by symbol";

		List<FundsMetaData> records = new ArrayList<>();

		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			while (rs.next()) {
				String symbol = rs.getString("symbol");
				String fundFamily = rs.getString("fund_family");
				String description = rs.getString("description");
				Double expenseRatio = rs.getDouble("expense_ratio");
				String fundThemeStr = rs.getString("fund_theme");
				String fundTypeStr = rs.getString("fund_type");
				String managementStyleStr = rs.getString("management_style");
				String category = rs.getString("category");
				String investmentStyle = rs.getString("investment_style");

				FundsEnum.FundTheme fundTheme = FundsEnum.FundTheme.valueOf(fundThemeStr);
				FundsEnum.FundType fundType = FundsEnum.FundType.valueOf(fundTypeStr);
				FundsEnum.ManagementStyle managementStyle = FundsEnum.ManagementStyle.valueOf(managementStyleStr);

				FundsMetaData record = new FundsMetaData(symbol, fundFamily, description, expenseRatio, fundTheme, fundType, managementStyle, category, investmentStyle);
				records.add(record);
			}

		} catch (Exception ex) {
			System.err.println("queryFundsMetaDataFromDatabase: error running sql <" + sql + ">");
			System.err.println(ex.getMessage());
			return records;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public int persistFundsMetaDataToDatabase(List<FundsMetaData> records) throws Exception {
		int rowsPersisted = 0;

		if (records != null) {
			try (Connection connection = connectDatabase()) {
				for (FundsMetaData record : records) {
					if (persistFundsMetaDataToDatabase(connection, record)) {
						++rowsPersisted;
					}
				}
			}
		}

		return rowsPersisted;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean persistFundsMetaDataToDatabase(Connection connection, FundsMetaData record) {
		String sql = "insert into funds_meta_data (symbol, fund_family, description, expense_ratio, fund_theme, fund_type, management_style, category, investment_style)" + NL +
					 " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)" + NL;
		String sqlOnDup = " on duplicate key update symbol = VALUES(symbol)," + NL +
												  " fund_family = VALUES(fund_family)," + NL +
												  " description = VALUES(description)," + NL +
												  " expense_ratio = VALUES(expense_ratio)," + NL +
												  " fund_theme = VALUES(fund_theme)," + NL +
												  " fund_type = VALUES(fund_type)," + NL +
												  " management_style = VALUES(management_style)," + NL +
												  " category = VALUES(category)," + NL +
												  " investment_style = VALUES(investment_style)";

		try (PreparedStatement stmt = connection.prepareStatement(sql + sqlOnDup)) {
			int index = 0;
			stmt.setString(++index, record.getSymbol());
			stmt.setString(++index, record.getFundFamily());
			stmt.setString(++index, record.getDescription());
			stmt.setDouble(++index, record.getExpenseRatio());
			stmt.setString(++index, record.getFundTheme().toString());
			stmt.setString(++index, record.getFundType().toString());
			stmt.setString(++index, record.getManagementStyle().toString());
			stmt.setString(++index, record.getCategory());
			stmt.setString(++index, record.getInvestmentStyle());

			stmt.executeUpdate();

		} catch (Exception ex) {
			System.err.println("persistFundsMetaDataToDatabase: error persisting record <" + record + ">");
			System.err.println(ex.getMessage());
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<PortfolioPositionsData> queryPortfolioPositionsDataFromDatabase(Instant dateDownloaded) {
		//we need the FundsMetaData object to set into the PortfolioPositionsData object
		final Map<String, FundsMetaData> fundsMetaDataMap = queryFundsMetaDataFromDatabase().stream()
				.collect(Collectors.toMap(FundsMetaData::getSymbol, Function.identity()));

		String sql = "select downloaded_timestamp, account_number, account_name, symbol, description, value, cost_basis, taxable_type, fund_owner" + NL +
				" from portfolio_positions_data" + NL;
		if (!dateDownloaded.equals(AllDates)) {
			sql += " where downloaded_timestamp = ?" + NL;
		}
		sql += " order by downloaded_timestamp, account_number, account_name, symbol";

		List<PortfolioPositionsData> records = new ArrayList<>();

		ResultSet rs = null;
		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {

			if (!dateDownloaded.equals(AllDates)) {
				java.sql.Timestamp timestamp = java.sql.Timestamp.from(dateDownloaded);
				stmt.setTimestamp(1, timestamp);
			}

			rs = stmt.executeQuery ();

			while (rs.next ()) {
				java.sql.Timestamp timestamp = rs.getTimestamp("downloaded_timestamp");
				String accountNumber = rs.getString("account_number");
				String accountName = rs.getString("account_name");
				String symbol = rs.getString("symbol");
				String description = rs.getString("description");
				Double currentValue = rs.getDouble("value");
				Double costBasis = rs.getDouble("cost_basis");
				String taxableTypeStr = rs.getString("taxable_type");
				String fundOwnerStr = rs.getString("fund_owner");

				FundsEnum.TaxableType taxableType = FundsEnum.TaxableType.valueOf(taxableTypeStr);
				FundsEnum.FundOwner fundOwner = FundsEnum.FundOwner.valueOf(fundOwnerStr);

				FundsMetaData fundsMetaData = fundsMetaDataMap.get(symbol);
				VendoUtils.myAssert(fundsMetaData != null, "fundsMetaData != null", "FK in DB should prevent this"); //do not use Java's assert as it is disabled by default

				PortfolioPositionsData record = new PortfolioPositionsData(timestamp.toInstant(), accountNumber, accountName, symbol, description, currentValue, costBasis, taxableType, fundOwner, fundsMetaData);
				records.add(record);
			}

		} catch (Exception ex) {
			System.err.println("queryPortfolioPositionsDataFromDatabase: error running sql <" + sql + "> for dateDownloaded <" + dateDownloaded + ">");
			System.err.println(ex.getMessage());
			return records;

		} finally {
			if (rs != null) {
				try { rs.close (); } catch (SQLException ignored) {}
			}
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public int persistPortfolioPositionsDataToDatabase(List<PortfolioPositionsData> records) throws Exception {
		int rowsPersisted = 0;

		if (records != null) {
			try (Connection connection = connectDatabase()) {
				for (PortfolioPositionsData record : records) {
					if (persistPortfolioPositionsDataToDatabase(connection, record)) {
						++rowsPersisted;
					}
				}
			}
		}

		return rowsPersisted;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean persistPortfolioPositionsDataToDatabase(Connection connection, PortfolioPositionsData record) throws Exception {
		final String sql = "insert into portfolio_positions_data (downloaded_timestamp, account_number, account_name, symbol, description, value, cost_basis, taxable_type, fund_owner)" + NL +
				" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
//		String sqlOnDup = " on duplicate key update ... [TBD]

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			int index = 0;
			VendoUtils.myAssert(null != record.getDateDownloaded(), "null != record.getDateDownloaded()", null); //do not use Java's assert as it is disabled by default
			stmt.setTimestamp(++index, java.sql.Timestamp.from(record.getDateDownloaded()));
			stmt.setString(++index, record.getAccountNumber());
			stmt.setString(++index, record.getAccountName());
			stmt.setString(++index, record.getSymbol());
			stmt.setString(++index, record.getDescription());
			stmt.setDouble(++index, record.getCurrentValue());
			stmt.setDouble(++index, record.getCostBasis());
			stmt.setString(++index, record.getTaxableType().toString());
			stmt.setString(++index, record.getFundOwner().toString());

			stmt.executeUpdate();

		} catch (Exception ex) {
			System.err.println("persistPortfolioPositionsDataToDatabase: error persisting record <" + record + ">");
			System.err.println(ex.getMessage());
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<AccountsHistoryData> queryContributionDataFromDatabase(Instant runDate) {
		String accountsToSkip = "FIS 401(K) PLAN"; //not interesting for now

		//Note distributions are already negative in the database
		String sql = "select run_date, account_name, account_number, action, symbol, description, SUM(commission) as commission, SUM(fees) as fees, SUM(amount) as amount, settlement_date, activity" + NL +
				     " from account_history_data" + NL +
//				     " where UPPER(action) rlike '.*CONTR.*'" + NL + //NOTE: rlike is not case-sensitive unless used on binary string
				     " where activity = '" + FundsEnum.Activity.Contribution + "'" + NL +
					 " and account_name != '" + accountsToSkip + "'" + NL +
				     " and ABS(amount) > " + skipSmallerAmounts + NL; //skip smaller transactions
		if (!runDate.equals(AllDates)) {
			sql += " and run_date = ?" + NL;
		}
		sql += " group by run_date, account_name, account_number, symbol, description" + NL +
			   " order by run_date";

		List<AccountsHistoryData> records = queryAccountsHistoryDataFromDatabase(runDate, sql);

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<AccountsHistoryData> queryDistributionDataFromDatabase(Instant runDate) {
		//Note distributions are already negative in the database
		String sql = "select run_date, account_name, account_number, action, symbol, description, SUM(commission) as commission, SUM(fees) as fees, SUM(amount) as amount, settlement_date, activity" + NL +
				     " from account_history_data" + NL +
//				     " where (UPPER(action) rlike '.*DISTR.*' OR UPPER(action) rlike '.*TAX.*')" + NL + //NOTE: rlike is not case-sensitive unless used on binary string
				     " where activity = '" + FundsEnum.Activity.Distribution + "'" + NL +
				     " and ABS(amount) > " + skipSmallerAmounts + NL; //skip smaller transactions
		if (!runDate.equals(AllDates)) {
			sql += " and run_date = ?" + NL;
		}
		sql += " group by run_date, account_name, account_number, symbol, description" + NL +
			   " order by run_date";

		List<AccountsHistoryData> records = queryAccountsHistoryDataFromDatabase(runDate, sql);

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<AccountsHistoryData> queryRedemptionDataFromDatabase(Instant runDate) {
		//Note redemptions are not negative in the database, so we need to negate them
		String sql = "select run_date, account_name, account_number, action, symbol, description, SUM(commission) as commission, SUM(fees) as fees, (-1) * SUM(amount) as amount, settlement_date, activity" + NL +
				     " from account_history_data" + NL +
//				     " where UPPER(action) rlike 'REDEMPTION FROM CORE ACCOUNT.*'" + NL + //NOTE: rlike is not case-sensitive unless used on binary string
				     " where activity = '" + FundsEnum.Activity.Redemption + "'" + NL +
				     " and account_name = 'Individual - TOD'" + NL +
				     " and ABS(amount) > " + skipSmallerAmounts + NL; //skip smaller transactions
		if (!runDate.equals(AllDates)) {
			sql += " and run_date = ?" + NL;
		}
		sql += " group by run_date, account_name, account_number, symbol, description" + NL +
			   " order by run_date";

		List<AccountsHistoryData> records = queryAccountsHistoryDataFromDatabase(runDate, sql);

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<AccountsHistoryData> queryAccountsHistoryDataFromDatabase(Instant runDate) {
		String sql = "select run_date, account_name, account_number, action, symbol, description, commission, fees, amount, settlement_date, activity" + NL +
				     " from account_history_data" + NL;
		if (!runDate.equals(AllDates)) {
			sql += " where run_date = ?" + NL;
		}
		sql += " order by run_date, account_name, account_number, action, symbol";

		List<AccountsHistoryData> records = queryAccountsHistoryDataFromDatabase(runDate, sql);

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	private List<AccountsHistoryData> queryAccountsHistoryDataFromDatabase(Instant runDate, String sql) {
		if (!runDate.equals(AllDates)) {
			throw new RuntimeException ("RetirementDao.queryAccountsHistoryDataFromDatabase: code path not implemented"); //TODO - implement
		}

		List<AccountsHistoryData> records = new ArrayList<>();

		ResultSet rs = null;
		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {

			if (!runDate.equals(AllDates)) {
				java.sql.Timestamp timestamp = java.sql.Timestamp.from(runDate);
				stmt.setTimestamp(1, timestamp);
			}

			rs = stmt.executeQuery ();

			while (rs.next ()) {
				java.sql.Timestamp runDateTimestamp = rs.getTimestamp("run_date");
				String accountName = rs.getString("account_name");
				String accountNumber = rs.getString("account_number");
				String action = rs.getString("action");
				String symbol = rs.getString("symbol");
				String description = rs.getString("description");
				Double commission = rs.getDouble("commission");
				Double fees = rs.getDouble("fees");
				Double amount = rs.getDouble("amount");
				java.sql.Timestamp settlementDateTimestamp = rs.getTimestamp("settlement_date");
				Instant settlementDateInstant = settlementDateTimestamp == null ? null : settlementDateTimestamp.toInstant(); //handle null
				String activityStr = rs.getString("activity");

				FundsEnum.Activity activity = StringUtils.isBlank(activityStr) ? FundsEnum.Activity.Unspecified : FundsEnum.Activity.valueOf(activityStr); //handle null

				AccountsHistoryData record = new AccountsHistoryData(runDateTimestamp.toInstant(), accountName, accountNumber, action, symbol, description, commission, fees, amount, settlementDateInstant, activity);
				records.add(record);
			}

		} catch (Exception ex) {
			System.err.println("queryAccountsHistoryDataFromDatabase: error running sql <" + sql + "> for runDate <" + runDate + ">");
			System.err.println(ex.getMessage());
			return records;

		} finally {
			if (rs != null) {
				try { rs.close (); } catch (SQLException ignored) {}
			}
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public int persistAccountsHistoryDataToDatabase(List<AccountsHistoryData> records) throws Exception {
		int rowsPersisted = 0;

		if (records != null) {
			try (Connection connection = connectDatabase()) {
				for (AccountsHistoryData record : records) {
					if (persistAccountsHistoryDataToDatabase(connection, record)) {
						++rowsPersisted;
					}
				}
			}
		}

		return rowsPersisted;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean persistAccountsHistoryDataToDatabase(Connection connection, AccountsHistoryData record) throws Exception {
		final String sql = "insert into account_history_data (run_date, account_name, account_number, action, symbol, description, commission, fees, amount, settlement_date, activity)" + NL +
				           " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//		String sqlOnDup = " on duplicate key update ... [TBD]

		try (PreparedStatement stmt = connection.prepareStatement(sql)) {
			int index = 0;
			VendoUtils.myAssert(null != record.getRunDate(), "null != record.getRunDate()", null); //do not use Java's assert as it is disabled by default
			stmt.setTimestamp(++index, java.sql.Timestamp.from(record.getRunDate()));
			stmt.setString(++index, record.getAccountName());
			stmt.setString(++index, record.getAccountNumber());
			stmt.setString(++index, record.getAction());
			stmt.setString(++index, record.getSymbol());
			stmt.setString(++index, record.getDescription());
			stmt.setDouble(++index, record.getCommission());
			stmt.setDouble(++index, record.getFees());
			stmt.setDouble(++index, record.getAmount());
			java.sql.Timestamp timestamp = record.getSettlementDate() != null ? java.sql.Timestamp.from(record.getSettlementDate()) : null; //handle null
			stmt.setTimestamp(++index, timestamp);
			FundsEnum.Activity activity = record.getActivity();
			stmt.setString(++index, activity == FundsEnum.Activity.Unspecified ? null : activity.toString()); //don't write 'Unspecified' to DB, leave as null

			stmt.executeUpdate();

		} catch (Exception ex) {
			System.err.println("persistAccountsHistoryDataToDatabase: error persisting record <" + record + ">");
			System.err.println(ex.getMessage());
			return false;
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<JRetirement.AggregateRecord> queryAggregateRecordsFromDatabase(FundsEnum.TaxableType taxableType) {
		String sql = "select downloaded_timestamp, count(*) as records, SUM(value) as total_value" + NL +
					 " from portfolio_positions_data" + NL;
		if (FundsEnum.TaxableType.Unspecified != taxableType) {
			sql += " where taxable_type = '" + taxableType + "'" + NL;
		}
		sql += " group by downloaded_timestamp" + NL +
			   " order by downloaded_timestamp";

		List<JRetirement.AggregateRecord> records = new ArrayList<>();

		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql);
			 ResultSet rs = stmt.executeQuery()) {

			int index = 0;
			while (rs.next()) {
				Timestamp timestamp = rs.getTimestamp("downloaded_timestamp");
				int count = rs.getInt("records");
				Double totalValue = rs.getDouble("total_value");

				JRetirement.AggregateRecord record = new JRetirement.AggregateRecord(++index, timestamp.toInstant(), count, totalValue);
				records.add(record);
			}

		} catch (Exception ex) {
			System.err.println("queryAggregateRecordsFromDatabase: error running sql <" + sql + ">");
			System.err.println(ex.getMessage());
			return records;
		}

		return records;
	}

	///////////////////////////////////////////////////////////////////////////
	public int deleteRecordsFromDatabase(List<Instant> instants) {
		final String sql = "delete from portfolio_positions_data where downloaded_timestamp = ?";

		int totalRowsDeleted = 0;
		try (Connection connection = connectDatabase();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {

			for (Instant instant : instants) {
				stmt.setTimestamp(1, java.sql.Timestamp.from(instant));
				stmt.addBatch();
			}

			int [] rowsDeleted = stmt.executeBatch();
			totalRowsDeleted += Arrays.stream (rowsDeleted).sum ();

		} catch (Exception ex) {
			System.err.println("deleteRecordsFromDatabase: error deleting records <" + instants + ">");
			System.err.println(ex.getMessage());
			return totalRowsDeleted;
		}

		return totalRowsDeleted;
	}

	///////////////////////////////////////////////////////////////////////////
	private Connection connectDatabase () throws Exception {
		//TODO - move connection info to properties file, with hard-coded defaults
		final String jdbcDriver = "com.mysql.cj.jdbc.Driver";
		final String dbUrl = "jdbc:mysql://localhost/retirement";
		final String dbUser = "root";
		final String dbPass = "root";

		Class.forName (jdbcDriver);
		return DriverManager.getConnection (dbUrl, dbUser, dbPass);
	}


	//private members
	private static volatile RetirementDao instance = null;
	private final int skipSmallerAmounts = 500;

	public static final Instant AllDates = Instant.ofEpochSecond(9999); //some fixed, hopefully unique time
	public static final String NL = System.getProperty ("line.separator");
}
