/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows, fail
} = Assert;

// 7.3.21 GetFunctionRealm: Handle revoked proxies
// https://bugs.ecmascript.org/show_bug.cgi?id=3018

{
  let {proxy, revoke} = Proxy.revocable(
    function() { fail `trap not called` },
    {apply() { return 123 }}
  );
  // Call toMethod on a bound function because that's the only code path where
  // GetFunctionRealm is not followed by another MOP.
  let bound = Function.prototype.bind.call(proxy, null);
  assertSame(123, bound());
  revoke();
  assertThrows(() => bound.toMethod({}), TypeError);
}
