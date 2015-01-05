::
:: Copyright (c) 2012-2015 André Bargull
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

:: Determine Java start command
if not defined JAVA_HOME (
  set JAVA_CMD=java
) else (
  set JAVA_CMD=%JAVA_HOME%\bin\java
)

:: Set default Java options
set JAVA_OPTS=%JAVA_OPTS% -ea -server -XX:+TieredCompilation

:: Invokedynamic implementation is buggy prior to Java 1.7.0_45
for /f delims^=-^"^ tokens^=2 %%j in ('java -fullversion 2^>^&1') do (
  set JAVA_VERSION=%%j
)
if %JAVA_VERSION% LSS 1.7.0_45 (
  set JAVA_OPTS=%JAVA_OPTS% -esa
  set USE_BOOTCP=1
) else (
  set USE_BOOTCP=0
)

:: Fully qualified name to main class
set MAINCLASS=@mainClass@

:: distribution, shaded, development
set EXEC_MODE=@exec.mode@

:: Determine base directory
set REL_PATH=%~dp0
IF "%EXEC_MODE%"=="distribution" (
  set BASE_DIR=%REL_PATH%\..
) ELSE IF "%EXEC_MODE%"=="shaded" (
  set BASE_DIR=%REL_PATH%\..\target
) ELSE IF "%EXEC_MODE%"=="development" (
  set BASE_DIR=%REL_PATH%\..\target
) ELSE (
  echo Unsupported option %EXEC_MODE%
  exit /B 1
)

:: Compute classpath
IF "%EXEC_MODE%"=="distribution" (
  set CLASSPATH=%BASE_DIR%\@exec.name@.jar
) ELSE IF "%EXEC_MODE%"=="shaded" (
  set CLASSPATH=%BASE_DIR%\@exec.name@.jar
) ELSE IF "%EXEC_MODE%"=="development" (
  set CLASSPATH=%BASE_DIR%\classes;%BASE_DIR%\dependencies\*
) ELSE (
  echo Unsupported option %EXEC_MODE%
  exit /B 1
)

:: Adjust classpath based on current Java version
if %USE_BOOTCP% EQU 1 (
  set JAVA_CLASSPATH=-Xbootclasspath/a:%CLASSPATH%
) else (
  set JAVA_CLASSPATH=-cp %CLASSPATH%
)

:: Configure JLine terminal settings
if %USE_BOOTCP% EQU 1 (
  set JLINE_TERMINAL=none
) else (
  set JLINE_TERMINAL=windows
)
set JAVA_OPTS=%JAVA_OPTS% -Djline.terminal=%JLINE_TERMINAL%

:: Start application
%JAVA_CMD% %JAVA_OPTS% %JAVA_CLASSPATH% %MAINCLASS% %*
