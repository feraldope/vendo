@REM watcherTest.bat
@echo off
setlocal

REM set DEBUG=/debug

call setJaProject.bat
call addClassPath.bat

echo on
java %PR_JAVA_OPTS_SMALL% -cp "%CLASSPATH%" %JA_CLASS_FOLDER%.ActiveMqTest %DEBUG% %*

endlocal
