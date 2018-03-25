@REM testProfiling.bat
@echo off
setlocal

REM overrides
set JA_ROOT=C:\users\java
set JA_PROJECT=AlbumServlet
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

REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_PROJECT%.AlbumImageDiffer C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_scaled25.jpg C:\Users\java\images\DSC03969_diff.jpg %*
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_PROJECT%.AlbumImageDiffer C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_diff.jpg %*
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_PROJECT%.AlbumImageDiffer C:\Users\java\images\DSC03969_scaled25.jpg C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_diff.jpg %*
java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_PROJECT%.AlbumImageDiffer %PR_ROOT%\jroot\%1 %PR_ROOT%\jroot\%2 %PR_ROOT%\jroot\%3 %4 %5 %6 %7 %8

:allDone

endlocal
