//AlbumImages.java

package com.vendo.albumServlet;

import java.io.File;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.math.util.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vendo.vendoUtils.VPair;
import com.vendo.vendoUtils.VendoUtils;


public class AlbumImages
{
	///////////////////////////////////////////////////////////////////////////
	static
	{
		Thread.setDefaultUncaughtExceptionHandler (new AlbumUncaughtExceptionHandler ());
	}

/*obsolete
	///////////////////////////////////////////////////////////////////////////
	//entry point for "update XML database" feature
	public static void main (String args[])
	{
//TODO - change CLI to read properties file, too

		AlbumFormInfo.getInstance (); //call ctor to load class defaults

		//CLI overrides
//		AlbumFormInfo._Debug = true;
//		AlbumFormInfo._logLevel = 5;
		AlbumFormInfo._profileLevel = 7;

		AlbumProfiling.getInstance ().enter/*AndTrace* (1);

		//process command line args
//		if (AlbumFormInfo._Debug) {
		if (false) {
			for (String arg : args)
				_log.debug ("AlbumImages.main: arg = \"" + arg + "\"");
		}

		//handle '/dir' arg (expects absolute/complete path to the root)
		if (args.length >= 2) {
			if (args[0].equalsIgnoreCase ("/dir")) {
				String rootFolder = processPath (args[1]);
				AlbumFormInfo.getInstance ().setRootFolder (rootFolder);
			}
		}

//		AlbumDirList.getInstance ().setSessionId ("CLI");

		String rootPath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl* false);
		_log.debug ("AlbumImages.main: rootPath = " + rootPath);

		shutdownExecutor ();

		AlbumProfiling.getInstance ().exit (1);

		AlbumProfiling.getInstance ().print (/*showMemoryUsage* true);
	}
*/

	///////////////////////////////////////////////////////////////////////////
	//create singleton instance
	public synchronized static AlbumImages getInstance ()
	{
		String rootPath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ false);

		if (AlbumFormInfo._logLevel >= 8) {
			_log.debug ("AlbumImages.getInstance: rootPath = " + rootPath);
			_log.debug ("AlbumImages.getInstance: _prevRootPath = " + _prevRootPath);
		}

		if (!rootPath.equals (_prevRootPath)) {
			if (!_prevRootPath.equals (""))
				_log.debug ("AlbumImages.getInstance: rootPath has changed to " + rootPath);
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

		final String rootPath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ false);
		if (AlbumFormInfo._logLevel >= 7)
			_log.debug ("AlbumImages ctor: rootPath = " + rootPath);
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
		path = path.replace ('\\', '/');

		if (!path.endsWith ("/"))
			path += '/';

		String parts[] = path.split ("/");

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

