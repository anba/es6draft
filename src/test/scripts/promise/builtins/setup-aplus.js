/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
"use strict";

var adapter = {
  resolved(...args) {
    return Promise.resolve(...args);
  },
  rejected(...args) {
    return Promise.reject(...args);
  },
  deferred() {
    let resolve, reject, promise = new Promise((resolveArg, rejectArg) => {
      resolve = resolveArg;
      reject = rejectArg;
    });
    return {resolve, reject, promise};
  }
};

var {describe, specify, it, beforeEach, afterEach} = require("testapi");
