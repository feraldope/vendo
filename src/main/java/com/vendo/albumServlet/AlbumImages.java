//AlbumImages.java

package com.vendo.albumServlet;

import com.vendo.vendoUtils.AlphanumComparator;
import com.vendo.vendoUtils.VFileList;
import com.vendo.vendoUtils.VFileList.ListMode;
import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.math.util.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class AlbumImages
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumImages getInstance ()
	{
		String rootPath = AlbumFormInfo.getInstance ().getRootPath (false);

		if (AlbumFormInfo._logLevel >= 8) {
			_log.debug ("AlbumImages.getInstance: rootPath = " + rootPath);
			_log.debug ("AlbumImages.getInstance: _prevRootPath = " + _prevRootPath);
		}

		if (!rootPath.equals (_prevRootPath)) {
			if (!_prevRootPath.equals ("")) {
				_log.debug ("AlbumImages.getInstance: rootPath has changed to " + rootPath);
			}
			_instance = null;
			_prevRootPath = rootPath;
		}

		if (_instance == null) {
			_instance = new AlbumImages ();
		}

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumImages ()
	{
//		if (AlbumFormInfo._logLevel >= 6)
		_log.debug ("AlbumImages ctor");

		final String rootPath = AlbumFormInfo.getInstance ().getRootPath (false);
		if (AlbumFormInfo._logLevel >= 7) {
			_log.debug ("AlbumImages ctor: rootPath = " + rootPath);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public synchronized static ExecutorService getExecutor ()
	{
		if (_executor == null || _executor.isTerminated () || _executor.isShutdown ()) {
			_executor = Executors.newFixedThreadPool (VendoUtils.getLogicalProcessors () - 1);
		}

		return _executor;
	}

	///////////////////////////////////////////////////////////////////////////
	public static void shutdownExecutor ()
	{
		_log.debug ("AlbumImages.shutdownExecutor: shutdown executor");
		getExecutor ().shutdownNow ();
	}

	///////////////////////////////////////////////////////////////////////////
	//used by CLI: extract final leaf from complete path
	public static String processPath (String path)
	{
		String replace = path.replace('\\', '/');

		if (!replace.endsWith ("/")) {
			replace += '/';
		}

		String[] parts = replace.split ("/");

		return parts[parts.length - 1];
	}

	///////////////////////////////////////////////////////////////////////////
	public void setForm (AlbumFormInfo form)
	{
		_form = form;
	}

	///////////////////////////////////////////////////////////////////////////
	public void processParams (HttpServletRequest request)
	{
		int imagesRemoved = 0;
		long currentTimestamp = 0;

		Enumeration<String> paramNames = request.getParameterNames ();
		while (paramNames.hasMoreElements ()) {
			String paramName = paramNames.nextElement ();
			if (AlbumFormInfo._logLevel >= 10) {
				_log.debug ("AlbumImages.processParams: got param \"" + paramName + "\"");
			}

			if (paramName.equalsIgnoreCase ("timestamp")) {
				String[] paramValues = request.getParameterValues (paramName);
				try {
					currentTimestamp = Long.parseLong (paramValues[0]);
				} catch (NumberFormatException exception) {
					currentTimestamp = 0;
				}
			}

			//delete requests that have same timestamp as previous should be ignored (can happen if user forces browser refresh)
			if ((paramName.startsWith (AlbumFormInfo._DeleteParam1) || paramName.startsWith (AlbumFormInfo._DeleteParam2)) && !_previousRequestTimestamps.contains (currentTimestamp)) {
				String[] paramValues = request.getParameterValues (paramName);
				if (paramValues[0].equalsIgnoreCase ("on")) {
					if (paramName.startsWith (AlbumFormInfo._DeleteParam1)) {
						String filename = paramName.substring (AlbumFormInfo._DeleteParam1.length ());
						imagesRemoved += renameImageFile (filename);
					} else { //paramName.startsWith (AlbumFormInfo._DeleteParam2)
						String wildName = paramName.substring (AlbumFormInfo._DeleteParam2.length ());
						imagesRemoved += renameImageFiles (wildName);
					}
				}
			}
		}
		_previousRequestTimestamps.add (currentTimestamp);

		if (imagesRemoved > 0) {
			int sleepMillis = 150 + imagesRemoved * 10;
			VendoUtils.sleepMillis (sleepMillis); //HACK - try to give AlbumImageDao some time to complete its file processing
			_log.debug ("AlbumImages.processParams: slept " + sleepMillis + " ms");
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public int processRequest ()
	{
		String[] filters = _form.getFilters ();
		String[] originalFilters = _form.getFilters ();
		String[] tagsIn = _form.getTags (AlbumTagMode.TagIn);
		String[] tagsOut = _form.getTags (AlbumTagMode.TagOut);
		String[] excludes = _form.getExcludes ();
		int maxFilters = _form.getMaxFilters ();
		long sinceInMillis = _form.getSinceInMillis (true);
		boolean collapseGroups = _form.getCollapseGroups();
		boolean tagFilterOperandOr = _form.getTagFilterOperandOr ();
		boolean useCase = _form.getUseCase ();

		cacheMaintenance (_form.getClearCache ()); //hack

//TODO - possible performance enhancement?? if any filter is "*" and operator=AND, set operator to OR

//TODO - need handling for tagsOut ??
		if (tagsIn.length > 0) {
			if (tagFilterOperandOr) { //OR (union) tags and filters
				String[] namesFromTags = AlbumTags.getInstance ().getNamesForTags (useCase, collapseGroups, tagsIn, tagsOut).toArray (new String[] {});

				//add any names derived from tags to filters (union)
				if (namesFromTags.length > 0) {
					filters = ArrayUtils.addAll (filters, namesFromTags);
				}

			} else { //AND (intersect) tags and filters
				//replace filters (intersection)
				filters = AlbumTags.getInstance ().getNamesForTags (useCase, collapseGroups, tagsIn, tagsOut, filters).toArray (new String[] {});
			}
		}

		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumImages.processRequest: filters.length = " + _decimalFormat0.format (filters.length) + " (after adding tags)");
		}

		//if we have to reduce the filters, always add back in the original filters
		if (filters.length > maxFilters) {
//TODO - if we are adding the original filters in at the end of this block, we should remove them at the beginning, otherwise the might end up in the list twice
			_form.addServletError ("Warning: too many filters (" + _decimalFormat0.format (filters.length) + "), reducing to " + maxFilters);
			List<String> filterList = new ArrayList<>(Arrays.asList(filters));
			Collections.shuffle(filterList); //might as well shuffle
			filters = filterList.stream()
								.limit(maxFilters - originalFilters.length)
								.sorted(VendoUtils.caseInsensitiveStringComparator)
								.collect(Collectors.toList())
								.toArray(new String[] {});
			filters = ArrayUtils.addAll (filters, originalFilters);
			_log.debug ("AlbumImages.processRequest: filters.length = " + _decimalFormat0.format (filters.length) + " (after shuffling and limiting to maxFilters)");
		}

		AlbumMode mode = _form.getMode ();

		if (filters.length == 0 && tagsIn.length == 0) { //handle some defaults
			switch (mode) {
				case DoDup:
				case DoSampler:	filters = new String[] {"*"};
				default:		break;
			}
		}

		switch (mode) {
			default:
			case DoDir:		return doDir (filters, excludes, sinceInMillis);
			case DoDup:		return doDup (filters, excludes, sinceInMillis);
			case DoSampler:	return doSampler (filters, excludes, sinceInMillis);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//rename image file on file system
	private int renameImageFiles (String wildName)
	{
		AlbumProfiling.getInstance ().enter (5);

		int imagesRemoved = 0;
		String rootPath = AlbumFormInfo.getInstance ().getRootPath (false);
		String subFolder = AlbumImage.getSubFolderFromName (wildName);

		List<String> fileList = new VFileList (rootPath + subFolder, wildName, false).getFileList (ListMode.FileOnly);
		_log.debug ("AlbumImages.renameImageFiles: renaming " + fileList.size () + " files");
		if (AlbumFormInfo._logLevel >= 7) {
			_log.debug ("AlbumImages.renameImageFiles: wildName = \"" + wildName + "\"");
			_log.debug ("AlbumImages.renameImageFiles: fileList = " + fileList);
		}

		for (String filename : fileList) {
			imagesRemoved += renameImageFile (filename);//path.getName (path.getNameCount () - 1).toString ());
		}

		AlbumProfiling.getInstance ().exit (5);

		return imagesRemoved;
	}

	///////////////////////////////////////////////////////////////////////////
	//rename image files on file system
	private int renameImageFile (String origName)
	{
//		AlbumProfiling.getInstance ().enter (5);

		int imagesRemoved = 0;
		String rootPath = AlbumFormInfo.getInstance ().getRootPath (false);
		String subFolder = AlbumImage.getSubFolderFromName (origName);
		String origPath = rootPath + subFolder + "/" + origName;
		String newPath = origPath + AlbumFormInfo._DeleteSuffix;
		if (AlbumFormInfo._logLevel >= 7) {
			_log.debug ("AlbumImages.renameImageFile: origPath \"" + origPath + "\"");
			_log.debug ("AlbumImages.renameImageFile: newPath \"" + newPath + "\"");
		}

		while (VendoUtils.fileExists (newPath)) {
			newPath += AlbumFormInfo._DeleteSuffix;
		}

		if (AlbumFormInfo._Debug) {
			_log.debug ("AlbumImages.renameImageFile: renaming \"" + origPath + "\"");
		}

		try {
			File newFile = new File (newPath);
			File origFile = new File (origPath);

			if (origFile.renameTo (newFile)) {
				imagesRemoved++;
			} else {
				String message = "rename failed (" + origFile.getCanonicalPath () + " to " + newFile.getCanonicalPath () + ")";
				_log.error ("AlbumImages.renameImageFile: " + message);
				_form.addServletError ("Error: " + message);
			}

		} catch (Exception ee) {
			_log.error ("AlbumImages.renameImageFile: error renaming file from \"" + origName + "\" to \"" + newPath + "\"", ee);
		}

//		AlbumProfiling.getInstance ().exit (5);

		return imagesRemoved;
	}

	///////////////////////////////////////////////////////////////////////////
	private int doDir (String[] filters, String[] excludes, long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumImages.doDir: exifDateIndex = " + AlbumFormInfo.getInstance ().getExifDateIndex ());
		}

		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		boolean useCase = form.getUseCase ();

		final AlbumFileFilter filter = new AlbumFileFilter (filters, excludes, useCase, sinceInMillis);

		_imageDisplayList = new LinkedList<AlbumImage> ();

		Collection<String> subFolders = AlbumImageDao.getInstance ().getAlbumSubFolders ();

		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());
		final Set<String> debugNeedsChecking = new ConcurrentSkipListSet<String> ();
		final Set<String> debugCacheMiss = new ConcurrentSkipListSet<String> ();

		AlbumProfiling.getInstance ().enter (5, "dao.doDir");
		for (final String subFolder : subFolders) {
			new Thread (() -> {
				final Collection<AlbumImage> imageDisplayList = AlbumImageDao.getInstance ().doDir (subFolder, filter, debugNeedsChecking, debugCacheMiss);
				if (imageDisplayList.size () > 0) {
					synchronized (_imageDisplayList) {
						_imageDisplayList.addAll (imageDisplayList);
					}
				}
				endGate.countDown ();
			}).start ();
		}
		try {
			endGate.await ();
		} catch (Exception ee) {
			_log.error ("AlbumImages.doDir: endGate:", ee);
		}

		AlbumProfiling.getInstance ().exit (5, "dao.doDir");

		if (form.getMode () != AlbumMode.DoDup) { //skip sorting here because doDup() will do it
			AlbumProfiling.getInstance ().enter (5, "sort " + _form.getSortType ());

			AlbumImage[] array = _imageDisplayList.toArray (new AlbumImage[] {});
			Arrays.parallelSort (array, new AlbumImageComparator (_form));
			_imageDisplayList = Arrays.asList (array);

			AlbumProfiling.getInstance ().exit (5, "sort " + _form.getSortType ());
		}

		if (form.getMode () == AlbumMode.DoDir) {
			generateExifSortCommands ();
		}

		if (form.getMode () == AlbumMode.DoDup) {
			generateDuplicateImageRenameCommands ();
		}

		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumImages.doDir: folderNeedsChecking: folders(" + debugNeedsChecking.size () + ") = " + debugNeedsChecking);
			_log.debug ("AlbumImages.doDir: cacheMiss: folders(" + debugCacheMiss.size () + ") = " + debugCacheMiss);
			_log.debug ("AlbumImages.doDir: _imageDisplayList.size = " + _decimalFormat0.format (_imageDisplayList.size ()));
		}

		AlbumProfiling.getInstance ().exit (1);

		//testing exifDate distribution
//		generateExifDateStatistics ();

		return _imageDisplayList.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private int doDup (String[] filters, String[] excludes, long sinceInMillis)
	{
		AlbumProfiling.getInstance().enterAndTrace(1);

		AlbumFormInfo form = AlbumFormInfo.getInstance();
		AlbumOrientation orientation = form.getOrientation();
		boolean collapseGroups = form.getCollapseGroups();
		boolean limitedCompare = form.getLimitedCompare();
		boolean dbCompare = form.getDbCompare();
		boolean looseCompare = form.getLooseCompare();
		boolean ignoreBytes = form.getIgnoreBytes();
		boolean useCase = form.getUseCase();
		boolean reverseSort = form.getReverseSort(); //reverses sense of limitedCompare
		boolean useExifDates = form.getUseExifDates();
		int exifDateIndex = form.getExifDateIndex();
		int maxStdDev = form.getMaxStdDev();

		int numImages = 0;
		if (dbCompare) {
			numImages = doDir(new String[]{"*"}, excludes, 0); //use "*" here, as filtering will be done below
		} else {
			numImages = doDir(filters, excludes, 0); //ignore sinceInMillis here; it will be honored below
		}

		//since we can't run this combo (way too many diffs), just return no image; it was probably a mistake on user's part
		if (form.getFilters ().length == 0 && !dbCompare && looseCompare && ignoreBytes) {
			form.addServletError ("Warning: too many comparisons; aborting");
			_imageDisplayList = new ArrayList<> ();
			return 0;
		}

		_nameOfExactDuplicateThatCanBeDeleted.clear ();
		_nameOfNearDuplicateThatCanBeDeleted.clear ();

		AlbumSortType sortType = AlbumSortType.ByNone;
		if (useExifDates) {
			sortType = AlbumSortType.ByExif;
		} else if (dbCompare) {
			sortType = AlbumSortType.ByNone; //will be sorted by db query
		} else if (ignoreBytes && !looseCompare) {
			sortType = AlbumSortType.ByHash;
		} else if (looseCompare) {
			sortType = AlbumSortType.ByNone;
		} else {
//			sortType = form.getSortType ();
			sortType = AlbumSortType.ByHash;
		}
		_log.debug ("AlbumImages.doDup: sortType = " + sortType);

		List<AlbumImage> list = new ArrayList<AlbumImage> (numImages);
		list.addAll (_imageDisplayList);

		//testing: debug code to find what is causing java.lang.IllegalArgumentException: Comparison method violates its general contract!
//		_log.debug ("AlbumImages.doDup: calling VComparators.verifyTransitivity on Collection with " + list.size () + " items...");
//		VComparators.verifyTransitivity (new AlbumImageComparator (sortType), list);

		if (sortType != AlbumSortType.ByNone) {
			AlbumProfiling.getInstance ().enter (5, "sort " + sortType);

			AlbumImage[] array = list.toArray (new AlbumImage[] {});
			Arrays.parallelSort (array, new AlbumImageComparator (sortType));
			list = Arrays.asList (array);

			AlbumProfiling.getInstance ().exit (5, "sort " + sortType);
		}

		AlbumImage[] images = list.toArray (new AlbumImage[] {});

		ArrayList<AlbumImagePair> dups = new ArrayList<AlbumImagePair> (200);

		String[] filters1 = form.getFilters (1);
		if (filters1.length == 0) {
			filters1 = new String[] {"*"};
		}
		final AlbumFileFilter filter1 = new AlbumFileFilter (filters1, excludes, useCase, sinceInMillis);

		//if looseCompare, determine work to be done
		int maxComparisons = getMaxComparisons (numImages);
		if (looseCompare && !dbCompare) {
			_log.debug ("AlbumImages.doDup: maxComparisons = " + _decimalFormat0.format (maxComparisons) + " (max possible combos, including mismatched orientation)");
			final int maxAllowedComparisons = 80 * 1000 * 1000;
			if (maxComparisons > maxAllowedComparisons) {
				form.addServletError ("Warning: too many comparisons (" + _decimalFormat0.format (maxComparisons) + "), disabling looseCompare");
				looseCompare = false;
			}
		}

		if (looseCompare && !dbCompare) {
			final List<VPair<Integer, Integer>> allCombos = getAllCombos (numImages);

			final int maxThreads = 3 * VendoUtils.getLogicalProcessors ();
			final int minPerThread = 100;
			final int chunkSize = calculateChunks (maxThreads, minPerThread, allCombos.size (), true).getFirst ();
			List<List<VPair<Integer, Integer>>> allComboChunks = ListUtils.partition (allCombos, chunkSize);
			final int numChunks = allComboChunks.size ();
			_log.debug ("AlbumImages.doDup: numChunks = " + numChunks + ", chunkSize = " + _decimalFormat0.format (chunkSize));

			long maxElapsedSecs1 = 30;
			long maxElapsedSecs2 = 30;
			final long endNano1 = System.nanoTime () + maxElapsedSecs1 * 1000 * 1000 * 1000;
			AtomicInteger timeElapsedCount1 = new AtomicInteger (0);
			AtomicInteger timeElapsedCount2 = new AtomicInteger (0);
			AtomicInteger cacheHits = new AtomicInteger (0);
			AtomicInteger cacheMisses = new AtomicInteger (0);

			final List<AlbumImagePair> pairsReady = Collections.synchronizedList (new ArrayList<AlbumImagePair> (1000));
			final List<AlbumImagePair> pairsWip = Collections.synchronizedList (new ArrayList<AlbumImagePair> (1000));

			{
				AlbumProfiling.getInstance ().enterAndTrace (5, "part 1");

				final CountDownLatch endGate = new CountDownLatch (numChunks);

				for (final List<VPair<Integer, Integer>> comboChunk : allComboChunks) {
//				_log.debug ("AlbumImages.doDup: comboChunk.size = " + _decimalFormat0.format (comboChunk.size ()));
					new Thread (() -> {
						for (VPair<Integer, Integer> vpair : comboChunk) {
							AlbumImage image1 = null;
							AlbumImage image2 = null;
							try {
								image1 = images[vpair.getFirst ()];
								image2 = images[vpair.getSecond ()];
							} catch (ArrayIndexOutOfBoundsException ee) {
								continue;
							}

							if (!image1.matchOrientation (image2.getOrientation ()) || !image1.matchOrientation (orientation)) {
								continue;
							}

							boolean sameBaseName = image1.equalBase (image2, collapseGroups);
							if (!limitedCompare || ((!reverseSort && !sameBaseName) ||
													( reverseSort &&  sameBaseName))) {
								//proceed with next test
							} else {
								continue;
							}

							//at least one of each pair compared must be accepted by filter1 (including name pattern, sinceInMillis, etc.)
							if (!filter1.accept (null, image1.getName ()) && !filter1.accept (null, image2.getName ())) {
								continue;
							}

//TODO - if this is true, can we add pair directly to ready list?
//						boolean equalAttrs = image1.equalAttrs (image2, looseCompare, ignoreBytes, useExifDates, exifDateIndex);

							String joinedNamesPlusAttrs = AlbumImagePair.getJoinedNames (image1, image2, true);
							AlbumImagePair pair = _looseCompareMap.get (joinedNamesPlusAttrs);
							if (pair != null) {
								if (AlbumImage.acceptDiff (pair.getAverageDiff (), pair.getStdDev (), maxStdDev - 5, maxStdDev)) { //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
									pairsReady.add (pair);
								}
								cacheHits.incrementAndGet ();
							} else {
								pairsWip.add (new AlbumImagePair (image1, image2));
//								_log.debug ("AlbumImages.doDup: cache miss for: " + new AlbumImagePair (image1, image2));
								cacheMisses.incrementAndGet ();
							}

							if (System.nanoTime () > endNano1) {
								if (timeElapsedCount1.incrementAndGet () == 1) { //only log first occurrence
									_log.debug ("AlbumImages.doDup: time elapsed @0 ************************************************************");
								}
								break;
							}
						}
						endGate.countDown ();
					}).start ();
				}

				try {
					endGate.await ();
				} catch (Exception ee) {
					_log.error ("AlbumTags.doDup: endGate:", ee);
				}

				AlbumProfiling.getInstance ().exit (5, "part 1");
			}

			_log.debug ("AlbumImages.doDup: pairsReady.size = " + _decimalFormat0.format (pairsReady.size ()));
			_log.debug ("AlbumImages.doDup: pairsWip.size = " + _decimalFormat0.format (pairsWip.size ()));
			_log.debug ("AlbumImages.doDup: cacheHits = " + _decimalFormat0.format (cacheHits.get ()));
			_log.debug ("AlbumImages.doDup: cacheMisses = " + _decimalFormat0.format (cacheMisses.get ()));

			///////////////////////////////////////////////////////////////////
			//determine set of images to collect scaled image data
			AlbumProfiling.getInstance ().enterAndTrace (5, "load toBeRead");
			Set<AlbumImage> toBeRead = new HashSet<AlbumImage> (pairsWip.size ());
			for (AlbumImagePair pair : pairsWip) {
				AlbumImage image1 = pair.getImage1 ();
				AlbumImage image2 = pair.getImage2 ();
				if (!_nameScaledImageMap.containsKey (image1.getNamePlusAttrs ())) {
					toBeRead.add (image1);
				}
				if (!_nameScaledImageMap.containsKey (image2.getNamePlusAttrs ())) {
					toBeRead.add (image2);
				}
			}
			AlbumProfiling.getInstance ().exit (5, "load toBeRead");

			{ //block to call readScaledImageData ()
				AlbumProfiling.getInstance ().enterAndTrace (5, "read RGB");
				final CountDownLatch endGate = new CountDownLatch (toBeRead.size ());

				for (final AlbumImage image : toBeRead) {
					Thread.currentThread ().setName (image.getName ());
					Runnable task = () -> {
						Thread.currentThread ().setName (image.getName ());
						ByteBuffer scaledImageData = image.readScaledImageData ();
						_nameScaledImageMap.put (image.getNamePlusAttrs (), scaledImageData);
						endGate.countDown ();
					};
					getExecutor ().execute (task);
				}
				try {
					endGate.await ();
				} catch (Exception ee) {
					_log.error ("AlbumImages.doDup: read RGB: endGate:", ee);
				}

				_log.debug ("AlbumImages.doDup: _nameScaledImageMap = " + _decimalFormat0.format (_nameScaledImageMap.size ()) + " of " + _decimalFormat0.format (_nameScaledImageMapMaxSize));
				AlbumProfiling.getInstance ().exit (5, "read RGB");
			}

			{ //block to call nearlyEquals ()
				AlbumProfiling.getInstance ().enterAndTrace (5, "compute diffs");

				final CountDownLatch endGate = new CountDownLatch (pairsWip.size ());

				final long endNano2 = System.nanoTime () + maxElapsedSecs2 * 1000 * 1000 * 1000;
				for (final AlbumImagePair pair : pairsWip) {
					Runnable task = () -> {
						Thread.currentThread ().setName (pair.getJoinedNames ());
						if (System.nanoTime () > endNano2) {
							if (timeElapsedCount2.incrementAndGet () == 1) { //only log first occurrence
								_log.debug ("AlbumImages.doDup: time elapsed @1 ************************************************************");
							}

						} else {
							AlbumImage image1 = pair.getImage1 ();
							AlbumImage image2 = pair.getImage2 ();
							ByteBuffer scaledImage1Data = _nameScaledImageMap.get (image1.getNamePlusAttrs ());
							ByteBuffer scaledImage2Data = _nameScaledImageMap.get (image2.getNamePlusAttrs ());

							VPair<Integer, Integer> diffPair = AlbumImage.getScaledImageDiff (scaledImage1Data, scaledImage2Data);
							int averageDiff = diffPair.getFirst ();
							int stdDev = diffPair.getSecond ();
							AlbumImagePair newPair = new AlbumImagePair (image1, image2, averageDiff, stdDev, AlbumImageDiffer.Mode.OnDemand.name ());
							if (AlbumImage.acceptDiff (averageDiff, stdDev, maxStdDev - 5, maxStdDev)) { //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
								pairsReady.add (newPair);
								_log.debug ("AlbumImages.doDup: " + newPair.getDetails3String ());
							}
							String joinedNamesPlusAttrs = AlbumImagePair.getJoinedNames (image1, image2, true);
							_looseCompareMap.put (joinedNamesPlusAttrs, newPair);
						}

						endGate.countDown ();
					};
					getExecutor ().execute (task);
				}
				try {
					endGate.await ();
				} catch (Exception ee) {
					_log.error ("AlbumImages.doDup: compute diffs: endGate:", ee);
				}

				_log.debug ("AlbumImages.doDup: pairsReady size = " + _decimalFormat0.format (pairsReady.size ()) + " items");
				_log.debug ("AlbumImages.doDup: _looseCompareMap = " + _decimalFormat0.format (_looseCompareMap.size ()) + " of " + _decimalFormat0.format (_looseCompareMapMaxSize));

				AlbumProfiling.getInstance ().exit (5, "compute diffs");
			}

			if (timeElapsedCount1.get () > 0) {
				String message = "time elapsed @0 in " + timeElapsedCount1.get () + " of " + numChunks + " threads; showing partial results";
				_log.warn ("AlbumImages.doDup: " + message + " ************************************************************");
				form.addServletError ("Warning: " + message);
			}
			if (timeElapsedCount2.get () > 0) {
				String message = "time elapsed @1 in " + _decimalFormat0.format (timeElapsedCount2.get ()) + " of " + _decimalFormat0.format (pairsWip.size ()) + " threads; showing partial results";
				_log.warn ("AlbumImages.doDup: " + message + "  ************************************************************");
				form.addServletError ("Warning: " + message);
			}

			AlbumProfiling.getInstance ().enterAndTrace (5, "dups.add");

			for (AlbumImagePair imagePair : pairsReady) {
				dups.add (imagePair);
				_log.debug ("AlbumImages.doDup: " + imagePair.getDetails3String ());
			}

			AlbumProfiling.getInstance ().exit (5, "dups.add");
		}

		if (!looseCompare || dbCompare) {
			if (dbCompare) {
				AlbumProfiling.getInstance ().enterAndTrace (5, "dups.db");

				//generate map of imageNames->images
				Map<String, AlbumImage> map = _imageDisplayList.stream ().collect (Collectors.toMap (AlbumImage::getName, i -> i));

				final double sinceDays = _form.getHighlightDays (); //TODO - using highlightDays is a HACK
				final boolean operatorOr = _form.getTagFilterOperandOr (); //TODO - using tagFilterOperandOr is a HACK
				//debugging
				long highlightInMillis = _form.getHighlightInMillis (false);
				_log.debug ("AlbumImages.doDup: since/highlight date/time: " + _dateFormat.format (new Date (highlightInMillis)));

				AlbumImageDiffer albumImageDiffer = AlbumImageDiffer.getInstance ();
				Collection<AlbumImageDiffData> imageDiffData = albumImageDiffer.selectNamesFromImageDiffs (maxStdDev - 5, maxStdDev, sinceDays, operatorOr); //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
				_log.debug ("AlbumImages.doDup: imageDiffData.size(): " + _decimalFormat0.format (imageDiffData.size ()));

				for (AlbumImageDiffData item : imageDiffData) {
					AlbumImage image1 = map.get (item.getName1 ());
					AlbumImage image2 = map.get (item.getName2 ());
					//images can be null if excludes was used
					if (image1 != null && image2 != null && image1.matchOrientation (image2.getOrientation ()) && image1.matchOrientation (orientation)) {
						//at least one of each pair must be accepted by filter1
						if (filter1.accept (null, image1.getName ()) || filter1.accept (null, image2.getName ())) {
							boolean sameBaseName = image1.equalBase (image2, collapseGroups);
							if (!limitedCompare || ((!reverseSort && !sameBaseName) ||
													( reverseSort &&  sameBaseName))) {
								AlbumImagePair pair = new AlbumImagePair (image1, image2, item.getAverageDiff (), item.getStdDev (), item.getSource (), item.getLastUpdate());
								dups.add (pair);
								String joinedNamesPlusAttrs = AlbumImagePair.getJoinedNames (image1, image2, true);
								_looseCompareMap.put (joinedNamesPlusAttrs, pair);
							}
						}
					}
				}

				_log.debug ("AlbumImages.doDup: _looseCompareMap = " + _decimalFormat0.format (_looseCompareMap.size ()) + " of " + _decimalFormat0.format (_looseCompareMapMaxSize));

				AlbumProfiling.getInstance ().exit (5, "dups.db");

			} else if (useExifDates) {
				AlbumProfiling.getInstance ().enterAndTrace (5, "dups.exif");

				Set<AlbumImagePair> dupSet = new HashSet<> (); //use Set to avoid duplicate AlbumImagePairs

				//check for exif dups using all valid values of NumFileExifDates
//				for (int ii = 0; ii < AlbumImage.NumFileExifDates; ii++) {
				for (int ii = 0; ii < AlbumImage.NumExifDates; ii++) {
					//get a new list, sorted for each exifDateIndex
					List<AlbumImage> list1 = new ArrayList<AlbumImage> (numImages);
					list1.addAll (_imageDisplayList);
					list1.sort (new AlbumImageComparator (sortType, ii));
					AlbumImage[] images1 = list1.toArray (new AlbumImage[] {});

					AlbumImage prevImage = null;
					for (AlbumImage image : images1) {
						if (image != null && prevImage != null) {
							//at least one of each pair compared must be accepted by filter1 (including name pattern and sinceInMillis)
							if (filter1.accept (null, image.getName ()) || filter1.accept (null, prevImage.getName ())) {
								if (image.equalAttrs (prevImage, looseCompare, ignoreBytes, useExifDates, ii)) {
									boolean sameBaseName = image.equalBase (prevImage, collapseGroups);
									if (!limitedCompare || ((!reverseSort && !sameBaseName) ||
															( reverseSort &&  sameBaseName))) {
										dupSet.add (new AlbumImagePair (image, prevImage));
									}
								}
							}
						}
						prevImage = image;
					}
				}
				dups.addAll (dupSet);

				AlbumProfiling.getInstance ().exit (5, "dups.exif");

			} else { //!dbCompare && !useExifDates
				AlbumImage prevImage = null;
				for (AlbumImage image : images) {
					if (image != null && prevImage != null) {
						//at least one of each pair compared must be accepted by filter1 (including name pattern and sinceInMillis)
						if (filter1.accept (null, image.getName ()) || filter1.accept (null, prevImage.getName ())) {
							if (image.equalAttrs (prevImage, looseCompare, ignoreBytes, useExifDates, exifDateIndex)) {
								boolean sameBaseName = image.equalBase (prevImage, collapseGroups);
								if (!limitedCompare || ((!reverseSort && !sameBaseName) ||
														( reverseSort &&  sameBaseName))) {
									dups.add (new AlbumImagePair (image, prevImage));
								}
							}
						}
					}
					prevImage = image;
				}
			}
		}

		_log.debug ("AlbumImages.doDup: dups.size = " + _decimalFormat0.format (dups.size ()) + " pairs");

		//log all duplicate sets
		if (dups.size () < 800) {
			AlbumProfiling.getInstance ().enter (5, "dups.sets");

			Map<String, AlbumImageSet> dupImageMap = new HashMap<> ();
			for (AlbumImagePair pair : dups) {
				boolean sameBaseName = pair.getImage1 ().equalBase (pair.getImage2 (), collapseGroups);
				if (!sameBaseName) {
					String joinedNames = pair.getJoinedNames ();
					AlbumImageSet dupSet = dupImageMap.get (joinedNames);
					if (dupSet != null) {
						dupSet.addPair (pair);
					} else {
						dupImageMap.put (joinedNames, new AlbumImageSet (joinedNames, pair));
					}
				}
			}
/*
//			Set<AlbumImageSet> dupImageMap = new HashSet<> ();
//			Set<Set<String>> dupImageMap = new HashSet<Set<String>> ();
			for (AlbumImagePair pair1 : dups) {
				for (AlbumImagePair pair2 : dups) {
					if (pair1.matchesAtLeastOneImage (pair2)) {
//						Set<String> imageSet = null;
						AlbumImageSet imageSet = null;
						for (AlbumImageSet set : dupImageMap) {
							if (set.matchesAtLeastOneImage (pair1) || set.matchesAtLeastOneImage (pair2)) {
//							if (pair1.matchesAtLeastOneImage (set) || pair2.matchesAtLeastOneImage (set)) {
								imageSet = set;
								break;
							}
						}
						if (imageSet == null) {
							imageSet = new AlbumImageSet ();
							dupImageMap.add (imageSet);
						}

						imageSet.addPairs (pair1, pair2);
//						imageSet.add (pair1.getImage1 ().getBaseName (false));
//						imageSet.add (pair1.getImage2 ().getBaseName (false));
//						imageSet.add (pair2.getImage1 ().getBaseName (false));
//						imageSet.add (pair2.getImage2 ().getBaseName (false));
					}
				}
			}
*/
/*
			Set<String> sortedDupDetailStringSet = new TreeSet<> (alphanumComparator);
//			Set<String> sortedDupDetailStringSet = new TreeSet<String> (VendoUtils.caseInsensitiveStringComparator);
//			for (Set<String> set : dupImageMap) {
			for (AlbumImageSet imageSet : dupImageMap.values()) {
				sortedDupDetailStringSet.add (imageSet.getDetailString());
			}

*/
			//log/print/display all duplicate sets
			AlphanumComparator alphanumComparator = new AlphanumComparator ();
			List<String> sortedDupDetailStringSet = dupImageMap.values().stream()
					.map(AlbumImageSet::getDetailString)
					.sorted(alphanumComparator)
					.collect(Collectors.toList());

			_log.debug ("AlbumImages.doDup: sortedDupDetailStringSet.size = " + _decimalFormat0.format (sortedDupDetailStringSet.size ()) + " dup sets ----------------------------------------");
			if (sortedDupDetailStringSet.size () > 0 && sortedDupDetailStringSet.size () < 800) {
				if (!dbCompare) {
					_form.addServletError ("Info: found duplicate sets: " + _decimalFormat0.format (sortedDupDetailStringSet.size ()));
				}
				for (String string : sortedDupDetailStringSet) {
					_log.debug (string);
					if (!dbCompare) {
						_form.addServletError ("Info: found duplicate sets: " + string);
					}
				}
			}

			//log/print/display all exact duplicates
			List<String> nameOfExactDuplicateThatCanBeDeletedList = new ArrayList<>();
			List<String> nameOfNearDuplicateThatCanBeDeletedList = new ArrayList<>();
			for (AlbumImageSet imageSet : dupImageMap.values()) {
				if (imageSet.secondAlbumRepresentsExactDuplicate()) {
					String secondBaseNameForSorting = imageSet.getBaseName(1); //select the larger (numerically)
					_nameOfExactDuplicateThatCanBeDeleted.add(secondBaseNameForSorting);
					nameOfExactDuplicateThatCanBeDeletedList.add("Info: found albums that can be deleted (exact duplicate pair): [" + secondBaseNameForSorting + "] - " + imageSet.getDetailString());
				} else if (imageSet.secondAlbumRepresentsNearDuplicate ()) {
					String secondBaseNameForSorting = imageSet.getBaseName(1); //select the larger (numerically)
					_nameOfNearDuplicateThatCanBeDeleted.add(secondBaseNameForSorting);
					nameOfNearDuplicateThatCanBeDeletedList.add("Info: found albums that can be deleted (near duplicate pair): [" + secondBaseNameForSorting + "] - " + imageSet.getDetailString());
				}
			}
			if (!nameOfExactDuplicateThatCanBeDeletedList.isEmpty()) {
				nameOfExactDuplicateThatCanBeDeletedList.sort(alphanumComparator);

				_form.addServletError("Info: found albums that can be deleted (exact duplicate pair): " + nameOfExactDuplicateThatCanBeDeletedList.size());
				nameOfExactDuplicateThatCanBeDeletedList.forEach(s -> _form.addServletError(s));

				_log.debug ("AlbumImages.doDup: nameOfExactDuplicateThatCanBeDeletedList.size = " + _decimalFormat0.format (nameOfExactDuplicateThatCanBeDeletedList.size ()) + " exact dup sets -----------------------------------");
				nameOfExactDuplicateThatCanBeDeletedList.forEach(s -> _log.debug(s));
			}

			if (!nameOfNearDuplicateThatCanBeDeletedList.isEmpty()) {
				nameOfNearDuplicateThatCanBeDeletedList.sort(alphanumComparator);

				_form.addServletError("Info: found albums that can be deleted (near duplicate pair): " + nameOfNearDuplicateThatCanBeDeletedList.size());
				nameOfNearDuplicateThatCanBeDeletedList.forEach(s -> _form.addServletError(s));

				_log.debug ("AlbumImages.doDup: nameOfNearDuplicateThatCanBeDeletedList.size = " + _decimalFormat0.format (nameOfNearDuplicateThatCanBeDeletedList.size ()) + " near dup sets -----------------------------------");
				nameOfNearDuplicateThatCanBeDeletedList.forEach(s -> _log.debug(s));
			}

			AlbumProfiling.getInstance ().exit (5, "dups.sets");
		}
/*
		//transfer pair info from AlbumImagePair to AlbumImagesPair
		if (dups.size () < 800) {
			AlbumProfiling.getInstance ().enter (5, "dups.pairs");

			Map<String, AlbumImagesPair> dupPairs = new HashMap<String, AlbumImagesPair> ();
			for (AlbumImagePair pair : dups) {
				boolean sameBaseName = pair.getImage1 ().equalBase (pair.getImage2 (), collapseGroups);
				if (!sameBaseName) {
					String joinedNames = AlbumImagesPair.getJoinedNames (pair.getImage1 (), pair.getImage2 ());
					AlbumImagesPair dupPair = dupPairs.get (joinedNames);
					if (dupPair != null) {
						dupPair.incrementNumberOfDuplicateMatches (pair.getImage1 ().compareToByPixels (pair.getImage2 ())); //pass pixelDiff
					} else {
						dupPairs.put (joinedNames, new AlbumImagesPair(pair.getImage1 (), pair.getImage2 ()));
					}
				}
			}

			List<AlbumImagesPair> sorted = dupPairs.values ().stream ()
					.filter (AlbumImagesPair::pairRepresentsDuplicates) //TODO: redundant??
					.sorted ((p1, p2) -> p1.getJoinedNames ().compareToIgnoreCase (p2.getJoinedNames ()))
					.collect (Collectors.toList ());

			_log.debug ("AlbumImages.doDup: dupPairs.size = " + _decimalFormat0.format (sorted.size ()) + " dup pairs ----------------------------------------");
			AlphanumComparator alphanumComparator = new AlphanumComparator ();
			if (sorted.size () > 0) {
				List<String> nameOfExactDuplicateThatCanBeDeletedList = new ArrayList<>();
				for (AlbumImagesPair pair : sorted) {
					if (pair.secondAlbumRepresentsExactDuplicate ()) {
						String baseName1 = pair.getImage1().getBaseName(false);
						String baseName2 = pair.getImage2().getBaseName(false);
						String secondBaseNameForSorting = (alphanumComparator.compare(baseName1, baseName2) < 0 ? baseName2 : baseName1); //select the larger (numerically)
						_log.debug(pair);
//						nameOfExactDuplicateThatCanBeDeletedList.add("Info: found exact duplicate pair: [" + secondBaseNameForSorting + "] - " + pair);
						_nameOfExactDuplicateThatCanBeDeleted.add(secondBaseNameForSorting);
						nameOfExactDuplicateThatCanBeDeletedList.add("Info: found albums that can be deleted (exact duplicate pair): [" + secondBaseNameForSorting + "] - " + pair);
					}
				}
				if (!nameOfExactDuplicateThatCanBeDeletedList.isEmpty()) {
					_form.addServletError("Info: found albums that can be deleted (exact duplicate pair): " + nameOfExactDuplicateThatCanBeDeletedList.size());
					nameOfExactDuplicateThatCanBeDeletedList.forEach(s -> _form.addServletError(s));
				}
			}

			AlbumProfiling.getInstance ().exit (5, "dups.pairs");
		}
*/
		if (dbCompare) {
			_imageDisplayList = AlbumImagePair.getImages (dups, AlbumSortType.ByNone); //already sorted by db query
		} else {
			_imageDisplayList = AlbumImagePair.getImages (dups, AlbumSortType.ByDate); //shows newest first in browser
		}

		AlbumProfiling.getInstance ().exit (1);

		return _imageDisplayList.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private int doSampler (String[] filters, String[] excludes, long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		int numImages = doDir (filters, excludes, sinceInMillis);

		List<AlbumImage> list = new ArrayList<AlbumImage> (numImages);
		list.addAll (_imageDisplayList);
		AlbumImage[] images = list.toArray (new AlbumImage[] {});

		boolean collapseGroups = _form.getCollapseGroups ();

//TODO - eliminate dups (for example don't add both file01-* and file*)
		List<AlbumImage> sampler = new ArrayList<AlbumImage> (numImages / 3);
		Set<String> set = new HashSet<String> (numImages / 3);
		for (AlbumImage image : images) {
			String wildName = image.getBaseName (collapseGroups) + "*";
			if (set.add (wildName)) { //returns true if added, false if dup
				sampler.add (image);
			}
		}
//		_imageDisplayList = new TreeSet<AlbumImage> (new AlbumImageComparator (_form));
		_imageDisplayList = new ArrayList<AlbumImage> (sampler.size ());
		_imageDisplayList.addAll (sampler);
//TODO - sort here?

		AlbumProfiling.getInstance ().exit (1);

		return _imageDisplayList.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getMaxComparisons (long numImages)
	{
		long maxComparisons = 1;
		if (numImages > 2) {
			maxComparisons = numImages * (numImages - 1) / 2;
		}

		if (maxComparisons > Integer.MAX_VALUE) {
			maxComparisons = Integer.MAX_VALUE;
		}

		return (int) maxComparisons;
	}

	///////////////////////////////////////////////////////////////////////////
	//tries to honor minPerChunk, but never more than maxChunks
	//returns <chunkSize, numChunks>
	public static VPair<Integer, Integer> calculateChunks (int maxChunks, int minPerChunk, int numItems)
	{
		return calculateChunks (maxChunks, minPerChunk, numItems, false);
	}
	public static VPair<Integer, Integer> calculateChunks (int maxChunks, int minPerChunk, int numItems, boolean verbose)
	{
		int numChunks = 0;
		int chunkSize = 0;

		if (numItems == 0) { //special case
			numChunks = 0;
			chunkSize = 1;

		} else if (numItems <= minPerChunk) {
			numChunks = 1;
			chunkSize = numItems;

		} else if (numItems <= maxChunks * minPerChunk) {
			numChunks = numItems / minPerChunk;
			chunkSize = VendoUtils.roundUp ((double) numItems / numChunks);

		} else {
			numChunks = maxChunks;
			chunkSize = VendoUtils.roundUp ((double) numItems / numChunks);
		}

		if (verbose) {
			_log.debug ("AlbumImages.calculateChunks: numItems = " + _decimalFormat0.format (numItems) + ", numChunks = " + _decimalFormat0.format (numChunks) + ", chunkSize = " + _decimalFormat0.format (chunkSize));
		}

		return VPair.of (chunkSize, numChunks);
	}

	///////////////////////////////////////////////////////////////////////////
	//generate list of all possible combos <- old comment
	public List<VPair<Integer, Integer>> getAllCombos (int numImages)
	{
		AlbumProfiling.getInstance ().enter (5);

//		if (numImages > _allCombosNumImages || _allCombos == null) {
//			numImages = (int) MathUtils.round ((double) numImages, -3, BigDecimal.ROUND_UP); //round up to next thousand
		numImages = (int) MathUtils.round ((double) numImages, -2, BigDecimal.ROUND_UP); //round up to next hundred

//			_log.debug ("AlbumImages.getAllCombos: cache miss: new numImages = " + _decimalFormat0.format (numImages));

		int maxComparisons = getMaxComparisons (numImages);
		_allCombos = new ArrayList<VPair<Integer, Integer>> (maxComparisons);
//		_allCombosNumImages = numImages;

		for (int i0 = 0; i0 < numImages; i0++) {
			for (int i1 = i0 + 1; i1 < numImages; i1++) {
				_allCombos.add (VPair.of (i0, i1));
			}
		}

		_log.debug ("AlbumImages.getAllCombos: numImages = " + _decimalFormat0.format (numImages) + ",  maxComparisons = " + _decimalFormat0.format (maxComparisons) + ", _allCombos.size() = " + _decimalFormat0.format (_allCombos.size ()));
//		}

		AlbumProfiling.getInstance ().exit (5);

		return _allCombos;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumRows ()
	{
		int cols = _form.getColumns ();
		int rows = _form.getPanels () / cols;
		if ((rows * cols) != _form.getPanels ()) {
			rows++; //handle case where columns is not evenly divisible by panels
		}

		return rows;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumSlices ()
	{
		int numImages = _imageDisplayList.size ();
		if (numImages == 0) {
			return 1;
		}

		int cols = _form.getColumns ();
		int rows = getNumRows ();

		int imagesPerSlice = rows * cols;
		int numSlices = numImages / imagesPerSlice;
		if ((numSlices * imagesPerSlice) != numImages) {
			numSlices++; //handle case where numImages is not evenly divisible by imagesPerSlice
		}

		return numSlices;
	}

	///////////////////////////////////////////////////////////////////////////
	//web color picker: http://www.colorpicker.com/
	public String getBgColor ()
	{
		switch (_form.getMode ()) {
			default:
			case DoDir:		return "#FDF5E5";
			case DoSampler:	return "#C0C0B0";
			case DoDup:
				if (_form.getDbCompare ()) {
					return "#478D70";
				} else if (_form.getLooseCompare ()) {
					return "#E37BCE";
				} else {
					return "#8DF5F6";
				}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFontColor (AlbumImage image) {
		String fontColor = "black"; //just right
		if (image.getWidth() < _form.getHighlightMinPixels () || image.getHeight() < _form.getHighlightMinPixels ()) {
			fontColor = "red"; //too small (by pixels)
		} else if (image.getNumBytes() > _form.getHighlightMaxKilobytes () * 1024L) {
			fontColor = "magenta"; //too large (by bytes)
		} else if (image.getTagString (_form.getCollapseGroups ()).isEmpty ()) {
			fontColor = "darkred"; //no tags defined
		}

		return fontColor;
	}

	///////////////////////////////////////////////////////////////////////////
	public String generatePageLinks ()
	{
		final int MaxSliceLinks = 40; //limit number of slice link shown

		int numSlices = getNumSlices ();
		if (numSlices == 1) {
			return "";
		}

		int currentSlice = _form.getSlice ();

		int firstSlice = 0;
		int lastSlice = numSlices;// - 1;
		if (numSlices < MaxSliceLinks) {
			//done - defaults set above are correct

		} else if (currentSlice < MaxSliceLinks / 2) {
			//root first slice at 0
			firstSlice = 0;
			lastSlice = MaxSliceLinks;

		} else if (currentSlice > numSlices - MaxSliceLinks / 2) {
			//root last slice at numSlices
			firstSlice = numSlices - MaxSliceLinks;
			lastSlice = numSlices;

		} else {
			//spread slices evenly around currentSlice
			firstSlice = currentSlice - MaxSliceLinks / 2;
			lastSlice = currentSlice + MaxSliceLinks / 2;
		}

		//add slice links
		StringBuilder sb = new StringBuilder (200 * numSlices);
		for (int ii = firstSlice; ii < lastSlice; ii++) {
			int slice = ii + 1; //note slice is 1-based in UI
			String text = (slice == currentSlice ? "<B>Current</B>" : Integer.toString (slice));
			sb.append (generatePageLinksHelper (slice, text));
		}

		//add other navigation links
		if (firstSlice > 1) {
			sb.append (generatePageLinksHelper (1, "[First]"));
		}
		if (currentSlice > 1) {
			sb.append (generatePageLinksHelper (currentSlice - 1, "[Previous]"));
		}
		if (currentSlice < numSlices) {
			sb.append (generatePageLinksHelper (currentSlice + 1, "[Next]"));
		}
		if (lastSlice < numSlices) {
			sb.append (generatePageLinksHelper (numSlices, "[Last]"));
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String generatePageLinksHelper (int slice, String text)
	{
		String server = AlbumFormInfo.getInstance ().getServer ();

		//add timestamp to URL to always causes server hit and avoid caching
		long nowInMillis = new GregorianCalendar ().getTimeInMillis ();
		String timestamp = String.valueOf (nowInMillis);

		StringBuilder sb = new StringBuilder (200);
		for (int ii = 0; ii < AlbumFormInfo._NumTagParams; ii++) {
			sb.append ("&tagMode").append (ii).append ("=").append (_form.getTagMode (ii).getSymbol ());
			sb.append ("&tag").append (ii).append ("=").append (_form.getTag (ii));
		}
		String tagParams = sb.toString ();

		/*StringBuilder*/ sb = new StringBuilder (200);
		sb.append (_spacing).append (NL)
			.append ("<A HREF=\"").append (server)
			.append ("?mode=").append (_form.getMode ().getSymbol ())
			.append ("&filter1=").append (_form.getFilter1 ())
			.append ("&filter2=").append (_form.getFilter2 ())
			.append ("&filter3=").append (_form.getFilter3 ())
			.append (tagParams)
			.append ("&exclude1=").append (_form.getExclude1 ())
			.append ("&exclude2=").append (_form.getExclude2 ())
			.append ("&exclude3=").append (_form.getExclude3 ())
			.append ("&columns=").append (_form.getColumns ())
			.append ("&sortType=").append (_form.getSortType ().getSymbol ())
			.append ("&panels=").append (_form.getPanels ())
			.append ("&sinceDays=").append (_form.getSinceDays ())
			.append ("&maxFilters=").append (_form.getMaxFilters ())
			.append ("&highlightDays=").append (_form.getHighlightDays ())
			.append ("&exifDateIndex=").append (_form.getExifDateIndex ())
			.append ("&maxStdDev=").append (_form.getMaxStdDev ())
			.append ("&rootFolder=").append (_form.getRootFolder ())
			.append ("&tagFilterOperandOr=").append (_form.getTagFilterOperandOr ())
			.append ("&collapseGroups=").append (_form.getCollapseGroups ())
			.append ("&limitedCompare=").append (_form.getLimitedCompare ())
			.append ("&dbCompare=").append (_form.getDbCompare ())
			.append ("&looseCompare=").append (_form.getLooseCompare ())
			.append ("&ignoreBytes=").append (_form.getIgnoreBytes ())
			.append ("&useExifDates=").append (_form.getUseExifDates ())
			.append ("&orientation=").append (_form.getOrientation ().getSymbol ())
			.append ("&useCase=").append (_form.getUseCase ())
			.append ("&reverseSort=").append (_form.getReverseSort ())
			.append ("&screenWidth=").append (_form.getScreenWidth ())
			.append ("&screenHeight=").append (_form.getScreenHeight ())
			.append ("&windowWidth=").append (_form.getWindowWidth ())
			.append ("&windowHeight=").append (_form.getWindowHeight ())
			.append ("&timestamp=").append (timestamp)
			.append ("&slice=").append (slice)
			.append (AlbumFormInfo._Debug ? "&debug=on" : "")
			.append ("#topAnchor")
			.append ("\">")
			.append (text)
			.append ("</A>").append (NL);

//TODO	return server + encodeUrl (sb.toString ());
		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	//handles max of two filters (an image, and optionally its pair)
	public String generateImageLink (String filter1, String filter2, AlbumMode mode, int columns, double sinceDays, boolean limitedCompare, boolean looseCompare)
	{
		String server = AlbumFormInfo.getInstance ().getServer ();

		int panels = _form.getPanels ();
		String filters = filter1 + "," + filter2;
		String limitedCompareStr = limitedCompare ? "&limitedCompare=true" : "";
		String looseCompareStr = looseCompare ? "&looseCompare=true" : "";

		//apparently we only propagate the sortType if it is by size (bytes or pixels); otherwise we use the default sortType
		AlbumSortType sortType = _form.getSortType ();
		String sortTypeStr = (sortType == AlbumSortType.BySizeBytes || sortType == AlbumSortType.BySizePixels) ? "&sortType=" + sortType.getSymbol () : "";

		StringBuilder sb = new StringBuilder (200);
		sb.append ("?mode=").append (mode.getSymbol ())
			.append ("&filter1=").append (filters)
			.append ("&filter2=").append (filters)
//TODO - propagate tags?
//TODO - propagate excludes?
			.append ("&columns=").append (columns)
			.append (sortTypeStr)
			.append ("&panels=").append (panels)
			.append ("&sinceDays=").append (sinceDays)
			.append ("&maxFilters=").append (_form.getMaxFilters ())
			.append ("&highlightDays=").append (_form.getHighlightDays ())
			.append ("&exifDateIndex=").append (_form.getExifDateIndex ())
			.append ("&maxStdDev=").append (_form.getMaxStdDev ())
			.append ("&rootFolder=").append (_form.getRootFolder ())
//		  	.append ("&tagFilterOperandOr=").append (_form.getTagFilterOperandOr ())
//		  	.append ("&collapseGroups=").append (_form.getCollapseGroups ())
//		  	.append ("&limitedCompare=").append (_form.getLimitedCompare ())
			.append (limitedCompareStr)
//		  	.append ("&dbCompare=").append (_form.getDbCompare ())
//			.append ("&looseCompare=").append (_form.getLooseCompare ())
			.append (looseCompareStr)
			.append ("&ignoreBytes=").append (_form.getIgnoreBytes ())
			.append ("&useExifDates=").append (_form.getUseExifDates ())
//		  	.append ("&orientation=").append (_form.getOrientation ().getSymbol ())
			.append ("&useCase=").append (_form.getUseCase ())
//		  	.append ("&reverseSort=").append (_form.getReverseSort ())
			.append ("&screenWidth=").append (_form.getScreenWidth ())
			.append ("&screenHeight=").append (_form.getScreenHeight ())
			.append ("&windowWidth=").append (_form.getWindowWidth ())
			.append ("&windowHeight=").append (_form.getWindowHeight ())
			.append ("&slice=").append (1)
			.append (AlbumFormInfo._Debug ? "&debug=on" : "")
			.append ("#topAnchor");

		return server + VendoUtils.escapeHtmlChars (sb.toString ());
//		return server + escapeHtml4 (sb.toString ()); //Nope: this only encodes four? chars like <, >, &, etc.
	}

	///////////////////////////////////////////////////////////////////////////
	public String generateTitle ()
	{
		String title = "Album";

		int numImages = _imageDisplayList.size ();
		if (numImages > 0) {
			title += " (" + numImages + ")";
		}

		if (_form.getMode () == AlbumMode.DoDup) {
			title += " - " + AlbumMode.DoDup.getSymbol ();

		} else {
			StringBuilder sb = new StringBuilder ();

			String[] filters = _form.getFilters ();
			for (String filter : filters) {
				if (filter.length () > 1 && filter.endsWith ("*")) {
					filter = filter.substring (0, filter.length () - 1); //strip trailing "*", unless "*" is the entire filter
				}

				if (filter.length () > 0) {
					sb.append (",").append (filter);
				}
			}

			String[] tagsIn = _form.getTags (AlbumTagMode.TagIn);
			for (String tagIn : tagsIn) {
				sb.append (",").append (tagIn);
			}

			int startIndex = (sb.length () > 1 ? 1 : 0); //skip initial comma, if there
			String filterTitle = sb.substring (startIndex);

			if (filterTitle.length () == 0 && numImages > 0) {
				filterTitle = "*";
			}

			if (filterTitle.length () > 0) {
				title += " - " + filterTitle;
			}
		}

		return title;
	}

	///////////////////////////////////////////////////////////////////////////
	public String generateHtml ()
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		int numImages = _imageDisplayList.size ();
		if (AlbumFormInfo._Debug) {
			_log.debug ("AlbumImages.generateHtml: numImages = " + numImages);
		}

		boolean isAndroidDevice = _form.isAndroidDevice ();

		String font1 = !isAndroidDevice ? "fontsize10" : "fontsize24";
		String tableWidthString = !isAndroidDevice ? "100%" : "1600"; //TODO - hardcoded

		String tagsMarker = "<tagsMarker>";
		String servletErrorsMarker = "<servletErrorsMarker>";

		if (numImages == 0) {
			StringBuilder sb1 = new StringBuilder (200);
			sb1.append ("<TABLE WIDTH=")
					.append (tableWidthString)
					.append (" CELLPADDING=0 CELLSPACING=0 BORDER=")
					.append (_tableBorderPixels)
					.append (">").append (NL)
					.append ("<TR>").append (NL)
					.append (servletErrorsMarker)
					.append ("<TD class=\"")
					.append (font1)
					.append ("\" ALIGN=RIGHT>")
//TODO - this only work for albums that were drilled into??
					.append ("<A HREF=\"#\" onClick=\"self.close();\">Close</A>").append (NL)
					.append ("</TD>").append (NL)
					.append ("</TR>").append (NL)
					.append ("</TABLE>").append (NL)
					.append ("No images").append (NL);
			String htmlString = sb1.toString ();

			//replace servletErrorsMarker with any servlet errors
			htmlString = htmlString.replace (servletErrorsMarker, getServletErrorsHtml ());

			AlbumProfiling.getInstance ().exit (1);
			return htmlString;
		}

		String imageUrlPath = AlbumFormInfo.getInstance ().getRootPath (true);

		AlbumMode mode = _form.getMode ();
		int defaultCols = !isAndroidDevice ? _form.getDefaultColumns () : 1; //TODO - hardcoded
		int cols = _form.getColumns ();
		int rows = getNumRows ();
		int numSlices = getNumSlices ();
		int slice = _form.getSlice ();
		boolean looseCompare = _form.getLooseCompare ();
		boolean collapseGroups = _form.getCollapseGroups ();
		//for mode = AlbumMode.DoDup, disable collapseGroups for tags
		boolean collapseGroupsForTags = mode == AlbumMode.DoDup ? false : collapseGroups;
		AlbumDuplicateHandling duplicateHandling = _form.getDuplicateHandling ();

		//reduce number of columns when it is greater than number of images
		if (cols > numImages) {
			cols = numImages;
		}

		int start = (slice - 1) * (rows * cols);
		int end = start + (rows * cols);

		if (start >= numImages) {
			_log.debug ("AlbumImages.generateHtml: no images in slice; reset slice to 1");

			slice = 1;
			_form.setSlice (slice);
			start = 0;//(slice - 1) * (rows * cols);
			end = start + (rows * cols);
		}

		double sinceDays = _form.getSinceDays ();
		long sinceInMillis = _form.getSinceInMillis (true);
		String sinceStr = sinceInMillis > 0 ? " (since " + _dateFormat.format (new Date (sinceInMillis)) + ")" : "";

//		int highlightMinPixels = _form.getHighlightMinPixels ();
//		int highlightMaxKilobytes = _form.getHighlightMaxKilobytes ();
		long highlightInMillis = _form.getHighlightInMillis (true);
		String highlightStr = highlightInMillis > 0 ? " (highlight " + _dateFormat.format (new Date (highlightInMillis)) + ")" : "";

		String pageLinksHtml = generatePageLinks () + _spacing;

		//header, slice links, and close link
		StringBuilder sb = new StringBuilder (8 * 1024);
		sb.append ("<A NAME=\"topAnchor\"></A>").append (NL);

		sb.append ("<TABLE WIDTH=")
				.append (tableWidthString)
				.append (" CELLPADDING=0 CELLSPACING=0 BORDER=")
				.append (_tableBorderPixels)
				.append (">").append (NL);

		sb.append (servletErrorsMarker);

		sb.append ("<TR>").append (NL)
				.append ("<TD class=\"")
				.append (font1)
				.append ("\" ALIGN=LEFT>")
				.append ("<NOBR>")
				.append ("Images ")
				.append (_decimalFormat0.format (start + 1))
				.append (" through ")
				.append (_decimalFormat0.format (Math.min (end, numImages)))
				.append (" of ")
				.append (_decimalFormat0.format (numImages))
				.append (" (")
				.append (numSlices)
				.append (numSlices == 1 ? " slice)": " slices)")
				.append (sinceStr)
				.append (highlightStr)
				.append ("</NOBR>")
				.append (tagsMarker)
				.append ("</TD>").append (NL)
				.append ("<TD class=\"")
				.append (font1)
				.append ("\" ALIGN=RIGHT>")
				.append (pageLinksHtml).append (NL)
//TODO - this only work for albums that were drilled into??
				.append ("<A HREF=\"#\" onClick=\"self.close();\">Close</A>").append (NL)
				.append ("</TD>").append (NL)
				.append ("</TR>").append (NL);

		sb.append ("</TABLE>").append (NL);

		final int padding = 2;
		final int scrollBarWidth = 25 + 7; //add a bit of spacing between images and scroll bar

//		int imageBorderPixels = (mode == AlbumMode.DoDup && looseCompare ? 2 : 1);
		int imageBorderPixels = (mode == AlbumMode.DoDup ? 2 : 1);
		int imageOutlinePixels = (mode == AlbumMode.DoSampler && (!_nameOfExactDuplicateThatCanBeDeleted.isEmpty() || !_nameOfNearDuplicateThatCanBeDeleted.isEmpty()) ? 2 : 0);
		String imageBorderColorStr = "blue";
		String imageBorderStyleStr = "solid"; //solid / dotted / dashed
		String imageOutlineStr = "";

		//if more than maxColumns requested, force horizontal scroll bar
		int tempCols = Math.min (cols, _form.getMaxColumns ());

		int tableWidth = 0;
		int imageWidth = 0;
		int imageHeight = 0;

		if (isAndroidDevice) {
			//using _form.getScreenWidth () did not work
			tableWidth = (_form.getWindowWidth () > _form.getWindowHeight () ? 2560 : 1600); //hack - hardcode
			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + imageOutlinePixels + padding));
			imageHeight = (_form.getWindowWidth () > _form.getWindowHeight () ? 1600 : 2560); //hack - hardcode

		} else {
			//values for firefox
			tableWidth = _form.getWindowWidth () - scrollBarWidth;
			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + imageOutlinePixels + padding));
//			imageHeight = _form.getWindowHeight () - 40; //for 1080p
//			imageHeight = _form.getWindowHeight () - 60; //for 1440p
			imageHeight = _form.getWindowHeight () - 80; //for 2160p (4k)
		}
		_log.debug ("AlbumImages.generateHtml: isAndroidDevice = " + _form.isAndroidDevice () + ", tableWidth = " + tableWidth + ", imageWidth = " + imageWidth + ", imageHeight = " + imageHeight);

		sb.append ("<TABLE WIDTH=")
				.append (tableWidth)
				.append (" CELLSPACING=0 CELLPADDING=")
				.append (padding)
				.append (" BORDER=")
				.append (_tableBorderPixels)
				.append (">").append (NL);

		AlbumImage[] images = _imageDisplayList.toArray (new AlbumImage[] {});

		String font2 = "fontsize10";
		if (isAndroidDevice) {
			font2 = "fontsize24";
		} else if (imageWidth <= 120) {
			font2 = "fontsize8";
		} else if (imageWidth <= 200) {
			font2 = "fontsize9";
		} else {
			font2 = "fontsize10";
		}

		Set<String> imagesInSlice = new HashSet<String> (); //use Set to eliminate duplicates
		int imageCount = 0;
		for (int row = 0; row < rows; row++) {
			sb.append ("<TR>").append (NL);

			String imageName;
			String href;

			for (int col = 0; col < cols; col++) {
				AlbumImage image = images[start + imageCount];
				image.calculateScaledSize (imageWidth, imageHeight);

				String extraDiffString = "";
				StringBuilder details = new StringBuilder ();

				boolean selectThisDuplicateImage = false;

				String fontColor = getFontColor (image);

				if (mode == AlbumMode.DoSampler) {
					//if collapseGroups is enabled, disable it in the drilldown when there is only one group
					AlbumMode newMode = AlbumMode.DoDir;
					boolean limitedCompare = false;

					int newCols = defaultCols;
					if (collapseGroups) {
						if (getNumMatchingImages (image.getBaseName (false), sinceInMillis) !=
							getNumMatchingImages (image.getBaseName (true), sinceInMillis)) {
							newMode = AlbumMode.DoSampler;
							newCols = _form.getColumns ();
							limitedCompare = true;
						}

					} else {
						//set image line style,  thickness, and color to distinguish near and exact duplicates
						boolean nearDup = _nameOfNearDuplicateThatCanBeDeleted.contains (image.getBaseName (false));						
						boolean exactDup = _nameOfExactDuplicateThatCanBeDeleted.contains (image.getBaseName (false));
						imageBorderStyleStr = (nearDup || exactDup ? "dashed" : "solid");
						imageBorderPixels = (nearDup || exactDup || mode == AlbumMode.DoDup ? 2 : 1);
						imageBorderColorStr = (exactDup ? "lime" : nearDup ? "yellow" : "blue");
						fontColor = (exactDup ? "lime" : nearDup ? "yellow" : getFontColor (image));
						imageOutlineStr = (exactDup ? "; outline: " + imageOutlinePixels + "px dotted red" : nearDup ? "; outline: " + imageOutlinePixels + "px dotted orange" : "");
					}

//count: here I actually need to know the number of images that will be shown in the drilldown page...
//like, getNumMatchingGroups()

//TODO - clean this up
					imageName = image.getBaseName (collapseGroups);
					String imageName1 = imageName + (collapseGroups ? "+" : ""); //plus sign here means digit
					href = generateImageLink (imageName1, imageName1, newMode, newCols, sinceDays, limitedCompare, looseCompare);

					details.append ("(")
							.append (collapseGroups ? getNumMatchingAlbums(imageName, sinceInMillis) + ":" : "")
							.append (getNumMatchingImages (imageName, sinceInMillis))
							.append (")");
//					details.append ("(");
//					if (collapseGroups) {
//						details.append(getNumMatchingAlbums(imageName, sinceInMillis))
//								.append(":");
//					}
//					details.append (getNumMatchingImages (imageName, sinceInMillis))
//							.append (")");

					imagesInSlice.add (imageName);

				} else if (mode == AlbumMode.DoDup) {
					//old: drill down to dir; always disable collapseGroups
//					AlbumMode newMode = AlbumMode.DoDir;
					AlbumMode newMode = AlbumMode.DoDup;

					boolean isEven = (((start + imageCount) & 1) == 0);
					imageName = image.getName ();
					AlbumImage partner = images[start + imageCount + (isEven ? +1 : -1)];

					String imageNonUniqueString = image.getBaseName (false);
					String partnerNonUniqueString = partner.getBaseName (false);

					imagesInSlice.add (imageNonUniqueString);
					imagesInSlice.add (partnerNonUniqueString);

					//set image border color based on average diff from looseCompareMap
					//for web color names, see http://www.w3schools.com/colors/colors_names.asp
					imageBorderColorStr = "black";
					if (looseCompare) {
						String joinedNamesPlusAttrs = AlbumImagePair.getJoinedNames (image, partner, true);
						AlbumImagePair pair = _looseCompareMap.get (joinedNamesPlusAttrs);
						if (pair != null) {
							extraDiffString += AlbumImage.HtmlNewline + pair.getDetails1String ();
							int minDiff = pair.getMinDiff ();
							imageBorderColorStr = (minDiff < 1 ? "white" : minDiff < 10 ? "green" : minDiff < 20 ? "yellow" : "orange");
						}
					}

					//set image line style to distinguish smaller image
					imageBorderStyleStr = image.compareToByPixels (partner) < 0 ? "dashed" : "solid";

					if (duplicateHandling != AlbumDuplicateHandling.SelectNone) {
//						long pixelDiff = image.getPixels () - partner.getPixels (); //note: does not take orientation into account here
						int pixelDiff = image.compareToByPixels (partner);
						switch (duplicateHandling) {
							default:
								throw new RuntimeException ("AlbumImages.generateHtml: invalid duplicateHandling \"" + duplicateHandling + "\"");
							case SelectFirst:
								if (isEven) {
									selectThisDuplicateImage = true;
								}
								break;

							case SelectSecond:
								if (!isEven) {
									selectThisDuplicateImage = true;
								}
								break;

							case SelectSmaller:
								if (pixelDiff < 0) {
									selectThisDuplicateImage = true;
								}
								break;

							case SelectSmallerFirst:
								if (pixelDiff == 0) {
									if (isEven) {
										selectThisDuplicateImage = true;
									}
								} else if (pixelDiff < 0) {
									selectThisDuplicateImage = true;
								}
								break;

							case SelectSmallerSecond:
								if (pixelDiff == 0) {
									if (!isEven) {
										selectThisDuplicateImage = true;
									}
								} else if (pixelDiff < 0) {
									selectThisDuplicateImage = true;
								}
								break;
						}
					}

					//set columns from image/partner that comes first alphabetically
					int imageCols = getNumMatchingImages (imageNonUniqueString, 0);
					int partnerCols = getNumMatchingImages (partnerNonUniqueString, 0);
					int newCols = (imageName.compareToIgnoreCase (partner.getName ()) < 0 ? imageCols : partnerCols);

					//but prevent using columns = 1
					if (newCols == 1) {
						newCols = (imageCols + partnerCols) / 2;
					}

					href = generateImageLink (imageNonUniqueString, partnerNonUniqueString, newMode, newCols, 0, false, true);

					details.append ("(")
							.append (imageCols)
							.append ("/")
							.append (image.getScaleString ())
							.append (")");

				} else { //mode == AlbumMode.DoDir
					//drill down to single image
					imageName = image.getName ();
					details.append ("(")
							.append (image.getScaleString ())
							.append (")");

					if (cols == 1) {
						details.append (" (")
								.append (imageCount + 1)
								.append (" of ")
								.append (numImages)
								.append (")");

					}
					imagesInSlice.add (image.getBaseName (false));

					int overWidth = image.getWidth () - _form.getWindowWidth ();
					int overHeight = image.getHeight () - _form.getWindowHeight ();
					boolean drillToImage = (overWidth > -5 || overHeight > -5);

					if (drillToImage) { //drill directly to image
						href = imageUrlPath + image.getSubFolder () + "/" + image.getNameWithExt ();

					} else { // drill to album with single image scaled to fit page
						AlbumMode newMode = AlbumMode.DoDir;
						int newCols = defaultCols;

//count: here I actually need to know the number of images that will be shown in the drilldown page...
//like, getNumMatchingGroups()

						href = generateImageLink (imageName, imageName, newMode, newCols, sinceDays, false, false);
					}
				}

				String imageAlignStr = "TOP"; //(cols == 1 ? "CENTER" : "RIGHT");
				String textAlignStr = (cols == 1 ? "CENTER" : "RIGHT");
/*TODO - fix align
				String textAlignStr = "CENTER";
				if (cols < defaultCols) {
					if (col == cols / 2) {
						textAlignStr = "CENTER";
					} else if (col < cols / 2) {
						textAlignStr = "RIGHT";
					} else {
						textAlignStr = "LEFT";
					}
				}
*/

				String fontWeightStr = "normal";
				if (highlightInMillis > 0 && image.getModified () >= highlightInMillis) { //0 means disabled
					fontWeightStr = "bold";
				}

				String fontStyleStr = "style=\"font-weight:" + fontWeightStr + ";color:" + fontColor + "\"";
				String imageStyleStr = "style=\"border:" + imageBorderPixels + "px " + imageBorderStyleStr + " " + imageBorderColorStr + " " + imageOutlineStr + "\"";

				//.append (NL)
				sb.append("<TD class=\"")
						.append (font2)
						.append ("\" ")
						.append (fontStyleStr)
						.append (" VALIGN=BOTTOM ALIGN=")
						.append (textAlignStr)
						.append (">")//.append (NL)
						.append (imageName)
						.append (details.toString())
						.append (_break).append(NL)

						.append ("<A HREF=\"")
						.append (href)
						.append ("\" ").append(NL)
						.append ("title=\"").append (image.toString (true, collapseGroupsForTags)).append (extraDiffString)
						.append ("\" target=_blank>").append (NL)
//				 		.append ("\" target=view>").append (NL)

						.append ("<IMG ")
						.append (imageStyleStr)
						.append (" SRC=\"")
						.append (imageUrlPath)
						.append (image.getSubFolder ())
						.append ("/")
						.append (image.getNameWithExt ())

						.append ("\" WIDTH=")
						.append (image.getScaledWidth ())
						.append (" HEIGHT=")
						.append (image.getScaledHeight ())
						.append (" ALIGN=")//.append (NL)
						.append (imageAlignStr)
						.append (">").append (NL)
						.append ("</A>").append (NL);

				//conditionally add Delete checkbox
				if (mode == AlbumMode.DoDir || mode == AlbumMode.DoDup || (mode == AlbumMode.DoSampler && !collapseGroups)) {
					String deleteParamStr = (mode == AlbumMode.DoDir || mode == AlbumMode.DoDup) ? AlbumFormInfo._DeleteParam1 + image.getNameWithExt () //single filename
																								 : AlbumFormInfo._DeleteParam2 + image.getBaseName(collapseGroups) + "*" + AlbumFormInfo._ImageExtension; //wildname

					sb.append (_break).append (NL)
						.append ("Delete<INPUT TYPE=\"CHECKBOX\" NAME=\"")
						.append (deleteParamStr)
//don't close form here .append ("\"></FORM>").append (NL)
						.append ("\"")
						.append (selectThisDuplicateImage ? " CHECKED" : "")
						.append (">").append (NL);
				}

				sb.append ("</TD>").append (NL);

				imageCount++;

				if ((start + imageCount) >= numImages) {
					break;
				}
			}

			sb.append ("</TR>").append (NL);

			if ((start + imageCount) >= numImages) {
				break;
			}
		}

		sb.append ("</TABLE>").append (NL);

		//footer: top link, slice links, and close link
		sb.append ("<TABLE WIDTH=100% CELLPADDING=0 CELLSPACING=0 BORDER=")
				.append (_tableBorderPixels)
				.append (">").append (NL)
				.append ("<TR>").append (NL)
				.append ("<TD class=\"")
				.append (font2)
				.append ("\" ALIGN=RIGHT>")
				.append (pageLinksHtml).append (NL)
				.append (_spacing).append (NL)
//TODO - this only work for albums that were drilled into??
				.append ("<A HREF=\"#\" onClick=\"self.close();\">Close</A>").append (NL)
				.append ("</TD>").append (NL)
				.append ("</TR>").append (NL)
//add a blank line at end to avoid problem when overlapping with Firefox's status area
//		  .append ("<TR><TD>&nbsp;</TD></TR>").append (NL)
				.append ("</TABLE>").append (NL);

		String htmlString = sb.toString ();

		//replace tagsMarker with any tags
		htmlString = htmlString.replace (tagsMarker, getTagsString (imagesInSlice, collapseGroupsForTags));

		//replace servletErrorsMarker with any servlet errors
		htmlString = htmlString.replace (servletErrorsMarker, getServletErrorsHtml ());

		if (AlbumFormInfo._Debug) {
			_log.debug ("AlbumImages.generateHtml: imageCount = " + imageCount);
		}

		AlbumProfiling.getInstance ().exit (1);

		return htmlString;
	}

	///////////////////////////////////////////////////////////////////////////
	private String getTagsString (Set<String> imagesInSlice, boolean collapseGroupsForTags)
	{
		StringBuilder sb = new StringBuilder (64);

		String tagsStr = AlbumTags.getInstance ().getTagsForBaseNames (imagesInSlice, collapseGroupsForTags);
		if (tagsStr.length () > 0) {
			sb.append (" (tags: ")
				.append (tagsStr)
				.append (")");
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String getServletErrorsHtml ()
	{
		final String bgErrorColor = "#FF0000"; //red background
		final String bgInfoColor = "#F0F0F0"; //white background
		final String bgWarnColor = "#E0E000"; //yellow background
		final String bgUnknownColor = "#0000E0"; //blue background

		String servletErrorsHtml = "";
		if (_form.getNumServletErrors () > 0) {
			StringBuilder sb = new StringBuilder (100);

			_form.getServletErrors ().forEach (s ->
					sb.append ("<TR>").append (NL)
						.append ("<TD class=\"")
						.append ("fontsize10")
						.append ("\" BGCOLOR=\"")
						.append (s.startsWith ("Error") ? bgErrorColor : s.startsWith ("Warning") ? bgWarnColor : s.startsWith ("Info") ? bgInfoColor : bgUnknownColor)
						.append ("\" ALIGN=LEFT>").append (NL)
						.append (s).append (NL)
						.append ("</TD>").append (NL)
						.append ("</TR>").append (NL)
//						.append ("<BR>").append (NL)
			);

			servletErrorsHtml = sb.toString ();
		}

		return servletErrorsHtml;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getNumMatchingImages (final String baseName, final long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enter (8);

		int num = AlbumImageDao.getInstance ().getNumMatchingImages (baseName, sinceInMillis);

//		_log.debug ("AlbumImages.getNumMatchingImages(\"" + baseName + "\"): " + num);

		AlbumProfiling.getInstance ().exit (8);

		return num;
	}

	///////////////////////////////////////////////////////////////////////////
	public static int getNumMatchingAlbums (final String baseName, final long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enter (8);

		int num = AlbumImageDao.getInstance ().getNumMatchingAlbums (baseName, sinceInMillis);

//		_log.debug ("AlbumImages.getNumMatchingAlbums(\"" + baseName + "\"): " + num);

		AlbumProfiling.getInstance ().exit (8);

		return num;
	}

	///////////////////////////////////////////////////////////////////////////
	//creates .BAT file with move commands
	public void generateExifSortCommands ()
	{
		StringBuilder sb = new StringBuilder(2048);

		//TODO - check filter for wildcards

		int numImages = _imageDisplayList.size ();
		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		Path rootPath = FileSystems.getDefault ().getPath (AlbumFormInfo.getInstance ().getRootPath (false));
		Path moveFile = FileSystems.getDefault ().getPath (rootPath.toString (), "moveRenameGeneratedFile.bat");
		boolean hasOneFilter = form.getFilters (0).length == 1;

		if (form.getMode () != AlbumMode.DoDir || form.getSortType () != AlbumSortType.ByExif || !hasOneFilter || numImages == 0) {
			deleteFileIgnoreException (moveFile);
			return;
		}

		String baseNameDest = form.getFilters (1)[0]; //only uses the first filter
		String dash = (baseNameDest.contains ("-") ? "" : "-"); //add dash unless already there
		Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), AlbumImage.getSubFolderFromName (baseNameDest));
		final String whiteList = "[0-9A-Za-z\\-_]"; //regex - all valid characters for basenames (disallow wildcards)

		//skip if list is already sorted by name
		List<AlbumImage> byName = new ArrayList<AlbumImage> (_imageDisplayList);
		Collections.sort (byName, new AlbumImageComparator (AlbumSortType.ByName));
		if (byName.equals (_imageDisplayList)) {
			_log.debug ("AlbumImages.generateExifSortCommands: image list is already sorted" + NL);

		} else if (baseNameDest.replaceAll (whiteList, "").length () > 0) {
			_log.debug ("AlbumImages.generateExifSortCommands: wildcards not allowed" + NL);

		} else {
			int index = 0;
			sb.append ("setlocal").append (NL);
			sb.append ("cd /d ").append (imagePath.toString ()).append (NL);
			for (AlbumImage image : _imageDisplayList) {
				String nexIndex = String.format ("%04d", ++index);
				sb.append ("move ").append (image.getName ()).append (".jpg ").append (baseNameDest).append (dash).append (nexIndex).append (".jpg").append (NL);
			}
			sb.append ("drSleep 0.5").append (NL);
			sb.append ("mov.exe /renum2 ").append (baseNameDest).append (dash).append ("*.jpg").append (NL);
		}

		if (moveFile.toFile ().length () > 0 || sb.length () > 0) {
			try (FileOutputStream outputStream = new FileOutputStream (moveFile.toFile ())) {
				outputStream.write (sb.toString ().getBytes ());
				outputStream.flush ();
				outputStream.close ();
				if (sb.length () > 0) {
					_log.debug ("AlbumImages.generateExifSortCommands: move commands written to file: " + moveFile.toString () + NL);
				}

			} catch (IOException ee) {
				_log.error ("AlbumImages.generateExifSortCommands: error writing output file: " + moveFile.toString () + NL);
				_log.error (ee); //print exception, but no stack trace
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static class ImageDuplicateDetails
	{
		///////////////////////////////////////////////////////////////////////////
		ImageDuplicateDetails (String filter)
		{
			_filter = filter;
		}

		///////////////////////////////////////////////////////////////////////////
		public void init (Collection<AlbumImage> imageDisplayList)
		{
			_firstMatchingImage = findFirstMatchingImage(_filter, imageDisplayList);
			_imageBaseName = _firstMatchingImage.getBaseName(false);
			_subFolder = _firstMatchingImage.getSubFolder();
			_digitsInImageBaseName = _imageBaseName.chars().filter(Character::isDigit).count();
//			_fileBaseName = _imageBaseName + (_imageBaseName.contains("-") ? "" : "-");
			_count = countMatchingImages(_filter, imageDisplayList);
			_averagePixels = getAveragePixels(_filter, imageDisplayList);
			_percentPortrait = getPercentPortrait(_filter, imageDisplayList);
		}

		///////////////////////////////////////////////////////////////////////////
		public static List<ImageDuplicateDetails> splitMultipleSubAlbums (String filter, Collection<AlbumImage> imageDisplayList)
		{
			List<ImageDuplicateDetails> items = new ArrayList<> ();

			AlbumImage firstImage = findFirstMatchingImage(filter, imageDisplayList);
			if (firstImage == null) {
				return items;
			}

			String digits = firstImage.getName().split("-")[1];
			if (digits.length() == 2) {
				items.add(new ImageDuplicateDetails(filter));
				return items;

			} else if (digits.length() == 3) {
				if (digits.startsWith("0")) {
					return items;

				} else {
					for (int ii = 0; ii < 10; ii++) {
						if (findFirstMatchingImage(filter + "-" + ii, imageDisplayList) != null) {
							items.add(new ImageDuplicateDetails(filter + "-" + ii));
						}
					}
				}
			}

			return items;
		}

		///////////////////////////////////////////////////////////////////////////
		public static String appendDash (String string)
		{
			return string + (string.contains("-") ? "" : "-");
		}

		///////////////////////////////////////////////////////////////////////////
		public static AlbumImage findFirstMatchingImage (String filter, Collection<AlbumImage> imageDisplayList)
		{
			for (AlbumImage image : imageDisplayList) {
				if (image.getName().startsWith(filter)) {
					return image;
				}
			}

			return null; //very bad
		}

		///////////////////////////////////////////////////////////////////////////
		public static Long countMatchingImages (String filter, Collection<AlbumImage> imageDisplayList)
		{
			return imageDisplayList.stream()
								   .filter(i -> i.getName().startsWith(filter))
								   .count();
		}

		///////////////////////////////////////////////////////////////////////////
		public static Long getAveragePixels (String filter, Collection<AlbumImage> imageDisplayList)
		{
			List<Long> pixels = new ArrayList<>();
			for (AlbumImage image : imageDisplayList) {
				if (image.getName().startsWith(filter)) {
					pixels.add(image.getPixels());
				}
			}

			return Math.round(pixels.stream()
									.mapToDouble(i -> i)
									.average()
									.getAsDouble());
		}

		///////////////////////////////////////////////////////////////////////////
		public static Long getPercentPortrait (String filter, Collection<AlbumImage> imageDisplayList) {
			List<AlbumOrientation> orientations = new ArrayList<>();
			for (AlbumImage image : imageDisplayList) {
				if (image.getName().startsWith(filter)) {
					orientations.add(image.getOrientation());
				}
			}

			long numPortrait = orientations.stream()
					.filter(p -> p == AlbumOrientation.ShowPortrait)
					.count();

			return Math.round(100. * (double) numPortrait / (double) orientations.size());
		}

		///////////////////////////////////////////////////////////////////////////
		public static boolean doesNumberOfDigitsInBaseNamesMatch (Collection<ImageDuplicateDetails> dups)
		{
			return dups.stream()
					.map(d -> d._digitsInImageBaseName)
					.distinct()
					.count() != 1;
		}

		///////////////////////////////////////////////////////////////////////////
		public static long numberOfDifferentSubFolders (Collection<ImageDuplicateDetails> dups)
		{
			return dups.stream()
					.map(d -> d._subFolder)
					.distinct()
					.count();
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public String toString ()
		{
			StringBuilder sb = new StringBuilder(getClass ().getSimpleName ());
			sb.append (": ").append (_filter);
			sb.append (", ").append (_firstMatchingImage.getName());
			sb.append (", ").append (_imageBaseName);
			sb.append (", ").append (_subFolder);
			sb.append (", ").append (_digitsInImageBaseName);
//			sb.append (", ").append (_fileBaseName);
			sb.append (", ").append (_decimalFormat2.format ((_averagePixels / 1e6)) + "MP");
			sb.append (", ").append (_percentPortrait).append("%");
			sb.append (", ").append (_count);

			return sb.toString ();
		}

		//members
		final String _filter;
		AlbumImage _firstMatchingImage = null;
		String _imageBaseName;
		String _subFolder;
		Long _digitsInImageBaseName;
//		String _fileBaseName;
		Long _count;
		Long _averagePixels;
		Long _percentPortrait;
	}

	///////////////////////////////////////////////////////////////////////////
	//creates .BAT file with move commands
	public void generateDuplicateImageRenameCommands ()
	{
		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands");

		StringBuilder sb = new StringBuilder(2048);

		int numImages = _imageDisplayList.size ();
		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		Path rootPath = FileSystems.getDefault ().getPath (AlbumFormInfo.getInstance ().getRootPath (false));
		Path moveFile = FileSystems.getDefault ().getPath (rootPath.toString (), "moveRenameGeneratedFile.bat");
//		boolean hasOneFilter = form.getFilters (0).length == 1;
		String[] filters = form.getFilters ();

		if (form.getMode () != AlbumMode.DoDup || !form.getLooseCompare() || !form.getIgnoreBytes() || numImages == 0 || numImages > 100 || form.filtersHaveWildCards()) {
			deleteFileIgnoreException (moveFile);
			return; //nothing to do
		}

		List<ImageDuplicateDetails> dups = new ArrayList<> ();
		for (String filter : filters) {
			List<ImageDuplicateDetails> tmp = ImageDuplicateDetails.splitMultipleSubAlbums (filter, _imageDisplayList);
			dups.addAll (tmp);
		}

		if (dups.isEmpty()) {
			deleteFileIgnoreException (moveFile);
			return; //nothing to do
		}

		for (ImageDuplicateDetails dup : dups) {
			dup.init(_imageDisplayList);
		}

		long numAlbums = dups.stream()
							 .map(i -> i._imageBaseName)
							 .distinct()
							 .count();
		if (numAlbums == 1) {
			deleteFileIgnoreException (moveFile);
			return; //nothing to do
		}

		long numTotalImages = dups.stream()
								  .mapToLong(i -> i._count)
								  .sum();

		//sort by the specified criteria
		List<ImageDuplicateDetails> sorted = dups.stream ()
				.sorted ((i1, i2) -> {

					//TODO - is comparing average pixel size always right?
					long averagePixelDiff = compareToWithSlop (i1._averagePixels, i2._averagePixels, false, 0.5); //sort in descending order
					if (averagePixelDiff != 0) {
						return averagePixelDiff > 0 ? 1 : -1;
					}

					//compare orientation
					long percentPortraitDiff = i2._percentPortrait - i1._percentPortrait; //sort in descending order
					final long maxPreferredPercentPortraitDiff = 5; //allowable variation from exact that will still be considered equal
					if (Math.abs (percentPortraitDiff) >= maxPreferredPercentPortraitDiff) {
						return percentPortraitDiff > 0 ? 1 : -1;
					}

					//compare count
					long countDiff = i2._count - i1._count; //sort in descending order
//					if (countDiff != 0) {
//						return countDiff > 0 ? 1 : -1;
//					}

					return countDiff == 0 ? 0 : countDiff > 0 ? 1 : -1;
				})
				.collect (Collectors.toList ());

		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands: after sort --------------------");
		for (ImageDuplicateDetails item : sorted) {
			_log.debug(item);
		}

		List<String> baseNamesDistinct = dups.stream()
											 .map(i -> i._imageBaseName)
											 .distinct()
											 .sorted(new AlphanumComparator()) //sort numerically
											 .collect(Collectors.toList());

		ImageDuplicateDetails firstItem = sorted.get(0);
		ImageDuplicateDetails destinationItem = dups.stream()
													.sorted((i1, i2) -> i1._imageBaseName.compareToIgnoreCase (i2._imageBaseName)) //TODO - should we use AlphanumComparator here??
													.collect(Collectors.toList())
													.get(0);

		String firstFilter = firstItem._filter;
		String destinationFilter = destinationItem._filter;

		String firstBaseName = firstItem._imageBaseName;
		String destinationBaseName = destinationItem._imageBaseName;

		boolean needsCleanup = !destinationBaseName.equalsIgnoreCase(destinationFilter) && //hack - in this case we end up with extra "1" on numbering
							   !firstBaseName.equalsIgnoreCase(firstFilter) &&
							   destinationBaseName.equalsIgnoreCase(firstBaseName);

		boolean browserNeedsRefresh = !destinationBaseName.equalsIgnoreCase(destinationFilter);

		//handle situation where we have both 2- and 3-digit albums (prefer, for example, Foo28 over Foo101)
		boolean mismatchedAlbumDigits = ImageDuplicateDetails.doesNumberOfDigitsInBaseNamesMatch(dups);
		
		sb.append ("REM auto-generated").append (NL);
		sb.append ("setlocal").append (NL);
		sb.append ("set REVERSE=0").append (NL);
		sb.append ("if %1@==r@ set REVERSE=1").append (NL);

		int index = 1;
		String indexStringFormat = (sorted.size() > 9 ? "%02d" : "%d");
		Path currentFolder =  FileSystems.getDefault ().getPath (rootPath.toString ());
		for (ImageDuplicateDetails item : sorted) {
			Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), item._firstMatchingImage.getSubFolder ());
			if (currentFolder.compareTo(imagePath) != 0) {
				sb.append("cd /d ").append(imagePath.toString()).append(NL);
				currentFolder = imagePath;
			}

			String indexString = String.format(indexStringFormat, index);
			String sourceFileNameWild = ImageDuplicateDetails.appendDash(item._filter) + "*.jpg";
			String destinatioFileNameWild = ImageDuplicateDetails.appendDash(firstFilter) + indexString + "*.jpg";
			sb.append("mov ").append(sourceFileNameWild).append(" ").append(destinatioFileNameWild).append(NL);

			if (!firstItem._subFolder.equalsIgnoreCase(item._subFolder)) {
				sb.append("move ").append(destinatioFileNameWild).append(" ..\\").append(firstItem._subFolder).append("\\").append(NL);
			}

			index++;
		}

		double sleepInSeconds = (numTotalImages >= 40 ? 1.0 : 0.5);

		if (!firstBaseName.equalsIgnoreCase(destinationBaseName)) { //  || mismatchedAlbumDigits) {
//			browserNeedsRefresh = true;
			sb.append ("drSleep ").append(sleepInSeconds).append (NL);
//			sb.append("call mx.bat ").append(destinationBaseName).append(" ").append(firstBaseName).append(NL);
			sb.append("mov ").append(firstBaseName).append("*.jpg").append(" ").append(destinationBaseName).append("*.jpg").append(NL);
		}


		if (!baseNamesDistinct.get(0).equalsIgnoreCase(destinationBaseName) && mismatchedAlbumDigits) {
//			browserNeedsRefresh = true;
			sb.append ("drSleep ").append(sleepInSeconds).append (NL);
//			sb.append ("drSleep 0.5").append (NL);
			sb.append("mov ").append(destinationBaseName).append("-*.jpg").append(" ").append(baseNamesDistinct.get(0)).append("-*.jpg").append(NL);
		}

		if (needsCleanup) { //hack - remove extra "1"
			browserNeedsRefresh = true;
			sb.append ("drSleep ").append(sleepInSeconds).append (NL);
//			sb.append ("drSleep 0.5").append (NL);
//			Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), item._firstMatchingImage.getSubFolder ());
//			sb.append ("cd /d ").append (imagePath.toString ()).append (NL);
			sb.append("mov ").append(destinationBaseName).append("-1*.jpg").append(" ").append(destinationBaseName).append("-*.jpg").append(NL); //TODO - "-1" could potentially be any digit
		}

//TODO this does not work quite right - it will write one row for each pair
		String optionalCommentForNow = (baseNamesDistinct.size() > 2 ? "REM " : "");
		for (String baseName : baseNamesDistinct) {
			if (!destinationBaseName.equalsIgnoreCase(baseName)) {
				sb.append(optionalCommentForNow).append("if %REVERSE%==1 mov ").append(destinationBaseName).append("*.jpg").append(" ").append(baseName).append("*.jpg").append(NL);
			}
		}
//		sb.append("REM call mx.bat ").append(destinationBaseName).append(" ").append(firstBaseName).append(NL);
//		sb.append("REM call mx.bat ").append(firstBaseName).append(" ").append(destinationBaseName).append(NL);

//TODO - can we force this?, i.e., make it automatic?
		if (browserNeedsRefresh) {
			sb.append("echo ******** BROWSER NEEDS REFRESH ********").append(NL);
		}

		if (moveFile.toFile ().length () > 0 || sb.length () > 0) {
			try (FileOutputStream outputStream = new FileOutputStream (moveFile.toFile ())) {
				outputStream.write (sb.toString ().getBytes ());
				outputStream.flush ();
				outputStream.close ();
				if (sb.length () > 0) {
					_log.debug ("AlbumImages.generateDuplicateImageRenameCommands: move commands written to file: " + moveFile.toString () + NL + sb.toString());
				}

			} catch (IOException ee) {
				_log.error ("AlbumImages.generateDuplicateImageRenameCommands: error writing output file: " + moveFile.toString () + NL);
				_log.error (ee); //print exception, but no stack trace
			}
		}

//		if (browserNeedsRefresh) {
			form.setForceBrowserCacheRefresh (true);
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static void deleteFileIgnoreException (Path path)
	{
		try {
			if (Files.exists (path)) {
				Files.delete (path);
			}
		} catch (IOException ex) {
			//ignore
		}
	}

/*TODO - fix this (only used by AlbumFileFilter to implement sinceInMillis
	///////////////////////////////////////////////////////////////////////////
	public long getImageModified (String name)
	{
		long modified = Long.MAX_VALUE;

		try {
			AlbumXml database = _databaseMap.get (name.substring (0, AlbumImage.SubFolderLength).toLowerCase ());

			AlbumImage image = database.getImage (name);

			modified = image.getModified ();

		} catch (Exception ee) {
//			_log.error ("AlbumImages.getImageModified: image \"" + name + "\" not found in database");
			throw new RuntimeException ("AlbumImages.getImageModified: image \"" + name + "\" not found in database");
		}

		return modified;
	}
*/

/*not currently used
	///////////////////////////////////////////////////////////////////////////
	//for best results, only send the URL parameters (i.e., everything after the "?")
	public static String encodeUrl (String url)
	{
		String encoded = url;
		try {
			encoded = URLEncoder.encode (url, "UTF-8"); //"ISO-8859-1");

		} catch (Exception ee) {
			_log.error ("AlbumImages.encodeUrl: failed on \"" + url + "\"", ee);
		}

		return encoded;
	}
*/

/* for testing exifDate distribution
	///////////////////////////////////////////////////////////////////////////
	private void generateExifDateStatistics ()
	{
		AlbumProfiling.getInstance ().enter (5);

		long stats[] = new long[AlbumImage.NumFileExifDates + 1];

		for (AlbumImage image : _imageDisplayList) {
			Set<Long> uniqueDates = new HashSet<Long> ();
			for (int ii = 0; ii < AlbumImage.NumFileExifDates; ii++) {
				Long exifDate = image.getExifDate (ii);
				if (exifDate > 0) {
					uniqueDates.add (exifDate);
				}
			}
			int numUniqueDates = uniqueDates.size ();
			stats[numUniqueDates]++;

//			if (numUniqueDates == 4) {
//				_log.debug ("AlbumImages.generateExifDateStatistics: [4] = " + image);
//			}
		}

		for (int ii = 0; ii < stats.length; ii++) {
			_log.debug ("AlbumImages.generateExifDateStatistics: [" + ii + "] = " + _decimalFormat0.format (stats[ii]));
		}

		AlbumProfiling.getInstance ().exit (5);
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//slopPercent specifies the allowable variation from exactly equal that will still be considered equal
	//similar code in AlbumOrientation#getOrientation
	public static int compareToWithSlop (long value1, long value2, boolean ascending, double slopPercent)
	{
		int factor = ascending ? 1 : -1;

		if (value1 == value2) {
			return 0;
		}

		double ratio = (double) value1 / (double) value2;
		if (ratio > 1. + slopPercent / 100.) {
			return factor * 1;
		} else if (ratio < 1. - slopPercent / 100.) {
			return factor * (-1);
		} else {
			return 0;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static void cacheMaintenance (boolean clearAll) //hack
	{
		if (_nameScaledImageMap == null) {
			_nameScaledImageMap = new ConcurrentHashMap<> (_nameScaledImageMapMaxSize); //hack
		}

		if (_looseCompareMap == null) {
			_looseCompareMap = new ConcurrentHashMap<> (_looseCompareMapMaxSize); //hack
		}

		if (clearAll) {
			AlbumProfiling.getInstance ().enter (5);

			AlbumImageDao.getInstance ().cacheMaintenance ();

//			_nameScaledImageMap.clear ();
//			_looseCompareMap.clear ();
			_nameScaledImageMap = new ConcurrentHashMap<> (_nameScaledImageMapMaxSize); //hack
			_looseCompareMap = new ConcurrentHashMap<> (_looseCompareMapMaxSize); //hack

			_allCombos = null;

			AlbumProfiling.getInstance ().exit (5);

		} else {
			int numItemsRemoved = 0;

			numItemsRemoved = cacheMaintenance (_nameScaledImageMap, _nameScaledImageMapMaxSize);
			if (numItemsRemoved > 0) {
				_log.debug ("AlbumImage.cacheMaintenance: _nameScaledImageMap: " + _decimalFormat0.format (numItemsRemoved) +
						" items removed, new size = " + VendoUtils.unitSuffixScale (_nameScaledImageMap.size (), 1) + " / " + VendoUtils.unitSuffixScale (_nameScaledImageMapMaxSize, 1));
			}

			numItemsRemoved = cacheMaintenance (_looseCompareMap, _looseCompareMapMaxSize);
			if (numItemsRemoved > 0) {
				_log.debug ("AlbumImage.cacheMaintenance: _looseCompareMap: " + _decimalFormat0.format (numItemsRemoved) +
						" items removed, new size = " + VendoUtils.unitSuffixScale (_looseCompareMap.size (), 1) + " / " + VendoUtils.unitSuffixScale (_looseCompareMapMaxSize, 1));
			}
		}

		long totalMem = Runtime.getRuntime ().totalMemory ();
		long maxMem   = Runtime.getRuntime ().maxMemory ();
		double memoryUsedPercent = 100 * (double) totalMem / maxMem;
		double nameScaledImageMapPercent = 100 * (double) _nameScaledImageMap.size () / _nameScaledImageMapMaxSize;
		double looseCompareMapPercent = 100 * (double) _looseCompareMap.size () / _looseCompareMapMaxSize;
		_log.debug ("AlbumImage.cacheMaintenance: memoryUsed: " + _decimalFormat1.format (memoryUsedPercent) + "%" +
				", _nameScaledImageMap: " + _decimalFormat0.format (nameScaledImageMapPercent) + "% of " + _decimalFormat0.format (_nameScaledImageMapMaxSize) + //VendoUtils.unitSuffixScale (_nameScaledImageMap.size (), 1) + " / " + VendoUtils.unitSuffixScale (_nameScaledImageMapMaxSize, 1) +
				", _looseCompareMap: " + _decimalFormat0.format (looseCompareMapPercent) + "% of " + _decimalFormat0.format (_looseCompareMapMaxSize)); //VendoUtils.unitSuffixScale (_looseCompareMap.size (), 1) + " / " + VendoUtils.unitSuffixScale (_looseCompareMapMaxSize, 1));
	}

	///////////////////////////////////////////////////////////////////////////
	private static <T, V> int cacheMaintenance (Map<T, V> map, int maxSize) //hack
	{
		int numItemsRemoved = 0;

		if (map.size () > maxSize) {
			AlbumProfiling.getInstance ().enter (5);

			int newSize = (3 * maxSize) / 4;
			numItemsRemoved = map.size () - newSize;

			Iterator<T> iter = map.keySet ().iterator ();
			for (int ii = 0; ii < numItemsRemoved; ii++) {
				iter.next ();
				iter.remove ();
			}

			AlbumProfiling.getInstance ().exit (5);
		}

		return numItemsRemoved;
	}


	//members
	private AlbumFormInfo _form = null;

	private Collection<AlbumImage> _imageDisplayList = null; //list of images to display

	private final int _tableBorderPixels = 0; //set to 0 normally, set to 1 for debugging

//	private static long _allCombosNumImages = 0;
	private static List<VPair<Integer, Integer>> _allCombos = null;

	public static final int _nameScaledImageMapMaxSize = 36 * 1024;
	public static final int _looseCompareMapMaxSize = 24 * 1024 * 1024;
	private static Map<String, ByteBuffer> _nameScaledImageMap = null;
	private static Map<String, AlbumImagePair> _looseCompareMap = null;

	private static final Set<Long> _previousRequestTimestamps = new HashSet<Long> ();
	private static final Set<String> _nameOfExactDuplicateThatCanBeDeleted = new HashSet<> ();
	private static final Set<String> _nameOfNearDuplicateThatCanBeDeleted = new HashSet<> ();

	private static final String _break = "<BR>";
	private static final String _spacing = "&nbsp;";

	private static final String NL = System.getProperty ("line.separator");
	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm"); //Note SimpleDateFormat is not thread safe
	private static final DecimalFormat _decimalFormat0 = new DecimalFormat ("###,##0"); //format as integer
	private static final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0.0");
	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0.00");

	private static AlbumImages _instance = null;
	private static String _prevRootPath = "";
	private static ExecutorService _executor = null;

	private static final Logger _log = LogManager.getLogger ();
}
