/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertFalse, assertUndefined
} = Assert;

const global = this;


// Destructuring default values in Catch are evaluated in a new lexical environment
try {
  throw {}
} catch ({a = eval("let letInCatch = 0;")}) {
}
assertSame("undefined", typeof letInCatch);
assertFalse("letInCatch" in global);
assertUndefined(global["letInCatch"]);


// Destructuring default values in ForOfStatement are evaluated in a new lexical environment
for (let {a = eval("let letInForOf = 0;")} of [{}]) {
}
assertSame("undefined", typeof letInForOf);
assertFalse("letInForOf" in global);
assertUndefined(global["letInForOf"]);


// Destructuring default values in ForInStatement are evaluated in a new lexical environment
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
