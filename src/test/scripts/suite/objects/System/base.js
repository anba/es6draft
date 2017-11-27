/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined,
  assertNotUndefined,
  fail,
} = Assert;

System.load("lib/promises.jsm");
const {
  reportFailure
} = System.get("lib/promises.jsm");

var base = "objects/System/";
var moduleName = base + "./resources/empty.jsm";

System.load(moduleName)
      .then(v => assertUndefined(v))
      .catch(reportFailure);

System.define(moduleName, "# invalid source #")
      .then(v => assertNotUndefined(v))
      .catch(reportFailure);
