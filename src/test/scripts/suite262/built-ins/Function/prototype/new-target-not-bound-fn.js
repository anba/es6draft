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
features: [Reflect]
---*/

function f() { return new.target; }
function g() { }
var bf = f.bind();

assert.sameValue(Reflect.construct(bf, [], g), g);
