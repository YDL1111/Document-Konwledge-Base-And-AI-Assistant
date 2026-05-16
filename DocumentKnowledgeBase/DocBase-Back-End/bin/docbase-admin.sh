#!/bin/sh
# ./docbase-admin.sh start|stop|restart|status

AppName=docbase-admin.jar
JVM_OPTS="-Dname=$AppName -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"

if [ "$1" = "" ]; then
    echo "Please input a command: {start|stop|restart|status}"
    exit 1
fi

start() {
    PID=`ps -ef | grep java | grep $AppName | grep -v grep | awk '{print $2}'`
    if [ x"$PID" != x"" ]; then
        echo "$AppName is running..."
    else
        nohup java $JVM_OPTS -jar $AppName > /dev/null 2>&1 &
        echo "Start $AppName success..."
    fi
}

stop() {
    PID=`ps -ef | grep java | grep $AppName | grep -v grep | awk '{print $2}'`
    if [ x"$PID" != x"" ]; then
        kill -TERM $PID
        echo "$AppName (pid:$PID) exiting..."
    else
        echo "$AppName already stopped."
    fi
}

restart() {
    stop
    sleep 2
    start
}

status() {
    PID=`ps -ef | grep java | grep $AppName | grep -v grep | wc -l`
    if [ $PID != 0 ]; then
        echo "$AppName is running..."
    else
        echo "$AppName is not running..."
    fi
}

case $1 in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
esac
