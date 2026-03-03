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

/*
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

*/
