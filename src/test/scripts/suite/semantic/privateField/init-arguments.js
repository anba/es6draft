/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError, assertThrows
} = Assert;

assertSyntaxError(`
function f() {
  class C {
    #private = arguments;
    get() { return this.#private; }
  }
  return new C();
}
`);

function g() {
  class C {
    #private = eval("arguments");
    get() { return this.#private; }
  }
  assertThrows(SyntaxError, () => new C());
}
g();
