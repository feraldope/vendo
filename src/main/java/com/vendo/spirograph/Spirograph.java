//Spirograph.java

package com.vendo.spirograph;

import javax.swing.JFrame;

public class Spirograph
{
	public static void main (String args[])
	{
		SpirographAppFrame appFrame = new SpirographAppFrame ("Spirograph V1.1");

		appFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		//note this ignores the size of the input panel
		appFrame.setSize (appFrame.getGraphicsPanelSize ());
		appFrame.setVisible (true);
	}
}
