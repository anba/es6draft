/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// B.3.5 VariableStatements in Catch blocks: Missing "not" in step ii
// https://bugs.ecmascript.org/show_bug.cgi?id=4272

var e = "global";

function testVarCheck(code, blockValue, functionValue) {
  try { throw null } catch (e) {
    assertSame(null, e);
    eval(code);
    assertSame(blockValue, e);
  }
  assertSame(functionValue, e);
}
function testNonStrict(code, value) {
  return testVarCheck(code, value, void 0);
}
function testStrict(code) {
  return testVarCheck(`"use strict"; ${code}`, null, "global");
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
testStrict("'use strict'; for (var e in {}) ;");

assertThrows(SyntaxError, () => testNonStrict("for (var e of []) ;"));
testStrict("'use strict'; for (var e of []) ;");

assertThrows(SyntaxError, () => testNonStrict("function e() {}"));
testStrict("'use strict'; for (var e in {}) ;");
