/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 15.19.4.2.1: clarification for @@iterator of Generator instances needed
// https://bugs.ecmascript.org/show_bug.cgi?id=1567

function* g() {}
let gen = g.constructor.prototype.prototype;
let iterProto = Object.getPrototypeOf(gen);

assertDataProperty(iterProto, Symbol.iterator, {
  value: gen[Symbol.iterator], writable: true, enumerable: false, configurable: true
});
