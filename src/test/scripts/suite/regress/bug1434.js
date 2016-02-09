/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 15.10.2.1: "ignoreCase" and "multiline" no longer properties on RegExp objects
// https://bugs.ecmascript.org/show_bug.cgi?id=1434

let r = /(?:)/;
assertUndefined(Object.getOwnPropertyDescriptor(r, "global"));
assertUndefined(Object.getOwnPropertyDescriptor(r, "ignoreCase"));
assertUndefined(Object.getOwnPropertyDescriptor(r, "multiline"));
assertUndefined(Object.getOwnPropertyDescriptor(r, "source"));
assertUndefined(Object.getOwnPropertyDescriptor(r, "sticky"));
assertUndefined(Object.getOwnPropertyDescriptor(r, "unicode"));
