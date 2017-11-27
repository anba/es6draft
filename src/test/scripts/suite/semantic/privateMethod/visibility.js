/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSyntaxError
} = Assert;

// `extends` expression doesn't have access to inner private methods.
assertSyntaxError(`
  class C extends (f = o => o.#m, Object) {
    #m() {}
  }
`);

// Private methods are visible to computed property keys. Access before declaration.
{
  function fn(o) {
    return class {
      [o.#m]() {}
      #m() {}
    };
  }
  assertThrows(ReferenceError, () => fn({}));
}

// Private methods are visible to computed property keys. Access after declaration.
{
  function fn(o) {
    return class {
      #m() {}
      [o.#m]() {}
    };
  }
  assertThrows(TypeError, () => fn({}));
}
