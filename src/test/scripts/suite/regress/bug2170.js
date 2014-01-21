/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 22.2.3.32 get % TypedArray%.prototype [ @@toStringTag ]: Missing .name for symbol-valued accessor
// https://bugs.ecmascript.org/show_bug.cgi?id=2170

let toStringTag = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(Int8Array.prototype), Symbol.toStringTag);
assertSame("get [Symbol.toStringTag]", toStringTag.get.name);
