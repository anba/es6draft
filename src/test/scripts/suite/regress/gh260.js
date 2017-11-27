/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// dropping for-in initializers was sufficiently incompatible as to be reverted by browsers
// https://github.com/tc39/ecma262/pull/260

for (var i = 0 in {}) ;
assertSyntaxError(`for (var {} = 0 in {}) ;`);
assertSyntaxError(`for (var {i} = 0 in {}) ;`);
assertSyntaxError(`for (var [] = 0 in {}) ;`);
assertSyntaxError(`for (var [i] = 0 in {}) ;`);

assertSyntaxError(`for (let i = 0 in {}) ;`);
assertSyntaxError(`for (let {} = 0 in {}) ;`);
assertSyntaxError(`for (let {i} = 0 in {}) ;`);
assertSyntaxError(`for (let [] = 0 in {}) ;`);
assertSyntaxError(`for (let [i] = 0 in {}) ;`);

assertSyntaxError(`for (const i = 0 in {}) ;`);
assertSyntaxError(`for (const {} = 0 in {}) ;`);
assertSyntaxError(`for (const {i} = 0 in {}) ;`);
assertSyntaxError(`for (const [] = 0 in {}) ;`);
assertSyntaxError(`for (const [i] = 0 in {}) ;`);

assertSyntaxError(`for (i = 0 in {}) ;`);
assertSyntaxError(`for ({} = 0 in {}) ;`);
assertSyntaxError(`for ({i} = 0 in {}) ;`);
assertSyntaxError(`for ([] = 0 in {}) ;`);
assertSyntaxError(`for ([i] = 0 in {}) ;`);
