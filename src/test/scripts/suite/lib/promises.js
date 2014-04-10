/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Promises(global) {
"use strict";

const $Promise = new Reflect.Realm().global.Promise;

function reportFailure(reason) {
  let p = $Promise.reject(reason);
  p.constructor = function(r) {
    r(() => {}, e => { throw e });
  };
  p.then();
}

function deferred(C) {
  let result = {};
  result.promise = new C((resolve, reject) => {
    Object.assign(result, {resolve, reject});
  });
  return result;
}

// export...
Object.defineProperty(global, "Promises", {value: {
  reportFailure, deferred
}});

})(this);
