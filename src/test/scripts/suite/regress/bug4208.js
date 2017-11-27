/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// B.3.5 missing catch parameter duplicate bindings restriction from 13.14.1
// https://bugs.ecmascript.org/show_bug.cgi?id=4208

assertSyntaxError(`
  try {} catch ({e, e}) {}
`);
assertSyntaxError(`
  try {} catch ([e, e]) {}
`);
