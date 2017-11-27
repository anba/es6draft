/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
info: ...
description: >
  ...

includes: [testTypedArray.js, detachArrayBuffer.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var sample = new TA(0);
  $DETACHBUFFER(sample.buffer);

  assert.throws(TypeError, function() {
    sample.Infinity;
  });
});
