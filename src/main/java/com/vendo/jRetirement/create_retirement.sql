/* Commands to create the "retirement" database and "retirement" table

mysql -u root -proot -e "create database retirement"
mysql -u root -proot -e "show databases"
mysql -u root -proot retirement < create_retirement.sql
mysql -u root -proot retirement -e "show tables"
mysql -u root -proot retirement -e "describe retirement"

REM testing
select * from retirement order by downloaded_timestamp
select count(*) from retirement.retirement
select downloaded_timestamp, count(*) from retirement group by downloaded_timestamp order by downloaded_timestamp

REM cleanup after testing
mysql -u root -proot retirement -e "delete from retirement"

REM recreate table from .sql backup file
mysql -u root -proot retirement < D:\Netscape\Program\retirement.backup.sql
*/

CREATE OR REPLACE TABLE retirement
(
	downloaded_timestamp TIMESTAMP NOT NULL,
	account_number VARCHAR(16) NOT NULL,
	account_name   VARCHAR(32) NOT NULL,
	symbol	       VARCHAR(16) NOT NULL,
	description	   VARCHAR(64) NOT NULL,
	value          DECIMAL(10, 2) NOT NULL, -- enough for $99,999,999.99
	cost_basis     DECIMAL(10, 2) NOT NULL, -- enough for $99,999,999.99
	PRIMARY KEY (downloaded_timestamp, account_number, account_name, symbol) -- prevent duplicates
);

CREATE INDEX downloaded_timestamp_idx on retirement (downloaded_timestamp);
CREATE INDEX account_number_idx       on retirement (account_number);
CREATE INDEX account_name_idx         on retirement (account_name);
CREATE INDEX symbol_idx               on retirement (symbol);

SHOW COLUMNS FROM retirement;
SHOW INDEX FROM retirement;
