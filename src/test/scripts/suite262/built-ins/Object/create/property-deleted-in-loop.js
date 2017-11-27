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
      ...
      b. If propDesc is not undefined and propDesc.[[Enumerable]] is true, then
        i. Let descObj be ? Get(props, nextKey).
        ...
---*/

var properties = {
  get a() {
    delete this.b;
    return {value: "pass"};
  },
  b: null
};

var obj = Object.create(null, properties);

assert.sameValue(obj.a, "pass");
