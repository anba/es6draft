/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertFalse, assertUndefined
} = Assert;

const global = this;


// Destructuring default values in Catch are evaluated in the lexical environment of the Catch node
try {
  throw {}
} catch ({a = eval("let letInCatch = 0;")}) {
}
assertSame("undefined", typeof letInCatch);
assertFalse("letInCatch" in global);
assertUndefined(global["letInCatch"]);


// Destructuring default values in ForOfStatement are evaluated in the lexical environment of the ForOfStatement node
for (let {a = eval("let letInForOf = 0;")} of [{}]) {
}
assertSame("undefined", typeof letInForOf);
assertFalse("letInForOf" in global);
assertUndefined(global["letInForOf"]);


// Destructuring default values in ForInStatement are evaluated in the lexical environment of the ForInStatement node
var proxy = new Proxy({}, {
  *enumerate() {
    yield {};
  }
});
for (let {a = eval("let letInForIn = 0;")} in proxy) {
}
assertSame("undefined", typeof letInForIn);
assertFalse("letInForIn" in global);
assertUndefined(global["letInForIn"]);


// Destructuring default values in ArrayComprehension are evaluated in the surrounding
// lexical environment, not in the lexical environment of the ArrayComprehension
[for ({a = eval("let letInArrayCompr = 0;")} of [{}]) 123];
assertSame("number", typeof letInArrayCompr);
assertFalse("letInArrayCompr" in global);
assertUndefined(global["letInArrayCompr"]);


// Destructuring default values in GeneratorComprehension are evaluated in the
// lexical environment of the GeneratorComprehension's synthetic arrow function
[...(for ({a = eval("let letInGenCompr = 0;")} of [{}]) 123)];
assertSame("undefined", typeof letInGenCompr);
assertFalse("letInGenCompr" in global);
assertUndefined(global["letInGenCompr"]);
