//AlbumImages.java

package com.vendo.albumServlet;

import com.google.common.cache.*;
import com.vendo.vendoUtils.*;
import com.vendo.vendoUtils.VFileList.ListMode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vendo.albumServlet.AlbumFormInfo._Debug;


public class AlbumImages
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public static AlbumImages getInstance()
	{
		if (_instance == null) {
			synchronized (AlbumImages.class) {
				if (_instance == null) {
					_instance = new AlbumImages ();
				}
			}
		}

		return _instance;
	}

	///////////////////////////////////////////////////////////////////////////
	private AlbumImages ()
	{
//		if (AlbumFormInfo._logLevel >= 6)
		_log.debug ("AlbumImages ctor");

		_shutdownThread = watchShutdownFile ();
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
	public void setForm (AlbumFormInfo form)
	{
		_form = form;
	}

	///////////////////////////////////////////////////////////////////////////
	public void processParams (HttpServletRequest request, AlbumFormInfo form)
	{
		int imagesRemoved = 0;
		long currentTimestamp = 0;
		Set<String> addToFiltersSet = new HashSet<> ();

		Enumeration<String> paramNames = request.getParameterNames ();
		while (paramNames.hasMoreElements ()) {
			String paramName = paramNames.nextElement ();
			if (AlbumFormInfo._logLevel >= 10) {
				_log.debug ("AlbumImages.processParams: got param \"" + paramName + "\"");
			}

			if ((paramName.startsWith (AlbumFormInfo._AddToFiltersParam))) {
				String[] paramValues = request.getParameterValues(paramName);
				if (paramValues[0].equalsIgnoreCase("on")) {
					String filter = paramName.substring(AlbumFormInfo._AddToFiltersParam.length());
					addToFiltersSet.add(filter);
				}
			}

			if (paramName.equalsIgnoreCase ("timestamp")) {
				String[] paramValues = request.getParameterValues (paramName);
				try {
					currentTimestamp = Long.parseLong (paramValues[0]);
				} catch (NumberFormatException exception) {
					currentTimestamp = 0;
				}
			}

			//crearCache requests that have same timestamp as previous should be ignored (can happen if user forces browser refresh)
			if (paramName.startsWith (AlbumFormInfo._ClearCacheParam) && _previousRequestTimestamps.contains (currentTimestamp)) {
				form.setClearCache(false);
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

		if (addToFiltersSet.size() > 0) {
			Set<String> set1 = new HashSet<> (addToFiltersSet);
			set1.addAll(Arrays.asList(form.getFilters(1)));
			form.setFilter1(set1.stream().sorted(_alphanumComparator).collect(Collectors.joining(",")));

			Set<String> set2 = new HashSet<> (addToFiltersSet);
			set2.addAll(Arrays.asList(form.getFilters(2)));
			form.setFilter2(set2.stream().sorted(_alphanumComparator).collect(Collectors.joining(",")));
		}

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

		//if we have to reduce the filters, always add back in the original filters //NO!!!
		if (filters.length > maxFilters) {
//TODO - if we are adding the original filters in at the end of this block, we should remove them at the beginning, otherwise the might end up in the list twice
			_form.addServletError ("Warning: too many filters (" + _decimalFormat0.format (filters.length) + "), reducing to " + maxFilters);
			List<String> filterList = new ArrayList<>(Arrays.asList(filters));
			Collections.shuffle(filterList); //might as well shuffle
			filters = filterList.stream()
//								.limit(maxFilters - originalFilters.length)
								.limit(maxFilters)
								.sorted(VendoUtils.caseInsensitiveStringComparator)
								.collect(Collectors.toList())
								.toArray(new String[] {});
//			filters = ArrayUtils.addAll (filters, originalFilters); //NO!!! this breaks tags
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
		String subFolder = AlbumImageDao.getInstance ().getSubFolderFromImageName (wildName);

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
		String subFolder = AlbumImageDao.getInstance ().getSubFolderFromImageName (origName);
		String origPath = rootPath + subFolder + "/" + origName;
		String newPath = origPath + AlbumFormInfo._DeleteSuffix;
		if (AlbumFormInfo._logLevel >= 7) {
			_log.debug ("AlbumImages.renameImageFile: origPath \"" + origPath + "\"");
			_log.debug ("AlbumImages.renameImageFile: newPath \"" + newPath + "\"");
		}

		while (VendoUtils.fileExists (newPath)) {
			newPath += AlbumFormInfo._DeleteSuffix;
		}

		if (_Debug) {
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
		AlbumProfiling.getInstance ().enter/*AndTrace*/ (1);

		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumImages.doDir: loading images from DAO...");
		}

		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		boolean useCase = form.getUseCase ();

		final AlbumFileFilter filter = new AlbumFileFilter (filters, excludes, useCase, sinceInMillis);

		_imageDisplayList = new LinkedList<> ();

		Collection<String> subFolders = AlbumImageDao.getInstance ().getAlbumSubFolders ();

		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());
		final Set<String> debugNeedsChecking = new ConcurrentSkipListSet<> ();
		final Map<String, Integer> debugCacheMiss = new ConcurrentHashMap<> ();
//		final Map<String, Integer> imageCountsByFolder = new ConcurrentHashMap<> ();

		AlbumProfiling.getInstance ().enter (5, "dao.doDir");
		for (final String subFolder : subFolders) {
			new Thread (() -> {
				final Collection<AlbumImage> imageDisplayList = AlbumImageDao.getInstance ().doDir (subFolder, filter, debugNeedsChecking, debugCacheMiss);
				if (imageDisplayList.size () > 0) {
					synchronized (_imageDisplayList) {
						_imageDisplayList.addAll (imageDisplayList);
//						imageCountsByFolder.put(subFolder, imageDisplayList.size());
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
			_imageDisplayList = Arrays.stream (array).collect(Collectors.toList());

			AlbumProfiling.getInstance ().exit (5, "sort " + _form.getSortType ());
		}

		if (form.getMode () == AlbumMode.DoDir) {
			generateExifSortCommands ();
		}

		if (form.getMode () == AlbumMode.DoDup && !form.getDbCompare()) {
			if (form.getLooseCompare()) {
				final int minImagesToFlagAsLargeAlbum = form.getMinImagesToFlagAsLargeAlbum() * (form.isAndroidDevice() ? 10 : 1); //hack - because android is mostly used for q*
				final int maxNumberOfAlbumsToDisplay = 50;

				Set<String> removed = removeAlbumsWithLargeCounts(minImagesToFlagAsLargeAlbum); //HACK; Note: operates directly on _imageDisplayList
				if (!removed.isEmpty()) {
					String removedStr = removed.stream()
							.limit(maxNumberOfAlbumsToDisplay)
							.sorted(_alphanumComparator)
							.collect(Collectors.joining(","));

					String message = "excluding the following " + removed.size() + " 'special' albums that have more than " + minImagesToFlagAsLargeAlbum + " images: " + removedStr;
					_log.debug("AlbumImages.removeAlbumsWithLargeCounts: " + message);
					form.addServletError("Warning: " + message);
				}
			}

			generateDuplicateImageRenameCommands();
		}

		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumImages.doDir: folderNeedsChecking: folders(" + debugNeedsChecking.size () + ") = " + debugNeedsChecking);
			_log.debug ("AlbumImages.doDir: cacheMiss: folders(" + debugCacheMiss.size () + ") = " +
					debugCacheMiss.keySet().stream().sorted(_alphanumComparator).collect(Collectors.joining(", ")));
			if (debugCacheMiss.size() <= 50) {
				debugCacheMiss.keySet().stream().sorted(_alphanumComparator).forEach(s -> _log.debug("AlbumImages.doDir: cacheMiss: folder: " + s + " -> " + _decimalFormat0.format (debugCacheMiss.get(s)) + " images"));
			}
//			if (imageCountsByFolder.size() <= 30) {
//				imageCountsByFolder.keySet().stream().sorted(_alphanumComparator).forEach(s -> _log.debug("AlbumImages.doDir: image counts: folders: " + s + " -> " + _decimalFormat0.format (imageCountsByFolder.get(s))));
//			}
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
		boolean reverseSort = form.getReverseSort(); //hack - reverses sense of limitedCompare
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
		if ((form.getFilters ().length == 0 && !dbCompare && looseCompare && ignoreBytes)) {
			form.addServletError ("Warning: too many comparisons; aborting");
			_imageDisplayList = new ArrayList<> ();
			return 0;
		}

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

		List<AlbumImage> list = new ArrayList<> (numImages);
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
		list = null; //TODO - eliminate need of list

		ArrayList<AlbumImagePair> dups = new ArrayList<> (200);

		String[] filters1 = form.getFilters (1);
		if (filters1.length == 0) {
			filters1 = new String[] {"*"};
		}
		final AlbumFileFilter filter1 = new AlbumFileFilter (filters1, excludes, useCase, sinceInMillis);

		String[] filters2 = form.getFilters (2);
//		if (filters2.length == 0) {
//			filters2 = new String[] {"*"};
//		}
		final AlbumFileFilter filter2 = new AlbumFileFilter (filters2, excludes, useCase, sinceInMillis);

		_duplicatesCache.clear();

		//if looseCompare, determine work to be done
		int maxComparisons = getMaxComparisons (numImages);
		if (looseCompare && !dbCompare) {
			_log.debug ("AlbumImages.doDup: maxComparisons = " + _decimalFormat0.format (maxComparisons) + " (max possible combos, including mismatched orientation)");
//			final int maxAllowedComparisons = 1000 * 1000 * 1000;
//			if (maxComparisons > maxAllowedComparisons) {
//				form.addServletError ("Warning: too many comparisons (" + _decimalFormat0.format (maxComparisons) + "), disabling looseCompare");
//				looseCompare = false;
//			}
		}

		if (looseCompare && !dbCompare) {
//			final int maxThreads = VendoUtils.getLogicalProcessors () - 1;
			final int maxThreads = 3 * VendoUtils.getLogicalProcessors ();
			final long maxPairsToPutInQueue = 3 * 1000 * 1000;
			final int queueSize = 2 * 1000 * 1000;

			CacheStats nameScaledImageCacheStatsStart = _nameScaledImageCache.stats();
			_nameScaledImageCacheAdded = ConcurrentHashMap.newKeySet();
			_nameScaledImageCacheEvicted = ConcurrentHashMap.newKeySet();

			CacheStats looseCompareDataCacheStatsStart = _looseCompareDataCache.stats();
			_looseCompareDataCacheAdded = ConcurrentHashMap.newKeySet();
			_looseCompareDataCacheEvicted = ConcurrentHashMap.newKeySet();

			final Set<AlbumImagePair> pairsReady = ConcurrentHashMap.newKeySet(1000);
			final Set<AlbumImageDiffDetails> toBeAddedToImageDiffsTable = ConcurrentHashMap.newKeySet();

			//create some helper maps
			Map<String, Integer> imageNameToIdMap = Arrays.stream(images).collect(Collectors.toMap(AlbumImage::getName, AlbumImage::getNameId));
			Map<Integer, String> imageIdToNameMap = Arrays.stream(images).collect(Collectors.toMap(AlbumImage::getNameId, AlbumImage::getName));

			//get list of ids for all images
			List<Integer> imageIds = Arrays.stream(images).map(AlbumImage::getNameId).distinct().collect(Collectors.toList());

			//query database for known diffs; has joined IDs as key
			final int maxImagesToQuery = 20 * 1000;
			Map<String, AlbumImageDiffDetails> imageDiffDetailsFromImageDiffs = AlbumImageDiffer.getInstance().getImagesFromImageDiffs(imageIds, maxImagesToQuery);
			if (imageDiffDetailsFromImageDiffs != null && imageDiffDetailsFromImageDiffs.size() > 0) {
				_log.debug("AlbumImages.doDup: imageDiffDetailsFromImageDiffs.size() before removing cruft: " + _decimalFormat0.format(imageDiffDetailsFromImageDiffs.size()));

				//remove any that don't match filters, or are obsolete (i.e., there is no longer an entry in the "images" table for either image_id (and therefore no AlbumImage object))
				imageDiffDetailsFromImageDiffs.entrySet().removeIf(i -> {
							List<Integer> imageIdList = Arrays.stream(i.getKey().split(",")).map(Integer::valueOf).collect(Collectors.toList());
							return imageIdToNameMap.get(imageIdList.get(0)) == null || imageIdToNameMap.get(imageIdList.get(1)) == null;
						}
				);

				_log.debug("AlbumImages.doDup: imageDiffDetailsFromImageDiffs.size() after removing cruft: " + _decimalFormat0.format(imageDiffDetailsFromImageDiffs.size()));

//TODO - this is wrong - need to compare imageDiffDetailsFromImageDiffs before and after
				if (false) { //statement block to compare before/after remove code
					Set<String> tempBefore = Arrays.stream(images).map(AlbumImage::getName).collect(Collectors.toSet());
					Set<String> tempAfter = new HashSet<>(imageDiffDetailsFromImageDiffs.keySet());
					Set<String> diffBefore = new HashSet<>(tempAfter);
					Set<String> diffAfter = new HashSet<>(tempBefore);
					diffBefore.removeAll(tempBefore);
					diffAfter.removeAll(tempAfter);
					int breakHere = 1;
				}

				if (false) { //statement block for logging
					List<String> info = imageDiffDetailsFromImageDiffs.values().stream()
							.map(i -> {
								String imageName1 = imageIdToNameMap.get(i.getNameId1());
								String imageName2 = imageIdToNameMap.get(i.getNameId2());
								if (imageName1 == null || imageName2 == null) {
									return null; //pair not found in map
								}
								return imageName1 + ", " + imageName2 + " - " + i.toString();
							})
							.filter(Objects::nonNull)
							.sorted(String.CASE_INSENSITIVE_ORDER)
							.collect(Collectors.toList());

					if (info.isEmpty()) {
						_log.debug("AlbumImages.doDup: NO matches found in image_diffs table" + NL);
					} else {
						_log.debug("AlbumImages.doDup: found " + info.size() + " matches from image_diffs table: " + NL + String.join(NL, info) + NL);
						_log.debug("AlbumImages.doDup: found " + info.size() + " matches from image_diffs table");
					}
				}
			}

			//temp debugging
			_skippedAt1.getAndSet(0);
			_skippedAt2.getAndSet(0);
			_skippedAt3.getAndSet(0);
			_skippedAt4.getAndSet(0);
			_skippedAt5.getAndSet(0);
			_skippedAt6.getAndSet(0);

			AlbumProfiling.getInstance ().enter/*AndTrace*/ (5, "dups.differ");

			final List<AlbumImage> imageDisplayList1 = _imageDisplayList.stream().filter(i -> filter1.accept(null, i.getName())).collect(Collectors.toList());
//			Collections.shuffle(imageDisplayList1);
			final List<AlbumImage> imageDisplayList2 = _imageDisplayList.stream().filter(i -> filter2.accept(null, i.getName())).collect(Collectors.toList());
			if (/*imageDisplayList2.size() == 0 &&*/ filter2.isEmpty()) {
				imageDisplayList2.addAll(imageDisplayList1);
			}
//			Collections.shuffle(imageDisplayList2);

			final int maxSizeToAlwaysCompareAllImages = 500;
			if (imageDisplayList1.size() <= maxSizeToAlwaysCompareAllImages && imageDisplayList2.size() <= maxSizeToAlwaysCompareAllImages) {
				Set<AlbumImage> imageDisplaySet = new HashSet<>(imageDisplayList1);
				imageDisplaySet.addAll(imageDisplayList2);

				imageDisplayList1.clear();
				imageDisplayList1.addAll(imageDisplaySet);
				imageDisplayList2.clear();
				imageDisplayList2.addAll(imageDisplaySet);
			} else {
				Collections.shuffle(imageDisplayList1);
				Collections.shuffle(imageDisplayList2);
			}

			_log.debug("AlbumImages.doDup: _imageDisplayList.size = " + _decimalFormat0.format (_imageDisplayList.size()));
			_log.debug("AlbumImages.doDup: imageDisplayList1.size = " + _decimalFormat0.format (imageDisplayList1.size()));
			_log.debug("AlbumImages.doDup: imageDisplayList2.size = " + _decimalFormat0.format (imageDisplayList2.size()));

			BlockingQueue<AlbumImagePair> queue = new ArrayBlockingQueue<> (queueSize);
			List<Thread> threads = new ArrayList<> ();
			AtomicInteger loopCount = new AtomicInteger (0);

			//start timer thread (optionally)
			AtomicReference<Timer> timer = new AtomicReference<>();
			Thread progessThread = null;
			final int maxLoopCount = imageDisplayList1.size() * imageDisplayList2.size();
			if (maxLoopCount > 100 * 1000) {
				progessThread = new Thread (() -> { //create high-priority thread to run timer
					timer.set(new Timer());
					TimerTask timerTask = new TimerTask() {
						@Override
						public void run() {
							final Thread thread = Thread.currentThread();

							if (_shutdownFlag.get()) {
								_log.info("AlbumImages.doDup: progress: shutting down progress thread");
								thread.interrupt();

							}

//							int priority = thread.getPriority();
//_log.debug("AlbumImages.doDup: timerTask priority = " + priority);
							thread.setName("DoDupProgressTimer");
							thread.setPriority(9);

							final double percentDone = 100. * loopCount.get() / maxLoopCount;
							_log.info("AlbumImages.doDup: progress: " +
									_decimalFormat1.format(percentDone) + "% (" +
									_decimalFormat0.format(loopCount.get()) + " of " +
									_decimalFormat0.format(maxLoopCount) + ") queue.size = " +
									_decimalFormat0.format(queue.size()));
						}
					};
					timer.get().scheduleAtFixedRate(timerTask, 100L, 3L * 1000); //milliseconds
				});
				progessThread.setName("DoDupProgressThread");
				progessThread.setPriority(9);
				progessThread.start();
			}

			//start differ threads
			for (int ii = 0; ii < maxThreads; ii++) {
				Thread differThread = new Thread (new ImageDifferTask(queue,
						maxStdDev,
						pairsReady,
						toBeAddedToImageDiffsTable,
						_shutdownFlag,
						_form));
				differThread.setUncaughtExceptionHandler (new VUncaughtExceptionHandler());
				differThread.setName("differThread:" + ii);
				differThread.start ();
				threads.add (differThread);
			}

			final Instant startInstant = Instant.now ();

			final Set<AlbumImagePair> alreadyRequested = ConcurrentHashMap.newKeySet(maxLoopCount / 2);
			final Set<AlbumImagePair> pairsPutInQueue = ConcurrentHashMap.newKeySet(maxLoopCount / 2);
			AtomicBoolean endedEarly = new AtomicBoolean(false);

			imageDisplayList1.stream().parallel().forEach(image1 -> {
				if (alreadyRequested.size() >= maxPairsToPutInQueue) {
					endedEarly.set(true);
					return;
				}

				if (_shutdownFlag.get()) {
					String message = "image differ aborted in outer loop";
//					_log.warn ("AlbumImages.doDup: " + message + " ********************************************************");
					form.addServletError ("Warning: " + message);

					try {
						while (queue.poll(1, TimeUnit.MILLISECONDS) != null) {
							//try to empty queue
						}
					} catch (InterruptedException ex) {
						_log.error ("AlbumImages.doDup: queue.poll failed");
						_log.error (ex);
					}

					return;
				}

				for (AlbumImage image2 : imageDisplayList2) {
					if (_shutdownFlag.get()) {
						String message = "image differ aborted in inner loop";
						_log.warn ("AlbumImages.doDup: " + message + " ********************************************************");
						form.addServletError ("Warning: " + message);
						break;
					}

					loopCount.incrementAndGet();

					if (alreadyRequested.size() >= maxPairsToPutInQueue) {
						endedEarly.set(true);
						break;
					}

					AlbumImagePair pair = new AlbumImagePair(image1, image2);
					if (!alreadyRequested.add(pair)) {
						_skippedAt1.incrementAndGet();
						continue;
					}

					if (image1.getName().equals(image2.getName())) {
						_skippedAt2.incrementAndGet();
						continue;
					}

					if (!image1.matchOrientation (image2.getOrientation ()) || !image1.matchOrientation (orientation)) {
						_skippedAt3.incrementAndGet();
						continue;
					}

					boolean sameBaseName = image1.equalBase (image2, collapseGroups);
					if (!limitedCompare || ((!reverseSort && !sameBaseName) ||
											( reverseSort &&  sameBaseName))) {
						//proceed to next test
					} else {
						_skippedAt4.incrementAndGet();
						continue;
					}

					AlbumImagePair stubPair = new AlbumImagePair (image1, image2);
					AlbumImagePair newPair = _looseCompareDataCache.getIfPresent(stubPair);
					if (newPair != null) {
						if (AlbumImage.acceptDiff (newPair.getAverageDiff(), newPair.getStdDev(), AlbumImageDiffer._maxAverageDiffUsedByBatFiles, AlbumImageDiffer._maxStdDevDiffUsedByBatFiles)) { //hardcoded values from BAT files
//TODO - should we always add here? if so, will update entries to have current timestamp
//							if (!imageDiffDetailsFromImageDiffs.containsKey(AlbumImageDiffDetails.getJoinedNameIds(image1.getNameId(), image2.getNameId()))) {
								toBeAddedToImageDiffsTable.add(new AlbumImageDiffDetails(image1.getNameId(), image2.getNameId(), newPair.getAverageDiff(), newPair.getStdDev(), 1, AlbumImageDiffer.Mode.OnDemand.name(), new Timestamp (new GregorianCalendar ().getTimeInMillis ())));
//							}
						}

						if (AlbumImage.acceptDiff(newPair.getAverageDiff(), newPair.getStdDev(), maxStdDev - 5, maxStdDev)) { //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
							pairsReady.add(newPair);
							_log.debug ("AlbumImages.doDup@1: " + newPair.getDetails3String ());
						}

						_skippedAt5.incrementAndGet();
						continue;
					}

					if (imageDiffDetailsFromImageDiffs != null && imageDiffDetailsFromImageDiffs.size() > 0) {
						String joinedIds = AlbumImageDiffDetails.getJoinedNameIds(imageNameToIdMap.get(image1.getName()), imageNameToIdMap.get(image2.getName()));
						AlbumImageDiffDetails imageDiffDetails = imageDiffDetailsFromImageDiffs.get(joinedIds);
						if (imageDiffDetails != null) {
							if (AlbumImage.acceptDiff(imageDiffDetails.getAvgDiff(), imageDiffDetails.getStdDev(), maxStdDev - 5, maxStdDev)) { //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
								AlbumImagePair newPair1 = new AlbumImagePair(image1, image2, imageDiffDetails.getAvgDiff(), imageDiffDetails.getStdDev(), AlbumImageDiffer.Mode.OnDemand.name(), imageDiffDetails.getLastUpdate());
								_looseCompareDataCache.put(newPair1, newPair1);

								pairsReady.add(newPair1);
								_log.debug("AlbumImages.doDup@2: " + newPair1.getDetails3String());

								_skippedAt6.incrementAndGet();
								continue;
							}
						}
					}

					pairsPutInQueue.add(pair);
					try {
						queue.put(pair);

					} catch (Exception ex) {
						_log.error ("AlbumImages.doDup: queue.put failed @1");
						_log.error (ex);
					}
				}
			});

			try {
				queue.put(ImageDifferTask.DONE_MARKER);
			} catch (Exception ex) {
				_log.error ("AlbumImages.doDup: queue.put failed @2");
				_log.error (ex);
			}

			if (endedEarly.get()) {
				String message = "reached max pairs in queue (" + _decimalFormat0.format (maxPairsToPutInQueue) + ")";
				_log.warn ("AlbumImages.doDup: " + message + " ********************************************************");
				form.addServletError ("Warning: " + message);
			}

			_log.debug("AlbumImages.doDup: pairs put in queue = " + _decimalFormat0.format (pairsPutInQueue.size()));

			//wait for all threads to finish
			for (Thread thread : threads) {
				try {
					thread.join ();

				} catch (Exception ex) {
					_log.error ("AlbumImages.doDup: thread.join failed");
					_log.error (ex);
				}
			}

			for (Thread thread : threads) {
				if (thread.isAlive()) {
					_log.error ("AlbumImages.doDup: thread is still running: " + thread.getName());
				}
			}

			if (timer.get() != null) {
				timer.get().cancel();
			}
			if (progessThread != null) {
				progessThread.interrupt();
			}

//TODO - fix this - does not handle situation when endedEarly
			long elapsedNanos = Duration.between (startInstant, Instant.now ()).toNanos ();
			double pairsPerSecond = (double) imageDisplayList1.size() * (double) imageDisplayList2.size() / ((double) elapsedNanos / 1e9);
			String message = "rate = " + VendoUtils.unitSuffixScale(pairsPerSecond) + "ps (" +
					_decimalFormat0.format ((long) imageDisplayList1.size() * imageDisplayList2.size()) + " / " +
					_decimalFormat1.format ((double) elapsedNanos / 1e9) + " seconds)";
			_log.debug("AlbumImages.doDup: " + message);
			_form.addServletError ("Info: " + message);

			AlbumProfiling.getInstance ().exit (5, "dups.differ");

			AlbumProfiling.getInstance ().enter (5, "dups.logging");

			_log.debug ("AlbumImages.doDup: _skippedAt1.size = " + _decimalFormat0.format (_skippedAt1.get ()) + " (pair already requested)");
			_log.debug ("AlbumImages.doDup: _skippedAt2.size = " + _decimalFormat0.format (_skippedAt2.get ()) + " (same image)");
			_log.debug ("AlbumImages.doDup: _skippedAt3.size = " + _decimalFormat0.format (_skippedAt3.get ()) + " (mismatched orientation)");
			_log.debug ("AlbumImages.doDup: _skippedAt4.size = " + _decimalFormat0.format (_skippedAt4.get ()) + " (limitedCompare)");
			_log.debug ("AlbumImages.doDup: _skippedAt5.size = " + _decimalFormat0.format (_skippedAt5.get ()) + " (matched from cache)");
			_log.debug ("AlbumImages.doDup: _skippedAt6.size = " + _decimalFormat0.format (_skippedAt6.get ()) + " (matched from image_diffs)");

			_log.debug ("AlbumImages.doDup: pairsReady.size = " + _decimalFormat0.format (pairsReady.size ()));

//			_log.debug("AlbumImages.doDup: size = " + _decimalFormat0.format(_nameScaledImageCache.size()));
//			_log.debug("AlbumImages.doDup: averageLoadPenalty = " + _decimalFormat1.format(nameScaledImageCacheStatsMinus.averageLoadPenalty() / 1e6) + " ms");
//			_log.debug("AlbumImages.doDup: evictionCount = " + _decimalFormat0.format(nameScaledImageCacheStatsMinus.evictionCount()));
//			_log.debug("AlbumImages.doDup: hitCount = " + _decimalFormat0.format(nameScaledImageCacheStatsMinus.hitCount()));
//			_log.debug("AlbumImages.doDup: hitRate = " + nameScaledImageCacheStatsMinus.hitRate());
//			_log.debug("AlbumImages.doDup: loadCount = " + _decimalFormat0.format(nameScaledImageCacheStatsMinus.loadCount()));
//			_log.debug("AlbumImages.doDup: loadSuccessCount = " + _decimalFormat0.format(nameScaledImageCacheStatsStart.loadSuccessCount()));
//			_log.debug("AlbumImages.doDup: missCount = " + _decimalFormat0.format(nameScaledImageCacheStatsMinus.missCount()));
//			_log.debug("AlbumImages.doDup: missRate = " + nameScaledImageCacheStatsMinus.missRate());
//			_log.debug("AlbumImages.doDup: totalLoadTime = " + _decimalFormat1.format(nameScaledImageCacheStatsMinus.totalLoadTime() / 1e6) + " ms");
//			_log.debug("AlbumImages.doDup: requestCount = " + _decimalFormat0.format(nameScaledImageCacheStatsMinus.requestCount()));

			{
				CacheStats nameScaledImageCacheStatsMinus = _nameScaledImageCache.stats().minus(nameScaledImageCacheStatsStart);
				double nameScaledImageCachePercent = 100 * (double) _nameScaledImageCache.size() / _nameScaledImageCacheMaxSize;
				_log.debug("nameScaledImageCache: " + _decimalFormat1.format(nameScaledImageCachePercent) + "% of " + _decimalFormat0.format(_nameScaledImageCacheMaxSize)
						+ NL + nameScaledImageCacheStatsMinus);

				_log.debug("DIST: Added:");
				Map<String, List<AlbumImage>> map1 = _nameScaledImageCacheAdded.stream().collect(Collectors.groupingBy(i -> i.getBaseName(true)));
				map1.keySet().stream().sorted().forEach(d -> _log.debug("Added: " + d + " -> " + _decimalFormat0.format(map1.get(d).size()) + " images"));

				_log.debug("DIST: Evicted:");
				Map<String, List<AlbumImage>> map2 = _nameScaledImageCacheEvicted.stream().collect(Collectors.groupingBy(i -> i.getBaseName(true)));
				map2.keySet().stream().sorted().forEach(d -> _log.debug("Evicted: " + d + " -> " + _decimalFormat0.format(map2.get(d).size()) + " images"));
			}
			{
				CacheStats looseCompareDataCacheStatsMinus = _looseCompareDataCache.stats().minus(looseCompareDataCacheStatsStart);
				double looseCompareDataCachePercent = 100 * (double) _looseCompareDataCache.size() / _looseCompareDataCacheMaxSize;
				_log.debug("looseCompareDataCache: " + _decimalFormat1.format(looseCompareDataCachePercent) + "% of " + _decimalFormat0.format(_looseCompareDataCacheMaxSize)
						+ NL + looseCompareDataCacheStatsMinus);

				_log.debug("DIST: Added:");
				Map<String, List<AlbumImage>> map1 = _looseCompareDataCacheAdded.stream().flatMap(p -> Stream.of(p.getImage1(), p.getImage2()))
						.collect(Collectors.groupingBy(i -> i.getBaseName(true)));
				map1.keySet().stream().sorted().forEach(d -> _log.debug("Added: " + d + " -> " + _decimalFormat0.format(map1.get(d).size() / 2) + " pairs"));

				_log.debug("DIST: Evicted:");
				Map<String, List<AlbumImage>> map2 = _looseCompareDataCacheEvicted.stream().flatMap(s -> Stream.of(s.getImage1(), s.getImage2()))
						.collect(Collectors.groupingBy(i -> i.getBaseName(true)));
				map2.keySet().stream().sorted().forEach(d -> _log.debug("Evicted: " + d + " -> " + _decimalFormat0.format(map2.get(d).size() / 2) + " pairs"));
			}

//save as example
//			if (nameScaledImageCacheHitsList != null) {
//				Map<String, Long> nameScaledImageCacheHitsDist = nameScaledImageCacheHitsList.stream()
//						.map(AlbumImage::getName)
//						.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//				_log.debug("AlbumImages.doDup: nameScaledImageCacheHitsDist = " + nameScaledImageCacheHitsDist);
//			}
//			if (nameScaledImageCacheMissedList != null) {
//				Map<String, Long> nameScaledImageCacheMissesDist = nameScaledImageCacheMissedList.stream()
//						.map(AlbumImage::getName)
//						.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//				_log.debug("AlbumImages.doDup: nameScaledImageCacheMissesDist = " + nameScaledImageCacheMissesDist);
//			}

			if (true) { //statement block for logging
				List<String> info = toBeAddedToImageDiffsTable.stream()
//						.filter(i -> !imageDiffDetailsFromImageDiffs.containsKey(AlbumImageDiffDetails.getJoinedNameIds(i.getNameId1(), i.getNameId2())))
						.map(i -> {
							String imageName1 = imageIdToNameMap.get(i.getNameId1());
							String imageName2 = imageIdToNameMap.get(i.getNameId2());
							if (imageName1 == null || imageName2 == null) {
								return null; //pair not found in map
							}
							return imageName1 + ", " + imageName2 + " - " + i;
						})
						.filter(Objects::nonNull)
						.sorted(String.CASE_INSENSITIVE_ORDER)
						.collect(Collectors.toList());

				if (!info.isEmpty()) {
					_log.debug ("AlbumImages.doDup: imageDiffDetails to be added to image_diffs table = " + _decimalFormat0.format (info.size()));
				} else {
					_log.debug ("AlbumImages.doDup: No imageDiffDetails to be added to image_diffs table");
				}
			}
			if (!toBeAddedToImageDiffsTable.isEmpty()) {
				int rowsInserted = AlbumImageDiffer.getInstance().insertImageIntoImageDiffs (toBeAddedToImageDiffsTable);
				_log.debug ("AlbumImages.doDup: imageDiffDetails actually added to database = " + _decimalFormat0.format (rowsInserted) + " items");
			}

			AlbumProfiling.getInstance ().exit (5, "dups.logging");

			AlbumProfiling.getInstance ().enterAndTrace (5, "dups.add");

			for (AlbumImagePair imagePair : pairsReady) {
				dups.add (imagePair);
//				_log.debug ("AlbumImages.doDup: " + imagePair.getDetails3String ());
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
				long highlightInMillis = _form.getHighlightInMillis (false);
				_log.debug ("AlbumImages.doDup: since/highlight date/time: " + _dateFormat.format (new Date (highlightInMillis)));

				AlbumImageDiffer albumImageDiffer = AlbumImageDiffer.getInstance ();
				Collection<AlbumImageDiffData> imageDiffData = albumImageDiffer.selectNamesFromImageDiffs (maxStdDev - 5, maxStdDev, sinceDays, operatorOr); //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
				_log.debug ("AlbumImages.doDup: imageDiffData.size(): " + _decimalFormat0.format (imageDiffData.size ()));

				for (AlbumImageDiffData imageDiffItem : imageDiffData) {
					AlbumImage image1 = map.get (imageDiffItem.getName1 ());
					AlbumImage image2 = map.get (imageDiffItem.getName2 ());
					//images can be null if excludes was used
					if (image1 != null && image2 != null && image1.matchOrientation (image2.getOrientation ()) && image1.matchOrientation (orientation)) {
						//at least one of each pair must be accepted by filter1
						if (filter1.accept (null, image1.getName ()) || filter1.accept (null, image2.getName ())) {
							boolean sameBaseName = image1.equalBase (image2, collapseGroups);
							if (!limitedCompare || ((!reverseSort && !sameBaseName) ||
													( reverseSort &&  sameBaseName))) {
								AlbumImagePair pair = new AlbumImagePair (image1, image2, imageDiffItem.getAverageDiff (), imageDiffItem.getStdDev (), imageDiffItem.getSource (), imageDiffItem.getLastUpdate());
								dups.add (pair);

//TODO - need/want this?
//								_looseCompareDataCache.put (pair, pair);
							}
						}
					}
				}

				AlbumProfiling.getInstance ().exit (5, "dups.db");

			} else if (useExifDates) {
				AlbumProfiling.getInstance ().enterAndTrace (5, "dups.exif");

				Set<AlbumImagePair> dupSet = new HashSet<> (); //use Set to avoid duplicate AlbumImagePairs

				//check for exif dups using all valid values of NumFileExifDates
//				for (int ii = 0; ii < AlbumImage.NumFileExifDates; ii++) {
				for (int ii = 0; ii < AlbumImage.NumExifDates; ii++) {
					//get a new list, sorted for each exifDateIndex
					List<AlbumImage> list1 = new ArrayList<> (numImages);
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
//TODO - have one max for all dups, another for near/exact dups
		final int maxDupsSetsToShowLinksFor = 1200; //was 1000
//		if (!dbCompare && dups.size () > maxDupsSetsToShowLinksFor) {
		if (/*!dbCompare && */dups.size () > maxDupsSetsToShowLinksFor) {
			_form.addServletError("Info: exceeded count@3; not shown (" + dups.size() + ")");
		} else {

			AlbumProfiling.getInstance ().enter (5, "dups.sets");

//			String[] allInputFilters = ArrayUtils.addAll(form.getFilters (1), form.getFilters (2));
//			List<String> allAlbumsAcrossAllInputFilters = getMatchingAlbumsForFilters (allInputFilters, false, 0);
//			if (allAlbumsAcrossAllInputFilters.size() > 0) {
//				if (allAlbumsAcrossAllInputFilters.size() <= 200) {
//					String filtersAll = allAlbumsAcrossAllInputFilters.stream().sorted(_alphanumComparator).collect(Collectors.joining(","));
//					String href = AlbumImages.getInstance().generateImageLink(filtersAll, filtersAll, AlbumMode.DoSampler, form.getColumns(), form.getSinceDays(), false, true);
//					StringBuilder html = new StringBuilder();
////TODO - move to helper class/method
//					html.append("<A HREF=\"")
//							.append(href)
//							.append("\" ")
//							.append("title=\"").append(filtersAll)
//							.append("\" target=_blank>")
//							.append(filtersAll)
//							.append("</A>");
//					_form.addServletError("Info: all albums across all input filters: [" + allAlbumsAcrossAllInputFilters.size() + "] " + html);
//				} else {
//					_form.addServletError("Info: all albums across all input filters: exceeded count@0; not shown (" + allAlbumsAcrossAllInputFilters.size() + ")");
//				}
//			}

			//this map is for single pairs only
			Map<String, AlbumAlbumPair> dupAlbumMap = new HashMap<> ();
			for (AlbumImagePair pair : dups) {
				boolean sameBaseName = pair.getImage1 ().equalBase (pair.getImage2 (), collapseGroups);
				if (!sameBaseName) {
					String joinedNames = pair.getJoinedNames ();
					AlbumAlbumPair dupSet = dupAlbumMap.get (joinedNames);
					if (dupSet != null) {
						dupSet.addImagePair (pair);
					} else {
						dupAlbumMap.put (joinedNames, new AlbumAlbumPair(joinedNames, pair));
					}
				}
				addToDuplicatesCache(_duplicatesCache, pair.getImage1 ().getName());
				addToDuplicatesCache(_duplicatesCache, pair.getImage2 ().getName());
			}

			//this object holds single and multi-pairs
			AlbumAlbumPairs albumPairs = new AlbumAlbumPairs();
			albumPairs.addAlbumPairs(dupAlbumMap.values());

			//only enable highlights if we have more than one album
			final boolean enableHighlightForDetailString = albumPairs.getAllAlbumsAcrossAllMatches(true).size() > 1;

			if (!dupAlbumMap.isEmpty()) {
				List<String> allAlbumsAcrossAllMatches = albumPairs.getAllAlbumsAcrossAllMatches(false);
				if (allAlbumsAcrossAllMatches.size() <= 200) {
					String filtersAll = allAlbumsAcrossAllMatches.stream().sorted(_alphanumComparator).collect(Collectors.joining(","));
					String href = AlbumImages.getInstance().generateImageLink(filtersAll, filtersAll, AlbumMode.DoSampler, form.getColumns(), form.getSinceDays(), false, true);
					StringBuilder html = new StringBuilder();
//TODO - move to helper class/method
					html.append("<A HREF=\"")
							.append(href)
							.append("\" ")
							.append("title=\"").append(filtersAll)
							.append("\" target=_blank>")
							.append(filtersAll)
							.append("</A>");
					_form.addServletError("Info: all albums across all matches: [" + allAlbumsAcrossAllMatches.size() + "] " + html);
				} else {
					_form.addServletError("Info: all albums across all matches: exceeded count@1; not shown (" + allAlbumsAcrossAllMatches.size() + ")");
				}

				String[] allInputFilters = ArrayUtils.addAll(form.getFilters (1), form.getFilters (2));
				List<String> allAlbumsAcrossAllInputFiltersAndMatches = getMatchingAlbumsForFilters (allInputFilters, false, 0);
				allAlbumsAcrossAllInputFiltersAndMatches.addAll(allAlbumsAcrossAllMatches);
				allAlbumsAcrossAllInputFiltersAndMatches = VendoUtils.caseInsensitiveSortAndDedup(allAlbumsAcrossAllInputFiltersAndMatches);
				if (allAlbumsAcrossAllInputFiltersAndMatches.size() <= 200) {
					String filtersAll = allAlbumsAcrossAllInputFiltersAndMatches.stream().sorted(_alphanumComparator).collect(Collectors.joining(","));
					String href = AlbumImages.getInstance().generateImageLink(filtersAll, filtersAll, AlbumMode.DoSampler, form.getColumns(), form.getSinceDays(), false, true);
					StringBuilder html = new StringBuilder();
//TODO - move to helper class/method
					html.append("<A HREF=\"")
							.append(href)
							.append("\" ")
							.append("title=\"").append(filtersAll)
							.append("\" target=_blank>")
							.append(filtersAll)
							.append("</A>");
					_form.addServletError("Info: all albums across all input filters and matches: [" + allAlbumsAcrossAllInputFiltersAndMatches.size() + "] " + html);
				} else {
					_form.addServletError("Info: all albums across all input filters and matches: exceeded count@0; not shown (" + allAlbumsAcrossAllInputFiltersAndMatches.size() + ")");
				}

				List<String> albumPairsDetails = albumPairs.getDetailsStrings(2); //only show actual multi-sets (i.e., more than 1 pair)
				_log.debug("AlbumImages.doDup: sortedDupDetailStringSet.size = " + _decimalFormat0.format(albumPairsDetails.size()) + " dup multi sets ----------------------------------------");
				if (albumPairsDetails.size() > 0) {
					if (albumPairsDetails.size() < 800) {
						_form.addServletError("Info: found duplicate multi sets: " + _decimalFormat0.format(albumPairsDetails.size()));
						for (String string : albumPairsDetails) {
//							_log.debug(string);
							_form.addServletError("Info: found duplicate multi sets: " + string);
						}
					} else {
						_form.addServletError("Info: found duplicate multi sets: exceeded count@4; not shown (" + albumPairsDetails.size() + ")");
					}
				}
			}

			//log/print/display all exact/near duplicates
			Set<AlbumAlbumPair> handledSets = new HashSet<>();
			List<String> nameOfExactDuplicateThatCanBeDeletedList = new ArrayList<>();
			List<String> nameOfNearDuplicateThatCanBeDeletedList = new ArrayList<>();
			List<String> allAlbumsAcrossCloseMatches = new ArrayList<>();
			for (AlbumAlbumPair imageSet : dupAlbumMap.values()) {
				if (imageSet.secondAlbumRepresentsExactDuplicate()) {
					String secondBaseNameForSorting = imageSet.getBaseName(1); //select the larger (numerically)
					addToDuplicatesCache(_nameOfExactDuplicateThatCanBeDeleted, secondBaseNameForSorting);
					nameOfExactDuplicateThatCanBeDeletedList.add("Info: found albums that can be deleted (exact duplicate pair): [" + secondBaseNameForSorting + "] - " + imageSet.getDetailString(enableHighlightForDetailString));
					handledSets.add(imageSet);
					allAlbumsAcrossCloseMatches.addAll(imageSet.getBaseNames());
				} else if (imageSet.secondAlbumRepresentsNearDuplicate ()) {
					String secondBaseNameForSorting = imageSet.getBaseName(1); //select the larger (numerically)
					addToDuplicatesCache(_nameOfNearDuplicateThatCanBeDeleted, secondBaseNameForSorting);
					nameOfNearDuplicateThatCanBeDeletedList.add("Info: found albums that can be deleted (near duplicate pair): [" + secondBaseNameForSorting + "] - " + imageSet.getDetailString(enableHighlightForDetailString));
					handledSets.add(imageSet);
					allAlbumsAcrossCloseMatches.addAll(imageSet.getBaseNames());
				}
			}
			if (!nameOfExactDuplicateThatCanBeDeletedList.isEmpty()) {
				nameOfExactDuplicateThatCanBeDeletedList.sort(_alphanumComparator);

				_form.addServletError("Info: found albums that can be deleted (exact duplicate pair): " + nameOfExactDuplicateThatCanBeDeletedList.size());
				nameOfExactDuplicateThatCanBeDeletedList.forEach(s -> _form.addServletError(s));

//				_log.debug ("AlbumImages.doDup: nameOfExactDuplicateThatCanBeDeletedList.size = " + _decimalFormat0.format (nameOfExactDuplicateThatCanBeDeletedList.size ()) + " exact dup sets -----------------------------------");
//				nameOfExactDuplicateThatCanBeDeletedList.forEach(_log::debug);
			}

			if (!nameOfNearDuplicateThatCanBeDeletedList.isEmpty()) {
				nameOfNearDuplicateThatCanBeDeletedList.sort(_alphanumComparator);

				_form.addServletError("Info: found albums that can be deleted (near duplicate pair): " + nameOfNearDuplicateThatCanBeDeletedList.size());
				nameOfNearDuplicateThatCanBeDeletedList.forEach(s -> _form.addServletError(s));

//				_log.debug ("AlbumImages.doDup: nameOfNearDuplicateThatCanBeDeletedList.size = " + _decimalFormat0.format (nameOfNearDuplicateThatCanBeDeletedList.size ()) + " near dup sets -----------------------------------");
//				nameOfNearDuplicateThatCanBeDeletedList.forEach(_log::debug);
			}

			if (!allAlbumsAcrossCloseMatches.isEmpty()) {
				if (allAlbumsAcrossCloseMatches.size() <= 250) {
					allAlbumsAcrossCloseMatches = allAlbumsAcrossCloseMatches.stream().sorted(_alphanumComparator).distinct().collect(Collectors.toList()); //dedup list
					String filtersAll = String.join(",", allAlbumsAcrossCloseMatches);
					String href = AlbumImages.getInstance().generateImageLink(filtersAll, filtersAll, AlbumMode.DoSampler, form.getColumns(), form.getSinceDays(), false, true);
					StringBuilder html = new StringBuilder();
//TODO - move to helper class/method
					html.append("<A HREF=\"")
							.append(href)
							.append("\" ")
							.append("title=\"").append(filtersAll)
							.append("\" target=_blank>")
							.append(filtersAll)
							.append("</A>");
					_form.addServletError("Info: all albums across close matches: [" + allAlbumsAcrossCloseMatches.size() + "] " + html);
				} else {
					_form.addServletError("Info: all albums across close matches: exceeded count@2; not shown (" + allAlbumsAcrossCloseMatches.size() + ")");
				}
			}

			//log/print/display all the rest
			List<String> sortedDupDetailStringSet = dupAlbumMap.values().stream()
					.filter(i -> !handledSets.contains(i))
					.map(p -> p.getDetailString(enableHighlightForDetailString))
					.sorted(_alphanumComparator)
					.collect(Collectors.toList());

			_log.debug ("AlbumImages.doDup: sortedDupDetailStringSet.size = " + _decimalFormat0.format (sortedDupDetailStringSet.size ()) + " dup sets ----------------------------------------");
			if (sortedDupDetailStringSet.size () > 0 && sortedDupDetailStringSet.size () < 800) {
				_form.addServletError ("Info: found duplicate sets: " + _decimalFormat0.format (sortedDupDetailStringSet.size ()));
				for (String string : sortedDupDetailStringSet) {
//					_log.debug (string);
					_form.addServletError ("Info: found duplicate sets: " + string);
				}
			}

			AlbumProfiling.getInstance ().exit (5, "dups.sets");
		}

		if (dbCompare) {
			_imageDisplayList = AlbumImagePair.getImages (dups, AlbumSortType.ByNone); //already sorted by db query
		} else {
			_imageDisplayList = AlbumImagePair.getImages (dups, AlbumSortType.ByDate); //shows the newest first in browser
		}

		AlbumProfiling.getInstance ().exit (1);

		return _imageDisplayList.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private int doSampler (String[] filters, String[] excludes, long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		int numImages = doDir (filters, excludes, sinceInMillis);

		List<AlbumImage> list = new ArrayList<> (numImages);
		list.addAll (_imageDisplayList);
		AlbumImage[] images = list.toArray (new AlbumImage[] {});

		boolean collapseGroups = _form.getCollapseGroups ();

//TODO - eliminate dups (for example don't add both file01-* and file*)
		List<AlbumImage> sampler = new ArrayList<> (numImages / 3);
		Set<String> set = new HashSet<> (numImages / 3);
		for (AlbumImage image : images) {
			String wildName = image.getBaseName (collapseGroups) + "*";
			if (set.add (wildName)) { //returns true if added, false if dup
				sampler.add (image);
			}
		}
//		_imageDisplayList = new TreeSet<> (new AlbumImageComparator (_form));
		_imageDisplayList = new ArrayList<> (sampler.size ());
		_imageDisplayList.addAll (sampler);
//TODO - sort here?

		AlbumProfiling.getInstance ().exit (1);

		return _imageDisplayList.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	//operates directly on _imageDisplayList
	//checks stuff other than image count; e.g., image numbering must be three digits and must start with "0"
	public Set<String> removeAlbumsWithLargeCounts (final int minImagesToFlagAsLargeAlbum)
	{
		AlbumProfiling.getInstance ().enter (5);

		//get baseNames to reduce number of calls to getNumMatchingImages() below (only call once per album, instead of once per image)
		List<String> baseNames = _imageDisplayList.stream()
												   .filter(i -> {
														String imageDigits = i.getName().replaceAll(".*\\d-", "");
														return imageDigits.length() == 3 && (imageDigits.startsWith("00") || imageDigits.startsWith("01"));  //image digits must be three and must start in the '0's (e.g. "001")
												   })
												   .map(i -> i.getBaseName(false))
												   .sorted(String.CASE_INSENSITIVE_ORDER)
												   .distinct()
												   .collect(Collectors.toList ());

		Set<String> baseNamesTooLarge = baseNames.stream()
												 .filter(i -> AlbumImageDao.getInstance ().getNumMatchingImages(i, 0) > minImagesToFlagAsLargeAlbum)
												 .collect(Collectors.toSet());

		_imageDisplayList = _imageDisplayList.stream()
											 .filter(i -> !baseNamesTooLarge.contains(i.getBaseName(false)))
											 .collect(Collectors.toList ());

		AlbumProfiling.getInstance ().exit (5);

		return baseNamesTooLarge;
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

//unused
//	///////////////////////////////////////////////////////////////////////////
//	public int getMaxComparisons (long numImages1, long numImages2)
//	{
//		long maxComparisons = 1;
//		if (numImages1 > 2 && numImages2 > 2) {
//			maxComparisons = numImages2 * (numImages2 - 1) / 2;
//		}
//
//		if (maxComparisons > Integer.MAX_VALUE) {
//			maxComparisons = Integer.MAX_VALUE;
//		}
//
//		return (int) maxComparisons;
//	}

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
		} else if (image.getNumBytes() > _form.getHighlightMaxKilobytes () * 1024L
				|| image.getWidth() > _form.getHighlightMaxPixels () || image.getHeight() > _form.getHighlightMaxPixels ()) {
			fontColor = "magenta"; //too large (by bytes or pixels)
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
			sb.append (_form.getTagModeHtml (ii));
			sb.append (_form.getTagHtml (ii));
//			sb.append ("&tagMode").append (ii).append ("=").append (_form.getTagMode (ii).getSymbol ());
//			sb.append ("&tag").append (ii).append ("=").append (_form.getTag (ii));
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
			.append ("&minImagesToFlagAsLargeAlbum=").append (_form.getMinImagesToFlagAsLargeAlbum ())
//			.append ("&rootFolder=").append (_form.getRootFolder ())
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
//			.append ("&screenWidth=").append (_form.getScreenWidth ())
//			.append ("&screenHeight=").append (_form.getScreenHeight ())
			.append ("&windowWidth=").append (_form.getWindowWidth ())
			.append ("&windowHeight=").append (_form.getWindowHeight ())
			.append ("&timestamp=").append (timestamp)
			.append ("&slice=").append (slice)
			.append (_Debug ? "&debug=on" : "")
//			.append ("#topAnchor")
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
		String limitedCompareStr = limitedCompare ? "&limitedCompare=true" : "";
		String looseCompareStr = looseCompare ? "&looseCompare=true" : "";

		//apparently we only propagate the sortType if it is by size (bytes or pixels); otherwise we use the default sortType
		AlbumSortType sortType = _form.getSortType ();
		String sortTypeStr = (sortType == AlbumSortType.BySizeBytes || sortType == AlbumSortType.BySizePixels) ? "&sortType=" + sortType.getSymbol () : "";

		StringBuilder sb = new StringBuilder (200);
		sb.append ("?mode=").append (mode.getSymbol ())
			.append ("&filter1=").append (filter1)
			.append ("&filter2=").append (filter2)
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
			.append ("&minImagesToFlagAsLargeAlbum=").append (_form.getMinImagesToFlagAsLargeAlbum ())
//			.append ("&rootFolder=").append (_form.getRootFolder ())
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
//			.append ("&screenWidth=").append (_form.getScreenWidth ())
//			.append ("&screenHeight=").append (_form.getScreenHeight ())
			.append ("&windowWidth=").append (_form.getWindowWidth ())
			.append ("&windowHeight=").append (_form.getWindowHeight ())
			.append ("&slice=").append (1)
			.append (_Debug ? "&debug=on" : "");
//			.append ("#topAnchor");

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
		if (_Debug) {
			_log.debug ("AlbumImages.generateHtml: numImages = " + numImages);
		}

		boolean isAndroidDevice = _form.isAndroidDevice ();

		String font1 = !isAndroidDevice ? "fontsize10" : "fontsize24";
		String tableWidthString = !isAndroidDevice ? "100%" : "2200"; //TODO - tablet hardcoded for portrait not landscape
		_log.debug ("AlbumImages.generateHtml: tableWidthString = " + tableWidthString);

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
		if (isAndroidDevice) {
			cols = mode == AlbumMode.DoDir ? 1 : 2; //hack - for android, only 1 or 2 columns (see also AlbumServlet.java)
		}
		int rows = getNumRows ();
		int numSlices = getNumSlices ();
		int slice = _form.getSlice ();
		boolean looseCompare = _form.getLooseCompare ();
		boolean collapseGroups = _form.getCollapseGroups ();
		//for mode = AlbumMode.DoDup, disable collapseGroups for tags
		boolean collapseGroupsForTags = mode == AlbumMode.DoDup ? false : collapseGroups;
		boolean interleaveSort = _form.getInterleaveSort();
		AlbumDuplicateHandling duplicateHandling = _form.getDuplicateHandling ();
		String[] allFilters = _form.getFilters();

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
			start = 0; //(slice - 1) * (rows * cols);
			end = start + (rows * cols);
		}

		double sinceDays = _form.getSinceDays ();
		long sinceInMillis = _form.getSinceInMillis (true);
		String sinceStr = sinceInMillis > 0 ? " (since " + _dateFormat.format (new Date (sinceInMillis)) + ")" : "";

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
		int imageOutlinePixels = 2;//(mode == AlbumMode.DoSampler && (!_nameOfExactDuplicateThatCanBeDeleted.isEmpty() || !_nameOfNearDuplicateThatCanBeDeleted.isEmpty()) ? 2 : 0);
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
//			tableWidth = (_form.getWindowWidth () > _form.getWindowHeight () ? 2560 : 1600); //hack - hardcode
//			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + imageOutlinePixels + padding));
//			imageHeight = (_form.getWindowWidth () > _form.getWindowHeight () ? 1600 : 2560); //hack - hardcode
			tableWidth = Math.min(_form.getWindowWidth(), _form.getWindowHeight());
			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + imageOutlinePixels + padding));
			imageHeight = Math.max(_form.getWindowWidth (), _form.getWindowHeight ());

		} else {
			//values for firefox
			tableWidth = _form.getWindowWidth () - scrollBarWidth;
			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + imageOutlinePixels + padding));
//			imageHeight = _form.getWindowHeight () - 40; //for 1080p
//			imageHeight = _form.getWindowHeight () - 60; //for 1440p
			imageHeight = _form.getWindowHeight () - 80; //for 2160p (4k)
		}
		if (isAndroidDevice) {
			_log.debug("AlbumImages.generateHtml: isAndroidDevice = " + isAndroidDevice + ", tableWidth = " + tableWidth + ", imageWidth = " + imageWidth + ", imageHeight = " + imageHeight);
			_log.debug("AlbumImages.generateHtml: isAndroidDevice = " + isAndroidDevice + ", form.windowWidth = " + _form.getWindowWidth() + ", form.windowHeight = " + _form.getWindowHeight());
		}

		sb.append ("<TABLE WIDTH=")
				.append (tableWidth)
				.append (" CELLSPACING=0 CELLPADDING=")
				.append (padding)
				.append (" BORDER=")
				.append (_tableBorderPixels)
				.append (">").append (NL);

		if (interleaveSort) {
			if (mode == AlbumMode.DoDir) { //TODO check for invalid combos and report below
				Map<String, List<AlbumImage>> map = _imageDisplayList.stream().collect(Collectors.groupingBy(i -> i.getBaseName(false)));
				map.keySet().stream().sorted().forEach(d -> map.get(d).sort(new AlbumImageComparator(AlbumSortType.ByName)));

				_imageDisplayList.clear();
				List<Iterator<AlbumImage>> iters = map.keySet().stream().sorted().map(map::get).map(List::iterator).collect(Collectors.toList());
				boolean mightHaveMore;
				do {
					mightHaveMore = false;
					for (Iterator<AlbumImage> iter : iters) {
						if (iter.hasNext()) {
							_imageDisplayList.add(iter.next());
							mightHaveMore = true;
						}
					}
				} while (mightHaveMore);

			} else {
				String message = "Invalid combination ... TBD";
				_log.debug("AlbumImages.generateHtml: " + message);
				_form.addServletError("Info: " + message);
			}
		}

		AlbumImage[] images = _imageDisplayList.toArray (new AlbumImage[] {});

		String font2 = "fontsize10";
		if (isAndroidDevice) {
			font2 = "fontsize24";
//			font2 = "fontsize28";
		} else if (imageWidth <= 120) {
			font2 = "fontsize8";
		} else if (imageWidth <= 200) {
			font2 = "fontsize9";
		} else {
			font2 = "fontsize10";
		}
		if (isAndroidDevice) {
			_log.debug("AlbumImages.generateHtml: isAndroidDevice = " + isAndroidDevice + ", imageWidth = " + imageWidth + ", font2 = " + font2);
		}

		//TODO - print map of dates and image counts
		if (_imageDisplayList.size() < 10 * 1000) {
			AlbumProfiling.getInstance ().enter (5, "dateDistribution");
/*TODO
			List<String> albums = _imageDisplayList.stream()
					.map(i -> i.getBaseName(false))
					.distinct()
					.collect(Collectors.toList());

			List<String> albumsMatchingFilters = getMatchingAlbumsForFilters (albums, false, 0);

			Map<LocalDate, List<AlbumImage>> map = albumsMatchingFilters.stream()
*/
			Map<LocalDate, List<AlbumImage>> map = _imageDisplayList.stream()
					.collect(Collectors.groupingBy(i -> Instant.ofEpochMilli(i.getModified()).atZone(ZoneId.systemDefault()).toLocalDate()));

			final int maxItemsToPrint = 10;
			_log.debug("DIST: Top " + maxItemsToPrint + " most recent dates:");
			map.keySet().stream().sorted(Comparator.reverseOrder()).limit(maxItemsToPrint).forEach(d -> {
				_log.debug("DIST: " + d + " -> " + map.get(d).size() + " images");
			});

			AlbumProfiling.getInstance ().exit (5, "dateDistribution");
		}

		if (_imageDisplayList.size() < 10 * 1000) {
			AlbumProfiling.getInstance ().enter (5, "pixelSizeDistribution");

			Map<String, List<AlbumImage>> map = _imageDisplayList.stream()
					.collect(Collectors.groupingBy(i -> "" + i.getWidth() + "x" + i.getHeight()));

			final int maxItemsToPrint = 10;
			_log.debug("DIST: Top " + maxItemsToPrint + " sizes in pixels:");
			map.keySet().stream().sorted(new AlphanumComparator(AlphanumComparator.SortOrder.Reverse)).limit(maxItemsToPrint).forEach(d -> {
				_log.debug("DIST: " + d + " -> " + map.get(d).size() + " images");
			});

			AlbumProfiling.getInstance ().exit (5, "pixelSizeDistribution");
		}

		//go through the slice once to see if any of the images are dups
		boolean anyDupsInSlice = false;
		int imageCount = 0;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				AlbumImage image = images[start + imageCount];
				if (findInDuplicatesCache(_duplicatesCache, image.getName ())) {
					anyDupsInSlice = true;
					break;
				}
				imageCount++;
				if ((start + imageCount) >= numImages) {
					break;
				}
			}
			if ((start + imageCount) >= numImages) {
				break;
			}
		}

		Set<AlbumImage> imagesInSlice = new HashSet<> (); //use Set to eliminate duplicates
		int numMatchesFilter = 0;
		/*int*/ imageCount = 0;
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
				boolean hasExifDates = false;

				String fontColor = getFontColor (image);

				if (mode == AlbumMode.DoSampler) {
					//if collapseGroups is enabled, disable it in the drilldown when there is only one group
					AlbumMode newMode = AlbumMode.DoDir;
					boolean limitedCompare = false;

					int newCols = defaultCols;
					if (collapseGroups) {
						if (AlbumImageDao.getInstance ().getNumMatchingImages (image.getBaseName (false), sinceInMillis) !=
							AlbumImageDao.getInstance ().getNumMatchingImages (image.getBaseName (true), sinceInMillis)) {
							newMode = AlbumMode.DoSampler;
							newCols = _form.getColumns ();
							limitedCompare = true;
						}

					} else {
						//set image line style, thickness, and color to distinguish near and exact duplicates, etc.
						boolean matches = Arrays.stream(allFilters).anyMatch(image.getBaseName(false)::equalsIgnoreCase);
//						boolean allmatch = Arrays.stream(allFilters).allMatch(image.getBaseName(false)::equalsIgnoreCase);
//						boolean matches = anyMatch && !allmatch;
						numMatchesFilter += (matches ? 1 : 0);
						boolean nearDup = findInDuplicatesCache(_nameOfNearDuplicateThatCanBeDeleted, image.getBaseName (false));
						boolean exactDup = findInDuplicatesCache(_nameOfExactDuplicateThatCanBeDeleted, image.getBaseName (false));
						fontColor = (exactDup ? "lime" : nearDup ? "yellow" : getFontColor (image));
						imageBorderStyleStr = (nearDup || exactDup ? "dashed" : "solid");
						imageBorderPixels = (nearDup || exactDup ? 2 : 1);
						imageBorderColorStr = (exactDup ? "lime"
											 : nearDup  ? "yellow"
											 : "blue");
						imageOutlineStr = (exactDup ? "; outline: " + imageOutlinePixels + "px dotted red"
										 : nearDup  ? "; outline: " + imageOutlinePixels + "px dotted orange"
										 : matches  ? "; outline: " + imageOutlinePixels + "px dashed fuchsia"
										 : "");

//						List<AlbumImage> matchingImages = getMatchingImagesForImage(image, collapseGroups, 0);
//						hasExifDates = matchingImages.stream().anyMatch(AlbumImage::hasExifDate);
					}

//count: here I actually need to know the number of images that will be shown in the drilldown page...
//like, getNumMatchingGroups()

//TODO - clean this up
					imageName = image.getBaseName (collapseGroups);
					String imageName1 = imageName + (collapseGroups ? "+" : ""); //plus sign here means digit
					String filters = imageName1 + "," + imageName1;
					href = generateImageLink (filters, filters, newMode, newCols, sinceDays, limitedCompare, looseCompare);

//TODO: rewrite as this is too slow
//					List<AlbumImage> matchingImages = getMatchingImagesForImage(image, collapseGroups, 0);
//					hasExifDates = matchingImages.stream().anyMatch(AlbumImage::hasExifDate);

					hasExifDates = AlbumImageDao.getInstance().getAlbumHasExifData(imageName, sinceInMillis);

					details.append ("(")
							.append (collapseGroups ? AlbumImageDao.getInstance ().getNumMatchingAlbums(imageName, sinceInMillis) + ":" : "")
							.append (AlbumImageDao.getInstance ().getNumMatchingImages (imageName, sinceInMillis))
							.append (")")
							.append (hasExifDates ? "*" : "");

					imagesInSlice.add (image);

				} else if (mode == AlbumMode.DoDup) {
					//old: drill down to dir; always disable collapseGroups
//					AlbumMode newMode = AlbumMode.DoDir;
					AlbumMode newMode = AlbumMode.DoDup;

					boolean isEven = (((start + imageCount) & 1) == 0);
					imageName = image.getName ();
					AlbumImage partner = images[start + imageCount + (isEven ? +1 : -1)];

					String imageNonUniqueString = image.getBaseName (false);
					String partnerNonUniqueString = partner.getBaseName (false);

					imagesInSlice.add (image);
					imagesInSlice.add (partner);

					//set image border color based on average diff from looseCompareMap
					//for web color names, see http://www.w3schools.com/colors/colors_names.asp
					imageBorderColorStr = "black";
					if (looseCompare) {
						AlbumImagePair stubPair = new AlbumImagePair (image, partner);
						AlbumImagePair pair = _looseCompareDataCache.getIfPresent (stubPair);
						if (pair != null) {
							extraDiffString += AlbumImage.HtmlNewline + pair.getDetails1String ();
							int minDiff = pair.getMinDiff ();
							imageBorderColorStr = (minDiff < 1 ? "white" : minDiff < 10 ? "green" : minDiff < 20 ? "yellow" : "orange");
						}
					}

					//set image line style to distinguish smaller image
					imageBorderStyleStr = image.compareToByPixels (partner) < 0 ? "dashed" : "solid";

					if (duplicateHandling != AlbumDuplicateHandling.SelectNone) {
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
					int imageCols = AlbumImageDao.getInstance ().getNumMatchingImages (imageNonUniqueString, 0);
					int partnerCols = AlbumImageDao.getInstance ().getNumMatchingImages (partnerNonUniqueString, 0);
//					int newCols = (imageName.compareToIgnoreCase (partner.getName ()) < 0 ? imageCols : partnerCols);
					int newCols = 8; //HACK - TODO for now hardcode to 8

					//but prevent using columns = 1
					if (newCols == 1) {
						newCols = (imageCols + partnerCols) / 2;
					}

					String filters = imageNonUniqueString + "," + partnerNonUniqueString;
					href = generateImageLink (filters, filters, newMode, newCols, 0, false, true);

					details.append ("(")
							.append (imageCols)
							.append ("/")
							.append (image.getScaleString ())
							.append (")")
//						    .append (hasExifDates ? "*" : "");
						    .append (image.hasExifDate() ? "*" : "");

				} else { //mode == AlbumMode.DoDir
					if (anyDupsInSlice) {
						//set image line style, thickness, and color to distinguish between dups and non-dups
						boolean isDup = findInDuplicatesCache(_duplicatesCache, image.getName());
						fontColor = (isDup ? "black" : "blue");
						imageBorderStyleStr = (isDup ? "solid" : "dashed");
						imageBorderPixels = isDup ? 1 : 2;
						imageBorderColorStr = (isDup ? "black" : "blue");
						imageOutlineStr = (isDup ? "" : "; outline: " + imageOutlinePixels + "px dotted blue");
					}

					//drill down to single image
					imageName = image.getName ();
					details.append ("(")
							.append (image.getScaleString ())
							.append (")");

					if (cols <= 2) {
						details.append (" (")
								.append (imageCount + 1)
								.append (" of ")
								.append (numImages)
								.append (")");

					}
//					details.append (hasExifDates ? "*" : "");
					details.append (image.hasExifDate() ? "*" : "");

					imagesInSlice.add (image);

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

						String filters = imageName + "," + imageName;
						href = generateImageLink (filters, filters, newMode, newCols, sinceDays, false, false);
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
						.append (details)
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

				boolean addedBreak = false;

				//conditionally add AddToFilters checkbox
				if (mode == AlbumMode.DoSampler && !collapseGroups) {
					String addParamStr = AlbumFormInfo._AddToFiltersParam + image.getBaseName(false);

					sb.append (_break).append (NL)
//						.append ("Add to Filters<INPUT TYPE=\"CHECKBOX\" NAME=\"")
						.append ("Add<INPUT TYPE=\"CHECKBOX\" NAME=\"")
						.append (addParamStr)
//don't close form here .append ("\"></FORM>").append (NL)
						.append ("\"")
//						.append (selectThisDuplicateImage ? " CHECKED" : "")
						.append (">").append (NL);
					addedBreak = true;
				}

				//conditionally add Delete checkbox
				if (mode == AlbumMode.DoDir || mode == AlbumMode.DoDup || (mode == AlbumMode.DoSampler && !collapseGroups)) {
					String deleteParamStr = (mode == AlbumMode.DoDir || mode == AlbumMode.DoDup) ? AlbumFormInfo._DeleteParam1 + image.getNameWithExt () //single filename
																								 : AlbumFormInfo._DeleteParam2 + image.getBaseName(collapseGroups) + "-*" + AlbumFormInfo._ImageExtension; //wildname

					sb.append (addedBreak ? _spacing : _break).append (NL)
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
				.append ("</TABLE>").append (NL);

		String htmlString = sb.toString ();

		//replace tagsMarker with any tags
		htmlString = htmlString.replace (tagsMarker, getTagsString (imagesInSlice, collapseGroupsForTags));

		//replace servletErrorsMarker with any servlet errors
		htmlString = htmlString.replace (servletErrorsMarker, getServletErrorsHtml ());

		if (numMatchesFilter == imagesInSlice.size()) {
			htmlString = htmlString.replace ("; outline: 2px dashed fuchsia", ""); //HACK - hardcoded string from above
		}

		if (_Debug) {
			_log.debug ("AlbumImages.generateHtml: imageCount = " + imageCount);
		}

		AlbumProfiling.getInstance ().exit (1);

		return htmlString;
	}

	///////////////////////////////////////////////////////////////////////////
	private String getTagsString (Set<AlbumImage> imagesInSlice, boolean collapseGroupsForTags)
	{
		StringBuilder sb = new StringBuilder (64);

		Set<String> imageNamesInSlice = imagesInSlice.stream()
//													 .map(i -> i.getBaseName(false))
													 .map(i -> i.getBaseName(collapseGroupsForTags))
													 .collect(Collectors.toSet());

		List<String> tags = AlbumTags.getInstance ().getTagsForBaseNames (imageNamesInSlice, collapseGroupsForTags);
		final int numTagsOriginal = tags.size();
		if (numTagsOriginal > 0) {
			final int maxTagsToShow = 50; //TODO - hardcoded
			if (numTagsOriginal > maxTagsToShow) {
				tags = tags.subList(0, maxTagsToShow);
				tags.add("...");
			}

			sb.append (" (")
				.append (numTagsOriginal)
				.append (" tags: ")
				.append (String.join(", ", tags))
				.append (")");
		}

		return sb.toString ();
	}

	///////////////////////////////////////////////////////////////////////////
	private String getServletErrorsHtml ()
	{
		final String bgErrorColor   = "#FF0000"; //red background
		final String bgInfoColor    = "#F0F0F0"; //white background
		final String bgWarnColor    = "#E0E000"; //yellow background
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
	public List<String> getMatchingAlbumsForFilters (final String[] filtersArray, final boolean collapseGroups, final long sinceInMillis) {
		return getMatchingAlbumsForFilters (Arrays.asList (filtersArray), collapseGroups, sinceInMillis);
	}
	///////////////////////////////////////////////////////////////////////////
	public List<String> getMatchingAlbumsForFilters (final List<String> filters1, final boolean collapseGroups, final long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enter (5);

		List<String> filters = VendoUtils.caseInsensitiveSortAndDedup (filters1);
//TODO - move this to DAO (as query)??
//TODO - this probably does not handle wildcards (e.g., '+')
//TODO - does not work for "*"
//TODO - does not work for "i" for example!!!!

		List<String> allMatchingAlbums = new ArrayList<>();

//TODO - don't call getImagesFromCache() more than once if more that one filter uses the same subFolder

		for (String filter : filters) {
//			Pattern filterPattern = Pattern.compile ("^" + (filter + "*").replaceAll ("\\*", ".*"), Pattern.CASE_INSENSITIVE); //convert to regex before compiling
			String subFolder = AlbumImageDao.getInstance().getSubFolderFromImageName(filter);
			if (subFolder == null) {
				continue;
			}

			Collection<AlbumImage> allImagesForSubFolder = AlbumImageDao.getInstance().getImagesFromCache(subFolder, null);

			allMatchingAlbums.addAll(allImagesForSubFolder.stream()
														.map(i -> i.getBaseName(collapseGroups))
//														.filter(n -> n.toLowerCase().startsWith(filter.toLowerCase()))
//														.filter(filterPattern.asPredicate())
														.filter(filter::equalsIgnoreCase)
														.sorted(_alphanumComparator)
														.distinct()
														.collect(Collectors.toList()));
		}

		if (filters.size() > 1) { //re-sort
			allMatchingAlbums = allMatchingAlbums.stream()
													.sorted(_alphanumComparator)
													.distinct()
													.collect(Collectors.toList());
		}

		AlbumProfiling.getInstance ().exit (5);

		return allMatchingAlbums;
	}

//not used, too slow - use AlbumImageDao#getAlbumHasExifData (?)
	///////////////////////////////////////////////////////////////////////////
//	public List<AlbumImage> getMatchingImagesForImage (final AlbumImage image, final boolean collapseGroups, final long sinceInMillis)
//	{
//		AlbumProfiling.getInstance ().enter (5);
//
//		String imageBaseName = image.getBaseName (collapseGroups);
//		String subFolder = AlbumImageDao.getInstance().getSubFolderFromImageName(imageBaseName);
//
//		Collection<AlbumImage> allImagesForSubFolder = AlbumImageDao.getInstance().getImagesFromCache(subFolder, null);
//
//		List<AlbumImage> allMatchingImages = allImagesForSubFolder.stream()
//				.filter(i -> i.equalBase(image, collapseGroups))
//				.collect(Collectors.toList());
//
//		AlbumProfiling.getInstance ().exit (5);
//
//		return allMatchingImages;
//	}

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

		deleteFileIgnoreException (moveFile);

		if (form.getMode () != AlbumMode.DoDir || form.getSortType () != AlbumSortType.ByExif || !hasOneFilter || numImages == 0) {

//TODO - add case for each error, instead of just general "not applicable"

			String error = "AlbumImages.generateExifSortCommands: aborting: not applicable";
			generateDuplicateImageRenameError (moveFile, error);
			return;
		}

		String baseNameDest = form.getFilters (1)[0]; //only uses the first filter
		String dash = (baseNameDest.contains ("-") ? "" : "-"); //add dash unless already there
		Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), AlbumImageDao.getInstance ().getSubFolderFromImageName (baseNameDest));
		final String whiteList = "[0-9A-Za-z\\-_]"; //regex - all valid characters for baseNames (disallow wildcards)

		//skip if list is already sorted by name
		List<AlbumImage> byName = new ArrayList<> (_imageDisplayList);
		byName.sort(new AlbumImageComparator(AlbumSortType.ByName));
		if (byName.equals (_imageDisplayList)) {
			_log.debug ("AlbumImages.generateExifSortCommands: image list is already sorted" + NL);

		} else if (baseNameDest.replaceAll (whiteList, "").length () > 0) {
			_log.debug ("AlbumImages.generateExifSortCommands: wildcards not allowed" + NL);

		} else {
			int index = 0;
			sb.append ("setlocal").append (NL);
			sb.append ("cd /d ").append (imagePath).append (NL);
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
					_log.debug ("AlbumImages.generateExifSortCommands: move commands written to file: " + moveFile + NL);
				}

			} catch (IOException ee) {
				_log.error ("AlbumImages.generateExifSortCommands: error writing output file: " + moveFile + NL);
				_log.error (ee); //print exception, but no stack trace
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//creates .BAT file with echo of error message
	public void generateDuplicateImageRenameError (Path moveFile, String error) {
//		_log.debug ("AlbumImages.generateDuplicateImageRenameError");

		if (moveFile.toFile ().length () > 0 || error.length () > 0) {
			try (FileOutputStream outputStream = new FileOutputStream (moveFile.toFile ())) {
				outputStream.write (("echo Error: " + error).getBytes());
				outputStream.flush ();
				outputStream.close ();
				if (error.length () > 0) {
					_log.error ("AlbumImages.generateDuplicateImageRenameError: error written to file: " + moveFile);
					_log.error ("AlbumImages.generateDuplicateImageRenameError: " + error + NL);
				}

			} catch (IOException ee) {
				_log.error ("AlbumImages.generateDuplicateImageRenameError: error writing output file: " + moveFile);
				_log.error (ee); //print exception, but no stack trace
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	//creates .BAT file with move commands
	public void generateDuplicateImageRenameCommands ()
	{
//		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands");

		StringBuilder sb = new StringBuilder(2048);

		int numImages = _imageDisplayList.size ();
		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		Path rootPath = FileSystems.getDefault ().getPath (AlbumFormInfo.getInstance ().getRootPath (false));
		Path moveFile = FileSystems.getDefault ().getPath (rootPath.toString (), "moveRenameGeneratedFile.bat");
		String[] filters = form.getFilters ();

		deleteFileIgnoreException (moveFile);

		if (form.getMode () != AlbumMode.DoDup || !form.getLooseCompare() /*|| !form.getIgnoreBytes()*/ || numImages == 0 || numImages > 1000 || form.filtersHaveWildCards()) {

//TODO - add case for each error, instead of just general "not applicable"

			String error = "AlbumImages.generateDuplicateImageRenameCommands: aborting: not applicable";
			generateDuplicateImageRenameError (moveFile, error);
			return;
		}

		//Note: getMatchingAlbumsForFilters should drop any filters that do not match albums exactly. For example, Foo does not match Foo01 and would be dropped
		List<String> albumsMatchingFilters = getMatchingAlbumsForFilters (filters, false, 0);
//TODO - print filters that were dropped because they do not match albums??

		if (albumsMatchingFilters.size() == 1) {
			String error = "AlbumImages.generateDuplicateImageRenameCommands: aborting: only one album; nothing to do";
			generateDuplicateImageRenameError (moveFile, error);
			return;

		} else if (albumsMatchingFilters.size() > 25) {
			String error = "AlbumImages.generateDuplicateImageRenameCommands: aborting: too many matching albums (" + albumsMatchingFilters.size () + ") for filters: " +
									VendoUtils.caseInsensitiveSortAndDedup (Arrays.asList (filters));
			generateDuplicateImageRenameError (moveFile, error);
			return;
		}

		List<AlbumImageDuplicateDetails> dups = new ArrayList<> ();
		for (String album : albumsMatchingFilters) {
			List<AlbumImageDuplicateDetails> dupDetails = AlbumImageDuplicateDetails.splitMultipleSubAlbums (album, _imageDisplayList);
			if (dupDetails == null) {
				String error = "AlbumImages.generateDuplicateImageRenameCommands: aborting: dupDetails = null for album = " + album;
				generateDuplicateImageRenameError (moveFile, error);
				return;
			}
			dups.addAll (dupDetails);
		}

		if (dups.isEmpty()) {
			String error = "AlbumImages.generateDuplicateImageRenameCommands: aborting: no dups found";
			generateDuplicateImageRenameError (moveFile, error);
			return;
		}

		for (AlbumImageDuplicateDetails dup : dups) {
			dup.init(_imageDisplayList);
		}

		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands");

		//sort by the specified criteria
		List<AlbumImageDuplicateDetails> sorted = dups.stream ().sorted ().collect (Collectors.toList ());

		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands: after sort --------------------");
		for (AlbumImageDuplicateDetails item : sorted) {
			_log.debug(item);
		}

		List<String> baseNamesDistinct = dups.stream()
											 .map(i -> i._imageBaseName)
											 .distinct()
											 .sorted(new AlphanumComparator()) //sort numerically
											 .collect(Collectors.toList());

		AlbumImageDuplicateDetails firstItem = sorted.get(0);

/*
		AlbumImageDuplicateDetails destinationItem = dups.stream()
													.sorted((i1, i2) -> i1._imageBaseName.compareToIgnoreCase (i2._imageBaseName)) //TODO - should we use AlphanumComparator here??
													.collect(Collectors.toList())
													.get(0);
*/
		AlbumImageDuplicateDetails destinationItem = dups.stream()
													.sorted(new AlphanumComparator()) //sort numerically by toString result
//													.sorted((i1, i2) -> i1._imageBaseName.compareToIgnoreCase (i2._imageBaseName)) //TODO - should we use AlphanumComparator here??
													.collect(Collectors.toList())
													.get(0);

//		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands: firstItem: " + firstItem);
//		_log.debug ("AlbumImages.generateDuplicateImageRenameCommands: destinationItem: " + destinationItem);

		String firstFilter = firstItem._filter;
		String destinationFilter = destinationItem._filter;

		String firstBaseName = firstItem._imageBaseName;
		String destinationBaseName = destinationItem._imageBaseName;

		String needsCleanupDigits = firstItem._needsCleanupDigits;

//temp debug
boolean b1 = destinationBaseName.equalsIgnoreCase(destinationFilter); //hack - in this case we end up with extra "1" (or "2") on numbering
boolean b2 = firstBaseName.equalsIgnoreCase(firstFilter);
boolean b3 = destinationBaseName.equalsIgnoreCase(firstBaseName);

//		boolean needsCleanup = !destinationBaseName.equalsIgnoreCase(destinationFilter) && //hack - in this case we end up with extra "1" (or "2") on numbering
//							   !firstBaseName.equalsIgnoreCase(firstFilter) &&
//							   destinationBaseName.equalsIgnoreCase(firstBaseName);
//		needsCleanup |= needsCleanupDigits != null;
		boolean needsCleanup = needsCleanupDigits != null;

		boolean browserNeedsRefresh = !destinationBaseName.equalsIgnoreCase(destinationFilter);

		//handle situation where we have both 2- and 3-digit albums (prefer, for example, Foo28 over Foo101)
		boolean mismatchedAlbumDigits = AlbumImageDuplicateDetails.doesNumberOfDigitsInBaseNamesMatch(dups);

		sb.append ("REM auto-generated").append (NL);
		sb.append ("setlocal").append (NL);
		sb.append ("set REVERSE=0").append (NL);
		sb.append ("if %1@==r@ set REVERSE=1").append (NL);
		sb.append ("touch ").append(AlbumImageDao.getInstance().getPauseFilename()).append (NL);

		int index = 1;
		String indexStringFormat = (sorted.size() > 9 ? "%02d" : "%d");
		Path currentFolder =  FileSystems.getDefault ().getPath (rootPath.toString ());
		for (AlbumImageDuplicateDetails item : sorted) {
			Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), item._firstMatchingImage.getSubFolder ());
			if (currentFolder.compareTo(imagePath) != 0) {
				sb.append("cd /d ").append(imagePath).append(NL);
				currentFolder = imagePath;
			}

			String indexString = String.format(indexStringFormat, index);
			String sourceFileNameWild = appendDash(item._filter) + "*.jpg";
			String destinationFileNameWild = appendDash(firstFilter) + indexString + "*.jpg";
			sb.append("mov ").append(sourceFileNameWild).append(" ").append(destinationFileNameWild).append(NL);

			if (!firstItem._subFolder.equalsIgnoreCase(item._subFolder)) {
//				sb.append ("drSleep ").append(sleepInSeconds).append (NL);
				sb.append("move ").append(destinationFileNameWild).append(" ..\\").append(firstItem._subFolder).append("\\").append(NL);
			}

			index++;
		}

		double sleepInSeconds = 0.1; //(numTotalImages >= 40 ? 1.0 : 0.5);

		sb.append("REM at 01").append (NL);

		if (!firstBaseName.equalsIgnoreCase(destinationBaseName)) { // || mismatchedAlbumDigits) {
			String srcSubFolderStr = AlbumImageDao.getInstance ().getSubFolderFromImageName (firstBaseName);
			String destSubFolderStr = AlbumImageDao.getInstance ().getSubFolderFromImageName (destinationBaseName);

//			browserNeedsRefresh = true;
			sb.append("drSleep ").append(sleepInSeconds).append (NL);
//			sb.append("call mx.bat ").append(destinationBaseName).append(" ").append(firstBaseName).append(NL);
			sb.append("cd ..\\").append(srcSubFolderStr).append("\\").append(NL);
			sb.append("mov ").append(firstBaseName).append("-*.jpg").append(" ").append(destinationBaseName).append("-*.jpg").append(NL);

			if (!srcSubFolderStr.equals(destSubFolderStr)) {
				sb.append("drSleep ").append(sleepInSeconds).append (NL);
				sb.append("cd ..\\").append(srcSubFolderStr).append("\\").append(NL);
				sb.append("move ..\\").append(srcSubFolderStr).append("\\").append(destinationBaseName).append("*").append(" ..\\").append(destSubFolderStr).append("\\").append(NL);
			}
		}

		sb.append("REM at 02").append (NL);

		if (!baseNamesDistinct.get(0).equalsIgnoreCase(destinationBaseName) && mismatchedAlbumDigits) {
//			browserNeedsRefresh = true;
			sb.append("drSleep ").append(sleepInSeconds).append (NL);
			sb.append("mov ").append(destinationBaseName).append("-*.jpg").append(" ").append(baseNamesDistinct.get(0)).append("-*.jpg").append(NL);
		}

		sb.append("REM at 03").append (NL);

		if (needsCleanup) { //hack - remove extra "1"
			browserNeedsRefresh = true;
			sb.append ("drSleep ").append(sleepInSeconds).append (NL);
//			Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), item._firstMatchingImage.getSubFolder ());
			Path imagePath = FileSystems.getDefault ().getPath (rootPath.toString (), destinationItem._firstMatchingImage.getSubFolder ());
			sb.append ("cd /d ").append (imagePath).append (NL);
//			sb.append("mov ").append(destinationBaseName).append("-1*.jpg").append(" ").append(destinationBaseName).append("-*.jpg").append(NL); //TODO - "-1" could potentially be any digit
			sb.append("mov ").append(destinationBaseName).append("-").append(needsCleanupDigits).append("*.jpg").append(" ").append(destinationBaseName).append("-*.jpg").append(NL); //TODO - "-1" could potentially be any digit
		}

		sb.append("REM at 04").append (NL);

//TODO this does not work quite right - it will write one row for each pair
		String optionalCommentForNow = (baseNamesDistinct.size() > 2 ? "REM " : "");
		if (baseNamesDistinct.size () <= 2) {
			for (String baseName : baseNamesDistinct) {
				if (!destinationBaseName.equalsIgnoreCase(baseName)) {
					Path imagePathSrc = FileSystems.getDefault ().getPath (rootPath.toString (), AlbumImageDao.getInstance ().getSubFolderFromImageName (baseName));
					Path imagePathDest = FileSystems.getDefault ().getPath (rootPath.toString (), AlbumImageDao.getInstance ().getSubFolderFromImageName (destinationBaseName));

//					sb.append(optionalCommentForNow).append("if %REVERSE%==1 mov ").append(destinationBaseName).append("*.jpg").append(" ").append(baseName).append("*.jpg").append(NL);
					sb.append(optionalCommentForNow).append("if %REVERSE%==1 (").append(NL);
					sb.append(optionalCommentForNow).append(" drSleep ").append(sleepInSeconds).append (NL);
					sb.append(optionalCommentForNow).append(" cd /d ").append (imagePathDest).append (NL);
					sb.append(optionalCommentForNow).append(" mov ").append(destinationBaseName).append("*.jpg").append(" ").append(baseName).append("*.jpg").append(NL);

					if (!imagePathSrc.equals(imagePathDest)) {
						sb.append(optionalCommentForNow).append(" drSleep ").append(sleepInSeconds).append (NL);
//						sb.append(optionalCommentForNow).append(" cd /d ").append (imagePathDest.toString ()).append (NL);
						sb.append(optionalCommentForNow).append(" move ").append(baseName).append("*.jpg").append(" ").append(imagePathSrc).append("\\").append(NL);
					}
					sb.append(optionalCommentForNow).append(")").append(NL);
				}
			}
		}

//		sb.append("REM call mx.bat ").append(destinationBaseName).append(" ").append(firstBaseName).append(NL);
//		sb.append("REM call mx.bat ").append(firstBaseName).append(" ").append(destinationBaseName).append(NL);

//TODO - can we force this?, i.e., make it automatic?
		if (browserNeedsRefresh) {
			sb.append("echo ******** BROWSER NEEDS REFRESH ********").append(NL);
		}

		sb.append ("del ").append(AlbumImageDao.getInstance().getPauseFilename()).append (NL);

		if (moveFile.toFile ().length () > 0 || sb.length () > 0) {
			try (FileOutputStream outputStream = new FileOutputStream (moveFile.toFile ())) {
				outputStream.write (sb.toString ().getBytes ());
				outputStream.flush ();
				outputStream.close ();
				if (sb.length () > 0) {
					_log.debug ("AlbumImages.generateDuplicateImageRenameCommands: move commands written to file: " + moveFile + NL + sb);
				}

			} catch (IOException ee) {
				_log.error ("AlbumImages.generateDuplicateImageRenameCommands: error writing output file: " + moveFile + NL);
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

	///////////////////////////////////////////////////////////////////////////
	public static String appendDash (String string)
	{
		return string + (string.contains("-") ? "" : "-");
	}


/*TODO - fix this (only used by AlbumFileFilter to implement sinceInMillis)
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
			Set<Long> uniqueDates = new HashSet<> ();
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
		if (clearAll) {
			_nameScaledImageCache = null;
			_looseCompareDataCache = null;
		}

		final int expireAfterAccess = 24; //hours
		if (_nameScaledImageCache == null) {
			_nameScaledImageCache = CacheBuilder.newBuilder()
					.maximumSize(_nameScaledImageCacheMaxSize)
					.expireAfterAccess(expireAfterAccess, TimeUnit.HOURS)
					.recordStats()
					.removalListener(new RemovalListener<AlbumImage, ByteBuffer>() {
						@Override
						public void onRemoval(@Nonnull RemovalNotification<AlbumImage, ByteBuffer> removal) {
							_nameScaledImageCacheEvicted.add(removal.getKey());
						}
					})
//					.weakKeys()
//					.weakValues()
					.build (new CacheLoader<AlbumImage, ByteBuffer>() {
						@Override
						public ByteBuffer load(@Nonnull AlbumImage image) {
							_nameScaledImageCacheAdded.add(image);
							return image.readScaledImageData();
						}
					});
		}

		if (_looseCompareDataCache == null) {
			_looseCompareDataCache = CacheBuilder.newBuilder()
					.maximumSize(_looseCompareDataCacheMaxSize)
					.expireAfterAccess(expireAfterAccess, TimeUnit.HOURS)
					.recordStats()
					.removalListener(new RemovalListener<AlbumImagePair, AlbumImagePair>() {
						@Override
						public void onRemoval(@Nonnull RemovalNotification<AlbumImagePair, AlbumImagePair> removal) {
							_looseCompareDataCacheEvicted.add(removal.getKey());
						}
					})
//					.weakKeys()
//					.weakValues()
					.build (new CacheLoader<AlbumImagePair, AlbumImagePair>() {
						@Override
						public AlbumImagePair load(@Nonnull AlbumImagePair imagePair) {
							_looseCompareDataCacheAdded.add(imagePair);

							AlbumImage image1 = imagePair.getImage1();
							AlbumImage image2 = imagePair.getImage2();

							//the expectation is that this scaled image data was already preloaded into the cache by earlier code
							ByteBuffer scaledImage1Data = null;
							try {
								scaledImage1Data = _nameScaledImageCache.get(image1);
							} catch (ExecutionException ee) {
								_log.error("AlbumImages.CacheLoader.load: _nameScaledImageCache.get failed on: " + image1.getName(), ee);
							}

							//the expectation is that this scaled image data was already preloaded into the cache by earlier code
							ByteBuffer scaledImage2Data = null;
							try {
								scaledImage2Data = _nameScaledImageCache.get(image2);
							} catch (ExecutionException ee) {
								_log.error("AlbumImages.CacheLoader.load: _nameScaledImageCache.get failed on: " + image2.getName(), ee);
							}

							VPair<Integer, Integer> diffPair = AlbumImage.getScaledImageDiff (scaledImage1Data, scaledImage2Data);
							int averageDiff = diffPair.getFirst ();
							int stdDev = diffPair.getSecond ();
							AlbumImagePair newPair = new AlbumImagePair (image1, image2, averageDiff, stdDev, AlbumImageDiffer.Mode.OnDemand.name (), null);
							return newPair;
						}
					});
		}

		if (clearAll) {
			_nameOfExactDuplicateThatCanBeDeleted = new HashMap<>();
			_nameOfNearDuplicateThatCanBeDeleted = new HashMap<>();
			_duplicatesCache = new HashMap<>();
		}

		long totalMem = Runtime.getRuntime ().totalMemory ();
		long maxMem   = Runtime.getRuntime ().maxMemory ();
		double memoryUsedPercent = 100 * (double) totalMem / maxMem;
		_log.debug ("AlbumImages.cacheMaintenance: memoryUsed: " + _decimalFormat1.format (memoryUsedPercent) + "%");
	}

	///////////////////////////////////////////////////////////////////////////
	private static <T, V> int cacheMaintenance (Map<T, V> map, int maxSize) //hack
	{
		int numItemsRemoved = 0;

		if (map.size () > maxSize) {
			AlbumProfiling.getInstance ().enter (5);

			int newSize = (19 * maxSize) / 20;
			numItemsRemoved = map.size () - newSize;

			Iterator<T> iter = map.keySet ().iterator (); //hack - this just randomly removes entries; should use LRU
			for (int ii = 0; ii < numItemsRemoved; ii++) {
				iter.next ();
				iter.remove ();
			}

			AlbumProfiling.getInstance ().exit (5);
		}

		return numItemsRemoved;
	}

	///////////////////////////////////////////////////////////////////////////
	//called by AlbumImageDao.getImagesFromCache
	public static void duplicatesCacheMaintenance (String subFolder)
	{
		Set<String> exactDupsSet = _nameOfExactDuplicateThatCanBeDeleted.get (subFolder);
		if (exactDupsSet != null) {
			exactDupsSet.clear();
		}
		Set<String> nearDupsSet = _nameOfNearDuplicateThatCanBeDeleted.get (subFolder);
		if (nearDupsSet != null) {
			nearDupsSet.clear();
		}
//		Set<String> allDupsSet = _duplicatesCache.get (subFolder);
//		if (allDupsSet != null) {
//			allDupsSet.clear();
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static void addToDuplicatesCache (Map<String, Set<String>> duplicatesCache, String imageName)
	{
		String subFolder = AlbumImageDao.getInstance().getSubFolderFromImageName(imageName);
		Set<String> dupSet = duplicatesCache.computeIfAbsent(subFolder, k -> new HashSet<>());
		dupSet.add (imageName);
	}

	///////////////////////////////////////////////////////////////////////////
	public static boolean findInDuplicatesCache (Map<String, Set<String>> duplicatesCache, String imageName)
	{
		String subFolder = AlbumImageDao.getInstance().getSubFolderFromImageName(imageName);
		Set<String> dupSet = duplicatesCache.get(subFolder);
		return dupSet != null && dupSet.contains(imageName);
	}

	///////////////////////////////////////////////////////////////////////////
	private static class ImageDifferTask implements Runnable
	{
		///////////////////////////////////////////////////////////////////////////
		public ImageDifferTask (BlockingQueue<AlbumImagePair> queue,
								final int maxStdDev,
								final Set<AlbumImagePair> pairsReady,
								final Set<AlbumImageDiffDetails> toBeAddedToImageDiffsTable,
								final AtomicBoolean shutdownFlag,
								final AlbumFormInfo form)
		{
			_queue = queue;
			_maxStdDev = maxStdDev;
			_pairsReady = pairsReady;
			_toBeAddedToImageDiffsTable = toBeAddedToImageDiffsTable;
			_shutdownFlag = shutdownFlag;
			_form = form;
		}

		///////////////////////////////////////////////////////////////////////////
		@Override
		public void run ()
		{
			try {
				boolean done = false;
				while (!done) {
					if (_shutdownFlag.get()) {
						String message = "image differ aborted in run()";
						_log.warn("ImageDifferTask.run: " + message + " ********************************************************");
						_form.addServletError("Warning: " + message);

						try {
							while (_queue.poll(1, TimeUnit.MILLISECONDS) != null) {
								//try to empty queue
							}
						} catch (InterruptedException ex) {
							_log.error ("ImageDifferTask.run: queue.poll failed");
							_log.error (ex);
						}

						done = true;

					} else {
						AlbumImagePair pair = _queue.take(); //will block if queue is empty

						if (pair == DONE_MARKER) {
							_queue.put(DONE_MARKER); //put done marker back in queue for other threads
							done = true;

						} else {
							diffImages(pair);
						}
					}
				}

			} catch (InterruptedException ex) {
				ex.printStackTrace ();
			}
		}

		///////////////////////////////////////////////////////////////////////////
		public void diffImages(AlbumImagePair inPair) {
			final AlbumImage image1 = inPair.getImage1();
			final AlbumImage image2 = inPair.getImage2();

			if (_shutdownFlag.get()) {
				String message = "image differ aborted in diffImages())";
				_log.warn("ImageDifferTask.diffImages: " + message + " ********************************************************");
				_form.addServletError("Warning: " + message);

				return;
			}

			//preload the scaled image data before calling _looseCompareDataCache.get below
			ByteBuffer scaledImage1Data = null;
			try {
				scaledImage1Data = _nameScaledImageCache.get(image1);
			} catch (ExecutionException ee) {
				_log.error("AlbumImages.diffImages: _nameScaledImageCache.get failed on: " + image1.getName(), ee);
			}

			//preload the scaled image data before calling _looseCompareDataCache.get below
			ByteBuffer scaledImage2Data = null;
			try {
				scaledImage2Data = _nameScaledImageCache.get(image2);
			} catch (ExecutionException ee) {
				_log.error("AlbumImages.diffImages: _nameScaledImageCache.get failed on: " + image2.getName(), ee);
			}

			AlbumImagePair stubPair = new AlbumImagePair (image1, image2);
			AlbumImagePair diffPair = null;
			try {
				diffPair = _looseCompareDataCache.get(stubPair);
			} catch (ExecutionException ee) {
				_log.error("AlbumImages.diffImages: _looseCompareDataCache.get failed on: " + stubPair, ee);
			}

			int averageDiff = diffPair.getAverageDiff ();
			int stdDev = diffPair.getStdDev ();

			if (AlbumImage.acceptDiff (averageDiff, stdDev, AlbumImageDiffer._maxAverageDiffUsedByBatFiles, AlbumImageDiffer._maxStdDevDiffUsedByBatFiles)) { //hardcoded values from BAT files
//TODO - should we always add here? if so, will update entries to have current timestamp
//				if (!_imageDiffDetailsFromImageDiffs.containsKey(AlbumImageDiffDetails.getJoinedNameIds(image1.getNameId(), image2.getNameId()))) {
					_toBeAddedToImageDiffsTable.add(new AlbumImageDiffDetails(image1.getNameId(), image2.getNameId(), averageDiff, stdDev, 1, AlbumImageDiffer.Mode.OnDemand.name(), new Timestamp (new GregorianCalendar ().getTimeInMillis ())));
//				}
			}

			if (AlbumImage.acceptDiff (averageDiff, stdDev, _maxStdDev - 5, _maxStdDev)) { //hardcoded value - TODO - need separate controls for maxStdDev and maxRgbDiff
				_pairsReady.add (diffPair);
				_log.debug ("AlbumImages.doDup@3: " + diffPair.getDetails3String ());
			}
		}

		final private BlockingQueue<AlbumImagePair> _queue;
		final private int _maxStdDev;
		final private Set<AlbumImagePair> _pairsReady;
		final private Set<AlbumImageDiffDetails> _toBeAddedToImageDiffsTable;
		final private AtomicBoolean _shutdownFlag;
		final private AlbumFormInfo _form;

		public static final AlbumImage _DummyImage = new AlbumImage (1, "", "", 0, 0, 0, 0, "", 0, 0, 0, 0);
		public static final AlbumImagePair DONE_MARKER = new AlbumImagePair(_DummyImage, _DummyImage);
	}

	///////////////////////////////////////////////////////////////////////////
	private Thread watchShutdownFile ()
	{
		Thread watchingThread = null;

		final Path path = FileSystems.getDefault ().getPath (_basePath, _shutdownFilename);

//		if (VendoUtils.fileExists (path)) {
//			System.err.println ("AlbumImages.watchShutdownFile.notify: file already exists: " + path.normalize ().toString ());
//			_shutdownFlag.set (true);
//
//			if (watchingThread != null) {
//				watchingThread.interrupt (); //exit thread
//			}
//
//			return watchingThread;
//		}

		try {
			Path dir = path.getRoot ().resolve (path.getParent ());
			String filename = path.getFileName ().toString ();

			_log.info ("AlbumImages.watchShutdownFile: watching for shutdown file: " + path.normalize ());

			Pattern pattern = Pattern.compile (filename, Pattern.CASE_INSENSITIVE);
			boolean recurseSubdirs = false;

			WatchDir watchDir = new WatchDir (dir, pattern, recurseSubdirs)
			{
				@Override
				protected void notify (Path dir, WatchEvent<Path> pathEvent)
				{
//					if (true || _Debug) {
//						Path file = pathEvent.context ();
//						Path path = dir.resolve (file);
//						_log.debug ("AlbumImages.watchShutdownFile.notify: " + pathEvent.kind ().name () + ": " + path.normalize ());
//					}

					if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_CREATE)) {
						_shutdownFlag.set (true);
//						Thread.currentThread ().interrupt (); //exit thread

					} else if (pathEvent.kind ().equals (StandardWatchEventKinds.ENTRY_DELETE)) {
						_shutdownFlag.set (false);
//						Thread.currentThread ().interrupt (); //exit thread
					}
				}

				@Override
				protected void overflow (WatchEvent<?> event)
				{
					_log.error ("AlbumImages.watchShutdownFile.overflow: received event: " + event.kind ().name () + ", count = " + event.count ());
					_log.error ("AlbumImages.watchShutdownFile.overflow: ", new Exception ("WatchDir overflow"));
				}
			};
			watchingThread = new Thread (watchDir);
			watchingThread.setName ("watchingThread");
			watchingThread.start ();

		} catch (Exception ee) {
			_log.error ("AlbumImages.watchShutdownFile: exception watching shutdown file", ee);
		}

		return watchingThread;
	}


	//members
	private AlbumFormInfo _form = null;

	private Collection<AlbumImage> _imageDisplayList = null; //list of images to display

	private final int _tableBorderPixels = 0; //set to 0 normally, set to 1 for debugging

	private static final AtomicInteger _skippedAt1 = new AtomicInteger (0);
	private static final AtomicInteger _skippedAt2 = new AtomicInteger (0);
	private static final AtomicInteger _skippedAt3 = new AtomicInteger (0);
	private static final AtomicInteger _skippedAt4 = new AtomicInteger (0);
	private static final AtomicInteger _skippedAt5 = new AtomicInteger (0);
	private static final AtomicInteger _skippedAt6 = new AtomicInteger (0);

	private final Thread _shutdownThread;
	private final AtomicBoolean _shutdownFlag = new AtomicBoolean ();
	private static final String _shutdownFilename = "shutdownServletDiffer.txt";

	private static LoadingCache<AlbumImage, ByteBuffer> _nameScaledImageCache = null;
	private static Set<AlbumImage> _nameScaledImageCacheAdded = null;
	private static Set<AlbumImage> _nameScaledImageCacheEvicted = null;

	private static LoadingCache<AlbumImagePair, AlbumImagePair> _looseCompareDataCache = null;
	private static Set<AlbumImagePair> _looseCompareDataCacheAdded = null;
	private static Set<AlbumImagePair> _looseCompareDataCacheEvicted = null;

	public static final int _nameScaledImageCacheMaxSize = 200 * 1024;
	public static final int _looseCompareDataCacheMaxSize = 10 * 1024 * 1024;
//	public static final int _nameScaledImageCacheMaxSize = 256 * 1024;
//	public static final int _looseCompareDataCacheMaxSize = 12 * 1024 * 1024;
	private static Map<String, Set<String>> _nameOfExactDuplicateThatCanBeDeleted = new HashMap<> (); //key=subfolder, value=set of duplicate base names
	private static Map<String, Set<String>> _nameOfNearDuplicateThatCanBeDeleted = new HashMap<> (); //key=subfolder, value=set of duplicate base names
	private static Map<String, Set<String>> _duplicatesCache = new HashMap<> (); //key=subfolder, value=set of duplicate images names

	private static final Set<Long> _previousRequestTimestamps = new HashSet<> ();

	private static final String _break = "<BR>";
	private static final String _spacing = "&nbsp;";

	private static final String NL = System.getProperty ("line.separator");
	private static final FastDateFormat _dateFormat = FastDateFormat.getInstance ("MM/dd/yy HH:mm"); //Note SimpleDateFormat is not thread safe
	private static final DecimalFormat _decimalFormat0 = new DecimalFormat ("###,##0"); //format as integer
	private static final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0.0");
	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0.00");
	private static final DateTimeFormatter _dateTimeFormatter = DateTimeFormatter.ofPattern ("mm'm':ss's'"); //for example: 03m:12s (note this wraps values >= 60 minutes)
	private static final String _basePath = "D:/Netscape/Program/"; //need trailing slash

	private static final AlphanumComparator _alphanumComparator = new AlphanumComparator();

	private static volatile AlbumImages _instance = null;
	private static ExecutorService _executor = null;

	private static final Logger _log = LogManager.getLogger ();
}
