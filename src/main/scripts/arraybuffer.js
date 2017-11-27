/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function ArrayBufferExt() {
"use strict";

const ArrayBufferPrototype = %Intrinsic("ArrayBufferPrototype");
const ArrayBuffer_prototype_byteLength = %LookupGetter(ArrayBufferPrototype, "byteLength");

const TypeError = %Intrinsic("TypeError");

%CreateMethodProperties(ArrayBufferPrototype, {
  get byteLength() {
    if (!%IsArrayBuffer(this)) {
      throw TypeError();
    }
    if (%IsDetachedBuffer(this)) {
      return 0;
    }
    return %CallFunction(ArrayBuffer_prototype_byteLength, this);
  }
});

})();
