::
:: Copyright (c) 2012-2013 André Bargull
:: Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
::
:: <https://github.com/anba/es6draft>
::

::
:: Description:
:: Helper script to start the simple REPL
::

@ECHO OFF

SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

:: Base directory and class information
set REL_PATH=%~dp0
set BUILD_DIR=%REL_PATH%\..\target
set CLASSPATH=%BUILD_DIR%\es6draft.jar
set MAINCLASS=com.github.anba.es6draft.repl.Repl

:: Determine Java start command
if not defined JAVA_HOME (
  set JAVA_CMD=java
) else (
  set JAVA_CMD=%JAVA_HOME%\bin\java
)

:: Set default Java options
set JAVA_OPTS=%JAVA_OPTS% -ea -server -XX:+TieredCompilation

:: Adjust classpath based on current Java version
for /f delims^=-^"^ tokens^=2 %%j in ('java -fullversion 2^>^&1') do (
  set JAVA_VERSION=%%j
)
if %JAVA_VERSION% LSS 1.7.0_45 (
  set JAVA_OPTS=%JAVA_OPTS% -esa
  set JAVA_CLASSPATH=-Xbootclasspath/a:%CLASSPATH%
) else (
  set JAVA_CLASSPATH=-cp %CLASSPATH%
)

:: Configure JLine terminal settings
if %JAVA_VERSION% LSS 1.7.0_45 (
  set JLINE_TERMINAL=none
) else (
  set JLINE_TERMINAL=windows
)
set JAVA_OPTS=%JAVA_OPTS% -Djline.terminal=%JLINE_TERMINAL%

:: Start application
%JAVA_CMD% %JAVA_OPTS% %JAVA_CLASSPATH% %MAINCLASS% %*
