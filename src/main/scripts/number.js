/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function NumberExtensions() {
"use strict";

const global = %GlobalObject();

const {
  Object, Number, Math,
} = global;

const {
  abs: Math_abs,
  floor: Math_floor,
  sign: Math_sign,
} = Math;

/*
 * Add 'Number.toInteger(v)'
 */
Object.defineProperties(Object.assign(Number, {
  toInteger(v) {
    var number = +v;
    if (number !== number) {
      return +0;
    }
    if (number === 0 || number === 1/0 || number === -1/0) {
      return number;
    }
    return Math_sign(number) * Math_floor(Math_abs(number));
  }
}), {
  toInteger: {enumerable: false}
});

})();
