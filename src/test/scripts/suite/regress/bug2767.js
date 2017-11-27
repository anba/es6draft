/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 12.14.5.2 DestructuringAssignmentEvaluation: Missing call to GetValue in step 4
// https://bugs.ecmascript.org/show_bug.cgi?id=2767

var log = "";
var o = {
  get a() { log += "|get-a"; },
  set a(x) { log += "|set-a"; },
  get b() { log += "|get-b"; },
};

with (o) {
  ({a = b} = o);
}
assertSame("|get-a|get-b|set-a", log);
