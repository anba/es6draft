/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
info: Check TypeError is thrown when array buffer is detached after getting "constructor" property.
description: >
  ...

features: [Symbol.species, Proxy]
includes: [testTypedArray.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var getPrototype = 0;
  var species = function(){}.bind(null);
  Object.defineProperty(species, "prototype", {
    get: function() {
      getPrototype += 1;
      revocable.revoke();
    }
  });
  var revocable = Proxy.revocable(species, {});
  var constructor = {};
  constructor[Symbol.species] = revocable.proxy;

  var sample = new TA(8);
  sample.buffer.constructor = constructor;

  var otherTA = TA !== Int8Array ? Int8Array : Uint8Array;

  assert.throws(TypeError, function() {
    new otherTA(sample);
  });
  assert.sameValue(getPrototype, 1);
});
