/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
info: Deleteing an uninitialized binding does not throw a ReferenceError
description: >
  ...
flags: [noStrict]
---*/

var result;

(function() {
  result = delete a;

  const a = 0;
})();

assert.sameValue(false, result);
