#!/bin/sh
#
# Gradle start up script for UN*X
#
GRADLE_APP_HOME=`dirname "$0"`
GRADLE_APP_HOME=`cd "$GRADLE_APP_HOME" && pwd`

CLASSPATH=$GRADLE_APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
