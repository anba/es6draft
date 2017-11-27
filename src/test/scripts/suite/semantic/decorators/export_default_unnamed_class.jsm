/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertEquals, assertUndefined,
} = Assert;

function CreateDecorator() {
  "use strict";
  let called = false, parameters;
  return {
    decorator(...rest) {
      assertUndefined(this);
      assertFalse(called);
      called = true;
      parameters = rest;
    },
    called() {
      return called;
    },
    parameters() {
      return parameters;
    },
  };
}

import {default as clazz} from "./export_default_unnamed_class.jsm";

// Default Unnamed ClassDeclaration
let {decorator, called, parameters} = CreateDecorator();
export default @decorator class { };
assertTrue(called());
assertEquals([clazz], parameters());
