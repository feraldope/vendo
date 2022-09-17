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
CREATE INDEX name_id_idx on images (name_id);
CREATE INDEX sub_folder_idx on images (sub_folder);
CREATE INDEX name_no_ext_idx on images (name_no_ext);
CREATE INDEX insert_date_idx on images (insert_date);
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
	count	  		SMALLINT UNSIGNED NOT NULL, -- max 65535
	source			VARCHAR(20) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	last_update	TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (name_id_1, name_id_2)
);
CREATE INDEX name_id_1_idx on image_diffs (name_id_1);
CREATE INDEX name_id_2_idx on image_diffs (name_id_2);
CREATE INDEX avg_diff_idx on image_diffs (avg_diff);
CREATE INDEX std_dev_idx on image_diffs (std_dev);
CREATE INDEX source_idx on image_diffs (source);
CREATE INDEX last_update_idx on image_diffs (last_update);
SHOW COLUMNS FROM image_diffs;
SHOW INDEX FROM image_diffs;
-- ALTER TABLE image_diffs MODIFY count SMALLINT UNSIGNED NOT NULL;

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

-- counts of all* databases
select count(*) as count, 'image_counts' as table_name from albumimages.image_counts
union all
select count(*) as count, 'image_diffs' as table_name from albumimages.image_diffs
union all
select count(*) as count, 'image_folder' as table_name from albumimages.image_folder
union all
select count(*) as count, 'images' as table_name from albumimages.images


-- images size distribution by 1-char subfolder (for determining which folders to move from D: to C:) (SEARCH TODO.TXT for mklink, AlbumServlet)
select lower(substring(name_no_ext,1,1)) as sub_folder1, sum(bytes) / (1024 * 1024 *1024) as giga_bytes, count(*) as count
from images
group by sub_folder1
order by giga_bytes desc

-- size and count of image folders (actual subfolders) (NOTE does not account for drive's block size; just adds raw bytes)
select sub_folder, sum(bytes)/(1024*1024*1024) as GBytes, count(*) as count from images group by sub_folder order by count desc
select sub_folder, sum(bytes)/(1024*1024*1024) as GBytes, count(*) as count from images group by sub_folder order by GBytes desc
--total size of all folders: add padding to files size, plus size of .dat file)
select (sum(bytes * 1.05) + 10500)/(1024*1024*1024) as GBytes, count(*) as count from images order by GBytes desc

-- distribution of images in actual subfolders
select lower(substring(name_no_ext,1,2)) as sub_folder2, sub_folder as sub_folder, count(*) as count from images group by sub_folder2, sub_folder order by count desc
select lower(substring(name_no_ext,1,2)) as sub_folder2, sub_folder as sub_folder, count(*) as count from images group by sub_folder2, sub_folder order by sub_folder2

-- distribution of images in 1-, 2-, and 3-char subfolders
-- single-char subfolder (NOTE SORT ASCENDING: for single char we want to see which folders are least used)
select lower(substring(name_no_ext,1,1)) as sub_folder1, count(*) as count from images group by sub_folder1 order by count asc
-- two-char subfolder
select lower(substring(name_no_ext,1,2)) as sub_folder2, count(*) as count from images group by sub_folder2 order by count desc
-- three-char subfolder
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images group by sub_folder3 order by count desc
-- four-char subfolder
select lower(substring(name_no_ext,1,4)) as sub_folder4, count(*) as count from images group by sub_folder4 order by count desc
-- five-char subfolder
select lower(substring(name_no_ext,1,5)) as sub_folder5, count(*) as count from images group by sub_folder5 order by count desc

-- WHEN a subfolder shows up at the top of the previous query, see how it needs to be split
-- !!! distribution of images in set of subfolders ('ka', 'ni', etc.)
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'ka%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'ni%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'se%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'sa%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'an%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'mi%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'ch%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'an%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'ja%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'li%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'al%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'lu%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'da%' group by sub_folder3 order by count desc
select lower(substring(name_no_ext,1,3)) as sub_folder3, count(*) as count from images where lower(name_no_ext) like 'na%' group by sub_folder3 order by count desc

