REM To compress a JPG file:
cd %PR_ROOT%
ju.bat /debug /scalePercent  25 /suffix  _25percent /dest C:\Users\david\Downloads\ /src C:\Users\david\Downloads\ DSC_9272.JPG
ju.bat /debug /scalePercent  50 /suffix  _50percent /dest C:\Users\david\Downloads\ /src C:\Users\david\Downloads\ DSC_9272.JPG
ju.bat /debug /scalePercent 100 /suffix _100percent /dest C:\Users\david\Downloads\ /src C:\Users\david\Downloads\ DSC_9272.JPG

