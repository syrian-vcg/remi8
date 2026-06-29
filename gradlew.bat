@rem gradlew.bat - Gradle Wrapper Script for REMI8 (Windows)
@rem يعمل على Windows

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

@rem التحقق من وجود Java
if "%JAVA_HOME%"=="" (
  set JAVACMD=java
) else (
  set JAVACMD=%JAVA_HOME%/bin/java
)

@rem مسار المشروع
set APP_HOME=%~dp0
set APP_HOME=%APP_HOME:~0,-1%

@rem ملف Wrapper JAR
set GRADLE_WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem خيارات JVM
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
set JVM_OPTS=%DEFAULT_JVM_OPTS%

@rem تشغيل Gradle
"%JAVACMD%" %JVM_OPTS% ^
  -classpath "%GRADLE_WRAPPER_JAR%" ^
  org.gradle.wrapper.GradleWrapperMain ^
  %*

@rem كود الخروج
exit /b %ERRORLEVEL%
