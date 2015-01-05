/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
function assertEq(actual, expected) {
  if (actual !== expected) {
    throw new Error(`Expected '${expected}', but got '${actual}'`);
  }
}

function obj(n, r) {
  return {
    valueOf() {
      r.push(n);
      return n;
    },
    toString() {
      throw new Error();
    }
  };
}

function t1() {
  let r = [];
  let a = obj("a", r),
      b = obj("b", r);
  let m = a + ("" + b);
  assertEq(m, "ab");
  assertEq(r.join(""), "ba");
}
t1();

function t2() {
  let r = [];
  let a = obj("a", r),
      b = obj("b", r),
      c = obj("c", r);
  let m = ((a + ("" + b)) + c);
  assertEq(m, "abc");
  assertEq(r.join(""), "bac");
}
t2();

// Test concat[2-4]
{
function testConcat_2() {
  let r = [];
  let a = obj("a", r),
      b = obj("b", r),
      c = obj("c", r);
  let m = "" + a + b + c;
  assertEq(m, "abc");
  assertEq(r.join(""), "abc");
}
testConcat_2();

function testConcat_3() {
  let r = [];
  let a = obj("a", r),
      b = obj("b", r),
      c = obj("c", r);
  let m = "" + a + b + c;
  assertEq(m, "abc");
  assertEq(r.join(""), "abc");
}
testConcat_3();

function testConcat_4() {
  let r = [];
  let a = obj("a", r),
      b = obj("b", r),
      c = obj("c", r),
      d = obj("d", r);
  let m = "" + a + b + c + d;
  assertEq(m, "abcd");
  assertEq(r.join(""), "abcd");
}
testConcat_4();

function testConcatAll() {
  function* names(i) {
    for (let j = 0; j < i; ++j) {
      yield String.fromCharCode(0x61 + j);
    }
  }
  for (let i = 1; i <= 20; ++i) {
    let source = `
      let result = [];
      ${[...names(i)].map(name =>
          `let ${name} = obj("${name}", result);`
        ).join("\n")
      };
      let concat = ${[`""`, ...names(i)].join(" + ")};
      assertEq(concat, "${[...names(i)].join("")}");
      assertEq(result.join(""), "${[...names(i)].join("")}");
    `;
    Function(source)();
  }
}
testConcatAll();

function testConcatAllParens() {
  function* names(i) {
    for (let j = 0; j < i; ++j) {
      yield String.fromCharCode(0x61 + j);
    }
  }
  for (let i = 1; i <= 20; ++i) {
    let source = `
      let result = [];
      ${[...names(i)].map(name =>
          `let ${name} = obj("${name}", result);`
        ).join("\n")
      };
      let concat = ${[...names(i)].reduce((a, v) => `(${a} + ${v})`, `""`)};
      assertEq(concat, "${[...names(i)].join("")}");
      assertEq(result.join(""), "${[...names(i)].join("")}");
    `;
    Function(source)();
  }
}
testConcatAllParens();

}
