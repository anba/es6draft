/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function MathExtensions(global) {
"use strict";

const Object = global.Object,
      Math = global.Math;

Object.defineProperties(Object.assign(Math, {
  fround(v) {
    return Math.roundFloat32(v);
  }
}), {
  fround: {enumerable: false}
});

})(this);
