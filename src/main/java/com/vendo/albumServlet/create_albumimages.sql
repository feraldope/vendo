/* Commands to create the "albumimages" database and tables
mysql -u root -proot -e "create database "albumimages"
mysql -u root -proot -e "show databases"
mysql -u root -proot "albumimages" < create_albumimages.sql
mysql -u root -proot albumimages -e "show tables"
mysql -u root -proot albumimages -e "describe images"

REM database files live here
tdir /s /on "C:\Program Files\MariaDB 10.0\data\albumimages"
*/

-- DROP TABLE IF EXISTS imagesX;

CREATE TABLE IF NOT EXISTS images
(
	name_id			INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
	insert_date		TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	sub_folder_int	TINYINT UNSIGNED NOT NULL, -- 1-based: 'a'=1, 'b'=2, etc.
	name_no_ext		VARCHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	bytes			INTEGER UNSIGNED NOT NULL,
	width			SMALLINT UNSIGNED NOT NULL,
	height			SMALLINT UNSIGNED NOT NULL,
	modified		BIGINT UNSIGNED NOT NULL,
	rgb_data		CHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	exifDate0		BIGINT UNSIGNED,
	exifDate1		BIGINT UNSIGNED,
	exifDate2		BIGINT UNSIGNED,
	exifDate3		BIGINT UNSIGNED,
	PRIMARY KEY (name_id, insert_date, name_no_ext, sub_folder_int)
);

-- DROP TABLE IF EXISTS image_folderX;

CREATE TABLE IF NOT EXISTS image_folder
(
	sub_folder_int	TINYINT UNSIGNED NOT NULL, -- 1-based: 'a'=1, 'b'=2, etc.
	last_update		TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (sub_folder_int)
);

-- DROP TABLE IF EXISTS image_countsX;
-- TRUNCATE TABLE image_countsX;

CREATE TABLE IF NOT EXISTS image_counts
(
	name_id			INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
	sub_folder_int	TINYINT UNSIGNED NOT NULL, -- 1-based: 'a'=1, 'b'=2, etc.
	base_name		VARCHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	collapse_groups	TINYINT UNSIGNED NOT NULL,
	image_count		INTEGER UNSIGNED NOT NULL,
	PRIMARY KEY (name_id, sub_folder_int, base_name, collapse_groups, image_count)
);

/* testing
mysql -u root -proot albumimages

-- manual cleanup of image_counts
select * from image_counts where image_count < 1 order by lower(base_name);
delete from image_counts where image_count = 0;

select * from image_counts where base_name like 'Ho%Fu%' order by base_name;
delete from image_counts where base_name like 'Ho%Fu%';

delete from images where name_no_ext like 'xbf%';

-- subquery: first letter of name in lowercase
select lower(substring(base_name,1,1)) from image_counts;
-- subquery: sub_folder_int converted to char
select sub_folder_int, char(sub_folder_int+96 using utf8) from image_counts;

-- ---------------------------------------------------------
-- mismatched entries in image_counts
-- find image files that are in wrong subfolders
select *, lower(substring(base_name,1,1)) as a1, char(sub_folder_int+96 using utf8) as a2 from image_counts having a1 != a2;
select * from image_counts having lower(substring(base_name,1,1)) != char(sub_folder_int+96 using utf8);
-- OR --
select sub_folder_int, char(sub_folder_int+96 using utf8) as sub_folder, t.base_name, t.image_count
from (
select base_name, image_count, sub_folder_int from image_counts having lower(substring(base_name,1,1)) != char(sub_folder_int+96 using utf8)
) as t
--
delete image_counts.*
from image_counts 
inner join (
  select name_id, base_name, sub_folder_int
  from image_counts
  having lower(substring(base_name,1,1)) != char(sub_folder_int+96 using utf8)
) as todelete on todelete.name_id = image_counts.name_id

-- table sizes (number of rows) and max indexes
select 'images' as name, count(*) as rows, max(name_id) as max from images
union all
select 'image_folder' as name, count(*) as rows, '--' as max from image_folder
union all
select 'image_counts' as name, count(*) as rows, max(name_id) as max from image_counts;

-- collapse_groups distribution (note the results will include obsolete entries where image_count = 0)
select collapse_groups, count(*) from image_counts group by collapse_groups;

-- image counts
select * from image_counts where base_name like 'Faa%' order by lower(base_name), collapse_groups;
select * from images where name_no_ext like 'Faa%';
select * from images where insert_date = (select max(insert_date) from images);

-- validate last_update and insert_date handling
select 'image_folder' as name, sub_folder_int, last_update as latest_entry from image_folder where last_update = (select max(last_update) from image_folder)
union all
select 'images' as name, sub_folder_int, insert_date as latest_entry from images where insert_date = (select max(insert_date) from images) group by sub_folder_int

-- inserts
-- insert into images (name_no_ext, sub_folder_int, bytes, width, height, modified, rgb_data) values ('foo', 'f', 347621, 1064, 1600, 1393631218042, 'ff191919ff191919ff1a1a1aff1a1a1aff1c1c1c');

select count(*) as "total" from images order by sub_folder_int;
select count(*) as "total" from images_tmp order by sub_folder_int;
select sub_folder_int, count(*) from images group by sub_folder_int order by sub_folder_int;

-- select CAST((sub_folder_int+CONVERT('a'),UNSIGNED,INTEGER)) as CHAR(1)), count(*) from images group by sub_folder_int order by sub_folder_int;
-- select CONVERT('a',UNSIGNED,INTEGER), count(*) from images group by sub_folder_int order by sub_folder_int;

-- image/folder distribution
select sub_folder_int, count(*) as count from images group by sub_folder_int order by count

-- exifDate distribution
select 'exifDate0' as name, count(*) as rows from images where exifDate0 > 0
union all
select 'exifDate1' as name, count(*) as rows from images where exifDate1 > 0
union all
select 'exifDate2' as name, count(*) as rows from images where exifDate2 > 0
union all
select 'exifDate3' as name, count(*) as rows from images where exifDate3 > 0

-- set all NULL exifDates to 0
select * from images where name_no_ext like 'Ue%';
update images set exifDate0 = 0 where exifDate0 is null and name_no_ext like 'Ue%';
update images set exifDate0 = 0 where exifDate0 is null;
update images set exifDate1 = 0 where exifDate1 is null;
update images set exifDate2 = 0 where exifDate2 is null;
update images set exifDate3 = 0 where exifDate3 is null;

-- mysql equivalent of select 1 from dual
select 1

-- ---------------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE
-- step 1: create new table images_tmp
-- step 2: copy data from images to images_tmp
INSERT INTO images_tmp (name_id, insert_date, sub_folder_int, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3)
				 SELECT name_id, now(),       sub_folder_int, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3
				 FROM images
-- step 3: drop table images
-- step 4: create new table images
-- step 5: copy data from images_tmp to images
INSERT INTO images (name_id, insert_date, sub_folder_int, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3)
			 SELECT name_id, insert_date, sub_folder_int, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3
				 FROM images_tmp
-- step 6: drop table images_tmp

*/