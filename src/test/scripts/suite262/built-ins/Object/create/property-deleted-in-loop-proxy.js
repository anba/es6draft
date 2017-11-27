/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
/*---
esid: sec-objectdefineproperties
description: |
  Property key is deleted from properties object while collecting the property descriptors.
info: >
  19.1.2.2 Object.create ( O, Properties )
    ...
    3. If Properties is not undefined, then
      a. Return ? ObjectDefineProperties(obj, Properties).
    ...

  19.1.2.3.1 Runtime Semantics: ObjectDefineProperties ( O, Properties )
    ...
    3. Let keys be ? props.[[OwnPropertyKeys]]().
    ...
    5. For each element nextKey of keys in List order, do
      a. Let propDesc be ? props.[[GetOwnProperty]](nextKey).
      b. If propDesc is not undefined and propDesc.[[Enumerable]] is true, then
        i. Let descObj be ? Get(props, nextKey).
        ...
features: [Proxy]
---*/

var properties = {
  a: {value: "pass"},
  b: null
};

var visited = [];
var visitedGet = [];

var p = new Proxy(properties, {
  getOwnPropertyDescriptor: function(target, propertyKey) {
    visited.push(propertyKey);
    if (propertyKey === "a") {
      delete target.b;
    }
    return Object.getOwnPropertyDescriptor(target, propertyKey);
  },
  get: function(target, propertyKey, receiver) {
    visitedGet.push(propertyKey);
    return target[propertyKey];
  }
});

var obj = Object.create(null, p);

assert.sameValue(visited.length, 2);
assert.sameValue(visited[0], "a");
assert.sameValue(visited[1], "b");

assert.sameValue(visitedGet.length, 1);
assert.sameValue(visited[0], "a");

assert.sameValue(obj.a, "pass");
