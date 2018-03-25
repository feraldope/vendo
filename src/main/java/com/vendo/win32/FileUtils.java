//original from:
// http://stackoverflow.com/questions/3249117/cross-platform-way-to-detect-a-symbolic-link-junction-point
// http://stackoverflow.com/questions/13733275/determine-whether-a-file-is-a-junction-in-windows-or-not

//Windows command to show junctions: dir /s /A:L

package com.vendo.win32;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import com.sun.jna.*;

import com.vendo.win32.Win32.*; //for Kernel32


/*
C:\Program Files (x86)\Microsoft Visual Studio 8\VC\PlatformSDK\Include\WinNT.h
#define FILE_ATTRIBUTE_READONLY             0x00000001
#define FILE_ATTRIBUTE_HIDDEN               0x00000002
#define FILE_ATTRIBUTE_SYSTEM               0x00000004
#define FILE_ATTRIBUTE_DIRECTORY            0x00000010
#define FILE_ATTRIBUTE_ARCHIVE              0x00000020
#define FILE_ATTRIBUTE_DEVICE               0x00000040
#define FILE_ATTRIBUTE_NORMAL               0x00000080
#define FILE_ATTRIBUTE_TEMPORARY            0x00000100
#define FILE_ATTRIBUTE_SPARSE_FILE          0x00000200
#define FILE_ATTRIBUTE_REPARSE_POINT        0x00000400
#define FILE_ATTRIBUTE_COMPRESSED           0x00000800
#define FILE_ATTRIBUTE_OFFLINE              0x00001000
#define FILE_ATTRIBUTE_NOT_CONTENT_INDEXED  0x00002000
#define FILE_ATTRIBUTE_ENCRYPTED            0x00004000
*/

public class FileUtils
{
	///////////////////////////////////////////////////////////////////////////
	public static int getWin32FileAttributes (Path file) throws IOException
	{
		return Kernel32.DLL.GetFileAttributesW (new WString (file.toRealPath ().toString ()));
	}

	///////////////////////////////////////////////////////////////////////////
	//note this may not work when used on a shared drive
	public static boolean isJunctionOrSymlink1 (Path file) //throws IOException
	{
		boolean isJunctionOrSymlink = false;

		try {
			int attributes = getWin32FileAttributes (file);

//			System.out.println ("attributes: " + String.format ("0x%04X", attributes) + " " + file.toRealPath ().toString ());

			isJunctionOrSymlink = (attributes > 0 && (0x400 & attributes) != 0);

		} catch (IOException ee) {
			ee.printStackTrace ();
		}

		return isJunctionOrSymlink;
	}

	///////////////////////////////////////////////////////////////////////////
	//note this does not seem to work when used on a shared drive
	public static boolean isJunctionOrSymlink2 (Path file) //throws IOException
	{
		boolean isReparsePoint = false;

		BasicFileAttributes attr = null;
		try {
			attr = Files.readAttributes (file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
		} catch (Exception ee) {
			ee.printStackTrace ();
		}

		if (DosFileAttributes.class.isInstance (attr)) {
			try {
				Method m = attr.getClass ().getDeclaredMethod ("isReparsePoint");
				m.setAccessible (true);
				isReparsePoint = (boolean) m.invoke (attr);

			} catch (Exception ee) {
				ee.printStackTrace ();
			}
		}

		return isReparsePoint;
	}
}


