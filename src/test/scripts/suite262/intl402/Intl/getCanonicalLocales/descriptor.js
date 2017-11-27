/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-intl.getcanonicallocales
description: >
  Intl.getCanonicalLocales property attributes.
info: >
  8.2.1 Intl.getCanonicalLocales (locales)

  17 ECMAScript Standard Built-in Objects:
    Every other data property described in clauses 18 through 26 and in
    Annex B.2 has the attributes { [[Writable]]: true, [[Enumerable]]: false,
    [[Configurable]]: true } unless otherwise specified.
includes: [propertyHelper.js]
---*/

verifyNotEnumerable(Intl, "getCanonicalLocales");
verifyWritable(Intl, "getCanonicalLocales");
verifyConfigurable(Intl, "getCanonicalLocales");
