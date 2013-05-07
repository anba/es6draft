/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Stacktrace(global) {
"use strict";

const Object = global.Object,
      Error = global.Error;

const Object_defineOwnProperty = Object.defineOwnProperty,
      Error_prototype_toString = Error.prototype.toString;

const getStackTrace = Object.getOwnPropertyDescriptor(Error.prototype, "stacktrace").get;

delete Error.prototype.stack;
Object.defineProperty(Error.prototype, "stack", {
  get() {
    var stacktrace = getStackTrace.call(this);
    if (stacktrace) {
      var out = Error_prototype_toString.call(this);
      for (var i = 0, len = stacktrace.length; i < len; ++i) {
        var elem = stacktrace[i];
        out += `\n    at ${elem.methodName} (${elem.fileName}:${elem.lineNumber})`;
      }
      return out;
    }
  },
  set(v) {
    Object_defineOwnProperty(this, "stack", {
      value: v, writable: true, enumerable: true, configurable: true
    });
  },
  enumerable: false, configurable: true
});

})(this);
