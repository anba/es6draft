/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 9.2.13 Function Declaration Instantiation: Set needsSpecialArgumentsBinding=false when "arguments" is a lexical name
// https://bugs.ecmascript.org/show_bug.cgi?id=2958

function f() {
  let arguments;
  assertUndefined(arguments);
}
f()
