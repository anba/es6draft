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

// Default Named ClassDeclaration
let {decorator, called, parameters} = CreateDecorator();
export default @decorator class Named { };
assertTrue(called());
assertEquals([Named], parameters());
