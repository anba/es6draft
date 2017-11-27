/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError,
} = Assert;

// super Arguments in methods?
// https://bugs.ecmascript.org/show_bug.cgi?id=3284

assertSyntaxError(`
class C {
  method() {
    new super();
  }
}
`);

assertSyntaxError(`
class C {
  method() {
    super();
  }
}
`);

assertSyntaxError(`function fdecl() { super(); }`);

assertSyntaxError(`(function fexpr() { super(); });`);

assertSyntaxError(`function* gdecl() { super(); }`);

assertSyntaxError(`(function* gexpr() { super(); });`);
