package com.vendo.shapes;

import java.awt.*;
import java.util.*;
import javax.swing.*;

public class ShapePanel extends JPanel
{
	private final int num = 50;
	private Shape shapes[] = new Shape [num];
	private Random randomNumbers = new Random ();

	public ShapePanel (int size)
	{
		setBackground (Color.WHITE);

		// create lines
		for (int ii = 0; ii < num; ii++) {
			// generate random coordinates
			int x1 = randomNumbers.nextInt (size);
			int y1 = randomNumbers.nextInt (size);
			int x2 = randomNumbers.nextInt (size);
			int y2 = randomNumbers.nextInt (size);
			int x = Math.min (x1, x2);
			int y = Math.min (y1, y2);
			int w = Math.abs (x2 - x1);
			int h = Math.abs (y2 - y1);
			// generate a random color
			Color c = new Color (randomNumbers.nextInt (256),
								 randomNumbers.nextInt (256),
								 randomNumbers.nextInt (256));

			int type = randomNumbers.nextInt (4);
			switch (type) {
			default:
			case 0: shapes[ii] = new Line (x, y, w, h, c); break;
			case 1: shapes[ii] = new Oval (x, y, w, h, c); break;
			case 2: shapes[ii] = new Rect (x, y, w, h, c); break;
			case 3: shapes[ii] = new Arc (x, y, w, h, c); break;
			}
		}
	}

	// for each shape array, draw the individual shapes
	public void paintComponent (Graphics g)
	{
		super.paintComponent (g);

		// draw the lines
		for (Shape shape : shapes)
			shape.draw (g);
	}
}
