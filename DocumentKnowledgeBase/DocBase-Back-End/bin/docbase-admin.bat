@echo off
set AppName=docbase-admin.jar
set JVM_OPTS="-Dname=%AppName% -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"

echo.
echo [1] Start %AppName%
echo [2] Stop %AppName%
echo [3] Restart %AppName%
echo [4] Status %AppName%
echo [5] Exit
echo.

echo Please choose an action:
set /p ID=
if "%ID%"=="1" goto start
if "%ID%"=="2" goto stop
if "%ID%"=="3" goto restart
if "%ID%"=="4" goto status
if "%ID%"=="5" exit
pause

:start
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
    set pid=%%a
    set image_name=%%b
)
if defined pid (
    echo %AppName% is already running
    pause
    goto:eof
)
start javaw %JVM_OPTS% -jar %AppName%
echo Starting %AppName%...
echo Start %AppName% success...
goto:eof

:stop
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
    set pid=%%a
    set image_name=%%b
)
if not defined pid (
    echo process %AppName% does not exist
) else (
    echo prepare to kill %image_name%
    echo start kill %pid% ...
    taskkill /f /pid %pid%
)
goto:eof

:restart
call :stop
call :start
goto:eof

:status
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
    set pid=%%a
    set image_name=%%b
)
if not defined pid (
    echo process %AppName% is stopped
) else (
    echo %image_name% is running
)
goto:eof
