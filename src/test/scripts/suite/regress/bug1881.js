/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 15.3.4.2: .prototype for function instances is not configurable
// https://bugs.ecmascript.org/show_bug.cgi?id=1881

function F() { }
assertDataProperty(F, "prototype", {value: F.prototype, writable: true, enumerable: false, configurable: false});
