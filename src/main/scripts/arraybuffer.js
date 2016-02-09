/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function ArrayBufferExt() {
"use strict";

const global = %GlobalTemplate();

const {
  Object, ArrayBuffer, TypeError
} = global;

const ArrayBuffer_byteLength = Object.getOwnPropertyDescriptor(ArrayBuffer.prototype, "byteLength").get;

const get_byteLength = Object.getOwnPropertyDescriptor({
  get byteLength() {
    if (!%IsArrayBuffer(this)) {
      throw TypeError();
    }
    if (%IsDetachedBuffer(this)) {
      return 0;
    }
    return %CallFunction(ArrayBuffer_byteLength, this);
  }
}, "byteLength").get;

Object.defineProperty(ArrayBuffer.prototype, "byteLength", {
  get: get_byteLength,
  enumerable: false, configurable: true
});

})();
