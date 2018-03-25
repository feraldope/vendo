//original from: http://stackoverflow.com/questions/1448858/how-to-color-system-out-println-output

//Colors definitions here: C:\Users\java\Win32\Win32.java

package com.vendo.win32;

import java.io.*;

import com.vendo.win32.Win32.Kernel32;


public class ConsoleUtil
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args) throws Exception
	{
		System.out.println ("has_console = " + (has_console () ? "true" : "false"));
		System.out.print ("Normal ");
		static_color_print (System.out,
							" White On Red ",
							Win32.CONSOLE_BACKGROUND_COLOR_RED,
							Win32.CONSOLE_FOREGROUND_COLOR_BRIGHT_WHITE);
		System.out.println (" Normal");
	}

	///////////////////////////////////////////////////////////////////////////
	//this will return false, e.g., if there is no Console
	public static boolean has_console ()
	{
		Win32.CONSOLE_SCREEN_BUFFER_INFO buffer_info = new Win32.CONSOLE_SCREEN_BUFFER_INFO ();
		int stdout_handle = Kernel32.DLL.GetStdHandle (Win32.STD_OUTPUT_HANDLE);
		boolean has_console = Kernel32.DLL.GetConsoleScreenBufferInfo (stdout_handle, buffer_info);
		return has_console;
	}

	///////////////////////////////////////////////////////////////////////////
	//this will return false, e.g., if there is no Console
	public static boolean static_save_settings ()
	{
		if (null == _static_console_screen_buffer_info) {
			_static_console_screen_buffer_info = new Win32.CONSOLE_SCREEN_BUFFER_INFO ();
		}
		int stdout_handle = Kernel32.DLL.GetStdHandle (Win32.STD_OUTPUT_HANDLE);
		return Kernel32.DLL.GetConsoleScreenBufferInfo (stdout_handle, _static_console_screen_buffer_info);
	}

	///////////////////////////////////////////////////////////////////////////
	//this will return false, e.g., if there is no Console
	public static boolean static_restore_color () throws Exception
	{
		if (null == _static_console_screen_buffer_info) {
			throw new Exception ("ConsoleUtil.static_restore_color: error: Must save settings before restore");
		}
		int stdout_handle = Kernel32.DLL.GetStdHandle (Win32.STD_OUTPUT_HANDLE);
		return Kernel32.DLL.SetConsoleTextAttribute (stdout_handle, _static_console_screen_buffer_info.wAttributes);
	}

	///////////////////////////////////////////////////////////////////////////
	public static void static_set_color (Short background_color, Short foreground_color)
	{
		int stdout_handle = Kernel32.DLL.GetStdHandle (Win32.STD_OUTPUT_HANDLE);
		if (null == background_color || null == foreground_color) {
			Win32.CONSOLE_SCREEN_BUFFER_INFO console_screen_buffer_info =  new Win32.CONSOLE_SCREEN_BUFFER_INFO ();
			Kernel32.DLL.GetConsoleScreenBufferInfo (stdout_handle, console_screen_buffer_info);
			short current_bg_and_fg_color = console_screen_buffer_info.wAttributes;
			if (null == background_color) {
				short current_bg_color = (short) (current_bg_and_fg_color / 0x10);
				background_color = new Short (current_bg_color);
			}
			if (null == foreground_color) {
				short current_fg_color = (short) (current_bg_and_fg_color % 0x10);
				foreground_color = new Short (current_fg_color);
			}
		}
		short bg_and_fg_color = (short) (background_color.shortValue () | foreground_color.shortValue ());
		Kernel32.DLL.SetConsoleTextAttribute (stdout_handle, bg_and_fg_color);
	}

	///////////////////////////////////////////////////////////////////////////
	public static <T> void static_color_print (PrintStream ostream, T value, Short background_color, Short foreground_color) throws Exception
	{
		static_save_settings ();
		try {
			static_set_color (background_color, foreground_color);
			ostream.print (value);
		}
		finally {
			static_restore_color ();
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public static <T> void static_color_println (PrintStream ostream, T value, Short background_color, Short foreground_color) throws Exception
	{
		static_save_settings ();
		try {
			static_set_color (background_color, foreground_color);
			ostream.println (value);
		}
		finally {
			static_restore_color ();
		}
	}

	private static Win32.CONSOLE_SCREEN_BUFFER_INFO _static_console_screen_buffer_info = null;
}
