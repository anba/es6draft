/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertNotSame,
  assertThrows,
} = Assert;

// 19.2.3.5 Function.prototype.toMethod: Call CloneMethod for all built-in function objects
// https://bugs.ecmascript.org/show_bug.cgi?id=2637

for (let builtin of [Object.prototype.toString, Array.prototype.join, Function]) {
  assertSame("function", typeof builtin.toMethod);

  // Throw TypeError if newHome is not Object
  for (let value of [void 0, null, true, false, 0, 1, 1.5, "", "abc", Symbol()]) {
    assertThrows(TypeError, () => builtin.toMethod(value));
  }

  // Call ToPropertyKey if methodName was passed
  class Err extends Error { }
  assertThrows(Err, () => builtin.toMethod({}, {toString(){ throw new Err }}));

  let clone1 = builtin.toMethod({});
  assertSame("function", typeof clone1);
  assertNotSame(builtin, clone1);

  let clone2 = builtin.toMethod({}, "m");
  assertSame("function", typeof clone2);
  assertNotSame(builtin, clone2);

  let clone3 = builtin.toMethod({}, Symbol());
  assertSame("function", typeof clone3);
  assertNotSame(builtin, clone3);
}
