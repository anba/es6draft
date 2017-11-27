/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
info: ToPropertyKey is called before ToObject
description: >
  ...
---*/

assert.sameValue(Object.prototype.isPrototypeOf.call(undefined, null), false);

assert.sameValue(Object.prototype.isPrototypeOf.call(null, null), false);
