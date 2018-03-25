//ProcessUtils.java

package com.vendo.win32;

//for Kernel32
import com.vendo.win32.Win32.Kernel32;


public class ProcessUtils
{
	///////////////////////////////////////////////////////////////////////////
	public static int getWin32ProcessId () //throws IOException
	{
		return Kernel32.DLL.GetCurrentProcessId ();
	}
}
