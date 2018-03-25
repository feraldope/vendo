@REM JpgInfoTest.bat
@echo off
setlocal

REM set JAVA_OPTS=-Xmx1024m

set IMAGES_FOLDER=%JA_ROOT%\src\main\java\com\vendo\images

REM call setJaProject.bat JpgInfo
call addClassPath.bat

REM compact, date-only format
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS% /debug /subdirs /folder %IMAGES_FOLDER% /file *.jpg /dateOnly

REM all data, filtered to interesting values
java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS% /debug /subdirs /folder %IMAGES_FOLDER% /file *.jpg | egrep -i "(.jpg|date|time)" | egrep -vi "(\[File\]|Profile|Exposure|Sub-Sec)"

endlocal
