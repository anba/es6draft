/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, assertTrue, fail,
} = Assert;

// 21.1.3.* String.prototype.{contains, endsWith, startsWith}: If isRegExp is not true → is true
// https://bugs.ecmascript.org/show_bug.cgi?id=3152

let regExpLike = {[Symbol.isRegExp]: true, toString(){ fail `unreachable` }};
assertThrows(TypeError, () => "aba".contains(regExpLike));
assertThrows(TypeError, () => "aba".startsWith(regExpLike));
assertThrows(TypeError, () => "aba".endsWith(regExpLike));

let notRegExpLike = {[Symbol.isRegExp]: false, toString(){ return "a"; }};
assertTrue("aba".contains(notRegExpLike));
assertTrue("aba".startsWith(notRegExpLike));
assertTrue("aba".endsWith(notRegExpLike));
