package com.vendo.shapes;

import java.awt.Color;
import java.awt.Graphics;

public class Rect extends Shape
{
	public Rect (int x, int y, int w, int h, Color c)
	{
		super (x, y, w, h, c);
	}

	public void draw (Graphics g)
	{
		g.setColor (c);
		g.drawRect (x, y, w, h);
	}
}
