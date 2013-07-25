/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function ArrayExtensions(global) {
"use strict";

const Object = global.Object,
      Array = global.Array,
      TypeError = global.TypeError;

Object.defineProperties(Object.assign(Array, {
  build(c, f) {
    if (typeof c != 'number') {
      throw new TypeError();
    }
    var a = new Array(c);
    for (var i = 0, len = a.length; i < len; ++i) {
      a[i] = f(i);
    }
    return a;
  }
}), {
  build: {enumerable: false}
});

})(this);
