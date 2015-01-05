/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Internal_Error() {
"use strict";

const global = %GlobalObject();

const {
  Object, Function, InternalError, RangeError, Symbol,
} = global;

const hasInstance = Function.prototype[Symbol.hasInstance];

/*
 * Make InternalError instances compatible to `instanceof RangeError`
 */
Object.defineProperty(RangeError, Symbol.hasInstance, {
  value(o) {
    return %CallFunction(hasInstance, RangeError, o) || %CallFunction(hasInstance, InternalError, o);
  }, configurable: true
});

})();
