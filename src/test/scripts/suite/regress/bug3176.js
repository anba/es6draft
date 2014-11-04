/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 13.6.4.5, 13.14.4 BindingInitialization: Add implicit ToObject instead of throwing TypeError?
// https://bugs.ecmascript.org/show_bug.cgi?id=3176

for (var [a] in {abc: 0}) ;
for (let [a] in {abc: 0}) ;
for (var [a] of ["abc"]) ;
for (let [a] of ["abc"]) ;
try { throw "abc"; } catch ([a]) {}
