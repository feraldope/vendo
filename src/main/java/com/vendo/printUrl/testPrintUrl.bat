@echo off
REM cause AlbumSerlvet to refresh itself using PrintUrl
setlocal

call setJaProject.bat PrintUrl
call addClassPath.bat

java %PR_JAVA_OPTS_SMALL% -cp "%CLASSPATH%" %JA_CLASS% /checkOnly %1 %2 %3 %4 %5 %6 %7 %8

endlocal
