@echo off
echo.
echo [DocBase Enterprise Knowledge Base] Start packaged backend service
echo.

cd %~dp0
cd ../docbase-admin/target

set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java -jar %JAVA_OPTS% docbase-admin.jar

cd ../..
pause
