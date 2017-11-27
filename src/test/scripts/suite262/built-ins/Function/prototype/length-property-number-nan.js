/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
description: ...
info: >
  ...
features: [Symbol]
---*/

// Modify Function.prototype.length to ensure its value is not used as a fallback.
Object.defineProperty(Function.prototype, "length", {value: 5});

function f(a, b, c) {}
var bf;

Object.defineProperty(f, "length", {value: NaN});
bf = f.bind();
assert.sameValue(bf.length, 0);
