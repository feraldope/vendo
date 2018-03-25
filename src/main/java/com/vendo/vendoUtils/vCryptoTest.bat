@REM vCryptoTest.bat
@echo off
setlocal

set BASE=explorer.exe
set BASE=plot.ini
set BASE=00.drich.log
set BASE=Rhododendron.bmp
set BASE=WindowsUpdate.log
set BASE=setVars.VendoUtils.test.txt

set ORIG=%BASE%.orig
set ENCRYPT=%BASE%.encrypt
set DECRYPT=%BASE%.decrypt

cd /d C:\users\java\VendoUtils
copy /y %SystemRoot%\%BASE% %ORIG% > nul

set JAVACMD=java -Xmx1024m -cp C:\users\java\dist\lib\vendo.jar;. VendoUtils.VCryptoTest

%JAVACMD% /debug /encrypt /inFile %ORIG% /outFile %ENCRYPT%
%JAVACMD% /debug /decrypt /inFile %ENCRYPT% /outFile %DECRYPT%

dir /os %BASE%*

fc %ORIG% %DECRYPT% | head
