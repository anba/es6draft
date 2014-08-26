/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

%Include("lib/assert.js");

assertEq(%IsGenerator(function*(){}()), true);
assertEq(%IsGenerator((for (x of []) x)), true);

assertEq(%IsGenerator(function*(){}), false);
assertEq(%IsGenerator({}), false);
assertEq(%IsGenerator(void 0), false);
assertEq(%IsGenerator(null), false);
assertEq(%IsGenerator(1), false);
