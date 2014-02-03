/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 15.11.1, 15.11.6.1: Subclass initialisation for Error and NativeError
// https://bugs.ecmascript.org/show_bug.cgi?id=1663

class MyError extends Error { constructor(){} }
let err1 = new MyError();
assertSame(err1, TypeError.call(err1));

class MyTypeError extends TypeError { constructor(){} }
let err2 = new MyTypeError();
assertSame(err2, Error.call(err2));
