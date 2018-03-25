//FlashCards.java - math flash cards program

package com.vendo.flashCards;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JTextField;

///////////////////////////////////////////////////////////////////////////////
public class FlashCards
{
	public static void main (String args[])
	{
		FlashCardsFrame flashCardsFrame = new FlashCardsFrame ();
		flashCardsFrame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		flashCardsFrame.setSize (360, 400); // w, h
		flashCardsFrame.setVisible (true);
	}
}

///////////////////////////////////////////////////////////////////////////////
class FlashCardsFrame extends JFrame
{
	Random rand = new Random();
	int statsCorrect;
	int statsIncorrect;
	int statsTotal;
	int sum;
	String equation;

	private FlowLayout layout;
	private Font font;

	private JTextField num1Field; //value 1
	private JTextField num2Field; //value 2
	private JTextField sumField; //for entering result
//	private JTextField blankField; //blank space
	private JTextField resultsField; //results feedback
	private JTextField statsField; //statistics

	public FlashCardsFrame()
	{
		super ("Flash Cards - V1.07");

		layout = new FlowLayout();
		setLayout(layout);

		font = new Font ("Monospaced", Font.BOLD, 36);

		int width1 = 8;
		int width2 = 15;

		num1Field = addField (width1, false, JTextField.RIGHT);
		num2Field = addField (width1, false, JTextField.RIGHT);
		sumField = addField (width1, true,  JTextField.RIGHT);
		/*blankField =*/ addField (width2, false, JTextField.CENTER);
		resultsField = addField (width2, false, JTextField.CENTER);
		statsField = addField (width2, false, JTextField.CENTER);

		//register event handler
		TextFieldHandler handler = new TextFieldHandler();
		sumField.addActionListener (handler);

		nextProblem ();
	}

	private JTextField addField (int width, boolean editable, int alignment)
	{
		JTextField textField = new JTextField (width);
		textField.setFont (font);
		textField.setHorizontalAlignment (alignment);
		textField.setEditable (editable);
		add (textField);
		return textField;
	}

	private int getOperation ()
	{
//		return 0;					//subtraction
//		return 1;					//addition
		return rand.nextInt (2);	//vary between addition and subtraction
	}

	private void nextProblem ()
	{
		int num1 = 1 + rand.nextInt (20);
		int num2 = 1 + rand.nextInt (10);
		int operation = getOperation ();
		if (operation == 0) //subtraction
			num2 *= (-1);

		//prevent subtractions with negative results
		if (operation == 0 && (num1 + num2) < 0) {
			//swap numbers
			int tmp = num1;
			num1 = num2 * (-1);
			num2 = tmp * (-1);
		}

		//for addition always put larger number on top
		if (operation == 1 && num2 > num1) {
			//swap numbers
			int tmp = num1;
			num1 = num2;
			num2 = tmp;
		}

		sum = num1 + num2;

		Integer num1Str = num1;
		Integer num2Str = num2;
		num1Field.setText (num1Str.toString ());
		if (operation == 0) //subtraction
			num2Field.setText (num2Str.toString ());
		else //addition needs leading plus sign
			num2Field.setText ("+" + num2Str);
		sumField.setText ("");

		equation = num1Field.getText () + num2Field.getText ();

		sumField.requestFocus(); //this does not seem to work
		sumField.grabFocus(); //this does not seem to work
	}

	private class TextFieldHandler implements ActionListener
	{
		public void actionPerformed (ActionEvent event)
		{
			if  (event.getSource() == sumField) {
				String str = event.getActionCommand();

				if (!str.equals ("")) {
					int num = Integer.parseInt (str);

					if (num == sum) {
						System.out.println (equation + "=" + num);
						statsCorrect++;
						str = "Correct!";
						nextProblem ();

					} else {
						System.out.println (equation + "=" + num + " X");
						statsIncorrect++;
						sumField.setText ("");
						str = "Try again";
					}

					resultsField.setText (str);

					statsTotal = statsCorrect + statsIncorrect;
					Float percent = 100 * (float) statsCorrect / (float) statsTotal;
					String percentStr = String.format ("%.1f%%", percent);
					String stats = statsCorrect + "/" + statsTotal + " = " + percentStr;
					statsField.setText (stats);
				}
			}
		}
	}
}
