/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertTrue, assertFalse
} = Assert;

// 21.2.5.2.1 (RegExpExec): Access to [[RegExpMatcher]] in step 4 is visible by non-standard RegExp.p.compile
// https://bugs.ecmascript.org/show_bug.cgi?id=2492

let called = false;
let r = /a/g;
r.lastIndex = {
  valueOf() {
    assertFalse(called);
    called = true;
    r.compile("b", "");
  }
};
let result = r.exec("b")

assertTrue(called);
assertEquals(Object.assign(["b"], {input: "b", index: 0, groups: undefined}), result);
