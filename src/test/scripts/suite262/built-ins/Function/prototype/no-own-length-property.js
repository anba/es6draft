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
---*/

// Modify Function.prototype.length to ensure its value is not used as a fallback.
Object.defineProperty(Function.prototype, "length", {value: 5});

function f(a, b, c) {}
delete f.length;

var bf = f.bind();

assert.sameValue(bf.length, 0);
