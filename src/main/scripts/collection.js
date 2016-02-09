/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function Collection() {
"use strict";

const global = %GlobalTemplate();

const {
  Object, WeakMap, WeakSet
} = global;

// Add WeakMap.prototype.clear
Object.defineProperty(Object.assign(WeakMap.prototype, {
  clear() {
    return %WeakMapClear(this);
  }
}), "clear", {enumerable: false});

// Add WeakSet.prototype.clear
Object.defineProperty(Object.assign(WeakSet.prototype, {
  clear() {
    return %WeakSetClear(this);
  }
}), "clear", {enumerable: false});

})();
