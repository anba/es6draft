/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals, assertNotSame, assertSame
} = Assert;

// 15.19.4.3.1: Throw TypeError if [[GeneratorState]] is not undefined
// https://bugs.ecmascript.org/show_bug.cgi?id=1545

function* gen() { yield 0; yield 1; return 2 }

let g1 = gen();
assertEquals({value: 0, done: false}, g1.next());

let g2 = gen.call(g1);
assertNotSame(g1, g2);
assertEquals({value: 0, done: false}, g2.next());

let g3 = gen.call();
assertEquals({value: 0, done: false}, g3.next());

assertEquals({value: 1, done: false}, g1.next());
assertEquals({value: 1, done: false}, g2.next());
assertEquals({value: 1, done: false}, g3.next());

assertEquals({value: 2, done: true}, g1.next());
assertEquals({value: 2, done: true}, g2.next());
assertEquals({value: 2, done: true}, g3.next());
