/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

// String.prototype.search(null) causes TypeError, different from ES5
// https://bugs.ecmascript.org/show_bug.cgi?id=4327

assertEquals(Object.assign(["null"], {index: 0, input: "null"}), "null".match(null));
assertSame("ok", "null".replace(null, "ok"));
assertSame(0, "null".search(null));
assertEquals(["", ""], "null".split(null));

