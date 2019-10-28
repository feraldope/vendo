/* Commands to create the "gihistory" database and "history" table

mysql -u root -proot -e "create database gihistory"
mysql -u root -proot -e "show databases"
mysql -u root -proot gihistory < create_gihistory.sql
mysql -u root -proot gihistory -e "show tables"
mysql -u root -proot gihistory -e "describe history"

REM testing
select * from history order by insert_date
select * from history where timestampdiff (day, insert_date, curdate()) < 1 order by insert_date
select * from history order by url
select * from history where args is not null order by url
select * from history where step is not null order by url
select * from history where step > 0 order by url
select * from history where start_index > 0 order by url
select * from history where end_index > 0 order by url

select count(*) from gihistory.history

REM cleanup after testing
mysql -u root -proot gihistory -e "delete from history where url like 'http%://google.com%'"

REM restore table from .sql backup file
mysql -u root -proot gihistory < D:\Netscape\Program\gihistory.backup.sql
*/

CREATE TABLE history
(
	insert_date	TIMESTAMP NOT NULL,
	url			VARCHAR(256) NOT NULL,
	args		VARCHAR(32),
	start_index	INTEGER,
	end_index	INTEGER,
	step		INTEGER
);
