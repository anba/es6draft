/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals, assertInstanceOf, fail
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

Array.prototype[Symbol.asyncIterator] = Array.prototype.values;

async function withNumbers() {
  var values = [];
  for await (var v of [1, 2]) values.push(v);
  return values;
}
withNumbers().then(values => assertEquals([1, 2], values)).catch(reportFailure);

async function withPromiseNumbers() {
  var values = [];
  for await (var v of [Promise.resolve(3), Promise.resolve(4)]) values.push(v);
  return values;
}
// withPromiseNumbers().then(values => assertEquals([3, 4], values)).catch(reportFailure);

async function withPromiseAndNumbers() {
  var values = [];
  for await (var v of [1, 2, Promise.resolve(3), Promise.resolve(4)]) values.push(v);
  return values;
}
// withPromiseAndNumbers().then(values => assertEquals([1, 2, 3, 4], values)).catch(reportFailure);

async function withRejectedPromise() {
  var values = [];
  for await (var v of [1, Promise.reject(2)]) if (v !== 1) fail `unreachable`;
  fail `unreachable`;
}
// withRejectedPromise().then(reportFailure, e => assertSame(2, e)).catch(reportFailure);

async function iteratorResultPromise() {
  var it = {
    [Symbol.asyncIterator]() {
      return {
        next() {
          return Promise.resolve({done: true, get value() { fail `unreachable`; }});
        }
      };
    }
  };
  for await (var v of it) ;
  return 0;
}
iteratorResultPromise().then(v => assertSame(0, v)).catch(reportFailure);

async function iteratorResultPromiseNotObject() {
  var it = {
    [Symbol.asyncIterator]() {
      return {
        next() {
          return Promise.resolve(0);
        }
      };
    }
  };
  for await (var v of it) ;
}
iteratorResultPromiseNotObject().then(reportFailure, e => assertInstanceOf(TypeError, e)).catch(reportFailure);
