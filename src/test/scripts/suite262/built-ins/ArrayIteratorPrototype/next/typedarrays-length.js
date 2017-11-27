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
includes: [testTypedArray.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var typedArray = new TA(1);
  Object.defineProperty(typedArray, "length", {
    get: function() {
      $ERROR("'length' getter accessed");
    }
  });

  var iterator = typedArray[Symbol.iterator](), result;

  result = iterator.next();
  assert.sameValue(result.value, 0, 'first result `value`');
  assert.sameValue(result.done, false, 'first result `done` flag');

  result = iterator.next();
  assert.sameValue(result.value, undefined, 'exhausted result `value`');
  assert.sameValue(result.done, true, 'exhausted result `done` flag');
});
