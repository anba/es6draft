/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-%arrayiteratorprototype%.next
description: |
  ...
info: >
    22.1.5.2.1 %ArrayIteratorPrototype%.next( )

    ...
    4. Let a be O.[[IteratedObject]].
    ...
    8. If a has a [[TypedArrayName]] internal slot, then
      a. If IsDetachedBuffer(a.[[ViewedArrayBuffer]]) is true, throw a TypeError exception.
      ...

features: [TypedArray]
includes: [testTypedArray.js, detachArrayBuffer.js]
---*/

testWithTypedArrayConstructors(function(TA) {
  var typedArray = new TA(1);
  var iterator = typedArray[Symbol.iterator](), result;

  result = iterator.next();
  assert.sameValue(result.value, 0, 'first result `value`');
  assert.sameValue(result.done, false, 'first result `done` flag');

  $DETACHBUFFER(typedArray.buffer);

  assert.throws(TypeError, function() {
    iterator.next();
  });
});
