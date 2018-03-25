// VCrypto.java
//
//	Encryption and decryption examples:
//	http://www.java2s.com/Code/Java/Security/Encryption.htm
//	http://www.java2s.com/Code/Java/Security/EncryptionanddecryptionwithAESECBPKCS7Padding.htm
//
//	To use this you will need to download the unrestricted policy files for the Sun JCE:
//	jce_policy-6.zip


package com.vendo.vendoUtils;

import java.io.*;
import java.util.*;

import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.*;

//import java.security.InvalidKeyException;


public class VCrypto
{
	///////////////////////////////////////////////////////////////////////////
	public VCrypto ()
	{
		Security.addProvider (new org.bouncycastle.jce.provider.BouncyCastleProvider ());

		_keyBytes = Arrays.copyOf (new StringBuffer ("java/lang/ArrayIndexOutOfBoundsException")
						  .reverse ().toString ().getBytes (), 32);
	}

	///////////////////////////////////////////////////////////////////////////
	public VCrypto (String keyBytes)
	{
		Security.addProvider (new org.bouncycastle.jce.provider.BouncyCastleProvider ());

		_keyBytes = keyBytes.getBytes ();
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean encryptFile (String inFilename, String outFilename) throws Exception
	{
		File inFile = new File (inFilename);
		FileInputStream inStream = new FileInputStream (inFile);

		File outFile = new File (outFilename);
		FileOutputStream outStream = new FileOutputStream (outFile);
//TODO ??
//		outStream.close ();

		return encryptStream (inStream, outStream);
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean decryptFile (String inFilename, String outFilename) throws Exception
	{
		File inFile = new File (inFilename);
		FileInputStream inStream = new FileInputStream (inFile);

		File outFile = new File (outFilename);
		FileOutputStream outStream = new FileOutputStream (outFile);
//TODO ??
//		outStream.close ();

		return decryptStream (inStream, outStream);
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean encryptStream (InputStream inStream, OutputStream outStream) throws Exception
	{
//TODO - does this always represent the number of bytes in the stream??
		int available = inStream.available ();
		byte[] bytes = new byte [available];
		inStream.read (bytes, 0, available);

		bytes = encryptBytes (bytes);

		outStream.write (bytes, 0, bytes.length);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public boolean decryptStream (InputStream inStream, OutputStream outStream) throws Exception
	{
//TODO - does this always represent the number of bytes in the stream??
		int available = inStream.available ();
		byte[] bytes = new byte [available];
		inStream.read (bytes, 0, available);

		bytes = decryptBytes (bytes);

		outStream.write (bytes, 0, bytes.length);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	public byte[] encryptBytes (byte[] bytes) throws Exception
	{
		SecretKeySpec key = new SecretKeySpec (_keyBytes, "AES");

		Cipher cipher = Cipher.getInstance ("AES/ECB/PKCS7Padding", "BC");
		cipher.init (Cipher.ENCRYPT_MODE, key);

		byte[] cipherText = new byte[cipher.getOutputSize (bytes.length)];
		int ctLength = cipher.update (bytes, 0, bytes.length, cipherText, 0);
		ctLength += cipher.doFinal (cipherText, ctLength);

		if (_Debug)
			_log.debug ("encryptBytes: ctLength = " + ctLength);

		return cipherText;
	}

	///////////////////////////////////////////////////////////////////////////
	public byte[] decryptBytes (byte[] bytes) throws Exception
	{
		SecretKeySpec key = new SecretKeySpec (_keyBytes, "AES");

		Cipher cipher = Cipher.getInstance ("AES/ECB/PKCS7Padding", "BC");
		cipher.init (Cipher.DECRYPT_MODE, key);

		byte[] plainText = new byte[cipher.getOutputSize (bytes.length)];
		int ptLength = cipher.update (bytes, 0, bytes.length, plainText, 0);
		ptLength += cipher.doFinal (plainText, ptLength);

		if (_Debug)
			_log.debug ("decryptBytes: ptLength = " + ptLength);

		byte[] newBytes = Arrays.copyOf (plainText, ptLength); //truncate away padding

		return newBytes;
	}

	//private members
	private byte[] _keyBytes = null;

	private static Logger _log = LogManager.getLogger (VCrypto.class);

	//global members
	public static boolean _Debug = false;

	public static final String NL = System.getProperty ("line.separator");
}
