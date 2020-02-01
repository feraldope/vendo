@REM testImageDiffer.bat
@echo off
setlocal

REM if exist %PR_ROOT%\. set PR_ROOT=D:/Netscape/Program
if exist %PR_ROOT%\. set DEST=/dest %PR_ROOT%

set DEBUG=/debug

call setJaProject.bat AlbumServlet
call addClassPath.bat

REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_scaled25.jpg C:\Users\java\images\DSC03969_diff.jpg %*
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_diff.jpg %*
REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% C:\Users\java\images\DSC03969_scaled25.jpg C:\Users\java\images\DSC03969.jpg C:\Users\java\images\DSC03969_diff.jpg %*

REM java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% %PR_ROOT%\jroot\%1 %PR_ROOT%\jroot\%2 %PR_ROOT%\jroot\%3 %4 %5 %6 %7 %8
java %JAVA_OPTS% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.AlbumImageDifferGen %DEBUG% %DEST% %*

endlocal
