/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const $Promise = new Reflect.Realm().global.Promise;

export function reportFailure(reason) {
  let p = $Promise.reject(reason);
  p.constructor = function(r) {
    r(() => {}, e => { throw e });
  };
  p.constructor[Symbol.species] = p.constructor;
  p.then();
}

export function deferred(C) {
  let result = {};
  result.promise = new C((resolve, reject) => {
    Object.assign(result, {resolve, reject});
  });
  return result;
}
