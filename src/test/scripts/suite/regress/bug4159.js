/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

// 21.2.5.* RegExp algorithm: in several places, in case of unicode-matching, index should be conditionally incremented by more than 1
// https://bugs.ecmascript.org/show_bug.cgi?id=4159

assertEquals(["", ""], /\b|\B/g[Symbol.match]("a"));
assertEquals(["", ""], /\b|\B/g[Symbol.match]("\u{ffff}"));
assertEquals(["", "", ""], /\b|\B/g[Symbol.match]("\u{10000}"));
assertEquals(["", "", ""], /\b|\B/g[Symbol.match]("\u{10ffff}"));

assertEquals(["", ""], /\b|\B/gu[Symbol.match]("a"));
assertEquals(["", ""], /\b|\B/gu[Symbol.match]("\u{ffff}"));
assertEquals(["", ""], /\b|\B/gu[Symbol.match]("\u{10000}"));
assertEquals(["", ""], /\b|\B/gu[Symbol.match]("\u{10ffff}"));


assertSame("_a_", /\b|\B/g[Symbol.replace]("a", "_"));
assertSame("_\u{ffff}_", /\b|\B/g[Symbol.replace]("\u{ffff}", "_"));
assertSame("_\u{d800}_\u{dc00}_", /\b|\B/g[Symbol.replace]("\u{10000}", "_"));
assertSame("_\u{dbff}_\u{dfff}_", /\b|\B/g[Symbol.replace]("\u{10ffff}", "_"));

assertSame("_a_", /\b|\B/gu[Symbol.replace]("a", "_"));
assertSame("_\u{ffff}_", /\b|\B/gu[Symbol.replace]("\u{ffff}", "_"));
assertSame("_\u{10000}_", /\b|\B/gu[Symbol.replace]("\u{10000}", "_"));
assertSame("_\u{10ffff}_", /\b|\B/gu[Symbol.replace]("\u{10ffff}", "_"));


assertEquals(["a"], /\b|\B/g[Symbol.split]("a"));
assertEquals(["\u{ffff}"], /\b|\B/g[Symbol.split]("\u{ffff}"));
assertEquals(["\u{d800}", "\u{dc00}"], /\b|\B/g[Symbol.split]("\u{10000}"));
assertEquals(["\u{dbff}", "\u{dfff}"], /\b|\B/g[Symbol.split]("\u{10ffff}"));

assertEquals(["a"], /\b|\B/gu[Symbol.split]("a"));
assertEquals(["\u{ffff}"], /\b|\B/gu[Symbol.split]("\u{ffff}"));
assertEquals(["\u{10000}"], /\b|\B/gu[Symbol.split]("\u{10000}"));
assertEquals(["\u{10ffff}"], /\b|\B/gu[Symbol.split]("\u{10ffff}"));
