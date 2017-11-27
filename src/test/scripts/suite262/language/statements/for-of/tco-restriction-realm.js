/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: ...
info: Tail-call restriction in for-of statements
description: >
  ...

features: [tail-call-optimization]
---*/

var code = "'use strict'; (function() { for (var v of [null]) return (class {})(); });";

var otherRealm = $262.createRealm();
var tco = otherRealm.evalScript(code);

assert.throws(otherRealm.global.TypeError, function() {
  tco();
});
