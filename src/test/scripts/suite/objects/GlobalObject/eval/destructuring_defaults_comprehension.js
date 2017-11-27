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


// Destructuring default values in ArrayComprehension are evaluated a new lexical environment,
// not in the lexical environment of the ArrayComprehension
[for ({a = eval("let letInArrayCompr = 0;")} of [{}]) 123];
assertSame("undefined", typeof letInArrayCompr);
assertFalse("letInArrayCompr" in global);
assertUndefined(global["letInArrayCompr"]);


// Destructuring default values in GeneratorComprehension are evaluated a new lexical environment
[...(for ({a = eval("let letInGenCompr = 0;")} of [{}]) 123)];
assertSame("undefined", typeof letInGenCompr);
assertFalse("letInGenCompr" in global);
assertUndefined(global["letInGenCompr"]);
