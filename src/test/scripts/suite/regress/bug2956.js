/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 21.1.3.14 String.prototype.replace: Apply ToString on replaceValue even if no match was found
// https://bugs.ecmascript.org/show_bug.cgi?id=2956

class MyError extends Error {}
assertThrows(MyError, () => "a".replace("b", {toString(){ throw new MyError }}));
assertThrows(MyError, () => "a".replace(/b/, {toString(){ throw new MyError }}));
