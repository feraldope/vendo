@REM watcherTest.bat
@echo off
setlocal

if exist %PR_ROOT%\. set DEST=E:/Netscape/Program

set DEBUG=/debug

call setJaProject.bat
call addClassPath.bat

echo on
java %PR_JAVA_OPTS_SMALL% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.WatchDir %DEBUG% %DEST% todo.dat %*

endlocal
