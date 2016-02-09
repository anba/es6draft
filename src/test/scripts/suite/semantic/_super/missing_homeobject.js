/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// ReferenceError thrown on function entry if [[HomeObject]] not present

// TODO: super property accessor no longer valid in normal functions/generators.

// assertThrows(ReferenceError, () => function(){ throw 0; super.x }());
// assertThrows(ReferenceError, () => function(a = (() => { throw 0 })){ super.x }());

// assertThrows(ReferenceError, () => function*(){ throw 0; super.x }());
// assertThrows(ReferenceError, () => function*(a = (() => { throw 0 })){ super.x }());
