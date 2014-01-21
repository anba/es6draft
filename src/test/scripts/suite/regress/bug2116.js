/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue, assertFalse,
  assertThrows,
} = Assert;

// 9.5.15 [[Construct]], 9.5.16 ProxyCreate: Only set [[Construct]] when target is constructor?
// https://bugs.ecmascript.org/show_bug.cgi?id=2116

assertThrows(() => new (new Proxy({}, {})), TypeError);
assertThrows(() => new (new Proxy(() => {}, {})), TypeError);
assertThrows(() => new (new Proxy({m(){}}.m, {})), TypeError);

function IsConstructor(o) {
  try {
    new (new Proxy(o, {construct: () => ({})}));
    return true;
  } catch (e) {
    return false;
  }
}

assertTrue(IsConstructor(Object));
assertTrue(IsConstructor(function(){}));
assertTrue(IsConstructor(function*(){}));

assertFalse(IsConstructor(Object.prototype));
assertFalse(IsConstructor(Object.prototype.toString));
assertFalse(IsConstructor(() => {}));
assertFalse(IsConstructor({m(){}}.m));
