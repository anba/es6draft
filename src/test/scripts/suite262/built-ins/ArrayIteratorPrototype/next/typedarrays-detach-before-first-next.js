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

features: [TypedArray]
includes: [testTypedArray.js, detachArrayBuffer.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var typedArray = new TA(1);
  var iterator = typedArray[Symbol.iterator]();

  $DETACHBUFFER(typedArray.buffer);

  assert.throws(TypeError, function() {
    iterator.next();
  });
});
