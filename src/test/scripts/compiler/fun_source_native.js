/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function getter(object, propertyKey) {
  var desc = Object.getOwnPropertyDescriptor(object, propertyKey);
  assertEq(desc != null, true);
  assertEq(desc.get != null, true);
  return desc.get;
}

function setter(object, propertyKey) {
  var desc = Object.getOwnPropertyDescriptor(object, propertyKey);
  assertEq(desc != null, true);
  assertEq(desc.set != null, true);
  return desc.set;
}

assertEq(Function.toString(), "function Function() { [native code] }");
assertEq(Object.toString(), "function Object() { [native code] }");
assertEq(Array.toString(), "function Array() { [native code] }");

assertEq(Function.prototype.toString(), "function () { [native code] }");
assertEq(Function.prototype.toString.toString(), "function toString() { [native code] }");
assertEq((function(){}).bind().toString(), "function BoundFunction() { [native code] }");

assertEq(Proxy.revocable({}, {}).revoke.toString(), "function () { [native code] }");

assertEq(new Proxy(function(){}, {}).toString(), "function () { [native code] }");
assertEq(new Proxy(() => {}, {}).toString(), "function () { [native code] }");
assertEq(new Proxy(class {}, {}).toString(), "function () { [native code] }");
assertEq(new Proxy(class { constructor() {} }, {}).toString(), "function () { [native code] }");

{
  let {proxy, revoke} = Proxy.revocable(function(){}, {});
  assertEq(Function.prototype.toString.call(proxy), "function () { [native code] }");
  revoke();
  assertEq(Function.prototype.toString.call(proxy), "function () { [native code] }");
}

assertEq(getter(Object.prototype, "__proto__").toString(), "function __proto__() { [native code] }");
assertEq(setter(Object.prototype, "__proto__").toString(), "function __proto__() { [native code] }");

assertEq(getter(Map.prototype, "size").toString(), "function size() { [native code] }");

assertEq(String.prototype[Symbol.iterator].toString(), "function iterator() { [native code] }");

assertEq(getter(Array, Symbol.species).toString(), "function species() { [native code] }");
assertEq(getter(RegExp, Symbol.species).toString(), "function species() { [native code] }");

var TypedArrayPrototype = Object.getPrototypeOf(Int8Array.prototype);
assertEq(getter(TypedArrayPrototype, Symbol.toStringTag).toString(), "function toStringTag() { [native code] }");

assertEq(getter(RegExp, "$&").toString(), "function () { [native code] }");
assertEq(getter(RegExp, "$+").toString(), "function () { [native code] }");
assertEq(getter(RegExp, "$`").toString(), "function () { [native code] }");
assertEq(getter(RegExp, "$'").toString(), "function () { [native code] }");
