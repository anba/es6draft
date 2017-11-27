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
features: [Proxy]
includes: [proxyTrapsHelper.js]
---*/

var bf = Function.prototype.bind();

assert.sameValue(typeof bf, "function", "Bound function is a callable object");

var p = new Proxy(bf, allowProxyTraps(null));
assert.throws(TypeError, function() {
  new p();
}, "Bound function is not a constructor");
