/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, assertSame, assertTrue
} = Assert;

// Object.prototype.toString is fallible
{
  class ToStringError extends Error { }
  assertThrows(ToStringError, () => ({get [Symbol.toStringTag](){ throw new ToStringError }}).toString());
}

// Non-String values are changed to "???" (except for undefined)
for (let tag of [null, true, false, 0, 1, 1.4, 0/0, {}, function(){}, Symbol()]) {
  assertSame("[object ???]", {[Symbol.toStringTag]: tag}.toString());
}
assertSame("[object Object]", {[Symbol.toStringTag]: void 0}.toString());

// String values are accepted
for (let tag of ["", "MyObject"]) {
  assertSame(`[object ${tag}]`, {[Symbol.toStringTag]: tag}.toString());
}

// Restricted names are prepended with ~
for (let tag of ["Arguments", "Array", "Boolean", "Date", "Error", "Function", "Number", "RegExp", "String"]) {
  assertSame(`[object ~${tag}]`, {[Symbol.toStringTag]: tag}.toString());
}

// Restricted names are not prepended with ~ if the original built-in tag matches
let restricted = {
  Arguments: [function(){ return arguments }(), function(){ "use strict"; return arguments }()],
  Array: [[]],
  Boolean: [new Boolean, true, false],
  Date: [new Date],
  Error: [new Error, new TypeError],
  Function: [function(){}, function*(){}, () => {}, class {}, {m(){}}.m, function(){}.bind(null), new Proxy(function(){}, {}), Object.create],
  Number: [new Number, 0, 1, -1.5, -1/0, 1/0, 0/0],
  RegExp: [/./],
  String: [new String, "", "abc"],
};

const global = this;

for (let tag of ["Arguments", "Array", "Boolean", "Date", "Error", "Function", "Number", "RegExp", "String"]) {
  for (let kind of ["Arguments", "Array", "Boolean", "Date", "Error", "Function", "Number", "RegExp", "String"]) {
    for (let v of restricted[kind]) {
      const isPrimitive = (typeof v === "boolean" || typeof v === "number" || typeof v === "string");
      if (isPrimitive) {
        Object.defineProperty(global[kind].prototype, Symbol.toStringTag, {value: tag, configurable: true});
      } else {
        assertTrue(typeof v === "object" || typeof v === "function");
        Object.defineProperty(v, Symbol.toStringTag, {value: tag, configurable: true});
      }
      if (tag === kind) {
        assertSame(`[object ${tag}]`, Object.prototype.toString.call(v));
      } else {
        assertSame(`[object ~${tag}]`, Object.prototype.toString.call(v));
      }
      if (isPrimitive) {
        delete global[kind].prototype[Symbol.toStringTag];
      }
    }
  }
}
