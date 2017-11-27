/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// IsArray, IsCallable, IsConstructor, IsRegExp should all throw when used on a revoked proxy, just like Object.isExtensible does
// https://bugs.ecmascript.org/show_bug.cgi?id=3840

var {proxy, revoke} = Proxy.revocable({}, {});
revoke();
assertThrows(TypeError, () => Array.isArray(proxy));

var {proxy, revoke} = Proxy.revocable([], {});
revoke();
assertThrows(TypeError, () => Array.isArray(proxy));
