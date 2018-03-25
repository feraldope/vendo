package com.vendo.shapes;

import java.awt.Color;
import java.awt.Graphics;

public class Shape
{
	protected int x;
	protected int y;
	protected int w;
	protected int h;
	protected Color c;

	public Shape (int x, int y, int w, int h, Color c)
	{
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.c = c;
	}

	public void draw (Graphics g)
	{
//		g.setColor (myColor);
//		g.drawLine (x1, y1, x2, y2);
	}
}
