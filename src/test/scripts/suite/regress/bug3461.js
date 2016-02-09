/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// 14.3.8 DefineMethod: Incorrect call to SetFunctionName
// https://bugs.ecmascript.org/show_bug.cgi?id=3461

class C {
  constructor(){}
}
assertSame("C", C.name);

assertSame("", class {constructor(){}}.name);
assertUndefined(Object.getOwnPropertyDescriptor(class {constructor(){}}, "name"));
