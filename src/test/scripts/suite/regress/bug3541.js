/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertThrows
} = Assert;

// 21.1.3.17 String.prototype.split: Handle `undefined` separator
// https://bugs.ecmascript.org/show_bug.cgi?id=3541


// String.prototype.split
assertEquals(["abc"], "abc".split());
assertEquals(["abc"], "abc".split(void 0));
assertThrows(TypeError, () => "abc".split(null));
assertEquals([], "abc".split(void 0, 0));
assertThrows(TypeError, () => "abc".split(null, 0));

// String.prototype.match
assertEquals(Object.assign([""], {index: 0, input: "abc"}), "abc".match());
assertEquals(Object.assign([""], {index: 0, input: "abc"}), "abc".match(void 0));
assertThrows(TypeError, () => "abc".match(null));

// String.prototype.search
assertSame(0, "abc".search());
assertSame(0, "abc".search(void 0));
assertThrows(TypeError, () => "abc".search(null));

// String.prototype.replace
assertSame("abc", "abc".replace());
assertSame("abc", "abc".replace(void 0));
assertThrows(TypeError, () => "abc".replace(null));
assertSame("", "undefined".replace(void 0, ""));
