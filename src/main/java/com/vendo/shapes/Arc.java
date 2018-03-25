package com.vendo.shapes;

import java.awt.Color;
import java.awt.Graphics;

public class Arc extends Shape
{
	public Arc (int x, int y, int w, int h, Color c)
	{
		super (x, y, w, h, c);
	}

	public void draw (Graphics g)
	{
		g.setColor (c);
		g.drawArc (x, y, w, h, 0, 180);
	}
}
