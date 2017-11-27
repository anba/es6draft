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
    #private = super();
  }
`);

assertSyntaxError(`
  class C extends class {} {
    #private = super();
  }
`);

assertSyntaxError(`
  class Outer {
    class C {
      #private = super();
    }
  }
`);

assertSyntaxError(`
  class Outer extends class {} {
    class C {
      #private = super();
    }
  }
`);
