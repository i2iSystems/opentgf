#!/bin/sh

########################################################################
# 0. Check if JAVA_HOME is set

if [ x"$JAVA_HOME" = x ]
then
	echo "JAVA_HOME must be set...!!!"
	exit 1
fi
if [ ! -x $JAVA_HOME/bin/java ]
then
	echo "$JAVA_HOME/bin/java must exist and be executable...!!!"
	exit 1
fi
START_JAVA=$JAVA_HOME/bin/java



echo "INFO>> opentgf  starting.."
nohup $START_JAVA -Xdebug   -Xms1024m -Xmx4096m -jar opentgf.jar  -nr 1 -srvc  DATA &


PID=$!
trap "echo Killing child pid $PID; kill -TERM $PID" 1 2 3 4 5 6 7 8 9 10 12 13 14 15 19 20

wait
