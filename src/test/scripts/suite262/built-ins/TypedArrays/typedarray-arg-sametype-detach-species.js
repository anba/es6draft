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

includes: [testTypedArray.js, detachArrayBuffer.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var species = function(){}.bind(null);
  Object.defineProperty(species, "prototype", {
    get: function() {
      throw new Test262Error("prototype property retrieved");
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

  assert.throws(TypeError, function() {
    new TA(sample);
  });
});
