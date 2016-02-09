/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// 13.6.4.1 Early Errors: Allow duplicate names in binding pattern for non-lexical declarations
// https://bugs.ecmascript.org/show_bug.cgi?id=3175

for (var {a,a} in {}) ;
for (var [a,a] in {}) ;
for (var [[a],[a]] in {}) ;

for (var {a,a} of [{}]) ;
for (var [a,a] of [[]]) ;
for (var [[a],[a]] of [[[], []]]) ;

assertSyntaxError(`for (let {a,a} in {}) ;`);
assertSyntaxError(`for (let [a,a] in {}) ;`);
assertSyntaxError(`for (let [[a],[a]] in {}) ;`);

assertSyntaxError(`for (let {a,a} of [{}]) ;`);
assertSyntaxError(`for (let [a,a] of [[]]) ;`);
assertSyntaxError(`for (let [[a],[a]] of [[[], []]]) ;`);

assertSyntaxError(`for (const {a,a} in {}) ;`);
assertSyntaxError(`for (const [a,a] in {}) ;`);
assertSyntaxError(`for (const [[a],[a]] in {}) ;`);

assertSyntaxError(`for (const {a,a} of [{}]) ;`);
assertSyntaxError(`for (const [a,a] of [[]]) ;`);
assertSyntaxError(`for (const [[a],[a]] of [[[], []]]) ;`);
