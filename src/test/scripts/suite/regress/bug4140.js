/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// class definitions: computed property name can be "constructor"
// https://bugs.ecmascript.org/show_bug.cgi?id=4140

class C1 {
  ["constructor"]() {
    return "constructor";
  }
}
assertSame("constructor", C1.prototype.constructor());

class C2 {
  get ["constructor"]() {
    return "constructor";
  }
}
assertSame("constructor", C2.prototype.constructor);

class C3 {
  set ["constructor"](x) {
    return "constructor";
  }
}
assertSame("constructor", Object.getOwnPropertyDescriptor(C3.prototype, "constructor").set());

class C4 {
  *["constructor"]() {
    return "constructor";
  }
}
assertSame("constructor", C4.prototype.constructor().next().value);

class C5 {
  static ["constructor"]() {
    return "constructor";
  }
}
assertSame("constructor", C5.constructor());

class C6 {
  static get ["constructor"]() {
    return "constructor";
  }
}
assertSame("constructor", C6.constructor);

class C7 {
  static set ["constructor"](x) {
    return "constructor";
  }
}
assertSame("constructor", Object.getOwnPropertyDescriptor(C7, "constructor").set());

class C8 {
  static *["constructor"]() {
    return "constructor";
  }
}
assertSame("constructor", C8.constructor().next().value);
