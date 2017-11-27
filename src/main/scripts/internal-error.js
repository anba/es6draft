/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Internal_Error() {
"use strict";

const InternalError = %Intrinsic("InternalError");
const RangeError = %Intrinsic("RangeError");

const Symbol_hasInstance = %WellKnownSymbol("hasInstance");
const Function_prototype_hasInstance = %Intrinsic("FunctionPrototype")[Symbol_hasInstance];
const Object_defineProperty = %Intrinsic("Object").defineProperty;

// Make InternalError instances compatible to `instanceof RangeError`
Object_defineProperty(RangeError, Symbol_hasInstance, {
  value(o) {
    return %CallFunction(Function_prototype_hasInstance, RangeError, o) ||
           %CallFunction(Function_prototype_hasInstance, InternalError, o);
  },
  configurable: true
});

})();
