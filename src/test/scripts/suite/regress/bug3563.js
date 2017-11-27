/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertInstanceOf, assertUndefined, assertNotUndefined
} = Assert;

// 15.1.1
// https://bugs.ecmascript.org/show_bug.cgi?id=3563

// Direct eval, script code
var err;
try {
  eval("new.target");
} catch (e) {
  err = e;
}
assertInstanceOf(SyntaxError, err);

// Indirect eval, script code
var err;
try {
  (1, eval)("new.target");
} catch (e) {
  err = e;
}
assertInstanceOf(SyntaxError, err);

// Direct eval, function code ([[Call]])
(function() {
  var newTarget = eval("new.target");
  assertUndefined(newTarget);
})();

// Indirect eval, function code ([[Call]])
(function() {
  var err;
  try {
    (1, eval)("new.target");
  } catch (e) {
    err = e;
  }
  assertInstanceOf(SyntaxError, err);
})();

// Direct eval, function code ([[Construct]])
new function() {
  var newTarget = eval("new.target");
  assertNotUndefined(newTarget);
};

// Indirect eval, function code ([[Construct]])
new function() {
  var err;
  try {
    (1, eval)("new.target");
  } catch (e) {
    err = e;
  }
  assertInstanceOf(SyntaxError, err);
};
