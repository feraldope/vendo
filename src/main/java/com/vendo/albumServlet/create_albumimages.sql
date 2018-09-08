/* Commands to create the "albumimages" database and tables
mysql -u root -proot -e "create database "albumimages"
mysql -u root -proot -e "show databases"
mysql -u root -proot "albumimages" < create_albumimages.sql
mysql -u root -proot albumimages -e "show tables"
mysql -u root -proot albumimages -e "describe images"

REM database files live here
tdir /s /on "C:\Program Files\MariaDB 10.0\data\albumimages"
tdir /s /on "F:\MariaDB 10.0\data\albumimages"
*/

-- -----------------------------------------------------------------------------
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
CREATE INDEX sub_folder_int_idx on images (sub_folder_int);
CREATE INDEX name_no_ext_idx on images (name_no_ext);
SHOW COLUMNS FROM images;
SHOW INDEX FROM images;

-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS image_folder
(
	sub_folder_int	TINYINT UNSIGNED NOT NULL, -- 1-based: 'a'=1, 'b'=2, etc.
	last_update		TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (sub_folder_int)
);
SHOW COLUMNS FROM image_folder;
SHOW INDEX FROM image_folder;

-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS image_counts
(
	name_id			INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
	sub_folder_int	TINYINT UNSIGNED NOT NULL, -- 1-based: 'a'=1, 'b'=2, etc.
	base_name		VARCHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	collapse_groups	TINYINT UNSIGNED NOT NULL,
	image_count		INTEGER UNSIGNED NOT NULL,
	PRIMARY KEY (name_id, sub_folder_int, base_name, collapse_groups, image_count)
);
CREATE INDEX sub_folder_int_idx on image_counts (sub_folder_int);
SHOW COLUMNS FROM image_counts;
SHOW INDEX FROM image_counts;

-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS image_diffs
(
	name_id_1		INTEGER UNSIGNED NOT NULL, -- (name_id_1 < name_id_2) should be true
	name_id_2		INTEGER UNSIGNED NOT NULL,
	avg_diff		TINYINT UNSIGNED NOT NULL, -- max 255
	max_diff		TINYINT UNSIGNED NOT NULL, -- max 255
	last_update	TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (name_id_1, name_id_2)
);
CREATE INDEX avg_diff_idx on image_diffs (avg_diff);
CREATE INDEX max_diff_idx on image_diffs (max_diff);
CREATE INDEX last_update_idx on image_diffs (last_update);
SHOW COLUMNS FROM image_diffs;
SHOW INDEX FROM image_diffs;

-- -----------------------------------------------------------------------------
/* testing
mysql -u root -proot albumimages

-- size of all databases
SELECT table_schema "DB Name", ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) "DB Size in MB" 
FROM information_schema.tables 
GROUP BY table_schema; 

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

-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE (1)
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

-- -----------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE (2)
-- step 1: create new table image_diffs_tmp
-- step 2: copy data from image_diffs to image_diffs_tmp
INSERT INTO image_diffs_tmp (name_id_1, name_id_2, avg_diff, max_diff, last_update)
				              SELECT name_id_1, name_id_2, avg_diff, max_diff, now()
				              FROM image_diffs
-- step 3: drop table image_diffs
-- step 4: create new table image_diffs
-- step 5: copy data from image_diffs_tmp to image_diffs
INSERT INTO image_diffs (name_id_1, name_id_2, avg_diff, max_diff, last_update)
                  SELECT name_id_1, name_id_2, avg_diff, max_diff, last_update
                  FROM image_diffs_tmp
-- step 6: drop table image_diffs_tmp

-- -----------------------------------------------------------------------------
-- select random rows from images table

select name_id, name_no_ext from images where name_id in 
    (select name_id from (select name_id from images 
    where (name_no_ext like 'se%' or name_no_ext like 'ba%')
    order by rand() limit 10000) t)
    
select name_id, name_no_ext from images where name_id in 
    (select name_id from (select name_id from images 
    where (name_no_ext not like 'q%' and name_no_ext not like 'x%' and name_no_ext not like 'se%' and name_no_ext not like 'ba%' )
    order by rand() limit 10000) t)

-- -----------------------------------------------------------------------------
-- queries for image_diffs table
select count(*) from image_diffs

select * from image_diffs where name_id_2 < name_id_1
select * from image_diffs where avg_diff < max_diff 

select i1.name_id, i1.name_no_ext, i2.name_id, i2.name_no_ext, d.avg_diff, d.max_diff, d.last_update
from image_diffs d
join images i1 on i1.name_id = d.name_id_1
join images i2 on i2.name_id = d.name_id_2
-- where d.avg_diff < d.max_diff 
-- order by i1.name_no_ext, i2.name_no_ext
order by d.last_update desc

-- find all matches in image_diffs table based on name
select RPAD(CONCAT(i1.name_no_ext, ',', i2.name_no_ext, ','), 40, ' ') as 'names',
	d.avg_diff, d.max_diff, d.last_update,
	timestampdiff (hour, curdate(), d.last_update) as hours
from image_diffs d
join images i1 on i1.name_id = d.name_id_1
join images i2 on i2.name_id = d.name_id_2
where d.avg_diff < d.max_diff and
	    d.avg_diff <= 20 and
      (i1.name_no_ext like 's4679%' or i2.name_no_ext like 's4679%')
order by i1.name_no_ext

--
xx delete from image_diffs where avg_diff <= 12
xx delete from image_diffs where max_diff = 30

select distinct name from albumtags.base1_names where name_id in (
select name_id from (
(select name_id from albumtags.base1_names_tags
inner join albumtags.tags on base1_names_tags.tag_id = tags.tag_id
where tags.tag_id = (
select tag_id from albumtags.tags
where tag = 'bumble'
)) order by rand() limit 500) t)

*/