/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Elison not allowed in ArrayBindingPattern and ArrayAssignmentPattern after RestElement
// https://bugs.ecmascript.org/show_bug.cgi?id=4098

assertSyntaxError(`[...a,] = []`);
assertSyntaxError(`var [...a,] = []`);
assertSyntaxError(`let [...a,] = []`);
assertSyntaxError(`const [...a,] = []`);
