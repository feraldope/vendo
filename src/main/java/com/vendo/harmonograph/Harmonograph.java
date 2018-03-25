//Harmonograph.java

package com.vendo.harmonograph;

import java.awt.*;
import javax.swing.*;

public class Harmonograph
{
	public static void main (String args[])
	{
		HarmAppFrame appFrame = new HarmAppFrame ("Harmonograph V2.0");
		appFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		Dimension size = appFrame.getGraphicsPanelSize ();
		int width = 0 + (int) size.getWidth ();
		int height = 180 + (int) size.getHeight ();
		appFrame.setSize (width, height);

		appFrame.setVisible (true);
	}
}
