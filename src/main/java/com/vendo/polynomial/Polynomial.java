//Polynomial.java

package com.vendo.polynomial;

import java.awt.*;
import javax.swing.*;

public class Polynomial
{
	public static void main (String args[])
	{
		PolynomialAppFrame appFrame = new PolynomialAppFrame ("Polynomial V1.1");
		appFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

		Dimension size = appFrame.getGraphicsPanelSize ();
		int width = (int) size.getWidth ();
		int height = 100 + (int) size.getHeight ();
		appFrame.setSize (width, height);

		appFrame.setVisible (true);
	}
}
