/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 'super' works from arrow function
{
  class Base {
    array() { return [1,2,3] }
  }
  class Derived extends Base {
    array() { return () => super.array() }
  }
  assertEquals([1,2,3], (new Derived).array()());
}

// 'super' works from generator comprehension
{
  class Base {
    array() { return [1,2,3] }
  }
  class Derived extends Base {
    array() { return (for (x of super.array()) x) }
  }
  assertEquals([1,2,3], [...(new Derived).array()]);
}
