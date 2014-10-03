/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function TypedArray() {
"use strict";

const global = %GlobalObject();

const {
  Object, TypeError,
} = global;

for (const type of ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"]) {
  const ctor = global[`${type}Array`];
  const {
    subarray
  } = ctor.prototype;

  /*
   * Remove polymorphism from 'subarray'
   */
  Object.defineProperty(ctor.prototype, "subarray", {
    value(begin, end) {
      if (!(this instanceof ctor)) throw TypeError();
      return %CallFunction(subarray, this, begin, end);
    },
    writable: true, enumerable: false, configurable: true
  });
}

})();
