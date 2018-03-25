@REM alphanumComparatorTest.bat
@echo off
setlocal

REM set BASE=sample

REM set ORIG=%BASE%.input.xml
REM set RESULT=%BASE%.result.xml

cd /d C:\users\java\VendoUtils

set JAVACMD=java -cp C:\users\java\dist\lib\vendo.jar;. VendoUtils.AlphanumComparatorTest
%JAVACMD% /debug

REM dir /os %BASE%*

REM fc %ORIG% %RESULT%
