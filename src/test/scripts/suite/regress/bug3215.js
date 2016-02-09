/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 7.3.11 SetIntegrityLevel(O, level): [[PreventExtensions]] must be triggered before [[OwnPropertyKeys]]
// https://bugs.ecmascript.org/show_bug.cgi?id=3215

function loggingProxy() {
  let logstring = "";
  let proxy = new Proxy({a:0, b:1}, new Proxy({}, {
    get(t, pk, r) {
      logstring += (logstring.length ? "," + pk : pk);
      return Reflect.get(t, pk, r);
    }
  }));
  return {proxy, log: () => logstring};
}

// SetIntegrityLevel (Object.freeze)
{
  let {proxy, log} = loggingProxy();
  Object.freeze(proxy);
  assertSame("preventExtensions,ownKeys,getOwnPropertyDescriptor,defineProperty,getOwnPropertyDescriptor,defineProperty", log());
}

// SetIntegrityLevel (Object.seal)
{
  let {proxy, log} = loggingProxy();
  Object.seal(proxy);
  assertSame("preventExtensions,ownKeys,defineProperty,defineProperty", log());
}
