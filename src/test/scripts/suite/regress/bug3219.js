/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// Remove duplicate check for constructor
// https://bugs.ecmascript.org/show_bug.cgi?id=3219

assertSyntaxError(`
  class C {
    constructor(){}
    constructor(){}
  }
`);

class ComputedConstructor {
  ["constructor"]() {
    return 123;
  }
}
assertSame(123, new ComputedConstructor().constructor());

class StaticComputedConstructor {
  static ["constructor"]() {
    return 123;
  }
}
assertSame(123, StaticComputedConstructor.constructor());

class StaticLiteralConstructor {
  static constructor() {
    return 123;
  }
}
assertSame(123, StaticLiteralConstructor.constructor());
