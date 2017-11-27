/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
id: ...
info: ToPropertyKey is called before ToObject
description: >
  ...
---*/

var propertyKey = {
  toString: function() {
    throw new Test262Error();
  }  
};

assert.throws(Test262Error, function() {
  Object.prototype.propertyIsEnumerable.call(undefined, propertyKey);
});

assert.throws(Test262Error, function() {
  Object.prototype.propertyIsEnumerable.call(null, propertyKey);
});
