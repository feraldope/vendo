@REM testLatency.bat
@echo off
setlocal

@REM Copy: 
@REM C:\Users\java\vendo\target\vendo-1.0-SNAPSHOT.jar
@REM C:\Users\java\vendo\target\dependency\*.jar
@REM

REM if exist %PR_ROOT%\. set PR_ROOT=D:/Netscape/Program
REM if exist %PR_ROOT%\. set DEST=/dest %PR_ROOT%

set DEBUG=/debug
set URL=/url www.google.com
set URL=/url www.ea.com
set URL=/url www.playstation.com
set URL=/url www.activision.com
set PORT=/port 80
set CYCLE_IN_MINUTES=/cycle 1

call setJaProject.bat JLatency
call addClassPath.bat

java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.%JA_PROJECT% %DEBUG% %URL% %PORT% %CYCLE_IN_MINUTES% %*

endlocal
