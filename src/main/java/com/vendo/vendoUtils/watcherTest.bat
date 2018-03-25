@REM watcherTest.bat
@echo off
setlocal

REM overrides
set JA_ROOT=C:\users\java
REM set JA_PROJECT=[already set]
REM set JAVA_OPTS=-Xmx1024m

set VENDO_JAR=%JA_ROOT%\dist\lib\vendo.jar
set SERVLET_JAR=%CATALINA_HOME%\lib\servlet-api.jar
set T3RDPARTY_JARS=%JA_ROOT%\lib\*

if not exist %VENDO_JAR% echo Error: %VENDO_JAR% not found.&goto allDone
if not exist "%SERVLET_JAR%" echo Error: %SERVLET_JAR% not found.&goto allDone

REM add "%JA_ROOT%\%JA_PROJECT%" to CLASSPATH to pick up log4j2.xml
set CLASSPATH=%JA_ROOT%\%JA_PROJECT%
set CLASSPATH=%CLASSPATH%;%VENDO_JAR%
set CLASSPATH=%CLASSPATH%;%SERVLET_JAR%
set CLASSPATH=%CLASSPATH%;%T3RDPARTY_JARS%

java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_PROJECT%.WatchDir %*

:allDone

endlocal
