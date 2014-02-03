/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertDataProperty
} = Assert;

// 15.1.4: WeakSet not exposed as constructor property on global object
// https://bugs.ecmascript.org/show_bug.cgi?id=1651

assertDataProperty(this, "WeakSet", {value: WeakSet, writable: true, enumerable: false, configurable: true});
