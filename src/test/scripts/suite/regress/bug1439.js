/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 11.13.1: AssignmentRestElement must restrict DestructuringAssignmentTarget to simple assignment targets
// https://bugs.ecmascript.org/show_bug.cgi?id=1439

assertSyntaxError(`[...[a]] = []`);
