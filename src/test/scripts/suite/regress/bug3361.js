/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.14.5: Non-simple destructuring target in rest-destructuring (and typo)
// https://bugs.ecmascript.org/show_bug.cgi?id=3361

// /dev/null for iterators
var log = "";
[...[]] = function*() { for (var c of "abc") { log += c; yield c } }()
assertSame("abc", log);
