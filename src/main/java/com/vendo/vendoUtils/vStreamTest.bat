@REM vStreamTest.bat
@echo off
setlocal

call setJaProject.bat AlbumServlet
call addClassPath.bat

@echo on
java -cp "%CLASSPATH%" com.vendo.vendoUtils.VStreamTest %DEBUG% %1 %2 %3 %4 %5 %6 %7 %8
