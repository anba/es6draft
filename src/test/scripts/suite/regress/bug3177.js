/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// 13.6.4.9 ForIn/OfBodyEvaluation: Add ToObject or type check in step 3.f.iii.2
// https://bugs.ecmascript.org/show_bug.cgi?id=3177

var a; for ([a] in {abc: 0}) ;
var a; for ([a] of ["abc"]) ;
