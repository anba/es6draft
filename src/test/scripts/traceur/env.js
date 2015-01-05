/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
traceur = {
  get(name) {
    if (name !== "./Options.js") {
      throw new Error("unknown module: " + name);
    }
    return class Options { };
  }
};

$traceurRuntime = {
  // 7.1.13 ToObject
  toObject(value) {
    if (value == null) {
      throw TypeError();
    }
    return Object(value);
  }
};
