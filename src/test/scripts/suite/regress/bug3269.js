/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 22.2.3.* %TypedArray%: toLocaleString and iterator methods currently use Get("length") instead of [[ArrayLength]]
// https://bugs.ecmascript.org/show_bug.cgi?id=3269

// Typed array with own "length" property
var ta = Object.defineProperty(new Int8Array([1,2,3]), "length", {
  value: 1, writable: true, enumerable: false, configurable: true
});

assertSame("1,2,3", ta.toString());
assertSame("1", ta.toLocaleString());

assertSame("1,4,9", ta.map(v => v * v).toString());
assertSame("1", Int8Array.from(ta, v => v * v).toString());
