/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const global = this;

let log = [];

Object.defineProperty(global, "property1", {
  set(v) {
    log.push({property1: v});
  }
});

Object.defineProperty(global, "property2", {
  set(v) {
    log.push({property2: v});
  }
});

export default log;
