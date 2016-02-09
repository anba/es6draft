/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// Inconsistent `prototype` property descriptors
// https://bugs.ecmascript.org/show_bug.cgi?id=4247

function* g() { }

assertDataProperty(g, "prototype", {value: g.prototype, writable: true, enumerable: false, configurable: false});
