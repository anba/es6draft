/*
 * Copyright (c) 2012-2015 André Bargull
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

// step 4: RegExp check uses Symbol.match
{
  class MyRegExp {
    get [Symbol.match]() { return true }
  }
  assertThrows(TypeError, () => "".startsWith(/./));
  assertThrows(TypeError, () => "".startsWith(new MyRegExp));

  let isRegExpDesc = Object.getOwnPropertyDescriptor(RegExp.prototype, Symbol.match);

  // Remove `RegExp.prototype[Symbol.match]`, IsRegExp proceeds by checking [[RegExpMatcher]]
  try {
    delete RegExp.prototype[Symbol.match];
    assertThrows(TypeError, () => "".startsWith(/./));
  } finally {
    Object.defineProperty(RegExp.prototype, Symbol.match, isRegExpDesc);
  }

  // Set `RegExp.prototype[Symbol.match]` to falsy value
  try {
    RegExp.prototype[Symbol.match] = false;
    assertTrue("/./".startsWith(/./));
    assertTrue("aa/./".startsWith(/./, 2));
  } finally {
    Object.defineProperty(RegExp.prototype, Symbol.match, isRegExpDesc);
  }
}
