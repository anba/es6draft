/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertDataProperty
} = Assert;

// 6.2.4: Current PropertyDescriptor.[[Origin]] design hazards
// https://bugs.ecmascript.org/show_bug.cgi?id=2382

let propertyName = "propertyName";
let target = {[propertyName]: 0};
let p2 = new Proxy(target, {
  getOwnPropertyDescriptor(t, pk) {
    return {
      value: 1, writable: true, enumerable: true,
      get configurable() {
        delete this.configurable;
        this.configurable = false;
        return true;
      }
    };
  }
});

assertDataProperty(p2, propertyName, {value: 1, enumerable: true, writable: true, configurable: true});
