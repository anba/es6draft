/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 12.14.5.2 DestructuringAssignmentEvaluation, 13.3.3.5 BindingInitialization: Different null/undefined checks
// https://bugs.ecmascript.org/show_bug.cgi?id=4351

assertThrows(TypeError, () => {
  with (new Proxy({}, {
    has(t, p) {
      fail `"has" trap executed`;
    }
  })) {
    var {a} = null;
  }
});
assertThrows(TypeError, () => {
  var {[fail `computed property name evaluated`]: a} = null;
});

assertThrows(TypeError, () => {
  var a;
  with (new Proxy({}, {
    has(t, p) {
      fail `"has" trap executed`;
    }
  })) {
    ({a} = null);
  }
});
assertThrows(TypeError, () => {
  var a;
  ({[fail `computed property name evaluated`]: a} = null);
});