--STOP Tomcat8 SERVICE FIRST
--FIRST update DB
--move to three-char subfolder
update images set sub_folder = 'kat' where lower(name_no_ext) like 'kat%'
update images set sub_folder = 'kar' where lower(name_no_ext) like 'kar%'
update images set sub_folder = 'mar' where lower(name_no_ext) like 'mar%'
update images set sub_folder = 'jen' where lower(name_no_ext) like 'jen%'
update images set sub_folder = 'car' where lower(name_no_ext) like 'car%'
update images set sub_folder = 'ang' where lower(name_no_ext) like 'ang%'
update images set sub_folder = 'ale' where lower(name_no_ext) like 'ale%'
update images set sub_folder = 'nik' where lower(name_no_ext) like 'nik%'
update images set sub_folder = 'jes' where lower(name_no_ext) like 'jes%'
update images set sub_folder = 'sex' where lower(name_no_ext) like 'sex%'
update images set sub_folder = 'sar' where lower(name_no_ext) like 'sar%'
update images set sub_folder = 'sam' where lower(name_no_ext) like 'sam%'
update images set sub_folder = 'mic' where lower(name_no_ext) like 'mic%'
update images set sub_folder = 'mia' where lower(name_no_ext) like 'mia%'
update images set sub_folder = 'cha' where lower(name_no_ext) like 'cha%'
update images set sub_folder = 'ann' where lower(name_no_ext) like 'ann%'
update images set sub_folder = 'jan' where lower(name_no_ext) like 'jan%'
update images set sub_folder = 'lil' where lower(name_no_ext) like 'lil%'
update images set sub_folder = 'ali' where lower(name_no_ext) like 'ali%'
update images set sub_folder = 'luc' where lower(name_no_ext) like 'luc%'
update images set sub_folder = 'dan' where lower(name_no_ext) like 'dan%'
update images set sub_folder = 'nat' where lower(name_no_ext) like 'nat%'
--move to one-char subfoler
update images set sub_folder = 'u' where lower(name_no_ext) like 'u%'
update images set sub_folder = 'y' where lower(name_no_ext) like 'y%'
update images set sub_folder = 'w' where lower(name_no_ext) like 'w%'
update images set sub_folder = 'o' where lower(name_no_ext) like 'o%'
-- should also do image_counts??
--SECOND move files to new folder on D: and B:
--see todo.txt for steps to move files (search for !!!IMPORTANT!!!)
--THEN RUN "ud" to update image counts


-- manual cleanup of image_counts (1)
select * from image_counts where image_count < 1 order by lower(base_name);
delete from image_counts where image_count = 0;

-- manual cleanup of image_counts (2)
-- all baseNames with 0 or 1 capital letters
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
select * from image_counts where base_name like 'Tana%' order by lower(base_name), collapse_groups;
select count(*) from image_counts where base_name like 'Nat%' order by lower(base_name), collapse_groups;
select sum(image_count) from image_counts where base_name like 'Nat%' order by lower(base_name), collapse_groups;
select * from images where name_no_ext like 'Nat%';
select count(*) from images where name_no_ext like 'Nat%';
select * from images where insert_date = (select max(insert_date) from images);
--delete from image_counts where base_name like 'Nat%'

