::
:: Copyright (c) André Bargull
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

:: Set classpath
set JAVA_CLASSPATH=-cp %CLASSPATH%

:: Configure JLine terminal settings
set JLINE_TERMINAL=windows
set JAVA_OPTS=%JAVA_OPTS% -Djline.terminal=%JLINE_TERMINAL%

:: Start application
%JAVA_CMD% %JAVA_OPTS% %JAVA_CLASSPATH% %MAINCLASS% %*
