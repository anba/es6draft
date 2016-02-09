/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

// 9.5.14 [[Construct]] method of Proxy Object, step 9: missing argument newTarget
// https://bugs.ecmascript.org/show_bug.cgi?id=3553

var constructArgs = [];
var target = function(){};
var p = new Proxy(target, {
  construct(constructor, argumentsList, newTarget) {
    constructArgs.push({constructor, argumentsList, newTarget});
    return {};
  }
});

{
  new p();

  assertSame(1, constructArgs.length);
  let {constructor, argumentsList, newTarget} = constructArgs.pop();
  assertSame(target, constructor);
  assertEquals([], argumentsList);
  assertSame(p, newTarget);
}

{
  Reflect.construct(p, []);

  assertSame(1, constructArgs.length);
  let {constructor, argumentsList, newTarget} = constructArgs.pop();
  assertSame(target, constructor);
  assertEquals([], argumentsList);
  assertSame(p, newTarget);
}

{
  Reflect.construct(p, [], p);

  assertSame(1, constructArgs.length);
  let {constructor, argumentsList, newTarget} = constructArgs.pop();
  assertSame(target, constructor);
  assertEquals([], argumentsList);
  assertSame(p, newTarget);
}

{
  function f(){}
  Reflect.construct(p, [], f);

  assertSame(1, constructArgs.length);
  let {constructor, argumentsList, newTarget} = constructArgs.pop();
  assertSame(target, constructor);
  assertEquals([], argumentsList);
  assertSame(f, newTarget);
}
