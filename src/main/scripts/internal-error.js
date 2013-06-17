/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Internal_Error(global) {
"use strict";

const Object = global.Object,
      Function = global.Function,
      InternalError = global.InternalError,
      RangeError = global.RangeError;

const instanceOf = Function.prototype.call.bind(Function.prototype[getSym("@@hasInstance")]);

Object.defineProperty(RangeError, getSym("@@hasInstance"), {
  value(o) {
    return instanceOf(RangeError, o) || instanceOf(InternalError, o);
  }, configurable: true
});

})(this);
