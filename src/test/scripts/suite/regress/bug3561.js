/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.3.5.1, SuperCall : super Argument
// https://bugs.ecmascript.org/show_bug.cgi?id=3561

class Base {
}

class Derived extends Base {
  constructor() {
    super();
  }
}

new Derived; // no error
