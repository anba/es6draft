/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

// Test normal functions
function testFunctions() {
  function testStackTrace(f, expectedName) {
    let methodName;
    try {
      f();
    } catch (e) {
      [{methodName}] = e.stackTrace;
    }
    assertEq(methodName, expectedName);
  }

  function f1() { throw new Error() }

  var f2 = function() { throw new Error() };
  var f3_ = function f3() { throw new Error() };
  var f4 = () => { throw new Error() };

  let f5 = function() { throw new Error() };
  let f6_ = function f6() { throw new Error() };
  let f7 = () => { throw new Error() };

  const f8 = function() { throw new Error() };
  const f9_ = function f9() { throw new Error() };
  const f10 = () => { throw new Error() };

  let f11, f12, f13;
  f11 = function() { throw new Error() };
  f12_ = function f12() { throw new Error() };
  f13 = () => { throw new Error() };

  let [
    f14 = function() { throw new Error() },
    f15_ = function f15() { throw new Error() },
    f16 = () => { throw new Error() },
  ] = [];

  let {
    f17 = function() { throw new Error() },
    f18_ = function f18() { throw new Error() },
    f19 = () => { throw new Error() },
  } = {};

  let {
    a: f20 = function() { throw new Error() },
    a: f21_ = function f21() { throw new Error() },
    a: f22 = () => { throw new Error() },
  } = {};

  let f23, f24_, f25;
  [
    f23 = function() { throw new Error() },
    f24_ = function f24() { throw new Error() },
    f25 = () => { throw new Error() },
  ] = [];

  let f26, f27_, f28;
  ({
    f26 = function() { throw new Error() },
    f27_ = function f27() { throw new Error() },
    f28 = () => { throw new Error() },
  } = {});

  let f29, f30_, f31;
  ({
    a: f29 = function() { throw new Error() },
    a: f30_ = function f30() { throw new Error() },
    a: f31 = () => { throw new Error() },
  } = {});

  let id1 = "id1", id2 = "id2", id3 = "id3", id4 = "id4", id5 = "id5", id6 = "id6";
  let p = {
    p1: "p.p1",
    p2: "p.p2",
    p3: "p.p3",
    p4: "p.p4",
    p5: "p.p5",
    p6: "p.p6",
  };

  let o1 = {
    m1() { throw new Error() },
    get m2() { throw new Error() },
    set m3(x) { throw new Error() },
    m4: function() { throw new Error() },
    m5_: function m5() { throw new Error() },
    m6: () => { throw new Error() },
  };
  let o2 = {
    ["m1"]() { throw new Error() },
    get ["m2"]() { throw new Error() },
    set ["m3"](x) { throw new Error() },
    ["m4"]: function() { throw new Error() },
    ["m5_"]: function m5() { throw new Error() },
    ["m6"]: () => { throw new Error() },
  };
  let o3 = {
    [1]() { throw new Error() },
    get [2]() { throw new Error() },
    set [3](x) { throw new Error() },
    [4]: function() { throw new Error() },
    [5]: function m5() { throw new Error() },
    [6]: () => { throw new Error() },
  };
  let o4 = {
    [id1]() { throw new Error() },
    get [id2]() { throw new Error() },
    set [id3](x) { throw new Error() },
    [id4]: function() { throw new Error() },
    [id5]: function m5() { throw new Error() },
    [id6]: () => { throw new Error() },
  };
  let o5 = {
    [p.p1]() { throw new Error() },
    get [p.p2]() { throw new Error() },
    set [p.p3](x) { throw new Error() },
    [p.p4]: function() { throw new Error() },
    [p.p5]: function m5() { throw new Error() },
    [p.p6]: () => { throw new Error() },
  };
  let o6 = {
    ["" + "c1"]() { throw new Error() },
    get ["" + "c2"]() { throw new Error() },
    set ["" + "c3"](x) { throw new Error() },
    ["" + "c4"]: function() { throw new Error() },
    ["" + "c5"]: function m5() { throw new Error() },
    ["" + "c6"]: () => { throw new Error() },
  };

  let q1 = {};
  q1.m1 = function() { throw new Error() };
  q1.m2_ = function m2() { throw new Error() };
  q1.m3 = () => { throw new Error() };
  let q2 = {};
  q2["m1"] = function() { throw new Error() };
  q2["m2_"] = function m2() { throw new Error() };
  q2["m3"] = () => { throw new Error() };
  let q3 = {};
  q3[1] = function() { throw new Error() };
  q3[2] = function m2() { throw new Error() };
  q3[3] = () => { throw new Error() };
  let q4 = {};
  q4[id1] = function() { throw new Error() };
  q4[id2] = function m2() { throw new Error() };
  q4[id3] = () => { throw new Error() };
  let q5 = {};
  q5[p.p1] = function() { throw new Error() };
  q5[p.p2] = function m2() { throw new Error() };
  q5[p.p3] = () => { throw new Error() };
  let q6 = {};
  q6["" + "c1"] = function() { throw new Error() };
  q6["" + "c2"] = function m2() { throw new Error() };
  q6["" + "c3"] = () => { throw new Error() };

  class C1 {
    m1() { throw new Error() }
    get m2() { throw new Error() }
    set m3(x) { throw new Error() }
    static m1() { throw new Error() }
    static get m2() { throw new Error() }
    static set m3(x) { throw new Error() }

    ["s1"]() { throw new Error() }
    get ["s2"]() { throw new Error() }
    set ["s3"](x) { throw new Error() }
    static ["s1"]() { throw new Error() }
    static get ["s2"]() { throw new Error() }
    static set ["s3"](x) { throw new Error() }

    [1]() { throw new Error() }
    get [2]() { throw new Error() }
    set [3](x) { throw new Error() }
    static [1]() { throw new Error() }
    static get [2]() { throw new Error() }
    static set [3](x) { throw new Error() }

    [id1]() { throw new Error() }
    get [id2]() { throw new Error() }
    set [id3](x) { throw new Error() }
    static [id1]() { throw new Error() }
    static get [id2]() { throw new Error() }
    static set [id3](x) { throw new Error() }

    [p.p1]() { throw new Error() }
    get [p.p2]() { throw new Error() }
    set [p.p3](x) { throw new Error() }
    static [p.p1]() { throw new Error() }
    static get [p.p2]() { throw new Error() }
    static set [p.p3](x) { throw new Error() }

    ["" + "c1"]() { throw new Error() }
    get ["" + "c2"]() { throw new Error() }
    set ["" + "c3"](x) { throw new Error() }
    static ["" + "c1"]() { throw new Error() }
    static get ["" + "c2"]() { throw new Error() }
    static set ["" + "c3"](x) { throw new Error() }
  }

  let C2 = class C2 {
    m1() { throw new Error() }
    get m2() { throw new Error() }
    set m3(x) { throw new Error() }
    static m1() { throw new Error() }
    static get m2() { throw new Error() }
    static set m3(x) { throw new Error() }

    ["s1"]() { throw new Error() }
    get ["s2"]() { throw new Error() }
    set ["s3"](x) { throw new Error() }
    static ["s1"]() { throw new Error() }
    static get ["s2"]() { throw new Error() }
    static set ["s3"](x) { throw new Error() }

    [1]() { throw new Error() }
    get [2]() { throw new Error() }
    set [3](x) { throw new Error() }
    static [1]() { throw new Error() }
    static get [2]() { throw new Error() }
    static set [3](x) { throw new Error() }

    [id1]() { throw new Error() }
    get [id2]() { throw new Error() }
    set [id3](x) { throw new Error() }
    static [id1]() { throw new Error() }
    static get [id2]() { throw new Error() }
    static set [id3](x) { throw new Error() }

    [p.p1]() { throw new Error() }
    get [p.p2]() { throw new Error() }
    set [p.p3](x) { throw new Error() }
    static [p.p1]() { throw new Error() }
    static get [p.p2]() { throw new Error() }
    static set [p.p3](x) { throw new Error() }

    ["" + "c1"]() { throw new Error() }
    get ["" + "c2"]() { throw new Error() }
    set ["" + "c3"](x) { throw new Error() }
    static ["" + "c1"]() { throw new Error() }
    static get ["" + "c2"]() { throw new Error() }
    static set ["" + "c3"](x) { throw new Error() }
  };

  let C3 = class {
    m1() { throw new Error() }
    get m2() { throw new Error() }
    set m3(x) { throw new Error() }
    static m1() { throw new Error() }
    static get m2() { throw new Error() }
    static set m3(x) { throw new Error() }

    ["s1"]() { throw new Error() }
    get ["s2"]() { throw new Error() }
    set ["s3"](x) { throw new Error() }
    static ["s1"]() { throw new Error() }
    static get ["s2"]() { throw new Error() }
    static set ["s3"](x) { throw new Error() }

    [1]() { throw new Error() }
    get [2]() { throw new Error() }
    set [3](x) { throw new Error() }
    static [1]() { throw new Error() }
    static get [2]() { throw new Error() }
    static set [3](x) { throw new Error() }

    [id1]() { throw new Error() }
    get [id2]() { throw new Error() }
    set [id3](x) { throw new Error() }
    static [id1]() { throw new Error() }
    static get [id2]() { throw new Error() }
    static set [id3](x) { throw new Error() }

    [p.p1]() { throw new Error() }
    get [p.p2]() { throw new Error() }
    set [p.p3](x) { throw new Error() }
    static [p.p1]() { throw new Error() }
    static get [p.p2]() { throw new Error() }
    static set [p.p3](x) { throw new Error() }

    ["" + "c1"]() { throw new Error() }
    get ["" + "c2"]() { throw new Error() }
    set ["" + "c3"](x) { throw new Error() }
    static ["" + "c1"]() { throw new Error() }
    static get ["" + "c2"]() { throw new Error() }
    static set ["" + "c3"](x) { throw new Error() }
  };

  class D1 {
    constructor() { throw new Error() }
  }
  let D2 = class D2 {
    constructor() { throw new Error() }
  };
  let D3 = class {
    constructor() { throw new Error() }
  };

  testStackTrace(f1, "f1");
  testStackTrace(f2, "f2");
  testStackTrace(f3_, "f3");
  testStackTrace(f4, "f4");
  testStackTrace(f5, "f5");
  testStackTrace(f6_, "f6");
  testStackTrace(f7, "f7");
  testStackTrace(f8, "f8");
  testStackTrace(f9_, "f9");
  testStackTrace(f10, "f10");
  testStackTrace(f11, "f11");
  testStackTrace(f12_, "f12");
  testStackTrace(f13, "f13");
  testStackTrace(f14, "f14");
  testStackTrace(f15_, "f15");
  testStackTrace(f16, "f16");
  testStackTrace(f17, "f17");
  testStackTrace(f18_, "f18");
  testStackTrace(f19, "f19");
  testStackTrace(f20, "f20");
  testStackTrace(f21_, "f21");
  testStackTrace(f22, "f22");
  testStackTrace(f23, "f23");
  testStackTrace(f24_, "f24");
  testStackTrace(f25, "f25");
  testStackTrace(f26, "f26");
  testStackTrace(f27_, "f27");
  testStackTrace(f28, "f28");
  testStackTrace(f29, "f29");
  testStackTrace(f30_, "f30");
  testStackTrace(f31, "f31");

  testStackTrace(o1.m1, "m1");
  testStackTrace(() => o1.m2, "get m2");
  testStackTrace(() => o1.m3 = 0, "set m3");
  testStackTrace(o1.m4, "m4");
  testStackTrace(o1.m5_, "m5");
  testStackTrace(o1.m6, "m6");

  testStackTrace(o2.m1, '["m1"]');
  testStackTrace(() => o2.m2, 'get ["m2"]');
  testStackTrace(() => o2.m3 = 0, 'set ["m3"]');
  testStackTrace(o2.m4, '["m4"]');
  testStackTrace(o2.m5_, "m5");
  testStackTrace(o2.m6, '["m6"]');

  testStackTrace(o3[1], '[1]');
  testStackTrace(() => o3[2], 'get [2]');
  testStackTrace(() => o3[3] = 0, 'set [3]');
  testStackTrace(o3[4], '[4]');
  testStackTrace(o3[5], "m5");
  testStackTrace(o3[6], '[6]');

  testStackTrace(o4[id1], '[id1]');
  testStackTrace(() => o4[id2], 'get [id2]');
  testStackTrace(() => o4[id3] = 0, 'set [id3]');
  testStackTrace(o4[id4], '[id4]');
  testStackTrace(o4[id5], "m5");
  testStackTrace(o4[id6], '[id6]');

  testStackTrace(o5[p.p1], '[p.p1]');
  testStackTrace(() => o5[p.p2], 'get [p.p2]');
  testStackTrace(() => o5[p.p3] = 0, 'set [p.p3]');
  testStackTrace(o5[p.p4], '[p.p4]');
  testStackTrace(o5[p.p5], "m5");
  testStackTrace(o5[p.p6], '[p.p6]');

  testStackTrace(o6["c1"], '[<...>]');
  testStackTrace(() => o6["c2"], 'get [<...>]');
  testStackTrace(() => o6["c3"] = 0, 'set [<...>]');
  testStackTrace(o6["c4"], '[<...>]');
  testStackTrace(o6["c5"], "m5");
  testStackTrace(o6["c6"], '[<...>]');

  testStackTrace(q1.m1, "q1.m1");
  testStackTrace(q1.m2_, "m2");
  testStackTrace(q1.m3, "q1.m3");

  testStackTrace(q2.m1, "q2.m1");
  testStackTrace(q2.m2_, "m2");
  testStackTrace(q2.m3, "q2.m3");

  testStackTrace(q3[1], "q3[1]");
  testStackTrace(q3[2], "m2");
  testStackTrace(q3[3], "q3[3]");

  testStackTrace(q4[id1], 'q4[id1]');
  testStackTrace(q4[id2], "m2");
  testStackTrace(q4[id3], 'q4[id3]');

  testStackTrace(q5[p.p1], 'q5[p.p1]');
  testStackTrace(q5[p.p2], "m2");
  testStackTrace(q5[p.p3], 'q5[p.p3]');

  testStackTrace(q6["c1"], "anonymous");
  testStackTrace(q6["c2"], "m2");
  testStackTrace(q6["c3"], "anonymous");

  testStackTrace((new C1).m1, "C1.m1");
  testStackTrace(() => (new C1).m2, "get C1.m2");
  testStackTrace(() => (new C1).m3 = 0, "set C1.m3");
  testStackTrace(C1.m1, "m1");
  testStackTrace(() => C1.m2, "get m2");
  testStackTrace(() => C1.m3 = 0, "set m3");

  testStackTrace((new C1)["s1"], 'C1["s1"]');
  testStackTrace(() => (new C1)["s2"], 'get C1["s2"]');
  testStackTrace(() => (new C1)["s3"] = 0, 'set C1["s3"]');
  testStackTrace(C1["s1"], '["s1"]');
  testStackTrace(() => C1["s2"], 'get ["s2"]');
  testStackTrace(() => C1["s3"] = 0, 'set ["s3"]');

  testStackTrace((new C1)[1], "C1[1]");
  testStackTrace(() => (new C1)[2], "get C1[2]");
  testStackTrace(() => (new C1)[3] = 0, "set C1[3]");
  testStackTrace(C1[1], "[1]");
  testStackTrace(() => C1[2], "get [2]");
  testStackTrace(() => C1[3] = 0, "set [3]");

  testStackTrace((new C1)[id1], "C1[id1]");
  testStackTrace(() => (new C1)[id2], "get C1[id2]");
  testStackTrace(() => (new C1)[id3] = 0, "set C1[id3]");
  testStackTrace(C1[id1], "[id1]");
  testStackTrace(() => C1[id2], "get [id2]");
  testStackTrace(() => C1[id3] = 0, "set [id3]");

  testStackTrace((new C1)[p.p1], "C1[p.p1]");
  testStackTrace(() => (new C1)[p.p2], "get C1[p.p2]");
  testStackTrace(() => (new C1)[p.p3] = 0, "set C1[p.p3]");
  testStackTrace(C1[p.p1], "[p.p1]");
  testStackTrace(() => C1[p.p2], "get [p.p2]");
  testStackTrace(() => C1[p.p3] = 0, "set [p.p3]");

  testStackTrace((new C1)["c1"], 'C1[<...>]');
  testStackTrace(() => (new C1)["c2"], 'get C1[<...>]');
  testStackTrace(() => (new C1)["c3"] = 0, 'set C1[<...>]');
  testStackTrace(C1["c1"], '[<...>]');
  testStackTrace(() => C1["c2"], 'get [<...>]');
  testStackTrace(() => C1["c3"] = 0, 'set [<...>]');

  testStackTrace((new C2).m1, "C2.m1");
  testStackTrace(() => (new C2).m2, "get C2.m2");
  testStackTrace(() => (new C2).m3 = 0, "set C2.m3");
  testStackTrace(C2.m1, "m1");
  testStackTrace(() => C2.m2, "get m2");
  testStackTrace(() => C2.m3 = 0, "set m3");

  testStackTrace((new C2)["s1"], 'C2["s1"]');
  testStackTrace(() => (new C2)["s2"], 'get C2["s2"]');
  testStackTrace(() => (new C2)["s3"] = 0, 'set C2["s3"]');
  testStackTrace(C2["s1"], '["s1"]');
  testStackTrace(() => C2["s2"], 'get ["s2"]');
  testStackTrace(() => C2["s3"] = 0, 'set ["s3"]');

  testStackTrace((new C2)[1], "C2[1]");
  testStackTrace(() => (new C2)[2], "get C2[2]");
  testStackTrace(() => (new C2)[3] = 0, "set C2[3]");
  testStackTrace(C2[1], "[1]");
  testStackTrace(() => C2[2], "get [2]");
  testStackTrace(() => C2[3] = 0, "set [3]");

  testStackTrace((new C2)[id1], "C2[id1]");
  testStackTrace(() => (new C2)[id2], "get C2[id2]");
  testStackTrace(() => (new C2)[id3] = 0, "set C2[id3]");
  testStackTrace(C2[id1], "[id1]");
  testStackTrace(() => C2[id2], "get [id2]");
  testStackTrace(() => C2[id3] = 0, "set [id3]");

  testStackTrace((new C2)[p.p1], "C2[p.p1]");
  testStackTrace(() => (new C2)[p.p2], "get C2[p.p2]");
  testStackTrace(() => (new C2)[p.p3] = 0, "set C2[p.p3]");
  testStackTrace(C2[p.p1], "[p.p1]");
  testStackTrace(() => C2[p.p2], "get [p.p2]");
  testStackTrace(() => C2[p.p3] = 0, "set [p.p3]");

  testStackTrace((new C2)["c1"], 'C2[<...>]');
  testStackTrace(() => (new C2)["c2"], 'get C2[<...>]');
  testStackTrace(() => (new C2)["c3"] = 0, 'set C2[<...>]');
  testStackTrace(C2["c1"], '[<...>]');
  testStackTrace(() => C2["c2"], 'get [<...>]');
  testStackTrace(() => C2["c3"] = 0, 'set [<...>]');

  testStackTrace((new C3).m1, "C3.m1");
  testStackTrace(() => (new C3).m2, "get C3.m2");
  testStackTrace(() => (new C3).m3 = 0, "set C3.m3");
  testStackTrace(C3.m1, "m1");
  testStackTrace(() => C3.m2, "get m2");
  testStackTrace(() => C3.m3 = 0, "set m3");

  testStackTrace((new C3)["s1"], 'C3["s1"]');
  testStackTrace(() => (new C3)["s2"], 'get C3["s2"]');
  testStackTrace(() => (new C3)["s3"] = 0, 'set C3["s3"]');
  testStackTrace(C3["s1"], '["s1"]');
  testStackTrace(() => C3["s2"], 'get ["s2"]');
  testStackTrace(() => C3["s3"] = 0, 'set ["s3"]');

  testStackTrace((new C3)[1], "C3[1]");
  testStackTrace(() => (new C3)[2], "get C3[2]");
  testStackTrace(() => (new C3)[3] = 0, "set C3[3]");
  testStackTrace(C3[1], "[1]");
  testStackTrace(() => C3[2], "get [2]");
  testStackTrace(() => C3[3] = 0, "set [3]");

  testStackTrace((new C3)[id1], "C3[id1]");
  testStackTrace(() => (new C3)[id2], "get C3[id2]");
  testStackTrace(() => (new C3)[id3] = 0, "set C3[id3]");
  testStackTrace(C3[id1], "[id1]");
  testStackTrace(() => C3[id2], "get [id2]");
  testStackTrace(() => C3[id3] = 0, "set [id3]");

  testStackTrace((new C3)[p.p1], "C3[p.p1]");
  testStackTrace(() => (new C3)[p.p2], "get C3[p.p2]");
  testStackTrace(() => (new C3)[p.p3] = 0, "set C3[p.p3]");
  testStackTrace(C3[p.p1], "[p.p1]");
  testStackTrace(() => C3[p.p2], "get [p.p2]");
  testStackTrace(() => C3[p.p3] = 0, "set [p.p3]");

  testStackTrace((new C3)["c1"], 'C3[<...>]');
  testStackTrace(() => (new C3)["c2"], 'get C3[<...>]');
  testStackTrace(() => (new C3)["c3"] = 0, 'set C3[<...>]');
  testStackTrace(C3["c1"], '[<...>]');
  testStackTrace(() => C3["c2"], 'get [<...>]');
  testStackTrace(() => C3["c3"] = 0, 'set [<...>]');

  testStackTrace(() => (new D1), "D1");
  testStackTrace(() => (new D2), "D2");
  testStackTrace(() => (new D3), "D3");
}
testFunctions();

