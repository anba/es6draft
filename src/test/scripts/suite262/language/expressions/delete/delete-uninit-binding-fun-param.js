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

(function(x = (result = delete a), a) { })();

assert.sameValue(false, result);
