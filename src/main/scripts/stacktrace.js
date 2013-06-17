/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Stacktrace(global) {
"use strict";

const Object = global.Object,
      Math = global.Math,
      Error = global.Error;

const Object_defineProperty = Object.defineProperty,
      Error_prototype_toString = Error.prototype.toString;

// stackTraceLimit defaults to 10
Error.stackTraceLimit = 10;

const getStackTrace = Object.getOwnPropertyDescriptor(Error.prototype, "stacktrace").get;

delete Error.prototype.stack;
Object.defineProperty(Error.prototype, "stack", {
  get() {
    var limit = Error.stackTraceLimit;
    if (typeof limit != 'number') {
      return;
    }
    var stacktrace = getStackTrace.call(this);
    if (!stacktrace) {
      return;
    }
    var out = Error_prototype_toString.call(this);
    var len = Math.min(stacktrace.length, Math.floor(limit));
    for (var i = 0; i < len; ++i) {
      var elem = stacktrace[i];
      out += `\n    at ${elem.methodName} (${elem.fileName}:${elem.lineNumber})`;
    }
    return out;
  },
  set(v) {
    Object_defineProperty(this, "stack", {
      value: v, writable: true, enumerable: true, configurable: true
    });
  },
  enumerable: false, configurable: true
});

})(this);
