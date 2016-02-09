/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertSame
} = Assert;

// B.1.3 HTML-like Comments: Extension allowed in functions within modules ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4360

var a = -1;
var b = 3;

var result =
a
<!-- b
;
assertTrue(result);
assertSame(-1, a);
assertSame(2, b);

var fn = (p = a
<!-- b
) => { return p };

assertTrue(fn());
assertSame(-1, a);
assertSame(1, b);
