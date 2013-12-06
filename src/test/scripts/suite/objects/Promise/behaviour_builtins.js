/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue,
  assertNativeFunction,
} = Assert;

// Test access to built-in functions:
// - DeferredConstructionFunction
// - ResolvePromiseFunction
// - RejectPromiseFunction
// - IdentityFunction
// - ThrowerFunction

// Simple, hack-ish way to break out of promise error handling...
function reportFailure(e) {
  nextTick(() => { throw e });
}

// Access to 'DeferredConstructionFunction'
{
  function F(resolver) {
    Object.assign(this, {resolver});
    resolver(() => {}, () => {});
  }

  let o = Promise.resolve.call(F);
  assertNativeFunction(o.resolver, undefined, 2);
}

// Access to 'ResolvePromiseFunction' and 'RejectPromiseFunction'
{
  let result = {};
  new Promise((resolve, reject) => {
    Object.assign(result, {resolve, reject});
  });
  assertNativeFunction(result.resolve, undefined, 1);
  assertNativeFunction(result.reject, undefined, 1);
}

// Access to 'IdentityFunction' and 'ThrowerFunction'
// Access to 'ResolvePromiseFunction' and 'RejectPromiseFunction'
{
  let skip = 2; // 2 = then + catch
  class XPromise extends Promise {
    then(fulfill, reject) {
      if (skip > 0) {
        skip -= 1;
      } else {
        assertNativeFunction(fulfill, undefined, 1);
        assertNativeFunction(reject, undefined, 1);
      }
      return super(fulfill, reject);
    }

    static deferred() {
      let result = {};
      result.promise = new this((resolve, reject) => {
        Object.assign(result, {resolve, reject});
      });
      return result;
    }
  }
  let d = XPromise.deferred();
  d.promise.then().catch(reportFailure);
  d.resolve(XPromise.resolve());
}
