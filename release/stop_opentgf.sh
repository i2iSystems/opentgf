#!/bin/sh
USER=`whoami`
PID=`ps -fu $USER|  grep java | grep opentgf | grep -v grep | awk '{ print $2 }'`

if [ -n "$PID" ]
then
	kill -9 $PID
	echo "opentgf shutdown! Please check"
else
	echo opentgf is not running.
fi

