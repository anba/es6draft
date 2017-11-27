/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertThrows,
} = Assert;

// 23.2.1  The Set Constructor

// undefined and null are ignored
new Set(void 0);
new Set(null);

// TypeError for non-iterable
for (let nonIterable of [0, 1, 0.5, 0 / 0, true, false, Symbol.iterator, Symbol()]) {
  assertThrows(TypeError, () => new Set(nonIterable));
}
for (let nonIterable of [{}, {a: 0}, /(?:)/, new Date, () => {}]) {
  assertThrows(TypeError, () => new Set(nonIterable));
}

// No TypeError for iterable
for (let iterable of [[], [0, 1], function*(){}(), "", "abc"]) {
  new Set(iterable);
}
