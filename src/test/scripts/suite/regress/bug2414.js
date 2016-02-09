/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// 22.2.3.32 get %TypedArray%.prototype[@@toStringTag]: Invalid assertion in step 5
// https://bugs.ecmascript.org/show_bug.cgi?id=2414

let typedArrayToStringTag = Object.getOwnPropertyDescriptor(Int8Array.__proto__.prototype, Symbol.toStringTag).get;
// assertUndefined(typedArrayToStringTag.call(new class extends Int8Array { constructor() { /* no super */ } }));
