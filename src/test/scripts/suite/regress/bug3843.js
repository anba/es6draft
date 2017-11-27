/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// Make 'super()' in non-derived constructors an early error
// https://bugs.ecmascript.org/show_bug.cgi?id=3843

assertSyntaxError(`
  class C {
    constructor() {
      super();
    }
  }
`);

class C extends Object {
  constructor() {
    super();
  }
}
