/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertUndefined, assertThrows
} = Assert;

// 23.4.3.5: Add type check for input argument in WeakSet.prototype.has
// https://bugs.ecmascript.org/show_bug.cgi?id=1938

const primitives = [void 0, null, +0, -0, 1, -1, 1.5, -1.5, Infinity, -Infinity, NaN, true, false, "", "abc", Symbol()];

let wm = new WeakMap();
for (let key of primitives) {
  assertFalse(wm.delete(key));
  assertFalse(wm.has(key));
  assertUndefined(wm.get(key));
  assertThrows(() => wm.set(key, null), TypeError);
}

let ws = new WeakSet();
for (let key of primitives) {
  assertFalse(ws.delete(key));
  assertFalse(ws.has(key));
  assertThrows(() => ws.add(key), TypeError);
}
