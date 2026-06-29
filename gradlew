#!/bin/sh
# gradlew - Gradle Wrapper Script for REMI8
# يعمل على Linux و macOS

##############################################################################
# إعدادات Gradle Wrapper
##############################################################################

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# الاكتشاف التلقائي لمجلد APP_HOME
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_HOME="${APP_HOME%/}"
APP_LIB_HOME="${APP_HOME}/lib"

# متغيرات البيئة
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# الكشف عن Java
if [ -n "$JAVA_HOME" ]; then
  if [ -x "$JAVA_HOME/jre/sh/java" ]; then
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ]; then
    die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
  fi
else
  JAVACMD="java"
  which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' found in PATH."
fi

# تحديد ملف Wrapper
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$GRADLE_WRAPPER_JAR" ]; then
  echo "تنزيل Gradle Wrapper..."
  mkdir -p "$APP_HOME/gradle/wrapper"
  # تنزيل تلقائي لـ gradle-wrapper.jar
  if command -v curl > /dev/null; then
    curl -fsSL -o "$GRADLE_WRAPPER_JAR" \
      "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null || true
  elif command -v wget > /dev/null; then
    wget -q -O "$GRADLE_WRAPPER_JAR" \
      "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null || true
  fi
fi

# وسيطات JVM
JVM_OPTS=""
GRADLE_OPTS="${GRADLE_OPTS:-} ${DEFAULT_JVM_OPTS}"

# بناء سطر الأوامر
set -- \
  -classpath "$GRADLE_WRAPPER_JAR" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"

exec "$JAVACMD" $JVM_OPTS $GRADLE_OPTS \
  -Dorg.gradle.appname="$APP_BASE_NAME" \
  -classpath "$GRADLE_WRAPPER_JAR" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
