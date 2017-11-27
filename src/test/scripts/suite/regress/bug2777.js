/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 21.2.5.2.2 RegExpBuiltinExec: Captured group may be undefined
// https://bugs.ecmascript.org/show_bug.cgi?id=2777

assertEquals(Object.assign(["b", void 0, "b"], {index: 0, input: "b"}), /(a)?(b)/.exec("b"));
