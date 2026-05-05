#!/bin/sh
# Gradle wrapper script
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
DEFAULT_JVM_OPTS='-Xmx64m -Xms64m'
MAX_FD=maximum
warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
