//original from:
// http://stackoverflow.com/questions/1448858/how-to-color-system-out-println-output
// http://stackoverflow.com/questions/3249117/cross-platform-way-to-detect-a-symbolic-link-junction-point

//JNA doc (and jna.jar download) here: https://github.com/twall/jna

package com.vendo.win32;

import java.util.*;

import com.sun.jna.*;


public class Win32
{
	public static final int STD_INPUT_HANDLE	= -10;
	public static final int STD_OUTPUT_HANDLE	= -11;
	public static final int STD_ERROR_HANDLE	= -12;

	public static final short CONSOLE_FOREGROUND_COLOR_BLACK		= 0x00;
	public static final short CONSOLE_FOREGROUND_COLOR_BLUE			= 0x01;
	public static final short CONSOLE_FOREGROUND_COLOR_GREEN		= 0x02;
	public static final short CONSOLE_FOREGROUND_COLOR_AQUA			= 0x03;
	public static final short CONSOLE_FOREGROUND_COLOR_RED			= 0x04;
	public static final short CONSOLE_FOREGROUND_COLOR_PURPLE		= 0x05;
	public static final short CONSOLE_FOREGROUND_COLOR_YELLOW		= 0x06;
	public static final short CONSOLE_FOREGROUND_COLOR_WHITE		= 0x07;
	public static final short CONSOLE_FOREGROUND_COLOR_GRAY			= 0x08;
	public static final short CONSOLE_FOREGROUND_COLOR_LIGHT_BLUE	= 0x09;
	public static final short CONSOLE_FOREGROUND_COLOR_LIGHT_GREEN	= 0x0A;
	public static final short CONSOLE_FOREGROUND_COLOR_LIGHT_AQUA	= 0x0B;
	public static final short CONSOLE_FOREGROUND_COLOR_LIGHT_RED	= 0x0C;
	public static final short CONSOLE_FOREGROUND_COLOR_LIGHT_PURPLE	= 0x0D;
	public static final short CONSOLE_FOREGROUND_COLOR_LIGHT_YELLOW	= 0x0E;
	public static final short CONSOLE_FOREGROUND_COLOR_BRIGHT_WHITE	= 0x0F;

	public static final short CONSOLE_BACKGROUND_COLOR_BLACK		= 0x00;
	public static final short CONSOLE_BACKGROUND_COLOR_BLUE			= 0x10;
	public static final short CONSOLE_BACKGROUND_COLOR_GREEN		= 0x20;
	public static final short CONSOLE_BACKGROUND_COLOR_AQUA			= 0x30;
	public static final short CONSOLE_BACKGROUND_COLOR_RED			= 0x40;
	public static final short CONSOLE_BACKGROUND_COLOR_PURPLE		= 0x50;
	public static final short CONSOLE_BACKGROUND_COLOR_YELLOW		= 0x60;
	public static final short CONSOLE_BACKGROUND_COLOR_WHITE		= 0x70;
	public static final short CONSOLE_BACKGROUND_COLOR_GRAY			= 0x80;
	public static final short CONSOLE_BACKGROUND_COLOR_LIGHT_BLUE	= 0x90;
	public static final short CONSOLE_BACKGROUND_COLOR_LIGHT_GREEN	= 0xA0;
	public static final short CONSOLE_BACKGROUND_COLOR_LIGHT_AQUA	= 0xB0;
	public static final short CONSOLE_BACKGROUND_COLOR_LIGHT_RED	= 0xC0;
	public static final short CONSOLE_BACKGROUND_COLOR_LIGHT_PURPLE	= 0xD0;
	public static final short CONSOLE_BACKGROUND_COLOR_LIGHT_YELLOW	= 0xE0;
	public static final short CONSOLE_BACKGROUND_COLOR_BRIGHT_WHITE	= 0xF0;

	///////////////////////////////////////////////////////////////////////////
	// typedef struct _COORD {
	//	SHORT X;
	//	SHORT Y;
	//  } COORD, *PCOORD;
	public static class COORD extends Structure
	{
		public short X;
		public short Y;

		protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] {"X", "Y"});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// typedef struct _SMALL_RECT {
	//	SHORT Left;
	//	SHORT Top;
	//	SHORT Right;
	//	SHORT Bottom;
	//  } SMALL_RECT;
	public static class SMALL_RECT extends Structure
	{
		public short Left;
		public short Top;
		public short Right;
		public short Bottom;

		protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] {"Left", "Top", "Right", "Bottom"});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// typedef struct _CONSOLE_SCREEN_BUFFER_INFO {
	//	COORD	  dwSize;
	//	COORD	  dwCursorPosition;
	//	WORD	   wAttributes;
	//	SMALL_RECT srWindow;
	//	COORD	  dwMaximumWindowSize;
	//  } CONSOLE_SCREEN_BUFFER_INFO;
	public static class CONSOLE_SCREEN_BUFFER_INFO extends Structure
	{
		public COORD dwSize;
		public COORD dwCursorPosition;
		public short wAttributes;
		public SMALL_RECT srWindow;
		public COORD dwMaximumWindowSize;

		protected List<String> getFieldOrder() {
			return Arrays.asList(new String[] {"dwSize", "dwCursorPosition", "wAttributes", "srWindow", "dwMaximumWindowSize"});
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// Source: https://github.com/twall/jna/nonav/javadoc/index.html
	//
	// These function prototypes come from e.g., "C:\Program Files (x86)\Microsoft Visual Studio 8\VC\PlatformSDK\Include\WinBase.h"
	public interface Kernel32 extends Library
	{
		public Kernel32 DLL = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

		// HANDLE WINAPI GetStdHandle(
		//		__in  DWORD nStdHandle
		//	);
		public int GetStdHandle(
				int nStdHandle);

		// BOOL WINAPI SetConsoleTextAttribute(
		//		__in  HANDLE hConsoleOutput,
		//		__in  WORD wAttributes
		//	);
		public boolean SetConsoleTextAttribute(
				int in_hConsoleOutput,
				short in_wAttributes);

		// BOOL WINAPI GetConsoleScreenBufferInfo(
		//		__in   HANDLE hConsoleOutput,
		//		__out  PCONSOLE_SCREEN_BUFFER_INFO lpConsoleScreenBufferInfo
		//	);
		public boolean GetConsoleScreenBufferInfo(
				int in_hConsoleOutput,
				CONSOLE_SCREEN_BUFFER_INFO out_lpConsoleScreenBufferInfo);

		// DWORD WINAPI GetLastError(void);
		public int GetLastError();

		// DWORD WINAPI GetFileAttributesW(
		//		__in   LPCWSTR lpFileName
		//	);
		public int GetFileAttributesW(WString fileName);

		// DWORD WINAPI GetCurrentProcessId(
		//		VOID
		// );
		public int GetCurrentProcessId();
	}
}
