#!/bin/sh

APP_HOME=$(cd "${0%/*}" || exit; pwd -P)
APP_NAME="Gradle"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ]; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

if [ ! -x "$JAVACMD" ]; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    exit 1
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    -Dorg.gradle.appname="$APP_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"

