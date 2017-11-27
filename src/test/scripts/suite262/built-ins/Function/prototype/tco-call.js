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

function target() {
  return "pass";
}
var bound = target.bind();

function tcoCall() {
  "use strict";
  return bound();
}

assert.sameValue(tcoCall(), "pass");
