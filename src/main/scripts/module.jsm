/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const defineProperty = Reflect.defineProperty;

function isPrimitive(v) {
  switch (typeof v) {
    case "object":
      return v === null;
    case "function":
      return false;
    default:
      return true;
  }
}

export default class Module {
  constructor(filename, dirname) {
    this.filename = filename;
    this.dirname = dirname;
    this.exports = {};
  }

  compile(moduleFn, require) {
    var {filename, dirname, exports} = this;
    %CallFunction(moduleFn, exports, exports, require, this, filename, dirname);

    // Handle `module.exports = value` and add implicit "default" export.
    var defaultExport = this.exports;
    if (isPrimitive(defaultExport)) {
      this.exports = {default: defaultExport};
    } else if (!("default" in defaultExport)) {
      // Non-enumerable for compatibility reasons.
      defineProperty(defaultExport, "default", {
        __proto__: null,
        value: defaultExport,
        writable: true, enumerable: false, configurable: true
      });
    }
  }
}

Object.freeze(Module);
Object.freeze(Module.prototype);
