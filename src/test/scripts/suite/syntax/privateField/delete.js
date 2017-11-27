/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

assertSyntaxError(`
  delete o.#private;
`);

assertSyntaxError(`
  class C {
    #private;

    m() {
      delete o.#private;
    }
  }
`);

assertSyntaxError(`
  class C {
    #private;

    m() {
      delete this.#private;
    }
  }
`);
