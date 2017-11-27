/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSyntaxError, fail
} = Assert;

class Err extends Error {}
function throwError() {
  throw new Err();
}

assertSyntaxError(`null.#unresolvable`);
assertSyntaxError(`(void 0).#unresolvable`);
assertSyntaxError(`({}).#unresolvable`);
assertSyntaxError(`1..#unresolvable`);

assertSyntaxError(`null.#unresolvable = 0`);
assertSyntaxError(`(void 0).#unresolvable = 0`);
assertSyntaxError(`({}).#unresolvable = 0`);
assertSyntaxError(`1..#unresolvable = 0`);

assertSyntaxError(`null.#unresolvable = throwError()`);
assertSyntaxError(`(void 0).#unresolvable = throwError()`);
assertSyntaxError(`({}).#unresolvable = throwError()`);
assertSyntaxError(`1..#unresolvable = throwError()`);


var lookup = new Proxy({}, new Proxy({}, {
  get(t, pk) {
    fail `trap:${pk} invoked`;
  }
}));
assertSyntaxError(`
  with (lookup) { ({}).#unresolvable; }
`);
assertSyntaxError(`
  with (lookup) { ({}).#unresolvable = 0; }
`);
assertSyntaxError(`
  with (lookup) { ({}).#unresolvable = [](); }
`);
