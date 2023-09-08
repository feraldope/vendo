//original color code from: http://stackoverflow.com/questions/1448858/how-to-color-system-out-println-output
//original enumWindows code from https://stackoverflow.com/questions/4478624/problem-using-jna-and-enumwindows

//Colors definitions here: C:\Users\java\Win32\Win32.java

package com.vendo.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.vendo.win32.Win32.Kernel32;

import java.awt.*;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

//see also
//https://www.heatonresearch.com/2018/10/02/jna-quickstart.html

public class ConsoleUtil
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args) throws Exception
	{

		if (false) {
			System.out.println("has_console = " + (has_console() ? "true" : "false"));
			System.out.print("Normal ");
			static_color_print(System.out,
					" White On Red ",
					Win32.CONSOLE_BACKGROUND_COLOR_RED,
					Win32.CONSOLE_FOREGROUND_COLOR_BRIGHT_WHITE);
			System.out.println(" Normal");
		}

		if (true) {
			try {
				ConsoleUtil consoleUtil = new ConsoleUtil();

				//usage <app> [no args]
				//usage <app> <pattern> [one arg]
				if (args.length <= 1) {
					consoleUtil.listWindows(args);

				} else if (args.length == 5) {
					//usage <app> <pattern> <x> <y> <w> <h> [five args]
					consoleUtil.moveWindow(args);
				}
			} catch (Exception e) {
				System.out.println("Error: caught exception: " + e);
			}

		}

		int breakHere = 1;
	}

	///////////////////////////////////////////////////////////////////////////
	private static class WindowData implements Comparable<WindowData> {
		public WindowData(Pointer hWnd, String windowText, Rectangle rectangle) {
			this.hWnd = hWnd;
			this.windowText = windowText;
			this.rectangle = rectangle;
		}
		public Pointer getHWnd() { return hWnd; }
		public String getWindowText() { return windowText; }
		public Rectangle getRectangle() { return rectangle; }

		private final Pointer hWnd;
		private final String windowText;
		private final Rectangle rectangle;

		@Override
		public String toString() {
			return "WindowData{" +
					"hWnd=" + getHWnd() +
					", windowText='" + getWindowText() + '\'' +
					", rectangle=" + getRectangle() +
					'}';
		}

		@Override
		public int compareTo(WindowData data) {
			return getWindowText().toLowerCase().compareTo(data.getWindowText().toLowerCase());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public void listWindows(String[] args) {
		String windowTextPatternStr = "";
		Pattern windowTextPattern = null;

		try {
			windowTextPatternStr = args[0]; //may be null
			windowTextPattern = Pattern.compile(windowTextPatternStr);
		} catch (PatternSyntaxException pse) {
			System.out.println("Error: invalid pattern <" + windowTextPatternStr + ">, ignoring");
		} catch (Exception e) {
			//ignore
		}

		enumWindows(windowTextPattern);

		windowsData.stream()
				   .sorted()
				   .forEach(System.out::println);
	}

	///////////////////////////////////////////////////////////////////////////
	public void moveWindow(String[] args) {
		String windowTextPatternStr = args[0];

		Pattern windowTextPattern = null;
		try {
			windowTextPattern = Pattern.compile(windowTextPatternStr);
		} catch (Exception e) {
			System.out.println("Error: invalid pattern <" + windowTextPatternStr + ">, ignoring");
		}

		int x, y, w, h;
		try {
			x = Integer.parseInt(args[1]);
			y = Integer.parseInt(args[2]);
			w = Integer.parseInt(args[3]);
			h = Integer.parseInt(args[4]);
		} catch (NumberFormatException e) {
			System.out.println("Error: failed to parse args: " + Arrays.toString(args));
			return;
		}

		enumWindows(windowTextPattern);

		windowsData.stream()
				   .sorted()
				   .forEach(System.out::println);

		boolean status = setWindowPosition(windowsData.get(0).getHWnd(), x, y, w, h);
		if (!status) {
			System.out.println("Error: failed to move window: " + windowTextPatternStr);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	public interface User32 extends StdCallLibrary {
		User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

		interface WNDENUMPROC extends StdCallCallback {
			boolean callback(Pointer hWnd, Pointer arg);
		}

		boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer arg);

		int GetWindowTextA(Pointer hWnd, byte[] lpString, int nMaxCount);

		boolean GetWindowRect(Pointer hWnd, WinDef.RECT rect);

		boolean SetWindowPos(Pointer hWnd, Pointer hWndAfter, int x, int y, int cx, int cy, int flags);
	}

	///////////////////////////////////////////////////////////////////////////
	public void enumWindows(Pattern windowTextPattern) {
		final User32 user32 = User32.INSTANCE;

		user32.EnumWindows(new User32.WNDENUMPROC() {
			@Override
			public boolean callback(Pointer hWnd, Pointer userData) {
				byte[] windowText = new byte[512];
				user32.GetWindowTextA(hWnd, windowText, 512);
				String wText = Native.toString(windowText);
				if (!wText.isEmpty()) {
					if (windowTextPattern != null) {
						Matcher matcher = windowTextPattern.matcher(wText);
						if (matcher.matches()) {
							Rectangle rectangle = getWindowRectangle(hWnd);
							windowsData.add(new WindowData(hWnd, wText, rectangle));
						}
					} else {
						Rectangle rectangle = getWindowRectangle(hWnd);
						windowsData.add(new WindowData(hWnd, wText, rectangle));
					}
				}
				return true;
			}
		}, null);

	}

	///////////////////////////////////////////////////////////////////////////
	protected Rectangle getWindowRectangle(Pointer hWnd) {
		final User32 user32 = User32.INSTANCE;

		Rectangle rectangle = new Rectangle();
		WinDef.RECT rect = new WinDef.RECT();
		boolean status = user32.GetWindowRect(hWnd, rect);
		if (status) {
			rectangle = new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
		}

		return rectangle;
	}

	///////////////////////////////////////////////////////////////////////////
	protected Boolean setWindowPosition(Pointer hWnd, int x, int y, int w, int h) {
		final User32 user32 = User32.INSTANCE;

		boolean status = user32.SetWindowPos(hWnd, new Pointer (-1), x, y, w, h, 0);

		return status;
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
				background_color = current_bg_color;
			}
			if (null == foreground_color) {
				short current_fg_color = (short) (current_bg_and_fg_color % 0x10);
				foreground_color = current_fg_color;
			}
		}
		short bg_and_fg_color = (short) (background_color | foreground_color);
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

	private List<WindowData> windowsData = new ArrayList<>();

	private static Win32.CONSOLE_SCREEN_BUFFER_INFO _static_console_screen_buffer_info = null;
}
