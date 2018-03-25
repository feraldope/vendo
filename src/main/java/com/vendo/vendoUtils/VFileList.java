//VFileList.java

package com.vendo.vendoUtils;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

//import org.apache.logging.log4j.*;


public class VFileList
{
	///////////////////////////////////////////////////////////////////////////
	public VFileList (String folderName, List<Pattern> filePatterns, boolean recurseSubdirs)
	{
		_folder = Paths.get (folderName); //note this can be a folder or a file
		_filePatterns = filePatterns; //may be null
		_recurseSubdirs = recurseSubdirs;
	}

	///////////////////////////////////////////////////////////////////////////
	public List<String> getFileList ()
	{
		List<String> fileList = getFileList2 ();

		Collections.sort (fileList, VendoUtils.caseInsensitiveStringComparator);

		return fileList;
	}

/* old way; keep as example
	///////////////////////////////////////////////////////////////////////////
	private List<String> getFileList1 () //does not recurse subdirs
	{
		List<String> fileList = new ArrayList<String> ();

//TODO: since we can't pass file to Files.list
//		if (Files.isDirectory (_folder, ...)
//			then pass _folder.getParent() instead

		try (Stream<Path> stream = Files.list (_folder)) {
			fileList = stream.filter (path -> matchesLeaf (path))
							 .map (String::valueOf)
//							 .sorted (VendoUtils.caseInsensitiveStringComparator)
							 .collect (Collectors.toList ());

		} catch (IOException ex) {
			ex.printStackTrace ();
		}

		return fileList;
	}
*/

	///////////////////////////////////////////////////////////////////////////
	private List<String> getFileList2 ()
	{
		List<String> fileList = new ArrayList<String> ();

		Set<FileVisitOption> opts = Collections.emptySet ();
//		EnumSet<FileVisitOption> opts = EnumSet.of (FileVisitOption.FOLLOW_LINKS);

		int maxDepth = (_recurseSubdirs ? Integer.MAX_VALUE : 1);

		try {
			Files.walkFileTree (_folder, opts, maxDepth, new SimpleFileVisitor<Path> () {
 				@Override
				public FileVisitResult visitFile (Path path, BasicFileAttributes attrs)
				{
//					System.err.println ("VFileList.getFileList: visitFile: path = " + path.toAbsolutePath ().normalize ().toString ());
					if (matchesLeaf (path)) {
						String filename = path.toAbsolutePath ().normalize ().toString ();
						fileList.add (filename);
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed (Path path, IOException ex)
				{
					if (ex != null) {
						ex.printStackTrace ();
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory (Path dir, IOException ex)
				{
					if (ex != null) {
						ex.printStackTrace ();
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (IOException ex) {
			throw new AssertionError ("Files#walkFileTree will not throw IOException if the FileVisitor does not");
		}

		return fileList;
	}

	///////////////////////////////////////////////////////////////////////////
	//match on leaf name (not whole path)
	boolean matchesLeaf (Path path)
	{
		if (_filePatterns == null) {
			return true;
		}

		String filename = path.getName (path.getNameCount () - 1).toString (); //filename is last leaf
//		System.err.println ("VFileList.matchesLeaf: filename = " + filename);

		for (Pattern filePattern : _filePatterns) {
			if (filePattern.matcher (filename).matches ()) {
				return true;
			}
		}

		return false;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder ();
		sb.append ("VFileList");
		sb.append (": folder: ");
		sb.append (_folder.toAbsolutePath ().normalize ().toString ());
		sb.append (", filePatterns: ");
		sb.append (_filePatterns);
		sb.append (", recurseSubdirs: ");
		sb.append (_recurseSubdirs);

		return sb.toString ();
	}


	//private members
	final private Path _folder;
	final private List<Pattern> _filePatterns;
	final private boolean _recurseSubdirs;

//	private static Logger _log = LogManager.getLogger (VFileList.class); //note this will only work for files in the VendoUtils package
}
