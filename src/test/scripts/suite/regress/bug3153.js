/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals, fail,
} = Assert;

// 21.1.3.* String.prototype.* : HasProperty(_ , @@isRegExp) → ToBoolean(Get(_, @@isRegExp))
// https://bugs.ecmascript.org/show_bug.cgi?id=3153

// String.prototype.match
{
  let regExpLike = {[Symbol.match](){ return ["B"]; }, toString(){ fail `unreachable`; }};
  assertEquals(["B"], "aba".match(regExpLike));

  let notRegExpLike = {[Symbol.match]: null, toString(){ return "a"; }};
  assertEquals(Object.assign(["a"], {index: 0, input: "aba"}), "aba".match(notRegExpLike));
}

// String.prototype.replace
{
  let regExpLike = {[Symbol.replace](){ return "B"; }, toString(){ fail `unreachable`; }};
  assertSame("B", "aba".replace(regExpLike, "A"));

  let notRegExpLike = {[Symbol.replace]: null, toString(){ return "a"; }};
  assertSame("Aba", "aba".replace(notRegExpLike, "A"));
}

// String.prototype.search
{
  let regExpLike = {[Symbol.search](){ return 1000; }, toString(){ fail `unreachable`; }};
  assertSame(1000, "aba".search(regExpLike));

  let notRegExpLike = {[Symbol.search]: null, toString(){ return "a"; }};
  assertSame(0, "aba".search(notRegExpLike));
}

// String.prototype.split
{
  let regExpLike = {[Symbol.split](){ return ["B"]; }, toString(){ fail `unreachable`; }};
  assertEquals(["B"], "aba".split(regExpLike));

  let notRegExpLike = {[Symbol.split]: null, toString(){ return "a"; }};
  assertEquals(["", "b", ""], "aba".split(notRegExpLike));
}


