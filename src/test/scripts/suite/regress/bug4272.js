/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertThrows
} = Assert;

// B.3.5 VariableStatements in Catch blocks: Missing "not" in step ii
// https://bugs.ecmascript.org/show_bug.cgi?id=4272

var e = "global";

function testVarCheck(code, blockValue, functionValueTest) {
  try { throw null } catch (e) {
    assertSame(null, e);
    eval(code);
    assertSame(blockValue, e);
  }
  assertTrue(functionValueTest(e));
}
function testNonStrict(code, value, functionValueTest = v => v === void 0) {
  return testVarCheck(code, value, functionValueTest);
}
function testStrict(code) {
  return testVarCheck(`"use strict"; ${code}`, null, v => v === "global");
}

testNonStrict("var e", null);
testStrict("'use strict'; var e");

testNonStrict("var e = 0", 0);
testStrict("'use strict'; var e = 0");

testNonStrict("for (var e; false; ) ;", null);
testStrict("'use strict'; for (var e; false; ) ;");

testNonStrict("for (var e = 0; false; ) ;", 0);
testStrict("'use strict'; for (var e = 0; false; ) ;");

testNonStrict("for (var e in {}) ;", null);
testStrict("'use strict'; for (var e in {}) ;");

testNonStrict("for (var e in {key: 1}) ;", "key");
testStrict("'use strict'; for (var e in {key: 1}) ;");

assertThrows(SyntaxError, () => testNonStrict("for (var e of []) ;"));
testStrict("'use strict'; for (var e of []) ;");

testNonStrict("function e() {}", null, v => typeof v === "function");
testStrict("'use strict'; function e() {}");
