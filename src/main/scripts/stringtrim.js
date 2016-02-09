/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
(function StringTrim() {
"use strict";

if (!%IsCompatibilityOptionEnabled("StringTrim")) {
  const global = %GlobalTemplate();

  const {
    Object, String, TypeError
  } = global;

  const trimLeftRE = /^\s+/, trimRightRE = /\s+$/;

  /*
   * Add 'trimLeft' and 'trimRight' to String.prototype
   */
  Object.defineProperties(Object.assign(String.prototype, {
    trimLeft() {
      if (this == null) throw TypeError();
      return %RegExpReplace(trimLeftRE, %ToString(this), "");
    },
    trimRight() {
      if (this == null) throw TypeError();
      return %RegExpReplace(trimRightRE, %ToString(this), "");
    },
  }), {
    trimLeft: {enumerable: false},
    trimRight: {enumerable: false},
  });
}

})();
