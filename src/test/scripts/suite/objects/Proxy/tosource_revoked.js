/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Call Function.prototype.toString() on revoked function proxy
{
  let {proxy, revoke} = Proxy.revocable(function(){}, {});
  // assertThrows(TypeError, () => Function.prototype.toString.call(proxy));
  assertSame("function () { [native code] }", Function.prototype.toString.call(proxy))
  revoke();
  // assertThrows(TypeError, () => Function.prototype.toString.call(proxy));
  assertSame("function () { [native code] }", Function.prototype.toString.call(proxy))
}
