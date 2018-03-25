@REM testListener.bat
@echo off
setlocal

REM set JAVA_OPTS=-Xmx1024m

REM call setJaProject.bat JHistory
call addClassPath.bat

java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.ClipboardListener %*

endlocal
