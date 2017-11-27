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

var obj = {};

assert.throws(TypeError, function() {
  Object.setPrototypeOf(obj, obj);
});
