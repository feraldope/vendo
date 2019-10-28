@REM testImageDiffer.bat
@echo off
setlocal

if exist %PR_ROOT%\. set PR_ROOT=D:/Netscape/Program

set DEBUG=/debug

call setJaProject.bat
call addClassPath.bat

echo on

REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_scaled25.jpg C:\Users\java\images\DSC03969_diff.jpg %*
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_diff.jpg %*
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% C:\Users\java\images\DSC03969_scaled25.jpg C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_diff.jpg %*
java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% %PR_ROOT%\jroot\%1 %PR_ROOT%\jroot\%2 %PR_ROOT%\jroot\%3 %4 %5 %6 %7 %8

endlocal
