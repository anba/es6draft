/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Annex C, eval and arguments as class names
// https://bugs.ecmascript.org/show_bug.cgi?id=3218

assertSyntaxError(`class eval {}`);
assertSyntaxError(`(class eval {});`);

assertSyntaxError(`class arguments {}`);
assertSyntaxError(`(class arguments {});`);
