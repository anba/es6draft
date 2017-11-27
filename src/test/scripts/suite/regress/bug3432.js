/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, assertNotUndefined, assertCallable
} = Assert;

// Accessing `RegExp.prototype.global` etc. directly must not throw to match existing implementations
// https://bugs.ecmascript.org/show_bug.cgi?id=3432

assertSame(undefined, RegExp.prototype.global);
assertSame(undefined, RegExp.prototype.ignoreCase);
assertSame(undefined, RegExp.prototype.multiline);
assertSame(undefined, RegExp.prototype.sticky);
assertSame(undefined, RegExp.prototype.unicode);
assertSame("(?:)", RegExp.prototype.source);
assertSame("", RegExp.prototype.flags);
assertSame("/(?:)/", RegExp.prototype.toString());


function callGetter(obj, propertyKey, thisValue) {
  let desc = Object.getOwnPropertyDescriptor(obj, propertyKey);
  assertNotUndefined(desc);
  assertCallable(desc.get);
  return desc.get.call(thisValue);
};

let foreignRegExpProto = new Reflect.Realm().global.RegExp.prototype;

assertThrows(TypeError, () => callGetter(RegExp.prototype, "global", foreignRegExpProto));
assertThrows(TypeError, () => callGetter(RegExp.prototype, "ignoreCase", foreignRegExpProto));
assertThrows(TypeError, () => callGetter(RegExp.prototype, "multiline", foreignRegExpProto));
assertThrows(TypeError, () => callGetter(RegExp.prototype, "sticky", foreignRegExpProto));
assertThrows(TypeError, () => callGetter(RegExp.prototype, "unicode", foreignRegExpProto));
assertThrows(TypeError, () => callGetter(RegExp.prototype, "source", foreignRegExpProto));
assertSame("", callGetter(RegExp.prototype, "flags", foreignRegExpProto));
assertSame("/(?:)/", RegExp.prototype.toString.call(foreignRegExpProto));
