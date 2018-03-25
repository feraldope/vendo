@REM JpgRgbDataTest.bat
@echo off
setlocal

REM set JAVA_OPTS=-Xmx1024m

call setJaProject.bat JpgRgbData
call addClassPath.bat

java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.JpgRgbData /debug /subdirs /folder . /file "%1*.jpg"

endlocal
