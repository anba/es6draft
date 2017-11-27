/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// duplicate bindings allowed in CatchParameter
// https://bugs.ecmascript.org/show_bug.cgi?id=4087

assertSyntaxError(`
  try {} catch ({e, e}) {}
`);

assertSyntaxError(`
  try {} catch ({a: {e}, b: {e}}) {}
`);

assertSyntaxError(`
  try {} catch ({a: [e], b: [e]}) {}
`);

assertSyntaxError(`
  try {} catch ([e, e]) {}
`);

assertSyntaxError(`
  try {} catch ([{e}, {e}) {}
`);

assertSyntaxError(`
  try {} catch ([{e}, {d: e}) {}
`);
