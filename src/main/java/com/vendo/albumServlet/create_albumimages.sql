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
	insert_date	TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	sub_folder	VARCHAR(5) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	name_no_ext	VARCHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	bytes			  INTEGER UNSIGNED NOT NULL,
	width			  SMALLINT UNSIGNED NOT NULL,
	height			SMALLINT UNSIGNED NOT NULL,
	modified		BIGINT UNSIGNED NOT NULL,
	rgb_data		CHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	exifDate0		BIGINT UNSIGNED,
	exifDate1		BIGINT UNSIGNED,
	exifDate2		BIGINT UNSIGNED,
	exifDate3		BIGINT UNSIGNED,
	PRIMARY KEY (name_id, insert_date, name_no_ext, sub_folder)
);
CREATE INDEX sub_folder_idx on images (sub_folder);
CREATE INDEX name_no_ext_idx on images (name_no_ext);
SHOW COLUMNS FROM images;
SHOW INDEX FROM images;

-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS image_folder
(
	sub_folder		VARCHAR(5) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	last_update		TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (sub_folder)
);
SHOW COLUMNS FROM image_folder;
SHOW INDEX FROM image_folder;

-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS image_counts
(
	name_id			INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
	sub_folder	VARCHAR(5) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	base_name		VARCHAR(40) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	collapse_groups	TINYINT UNSIGNED NOT NULL,
	image_count		INTEGER UNSIGNED NOT NULL,
	PRIMARY KEY (name_id, sub_folder, base_name, collapse_groups, image_count)
);
CREATE INDEX sub_folder_idx on image_counts (sub_folder);
SHOW COLUMNS FROM image_counts;
SHOW INDEX FROM image_counts;

-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS image_diffs
(
	name_id_1		INTEGER UNSIGNED NOT NULL, -- (name_id_1 < name_id_2) should be true
	name_id_2		INTEGER UNSIGNED NOT NULL,
	avg_diff		TINYINT UNSIGNED NOT NULL, -- max 255
	std_dev 		TINYINT UNSIGNED NOT NULL, -- max 255
	count	  		TINYINT UNSIGNED NOT NULL, -- max 255
	source			VARCHAR(20) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	last_update	TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (name_id_1, name_id_2)
);
CREATE INDEX avg_diff_idx on image_diffs (avg_diff);
CREATE INDEX std_dev_idx on image_diffs (std_dev);
CREATE INDEX source_idx on image_diffs (source);
CREATE INDEX last_update_idx on image_diffs (last_update);
SHOW COLUMNS FROM image_diffs;
SHOW INDEX FROM image_diffs;

