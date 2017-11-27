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

features: [Symbol.species]
includes: [testTypedArray.js, detachArrayBuffer.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var getPrototype = 0;
  var species = function(){}.bind(null);
  Object.defineProperty(species, "prototype", {
    get: function() {
      getPrototype += 1;
    }
  });
  var constructor = {};
  constructor[Symbol.species] = species;

  var sample = new TA(8);

  var sampleBuffer = sample.buffer;
  Object.defineProperty(sampleBuffer, "constructor", {
    get: function() {
      $DETACHBUFFER(sampleBuffer);
      return constructor;
    }
  });
  var otherTA = TA !== Int8Array ? Int8Array : Uint8Array;

  assert.throws(TypeError, function() {
    new otherTA(sample);
  });
  assert.sameValue(getPrototype, 1);
});
