/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Intl.getCanonicalLocales.name is "getCanonicalLocales".
info: >
  8.2.1 Intl.getCanonicalLocales (locales)

  17 ECMAScript Standard Built-in Objects:
    Every built-in Function object, including constructors, that is not
    identified as an anonymous function has a name property whose value
    is a String. Unless otherwise specified, this value is the name that
    is given to the function in this specification. For functions that
    are specified as properties of objects, the name value is the property
    name string used to access the function.

    Unless otherwise specified, the name property of a built-in Function
    object, if it exists, has the attributes { [[Writable]]: false,
    [[Enumerable]]: false, [[Configurable]]: true }.
includes: [propertyHelper.js]
---*/

assert.sameValue(Intl.getCanonicalLocales.name, "getCanonicalLocales");

verifyNotEnumerable(Intl.getCanonicalLocales, "name");
verifyNotWritable(Intl.getCanonicalLocales, "name");
verifyConfigurable(Intl.getCanonicalLocales, "name");
