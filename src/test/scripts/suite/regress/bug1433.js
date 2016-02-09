/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
} = Assert;

// 15.5.4.11: String.prototype.replace calls function even if no match was found
// https://bugs.ecmascript.org/show_bug.cgi?id=1433

assertSame("aaa", "aaa".replace("bbb", () => { throw new Error }));

RegExp.prototype[Symbol.replace] = null;
assertSame("aaa", "aaa".replace(/aaa/, () => { throw new Error }));
assertSame("bbb", "/aaa/".replace(/aaa/, "bbb"));
