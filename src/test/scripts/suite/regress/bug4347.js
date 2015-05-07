/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 17 ECMAScript Standard Built-in Objects: .length for functions without subclause headings?
// https://bugs.ecmascript.org/show_bug.cgi?id=4347

let promiseResolve, promiseReject;
new Promise((resolve, reject) => {
  promiseResolve = resolve;
  promiseReject = reject;
});
assertSame(1, promiseResolve.length);
assertSame(1, promiseReject.length);

let promiseCapabilitiesExecutor;
Promise.resolve.call(function(executor) {
  executor(() => {}, () => {});
  promiseCapabilitiesExecutor = executor;
}, 0);
assertSame(2, promiseCapabilitiesExecutor.length);

let promiseResolveElement;
Promise.all.call(class {
  constructor(executor) {
    executor(() => {}, () => {});
  }
  static resolve() {
    return {
      then(resolve) {
        promiseResolveElement = resolve;
      }
    };
  }
}, [null]);
assertSame(1, promiseResolveElement.length);

let {revoke} = Proxy.revocable({}, {});
assertSame(0, revoke.length);
