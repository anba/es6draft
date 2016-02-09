/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.2.4.2: "Object" is censored
// https://bugs.ecmascript.org/show_bug.cgi?id=1148

const ObjectToString = Object.prototype.toString.call.bind(Object.prototype.toString);
assertSame("[object Object]", ObjectToString({[Symbol.toStringTag]: "Object"}));
assertSame("[object Object]", ObjectToString(Object.assign([], {[Symbol.toStringTag]: "Object"})));
