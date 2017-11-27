/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertSame, assertNull
} = Assert;

// B.3.1 Incorrect definition of invalid duplicate __proto__ properties
// https://bugs.ecmascript.org/show_bug.cgi?id=3510

assertSyntaxError(`({__proto__: a, __proto__: b})`);
assertSyntaxError(`({__proto__: a, "__proto__": b})`);
assertSyntaxError(`({"__proto__": a, __proto__: b})`);
assertSyntaxError(`({"__proto__": a, "__proto__": b})`);

{
  let o1 = {__proto__: null, ["__proto__"]: 123};
  assertSame(123, o1.__proto__);
  assertNull(Object.getPrototypeOf(o1));

  let o2 = {["__proto__"]: 123, __proto__: null};
  assertSame(123, o2.__proto__);
  assertNull(Object.getPrototypeOf(o2));
}

{
  let __proto__ = 123;

  let o1 = {__proto__: null, __proto__};
  assertSame(123, o1.__proto__);
  assertNull(Object.getPrototypeOf(o1));

  let o2 = {__proto__, __proto__: null};
  assertSame(123, o2.__proto__);
  assertNull(Object.getPrototypeOf(o2));
}

{
  let o1 = {__proto__: null, __proto__(){ return 123 }};
  assertSame(123, o1.__proto__());
  assertNull(Object.getPrototypeOf(o1));

  let o2 = {__proto__(){ return 123 }, __proto__: null};
  assertSame(123, o2.__proto__());
  assertNull(Object.getPrototypeOf(o2));
}
