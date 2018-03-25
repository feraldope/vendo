// VZipper.java
//
//	see example code at:
//	http://java.sun.com/j2se/1.4.2/docs/api/java/util/zip/Deflater.html

package com.vendo.vendoUtils;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.logging.log4j.*;


public class VZipper
{
	///////////////////////////////////////////////////////////////////////////
	public boolean compressFile (String inFilename, String outFilename) throws Exception
	{
		File inFile = new File (inFilename);
		FileInputStream inStream = new FileInputStream (inFile);

		File outFile = new File (outFilename);
		FileOutputStream outStream = new FileOutputStream (outFile);

		return compressStream (inStream, outStream);
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean decompressFile (String inFilename, String outFilename) throws Exception
	{
		File inFile = new File (inFilename);
		FileInputStream inStream = new FileInputStream (inFile);

		File outFile = new File (outFilename);
		FileOutputStream outStream = new FileOutputStream (outFile);

		return decompressStream (inStream, outStream);
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean compressStream (InputStream inStream, OutputStream outStream) throws Exception
	{
//TODO - does this always represent the number of bytes in the stream??
		int available = inStream.available ();
		byte[] bytes = new byte [available];
		inStream.read (bytes, 0, available);

		bytes = compressBytes (bytes);

		outStream.write (bytes, 0, bytes.length);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean decompressStream (InputStream inStream, OutputStream outStream) throws Exception
	{
//TODO - does this always represent the number of bytes in the stream??
		int available = inStream.available ();
		byte[] bytes = new byte [available];
		inStream.read (bytes, 0, available);

		bytes = decompressBytes (bytes);

		outStream.write (bytes, 0, bytes.length);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public byte[] compressBytes (byte[] input) throws Exception
	{
		if (_Debug)
			_log.debug ("comp: orig length = " + input.length);

		byte[] compressed = new byte[input.length];
		Deflater compresser = new Deflater ();
		compresser.setInput (input);
		compresser.finish ();
		int compressedDataLength = compresser.deflate (compressed);

		if (_Debug) {
			String ratio = String.format ("%.1f", (double) input.length / compressedDataLength);

			_log.debug ("comp: final length = " + compressedDataLength);
			_log.debug ("comp: compression ratio = " + ratio + ":1");
		}

		byte[] output = Arrays.copyOf (compressed, compressedDataLength); //truncate away padding
		return output;
	}

	///////////////////////////////////////////////////////////////////////////
	public byte[] decompressBytes (byte[] input) throws Exception
	{
		if (_Debug)
			_log.debug ("decomp: orig length = " + input.length);

		Inflater decompresser = new Inflater ();
		decompresser.setInput (input, 0, input.length);
		byte[] result = new byte[input.length * 50];
		int resultLength = decompresser.inflate (result);
		decompresser.end ();

		if (_Debug)
			_log.debug ("decomp: final length = " + resultLength);

		byte[] output = Arrays.copyOf (result, resultLength); //truncate away padding
		return output;
	}


	//private members
	private static Logger _log = LogManager.getLogger (VZipper.class);

	//global members
	public static boolean _Debug = false;

	public static final String NL = System.getProperty ("line.separator");
}
