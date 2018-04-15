/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertTrue, assertFalse
} = Assert;

// 15.10.6.2 (RegExpExec): Access to [[RegExpMatcher]] in step 2 is visible by non-standard RegExp.p.compile
// https://bugs.ecmascript.org/show_bug.cgi?id=1575

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