// Test generator functions
function testGenerators() {
  function testStackTrace(f, expectedName) {
    let methodName;
    try {
      f().next();
    } catch (e) {
      [{methodName}] = e.stackTrace;
    }
    assertEq(methodName, expectedName);
  }

  function* f1() { throw new Error() }

  var f2 = function*() { throw new Error() };
  var f3_ = function* f3() { throw new Error() };
  var f4 = (for (v of 0) v);

  let f5 = function*() { throw new Error() };
  let f6_ = function* f6() { throw new Error() };
  let f7 = (for (v of 0) v);

  const f8 = function*() { throw new Error() };
  const f9_ = function* f9() { throw new Error() };
  const f10 = (for (v of 0) v);

  let f11, f12, f13;
  f11 = function*() { throw new Error() };
  f12_ = function* f12() { throw new Error() };
  f13 = (for (v of 0) v);

  let [
    f14 = function*() { throw new Error() },
    f15_ = function* f15() { throw new Error() },
    f16 = (for (v of 0) v),
  ] = [];

  let {
    f17 = function*() { throw new Error() },
    f18_ = function* f18() { throw new Error() },
    f19 = (for (v of 0) v),
  } = {};

  let {
    a: f20 = function*() { throw new Error() },
    a: f21_ = function* f21() { throw new Error() },
    a: f22 = (for (v of 0) v),
  } = {};

  let f23, f24_, f25;
  [
    f23 = function*() { throw new Error() },
    f24_ = function* f24() { throw new Error() },
    f25 = (for (v of 0) v),
  ] = [];

  let f26, f27_, f28;
  ({
    f26 = function*() { throw new Error() },
    f27_ = function* f27() { throw new Error() },
    f28 = (for (v of 0) v),
  } = {});

  let f29, f30_, f31;
  ({
    a: f29 = function*() { throw new Error() },
    a: f30_ = function* f30() { throw new Error() },
    a: f31 = (for (v of 0) v),
  } = {});

  let id1 = "id1", id2 = "id2", id3 = "id3", id4 = "id4", id5 = "id5", id6 = "id6";
  let p = {
    p1: "p.p1",
    p2: "p.p2",
    p3: "p.p3",
    p4: "p.p4",
    p5: "p.p5",
    p6: "p.p6",
  };

  let o1 = {
    *m1() { throw new Error() },
    m4: function*() { throw new Error() },
    m5_: function* m5() { throw new Error() },
    m6: (for (v of 0) v),
  };
  let o2 = {
    *["m1"]() { throw new Error() },
    ["m4"]: function*() { throw new Error() },
    ["m5_"]: function* m5() { throw new Error() },
    ["m6"]: (for (v of 0) v),
  };
  let o3 = {
    *[1]() { throw new Error() },
    [4]: function*() { throw new Error() },
    [5]: function* m5() { throw new Error() },
    [6]: (for (v of 0) v),
  };
  let o4 = {
    *[id1]() { throw new Error() },
    [id4]: function*() { throw new Error() },
    [id5]: function* m5() { throw new Error() },
    [id6]: (for (v of 0) v),
  };
  let o5 = {
    *[p.p1]() { throw new Error() },
    [p.p4]: function*() { throw new Error() },
    [p.p5]: function* m5() { throw new Error() },
    [p.p6]: (for (v of 0) v),
  };
  let o6 = {
    *["" + "c1"]() { throw new Error() },
    ["" + "c4"]: function*() { throw new Error() },
    ["" + "c5"]: function* m5() { throw new Error() },
    ["" + "c6"]: (for (v of 0) v),
  };

  let q1 = {};
  q1.m1 = function*() { throw new Error() };
  q1.m2_ = function* m2() { throw new Error() };
  q1.m3 = (for (v of 0) v);
  let q2 = {};
  q2["m1"] = function*() { throw new Error() };
  q2["m2_"] = function* m2() { throw new Error() };
  q2["m3"] = (for (v of 0) v);
  let q3 = {};
  q3[1] = function*() { throw new Error() };
  q3[2] = function* m2() { throw new Error() };
  q3[3] = (for (v of 0) v);
  let q4 = {};
  q4[id1] = function*() { throw new Error() };
  q4[id2] = function* m2() { throw new Error() };
  q4[id3] = (for (v of 0) v);
  let q5 = {};
  q5[p.p1] = function*() { throw new Error() };
  q5[p.p2] = function* m2() { throw new Error() };
  q5[p.p3] = (for (v of 0) v);
  let q6 = {};
  q6["" + "c1"] = function*() { throw new Error() };
  q6["" + "c2"] = function* m2() { throw new Error() };
  q6["" + "c3"] = (for (v of 0) v);

  class C1 {
    *m1() { throw new Error() }
    static *m1() { throw new Error() }

    *["s1"]() { throw new Error() }
    static *["s1"]() { throw new Error() }

    *[1]() { throw new Error() }
    static *[1]() { throw new Error() }

    *[id1]() { throw new Error() }
    static *[id1]() { throw new Error() }

    *[p.p1]() { throw new Error() }
    static *[p.p1]() { throw new Error() }

    *["" + "c1"]() { throw new Error() }
    static *["" + "c1"]() { throw new Error() }
  }

  let C2 = class C2 {
    *m1() { throw new Error() }
    static *m1() { throw new Error() }

    *["s1"]() { throw new Error() }
    static *["s1"]() { throw new Error() }

    *[1]() { throw new Error() }
    static *[1]() { throw new Error() }

    *[id1]() { throw new Error() }
    static *[id1]() { throw new Error() }

    *[p.p1]() { throw new Error() }
    static *[p.p1]() { throw new Error() }

    *["" + "c1"]() { throw new Error() }
    static *["" + "c1"]() { throw new Error() }
  };

  let C3 = class {
    *m1() { throw new Error() }
    static *m1() { throw new Error() }

    *["s1"]() { throw new Error() }
    static *["s1"]() { throw new Error() }

    *[1]() { throw new Error() }
    static *[1]() { throw new Error() }

    *[id1]() { throw new Error() }
    static *[id1]() { throw new Error() }

    *[p.p1]() { throw new Error() }
    static *[p.p1]() { throw new Error() }

    *["" + "c1"]() { throw new Error() }
    static *["" + "c1"]() { throw new Error() }
  };

  testStackTrace(f1, "f1");
  testStackTrace(f2, "f2");
  testStackTrace(f3_, "f3");
  testStackTrace(() => f4, "f4");
  testStackTrace(f5, "f5");
  testStackTrace(f6_, "f6");
  testStackTrace(() => f7, "f7");
  testStackTrace(f8, "f8");
  testStackTrace(f9_, "f9");
  testStackTrace(() => f10, "f10");
  testStackTrace(f11, "f11");
  testStackTrace(f12_, "f12");
  testStackTrace(() => f13, "f13");
  testStackTrace(f14, "f14");
  testStackTrace(f15_, "f15");
  testStackTrace(() => f16, "f16");
  testStackTrace(f17, "f17");
  testStackTrace(f18_, "f18");
  testStackTrace(() => f19, "f19");
  testStackTrace(f20, "f20");
  testStackTrace(f21_, "f21");
  testStackTrace(() => f22, "f22");
  testStackTrace(f23, "f23");
  testStackTrace(f24_, "f24");
  testStackTrace(() => f25, "f25");
  testStackTrace(f26, "f26");
  testStackTrace(f27_, "f27");
  testStackTrace(() => f28, "f28");
  testStackTrace(f29, "f29");
  testStackTrace(f30_, "f30");
  testStackTrace(() => f31, "f31");

  testStackTrace(o1.m1, "m1");
  testStackTrace(o1.m4, "m4");
  testStackTrace(o1.m5_, "m5");
  testStackTrace(() => o1.m6, "m6");

  testStackTrace(o2.m1, '["m1"]');
  testStackTrace(o2.m4, '["m4"]');
  testStackTrace(o2.m5_, "m5");
  testStackTrace(() => o2.m6, '["m6"]');

  testStackTrace(o3[1], '[1]');
  testStackTrace(o3[4], '[4]');
  testStackTrace(o3[5], "m5");
  testStackTrace(() => o3[6], '[6]');

  testStackTrace(o4[id1], '[id1]');
  testStackTrace(o4[id4], '[id4]');
  testStackTrace(o4[id5], "m5");
  testStackTrace(() => o4[id6], '[id6]');

  testStackTrace(o5[p.p1], '[p.p1]');
  testStackTrace(o5[p.p4], '[p.p4]');
  testStackTrace(o5[p.p5], "m5");
  testStackTrace(() => o5[p.p6], '[p.p6]');

  testStackTrace(o6["c1"], '[<...>]');
  testStackTrace(o6["c4"], '[<...>]');
  testStackTrace(o6["c5"], "m5");
  testStackTrace(() => o6["c6"], '[<...>]');

  testStackTrace(q1.m1, "q1.m1");
  testStackTrace(q1.m2_, "m2");
  testStackTrace(() => q1.m3, "q1.m3");

  testStackTrace(q2.m1, "q2.m1");
  testStackTrace(q2.m2_, "m2");
  testStackTrace(() => q2.m3, "q2.m3");

  testStackTrace(q3[1], "q3[1]");
  testStackTrace(q3[2], "m2");
  testStackTrace(() => q3[3], "q3[3]");

  testStackTrace(q4[id1], 'q4[id1]');
  testStackTrace(q4[id2], "m2");
  testStackTrace(() => q4[id3], 'q4[id3]');

  testStackTrace(q5[p.p1], 'q5[p.p1]');
  testStackTrace(q5[p.p2], "m2");
  testStackTrace(() => q5[p.p3], 'q5[p.p3]');

  testStackTrace(q6["c1"], "anonymous");
  testStackTrace(q6["c2"], "m2");
  testStackTrace(() => q6["c3"], "gencompr");

  testStackTrace((new C1).m1, "C1.m1");
  testStackTrace(C1.m1, "m1");

  testStackTrace((new C1)["s1"], 'C1["s1"]');
  testStackTrace(C1["s1"], '["s1"]');

  testStackTrace((new C1)[1], "C1[1]");
  testStackTrace(C1[1], "[1]");

  testStackTrace((new C1)[id1], "C1[id1]");
  testStackTrace(C1[id1], "[id1]");

  testStackTrace((new C1)[p.p1], "C1[p.p1]");
  testStackTrace(C1[p.p1], "[p.p1]");

  testStackTrace((new C1)["c1"], 'C1[<...>]');
  testStackTrace(C1["c1"], '[<...>]');

  testStackTrace((new C2).m1, "C2.m1");
  testStackTrace(C2.m1, "m1");

  testStackTrace((new C2)["s1"], 'C2["s1"]');
  testStackTrace(C2["s1"], '["s1"]');

  testStackTrace((new C2)[1], "C2[1]");
  testStackTrace(C2[1], "[1]");

  testStackTrace((new C2)[id1], "C2[id1]");
  testStackTrace(C2[id1], "[id1]");

  testStackTrace((new C2)[p.p1], "C2[p.p1]");
  testStackTrace(C2[p.p1], "[p.p1]");

  testStackTrace((new C2)["c1"], 'C2[<...>]');
  testStackTrace(C2["c1"], '[<...>]');

  testStackTrace((new C3).m1, "C3.m1");
  testStackTrace(C3.m1, "m1");

  testStackTrace((new C3)["s1"], 'C3["s1"]');
  testStackTrace(C3["s1"], '["s1"]');

  testStackTrace((new C3)[1], "C3[1]");
  testStackTrace(C3[1], "[1]");

  testStackTrace((new C3)[id1], "C3[id1]");
  testStackTrace(C3[id1], "[id1]");

  testStackTrace((new C3)[p.p1], "C3[p.p1]");
  testStackTrace(C3[p.p1], "[p.p1]");

  testStackTrace((new C3)["c1"], 'C3[<...>]');
  testStackTrace(C3["c1"], '[<...>]');

}
testGenerators();

