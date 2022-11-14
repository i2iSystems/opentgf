#!/bin/sh
USER=`whoami`
PID=`ps -fu $USER|  grep java | grep i2iTGF | grep -v grep | awk '{ print $2 }'`

if [ -n "$PID" ]
then
	kill -9 $PID
	echo "i2iTGF shutdown! Please check"
else
	echo i2iTGF is not running.
fi

