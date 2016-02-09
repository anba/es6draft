/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertThrows
} = Assert;

// Clone revoke function, assert revocation still works
{
  let {proxy, revoke} = Proxy.revocable({prop: 0}, {});
  let revokeClone = revoke.toMethod({});
  assertNotSame(revoke, revokeClone);
  assertSame(0, proxy.prop);
  revokeClone();
  assertThrows(TypeError, () => proxy.prop);
}

// Call revoke, then cloned revoke
{
  let {proxy, revoke} = Proxy.revocable({prop: 0}, {});
  let revokeClone = revoke.toMethod({});
  revoke();
  revokeClone();
  assertThrows(TypeError, () => proxy.prop);
}

// Call cloned revoke, then revoke
{
  let {proxy, revoke} = Proxy.revocable({prop: 0}, {});
  let revokeClone = revoke.toMethod({});
  revokeClone();
  revoke();
  assertThrows(TypeError, () => proxy.prop);
}
