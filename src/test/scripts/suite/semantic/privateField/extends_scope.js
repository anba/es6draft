/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// `extends` expression doesn't have access to inner private fields.
assertSyntaxError(`
class C extends (f = o => o.#a, Object) {
  #a = 123;
}
`);
