/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

//  Insufficient/Incorrect duplicate computed property key checks
// https://bugs.ecmascript.org/show_bug.cgi?id=1863

(function(){
  let obj1 = {a: 1, ["a"]: 2};
  assertSame(2, obj1.a);

  let obj2 = {["a"]: 1, a: 2};
  assertSame(2, obj2.a);

  let obj3 = {_x: 0, get ["x"](){ return this._x }, set ["x"](v){ this._x = v }};
  assertSame(0, obj3.x);
  obj3.x = 1;
  assertSame(1, obj3.x);
})();

(function(){
  "use strict";

  let obj1 = {a: 1, ["a"]: 2};
  assertSame(2, obj1.a);

  let obj2 = {["a"]: 1, a: 2};
  assertSame(2, obj2.a);

  let obj3 = {_x: 0, get ["x"](){ return this._x }, set ["x"](v){ this._x = v }};
  assertSame(0, obj3.x);
  obj3.x = 1;
  assertSame(1, obj3.x);
})();
