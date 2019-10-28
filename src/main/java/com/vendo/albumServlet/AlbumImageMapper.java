//AlbumImageMapper.java

package com.vendo.albumServlet;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;


public interface AlbumImageMapper {

	//selects

	public Timestamp selectLastUpdateFromImageFolder (@Param("sub_folder") String subFolder);

	public Timestamp selectMaxLastUpdateFromImageFolder ();

	public Timestamp selectMaxInsertDateFromImages (@Param("sub_folder") String subFolder);

	public List<AlbumImageFileDetails> selectImageFileDetailsFromImages (@Param("sub_folder") String subFolder);

	public List<AlbumImage> selectImagesFromImages (@Param("sub_folder") String subFolder);

	public int selectImageCountFromImages (@Param("sub_folder") String subFolder, @Param("name_no_ext") String nameNoExt);

	public int selectImageCountFromImageCounts (@Param("sub_folder") String subFolder, @Param("base_name") String baseName);

	public int selectAlbumCountFromImageCounts (@Param("sub_folder") String subFolder, @Param("base_name") String baseName);

	public List<AlbumImageCount> selectImageCountsFromImageCounts (@Param("sub_folder") String subFolder);

	public List<AlbumImageCount> selectMismatchedEntriesFromImageCounts ();

	public List<AlbumImageDiffDetails> selectImagesFromImageDiffs ();

	public Collection<AlbumImageData> selectNamesFromImages (@Param("names") Collection<String> names);

	//inserts (updates)

	public int insertLastUpdateIntoImageFolder (@Param("sub_folder") String subFolder, @Param("last_update") Timestamp lastUpdate);

	public int insertImageIntoImages (AlbumImage image);

	public int insertImageCountsIntoImageCounts (@Param("sub_folder") String subFolder, @Param("base_name") String baseName, @Param("collapse_groups") int collapseGroups, @Param("image_count") int value);

	public int insertImageIntoImageDiffs (AlbumImageDiffDetails imageDiffDetails);

	//deletes

	public int deleteImageFromImages (@Param("sub_folder") String subFolder, @Param("name_no_ext") String nameNoExt);

	public int deleteImageCountsFromImageCounts (@Param("sub_folder") String subFolder, @Param("base_name") String baseName);

	public int deleteZeroCountsFromImageCounts ();

	public int deleteMismatchedEntriesFromImageCounts ();
}
