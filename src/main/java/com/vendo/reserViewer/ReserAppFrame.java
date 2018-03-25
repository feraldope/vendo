//ReserAppFrame.java

package com.vendo.reserViewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import com.vendo.vendoUtils.TableSorter;
import com.vendo.vendoUtils.VendoUtils;


//public class ReserAppFrame extends JFrame implements ActionListener
public class ReserAppFrame extends JPanel implements ActionListener
 {
	///////////////////////////////////////////////////////////////////////////
	public ReserAppFrame (String title, String mainClass, String filename)
	{
		super (new BorderLayout ());

		_frame = new JFrame (title);
		_frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

/*
		//create menu
		JMenuBar menuBar = new JMenuBar ();
		setJMenuBar (menuBar);
		JMenu menu = new JMenu ("File");
		menuBar.add (menu);

		JMenuItem menuItem = new JMenuItem ("Restart");
		menuItem.addActionListener (this);
		menu.add (menuItem);

		menuItem = new JMenuItem ("Exit");
		menuItem.addActionListener (this);
		menu.add (menuItem);
*/

		//generate command to restart app using java/javaw
		StringBuffer sb = new StringBuffer ();
		sb.append (System.getProperty ("java.home"))
		  .append (_slash)
		  .append ("bin")
		  .append (_slash)
		  .append (VendoUtils.isWindowsOs () ? "javaw" : "java");	//NOTE we hardcode javaw for windows
		String java = sb.toString ();

		sb = new StringBuffer ();
		sb.append (quote (java))
		  .append (" -cp ")
		  .append (quote (System.getProperty ("java.class.path")))
		  .append (_space)
		  .append (mainClass)
		  .append (_space)
		  .append (quote (filename))
		  .append (ReserViewer._Debug ? " /debug" : "");
		_restartCommand = sb.toString ();

//		if (ReserViewer._Debug) {
		if (false) {
			System.out.println ("System.getProperty ('user.dir') = " + quote (System.getProperty ("user.dir")));
			System.out.println ("System.getProperty ('java.class.path') = " + quote (System.getProperty ("java.class.path")));
			System.out.println ("restartCommand = " + _restartCommand);
		}

		//add handler to exit app on close window
		_frame.addWindowListener (new WindowAdapter () {
			public void windowClosing (WindowEvent event) {
				System.exit (0);
			}
		});
	}

	///////////////////////////////////////////////////////////////////////////
	public void init ()
	{
		final Border empty = BorderFactory.createEmptyBorder ();
		JTextField info = new JTextField (_records.getInfoStr (), 10);
		info.setEditable (false);
		info.setHorizontalAlignment (JTextField.RIGHT);
		info.setBorder(empty);
		info.setFont (_infoFont);

		JButton restartButton = new JButton ("Restart");
		restartButton.addActionListener (this);

		Box topPanel = Box.createHorizontalBox ();
		for (int ii = 0; ii < 20; ii++) //cheezy attempt to move restart button to right-hand side
			topPanel.add (new JPanel ());
		topPanel.add (info);
		for (int ii = 0; ii < 4; ii++) //cheezy attempt to move restart button to right-hand side
			topPanel.add (new JPanel ());
		topPanel.add (restartButton);
		topPanel.add (Box.createHorizontalGlue ());

		TableSorter sorter = new TableSorter (_records);
		JTable table = new JTable (sorter);
		sorter.setTableHeader (table.getTableHeader ());
		sorter.setSortingStatus (4, TableSorter.ASCENDING); //set default sort on date column

		_records.initTableSorter (sorter);

		table.getTableHeader ().setToolTipText ("Click to specify sorting; Control-Click to specify secondary sorting");
		table.getTableHeader ().setDefaultRenderer (new ReserCellRenderer (table.getTableHeader ().getDefaultRenderer (), _tableHeaderFont));
		table.setRowSelectionAllowed (false);

//		table.setShowGrid (false);
//		table.setShowHorizontalLines (false);
//		table.setShowVerticalLines (false);

		int tableWidth = 0;
		for (int ii = 0; ii < _records.getColumnCount (); ii++) {
			int cellWidth = _records.getCellWidth (ii) * _tableCellFont.getSize ();
			table.getColumnModel ().getColumn (ii).setPreferredWidth (cellWidth);
			tableWidth += cellWidth;
		}

		final int cellHeight =  (_tableCellFont.getSize () * 5) / 3;
		final int tableHeight = _records.getRowCount () * cellHeight;
		table.setRowHeight (cellHeight);
		table.setPreferredScrollableViewportSize (new Dimension (tableWidth, tableHeight));

		table.setDefaultRenderer (String.class, new ReserCellRenderer (table.getDefaultRenderer (String.class), _tableCellFont));
		table.setDefaultRenderer (ReserChannel.class, new ReserCellRenderer (table.getDefaultRenderer (ReserChannel.class), _tableCellFont));
		table.setDefaultRenderer (ReserDate.class, new ReserCellRenderer (table.getDefaultRenderer (ReserDate.class), _tableCellFont));
		table.setDefaultRenderer (ReserTime.class, new ReserCellRenderer (table.getDefaultRenderer (ReserTime.class), _tableCellFont));
		table.setDefaultRenderer (ReserDuration.class, new ReserCellRenderer (table.getDefaultRenderer (ReserDuration.class), _tableCellFont));

		JScrollPane scrollPane = new JScrollPane (table);

		Box bottomPanel = Box.createHorizontalBox ();
		bottomPanel.add (Box.createHorizontalGlue ());
		bottomPanel.add (scrollPane);
		bottomPanel.add (Box.createHorizontalGlue ());

		add (topPanel, BorderLayout.NORTH);
		add (bottomPanel, BorderLayout.CENTER);

		setOpaque (true);
		_frame.setContentPane (this);
		_frame.pack ();
		_frame.setVisible (true);
	}

	///////////////////////////////////////////////////////////////////////////
    public void actionPerformed (ActionEvent ee)
	{
		String str = ee.getActionCommand ();

		if (str.equals ("Exit")) { //obsolete
			System.exit (0);

		} else if (str.equals ("Restart")) {
			try {
				Runtime.getRuntime ().exec (_restartCommand);
			} catch (Exception ex) {
				String msg = "Error executing \"" + _restartCommand + "\"";
				System.out.println (msg);
				JOptionPane.showMessageDialog (null, msg, "Error", JOptionPane.ERROR_MESSAGE);
			}

			System.exit (0);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private String quote (String string)
	{
		final String quote = new String ("\"");

		if (VendoUtils.isWindowsOs ())
			return quote + string + quote;
		else
			return string;
	}

	///////////////////////////////////////////////////////////////////////////
	public void setRecords (ReserRecords records)
	{
		_records = records;
	}

	///////////////////////////////////////////////////////////////////////////
	//override font and horizontal alignment
	private class ReserCellRenderer implements TableCellRenderer
	{
		public ReserCellRenderer (TableCellRenderer tableCellRenderer, Font font)
		{
			_tableCellRenderer = tableCellRenderer;
			_font = font;
		}

		public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component component = _tableCellRenderer.getTableCellRendererComponent (table, value, isSelected, hasFocus, row, column);
			if (component instanceof JLabel) {
				((JLabel) component).setHorizontalAlignment (JLabel.CENTER);
				((JLabel) component).setFont (_font);
			}

			return component;
		}

		//private members
		private TableCellRenderer _tableCellRenderer;
		private Font _font;
	}

	//members
	private JFrame _frame;
	private String _restartCommand;
	private ReserRecords _records;

	private static Font _tableHeaderFont = new Font ("SansSerif", Font.BOLD, 14);
	private static Font _tableCellFont = new Font ("SansSerif", Font.PLAIN, 14);
	private static Font _infoFont = new Font ("SansSerif", Font.PLAIN, 14);

	private final String _space = " ";
	private final String _slash = System.getProperty ("file.separator");
}
