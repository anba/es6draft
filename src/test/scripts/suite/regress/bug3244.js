/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 13.6.4.9 ForIn/OfBodyEvaluation: Skip IteratorClose in for-in statement loop?
// https://bugs.ecmascript.org/show_bug.cgi?id=3244

var log;
Object.defineProperty(Object.prototype, "return", {
  value() {
    log += "|return";
    return {value: void 0, done: true};
  }, configurable: true
});

log = "";
for (var k in {k: 0}) {
  log += "body";
}
assertSame("body", log);

log = "";
for (var k in {k: 0}) {
  log += "body";
  continue;
}
assertSame("body", log);

log = "";
for (var k in {k: 0}) {
  log += "body";
  break;
}
assertSame("body", log);

log = "";
(function() {
  for (var k in {k: 0}) {
    log += "body";
    return;
  }
})();
assertSame("body", log);

log = "";
try {
  for (var k in {k: 0}) {
    log += "body";
    throw null;
  }
} catch (e) { }
assertSame("body", log);
