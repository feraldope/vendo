/* Commands to create the "retirement" database and "retirement" table

mysql -u root -proot -e "create database retirement"
mysql -u root -proot -e "show databases"
mysql -u root -proot retirement < create_retirement.sql
mysql -u root -proot retirement -e "show tables"
mysql -u root -proot retirement -e "describe retirement"

REM testing
select * from portfolio_positions_data order by downloaded_timestamp, account_number
select count(*) from portfolio_positions_data

select * from portfolio_positions_data where symbol = 'VGHCX' order by downloaded_timestamp, account_number

-- query all 'Pending Activity'
select downloaded_timestamp,
symbol,
-- description,
SUM(value), count(*)
from portfolio_positions_data
where symbol like 'Pending%'
-- where symbol = 'VGHAX'
-- where symbol = 'VIG'
-- where (description like '%Money%' OR symbol like '%Pending%')
group by downloaded_timestamp, symbol
order by downloaded_timestamp, symbol

REM cleanup after testing
mysql -u root -proot retirement -e "delete from portfolio_positions_data"
mysql -u root -proot retirement -e "delete from account_history_data"
REM delete this last because of FOREIGN_KEYs
mysql -u root -proot retirement -e "delete from funds_meta_data"

REM recreate tables from .sql backup file
mysql -u root -proot retirement < D:\Netscape\Program\retirement.backup.sql

-- drop table portfolio_positions_data
-- drop table account_history_data
*/

-- -----------------------------------------------------------------------------
CREATE OR REPLACE TABLE funds_meta_data (
    symbol           VARCHAR(24) PRIMARY KEY, -- prevents duplicates, creates index, allows foreign key
    fund_family      VARCHAR(24) NOT NULL,
    description      VARCHAR(64) NOT NULL,
	expense_ratio    DECIMAL(8, 4) NOT NULL, -- enough for $9,999.9999
    fund_theme       VARCHAR(24) NOT NULL,
    fund_type        VARCHAR(24) NOT NULL,
    management_style VARCHAR(24) NOT NULL,
    category         VARCHAR(24) NOT NULL,
    investment_style VARCHAR(24) NOT NULL
);

-- CREATE INDEX symbol_idx on funds_meta_data (symbol); -- don't need this if primary key only has symbol as it creates an index

SHOW COLUMNS FROM funds_meta_data;
SHOW INDEX FROM funds_meta_data;

-- -----------------------------------------------------------------------------
CREATE OR REPLACE TABLE portfolio_positions_data (
	downloaded_timestamp TIMESTAMP NOT NULL,
	account_number VARCHAR(16) NOT NULL,
	account_name   VARCHAR(32) NOT NULL,
	symbol	       VARCHAR(16) NOT NULL,
	description	   VARCHAR(64) NOT NULL,
	value          DECIMAL(10, 2) NOT NULL, -- enough for $99,999,999.99
	cost_basis     DECIMAL(10, 2) NOT NULL, -- enough for $99,999,999.99
	taxable_type   VARCHAR(16) NOT NULL,
	fund_owner     VARCHAR(8) NOT NULL,
	PRIMARY KEY (downloaded_timestamp, account_number, account_name, symbol), -- prevent duplicates, creates composite index
    CONSTRAINT fk_pp_symbol FOREIGN KEY (symbol) REFERENCES funds_meta_data(symbol) -- constraint name must be unique across all tables
);

CREATE INDEX downloaded_timestamp_idx on portfolio_positions_data (downloaded_timestamp);
CREATE INDEX account_number_idx       on portfolio_positions_data (account_number);
CREATE INDEX account_name_idx         on portfolio_positions_data (account_name);
CREATE INDEX symbol_idx               on portfolio_positions_data (symbol);
CREATE INDEX fund_owner               on portfolio_positions_data (fund_owner);

SHOW COLUMNS FROM portfolio_positions_data;
SHOW INDEX FROM portfolio_positions_data;

-- show foreign keys
SELECT * FROM information_schema.TABLE_CONSTRAINTS
 WHERE information_schema.TABLE_CONSTRAINTS.CONSTRAINT_TYPE = 'FOREIGN KEY'
 AND information_schema.TABLE_CONSTRAINTS.TABLE_SCHEMA = 'retirement'

-- show foreign keys
SELECT * FROM   INFORMATION_SCHEMA.KEY_COLUMN_USAGE
 WHERE CONSTRAINT_SCHEMA = 'retirement'

