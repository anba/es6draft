/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// Call Function.prototype.toString() on revoked function proxy
{
  let {proxy, revoke} = Proxy.revocable(function(){}, {});
  assertThrows(TypeError, () => Function.prototype.toString.call(proxy));
  revoke();
  assertThrows(TypeError, () => Function.prototype.toString.call(proxy));
}
