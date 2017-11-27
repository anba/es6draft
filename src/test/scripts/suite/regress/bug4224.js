/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 26.2.2.1 Proxy.revocable: "revoke" as data instead of method property?
// https://bugs.ecmascript.org/show_bug.cgi?id=4224

var r = Proxy.revocable({}, {});
assertDataProperty(r, "revoke", {value: r.revoke, writable: true, enumerable: true, configurable: true});
