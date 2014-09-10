/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertInstanceOf, assertSame, assertTrue, assertThrows
} = Assert;

for (let sym of [Symbol.create, Symbol(), Symbol.for("uid")]) {
  // ToPrimitive
  assertSame(sym, Symbol.prototype.valueOf.call(sym));
  assertSame(sym, Symbol.prototype[Symbol.toPrimitive].call(sym));
  assertSame(sym, Symbol.prototype.valueOf.call(Object(sym)));
  assertSame(sym, Symbol.prototype[Symbol.toPrimitive].call(Object(sym)));

  // ToBoolean
  assertTrue(!!sym);

  // ToNumber
  assertThrows(TypeError, () => Number(sym));
  assertThrows(TypeError, () => (+sym));
  assertThrows(TypeError, () => (~~sym));
  assertThrows(TypeError, () => (sym | 0));
  assertThrows(TypeError, () => (sym < 0));
  assertThrows(TypeError, () => (sym < ""));

  // ToString
  assertSame(Symbol.prototype.toString.call(sym), String(sym));
  assertThrows(TypeError, () => ("" + sym));
  assertThrows(TypeError, () => (sym + ""));
  assertThrows(TypeError, () => ("abc" + sym));
  assertThrows(TypeError, () => (sym + "abc"));

  // ToObject
  assertInstanceOf(Symbol, Object(sym));

  // Equality Comparison
  assertTrue(sym == sym);
  assertFalse(sym != sym);
  for (let v of [Symbol(), null, void 0, false, true, 0, 1, "", "abc", () => {}, {}]) {
    assertFalse(sym == v);
    assertTrue(sym != v);
  }
}
