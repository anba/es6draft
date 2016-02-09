/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame
} = Assert;

// 9.2.3 [[Construct]]: Disallow tail calls in class constructors
// https://bugs.ecmascript.org/show_bug.cgi?id=3930

function returnCaller() {
  return returnCaller.caller;
}

class Base {
  constructor() {
    return returnCaller();
  }
}

class Derived extends Base {
  constructor() {
    super();
    return returnCaller();
  }
}

class DerivedWithSuper extends Base {
  constructor() {
    return super();
  }
}

class DerivedWithSuperArrow extends Base {
  constructor() {
    return (() => super())();
  }
}

function F(clazz) {
  return new clazz();
}

assertSame(F, F(Base));
assertSame(F, F(Derived));
assertNotSame(F, F(DerivedWithSuper));
assertNotSame(F, F(DerivedWithSuperArrow));
