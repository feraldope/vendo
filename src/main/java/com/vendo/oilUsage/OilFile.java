//OilFile.java

package com.vendo.oilUsage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;

//import org.apache.logging.log4j.*;


public class OilFile
{
	///////////////////////////////////////////////////////////////////////////
	public OilFile (Path dataFile)
	{
		_dataFile = dataFile;
	}

	///////////////////////////////////////////////////////////////////////////
	//returns sorted list of all data records in file
	public Collection<OilFileData> readFile ()
	{
		ArrayList<OilFileData> oilFileDataList = new ArrayList<OilFileData> ();

		Scanner scanner = null;
		try {
			scanner = new Scanner (new FileInputStream (_dataFile.toFile ()));

		} catch (FileNotFoundException ex) {
			ex.printStackTrace ();
			return null;
		}

		while (scanner.hasNextLine ()) {
			String string = scanner.nextLine ();

			if (string.startsWith ("[EOF]")) { //honor logical end-of-file marker in data file
				break;
			}

			try {
				OilFileData oilFileData = new OilFileData (string);
				oilFileDataList.add (oilFileData);

			} catch (OilUsageNotADataRowException ex) {
				//ignore exceptions which indicate the string was not a valid data row

			} catch (Exception ex) {
				ex.printStackTrace ();
			}
		}
		scanner.close ();

		Collections.sort (oilFileDataList, OilFileData.oilFileDataComparator);

		//set each record's start date to be equal to the previous record's end date
		for (int ii = 1; ii < oilFileDataList.size (); ii++) { //note ii starts at 1
			oilFileDataList.get (ii).setStartDate (oilFileDataList.get (ii - 1).getEndDate ());
		}

		return oilFileDataList;
	}


	//private members
	private Path _dataFile;

//	private static final Logger _log = LogManager.getLogger ();
}
