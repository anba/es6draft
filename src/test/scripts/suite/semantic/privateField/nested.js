/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

class ClassWithNested {
  #private = "class-with-nested";
  m(obj) { return obj.#private; }

  static test() {
    class Nested {
      m(obj) { return obj.#private; }
    }
    let obj = new ClassWithNested();

    assertSame("class-with-nested", ClassWithNested.prototype.m(obj));
    assertSame("class-with-nested", Nested.prototype.m(obj));
  }
}

ClassWithNested.test();

class Outer {
  #private = "outer";
  m(obj) { return obj.#private; }

  static test() {
    class Inner {
      #private = "inner";
      m(obj) { return obj.#private; }
    }
    let outer = new Outer();
    let inner = new Inner();

    assertSame("outer", Outer.prototype.m(outer));
    assertThrows(TypeError, () => Outer.prototype.m(inner));

    assertThrows(TypeError, () => Inner.prototype.m(outer));
    assertSame("inner", Inner.prototype.m(inner));
  }
}

Outer.test();