-- -----------------------------------------------------------------------------
CREATE OR REPLACE TABLE account_history_data (
	run_date        TIMESTAMP NOT NULL,
	account_name    VARCHAR(32) NOT NULL,
	account_number  VARCHAR(16) NOT NULL,
	action	        VARCHAR(128) NOT NULL,
	symbol	        VARCHAR(16) NOT NULL,
	description	    VARCHAR(64) NOT NULL,
	commission      DECIMAL(8, 2), -- enough for $999,999.99
	fees            DECIMAL(8, 2), -- enough for $999,999.99
	amount          DECIMAL(10, 2) NOT NULL, -- enough for $99,999,999.99
	settlement_date TIMESTAMP,
	activity        VARCHAR(16),
	PRIMARY KEY (run_date, account_number, action, symbol, amount), -- prevent duplicates, creates composite index
    CONSTRAINT fk_ah_symbol FOREIGN KEY (symbol) REFERENCES funds_meta_data(symbol) -- constraint name must be unique across all tables
);

CREATE INDEX run_date_idx       on account_history_data (run_date);
CREATE INDEX account_number_idx on account_history_data (account_number);
CREATE INDEX symbol_idx         on account_history_data (symbol);

SHOW COLUMNS FROM account_history_data;
SHOW INDEX FROM account_history_data;

-- show foreign keys
SELECT * FROM information_schema.TABLE_CONSTRAINTS
 WHERE information_schema.TABLE_CONSTRAINTS.CONSTRAINT_TYPE = 'FOREIGN KEY'
 AND information_schema.TABLE_CONSTRAINTS.TABLE_SCHEMA = 'retirement'

-- show foreign keys
SELECT * FROM   INFORMATION_SCHEMA.KEY_COLUMN_USAGE
 WHERE CONSTRAINT_SCHEMA = 'retirement'

-- -----------------------------------------------------------------------------
-- view with all raw portfolio data
CREATE OR REPLACE VIEW data_view AS
SELECT p.downloaded_timestamp as time,
--     p.account_number,
	   p.account_name,
--	   p.symbol,
       case
          when f.category = 'CD' then f.category
          when p.symbol = '31617E778' then p.description
          when p.symbol = '857444624' then p.description
          when p.symbol = '85744A687' then p.description
          when p.symbol = '857480552' then p.description
          else p.symbol
       end as symbol,
       p.description,
       case
          when f.category = 'CD' then CONCAT(f.category, ': ', f.description)
          else CONCAT(p.symbol, ': ', f.description)
       end as symbol_description,
	   p.value,
--	   p.cost_basis,
	   p.taxable_type,
	   p.fund_owner,
--     f.symbol,
       f.fund_family,
--     f.description,
--	   f.expense_ratio,
       f.fund_theme,
       f.fund_type,
       f.management_style,
       f.category,
       f.investment_style,       
       case
          when f.fund_type = 'BondETF' then 'Bond'
          when f.fund_type = 'BondFund' then 'Bond'
          when f.fund_type = 'Cash' then 'Cash'
          when f.fund_type = 'Crypto' then 'Equity'
          when f.fund_type = 'StockETF' then 'Equity'
          when f.fund_type = 'StockFund' then 'Equity'
          else 'NotRecognized'
       end as allocation_type
FROM funds_meta_data f
JOIN portfolio_positions_data p on p.symbol = f.symbol
WHERE p.symbol != 'Pending Activity';

-- select * from data_view

-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW percentage_view_taxable_type AS
WITH total_table as (
 SELECT time, sum(value) as total
 FROM data_view
 GROUP BY time
) SELECT d.time, d.taxable_type, sum(d.value) AS sub_total, t.total, 100 * sum(d.value) / t.total as percentage
FROM total_table t
JOIN data_view d on d.time = t.time
GROUP BY d.time, d.taxable_type
ORDER BY d.time, d.taxable_type;

-- select * from percentage_view_taxable_type

-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW percentage_view_allocation_type AS
WITH total_table as (
 SELECT time, sum(value) as total
 FROM data_view
 GROUP BY time
) SELECT d.time, d.allocation_type, sum(d.value) AS sub_total, t.total, 100 * sum(d.value) / t.total as percentage
FROM total_table t
JOIN data_view d on d.time = t.time
GROUP BY d.time, d.allocation_type
ORDER BY d.time, d.allocation_type;

-- select * from percentage_view_allocation_type

