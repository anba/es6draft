/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Internal_Error(global) {
"use strict";

const {
  Object, Function, InternalError, RangeError, Symbol,
} = global;

const instanceOf = Function.prototype.call.bind(Function.prototype[Symbol.hasInstance]);

/*
 * Make InternalError instances compatible to `instanceof` with RangeError constructor
 */
Object.defineProperty(RangeError, Symbol.hasInstance, {
  value(o) {
    return instanceOf(RangeError, o) || instanceOf(InternalError, o);
  }, configurable: true
});

})(this);
