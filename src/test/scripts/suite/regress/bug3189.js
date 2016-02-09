/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertSame
} = Assert;

// 21.1.1.1 String: Handle reentrant initialization
// https://bugs.ecmascript.org/show_bug.cgi?id=3189

// var s = new class extends String { constructor() { /* no super */ } };
// assertThrows(TypeError, () => String.call(s, {toString(){ String.call(s, "A"); return "B"; }}));
// assertSame("A", s.toString());
