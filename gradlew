#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
# Gradle Wrapper Script — REMI8 Engine
#

##############################################################################
# Shell Script Variables
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`
APP_HOME=`cd "$APP_HOME" && pwd`

GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
GRADLE_WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

##############################################################################
# Java Detection
##############################################################################

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME='$JAVA_HOME' — cannot find java executable." >&2
        exit 1
    fi
else
    JAVACMD="java"
    java -version >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME not set and 'java' not found in PATH." >&2
        exit 1
    }
fi

##############################################################################
# JVM Options — بدون علامات اقتباس داخل المتغير
##############################################################################

JVM_OPTS="-Xmx512m -Xms64m -XX:MaxMetaspaceSize=256m"

# إضافة خيارات المستخدم إن وجدت
if [ -n "$GRADLE_OPTS" ]; then
    JVM_OPTS="$JVM_OPTS $GRADLE_OPTS"
fi

if [ -n "$JAVA_OPTS" ]; then
    JVM_OPTS="$JVM_OPTS $JAVA_OPTS"
fi

##############################################################################
# Classpath
##############################################################################

CLASSPATH="$GRADLE_WRAPPER_JAR"

##############################################################################
# Execute
##############################################################################

exec "$JAVACMD" \
    $JVM_OPTS \
    -Dorg.gradle.appname="$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
