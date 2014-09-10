/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Check new ArrayBuffer is not detached
// https://bugs.ecmascript.org/show_bug.cgi?id=3060

let buf = new ArrayBuffer(0);
buf.constructor = function Constructor(len) {
  let _new = new ArrayBuffer(len);
  // Internal API call to detach array buffer
  detachArrayBuffer(_new);
  return _new;
};
assertThrows(TypeError, () => buf.slice(0));
