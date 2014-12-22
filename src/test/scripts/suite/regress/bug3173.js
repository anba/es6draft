/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSyntaxError
} = Assert;

// 12.14.5.3 IteratorDestructuringAssignmentEvaluation: Unreachable step 5 for AssignmentRestElement
// https://bugs.ecmascript.org/show_bug.cgi?id=3173

[...[]] = [];
[...[a]] = [];
[...{}] = [];
[...{a}] = [];
[...{a: b}] = [];
