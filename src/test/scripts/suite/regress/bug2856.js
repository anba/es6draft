/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// 9.2.8 AddRestrictedFunctionProperties: Use %ThrowTypeError% from function's [[Realm]]?
// https://bugs.ecmascript.org/show_bug.cgi?id=2856

{
  let r = new Reflect.Realm()
  let f = r.eval("function f() {'use strict'} f")

  let foreignThrower = Object.getOwnPropertyDescriptor(f, "caller");
  assertUndefined(foreignThrower);
}
