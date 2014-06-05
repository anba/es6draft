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
  Object, Math, TypeError,
} = global;

const {
  max: Math_max,
  min: Math_min,
} = Math;

for (const type of ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"]) {
  const ctor = global[`${type}Array`];
  const {
    subarray, set
  } = ctor.prototype;

  /*
   * Add 'move' operation
   */
  Object.defineProperty(ctor.prototype, "move", {
    value(start, end, dest) {
      start = +start; end = +end; dest = +dest;
      const len = +this.length;
      if (end < 0) {
        end = len + end;
      }
      if (dest < 0) {
        dest = len + dest;
      }
      dest = Math_max(0, Math_min(len, dest));
      %CallFunction(set, this, %CallFunction(subarray, this, start, end), dest);
    },
    writable: true, enumerable: false, configurable: true
  });

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
