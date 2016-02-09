/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertThrows, assertNull
} = Assert;

// 21.1.3.17 String.prototype.split: Handle `undefined` separator
// https://bugs.ecmascript.org/show_bug.cgi?id=3541


// String.prototype.split
assertEquals(["abc"], "abc".split());
assertEquals(["abc"], "abc".split(void 0));
assertEquals(["abc"], "abc".split(null));
assertEquals(["undefined"], "undefined".split(void 0));
assertEquals(["", ""], "null".split(null));
assertEquals([], "abc".split(void 0, 0));
assertEquals([], "abc".split(null, 0));
assertEquals([], "undefined".split(void 0, 0));
assertEquals([], "null".split(null, 0));

// String.prototype.match
assertEquals(Object.assign([""], {index: 0, input: "abc"}), "abc".match());
assertEquals(Object.assign([""], {index: 0, input: "abc"}), "abc".match(void 0));
assertNull("abc".match(null));
assertEquals(Object.assign([""], {index: 0, input: "undefined"}), "undefined".match(void 0));
assertEquals(Object.assign(["null"], {index: 0, input: "null"}), "null".match(null));

// String.prototype.search
assertSame(0, "abc".search());
assertSame(0, "abc".search(void 0));
assertSame(-1, "abc".search(null));
assertSame(0, "undefined".search(void 0));
assertSame(0, "null".search(null));

// String.prototype.replace
assertSame("abc", "abc".replace());
assertSame("abc", "abc".replace(void 0));
assertSame("abc", "abc".replace(null));
assertSame("", "undefined".replace(void 0, ""));
assertSame("", "null".replace(null, ""));
