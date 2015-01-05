/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Stacktrace() {
"use strict";

const global = %GlobalObject();

const {
  Object, Math, Error,
} = global;

const Object_defineProperty = Object.defineProperty,
      Error_prototype_toString = Error.prototype.toString,
      Math_floor = Math.floor,
      Math_min = Math.min;

// stackTraceLimit defaults to 10
Error.stackTraceLimit = 10;

const getStackTrace = Object.getOwnPropertyDescriptor(Error.prototype, "stackTrace").get;

delete Error.prototype.stack;
Object.defineProperty(Error.prototype, "stack", {
  get() {
    var limit = Error.stackTraceLimit;
    if (typeof limit != 'number') {
      return;
    }
    var stackTrace = %CallFunction(getStackTrace, this);
    if (!stackTrace) {
      return;
    }
    var out = %CallFunction(Error_prototype_toString, this);
    var len = Math_min(stackTrace.length, Math_floor(limit));
    for (var i = 0; i < len; ++i) {
      var elem = stackTrace[i];
      out += `\n    at ${elem.methodName} (${elem.fileName}:${elem.lineNumber})`;
    }
    return out;
  },
  set(v) {
    Object_defineProperty(this, "stack", {
      __proto__: null, value: v, writable: true, enumerable: true, configurable: true
    });
  },
  enumerable: false, configurable: true
});

})();
