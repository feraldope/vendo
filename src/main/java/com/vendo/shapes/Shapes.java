package com.vendo.shapes;

import javax.swing.*;

public class Shapes
{
   public static void main (String args[])
   {
      final int size = 500;

	  ShapePanel panel = new ShapePanel (size);
      JFrame application = new JFrame ();

      application.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
      application.add (panel);
      application.setSize (size, size);
      application.setVisible (true);
   }
}