		String rootPath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ false);
		Enumeration<String> paramNames = request.getParameterNames ();
		while (paramNames.hasMoreElements ()) {
			String paramName = paramNames.nextElement ();
			if (AlbumFormInfo._logLevel >= 10) {
				_log.debug ("AlbumImages.processParams: got param \"" + paramName + "\"");
			}

			if (paramName.equalsIgnoreCase ("timestamp")) {
				String[] paramValues = request.getParameterValues (paramName);
				currentTimestamp = Long.valueOf (paramValues[0]);
			}

			//delete requests that have same timestamp as previous should be ignored (can happen if user forces browser refresh)
			if (paramName.endsWith (AlbumFormInfo._DeleteSuffix) && _previousRequestTimestamp != currentTimestamp) {
				String[] paramValues = request.getParameterValues (paramName);
				if (paramValues[0].equalsIgnoreCase ("on")) {
					//rename image file on file system
					String newName = rootPath + paramName;
					String origName = newName.substring (0, newName.length () - AlbumFormInfo._DeleteSuffix.length ());
					while (VendoUtils.fileExists (newName)) {
						newName += AlbumFormInfo._DeleteSuffix;
					}

					if (AlbumFormInfo._Debug) {
						_log.debug ("AlbumImages.processParams: renaming \"" + origName + "\"");
					}

					try {
						File newFile = new File (newName);
						File origFile = new File (origName);

						if (!origFile.renameTo (newFile)) {
							_log.error ("AlbumImages.processParams: rename failed (" + origFile.getCanonicalPath () +
																			  " to " + newFile.getCanonicalPath () + ")");
						} else {
							imagesRemoved++;
						}

					} catch (Exception ee) {
						_log.error ("AlbumImages.processParams: error renaming file from \"" + origName + "\" to \"" + newName + "\"", ee);
					}
				}
			}
		}

		if (imagesRemoved > 0) {
			int sleepMillis = 20 + imagesRemoved * 20;
			VendoUtils.sleepMillis (sleepMillis); //hack - try to give AlbumImageDao some time to complete its file processing
			_log.debug ("AlbumImages.processParams: slept " + sleepMillis + " ms");
		}

		_previousRequestTimestamp = currentTimestamp;
	}

	///////////////////////////////////////////////////////////////////////////
	public int processRequest ()
	{
		String[] filters = _form.getFilters ();
		String[] tagsIn = _form.getTags (AlbumTagMode.TagIn);
		String[] tagsOut = _form.getTags (AlbumTagMode.TagOut);
		String[] excludes = _form.getExcludes ();
		int maxFilters = _form.getMaxFilters ();
		long sinceInMillis = _form.getSinceInMillis ();
		boolean tagFilterOperandOr = _form.getTagFilterOperandOr ();
		boolean useCase = _form.getUseCase ();

		cacheMaintenance (_form.getClearCache ()); //hack

//TODO - possible performance enhancement?? if any filter is "*" and operator=AND, set operator to OR

//TODO - need handling for tagsOut ??
		if (tagsIn.length > 0) {
			if (tagFilterOperandOr) { //OR (union) tags and filters
				String[] namesFromTags = AlbumTags.getInstance ().getNamesForTags (useCase, tagsIn, tagsOut).toArray (new String[] {});

				//add any names derived from tags to filters (union)
				if (namesFromTags.length > 0) {
					filters = addArrays (filters, namesFromTags);
				}

			} else { //AND (intersect) tags and filters
				//replace filters (intersection)
				filters = AlbumTags.getInstance ().getNamesForTags (useCase, tagsIn, tagsOut, filters).toArray (new String[] {});
			}
		}

		if (AlbumFormInfo._logLevel >= 5) {
			_log.debug ("AlbumImages.processRequest: filters.length = " + filters.length + " (after adding tags)");
		}

		AlbumMode mode = _form.getMode ();

		if (filters.length > maxFilters) {
			_form.addServletError ("Warning: too many filters (" + _decimalFormat2.format (filters.length) + "), reducing to " + maxFilters);
			filters = Arrays.copyOf (filters, maxFilters);
			_log.debug ("AlbumImages.processRequest: filters.length = " + filters.length + " (after limiting to maxFilters)");
		}

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
	private int doDir (String[] filters, String[] excludes, long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		if (AlbumFormInfo._logLevel >= 5)
			_log.debug ("AlbumImages.doDir: exifDateIndex = " + AlbumFormInfo.getInstance ().getExifDateIndex ());

		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		boolean useCase = form.getUseCase ();

		final AlbumFileFilter filter = new AlbumFileFilter (filters, excludes, useCase, sinceInMillis);

		_imageDisplayList = new LinkedList<AlbumImage> ();

		Collection<String> subFolders = AlbumImageDao.getInstance ().getAlbumSubFolders ();

		final CountDownLatch endGate = new CountDownLatch (subFolders.size ());

		AlbumProfiling.getInstance ().enter (5, "dao.doDir");
		for (final String subFolder : subFolders) {
			new Thread (() -> {
				final Collection<AlbumImage> imageDisplayList = AlbumImageDao.getInstance ().doDir (subFolder, filter);
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

		AlbumProfiling.getInstance ().enter (5, "sort " + _form.getSortType ());
		Collections.sort ((List<AlbumImage>) _imageDisplayList, new AlbumImageComparator (_form));
		AlbumProfiling.getInstance ().exit (5, "sort " + _form.getSortType ());

		if (AlbumFormInfo._logLevel >= 5)
			_log.debug ("AlbumImages.doDir: _imageDisplayList.size = " + _decimalFormat2.format (_imageDisplayList.size ()));

		AlbumProfiling.getInstance ().exit (1);

		//testing exifDate distribution
//		generateExifDateStatistics ();

		return _imageDisplayList.size ();
	}

	///////////////////////////////////////////////////////////////////////////
	private int doDup (String[] filters, String[] excludes, long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		AlbumFormInfo form = AlbumFormInfo.getInstance ();
		boolean collapseGroups = form.getCollapseGroups ();
		boolean limitedCompare = form.getLimitedCompare ();
		boolean dbCompare = form.getDbCompare ();
		boolean looseCompare = form.getLooseCompare ();
		boolean ignoreBytes = form.getIgnoreBytes ();
		boolean useCase = form.getUseCase ();
		boolean reverseSort = form.getReverseSort (); //reverses sense of limitedCompare
		boolean useExifDates = form.getUseExifDates ();
		int exifDateIndex = form.getExifDateIndex ();

		int numImages = 0;
		if (dbCompare) {
			numImages = doDir (new String[] {"*"}, excludes, /*sinceInMillis*/ 0); //ignore sinceInMillis here; it will be honored below
		} else {
			numImages = doDir (filters, excludes, /*sinceInMillis*/ 0); //ignore sinceInMillis here; it will be honored below
		}

		AlbumSortType sortType = AlbumSortType.ByNone;
		if (useExifDates) {
			sortType = AlbumSortType.ByExif;
		} else if (dbCompare) {
			sortType = AlbumSortType.ByNone;
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
//		VComparators.verifyTransitivity (new AlbumImageComparator (sortType), list);

		if (sortType != AlbumSortType.ByNone) {
			AlbumProfiling.getInstance ().enter (5, "sort " + sortType);
			Collections.sort (list, new AlbumImageComparator (sortType));
			AlbumProfiling.getInstance ().exit (5, "sort " + sortType);
		}

		AlbumImage[] images = list.toArray (new AlbumImage[] {});

		ArrayList<AlbumImagePair> dups = new ArrayList<AlbumImagePair> (200);

		String[] filters1 = form.getFilters (/*index*/ 1);
		if (filters1.length == 0) {
			filters1 = new String[] {"*"};
		}
		final AlbumFileFilter filter1 = new AlbumFileFilter (filters1, excludes, useCase, sinceInMillis);

		int maxRgbDiffs = AlbumFormInfo.getInstance ().getMaxRgbDiffs ();
		String maxRgbDiffsStr = String.valueOf (maxRgbDiffs);

		//if looseCompare, determine work to be done
		int maxComparisons = getMaxComparisons (numImages);
		if (looseCompare && !dbCompare) {
			_log.debug ("AlbumImages.doDup: maxComparisons = " + _decimalFormat2.format (maxComparisons) + " (max possible combos)");
			final int maxAllowedComparisons = 80 * 1000 * 1000;
			if (maxComparisons > maxAllowedComparisons) {
				form.addServletError ("Warning: too many comparisons (" + _decimalFormat2.format (maxComparisons) + "), disabling looseCompare");
				looseCompare = false;
			}
		}

		if (looseCompare && !dbCompare) {
			final List<VPair<Integer, Integer>> allCombos = getAllCombos (numImages);

			int maxThreads = 3 * VendoUtils.getLogicalProcessors ();
//			int maxThreads = VendoUtils.getLogicalProcessors () - 1;

			int chunkSize = allCombos.size ();
			if (chunkSize == 0) {
				chunkSize = 1;
			} else if (chunkSize > maxThreads * 20) {
				chunkSize = 1 + allCombos.size () / maxThreads;
			}

			List<List<VPair<Integer, Integer>>> allComboChunks = ListUtils.partition (allCombos, chunkSize);
			int numChunks = allComboChunks.size ();
			_log.debug ("AlbumImages.doDup: numChunks = " + numChunks + ", chunkSize = " + _decimalFormat2.format (chunkSize));

			long maxElapsedSecs1 = 30;
			long endNano1 = System.nanoTime () + maxElapsedSecs1 * 1000 * 1000 * 1000;
			AtomicInteger timeElapsedCount = new AtomicInteger (0);

			final List<AlbumImagePair> pairsReady = Collections.synchronizedList (new ArrayList<AlbumImagePair> (1000));
			final List<AlbumImagePair> pairsWip = Collections.synchronizedList (new ArrayList<AlbumImagePair> (1000));

			{
			AlbumProfiling.getInstance ().enterAndTrace (5, "part 1");

			final CountDownLatch endGate = new CountDownLatch (numChunks);

			for (List<VPair<Integer, Integer>> comboChunk : allComboChunks) {
//				_log.debug ("AlbumImages.doDup: comboChunk.size = " + _decimalFormat2.format (comboChunk.size ()));
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

						if (image1.getOrientation () != image2.getOrientation ()) {
							continue;
						}

						boolean sameBaseName = image1.equalBase (image2, collapseGroups);
						if (!limitedCompare || (limitedCompare && (!reverseSort && !sameBaseName) ||
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

						String joinedNamesPlus = AlbumImagePair.getJoinedNames (image1, image2, maxRgbDiffsStr);
						AlbumImagePair pair = _looseCompareMap.get (joinedNamesPlus);
						if (pair != null) {
							if (pair.getAverageDiff () <= maxRgbDiffs) {
								pairsReady.add (pair);
							}
						} else {
							pairsWip.add (new AlbumImagePair (image1, image2));
						}

						if (System.nanoTime () > endNano1) {
							if (timeElapsedCount.incrementAndGet () == 1) { //only log first occurrence
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

			_log.debug ("AlbumImages.doDup: pairsReady.size = " + _decimalFormat2.format (pairsReady.size ()));
			_log.debug ("AlbumImages.doDup: pairsWip.size = " + _decimalFormat2.format (pairsWip.size ()));

			///////////////////////////////////////////////////////////////////
			//determine set of images to collect scaled image data
			AlbumProfiling.getInstance ().enterAndTrace (5, "load toBeRead");
			Set<AlbumImage> toBeRead = new HashSet<AlbumImage> (pairsWip.size ());
			for (AlbumImagePair pair : pairsWip) {
				AlbumImage image1 = pair.getImage1 ();
				AlbumImage image2 = pair.getImage2 ();
				if (!_nameScaledImageMap.containsKey (image1.getNamePlus ())) {
					toBeRead.add (image1);
				}
				if (!_nameScaledImageMap.containsKey (image2.getNamePlus ())) {
					toBeRead.add (image2);
				}
			}
			AlbumProfiling.getInstance ().exit (5, "load toBeRead");

			{ //block to call readScaledImageData ()
			AlbumProfiling.getInstance ().enterAndTrace (5, "read RGB");
			final CountDownLatch endGate = new CountDownLatch (toBeRead.size ());

			for (final AlbumImage image : toBeRead) {
				Runnable task = () -> {
					ByteBuffer scaledImageData = image.readScaledImageData ();
					_nameScaledImageMap.put (image.getNamePlus (), scaledImageData);
					endGate.countDown ();
				};
				getExecutor ().execute (task);
			}
			try {
				endGate.await ();
			} catch (Exception ee) {
				_log.error ("AlbumImages.doDup: read RGB: endGate:", ee);
			}

			_log.debug ("AlbumImages.doDup: _nameScaledImageMap size = " + _decimalFormat2.format (_nameScaledImageMap.size ()) + " items");
			AlbumProfiling.getInstance ().exit (5, "read RGB");
			}

			{ //block to call nearlyEquals ()
			AlbumProfiling.getInstance ().enterAndTrace (5, "compute diffs");

			final CountDownLatch endGate = new CountDownLatch (pairsWip.size ());

			for (final AlbumImagePair pair : pairsWip) {
				Runnable task = () -> {
					AlbumImage image1 = pair.getImage1 ();
					AlbumImage image2 = pair.getImage2 ();
					ByteBuffer scaledImage1Data = _nameScaledImageMap.get (image1.getNamePlus ());
					ByteBuffer scaledImage2Data = _nameScaledImageMap.get (image2.getNamePlus ());
					int averageDiff = AlbumImage.getScaledImageDiff (scaledImage1Data, scaledImage2Data, maxRgbDiffs);
					pair.setAverageDiff (averageDiff);
					if (averageDiff <= maxRgbDiffs) {
						pairsReady.add (pair);
					}
					String joinedNamesPlus = AlbumImagePair.getJoinedNames (image1, image2, maxRgbDiffsStr);
					_looseCompareMap.put (joinedNamesPlus, pair);

					endGate.countDown ();
				};
				getExecutor ().execute (task);
			}
			try {
				endGate.await ();
			} catch (Exception ee) {
				_log.error ("AlbumImages.doDup: compute diffs: endGate:", ee);
			}

			_log.debug ("AlbumImages.doDup: pairsReady size = " + _decimalFormat2.format (pairsReady.size ()) + " items");

			AlbumProfiling.getInstance ().exit (5, "compute diffs");
			}

			if (timeElapsedCount.get () > 0) {
				String message = "Warning: time elapsed in " + timeElapsedCount.get () + " of " + numChunks + " threads; partial results ************************************************************";
				_log.debug ("AlbumImages.doDup: " + message);
				form.addServletError (message);
			}

			AlbumProfiling.getInstance ().enterAndTrace (5, "dups.add");

			for (AlbumImagePair imagePair : pairsReady) {
				dups.add (imagePair);

				int averageDiff = imagePair.getAverageDiff ();
				_log.debug ("AlbumImages.doDup: " + averageDiff + " " + imagePair.getImage1 ().getName () + " " + imagePair.getImage2 ().getName ());
			}

			AlbumProfiling.getInstance ().exit (5, "dups.add");
		}

		if (!looseCompare || dbCompare) {
			if (dbCompare) {
				AlbumProfiling.getInstance ().enterAndTrace (5, "dups.db");

				//we need a map of imageNames->images
				Map<String, AlbumImage> map = new HashMap<String, AlbumImage> ();
				for (AlbumImage image : _imageDisplayList) {
					map.put (image.getName (), image);
				}

				AlbumImageDiffer albumImageDiffer = AlbumImageDiffer.getInstance ();
				final double sinceDays = _form.getHighlightDays (); //TODO - using highlightDays is a HACK
				Collection<AlbumImageDiffData> imageDiffData = albumImageDiffer.selectNamesFromImageDiffs (_form.getMaxRgbDiffs (), sinceDays);

				for (AlbumImageDiffData item : imageDiffData) {
					AlbumImage image1 = map.get (item.getName1 ());
					AlbumImage image2 = map.get (item.getName2 ());
					if (image1.getOrientation () == image2.getOrientation ()) {
						//at least one of each pair must be accepted by filter1
						if (filter1.accept (null, image1.getName ()) || filter1.accept (null, image2.getName ())) {
							AlbumImagePair pair = new AlbumImagePair (image1, image2, item.getAverageDiff ());
							dups.add (pair);
							String joinedNamesPlus = AlbumImagePair.getJoinedNames (image1, image2, maxRgbDiffsStr);
							_looseCompareMap.put (joinedNamesPlus, pair);
						}
					}
				}

				AlbumProfiling.getInstance ().exit (5, "dups.db");

			} else if (useExifDates) {
				AlbumProfiling.getInstance ().enterAndTrace (5, "dups.exif");

				Set<AlbumImagePair> dupSet = new HashSet<AlbumImagePair> (); //use Set to avoid duplicate AlbumImagePairs

//				for (int ii = 0; ii < AlbumImage.NumFileExifDates; ii++) {
				for (int ii = 0; ii < AlbumImage.NumExifDates; ii++) {
					//get a new list, sorted for each exifDateIndex
					List<AlbumImage> list1 = new ArrayList<AlbumImage> (numImages);
					list1.addAll (_imageDisplayList);
					Collections.sort (list1, new AlbumImageComparator (sortType, ii /*exifDateIndex*/));
					AlbumImage[] images1 = list1.toArray (new AlbumImage[] {});

					AlbumImage prevImage = null;
					for (AlbumImage image : images1) {
						if (image != null && prevImage != null) {
							//at least one of each pair compared must be accepted by filter1 (including name pattern and sinceInMillis)
							if (filter1.accept (null, image.getName ()) || filter1.accept (null, prevImage.getName ())) {
								if (image.equalAttrs (prevImage, looseCompare, ignoreBytes, useExifDates, ii /*exifDateIndex*/)) {
									boolean sameBaseName = image.equalBase (prevImage, collapseGroups);
									if (!limitedCompare || (limitedCompare && (!reverseSort && !sameBaseName) ||
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
								if (!limitedCompare || (limitedCompare && (!reverseSort && !sameBaseName) ||
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

		_log.debug ("AlbumImages.doDup: dups.size = " + _decimalFormat2.format (dups.size ()) + " pairs");

		//log all duplicate sets
		if (dups.size () < 400) {
			AlbumProfiling.getInstance ().enter (5, "dups.sets");

			Set<Set<String>> dupSets = new HashSet<Set<String>> ();
			for (AlbumImagePair pair1 : dups) {
				for (AlbumImagePair pair2 : dups) {
					if (pair1.matchesAtLeastOneImage (pair2)) {
						Set<String> matchingSet = null;
						for (Set<String> set : dupSets) {
							if (pair1.matchesAtLeastOneImage (set) || pair2.matchesAtLeastOneImage (set)) {
								matchingSet = set;
								break;
							}
						}

						if (matchingSet == null) {
							matchingSet = new HashSet<String> ();
							dupSets.add (matchingSet);
						}

						matchingSet.add (pair1.getImage1 ().getBaseName (/*collapseGroups*/ false));
						matchingSet.add (pair1.getImage2 ().getBaseName (/*collapseGroups*/ false));
						matchingSet.add (pair2.getImage1 ().getBaseName (/*collapseGroups*/ false));
						matchingSet.add (pair2.getImage2 ().getBaseName (/*collapseGroups*/ false));
					}
				}
			}
			Set<String> sortedDupSet = new TreeSet<String> (VendoUtils.caseInsensitiveStringComparator);
			for (Set<String> set : dupSets) {
				if (set.size () > 1) {
					List<String> sorted = set.stream ()
//											 .map (v -> v.getBaseName (/*collapseGroups*/ false))
											 .sorted (VendoUtils.caseInsensitiveStringComparator)
											 .collect (Collectors.toList ());
					String str = VendoUtils.arrayToString (sorted.toArray (new String[] {}), ",");
					sortedDupSet.add (str);
				}
			}
			if (sortedDupSet.size () > 0) {
				if (sortedDupSet.size () < 40) {
					for (String string : sortedDupSet) {
						_log.debug ("AlbumImages.doDup: duplicate set: " + string);
					}
				}
				_log.debug ("AlbumImages.doDup: sortedDupSet.size = " + _decimalFormat2.format (sortedDupSet.size ()) + " sets --------------------------------");
			}

			AlbumProfiling.getInstance ().exit (5, "dups.sets");
		}

		if (dbCompare) {
			_imageDisplayList = AlbumImagePair.getImages (dups, AlbumSortType.ByNone); //already sorted by averageDiff by db query
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
	//generate list of all possible combos <- old comment
	public List<VPair<Integer, Integer>> getAllCombos (int numImages)
	{
		AlbumProfiling.getInstance ().enter (5);

		if (numImages > _allCombosNumImages || _allCombos == null) {
			numImages = (int) MathUtils.round ((double) numImages, -3, BigDecimal.ROUND_UP); //round up to next thousand

			_log.debug ("AlbumImages.getAllCombos: cache miss: new numImages = " + _decimalFormat2.format (numImages));

			int maxComparisons = getMaxComparisons (numImages);
			_allCombos = new ArrayList<VPair<Integer, Integer>> (maxComparisons);
			_allCombosNumImages = numImages;

			for (int i0 = 0; i0 < numImages; i0++) {
				for (int i1 = i0 + 1; i1 < numImages; i1++) {
					_allCombos.add (VPair.of (i0, i1));
				}
			}
		}

		AlbumProfiling.getInstance ().exit (5);

		return _allCombos;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumRows ()
	{
		int cols = _form.getColumns ();
		int rows = _form.getPanels () / cols;
		if ((rows * cols) != _form.getPanels ())
			rows++; //handle case where columns is not evenly divisible by panels

		return rows;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumSlices ()
	{
		int numImages = _imageDisplayList.size ();
		if (numImages == 0)
			return 1;

		int cols = _form.getColumns ();
		int rows = getNumRows ();

		int imagesPerSlice = rows * cols;
		int numSlices = numImages / imagesPerSlice;
		if ((numSlices * imagesPerSlice) != numImages)
			numSlices++; //handle case where numImages is not evenly divisible by imagesPerSlice

		return numSlices;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getBgColor ()
	{
		//web color picker: http://www.colorpicker.com/

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
	public String generatePageLinks ()
	{
		final int MaxSliceLinks = 40; //limit number of slice link shown

		int numSlices = getNumSlices ();
		if (numSlices == 1)
			return new String ();

		int currentSlice = _form.getSlice ();

		int firstSlice = 0;
		int lastSlice = numSlices;// - 1;
		if (numSlices < MaxSliceLinks) {
			;//done - defaults set above are correct

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
		if (firstSlice > 1)
			sb.append (generatePageLinksHelper (1, "[First]"));
		if (currentSlice > 1)
			sb.append (generatePageLinksHelper (currentSlice - 1, "[Previous]"));
		if (currentSlice < numSlices)
			sb.append (generatePageLinksHelper (currentSlice + 1, "[Next]"));
		if (lastSlice < numSlices)
			sb.append (generatePageLinksHelper (numSlices, "[Last]"));

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
		  .append ("&maxRgbDiffs=").append (_form.getMaxRgbDiffs ())
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
//		  .append ("&screenWidth=").append (_form.getScreenWidth ())
//		  .append ("&screenHeight=").append (_form.getScreenHeight ())
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
	//handles max of two filters
	public String generateImageLink (String filter1, String filter2, AlbumMode mode, int columns, double sinceDays)
	{
		String server = AlbumFormInfo.getInstance ().getServer ();

		int panels = _form.getPanels ();
		AlbumSortType sortType = _form.getSortType ();

		String filters = filter1 + "," + filter2;

		StringBuilder sb = new StringBuilder (200);
		sb.append ("?mode=").append (mode.getSymbol ())
		  .append ("&filter1=").append (filters)
//TODO - propagate tags?
//TODO - propagate excludes?
		  .append ("&columns=").append (columns);
		if (sortType == AlbumSortType.BySizeBytes || sortType == AlbumSortType.BySizePixels) {
			sb.append ("&sortType=").append (_form.getSortType ().getSymbol ());
		}
		sb.append ("&panels=").append (panels)
		  .append ("&sinceDays=").append (sinceDays)
		  .append ("&maxFilters=").append (_form.getMaxFilters ())
		  .append ("&highlightDays=").append (_form.getHighlightDays ())
		  .append ("&exifDateIndex=").append (_form.getExifDateIndex ())
		  .append ("&maxRgbDiffs=").append (_form.getMaxRgbDiffs ())
		  .append ("&rootFolder=").append (_form.getRootFolder ())
//		  .append ("&tagFilterOperandOr=").append (_form.getTagFilterOperandOr ())
//		  .append ("&collapseGroups=").append (_form.getCollapseGroups ())
//		  .append ("&limitedCompare=").append (_form.getLimitedCompare ())
//		  .append ("&dbCompare=").append (_form.getDbCompare ())
		  .append ("&looseCompare=").append (_form.getLooseCompare ())
		  .append ("&ignoreBytes=").append (_form.getIgnoreBytes ())
		  .append ("&useExifDates=").append (_form.getUseExifDates ())
//		  .append ("&orientation=").append (_form.getOrientation ().getSymbol ())
		  .append ("&useCase=").append (_form.getUseCase ())
//		  .append ("&reverseSort=").append (_form.getReverseSort ())
//		  .append ("&screenWidth=").append (_form.getScreenWidth ())
//		  .append ("&screenHeight=").append (_form.getScreenHeight ())
		  .append ("&windowWidth=").append (_form.getWindowWidth ())
		  .append ("&windowHeight=").append (_form.getWindowHeight ())
		  .append ("&slice=").append (1)
		  .append (AlbumFormInfo._Debug ? "&debug=on" : "")
		  .append ("#topAnchor");

//TODO	return server + encodeUrl (sb.toString ());
		return server + sb.toString ().replaceAll ("\\+", "%2b");
	}

	///////////////////////////////////////////////////////////////////////////
	public String generateTitle ()
	{
		String title = "Album";

		int numImages = _imageDisplayList.size ();
		if (numImages > 0)
			title += " (" + numImages + ")";

		if (_form.getMode () == AlbumMode.DoDup) {
			title += " - " + AlbumMode.DoDup.getSymbol ();

		} else {
			StringBuilder sb = new StringBuilder ();

			String[] filters = _form.getFilters ();
			for (String filter : filters) {
				if (filter.length () > 1 && filter.endsWith ("*")) //strip trailing "*", unless "*" is the entire filter
					filter = filter.substring (0, filter.length () - 1);

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

			if (filterTitle.length () == 0 && numImages > 0)
				filterTitle = "*";

			if (filterTitle.length () > 0)
				title += " - " + filterTitle;
		}

		return title;
	}

	///////////////////////////////////////////////////////////////////////////
	public String generateHtml ()
	{
		AlbumProfiling.getInstance ().enterAndTrace (1);

		int numImages = _imageDisplayList.size ();
		if (AlbumFormInfo._Debug)
			_log.debug ("AlbumImages.generateHtml: numImages = " + numImages);

		boolean isAndroidDevice = _form.isAndroidDevice ();
		boolean isNexus7Device = _form.isNexus7Device ();

		String font1 = "fontsize10";
		if (isAndroidDevice) {
			font1 = "fontsize24";
		}

		String servletErrorsHtml = new String ();
		if (_form.getNumServletErrors () > 0) {
			StringBuilder sb1 = new StringBuilder (200);
			if (numImages > 0) {
				sb1.append ("<TR>").append (NL);
			}

			sb1.append ("<TD class=\"")
			   .append (font1)
			   .append ("\" ALIGN=LEFT>")
			   .append (_form.getServletErrorsHtml ()).append (NL)
			   .append ("</TD>").append (NL);

			if (numImages > 0) {
				sb1.append ("</TR>").append (NL);
			}
			servletErrorsHtml = sb1.toString ();
		}

		if (numImages == 0) {
			StringBuilder sb1 = new StringBuilder (200);
			sb1.append ("<TABLE WIDTH=100% CELLPADDING=0 CELLSPACING=0 BORDER=")
			   .append (_tableBorderPixels)
			   .append (">").append (NL)
			   .append ("<TR>").append (NL)
			   .append (servletErrorsHtml)
			   .append ("<TD class=\"")
			   .append (font1)
			   .append ("\" ALIGN=RIGHT>")
//TODO - this only work for albums that were drilled into??
			   .append ("<A HREF=\"#\" onClick=\"self.close();\">Close</A>").append (NL)
			   .append ("</TD>").append (NL)
			   .append ("</TR>").append (NL)
			   .append ("</TABLE>").append (NL)
			   .append ("No images").append (NL);

			AlbumProfiling.getInstance ().exit (1);
			return sb1.toString ();
		}

		String imageUrlPath = AlbumFormInfo.getInstance ().getRootPath (/*asUrl*/ true);
		String tagMarker = "<tagMarker>";

		AlbumMode mode = _form.getMode ();
		int defaultCols = _form.getDefaultColumns ();
		int cols = _form.getColumns ();
		int rows = getNumRows ();
		int numSlices = getNumSlices ();
		int slice = _form.getSlice ();
		int maxRgbDiffs = _form.getMaxRgbDiffs ();
		String maxRgbDiffsStr = String.valueOf (maxRgbDiffs);
		boolean looseCompare = _form.getLooseCompare ();
		boolean collapseGroups = _form.getCollapseGroups ();
		//for mode = AlbumMode.DoDup, disable collapseGroups for tags
		boolean collapseGroupsForTags = mode == AlbumMode.DoDup ? false : collapseGroups;

		//reduce number of columns when it is greater than number of images
		if (cols > numImages)
			cols = numImages;

		int start = (slice - 1) * (rows * cols);
		int end = start + (rows * cols);

		if (start >= numImages) {
			_log.debug ("AlbumImages.generateHtml: no images in slice; reset slice to 1");

			slice = 1;
			_form.setSlice (slice);
			start = (slice - 1) * (rows * cols);
			end = start + (rows * cols);
		}

		double sinceDays = _form.getSinceDays ();
		long sinceInMillis = _form.getSinceInMillis ();
		String sinceStr = new String ();
		if (sinceInMillis > 0)
			sinceStr = " (since " + _dateFormat.format (new Date (sinceInMillis)) + ")";

		int highlightMinPixels = _form.getHighlightMinPixels ();
		int highlightMaxKilobytes = _form.getHighlightMaxKilobytes ();
		long highlightInMillis = _form.getHighlightInMillis ();
		String highlightStr = new String ();
		if (highlightInMillis > 0)
			highlightStr = " (highlight " + _dateFormat.format (new Date (highlightInMillis)) + ")";

		String pageLinksHtml = generatePageLinks () + _spacing;

		//header, slice links, and close link
		StringBuilder sb = new StringBuilder (8 * 1024);
		sb.append ("<TABLE WIDTH=100% CELLPADDING=0 CELLSPACING=0 BORDER=")
		  .append (_tableBorderPixels)
		  .append (">").append (NL);

		sb.append (servletErrorsHtml);

		sb.append ("<TR>").append (NL)
		  .append ("<TD class=\"")
		  .append (font1)
		  .append ("\" ALIGN=LEFT>")
		  .append ("<NOBR>")
		  .append ("Images ")
		  .append (_decimalFormat2.format (start + 1))
		  .append (" through ")
		  .append (_decimalFormat2.format (Math.min (end, numImages)))
		  .append (" of ")
		  .append (_decimalFormat2.format (numImages))
		  .append (" (")
		  .append (numSlices)
		  .append (numSlices == 1 ? " slice)": " slices)")
		  .append (sinceStr)
		  .append (highlightStr)
		  .append ("</NOBR>")
		  .append (tagMarker)
		  .append ("</TD>").append (NL)
		  .append ("<a name=\"topAnchor\"></a>").append (NL)
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
		String imageBorderColor = "blue";
		String imageBorderStyle = "solid";

		//if more than maxColumns requested, force horizontal scroll bar
		int tempCols = Math.min (cols, _form.getMaxColumns ());

		int tableWidth = 0;
		int imageWidth = 0;
		int imageHeight = 0;

		if (isNexus7Device) {
			tableWidth = (_form.getWindowWidth () > _form.getWindowHeight () ? 1920 : 1200); //hack - hardcode
			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + padding));
			imageHeight = (_form.getWindowWidth () > _form.getWindowHeight () ? 1200 : 1920); //hack - hardcode

		} else {
			//values for firefox
			tableWidth = _form.getWindowWidth () - scrollBarWidth;
			imageWidth = tableWidth / tempCols - (2 * (imageBorderPixels + padding));
//			imageHeight = _form.getWindowHeight () - 40; //for 1080p
			imageHeight = _form.getWindowHeight () - 60; //for 1440p
		}
		_log.debug ("AlbumImages.generateHtml: _form.isNexus7Device () = " + _form.isNexus7Device () + ", tableWidth = " + tableWidth + ", imageWidth = " + imageWidth + ", imageHeight = " + imageHeight);

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
			String averageDiffString = new String ();

			for (int col = 0; col < cols; col++) {
				AlbumImage image = images[start + imageCount];
				image.calculateScaledSize (imageWidth, imageHeight);

				StringBuilder details = new StringBuilder ();

				if (mode == AlbumMode.DoSampler) {
					//if collapseGroups is enabled, disable it in the drilldown when there is only one group
					AlbumMode newMode = AlbumMode.DoDir;

					int newCols = defaultCols;
					if (collapseGroups) {
						if (getNumMatchingImages (image.getBaseName (/*collapseGroups*/ false), sinceInMillis) !=
							getNumMatchingImages (image.getBaseName (/*collapseGroups*/ true), sinceInMillis)) {
							newMode = AlbumMode.DoSampler;
							newCols = _form.getColumns ();
						}
					}

//count: here I actually need to know the number of images that will be shown in the drilldown page...
//like, getNumMatchingGroups()

//TODO - clean this up
					imageName = image.getBaseName (collapseGroups);
					String imageName1 = imageName + (collapseGroups ? "+" : ""); //plus sign here means digit
					href = generateImageLink (imageName1, imageName1, newMode, newCols, sinceDays);
					details.append ("(")
						   .append (getNumMatchingImages (imageName, sinceInMillis))
						   .append (")");

					imagesInSlice.add (imageName);

				} else if (mode == AlbumMode.DoDup) {
					//drill down to dir; always disable collapseGroups
					AlbumMode newMode = AlbumMode.DoDir;

					boolean isEven = (((start + imageCount) & 1) == 0);
					imageName = image.getName ();
					AlbumImage partner = images[start + imageCount + (isEven ? +1 : -1)];

					String imageNonUniqueString = image.getBaseName (/*collapseGroups*/ false);
					String partnerNonUniqueString = partner.getBaseName (/*collapseGroups*/ false);

					imagesInSlice.add (imageNonUniqueString);
					imagesInSlice.add (partnerNonUniqueString);

					//set image border color based on average diff from looseCompareMap
					//for web color names, see http://www.w3schools.com/colors/colors_names.asp
					imageBorderColor = "black";
					if (looseCompare) {
						String joinedNamesPlus = AlbumImagePair.getJoinedNames (image, partner, maxRgbDiffsStr);
						AlbumImagePair pair = _looseCompareMap.get (joinedNamesPlus);
						if (pair != null) {
							Integer cachedDiff = _looseCompareMap.get (joinedNamesPlus).getAverageDiff ();
							averageDiffString = AlbumImage.HtmlNewline + "Average Diff = ";
							if (cachedDiff != null) {
								int averageDiff = cachedDiff.intValue ();
								imageBorderColor = (averageDiff < 1 ? "white" : averageDiff < 10 ? "green" : averageDiff < 20 ? "yellow" : "orange");
								averageDiffString += cachedDiff;
//							} else {
//								imageBorderColor = "red"; //not found in looseCompareMap
//								averageDiffString += "unknown";
							}
						}
					}

					//set image line style to distinguish smaller image
					imageBorderStyle = "solid"; //solid / dotted / dashed
					long pixelDiff = image.getPixels () - partner.getPixels (); //note: does not take orientation into account here
					if (pixelDiff < 0) {
						imageBorderStyle = "dashed";
					}

					//set columns from image/partner that comes first alphabetically
					int imageCols = getNumMatchingImages (imageNonUniqueString, 0);
					int partnerCols = getNumMatchingImages (partnerNonUniqueString, 0);
					int newCols = (imageName.compareToIgnoreCase (partner.getName ()) < 0 ? imageCols : partnerCols);

					//but prevent using columns = 1
					if (newCols == 1)
						newCols = (imageCols + partnerCols) / 2;

					href = generateImageLink (imageNonUniqueString, partnerNonUniqueString, newMode, newCols, /*sinceDays*/0);
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

					imagesInSlice.add (image.getBaseName (/*collapseGroups*/ false));

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

						href = generateImageLink (imageName, imageName, newMode, newCols, sinceDays);
					}
				}

				String imageAlign = "TOP"; //(cols == 1 ? "CENTER" : "RIGHT");
				String textAlign = (cols == 1 ? "CENTER" : "RIGHT");
/*TODO - fix align
				String textAlign = "CENTER";
				if (cols < defaultCols) {
					if (col == cols / 2) {
						textAlign = "CENTER";
					} else if (col < cols / 2) {
						textAlign = "RIGHT";
					} else {
						textAlign = "LEFT";
					}
				}
*/
				String fontColor = "black";
				if (image.getWidth () < highlightMinPixels || image.getHeight () < highlightMinPixels) {
					fontColor = "red";
				}
				if (image.getNumBytes() > highlightMaxKilobytes * 1024) {
					fontColor = "yellow";
				}

				String fontWeight = "normal";
				if (highlightInMillis > 0 && image.getModified () >= highlightInMillis) //0 means disabled
					fontWeight = "bold";

				String fontStyle = "style=\"font-weight:" + fontWeight + ";color:" + fontColor + "\"";

				String imageStyle = "style=\"border:" + imageBorderPixels + "px " + imageBorderStyle + " " + imageBorderColor + "\"";

				sb.append ("<TD class=\"")
				  .append (font2)
				  .append ("\" ")
				  .append (fontStyle)
				  .append (" VALIGN=BOTTOM ALIGN=")
				  .append (textAlign)
				  .append (">").append (NL)
				  .append (imageName)
				  .append (details.toString ())
				  .append (_break).append (NL)

				  .append ("<A HREF=\"")
				  .append (href)
				  .append ("\" ").append (NL)
				  .append ("title=\"")
				  .append (image.toString (/*full*/ true, collapseGroupsForTags) + averageDiffString)
				  .append ("\" target=_blank>").append (NL)
//				  .append ("\" target=view>").append (NL)

				  .append ("<IMG ")
				  .append (imageStyle)
				  .append (" SRC=\"")
				  .append (imageUrlPath)
				  .append (image.getSubFolder ())
				  .append ("/")
				  .append (image.getNameWithExt ())

				  .append ("\" WIDTH=")
				  .append (image.getScaledWidth ())
				  .append (" HEIGHT=")
				  .append (image.getScaledHeight ())
				  .append (" ALIGN=").append (NL)
				  .append (imageAlign)
				  .append (">").append (NL)
				  .append ("</A>").append (NL);

				//conditionally add Delete checkbox
				if (mode == AlbumMode.DoDir || mode == AlbumMode.DoDup) {
					sb.append (_break).append (NL)
					  .append ("<FORM>Delete<INPUT TYPE=\"CHECKBOX\" NAME=\"")
					  .append (image.getSubFolder ())
					  .append ("/")
					  .append (image.getNameWithExt ())
					  .append (AlbumFormInfo._DeleteSuffix)
//don't close form	  .append ("\"></FORM>").append (NL);
					  .append ("\">").append (NL);
				}

				sb.append ("</TD>").append (NL);

				imageCount++;

				if ((start + imageCount) >= numImages)
					break;
			}

			sb.append ("</TR>").append (NL);

			if ((start + imageCount) >= numImages)
				break;
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
		  .append ("<A HREF=\"#AlbumTop\"><NOBR>Top</NOBR></A>").append (NL)
		  .append (_spacing).append (NL)
//TODO - this only work for albums that were drilled into??
		  .append ("<A HREF=\"#\" onClick=\"self.close();\">Close</A>").append (NL)
		  .append ("</TD>").append (NL)
		  .append ("</TR>").append (NL)
//add a blank line at end to avoid problem when overlapping with Firefox's status area
		  .append ("<TR><TD>&nbsp</TD></TR>").append (NL)
		  .append ("</TABLE>").append (NL);

		//replace tagMarker with tags
		String tagStr = AlbumTags.getInstance ().getTagsForBaseNames (imagesInSlice, collapseGroupsForTags);
		if (tagStr.length () != 0) {
			StringBuilder sb1 = new StringBuilder (512);
			sb1.append (" (tags: ")
			   .append (tagStr)
			   .append (")");
			tagStr = sb1.toString ();
		}
		String htmlString = sb.toString ().replace (tagMarker, tagStr);

		if (AlbumFormInfo._Debug)
			_log.debug ("AlbumImages.generateHtml: imageCount = " + imageCount);

		AlbumProfiling.getInstance ().exit (1);

		return htmlString;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getNumMatchingImages (final String baseName, final long sinceInMillis)
	{
		AlbumProfiling.getInstance ().enter (8);

		int num = AlbumImageDao.getInstance ().getNumMatchingImages (baseName, sinceInMillis);

//		_log.debug ("AlbumImages.getNumMatchingImages(\"" + baseName + "\"): " + num);

		AlbumProfiling.getInstance ().exit (8);

		return num;
	}

/*TODO - fix this (only used by AlbumFileFilter to implement sinceInMillis
	///////////////////////////////////////////////////////////////////////////
	public long getImageModified (String name)
	{
		long modified = Long.MAX_VALUE;

		try {
			AlbumXml database = _databaseMap.get (name.substring (0, 1).toLowerCase ());

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
			_log.debug ("AlbumImages.generateExifDateStatistics: [" + ii + "] = " + _decimalFormat2.format (stats[ii]));
		}

		AlbumProfiling.getInstance ().exit (5);
	}
*/

	///////////////////////////////////////////////////////////////////////////
	public static <T> String collectionToString (Collection<T> collection)
	{
		return arrayToString (collection.toArray (new Object[] {}));
	}

	///////////////////////////////////////////////////////////////////////////
	public static String arrayToString (Object[] items)
	{
		StringBuilder sb = new StringBuilder (items.length * 15);

		for (Object item : items) {
			sb.append (", ").append (item.toString ());
		}

		int startIndex = (sb.length () > 2 ? 2 : 0); //skip initial comma and space, if there

		return sb.substring (startIndex);
	}

	///////////////////////////////////////////////////////////////////////////
	public static String[] addArrays (String[] items1, String[] items2)
	{
		ArrayList<String> list = new ArrayList<String> (items1.length + items2.length);
		list.addAll (Arrays.asList (items1));
		list.addAll (Arrays.asList (items2));

		return list.toArray (new String[] {});
	}

	///////////////////////////////////////////////////////////////////////////
	public static void cacheMaintenance (boolean clearAll) //hack
	{
		if (_nameScaledImageMap == null) {
			_nameScaledImageMap = new ConcurrentHashMap<String, ByteBuffer> (_nameScaledImageMapMaxSize); //hack
		}

		if (_looseCompareMap == null) {
			_looseCompareMap = new ConcurrentHashMap<String, AlbumImagePair> (_looseCompareMapMaxSize); //hack
		}

		if (clearAll) {
			AlbumProfiling.getInstance ().enter/*AndTrace*/ (5);

			AlbumImageDao.getInstance ().cacheMaintenance ();

			long totalMem = Runtime.getRuntime ().totalMemory ();
			long maxMem   = Runtime.getRuntime ().maxMemory ();
			double memoryUsedPercent = 100 * (double) totalMem / maxMem;
			_log.debug ("AlbumImage.cacheMaintenance: memoryUsedPercent: " + _decimalFormat1.format (memoryUsedPercent) + "%" +
													", _nameScaledImageMap: " + _decimalFormat2.format (_nameScaledImageMap.size ()) +
													", _looseCompareMap: " + _decimalFormat2.format (_looseCompareMap.size ()));

//			_nameScaledImageMap.clear ();
//			_looseCompareMap.clear ();
			_nameScaledImageMap = new ConcurrentHashMap<String, ByteBuffer> (_nameScaledImageMapMaxSize); //hack
			_looseCompareMap = new ConcurrentHashMap<String, AlbumImagePair> (_looseCompareMapMaxSize); //hack

			AlbumProfiling.getInstance ().exit (5);

		} else {
			int numItemsRemoved = 0;

			numItemsRemoved = cacheMaintenance (_nameScaledImageMap, _nameScaledImageMapMaxSize);
			if (numItemsRemoved > 0) {
				_log.debug ("AlbumImage.cacheMaintenance: _nameScaledImageMap: " + _decimalFormat2.format (numItemsRemoved) + " items removed, new size = " + _decimalFormat2.format (_nameScaledImageMap.size ()));
			}

			numItemsRemoved = cacheMaintenance (_looseCompareMap, _looseCompareMapMaxSize);
			if (numItemsRemoved > 0) {
				_log.debug ("AlbumImage.cacheMaintenance: _looseCompareMap: " + _decimalFormat2.format (numItemsRemoved) + " items removed, new size = " + _decimalFormat2.format (_looseCompareMap.size ()));
			}

			long totalMem = Runtime.getRuntime ().totalMemory ();
			long maxMem   = Runtime.getRuntime ().maxMemory ();
			double memoryUsedPercent = 100 * (double) totalMem / maxMem;
			_log.debug ("AlbumImage.cacheMaintenance: memoryUsedPercent: " + _decimalFormat1.format (memoryUsedPercent) + "%" +
													", _nameScaledImageMap: " + _decimalFormat2.format (_nameScaledImageMap.size ()) +
													", _looseCompareMap: " + _decimalFormat2.format (_looseCompareMap.size ()));
		}
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

	private long _previousRequestTimestamp = 0;

	private Collection<AlbumImage> _imageDisplayList = null; //list of images to display

	private int _tableBorderPixels = 0; //set to 0 normally, set to 1 for debugging

	private long _allCombosNumImages = 0;
	private List<VPair<Integer, Integer>> _allCombos = null;

	public static final int _nameScaledImageMapMaxSize = 24 * 1000;
	public static final int _looseCompareMapMaxSize = 24 * 1000 * 1000;
	private static Map<String, ByteBuffer> _nameScaledImageMap = null;
	private static Map<String, AlbumImagePair> _looseCompareMap = null;

	private static final String _break = "<BR>";
	private static final String _spacing = "&nbsp";

	private static final String NL = System.getProperty ("line.separator");
	private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy HH:mm");
	private static final DecimalFormat _decimalFormat1 = new DecimalFormat ("###,##0.0");
	private static final DecimalFormat _decimalFormat2 = new DecimalFormat ("###,##0"); //int

	private static AlbumImages _instance = null;
	private static String _prevRootPath = "";
	private static ExecutorService _executor = null;

	private static Logger _log = LogManager.getLogger ();
}
