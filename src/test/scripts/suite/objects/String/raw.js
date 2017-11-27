/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertDataProperty, assertBuiltinFunction, assertThrows,
  assertFalse, assertTrue,
} = Assert;


// String.raw

assertDataProperty(String, "raw", {enumerable: false, configurable: true, writable: true, value: String.raw});
assertBuiltinFunction(String.raw, "raw", 1);

assertThrows(TypeError, () => { String.raw() });
assertThrows(TypeError, () => { String.raw(null) });
assertThrows(TypeError, () => { String.raw({}) });

{
  let getterCalled = false;
  assertSame("", String.raw({get raw(){
    assertFalse(getterCalled);
    getterCalled = true;
    return {};
  }}));
  assertTrue(getterCalled);
}

{
 let getterCalled = false;
 assertSame("", String.raw({raw: {get length(){
    assertFalse(getterCalled);
    getterCalled = true;
    return 0;
  }}}));
  assertTrue(getterCalled);
}

{
  let valueOfCalled = false;
  assertSame("", String.raw({raw: {length: {valueOf() {
    assertFalse(valueOfCalled);
    valueOfCalled = true;
    return 0;
  }}}}));
  assertTrue(valueOfCalled);
}

assertSame("", String.raw({raw: {length: 0}}));
assertSame("", String.raw({raw: {length: 0/0}}));
// OOM: assertSame("", String.raw({raw: {length: +1/0}}));
assertSame("", String.raw({raw: {length: -1/0}}));

assertSame("A", String.raw({raw: {length: 1, '0': "A"}}));
assertSame("A", String.raw({raw: {length: 1, '0': "A"}}, "-"));
assertSame("A", String.raw({raw: {length: 1, '0': "A", '1': "B"}}, "-"));
assertSame("A-B", String.raw({raw: {length: 2, '0': "A", '1': "B"}}, "-"));
assertSame("A-B_C", String.raw({raw: {length: 3, '0': "A", '1': "B", '2': "C"}}, "-", "_"));

assertSame("undefined", String.raw({raw: {length: 1}}));
assertSame("undefined-undefined", String.raw({raw: {length: 2}}, "-"));
assertSame("A-undefined", String.raw({raw: {length: 2, '0': "A"}}, "-"));
assertSame("undefined-B", String.raw({raw: {length: 2, '1': "B"}}, "-"));

assertSame("AB", String.raw({raw: {length: 2, '0': "A", '1': "B"}}));

assertSame("ToString", String.raw({raw: {
  length: 1,
  '0': {
    toString() { return "ToString" },
    valueOf() { throw "ValueOf" }
  }
}}));

assertSame("AToStringB", String.raw({raw: {length: 2, '0': 'A', '1': 'B'}}, {
  toString() { return "ToString" },
  valueOf() { throw "ValueOf" }
}));

let log = "";
assertSame("A-B", String.raw({raw: {
  length: 2,
  '0': {toString() { log += "A"; return "A"; }},
  '1': {toString() { log += "B"; return "B"; }}
}}, {toString() { log += "-"; return "-"; }}));
assertSame("A-B", log);
