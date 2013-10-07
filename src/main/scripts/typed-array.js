/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function TypedArray(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      Math = global.Math,
      TypeError = global.TypeError;

const Math_max = Math.max,
      Math_min = Math.min;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const types = ["Int8", "Uint8", "Uint8Clamped", "Int16", "Uint16", "Int32", "Uint32", "Float32", "Float64"];
const TypedArrays = [for (type of types) global[type + "Array"]];

[...TypedArrays].forEach(
  ctor => {
    const subarray = ctor.prototype.subarray,
          set = ctor.prototype.set;
    Object.defineProperty(ctor.prototype, "move", {
      value(start, end, dest) {
        start = +start; end = +end; dest = +dest;
        const len = this.length;
        if (end < 0) {
          end = len + end;
        }
        if (dest < 0) {
          dest = len + dest;
        }
        dest = Math_max(0, Math_min(len, dest));
        $CallFunction(set, this, $CallFunction(subarray, this, start, end), dest);
      },
      writable: true, enumerable: false, configurable: true
    });

    Object.defineProperty(ctor.prototype, "subarray", {
      value(begin, end) {
        if (!(this instanceof ctor)) throw new TypeError();
        return $CallFunction(subarray, this, begin, end);
      },
      writable: true, enumerable: false, configurable: true
    });
  }
);

})(this);
