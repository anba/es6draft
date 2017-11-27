/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertUndefined, assertDataProperty
} = Assert;

// 9.4.4.1 [[GetOwnProperty]], 9.4.4.3 [[Get]]: Strict caller restriction for environments without extension?
// https://bugs.ecmascript.org/show_bug.cgi?id=2718

(function() {
  let args = arguments;

  // .caller property not supported
  assertUndefined(args.caller);
  assertUndefined(Object.getOwnPropertyDescriptor(args, "caller"));

  // Set .caller to strict function
  var f = function(){ "use strict" };
  args.caller = f;

  // [[Get]] and [[GetOwnProperty]] are not supposed to throw
  assertSame(f, args.caller);
  assertDataProperty(args, "caller", {value: f, writable: true, enumerable: true, configurable: true});

  // Can delete .caller
  assertTrue(delete args.caller);
})();
