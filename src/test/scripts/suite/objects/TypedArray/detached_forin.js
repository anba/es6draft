/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, assertEquals, fail
} = Assert;

// Property keys enumeration on detached typed arrays

assertThrows(TypeError, () => {
  var ta = new Int8Array(2);
  detachArrayBuffer(ta.buffer);
  for (var k in ta) {
    fail `unreachable`;
  }
  fail `unreachable`;
});


var log = [];
assertThrows(TypeError, () => {
  var ta = new Int8Array(2);
  for (var k in ta) {
    log.push(k);
    if (k === "0") detachArrayBuffer(ta.buffer);
  }
  fail `unreachable`;
});
assertEquals(["0"], log);


var log = [];
var ta = new Int8Array(2);
for (var k in ta) {
  log.push(k);
  if (k === "1") detachArrayBuffer(ta.buffer);
}
assertEquals(["0", "1"], log);
