/*
 * Copyright (c) 2012-2014 André Bargull
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
  assertThrows(() => proxy.prop, TypeError);
}

// Call revoke, then cloned revoke
{
  let {proxy, revoke} = Proxy.revocable({prop: 0}, {});
  let revokeClone = revoke.toMethod({});
  revoke();
  revokeClone();
  assertThrows(() => proxy.prop, TypeError);
}

// Call cloned revoke, then revoke
{
  let {proxy, revoke} = Proxy.revocable({prop: 0}, {});
  let revokeClone = revoke.toMethod({});
  revokeClone();
  revoke();
  assertThrows(() => proxy.prop, TypeError);
}
