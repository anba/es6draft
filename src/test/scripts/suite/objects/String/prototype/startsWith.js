/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertThrows,
  assertTrue,
} = Assert;

// step 1: RequireObjectCoercible()
{
  assertThrows(TypeError, () => String.prototype.startsWith.call(null));
  assertThrows(TypeError, () => String.prototype.startsWith.call(void 0));
}

// steps 2-4: ToString() and then RegExp check
{
  class XError extends Error { }
  let noToString = new class {
    toString() { throw new XError }
  };
  assertThrows(XError, () => String.prototype.startsWith.call(noToString, /./));
}

// step 4: RegExp check uses Symbol.isRegExp
{
  class MyRegExp {
    get [Symbol.isRegExp]() { return true }
  }
  assertThrows(TypeError, () => "".startsWith(/./));
  assertThrows(TypeError, () => "".startsWith(new MyRegExp));

  // temporarily remove `RegExp.prototype[Symbol.isRegExp]`
  let isRegExpDesc = Object.getOwnPropertyDescriptor(RegExp.prototype, Symbol.isRegExp);
  try {
    delete RegExp.prototype[Symbol.isRegExp];
    assertTrue("/./".startsWith(/./));
    assertTrue("aa/./".startsWith(/./, 2));
  } finally {
    Object.defineProperty(RegExp.prototype, Symbol.isRegExp, isRegExpDesc);
  }
}
