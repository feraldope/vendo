//Original inspiration from
// http://www.coderanch.com/t/377833/java/java/listen-clipboard
// http://javarevisited.blogspot.com/2013/12/inter-thread-communication-in-java-wait-notify-example.html?_sm_au_=iVV5n6q6pSsPTZ0P

package com.vendo.jHistory;

import com.vendo.vendoUtils.VendoUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.LinkedList;
import java.util.Queue;

class ClipboardListener extends Thread implements ClipboardOwner
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args)
	{
		_trace = true; //enable debug/trace when run from CLI

		final Queue<StringBuffer> queue = new LinkedList<StringBuffer> ();
		ClipboardListener clipboardListener = new ClipboardListener (queue);
		clipboardListener.start ();

		System.out.println ("Listening to clipboard...");

		while (true) {
			try {
				synchronized (queue) {
					//waiting condition - wait until Queue is not empty
					while (queue.size () == 0) {
						try {
							if (_trace) {
								System.out.println ("ClipboardListener.run: queue is empty, waiting");
							}
							queue.wait ();

						} catch (InterruptedException ex) {
							ex.printStackTrace ();
						}
					}
					StringBuffer stringBuffer = queue.poll ();
					String string = stringBuffer.toString ().trim ();

					System.out.println ("ClipboardListener.main: consuming: " + string);
					queue.notify ();
				}

			} catch (Exception ex) {
				ex.printStackTrace ();
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public ClipboardListener (Queue<StringBuffer> queue)
	{
		super("ClipboardListener");
		_queue = queue;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void run ()
	{
		Transferable contents = null;
		try {
			contents = _clipboard.getContents (this);
			regainOwnership (contents);

		} catch (Exception ex) {
			ex.printStackTrace ();
		}

		System.out.println ("ClipboardListener.run: entering synchronized block");

		//wait here forever
		try {
			synchronized (contents) {
				contents.wait ();
			}

		} catch (Exception ex) {
			ex.printStackTrace ();
		}

//		if (_trace) {
			System.out.println ("ClipboardListener.run: leaving run() method");
//		}
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public void lostOwnership (Clipboard unused1, Transferable unused2)
	{
		int retries = 5;
		Transferable contents = null;

		do {
			VendoUtils.sleepMillis (10); //hack

			try {
				contents = _clipboard.getContents (this);
				regainOwnership (contents);

			} catch (Exception ex) {
				ex.printStackTrace ();
			}

		} while (contents == null && --retries > 0);
	}

	///////////////////////////////////////////////////////////////////////////
	void regainOwnership (Transferable contents)
	{
		VendoUtils.sleepMillis (50); //hack

		try {
			_clipboard.setContents (contents, this);
			processContents (contents);

		} catch (Exception ex) {
			ex.printStackTrace ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	void processContents (Transferable contents)
	{
		String string = "";

//		if (_trace && contents != null) {
//			DataFlavor[] dataFlavors = contents.getTransferDataFlavors ();
//			for (DataFlavor dataFlavor : dataFlavors) {
//				System.out.println ("ClipboardListener.processContents: dataFlavor = " + dataFlavor);
//			}
//		}

		if (contents != null && contents.isDataFlavorSupported (DataFlavor.stringFlavor)) {
			try {
				string = ((String) contents.getTransferData (DataFlavor.stringFlavor)).trim ();

			} catch (Exception ee) {
				ee.printStackTrace ();
			}

			synchronized (_queue) {
				//waiting condition - wait until Queue is not empty
				while (_queue.size () >= 1) {
					try {
						if (_trace) {
							System.out.println ("ClipboardListener.processContents: queue is full, waiting");
						}
						_queue.wait ();

					} catch (InterruptedException ex) {
						ex.printStackTrace ();
					}
				}
				if (_trace) {
					System.out.println ("ClipboardListener.processContents: producing: " + string);
				}

				StringBuffer stringBuffer = new StringBuffer ();
				stringBuffer.insert (0, string);

				_queue.add (stringBuffer);
				_queue.notify ();
			}
		}
	}

	//private members
	private static boolean _trace = false;
	private final Queue<StringBuffer> _queue;
	private final Clipboard _clipboard = Toolkit.getDefaultToolkit ().getSystemClipboard ();
}
