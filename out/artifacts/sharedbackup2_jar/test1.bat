XCOPY sharedbackup2.jar storage /Y
XCOPY sharedbackup2.jar storage_2 /Y
cd storage
start test.bat
cd ..\storage_2
start test.bat
