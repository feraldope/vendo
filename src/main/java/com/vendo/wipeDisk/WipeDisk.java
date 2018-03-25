//WipeDisk.java

package com.vendo.wipeDisk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WipeDisk
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
//todo - print usage
		int megaBytes = 20;
		String outFilename = "z";

		if (args.length >= 1)
			outFilename = args[0];

		if (args.length >= 2) {
			megaBytes = Integer.parseInt (args[1]);
//todo - catch exception

			if (megaBytes <= 0)
				megaBytes = 20;
		}

		WipeDisk wipeDisk = new WipeDisk ();
		wipeDisk.writeFile (outFilename, megaBytes);
	}

	///////////////////////////////////////////////////////////////////////////
	public WipeDisk ()
	{
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeFile (String filename, int megaBytes)
	{
		FileOutputStream outputStream;
		try {
			File file = new File (filename);
			if (file.exists ()) {
				System.err.println ("Error: file \"" + filename + "\" already exists");
				return false;
			}

			outputStream = new FileOutputStream (file);

		} catch (IOException ee) {
			System.err.println ("Error opening output file \"" + filename + "\"");
			return false;
		}

		try {

			writeBytes (outputStream, megaBytes);

//			outputStream.close ();

		} catch (IOException ee) {
			System.err.println ("Error writing to output file \"" + filename + "\"");

		} finally {
//			outputStream.close ();
		}

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean writeBytes (FileOutputStream outputStream, int megaBytes) throws IOException
	{
		byte bitsOn = (byte) 0xff;
		byte bitsOff = (byte) 0x0;

		byte megaByteArray[] = new byte [1024 * 1024];
		for (int ii = 0; ii < 1024 * 1024; ii += 2) {
			megaByteArray[ii] = bitsOn;
			megaByteArray[ii + 1] = bitsOff;
		}

		try {
			for (int ii = 0; ii < megaBytes; ii++) {
				outputStream.write (megaByteArray);
				outputStream.flush ();
				System.out.print ((ii + 1) + ".");
			}
			System.out.println (NL + "Done.");

		} catch (IOException ee) {
			System.err.println (NL + "Error writing to output file - disk might be full");

		} finally {
			outputStream.close ();
		}

		return true;
	}

	public static final String NL = System.getProperty ("line.separator");
}
