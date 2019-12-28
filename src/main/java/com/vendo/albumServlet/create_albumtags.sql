/* Commands to create the "albumtags" database and tables
mysql -u root -proot -e "create database albumtags"
mysql -u root -proot -e "show databases"
mysql -u root -proot albumtags < create_albumtags.sql
mysql -u root -proot albumtags
show tables;
describe tags;
describe names;
describe names_tags;

echo show variables | mysql -u root -proot albumimages
echo show variables | mysql -u root -proot albumimages | grep -i innodb | grep -i size

REM testing
mysql -u root -proot albumtags
select * from tags order by lower(tag);
select * from tags order by lower(name);
select count(*) from tags;

REM cleanup after testing
mysql -u root -proot albumtags
delete from tags where tag like 'unknown';
*/

/* cleanup for testing
mysql -u root -proot albumtags
truncate table tags;
truncate table names;
truncate table names_tags;
-- or
drop table config;
drop table tags;
drop table base1_names;
drop table base2_names;
drop table raw_names;
drop table base1_names_tags;
drop table base2_names_tags;
drop table raw_names_tags;
*/

CREATE TABLE IF NOT EXISTS config
(
	name		VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	string_value VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS tags
(
	tag_id		INT UNSIGNED NOT NULL AUTO_INCREMENT,
	tag			VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	is_tattoo	TINYINT UNSIGNED NOT NULL,
	PRIMARY KEY (tag_id)
);
CREATE INDEX is_tattoo_idx on tags (is_tattoo);

CREATE TABLE IF NOT EXISTS base1_names
(
	name_id		INT UNSIGNED NOT NULL AUTO_INCREMENT,
	name		VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	PRIMARY KEY	(name_id)
);

CREATE TABLE IF NOT EXISTS base2_names
(
	name_id		INT UNSIGNED NOT NULL AUTO_INCREMENT,
	name		VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	PRIMARY KEY	(name_id)
);

CREATE TABLE IF NOT EXISTS raw_names
(
	name_id		INT UNSIGNED NOT NULL AUTO_INCREMENT,
	name		VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	PRIMARY KEY	(name_id)
);

CREATE TABLE IF NOT EXISTS base1_names_tags
(
	tag_id		INT UNSIGNED NOT NULL,
	name_id		INT UNSIGNED NOT NULL,
	PRIMARY KEY	(tag_id, name_id)
);

CREATE TABLE IF NOT EXISTS base2_names_tags
(
	tag_id		INT UNSIGNED NOT NULL,
	name_id		INT UNSIGNED NOT NULL,
	PRIMARY KEY	(tag_id, name_id)
);

CREATE TABLE IF NOT EXISTS raw_names_tags
(
	tag_id		INT UNSIGNED NOT NULL,
	name_id		INT UNSIGNED NOT NULL,
	PRIMARY KEY	(tag_id, name_id)
);

CREATE TABLE IF NOT EXISTS tags_filters
(
	id			INT UNSIGNED NOT NULL AUTO_INCREMENT,
	tag			VARCHAR(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL UNIQUE,
	filters		TEXT CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	PRIMARY KEY (id)
);

/*

drop table config;
drop table tags;
drop table base1_names;
drop table base2_names;
drop table raw_names;
drop table base1_names_tags;
drop table base2_names_tags;
drop table raw_names_tags;
drop table tags_filters;

-- -----------------------------------------------------------------------------
-- database engine and status data for each table
select * 
from information_schema.tables 
where table_schema in ('albumtags', 'albumimages')
order by table_schema, table_name

-- database table sizes
select table_schema as 'Schema', table_name as 'Table', ROUND((data_length + index_length) / 1024 / 1024) as 'Size in MB'
from information_schema.tables 
where table_schema in ('albumtags', 'albumimages', 'gihistory')
order by table_schema, table_name

-- -----------------------------------------------------------------------------
-- table sizes (number of rows)
select 'config' as name, count(*) as rows from config
union all
select 'tags' as name, count(*) as rows from tags
union all
select 'base1_names' as name, count(*) as rows from base1_names
union all
select 'base2_names' as name, count(*) as rows from base2_names
union all
select 'raw_names' as name, count(*) as rows from raw_names
union all
select 'base1_names_tags' as name, count(*) as rows from base1_names_tags
union all
select 'base2_names_tags' as name, count(*) as rows from base2_names_tags
union all
select 'raw_names_tags' as name, count(*) as rows from raw_names_tags
union all
select 'tags_filters' as name, count(*) as rows from tags_filters;

-- -----------------------------------------------------------------------------
-- distribution of tags in file (i.e., raw_names_tags)
select t.tag, count(*) count
from raw_names_tags rnt
join tags t on rnt.tag_id = t.tag_id
group by rnt.tag_id
order by count desc, tag asc
 
-- -----------------------------------------------------------------------------
-- inserts
insert into tags (tag) values ('red'), ('dry'), ('hot'), ('blue'), ('wet'), ('cold'), ('pink'), ('high'), ('tall');
insert into names (name, name_type) values ('Foo03', 1), ('Bar01', 1), ('Foo01', 1), ('Boo03', 1), ('Bar05', 1), ('Tob02', 1), ('Tob01', 1);

insert into names_tags (name_id, name_type, tag_id) values ((select name_id from names where name = 'Boo03'), (select name_type from names where name = 'Boo03'), (select tag_id from tags where tag = 'hot'));
insert into names_tags (name_id, tag_id) values ((select name_id from names where name = 'Tob01'), (select tag_id from tags where tag = 'hot'));
insert into names_tags (name_id, tag_id) values ((select name_id from names where name = 'Tob01'), (select tag_id from tags where tag = 'red'));
insert into names_tags (name_id, tag_id) values ((select name_id from names where name = 'Tob01'), (select tag_id from tags where tag = 'dry'));
insert into names_tags (name_id, tag_id) values ((select name_id from names where name = 'Tob02'), (select tag_id from tags where tag = 'dry'));
insert into names_tags (name_id, tag_id) values ((select name_id from names where name = 'Tob02'), (select tag_id from tags where tag = 'pink'));

-- -----------------------------------------------------------------------------
-- queries
select * from tags order by lower (tag);
select * from base_names order by lower (name);
select * from raw_names order by lower (name);

select * from base_names where name like 'bab%' order by lower (name);
select * from raw_names where name like 'bab%' order by lower (name);

-- select * from names_tags order by tag_id, name_id, name_type;
--
select count(*) from tags;
select name_type, count(*) from names group by name_type;
select name_type, count(*) from names_tags group by name_type;

-- -----------------------------------------------------------------------------
REM compare all base name entries vs. raw
mysql -u root -proot albumtags -e "select * from names where name_type = 1 order by lower (name)" > r1
mysql -u root -proot albumtags -e "select * from names where name_type = 2 order by lower (name)" > r2

-- compare count of base name entries vs. raw
select name_type, count(*) from names where lower(name) like '%' group by name_type;
select name_type, count(*) from names where lower(name) like 'b%' group by name_type;

-- -----------------------------------------------------------------------------
-- find potentially malformed/incomplete names in raw (e.g., don't end in "+"/"*" or don't have number or two uppercase letters)
-- implemented in Java here: AlbumTags.checkForMalformedFilters();
select name from raw_names
 where
 name not regexp '.*[\+].*' and
 name not regexp '.*[\*].*' and
 name not regexp '.*[^0-9][0-9].*' and
 name not regexp '.*[A-Z].*[A-Z].*' and
 name not regexp 'xbf_.*'
 order by lower(name);

-- -----------------------------------------------------------------------------
-- show all tags for base names; note does not like space between 'group_concat' and '('
select n.name, n.name_type, group_concat(distinct t.tag order by lower(t.tag) separator ', ') as tags
from names n
join names_tags nt on n.name_id = nt.name_id
join tags t on nt.tag_id = t.tag_id
where n.name_type = 1
group by n.name;

-- show all tags for raw names; note does not like space between 'group_concat' and '('
select n.name, n.name_type, group_concat(distinct t.tag order by lower(t.tag) separator ', ') as tags
from names n
join names_tags nt on n.name_id = nt.name_id
join tags t on nt.tag_id = t.tag_id
where n.name_type = 2
group by n.name;

-- -----------------------------------------------------------------------------
-- find all names for a tag
select * from base1_names where name_id in (select distinct name_id from base1_names_tags where tag_id in (select tag_id from tags where tag like 'star%'));
select * from base1_names where name_id in (select distinct name_id from base1_names_tags where tag_id in (select tag_id from tags where tag like 'spider%'));
select * from base1_names where name_id in (select distinct name_id from base1_names_tags where tag_id in (select tag_id from tags where tag like 'snake%')) order by name;

-- -----------------------------------------------------------------------------
-- query (no intersect) on 1 tag
select distinct name from raw_names where name_id in (
                (select name_id from raw_names_tags
                        inner join tags on raw_names_tags.tag_id = tags.tag_id
                                where tags.tag_id = (
                                        select tag_id from tags where tag = 'spider'))
) order by lower(name)

-- query (no intersect) on 1 tag with name
select distinct name from base1_names where name_id in (
        select name_id from (
                (select name_id from base1_names_tags
                        inner join tags on base1_names_tags.tag_id = tags.tag_id
                                where tags.tag_id = (
                                        select tag_id from tags where tag = 'spider'))
                union all
                (select name_id from base1_names where lower(name) regexp '^a.*')
        ) as tbl group by tbl.name_id having count(*) = 2
) order by lower(name)

-- query intersect on 2 tags
select distinct name from base1_names where name_id in (
        select name_id from (
                (select name_id from base1_names_tags
                        inner join tags on base1_names_tags.tag_id = tags.tag_id
                                where tags.tag_id = (
                                        select tag_id from tags where tag = 'spider'))
                union all
                (select name_id from base1_names_tags
                        inner join tags on base1_names_tags.tag_id = tags.tag_id
                                where tags.tag_id = (
                                        select tag_id from tags where tag = 'snake'))
        ) as tbl group by tbl.name_id having count(*) = 2
) order by lower(name)

-- query intersect on 2 tags with name
select distinct name from base1_names where name_id in (
        select name_id from (
                (select name_id from base1_names_tags
                        inner join tags on base1_names_tags.tag_id = tags.tag_id
                                where tags.tag_id = (
                                        select tag_id from tags where tag = 'spider'))
                union all
                (select name_id from base1_names_tags
                        inner join tags on base1_names_tags.tag_id = tags.tag_id
                                where tags.tag_id = (
                                        select tag_id from tags where tag = 'snake'))
                union all
                (select name_id from base1_names where lower(name) regexp '^a.*')
        ) as tbl group by tbl.name_id having count(*) = 3
) order by lower(name)

--

-- example query intersect
select * from (
	(select names_tags.* from names_tags inner join tags on names_tags.tag_id=tags.tag_id where tags.tag_id=31)
	union all
	(select names_tags.* from names_tags inner join tags on names_tags.tag_id=tags.tag_id where tags.tag_id=32)
) as tbl group by tbl.name_id having count(*)=2;
-- original from: http://stackoverflow.com/questions/2300322/intersect-in-mysql

-- -----------------------------------------------------------------------------
-- enhancing getTagsForBaseName() (unfortunately slow when large number of items match)
select group_concat(distinct t.tag order by lower(t.tag) separator ', ') as tags
 from base_names bn
 join base_names_tags bnt on bn.name_id = bnt.name_id
 join tags t on bnt.tag_id = t.tag_id
 where bn.name rlike 'Pau.*e.*[0-9]'

-- -----------------------------------------------------------------------------
select bn.name, group_concat(distinct t.tag order by lower(t.tag) separator ', ') as tags
 from base_names bn
 join base_names_tags bnt on bn.name_id = bnt.name_id
 join tags t on bnt.tag_id = t.tag_id
-- where bn.name = ?
 where bn.name in ('name01', 'name02')
 group by bn.name

-- -----------------------------------------------------------------------------
select * from albumtags.tags_filters where tag like 'B%' order by tag
select * from tags_filters where filters != ''
select count(*) from tags_filters

-- -----------------------------------------------------------------------------
-- find albums that have 0 tags
-- uses function albumimages.alphas
-- OLD and SLOW
select distinct albumimages.alphas(i.name_no_ext) as name, c.image_count as count, i.sub_folder 
  from albumimages.images i 
join albumimages.image_counts c on c.sub_folder = i.sub_folder and albumimages.alphas(i.name_no_ext) = c.base_name
  where i.sub_folder = 'cl' and c.collapse_groups = 1 
  and albumimages.alphas(i.name_no_ext) not in (
    select distinct albumimages.alphas(t.name) as name 
    from albumtags.base2_names t 
    where lower(t.name) like 'cl%'
  )

-- find albums that have 0 tags
-- NEW and FASTER
-- AlbumTags.getAlbumsWithNoTags
with temp_table as (
select ic.base_name as name, ic.image_count as count
 from albumimages.image_counts ic
 where ic.collapse_groups = 1
 and ic.sub_folder not in ('q-', 'qb', 'qd', 'qf', 'qg', 'qh', 'qj', 'qm', 'qt', 'xx', 'xb')
-- and ic.image_count >= 10
 and ic.base_name not in (
  select bn.name
  from albumtags.base2_names bn
 )
) select count(*) from temp_table


-- -----------------------------------------------------------------------------
-- calculated no-tattoos
select name from base2_names
where base2_names.name_id not in (
    select base2_names_tags.name_id
    from base2_names_tags 
    inner join tags on base2_names_tags.tag_id = tags.tag_id
    where tags.is_tattoo = 1
)
order by base2_names.name

select * from tags where is_tattoo = 0
order by lower(tag)







*/