// Test async functions
function testAsync() {
  function reportFailure(reason) {
    let p = Promise.reject(reason);
    p.constructor = function(r) {
      r(() => {}, e => { throw e });
    };
    p.constructor[Symbol.species] = p.constructor;
    p.then();
  }
  function testStackTrace(f, expectedName) {
    f().then(
      () => {
        throw new Error("no exception");
      },
      e => {
        let [{methodName}] = e.stackTrace;
        assertEq(methodName, expectedName);
      }
    ).catch(reportFailure);
  }

  async function f1() { throw new Error() }

  var f2 = async function() { throw new Error() };
  var f3_ = async function f3() { throw new Error() };
  var f4 = async () => { throw new Error() };

  let f5 = async function() { throw new Error() };
  let f6_ = async function f6() { throw new Error() };
  let f7 = async () => { throw new Error() };

  const f8 = async function() { throw new Error() };
  const f9_ = async function f9() { throw new Error() };
  const f10 = async () => { throw new Error() };

  let f11, f12, f13;
  f11 = async function() { throw new Error() };
  f12_ = async function f12() { throw new Error() };
  f13 = async () => { throw new Error() };

  let [
    f14 = async function() { throw new Error() },
    f15_ = async function f15() { throw new Error() },
    f16 = async () => { throw new Error() },
  ] = [];

  let {
    f17 = async function() { throw new Error() },
    f18_ = async function f18() { throw new Error() },
    f19 = async () => { throw new Error() },
  } = {};

  let {
    a: f20 = async function() { throw new Error() },
    a: f21_ = async function f21() { throw new Error() },
    a: f22 = async () => { throw new Error() },
  } = {};

  let f23, f24_, f25;
  [
    f23 = async function() { throw new Error() },
    f24_ = async function f24() { throw new Error() },
    f25 = async () => { throw new Error() },
  ] = [];

  let f26, f27_, f28;
  ({
    f26 = async function() { throw new Error() },
    f27_ = async function f27() { throw new Error() },
    f28 = async () => { throw new Error() },
  } = {});

  let f29, f30_, f31;
  ({
    a: f29 = async function() { throw new Error() },
    a: f30_ = async function f30() { throw new Error() },
    a: f31 = async () => { throw new Error() },
  } = {});

  let id1 = "id1", id2 = "id2", id3 = "id3", id4 = "id4", id5 = "id5", id6 = "id6";
  let p = {
    p1: "p.p1",
    p2: "p.p2",
    p3: "p.p3",
    p4: "p.p4",
    p5: "p.p5",
    p6: "p.p6",
  };

  let o1 = {
    async m1() { throw new Error() },
    m4: async function() { throw new Error() },
    m5_: async function m5() { throw new Error() },
    m6: async () => { throw new Error() },
  };
  let o2 = {
    async ["m1"]() { throw new Error() },
    ["m4"]: async function() { throw new Error() },
    ["m5_"]: async function m5() { throw new Error() },
    ["m6"]: async () => { throw new Error() },
  };
  let o3 = {
    async [1]() { throw new Error() },
    [4]: async function() { throw new Error() },
    [5]: async function m5() { throw new Error() },
    [6]: async () => { throw new Error() },
  };
  let o4 = {
    async [id1]() { throw new Error() },
    [id4]: async function() { throw new Error() },
    [id5]: async function m5() { throw new Error() },
    [id6]: async () => { throw new Error() },
  };
  let o5 = {
    async [p.p1]() { throw new Error() },
    [p.p4]: async function() { throw new Error() },
    [p.p5]: async function m5() { throw new Error() },
    [p.p6]: async () => { throw new Error() },
  };
  let o6 = {
    async ["" + "c1"]() { throw new Error() },
    ["" + "c4"]: async function() { throw new Error() },
    ["" + "c5"]: async function m5() { throw new Error() },
    ["" + "c6"]: async () => { throw new Error() },
  };

  let q1 = {};
  q1.m1 = async function() { throw new Error() };
  q1.m2_ = async function m2() { throw new Error() };
  q1.m3 = async () => { throw new Error() };
  let q2 = {};
  q2["m1"] = async function() { throw new Error() };
  q2["m2_"] = async function m2() { throw new Error() };
  q2["m3"] = async () => { throw new Error() };
  let q3 = {};
  q3[1] = async function() { throw new Error() };
  q3[2] = async function m2() { throw new Error() };
  q3[3] = async () => { throw new Error() };
  let q4 = {};
  q4[id1] = async function() { throw new Error() };
  q4[id2] = async function m2() { throw new Error() };
  q4[id3] = async () => { throw new Error() };
  let q5 = {};
  q5[p.p1] = async function() { throw new Error() };
  q5[p.p2] = async function m2() { throw new Error() };
  q5[p.p3] = async () => { throw new Error() };
  let q6 = {};
  q6["" + "c1"] = async function() { throw new Error() };
  q6["" + "c2"] = async function m2() { throw new Error() };
  q6["" + "c3"] = async () => { throw new Error() };

  class C1 {
    async m1() { throw new Error() }
    static async m1() { throw new Error() }

    async ["s1"]() { throw new Error() }
    static async ["s1"]() { throw new Error() }

    async [1]() { throw new Error() }
    static async [1]() { throw new Error() }

    async [id1]() { throw new Error() }
    static async [id1]() { throw new Error() }

    async [p.p1]() { throw new Error() }
    static async [p.p1]() { throw new Error() }

    async ["" + "c1"]() { throw new Error() }
    static async ["" + "c1"]() { throw new Error() }
  }

  let C2 = class C2 {
    async m1() { throw new Error() }
    static async m1() { throw new Error() }

    async ["s1"]() { throw new Error() }
    static async ["s1"]() { throw new Error() }

    async [1]() { throw new Error() }
    static async [1]() { throw new Error() }

    async [id1]() { throw new Error() }
    static async [id1]() { throw new Error() }

    async [p.p1]() { throw new Error() }
    static async [p.p1]() { throw new Error() }

    async ["" + "c1"]() { throw new Error() }
    static async ["" + "c1"]() { throw new Error() }
  };

  let C3 = class {
    async m1() { throw new Error() }
    static async m1() { throw new Error() }

    async ["s1"]() { throw new Error() }
    static async ["s1"]() { throw new Error() }

    async [1]() { throw new Error() }
    static async [1]() { throw new Error() }

    async [id1]() { throw new Error() }
    static async [id1]() { throw new Error() }

    async [p.p1]() { throw new Error() }
    static async [p.p1]() { throw new Error() }

    async ["" + "c1"]() { throw new Error() }
    static async ["" + "c1"]() { throw new Error() }
  };

  testStackTrace(f1, "f1");
  testStackTrace(f2, "f2");
  testStackTrace(f3_, "f3");
  testStackTrace(f4, "f4");
  testStackTrace(f5, "f5");
  testStackTrace(f6_, "f6");
  testStackTrace(f7, "f7");
  testStackTrace(f8, "f8");
  testStackTrace(f9_, "f9");
  testStackTrace(f10, "f10");
  testStackTrace(f11, "f11");
  testStackTrace(f12_, "f12");
  testStackTrace(f13, "f13");
  testStackTrace(f14, "f14");
  testStackTrace(f15_, "f15");
  testStackTrace(f16, "f16");
  testStackTrace(f17, "f17");
  testStackTrace(f18_, "f18");
  testStackTrace(f19, "f19");
  testStackTrace(f20, "f20");
  testStackTrace(f21_, "f21");
  testStackTrace(f22, "f22");
  testStackTrace(f23, "f23");
  testStackTrace(f24_, "f24");
  testStackTrace(f25, "f25");
  testStackTrace(f26, "f26");
  testStackTrace(f27_, "f27");
  testStackTrace(f28, "f28");
  testStackTrace(f29, "f29");
  testStackTrace(f30_, "f30");
  testStackTrace(f31, "f31");

  testStackTrace(o1.m1, "m1");
  testStackTrace(o1.m4, "m4");
  testStackTrace(o1.m5_, "m5");
  testStackTrace(o1.m6, "m6");

  testStackTrace(o2.m1, '["m1"]');
  testStackTrace(o2.m4, '["m4"]');
  testStackTrace(o2.m5_, "m5");
  testStackTrace(o2.m6, '["m6"]');

  testStackTrace(o3[1], '[1]');
  testStackTrace(o3[4], '[4]');
  testStackTrace(o3[5], "m5");
  testStackTrace(o3[6], '[6]');

  testStackTrace(o4[id1], '[id1]');
  testStackTrace(o4[id4], '[id4]');
  testStackTrace(o4[id5], "m5");
  testStackTrace(o4[id6], '[id6]');

  testStackTrace(o5[p.p1], '[p.p1]');
  testStackTrace(o5[p.p4], '[p.p4]');
  testStackTrace(o5[p.p5], "m5");
  testStackTrace(o5[p.p6], '[p.p6]');

  testStackTrace(o6["c1"], '[<...>]');
  testStackTrace(o6["c4"], '[<...>]');
  testStackTrace(o6["c5"], "m5");
  testStackTrace(o6["c6"], '[<...>]');

  testStackTrace(q1.m1, "q1.m1");
  testStackTrace(q1.m2_, "m2");
  testStackTrace(q1.m3, "q1.m3");

  testStackTrace(q2.m1, "q2.m1");
  testStackTrace(q2.m2_, "m2");
  testStackTrace(q2.m3, "q2.m3");

  testStackTrace(q3[1], "q3[1]");
  testStackTrace(q3[2], "m2");
  testStackTrace(q3[3], "q3[3]");

  testStackTrace(q4[id1], 'q4[id1]');
  testStackTrace(q4[id2], "m2");
  testStackTrace(q4[id3], 'q4[id3]');

  testStackTrace(q5[p.p1], 'q5[p.p1]');
  testStackTrace(q5[p.p2], "m2");
  testStackTrace(q5[p.p3], 'q5[p.p3]');

  testStackTrace(q6["c1"], "anonymous");
  testStackTrace(q6["c2"], "m2");
  testStackTrace(q6["c3"], "anonymous");

  testStackTrace((new C1).m1, "C1.m1");
  testStackTrace(C1.m1, "m1");

  testStackTrace((new C1)["s1"], 'C1["s1"]');
  testStackTrace(C1["s1"], '["s1"]');

  testStackTrace((new C1)[1], "C1[1]");
  testStackTrace(C1[1], "[1]");

  testStackTrace((new C1)[id1], "C1[id1]");
  testStackTrace(C1[id1], "[id1]");

  testStackTrace((new C1)[p.p1], "C1[p.p1]");
  testStackTrace(C1[p.p1], "[p.p1]");

  testStackTrace((new C1)["c1"], 'C1[<...>]');
  testStackTrace(C1["c1"], '[<...>]');

  testStackTrace((new C2).m1, "C2.m1");
  testStackTrace(C2.m1, "m1");

  testStackTrace((new C2)["s1"], 'C2["s1"]');
  testStackTrace(C2["s1"], '["s1"]');

  testStackTrace((new C2)[1], "C2[1]");
  testStackTrace(C2[1], "[1]");

  testStackTrace((new C2)[id1], "C2[id1]");
  testStackTrace(C2[id1], "[id1]");

  testStackTrace((new C2)[p.p1], "C2[p.p1]");
  testStackTrace(C2[p.p1], "[p.p1]");

  testStackTrace((new C2)["c1"], 'C2[<...>]');
  testStackTrace(C2["c1"], '[<...>]');

  testStackTrace((new C3).m1, "C3.m1");
  testStackTrace(C3.m1, "m1");

  testStackTrace((new C3)["s1"], 'C3["s1"]');
  testStackTrace(C3["s1"], '["s1"]');

  testStackTrace((new C3)[1], "C3[1]");
  testStackTrace(C3[1], "[1]");

  testStackTrace((new C3)[id1], "C3[id1]");
  testStackTrace(C3[id1], "[id1]");

  testStackTrace((new C3)[p.p1], "C3[p.p1]");
  testStackTrace(C3[p.p1], "[p.p1]");

  testStackTrace((new C3)["c1"], 'C3[<...>]');
  testStackTrace(C3["c1"], '[<...>]');
}
testAsync();
