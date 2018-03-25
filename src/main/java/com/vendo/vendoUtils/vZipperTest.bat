@REM vZipperTest.bat
@echo off
setlocal

set BASE=explorer.exe
set BASE=plot.ini
set BASE=00.drich.log
set BASE=Rhododendron.bmp
set BASE=WindowsUpdate.log
set BASE=setVars.VendoUtils.test.txt

set ORIG=%BASE%.orig
set COMP=%BASE%.comp
set DECOMP=%BASE%.decomp

cd /d C:\users\java\VendoUtils
copy /y %SystemRoot%\%BASE% %ORIG% > nul

set JAVACMD=java -Xmx1024m -cp C:\users\java\dist\lib\vendo.jar;. VendoUtils.VZipperTest

%JAVACMD% /debug /compress /inFile %ORIG% /outFile %COMP%
%JAVACMD% /debug /decompress /inFile %COMP% /outFile %DECOMP%

dir /os %BASE%*

fc %ORIG% %DECOMP% | head
