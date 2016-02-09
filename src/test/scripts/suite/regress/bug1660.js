/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// 15.10.3.3, EscapeRegExpPattern: Line breaks and escape sequences not handled
// https://bugs.ecmascript.org/show_bug.cgi?id=1660

for (let lineTerminator of "\n\r\u2028\u2029") {
  let re = new RegExp(lineTerminator);
  assertTrue(re.test(lineTerminator));
  let copy = eval(`/${re.source}/`);
  assertTrue(copy.test(lineTerminator));
}

for (let lineTerminator of "\n\r\u2028\u2029") {
  let re = new RegExp("\\" + lineTerminator);
  assertTrue(re.test(lineTerminator));
  let copy = eval(`/${re.source}/`);
  assertTrue(copy.test(lineTerminator));
}
