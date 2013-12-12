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

set REL_PATH=%~dp0
set BUILD_DIR=%REL_PATH%\..\target
set CLASSPATH=%BUILD_DIR%\es6draft.jar
set MAINCLASS=com.github.anba.es6draft.repl.Repl

if not defined JAVA_HOME ( set JAVA_CMD=java ) else ( set JAVA_CMD=%JAVA_HOME%\bin\java )

set JAVA_OPTS=%JAVA_OPTS% -ea -esa -server -XX:+TieredCompilation
set JAVA_CLASSPATH=-Xbootclasspath/a:%CLASSPATH%

%JAVA_CMD% %JAVA_OPTS% %JAVA_CLASSPATH% %MAINCLASS% %*
