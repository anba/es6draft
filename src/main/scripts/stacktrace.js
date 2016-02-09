/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Stacktrace() {
"use strict";

const global = %GlobalTemplate();

const {
  Object, Math, Error,
} = global;

const Object_defineProperty = Object.defineProperty,
      Object_setPrototypeOf = Object.setPrototypeOf,
      Error_prototype_toString = Error.prototype.toString,
      Math_floor = Math.floor,
      Math_min = Math.min;

// stackTraceLimit defaults to 10
Error.stackTraceLimit = 10;

const stackFrameProto = {
  getThis() {
    throw new Error("Not implemented");
  },
  getTypeName() {
    throw new Error("Not implemented");
  },
  getFunction() {
    throw new Error("Not implemented");
  },
  getFunctionName() {
    return this.methodName;
  },
  getMethodName() {
    throw new Error("Not implemented");
  },
  getFileName() {
    return this.fileName;
  },
  getLineNumber() {
    return this.lineNumber;
  },
  getColumnNumber() {
    throw new Error("Not implemented");
  },
  getEvalOrigin() {
    throw new Error("Not implemented");
  },
  isTopLevel() {
    throw new Error("Not implemented");
  },
  isEval() {
    throw new Error("Not implemented");
  },
  isNative() {
    throw new Error("Not implemented");
  },
  isConstructor() {
    throw new Error("Not implemented");
  },
};

const getStackTrace = Object.getOwnPropertyDescriptor(Error.prototype, "stackTrace").get;
var prepareStackTraceLock = false;

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
    var len = Math_min(stackTrace.length, Math_floor(limit));
    var prepare = Error.prepareStackTrace;
    if (!prepareStackTraceLock && typeof prepare == 'function') {
      prepareStackTraceLock = true;
      // Hide additional frames from user.
      if (len < stackTrace.length) {
        stackTrace.length = len;
      }
      // Add stack trace API methods.
      for (var i = 0; i < stackTrace.length; ++i) {
        %CallFunction(Object_setPrototypeOf, null, stackTrace[i], stackFrameProto);
      }
      try {
        return %CallFunction(prepare, Error, this, stackTrace);
      } finally {
        prepareStackTraceLock = false;
      }
    }
    var out = %CallFunction(Error_prototype_toString, this);
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
