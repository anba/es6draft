/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// SpeciesConstructor: should not let null trigger defaults
// https://bugs.ecmascript.org/show_bug.cgi?id=3725

var buf = new ArrayBuffer(1);

buf.constructor = {
  [Symbol.species]: void 0
};
assertSame(ArrayBuffer.prototype, Object.getPrototypeOf(buf.slice(0)));

buf.constructor = {
  [Symbol.species]: null
};
assertSame(ArrayBuffer.prototype, Object.getPrototypeOf(buf.slice(0)));

class Buffer extends ArrayBuffer { }
buf.constructor = {
  [Symbol.species]: Buffer
};
assertSame(Buffer.prototype, Object.getPrototypeOf(buf.slice(0)));

buf.constructor = {
  [Symbol.species]: 0
};
assertThrows(TypeError, () => buf.slice(0));
