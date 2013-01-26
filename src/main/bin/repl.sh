#!/bin/bash
#
# Copyright (c) 2012-2013 André Bargull
# Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
#
# <https://github.com/anba/es6draft>
#

#
# Description:
# Helper script to start the simple REPL
#

REL_PATH="$( dirname "$0" )"
BUILD_DIR="${REL_PATH}/../../../target"

CLASSES="${BUILD_DIR}/classes"
DEP_DIR="${BUILD_DIR}/dependencies"
DEPENDENCIES=`ls -1 "${DEP_DIR}" | sed 's,^,'"${DEP_DIR}"'/&,' | sed ':a;{N; s/\n/;/; ta}'`
MAINCLASS="com.github.anba.es6draft.repl.Repl"

java -ea -esa -Xbootclasspath/a:"${CLASSES};${DEPENDENCIES}" "${MAINCLASS}" "$@"
