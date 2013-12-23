/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function NumberExtensions(global) {
"use strict";

const {
  Object, Number, Math,
} = global;

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
    return Math.sign(number) * Math.floor(Math.abs(number));
  }
}), {
  toInteger: {enumerable: false}
});

})(this);