/*
-- -----------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE (1)
-- step 1: create new table images_tmp
-- step 2: copy data from images to images_tmp
INSERT INTO images_tmp (name_id, insert_date, sub_folder, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3)
				 SELECT name_id, insert_date, char(sub_folder_int+96 using utf8) as sub_folder, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3
				 FROM images
-- step 3: drop table images
-- step 4: create new table images
-- step 5: copy data from images_tmp to images
INSERT INTO images (name_id, insert_date, sub_folder, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3)
			   SELECT name_id, insert_date, sub_folder, name_no_ext, bytes, width, height, modified, rgb_data, exifDate0, exifDate1, exifDate2, exifDate3
				 FROM images_tmp
-- step 6: drop table images_tmp

-- -----------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE (2)
-- step 1: create new table image_diffs_tmp
-- step 2: copy data from image_diffs to image_diffs_tmp
INSERT INTO image_diffs_tmp (name_id_1, name_id_2, avg_diff, count, source, last_update)
				              SELECT name_id_1, name_id_2, avg_diff, count, source, last_update
				              FROM image_diffs
-- step 3: drop table image_diffs
-- step 4: create new table image_diffs
-- step 5: copy data from image_diffs_tmp to image_diffs
INSERT INTO image_diffs (name_id_1, name_id_2, avg_diff, std_dev, source, count, last_update)
                  SELECT name_id_1, name_id_2, avg_diff, 35, source, count, last_update
                  FROM image_diffs_tmp
-- step 6: drop table image_diffs_tmp

-- -----------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE (3)
-- step 1: create new table image_folder_tmp
-- step 2: copy data from image_folder to image_folder_tmp
INSERT INTO image_folder_tmp (sub_folder, last_update)
				              SELECT char(sub_folder_int+96 using utf8) as sub_folder, last_update
				              FROM image_folder
-- step 3: drop table image_folder
-- step 4: create new table image_folder
-- step 5: copy data from image_folder_tmp to image_folder
INSERT INTO image_folder (sub_folder, last_update)
                  SELECT sub_folder, last_update
                  FROM image_folder_tmp
-- step 6: drop table image_folder_tmp

-- -----------------------------------------------------------------------------
-- ADDING COLUMN TO TABLE (4)
-- step 1: create new table image_counts_tmp
-- step 2: copy data from image_counts to image_counts_tmp
INSERT INTO image_counts_tmp (name_id, sub_folder, base_name, collapse_groups, image_count)
				              SELECT name_id, char(sub_folder_int+96 using utf8) as sub_folder, base_name, collapse_groups, image_count
				              FROM image_counts
-- step 3: drop table image_counts
-- step 4: create new table image_counts
-- step 5: copy data from image_counts_tmp to image_counts
INSERT INTO image_counts (name_id, sub_folder, base_name, collapse_groups, image_count)
                  SELECT name_id, sub_folder, base_name, collapse_groups, image_count
                  FROM image_counts_tmp
-- step 6: drop table image_counts_tmp

-- -----------------------------------------------------------------------------
-- update image folders with new subfolder length
update images set sub_folder = lower(substring(name_no_ext,1,2)) 
where lower(name_no_ext) like 'a%' 
OR lower(name_no_ext) like 'b%'
OR lower(name_no_ext) like 'c%'

-- look for any stragglers
select * from images 
where char_length(sub_folder) != 2

-- update image_counts folders with new subfolder length
update image_counts set sub_folder = lower(substring(base_name,1,2)) 

-- look for any stragglers
delete from image_counts 
-- select * from image_counts 
where char_length(sub_folder) != 2

-- look for any stragglers
delete from image_folder
-- select * from image_counts 
where char_length(sub_folder) != 2

-- -----------------------------------------------------------------------------
-- testing
mysql -u root -proot albumimages

-- size of all databases
SELECT table_schema "DB Name", ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) "DB Size in MB" 
FROM information_schema.tables 
GROUP BY table_schema; 

-- distribution of images in subfolders
-- single-char subfolder
select lower(substring(name_no_ext,1,1)) as sub_folder1, count(*) as count from images group by sub_folder1 order by count desc
-- two-char subfolder
select lower(substring(name_no_ext,1,2)) as sub_folder2, count(*) as count from images group by sub_folder2 order by count desc

-- manual cleanup of image_counts (1)
select * from image_counts where image_count < 1 order by lower(base_name);
delete from image_counts where image_count = 0;

-- manual cleanup of image_counts (2)
-- all basenames with 0 or 1 capital letters
select * from image_counts where base_name rlike '^[A-Za-z]{1}[a-z]+$' order by base_name;
-- delete from image_counts where base_name rlike '^[A-Za-z]{1}[a-z]+$';

delete from image_counts where base_name like 'Ho%Fu%';
-- delete from images where name_no_ext like 'xbf%';

-- subquery: first letter of name in lowercase
select lower(substring(base_name,1,1)) from image_counts;
-- subquery: sub_folder_int converted to char
select sub_folder_int, char(sub_folder_int+96 using utf8) from image_counts;

-- -----------------------------------------------------------------------------
-- mismatched entries in image_counts
-- find image files that are in wrong subfolders
select *, lower(substring(base_name,1,LENGTH(sub_folder))) as a1, sub_folder as a2 from image_counts having a1 != a2;
select * from image_counts having lower(substring(base_name,1,LENGTH(sub_folder))) != sub_folder;
-- OR --
select sub_folder, t.base_name, t.image_count
from (
select base_name, image_count, sub_folder from image_counts having lower(substring(base_name,1,LENGTH(sub_folder))) != sub_folder
) as t
--
delete image_counts.*
from image_counts 
inner join (
  select name_id, base_name, sub_folder
  from image_counts
  having lower(substring(base_name,1,LENGTH(sub_folder))) != sub_folder
) as todelete on todelete.name_id = image_counts.name_id

-- table sizes (number of rows) and max indexes
select 'images' as name, count(*) as rows1, max(name_id) as max from images
union all
select 'image_folder' as name, count(*) as rows1, '--' as max from image_folder
union all
select 'image_counts' as name, count(*) as rows1, max(name_id) as max from image_counts;

-- collapse_groups distribution (note the results will include obsolete entries where image_count = 0)
select collapse_groups, count(*) from image_counts group by collapse_groups;

-- image counts
select * from image_counts where base_name like 'Faa%' order by lower(base_name), collapse_groups;
select * from images where name_no_ext like 'Faa%';
select * from images where insert_date = (select max(insert_date) from images);

-- validate last_update and insert_date handling
select 'image_folder' as name, sub_folder, last_update as latest_entry from image_folder where last_update = (select max(last_update) from image_folder)
union all
select 'images' as name, sub_folder, insert_date as latest_entry from images where insert_date = (select max(insert_date) from images) group by sub_folder

-- inserts
-- insert into images (name_no_ext, sub_folder, bytes, width, height, modified, rgb_data) values ('foo', 'f', 347621, 1064, 1600, 1393631218042, 'ff191919ff191919ff1a1a1aff1a1a1aff1c1c1c');

select count(*) as "total" from images order by sub_folder;
select count(*) as "total" from images_tmp order by sub_folder;
select sub_folder, count(*) from images group by sub_folder 
order by count(*)
-- order by sub_folder;

-- select CAST((sub_folder_int+CONVERT('a'),UNSIGNED,INTEGER)) as CHAR(1)), count(*) from images group by sub_folder_int order by sub_folder_int;
-- select CONVERT('a',UNSIGNED,INTEGER), count(*) from images group by sub_folder_int order by sub_folder_int;

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

select d.* from image_diffs d where d.count > 1

select d.count as count, count(d.count) as rows from image_diffs d group by d.count

select * from image_diffs where name_id_2 < name_id_1

-- query image_diffs table
select i1.name_id, i1.name_no_ext, i2.name_id, i2.name_no_ext, d.avg_diff, source, d.last_update
from image_diffs d
join images i1 on i1.name_id = d.name_id_1
join images i2 on i2.name_id = d.name_id_2
where d.avg_diff < 25 
-- order by i1.name_no_ext, i2.name_no_ext
order by d.last_update desc

-- find all matches in image_diffs table based on name
select RPAD(CONCAT(i1.name_no_ext, ',', i2.name_no_ext, ','), 40, ' ') as 'names',
	d.avg_diff, count, source, d.last_update,
	timestampdiff (hour, curdate(), d.last_update) as hours
from image_diffs d
join images i1 on i1.name_id = d.name_id_1
join images i2 on i2.name_id = d.name_id_2
where d.avg_diff <= 20 and
      (i1.name_no_ext like 'Jo%' or i2.name_no_ext like 'Jo%')
order by count desc

-- image_diffs cleanup
xx delete from image_diffs where avg_diff >= 19
xx delete from image_diffs where timestampdiff (hour, last_update, current_timestamp) > 1

select distinct name from albumtags.base1_names where name_id in (
select name_id from (
(select name_id from albumtags.base1_names_tags
inner join albumtags.tags on base1_names_tags.tag_id = tags.tag_id
where tags.tag_id = (
select tag_id from albumtags.tags
where tag = 'bumble'
)) order by rand() limit 500) t)

-- query image_diffs table for AlbumImages.doDups, with orientation test
set @row_number := 0;
select @row_number := @row_number + 1 as row,
	RPAD(CONCAT(i1.name_no_ext, ',', i2.name_no_ext, ','), 42, ' ') as 'names                                   ',
	d.avg_diff as avg,
	d.source as 'source',
	d.count as 'count',
	d.last_update as 'last update     ',	
	timestampdiff (hour, d.last_update, current_timestamp()) as hours,
	case
	when cast(i1.width as int) - cast(i1.height as int) > 10 then 'L'
	when cast(i1.height as int) - cast(i1.width as int) > 10 then 'P'
	else 'S' end as i1_orient,
	case
	when cast(i2.width as int) - cast(i2.height as int) > 10 then 'L'
	when cast(i2.height as int) - cast(i2.width as int) > 10 then 'P'
	else 'S' end as i2_orient
from image_diffs d
join images i1 on i1.name_id = d.name_id_1
join images i2 on i2.name_id = d.name_id_2
where d.avg_diff <= 15 and -- MAX_RGB_DIFF and
	timestampdiff (hour, d.last_update, current_timestamp) <= 12 -- and -- SINCE_HOURS
  having strcmp(i1_orient, i2_orient) = 0
-- order by avg_diff, i1.name_no_ext, i2.name_no_ext
-- order by avg desc, i1.name_no_ext
order by d.last_update

-- number of rows with mismatched orientation vs all rows
select count(*) from (
select 
d.name_id_1,
d.name_id_2,
d.last_update,
	case
	when cast(i1.width as int) - cast(i1.height as int) > 10 then 'L'
	when cast(i1.height as int) - cast(i1.width as int) > 10 then 'P'
	else 'S' end as i1o,
	case
	when cast(i2.width as int) - cast(i2.height as int) > 10 then 'L'
	when cast(i2.height as int) - cast(i2.width as int) > 10 then 'P'
	else 'S' end as i2o
from image_diffs d
join images i1 on i1.name_id = d.name_id_1
join images i2 on i2.name_id = d.name_id_2
where d.avg_diff < 25
having strcmp(i1o, i2o) != 0
order by d.last_update
) as temp
union all
select count(*) from image_diffs

-- -----------------------------------------------------------------------------
-- count distribution (includes rows that no longer have existing images)
select 'Total' as count, count(count) as rows1 from image_diffs
union all 
select count as count, count(count) as rows1 from image_diffs group by count desc

-- avg_diff distribution (includes rows that no longer have existing images)
select 'Total' as diff, count(avg_diff) as rows1 from image_diffs
union all 
select avg_diff as diff, count(avg_diff) as rows1 from image_diffs group by avg_diff 

-- image_diffs date ranges
select 'Min' as name, min(last_update) as last_update from image_diffs
union all 
select 'Max' as name, max(last_update) as last_update from image_diffs

-- -----------------------------------------------------------------------------
-- find albums that have a high number of images and a large byte size (and optionally EXIF date)
select i.base_name, 
 round(sum(i.mbytes), 1) as total_mbytes, 
 ic.image_count, 
 round(sum(i.mbytes) / ic.image_count, 1) as avg_mbytes,
 count(i.mbytes) as number_over_size_threshold, 
 round(100 * count(i.mbytes) / ic.image_count, 1) as percent_over_size_threshold
 -- i.exif
from (
 select left(name_no_ext, locate('-', name_no_ext) - 1) as base_name, bytes / (1024 * 1024) as mbytes, 
  ((exifDate0 + exifDate1 + exifDate2 + exifDate3) > 0) as has_exif
 from images
 -- where (exifDate0 > 0 or exifDate1 > 0 or exifDate2 > 0 or exifDate3 > 0)
) as i
join image_counts ic on ic.base_name = i.base_name
where ic.image_count > 70
and ic.base_name not like 'ama%'
and ic.base_name not like 'q%'
-- and ic.base_name not like 's%'
and ic.base_name not like 'x%'
group by base_name
-- order by base_name
-- order by total_mbytes desc
-- order by total_mbytes desc, number_over_size_threshold desc, image_count desc
order by avg_mbytes desc, total_mbytes desc, number_over_size_threshold desc, image_count desc

-- -----------------------------------------------------------------------------
-- "remove all numeric characters from column mysql"
-- original from https://stackoverflow.com/questions/11431831/remove-all-numeric-characters-from-column-mysql
DROP FUNCTION IF EXISTS alphas; 
DELIMITER | 
CREATE FUNCTION alphas( str CHAR(64) ) RETURNS CHAR(64) 
BEGIN 
  DECLARE i, len SMALLINT DEFAULT 1; 
  DECLARE ret CHAR(64) DEFAULT ''; 
  DECLARE c CHAR(1); 
  SET len = CHAR_LENGTH( str ); 
  REPEAT 
    BEGIN 
      SET c = MID( str, i, 1 ); 
      IF c REGEXP '[[:alpha:]]' THEN 
        SET ret=CONCAT(ret,c); 
      END IF; 
      SET i = i + 1; 
    END; 
  UNTIL i > len END REPEAT; 
  RETURN ret; 
-- END | 
END;
DELIMITER ; 

SELECT alphas('123ab45cde6789fg0000000000000000000000000000'); 
+----------------------------+ 
| alphas('123ab45cde6789fg') | 
+----------------------------+ 
| abcdefg                    | 
+----------------------------+ 

*/