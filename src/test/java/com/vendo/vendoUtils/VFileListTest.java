package com.vendo.vendoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.function.Predicate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vendo.vendoUtils.VFileList.ListMode;

public class VFileListTest {
	File imageFolder = null;
	String relativePath = "src/main/java/com/vendo";

    Predicate<String> hasJpgExtension = s -> s.toLowerCase ().endsWith (".jpg");
    Predicate<String> containSlash = s -> s.contains ("\\") || s.contains ("/");
    Predicate<String> containRelativePath = s -> s.replaceAll ("\\\\", "/").contains (relativePath);

	///////////////////////////////////////////////////////////////////////////
	@Before
	public void setUp() throws Exception {
		URL url = this.getClass().getClassLoader().getResource("");
		File file = null;
	    try {
	        file = new File (url.toURI ());
	    } catch (URISyntaxException e) {
	        file = new File (url.getPath ());
	    } finally {
	    }
	    imageFolder = FileSystems.getDefault ().getPath (file.toString (), "../..", relativePath, "images").toFile ();
	}

	///////////////////////////////////////////////////////////////////////////
	@After
	public void tearDown() throws Exception {
	}

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testWithWildname_CompletePath () {
		List<String> fileList = new VFileList (imageFolder.toString (), "*.jpg", /*recurseSubdirs*/ false).getFileList (ListMode.CompletePath);

		assertEquals (9, fileList.size ());
		assertTrue (fileList.stream ().allMatch (containRelativePath));
		assertTrue (fileList.stream ().allMatch (hasJpgExtension));

		assertTrue (fileList.get (0).endsWith ("DSC03969.JPG"));
		assertTrue (fileList.get (1).endsWith ("DSC03969_scaled25.JPG"));
	}

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testWithWildname_FileOnly () {
		List<String> fileList = new VFileList (imageFolder.toString (), "*.jpg", /*recurseSubdirs*/ false).getFileList (ListMode.FileOnly);

		assertEquals (9, fileList.size ());
		assertTrue (fileList.stream ().noneMatch (containSlash));
		assertTrue (fileList.stream ().allMatch (hasJpgExtension));

		assertTrue (fileList.get (0).compareToIgnoreCase ("DSC03969.JPG") == 0);
		assertTrue (fileList.get (1).compareToIgnoreCase ("DSC03969_scaled25.JPG") == 0);
	}

	///////////////////////////////////////////////////////////////////////////
	@Test
	public void testWithWildname_RecurseSubdirs () {
		List<String> fileList = new VFileList (imageFolder.toString () + "/../..", "*.jpg", /*recurseSubdirs*/ true).getFileList (ListMode.CompletePath);

		assertEquals (9, fileList.size ());
		assertTrue (fileList.stream ().allMatch (containRelativePath));
		assertTrue (fileList.stream ().allMatch (hasJpgExtension));

		assertTrue (fileList.get (0).endsWith ("DSC03969.JPG"));
		assertTrue (fileList.get (1).endsWith ("DSC03969_scaled25.JPG"));
	}

}
