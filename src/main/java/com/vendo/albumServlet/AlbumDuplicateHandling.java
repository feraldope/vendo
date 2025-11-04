//AlbumDuplicate.java

package com.vendo.albumServlet;

import java.util.ArrayList;
import java.util.List;

//import org.apache.logging.log4j.*;


public enum AlbumDuplicateHandling {
	SelectNone ("Select None",                                 Mode.IsForSelecting),
	SelectFirst ("Select First",                               Mode.IsForSelecting), //select first image in pair
	SelectSecond ("Select Second",                             Mode.IsForSelecting), //select second image in pair
	SelectSmaller ("Select Smaller",                           Mode.IsForSelecting), //select the smaller image
	SelectSmallerFirst ("Select Smaller / First",              Mode.IsForSelecting), //select the smaller image, or first in pair if same size
	SelectSmallerSecond ("Select Smaller / Second",            Mode.IsForSelecting), //select the smaller image, or second in pair if same size
	ShowOnlyMisMatchByPixels ("Show Only Mis-match By Pixels", Mode.IsForShowing);   //only show duplicates that are mis-matched by pixels size

	public enum Mode {
		IsForSelecting, //enums with this mode are used for selecting which duplicate images will be DELETED
		IsForShowing    //enums with this mode are used for selecting which duplicate images will be SHOWN
	}

	///////////////////////////////////////////////////////////////////////////
	AlbumDuplicateHandling(String name, Mode mode) {
		String prefix = Mode.IsForSelecting == mode ? "select" : "show";
		value = new AlbumStringPair (name, prefix + name);
		this.mode = mode;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getName () {
		return value.getName ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getSymbol () {
		return value.getSymbol ();
	}

	///////////////////////////////////////////////////////////////////////////
	public Mode getMode () {
		return mode;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isForSelecting () {
		return Mode.IsForSelecting == mode;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean isForShowing () {
		return Mode.IsForShowing == mode;
	}

	///////////////////////////////////////////////////////////////////////////
	private static void init () {
		if (values == null) {
			List<AlbumStringPair> arrayList = new ArrayList<> ();

			for (AlbumDuplicateHandling ff : values ()) {
				arrayList.add (new AlbumStringPair (ff.getName (), ff.getSymbol ()));
			}

			values = arrayList.toArray (new AlbumStringPair[] {});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumStringPair[] getValues () {
		init ();

		return values;
	}

	///////////////////////////////////////////////////////////////////////////
	public static AlbumDuplicateHandling getValue (String symbol) {
		//brute-force method
		for (AlbumDuplicateHandling ff : values ()) {
			if (ff.getSymbol ().equals (symbol)) {
				return ff;
			}
		}

		throw new RuntimeException ("AlbumDuplicateHandling.getValue: invalid symbol \"" + symbol + "\"");
	}


	//members
	private final AlbumStringPair value;
	private final Mode mode;

	private static AlbumStringPair[] values;
//	private static Logger log = LogManager.getLogger ();
}
