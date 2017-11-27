/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction, assertSame, assertNotSame, assertEquals, fail
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

/* Promise.prototype.finally ( onFinally ) */

assertBuiltinFunction(Promise.prototype.finally, "finally", 1);

// Promise already resolved
{
  let p1 = Promise.resolve("<value>");
  let p2 = p1.finally((...args) => {
    assertSame(0, args.length);
    return "ignored";
  });
  let p3 = p2.then(v => {
    assertSame("<value>", v);
  }, () => fail `reject handler called`);

  p3.catch(reportFailure);
}

// Promise already rejected
{
  let p1 = Promise.reject("<value>");
  let p2 = p1.finally((...args) => {
    assertSame(0, args.length);
    return "ignored";
  });
  let p3 = p2.then(() => fail `resolve handler called`, v => {
    assertSame("<value>", v);
  });

  p3.catch(reportFailure);
}

// Handler throws error (resolved promise)
{
  let p1 = Promise.resolve("<value>");
  let p2 = p1.finally(() => { throw "<error>"; });
  let p3 = p2.then(() => fail `resolve handler called`, v => {
    assertSame("<error>", v);
  });

  p3.catch(reportFailure);
}

// Handler throws error (rejected promise)
{
  let p1 = Promise.reject("<value>");
  let p2 = p1.finally(() => { throw "<error>"; });
  let p3 = p2.then(() => fail `resolve handler called`, v => {
    assertSame("<error>", v);
  });

  p3.catch(reportFailure);
}

// Argument is not callable (resolved promise)
{
  let p1 = Promise.resolve("<value>");
  let p2 = p1.finally(null);
  let p3 = p2.then(v => {
    assertSame("<value>", v);
  }, () => fail `reject handler called`);

  p3.catch(reportFailure);
}

// Argument is not callable (resolved promise)
{
  let p1 = Promise.reject("<value>");
  let p2 = p1.finally(null);
  let p3 = p2.then(() => fail `resolve handler called`, v => {
    assertSame("<value>", v);
  });

  p3.catch(reportFailure);
}

// Promise is resolved
{
  let deferred;
  let p1 = new Promise((resolve, reject) => { deferred = {resolve, reject, promise: this}; });
  let p2 = p1.finally((...args) => {
    assertSame(0, args.length);
    return "ignored";
  });
  let p3 = p2.then(v => {
    assertSame("<value>", v);
  }, () => fail `reject handler called`);

  p3.catch(reportFailure);

  deferred.resolve("<value>");
}

// Promise is rejected
{
  let deferred;
  let p1 = new Promise((resolve, reject) => { deferred = {resolve, reject, promise: this}; });
  let p2 = p1.finally((...args) => {
    assertSame(0, args.length);
    return "ignored";
  });
  let p3 = p2.then(() => fail `resolve handler called`, v => {
    assertSame("<value>", v);
  });

  p3.catch(reportFailure);

  deferred.reject("<value>");
}
