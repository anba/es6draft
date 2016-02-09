/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertNotUndefined, assertCallable, assertThrows, assertEquals
} = Assert;

// 26.3.2 [ @@iterator ]: Missing type checks
// https://bugs.ecmascript.org/show_bug.cgi?id=3381

import* as self from "./bug3381.jsm";

var desc = Object.getOwnPropertyDescriptor(self, Symbol.iterator);
assertNotUndefined(desc);
assertNotUndefined(desc.value);

var {value: iter} = desc;
assertCallable(iter);

for (var v of [void 0, null, 0, "abc"]) {
  assertThrows(TypeError, () => iter.call(v));
}

for (var o of [{}, {a: 0}, {a: 0, b: 1}]) {
  assertEquals([...Reflect.enumerate(o)], [...iter.call(o)])
}
