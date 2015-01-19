/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 21.2.2.10: Out of range unicode escapes in regexp
// https://bugs.ecmascript.org/show_bug.cgi?id=3440

/\u{10ffff}/u;

for (let cp of [0x110000, 0x110001]) {
  assertSyntaxError(`/\\u{${cp.toString(16)}}/u`);
}