/*
-- -----------------------------------------------------------------------------
select count(*) from data_view

select * from data_view
-- where date(time) >= '2026-01-01'
 where date(time) = '2026-04-02'
-- and category = 'CD'
 order by time desc, account_name, symbol

-- query to investigate removing PendingActivity from view
select distinct symbol, fund_family from data_view where (symbol = 'Pending Activity' OR fund_family = 'PendingActivity')
select * from data_view where symbol = 'Pending Activity' and fund_family != 'PendingActivity'

-- -----------------------------------------------------------------------------
-- trying to fix timezone issue in grafana
-- See https://stackoverflow.com/questions/930900/how-do-i-set-the-time-zone-of-mysql
select now()
 , @@session.time_zone
 , UTC_TIMESTAMP
 , UTC_TIMESTAMP()
 , TIMEDIFF(NOW(), UTC_TIMESTAMP())

-- NOTE (see also todo.txt): I had to "fix" this in grafana: set "Time Zone" = "Coordinated Universal Time" in Dashboard->Settings to get times to at least have the correct date

-- -----------------------------------------------------------------------------
-- grafana query doesn't work in grafana but works fine here
SELECT $__timeGroup(time,$__interval,previous) AS "time", fund_theme, SUM(value) AS "value" FROM retirement.data_view GROUP BY $__timeGroup(time, $__interval), fund_theme ORDER BY $__timeGroupAlias(time, $__interval)
SELECT time AS "time", fund_theme, SUM(value) AS "value" FROM retirement.data_view GROUP BY time, fund_theme ORDER BY time

-- example that works?
SELECT $__timeGroup(time,$__interval,previous) , fund_theme, SUM(value) AS "value"
FROM retirement.data_view
GROUP BY $__timeGroup(time, $__interval), fund_theme
ORDER BY $__timeGroup(time, $__interval) , fund_theme

-- if you get this error:
db query error: Error 1064 (42000): You have an error in your SQL syntax; check the manual that corresponds to your MariaDB server version for the right syntax to use near 'AS "time"' at line 1
-- confirm that all of the uses of $__timeGroupXxx in the editor are the same (not mixed, like $__timeGroup and $__timeGroupAlias

-- -----------------------------------------------------------------------------
select count(*) from funds_meta_data

select distinct symbol from funds_meta_data order by symbol

select * from funds_meta_data order by fund_family, symbol

-- -----------------------------------------------------------------------------
select count(*) from portfolio_positions_data;

-- summary
select downloaded_timestamp, count(*), format(sum(value) / 1000, 0) as 'Value $K',
 case
    when cost_basis = 0 then 'unknown'
    else format((sum(value) - sum(cost_basis)) / 1000, 0)
 end as 'Gain  $K'
 from portfolio_positions_data
 group by downloaded_timestamp
 order by downloaded_timestamp

select * from portfolio_positions_data where fund_owner = 'unknown'
select distinct account_number, symbol, account_name, description from portfolio_positions_data where fund_owner = 'unknown'

select count(distinct downloaded_timestamp) from portfolio_positions_data;

select distinct taxable_type from portfolio_positions_data order by taxable_type;
select distinct account_name from portfolio_positions_data order by account_name;

select date(p.downloaded_timestamp) as date, p.* from portfolio_positions_data p
 where date(p.downloaded_timestamp) >= '2026-02-18'
 order by p.symbol, p.account_name

select * from portfolio_positions_data where account_name = 'ROTH IRA' and value = 7500

select * from portfolio_positions_data where date(downloaded_timestamp) = '2026-06-04'
delete from portfolio_positions_data where date(downloaded_timestamp) = '2026-06-04'

-- -----------------------------------------------------------------------------
select count(*) from account_history_data;

select count(distinct run_date) from account_history_data;

select distinct account_name from account_history_data order by account_name;

select date(a.run_date) as date, a.* from account_history_data a
-- where date(a.run_date) >= '2026-02-04'
 order by a.symbol, a.account_name

select * from account_history_data where lower(action) rlike '.*bought.*'

select * from account_history_data where (ABS(fees) > 0 OR ABS(commission) > 0) OR (action rlike '.*FEE.*' AND ABS(amount) > 0) order by run_date

-- get list of all 'interesting' actions
select action, count(amount), min(cast(run_date as date)), max(cast(run_date as date)), sum(amount)
 from account_history_data
 where action not rlike 'DIVIDEND.*'
  and action not rlike 'Change in Market.*'
  and action not rlike 'COMMISSION CREDIT.*'
  and action not rlike 'Exchange In.*'
  and action not rlike 'Exchange Out.*'
  and action not rlike 'INTEREST.*'
  and action not rlike 'LONG-TERM CAP GAIN.*'
  and action not rlike 'RECEIVED FROM YOU.*'
  and action not rlike 'RECORDKEEPING FEE.*'
  and action not rlike 'REINVESTMENT.*'
  and action not rlike 'REVENUE CREDIT.*'
  and action not rlike 'ROLLOVER CASH.*'
  and action not rlike 'ROLLOVER SHARES.*'
  and action not rlike 'SHORT-TERM CAP GAIN.*'
  and action not rlike 'TERMINATED MAINTENANCE.*'
  and action not rlike 'TFR OF ASSETS.*'
  and action not rlike 'TRANSFER OF ASSETS.*'
  and action not rlike 'TRANSFERRED FROM.*'
  and action not rlike 'YOU BOUGHT.*'
  and action not rlike 'YOU SOLD.*'
 group by action
 order by action

select * from account_history_data
 where activity is not null
-- and activity = 'Redemption'
 order by run_date

-- original query from queryDistributionDataFromDatabase()
-- select run_date, account_name, account_number, action, symbol, description, SUM(commission) as commission, SUM(fees) as fees, SUM(amount) as amount, settlement_date, activity
select run_date, account_name, account_number, action, symbol, description, (commission) as commission, (fees) as fees, (amount) as amount, settlement_date, activity
 from account_history_data
-- where (UPPER(action) rlike '.*DISTR.*' OR UPPER(action) rlike '.*TAX.*') -- NOTE: rlike is not case-sensitive unless used on binary string
 where activity = 'Distribution'
 and ABS(amount) >= 10 -- /skip smaller transactions
 and run_date >= '2026-01-01'
-- group by run_date, account_name, account_number, symbol, description
 order by run_date

select * from account_history_data where run_date = '2026-04-21'

-- cleanup (you probably also need to delete the source Accounts_History_xxx.csv file that had the 'bad' entry)
select * from account_history_data where run_date = '2026-04-29'
delete from account_history_data where run_date = '2026-06-04'

select * from account_history_data where run_date = '2024-07-29'

-- obsolete after adding 'activity' column
    -- contributions
    select * from account_history_data
     where upper(action) rlike '.*CONTR.*'
     and account_name not rlike 'FIS.*'
     order by run_date

    -- distributions - detail
    select * from account_history_data
     where (upper(action) rlike '.*DISTR.*' OR upper(action) rlike '.*TAX.*')
     and amount < 0
     order by run_date

    -- distributions - aggregate (Note distributions are already negative in the database)
    select run_date, account_name, account_number, action, symbol, description, SUM(commission), SUM(fees), SUM(amount), settlement_date
     from account_history_data
     where (UPPER(action) rlike '.*DISTR.*' OR UPPER(action) rlike '.*TAX.*')
     and ABS(amount) > 500
     group by run_date, account_name, account_number, symbol, description
     order by run_date

    -- redemptions - aggregate (Note redemptions are not negative in the database, so we need to negate them)
    select run_date, account_name, account_number, action, symbol, description, SUM(commission) as commission, SUM(fees) as fees, (-1) * SUM(amount) as amount, settlement_date
     from account_history_data
    -- where UPPER(action) rlike 'REDEMPTION FROM CORE ACCOUNT.*'
     where UPPER(action) rlike 'REDEMPTION.*'
     and account_name = 'Individual - TOD'
     and ABS(amount) > 500
     group by run_date, account_name, account_number, symbol, description
     order by run_date

-- -----------------------------------------------------------------------------
-- BUG: percentage_view_taxable_type shows over 100% for some dates (e.g., '2026-01-05')
-- CAUSE: it was because of rows in portfolio_positions_data that had symbol = 'Pending Activity'
-- FIXED: changed percentage_view_taxable_type to query data_view as it already filters these records

select time, sum(percentage) 
from percentage_view_taxable_type
group by time
-- having sum(percentage) != 100
having sum(percentage) < 99.9999 OR sum(percentage) > 100.0001
order by sum(percentage) desc

select time, taxable_type, sub_total, total, percentage from percentage_view_taxable_type
where DATE(time) between '2026-01-05' and  '2026-01-05'
UNION ALL
select time, 'Total:' as taxable_type, sum(sub_total) as sub_total, '--' as total, sum(percentage) as percentage from percentage_view_taxable_type
where DATE(time) between '2026-01-05' and  '2026-01-05'

select * from portfolio_positions_data
where DATE(downloaded_timestamp) = '2026-01-05'

select * from portfolio_positions_data
where symbol = 'Pending Activity'
and DATE(downloaded_timestamp) >= '2026-01-01'
order by downloaded_timestamp, account_number, symbol

-- -----------------------------------------------------------------------------
-- Warning: found duplicate dates in PortfolioPositionData:
-- 2026-05-20=[05/20/26 09:33:00, 05/20/26 09:35:00, 05/20/26 10:11:00]
-- GO AHEAD AND DELETE ALL FOR THIS DATE, THEN RERUN jret.bat (which will reload latest file)
select * from portfolio_positions_data where DATE(downloaded_timestamp) = '2026-05-20'
-- delete from portfolio_positions_data where DATE(downloaded_timestamp) = '2026-05-20'

-- -----------------------------------------------------------------------------

*/
