/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertSame
} = Assert;

// 15.10.6.11/15.10.6.12: RegExp.prototype.{match, replace} algorithm is wrong for empty matches
// https://bugs.ecmascript.org/show_bug.cgi?id=1467

assertEquals(["", ""], /^|$/g[Symbol.match]("aa"));
assertEquals(["", "", "", ""], /\b/g[Symbol.match]("aa aa"));

assertSame("baab", /^|$/g[Symbol.replace]("aa", "b"));
assertSame("baab baab", /\b/g[Symbol.replace]("aa aa", "b"))
assertSame("bbaabb", /^|$/g[Symbol.replace]("aa", "bb"));
