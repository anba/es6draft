/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows
} = Assert;

// ReferenceError thrown on function entry if [[HomeObject]] not present

assertThrows(() => function(){ throw 0; super.x }(), ReferenceError);
assertThrows(() => function(a = (() => { throw 0 })){ super.x }(), ReferenceError);

assertThrows(() => function*(){ throw 0; super.x }(), ReferenceError);
assertThrows(() => function*(a = (() => { throw 0 })){ super.x }(), ReferenceError);
