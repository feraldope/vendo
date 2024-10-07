//AlbumImageMapper.java

package com.vendo.albumServlet;

import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;


public interface AlbumImageMapper {

	//selects

	public Timestamp selectLastUpdateFromImageFolderTable (@Param("sub_folder") String subFolder);

	public Timestamp selectMaxLastUpdateFromImageFolderTable ();

	public Timestamp selectMaxInsertDateFromImagesTable (@Param("sub_folder") String subFolder);

	public List<AlbumImageFileDetails> selectImageFileDetailsFromImagesTable (@Param("sub_folder") String subFolder);

	public List<AlbumImage> selectImagesFromImagesTable (@Param("sub_folder") String subFolder);

	public int selectImageCountFromImagesTable (@Param("sub_folder") String subFolder, @Param("name_no_ext") String nameNoExt);

	public int selectImageCountFromImageCountsTable (@Param("sub_folder") String subFolder, @Param("base_name") String baseName);

	public int selectAlbumCountFromImageCounts (@Param("sub_folder") String subFolder, @Param("base_name") String baseName);

	public List<AlbumImageCount> selectImageCountsFromImageCountsTable (@Param("sub_folder") String subFolder);

	public List<AlbumImageCount> selectMismatchedEntriesFromImageCounts ();

	public List<AlbumImageDiffDetails> selectAllImagesFromImageDiffsTable ();

	public List<AlbumImageDiffDetails> selectImagesFromImageDiffsTable (@Param("nameIds") Collection<Integer> nameIds);

	public Collection<AlbumImageData> selectNamesFromImagesTable (@Param("names") Collection<String> names);

	public int selectMaxNameIdFromImagesTable ();

	//inserts (updates)

	public int insertLastUpdateIntoImageFolderTable (@Param("sub_folder") String subFolder, @Param("last_update") Timestamp lastUpdate);

	public int insertImageIntoImagesTable (AlbumImage image);

	public int insertImageCountsIntoImageCountsTable (@Param("sub_folder") String subFolder, @Param("base_name") String baseName, @Param("collapse_groups") int collapseGroups, @Param("image_count") int value);

	public int insertImageIntoImageDiffs (AlbumImageDiffDetails imageDiffDetails);

	//deletes

	public int deleteImageFromImagesTable (@Param("sub_folder") String subFolder, @Param("name_no_ext") String nameNoExt);

	public int deleteImageCountsFromImageCountsTable (@Param("sub_folder") String subFolder, @Param("base_name") String baseName);

	public int deleteZeroCountsFromImageCountsTable ();

	public int deleteMismatchedEntriesFromImageCountsTable ();
}
