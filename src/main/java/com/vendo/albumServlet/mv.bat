@REM mv.bat
@echo off
setlocal

REM set PROFILE=-Xrunhprof:cpu=times

REM java %PROFILE% -cp C:\users\java\dist\lib\vendo.jar;.;C:\apache-tomcat-6.0.10\lib\servlet-api.jar;C:\apache-tomcat-6.0.10\common\lib\servlet-api.jar;C:\users\java\AlbumServlet AlbumServlet.AlbumFileRename
REM java %PROFILE% -cp %CLASSPATH% AlbumServlet.AlbumFileRename
java %PROFILE% -cp C:\users\java\dist\lib\vendo.jar;%CLASSPATH% %JA_PROJECT%.AlbumFileRename %1 %2 %3 %4 %5 %6 %7 %8
