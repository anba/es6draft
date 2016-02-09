/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 11.2.4: Early error restriction for the 'super' keyword w.r.t. eval code contained in function code
// https://bugs.ecmascript.org/show_bug.cgi?id=1204

class Base {
  f() {
    return "base";
  }
}

class Derived extends Base {
  f() {
    return "@" + eval("super.f()");
    super.create_homeobject_entry;
  }
}

assertSame("@base", new Derived().f());
