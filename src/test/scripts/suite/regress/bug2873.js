/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// B.3.2 Web Block Function Decl: Redeclaration checks when parameter environment is present
// https://bugs.ecmascript.org/show_bug.cgi?id=2873

// Parameter environment present
{
  function f(a = () => "default") {
    if (true) { function a() { return "web-compat" } }
    return a();
  }
  assertSame("web-compat", f());
  assertSame("web-compat", f(() => "param"));
}

// Parameter environment not present
{
  function f(a) {
    if (true) { function a() { return "web-compat" } }
    return a();
  }
  assertSame("web-compat", f());
  assertSame("web-compat", f(() => "param"));
}
