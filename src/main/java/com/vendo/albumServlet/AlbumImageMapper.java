//AlbumImageMapper.java

package com.vendo.albumServlet;

import org.apache.ibatis.annotations.*;

import java.sql.*;
import java.util.*;


public interface AlbumImageMapper {

	//selects

	public Timestamp selectLastUpdateFromImageFolder (@Param("sub_folder_int") int subFolderInt);

	public Timestamp selectMaxLastUpdateFromImageFolder ();

	public Timestamp selectMaxInsertDateFromImages (@Param("sub_folder_int") int subFolderInt);

	public List<AlbumImageFileDetails> selectImageFileDetailsFromImages (@Param("sub_folder_int") int subFolderInt);

	public List<AlbumImage> selectImagesFromImages (@Param("sub_folder_int") int subFolderInt);

	public int selectImageCountFromImageCounts (@Param("sub_folder_int") int subFolderInt, @Param("base_name") String baseName);

	public List<AlbumImageCount> selectImageCountsFromImageCounts (@Param("sub_folder_int") int subFolderInt);

	public List<AlbumImageCount> selectMismatchedEntriesFromImageCounts ();

	//inserts (updates)

	public int insertLastUpdateIntoImageFolder (@Param("sub_folder_int") int subFolderInt, @Param("last_update") Timestamp lastUpdate);

	public int insertImageIntoImages (AlbumImage image);

	public int insertImageCountsIntoImageCountsPlus (@Param("sub_folder_int") int subFolderInt, @Param("base_name") String baseName, @Param("collapse_groups") int collapseGroups, @Param("image_count") int value);
	public int insertImageCountsIntoImageCountsMinus (@Param("sub_folder_int") int subFolderInt, @Param("base_name") String baseName, @Param("collapse_groups") int collapseGroups, @Param("image_count") int value);
	public int insertImageCountsIntoImageCountsEquals (@Param("sub_folder_int") int subFolderInt, @Param("base_name") String baseName, @Param("collapse_groups") int collapseGroups, @Param("image_count") int value);

	//deletes

	public int deleteImageFromImages (@Param("sub_folder_int") int subFolderInt, @Param("name_no_ext") String nameNoExt);

	public int deleteZeroCountsFromImageCounts ();

	public int deleteMismatchedEntriesFromImageCounts ();
}
