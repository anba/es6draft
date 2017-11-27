/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Intl.getCanonicalLocales is a built-in function object.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)

  17 ECMAScript Standard Built-in Objects:
    Unless specified otherwise, a built-in object that is callable as
    a function is a built-in Function object with the characteristics
    described in 9.3. Unless specified otherwise, the [[Extensible]]
    internal slot of a built-in object initially has the value true.

    Unless otherwise specified every built-in function and every
    built-in constructor has the Function prototype object, which is
    the initial value of the expression Function.prototype (19.2.3),
    as the value of its [[Prototype]] internal slot.

    Built-in function objects that are not identified as constructors
    do not implement the [[Construct]] internal method unless otherwise
    specified in the description of a particular function.
---*/

assert.sameValue(typeof Intl.getCanonicalLocales, "function");

assert.sameValue(Object.getPrototypeOf(Intl.getCanonicalLocales), Function.prototype);

assert(Object.isExtensible(Intl.getCanonicalLocales));

assert.throws(TypeError, function() {
  new Intl.getCanonicalLocales();
});

assert.sameValue(Object.prototype.hasOwnProperty.call(Intl.getCanonicalLocales, "prototype"), false);
