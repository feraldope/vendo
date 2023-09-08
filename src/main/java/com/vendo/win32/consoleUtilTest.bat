@REM testWatcher.bat
@echo off
setlocal

REM overrides
REM set JA_ROOT=C:\users\java
REM set JA_PROJECT=[already set]
REM set JAVA_OPTS=-Xmx1024m

REM set VENDO_JAR=%JA_ROOT%\dist\lib\vendo.jar
REM set SERVLET_JAR=%CATALINA_HOME%\lib\servlet-api.jar
REM set T3RDPARTY_JARS=%JA_ROOT%\lib\*

REM if not exist %VENDO_JAR% echo Error: %VENDO_JAR% not found.&goto allDone
REM if not exist "%SERVLET_JAR%" echo Error: %SERVLET_JAR% not found.&goto allDone

REM add "%JA_ROOT%\%JA_PROJECT%" to CLASSPATH to pick up log4j2.xml
REM set CLASSPATH=%JA_ROOT%\%JA_PROJECT%
REM set CLASSPATH=%CLASSPATH%;%VENDO_JAR%
REM set CLASSPATH=%CLASSPATH%;%SERVLET_JAR%
REM set CLASSPATH=%CLASSPATH%;%T3RDPARTY_JARS%

REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_PROJECT%.ConsoleUtil %*

REM :allDone

REM endlocal

call setJaProject.bat Win32
call addClassPath.bat

REM @echo on
java %PR_JAVA_OPTS_LARGE% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.ConsoleUtil %DEBUG% %*

endlocal
