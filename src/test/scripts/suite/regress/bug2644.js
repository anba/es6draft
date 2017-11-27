/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.2.14 Function Declaration Instantiation: "arguments" binding not checked when processing variable statements
// https://bugs.ecmascript.org/show_bug.cgi?id=2644

function f(y) {
  var arguments;
  return arguments;
}
assertSame("object", typeof f(0));
