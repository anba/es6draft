/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue
} = Assert;

// 21.2.2.8.2 Canonicalize: Non-unicode canonicalization not compatible with ES5
// https://bugs.ecmascript.org/show_bug.cgi?id=2256

assertFalse(/\u00df/i.test("\u1e9e"));
assertFalse(/\u1e9e/i.test("\u00df"));

assertTrue(/\u00df/iu.test("\u1e9e"));
assertTrue(/\u1e9e/iu.test("\u00df"));
