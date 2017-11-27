/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Stacktrace() {
"use strict";

const Object = %Intrinsic("Object");
const Math = %Intrinsic("Math");
const Error = %Intrinsic("Error");

const Object_setPrototypeOf = Object.setPrototypeOf,
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

const getStackTrace = %LookupGetter(Error.prototype, "stackTrace");
var prepareStackTraceLock = false;

%CreateMethodProperties(Error.prototype, {
  get stack() {
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
      try {
        // Hide additional frames from user.
        if (len < stackTrace.length) {
          stackTrace.length = len;
        }
        // Add stack trace API methods.
        for (var i = 0; i < len; ++i) {
          %CallFunction(Object_setPrototypeOf, null, stackTrace[i], stackFrameProto);
        }
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
  set stack(v) {
    %CreateDataPropertyOrThrow(this, "stack", v);
  }
});

})();
