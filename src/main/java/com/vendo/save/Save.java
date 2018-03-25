//Save.java

package com.vendo.save;

import javax.swing.JFrame;
import java.awt.Dimension;

public class Save
{
	public static void main (String args[])
	{
//		String filename = "save.default.dat";
		String filename = "save.dave.dat";
		if (args.length >= 1)
			filename = args[0];

		SaveAppFrame appFrame = new SaveAppFrame ("Save V1.01 - " + filename);
		appFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		SaveRecords records = new SaveRecords ();
		if (records.readFile (filename) == 0)
			System.exit (1); //SaveRecords.readFile () displays error
		appFrame.setRecords (records);

		Dimension size = appFrame.getPreferredSize ();
		int width = (int) size.getWidth ();
		int height = (int) size.getHeight ();
		appFrame.setSize (width, height);

		appFrame.draw ();
		appFrame.setVisible (true);
	}
}