-- images size in GB
select count(*), sum(bytes) / (1024 * 1024 * 1024) as GigaBytes
, min(date(insert_date))
, date(current_timestamp)
from images
where timestampdiff (day, date(insert_date), date(current_timestamp)) <= 1 -- use 0 for today (but less than 1 doesn't seem to work quite right)
-- and sub_folder = 'li'

-- images size distribution by folder
select sub_folder, sum(bytes) / (1024 * 1024) as mega_bytes, count(*) as files
from images
group by sub_folder
order by mega_bytes desc, files desc

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

-- count distribution
select d.count as count, count(d.count) as rows1 from image_diffs d group by d.count order by d.count desc

-- diff distribution
select d.avg_diff as avg_diff, count(d.avg_diff) as rows1 from image_diffs d group by d.avg_diff order by d.avg_diff desc
select d.std_dev as std_dev, count(d.std_dev) as rows1 from image_diffs d group by d.std_dev order by d.std_dev desc

-- check for invalid state, should return 0 rows
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
where d.avg_diff <= 20 
     and (i1.name_no_ext like 'Jo%' or i2.name_no_ext like 'Jo%')
order by count desc

-- image_diffs cleanup
xx delete from image_diffs where avg_diff >= 19
xx delete from image_diffs where timestampdiff (hour, last_update, current_timestamp) > 1

-- avoid hitting 255 max for column (obsolete)
-- select * from image_diffs where count > 250
-- update image_diffs set count = 127 where count > 127

--
select distinct name from albumtags.base1_names where name_id in (
select name_id from (
(select name_id from albumtags.base1_names_tags
inner join albumtags.tags on base1_names_tags.tag_id = tags.tag_id
where tags.tag_id = (
select tag_id from albumtags.tags
where tag = 'bumble'
)) order by rand() limit 500) t)

-- original: query image_diffs table for AlbumImages.doDups, with orientation test
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
where -- d.avg_diff <= 15 and -- MAX_RGB_DIFF and
	timestampdiff (hour, d.last_update, current_timestamp) <= 1 -- and -- SINCE_HOURS
  having strcmp(i1_orient, i2_orient) = 0
-- order by avg_diff, i1.name_no_ext, i2.name_no_ext
-- order by avg desc, i1.name_no_ext
order by d.last_update desc

-- number of rows with mismatched orientation vs all rows (SHOULD ALWAYS BE ZERO because this is prevented in the code)
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

select * from image_diffs limit 100

select max(name_id_1), max(name_id_2) from image_diffs

select max(name_id) from images

-- cleanup obsolete rows
-- rows that no longer have corresponding images
-- use D:\Netscape\Program\bin\idCleanup.bat to remove these rows
select count(*) from image_diffs
where (name_id_1 not in (select name_id from images)) OR (name_id_2 not in (select name_id from images))
--delete from image_diffs where (name_id_1 not in (select name_id from images)) OR (name_id_2 not in (select name_id from images))


-- cleanup likely useless rows
select count(*) from image_diffs where avg_diff >= 35 and std_dev >= 35 order by avg_diff desc
select avg_diff, std_dev from image_diffs where avg_diff >= 30 and std_dev >= 30 order by avg_diff desc
-- note this might remove useful rows: should only delete when both values > threshold
    select avg_diff, count(*) from image_diffs where avg_diff >= 50 group by avg_diff order by avg_diff desc
    --delete from image_diffs where avg_diff >= 50

    select std_dev, count(*) from image_diffs where std_dev >= 50 group by std_dev order by std_dev desc
    --delete from image_diffs where std_dev >= 50

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
-- DELIMITER |
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
-- DELIMITER ;

SELECT alphas('123ab45cde6789fg0000000000000000000000000000');
+----------------------------+
| alphas('123ab45cde6789fg') |
+----------------------------+
| abcdefg                    |
+----------------------------+

-- -----------------------------------------------------------------------------
-- "How to Optimize MySQL Tables and Defragment to Recover Space"
-- https://www.thegeekstuff.com/2016/04/mysql-optimize-table/

-- see dbInfo.bat

-- NOTE that data_free_kb seems to always be 4096KB or larger
select table_schema, table_name,
round(data_length/1024) as data_length_kb,
round(data_free/1024) as data_free_kb,
round(100*data_free/(data_length+data_free)) as percent_free
from information_schema.tables
where table_schema in ('albumimages', 'albumtags')
order by table_schema, table_name

-- optimize table from command line (maybe don't? Message says: "Table does not support optimize, doing recreate + analyze instead")
mysql -u root -proot albumimages
--optimize table image_diffs;

    MariaDB [albumimages]> optimize table image_diffs;
    +-------------------------+----------+----------+-------------------------------------------------------------------+
    | Table                   | Op       | Msg_type | Msg_text                                                          |
    +-------------------------+----------+----------+-------------------------------------------------------------------+
    | albumimages.image_diffs | optimize | note     | Table does not support optimize, doing recreate + analyze instead |
    | albumimages.image_diffs | optimize | status   | OK                                                                |
    +-------------------------+----------+----------+-------------------------------------------------------------------+
    2 rows in set (31 min 36.564 sec)

-- table metadata
select *
from information_schema.tables
where table_schema in ('albumimages', 'albumtags')
order by table_schema, table_name

REM database files on disk
tdir/s "C:\Program Files\MariaDB 10.4\data\albumimages"
tdir/s "C:\Program Files\MariaDB 10.4\data\albumtags"

REM backups
tdir/s A:\Netscape.data.backup\sql

-- -----------------------------------------------------------------------------
-- find all new images within 120 days
select count(*) from images
-- select *,  UNIX_TIMESTAMP(), (UNIX_TIMESTAMP() - (modified/1000)), FROM_UNIXTIME(modified/1000) from images
where (UNIX_TIMESTAMP() - (modified/1000)) <= (120 * 24 * 60 * 60)
-- limit 100

-- -----------------------------------------------------------------------------
-- get max date (i.e., newest) from album
select max(modified), alphas(name_no_ext) as base_name from images where name_no_ext like 'Mar%' group by base_name

SELECT alphas('123ab45cde6789fg0000000000000000000000000000');
SELECT alphas('12-3ab45cd-e6789fg00000000000000000-00000000000');

*/
