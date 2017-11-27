/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: sec-function-calls-runtime-semantics-evaluation
info: TCO with cross-realm callers.
description: >
  ...

features: [tail-call-optimization, class, Proxy]
---*/

var code = "(class { constructor() { var p = Proxy.revocable(function(){}, {}); var f = p.proxy; p.revoke(); return f(); } });";

var otherRealm = $262.createRealm();
var tco = otherRealm.evalScript(code);

assert.throws(TypeError, function() {
  new tco();
});
