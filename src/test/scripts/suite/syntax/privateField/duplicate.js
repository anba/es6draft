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
  class C {
    #private;
    #private;
  }
`);

assertSyntaxError(String.raw`
  class D {
    #private;
    #priv\u0061te;
  }
`);
