/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

let ta = new Int8Array([1, 2, 3]);
let p = Object.getPrototypeOf(ta);
for (let i = 0; i < 20; ++i) p = Object.create(p);
Object.setPrototypeOf(ta, p);

// No crash
let a = [...ta];

assertEquals([1, 2, 3], a);
